package mms.router;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;

import de.sciss.net.OSCChannel;
import de.sciss.net.OSCClient;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import de.sciss.net.OSCPacketCodec;
import de.sciss.net.OSCServer;

//import com.illposed.osc.OSCListener;
//import com.illposed.osc.OSCMessage;
//import com.illposed.osc.OSCPortIn;
//import com.illposed.osc.OSCPortOut;

import mms.Command;
import mms.Constants;
import mms.MusicalAgent;

import jade.core.Agent;
import jade.core.BaseService;
import jade.core.ServiceException;
import jade.util.Logger;

public class RouterService extends BaseService {

	//----------------------------------------------------------
	// Log
	private static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	//----------------------------------------------------------
	// Nome do serviço
	public static final String NAME = "Router";
	
	//----------------------------------------------------------
	//	
    private static HashMap<String, CommandClientInterface> list = new HashMap<String, CommandClientInterface>();
        
    //----------------------------------------------------------
	// OSC
    private int 		oscPort 	= 57111;
//	private OSCPortOut 	oscSender;
//	private OSCPortIn 	oscReceiver;
	
	private OSCClient 	oscClient;
	private OSCServer 	oscServer;
    

    @Override
	public String getName() {
		
		return NAME;
		
	}

	public void boot(jade.core.Profile p) throws ServiceException {

		super.boot(p);
		
		// Starts the Router Server
		// TODO Pegar parâmetros (porta OSC, etc...)
				
		// Starts the OSC Server
//		try {
//			oscReceiver = new OSCPortIn(oscPort);
////			oscReceiver.addListener("/mms/Listener_1", new Listener());
//			oscReceiver.addListener("/mms", new Listener());
//			oscReceiver.startListening();
//			oscSender = new OSCPortOut();
//		} catch (SocketException e) {
//			e.printStackTrace();
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		} finally {
//	        System.out.println("[" + getName() + "] OSC service started");
//		}
		try {
			OSCPacketCodec codec = new OSCPacketCodec(OSCPacketCodec.MODE_FAT_V1);
			oscClient = OSCClient.newUsing(codec, OSCChannel.UDP);
			oscClient.setTarget(new InetSocketAddress(InetAddress.getLocalHost(), 57110));
			oscClient.start();
			
			oscServer = OSCServer.newUsing(OSCChannel.UDP, 57111);
			oscServer.addOSCListener(new Listener());
			oscServer.start();
		}
		catch( IOException e1 ) {
			e1.printStackTrace();
			return;
		}
		
		// TODO Deve inicializar aqui os consoles externos
//        Console console = new Console();
//		console.router = getHelper(null).connect(console);
//        new Thread(console).start();
        
		// Console GUI
		ConsoleGUI console = new ConsoleGUI();
		console.router = getHelper(null).connect(console);
		console.setVisible(true);

        System.out.println("[" + getName() + "] Router service started");
		
	}
	
	@Override
	public void shutdown() {
		try {
			oscClient.stop();
			oscClient.dispose();
			oscServer.stop();
			oscServer.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public RouterHelper getHelper(Agent a) {
		
		return new RouterHelperImp();
	
	}
	
	//--------------------------------------------------------------------------------
	// OSC Control 
	//--------------------------------------------------------------------------------
	
	private void sendOSCMessage(String recipient, Command cmd) {
		if (cmd.getCommand().equals("OSC")) {
			String[] str = cmd.getParameter("CONTENT").split(" ");
			Object[] obj = new Object[str.length];
			for (int i = 0; i < str.length; i++) {
				try {
					obj[i] = Float.parseFloat(str[i]);
				} catch (Exception e) {
					obj[i] = str[i];
				}
			}
			OSCMessage msg = new OSCMessage(recipient, obj);
			try {
				oscClient.send(msg);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public class Listener implements OSCListener {

		@Override
		public void messageReceived(OSCMessage m, SocketAddress addr, long time) {
			// Obtém o agente e componente destino
			String[] address = m.getName().split("/");
			if (address.length <= 1 || !address[1].equals(Constants.FRAMEWORK_NAME)) {
	    		MusicalAgent.logger.info("[OSCService] " + "Malformed address: " + m.getName());
				return;
			}
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < m.getArgCount(); i++) {
				sb.append(m.getArg(i));
				sb.append(" ");
			}
			Command cmd = Command.parse(sb.toString());
			getHelper(null).sendCommand(m.getName(), cmd);
		}
		
	}
	
	//--------------------------------------------------------------------------------
	// Service Helper 
	//--------------------------------------------------------------------------------

	public class RouterHelperImp implements RouterHelper {

		@Override
		public void init(Agent arg0) {
		}

		@Override
		public RouterHelper connect(CommandClientInterface cmdInterface) {
	       
			if (list.containsKey(cmdInterface.getAddress())) {
				
	            System.err.println("[" + getName() + "] Client already exists!");
	            return null;
	            
	        } else {
	        	
	            // Keeps client information
	            list.put(cmdInterface.getAddress(), cmdInterface);
	            System.out.println("[" + getName() + "] Command interface " + cmdInterface.getAddress() + " connected.");
	            return this;
	            
	        }
			
		}

		@Override
		public boolean disconnect(CommandClientInterface cmdInterface) {
			
	        list.remove(cmdInterface.getAddress());
	        System.out.println("[" + getName() + "] Command interface " + cmdInterface.getAddress() + " disconnected.");
	        return true;
	        
		}

		@Override
		public void sendCommand(String recipient, Command cmd) {
			
	        // Verifies the destination of the command
	        String[] str = recipient.split("/");
	        if (str.length < 2) {
	            System.err.println("[" + getName() + "] Malformed address: " + recipient);
	            return;
	        }
	        
	        if (str[1].equals(Constants.FRAMEWORK_NAME)) {
	        	
//	            System.out.println("[Router] Command to MMS received: " + cmd);
		        // Sends the command
	            if (list.containsKey(recipient)) {
	            	CommandClientInterface recipientInterface = list.get(recipient);
	            	// TODO Transformar em uma Thread! Pode dar problema
//	            	Command cmd = Command.parse(sender, recipient, msg);
            		recipientInterface.receiveCommand(recipient, cmd);
	            } else {
	            	System.out.println("[" + getName() + "] Address " + recipient + " is not registered.");
	            }
	            
	        } else if (str[1].equals("console")) {
	        	
//	            System.out.println("[Router] Command to CONSOLE received: " + cmd);
	            if (list.containsKey(recipient)) {
	            	CommandClientInterface recipientInterface = list.get(recipient);
	            	if (recipientInterface != null) {
	            		recipientInterface.receiveCommand(recipient, cmd);
	            	}
	            }
	            	            
	        } else if (str[1].equals("file")) {
	        	
//	            System.out.println("[Router] Command to FILE received: " + cmd);
	            
	        } 
	        // Se não for local, envia via OSC
	        else {
	        	
//	            System.out.println("[Router] Command to OSC received: " + cmd);
	            sendOSCMessage(recipient, cmd);
	            
	        }
		}

	}

}
