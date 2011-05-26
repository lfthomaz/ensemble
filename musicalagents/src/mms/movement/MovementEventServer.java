
package mms.movement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import mms.Command;
import mms.Constants;
import mms.Event;
import mms.EventHandlerInfo;
import mms.EventServer;
import mms.Parameters;
import mms.clock.TimeUnit;
import mms.memory.EventMemory;
import mms.memory.Memory;
import mms.memory.MemoryException;
import mms.world.Law;
import mms.world.Vector;
import mms.world.World;

/**
 * O MovementServer representa as leis físicas do movimento no mundo virtual.
 */
public class MovementEventServer extends EventServer {
	
	public static final String MOVEMENT = "MOVEMENT";

	public static final String CMD_WALK 		= "WALK";
	public static final String CMD_TURN 		= "TURN";
	public static final String CMD_TRANSPORT 	= "TRANSPORT";
	public static final String CMD_STOP 		= "STOP";
	public static final String CMD_INFO 		= "INFO";
	
	public static final String PARAM_AGENT 		= "AGENT";
	public static final String PARAM_POS 		= "POS";
	public static final String PARAM_VEL 		= "VEL";
	public static final String PARAM_ACC 		= "ACC";
	
	private static Lock lock = new ReentrantLock();

	private boolean osc = true;
	
	private World 	world;
	private Law 	movLaw;

	/**
	 * List of agents with movement commands enabled
	 */
	HashMap<String,Vector> acc_command = new HashMap<String, Vector>();
	HashMap<String,Double> stop_command = new HashMap<String,Double>();

	@Override
	public boolean configure() {
		setCommType("mms.comm.direct.CommDirect");
		setEventType(MOVEMENT);
		String[] period = getParameters().get("PERIOD", "100 1000").split(" ");
		setEventExchange(Long.valueOf(period[0]), Long.valueOf(period[1]));
		return true;
	}
	
	@Override
	public boolean init() {
		
		this.world = envAgent.getWorld();
		this.movLaw = world.getLaw(MOVEMENT);

		return true;
		
	}
	
	@Override
	public boolean finit() {
		
		return true;
		
	}
	
	private Memory createEntityMemory(String entityName) {
		
		// Gets the Movement Memory
		Memory movMemory = (Memory)world.getEntityStateAttribute(entityName, MOVEMENT);
		// If there is no memory, creates one for this entity
		if (movMemory == null) {
			movMemory = new EventMemory();
			movMemory.start(envAgent, entityName, 1.0, 0, null);
			world.addEntityStateAttribute(entityName, MOVEMENT, movMemory);
		}

		return movMemory;
		
	}

