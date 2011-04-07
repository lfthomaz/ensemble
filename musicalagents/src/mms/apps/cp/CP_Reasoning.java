package mms.apps.cp;

import java.util.ArrayList;

import mms.Actuator;
import mms.Command;
import mms.Constants;
import mms.EventHandler;
import mms.MusicalAgent;
import mms.Parameters;
import mms.Reasoning;
import mms.Sensor;
import mms.clock.TimeUnit;
import mms.memory.Memory;
import mms.memory.MemoryException;
import mms.processing.Process;
import mms.processing.ProcessFactory;
import mms.processing.ProcessFactory.AudioOperation;
import mms.tools.AudioInputFile;

public class CP_Reasoning extends Reasoning {

	// Audio
	Actuator 	mouth;
	Memory 		mouthMemory;
	Sensor 		ear;
	Memory 		earMemory;
	int 		chunk_size;

	// Movement
	Actuator	legs;
	Memory 		legsMemory;
	Sensor 		eyes;
	Memory 		eyesMemory;
	
	// Reasoning state
	enum ReasoningState {
		NOT_DEFINED,
		LISTENING,
		ANALYSING,
		PLAYING,
		ERROR
	}
	ReasoningState state = ReasoningState.NOT_DEFINED;
	
	// Audio file - wavetable
	private AudioInputFile 	in;
	private float 			sampleRate;
	private double[] 		wavetable;
	
	// Audio Processor - Onset Detection
	private Process 		onsetproc;
	
	// Common Variables
	int 	number_beats;
	long 	frame_duration;
	long 	measure_duration;
	long 	beat_duration;
	boolean master = false;
	
	// Play variables
	long 		start_time;
	long 		start_playing_time;
	boolean[] 	pattern;
	ArrayList<Long> beats = new ArrayList<Long>();
	int 		next_sample_play = 0;
	int 		measure_counter = 0;
	int 		phase = 0;
	int 		slide = 0;
	int 		last_phase = 0;
	int 		actual_phase = 0;
	
	// Listening Variables;
	ArrayList<Long> detected_beats_time = new ArrayList<Long>();
	ArrayList<Double> detected_beats_energy = new ArrayList<Double>();
	
	double error = 0.05; // porcentagem permitida na diferença entre o onset e o suposto lugar da batida
	
	// Buffers de trabalho
	private double[] chunk;
	
	protected boolean init() {
		
		// Gets and checks arguments from the KB
		String arg_filename = getAgent().getKB().readFact("wavetable");
		if (arg_filename == null) {
			System.err.println("wavetable fact does not exists!");
			return false;
		}
		String arg_role 	= getAgent().getKB().readFact("role");
		if (arg_role == null || !(arg_role.equals("master") || arg_role.equals("slave"))) {
			System.err.println("'role' fact does not exists or is neither 'master', nor 'slave'!");
			return false;
		}
		if (arg_role.equals("master")) {
			master = true;
		}
		
		String arg_bpm 		= getAgent().getKB().readFact("bpm");
		try {
			beat_duration = 60000 / Integer.parseInt(arg_bpm);
		} catch (Exception e) {
			System.err.println("'bpm' argument does not exists or is not a valid number! Using 120 as default.");
			beat_duration = 60000 / 120;
		}
		String arg_pattern 	= getAgent().getKB().readFact("pattern");
		
		if (arg_role.equals("slave")) {
			String arg_phase 	= getAgent().getKB().readFact("phase");
			try {
				phase = Integer.parseInt(arg_phase);
			} catch (Exception e) {
				System.err.println("'phase' argument is not a valid number!");
			}
			String arg_slide 	= getAgent().getKB().readFact("slide");
			try {
				slide = Integer.parseInt(arg_slide);
			} catch (Exception e) {
				System.err.println("'slide' argument is not a valid number!");
			}
		}
		
		// Opens the audio file and imports it as a wavetable
		try {
			in = new AudioInputFile(arg_filename, true);
			long samples = in.getNumberSamples();
			sampleRate = in.getSampleRate();
			// TODO Fazer checagens de tamanho do arquivo
			wavetable = in.readNextChunk((int)samples);
		} catch (Exception e) {
			getAgent().logger.severe("[" + getName() + "] " + "Error in opening the file " + arg_filename);
			return false;
		}
		
		// Configures the agente after its role
		if (arg_role.equals("master")) {
		
			// If the pattern was not given, creates one randomnly
			if (arg_pattern == null) {
				// From 5 to 16 beats
				number_beats = 5 + (int)(Math.round(Math.random()*11));
				pattern = new boolean[number_beats];
				pattern[0] = true; // Primeira batida sempre deve existir
				String str = "[MASTER] Random pattern = [1";
				for (int i = 1; i < pattern.length; i++) {
					pattern[i] = (int)(Math.round(Math.random())) != 0 ? Boolean.TRUE : Boolean.FALSE;
					str = str + (pattern[i] ? "1" : "0");
				}
				System.out.println(str+"]");
			} else {
				char[] char_pattern = getAgent().getKB().readFact("pattern").toCharArray();
				number_beats = char_pattern.length;
				pattern = new boolean[number_beats];
				for (int i = 0; i < char_pattern.length; i++) {
					if (char_pattern[i] == '1') {
						pattern[i] = true;
					}
				}
			}
			
			// Builds the time array of the beats
			measure_duration = beat_duration * number_beats;
			for (int i = 0; i < pattern.length; i++) {
				if (pattern[i]) {
					beats.add(i * beat_duration);
				}
			}
			
			System.out.println("\twavetable_length = " + wavetable.length);
			System.out.println("\tbeat_duration = " + beat_duration);
			System.out.println("\tmeasure_duration = " + measure_duration);
			System.out.print("\tbeats = [");
			for (int i = 0; i < beats.size(); i++) {
				System.out.print(" " + beats.get(i));
			}
			System.out.println(" ]");

			state = ReasoningState.PLAYING;
			
		} else if (getAgent().getKB().readFact("role").equals("slave")) {

			// Creates de Onset Processor
			Parameters onset_args = new Parameters();
			onset_args.put("frame_size", "512");
			onset_args.put("sample_rate", "44100.0");
			onset_args.put("onset_output", "sample");
			onsetproc = ProcessFactory.createAudioProcessor(AudioOperation.ONSET_DETECTION, onset_args);

			state = ReasoningState.ANALYSING;
			
		}
		
		return true;
		
	}
	
