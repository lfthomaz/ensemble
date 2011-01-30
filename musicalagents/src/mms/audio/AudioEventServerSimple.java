package mms.audio;

import jade.util.Logger;

import java.util.Enumeration;
import java.util.HashMap;

import mms.Constants;
import mms.Event;
import mms.EventServer;
import mms.MusicalAgent;
import mms.Parameters;
import mms.clock.TimeUnit;
import mms.kb.AudioMemory;
import mms.kb.Memory;
import mms.kb.MemoryException;
import mms.world.World;

/**
 * An implementation of a simple Audio Event Server. 
 * It propagates audio in the virtual environment without any physical simulation. 
 * @author lfthomaz
 *
 */
public class AudioEventServerSimple extends EventServer {

	// Log
	public static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	// Utilizado para comparar o tempo (ajustar de acordo com a precis‹o desejada), em segundos
	private final double 	EPSILON 		= 1E-10;
	private final int 		MAX_ITERATIONS 	= 10;

	// TODO Tornar parametriz‡vel os valores utilizados em AudioEventServer
	private double	SPEED_SOUND			= 343.3; // speed of sound (m/s)
	private double 	REFERENCE_DISTANCE 	= 1.0;
	private double 	ROLLOFF_FACTOR 		= 1.0;
    private int 	SAMPLE_RATE 		= 44100;
    private double 	STEP 				= 1 / SAMPLE_RATE;
    private int 	CHUNK_SIZE 			= 4410;
	
    // TODO Ver como armazenar o last_delta de cada source
    private double last_delta 	= Double.MIN_VALUE;
    
    // Table that stores sent audio chunks
    private HashMap<String, Memory> memories;

	// Descrição do mundo
	private World world;
	
	@Override
	public void configure() {
		// Valores default
		setEventType(Constants.EVT_AUDIO);
		Parameters parameters = getParameters();
		if (parameters.containsKey(Constants.PARAM_COMM_CLASS)) {
			setCommType(parameters.get(Constants.PARAM_COMM_CLASS));
		} else {
			setCommType("mms.comm.direct.CommDirect");
		}
		if (parameters.containsKey(Constants.PARAM_COMM_CLASS)) {
			String[] str = (parameters.get(Constants.PARAM_PERIOD)).split(" ");
			setEventExchange(Integer.valueOf(str[0]), Integer.valueOf(str[1]), Integer.valueOf(str[2]), Integer.valueOf(str[3]));
		} else {
			setEventExchange(100, 40, 80, 1000);
		}
	}

	@Override
	// TODO E se eu estender está classe? Preciso chamar esse init() mesmo assim??
	protected boolean init(Parameters parameters) {

		// Inicialização dos parâmetros
		this.SPEED_SOUND		= Double.valueOf(parameters.get("SPEED_SOUND", "343.3"));
		this.REFERENCE_DISTANCE = Double.valueOf(parameters.get("REFERENCE_DISTANCE", "1.0"));
		this.ROLLOFF_FACTOR 	= Double.valueOf(parameters.get("ROLLOFF_FACTOR", "1.0"));
		
		// TODO Parametrizar
		this.SAMPLE_RATE 		= 44100;
		this.STEP 				= 1 / (double)SAMPLE_RATE;
		
		// Chunk size deve ser baseado na freqüência
		// TODO Cuidado com aproximações aqui!
		this.CHUNK_SIZE 		= (int)Math.round(SAMPLE_RATE * ((double)period / 1000));
//		System.out.printf("%d %f %d\n", SAMPLE_RATE, STEP, CHUNK_SIZE);
		
		world = envAgent.getWorld();
		
		memories = new HashMap<String, Memory>();
		
		return true;

	}

