package mmsjack.tests;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;

import mmsjack.JACKCallback;
import mmsjack.JackPortFlags;
import mmsjack.SWIGTYPE_p_jack_client_t;
import mmsjack.SWIGTYPE_p_jack_port_t;
import mmsjack.mmsjack;
import mmsjack.mmsjackConstants;


public class JACKServerService {

	//----------------------------------------------------------
	// Service name
	public static final String NAME = "JACKServer";

	private HashMap<String, JACKInfo> infos = new HashMap<String, JACKInfo>();
	
	public String getName() {
		
		return NAME;
		
	}

	public void boot() {
		
		System.out.println("[" + getName() + "] Starting JACK server...");
		
		try {
			System.loadLibrary("mmsjack");
		} catch (UnsatisfiedLinkError  e) {
			e.printStackTrace();
			System.err.println("[" + getName() + "] mmsjack library not found... JACK will not be available!");
			return;
		}

		System.out.println("[" + getName() + "] JACK server started");
		
	}
	
	public void shutdown() {

		if (infos.size() > 0) {
			// Unregisters all open clients
			for (JACKInfo info : infos.values()) {
				mmsjack.jack_client_close(info.client);
			}
		}
	}
		
	public JACKServerHelper getHelper() {
		
		return new JACKServerHelperImp();
	
	}
	
	public class JACKServerHelperImp implements JACKServerHelper {

		public String agentName;
		
		@Override
		public boolean registerOutputPort(String component, String portName, String connectPort, JACKCallback cb) {

			JACKInfo info = new JACKInfo();
			
			// Starts the JACK client
			// TODO Guardar a informação do cliente em um HashMap, com o agente + componente de onde originou
			info.client_name = "mms_" + agentName+ "_" + component;
			info.client = mmsjack.jack_client_open(info.client_name, cb);
			if (info.client == null) {
				System.err.println("[" + getName() + "] JACK server not running... JACK will not be available!");
	            return false;
			}
			// Activates the JACK client
			if (mmsjack.jack_activate(info.client) != 0) {
				System.err.println("[" + getName() + "] Cannot activate JACK client... JACK will not be available!");
	            return false;
			}
			// Registers the port
			info.port_name = info.client_name + ":" + portName;
			info.port = mmsjack.jack_port_register(info.client, 
													portName, 
													mmsjackConstants.JACK_DEFAULT_AUDIO_TYPE, 
													JackPortFlags.JackPortIsOutput);
			// Searches the desired playback port
			String[] playback_ports = mmsjack.jack_get_ports(info.client, connectPort, null,JackPortFlags.JackPortIsPhysical|JackPortFlags.JackPortIsInput);
			if (playback_ports == null) {
				System.err.println("[" + getName() + "] Cannot find any physical playback ports");
				unregisterPort(component, portName);
				return false;
			}
			if (playback_ports.length > 1) {
				System.err.println("[" + getName() + "] More than one port with that name");
				unregisterPort(component, portName);
				return false;
			}
			
			// Connects the port
			info.connectPort = connectPort;
			if (mmsjack.jack_connect(info.client, info.port_name, playback_ports[0]) != 0) {
				System.err.println("[" + getName() + "] Cannot connect playback ports");
				unregisterPort(component, portName);
				return false;
			}
			
			return true;
			
		}
		
		@Override
		public boolean registerInputPort(String component, String portName, String connectPort, JACKCallback cb) {
//			mmsjack.jack_port_register(client, portName, mmsjackConstants.JACK_DEFAULT_AUDIO_TYPE, JackPortFlags.JackPortIsInput);
			return true;
		}	

		@Override
		public boolean unregisterPort(String component, String portName) {
			String client_name = "mms_" + agentName + "_" + component; 
			if (infos.containsKey(client_name)) {
				JACKInfo info = infos.get(client_name);
				mmsjack.jack_port_unregister(info.client, info.port);
			}
			return true;
		}

	}
	
	class JACKInfo {
		
		String client_name;
		SWIGTYPE_p_jack_client_t client;
		String port_name;
		SWIGTYPE_p_jack_port_t port;
		String connectPort;
		
		public String toString() {
			
			return "JACKInfo: " + client_name + " " + client + " " + port_name + " " + port + " " + connectPort;
		}
		
	}
	
	public static void main(String[] args) {
		
		JACKServerService jack = new JACKServerService();
		
		jack.boot();

		JACKServerHelperImp helper = (JACKServerHelperImp)jack.getHelper();
		helper.agentName = "Listener";
		
		helper.registerOutputPort("AudioReasoning", "ear_left", "system:playback_2", new ServerCallback());
		try {
			Thread.sleep(1000);
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
			double dSample = 0.3 * Math.sin(2 * Math.PI * freq * t);
			fOut.put((float)dSample);
			t = t + step;
		}
		return 0;
	}
	
}

