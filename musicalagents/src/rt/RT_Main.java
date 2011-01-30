package rt;

import mms.Constants;
import mms.Parameters;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class RT_Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Cria o Container JADE e inicializa o Agente Ambiente
		Runtime rt = Runtime.instance();
		Profile p = new ProfileImpl();
		p.setParameter(Profile.MAIN_HOST, "localhost");
		p.setParameter(Profile.SERVICES, "mms.clock.VirtualClockService;mms.comm.direct.CommDirectService;mms.osc.OSCServerService");
		
		// Le o arquivo de configura��o e coloca os par�metros no Profile do Container
		p.setParameter(Constants.CLOCK_MODE, Constants.CLOCK_CPU);
		p.setParameter(Constants.PROCESS_MODE, Constants.MODE_REAL_TIME);
		p.setParameter(Constants.CLASS_ENTITY_STATE, "mms.world.EntityState3D");
		
		ContainerController cc = rt.createMainContainer(p);
		if (cc != null) {
			try {

				Parameters parameters;
				Object[] arguments;

				// TODO Devemos informar no momento da cria��o as classes e par�metros que ser�o usados no Agente Ambiente
				parameters = new Parameters();
				arguments = new Object[1];
				arguments[0] = parameters;
				AgentController ac = cc.createNewAgent("Environment", "rt.RT_EnvironmentAgent", arguments);
				ac.start();
				
				parameters = new Parameters();
				parameters.put("filename", "siren.wav");
				parameters.put("pos_x","-50.0");
				parameters.put("pos_y","5.0");
				parameters.put("vel_x","20.0");
				parameters.put("vel_y","0.0");
				arguments = new Object[1];
				arguments[0] = parameters;
				ac = cc.createNewAgent("Drummer", "rt.RT_MusicalAgent", arguments);
				ac.start();

//				parameters = new Hashtable<String, String>();
//				parameters.put("filename", "Guitar_44_16.wav");
//				parameters.put("pos_x","10.0");
//				parameters.put("pos_y","0.0");
//				parameters.put("vel_x","0.0");
//				parameters.put("vel_y","0.0");
//				arguments = new Object[1];
//				arguments[0] = parameters;
//				ac = cc.createNewAgent("Guitarist", "rt.RT_MusicalAgent", arguments);
//				ac.start();

//				parameters = new Hashtable<String, String>();
//				parameters.put("filename", "Bass_44_16.wav");
//				parameters.put("pos_x", "10.0");
//				parameters.put("pos_y", "10.0");r
//				parameters.put("vel_x","0.0");
//				parameters.put("vel_y","0.0");
//				arguments = new Object[1];
//				arguments[0] = parameters;
//				ac = cc.createNewAgent("Bassist", "rt.RT_MusicalAgent", arguments);
//				ac.start();
				
				parameters = new Parameters();
				parameters.put("pos_x","0.0");
				parameters.put("pos_y","0.0");
				parameters.put("vel_x","0.0");
				parameters.put("vel_y","0.0");
				arguments = new Object[1];
				arguments[0] = parameters;
				ac = cc.createNewAgent("Player", "rt.RT_PlayerMusicalAgent", arguments);
				ac.start();

			} catch (Exception e) {
				System.out.println("Error: " + e.toString());
			}
		}
		
	}

}
