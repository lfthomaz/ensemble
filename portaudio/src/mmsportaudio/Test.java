package portaudio;

import java.nio.ByteBuffer;

public class Test {
	
	final static int FLOAT_32 = 0x00000001;
	final static int SIGNED_INTEGER_8 = 0x00000010;
	final static int UNSIGNED_INTEGER_8 = 0x00000020;
	final static int SIGNED_INTEGER_16 = 0x00000008;
	final static int SIGNED_INTEGER_24 = 0x00000004;
	final static int SIGNED_INTEGER_32 = 0x00000002;

	static {
		// TODO Vai ter que verificar aqui qual sistema est‡ rodando
		//		System.loadLibrary("mmsportaudio");
	}

	static double freq = 440.0;
	static double fs = 44100.0;
	static double step = 1/fs;
	
	static double t = 0;

	public static void main(String[] args) {
		
		System.out.println("Starting PortAudio...");
		int err = portaudio.Pa_Initialize();

		System.out.println("devices = " + portaudio.Pa_GetDeviceCount());
		for (int i = 0; i < portaudio.Pa_GetDeviceCount(); i++) {
			PaDeviceInfo info = portaudio.Pa_GetDeviceInfo(i);
			//System.out.println("maxInChannels = " + info.getMaxInputChannels());
			System.out.println("[" + i + "] " + info.getName());
		}

		PaCallback cb = new PaCallback() {
			
			@Override
			public int callback(ByteBuffer input, ByteBuffer output,
					long frameCount, double inputBufferAdcTime,
					double currentTime, double outputBufferDacTime) {

				System.out.println("Java::callback() - begin...");
				System.out.println("Java::callback() - " + frameCount + " - " + outputBufferDacTime);
				System.out.println("Java::callback() - t = " + t);
				System.out.println("Java::callback() - t = " + output.capacity());
				int i = 0;
				while (output.remaining() > 0) {
					double dSample = 0.5 * Math.sin(2 * Math.PI * freq * t);
					int nSample = (int) Math.round(dSample * 32767.0); // scaling and conversion to integer
					output.put((byte)(nSample & 0xFF));
					output.put((byte)((nSample >> 8) & 0xFF));
					output.put((byte)(0 & 0xFF));
					output.put((byte)((0 >> 8) & 0xFF));
					t = t + step;
					i++;
				}
				System.out.println("Java::callback() - i = " + i);
				return paContinue;
				
			}
			
			@Override
			public void hook() {
				System.out.println("Java::hook()");
			}
		};
		
//		System.out.println("Opening default stream...");
//		long stream = portaudio.Pa_OpenDefaultStream( 
//											0, 
//											1, 
//											SIGNED_INTEGER_16, 
//											44100, 
//											256, 
//											cb);
//		System.out.println("Java::stream = " + stream);

		System.out.println("Opening stream...");
		// Gets DeviceInfo
		PaDeviceInfo info = portaudio.Pa_GetDeviceInfo(2);
		// Sets Parameters
		PaStreamParameters outputParameters = new PaStreamParameters();
		outputParameters.setChannelCount(2);
		outputParameters.setDevice(2);
		outputParameters.setHostApiSpecificStreamInfo(null);
		outputParameters.setSampleFormat(SIGNED_INTEGER_16);
		outputParameters.setSuggestedLatency(info.getDefaultLowOutputLatency());
		// Opens the stream
		long stream = portaudio.Pa_OpenStream(null, outputParameters, 44100.0, 256, 0, cb);
		System.out.println("Java::stream = " + stream);

		System.out.println("Starting stream...");
		err = portaudio.Pa_StartStream(stream);

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Stoping stream...");
		err = portaudio.Pa_StopStream(stream);

		System.out.println("Closing stream...");
		err = portaudio.Pa_CloseStream(stream);
		
		System.out.println("Terminating PortAudio...");
		err = portaudio.Pa_Terminate();

	}

}
