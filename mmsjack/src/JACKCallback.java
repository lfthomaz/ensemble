import java.nio.ByteBuffer;

public interface JACKCallback {

	public int process(String portName, ByteBuffer buffer, int nframes, double time); 
	
}
