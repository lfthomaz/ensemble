package mms.apps.lm;

import mms.Actuator;
import mms.Event;
import mms.MusicalAgent;
import mms.Parameters;

public class LM_MovementActuator extends Actuator {

	@Override
	protected void configure(Parameters parameters) {
		setEventType("MOVEMENT");
	}

	@Override
	protected boolean init() {
		
		//getAgent().getKB().writeEventRepository(getEventType(), new String());
		return true;
		
	}

	@Override
	public void process(Event evt) {

		//String rep = (String)getAgent().getKB().readEventRepository(this.getEventType());
		//evt.content = getAgent().getLocalName() + " " + rep;
		
	}

}
