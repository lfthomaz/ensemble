
package mms.movement;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;

import mms.Constants;
import mms.Event;
import mms.EventHandlerInfo;
import mms.EventServer;
import mms.Parameters;
import mms.clock.TimeUnit;
import mms.commands.Command;
import mms.kb.EventMemory;
import mms.kb.Memory;
import mms.kb.MemoryException;
import mms.world.Vector;
import mms.world.World;
import mms.world.law.Law;

/**
 * O MovementServer representa as leis físicas do movimento no mundo virtual.
 */
public class MovementEventServer extends EventServer {

	long time_1 = 0;
	long time_2 = 0;
	long time_3 = 0;
	long time_4 = 0;
	long time_5 = 0;
	long time_6 = 0;

	private static Lock lock = new ReentrantLock();

	private boolean osc = false;
	
	private World 	world;
	private Law 	movLaw;

	/**
	 * List of agents with movement commands enabled
	 */
	HashMap<String,Vector> acc_command = new HashMap<String, Vector>();
	HashMap<String,Double> stop_command = new HashMap<String,Double>();
	
	private int dx = 0;
	private int dy = 0;
	private double coeficient = 0.1;

	@Override
	protected void configure() {
		setCommType("mms.comm.direct.CommDirect");
		setEventType("MOVEMENT");
		String[] period = getParameters().get("PERIOD", "100 1000").split(" ");
		setEventExchange(Long.valueOf(period[0]), Long.valueOf(period[1]));
	}
	
	@Override
	protected boolean init(Parameters parameters) {
		
		this.osc = Boolean.parseBoolean(envAgent.getProperty(Constants.OSC, "FALSE"));
		
		this.world = envAgent.getWorld();
		this.movLaw = world.getLaw("MOVEMENT");

		envAgent.getOSC().registerListener(new Listener(), "/mms/movement");
		
		return true;
		
	}
	
	private Memory createEntityMemory(String entityName) {
		
		// Gets the Movement Memory
		Memory movMemory = (Memory)world.getEntityStateAttribute(entityName, "MOVEMENT");
		// If there is no memory, creates one for this entity
		if (movMemory == null) {
			movMemory = new EventMemory();
			movMemory.start(envAgent, entityName, 5.0, 0, null);
		}

		// Verifies if there is an initial position for the entity and writes it in the memory
		MovementState movState = new MovementState(world.dimensions);
		movState.instant = clock.getCurrentTime(TimeUnit.SECONDS);
		Vector position = (Vector)world.getEntityStateAttribute(entityName, "POSITION");
		if (position != null) { 
			movState.position = position;
		}
		movState.position = new Vector(world.dimensions);
		movState.velocity = new Vector(world.dimensions);
		movState.acceleration = new Vector(world.dimensions);
		movState.orientation = new Vector(world.dimensions);
		movState.angularVelocity = new Vector(world.dimensions);
		try {
			movMemory.writeMemory(movState);
		} catch (MemoryException e) {
			e.printStackTrace();
		}
		
		return movMemory;
		
	}

	private void updateMovementState(String entity, MovementState movState, Memory movMemory, double t) {

		// Updates the movement state
		MovementState newState = new MovementState(world.dimensions);
		movLaw.changeState(movState, t, newState);
		movState = newState;
		try {
			movMemory.writeMemory(movState);
		} catch (MemoryException e) {
			e.printStackTrace();
		}

//		System.out.println(entity + " newState = " + newState.position);
		
		// Sends an OSC message
		if (osc) {
			sendOSCPosition(entity, newState);
		}
		
		// Creates a response event if there is a sensor registered
		String[] sensors = searchRegisteredEventHandler(entity, "", Constants.EVT_MOVEMENT, Constants.COMP_SENSOR);
//				System.out.println("Found " + sensors.length + " sensors!");
		if (sensors.length == 1) {
			EventHandlerInfo info = EventHandlerInfo.parse(sensors[0]);
			Event evt = new Event();
			Command cmd2 = new Command("INFO");
			cmd2.addParameter("pos", movState.position.toString());
			cmd2.addParameter("vel", movState.velocity.toString());
			cmd2.addParameter("ori", movState.orientation.toString());
			evt.objContent = cmd2.toString();
			addOutputEvent(info.agentName, info.componentName, evt);
		}
		
//		System.out.println("\tpos = " + movState.position);
//		System.out.println("\tvel = " + movState.velocity);
//		System.out.println("\tacc = " + movState.acceleration);
//		System.out.println("\tori = " + movState.orientation);
//		System.out.println("\tang = " + movState.angularVelocity);

	}
	
