package mms;

import mms.Constants.EA_STATE;
import mms.Constants.MA_STATE;
import mms.clock.TimeUnit;
import mms.clock.VirtualClockHelper;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentState;

public class Reasoning extends MusicalAgentComponent {

	ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
	Behaviour cyclicBehaviour = null;
	
	VirtualClockHelper clock;

	// TODO No caso de batch, que o process é chamado por uma thread, o que acontece com os métodos newSense e needActuatuion nesse caso?!?!
	@Override
	protected final boolean start() {
		
		// Sets component type
		setType(Constants.COMP_REASONING);
		
		// Gets clock service
		clock = getAgent().getClock();

		// TODO Verificar quais são os eventHandlers necessários para o funcionamento do raciocinio
		
		// Calls user initialization code
		if (!init()) {
			return false;
		}
		
		// Sets the agent's state to INITIALIZED
		setState(EA_STATE.INITIALIZED);
		
		// Cycle Behaviour que controla o raciocínio
		if (parameters.get(Constants.PARAM_REAS_CYCLIC, "false").equals("true")) {
			cyclicBehaviour = new ReasonCyclic(getAgent()); 
			getAgent().addBehaviour(tbf.wrap(cyclicBehaviour));
		}

		return true;

	}
	
	@Override
	protected final boolean stop() {

		// Removes the CyclicBehaviour
		if (cyclicBehaviour != null) {
			getAgent().removeBehaviour(cyclicBehaviour);
		}
		
		// Calls user finalization method
		if (!finit()) {
			return false;
		}
		
		// Sets the agent's state to 
		setState(Constants.EA_STATE.FINALIZED);
		
		return true;
	}
	
	public void setWakeUp(long time) {
		
		// No caso de processamento Batch, coloca o Agente para dormir até o primeiro turno
		getAgent().getClock().schedule(getAgent(), new ReasonBatch(), time);
		
	}
	
	class ReasonBatch implements Runnable {

		public void run() {
		
			// Apenas processa o raciocínio se o agente estiver ativo
			if (getAgent().state == MA_STATE.REGISTERED) {
			
				MusicalAgent.logger.info("[" + getAgent().getLocalName() + "] " + "Iniciei o raciocínio");
				
				try {
					process();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
//				if (getAgent().getProperty(Constants.PROCESS_MODE, null).equals(Constants.MODE_BATCH)) {
//					//System.out.println(getAgent().getLocalName() + " foi dormir!");
//					// TODO E se quiser dormir mais de 1 turno???
//					long when = (long)clock.getCurrentTime(TimeUnit.TURNS) + 1; 
//					setWakeUp(when);
//				}
//				
				getAgent().reasoningProcessDone(getName());
				
			}
			
		}
		
	}
	
	/**
	 * Processamento cíclico do raciocínio
	 * @author lfthomaz
	 *
	 */
	private class ReasonCyclic extends CyclicBehaviour {

		public ReasonCyclic(Agent a) {
			super(a);
		}
		
		public void action() {
			try {
				process();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	
	}

	//--------------------------------------------------------------------------------
	// User implemented methods
	//--------------------------------------------------------------------------------

	/**
	 * Chamado no momento em que um novo EventHandler é registrado no Agente Musical
	 */
	protected void eventHandlerRegistered(EventHandler evtHdl) throws Exception {};
	
	/**
	 * Chamado no momento em que um novo EventHandler é registrado no Agente Musical
	 */
	protected void eventHandlerDeregistered(EventHandler evtHdl) throws Exception {};

	/**
	 * Chamado no momento que o Agente Musical recebe um novo evento (e o raciocínio está registrado no Sensor)
	 * @param eventType
	 * @param instant
	 * @param duration
	 * @throws Exception
	 */
	public void newSense(Sensor sourceSensor, double instant, double duration) throws Exception {};

	/**
	 * Chamado no caso de eventos frequentes, quando existe a necessidade de uma ação
	 * @param sourceActuator
	 * @param workingFrame
	 * @throws Exception
	 */
	public void needAction(Actuator sourceActuator, long workingFrame) throws Exception {};
	
	/**
	 * Chamado no caso de eventos frequentes, quando existe a necessidade de uma ação
	 * @param sourceActuator
	 * @param instant
	 * @param duration
	 * @throws Exception
	 */
	public void needAction(Actuator sourceActuator, double instant, double duration) throws Exception {};

	/**
	 * Método de processamento do raciocínio
	 * @throws Exception
	 */
	public void process() throws Exception {}

}
