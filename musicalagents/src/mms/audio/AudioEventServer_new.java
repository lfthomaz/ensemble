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
import mms.movement.MovementState;
import mms.world.Vector;
import mms.world.World;
import mms.world.law.Law;

public class AudioEventServer_new extends EventServer {

	// Log
	public static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	long time_1 = 0;
	long time_2 = 0;
	long time_3 = 0;
	long time_4 = 0;
	long time_5 = 0;
	long time_6 = 0;
	
	long calls_1 = 0;
	long calls_2 = 0;
	long calls_3 = 0;
	long calls_4 = 0;
	long calls_5 = 0;
	long calls_6 = 0;

	private long time_newton = 0;
	private long time_newton_1 = 0;
	private long time_newton_2 = 0;
	private long time_newton_3 = 0;
	private long time_newton_4 = 0;
	private long time_newton_5 = 0;
	private long time_newton_6 = 0;
	private long time_function = 0;
	private long calls_function = 0;
	
	//---- WORK VARIABLES ----
	// Newton function's variables 
	double[] f_res	= new double[2];
	double[] f		= new double[2];
	double[] fl	 	= new double[2];
	double[] fh 	= new double[2];
	// States
	MovementState rcv_state;
	MovementState src_state;
	
	// Utilizado para comparar o tempo (ajustar de acordo com a precis‹o desejada), em segundos
	private final double 	EPSILON 		= 1E-10;
	private final int 		MAX_ITERATIONS 	= 10;

	// TODO Tornar parametrizável os valores utilizados em AudioEventServer
	private double	SPEED_SOUND			= 343.3; // speed of sound (m/s)
	private double 	REFERENCE_DISTANCE 	= 1.0;
	private double 	ROLLOFF_FACTOR 		= 1.0;
    private int 	SAMPLE_RATE 		= 44100;
    private double 	STEP 				= 1 / SAMPLE_RATE;
    private int 	CHUNK_SIZE 			= 4410;
	
    // TODO Ver como armazenar o last_delta de cada source
    private HashMap<String, Double> last_deltas = new HashMap<String, Double>();
    
    // Table that stores sent audio chunks
    private HashMap<String, Memory> memories = new HashMap<String, Memory>();

	// Descrição do mundo
	private World world;
	
	private Law movLaw;
	
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
//		System.out.printf("%d %f %d\n", SAMPLE_RATE, STEP, CHUNK_SIZE);
		
		this.world = envAgent.getWorld();
		
		MovementState rcv_state = new MovementState(world.dimensions);
		MovementState src_state = new MovementState(world.dimensions);
		
