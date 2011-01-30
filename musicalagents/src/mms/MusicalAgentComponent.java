package mms;

import mms.Constants.EA_STATE;
import mms.commands.Command;

public abstract class MusicalAgentComponent {

	private String 			myName;
	private MusicalAgent 	myAgent;
	private String 			myType;
	private EA_STATE 		myState = EA_STATE.CREATED;
	
	protected Parameters parameters = new Parameters();
	
	public final String getName() {
		return myName;
	}
	
	protected final void setName(String myName) {
		if (myState == EA_STATE.CREATED) {
			this.myName = myName;
		} else {
			System.err.println("Cannot set name after being initialized...");
		}
	}
	
	public final MusicalAgent getAgent() {
		return myAgent;
	}

	protected final void setAgent(MusicalAgent myAgent) {
		if (myState == EA_STATE.CREATED) {
			this.myAgent = myAgent;
		} else {
			System.err.println("Cannot set agent after being initialized...");
		}
	}

	public final String getType() {
		return myType;
	}
	
	protected final void setType(String myType) {
		if (myState == EA_STATE.CREATED) {
			this.myType = myType;
		} else {
			System.err.println("Cannot set name after being initialized...");
		}
	}
	
	public final EA_STATE getState() {
		return myState;
	}

	protected final void setState(EA_STATE myState) {
		this.myState = myState;
	}

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

	public final void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}
	
	public final Parameters getParameters() {
		return parameters;
	}
	
	//--------------------------------------------------------------------------------
	// Life Cicle
	//--------------------------------------------------------------------------------
	
	/**
	 * Método de configuração do componente, a ser implementado pelo usuário
	 */
	protected void configure(Parameters parameters) {}
	
	/**
	 * Inicializa o componente 
	 */
	protected abstract boolean start(); 

	/**
	 * Initialization user method
	 * @return 
	 */
	protected boolean init() {
		return true;
	};
	
	/**
	 * Finalization user method
	 * @return 
	 */
	// TODO Implementar o controle do finalize()
	protected boolean finit() {
		return true;
	};
	
	/**
	 * Inicializa o componente 
	 */
	protected abstract boolean end(); 

	//--------------------------------------------------------------------------------
	// User implemented method
	//--------------------------------------------------------------------------------

	/**
	 * 
	 */
	public void processCommand(Command cmd) {
		System.out.println("[" + this.getAgent().getLocalName() + ":" + getName()  +"] " + "User command received: " + cmd);
	}

	/**
	 * Called when a parameter has been updated
	 * @param paramName
	 */
	public void parameterUpdated(String paramName) {
	
	}
	
}

