package mms;

//import java.io.PrintWriter;

import mms.Constants.EA_STATE;
import mms.Constants.EH_STATUS;
import mms.clock.TimeUnit;
import mms.memory.MemoryException;

public class Sensor extends EventHandler implements Sensing {
	
	@Override
	public final boolean start() {

		// Sets component type
		setType(Constants.COMP_SENSOR);
		
		if (!super.start()) {
			return false;
		}
		
		myComm.sensing = true;

		// Calls user initialization code
		if (!init()) {
			return false;
		}
		
		// Sets the agent's state to INITIALIZED
		setState(EA_STATE.INITIALIZED);
		
		return true;

	}
	
	@Override
	public final boolean stop() {
		return super.stop();
	}
	
	// Chamado por um raciocínio para registrá-lo como listener dos eventos
	public void registerListener(Reasoning reasoning) {
		listeners.add(reasoning);
	}
	
	// Método chamado pelo Comm ao receber um evento
	public void sense(Event evt) {

		if (status == EH_STATUS.REGISTERED && evt.eventType.equals(eventType)) {
			MusicalAgent.logger.info("[" + getAgent().getAgentName() + ":" + getComponentName() + "] " + "Event received");
			
//			System.out.println(getAgent().getClock().getCurrentTime() + " [" + getAgent().getAgentName() + ":" + getName() + "] " + " Recebi um evento frame = " + evt.frame);
			
			// Chama o método do usuário
			// TODO Melhor antes ou depois? Ou ter os dois?
			try {
				process(evt);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// Armazenar o evento na Base de Conhecimentos
			try {
				if (myMemory == null) {
					System.err.println("ERROR: no memory registered");
				} else {
					myMemory.writeMemory(evt.objContent, evt.instant, evt.duration, TimeUnit.SECONDS);
				}
//				System.out.println("[" + getAgent().getAgentName() + "] Guardei na memória um evento no instante " + evt.instant + " de duração " + evt.duration);
			} catch (MemoryException e1) {
				MusicalAgent.logger.warning("[" + getAgent().getAgentName() + ":" + getComponentName() + "] " + "Não foi possível armazenar na memória");
			}
			
			// Avisar os raciocínios registrados
			for (Reasoning reasoning : listeners) {
				try {
					reasoning.newSense(this, evt.instant, evt.duration);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// No caso de event BATCH, envia o ACK
			if (getAgent().getProperty(Constants.PROCESS_MODE, null).equals(Constants.MODE_BATCH)) {
				Command cmd = new Command(getAddress(), "/" + Constants.FRAMEWORK_NAME + "/" + Constants.ENVIRONMENT_AGENT, Constants.CMD_BATCH_EVENT_ACK);
				sendCommand(cmd);
			}
			
			MusicalAgent.logger.info("[" + getAgent().getAgentName() + ":" + getComponentName() + "] " + "Processei evento " + evt.timestamp);
		}
		
	}

	//--------------------------------------------------------------------------------
	// User implemented methods
	//--------------------------------------------------------------------------------

	/**
	 * Faz o pré-processamento do evento recebido.
	 */
	protected void process(Event evt) throws Exception  {
//		System.out.println("[Sensor] Entrei no process()");
	}

}
