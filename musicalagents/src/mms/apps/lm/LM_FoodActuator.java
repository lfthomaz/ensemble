package mms.apps.lm;

import mms.Actuator;
import mms.Parameters;

public class LM_FoodActuator extends Actuator {

	@Override
	protected void configure(Parameters parameters) {
		setEventType("ENERGY");
	}

}
