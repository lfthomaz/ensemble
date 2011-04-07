package mms;

import mms.clock.VirtualClockHelper;
import mms.clock.VirtualClockService;
import mms.router.CommandClientInterface;
import mms.router.RouterHelper;
import mms.router.RouterService;
import mms.router.osc.OSCServerHelper;
import mms.router.osc.OSCServerService;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.util.Logger;

public abstract class MMSAgent extends Agent implements CommandClientInterface {

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
	public Parameters getParameters() {
		return parameters;
	}
	
	/** 
	 * Clock Service
	 */
	private VirtualClockHelper clock;
	
	/** 
	 * Clock getter
	 * @return mms clock service
	 */
	public final VirtualClockHelper getClock() {
		return clock;
	}
	
	/**
	 * OSC Service
	 */
	private OSCServerHelper osc;
	
	/**
	 * OSC getter
	 */
	public final OSCServerHelper getOSC() {
		return osc;
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
		
//		System.out.println("[" + getLocalName() + "] MMSAgent setup()");
		
		// 1. Obtém os parâmetros de entrada do Agente
		Object[] arguments = getArguments();
		if (arguments != null && arguments[0] instanceof Parameters) {
			parameters = (Parameters)arguments[0];
		}
		isBatch = getProperty(Constants.PROCESS_MODE, null).equals(Constants.MODE_BATCH);
	
		// 2. Inicializa os serviços básicos do agente
		try {
			clock = (VirtualClockHelper)getHelper(VirtualClockService.NAME);
			osc = (OSCServerHelper)getHelper(OSCServerService.NAME);
			router = (RouterHelper)getHelper(RouterService.NAME);
			router.connect(this);
		} catch (ServiceException e) {
			logger.severe("[" + this.getLocalName() + "] " + "Service not available");
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

	// ---------------------------------------------- 
	// Command Interface 
	// ---------------------------------------------- 

	@Override
	public String getAddress() {
		return "MMS:"+getLocalName();
	}

	//--------------------------------------------------------------------------------
	// User implemented methods
	//--------------------------------------------------------------------------------

	/**
	 * 
	 */
	protected void configure() {
//		System.out.println("[" + getLocalName() + "] MMSAgent configure()");
	}
	
	/**
	 * 
	 */
	protected void start() {
//		System.out.println("[" + getLocalName() + "] MMSAgent start()");
	}
	
	/**
	 * 
	 */
	protected void init() {
//		System.out.println("[" + getLocalName() + "] MMSAgent init()");
	}
	
	/**
	 * 
	 */
	protected void finit() {
//		System.out.println("[" + getLocalName() + "] MMSAgent finit()");
	}

	/**
	 * 
	 */
	protected void stop() {
//		System.out.println("[" + getLocalName() + "] MMSAgent finit()");
	}

}
