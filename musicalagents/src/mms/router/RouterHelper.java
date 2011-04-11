package mms.router;

import mms.Command;
import jade.core.ServiceHelper;

public interface RouterHelper extends ServiceHelper {

	public void sendCommand(String recipient, Command cmd);
	
	public RouterHelper connect(CommandClientInterface cmdInterface);
	
	public boolean disconnect(CommandClientInterface cmdInterface);
	
}
