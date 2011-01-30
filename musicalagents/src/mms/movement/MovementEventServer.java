
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

	@Override
	public void process() {

//		long start = System.nanoTime();

		double t = (double)clock.getCurrentTime()/1000;
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
								movState.acceleration.update(0,0,0);
								movState.angularVelocity.update(0,0,0);
								stop_command.remove(entity);
							}
						}

						// Updates the movement state
						MovementState newState = new MovementState(world.dimensions);
						movLaw.changeState(movState, t, newState);
						movState = newState;
						try {
							movMemory.writeMemory(movState);
						} catch (MemoryException e) {
							e.printStackTrace();
						}
	
//						System.out.println(entity + " newState = " + newState.position);
						
						// Sends an OSC message
						if (osc) {
							sendOSCPosition(entity, newState);
						}
						
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
					
//					System.out.println("\tpos = " + movState.position);
//					System.out.println("\tvel = " + movState.velocity);
//					System.out.println("\tacc = " + movState.acceleration);
//					System.out.println("\tori = " + movState.orientation);
//					System.out.println("\tang = " + movState.angularVelocity);
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
//		System.out.printf("time_1 = %.3f\n", ((double)time_1/1000000));
//		
//		time_1 = 0;
    	
	}

	@Override
	// TODO Só pode mudar o estado no próximo frame
	public void processSense(Event evt) {
		
		lock.lock();
		
		try {
			double t = (double)clock.getCurrentTime()/1000;
	
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
				}
				// No caso do comando ter uma duração pré-definida, guardar na lista o momento em que deve parar
				String str_dur = cmd.getParameter("dur");
				if (str_dur != null) {
					double dur = Double.valueOf(str_dur); 
					if (dur > 0.0) {
						stop_command.put(entity, (t+dur));
					}
				}
				// Updates the movement state
				MovementState newState = new MovementState(world.dimensions);
				movLaw.changeState(movState, t, newState);
				try {
					movMemory.writeMemory(newState);
				} catch (MemoryException e) {
					e.printStackTrace();
				}
			}
			// TODO Ter uma lista de quais entidades tiveram seu estado atualizado, assim economizamos no envio de Eventos ?!?!

		} finally {
			lock.unlock();
		}
			
	}
	
	@Override
	protected Parameters actuatorRegistered(String agentName,
			String eventHandlerName, Parameters userParam) throws Exception {
		
		MovementState movState = new MovementState(world.dimensions);
		movState.instant = ((double)clock.getCurrentTime() / 1000);

		// Verifies if there is an initial position for the entity
		Vector position = (Vector)world.getEntityStateAttribute(agentName, "POSITION");
		if (position != null) { 
			movState.position = position;
		}
		
		// Get the initial parameters
		if (userParam.containsKey("pos")) {
			movState.position = Vector.parse(userParam.get("pos"));
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
				
		// Creates a memory for the entity
		Memory movMemory = new EventMemory();
		movMemory.start(envAgent, agentName, 10, 0, null);
		movMemory.writeMemory(movState);

    	// Inserts an attribute in the Entity State
    	world.addEntityStateAttribute(agentName, "MOVEMENT", movMemory);
    	
//		String[] sensors = searchRegisteredEventHandler(agentName, "", Constants.EVT_MOVEMENT, Constants.COMP_SENSOR);
//		if (sensors.length == 1) {
//			System.out.println("Found " + sensors.length + " sensors!");
//			EventHandlerInfo info = EventHandlerInfo.parse(sensors[0]);
//			Event evt = new Event();
//			Command cmd2 = new Command("INFO");
//			cmd2.addParameter("pos", state.position.toString());
//			cmd2.addParameter("vel", state.velocity.toString());
//			cmd2.addParameter("ori", state.orientation.toString());
//			evt.objContent = cmd2.toString();
//			addOutputEvent(info.agentName, info.componentName, evt);
//			System.err.println("Vou enviar - actuatorRegistered - " + info.agentName + ":" + info.componentName);
//			// Enviar os eventos
//			try {
//				act();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}

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
		
//		Event evt = new Event();
//		Command cmd = new Command("INFO");
//		Memory movMemory = (Memory)world.getEntityStateAttribute(agentName, "MOVEMENT");
//		MovementState movState = (MovementState)movMemory.readMemory(t, TimeUnit.SECONDS);
//		cmd.addParameter("pos", state.position.toString());
//		cmd.addParameter("vel", state.velocity.toString());
//		cmd.addParameter("ori", state.orientation.toString());
//		evt.objContent = cmd.toString();
//		addOutputEvent(agentName, eventHandlerName, evt);
//		System.err.println("Vou enviar - sensorRegistered");
//		// Enviar os eventos
//		try {
//			act();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
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
//				String x = (String)arguments[2];
//				float y = (Float)arguments[3];
//				float z = (Float)arguments[4];
//				System.out.printf("pos %s %f %f %f\n", agentName, x, y, z);
//				System.out.println("pos " + x);
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
