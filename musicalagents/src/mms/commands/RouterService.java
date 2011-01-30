package mms.commands;

import java.util.HashMap;

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
	
    private static HashMap<String, CommandClientInterface> list = new HashMap<String, CommandClientInterface>();

    @Override
	public String getName() {
		
		return NAME;
		
	}

	public void boot(jade.core.Profile p) throws ServiceException {

		super.boot(p);
		
		// Starts the Router Server
		
		// TODO Deve inicializar aqui os consoles externos
        Console console = new Console();
		console.router = getHelper(null).connect(console);
        new Thread(console).start();
        
        // Console GUI
		
	}
	
	public RouterHelper getHelper(Agent a) {
		
		return new RouterHelperImp();
	
	}
	
	public class RouterHelperImp implements RouterHelper {

		@Override
		public void init(Agent arg0) {
		}

		@Override
		public RouterHelper connect(CommandClientInterface cmdInterface) {
	       
			if (list.containsKey(cmdInterface.getAddress())) {
				
	            System.err.println("[ROUTER] Client already exists!");
	            return null;
	            
	        } else {
	        	
	            // Keeps client information
	            list.put(cmdInterface.getAddress(), cmdInterface);
	            // Makes the returning connection
//	            cmdInterface.connect(getAddress(), this);
	            System.out.println("[ROUTER] Command interface " + cmdInterface.getAddress() + " connected.");
	            return this;
	            
	        }
			
		}

		@Override
		public boolean disconnect(CommandClientInterface cmdInterface) {
			
	        list.remove(cmdInterface.getAddress());
	        System.out.println("[ROUTER] Command interface " + cmdInterface.getAddress() + " disconnected.");
	        return true;
	        
		}

		@Override
		public void sendCommand(CommandClientInterface cmdInterface, Command cmd) {
			
	        // Verifies the destination of the command
	        String[] recipient = cmd.getRecipient().split(":");
	        if (recipient.length < 2) {
	            System.out.println("[ROUTER] Malformed address: " + cmd.getRecipient());
	            return;
	        }
	        
//	        if (recipient[0].equals("MMS")) {
//	        	
//	            System.out.println("[ROUTER] Command to MMS received: " + cmd);
//	            
//	        } else if (recipient[0].equals("CONSOLE")) {
//	        	
//	            System.out.println("[ROUTER] Command to CONSOLE eceived: " + cmd);
//	            
//	        } else if (recipient[0].equals("OSC")) {
//	        	
//	            System.out.println("[ROUTER] Command to OSC received: " + cmd);
//	            
//	        } else if (recipient[0].equals("FILE")) {
//	        	
//	            System.out.println("[ROUTER] Command to FILE received: " + cmd);
//	            
//	        } else {
//	        	
//	        	System.err.println("[ROUTER] Destination not recognized: " + recipient[0]);
//	        	return;
//	        	
//	        }
//	        
	        // Sends the command
	        String recipientType = recipient[0];
            String recipientName = recipient[1];
            String key = recipientType + ":" + recipientName;
            if (list.containsKey(key)) {
            	CommandClientInterface recipientInterface = list.get(key);
            	recipientInterface.input(recipientInterface, cmd);
            } else {
            	System.out.println("[ROUTER] Address " + key + " is not registered.");
            }

		}

	}

}
