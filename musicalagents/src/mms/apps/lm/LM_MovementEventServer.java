package mms.apps.lm;

import java.util.Hashtable;
import java.util.Set;

import mms.apps.lm.LM_World.Agent;
import mms.EnvironmentAgent;
import mms.Event;
import mms.EventServer;
import mms.Parameters;

public class LM_MovementEventServer extends EventServer {

	// Mundo virtual
	LM_World world;
	
	// Campos para o evento
	private String 	destAgentName;
	private String 	destAgentCompName;
	private String  destAgentNote;
	private Agent 	proxAgent;
	
	// 
	private int newAgentsCounter = 0;
	
	@Override
	protected void configure() {
		setEventType("MOVEMENT");	
	}

	@Override
	protected boolean init(Parameters parameters) {
		
		world = (LM_World)envAgent.getWorld();
		return true;
		
	}

	
	private Agent checkAgentPresence(int x, int y) {

		Agent agent = null;
		if ((x >= 0 && x < LM_Constants.WorldSize) && (y >= 0 && y < LM_Constants.WorldSize)) {
			agent = world.squareLattice[x][y].agent;
		}
		return agent;
		
	}
	
	@Override
	public void process() {

		// Verifica quais agentes est�o em contato um com outro atrav�s do tent�culo
		Set<String> set = sensors.keySet();
		for (String sensor : set) {

			String[] str = sensor.split(":");
			Hashtable<String,String> param = sensors.get(str[0] + ":" + str[1]);

			Agent agent = world.agents.get(str[0]);
			if (agent != null) {
			
				int x = agent.pos_x;
				int y = agent.pos_y;
				
				if (param == null) {
					System.out.println(str[0] + ":" + str[1] + " param NULL");
				}
				if (sensor == null) {
					System.out.println("sensor NULL");
				}
				
				String position = param.get("position");
				
				switch (agent.direction) {
				
				case LM_World.DIR_N:
					if (position.equals("LEFT")) {
						proxAgent = checkAgentPresence(x, y-1);
					} else if (position.equals("FRONT")) {
						proxAgent  = checkAgentPresence(x-1, y);
					} else if (position.equals("RIGHT")) {
						proxAgent  = checkAgentPresence(x, y+1);
					}
					break;
				case LM_World.DIR_NE:
					if (position.equals("LEFT")) {
						proxAgent = checkAgentPresence(x-1, y-1);
					} else if (position.equals("FRONT")) {
						proxAgent = checkAgentPresence(x-1, y+1);
					} else if (position.equals("RIGHT")) {
						proxAgent = checkAgentPresence(x+1, y+1);
					}
					break;
				case LM_World.DIR_E:
					if (position.equals("LEFT")) {
						proxAgent = checkAgentPresence(x-1, y);
					} else if (position.equals("FRONT")) {
						proxAgent = checkAgentPresence(x, y+1);
					} else if (position.equals("RIGHT")) {
						proxAgent = checkAgentPresence(x+1, y);
					}
					break;
				case LM_World.DIR_SE:
					if (position.equals("LEFT")) {
						proxAgent = checkAgentPresence(x-1, y+1);
					} else if (position.equals("FRONT")) {
						proxAgent = checkAgentPresence(x+1, y+1);
					} else if (position.equals("RIGHT")) {
						proxAgent = checkAgentPresence(x+1, y-1);
					}
					break;
				case LM_World.DIR_S:
					if (position.equals("LEFT")) {
						proxAgent = checkAgentPresence(x, y+1);
					} else if (position.equals("FRONT")) {
						proxAgent = checkAgentPresence(x+1, y);
					} else if (position.equals("RIGHT")) {
						proxAgent = checkAgentPresence(x, y-1);
					}
					break;
				case LM_World.DIR_SW:
					if (position.equals("LEFT")) {
						proxAgent = checkAgentPresence(x+1, y+1);
					} else if (position.equals("FRONT")) {
						proxAgent = checkAgentPresence(x+1, y-1);
					} else if (position.equals("RIGHT")) {
						proxAgent = checkAgentPresence(x-1, y-1);
					}
					break;
				case LM_World.DIR_W:
					if (position.equals("LEFT")) {
						proxAgent = checkAgentPresence(x+1, y);
					} else if (position.equals("FRONT")) {
						proxAgent = checkAgentPresence(x, y-1);
					} else if (position.equals("RIGHT")) {
						proxAgent = checkAgentPresence(x-1, y);
					}
					break;
				case LM_World.DIR_NW:
					if (position.equals("LEFT")) {
						proxAgent = checkAgentPresence(x-1, y+1);
					} else if (position.equals("FRONT")) {
						proxAgent  = checkAgentPresence(x-1, y-1);
					} else if (position.equals("RIGHT")) {
						proxAgent  = checkAgentPresence(x+1, y-1);
					}
					break;
				}
				
				// Se existe um agente pr�ximo, guarda as informa��es e chama o action()
				if (proxAgent != null) {
					destAgentName 		= str[0];
					destAgentCompName 	= str[1];
					destAgentNote 		= envAgent.agentsPublicFacts.get(destAgentName+":"+"SoundGenoma").substring(0, 1);
					act();
				}
			}
		}
		
	}

