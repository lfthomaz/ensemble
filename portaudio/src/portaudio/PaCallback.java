package portaudio;

import java.nio.ByteBuffer;

public abstract class PaCallback {

		final static int paContinue = 0;
		final static int paComplete = 1;
		final static int paAbort = 2;
	
//		public abstract int callback(ByteBuffer input, ByteBuffer output, long frameCount, PaStreamCallbackTimeInfo timeInfo);
		public abstract int callback(ByteBuffer input, 
										ByteBuffer output, 
										long frameCount, 
										double inputBufferAdcTime, 
										double currentTime, 
										double outputBufferDacTime);

		public abstract void hook();

}
