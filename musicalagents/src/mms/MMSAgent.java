package mms;

import mms.clock.VirtualClockHelper;
import mms.clock.VirtualClockService;
import mms.router.CommandClientInterface;
import mms.router.RouterHelper;
import mms.router.RouterService;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.util.Logger;

public abstract class MMSAgent extends Agent implements LifeCycle {

	/**
	 *  Log
	 */
	public static final Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	/**
	 *  Agent Parameters
	 */
	protected Parameters parameters = null;

	/**
	 * Parameters getter
	 * @return initialized parameters
	 */
	@Override
	public final Parameters getParameters() {
		return parameters;
	}

	/**
	 * Parameters setter
	 * @return initialized parameters
	 */
	@Override
	public final void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}
	
	/** 
	 * Clock Service
	 */
	private VirtualClockHelper clock;
	
	/** 
	 * Gets agent's name
	 * @return 
	 */
	public final String getAgentName() {
		return getLocalName();
	}
	
	/** 
	 * Clock getter
	 * @return mms clock service
	 */
	public final VirtualClockHelper getClock() {
		return clock;
	}
	
	/**
	 * Router Service
	 */
	private RouterHelper router;
	
	public RouterHelper getRouter() {
		return router;
	}
	
	protected boolean isBatch = false;
	
	/**
	 * Initialization method called by JADE
	 */
	protected void setup() {
		
		System.out.println("[" + getAgentName() + "] MMSAgent setup()");
		
		// 1. Obtém os parâmetros de entrada do Agente
		Object[] arguments = getArguments();
		if (arguments != null && arguments[0] instanceof Parameters) {
			parameters = (Parameters)arguments[0];
		}
		isBatch = getProperty(Constants.PROCESS_MODE, null).equals(Constants.MODE_BATCH);
	
		// 2. Inicializa os serviços básicos do agente
		try {
			clock = (VirtualClockHelper)getHelper(VirtualClockService.NAME);
			router = (RouterHelper)getHelper(RouterService.NAME);
		} catch (ServiceException e) {
			logger.severe("[" + this.getAgentName() + "] " + "Service not available");
			this.doDelete();
		}
		
		// 3. Executa o método de configuração do usuário
		configure();

		// 4. Inicializa o agente
		start();

	}
	
	/**
	 * Finalization method called by JADE
	 */
	protected void takeDown() {
		System.out.println("takeDown()");
	}

	//--------------------------------------------------------------------------------
	// User implemented methods
	//--------------------------------------------------------------------------------

	/**
	 * 
	 */
	public boolean configure() {
//		System.out.println("[" + getAgentName() + "] MMSAgent configure()");
		return true;
	}
	
	/**
	 * 
	 */
	public boolean start() {
//		System.out.println("[" + getAgentName() + "] MMSAgent start()");
		return true;
	}
	
	/**
	 * 
	 */
	public boolean init() {
//		System.out.println("[" + getAgentName() + "] MMSAgent init()");
		return true;
	}
	
	/**
	 * 
	 */
	public boolean finit() {
//		System.out.println("[" + getAgentName() + "] MMSAgent finit()");
		return true;
	}

	/**
	 * 
	 */
	public boolean stop() {
//		System.out.println("[" + getAgentName() + "] MMSAgent finit()");
		return true;
	}

}