	@Override
	public void processSense(Event evt) {

		//System.out.println("LM_MovementEventServer: " + evt.timestamp + " " + evt.content);
		
		String str[] = ((String)evt.objContent).split(" ");
		String agentName = str[0];
		String instr = str[1];
		
		Agent agent = world.agents.get(agentName);
		
		if (instr.equals("W")) {
			
			int new_pos_x = agent.pos_x;
			int new_pos_y = agent.pos_y;
			
			switch (agent.direction) {
			case LM_World.DIR_N:
				new_pos_x = new_pos_x - 1;
				if (new_pos_x < 0) {
					new_pos_x = LM_Constants.WorldSize - 1;
				}
				break;
			case LM_World.DIR_NW: 
				new_pos_x = new_pos_x - 1;
				if (new_pos_x < 0) {
					new_pos_x = LM_Constants.WorldSize - 1;
				}
				new_pos_y = new_pos_y - 1;
				if (new_pos_y < 0) {
					new_pos_y = LM_Constants.WorldSize - 1;
				}
				break;
			case LM_World.DIR_W:  
				new_pos_y = new_pos_y - 1;
				if (new_pos_y < 0) {
					new_pos_y = LM_Constants.WorldSize - 1;
				}
				break;
			case LM_World.DIR_SW: 
				new_pos_x = new_pos_x + 1;
				if (new_pos_x >= LM_Constants.WorldSize) {
					new_pos_x = 0;
				}
				new_pos_y = new_pos_y - 1;
				if (new_pos_y < 0) {
					new_pos_y = LM_Constants.WorldSize - 1;
				}
				break;
			case LM_World.DIR_S:
				new_pos_x = new_pos_x + 1;
				if (new_pos_x >= LM_Constants.WorldSize) {
					new_pos_x = 0;
				}
				break;
			case LM_World.DIR_SE:
				new_pos_x = new_pos_x + 1;
				if (new_pos_x >= LM_Constants.WorldSize) {
					new_pos_x = 0;
				}
				new_pos_y = new_pos_y + 1;
				if (new_pos_y >= LM_Constants.WorldSize) {
					new_pos_y = 0;
				}
				break;
			case LM_World.DIR_E:
				new_pos_y = new_pos_y + 1;
				if (new_pos_y >= LM_Constants.WorldSize) {
					new_pos_y = 0;
				}
				break;
			case LM_World.DIR_NE:
				new_pos_x = new_pos_x - 1;
				if (new_pos_x < 0) {
					new_pos_x = LM_Constants.WorldSize - 1;
				}
				new_pos_y = new_pos_y + 1;
				if (new_pos_y >= LM_Constants.WorldSize) {
					new_pos_y = 0;
				}
				break;
			}
			
			// n�o pode andar em cima de um agente
			if (world.squareLattice[new_pos_x][new_pos_y].agent == null) {
				world.squareLattice[agent.pos_x][agent.pos_y].agent = null;
				agent.pos_x = new_pos_x;
				agent.pos_y = new_pos_y;
				world.squareLattice[new_pos_x][new_pos_y].agent = agent;
			}
			
		}
		else if (instr.equals("T+")) {
			
			agent.direction = agent.direction + 1;
			if (agent.direction == 8) {
				agent.direction = 0;
			}
			
		}
		else if (instr.equals("T-")) {
			
			agent.direction = agent.direction - 1;
			if (agent.direction == -1) {
				agent.direction = 7;
			}
			
		}
		else if (instr.equals("Ts")) {

			// TODO implementar a instru��o Ts
			
		}
		
	}
	
	public Hashtable<String, String> actuatorRegistered(String agentName, String eventHandlerName, Hashtable<String, String> param) {
		
		//System.out.println("LM_MovementEventServer: Novo atuador registrado");
		int pos_x = -1;
		int pos_y = -1;
		
		if (param.size() == 2) {
			pos_x = Integer.valueOf(param.get("pos_x"));
			pos_y = Integer.valueOf(param.get("pos_y"));
		} // Se n�o foi passada uma posi��o como par�metro, ou se j� existe um agente na posi��o, posiciona o agente aleatoriamente no mundo virtual
		else {
			boolean found = false;
			while(!found) {
				pos_x = (int)(Math.floor(Math.random() * LM_Constants.WorldSize));
				pos_y = (int)(Math.floor(Math.random() * LM_Constants.WorldSize));
				if (world.squareLattice[pos_x][pos_y].agent == null) {
					found = true;
				}
			}
		}
		
		// Crio um novo agente
		Agent agent = world.new Agent(agentName);
		agent.pos_x = pos_x;
		agent.pos_y = pos_y;
		//agent.direction = (int) Math.floor(Math.random() * 4); 
		agent.direction = 0; 
		world.agents.put(agentName, agent);
		
		world.squareLattice[pos_x][pos_y].agent = agent;

		System.out.println("Coloquei um novo agente " + agentName + " em (" + pos_x + ", " + pos_y + ") com dir " + agent.direction);
		
		return null;
		
	}
	
	// TODO Implementar a retirada do agente do mundo
	public void actuatorDeregistered(String agentName, String eventHandlerName) {
		
		Agent agent = world.agents.get(agentName);
		world.squareLattice[agent.pos_x][agent.pos_y].agent = null;
		world.agents.remove(agentName);
		
	}

	@Override
	protected Event processAction(Event evt) {
		Event event 			= new Event();
		event.destAgentName 	= destAgentName;
		event.destAgentCompName = destAgentCompName;
		event.objContent 		= destAgentNote;

		return event;
	}
	
}
