package mms.audio;

import jade.util.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
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
import mms.movement.MovementState;
import mms.world.Vector;
import mms.world.World;
import mms.world.law.MovementLaw;

public class AudioEventServerLight extends EventServer {

	// Log
	public static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

    private enum PROCESS_MODE {NORMAL, LIN_INT, POL_INT};

    //---- WORK VARIABLES ----
	// Newton function's variables 
	double[] f_res	= new double[2];
	double[] f		= new double[2];
	double[] fl	 	= new double[2];
	double[] fh 	= new double[2];
	// States
	MovementState 	rcv_state;
	MovementState 	src_state;
	Vector 			vec_aux;
	Vector 			rcv_comp_pos;
	Vector 			src_comp_pos;
	
	// Utilizado para comparar o tempo (ajustar de acordo com a precis‹o desejada), em segundos
	private final double 	EPSILON 		= 1E-6;
	private final int 		MAX_ITERATIONS 	= 10;

	// TODO Tornar parametrizável os valores utilizados em AudioEventServer
	private double	SPEED_SOUND			= 343.3; // speed of sound (m/s)
	private double 	REFERENCE_DISTANCE 	= 1.0;
	private double 	ROLLOFF_FACTOR 		= 1.0;
    private int 	SAMPLE_RATE 		= 44100;
    private double 	STEP 				= 1 / SAMPLE_RATE;
    private int 	CHUNK_SIZE 			= 4410;
    private int 	DIVISION_FACTOR		= 2;
    private int 	NUMBER_OF_POINTS	= 3;
    private PROCESS_MODE mode 			= PROCESS_MODE.NORMAL;
	
    // Table that stores the last calculated delta of each pair
    double[] deltas;
    private HashMap<String, Double> last_deltas = new HashMap<String, Double>();
    
    // Table that stores sent audio chunks
    private HashMap<String, Memory> memories = new HashMap<String, Memory>();

	// Descrição do mundo
	private World world;
	
	private MovementLaw movLaw;
	
	// Performance
	int number_of_frames;
	long proc_time_1, proc_time_2, proc_time_3;
	PrintWriter file_perf, file_perf_1, file_perf_2, file_perf_3;
	
	@Override
	public void configure() {
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
			setEventExchange(500, 200, 400, 1000);
		}
	}

	@Override
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
		this.deltas 			= new double[CHUNK_SIZE];
//		System.out.printf("%d %f %d\n", SAMPLE_RATE, STEP, CHUNK_SIZE);
		
		this.world = envAgent.getWorld();
		
		rcv_state = new MovementState(world.dimensions);
		src_state = new MovementState(world.dimensions);
		vec_aux = new Vector(world.dimensions);
		
		// Gets the Movement Law
		this.movLaw = (MovementLaw)world.getLaw("MOVEMENT");
		
