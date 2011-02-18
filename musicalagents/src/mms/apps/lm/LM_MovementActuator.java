package mms.apps.lm;

import mms.Actuator;
import mms.Parameters;

public class LM_MovementActuator extends Actuator {

	@Override
	protected void configure(Parameters parameters) {
		setEventType("MOVEMENT");
	}

}
