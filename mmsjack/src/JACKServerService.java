//import jade.core.Agent;
//import jade.core.ServiceException;
//import jade.util.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;

import mmsjack.JackOptions;
import mmsjack.JackPortFlags;
import mmsjack.SWIGTYPE_p_jack_client_t;
import mmsjack.SWIGTYPE_p_jack_port_t;
import mmsjack.mmsjack;
import mmsjack.mmsjackConstants;

//import mms.MusicalAgent;

public class JACKServerService {

	static {
		
	}
	
	//----------------------------------------------------------
	// Log
//	private static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	//----------------------------------------------------------
	// Nome do serviço
	public static final String NAME = "JACKServer";

	//----------------------------------------------------------
	// JACK
	private SWIGTYPE_p_jack_client_t client;
	
	private HashMap<String, SWIGTYPE_p_jack_client_t> clients = new HashMap<String, SWIGTYPE_p_jack_client_t>();
	
//	@Override
	public String getName() {
		
		return NAME;
		
	}

//	public void boot(jade.core.Profile p) throws ServiceException {
	public void boot() {
//		super.boot(p);
		
		System.out.println("[" + getName() + "] Starting JACK server...");
		
		try {
			System.loadLibrary("mmsjack");
		} catch (UnsatisfiedLinkError  e) {
			// TODO: handle exception
			System.err.println("[" + getName() + "] mmsjack library not found... JACK will not be available!");
			return;
		}

		System.out.println("[" + getName() + "] JACK server started");
		
	}
	
	public void shutdown() {
		if (client != null) {
			mmsjack.jack_client_close(client);
		}
	}
		
	public JACKServerHelper getHelper(/*Agent a*/) {
		
		return new JACKServerHelperImp();
	
	}
	
	public class JACKServerHelperImp implements JACKServerHelper {

		@Override
		public int registerOutputPort(String component, String portName, String connectPort, JACKCallback cb) {
			// Starts the JACK client
			client = mmsjack.jack_client_open("mms_" + component, cb);
			if (client == null) {
				System.err.println("[" + getName() + "] JACK server not running... JACK will not be available!");
	            return -1;
			}
			// Activates the JACK client
			if (mmsjack.jack_activate(client) != 0) {
				System.err.println("[" + getName() + "] Cannot activate JACK client... JACK will not be available!");
	            return -1;
			}
			// Registers the port
			SWIGTYPE_p_jack_port_t port = mmsjack.jack_port_register(client, 
																	portName, 
																	mmsjackConstants.JACK_DEFAULT_AUDIO_TYPE, 
																	JackPortFlags.JackPortIsOutput);
			// Searches the desired playback port
			String[] playback_ports = mmsjack.jack_get_ports(client, null, null,JackPortFlags.JackPortIsPhysical|JackPortFlags.JackPortIsInput);
			if (playback_ports == null) {
				System.err.println("Cannot find any physical playback ports");
				unregisterPort(portName);
				return -1;
			}
			// Connects the port
			if (mmsjack.jack_connect(client, mmsjack.jack_port_name(port), playback_ports[0]) != 0) {
				System.err.println("cannot connect playback ports");
				unregisterPort(portName);
				return -1;
			}
			return 0;
		}
		
		@Override
		public int registerInputPort(String component, String portName, String connectPort, JACKCallback cb) {
//			mmsjack.jack_port_register(client, portName, mmsjackConstants.JACK_DEFAULT_AUDIO_TYPE, JackPortFlags.JackPortIsInput);
			return 0;
		}	

		@Override
		public int unregisterPort(String portName) {
			SWIGTYPE_p_jack_port_t port = mmsjack.jack_port_by_name(client, portName);
			mmsjack.jack_port_unregister(client, port);
			return 0;
		}

		@Override
		public String[] listInputPorts() {
			if (client != null) {
				return mmsjack.jack_get_ports(client, null, null,JackPortFlags.JackPortIsPhysical|JackPortFlags.JackPortIsOutput);
			} else {
				return null;
			}
		}

		@Override
		public String[] listOutputPorts() {
			if (client != null) {
				return mmsjack.jack_get_ports(client, null, null,JackPortFlags.JackPortIsPhysical|JackPortFlags.JackPortIsInput);
			} else {
				return null;
			}
		}

	}

	public static void main(String[] args) {
		JACKServerService jack = new JACKServerService();
		
		jack.boot();

		JACKServerHelperImp helper = (JACKServerHelperImp)jack.getHelper();
		
//		String[] inputs = helper.listInputPorts();
//		System.out.println("Input ports:");
//		for (int i = 0; i < inputs.length; i++) {
//			System.out.printf("[%d] %s\n", i, inputs[i]);
//		}
//		
//		String[] outputs = helper.listOutputPorts();
//		System.out.println("Output ports:");
//		for (int i = 0; i < outputs.length; i++) {
//			System.out.printf("[%d] %s\n", i, outputs[i]);
//		}

		helper.registerOutputPort("AudioReasoning", "ear_left", "system:playback_1", new ServerCallback());
		try {
			Thread.sleep(20000);
		} catch (Exception e) {
			System.err.println(e.toString());
		}
//		helper.unregisterPort("system:playback_1");
		
		jack.shutdown();
	}

}

class ServerCallback implements JACKCallback {

	double t = 0;
	double freq = 440.0;
	double fs = 44100.0;
	double step = 1/fs;

	@Override
	public int process(ByteBuffer buffer, int nframes, double time) {
		System.out.printf("Java::callback(%d)\n", nframes);
		FloatBuffer fOut = buffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
		while (fOut.remaining() > 0) {
			double dSample = 0.5 * Math.sin(2 * Math.PI * freq * t);
			fOut.put((float)dSample);
			t = t + step;
		}
		return 0;
	}
	
}
