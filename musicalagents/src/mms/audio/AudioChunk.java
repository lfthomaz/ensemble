package mms.audio;

import mms.world.Vector;

public class AudioChunk {
		
	public double[] 	chunk;
	public int 			chunk_size;
	public int 			frame;
	public Vector 		position;
	public Vector	 	velocity;
	public AudioChunk 	next;
	public double 		time_begin;
	public double 		time_end;
	public long 		sample_begin;
		
}
