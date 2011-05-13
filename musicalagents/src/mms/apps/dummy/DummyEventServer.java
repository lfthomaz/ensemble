package mms.apps.dummy;

import java.util.Enumeration;

import mms.Event;
import mms.EventServer;
import mms.Parameters;
import mms.memory.EventMemory;
import mms.memory.Memory;
import mms.world.World;

public class DummyEventServer extends EventServer {
	
	private World world;
	
	private int eventsReceived = 0;
	
	@Override
	public boolean configure() {
		setEventType("DUMMY");
		setEventExchange(100, 40, 80, 2000);
		return true;
	}

	@Override
	public boolean init() {
		world = envAgent.getWorld();
		return true;
	}

	@Override
	public boolean finit() {
		return true;
	}
	
	@Override
	protected Parameters actuatorRegistered(String agentName, String eventHandlerName, Parameters userParam) throws Exception {

		// Parâmetos
		Parameters userParameters = new Parameters();
//		userParameters.put(Constants.PARAM_CHUNK_SIZE, String.valueOf());
		
		// Cria uma memória para o atuador
		String memoryName = agentName+":"+eventHandlerName;
		Memory memory = new EventMemory();
		memory.start(envAgent, "DUMMY", 2.5, 2.5, userParameters);
    	world.addEntityStateAttribute(agentName, "DUMMY", memory);
    	
		return userParameters;

	}
	
	@Override
	protected Parameters sensorRegistered(String agentName, String eventHandlerName, Parameters userParam) throws Exception {
		
		// Parâmetos
		Parameters userParameters = new Parameters();

		return userParameters;

	}
	
	@Override
	protected void processSense(Event evt) throws Exception {
 
		if (evt.frame == workingFrame) {
			eventsReceived++;
		} else {
//			eventsReceived++;
			System.out.println("PROBLEMAS!");
		}
		
		// Recebe o evento e armazena na memória
		Memory memory = (Memory)world.getEntityStateAttribute(evt.oriAgentName, "DUMMY");
		memory.writeMemory(evt);
		
	}
	
	@Override
	protected void process() throws Exception {
		System.out.println("process - " + workingFrame + " - " + eventsReceived);
		eventsReceived = 0;
		for (Enumeration<String> s = sensors.keys(); s.hasMoreElements();) {

			String s_key = s.nextElement();
			String[] sensor = s_key.split(":");

			// Cria o evento a ser enviado para o sensor
			Event evt = new Event();
			evt.destAgentName = sensor[0];
			evt.destAgentCompName = sensor[1];
			double[] buf = new double[1024];
			evt.objContent = buf;
//			evt.instant = instant;
//			evt.duration = (double)(CHUNK_SIZE * STEP);
			
			// Puts the newly created event in the output queue
			addOutputEvent(evt.destAgentName, evt.destAgentCompName, evt);
			
		}
	}

}
