import java.nio.ByteBuffer;

public interface JACKCallback {

	public int process(ByteBuffer buffer, int nframes, double time); 
	
}
