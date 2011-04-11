package mms.apps.lm;

import mms.EnvironmentAgent;

public class LM_Environment extends EnvironmentAgent {

	@Override
	public boolean configure() {

		this.addEventServer("mms.apps.lm.LM_MovementEventServer", null);
		this.addEventServer("mms.apps.lm.LM_SoundEventServer", null);
//		this.addEventServer("mms.apps.lm.LM_EnergyEventServer", null);
		this.addEventServer("mms.apps.lm.LM_LifeCycleEventServer", null);
		
		return true;

	}

	@Override
	protected void preUpdateClock() {
		
		world.getWorldGUI().update();
		
	}
	
}
