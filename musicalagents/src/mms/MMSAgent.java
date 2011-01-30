package mms;

import mms.clock.VirtualClockHelper;
import mms.clock.VirtualClockService;
import mms.commands.Command;
import mms.commands.CommandClientInterface;
import mms.commands.RouterHelper;
import mms.commands.RouterService;
import mms.osc.OSCServerHelper;
import mms.osc.OSCServerService;
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
	
	/**
	 * Método de inicialização utilizado pelo JADE
	 */
	protected void setup() {
		
//		System.out.println("[" + getLocalName() + "] MMSAgent setup()");
		
		// 1. Obtém os parâmetros de entrada do Agente
		Object[] arguments = getArguments();
		if (arguments != null && arguments[0] instanceof Parameters) {
			parameters = (Parameters)arguments[0];
		}
		
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

		// 5. Executa o método de inicialização do usuário
		init();
		
		// 6. Atualiza o estado do agente (?)
		
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
	protected void finalize() {
		System.out.println("[" + getLocalName() + "] MMSAgent finalize()");
	}

}
