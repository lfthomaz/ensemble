package mms.audio;

import java.nio.ByteBuffer;

import jade.util.Logger;

import portaudio.PaCallback;
import portaudio.PaDeviceInfo;
import portaudio.PaStreamParameters;
import portaudio.portaudio;

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

	// PortAudio
	long 	stream = 0;
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
		
		device = Integer.valueOf(getParameter("device", "-1"));
		channel = Integer.valueOf(getParameter("channel", "0"));
		
		// Initializes PortAudio
		System.out.println("Starting portaudio...");
		int err = portaudio.Pa_Initialize();

		if (device != -1) {
			System.out.println("Opening stream...");
			// Gets DeviceInfo
			PaDeviceInfo info = portaudio.Pa_GetDeviceInfo(device);
			double sr = info.getDefaultSampleRate();
			maxChannels = info.getMaxOutputChannels();
			// Sets Parameters
			PaStreamParameters outputParameters = new PaStreamParameters();
			outputParameters.setDevice(device);
			outputParameters.setChannelCount(maxChannels);
			outputParameters.setHostApiSpecificStreamInfo(null);
			outputParameters.setSampleFormat(portaudio.SIGNED_INTEGER_16);
			outputParameters.setSuggestedLatency(info.getDefaultLowOutputLatency());
			// Opens the stream
			stream = portaudio.Pa_OpenStream(null, outputParameters, sr, 256, 0, new Callback());
			System.out.println("Java::stream = " + stream);
		} else {
			stream = portaudio.Pa_OpenDefaultStream(0, 1, portaudio.SIGNED_INTEGER_16, 44100.0, 256, new Callback());
			System.out.println("Java::stream (default) = " + stream);
		}

		return true;
		
	}

	@Override
	public boolean finit() {

		portaudio.Pa_StopStream(stream);
		portaudio.Pa_CloseStream(stream);
		portaudio.Pa_Terminate();
		
		return true;
		
	}

	@Override
	protected void eventHandlerRegistered(EventHandler evtHdl) {
		
		if (evtHdl.getEventType().equals(Constants.EVT_AUDIO)) {
			ear = (Sensor)evtHdl;
			ear.registerListener(this);
			period = Double.valueOf(ear.getParameter("PERIOD"))/1000.0;
			earMemory = getAgent().getKB().getMemory(ear.getName());
		}
		
	}

	@Override
	public void newSense(Sensor sourceSensor, double instant, double duration) {
		System.out.println(getAgent().getClock().getCurrentTime(TimeUnit.SECONDS) + " Recebi evento de " + instant + " até " + (instant+duration));
		double[] buf = (double[])earMemory.readMemory(instant, duration, TimeUnit.SECONDS);
		if (firstSound) {
			firstSound = false;
			System.out.println("Starting stream " + stream);
			portaudio.Pa_StartStream(stream);
		}
	}

	class Callback extends PaCallback {
		
		@Override
		public int callback(ByteBuffer input, ByteBuffer output,
				long frameCount, double inputBufferAdcTime,
				double currentTime, double outputBufferDacTime) {
			
			double now = getAgent().getClock().getCurrentTime(TimeUnit.SECONDS);
			
			System.out.printf("CALLBACK %f %f %f\n", now, currentTime, outputBufferDacTime);
			
			// If it's the first call, sets the startTime based in the mms's clock
			if (firstCall) {
				startTime = now-0.05;
				instant = startTime;
				firstCall = false;
//				System.out.println("First call = " + instant);
			}
			double duration = (double)(output.capacity()/4) * step;
			System.out.println(now + " vou ler de instant = " + instant + " até " + (instant+duration));
			double[] buf = (double[])earMemory.readMemory(instant, duration, TimeUnit.SECONDS);
//			if (buf[0] != 0.0 && firstSound) {
//				System.out.println(getAgent().getClock().getCurrentTime() + " callback() NAO É ZERO!!! instant = " + (instant));
//				firstSound = false;
//			}
			byte[] buffer = AudioTools.convertDoubleByte(buf, 0, buf.length);
//			System.out.println("buffer="+buffer.length + " - capacity=" + arg1.capacity());
			int ptr = 0;
			while (output.remaining() > 0) {
				for (int i = 0; i < maxChannels; i++) {
					// If it is the righ channel
					if (i == channel) {
						output.put(buffer[ptr++]);
						output.put(buffer[ptr++]);
					}
					// Else, silence
					else {
						output.put((byte)(0 & 0xFF));
						output.put((byte)((0 >> 8) & 0xFF));
					}
				}
			}
			instant = instant + duration;
			return paContinue;
			
		}
		
		@Override
		public void hook() {
		}
		
	};

	
}
