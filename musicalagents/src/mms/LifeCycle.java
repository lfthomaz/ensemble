package mms;

public interface LifeCycle {

	/**
	 * User-implemented method that configures the component, setting up arguments and essential properties
	 * @param args
	 */
	public boolean configure(Parameters args);
	
	/**
	 * System-implemented initialization method
	 * @return 
	 */
	public boolean start();
	
	/**
	 * User-implemented initialization method, called by start()
	 */
	public boolean init();
	
//	public void process();
	
	/**
	 * User-implemented finalization method, called by end()
	 * @return 
	 */
	public boolean fini();
	
	/**
	 * System-implemented finalization method
	 * @return 
	 */
	public boolean end();
	
	public void setParameters(Parameters parameters);
	
	public Parameters getParameters();
	
}