	protected void eventHandlerRegistered(EventHandler evtHdl) {
		
		// Checar se é um atuador de som e adicionar na lista
		if (evtHdl instanceof Actuator && evtHdl.getEventType().equals(Constants.EVT_AUDIO)) {
			
			mouth = (Actuator)evtHdl;
			mouth.registerListener(this);
			mouthMemory = getAgent().getKB().getMemory(mouth.getName());
			chunk_size = Integer.parseInt(mouth.getParameter(Constants.PARAM_CHUNK_SIZE, "0"));
			start_time = Integer.parseInt(mouth.getParameter(Constants.PARAM_START_TIME, "0"));
			chunk = new double[chunk_size];
			frame_duration = (long)Math.floor((float)chunk_size / sampleRate * 1000);
			start_playing_time = start_time + 2*frame_duration + (long)(Math.random()*(double)frame_duration);
			System.out.println("start_playing_time = " + start_playing_time);
			
		} else if (evtHdl instanceof Sensor && evtHdl.getEventType().equals(Constants.EVT_AUDIO)) {
			
			ear = (Sensor)evtHdl;
			ear.registerListener(this);
			earMemory = getAgent().getKB().getMemory(ear.getName());
			
		} else if (evtHdl instanceof Actuator && evtHdl.getEventType().equals(Constants.EVT_MOVEMENT)) {
			
			legs = (Actuator)evtHdl;
			legs.registerListener(this);
			legsMemory = getAgent().getKB().getMemory(legs.getName());
				
		} else if (evtHdl instanceof Sensor && evtHdl.getEventType().equals(Constants.EVT_MOVEMENT)) {
			
			eyes = (Sensor)evtHdl;
			eyes.registerListener(this);
			eyesMemory = getAgent().getKB().getMemory(eyes.getName());
				
		}
		
	}
	
