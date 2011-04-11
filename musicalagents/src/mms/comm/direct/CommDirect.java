package mms.comm.direct;

import mms.Acting;
import mms.Event;
import mms.MusicalAgent;
import mms.Sensing;
import mms.comm.Comm;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.util.Logger;

public class CommDirect extends Comm {
	
	//----------------------------------------------------------
	// Log
	private static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	//----------------------------------------------------------
	// Serviço de CommDirect
	protected CommDirectHelper commDirect;
	
	@Override
	public boolean start() {
		
		try {
//			System.out.println("COMM = " + myAgent.getName());
			commDirect = (CommDirectHelper)myAgent.getHelper(CommDirectService.NAME);
		} catch (ServiceException e) {
//			logger.severe("[" + myAgent.getAgentName() + "] " + "CommDirect service not available");
			System.err.println("[" + myAgent.getAgentName() + "] " + "CommDirect service not available");
			return false;
		}

		// registrar Agente/EventHandler no Service CommDirect
		commDirect.register(myAgent.getAgentName(), myAccessPoint, this);
		
		return true;
		
	}
	
	@Override
	public boolean stop() {
		
		commDirect.deregister(myAgent.getAgentName(), myAccessPoint);
		
		return true;
		
	}

	@Override
	public void receive(Event evt) {
//		MusicalAgent.logger.info("[" + myAgent.getAID().getAgentName() + ":" + myAccessPoint + "] " + "Enviei evento via CommDirect");
		//eventQueue.add(evt);
		if (mySensor != null) {
			mySensor.sense(evt);
		} else {
			MusicalAgent.logger.warning("[" + myAgent.getName() + "] " + "ERRO: não pertenço um sensor!");
		}
 	}

	@Override
	public void send(Event evt) {
//		MusicalAgent.logger.info("[" + myAgent.getAID().getAgentName() + ":" + myAccessPoint + "] " + "Enviei evento via CommDirect");
		commDirect.send(evt);
	}

}
