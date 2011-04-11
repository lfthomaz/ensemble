package mms.apps.rt;

import mms.Constants;
import mms.MusicalAgent;
import mms.Parameters;

public class RT_ListenerMusicalAgent extends MusicalAgent {

	public boolean configure() {
		
		Parameters args = new Parameters();
		args.put(Constants.PARAM_EVT_TYPE, Constants.EVT_AUDIO);
		args.put(Constants.PARAM_COMM_CLASS, "mms.comm.direct.CommDirect");
//		args.put("pos_x", parameters.get("pos_x"));
//		args.put("pos_y", parameters.get("pos_y"));
		addComponent("ear", "mms.Sensor", args);

//		Actuator feet = new Actuator("feet", this, "MOVEMENT");
//		feet.addParameter("pos_x", parameters.get("pos_x"));
//		feet.addParameter("pos_y", parameters.get("pos_y"));
//		feet.addParameter("vel_x", parameters.get("vel_x"));
//		feet.addParameter("vel_y", parameters.get("vel_y"));
//		feet.addParameter("acc_x", parameters.get("acc_x"));
//		feet.addParameter("acc_y", parameters.get("acc_y"));
//		addComponent(feet);

		addComponent("player", "rt.RT_ListenerReasoning", null);
		
		return true;
		
	}

}
