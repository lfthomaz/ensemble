package mms.apps.rt;

import mms.Constants;
import mms.EnvironmentAgent;
import mms.Parameters;

public class RT_EnvironmentAgent extends EnvironmentAgent {

	public void configure() {
		
		Parameters parameters;
		
		// OSC Message EventServer
//		addEventServer("mms.osc.OSCEventServer", null);

		// Movement EventServer
//		parameters = new Parameters();
//		parameters.put("es_comm", "mms.comm.CommDirect");
//		parameters.put("es_type", "MOVEMENT");
//		parameters.put("es_period", "100 30 90 5000");
//		addEventServer("mms.movement.MovementEventServer", null);

		// Audio EventServer
		parameters = new Parameters();
		parameters.put("es_comm", "mms.comm.CommDirect");
		parameters.put("es_type", Constants.EVT_AUDIO);
		parameters.put("es_period", "100 45 90 5000");
		addEventServer("mms.audio.AudioEventServerSimple", null);
		
	}
	
	@Override
	protected void init() {
	}

}