	public void needAction(Actuator sourceActuator, double instant, double duration) {

		double start = System.currentTimeMillis();
		
		// Limpa chunk de saída
		for (int i = 0; i < chunk.length; i++) {
			chunk[i] = 0.0f;
		}

		switch (state) {

		case PLAYING:
			
			// Calcula os tempos referentes ao ínicio e fim do frame (em ms)
			if (master) {
				System.out.println("instant = " + instant);
			}
			
			if (!(start_playing_time > instant * 1000 && start_playing_time > (instant + duration) * 1000)) {
			
				long ti = ((long)(instant * 1000) - start_playing_time) % measure_duration;
				long tf = (ti + frame_duration) % measure_duration;
				
				// Coloca o resto da wavetable que sobrou da último frame
				// TODO VERIFICAR PARA O CASO DO WAVETABLE OCUPAR MAIS DE 2 FRAMES ou 2 batidas com intervalo pequeno entre elas
				if (next_sample_play > 0) {
					int samples_to_copy = Math.min(chunk_size, wavetable.length - next_sample_play);
					for (int j = 0; j < samples_to_copy; j++) {
						chunk[j] = wavetable[next_sample_play++];
					}
					if (next_sample_play >= wavetable.length) {
						next_sample_play = 0;
					}
				}
				
				// Verificar se existe alguma batida entre [ti,ti+frame_duration[
				for (int i = 0; i < beats.size(); i++) {
					long beat_value = beats.get(i);
					if (beat_value >= ti && beat_value < ti+frame_duration) {
						// Procura o local certo de inserir a wavetable neste frame
						long tm = beat_value - ti;
						int start_sample = (int)(Math.floor(sampleRate * tm)/1000);
						int samples_to_copy = Math.min(chunk_size - start_sample, wavetable.length);
	//					System.out.println("samples_to_copy = " + samples_to_copy);
						for (int j = 0; j < samples_to_copy; j++) {
							chunk[j + start_sample] += (i==0 ? 2 : 1) * wavetable[j];
							next_sample_play++;
						} 
						// Se não coube tudo, marca a posição para copiar no próximo frame
						if (next_sample_play == wavetable.length) {
							next_sample_play = 0;
						}

					}
				}
				
				// Mudança de compasso dentro deste frame
				if (tf < ti) {
					// atualiza o contador
					measure_counter++;
//					System.out.println("Tem começo de compasso!!!");
					// Fazer a mudança de fase, caso necessário
					if (!master && slide != 0 && measure_counter % slide == 0) {
						System.out.println("slide = " + slide + " measure_counter = " + measure_counter);
						actual_phase = (actual_phase + phase) % number_beats;
						System.out.println("actual_phase = " + actual_phase);
						// Reconstruir o array the beats
						int beats_size = beats.size();
						beats.clear();
						for (int i = 0; i < beats_size; i++) {
							int index = (i + actual_phase) % beats_size;
							long beat = detected_beats_time.get(index) - detected_beats_time.get(0);
							beats.add((beat + measure_duration - (actual_phase * beat_duration)) % measure_duration);
						}
						System.out.print("\tbeats = [");
						for (int i = 0; i < beats.size(); i++) {
							System.out.print(" " + beats.get(i));
						}
						System.out.println(" ]");
					}
					// Verificar quais batidas pertencem
					for (int i = 0; i < beats.size(); i++) {
						long beat_value = beats.get(i);
						if (beat_value >= 0 && beat_value < tf) {
							// Procura o local certo de inserir a wavetable neste frame
							long tm = (measure_duration - ti) + beat_value;
							int start_sample = (int)(Math.floor(sampleRate * tm)/1000);
							int samples_to_copy = Math.min(chunk_size - start_sample, wavetable.length);
		//					System.out.println("samples_to_copy = " + samples_to_copy);
							for (int j = 0; j < samples_to_copy; j++) {
								chunk[j + start_sample] += (i==0 ? 2 : 1) * wavetable[j];
								next_sample_play++;
							} 
							// Se não coube tudo, marca a posição para copiar no próximo frame
							if (next_sample_play == wavetable.length) {
								next_sample_play = 0;
							}

						}
					}
				}
				
			}		
			
			// Armazena o chunk de saída na memória e atua
			try {
				mouthMemory.writeMemory(chunk, instant, duration, TimeUnit.SECONDS);
				mouth.act();
			} catch (MemoryException e1) {
				MusicalAgent.logger.warning("[" + getAgent().getLocalName() + ":" + getName() + "] " + "Não foi possível armazenar na memória");
			}
			
			break;
	
		}

//		System.out.println("REAS time = " + (System.currentTimeMillis() - start));
	}
	
