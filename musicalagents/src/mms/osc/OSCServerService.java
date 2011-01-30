package mms.osc;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

import mms.MusicalAgent;
import jade.core.Agent;
import jade.core.BaseService;
import jade.core.ServiceException;
import jade.util.Logger;

public class OSCServerService extends BaseService {

	//----------------------------------------------------------
	// Log
	private static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	//----------------------------------------------------------
	// Nome do serviço
	public static final String NAME = "OSCServer";

	//----------------------------------------------------------
	// OSC
	private OSCPortOut 	sender;
	private OSCPortIn 	receiver;
	
	@Override
	public String getName() {
		
		return NAME;
		
	}

	public void boot(jade.core.Profile p) throws ServiceException {
		super.boot(p);
		
		// Starts the OSC Server
		try {
			
			Listener listener = new Listener();
			receiver = new OSCPortIn(6666);
			receiver.addListener("/mms/.*", listener);
//			receiver.addListener("/mms/movement", listener);
//			receiver.addListener("/mms/Bassist/foot", listener);
//			receiver.addListener("/mms/Drummer/foot", listener);
//			receiver.addListener("/mms/Player/foot", listener);
			receiver.startListening();
			
			boolean b1 = Pattern.matches("/mms/.*", "/mms/agent1");
			
			String key = "/mms/agent1/foot";
			boolean b2 = key.matches("/mms/.*");
			
			sender = new OSCPortOut();
			
			
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
	}
	
	public class Listener implements OSCListener {

		@Override
		public void acceptMessage(Date arg0, OSCMessage arg1) {
			
			// Obtém o agente e componente destino
			String[] address = arg1.getAddress().split("/");
			if (address.length <= 1 || !address[1].equals("mms")) {
	    		MusicalAgent.logger.info("[OSCService] " + "Malformed address: " + arg1.getAddress());
				return;
			}
			String agentName = address[2];
			String compName = address[3];
			
			// Obtém os argumentos
			Object[] arguments = arg1.getArguments();
			System.out.printf("%s:%s - ", agentName, compName);
			for (int i = 0; i < arguments.length; i++) {
				System.out.print("(" + arguments[i].getClass() + ")" + arguments[i].toString() + " ");
			}
			System.out.println();
			
			
			
		}
		
	}
	
	public OSCServerHelper getHelper(Agent a) {
		
		return new OSCServerHelperImp();
	
	}
	
	public class OSCServerHelperImp implements OSCServerHelper {

		@Override
		public void init(Agent arg0) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void send(Object[] args) {
			OSCMessage msg = new OSCMessage("/pd");
			for (int i = 0; i < args.length; i++) {
				msg.addArgument(args[i]);
			}
			try {
				sender.send(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void registerListener(OSCListener lst, String address) {
//			receiver.stopListening();
			receiver.addListener(address, lst);
//			receiver.startListening();
		}

	}

}
