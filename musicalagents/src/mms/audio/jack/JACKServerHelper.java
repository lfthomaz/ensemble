package mms.audio.jack;

import mmsjack.JACKCallback;
import jade.core.ServiceHelper;

public interface JACKServerHelper extends ServiceHelper {
	
	public boolean registerInputPort(String component, String portName, String connectPort, JACKCallback cb);
	
	public boolean registerOutputPort(String component, String portName, String connectPort, JACKCallback cb);

	public boolean unregisterPort(String component, String portName);

}
