package mms.audio;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

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
	HashMap<Long, StreamInfo> 	streamInfos = new HashMap<Long, StreamInfo>(2);
	HashMap<Long, String> 		streams_sensors = new HashMap<Long, String>(2);
	HashMap<String, Long> 		sensors_streams = new HashMap<String, Long>(2);
	double 						callbackStartTime, period;
	double 						step = 1/44100.0;
	
	// Parameters
	HashMap<String,Integer> devices = new HashMap<String, Integer>();
	HashMap<String,Integer> channels = new HashMap<String, Integer>();
	
	// Sensor
	HashMap<String,Memory> 	earMemories = new HashMap<String, Memory>(2);

	@Override
	public boolean init() {
		
		String[] str = getParameter("channel", "").split(";");
		for (int i = 0; i < str.length; i++) {
			String[] str2 = str[i].split(":");
			String[] str3 = str2[1].split(",");
			devices.put(str2[0], Integer.valueOf(str3[0]));
			channels.put(str2[0], Integer.valueOf(str3[1]));
		}
		
		// Initializes PortAudio
		System.out.println("Starting portaudio...");
		int err = portaudio.Pa_Initialize();

		return true;
		
	}

	@Override
	public boolean finit() {

		portaudio.Pa_Terminate();
		
		return true;
		
	}

	@Override
	protected void eventHandlerRegistered(EventHandler evtHdl) {
		
		if (evtHdl instanceof Sensor && evtHdl.getEventType().equals(Constants.EVT_AUDIO)) {
			Sensor ear = (Sensor)evtHdl;
			String sensorName = evtHdl.getName();
			ear.registerListener(this);
			period = Double.valueOf(ear.getParameter("PERIOD"))/1000.0;
			earMemories.put(sensorName, getAgent().getKB().getMemory(ear.getName()));
			// Creates a portaudio stream
			long stream = 0;
			if (devices.containsKey(sensorName)) {
				System.out.println("Opening stream...");
				// Gets DeviceInfo
				int device = devices.get(sensorName);
				PaDeviceInfo info = portaudio.Pa_GetDeviceInfo(device);
				double sr = info.getDefaultSampleRate();
				int channelCount = info.getMaxOutputChannels();
				// Sets Parameters
				PaStreamParameters outputParameters = new PaStreamParameters();
				outputParameters.setDevice(device);
				outputParameters.setChannelCount(channelCount);
				outputParameters.setHostApiSpecificStreamInfo(null);
				outputParameters.setSampleFormat(portaudio.SIGNED_INTEGER_16);
				outputParameters.setSuggestedLatency(info.getDefaultLowOutputLatency());
				// Opens the stream
				stream = portaudio.Pa_OpenStream(null, outputParameters, sr, 256, 0, new Callback());
				System.out.println(sensorName + " stream = " + stream);
				// Stores Stream parameters
				StreamInfo streamInfo = new StreamInfo();
				streamInfo.stream = stream;
				streamInfo.evtHdlName = sensorName;
				streamInfo.device = device;
				streamInfo.channel = channels.get(sensorName);
				streamInfo.channelCount = channelCount;
				streamInfo.latency = portaudio.Pa_GetStreamInfo(stream).getOutputLatency();
				streamInfos.put(stream, streamInfo);
//			} else {
//				devices.put(evtHdl.getName(), -1);
//				channels.put(evtHdl.getName(), 0);
//				stream = portaudio.Pa_OpenDefaultStream(0, 1, portaudio.SIGNED_INTEGER_16, 44100.0, 256, new Callback());
//				streams_sensors.put(stream, sensorName);
//				sensors_streams.put(sensorName, stream);
//				System.out.println(sensorName + " stream = " + stream);
			}
			if (stream != 0) {
				streams_sensors.put(stream, sensorName);
				sensors_streams.put(sensorName, stream);
				System.out.println("Starting stream " + stream);
				portaudio.Pa_StartStream(stream);
			}
		}
		
	}

	@Override
	protected void eventHandlerDeregistered(EventHandler evtHdl)
			throws Exception {
		String sensorName = evtHdl.getName();
		if (sensors_streams.containsKey(sensorName)) {
			long stream = sensors_streams.get(sensorName);
			System.out.println("Stoping stream " + stream);
			portaudio.Pa_StopStream(stream);
			System.out.println("Closing stream " + stream);
			portaudio.Pa_CloseStream(stream);
			streams_sensors.remove(stream);
			sensors_streams.remove(sensorName);
			streamInfos.remove(sensorName);
		}
	}
	
	@Override
	public void newSense(Sensor sourceSensor, double instant, double duration) {

//		System.out.println(System.currentTimeMillis() + " Recebi evento de " + instant + " até " + (instant+duration));

	}

	class StreamInfo {
		long 	stream;
		String 	evtHdlName;
		int 	device;
		int 	channel;
		int 	channelCount;
		double 	latency;
		boolean firstCall = true;
		double 	instant = 0.0;
	}
	
	class Callback extends PaCallback {
		
		@Override
		public int callback(long stream, ByteBuffer input, ByteBuffer output,
				long frameCount, double inputBufferAdcTime,
				double currentTime, double outputBufferDacTime) {

			StreamInfo info = streamInfos.get(stream);
			
			// If it's the first call, sets the startTime based in the mms's clock
			if (info.firstCall) {
				info.instant = getAgent().getClock().getCurrentTime(TimeUnit.SECONDS) - info.latency;
				info.firstCall = false;
			}

//			System.out.printf(System.currentTimeMillis() + " callback = %f %f %f\n", info.instant, outputBufferDacTime, portaudio.Pa_GetStreamTime(stream));
			
			double duration = (double)(frameCount) * step;
//			System.out.println("vou ler de instant = " + instant + " até " + (instant+duration));
			Memory earMemory = earMemories.get(info.evtHdlName);
			double[] buf = (double[])earMemory.readMemory(info.instant, duration, TimeUnit.SECONDS);
			byte[] buffer = AudioTools.convertDoubleByte(buf, 0, buf.length);
			int ptr = 0;
			while (output.remaining() > 0) {
				for (int i = 0; i < info.channelCount; i++) {
					// If it is the chosen channel
					if (i == info.channel) {
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
			info.instant = info.instant + duration;
			return paContinue;
			
		}
		
		@Override
		public void hook(long stream) {
		}
		
	};

	
}
