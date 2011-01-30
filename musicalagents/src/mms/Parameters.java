package mms;

import java.util.Enumeration;
import java.util.Hashtable;

public class Parameters extends Hashtable<String, String> {

	public synchronized void merge(Parameters param) {
		Enumeration<String> keys = param.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			String value = param.get(key);
			this.put(key, value);
		}
	}
	
	public synchronized String get(String key, String def) {
		if (super.containsKey(key)) {
			return super.get(key);
		} else {
			return def;
		}
	}
	
	public String toString() {
		
		String str = "";
		
		if (this.size() > 0) { 
			Enumeration<String> key = this.keys();
			while (key.hasMoreElements()) {
				String element = (String) key.nextElement();
				str = str + element + "=" + this.get(element) + ";";
			}
			str = "{" + str.substring(0, str.length()-1) + "}"; 
		} else {
			str = "{ }";
		}
		
		
		return str;
		
	}
	
}
