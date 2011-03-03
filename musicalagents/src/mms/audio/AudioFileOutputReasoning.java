package mms.audio;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import javax.sound.sampled.SourceDataLine;

import jade.util.Logger;

import mms.Constants;
import mms.EventHandler;
import mms.MusicalAgent;
import mms.Reasoning;
import mms.Sensor;
import mms.clock.TimeUnit;
import mms.kb.Memory;
import mms.tools.AudioTools;

public class AudioFileOutputReasoning extends Reasoning {

	// Log
	public static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	// Memories
	private HashMap<String, Memory> earMemories = new HashMap<String, Memory>();

	// Files
	private HashMap<String, FileOutputStream> outFiles = new HashMap<String, FileOutputStream>();

	// Parameters
	int 	device;
	int 	channel;
	int 	maxChannels;
	
	@Override
	public boolean init() {
		
		return true;
		
	}

	public boolean finit() {

		Collection<FileOutputStream> files = outFiles.values();
		for (FileOutputStream file : files) {
			try {
				file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return true;
	}

	@Override
	protected void eventHandlerRegistered(EventHandler evtHdl) {
	
		if (evtHdl.getEventType().equals(Constants.EVT_AUDIO)) {

			// Stores ear's memory
			Sensor ear = (Sensor)evtHdl;
			ear.registerListener(this);
			earMemories.put(ear.getName(), getAgent().getKB().getMemory(ear.getName()));
		
			// Assigns a channel in the audio interface to this ear
			if (ear.getParameters().containsKey("channel")) {
//				System.out.println("CHANNEL parameter detected in ear = " + ear.getParameter("channel"));
				String channel_param = ear.getParameter("channel");
				// Creates a file
				try {
					FileOutputStream file = new FileOutputStream(getAgent().getAgentName()+"_"+channel_param+"_out.dat");
					outFiles.put(ear.getName(), file);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			} else {
				// Gets the next available channel
				// TODO O QUE FAZER AQUI? TEMOS QUE TER UMA LISTA dos canais já utilizados!
				System.out.println("SEM PARAMETROS DE CANAL!!!");
			}			
			
		}
			
	}

	@Override
	public void newSense(Sensor sourceSensor, double instant, double duration) {

		if (sourceSensor.getEventType().equals("AUDIO")) {
			
			String earName = sourceSensor.getName();
			Memory earMemory = earMemories.get(earName);
			FileOutputStream file = outFiles.get(earName);
			if (file != null) {
				double[] buf = (double[])earMemory.readMemory(instant, duration, TimeUnit.SECONDS);
				try {
					file.write(AudioTools.convertDoubleByte(buf, 0, buf.length));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
		
	}

}