	@Override
	public void process() {

//		long start = System.nanoTime();

		double t = clock.getCurrentTime(TimeUnit.SECONDS);
//		System.out.println("Process - t = " + t + " s");
		
		lock.lock();
		try {

			// Process the movement of each entity
			Set<String> entities = world.getEntityList();
			for (String entity : entities) {
				
//				System.out.println("Processing movement of '" + entity + "'");

				Memory movMemory = (Memory)world.getEntityStateAttribute(entity, "MOVEMENT");
				
				if (movMemory != null) {

					MovementState movState = (MovementState)movMemory.readMemory(t, TimeUnit.SECONDS);
					
					// If necessary, updates the movement state
					if (movState.acceleration.magnitude > 0 || 
							movState.velocity.magnitude > 0 || 
							movState.angularVelocity.magnitude > 0) {

						// Checks if the movement has a duration and updates the acceleration 
						if (stop_command.containsKey(entity)) {
							double dur = stop_command.get(entity);
							if (dur >= movState.instant && dur < t) {
								movState.acceleration.zero();
								movState.angularVelocity.zero();
								stop_command.remove(entity);
							}
						}

						updateMovementState(entity, movState, movMemory, t);

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
		
//		time_1 = time_1 + (System.nanoTime() - start);
//		
//		System.out.printf("MS time = %.3f\n", ((double)time_1/1000000));
//		
//		time_1 = 0;
    	
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
				Memory movMemory = (Memory)world.getEntityStateAttribute(entity, "MOVEMENT");
				MovementState movState = (MovementState)movMemory.readMemory(t, TimeUnit.SECONDS);
				if (movState != null) {
			//		System.out.println("\tCommand found - " + cmd.toString());
					if (cmd.getCommand().equals("WALK")) {
						// TODO Aqui deveria avaliar a possibilidade da mudança (mudanças bruscas não poderiam acontecer)
						movState.acceleration = Vector.parse(cmd.getParameter("acc"));
					} else if (cmd.getCommand().equals("TURN")) {
						movState.angularVelocity = Vector.parse(cmd.getParameter("ang_vel"));
					} else if (cmd.getCommand().equals("STOP")) {
						movState.velocity.zero();
						movState.acceleration.zero();
						movState.angularVelocity.zero();
						stop_command.remove(entity);
					} else if (cmd.getCommand().equals("TELEPORT")) {
						movState.position = Vector.parse(cmd.getParameter("pos"));
					}
					// No caso do comando ter uma duração pré-definida, guardar na lista o momento em que deve parar
					String str_dur = cmd.getParameter("dur");
					if (str_dur != null) {
						double dur = Double.valueOf(str_dur); 
						if (dur > 0.0) {
							stop_command.put(entity, (t+dur));
						}
					}
	
					updateMovementState(entity, movState, movMemory, t);
				}
			}

		} finally {
			lock.unlock();
		}
			
	}
	
	@Override
	protected Parameters actuatorRegistered(String agentName,
			String eventHandlerName, Parameters userParam) throws Exception {
		
		// Gets the Movement Memory
		Memory movMemory = (Memory)world.getEntityStateAttribute(agentName, "MOVEMENT");
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
				System.out.println(agentName + " position is " + movState.position);
			} else {
				movState.position = Vector.parse(pos);
			}
		} else {
			movState.position = new Vector(world.dimensions);
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
    	world.addEntityStateAttribute(agentName, "MOVEMENT", movMemory);
    	
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
    		Object[] args = new Object[6];
    		args[0] = new String("agent");
    		args[1] = new String(agentName);
    		args[2] = new String("src");
    		args[3] = new Float(movState.position.getValue(0));
    		args[4] = new Float(movState.position.getValue(1));
    		args[5] = new Float(movState.position.getValue(2));
    		envAgent.getOSC().send(args);
    	}

		return userParam;
		
	}
	
