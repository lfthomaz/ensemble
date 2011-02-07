package mms.apps.dummy;

import mms.Actuator;
import mms.Constants;
import mms.EventHandler;
import mms.Reasoning;
import mms.Sensor;
import mms.clock.TimeUnit;
import mms.kb.Memory;

public class AudioR extends Reasoning {
	
	Actuator mouth;
	Memory mouthMemory;
	Sensor ear;
	Memory earMemory;
	int chunk_size;
	
	// TODO Deveria ser mais simples para se registra no EventHandler e obter sua memória de trabalho
	@Override
	protected void eventHandlerRegistered(EventHandler evtHdl) throws Exception {
		if (evtHdl instanceof Actuator && evtHdl.getEventType().equals("AUDIO")) {
			mouth = (Actuator)evtHdl;
			mouth.registerListener(this);
			mouthMemory = getAgent().getKB().getMemory(evtHdl.getName());
			chunk_size = Integer.parseInt(mouth.getParameter(Constants.PARAM_CHUNK_SIZE, "0"));
		}
		else if (evtHdl instanceof Sensor && evtHdl.getEventType().equals("AUDIO")) {
			ear = (Sensor)evtHdl;
			ear.registerListener(this);
			earMemory = getAgent().getKB().getMemory(ear.getName());
		}
	}
	
	@Override
	public void newSense(String eventType, double instant, double duration)
			throws Exception {
		double[] buf = (double[])earMemory.readMemory(instant, duration, TimeUnit.SECONDS);
	}
	
	@Override
	public void needAction(Actuator sourceActuator, double instant,
			double duration) throws Exception {
		double[] chunk = new double[chunk_size];
		mouthMemory.writeMemory(chunk, instant, duration, TimeUnit.SECONDS);
		mouth.act();
		System.out.println("Envie!");
	}

}
