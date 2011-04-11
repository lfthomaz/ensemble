package mms.router;

import mms.Command;

/**
 *
 * @author lfthomaz
 */
public interface CommandClientInterface {

    public String getAddress();

    public void processCommand(String recipient, Command cmd);
    
    public void receiveCommand(String recipient, Command cmd);

    public void sendCommand(String recipient, Command cmd);
    

}