		// Gets the Movement Law
		this.movLaw = world.getLaw("MOVEMENT");
		
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
		// TODO Memória do sensor deveria ser parametrizável!!!
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

//		System.out.println("SENSORS = " + sensors.size() + " - ACTUATORS = " + actuators.size());
		for (Enumeration<String> s = sensors.keys(); s.hasMoreElements();) {
			
			long start = System.nanoTime();
			
			// Cria o evento a ser enviado para o sensor
			String s_key = s.nextElement();
//			AudioEvent evt = new AudioEvent(CHUNK_SIZE);
			Event evt = new Event();
			String[] sensor = s_key.split(":");
			evt.destAgentName = sensor[0];
			evt.destAgentCompName = sensor[1];
			double[] buf = new double[CHUNK_SIZE];
			evt.objContent = buf;
			evt.instant = instant;
			evt.duration = (double)(CHUNK_SIZE * STEP);
			
			time_1 = time_1 + (System.nanoTime() - start); calls_1++;

//			System.out.println("ACTUATORS = " + actuators.size());
			// Calcula a contribuição de cada fonte sonora
			for (Enumeration<String> a = actuators.keys(); a.hasMoreElements();) {
//
				start = System.nanoTime();

				String a_key = a.nextElement();

				String pair = s_key + "<>" + a_key;
//				System.out.println("pair = " + pair);
				
				Memory mem = memories.get(a_key);
				
				time_3 = time_3 + (System.nanoTime() - start); calls_3++;

				String[] actuator = a_key.split(":");
				
				// Se for o próprio agente, reproduzo o som sem modificações (ou posso não ouvir)
				if (actuator[0].equals(sensor[0])) {
					start = System.nanoTime();
					System.arraycopy((double[])mem.readMemory(instant, (double)(CHUNK_SIZE * STEP), TimeUnit.SECONDS), 0, buf, 0, CHUNK_SIZE);
					time_5 = time_5 + (System.nanoTime() - start); calls_5++;
				}
				// Caso contrário, simulo a propagação
				else {
					double t, guess;
					// Percorrer todos os samples do chunk a ser preenchido
					long start_2 = System.nanoTime();
					for (int j = 0; j < CHUNK_SIZE; j++) {
						
//						start = System.nanoTime();

	//					System.out.println("sample " + j);
						t = instant + (j * STEP);
						if (last_deltas.containsKey(pair)) {
							guess = Double.valueOf(last_deltas.get(pair));
						} else {
							// Só roda na primeira vez!
							world.getEntityState(src_state, actuator[0], t, TimeUnit.SECONDS);
//					    	System.out.println("src_state = " + src_state.position);
							world.updateStateMovLaw(src_state, instant);
							world.getEntityState(rcv_state, sensor[0], t, TimeUnit.SECONDS);
//					    	System.out.println("rcv_state = " + rcv_state.position);
							world.updateStateMovLaw(rcv_state, instant);
							double distance = src_state.position.getDistance(rcv_state.position);
							guess = distance / SPEED_SOUND;
//							System.out.println("initial guess for " + pair + " = " + guess);
						}
						
//						time_4 = time_4 + (System.nanoTime() - start); calls_4++;
						
	//					System.out.println(a_key + " " + s_key + " " + t + " " + guess + " 0.0 " + (t-0.1) );
//						try {
//							double value = 0.0;
//							double gain = 0.0;
//							long time = System.currentTimeMillis();
//							// TODO Quanto deve ser o delta máximo? Aqui deixamos 10 segundos
//							double delta = newton_raphson(actuator[0], sensor[0], t, guess, 0.0, 10.0);
//	//						System.out.printf("[%s-%s] t = %f - delta = %f\n", sensor[0], actuator[0], t, delta);
//							long elapsed_time = System.currentTimeMillis() - time;
//							time_newton = time_newton + elapsed_time;
////							if (elapsed_time > 1) {
////								System.out.println("WARNING_NEWTON: " + elapsed_time + " ms at t = " + t);
////							}
//							// TODO decidir um jeito melhor de devolver um delta inválido do que -1.0
//							if (delta < 0.0) {
//									System.err.println("[ERROR] delta = " + delta);
//									buf[j] = 0.0;
//							} else {
//								gain = Math.min(1.0, REFERENCE_DISTANCE / (REFERENCE_DISTANCE + ROLLOFF_FACTOR * ((delta * SPEED_SOUND) - REFERENCE_DISTANCE)));
////								gain = 0.0;
//								value = (Double)mem.readMemory(t-delta, TimeUnit.SECONDS);
//	//							System.out.println("f(" + t + " / " + (t - delta)+ ") = " + value + " - gain = " + gain);
//							}
//							buf[j] = buf[j] + (value * gain);
//							last_deltas.put(pair, delta);
//						} catch (Exception e) {
//							e.printStackTrace();
//							System.exit(-1);
//						}
					}
					time_6 = time_6 + (System.nanoTime() - start_2); calls_6++;
				}
			}			
			
//			start = System.nanoTime();

			// Coloca o evento criado na fila de resposta
			addOutputEvent(evt.destAgentName, evt.destAgentCompName, evt);
			
//			time_2 = time_2 + (System.nanoTime() - start); calls_2++;
			
		}
		
//		System.out.println("process() time = " + (System.currentTimeMillis()-time_process) + " - newton = " + time_newton + "/" 
//				+ time_newton_1 + "/" + time_newton_2 + "/"
//				+ time_newton_3 + "/" + time_newton_4 + "/"
//				+ time_newton_5
//				+ " - function = " + time_function + " - calls = " + calls_function);
//		System.out.println("HashMap size = " + ((World3D)world.;
//		time_newton = 0;
//		time_newton_1 = 0;
//		time_newton_2 = 0;
//		time_newton_3 = 0;
//		time_newton_4 = 0;
//		time_newton_5 = 0;
//		time_newton_6 = 0;
//		time_function = 0;
//		calls_function = 0;
//		
//		System.out.println("\tcalls = " + world.calls + " - time = " + world.time_1 + "/" + world.time_2);
//		world.calls = 0;
//		world.time_1 = 0;
//		world.time_2 = 0;
		