//		try {
//			out = new PrintWriter(new BufferedWriter(new FileWriter("foo.out")));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		buffer = new StringBuilder(50);
		try {
			file_perf = new PrintWriter(new FileOutputStream("out_perf.txt"), false);
//			file_perf_1 = new PrintWriter(new FileOutputStream("out_perf_1.txt"), false);
//			file_perf_2 = new PrintWriter(new FileOutputStream("out_perf_2.txt"), false);
//			file_perf_3 = new PrintWriter(new FileOutputStream("out_perf_3.txt"), false);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return true;

	}

	@Override
	public Parameters actuatorRegistered(String agentName, String eventHandlerName, Parameters userParam) {
		
		Parameters userParameters = new Parameters();
		userParameters.put(Constants.PARAM_CHUNK_SIZE, String.valueOf(CHUNK_SIZE));
		userParameters.put(Constants.PARAM_SAMPLE_RATE, String.valueOf(SAMPLE_RATE));
		userParameters.put(Constants.PARAM_STEP, String.valueOf(STEP));
		userParameters.put(Constants.PARAM_PERIOD, String.valueOf(period));
		userParameters.put(Constants.PARAM_START_TIME, String.valueOf(startTime));
		
		// Cria uma memória para o atuador
		// TODO Memória do atuador deveria ser parametrizável!!!
		String memoryName = agentName+":"+eventHandlerName;
		Memory memory = new AudioMemory();
		memory.start(envAgent, Constants.EVT_AUDIO, 5.0, 5.0, userParameters);
		memories.put(memoryName, memory);

		return userParameters;
		
	}
	
	@Override
	public Parameters sensorRegistered(String agentName, String eventHandlerName, Parameters userParam) throws Exception {
		
		Parameters userParameters = new Parameters();
		userParameters.put(Constants.PARAM_CHUNK_SIZE, String.valueOf(CHUNK_SIZE));
		userParameters.put(Constants.PARAM_SAMPLE_RATE, String.valueOf(SAMPLE_RATE));
		userParameters.put(Constants.PARAM_STEP, String.valueOf(STEP));
		userParameters.put(Constants.PARAM_PERIOD, String.valueOf(period));
		userParameters.put(Constants.PARAM_START_TIME, String.valueOf(startTime));

		// Cria uma memória para o sensor
		// TODO Memória do sensor deve ser parametrizável
		String memoryName = agentName+":"+eventHandlerName;
		Memory memory = new AudioMemory();
		memory.start(envAgent, Constants.EVT_AUDIO, 5.0, 5.0, userParameters);
		memories.put(memoryName, memory);
		
		return userParameters;
	}
	
	// TODO Verificar problemas de concorrência!
	@Override
	public void processSense(Event evt) {

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
	
	@Override
	public void process() {
		
		long time_process = System.nanoTime();

		// TODO Ver se vamos trabalhar com milisegundos ou segundos
		double instant = (double)(startTime + workingFrame * period) / 1000;
//		file_perf.println("t = " + instant);

//		System.out.println("SENSORS = " + sensors.size() + " - ACTUATORS = " + actuators.size());
		for (Enumeration<String> s = sensors.keys(); s.hasMoreElements();) {
			
			String s_key = s.nextElement();
			String[] sensor = s_key.split(":");
			rcv_comp_pos = Vector.parse(sensors.get(s_key).get(Constants.PARAM_POSITION, "(0;0;0)"));

			// Cria o evento a ser enviado para o sensor
			Event evt = new Event();
			evt.destAgentName = sensor[0];
			evt.destAgentCompName = sensor[1];
			double[] buf = new double[CHUNK_SIZE];
			evt.objContent = buf;
			evt.instant = instant;
			evt.duration = (double)(CHUNK_SIZE * STEP);
			
			// Calculates the contribution of each sound source
			for (Enumeration<String> a = actuators.keys(); a.hasMoreElements();) {

				String a_key = a.nextElement();
				String pair = s_key + "<>" + a_key;
				src_comp_pos = Vector.parse(actuators.get(a_key).get(Constants.PARAM_POSITION, "(0;0;0)"));
				
				AudioMemory mem = (AudioMemory)memories.get(a_key);

				String[] actuator = a_key.split(":");
				
				// If it's the same agente, just copy
				if (actuator[0].equals(sensor[0])) {
					
					System.arraycopy((double[])mem.readMemory(instant, (double)(CHUNK_SIZE * STEP), TimeUnit.SECONDS), 0, buf, 0, CHUNK_SIZE);
					
				}
				// Else, simulates the propagation of sound
				else {
					
					double t, guess;
					
					// Gets the movement memory
					Memory mem_mov_src = (Memory)world.getEntityStateAttribute(actuator[0], "MOVEMENT");
					Memory mem_mov_rcv = (Memory)world.getEntityStateAttribute(sensor[0], "MOVEMENT");

					// Guess
					if (last_deltas.containsKey(pair)) {
						guess = last_deltas.get(pair);
					} else {
						// Only runs the first time
						MovementState src_state_old = (MovementState)mem_mov_rcv.readMemory(instant, TimeUnit.SECONDS);
						movLaw.changeState(src_state_old, instant, src_state);
//				    	System.out.println("src_state = " + src_state.position);
						
						MovementState rcv_state_old = (MovementState)mem_mov_src.readMemory(instant, TimeUnit.SECONDS);
						movLaw.changeState(rcv_state_old, instant, rcv_state);
//				    	System.out.println("rcv_state = " + rcv_state.position);

						// Adjusts the position according to the component relative position to the center of the agent
						src_state.position.add(src_comp_pos);
						rcv_state.position.add(rcv_comp_pos);
						
						double distance = src_state.position.getDistance(rcv_state.position);
						guess = distance / SPEED_SOUND;
//						System.out.println("initial guess for " + pair + " = " + guess);
					}

					// Finds the deltas for all the samples in the chunk, according to the chosen process mode
					double delta = 0.0, delta_i = 0.0, delta_f = 0.0;

					switch (mode) {
					case NORMAL:
						long start = System.nanoTime();
						// For each sample...
						for (int j = 0; j < CHUNK_SIZE; j++) {
							t = instant + (j * STEP);
							delta = newton_raphson(mem_mov_src, mem_mov_rcv, t, guess, 0.0, mem_mov_src.getPast());
							if (delta < 0.0) {
								System.err.println("[ERROR] delta = " + delta);
								delta = 0.0;
							}
							deltas[j] = delta;
							guess = delta;
						}
						break;
					case POL_INT:
						start = System.nanoTime();
						double[] xa = new double[NUMBER_OF_POINTS]; 
						double[] ya = new double[NUMBER_OF_POINTS]; 

						// calculates points for the polinomial interpolation
						xa[0] = instant;
						ya[0] = newton_raphson(mem_mov_src, mem_mov_rcv, xa[0], guess, 0.0, mem_mov_src.getPast());
						int samples_jump = CHUNK_SIZE / NUMBER_OF_POINTS;
						for (int i = 0; i < NUMBER_OF_POINTS-2; i++) {							
							xa[i] = instant + (i * samples_jump * STEP);
							ya[i] = newton_raphson(mem_mov_src, mem_mov_rcv, xa[i], guess, 0.0, mem_mov_src.getPast());
						}
						xa[NUMBER_OF_POINTS-1] = instant + ((CHUNK_SIZE-1) * STEP);
						ya[NUMBER_OF_POINTS-1] = newton_raphson(mem_mov_src, mem_mov_rcv, xa[NUMBER_OF_POINTS-2], delta_i, 0.0, mem_mov_src.getPast());

						// For each sample in this division...
						for (int i = 0; i < CHUNK_SIZE; i++) {
							
							t = instant + (i * STEP);
							
							delta = polint(xa, ya, t);
							deltas[i] = delta;

						}
						proc_time_2 = System.nanoTime() - start;
						break;
					case LIN_INT:
						start = System.nanoTime();
						samples_jump = CHUNK_SIZE / DIVISION_FACTOR;
						for (int i = 0; i < CHUNK_SIZE; i += samples_jump) {
							
							t = instant + (i * STEP);
							
							// first delta of the chunk
							delta_i = newton_raphson(mem_mov_src, mem_mov_rcv, t, guess, 0.0, mem_mov_src.getPast());
							// last delta of the chunk (if it's the last division of the chunk, gets the last sample)
							if (i + samples_jump >= CHUNK_SIZE) {
								delta_f = newton_raphson(mem_mov_src, mem_mov_rcv, (instant + ((CHUNK_SIZE-1) * STEP)), delta_i, 0.0, mem_mov_src.getPast());
							} else {
								delta_f = newton_raphson(mem_mov_src, mem_mov_rcv, (instant + ((i+samples_jump-1) * STEP)), delta_i, 0.0, mem_mov_src.getPast());
							}
	//						System.out.println("delta_i = " + delta_i + " - delta_f = " + delta_f);
	
							// TODO Verificar os valores de delta! Não pode ser menor que zero!!!
							double delta_step = (delta_f - delta_i) / (samples_jump - 1);
							
							// For each sample in this division...
							for (int j = 0; (i+j < CHUNK_SIZE && j < samples_jump); j++) {
								
								t = instant + ((i+j) * STEP);
								
								delta = delta_i + (delta_step * j);
								deltas[i+j] = delta;
								
							}
							
						}
						proc_time_3 = System.nanoTime() - start;
						break;
					default:
						break;
					}
					
					// Fills the buffer
					for (int i = 0; i < CHUNK_SIZE; i++) {
						t = instant + (i * STEP);
						double gain = Math.min(1.0, REFERENCE_DISTANCE / (REFERENCE_DISTANCE + ROLLOFF_FACTOR * ((delta * SPEED_SOUND) - REFERENCE_DISTANCE)));
						double value = 0.0;
						value = mem.readMemoryDouble(t-deltas[i], TimeUnit.SECONDS);
						buf[i] = buf[i] + (value * gain);
					}
					
					// Stores the last deltas for the next computation
					last_deltas.put(pair, delta);
				}
			}
			
			// Puts the newly created event in the output queue
			addOutputEvent(evt.destAgentName, evt.destAgentCompName, evt);
			
		}
		
		System.out.printf("AS time = %.3f \t(t = %.3f)\n", ((double)(System.nanoTime()-time_process)/1000000), instant);

	}

    private void function(MovementState src_state, MovementState rcv_state, double t, double delta) {
    	
    	if (src_state == null || rcv_state == null) {
			// Se é null, é porque o agente não existia nesse momento
    		System.err.println("WARNING: Tentou buscar amostra no futuro ou antes do início da simulação (" + (t - delta) + ")");
    		f_res[0] = 0; f_res[1] = 0;
    	}
    	
    	Vector q = rcv_state.position;
    	Vector p = src_state.position;
    	Vector v = src_state.velocity;
    	
    	f_res[0] 	= (q.magnitude * q.magnitude) - 2 * q.dotProduct(p) + (p.magnitude * p.magnitude) - (delta * delta * SPEED_SOUND * SPEED_SOUND);
    	p.copy(vec_aux);
    	vec_aux.subtract(q);
    	f_res[1] 	= 2 * v.dotProduct(vec_aux) - (2 * delta * SPEED_SOUND * SPEED_SOUND);

    }
    
    private double newton_raphson(Memory mem_src, Memory mem_rcv, double t, double initial_guess, double x1, double x2) {
    	    	
    	double dx, dx_old, rts, xl, xh, temp;
    	MovementState rcv_state_old, src_state_old;

//    	System.out.println("newton() - t = " + t + " - guess = " + initial_guess);
    	
		rcv_state_old = (MovementState)mem_rcv.readMemory(t, TimeUnit.SECONDS);
		movLaw.changeState(rcv_state_old, t, rcv_state);
		rcv_state.position.add(rcv_comp_pos); // Component relative position

		src_state_old = (MovementState)mem_src.readMemory(t-x1, TimeUnit.SECONDS);
		movLaw.changeState(src_state_old, t-x1, src_state);
		src_state.position.add(src_comp_pos); // Component relative position
    	function(src_state, rcv_state, t, x1);
    	fl[0] = f_res[0]; fl[1] = f_res[1];
    	
		src_state_old = (MovementState)mem_src.readMemory(t-x2, TimeUnit.SECONDS);
		movLaw.changeState(src_state_old, t-x2, src_state);
		src_state.position.add(src_comp_pos); // Component relative position
		function(src_state, rcv_state, t, x2);
    	fh[0] = f_res[0]; fh[1] = f_res[1];
    	
    	if ((fl[0] > 0.0 && fh[0] > 0.0) || (fl[0] < 0.0 && fh[0] < 0.0)) {		
    		System.err.println("Root must be bracketed in rtsafe");
    		return -1;
    	}
		if (fl[0] == 0.0) {
			return x1;
		}
		if (fh[0] == 0.0) { 
			return x2;
		}
		// Orient the search so that f(xl) < 0.0
		if (fl[0] < 0.0) {
			xl = x1;
			xh = x2;
		} else {
			xh = x1;
			xl = x2;
		}
		// Initialize the guess for root, the stepsize before last, and the last step
    	rts = initial_guess;
		dx_old = Math.abs(x2-x1);
		dx = dx_old;
		src_state_old = (MovementState)mem_src.readMemory(t-rts, TimeUnit.SECONDS);
//		System.out.printf("fui buscar o instante %f e retornou %f\n", t-rts, src_state_old.instant);
		movLaw.changeState(src_state_old, t-rts, src_state);
//		System.out.printf("src_state(%f)=%f\n", t-rts, src_state.position.getValue(0));
		src_state.position.add(src_comp_pos); // Component relative position
		function(src_state, rcv_state, t, rts);
    	f[0] = f_res[0]; f[1] = f_res[1];
    	if (f == null) {
    		System.err.println("WARNING: newton tried to search a sample before the begining of the simulation or in the future (" + (t - rts) + ")");
    		return 0.0;
    	}
    	// Loop over allowed iterations
		boolean found = false;
		for (int i = 0; i < MAX_ITERATIONS; i++) {
			// Bisect if Newton out of range, or not decreasing fast enough
			if ((((rts-xh)*f[1]-f[0])*((rts-xl)*f[1]-f[0]) >= 0.0)
				|| (Math.abs(2.0*f[0]) > Math.abs(dx_old*f[1]))) {
				dx_old = dx;
				dx = 0.5 * (xh - xl);
				rts = xl + dx;
				// Change in root is negligible
				if (xl == rts) {
					found = true;
					break;
				}
			// Newton step acceptable. Take it.
			} else {
				dx_old = dx;
	        	dx = f[0] / f[1];
	        	temp = rts;
	        	rts -= dx;
	        	if (temp == rts) {
					found = true;
					break;

	        	}
			}
			// Convergence criterion
        	if (Math.abs(dx) < EPSILON) {
				found = true;
				break;
        	}
        	// The one new function evaluation per iteration
    		src_state_old = (MovementState)mem_src.readMemory(t-rts, TimeUnit.SECONDS);
//    		System.out.printf("fui buscar o instante %f e retornou %f\n", t-rts, src_state_old.instant);
    		movLaw.changeState(src_state_old, t-rts, src_state);
//    		System.out.printf("src_state(%f)=%f\n", t-rts, src_state.position.getValue(0));
    		src_state.position.add(src_comp_pos); // Component relative position
    		function(src_state, rcv_state, t, rts);
        	f[0] = f_res[0]; f[1] = f_res[1];
    		// Maintain the bracket on the root
    		if (f[0] < 0.0) {
    			xl = rts;
    		} else {
    			xh = rts;
    		}
    	}

		if (!found) {
 			// TODO Está chegando nesse ponto em alguns casos!!!
//			System.err.println("WARNING: Maximum number of iterations exceeded in rtsafe (t = " + t + ") - delta = " + rts);
		}
    	
		return rts;
    }

    private double polint(double[] xa, double[] ya, double x) {
    	
    	double y, dy;
    	int ns = 0;
    	double den, dif, dift, ho, hp, w;
    	double[] c, d;
    	
    	dif = Math.abs(x-xa[0]);
    	// TODO Otimizar e criar antes
    	int n = xa.length;
    	c = new double[n];
    	d = new double[n];
    	
    	// Here we find the index ns of the closest table entry 1 = true
    	for (int i = 0; i < n; i++) {
    		dift = Math.abs(x-xa[i]);
    		if (dift < dif) {
				ns = i;
				dif = dift;
			}
			// Initializes the tableau of c's and d's
			c[i] = ya[i];
			d[i] = ya[i];
		}
    	y = ya[ns--];
    	for (int m = 1; m < n; m++) {
			for (int i = 0; i < n-m; i++) {
				ho = xa[i] - x;
				hp = xa[i+m] - x;
				w = c[i+1] - d[i];
				if ((den=ho-hp) == 0.0) {
					System.err.println("Error in routine polint!");
				}
				den = w / den;
				d[i] = hp * den;
				c[i] = ho * den;
			}
			y += (dy = (2 *(ns+1) < (n-m) ? c[ns+1] : d[ns--]));
		}
    	
    	return y;
    	
    }

}
