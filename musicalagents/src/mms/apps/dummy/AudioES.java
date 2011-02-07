package mms.apps.dummy;

import java.util.HashMap;

import mms.Constants;
import mms.Event;
import mms.EventServer;
import mms.Parameters;
import mms.Sensor;
import mms.kb.AudioMemory;
import mms.kb.Memory;

public class AudioES extends EventServer {

	int SAMPLE_RATE = 44100;
	int CHUNK_SIZE = 0;
	double STEP = 1/(double)SAMPLE_RATE;
	
    // Table that stores sent audio chunks
    private HashMap<String, Memory> memories = new HashMap<String,Memory>();
	
	@Override
	protected void configure() {
		setEventType("AUDIO");
		// TODO Não é automático o tratamento dos parâmetros vindo do XML (ver AudioEventServer)
		setEventExchange(5000, 2000, 4000, 1000);
	}
	
	@Override
	protected boolean init(Parameters parameters) throws Exception {
		
		this.CHUNK_SIZE	= 4410;
		
		return true;
		
	}
	
	@Override
	protected Parameters actuatorRegistered(String agentName,
			String eventHandlerName, Parameters userParam) throws Exception {
		
		Parameters userParameters = new Parameters();
		userParameters.put(Constants.PARAM_CHUNK_SIZE, String.valueOf(CHUNK_SIZE));
		userParameters.put(Constants.PARAM_SAMPLE_RATE, String.valueOf(SAMPLE_RATE));
		userParameters.put(Constants.PARAM_STEP, String.valueOf(STEP));
		userParameters.put(Constants.PARAM_PERIOD, String.valueOf(period));
		userParameters.put(Constants.PARAM_START_TIME, String.valueOf(startTime));

		// Cria uma memória para o atuador
		String memoryName = agentName+":"+eventHandlerName;
		Memory memory = new AudioMemory();
		memory.start(envAgent, Constants.EVT_AUDIO, 5.0, 5.0, userParameters);
		memories.put(memoryName, memory);
		
		
		return userParameters;

	}
	
	@Override
	protected Parameters sensorRegistered(String agentName,
			String eventHandlerName, Parameters userParam) throws Exception {

		return super.sensorRegistered(agentName, eventHandlerName, userParam);
		
	}
	
	@Override
	protected void processSense(Event evt) throws Exception {

	}
	
	@Override
	protected void process() throws Exception {
		
		System.out.println("SENSOR " + sensors.size() + " / ACTUATORS = " + actuators.size());

		// Simula o processamento
		Thread.sleep(10);
		
	}

}
