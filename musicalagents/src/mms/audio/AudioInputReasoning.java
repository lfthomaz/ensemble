package mms.audio;

import jade.util.Logger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.jpab.Callback;
import org.jpab.Device;
import org.jpab.PortAudio;
import org.jpab.PortAudioException;
import org.jpab.Stream;
import org.jpab.StreamConfiguration;
import org.jpab.Callback.State;
import org.jpab.StreamConfiguration.Mode;

import mms.Actuator;
import mms.Constants;
import mms.MusicalAgent;
import mms.Sensor;
import mms.Constants.EA_STATE;
import mms.EventHandler;
import mms.Reasoning;
import mms.audio.AudioOutputReasoning.ProcessAudio;
import mms.clock.TimeUnit;
import mms.kb.Memory;
import mms.kb.MemoryException;
import mms.tools.AudioTools;

public class AudioInputReasoning extends Reasoning {

	// Log
	public static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	// File
//	FileOutputStream out_byte_sink 	= null;

	// PortAudio
	Stream 	inStream;
	boolean firstCall = true, firstSound = true;
	double 	startTime, instant, period;
	double 	step = 1/44100.0;
	int 	frameSize;

	// Parameters
	int 	device;
	int 	channel;
	int 	maxChannels;
	
	// Actuator
	Actuator 	mouth;
	Memory 		mouthMemory;
	
	/**
	 * Init the Mic Line
	 */
	@Override
	public boolean init() {

//		// Opens audio file for writing
//		try {
//			out_byte_sink = new FileOutputStream(getAgent().getAgentName()+"_out.dat");
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
		
		device = Integer.valueOf(getParameter("device", "-1"));
		channel = Integer.valueOf(getParameter("channel", "0"));

		// Initializes PortAudio
		try {
			// Configuration
			StreamConfiguration conf;
			if (device == -1) {
				conf = PortAudio.getDefaultStreamConfiguration(Mode.INPUT_ONLY);
			} else {
				List<Device> devices = PortAudio.getDevices();
	//			for (int i = 0; i < devices.size(); i++) {
	//				System.out.println("["+i+"] " + devices.get(i).getName());
	//			}
				conf = new StreamConfiguration();
				Device inDevice = devices.get(1); // FA-101 In 1
				conf.setMode(StreamConfiguration.Mode.INPUT_ONLY); /* acho que é opcional */
				conf.setInputDevice(inDevice);
				conf.setInputChannels(inDevice.getMaxInputChannels());
				conf.setInputLatency(inDevice.getDefaultLowInputLatency());
				conf.setInputFormat(StreamConfiguration.SampleFormat.SIGNED_INTEGER_16);
				/*conf.setFlags()*/ 
				conf.setSampleRate(inDevice.getDefaultSampleRate());
			}
			// Open Stream
			inStream = PortAudio.createStream(conf, new ProcessAudio(), null);
			maxChannels = inStream.getConfiguration().getInputChannels();
		} catch (PortAudioException e) {
			e.printStackTrace();
			getAgent().logger.severe("[" + getName() + "] " + "PortAudio initialization error!");
			return false;
		}

		return true;
	}
	
	/**
	 * Finalizes the Mic Line
	 */
	public boolean finit() {
//		try {
//			out_byte_sink.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	
		try {
			inStream.close();
		} catch (PortAudioException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	@Override
	protected void eventHandlerRegistered(EventHandler evtHdl) {

		if (evtHdl instanceof Actuator && evtHdl.getEventType().equals(Constants.EVT_AUDIO)) {
			mouth = (Actuator)evtHdl;
			mouth.setAutomaticAction(true);
			mouth.registerListener(this);
			period = Double.valueOf(mouth.getParameter("PERIOD"))/1000.0;
			mouthMemory = getAgent().getKB().getMemory(mouth.getName());
			try {
				inStream.start();
			} catch (PortAudioException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void needAction(Actuator sourceActuator, double instant, double duration) {
//		System.out.println("[" + getAgent().getClock().getCurrentTime() + "] needAction() - instant = " + instant);
//		try {
//			mouthMemory.writeMemory(chunk, instant, duration, TimeUnit.SECONDS);
//			mouth.act();
//		} catch (MemoryException e) {
//			e.printStackTrace();
//		}
	}
	
	class ProcessAudio implements Callback {

		@Override
		public State callback(ByteBuffer arg0, ByteBuffer arg1) {
			
			long now = (long)getAgent().getClock().getCurrentTime(TimeUnit.MILLISECONDS);
			
			// If it's the first call, sets the startTime based in the mms's clock
			if (firstCall) {
				startTime = (now)/1000.0;
				instant = startTime;
				firstCall = false;
				System.out.println("[AudioInputReasoning] First call = " + instant);
			}
//			System.out.println("instant = " + instant + " - duration = " + duration);
			
			int ptr = 0;
			byte[] buffer = new byte[arg0.capacity()/maxChannels];
			while (arg0.remaining() > 0) {
				for (int i = 0; i < maxChannels; i++) {
					// Se foi o canal escolhido, guarda o sample
					if (i == channel) {
						buffer[ptr++] = arg0.get();
						buffer[ptr++] = arg0.get();
					}
					// Descarta os outros canais
					else {
						arg0.get();
						arg0.get();
					}
				}
			}

//		    try {
//				out_byte_sink.write(buffer);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}

			double[] d_buf = AudioTools.convertByteDouble(buffer, 0, buffer.length);
			double duration = d_buf.length * step;
			try {
				mouthMemory.writeMemory(d_buf, (instant+period+period), duration, TimeUnit.SECONDS);
//				System.out.println(now + " " + getAgent().getClock().getCurrentTime() + " Escrevi do instante " + (instant+period) + " até " + (instant+period+duration));
			} catch (MemoryException e) {
				e.printStackTrace();
			}

			instant = instant + duration;

			return State.RUNNING;
		}
		
	}
	
}
