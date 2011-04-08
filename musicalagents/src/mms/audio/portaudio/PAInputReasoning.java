package mms.audio.portaudio;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

import jade.util.Logger;

import portaudio.PaCallback;
import portaudio.PaDeviceInfo;
import portaudio.PaStreamInfo;
import portaudio.PaStreamParameters;
import portaudio.portaudio;

import mms.Actuator;
import mms.Constants;
import mms.EventHandler;
import mms.MusicalAgent;
import mms.Reasoning;
import mms.Sensor;
import mms.clock.TimeUnit;
import mms.memory.Memory;
import mms.memory.MemoryException;
import mms.tools.AudioTools;

public class PAInputReasoning extends Reasoning {

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
	HashMap<String,Memory> 	mouthMemories = new HashMap<String, Memory>(2);

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
		
		if (evtHdl instanceof Actuator && evtHdl.getEventType().equals(Constants.EVT_AUDIO)) {
			Actuator mouth = (Actuator)evtHdl;
			String actuatorName = evtHdl.getName();
			mouth.registerListener(this);
			period = Double.valueOf(mouth.getParameter("PERIOD"))/1000.0;
			mouthMemories.put(actuatorName, getAgent().getKB().getMemory(mouth.getName()));
			// Creates a portaudio stream
			long stream = 0;
			if (devices.containsKey(actuatorName)) {
				System.out.println("Opening stream...");
				// Gets DeviceInfo
				int device = devices.get(actuatorName);
				PaDeviceInfo info = portaudio.Pa_GetDeviceInfo(device);
				if (info == null) {
					System.err.println("[PORTAUDIO] Audio device '" + device + "' not available...");
					return;
				}
				double sr = info.getDefaultSampleRate();
				int channelCount = info.getMaxInputChannels();
				if (channelCount == 0) {
					System.err.println("[PORTAUDIO] There are no input channels in audio device '" + device + "'");
					return;
				}
				// Sets Parameters
				PaStreamParameters inputParameters = new PaStreamParameters();
				inputParameters.setDevice(device);
				inputParameters.setChannelCount(channelCount);
				inputParameters.setHostApiSpecificStreamInfo(null);
				inputParameters.setSampleFormat(portaudio.SIGNED_INTEGER_16);
				inputParameters.setSuggestedLatency(info.getDefaultLowInputLatency());
				// Opens the stream
				stream = portaudio.Pa_OpenStream(inputParameters, null, sr, 256, 0, new Callback());
				if (stream == 0) {
					System.err.println("[PORTAUDIO] Stream not available for audio device '" + device + "'");
					return;
				}
				// Stores Stream parameters
				StreamInfo streamInfo = new StreamInfo();
				streamInfo.stream = stream;
				streamInfo.evtHdlName = actuatorName;
				streamInfo.device = device;
				streamInfo.channel = channels.get(actuatorName);
				streamInfo.channelCount = channelCount;
				streamInfo.latency = portaudio.Pa_GetStreamInfo(stream).getInputLatency();
				streamInfos.put(stream, streamInfo);
				streams_sensors.put(stream, actuatorName);
				sensors_streams.put(actuatorName, stream);
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
	public void needAction(Actuator sourceActuator, double instant,
			double duration) throws Exception {
		// Teoricamente, já vai estar escrito na memória o que deve ser enviado,
		// pois foi preenchido pelo callback do PA
		sourceActuator.act();
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

//			System.out.printf(System.currentTimeMillis() + " callback = %f %f %f\n", instant, outputBufferDacTime, portaudio.Pa_GetStreamTime(stream));
			
			int ptr = 0;
			byte[] buffer = new byte[input.capacity()/info.channelCount];
			while (input.remaining() > 0) {
				for (int i = 0; i < info.channelCount; i++) {
					// Se foi o canal escolhido, guarda o sample
					if (i == info.channel) {
						buffer[ptr++] = input.get();
						buffer[ptr++] = input.get();
					}
					// Descarta os outros canais
					else {
						input.get();
						input.get();
					}
				}
			}

			double[] d_buf = AudioTools.convertByteDouble(buffer, 0, buffer.length);
			double duration = d_buf.length * step;
			try {
				Memory mouthMemory = mouthMemories.get(info.evtHdlName);
				double instant = info.instant + (2 * period) + info.latency;
				mouthMemory.writeMemory(d_buf, instant, duration, TimeUnit.SECONDS);
//				System.out.println(now + " " + getAgent().getClock().getCurrentTime() + " Escrevi do instante " + (instant+period) + " até " + (instant+period+duration));
			} catch (MemoryException e) {
				e.printStackTrace();
			}

			info.instant = info.instant + duration;
			return paContinue;
			
		}
		
		@Override
		public void hook(long stream) {
		}
		
	};

	
}
