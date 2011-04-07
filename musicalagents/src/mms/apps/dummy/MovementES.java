package mms.apps.dummy;

import mms.Constants;
import mms.EventServer;
import mms.Parameters;
import mms.clock.TimeUnit;
import mms.memory.AudioMemory;
import mms.memory.CircularBufferMemory;
import mms.memory.Memory;
import mms.world.EntityState;

public class MovementES extends EventServer {

	Memory mem = null;
	
	@Override
	protected void configure() {
		setEventType("MOVEMENT");
		setEventExchange(1000, 0);
	}
	
	@Override
	protected Parameters actuatorRegistered(String agentName,
			String eventHandlerName, Parameters userParam) throws Exception {
		
		userParam.put(Constants.PARAM_PERIOD, "100");
		userParam.put(Constants.PARAM_START_TIME, String.valueOf(this.startTime));
		mem = new CircularBufferMemory();
		mem.start(envAgent, "MOVEMENT", 5.0, 5.0, userParam);
		
		return userParam;
	
	}
	
	@Override
	protected void process() throws Exception {

		double t = (double)(envAgent.getClock().getCurrentTime()) / 1000.0;
		if (mem != null) {
			// A cada ciclo, armazenar a informação no CircularBuffer
			EntityState state = new EntityState();
			state.setInstant(t);
			mem.writeMemory(state, t, 0.1, TimeUnit.SECONDS);
			// Caso não tenha informação, copiar a posição antiga
			
		}
		
	
	}

}
