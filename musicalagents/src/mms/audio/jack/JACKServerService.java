package mms.audio.jack;

import jade.core.Agent;
import jade.core.BaseService;
import jade.core.ServiceException;
import jade.util.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Iterator;

import mms.MusicalAgent;
import mmsjack.JACKCallback;
import mmsjack.JackOptions;
import mmsjack.JackPortFlags;
import mmsjack.SWIGTYPE_p_jack_client_t;
import mmsjack.SWIGTYPE_p_jack_port_t;
import mmsjack.mmsjack;
import mmsjack.mmsjackConstants;

public class JACKServerService extends BaseService {

	//----------------------------------------------------------
	// Log
	private static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	//----------------------------------------------------------
	// Service name
	public static final String NAME = "JACKServer";

	private HashMap<String, JACKInfo> infos = new HashMap<String, JACKInfo>();
	
	@Override
	public String getName() {
		
		return NAME;
		
	}

	@Override
	public void boot(jade.core.Profile p) throws ServiceException {
		
		super.boot(p);
		
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
	
	@Override
	public void shutdown() {

		if (infos.size() > 0) {
			// Unregisters all open clients
			for (JACKInfo info : infos.values()) {
				mmsjack.jack_client_close(info.client);
			}
		}
	}
		
	@Override
	public JACKServerHelper getHelper(Agent a) {
		
		return new JACKServerHelperImp();
	
	}
	
	public class JACKServerHelperImp implements JACKServerHelper {

		String agentName;
		
		@Override
		public void init(Agent arg0) {
			agentName = arg0.getLocalName();
		}

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

}