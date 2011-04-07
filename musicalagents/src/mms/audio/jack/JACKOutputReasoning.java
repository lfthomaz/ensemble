package mms.audio.jack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;

import jade.core.ServiceException;
import jade.util.Logger;

import mms.Constants;
import mms.EventHandler;
import mms.MusicalAgent;
import mms.Reasoning;
import mms.Sensor;
import mms.clock.TimeUnit;
import mms.kb.Memory;
import mmsjack.JACKCallback;

public class JACKOutputReasoning extends Reasoning {

	// Log
	public static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	// JACK
	JACKServerHelper jack; 
	double 						callbackStartTime, period;
	double 						step = 1/44100.0;
	
	HashMap<String,String> mapping = new HashMap<String, String>();
	
	// Sensor
	// TODO Por enquanto, só permite uma porta por reasoning
//	HashMap<String,Memory> 	earMemories = new HashMap<String, Memory>(2);
	Memory earMemory;

	@Override
	public boolean init() {
		
		// Gets the helper from JACK service
		try {
			jack = (JACKServerHelper)getAgent().getHelper(JACKServerService.NAME);
		} catch (ServiceException e) {
			System.err.println("[" + this.getAgent().getAgentName() + ":" + getName()+ "] " + "JACK not available!");
			return false;
		}

		String[] str = getParameter("mapping", "").split(";");
		if (str.length == 2) {
			mapping.put(str[0], str[1]);
		} else {
			System.err.println("[" + this.getAgent().getAgentName() + ":" + getName()+ "] " + "no mapping in parameters!");
		}
		
		return true;
		
	}

	@Override
	public boolean finit() {

		return true;
		
	}

	@Override
	protected void eventHandlerRegistered(EventHandler evtHdl) {
		
		if (evtHdl instanceof Sensor && evtHdl.getEventType().equals(Constants.EVT_AUDIO)) {
			Sensor ear = (Sensor)evtHdl;
			String sensorName = ear.getName();
			if (mapping.containsKey(sensorName)) {
				ear.registerListener(this);
				period = Double.valueOf(ear.getParameter("PERIOD"))/1000.0;
				earMemory = getAgent().getKB().getMemory(ear.getName());
				// Creats a JACK client
				jack.registerOutputPort(getName(), sensorName, mapping.get(sensorName), new Process());
			}
		}
		
	}

	@Override
	protected void eventHandlerDeregistered(EventHandler evtHdl) {
		String sensorName = evtHdl.getName();
		if (mapping.containsKey(sensorName)) {
////		helper.unregisterPort("system:playback_1");
		}
	}
	
	@Override
	public void newSense(Sensor sourceSensor, double instant, double duration) {

//		System.out.println(System.currentTimeMillis() + " Recebi evento de " + instant + " até " + (instant+duration));

	}

	class Process implements JACKCallback {

		boolean firstCall = true;
		double instant = 0;

		@Override
		public int process(ByteBuffer buffer, int nframes, double time) {

//			System.out.printf("Java::callback(%d)\n", nframes);

			if (firstCall) {
				instant = getAgent().getClock().getCurrentTime(TimeUnit.SECONDS) - (nframes * step);
				firstCall = false;
			}
			
			double duration = (double)(nframes) * step;
			double[] buf = (double[])earMemory.readMemory(instant, duration, TimeUnit.SECONDS);
			FloatBuffer fOut = buffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
			int ptr = 0;
			while (fOut.remaining() > 0) {
				fOut.put((float)buf[ptr++]);
			}
			instant = instant + duration;
			return 0;
		}

	};

	
}