	@Override
	protected Parameters actuatorRegistered(String agentName,
			String eventHandlerName, Parameters userParam) throws Exception {
		
		// Gets the Movement Memory
		Memory movMemory = (Memory)world.getEntityStateAttribute(agentName, MOVEMENT);
		// If there is no memory, creates one for this entity
		if (movMemory == null) {
			movMemory = createEntityMemory(agentName);
		}

		MovementState movState = new MovementState(world.dimensions);
		movState.instant = clock.getCurrentTime(TimeUnit.SECONDS);

		// Get the initial parameters
		if (userParam.containsKey("pos")) {
			String pos = userParam.get("pos") ;
			if (pos.equals("random")) {
				movState.position = new Vector();
				for (int i = 0; i < world.dimensions; i++) {
					movState.position.setValue(i, Math.random() * world.form_size - world.form_size_half);
				}
			} else {
				movState.position = Vector.parse(pos);
			}
		} else {
			// Verifies if there is an initial position for the entity and writes it in the memory
			Vector position = (Vector)world.getEntityStateAttribute(agentName, "POSITION");
			if (position != null) { 
				movState.position = position;
			} else {
				movState.position = new Vector(world.dimensions);
			}
		}
		if (userParam.containsKey("vel")) {
			movState.velocity = Vector.parse(userParam.get("vel"));
		} else {
			movState.velocity = new Vector(world.dimensions);
		}
		if (userParam.containsKey("acc")) {
			movState.acceleration = Vector.parse(userParam.get("acc"));
		} else {
			movState.acceleration = new Vector(world.dimensions);
		}
		movState.orientation = new Vector(world.dimensions);
		movState.angularVelocity = new Vector(world.dimensions);

		// Writes the new movement state
		movMemory.writeMemory(movState);

		// Inserts an attribute in the Entity State
    	world.addEntityStateAttribute(agentName, MOVEMENT, movMemory);
    	
		String[] sensors = searchRegisteredEventHandler(agentName, "", Constants.EVT_MOVEMENT, Constants.COMP_SENSOR);
		if (sensors.length == 1) {
//			System.out.println("Found " + sensors.length + " sensors!");
			EventHandlerInfo info = EventHandlerInfo.parse(sensors[0]);
			Event evt = new Event();
			Command cmd2 = new Command("INFO");
			cmd2.addParameter("pos", movState.position.toString());
			cmd2.addParameter("vel", movState.velocity.toString());
			cmd2.addParameter("ori", movState.orientation.toString());
			evt.objContent = cmd2.toString();
			addOutputEvent(info.agentName, info.componentName, evt);
//			System.err.println("Vou enviar - actuatorRegistered - " + info.agentName + ":" + info.componentName);
			// Enviar os eventos
			try {
				act();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Mensagem OSC
    	if (osc) {
    		// Register the agent
    		String str = "agent " + agentName + " src";
    		for (int i = 0; i < movState.position.dimensions; i++) {
				str += " " + movState.position.getValue(i);
			}
    		Command cmd = new Command(getAddress(), "/pd", "OSC");
    		cmd.addParameter("CONTENT", str);
    		sendCommand(cmd); 
    		// Informs the position
    		sendOSCPosition(agentName, movState);
    	}
    	
		return userParam;
		
	}
	
	@Override
	protected Parameters sensorRegistered(String agentName, String eventHandlerName, Parameters userParam) throws Exception {
		
		// Gets the Movement Memory
		Memory movMemory = (Memory)world.getEntityStateAttribute(agentName, MOVEMENT);
		// If there is no memory, creates one for this entity
		if (movMemory == null) {
			movMemory = createEntityMemory(agentName);
		}

		// Verifies if there is an initial position for the entity and writes it in the memory
		MovementState movState = new MovementState(world.dimensions);
		movState.instant = clock.getCurrentTime(TimeUnit.SECONDS);
		Vector position = (Vector)world.getEntityStateAttribute(agentName, "POSITION");
		if (position != null) { 
			movState.position = position;
		} else {
			movState.position = new Vector(world.dimensions);
		}
		movState.velocity = new Vector(world.dimensions);
		movState.acceleration = new Vector(world.dimensions);
		movState.orientation = new Vector(world.dimensions);
		movState.angularVelocity = new Vector(world.dimensions);
		try {
			movMemory.writeMemory(movState);
		} catch (MemoryException e) {
			e.printStackTrace();
		}

		// Sends the position to the sensor
		Event evt = new Event();
		Command cmd = new Command("INFO");
		cmd.addParameter("pos", movState.position.toString());
		cmd.addParameter("vel", movState.velocity.toString());
		cmd.addParameter("ori", movState.orientation.toString());
		evt.objContent = cmd.toString();
		addOutputEvent(agentName, eventHandlerName, evt);
		
		return userParam;
	}
	
	@Override
	// TODO Só pode mudar o estado no próximo frame
	public void processSense(Event evt) {
		
		lock.lock();
		
		try {
			double t = clock.getCurrentTime(TimeUnit.SECONDS);
	
	//		System.out.println("[MovementEventServer] processSense() = " + evt.objContent);
			String strContent = (String)evt.objContent;
			String entity = evt.oriAgentName;
//			System.out.println("Processing command of '" + entity + "' at t = " + t + " " + strContent);
	
			// Processar novo comando
			Command cmd = Command.parse(strContent);
			if (cmd != null) {
				// Gerar um novo estado
				Memory movMemory = (Memory)world.getEntityStateAttribute(entity, MOVEMENT);
				MovementState oldState = ((MovementState)movMemory.readMemory(t, TimeUnit.SECONDS));
				if (oldState != null) {
					MovementState newState = new MovementState(world.dimensions);
					movLaw.changeState(oldState, clock.getCurrentTime(TimeUnit.SECONDS), newState);
					if (cmd.getCommand().equals(CMD_WALK)) {
						// TODO Aqui deveria avaliar a possibilidade da mudança (mudanças bruscas não poderiam acontecer)
						newState.acceleration = Vector.parse(cmd.getParameter("acc"));
					} else if (cmd.getCommand().equals(CMD_TURN)) {
						newState.angularVelocity = Vector.parse(cmd.getParameter("ang_vel"));
					} else if (cmd.getCommand().equals(CMD_STOP)) {
						newState.velocity.zero();
						newState.acceleration.zero();
						newState.angularVelocity.zero();
						stop_command.remove(entity);
					} else if (cmd.getCommand().equals(CMD_TRANSPORT)) {
						newState.position = Vector.parse(cmd.getParameter("pos"));
					}
					// No caso do comando ter uma duração pré-definida, guardar na lista o momento em que deve parar
					String str_dur = cmd.getParameter("dur");
					if (str_dur != null) {
						double dur = Double.valueOf(str_dur); 
						if (dur > 0.0) {
							stop_command.put(entity, (t+dur));
						}
					}
//					System.out.println("new state = " + newState.instant + " " + newState.position + " " + newState.velocity + " " + newState.acceleration);
					try {
						movMemory.writeMemory(newState);
					} catch (MemoryException e) {
						e.printStackTrace();
					}
				}
			}
		} finally {
			lock.unlock();
		}
			
	}
	
	@Override
	public void process() {

		double t = clock.getCurrentTime(TimeUnit.SECONDS);
		
		lock.lock();
		try {

			// Process the movement of each entity
			Set<String> entities = world.getEntityList();
			for (String entity : entities) {
				
				Memory movMemory = (Memory)world.getEntityStateAttribute(entity, MOVEMENT);
				
				boolean positionChanged = false;
				if (movMemory != null) {

					MovementState oldState = (MovementState)movMemory.readMemory(t, TimeUnit.SECONDS);
					
					// If necessary, updates the movement state
					if (oldState != null && (oldState.acceleration.magnitude > 0 || 
											oldState.velocity.magnitude > 0 || 
											oldState.angularVelocity.magnitude > 0)) {

						MovementState newState = new MovementState(world.dimensions);
						movLaw.changeState(oldState, clock.getCurrentTime(TimeUnit.SECONDS), newState);

						// Checks if the movement has a duration and updates the acceleration 
						if (stop_command.containsKey(entity)) {
							double dur = stop_command.get(entity);
							if (dur >= newState.instant && dur < t) {
								newState.acceleration.zero();
								newState.angularVelocity.zero();
								stop_command.remove(entity);
							}
						}

						try {
							movMemory.writeMemory(newState);
						} catch (MemoryException e) {
							e.printStackTrace();
						}
						positionChanged = true;

						// Sends an OSC message
						if (osc && positionChanged) {
							sendOSCPosition(entity, newState);
						}
						
						// Creates a response event if there is a sensor registered
						String[] sensors = searchRegisteredEventHandler(entity, "", Constants.EVT_MOVEMENT, Constants.COMP_SENSOR);
	//							System.out.println("Found " + sensors.length + " sensors!");
						if (sensors.length == 1) {
							EventHandlerInfo info = EventHandlerInfo.parse(sensors[0]);
							Event evt = new Event();
							Command cmd2 = new Command("INFO");
							cmd2.addParameter("pos", newState.position.toString());
							cmd2.addParameter("vel", newState.velocity.toString());
							cmd2.addParameter("ori", newState.orientation.toString());
							evt.objContent = cmd2.toString();
							addOutputEvent(info.agentName, info.componentName, evt);
						}
							
					}
				}
			}
					
			// Sends events
			try {
				act();
			} catch (Exception e) {
				e.printStackTrace();
			}

		} finally {
			lock.unlock();
		}
		
	}

	/**
	 * Sends entity's position via OSC protocol
	 * @param state
	 */
	private void sendOSCPosition(String entityName, MovementState state) {

		// Envia a nova posição via OSC
//		Command cmd = new Command("MOVEMENT_UPDATE");
//		cmd.addParameter("NAME", entityName);
//		cmd.addParameter("POS", state.position.toString());
//		cmd.addParameter("VEL", state.velocity.toString());
//		cmd.addParameter("ACC", state.acceleration.toString());
		String str = "pos " + entityName;
		for (int i = 0; i < state.position.dimensions; i++) {
			str += " " + state.position.getValue(i);
		}
		Command cmd = new Command(getAddress(), "/pd", "OSC");
		cmd.addParameter("CONTENT", str);
		sendCommand(cmd);

	}
	
	@Override
	public void processCommand(Command cmd) {

		if (cmd.getCommand().equals(CMD_TRANSPORT)) {
			
			String agentName = cmd.getParameter("AGENT");
			Vector pos = Vector.parse(cmd.getParameter("POS"));
			
			Memory movMemory = (Memory)world.getEntityStateAttribute(agentName, MOVEMENT);
			
			MovementState movState = new MovementState(world.dimensions);
			movState.position = pos;
			movState.acceleration = new Vector(world.dimensions);
			
			try {
	              movMemory.writeMemory(movState);
	         } catch (MemoryException e) {
	              e.printStackTrace();
	         }
			
		}

	}
	
}
