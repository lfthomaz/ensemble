package mms;

import java.util.Set;


public class Command {

	private String 		source;
	private String 		recipient;
	private String 		command;
	private Parameters	parameters = new Parameters();
	private Parameters	userParameters = new Parameters();
	
	public Command(String command) {
		this.command = command;
	}
	
	public Command(String source, String recipient, String command) {
		this.source = source;
		this.recipient = recipient;
		this.command = command;
	}
	
	public String getSource() {
		return source;
	}
	public String getRecipient() {
		return recipient;
	}
	public String getCommand() {
		return command;
	}

	public void addParameter(String key, String value) {
		if (parameters == null) {
			parameters = new Parameters();	
		}
		parameters.put(key, value);
	}
	
	public void addParameters(Parameters hash) {
		if (hash != null) {
			Set<String> set = hash.keySet();
			for (String key : set) {
				parameters.put(key, hash.get(key));
			}
		}
	}

	public String getParameter(String key) {
		if (parameters != null) {
			String ret = parameters.get(key);
			if (ret == null || ret.equals("")) {
//				System.out.println("[Command] Parâmetro '" + key + "' inexistente");
			}
			return ret;
		} else {
			return null;
		}
	}
	
	public Parameters getParameters() {
		return parameters;
	}
	
	public void addUserParameter(String key, String value) {
		userParameters.put(key, value);				
	}
	
	public void addUserParameters(Parameters hash) {
		if (hash != null) {
			Set<String> set = hash.keySet();
			for (String key : set) {
				userParameters.put(key, hash.get(key));
			}
		}
	}
	
	public String getUserParameter(String key) {
		return userParameters.get(key);
	}
	
	public Parameters getUserParameters() {
		return userParameters;
	}
	
	public static Command parse(String source, String recipient, String str) {
		Command cmd = Command.parse(str);
		if (cmd != null) {
			cmd.source = source;
			cmd.recipient = recipient;
			return cmd;
		} else {
			return null;
		}
	}
	
	public static Command parse(String str) {
		
		String[] strSplited = str.split(" :");

		if (strSplited == null || strSplited.length < 1) {
			System.out.println("[ERROR] parse() - Malformed command: " + str);
			return null;
		}

		Command cmd = new Command(strSplited[0].trim());

		for (int i = 1; i < strSplited.length; i++) {
			String[] parameter = strSplited[i].split(" ");

			if (parameter.length != 2) {
				System.out.println("[ERROR] parse() - Malformed command: " + str);
				return null;
			}
			
			String key = parameter[0].trim();
			String value= parameter[1].trim();
			if (key.startsWith("user_")) {
				key = key.replaceFirst("user_", "");
				cmd.addUserParameter(key, value);
			} else {
				cmd.addParameter(key, value);
			}
			
		}
		
		return cmd;
		
	}
	
	public String toString() {
		
		if (command == null || command.trim().equals("")) {
			System.out.println("[ERROR] toString() - Malformed command!");
			System.exit(-1);
		}
		
		String ret = command;

		Set<String> set = parameters.keySet();
		for (String key : set) {
			String value = parameters.get(key);
			ret = ret + " :" + key + " " + value;
		}
		
		set = userParameters.keySet();
		for (String key : set) {
			String value = userParameters.get(key);
			ret = ret + " :user_" + key + " " + value;
		}
		
		return ret;
	}
	
//	public static void main(String[] args) {
//		
//		String str = "EVENT-REGISTER :compSendr Foot :compType ACTUATOR :compEvtType MOVEMENT :pos_x 2 :pos_y 4";
//		Command cmd = Command.parse(str);
//		if (cmd != null) {
//			System.out.println(cmd.toString());
//		}
//		
//	}
	
}