	@Override
	public void newSense(Sensor sourceSensor, double instant, double duration) {

		if (sourceSensor == ear) {
		
			// Se estiver no modo de escuta, analisa o frame para achar as batidas
			switch (state) {
			
			case ANALYSING:
				
				// Pega o evento sonoro da base
				double[] buf = (double[])earMemory.readMemory(instant, duration, TimeUnit.SECONDS);
	
				// Onset
				Parameters onset_args = new Parameters();
				onset_args.put("start_instant", String.valueOf(instant));
				
				if (onsetproc != null) {
					Object out = onsetproc.process(onset_args, buf);
					
					// Beats
					if (out != null && out instanceof double[]) {
						double[] onset = (double[])out;
						if (onset.length > 0) {
							for (int i = 0; i < onset.length; i++) {
								// Add beat
								double beat = onset[i];
								detected_beats_time.add((long)((instant + (beat/sampleRate)) * 1000));
							}
						}
					
						// ------
						// Updates the energy of each onset
						int ti = 0;
						int tf = buf.length;
						// If there is an older onset
						if (detected_beats_energy.size() > 0) {
							ti = 0;
							if (onset.length > 0) {
								tf = (int)onset[0];
							}
							if (ti != tf) {
								double energy = detected_beats_energy.get(detected_beats_energy.size()-1);
								for (int j = ti; j < tf; j++) {
									energy = energy + (buf[j] * buf[j]);
								}
		//						System.out.printf("rms residual = %.3f (ti=%d tf=%d)\n", energy, ti, tf);
								detected_beats_energy.set(detected_beats_energy.size()-1, energy);
							}
						}
						// For each new onset
						for (int i = 0; i < onset.length; i++) {
							ti = (int)onset[i];
							if (i < onset.length-1) {
								tf = (int)onset[i+1];
							} else {
								tf = buf.length;
							}
							double energy = 0;
							for (int j = ti; j < tf; j++) {
								energy = energy + (buf[j] * buf[j]);
							}
		//					System.out.printf("rms novo = %.3f (ti=%d tf=%d)\n", energy, ti, tf);
							detected_beats_energy.add(energy);
						}
					}
				}
				
				String str = "time = [ ";
				for (int i = 0; i < detected_beats_time.size(); i++) {
					str = str + String.format("%d ", detected_beats_time.get(i));
				}
				System.out.println(str+"]");
				
				break;
				
			}
		
		} else if (sourceSensor == eyes) {
			
			String str = (String)eyesMemory.readMemory(instant, duration, TimeUnit.SECONDS);
			System.out.println("EYES: " + str);
			
		}
		
	}
	
	public void process() throws Exception {

		switch (state) {

		case ANALYSING:

			// If there's more than one beat, we can start the search!
			if (detected_beats_energy.size() > 1) {
				double first_beat_energy = detected_beats_energy.get(0);
				int size = detected_beats_energy.size();
				for (int i = 1; i < size; i++) {
					// Compares the energy of the first beat with the others
					// If the difference is within an
					double max = Math.max(first_beat_energy, detected_beats_energy.get(i));
					double min = Math.min(first_beat_energy, detected_beats_energy.get(i));
					if ((max-min)/min < error) {
						long pattern_start_time = detected_beats_time.get(0);
						long pattern_repetiton_time = detected_beats_time.get(i);
						System.out.printf("Pattern found at beat = %d - t = %d\n", i, pattern_start_time);
						measure_duration = pattern_repetiton_time - pattern_start_time;
						System.out.println("Measure duration = " + measure_duration);
						number_beats = (int)Math.round(measure_duration/beat_duration);
						System.out.println("Number of beats = " + number_beats);
						System.out.print("\tbeats = [");
						for (int j = 0; j < i; j++) {
							long beat = detected_beats_time.get(j) - pattern_start_time;
							beats.add(beat);
							System.out.print(" " + beat);
						}
						System.out.println(" ]");
						// Starts at the third repetition
						start_playing_time = pattern_repetiton_time + measure_duration;
						System.out.println("Start playing time = " + start_playing_time);
						// Changes agent's state
						state = ReasoningState.PLAYING;
						
					}
				}
			}
			
			break;
			
		case PLAYING:
			
			// Movimentação
			if (!master && legs != null) {
				if (last_phase != actual_phase) {
					last_phase = actual_phase;
					// Ouve mudança de fase -> movimentar
					String x = actual_phase == 0 || actual_phase == 3 ? "20" : "-20"; 
					String y = actual_phase == 0 || actual_phase == 1 ? "20" : "-20";
					String cmd = "TELEPORT :pos ("+x+";"+y+";0)";
					legsMemory.writeMemory(cmd);
					legs.act();
				}
			}
			
			break;

		}

	}
	
}
