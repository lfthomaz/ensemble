package mms.processing.audio;

import aubio.SWIGTYPE_p_aubio_onsetdetection_t;
import aubio.SWIGTYPE_p_aubio_pvoc_t;
import aubio.SWIGTYPE_p_cvec_t;
import aubio.SWIGTYPE_p_fvec_t;
import aubio.aubio_onsetdetection_type;
import aubio.aubiowrapper;
import mms.Parameters;
import mms.processing.Process;

public class Aubio_Onset extends Process {

	private static int fftsize = 512;
	private static int hopsize = 256;

	private static SWIGTYPE_p_aubio_onsetdetection_t o;
	private static SWIGTYPE_p_aubio_pvoc_t pv;
	
	// TODO Pode dar problema se dois agentes usarem ao mesmo tempo
	static {
		pv = aubiowrapper.new_aubio_pvoc(512, 256, 1);
		// Initialize Onset Detection
		o = aubiowrapper.new_aubio_onsetdetection(aubio_onsetdetection_type.aubio_onset_complex, fftsize, 1);
	}

	@Override
	public Object process(Parameters arguments, Object in) {
		
		// Creates the input vector
		double[] chunk = (double[])in;
		SWIGTYPE_p_fvec_t ibuf = aubiowrapper.new_fvec(fftsize, 1);
		SWIGTYPE_p_fvec_t onset = aubiowrapper.new_fvec(fftsize, 1);
      
		// Creates the output vector
		SWIGTYPE_p_cvec_t fftgrain = aubiowrapper.new_cvec(fftsize, 1);

		int ptr = 0;
		while (ptr + fftsize < chunk.length) {
			// ibuf
			for (int i = 0; i < fftsize; i++) {
				aubiowrapper.fvec_write_sample(ibuf, (float)chunk[ptr+i], 0, i);
			}
			aubiowrapper.aubio_pvoc_do (pv, ibuf, fftgrain);
			aubiowrapper.aubio_onsetdetection(o, fftgrain, onset);
		    boolean isonset = aubiowrapper.aubio_peakpick_pimrt(onset, parms);
			if (isonset) {
				/* test for silence */
				if (aubiowrapper.aubio_silence_detection(ibuf, silence)==1) {
					isonset=0;
				} else {
					for (pos = 0; pos < overlap_size; pos++){
						obuf->data[0][pos] = woodblock->data[0][pos];
					}
				}
			}
		      
		}
		return null;
	}

}
