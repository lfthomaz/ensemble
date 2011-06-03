package mms.audio.jack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;

import jade.core.ServiceException;
import jade.domain.introspection.SuspendedAgent;
import jade.util.Logger;

import mms.Actuator;
import mms.Constants;
import mms.EventHandler;
import mms.MusicalAgent;
import mms.Reasoning;
import mms.Sensor;
import mms.audio.AudioConstants;
import mms.clock.TimeUnit;
import mms.memory.Memory;
import mms.memory.MemoryException;
import mms.tools.AudioTools;
import mmsjack.JACKCallback;

public class JACKInputReasoning extends Reasoning {

	// Log
//	public static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	// JACK
	JACKServerHelper jack; 
	double 						callbackStartTime;
	double 						step = 1/44100.0;
	
	HashMap<String,String> mapping = new HashMap<String, String>();
	
	// Actuator
	// TODO Por enquanto, só permite uma porta por reasoning
//	HashMap<String,Memory> 	earMemories = new HashMap<String, Memory>(2);
	double 		period;
	Memory 		mouthMemory;
	Actuator 	mouth;
	double[] 	dBuffer;

	@Override
	public boolean init() {
		
		// Gets the helper from JACK service
		try {
			jack = (JACKServerHelper)getAgent().getHelper(JACKServerService.NAME);
		} catch (ServiceException e) {
			System.err.println("[" + this.getAgent().getAgentName() + ":" + getComponentName()+ "] " + "JACK not available!");
			return false;
		}

		String[] str = getParameter("mapping", "").split(";");
		if (str.length == 1) {
			mapping.put(str[0], "");
		}
		else if (str.length == 2) {
			mapping.put(str[0], str[1]);
		} 
		else {
			System.err.println("[" + this.getAgent().getAgentName() + ":" + getComponentName()+ "] " + "no mapping in parameters!");
		}
		
		return true;
		
	}

	@Override
	public boolean finit() {

		return true;
		
	}

	@Override
	protected void eventHandlerRegistered(EventHandler evtHdl) {
		
		if (evtHdl instanceof Actuator && evtHdl.getEventType().equals(AudioConstants.EVT_TYPE_AUDIO)) {
			String actuatorName = evtHdl.getComponentName();
			if (mapping.containsKey(actuatorName)) {
				mouth = (Actuator)evtHdl;
				mouth.registerListener(this);
				period = Double.valueOf(mouth.getParameter("PERIOD"))/1000.0;
				mouthMemory = getAgent().getKB().getMemory(mouth.getComponentName());
				// Creats a JACK client
				jack.registerInputPort(getComponentName(), actuatorName, mapping.get(actuatorName), new Process());
			}
		}
		
	}

	@Override
	protected void eventHandlerDeregistered(EventHandler evtHdl) {
		String actuatorName = evtHdl.getComponentName();
		if (mapping.containsKey(actuatorName)) {
//			helper.unregisterPort("system:playback_1");
		}
	}

	@Override
	public void needAction(Actuator sourceActuator, double instant,
			double duration) throws Exception {

//		System.out.println("needAction() - t = " + instant + " até " + (instant+duration));
		// Teoricamente, já vai estar escrito na memória o que deve ser enviado,
		// pois foi preenchido pelo callback do JACK
		mouth.act();
		
	}
	
	class Process implements JACKCallback {

		boolean firstCall = true;
		double instant = 0;

		@Override
		public int process(ByteBuffer buffer, int nframes, double time) {

			if (firstCall) {
				// It must be 2 frames plus the latency in the future
				instant = getAgent().getClock().getCurrentTime(TimeUnit.SECONDS) + 
							(period * 2) + 
							(nframes * step);
				dBuffer = new double[nframes];
				firstCall = false;
			}
			

			double duration = (double)(nframes) * step;
//			System.out.println("Java::callback - t = " + instant + " até " + (instant+duration));

			FloatBuffer fIn = buffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
			int i = 0;
			while (fIn.remaining() > 0) {
				dBuffer[i++] = (double)fIn.get();
			}
			try {
				mouthMemory.writeMemory(dBuffer, instant, duration, TimeUnit.SECONDS);
//				System.out.println(now + " " + getAgent().getClock().getCurrentTime() + " Escrevi do instante " + (instant+period) + " até " + (instant+period+duration));
			} catch (MemoryException e) {
				e.printStackTrace();
			}
			instant = instant + duration;
			
			return 0;
		}

	};

	
}
