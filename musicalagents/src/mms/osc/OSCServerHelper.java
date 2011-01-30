package mms.osc;

import jade.core.ServiceHelper;

import java.util.HashMap;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;

public interface OSCServerHelper extends ServiceHelper {

	public void send(Object[] args);
	
	public void registerListener(OSCListener lst, String address);
	
}
