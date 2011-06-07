package mms.movement;

import java.util.ArrayList;

import mms.Actuator;
import mms.Command;
import mms.EventHandler;
import mms.KnowledgeBase;
import mms.Reasoning;
import mms.Sensor;
import mms.clock.TimeUnit;
import mms.memory.Memory;
import mms.memory.MemoryException;
import mms.world.Vector;

/**
 * Given a set of waypoints and a time constraint to get there, this reasoning tries to walk
 * @author lfthomaz
 *
 */
public class MovementReasoning extends Reasoning {

//	private KnowledgeBase kb;
	
	private Actuator	legs;
	private Sensor 		eyes;
	
	private Memory 		legsMemory;
	private Memory 		eyesMemory;
	
	// Waypoints
	private ArrayList<Vector> 	waypoints = new ArrayList<Vector>();
	private ArrayList<Double> 	time_constrains = new ArrayList<Double>();
	private boolean 			loop = false;
	private int					active_waypoint = 0;;
	private double 				precision = 0.1;
	private double 				last_distance = 0.0;
	private double 				total_distance = 0.0;
	private Vector 				last_acc;
	private boolean 			inverted;
	
	// 
	private Vector 				actual_pos = null;
	private Vector 				actual_vel = null;
	private Vector 				actual_ori = null;
	
	// 
	private double MAX_ACELERATION = 10.0;
	
	public boolean init() {
		
//		kb = getAgent().getKB();
		
		String str = getParameter("waypoints", null);
		if (str != null) {
			String[] wps = str.split(":");
			for (int i = 0; i < wps.length; i++) {
				String[] wp = wps[i].split(" "); 
				waypoints.add(Vector.parse(wp[0]));
				time_constrains.add(Double.valueOf(wp[1]));
//				System.out.println("add wp " + waypoints.get(i) + " - time " + time_constrains.get(i));
			}
			loop = Boolean.parseBoolean(getParameter("loop", "false"));
//			System.out.println("loop = " + loop);
		}
		
		
		return true;
	}

	@Override
	protected void eventHandlerRegistered(EventHandler evtHdl) {
		if (evtHdl instanceof Actuator && evtHdl.getEventType().equals(MovementConstants.EVT_TYPE_MOVEMENT)) {
			legs = (Actuator)evtHdl;
			legs.registerListener(this);
			legsMemory = getAgent().getKB().getMemory(legs.getComponentName());
		}
		else if (evtHdl instanceof Sensor && evtHdl.getEventType().equals(MovementConstants.EVT_TYPE_MOVEMENT)) {
			eyes = (Sensor)evtHdl;
			eyes.registerListener(this);
			eyesMemory = getAgent().getKB().getMemory(eyes.getComponentName());
		}
	}

	@Override
	public void newSense(Sensor sourceSensor, double instant, double duration) {
		
		String str = (String)eyesMemory.readMemory(instant, TimeUnit.SECONDS);
		Command cmd = Command.parse(str);
		if (cmd != null) {
			actual_pos = Vector.parse(cmd.getParameter(MovementConstants.PARAM_POS));
			actual_vel = Vector.parse(cmd.getParameter(MovementConstants.PARAM_VEL));
			actual_ori = Vector.parse(cmd.getParameter(MovementConstants.PARAM_ORI));
		}
//		System.out.println(getAgent().getAgentName() + " - new position " + actual_pos + " velocity " + actual_vel);

	}
	
	@Override
	public void processCommand(Command cmd) {
		
		if (cmd.getCommand().equals(MovementConstants.CMD_WALK)) {
			if (cmd.containsParameter(MovementConstants.PARAM_POS) && cmd.containsParameter(MovementConstants.PARAM_TIME)) {
				System.out.println("[" + getAgent().getAgentName() + "] Walking...");
				waypoints.clear();
				time_constrains.clear();
				active_waypoint = 0;
				last_distance = 0;
				loop = false;
				time_constrains.add(Double.valueOf(cmd.getParameter(MovementConstants.PARAM_TIME)));
				waypoints.add(Vector.parse(cmd.getParameter(MovementConstants.PARAM_POS)));
			}
		}
		else if (cmd.getCommand().equals(MovementConstants.CMD_STOP)) {
			System.out.println("[" + getAgent().getAgentName() + "] Stoping...");
			waypoints.clear();
			time_constrains.clear();
			active_waypoint = 0;
			sendStopCommand();
		}
		
	}
	
