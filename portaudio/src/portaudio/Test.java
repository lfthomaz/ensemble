package portaudio;

import java.awt.image.SampleModel;
import java.nio.ByteBuffer;

public class Test {
	
	enum Running_Mode {LIST, DEFAULT, CONFIGURABLE};
	static Running_Mode mode = Running_Mode.DEFAULT;
	
	static int device = 10;
	static int channel = 0;
	static int channelCount = 1;
	static long stream = 0;
	
	static double freq = 440.0;
	static double fs = 44100.0;
	static double step = 1/fs;
	
	static double t = 0;

	public static void main(String[] args) {
		
		if (args.length == 1 && args[0].equals("list")) {
			mode = Running_Mode.LIST;
		}
		else if (args.length == 2) {
			mode = Running_Mode.CONFIGURABLE;
			device = Integer.valueOf(args[0]);
			channel = Integer.valueOf(args[1]);
		}
		
		System.out.println("Starting portaudio...");
		int err = portaudio.Pa_Initialize();
		
		PaCallback cb = new PaCallback() {
			
			@Override
			public int callback(long stream, ByteBuffer input, ByteBuffer output,
					long frameCount, double inputBufferAdcTime,
					double currentTime, double outputBufferDacTime) {

				System.out.println("Java::callback() - t = " + t + " - " + frameCount + " - " + outputBufferDacTime);
				while (output.remaining() > 0) {
					for (int i = 0; i < channelCount; i++) {
						// If it is the chosen channel
						if (i == channel) {
							double dSample = 0.5 * Math.sin(2 * Math.PI * freq * t);
							int nSample = (int) Math.round(dSample * 32767.0); // scaling and conversion to integer
							output.put((byte)(nSample & 0xFF));
							output.put((byte)((nSample >> 8) & 0xFF));
						}
						// Else, silence
						else {
							output.put((byte)(0 & 0xFF));
							output.put((byte)((0 >> 8) & 0xFF));
						}
					}
					t = t + step;
				}
				return paContinue;
				
			}
			
			@Override
			public void hook(long stream) {
				System.out.println("Java::hook() + " + stream);
			}		
		};
		
		switch (mode) {
		case LIST:
			System.out.println("devices = " + portaudio.Pa_GetDeviceCount());
			for (int i = 0; i < portaudio.Pa_GetDeviceCount(); i++) {
				PaDeviceInfo info = portaudio.Pa_GetDeviceInfo(i);
				System.out.printf("[%d] %s (%d,%d)\n", i, info.getName(), info.getMaxInputChannels(), info.getMaxOutputChannels());
			}
			System.out.println("Terminating portaudio...");
			err = portaudio.Pa_Terminate();
			System.exit(0);
			break;
		case DEFAULT:
			System.out.println("Opening default stream...");
			stream = portaudio.Pa_OpenDefaultStream( 
												0, 
												1, 
												portaudio.SIGNED_INTEGER_16, 
												44100, 
												256, 
												cb);
			System.out.println("Java::stream = " + stream);
			System.out.println("Opening stream...");
			break;
		case CONFIGURABLE:
			// Gets DeviceInfo
			PaDeviceInfo info = portaudio.Pa_GetDeviceInfo(device);
			System.out.println("channels = " + info.getMaxOutputChannels());
			channelCount = info.getMaxOutputChannels();
			// Sets Parameters
			PaStreamParameters outputParameters = new PaStreamParameters();
			outputParameters.setChannelCount(channelCount);
			outputParameters.setDevice(device);
			outputParameters.setHostApiSpecificStreamInfo(null);
			outputParameters.setSampleFormat(portaudio.SIGNED_INTEGER_16);
			outputParameters.setSuggestedLatency(info.getDefaultLowOutputLatency());
			// Opens the stream
			stream = portaudio.Pa_OpenStream(null, outputParameters, 44100.0, 256, 0, cb);
			System.out.println("Java::stream = " + stream);
			break;
		}

		System.out.println("Starting stream...");
		err = portaudio.Pa_StartStream(stream);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Stoping stream...");
		err = portaudio.Pa_StopStream(stream);

		System.out.println("Closing stream...");
		err = portaudio.Pa_CloseStream(stream);
		
		System.out.println("Terminating portaudio...");
		err = portaudio.Pa_Terminate();

	}

}
