package mms;

import mms.Constants.EA_STATE;
import mms.Constants.EH_STATUS;
import mms.comm.Comm;
import mms.memory.Memory;
import mms.world.Vector;

import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public abstract class EventHandler extends MusicalAgentComponent {

	/**
	 * Event Handler's state
	 * 0: not registered, 1: registered
	 */
	protected EH_STATUS	status 			= EH_STATUS.NOT_REGISTERED;

	/**
	 * Event Handler's parameters
	 */
	protected String 		eventType 			= null;
	protected String		eventExchange 		= Constants.EVT_EXC_NOT_DEFINED;
	protected String 		commType 			= "mms.comm.direct.CommDirect";
	protected Vector		relative_position 	= new Vector();
	
	protected Comm myComm;
	
	protected Memory myMemory;
	
	// Eventos periódicos - Tempos configurados pelo EventServer
	protected long startTime;
	protected long frameTime;
	protected long period;
	protected long sendDeadline;
	
	protected long workingFrame;
	protected long happeningFrame;
		
	@Override
	public boolean start() {
		
		// Define o tipo de evento que o EventHandler irá tratar
		// TODO Colocar um tratamento de erro aqui!
		if (this.eventType == null) {
			this.eventType = parameters.get(Constants.PARAM_EVT_TYPE);
		}
		
		// Initializes the communication channel
		try {
			Class commClass = Class.forName(commType);
			myComm = (Comm)commClass.newInstance();
			Parameters commParam = new Parameters();
			commParam.put(Constants.PARAM_COMM_AGENT, getAgent());
			if (getType().equals(Constants.COMP_SENSOR)) {
				commParam.put(Constants.PARAM_COMM_SENSING, this);
			} 
			else if (getType().equals(Constants.COMP_ACTUATOR)) {
				commParam.put(Constants.PARAM_COMM_ACTING, this);
			}
			commParam.put(Constants.PARAM_COMM_AP, getComponentName());
			myComm.setParameters(commParam);
			myComm.configure();
			if (!myComm.start()) {
				return false;
			}
		} catch (Exception e) {
//    		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "Comm class " + commType + " not found!");
			e.printStackTrace();
			return false;
		}
		
		return true;

	}
	
	@Override
	public boolean stop() {
		
		// Terminates de communication channel
		myComm.stop();
		
		return true;
		
	}
	
	public void setEventType(String eventType) {
		if (getState() == EA_STATE.CREATED) {
			this.eventType = eventType;
		} else {
    		MusicalAgent.logger.info("[" + getAgent().getAgentName() + ":" + getComponentName() + "] " + "Trying to set eventType after initialization!");
		}
	}

	public String getEventType() {
		return eventType;
	}
	
	public void setRelativePosition(Vector relative_position) {
		relative_position.copy(this.relative_position);
	}
	
	// TODO Precisa ser algo mais genérico que Vector3D!!!!
	public Vector getRelativePosition() {
		return relative_position;
	}
	
	/**
	 * Registra o EventHandler no EventServer correspondente 
	 */
	public void register() {

		// Procura o Agente Ambiente responsável pelo tipo de evento e o registra
		// TODO devemos garantir que só existe um agente responsável por tipo de evento
		// TODO o que acontece se não encontrar o Agente Ambiente?!?!
		// TODO pode necessitar de parâmetros, por exemplo, ouvido 'esquerdo' e 'direito'
		boolean es_registered = false;
		try {
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType(eventType);
			template.addServices(sd);
			for (int tries = 0; tries < 3; tries++) {
				DFAgentDescription[] result = DFService.search(getAgent(), template);
				if (result.length == 1) {
					es_registered = true;
					break;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception fe) {
			System.err.println("Environment Agent not found - ending MMS...");
			System.exit(-1);
		}
		// If found, sends the register command
		if (es_registered) {
			Command cmd = new Command(Constants.CMD_EVENT_REGISTER );
			cmd.addParameter(Constants.PARAM_COMP_NAME, getComponentName());
			cmd.addParameter(Constants.PARAM_COMP_TYPE, getType());
			cmd.addParameter(Constants.PARAM_EVT_TYPE, getEventType());
			cmd.addParameter(Constants.PARAM_REL_POS, getRelativePosition().toString());
			cmd.addUserParameters(getParameters());
			getAgent().sendMessage(cmd);
		} else {
//			MusicalAgent.logger.info("[" + getAgent().getAgentName() + ":" + getName() + "] " + "EventServer " + eventType + " not found");
			System.out.println("[" + getAgent().getAgentName() + ":" + getComponentName() + "] " + "EventServer " + eventType + " not registered");
		}

	}
	
	public void confirmRegistration(String eventExecution, Parameters serverParameters, Parameters extraParameters) {

		// Altera o status
		this.status = EH_STATUS.REGISTERED;

		this.eventExchange = eventExecution;

		if (eventExecution.equals(Constants.EVT_EXC_PERIODIC)) {
			// Configuration
			this.startTime 		= Long.valueOf(serverParameters.get(Constants.PARAM_START_TIME));
			this.workingFrame 	= Long.valueOf(serverParameters.get(Constants.PARAM_WORKING_FRAME));
			// TODO happeningFrame também deve ser enviado, para o caso de não ser o (workingFrame + 1) 
			this.happeningFrame = workingFrame + 1;
			// TODO Dar um tempo de sobra para o envio do evento
			this.period 		= Long.valueOf(serverParameters.get(Constants.PARAM_PERIOD));
			this.sendDeadline 	= Long.valueOf(serverParameters.get(Constants.PARAM_RCV_DEADLINE));

			// Armazena os parâmetros no Componente
			addParameter(Constants.PARAM_START_TIME, serverParameters.get(Constants.PARAM_START_TIME));
//			addParameter(Constants.PARAM_WORKING_FRAME, serverParameters.get(Constants.PARAM_WORKING_FRAME));
			addParameter(Constants.PARAM_PERIOD, serverParameters.get(Constants.PARAM_PERIOD));
			addParameter(Constants.PARAM_RCV_DEADLINE, serverParameters.get(Constants.PARAM_RCV_DEADLINE));
//			System.out.println(startTime + " " + workingFrame + " " + period + " " + sendDeadline);
		}
		addParameters(extraParameters);
		
		// Cria a memória relativa a esse EventHandler
		// TODO Falta ver a questão da expiração (de onde vai vir o parâmetro?)
		// TODO Pode ser que de problema a criação da memória estar aqui, se o usuário quiser usá-la antes
		myMemory = getAgent().getKB().createMemory(getComponentName(), eventType, 5.0, getParameters());
		if (myMemory == null) {
			System.err.println("Não foi possível criar a memória");
		}
//		MusicalAgent.logger.info("[" + getAgent().getAgentName() + ":" + getName() + "] " + "Memória de '" + getName() + "' do tipo '" + eventType + "' foi criada");

		// No caso de ser uma troca de evento frequente, armazena os parÃ¢metros
		if (eventExecution.equals(Constants.EVT_EXC_PERIODIC) && getType().equals(Constants.COMP_ACTUATOR)) {
			Actuator act = (Actuator)this;
			act.setEventFrequency();
		}
		
		// Avisa o agente do novo EventHandler registrado
		getAgent().eventHandlerRegistered(getComponentName());
		
		MusicalAgent.logger.info("[" + getAgent().getAgentName() + ":" + getComponentName() + "] " + "Register of '" + getComponentName() + "' confirmed");
		
	}
	
	/**
	 * Remove o registro do EventHandler do EventServer correspondente
	 */
	public void deregister() {
		
		// Envia mensagem para tirar o EventHandler do registro
		Command cmd = new Command(Constants.CMD_EVENT_DEREGISTER );
		cmd.addParameter(Constants.PARAM_COMP_NAME, getComponentName());
		cmd.addParameter(Constants.PARAM_COMP_TYPE, getType());
		cmd.addParameter(Constants.PARAM_EVT_TYPE, getEventType());
		getAgent().sendMessage(cmd);
		
	}
	
	public void confirmDeregistration() {
		
		this.status = EH_STATUS.NOT_REGISTERED;
		
		getAgent().eventHandlerDeregistered(getComponentName());
		
	}
	
}
