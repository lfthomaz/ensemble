package mms.apps.lm;

import java.util.Set;

import mms.apps.lm.LM_World.Agent;
import mms.Event;
import mms.EventServer;
import mms.Parameters;

public class LM_EnergyEventServer extends EventServer {

	LM_World world;
	
	// Dados para o evento
	String 	agentName;
	String 	agentComponentName;
	float 	food;
	
	@Override
	protected void configure() {
		setEventType("ENERGY");
	}

	@Override
	protected boolean init(Parameters parameters) {

		// obt�m o ambiente virtual
		world = (LM_World)envAgent.getWorld();
		// TODO reservar a comida para os agentes iniciais
		
		// Distribui comida aleatoriamente
		for (int i = 0; i < world.squareLattice.length; i++) {
			for (int j = 0; j < world.squareLattice[i].length; j++) {
				if (Math.random() <= LM_Constants.InitFoodProb) {
					// TODO O valor deve ser configur�vel
					world.squareLattice[i][j].food = 3.0f;
				}
			}
		}
		
		return true;
		
	}

	@Override
	public void process() {
		
		// Verifica o tabuleiro para ver se existe agente em alguma posi��o com comida
		Set<String> set = sensors.keySet();
		for (String sensor : set) {

			String[] str = sensor.split(":");
			Agent agent = world.agents.get(str[0]);
			food = world.squareLattice[agent.pos_x][agent.pos_y].food;
			if (food > 0.0f) {

				// Envia o evento para o agente
				agentName 			= str[0];
				agentComponentName 	= str[1];
				act();
				
				// Apaga a comida usada do tabuleiro
				world.squareLattice[agent.pos_x][agent.pos_y].food = 0.0f;
				
			}

		}
			
	}

	@Override
	protected Event processAction(Event evt) {
		
		Event event 			= new Event();
		event.destAgentName 	= agentName;
		event.destAgentCompName = agentComponentName;
		event.objContent 		= String.valueOf(food);

		return event;
		
	}

}
