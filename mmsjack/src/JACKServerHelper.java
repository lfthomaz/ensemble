
public interface JACKServerHelper {
	
	public String[] listInputPorts();

	public String[] listOutputPorts();

	public int registerInputPort(String component, String portName, String connectPort, JACKCallback cb);
	
	public int registerOutputPort(String component, String portName, String connectPort, JACKCallback cb);

	public int unregisterPort(String name);

}