	@Override
	public void process() {

		if (actual_pos == null) {
			String str = (String)eyesMemory.readMemory(eyesMemory.getLastInstant(), TimeUnit.SECONDS);
			Command cmd = Command.parse(str);
			if (cmd != null) {
				actual_pos = Vector.parse(cmd.getParameter(MovementConstants.PARAM_POS));
				actual_vel = Vector.parse(cmd.getParameter(MovementConstants.PARAM_VEL));
				actual_ori = Vector.parse(cmd.getParameter(MovementConstants.PARAM_ORI));
			}
		}

		if (legsMemory != null && actual_pos != null && waypoints.size() != 0) {
			// Tenho destino?
			if (active_waypoint < waypoints.size()) {
				Vector dest_pos = waypoints.get(active_waypoint);
				double actual_distance = actual_pos.getDistance(dest_pos);
				// Se passei da metade, desacelarar?
				if (!inverted && actual_distance < (total_distance/2)) {
					// Vou inverter a minha aceleração
//					System.out.println("METADE DO CAMINHO!!!");
					inverted = true;
					last_acc.inverse();
					sendAccCommand(last_acc, 0.0);
				}
				// Cheguei?
				if (actual_distance < precision) {
					System.out.println("Cheguei no waypoint " + active_waypoint + " - " + waypoints.get(active_waypoint));
					// Parar o agente
					sendStopCommand();
					// Mudar o waypoint
					last_distance = 0.0;
					active_waypoint++;
					System.out.println("active wp = " + active_waypoint);
					if (active_waypoint == waypoints.size() && loop) {
						active_waypoint = 0;
					} else if (active_waypoint == waypoints.size() && !loop) {
						waypoints.clear();
						time_constrains.clear();
					}
				}
				else {
					// Estou parado ou passei
					if (actual_vel != null &&
							(actual_vel.getMagnitude() == 0 || (actual_vel.getMagnitude() > 0 && last_distance < actual_distance))) {
						// TODO Mudar para o m�todo de Newton!!!
						// Calcular quanto e por quanto tempo devo acelerar
						double time_constrain = time_constrains.get(active_waypoint);
//						System.out.println("actual_pos = " + actual_pos + " - dest_pos = " + dest_pos + " - time_constraint = " + time_constrain);
						double acc_mag = MAX_ACELERATION;
						double t1 = 0.2; 
						boolean found = false;
						int iterations = 0;
						
						while (!found && iterations < 10) {
							// Movimento é composto de um MUV + MU
							// S = (a*t1ˆ2)/2 + a*t1*t2, sendo que t1+t2 deve ser aprox. o time_constraint
							// Se t1+t2 for maior, vou aumentar o t1, se for menor, vou diminuir acc_mag
							double t2 = (actual_distance - (acc_mag * t1 * t1 / 2)) / (acc_mag*t1);
							if (Math.abs(time_constrain-t1-t2) < 0.1) {
								found = true;
							} else {
								if (t1+t2 < time_constrain) {
									acc_mag = acc_mag - 2.0;
								} else {
									t1 = t1 + 0.2;
								}
							}
							iterations++;
						}

						// Calcular a direção na qual deve andar
						total_distance = actual_pos.getDistance(dest_pos);
						Vector acc = new Vector((dest_pos.getValue(0)-actual_pos.getValue(0)), 
								(dest_pos.getValue(1)-actual_pos.getValue(1)), 
								(dest_pos.getValue(2)-actual_pos.getValue(2)));
						acc.normalizeVector();
						acc.product(acc_mag);
//						System.out.println("acc_vec = " + acc);
						// Enviar comando
						last_acc = acc;
						inverted = false;
						sendAccCommand(acc, 0.0);
//						sendAccCommand(acc, t1);
					}
					last_distance = actual_distance;
				}
			} 
			// Não tenho destino
			else {
				// Se estiver em movimento, parar
				if (actual_vel != null && actual_vel.getMagnitude() > 0) {
					sendStopCommand();
				}
			}
		
		}
		
	}
	
	private void sendStopCommand() {
		String cmd = MovementConstants.CMD_STOP;
//		System.out.println(cmd);
		try {
			legsMemory.writeMemory(cmd);
			legs.act();
		} catch (MemoryException e) {
			e.printStackTrace();
		}
	}
	
	private void sendAccCommand(Vector acc, double dur) {
		String cmd = MovementConstants.CMD_WALK + 
			" :" + MovementConstants.PARAM_ACC + " " + acc.toString();
		if (dur > 0.0) {
			cmd += " :" + MovementConstants.PARAM_DUR + " " + Double.toString(dur);
		}
//		System.out.println("acc command: " + cmd);
		try {
			legsMemory.writeMemory(cmd);
			legs.act();
		} catch (MemoryException e) {
			e.printStackTrace();
		}
	}
	
}
