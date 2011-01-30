package mms;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import mms.Constants.EA_STATE;
import mms.commands.Command;
import mms.commands.CommandClientInterface;
import mms.world.World;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

/**
 * An agent that represents the environment.
 * @author Leandro Ferrari Thomaz
 *
 */
public class EnvironmentAgent extends MMSAgent {
	
	// ---------------------------------------------- 
	// General variables 
	// ---------------------------------------------- 

	/**
	 * Estado do Agente Ambiente
	 */
	private EA_STATE state = EA_STATE.CREATED;
	
	protected DFAgentDescription dfd;
	
	/**
	 *  Lock
	 */
	private Lock lock = new ReentrantLock();
	
	/**
	 *  Descrição do Mundo Virtual
	 */
	protected World world;
	
	/**
	 *  Event Servers registrados (por tipo de evento)
	 */
	protected ConcurrentHashMap<String, EventServer> eventServers = new ConcurrentHashMap<String, EventServer>();
	
	/**
	 *  Tabela de fatos públicos dos agentes (Fenótipo)
	 */
	public ConcurrentHashMap<String, String> agentsPublicFacts = new ConcurrentHashMap<String, String>();
	
	/**
	 *  Contador para manter nome dos agentes único
	 */
	private int numberCreatedAgents;
	
	// ---------------------------------------------- 
	// Batch processing control variables 
	// ---------------------------------------------- 
	
	private long lastUpdateTime = System.currentTimeMillis();
	/**
	 *  Tempo mínimo de espera entre cada turno 
	 */
	private long waitTimeTurn;
	/**
	 *  Controla se deve esperar todos os agentes serem criados para iniciar a simulação
	 */
	private boolean waitAllAgents;
	/**
	 *  Número inicial de Agentes Musicais
	 */
	private long initialAgents;
	/**
	 *  Número de agentes registrados no Ambiente
	 */
	private int registeredAgents;
	/**
	 *  Número de agentes registrados no Ambiente para o próximo turno
	 */
	private int registeredAgentsNextTurn;
	/**
	 *  Número de agentes registrados que finalizaram suas ações no turno atual
	 */
	private int registeredAgentsReady;
	/**
	 *  Número de eventos enviados pelos Agentes Musicais no turno atual
	 */
	private int agentEventsSent;
	/**
	 *  Número de eventos processados pelos EventServers no turno atual
	 */
	private int agentEventsProcessed;
	/**
	 *  Número de eventos enviados pelos EventServers para os Agentes no turno atual	
	 */
	private int evtSrvEventsSent;
	/**
	 * Número de eventos processados pelos Agentes Musicais no turno atual
	 */
	private int evtSrvEventsProcessed;

	//--------------------------------------------------------------------------------
	// Agente getters / setters
	//--------------------------------------------------------------------------------
	
	public final World getWorld() {
		return world;
	}
	
	//--------------------------------------------------------------------------------
	// Initialization
	//--------------------------------------------------------------------------------
	
