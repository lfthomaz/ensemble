package mms.apps.lm;

import mms.EnvironmentAgent;

public class LM_Environment extends EnvironmentAgent {

	@Override
	protected void init() {

		this.addEventServer("mms.apps.lm.LM_SoundEventServer", null);
		this.addEventServer("mms.apps.lm.LM_LifeCycleEventServer", null);
		this.addEventServer("mms.apps.lm.LM_MovementEventServer", null);
		this.addEventServer("mms.apps.lm.LM_EnergyEventServer", null);

	}

	@Override
	protected void preUpdateClock() {
		
		world.getWorldGUI().update();
		
	}
	
}
