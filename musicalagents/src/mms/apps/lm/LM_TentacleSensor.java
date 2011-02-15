package mms.apps.lm;

import mms.Event;
import mms.MusicalAgent;
import mms.Parameters;
import mms.Sensor;

public class LM_TentacleSensor extends Sensor {

	@Override
	protected void configure(Parameters parameters) {
		setEventType("MOVEMENT");
	}

	@Override
	protected boolean init() {
		getAgent().getKB().updateFact("SpeciePresent", "0");
		return true;
	}

	@Override
	protected void process(Event evt) {
		
		System.out.println(getAgent().getLocalName() + ":" + getName() + " recebeu um evento: " + (String)evt.objContent);
		//int note = Integer.parseInt(evt.content);
		//getAgent().getKB().writeFact("SpeciePresent", note);
		
	}

}
