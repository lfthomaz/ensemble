package mmsjack.test;

import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import mmsjack.JACKCallback;
import mmsjack.SWIGTYPE_p_jack_client_t;
import mmsjack.mmsjack;

public class Test {

	private static SWIGTYPE_p_jack_client_t 	client;

	public static void main(String[] args) {
		client = mmsjack.jack_client_open("MyClient", new JACKCallback() {
			@Override
			public int process(int nframes, double time) {
				System.out.println("BLA!");
				return 0;
			}
		});
		if (client == null) {
			System.err.println("Error");
		}
		// Activates the JACK client
		if (mmsjack.jack_activate(client) != 0) {
			System.err.println("Error");
		}
	}
	
}