	@Override
	public Parameters actuatorRegistered(String agentName, String eventHandlerName, Parameters userParameters) {
		
		Parameters extraParameters = new Parameters();
		extraParameters.put(Constants.PARAM_CHUNK_SIZE, String.valueOf(CHUNK_SIZE));
		extraParameters.put(Constants.PARAM_SAMPLE_RATE, String.valueOf(SAMPLE_RATE));
		extraParameters.put(Constants.PARAM_STEP, String.valueOf(STEP));
		extraParameters.put(Constants.PARAM_PERIOD, String.valueOf(period));
		extraParameters.put(Constants.PARAM_START_TIME, String.valueOf(startTime));
		
		// Cria uma memória para o atuador
		String memoryName = agentName+":"+eventHandlerName;
		Memory memory = new AudioMemory();
		memory.start(envAgent, Constants.EVT_AUDIO, 5.0, 5.0, extraParameters);
		memories.put(memoryName, memory);
		
		return extraParameters;
		
	}
	
	@Override
	public Parameters sensorRegistered(String agentName, String eventHandlerName, Parameters userParameters) throws Exception {
		
		Parameters extraParameters = new Parameters();
		extraParameters.put(Constants.PARAM_CHUNK_SIZE, String.valueOf(CHUNK_SIZE));
		extraParameters.put(Constants.PARAM_SAMPLE_RATE, String.valueOf(SAMPLE_RATE));
		extraParameters.put(Constants.PARAM_STEP, String.valueOf(STEP));
		extraParameters.put(Constants.PARAM_PERIOD, String.valueOf(period));
		extraParameters.put(Constants.PARAM_START_TIME, String.valueOf(startTime));

		// Cria uma memória para o sensor
		String memoryName = agentName+":"+eventHandlerName;
		Memory memory = new AudioMemory();
		memory.start(envAgent, Constants.EVT_AUDIO, 5.0, 5.0, extraParameters);
		memories.put(memoryName, memory);

		return extraParameters;
	}
	
	@Override
	public void process() {

		// TODO Ver se vamos trabalhar com milisegundos ou segundos
		double instant = (double)(startTime + workingFrame * period) / 1000;

		System.out.println("SENSORS = " + sensors.size() + " - ACTUATORS = " + actuators.size());
		for (Enumeration<String> s = sensors.keys(); s.hasMoreElements();) {
			
			// Cria o evento a ser enviado para o sensor
			String s_key = s.nextElement();
			Event evt = new Event();
			String[] sensor = s_key.split(":");
			evt.destAgentName = sensor[0];
			evt.destAgentCompName = sensor[1];
			double[] buf = new double[CHUNK_SIZE];
			evt.objContent = buf;
			evt.instant = instant;
			evt.duration = (double)(CHUNK_SIZE * STEP);

			// Calcula a contribuição de cada fonte sonora
			for (Enumeration<String> a = actuators.keys(); a.hasMoreElements();) {
				
				String a_key = a.nextElement();
				Memory mem = memories.get(a_key);
				// Percorrer todos os samples do chunk a ser preenchido
				for (int j = 0; j < CHUNK_SIZE; j++) {
					double t = instant + (j * STEP);
					double value = (Double)mem.readMemory(t, TimeUnit.SECONDS);
					buf[j] = buf[j] + value;

				}
				
			}			

			// Puts the newly created event in the output events' queue
			addOutputEvent(evt.destAgentName, evt.destAgentCompName, evt);
			
		}
				
	}

	// TODO Verificar problemas de concorrência!
	public void processSense(Event evt) {
		
//		System.out.println("RECEBI UM EVENTO DE " + evt.oriAgentName + " " + evt.frame);
		
		// TODO Tratar depois o que acontece quando muda o tamanho do chunk
//		System.out.println("Inseri na tabela - frame = " + workingFrame + " - pos = " + state.position);
		Memory mem = memories.get(evt.oriAgentName+":"+evt.oriAgentCompName);
		try {
			mem.writeMemory(evt.objContent, evt.instant, evt.duration, TimeUnit.SECONDS);
//			System.out.println("Recebi um evento " + evt.instant + " " + evt.duration);
		} catch (MemoryException e) {
			e.printStackTrace();
		}
		
	}

}