	@Override
	protected Parameters sensorRegistered(String agentName,
			String eventHandlerName, Parameters userParam) throws Exception {
		
		// Gets the Movement Memory
		Memory movMemory = (Memory)world.getEntityStateAttribute(agentName, "MOVEMENT");
		// If there is no memory, creates one for this entity
		if (movMemory == null) {
			movMemory = createEntityMemory(agentName);
		}

		Event evt = new Event();
		Command cmd = new Command("INFO");
		MovementState movState = (MovementState)movMemory.readMemory(clock.getCurrentTime(TimeUnit.SECONDS), TimeUnit.SECONDS);
		cmd.addParameter("pos", movState.position.toString());
		cmd.addParameter("vel", movState.velocity.toString());
		cmd.addParameter("ori", movState.orientation.toString());
		evt.objContent = cmd.toString();
		addOutputEvent(agentName, eventHandlerName, evt);
//			System.err.println("Vou enviar - sensorRegistered");
		// Enviar os eventos
		try {
			act();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return userParam;
	}
	
	/**
	 * Sends entity's position via OSC protocol
	 * @param state
	 */
	private void sendOSCPosition(String entityName, MovementState state) {

		// Envia a nova posição via OSC
		Object[] args = new Object[5];
		args[0] = new String("pos");
		args[1] = new String(entityName);
		args[2] = new Float(state.position.getValue(0));
		args[3] = new Float(state.position.getValue(1));
		args[4] = new Float(state.position.getValue(2));
		envAgent.getOSC().send(args);

	}
	
	class Listener implements OSCListener {

		@Override
		public void acceptMessage(Date time, OSCMessage message) {

			// Obtém os argumentos
			System.out.println(message.toString());
			Object[] arguments = message.getArguments();
			// TODO Problemas com a conversão de arguments para Float
			// TODO Se eu mudar a posição aqui, vai mandar uma atualizaçai de volta para o gui??
			if (arguments[0].equals("pos")) {
				String agentName = (String)arguments[1];

				System.out.println("OSC args count(" + arguments.length + "): ");

				//recupera a nova posicao
				float x = Float.parseFloat(arguments[2].toString());
				float y = Float.parseFloat(arguments[3].toString());
				float z = Float.parseFloat(arguments[4].toString());
				
				System.out.printf("Incoming OSC: pos %s %f %f %f\n", agentName, x, y, z);
				//System.out.println("pos " + x);
				
				//acc_command.put(agentName,new Vector(x,y,z));
				Memory movMemory = (Memory)world.getEntityStateAttribute(agentName, "MOVEMENT");
				
				MovementState movState = new MovementState(world.dimensions);
				movState.position = new Vector(x,y,z);
				movState.acceleration = new Vector(world.dimensions);
				
				updateMovementState(agentName, movState ,movMemory,clock.getCurrentTime(TimeUnit.SECONDS));
				
			}
			else if (arguments[0].equals("mouse")) {
				String agentName = (String)arguments[1];
				int delta_x = (Integer)arguments[2];
				int delta_y = (Integer)arguments[3];
	//			System.out.printf("frame: %d - recebi dx=%d dy=%d\n", workingFrame, delta_x, delta_y);
				if (Math.abs(delta_y) > 50) {
					dx = (-delta_y);
				}
				if (Math.abs(delta_x) > 50) {
					dy = (-delta_x);
				}
				
				// TODO Vai mudar a força do próximo frame, mas não diretamente no process()
//				world.getEntityState(state, agentName);
				if (dx != 0 || dy != 0) {
					System.out.printf("dx=%d dy=%d\n", dx, dy);
					acc_command.put(agentName, new Vector(dx*coeficient, dy*coeficient, 0.0));
	//				System.out.println("Acc = " + state.acceleration);
				}
			}

		}
		
	}
	
}
