package mms;

import jade.util.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import mms.Constants.ES_STATE;
import mms.clock.TimeUnit;
import mms.clock.VirtualClockHelper;
import mms.comm.Comm;
import mms.router.CommandClientInterface;

public abstract class EventServer implements Sensing, Acting, CommandClientInterface {

	/**
	 * Logger
	 */
	public static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	/**
	 * Lock
	 */
	private Lock lock = new ReentrantLock();
	private Lock outputLock = new ReentrantLock();

	/**
	 * Arguments
	 */
	Parameters parameters = null;

	/**
	 * Environment Agent
	 */
	protected EnvironmentAgent envAgent;

	/**
	 * Clock service
	 */
	protected VirtualClockHelper clock;

	/**
	 * Estado atual do EventServer
	 */
	private ES_STATE 	eventServerState 	= ES_STATE.CREATED; 
	
	/**
	 * Event Server's parameters
	 */
	private String 		eventType 			= "DUMMY";
	private String		eventExchange 		= Constants.EVT_EXC_SPORADIC;
	private String 		commType 			= "mms.comm.direct.CommDirect";
	private boolean		isBatch				= false;

	// Periodic Events' variables
	protected long 		period;
	protected long 		sendDeadline;
	protected long 		receiveDeadline;
	protected long 		periodDeadline;
	protected long 		startTime;
	protected long 		frameTime;
	protected long 		waitTime;
	
	protected int 		workingFrame;
	protected int 		happeningFrame;
	protected int 		receivingFrame;
	
	private	long 		nextStateChange;
	
	// TODO Melhorar forma de armazenamento
	/**
	 * Tabela de atuadores registrados, no formato agentName:agentComponentName
	 */
	protected ConcurrentHashMap<String, Parameters> actuators = new ConcurrentHashMap<String, Parameters>();
	/**
	 * Tabela de sensores registrados, no formato agentName:agentComponentName
	 */
	protected ConcurrentHashMap<String, Parameters> sensors   = new ConcurrentHashMap<String, Parameters>();
	
	protected ConcurrentHashMap<String, Parameters> eventHandlers = new ConcurrentHashMap<String, Parameters>();
	
	/**
	 * Armazena os eventos que chegam adiantados (que pertencem a uma janela no futuro)
	 */
	protected ArrayList<Event> earlyEvents = new ArrayList<Event>();
	
	protected Comm comm;

	protected Sensor mySensor;
	protected Actuator myActuator;
	
	private ArrayList<Event> inputEvents = new ArrayList<Event>();
	private ArrayList<Event> outputEvents = new ArrayList<Event>();
	
	// Statistics
	BufferedWriter out;
	private long partialchunksProcessed			= 0;
	private long totalChunksLost				= 0;
	private long totalChunksProcessed			= 0;
	private long totalChunksExpected			= 0;

	//--------------------------------------------------------------------------------
	// Event Server initialization
	//--------------------------------------------------------------------------------
	
