package mmsjack.tests;

import mmsjack.JACKCallback;

public interface JACKServerHelper {
	
	public boolean registerInputPort(String component, String portName, String connectPort, JACKCallback cb);
	
	public boolean registerOutputPort(String component, String portName, String connectPort, JACKCallback cb);

	public boolean unregisterPort(String component, String name);

}
