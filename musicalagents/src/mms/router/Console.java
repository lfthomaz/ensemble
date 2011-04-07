package mms.router;

import java.util.Scanner;

import mms.Command;

/**
 *
 * @author lfthomaz
 */
public class Console implements Runnable, CommandClientInterface {

    private String myAddress = "CONSOLE";

    RouterHelper router = null;

//    public boolean connect(String name, CommandClientInterface cmdInterface) {
//        if (name.equals("ROUTER")) {
//            router = cmdInterface;
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    public boolean disconnect(String name) {
//        if (name.equals("ROUTER")) {
//            router = null;
//            return true;
//        } else {
//            return false;
//        }
//    }

	@Override
	public String getAddress() {
		return myAddress;
	}

    public void input(CommandClientInterface cmdInterface, Command cmd) {
        System.out.println("[CONSOLE] Command received: " + cmd);
    }

    public void run() {

       	Scanner sc = new Scanner(System.in);
        while(true) {
        	try {
    			Thread.sleep(500);
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		}
    		System.out.print("> ");
            String line = sc.nextLine().trim();
            // Verifica o endereço
            String[] str = line.split(" ");
            String address = str[0];
            // Se tivermos um endereço e algum comando após
            if (address.length() > 0 && str.length > 1) {
	            Command cmd = Command.parse(getAddress(), address, line.substring(address.length()+1));
	            if (cmd != null) {
	                if (cmd.getCommand().equals("EXIT")) {
	                    router.disconnect(this);
	                    break;
	                }
	                else {
	                    System.out.println("[CONSOLE] Sending to '" + address + "' command '" + cmd + "'");
	                    if (router != null) {
	                        router.sendCommand(this, cmd);
	                    }
	                }
	            }
            } else {
                System.out.println("[CONSOLE] Malformed address and/or command");
            }
        }

        sc.close();
    }

}
