package rt;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import jade.util.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import mms.Constants;
import mms.EventHandler;
import mms.MusicalAgent;
import mms.Reasoning;
import mms.Sensor;
import mms.clock.TimeUnit;
import mms.kb.Memory;
import mms.tools.AudioTools;

public class RT_ListenerReasoning extends Reasoning {

	// Log
	public static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	Sensor 	ear;
	Memory 	earMemory;
	
	private enum CHANNELS {LEFT, RIGHT, BOTH};
	private CHANNELS channel = CHANNELS.BOTH; 
	
	SourceDataLine	line 			= null;
	FileOutputStream out_byte_sink 	= null;
	long 			currentChunk	= 1;
	long 			playedChunk		= 1;
	
	@Override
	public boolean init() {
		
		// Channel
		if (getParameters().containsKey("channel")) {
			String str_channel = getParameter("channel"); 
			if (str_channel.equals("LEFT")) {
				channel = CHANNELS.LEFT;
			}
			else if (str_channel.equals("RIGHT")) {
				channel = CHANNELS.RIGHT;
			}
		}
		
		// Opens audio file for writing
		try {
			out_byte_sink 		= new FileOutputStream(getAgent().getAgentName()+"_out.dat");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// Initializes the Audio System
		AudioFormat format = new AudioFormat(44100f, 16, 1, true, false);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		if (!AudioSystem.isLineSupported(info)) {
			getAgent().logger.severe("[" + getName() + "] " + "Line not supported");
			return false;
		}
		
		// Obtain and open the line.
		try {
		    line = (SourceDataLine) AudioSystem.getLine(info);
		    line.open(format);
			// Channel control
			if (line.isControlSupported(FloatControl.Type.PAN)) {
	            FloatControl pan = (FloatControl) line.getControl(FloatControl.Type.PAN);
	            if (channel == CHANNELS.RIGHT) {
	                pan.setValue(1.0f);
	            } else if (channel == CHANNELS.LEFT) { 
	                pan.setValue(-1.0f);
	            } else {
	            	pan.setValue(0.0f);
	            }
			}
		    line.start();
		} catch (LineUnavailableException ex) {
			getAgent().logger.severe("[" + getName() + "] " + "Line Unavailable");
			return false;
		}
		
		return true;
		
	}

	public boolean finit() {
		try {
			out_byte_sink.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		line.stop();
		line.close();
		return true;
	}

	@Override
	protected void eventHandlerRegistered(EventHandler evtHdl) {

		if (evtHdl.getEventType().equals(Constants.EVT_AUDIO)) {
			
			ear = (Sensor)evtHdl;
			ear.registerListener(this);
			earMemory = getAgent().getKB().getMemory(ear.getName());
		
			// TODO Aqui devemos fazer a distribuição de canais, caso o agente tenha mais de uma orelha!!!
			if (ear.getParameters().containsKey("channel")) {
				System.out.println("CHANNEL parameter detected in ear!");
			}

		}
		
	}

	// TODO Não pode tocar imediatamente, tem que respeitar o tempo, senão não podemos sincronazar com outros agentes e com outros acontecimentos no ambiente
	// TODO O JavaSound consegue fazer isso?!?!?!? No lo creo!
	@Override
	public void newSense(String eventType, double instant, double duration) {

		double[] buf = (double[])earMemory.readMemory(instant, duration, TimeUnit.SECONDS);

		byte[] buffer = AudioTools.convertDoubleByte(buf, 0, buf.length);
//		System.out.println(System.currentTimeMillis() + " Player: recebi chunk de tamanho " + evt.chunkLength);
//		queue.add(buffer);
		line.write(buffer, 0, buffer.length);
	    try {
			out_byte_sink.write(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		MusicalAgent.logger.info("[" + getAgent().getLocalName() + ":" + getName() + "] " + "Inseri chunk " + instant + " na fila para tocar");
//		System.out.println("size = " + queue.size());
		currentChunk++;

	}

	@Override
	public void process() {
		// TODO Auto-generated method stub

	}
	
}
