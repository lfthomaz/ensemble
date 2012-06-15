package ensemble.apps.pp;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;

import org.boris.jvst.VSTException;

import ensemble.Actuator;
import ensemble.Command;
import ensemble.Constants;
import ensemble.EventHandler;
import ensemble.Reasoning;
import ensemble.Sensor;
import ensemble.audio.AudioConstants;
import ensemble.audio.vst.VstProcessReasoning;
import ensemble.clock.TimeUnit;
import ensemble.memory.Memory;
import ensemble.memory.MemoryException;
import ensemble.movement.MovementConstants;

public class PP_SampleReasoning extends Reasoning{

	// Audio
	Actuator 	mouth;
	Memory 		mouthMemory;
	Sensor 		ear;
	Memory 		earMemory;
	int 		chunk_size;
	float 		sampleRate;
	double 		frame_duration;
	
	
	//Time Related
	double 		start_time;
	
	//Auxiliar Memory
	Memory internalMemory;

	
	// Movement
	Actuator	legs;
	Memory 		legsMemory;
	Sensor 		eyes;
	Memory 		eyesMemory;

	
	// InputMode
	enum InputMode {
		NOT_DEFINED,
		FILE_ONLY,
		MIC_ONLY,
		FILE_ON_PLAY,
		SUM_ON_PLAY,
		RANDOM_FILES,
		VARIABLE
	}
	
	InputMode inputMode = InputMode.NOT_DEFINED;
	
	//VST definitions
	
	enum VSTMode {
		NOT_DEFINED,
		FIXED,
		CHAIN,
		VARIABLE		
	}

	VSTMode vstMode = VSTMode.NOT_DEFINED;

	//VST LIST
	public Hashtable<String, String> vstReference =  new Hashtable<String, String>();
	
	
	public String[] vstList;

	//Repertoire
	/*
		•	Sinos (5 samples)
		•	Tremolo (4 samples)
		•	Nylon (2 samples)
		•	Percussão – sino (5 samples)
		•	pizzicato – secos (4 samples)
		•	Percussões (7 samples)

	 * */
	public Hashtable<String, String> sinos =  new Hashtable<String, String>();
	public Hashtable<String, String> tremolos =  new Hashtable<String, String>();
	public Hashtable<String, String> nylon =  new Hashtable<String, String>();
	public Hashtable<String, String> percussaoSino =  new Hashtable<String, String>();
	public Hashtable<String, String> pizzicatos =  new Hashtable<String, String>();
	public Hashtable<String, String> percussoes =  new Hashtable<String, String>();
	
	public Hashtable<String, String> curto =  new Hashtable<String, String>();
	public Hashtable<String, String> longo =  new Hashtable<String, String>();
	
	public String[] audioFileList;

	
	// Reasoning state
	enum ReasoningState {
		NOT_DEFINED,
		LISTENING,
		RECORDING,
		PROCESSING,
		PLAYING,
		ERROR
	}
	
	ReasoningState state = ReasoningState.NOT_DEFINED;
	
	
	
	
	@Override
	public boolean init() {
		

//		vstReference.put("OVERDRIVE", "lib\\vst\\mda Overdrive.dll");
		vstReference.put("DELAY", "lib\\vst\\mda Delay.dll");
//		vstReference.put("FILTER", "lib\\vst\\mda MultiBand.dll");
//		vstReference.put("TALKBOX", "lib\\vst\\mda TalkBox.dll");
//		vstReference.put("REPYSCHO", "lib\\vst\\mda RePsycho!.dll");
//		vstReference.put("FLANGER", "lib\\vst\\mda ThruZero.dll");
//		vstReference.put("REVERB", "lib\\vst\\DX Reverb Light.dll");
//		vstReference.put("EFILTER", "lib\\vst\\EngineersFilter.dll");
//		vstReference.put("TRACKER", "lib\\vst\\mda Tracker.dll");
//		vstReference.put("PITCHSHIFTER", "lib\\vst\\MadShifta.dll");
		
		
		//FILES
		sinos.put("1", "media/piano_preparado/sino/sino.wav");
		
		tremolos.put("1", "media/piano_preparado/tremolo02.wav");
		
		nylon.put("1", "media/piano_preparado/nylon/nylon.wav");
		
		percussaoSino.put("1", "media/piano_preparado/percussao_sino/bolinha_gliss.wav");
		
		pizzicatos.put("1", "media/piano_preparado/pizzicato/estalos_otimos.wav");
		
		percussoes.put("1", "media/piano_preparado/percussoes/bass_curto.wav");
		
		
		
		curto.put("1", "media/piano_preparado/curto/sino.wav");
		
		longo.put("1", "media/piano_preparado/longo/tremolo.wav");
		

		return true;
		
	}
	
