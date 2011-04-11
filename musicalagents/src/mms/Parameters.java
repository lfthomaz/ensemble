package mms;

import java.util.HashMap;
import java.util.Set;

public class Parameters extends HashMap<String, String> {

	public synchronized void merge(Parameters param) {
		Set<String> keys = param.keySet();
		for (String key: keys) {
			this.put(key, param.get(key));
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
			Set<String> keys = this.keySet();
			for (String key: keys) {
				str = str + key + "=" + this.get(key) + ";";
			}
			str = "{" + str.substring(0, str.length()-1) + "}"; 
		} else {
			str = "{ }";
		}
		
		
		return str;
		
	}
	
}
