package mms.comm;

import mms.Acting;
import mms.Event;
import mms.Sensing;
import jade.core.Agent;

public abstract class Comm {
	
	protected Agent 		myAgent;
	// TODO nome do ponto de acesso desse comm, pare ser utilizado na troca de eventos
	protected String 		myAccessPoint;
	protected Sensing 		mySensor;
	protected Acting		myActuator;
	
	/*
	 *  Controlam se um Comm está apto a receber eventos
	 */
	public boolean sensing 	= true;					
	/*
	 *  Controlam se o Comm está apto a enviar eventos
	 */
	public boolean actuating 	= true;					
	
	public void configure(Agent myAgent, Sensing mySensor, Acting myActuator, String myAccessPoint) {
		this.myAgent 		= myAgent;
		this.mySensor 		= mySensor;
		this.myActuator 	= myActuator;
		this.myAccessPoint 	= myAccessPoint;

		if (mySensor == null) {
			sensing = false;
		}
		if (myActuator == null) {
			actuating = false;
		}
	}

	/**
	 * Initializes the communication channel
	 * @return
	 */
	public abstract boolean start();
	
	/**
	 * Terminates de communication channel
	 * @return
	 */
	public abstract boolean end();
	
	// Event listener
	public abstract void receive(Event evt);

	// Event source
	public abstract void send(Event evt);

}