	protected void eventHandlerRegistered(EventHandler evtHdl) {
		
		// Checks if it is a sound Actuator
		if (evtHdl instanceof Actuator && evtHdl.getEventType().equals(AudioConstants.EVT_TYPE_AUDIO)) {
			
			mouth = (Actuator)evtHdl;
			mouth.registerListener(this);
			internalMemory = getAgent().getKB().getMemory( mouth.getComponentName() + Constants.SUF_AUXILIAR_MEMORY);
			mouthMemory = getAgent().getKB().getMemory( mouth.getComponentName());
			chunk_size = Integer.parseInt(mouth.getParameter(Constants.PARAM_CHUNK_SIZE, "0"));
			frame_duration = chunk_size / sampleRate;

			//Defines the InputMode
			String str = getParameter("inputMode", "");
			System.out.println("inputMode: " + str);
			if(str.equalsIgnoreCase("FILE_ONLY")){
				inputMode= InputMode.FILE_ONLY;
			}else if(str.equalsIgnoreCase("MIC_ONLY")){
				inputMode= InputMode.MIC_ONLY;
			}else if(str.equalsIgnoreCase("RANDOM_FILES")){
				inputMode= InputMode.RANDOM_FILES;
			}
			//System.out.println("inputMode: " + inputMode.toString());

			//Defines the VSTMode
			str = getParameter("vstMode", "");
			//System.out.println("vstMode: " + str);			
			if(str.equalsIgnoreCase("FIXED")){
				vstMode= VSTMode.FIXED;
			}else if(str.equalsIgnoreCase("VARIABLE")){
				vstMode= VSTMode.VARIABLE;
			}
			
			// Recover the vstPlugins
			String[] vsts = getParameter("vstPlugins", "").split(";");
			
			if(vsts !=null && vsts.length>0){
				vstList = new String[vsts.length];
			}
			
			
			for (int i = 0; i<vsts.length; i++){
				if( vstReference.containsKey(vsts[i])){
					vstList[i] = vstReference.get(vsts[i]);
					//System.out.println("vst[" + i +"]: " + vstList[i]);
				}				
			}
			
			//SETS VST FOR VARIABLE EFFECTS
			if(vstMode == VSTMode.VARIABLE){
				
				
				str = getParameter("vstNumber", "1");
				int vstNumber = Integer.parseInt(str);
				
				ArrayList<String> vstPaths = new ArrayList<String>();			
				Iterator itr = vstReference.values().iterator(); 
				while(itr.hasNext()) {
					vstPaths.add((String) itr.next());				    
				} 

			     Collections.shuffle(vstPaths);
			     
			     vstList = new String[vstNumber];
			     
			     for(int i = 0; i < vstNumber; i++){
			    	 vstList[i] = vstPaths.get(i);	
			    	 //System.out.println("vst[" + i +"]: " + vstList[i]);
			     }
			     getAgent().getKB().addParameter("VSTIndex", "0");
			    
			}
			
			
			//SETS RANDOM FILES
			if(inputMode == InputMode.RANDOM_FILES){
				str = getParameter("fileNumber", "1");
				int fileNumber = Integer.parseInt(str);
				
				str = getParameter("fileGroup", "1");
				int fileGroup = Integer.parseInt(str);
				
				if(fileGroup==1){
					audioFileReference = audioFileReference1;
				}else if(fileGroup==2){
					audioFileReference = audioFileReference2;
				}else if(fileGroup==3){
					audioFileReference = audioFileReference3;
				}else {audioFileReference = audioFileReference4;}
				
				ArrayList<String> filePaths = new ArrayList<String>();			
				Iterator itr = audioFileReference.values().iterator(); 
				while(itr.hasNext()) {
					filePaths.add((String) itr.next());				    
				} 

			     Collections.shuffle(filePaths);
			     
			     audioFileList = new String[fileNumber];
			     
			     for(int i = 0; i < fileNumber; i++){
			    	 audioFileList[i] = filePaths.get(i);	
			    	 //System.out.println("file[" + i +"]: " + audioFileList[i]);
			     }
			     getAgent().getKB().addParameter("FileIndex", "0");
			}
			
		}else if (evtHdl instanceof Sensor && evtHdl.getEventType().equals(AudioConstants.EVT_TYPE_AUDIO)) {
			//Checks if it is a sound sensor
		}else if (evtHdl instanceof Actuator && evtHdl.getEventType().equals(MovementConstants.EVT_TYPE_MOVEMENT)) {
			// Checks if it is a Movement Actuator
		} else if (evtHdl instanceof Sensor && evtHdl.getEventType().equals(MovementConstants.EVT_TYPE_MOVEMENT)) {
			// Checks if it is a Movement sensor
		}
	
	}
	
