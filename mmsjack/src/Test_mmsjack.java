import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import mmsjack.*;

public class Test_mmsjack {

	public void init() {
		
	}
	public static void main(String[] args) {
		System.loadLibrary("mmsjack");
		
		SWIGTYPE_p_jack_client_t client = mmsjack.jack_client_new("mms/Musician_1/AudioReasoning");
		if (client == null) {
			System.err.println("jack server not running?\n");
            return;
		}

		SWIGTYPE_p_jack_client_t client2 = mmsjack.jack_client_new("Musician_2");
		if (client2 == null) {
			System.err.println("jack server not running?\n");
            return;
		}

		System.out.printf("engine sample rate: %d\n", mmsjack.jack_get_sample_rate(client));

//		SWIGTYPE_p_jack_port_t input_port = mmsjack.jack_port_register(client, "input", mmsjackConstants.JACK_DEFAULT_AUDIO_TYPE, JackPortFlags.JackPortIsInput, 0);
		Callback2 cb = new Callback2();
		SWIGTYPE_p_jack_port_t output_port = mmsjack.jack_port_register(client, "ear_right", mmsjackConstants.JACK_DEFAULT_AUDIO_TYPE, JackPortFlags.JackPortIsOutput, cb);
		Callback2 cb2 = new Callback2();
		SWIGTYPE_p_jack_port_t output_port_2 = mmsjack.jack_port_register(client, "ear_left", mmsjackConstants.JACK_DEFAULT_AUDIO_TYPE, JackPortFlags.JackPortIsOutput, cb2);
		Callback2 cb3 = new Callback2();
		SWIGTYPE_p_jack_port_t output_port_3 = mmsjack.jack_port_register(client2, "speaker", mmsjackConstants.JACK_DEFAULT_AUDIO_TYPE, JackPortFlags.JackPortIsOutput, cb3);
//		cb.output_port = output_port;
//		cb.input_port = input_port;

		int err = mmsjack.jack_activate(client);
		if (err != 0) {
            System.err.println("cannot activate client");
            return;
		}
		
		String[] playback_ports = mmsjack.jack_get_ports(client, null, null,JackPortFlags.JackPortIsPhysical|JackPortFlags.JackPortIsInput);
		if (playback_ports == null) {
			System.err.println("Cannot find any physical playback ports");
			System.exit(1);
		}
		for (int i = 0; i < playback_ports.length; i++) {
			System.out.println(playback_ports[i]);
		}

		err = mmsjack.jack_connect(client, mmsjack.jack_port_name(output_port), playback_ports[0]);
		if (err != 0) {
			System.err.println("cannot connect playback ports");
		}

//		String[] capture_ports = mmsjack.jack_get_ports(client, null, null, JackPortFlags.JackPortIsPhysical|JackPortFlags.JackPortIsOutput);
//		if (capture_ports == null) {
//			System.err.println("Cannot find any physical capture ports");
//			System.exit(1);
//		}
//		for (int i = 0; i < capture_ports; i++) {
//			System.out.println(capture_ports[i]);
//		}
//		
//		err = mmsjack.jack_connect(client, capture_ports[0], mmsjack.jack_port_name(input_port));
//		if (err != 0) {
//			System.err.println("cannot connect capture ports");
//		}

		try {
			Thread.sleep(10000);
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		
		mmsjack.jack_client_close(client);
		
	}
	
}

class Callback2 implements JACKCallback {

	double t = 0;
	double freq = 440.0;
	double fs = 44100.0;
	double step = 1/fs;

	@Override
	public int process(String portName, ByteBuffer buffer, int nframes, double time) {
//		System.out.printf("Java::callback(%d) - %s\n", nframes, portName);
		FloatBuffer fOut = buffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
		while (fOut.remaining() > 0) {
			double dSample = 0.5 * Math.sin(2 * Math.PI * freq * t);
			fOut.put((float)dSample);
			t = t + step;
		}
		return 0;
	}
	
}

class Callback extends JackCallback {
	
	double t = 0;
	double freq = 440.0;
	double fs = 44100.0;
	double step = 1/fs;

	public SWIGTYPE_p_jack_port_t output_port;
	public SWIGTYPE_p_jack_port_t input_port;
	
	@Override
	public int callback(int nframes) {
//		System.out.printf("Java::callback(%d)\n", nframes);
		
		long time = mmsjack.jack_get_time();
		System.out.printf("time = %d\n", time);
		
		ByteBuffer out = (ByteBuffer)mmsjack.jack_port_get_buffer(output_port, nframes);
		if (out == null) {
			System.out.println("FUDEU CAPITÌO!");
		}
		System.out.printf("out = %d\n", out.capacity());
		
		FloatBuffer fOut = out.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
		while (fOut.remaining() > 0) {
			double dSample = 0.5 * Math.sin(2 * Math.PI * freq * t);
			fOut.put((float)dSample);
			t = t + step;
		}
		
		return 0;
	}

}