	/**
	 * Called by the system to initalize an EventServer.
	 * Should not be called by the user!
	 */
	// TODO colocar um retorno para verificar se a configuração foi bem sucedida
	protected final boolean start(EnvironmentAgent envAgent, Parameters parameters) {
		
		logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Starting initialization...");

		// TODO Verificar se está configurado, caso contrário, não libera a inicialização 
		
		this.envAgent = envAgent;
		
		// Obtém o clock
		this.clock = envAgent.getClock();
		
		isBatch = envAgent.getProperty(Constants.PROCESS_MODE, null).equals(Constants.MODE_BATCH);
		
		eventServerState = ES_STATE.CONFIGURED;
				
		// Inicializa a interface de comunicação
		try {
			Class commClass = Class.forName(commType);
			comm = (Comm)commClass.newInstance();
			comm.configure(envAgent, this, this, getEventType());
			if (!comm.start()) {
				return false;
			}
			
		} catch (Exception e) {
//    		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Comm class " + commType + " not found!");
			return false;
		}
		
		// Registra o tipo de evento no diretório do Jade
		envAgent.registerService(envAgent.getAgentName()+"-"+getEventType(), getEventType());

		// Cria o arquivo de estatísticas
		try {
			out = new BufferedWriter(new FileWriter("stats_" + eventType + ".txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Chama o método de inicialização do usuário
		try {
			init(parameters);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		if (eventExchange.equals(Constants.EVT_EXC_SPORADIC)) {
			// Nothing to do
		}
		// Caso seja uma troca de eventos híbrida, inicializar o processamento periódico
		else if (eventExchange.equals(Constants.EVT_EXC_HYBRID)) {
			
			startTime = waitTime + (long)clock.getCurrentTime(TimeUnit.MILLISECONDS);
			System.out.println((long)clock.getCurrentTime(TimeUnit.MILLISECONDS) + "\t startTime = " + startTime);
			try {
				out.write("[" + (long)clock.getCurrentTime(TimeUnit.MILLISECONDS) + "] \t startTime = " + startTime + "\n");
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}

			nextStateChange = startTime;
			clock.execute(envAgent, new HybridScheduler());
			
		}
		// Caso seja uma troca de eventos periódica, inicializar a máquina de estados
		else if (eventExchange.equals(Constants.EVT_EXC_PERIODIC)) {

			this.happeningFrame 	= -1;
			this.workingFrame 		= 0;
			this.receivingFrame 	= 0;
			
			// Criar dois WakeBehaviour, um para chamar o process(), outro para o action()
			// TODO Quanto tempo devo deixar para chamar a primeira vez? Talvez tanto faz, pq setExchange roda antes de tudo
			//startTime = clock.getCurrentTime();
			
			// TODO Adicionar parâmetro que permite esperar o registro de todos os atuadores/sensores!
			// waitTime = getParameter();
			// Transforma o waitTime em nanosegundos
			// TODO talvez seja melhor que o waitTime seja um sleep, e n�o um valor adicionado ao currentTime
			startTime = waitTime + (long)clock.getCurrentTime(TimeUnit.MILLISECONDS);
			
//			System.out.println((long)clock.getCurrentTime(TimeUnit.MILLISECONDS) + "\t waitTime = " + waitTime);
//			System.out.println((long)clock.getCurrentTime(TimeUnit.MILLISECONDS) + "\t startTime = " + startTime);
			try {
				out.write("[" + (long)clock.getCurrentTime(TimeUnit.MILLISECONDS) + "] \t waitTime = " + waitTime + "\n");
				out.write("[" + (long)clock.getCurrentTime(TimeUnit.MILLISECONDS) + "] \t startTime = " + nextStateChange + "\n");
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}

			frameTime = startTime;
//			envAgent.addBehaviour(new ActionScheduler());
			clock.execute(envAgent, new PeriodicScheduler());
			
		}
		
		eventServerState = ES_STATE.INITIALIZED;
		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Initialized");
//		System.out.println("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Initialized");
		
		return true;
		
	}
	
	// TODO Implementar a finalização segura de um EventServer
	protected final boolean end() {
		
		System.out.println("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Finalizing the Event Server...");

		// Chamar o código do usuário
		try {
			finit();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Finalizar o PeriodScheduler, se for o caso
		eventServerState = ES_STATE.TERMINATED;

		// Deregistrar todos os atuadores
		for (Enumeration<String> a = actuators.keys(); a.hasMoreElements();) {
			String[] a_key = a.nextElement().split(":");
			deregisterEventHandler(a_key[0], a_key[1], Constants.COMP_ACTUATOR);
		}
		
		// Deregistrar todo os sensores
		for (Enumeration<String> s = sensors.keys(); s.hasMoreElements();) {
			String[] s_key = s.nextElement().split(":");
			deregisterEventHandler(s_key[0], s_key[1], Constants.COMP_SENSOR);
		}
		
		// Fecha o arquivo de log
		try {
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Finalized");
		
		return true;
		
	}
	
	//--------------------------------------------------------------------------------
	// Getters / Setters
	//--------------------------------------------------------------------------------
	
	public final void addParameter(String key, String value) {
		parameters.put(key, value);
	}

	public final void addParameters(Parameters newParameters) {
		if (newParameters != null) {
			parameters.putAll(newParameters);
		}
	}
	
	public final String getParameter(String key) {
		return parameters.get(key);
	}
	
	public final String getParameter(String key, String defaultValue) {
		if (parameters.containsKey(key)) {
			return parameters.get(key);
 		} else {
 			return defaultValue;
 		}
	}

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}
	
	public Parameters getParameters() {
		return parameters;
	}

	public void setCommType(String commType) {
		if (eventServerState == ES_STATE.CREATED) {
			this.commType = commType;
		} else {
//    		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Trying to set commClass after initialization!");
		}
	}

	public String getCommType() {
		return commType;
	}
	
	public void setEventType(String eventType) {
		if (eventServerState == ES_STATE.CREATED) {
			this.eventType = eventType;
		} else {
//    		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Trying to set eventType after initialization!");
		}
	}

	public String getEventType() {
		return eventType;
	}
	
//	public void setEventExchange(String eventExchange) {
//		if (eventServerState == ES_STATE.CREATED) {
//			this.eventExchange = eventExchange;
//		} else {
////    		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Trying to set eventExchange after initialization!");
//		}
//	}
	
	public void setEventExchange(long period, long waitTime) {
		if (eventServerState == ES_STATE.CREATED) {
			this.eventExchange = Constants.EVT_EXC_HYBRID;
			this.period = period;
			this.waitTime = waitTime;
		}
	}
	
	// TODO Como mudar o período durante a execução
	public void setEventExchange(long period, long receiveDeadline, long sendDeadline, long waitTime) {
		if (eventServerState == ES_STATE.CREATED) {
			this.eventExchange 		= Constants.EVT_EXC_PERIODIC;
			this.period 			= period;
			this.receiveDeadline 	= receiveDeadline;
			this.sendDeadline 		= sendDeadline;
			this.periodDeadline 	= period;
			this.waitTime 			= waitTime;
		} else {
//    		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Trying to set eventExchange after initialization!");
		}
	}

	public String getEventExchange() {
		return eventExchange;
	}
	
	//------------------------------
	
	/**
	 * Searches the registered event handlers using filters
	 */
	public String[] searchRegisteredEventHandler(String agentName, String componentName, String eventType, String ehType) {
		ArrayList<String> list = new ArrayList<String>();
		Enumeration<String> ehs = eventHandlers.keys();
		while (ehs.hasMoreElements()) {
			String str = ehs.nextElement();
			String[] str_split = str.split(":");
			if ((agentName.isEmpty() || agentName.equals(str_split[0])) &&
					(componentName.isEmpty() || componentName.equals(str_split[1])) &&
					(eventType.isEmpty() || eventType.equals(str_split[2])) &&
					(ehType.isEmpty() || ehType.equals(str_split[3]))) {
				list.add(str);
			}
		}
		String[] ret = new String[list.size()];
		return list.toArray(ret);
	}
	
	private void writeStats() {
		if (out != null) {
			try {
	//			System.out.println(totalChunksProcessed + " " + totalChunksExpected);
				out.write(totalChunksProcessed + " " + totalChunksExpected);
				float aux = (float)totalChunksProcessed / (float)totalChunksExpected;
				out.write(" % de chunks recebidos = " + String.valueOf(aux * 100) + "\n");
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
//		System.out.println("Parcial chunks = " + partialchunksProcessed);
		partialchunksProcessed = 0;
	}

	public void registerEventHandler(String agentName, String eventHandlerName, String eventHandlerType, Parameters userParameters) {

//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + "] " + "Recebi pedido de registro de " + agentName + ":" + eventHandlerName);

		// Envia os parâmetros necessários
		Command cmd = new Command(Constants.CMD_EVENT_REGISTER_ACK);
		Parameters extraParameters = null; 

		if (eventHandlerType.equals(Constants.COMP_ACTUATOR)) {
		
			actuators.put(agentName + ":" + eventHandlerName, userParameters);
			eventHandlers.put(agentName+":"+eventHandlerName+":"+eventType+":"+Constants.COMP_ACTUATOR, userParameters);
			
			// Passar o controle para o código do usuário, caso seja necessário adicionar algum parâmetro extra
			// Ex.: o tamanho do chunk no caso de eventos de áudio
			try {
				extraParameters = actuatorRegistered(agentName, eventHandlerName, userParameters);
			} catch (Exception e) {
				e.printStackTrace();
//				MusicalAgent.logger.warning("[" + envAgent.getAgentName() + "] " + "Erro ao registrar o atuador");
			}

		}
		else if (eventHandlerType.equals(Constants.COMP_SENSOR)) {
			
			sensors.put(agentName + ":" + eventHandlerName, userParameters);
			eventHandlers.put(agentName+":"+eventHandlerName+":"+eventType+":"+Constants.COMP_SENSOR, userParameters);
			
			// Passar o controle para o código do usuário, caso seja necessário adicionar algum parâmetro extra
			// Ex.: o tamanho do chunk no caso de eventos de áudio
			try {
				extraParameters = sensorRegistered(agentName, eventHandlerName, userParameters);
			} catch (Exception e) {
				e.printStackTrace();
//				MusicalAgent.logger.warning("[" + envAgent.getAgentName() + "] " + "Erro ao registrar o sensor");
			}

		}
		else {
//			MusicalAgent.logger.warning("[" + envAgent.getAgentName() + "] " + "RegisterEventHandler received an strange component!");
			return;
		}
		
		// Send the register ack command
		// Periodic event
		if (getEventExchange() == Constants.EVT_EXC_PERIODIC) {
			cmd.addParameter(Constants.PARAM_COMP_NAME, eventHandlerName);
			cmd.addParameter(Constants.PARAM_EVT_EXECUTION, Constants.EVT_EXC_PERIODIC);
			cmd.addParameter(Constants.PARAM_PERIOD, Long.toString(period));
			cmd.addParameter(Constants.PARAM_RCV_DEADLINE, Long.toString(receiveDeadline));
			cmd.addParameter(Constants.PARAM_START_TIME, Long.toString(startTime)); 
			// Deve começar no próximo frame
			cmd.addParameter(Constants.PARAM_WORKING_FRAME, Long.toString(workingFrame+2));
		} else {
			// Sporadic and hybrid event
			cmd.addParameter(Constants.PARAM_COMP_NAME, eventHandlerName);
			cmd.addParameter(Constants.PARAM_EVT_EXECUTION, Constants.EVT_EXC_SPORADIC);
		}
			
		// Add extra parameters, if there is any
		if (extraParameters != null) {
			cmd.addUserParameters(extraParameters);
		}
		
		System.out.println((long)clock.getCurrentTime(TimeUnit.MILLISECONDS) + " [" + envAgent.getAgentName() + "] " + "Recebi pedido de registro de " + agentName + ":" + eventHandlerName);
		System.out.println("\t"+cmd.getParameters());
		
		// Send the message
		envAgent.sendMessage(agentName, cmd);

	}
	
	public void deregisterEventHandler(String agentName, String eventHandlerName, String eventHandlerType) {
		
		if (eventHandlerType.equals(Constants.COMP_ACTUATOR)) {
			
			try {
				actuatorDeregistered(agentName, eventHandlerName);
				actuators.remove(agentName + ":" + eventHandlerName);
				eventHandlers.remove(agentName+":"+eventHandlerName+":"+eventType+":"+Constants.COMP_ACTUATOR);
				Command cmd = new Command(Constants.CMD_EVENT_DEREGISTER_ACK);
				cmd.addParameter(Constants.PARAM_COMP_NAME, eventHandlerName);
				envAgent.sendMessage(agentName, cmd);
				System.out.println("[" + envAgent.getAgentName() + "] Actuator " + agentName + ":" + eventHandlerName + " deregistered");
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		} else if (eventHandlerType.equals(Constants.COMP_SENSOR)) {
		
			try {
				sensorDeregistered(agentName, eventHandlerName);
				sensors.remove(agentName + ":" + eventHandlerName);
				eventHandlers.remove(agentName+":"+eventHandlerName+":"+eventType+":"+Constants.COMP_SENSOR);
				Command cmd = new Command(Constants.CMD_EVENT_DEREGISTER_ACK);
				cmd.addParameter(Constants.PARAM_COMP_NAME, eventHandlerName);
				envAgent.sendMessage(agentName, cmd);
				System.out.println("[" + envAgent.getAgentName() + "] Sensor " + agentName + ":" + eventHandlerName + " deregistered");
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + "] " + "Desregistro de " + agentName + ":" + eventHandlerName);

	}
	
	//--------------------------------------------------------------------------------
	// Métodos de recebimento e envio de Eventos
	//--------------------------------------------------------------------------------

	protected void senseEarlyEvents(Event evt) {
		totalChunksProcessed++;
		partialchunksProcessed++;
		try {
			processSense(evt);
		} catch (Exception e) {
			e.printStackTrace();
		}
//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Processei o evento adiantado de " + evt.oriAgentName + ":" + evt.oriAgentCompName);
	}
	
	// TODO Pode ser um problema se o processSense() demorar muito! Mudar para Threads!!
	public void sense(Event evt) {
		
//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Event received: " + evt);
//		System.out.println(clock.getCurrentTime() + " [" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Event received: " + evt);

		if (out != null) {
			try {
				out.write("[" + (long)clock.getCurrentTime(TimeUnit.MILLISECONDS) + "] " + evt + "\n");
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Verifica se o evento é do mesmo tipo do ES
		if (!evt.eventType.equals(getEventType())) {
//			MusicalAgent.logger.warning("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Evento não é do tipo correto");
			return;
		}
			
		// Verifica a que janela pertence o evento
		if (!isBatch && getEventExchange().equals(Constants.EVT_EXC_PERIODIC)) {
			if (evt.frame < workingFrame) {
//				MusicalAgent.logger.warning("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Frame atrasado");
				System.out.println("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + evt.oriAgentName + ":" + evt.oriAgentCompName + " - Late frame: received frame = " + evt.frame + ", expected = " + workingFrame);
				return;
			}
			else if ((evt.frame == workingFrame && eventServerState != ES_STATE.WAITING_AGENTS)) {
//				MusicalAgent.logger.warning("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Frame atrasado");
				System.out.println("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + evt.oriAgentName + ":" + evt.oriAgentCompName + " -  Same frame, late arrival: received frame = " + evt.frame + ", expected = " + workingFrame);
				return;
			}
			else if (evt.frame > workingFrame) {
				lock.lock();
				try {
					boolean flag = (eventServerState != ES_STATE.WAITING_AGENTS); 
					if (flag) {
						earlyEvents.add(evt);
					}
				} finally {
					lock.unlock();
				}
//				MusicalAgent.logger.warning("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Frame adiantado");
				if (evt.frame-workingFrame > 1) {
					System.out.println("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Early frame: received frame = " + evt.frame + ", expected = " + workingFrame);
				}
				return;
			}
		}
		
		// chama o método implementado pelo usuário
		totalChunksProcessed++;
		partialchunksProcessed++;
		try {
			processSense(evt);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// avisa o ambiente que o evento foi processado (importante para Batch)
		if (isBatch) {
			envAgent.eventProcessed();
		}

//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Processei o evento");

	}
	
	// Nesse caso, enviamos todos os eventos que estão na fila outputEvents, assim podemos enviar quantos eventos quisermos e na hora que quisermos
	public void act() {

//		double rand = Math.random();
//		System.out.println("[" + getEventType() + "] Entrei no act() - rand " + rand + " - " + outputEvents.size());
		
		outputLock.lock();
		try {

//			System.out.println("[" + getEventType() + "] Entrei no act()+lock()");
			
			for (Event evt : outputEvents) {
				
				// Completa os dados do evento
				evt.oriAgentName	 	= envAgent.getAgentName();
				evt.oriAgentCompName 	= getEventType();
				evt.eventType 		 	= getEventType();
				if (getEventExchange().equals(Constants.EVT_EXC_PERIODIC)) {
					evt.frame 			= workingFrame;
					evt.instant 		= (double)(startTime + (workingFrame * period))/1000;
					evt.duration 		= (double)period/1000;
				} else {
					evt.frame 			= -1;
					evt.instant 		= clock.getCurrentTime(TimeUnit.SECONDS);
					evt.duration 		= 0;
				}
				evt.timestamp 			= (long)clock.getCurrentTime(TimeUnit.MILLISECONDS); 
		        
				// Chama o método implementado pelo usuário
				try {
					evt = processAction(evt);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Envia o evento
			    comm.send(evt);

		        // Avisa o Agente Ambiente do evento enviado
		        envAgent.eventSent();

//		   		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Gerei um evento");
			
			}
			// Apaga a fila
//			System.out.println("Vou dar clear - rand " + rand + " - " + outputEvents.size());
			outputEvents.clear();

		} finally {
			outputLock.unlock();
		}

	}
	
	/**
	 * Adds a event to a queue to be sent the next time act() is called.
	 * It can be used by a sporadic event server to send differents events for differents recipients at the same time
	 * @param destAgentName
	 * @param destCompName
	 * @param evt
	 */
	protected void addOutputEvent(String destAgentName, String destCompName, Event evt) {
		
		outputLock.lock();
		try {
			evt.destAgentName = destAgentName;
			evt.destAgentCompName = destCompName;
			outputEvents.add(evt);
		} finally {
			outputLock.unlock();
		}
		
	}
	
	//--------------------------------------------------------------------------------
	// Hybrid Exchange Methods
	//--------------------------------------------------------------------------------
	
	private class HybridScheduler implements Runnable {

		public void run() {

			try {
				long currentTime = (long)clock.getCurrentTime(TimeUnit.MILLISECONDS); 
				long elapsedTime = currentTime - nextStateChange;
				out.write("[" + (long)clock.getCurrentTime(TimeUnit.MILLISECONDS) + "] >> PROCESSING << (" + elapsedTime + ")\n");
				process();
				out.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			nextStateChange = nextStateChange + period;
			
			if (eventServerState != ES_STATE.TERMINATED) {
				clock.schedule(envAgent, this, nextStateChange);
			}

		}
		
	}
	
	//--------------------------------------------------------------------------------
	// Periodic Exchange Methods
	//--------------------------------------------------------------------------------
	
//	private class ActionScheduler extends OneShotBehaviour {
	private class PeriodicScheduler implements Runnable {

//		long wakeTime;
//		
//		public ActionScheduler(long wakeTime) {
//			this.wakeTime = wakeTime;
//		}
		
//		public void action() {
		public void run() {

//			int num = (int)Math.floor((Math.random() * 100));

//			System.out.println(eventServerState);
//			System.out.println(clock.getCurrentTime() + "\t [EventServer:"+ eventType + "] \t Entrei no action() - " + num);
			
			long currentTime = (long)clock.getCurrentTime(TimeUnit.MILLISECONDS); 
			long elapsedTime = currentTime - nextStateChange;
//			if (currentTime - nextStateChange > 20 && eventServerState != ES_STATE.WAITING_BEGIN && eventServerState != ES_STATE.INITIALIZED && eventServerState != ES_STATE.CONFIGURED) {
//				System.err.println(clock.getCurrentTime() + " ********* WARNING (" + (currentTime - nextStateChange) + ") - " + eventServerState + " ********** " + num );
//				System.exit(-1);
//			}
			
//			System.out.println(clock.getCurrentTime() + " \t 1");
			
			switch (eventServerState) {

			case INITIALIZED:
			
				eventServerState = ES_STATE.WAITING_BEGIN;
//				System.out.println("--------------------------------------------------------------------------------");
//				System.out.println(clock.getCurrentTime() + " >> 1 - WAITING_BEGIN << ");
				try {
					out.write("[" + (long)clock.getCurrentTime(TimeUnit.MILLISECONDS) + "] >> 1 - WAITING_BEGIN << \n");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
//	    		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + ">> 1 - WAITING_BEGIN <<");
				nextStateChange = startTime;
				receivingFrame++;
				
				break;

			// Frame start
			case WAITING_BEGIN:
			case SENDING_RESPONSE:

				// Escreve as estatásticas do último turno
				if (eventServerState == ES_STATE.SENDING_RESPONSE) {
					writeStats();
					totalChunksExpected += actuators.size();
				}
				
				workingFrame++;
				happeningFrame++;

				// Muda o estado do agente e volta a receber eventos
				eventServerState = ES_STATE.WAITING_AGENTS;

				// Tirei para que possa receber eventos futuros
				// comm.sensing = true;
//				System.out.println("--------------------------------------------------------------------------------");
//				System.out.println(clock.getCurrentTime() + " >> 2 - WAITING_AGENTS << (" + elapsedTime + ") (workingFrame " + workingFrame + ") (t " + ((double)(period * workingFrame)/1000) + "s)");
				try {
					out.write("[" + (long)clock.getCurrentTime(TimeUnit.MILLISECONDS) + "] >> 2 - WAITING_AGENTS << (" + elapsedTime + ") (workingFrame " + workingFrame + ") (t " + ((double)(period * workingFrame)/1000) + "s)\n");;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
//	    		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + ">> 2 - WAITING_AGENTS << (" + elapsedTime + ") (workingFrame " + workingFrame + ") (t " + ((startTime + (double)(period * workingFrame))/1000) + "s)");;
				nextStateChange = frameTime + receiveDeadline;
				
				break;

			// ReceiveDeadline
			case WAITING_AGENTS:
				// TODO Talvez não seja o melhor lugar para atualizar isso!
				// TODO E se o per�odo for muito curto e n�o der tempo de atualizar? Ele vai se perder!!!
				receivingFrame++;	
				
				// Muda o estado do agente e para de receber eventos
				eventServerState = ES_STATE.PROCESSING;
				// TODO Tirei para que possa receber eventos futuros
				// comm.sensing = false;

				// Insere os eventos adiantados (para essa workingFrame)
				try {
					lock.lock();
					for (Iterator<Event> iterator = earlyEvents.iterator(); iterator.hasNext();) {
						Event evt = iterator.next();
						if (evt.frame == workingFrame) {
							senseEarlyEvents(evt);
						}
					}
					earlyEvents.clear();
				} finally {
					lock.unlock();
				}
				
				// Inicia o processamento dos eventos
//				System.out.println(clock.getCurrentTime() + " >> 3 - PROCESSING << (" + elapsedTime + ") ");
				try {
					out.write("[" + (long)clock.getCurrentTime(TimeUnit.MILLISECONDS) + "] >> 3 - PROCESSING << (" + elapsedTime + ")\n");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
//	    		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + ">> 3 - PROCESSING << (" + elapsedTime + ") ");
				
//				System.out.println("Entrou no process()\t " + clock.getCurrentTime());
				long time = (long)clock.getCurrentTime(TimeUnit.MILLISECONDS);
				try {
					process();
				} catch (Exception e) {
					e.printStackTrace();
				}
//	    		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "process() demorou " + (clock.getCurrentTime() - time));
//				System.out.println(clock.getCurrentTime() + "\t process() demorou " + (clock.getCurrentTime() - time));
//				System.out.println("Saiu do process()\t " + clock.getCurrentTime());
				try {
					out.write("[" + (long)clock.getCurrentTime(TimeUnit.MILLISECONDS) + "] \t process() demorou " + ((long)clock.getCurrentTime(TimeUnit.MILLISECONDS) - time) + "\n");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				// Agenda a pr�xima mudan�a de estado
				nextStateChange = frameTime + sendDeadline;

				break;

			// SendDeadline
			case PROCESSING:
				eventServerState = ES_STATE.SENDING_RESPONSE;
				frameTime = frameTime + period;
//				System.out.println(clock.getCurrentTime() + " >> 4 - SENDING_RESPONSE << (" + elapsedTime + ") ");
				try {
					out.write("[" + (long)clock.getCurrentTime(TimeUnit.MILLISECONDS) + "] >> 4 - SENDING_RESPONSE << (" + elapsedTime + ") \n");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
//	    		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + ">> 4 - SENDING_RESPONSE << (" + elapsedTime + ") ");
				nextStateChange = frameTime;
				act();
		        break;

			default:
				break;
			}
			
//			System.out.println(clock.getCurrentTime() + " \t 2");
			try {
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Agenda a próxima mudança de estado
//			System.out.println(clock.getCurrentTime() + "\t [EventServer:"+ eventType + "] \t Vou agendar \t - " + num);
			if (eventServerState != ES_STATE.TERMINATED) {
				clock.schedule(envAgent, this, nextStateChange);
			}
//			System.out.println(clock.getCurrentTime() + "\t [EventServer:"+ eventType + "] \t Vou dormir até \t" + nextStateChange);			
//			System.out.println(clock.getCurrentTime() + " \t 3");
//			System.out.println("-------");

//			System.out.println("Sai do action()\t " + clock.getCurrentTime() + " - " + num);
			
		}
		
	}
	
	//--------------------------------------------------------------------------------
	// Command Interface
	//--------------------------------------------------------------------------------
	
	@Override
	public final String getAddress() {
		return "/" + Constants.FRAMEWORK_NAME + "/" + envAgent.getAgentName() + "/" + getEventType();
	}

	@Override
	public final void receiveCommand(String recipient, Command cmd) {
        System.out.println("[" + getAddress() +"] Command received: " + cmd);
        processCommand(recipient, cmd);
	}
	
	@Override
	public final void sendCommand(String recipient, Command cmd) {
		envAgent.getRouter().sendCommand(recipient, cmd);
		
	}
	
	//--------------------------------------------------------------------------------
	// User implemented methods
	//--------------------------------------------------------------------------------
	
	/**
	 * User must configure here the commClass, eventType and eventExchange (and the period, if any).
	 */
	protected abstract void configure();
	
	/**
	 * User initialization method
	 * @param parameters
	 * @throws Exception
	 */
	protected boolean init(Parameters parameters) throws Exception {
//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "init()");
		return true;
	}
	
	/**
	 * User finalization method
	 * @param parameters
	 * @throws Exception
	 */
	protected boolean finit() throws Exception {
//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "init()");
		return true;
	}

	/**
	 * Método chamado ao receber um evento. Deve armazenar os eventos recebidos em uma estrutura de dados, para depois ser processado.
	 */
	protected void processSense(Event evt) throws Exception {
//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "processSense()");
	}

	/**
	 * Método chamado para enviar um evento pelo process().
	 * No caso de eventos frequentes, é chamado automaticamente quando o sendDeadline é atingindo.
	 * @return
	 * @throws Exception
	 */
	protected Event processAction(Event evt) throws Exception {
//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "processAction()");
		return evt;
	}
	
	/**
	 * Método chamado para efetuar algum processamento sobre os eventos recebidos, e enviar eventos em seguida.
	 * No caso de eventos frequentes, é chamado automaticamente quando o receiveDeadline é atingindo.
	 * @throws Exception
	 */
	// TODO Pode lançar um TimeOutException caso não seja executada no tempo necessário!
	protected void process() throws Exception {
//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "process()");
	}

	protected Parameters actuatorRegistered(String agentName, String eventHandlerName, Parameters userParam) throws Exception {
//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "actuatorRegistered()");
		return null;
	}
	
	protected void actuatorDeregistered(String agentName, String eventHandlerName) throws Exception {
//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "actuatorDeregistered()");
	}
	
	protected Parameters sensorRegistered(String agentName, String eventHandlerName, Parameters userParam) throws Exception {
//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "sensorRegistered()");
		return null;
	}
	
	protected void sensorDeregistered(String agentName, String eventHandlerName) throws Exception {
//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "sensorDeregistered()");
	}

	/**
	 * Called when a parameter has been updated
	 * @param paramName
	 */
	public void parameterUpdated(String paramName) {
		System.out.println("[" + envAgent.getAgentName() + ":" + getEventType()  +"] " + "Parameter '" + paramName + "'updated");
	}

	@Override
	public void processCommand(String recipient, Command cmd) {
	}

}
