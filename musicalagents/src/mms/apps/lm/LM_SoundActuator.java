package mms.apps.lm;

import mms.Actuator;
import mms.Event;
import mms.MusicalAgent;
import mms.Parameters;

public class LM_SoundActuator extends Actuator {

	@Override
	protected void configure(Parameters parameters) {
		setEventType("SOUND");
	}

	@Override
	protected boolean init() {
		//getAgent().getKB().writeEventRepository(getEventType(), new String());
		return true;
	}

	@Override
	public void process(Event evt) {
		
		// Ao cantar, basta pegar a primeira nota do genoma musical
		
		// evt = new Event();
		//evt.content = (String)getAgent().getKB().readEventRepository("SOUND");
		
		// return evt;
		
	}

}
