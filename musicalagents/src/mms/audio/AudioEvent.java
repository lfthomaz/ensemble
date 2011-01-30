package mms.audio;

import mms.Event;

public class AudioEvent extends Event {
	
	// Tamanho e Formato do Chunk
	public int 		bitsPerSample;
	public int 		sampleRate;
	
	// ID sequêncial do fragmento
	public long 	chunkID;

	// Dados do Chunk (audio, MIDI etc.)
	public double[] chunk;
	public int    	chunkLength;
	
	public AudioEvent(int chunkLenght) {
//		this.bitsPerSample 	= bitsPerSample;
//		this.sampleRate 	= sampleRate;
		this.chunkLength 	= chunkLenght;
		this.chunk 			= new double[chunkLenght];
	}
	
}
