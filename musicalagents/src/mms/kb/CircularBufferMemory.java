package mms.kb;

import mms.Constants;
import mms.Parameters;
import mms.clock.TimeUnit;

public class CircularBufferMemory extends Memory {

	private double 	period;
	private double 	startTime;
	
	private int 	samples;
	private int 	pastSamples;
	private int 	futureSamples;
	private int 	ptrBegin;
	private long 	ptrCounter;
	private double 	instantBegin;
	private double 	instantEnd;
	
	private Object[] buffer;

	@Override
	public void init(Parameters parameters) {

		// Get parameters
		this.period = (double)(Long.valueOf(parameters.get(Constants.PARAM_PERIOD)))/1000.0;
		this.startTime = (double)(Long.valueOf(parameters.get(Constants.PARAM_START_TIME)))/1000.0;
		
		// Initializes the buffer
		pastSamples = (int)((past) / period);
		futureSamples = (int)((future) / period);
		samples = pastSamples+futureSamples;
//		halfSamples = samples / 2;
		this.buffer = new Object[samples];

		// Time adjustments
		instantBegin = this.startTime - (pastSamples * period);
		instantEnd = this.startTime + (futureSamples * period);
//		System.out.println("sample = " + samples + " - pastSamples = " + pastSamples + " - futureSamples = " + futureSamples + " - instantBegin = " + instantBegin+ " - instantEnd = " + instantEnd);
		ptrBegin = 0;
		ptrCounter = 0;
//		ptrNow = pastSamples;
//		ptrEnd = samples - 1;
//		System.out.println("ptrNow = " + ptrNow + " - ptrEnd = " + ptrEnd);
		
	}

	@Override
	public double getFirstInstant() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getLastInstant() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object readMemory(double instant, TimeUnit unit) {

		double sample = ptrBegin + (instant - instantBegin) / period;
		int sample_low = (int)Math.floor(sample) % samples;

		return buffer[sample_low];
		
	}

	@Override
	public Object readMemory(double instant, double duration, TimeUnit unit) {
	
		double sample = ptrBegin + (instant - instantBegin) / period;
		int sample_low = (int)Math.floor(sample) % samples;

		int num_samples = 1 + (int)Math.floor((duration / period));
		Double[] ret = new Double[num_samples];
		for (int i = 0; i < num_samples; i++) {
			ret[i] = (Double)buffer[(sample_low + i) % samples];
		}
		
		return ret;

	}

	@Override
	public void resetMemory() {
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = null;
		}
	}

	@Override
	public void writeMemory(Object object, double instant, double duration,
			TimeUnit unit) throws MemoryException {

		updateMemory();
		
		// Encontra o sample onde se iniciará a cópia do buffer na memória
		int ptrBuffer;
		if (instant >= instantBegin && instant < instantEnd) {
			ptrBuffer = (ptrBegin + (int)Math.round((instant - instantBegin) / period)) % samples;
		}
		else if (instant < instantBegin && instant + duration >= instantBegin) {
			ptrBuffer = ptrBegin;
		}
		else {
			System.out.println("[WARNING] Instant '" + instant + "' is outside memory's bounds");
			return;
		}
		
		// Referencia o objeto
		buffer[ptrBuffer] = object;
			
//		System.out.println("readMemoryAbsolut() duration = " + (System.nanoTime() - start));
		
	}

	@Override
	public void writeMemory(Object object) throws MemoryException {
		
	}
	
	private void updateMemory() {
		
//		System.out.println("updateMemory()");
		
//		long start = System.nanoTime();
		
		double now = clock.getCurrentTime(TimeUnit.SECONDS);
		double newInstantBegin = now - past;
		int displaced_samples = (int)Math.floor((newInstantBegin-instantBegin) / period);
//		System.out.println("instantBegin = " + instantBegi n + " - now = " + now + " - displaces = " + displaced_samples);
		
		if (displaced_samples < 0) {
			System.err.println("ERRO!!! getNow() voltou no tempo!!!");
		}
		else if (displaced_samples == 0) {
			// não houve mudança no tempo, retornar sem alterar nada
//			System.out.println("Sem mudanças!");
			return;
		}
		else if (displaced_samples > 0 && displaced_samples < samples) {
			
			int ptrNewBegin = (ptrBegin + displaced_samples) % samples;
			for (int i = ptrBegin; i != ptrNewBegin; i = (i+1)%samples) {
				buffer[i] = null;
			}
			ptrBegin = ptrNewBegin;

		} else {
//			System.out.println("diff_begin_now >= samples");
			ptrBegin = 0;
			// Preenche toda a memória com zeros onde for necessário
			for (int i = 0; i < samples; i++) {
				buffer[i] = null;
			}
		}
		
		ptrCounter = ptrCounter + displaced_samples;
		instantBegin = this.startTime + ((ptrCounter-pastSamples) * period);
		instantEnd = this.startTime + ((ptrCounter+futureSamples) * period);
		
//		System.out.println(" instantBegin = " + instantBegin+ " - instantEnd = " + instantEnd);

//		long duration = System.nanoTime() - start;
//		System.out.println("update() duration = " + duration);
		
	}

	
}