	@Override
	public void newSense(Sensor sourceSensor, double instant, double duration) {
		
		if (sourceSensor == ear) {
			
		} else if (sourceSensor == eyes) {
		
		}
	}

	public void needAction(Actuator sourceActuator, double instant, double duration) {

		switch (state) {

		case PLAYING:
			//Stops the input bypass
			Command cmd = new Command(getAddress(), "/"+ Constants.FRAMEWORK_NAME + "/" + getAgent().getAgentName() + "/JACKInputToMemoryReasoning", "STOP");
			sendCommand(cmd);		
			
			// Acts
			//mouth.act();
			
			break;
	
		}
		
		switch (inputMode) {
		
		case FILE_ONLY:

			try {

				double[] dBuffer = new double[chunk_size];
				double[] dTransBuffer = new double[chunk_size];
				dBuffer = (double[])internalMemory.readMemory(instant - duration, duration, TimeUnit.SECONDS);
				if (vstList != null && vstList.length > 0) {

					switch (vstMode) {

					case FIXED:
						
						new VstProcessReasoning().ProcessAudio(vstList[0],dBuffer, dTransBuffer, chunk_size);
						mouthMemory.writeMemory(dTransBuffer, instant + duration, duration, TimeUnit.SECONDS);

						break;

					case NOT_DEFINED:
						mouthMemory.writeMemory(dBuffer, instant + duration, duration, TimeUnit.SECONDS);
						break;
					case VARIABLE:
						
						break;
					}

				}
				//System.out.println("Instant: " + instant + " Duration: " + duration );
				
				//System.out.println("Guardei na memória principal um evento no instante " + instant + " de duração " + duration);
				mouth.act();
					
			} catch (MemoryException e) {
				e.printStackTrace();
			} catch (VSTException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			break;
			
		case RANDOM_FILES:
			
			try {

				double[] dBuffer = new double[chunk_size];
				double[] dTransBuffer = new double[chunk_size];
				dBuffer = (double[])internalMemory.readMemory(instant - duration, duration, TimeUnit.SECONDS);
				if (vstList != null && vstList.length > 0) {

					switch (vstMode) {

					case FIXED:
						new VstProcessReasoning().ProcessAudio(vstList[0],dBuffer, dTransBuffer, chunk_size);
						mouthMemory.writeMemory(dTransBuffer, instant + duration, duration, TimeUnit.SECONDS);
						break;

					case NOT_DEFINED:
						mouthMemory.writeMemory(dBuffer, instant + duration, duration, TimeUnit.SECONDS);
						break;
					case VARIABLE:
						
						//Indice de Arquivo
						int actualFileIndex = Integer.parseInt(getAgent().getKB().getParameter("FileIndex","0"));
						//Indice de VST
						int actualVstIndex = Integer.parseInt(getAgent().getKB().getParameter("VSTIndex","0"));
						
						
						if(getAgent().getKB().getParameter("playState")!=null && getAgent().getKB().getParameter("playState")==AudioConstants.CMD_STOP){
							Command cmd = new Command(getAddress(), "/"+ Constants.FRAMEWORK_NAME + "/" + getAgent().getAgentName() + "/FileInputReasoning", AudioConstants.CMD_PLAY);
							System.out.println("INDEX = " + actualFileIndex);
							
							cmd.addParameter("filename", audioFileList[actualFileIndex]);
							//System.out.println("FILE = " + audioFileList[actualFileIndex]);
							sendCommand(cmd);
							
							//Adaptacao do indice
							if((actualFileIndex +1) < Integer.parseInt(getParameter("fileNumber", "1"))){
								actualFileIndex++;
							}else {actualFileIndex = 0;}
							
							if((actualVstIndex +1) < Integer.parseInt(getParameter("vstNumber", "1"))){
								actualVstIndex++;
							}else {actualVstIndex = 0;}
								
							getParameters().put("playState", "PLAY");
							getParameters().put("FileIndex", String.valueOf(actualFileIndex));
							getParameters().put("VSTIndex", String.valueOf(actualVstIndex));
							getAgent().getKB().setParameters(getParameters());
							
							
							
						}
						
						//System.out.println("VST = " +vstList[actualVstIndex]);
						new VstProcessReasoning().ProcessAudio(vstList[actualVstIndex],dBuffer, dTransBuffer, chunk_size);
						mouthMemory.writeMemory(dTransBuffer, instant + duration, duration, TimeUnit.SECONDS);
						
						break;
					}

				}
				//System.out.println("Instant: " + instant + " Duration: " + duration );
				
				//System.out.println("Guardei na memória principal um evento no instante " + instant + " de duração " + duration);
				mouth.act();
					
			} catch (MemoryException e) {
				e.printStackTrace();
			} catch (VSTException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case MIC_ONLY:
			
			try {

				double[] dBuffer = new double[chunk_size];
				dBuffer = (double[])internalMemory.readMemory(instant - duration, duration, TimeUnit.SECONDS);
				if (vstList != null && vstList.length > 0) {

					switch (vstMode) {

					case FIXED:

						double[] dTransBuffer = new double[chunk_size];
						long start = System.currentTimeMillis();
						new VstProcessReasoning().ProcessAudio(vstList[0],dBuffer, dTransBuffer, chunk_size);
						long end = System.currentTimeMillis();
						System.out.println("start = " + start +" end = " + end + " dif= " + (end-start) );
						mouthMemory.writeMemory(dTransBuffer, instant , duration, TimeUnit.SECONDS);

						break;

					case NOT_DEFINED:
						mouthMemory.writeMemory(dBuffer, instant, duration, TimeUnit.SECONDS);
						break;
					}

				}
				mouth.act();
					
			} catch (MemoryException e) {
				e.printStackTrace();
			} catch (VSTException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			break;
		case NOT_DEFINED:
		
		try {
			
			double[] dBuffer = new double[chunk_size];
			
			dBuffer = (double[])internalMemory.readMemory(instant - duration, duration, TimeUnit.SECONDS);

			mouthMemory.writeMemory(dBuffer, instant + duration, duration, TimeUnit.SECONDS);
			
			//System.out.println("Guardei na memória principal um evento no instante " + instant + " de duração " + duration);

			//TESTE COMMAND	
			//			if(instant >12){
//				//Stops the input bypass
				Command cmd = new Command(getAddress(), "/"+ Constants.FRAMEWORK_NAME + "/" + getAgent().getAgentName() + "/MicInputReasoning", "STOP");
//				sendCommand(cmd);
//				}else if(instant >20){
//					
//					Command cmd = new Command(getAddress(), "/"+ Constants.FRAMEWORK_NAME + "/" + getAgent().getAgentName() + "/MicInputReasoning", "START");
//					sendCommand(cmd);
//					
//				}
					
			
			mouth.act();
				
		} catch (MemoryException e) {
			e.printStackTrace();
		} 
		break;
	}
//		System.out.println("REAS time = " + (System.currentTimeMillis() - start));
	}

	@Override
	public void processCommand(Command cmd) {
		if (cmd.getCommand().equals(AudioConstants.CMD_VST_OFF)) {
			vstMode = VSTMode.NOT_DEFINED;
		} else if (cmd.getCommand().equals(AudioConstants.CMD_VST_ON)) {
			vstMode = VSTMode.FIXED;
		} 
	}
	
	public void process() throws Exception {

		switch (state) {
		
		case LISTENING:
			
			break;
			
		case RECORDING:
			
			break;

		case PLAYING:
			//Command cmd = new Command(getAddress(), "/"+ Constants.FRAMEWORK_NAME + "/" + getAgent().getAgentName() + "/MovementReasoning", "WALK");
//			
//			cmd.addParameter(MovementConstants.PARAM_POS, "("+x+";"+ycmd.addParameter(MovementConstants.PARAM_TIME, "sendCommand(cmd);" +"
			break;
			
		case PROCESSING:
		
			break;
		}
	}
	
	
	
}