		System.out.printf("elapsed time = %.3f\n", ((double)(System.nanoTime()-time_process)/1000000));
		System.out.printf("time_1 = %.3f \t\tcalls_1 = %d\n", ((double)time_1/1000000), calls_1);
		System.out.printf("time_2 = %.3f \t\tcalls_2 = %d\n", ((double)time_2/1000000), calls_2);
		System.out.printf("time_3 = %.3f \t\tcalls_3 = %d\n", ((double)time_3/1000000), calls_3);
		System.out.printf("time_4 = %.3f \t\tcalls_4 = %d\n", ((double)time_4/1000000), calls_4);
		System.out.printf("time_5 = %.3f \t\tcalls_5 = %d\n", ((double)time_5/1000000), calls_5);
		System.out.printf("time_6 = %.3f \t\tcalls_6 = %d\n", ((double)time_6/1000000), calls_6);
		time_1 = 0; calls_1 = 0;
		time_2 = 0; calls_2 = 0;
		time_3 = 0; calls_3 = 0;
		time_4 = 0; calls_4 = 0;
		time_5 = 0; calls_5 = 0;
		time_6 = 0; calls_6 = 0;
		
	}

    public void function(MovementState src_state, MovementState rcv_state, double t, double delta) {
    	
		calls_function++;
    	
//    	System.out.println("function(" + t + "," + delta + ")");
//
//    	System.out.println("getSourceState("+source_name+","+(t - delta)+")");
    	
    	if (src_state == null || rcv_state == null) {
			// Se é null, é porque o agente não existia nesse momento
    		System.err.println("WARNING: Tentou buscar amostra no futuro ou antes do início da simulação (" + (t - delta) + ")");
    		f_res[0] = 0; f_res[1] = 0;
    	}
    	
    	// TODO Aqui é o problema, pq a gente está usando o nome completo do componente, mas o mundo armazena só o nome do agente!!!
    	Vector q = rcv_state.position;
    	Vector p = src_state.position;
    	Vector v = src_state.velocity;
    	long time_elapsed = System.currentTimeMillis();
    	f_res[0] 	= (q.magnitude * q.magnitude) - 2 * q.dotProduct(p) + (p.magnitude * p.magnitude) - (delta * delta * SPEED_SOUND * SPEED_SOUND);
    	f_res[1] 	= 2 * v.dotProduct(q.subtract(p)) - (2 * delta * SPEED_SOUND * SPEED_SOUND);
    	time_function = time_function + (System.currentTimeMillis() - time_elapsed);

//    	System.out.println("--- t = " + t + " delta = " + delta + " ---");
//    	System.out.println("\tq = " + q);
//    	System.out.println("\tp = " + p);
//    	System.out.println("\tv = " + v);
//    	System.out.println("\trtn = " + rtn);
    	
    	
//		return res; 

    }
    
    public double newton_raphson(String source_name, String receiver_name, double t, double initial_guess, double x1, double x2) {
    	
//    	double[] f, fl, fh;
    	double dx, dx_old, rts, xl, xh, temp;

    	long time_elapsed = System.currentTimeMillis();
    	
    	world.getEntityState(rcv_state, receiver_name, t, TimeUnit.SECONDS);
    	time_newton_1 = time_newton_1 + (System.currentTimeMillis() - time_elapsed);
    	time_elapsed = System.currentTimeMillis();
    	world.updateStateMovLaw(rcv_state, t);
    	time_newton_2 = time_newton_2 + (System.currentTimeMillis() - time_elapsed);


//    	System.out.println("newton_raphson("+source_name+","+receiver_name+","+t+","+initial_guess+","+x1+","+x2+")");
    	
//		long time = System.currentTimeMillis();
    	world.getEntityState(src_state, source_name, t-x1, TimeUnit.SECONDS);
    	world.updateStateMovLaw(src_state, t-x1);
//    	System.out.println("2 " + source_name + " (" + (t-x1) + ") = " + src_state.position);
    	function(src_state, rcv_state, t, x1);
    	fl[0] = f_res[0]; fl[1] = f_res[1];
    	world.getEntityState(src_state, source_name, t-x2, TimeUnit.SECONDS);
    	world.updateStateMovLaw(src_state, t-x2);
		function(src_state, rcv_state, t, x2);
    	fh[0] = f_res[0]; fh[1] = f_res[1];
    	time_elapsed = System.currentTimeMillis();
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
    	time_newton_3 = time_newton_3 + (System.currentTimeMillis() - time_elapsed);
    	time_elapsed = System.currentTimeMillis();
    	world.getEntityState(src_state, source_name, t-rts, TimeUnit.SECONDS);
    	world.updateStateMovLaw(src_state, t-rts);
		function(src_state, rcv_state, t, rts);
    	f[0] = f_res[0]; f[1] = f_res[1];
    	if (f == null) {
    		System.err.println("WARNING: Tentou buscar amostra no futuro ou antes do início da simulação (" + (t - rts) + ")");
    		return 0.0;
    	}
    	time_newton_4 = time_newton_4 + (System.currentTimeMillis() - time_elapsed);
    	time_elapsed = System.currentTimeMillis();
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
//					return rts;
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
//	        		return rts;
					found = true;
					break;

	        	}
			}
			// Convergence criterion
        	if (Math.abs(dx) < EPSILON) {
//        		return rts;
				found = true;
				break;
        	}
        	// The one new function evaluation per iteration
        	world.getEntityState(src_state, source_name, t-rts, TimeUnit.SECONDS);
        	world.updateStateMovLaw(src_state, t-rts);
    		function(src_state, rcv_state, t, rts);
        	f[0] = f_res[0]; f[1] = f_res[1];
    		// Maintain the bracket on the root
    		if (f[0] < 0.0) {
    			xl = rts;
    		} else {
    			xh = rts;
    		}
    	}

    	time_newton_5 = time_newton_5 + (System.currentTimeMillis() - time_elapsed);

		if (found) {
//			System.out.println("Newton found the root = " + rts);
		} else {
			// TODO Está chegando nesse ponto em alguns casos!!!
			System.err.println("WARNING: Maximum number of iterations exceeded in rtsafe for " + source_name + "<>" + receiver_name + " (t = " + t + ") - delta = " + rts);
		}
    	
		return rts;
    }

}
