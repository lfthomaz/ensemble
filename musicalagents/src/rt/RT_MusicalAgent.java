package rt;

import mms.Actuator;
import mms.Constants;
import mms.MusicalAgent;
import mms.Parameters;
import mms.Reasoning;
import mms.Sensor;
import mms.kb.AudioMemory;

public class RT_MusicalAgent extends MusicalAgent {

	@Override
	protected void configure() {
	
		// Raciocínios
//		addComponent(new RT_Reasoning("Reasoning", this));
//		Reasoning reasoning = new RT_MovementReasoning("MovementReasoning", this);
//		reasoning.addParameter("pos_x", parameters.get("pos_x","0.0"));
//		reasoning.addParameter("pos_y", parameters.get("pos_y","0.0"));
//		addComponent(reasoning);
		
		// Atuador de movimento do Agente
//		Actuator feet = new Actuator("feet", this, "MOVEMENT");
//		feet.addParameter("pos_x", parameters.get("pos_x","0.0"));
//		feet.addParameter("pos_y", parameters.get("pos_y","0.0"));
//		feet.addParameter("vel_x", parameters.get("vel_x","0.0"));
//		feet.addParameter("vel_y", parameters.get("vel_y","0.0"));
//		feet.addParameter("acc_x", parameters.get("acc_x","0.0"));
//		feet.addParameter("acc_y", parameters.get("acc_y","0.0"));
//		addComponent(feet);
		
		// Atuador e Sensor de Som
//		addComponent(new Sensor("Ear", this, Constants.EVT_AUDIO));
//		addComponent(new Actuator("Mouth", this, Constants.EVT_AUDIO));

	}
	
	@Override
	protected void init() {

		System.out.println("Recebu o filename = " + parameters.get("filename"));
		
		// Fatos necessários na base de conhecimento
		getKB().updateFact("filename", parameters.get("filename"));
		
	}

}