	/**
	 * Inicializa o Agente Ambiente
	 */
	protected void start() {

//		System.out.println("EA start()");
		
		lock.lock();
		try {
		
			// 1. Obtém as propriedades da simulação
			waitTimeTurn = Long.valueOf(getProperty(Constants.WAIT_TIME_TURN, "100"));
			waitAllAgents = Boolean.valueOf(getProperty(Constants.WAIT_ALL_AGENTS, "true"));
	
			// 2. Cria um mundo genérico (que poderá ser alterado pelo usuário)
			// TODO Esse é o melhor lugar para colocar?
			Class worldClass;
			try {
				worldClass = Class.forName(parameters.get(Constants.CLASS_WORLD, "mms.world.World"));
				world = (World)worldClass.newInstance();
				world.setParameters(getParameters());
				world.configure();
				world.start(this);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
			// 3. Registra o Ambiente no DS
			this.registerService(this.getLocalName(), Constants.EVT_ENVIRONMENT);
			
			// 4. Inicia a recepção de Mensagens de Controle 
			this.addBehaviour(new ReceiveMessages(this));
			
			// 5. Inicia os EventServers
			Collection<EventServer> servers = eventServers.values();
			for (Iterator<EventServer> iterator = servers.iterator(); iterator.hasNext();) {
				EventServer eventServer = iterator.next();
				eventServer.start(this, parameters);
			}
	
			// 6. Fim da inicialização do Agente Ambiente
			// TODO Deveria vir após o init()
			state = EA_STATE.INITIALIZED;
			logger.info("[" + this.getAID().getLocalName() + "] " + "Initialized");
			System.out.println("[" + this.getAID().getLocalName() + "] " + "Initialized");
		
		} finally {
			lock.unlock();
		}

		// Cria os agentes necessários, caso solicitado
		// TODO Pode ser resolvido pelo Loader
		initialAgents = 0;
		String agentClass;
		if (parameters != null && parameters.containsKey(Constants.INIT_NUMBER_INITIAL_AGENTS)) {
			initialAgents 	= Integer.valueOf(parameters.get(Constants.INIT_NUMBER_INITIAL_AGENTS));	
			agentClass 		= parameters.get(Constants.CLASS_MUSICAL_AGENT);	
			// TODO permitir dar nome ao agente e passar parâmetros
			for (int i = 0; i < initialAgents; i++) {
				createMusicalAgent(null, agentClass, null);
			}
		}

		// Inicia a simulação
		// Caso solicitado, aguarda a criação de todos os agentes (para processamento Batch)
		if (getProperty(Constants.PROCESS_MODE, null).equals(Constants.MODE_BATCH)) {
			if (waitAllAgents) {
				// TODO Timeout para o caso de algum agente travar na inicialização
				this.addBehaviour(new CheckRegister(this));
			} else {
	//			this.addBehaviour(tbf.wrap(new CheckEndTurn(this)));
				this.addBehaviour(new CheckEndTurn(this));
			}
		}
		
	}
	
	/**
	 * Registra um tipo de evento tratado por este Agente Ambiente no diretório do JADE
	 * @param name nome do EventServer que trata o evento
	 * @param type tipo do evento
	 */
	protected final void registerService(String name, String type) {
	
		ServiceDescription sd = new ServiceDescription();
		sd.setName(name);
		sd.setType(type);
		
		try {
			if (dfd == null) {
				dfd = new DFAgentDescription();		
				dfd.setName(this.getAID());
				dfd.addServices(sd);
				DFService.register(this, dfd);
			} else {
				dfd.addServices(sd);
				DFService.modify(this, dfd);
			}
			logger.info("[" + this.getAID().getLocalName() + "] " + "Event type " + type + " registered in the DS");
		} catch (FIPAException fe) {
			System.out.println(fe.toString());
			System.out.println("ERRO: Não foi possível registrar o serviço!");
		}

	}
	
	/**
	 * Cria uma instância de um EventServer esporádico ou frequente
	 * @param className classe Java do EventServer
	 * @param arguments parâmetros a serem passados para o objeto criado
	 */
	public final void addEventServer(String className, Parameters arguments) {
		
		lock.lock();
		try {
			try {
				// Criar instância do EventServer
				Class esClass = Class.forName(className);
				EventServer es = (EventServer)esClass.newInstance();
				// Configurar o EventServer
				es.setParameters(arguments);
				es.configure();
				// Adicionar na tabela
				if (eventServers.containsKey(es.getEventType())) {
					System.err.println("ERROR: There is already an Event Server with event type " + es.getEventType());
					return;
				} else {
					eventServers.put(es.getEventType(), es);
				}
				// Caso o Agente Ambiente já tiver sido inicializado, inicializar o EventServer
				if (state == EA_STATE.INITIALIZED) {
					es.start(this, arguments);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.err.println("ERROR: Not possible to create an instance of " + className);
			} catch (InstantiationException e) {
				e.printStackTrace();
				System.err.println("ERROR: Not possible to create an instance of " + className);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				System.err.println("ERROR: Not possible to create an instance of " + className);
			}
		} finally {
			lock.unlock();
		}
		
	}
	
	/**
	 * Removes an EventServer from the EnvironmentAgent
	 * @param server
	 */
	public final void removeEventServer(String eventType) {
		
		if (eventServers.containsKey(eventType)) {
			EventServer server = eventServers.remove(eventType);
			server.end();
		} else {
			System.err.println("["+getLocalName()+"] Event server " + eventType + " does not exist.");
		}
		
	}
	
	//--------------------------------------------------------------------------------
	// Message handling
	//--------------------------------------------------------------------------------

	/**
	 * Envia um comando a um agente
	 * @param receiver agente destino do comando
	 * @param command comando a ser enviado
	 */
	public final void sendMessage(String receiver, Command command) {
		
		String[] str = new String[1];
		str[0] = receiver; 
		sendMessage(str, command);
		
	}

	/**
	 * Envia um comando a mais de um agente
	 * @param receiver agentes destino do comando
	 * @param command comando a ser enviado
	 */
	
	@Override
	public void input(CommandClientInterface cmdInterface, Command cmd) {
		
        System.out.println("[" + getAddress() +"] Command received: " + cmd);
        // Se for para o Agente, processa o comando, se for para algum de seus componentes, rotear
        String[] recipient = cmd.getRecipient().split(":");
        if (recipient.length == 2) {
        	processMessage(cmd.getSource(), cmd);
        } 
        else if (recipient.length == 3) {
        	System.out.println("BLA1");
        	if (eventServers.containsKey(recipient[2])) {
            	System.out.println("BLA2");
        		EventServer es = eventServers.get(recipient[2]);
        		// Se for mudança de parâmetros, faz diretamente, caso contrário envia o comando para o componente
        		if (cmd.getCommand().equals(Constants.CMD_PARAM)) {
                	System.out.println("BLA3");
        			String param = cmd.getParameter("NAME");
        			String value = cmd.getParameter("VALUE");
        			if (param != null && value != null) {
        				es.addParameter(param, value);
        				es.parameterUpdated(param);
        			}
        		}
        		else {
        			es.processCommand(cmd);
        		}
        	} 
        	else {
        		System.out.println("[" + getAddress() +"] EventServer does not exist: " + recipient[2]);
        	}
        }
		
	}

	protected final void sendMessage(String[] receiver, Command command) {
		
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		String receivers = "";
		for (int i = 0; i < receiver.length; i++) {
			msg.addReceiver(new AID(receiver[i], AID.ISLOCALNAME));
			receivers = receivers + receiver[i] + " ";
		}
		msg.setConversationId("CommMsg");
		msg.setContent(command.toString());
		this.send(msg);

		MusicalAgent.logger.info("[" + this.getAID().getLocalName() + "] " + "Message sent to " + receivers + "(" + msg.getContent() + ")");
		
	}
	
	protected final void processMessage(String sender, Command cmd) {
		
		
		if (cmd.getCommand().equals(Constants.CMD_CREATE_AGENT)) {
			
			String agentName = cmd.getParameter("NAME");
			String agentClass = cmd.getParameter("CLASS");
			Parameters parameters = cmd.getParameters();
			createMusicalAgent(agentName, agentClass, parameters);
			
		} else if (cmd.getCommand().equals(Constants.CMD_DESTROY_AGENT)) {								
			
			String agentName = cmd.getParameter("NAME");
			destroyAgent(agentName);
			
		} else if (cmd.getCommand().equals(Constants.CMD_ADD_EVENT_SERVER)) {								
		
			String className = cmd.getParameter("NAME");
			Parameters arguments = cmd.getParameters();
			addEventServer(className, arguments);
			
		} else if (cmd.getCommand().equals(Constants.CMD_REMOVE_EVENT_SERVER)) {								
			
			String esName = cmd.getParameter("NAME");
			removeEventServer(esName);
			
		} else if (cmd.getCommand().equals(Constants.CMD_EVENT_REGISTER)) {								

			String componentName = cmd.getParameter(Constants.PARAM_COMP_NAME);
			String eventHandlerType = cmd.getParameter(Constants.PARAM_COMP_TYPE);
			String eventType = cmd.getParameter(Constants.PARAM_EVT_TYPE);
			Parameters userParam = cmd.getUserParameters();

			// Repassar pedido de registro para o EventServer responsável
			EventServer evtServer = eventServers.get(eventType);
			if (evtServer != null) {
				evtServer.registerEventHandler(sender, componentName, eventHandlerType, userParam);
			} else {
				logger.info("[" + getAID().getLocalName() + "] " + "EventServer " + eventType + " not found");
			}
			
		}
		else if (cmd.getCommand().equals(Constants.CMD_EVENT_DEREGISTER)) {
			
			String componentName = cmd.getParameter(Constants.PARAM_COMP_NAME);
			String eventHandlerType = cmd.getParameter(Constants.PARAM_COMP_TYPE);
			String eventType = cmd.getParameter(Constants.PARAM_EVT_TYPE);

			// Repassar pedido de registro para o EventServer respons�vel
			EventServer evtServer = eventServers.get(eventType);
			if (evtServer != null) {
				evtServer.deregisterEventHandler(sender, componentName, eventHandlerType);
			}
			
		}
		else if (cmd.getCommand().equals(Constants.CMD_AGENT_REGISTER)) {

			MusicalAgent.logger.info("[" + getLocalName() + "] " + "Recebi pedido de registro de " + sender);
			registerAgent(sender, cmd.getParameters());

		}
		else if (cmd.getCommand().equals(Constants.CMD_AGENT_DEREGISTER)) {

			MusicalAgent.logger.info("[" + getLocalName() + "] " + "Recebi pedido de desregistro de " + sender);
			deregisterAgent(sender);

		}
		else if (cmd.getCommand().equals(Constants.CMD_AGENT_READY)) {

			MusicalAgent.logger.info("[" + getLocalName() + "] " + "Agente " + sender + " pronto pra iniciar a simula��o");
			prepareAgent(sender);

		}
		else if (cmd.getCommand().equals(Constants.CMD_BATCH_TURN)) {
		
			if (getProperty(Constants.PROCESS_MODE, null).equals(Constants.MODE_BATCH)) {
				MusicalAgent.logger.info("[" + getLocalName() + "] " + "Recebi mudança de turno de " + sender);
				int numberEventsSent = Integer.valueOf(cmd.getParameter(Constants.PARAM_NUMBER_EVT_SENT));
				agentProcessed(numberEventsSent);
			}
			
		}
		else if (cmd.getCommand().equals(Constants.CMD_BATCH_EVENT_ACK)) {

			if (getProperty(Constants.PROCESS_MODE, null).equals(Constants.MODE_BATCH)) {
				eventAgentProcessed();
			}
			
		}
		else if (cmd.getCommand().equals(Constants.CMD_PUBLIC_FACT_UPDATE)) {
			
			String fact = cmd.getParameter(Constants.PARAM_FACT_NAME);
			String value = cmd.getParameter(Constants.PARAM_FACT_VALUE);
			agentsPublicFacts.put(sender + ":" + fact, value);
			
		}
		else {
			
			System.out.println("[" + getLocalName() + "] " + "Command not recognized: " + cmd.getCommand());
			
		}
		
	}
	
	/**
	 * Behaviour cíclico interno do agente responsável por receber e tratar as mensagens enviadas ao ambiente pelos agentes musicais.
	 */
	private final class ReceiveMessages extends CyclicBehaviour {

		MessageTemplate mt;
		
		public ReceiveMessages(Agent a) {
			super(a);
			mt = MessageTemplate.MatchConversationId("CommMsg");
		}
		
		public void action() {
			
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				
//				MusicalAgent.logger.info("[" + getAID().getLocalName() + "] " + "Message received from " + msg.getSender().getLocalName() + " (" + msg.getContent() + ")");

				// TODO switch com os possíveis comandos
				String sender = msg.getSender().getLocalName();
				Command cmd = Command.parse(msg.getContent());
				// TODO Podemos criar aqui uma thread, assim tratamos msgs em paralelo
				if (cmd != null) {
					processMessage(sender, cmd);
				}
			
			} else {
				
				block();
				
			}
			
		}
	
	}

	//--------------------------------------------------------------------------------
	// Agent management (create, destroy, register, deregister)
	//--------------------------------------------------------------------------------

	/**
	 * Cria um novo Agente no Ambiente. Se o nome do agente é null ou vazio, cria um nome sequencial, baseado na classe.
	 */
	public final synchronized String createMusicalAgent(String agentName, String agentClass, Parameters parameters) {

		Object[] arguments = null; 
		if (parameters != null) {
			arguments = new Object[1];
			arguments[0] = parameters;
		}
		
		if (agentName == null || agentName.equals("")) {
			numberCreatedAgents++;
			agentName = new String(agentClass + "_" + numberCreatedAgents);
		}

		try {
			ContainerController cc = getContainerController();
			Class maClass = Class.forName(agentClass);
			MusicalAgent ma = (MusicalAgent)maClass.newInstance();
			ma.setArguments(arguments);
			AgentController ac = cc.acceptNewAgent(agentName, ma);
			ac.start();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		logger.info("[" + this.getAID().getLocalName() + "] " + "Created a new agent named " + agentName);

		return agentName;
		
	}
	
	/**
	 * Destrói um agente presente no Ambiente.
	 * @param agentName nome do agente a ser destruido
	 */
	public final synchronized void destroyAgent(String agentName) {
		
		// Não podemos matar o agente diretamente pelo doDelete()
		Command cmd = new Command(Constants.CMD_KILL_AGENT);
		sendMessage(agentName, cmd);
		
//		MusicalAgent.logger.info("[" + this.getAID().getLocalName() + "] Destroyed agent " + agentName);
		System.out.println("[" + this.getAID().getLocalName() + "] Destroyed agent " + agentName);

	}
	
	/**
	 * Registra um agente musical no Ambiente.
	 * @param agentName
	 */
	protected final synchronized void registerAgent(String agentName, Parameters parameters) {

		// Adiciona o agente ao mundo virtual
		world.addEntity(agentName, parameters);
		
	}
	
	/**
	 * 
	 * @param agentName
	 */
	protected final synchronized void prepareAgent(String agentName) {

		Command cmd = new Command(Constants.CMD_AGENT_READY_ACK);
		
		// No caso de processamento BATCH
		if (getProperty(Constants.PROCESS_MODE, null).equals(Constants.MODE_BATCH)) {
			// Adicionar Agente à lista de registrados 
			registeredAgentsNextTurn++;
			
			// Enviar mensagem de ACK, indicando que o agente deve acordar no próximo turno
			cmd.addParameter("turn", String.valueOf((getClock().getCurrentTime() + 1)));
		
			this.addBehaviour(new CheckEndTurn(this));
		}

		// Envia a resposta
		sendMessage(agentName, cmd);
		
	}

	/**
	 * Retira um agente musical do registro de agentes ativos no ambiente.
	 * @param agentName
	 */
	protected final synchronized void deregisterAgent(String agentName) {

		if (getProperty(Constants.PROCESS_MODE, null).equals(Constants.MODE_BATCH)) {
			// Retirar Agente da lista de registrados 
			registeredAgents--;
			registeredAgentsNextTurn--;
			
			this.addBehaviour(new CheckEndTurn(this));
		}
		
		world.removeEntity(agentName);

	}

	//--------------------------------------------------------------------------------
	// Batch Mode related methods
	//--------------------------------------------------------------------------------

	/**
	 * Classe interna responsável por verificar se uma simulação pode ser iniciada, ou seja, se todos os agentes estão prontos.
	 */
	private final class CheckRegister extends CyclicBehaviour {

		public CheckRegister(Agent a) {
			super(a);
		}
		
		public void action() {

			// Enquanto não tiver nenhum agente registrado para o próximo turno, continua checando ou atualiza o clock
			if (initialAgents == registeredAgents) {
				
				// Atualiza o clock virtual		
				myAgent.addBehaviour(new CheckEndTurn(myAgent));

				// Finaliza o behaviour cíclico
				myAgent.removeBehaviour(this);
				
			}
			
		}
		
	}

	/**
	 * Classe internar responsável por verificar se todos os agentes já agiram no turno atual.
	 * @author lfthomaz
	 *
	 */
	private final class CheckEndTurn extends OneShotBehaviour {
		
		public CheckEndTurn(Agent a) {
			super(a);
		}
		
		public void action() {

			// TODO Aguardar um n�mero de agentes pr�-definidos estarem registrados 
			// Aguarda a finaliza��o dos Agentes Musicais e o processamento de todos os eventos pelos EventServers
			if (//registeredAgentsNextTurn > 0 && 
				registeredAgentsReady >= registeredAgents && 
				agentEventsProcessed >= agentEventsSent) {

				// Inicia o processamento dos EventServer (para atualizar o ambiente)
				// TODO tornar o processamento paralelo ou n�o � necessario??
				// TODO o usu�rio pode escolher a ordem em que os EventServers ser�o processados!!!
			     for (Enumeration<EventServer> e = eventServers.elements(); e.hasMoreElements();) {
			    	 EventServer evtServer = e.nextElement();
			    	 try {
				    	 evtServer.process();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
			     }

				// Atualizar as vari�veis de controle
				registeredAgents 		= registeredAgentsNextTurn;
				registeredAgentsReady 	= 0;
				agentEventsSent 		= 0;
				agentEventsProcessed 	= 0;
				evtSrvEventsSent 		= 0;
				evtSrvEventsProcessed 	= 0;
				
				// Chama um processo do usu�rio antes de mudar o clock (bom para atualizar o GUI)
				preUpdateClock();
				
				// Deve aguardar os ACKs dos agentes, caso tenha enviado eventos
				// TODO M�quina de estados?!!??
				//if ()
				
				// Suspende a simula��o por um tempo
				// TODO poder� ser programado pelo usu�rio, para ter um tempo m�nimo para atualizar o turno
				long elapsedTime = System.currentTimeMillis() - lastUpdateTime;
				if (elapsedTime < waitTimeTurn) {
					try {
						Thread.sleep(waitTimeTurn - elapsedTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
								
				// Se n�o existe nenhum agente registrado para o pr�ximo turno, j� pode agendar um novo CheckEndTurn
				if (getProperty(Constants.PROCESS_MODE, null).equals(Constants.MODE_BATCH) && registeredAgentsNextTurn == 0) {
					addBehaviour(new CheckEndTurn(myAgent));
				}

				// Atualiza o clock virtual
				getClock().updateClock();
				
				lastUpdateTime = System.currentTimeMillis();
			}
		}
	
	}

	/**
	 * Registra a mensagem de um agente indicando que terminou o turno.
	 */
	private final synchronized void agentProcessed(int events) {
		
		// S� serve para funcionamento 
		// Incrementar contador de agentes prontos
		// Quando alcan�ar o total, atualizar o clock
		registeredAgentsReady++;
		agentEventsSent = agentEventsSent + events;
		
		this.addBehaviour(new CheckEndTurn(this));

	}
	
	/**
	 * Registra a informação do EventServer que um evento foi processado
	 */
	public final synchronized void eventProcessed() {

		agentEventsProcessed++;

		this.addBehaviour(new CheckEndTurn(this));

	}
	
	/**
	 * Registra que um evento foi enviado pelo EventServer
	 */
	public final synchronized void eventSent() {
		
		// Incrementar contador de eventos enviados pelo EventServer
		evtSrvEventsSent++;
		
	}
	
	/**
	 * Registra que um Agente terminou de processar
	 */
	private final synchronized void eventAgentProcessed() {
		
		evtSrvEventsProcessed++;
		
		this.addBehaviour(new CheckEndTurn(this));

	}
	
	//--------------------------------------------------------------------------------
	// User implemented methods
	//--------------------------------------------------------------------------------

	/**
	 * Método executado pelo Ambiente, quando em modo BATCH, imediatamente antes de alterar o turno.
	 */
	protected void preUpdateClock() {};
	
}
