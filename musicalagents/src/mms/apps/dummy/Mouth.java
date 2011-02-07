package mms.apps.dummy;

import mms.Actuator;

public class Mouth extends Actuator {
	
	public void configure(mms.Parameters parameters) {
		setEventType("AUDIO");
	};

}
