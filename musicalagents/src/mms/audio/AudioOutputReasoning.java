package mms.audio;

import java.nio.ByteBuffer;
import java.util.List;

import jade.util.Logger;

import org.jpab.Callback;
import org.jpab.Device;
import org.jpab.PortAudio;
import org.jpab.PortAudioException;
import org.jpab.Stream;
import org.jpab.StreamConfiguration;

import mms.Constants;
import mms.EventHandler;
import mms.MusicalAgent;
import mms.Reasoning;
import mms.Sensor;
import mms.clock.TimeUnit;
import mms.kb.Memory;
import mms.tools.AudioTools;

public class AudioOutputReasoning extends Reasoning {

	// Log
	public static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	// File
//	FileOutputStream out_byte_sink 	= null;

	// PortAudio
	Stream 	outStream;
	boolean firstCall = true, firstSound = true;
	double 	startTime, instant, period;
	double 	step = 1/44100.0;
	int 	frameSize;
	
	// Parameters
	int 	device;
	int 	channel;
	int 	maxChannels;
	
	// Sensor
	Sensor 	ear;
	Memory 	earMemory;

	@Override
	public boolean init() {
		
		// Opens audio file for writing
//		try {
//			out_byte_sink = new FileOutputStream(getAgent().getAgentName()+"_out.dat");
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
		
		device = Integer.valueOf(getParameter("device", "-1"));
		channel = Integer.valueOf(getParameter("channel", "0"));

		// Initializes PortAudio
		try {
			StreamConfiguration conf;
			if (device != -1) {
				// Configuration
				List<Device> devices = PortAudio.getDevices();
				conf = new StreamConfiguration();
				Device outDevice = devices.get(device);
				conf.setMode(StreamConfiguration.Mode.OUTPUT_ONLY);
				conf.setOutputDevice(outDevice);
				conf.setOutputChannels(outDevice.getMaxOutputChannels());
				conf.setOutputLatency(outDevice.getDefaultLowOutputLatency());
				conf.setOutputFormat(StreamConfiguration.SampleFormat.SIGNED_INTEGER_16);
				/*conf.setFlags()*/ 
				conf.setSampleRate(outDevice.getDefaultSampleRate());
			} else {
				conf = PortAudio.getDefaultStreamConfiguration(StreamConfiguration.Mode.OUTPUT_ONLY);
			}
			outStream = PortAudio.createStream(conf, new ProcessAudio(), null);
			maxChannels = outStream.getConfiguration().getOutputChannels();
		} catch (PortAudioException e) {
			e.printStackTrace();
			getAgent().logger.severe("[" + getName() + "] " + "PortAudio initialization error!");
			return false;
		}

		return true;
		
	}

	public boolean finit() {
//		try {
//			out_byte_sink.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		try {
			outStream.close();
		} catch (PortAudioException e) {
			e.printStackTrace();
		}
		
		return true;
	}

	@Override
	protected void eventHandlerRegistered(EventHandler evtHdl) {
		if (evtHdl.getEventType().equals(Constants.EVT_AUDIO)) {
			ear = (Sensor)evtHdl;
			ear.registerListener(this);
			period = Double.valueOf(ear.getParameter("PERIOD"))/1000.0;
			earMemory = getAgent().getKB().getMemory(ear.getName());
			try {
				outStream.start();
			} catch (PortAudioException e) {
				e.printStackTrace();
			}
		}
	}

 	@Override
	public void newSense(String eventType, double instant, double duration) {

//		System.out.println(getAgent().getClock().getCurrentTime() + " Recebi evento de " + instant + " até " + (instant+duration));
		double[] buf = (double[])earMemory.readMemory(instant, duration, TimeUnit.SECONDS);
//		for (int i = 0; i < buf.length; i++) {
//			if (Math.abs(buf[0]) > 0.2) {
//				System.out.println(getAgent().getClock().getCurrentTime() + " newSense() NAO É ZERO!!! instant = " + instant);
//				break;
//			}
//		}

	}

	class ProcessAudio implements Callback {

		@Override
		public State callback(ByteBuffer arg0, ByteBuffer arg1) {
			long now = getAgent().getClock().getCurrentTime();
			// If it's the first call, sets the startTime based in the mms's clock
			if (firstCall) {
				startTime = now /1000.0;
				instant = startTime;
				firstCall = false;
//				System.out.println("First call = " + instant);
			}
			double duration = (double)(arg1.capacity()/4) * step;
//			System.out.println(now + " vou ler de instant = " + instant + " até " + (instant+duration));
			double[] buf = (double[])earMemory.readMemory(instant, duration, TimeUnit.SECONDS);
//			if (buf[0] != 0.0 && firstSound) {
//				System.out.println(getAgent().getClock().getCurrentTime() + " callback() NAO É ZERO!!! instant = " + (instant));
//				firstSound = false;
//			}
			byte[] buffer = AudioTools.convertDoubleByte(buf, 0, buf.length);
//			System.out.println("buffer="+buffer.length + " - capacity=" + arg1.capacity());
			int ptr = 0;
			while (arg1.remaining() > 0) {
				for (int i = 0; i < maxChannels; i++) {
					// Se foi o canal escolhido, escreve o sample
					if (i == channel) {
						arg1.put(buffer[ptr++]);
						arg1.put(buffer[ptr++]);
					}
					// Caso contrário, silêncio
					else {
						arg1.put((byte)(0 & 0xFF));
						arg1.put((byte)((0 >> 8) & 0xFF));
					}
				}
			}
			instant = instant + duration;
			return State.RUNNING;
		}
		
	}
	
}
