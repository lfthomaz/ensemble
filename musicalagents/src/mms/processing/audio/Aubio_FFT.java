package mms.processing.audio;

import aubio.SWIGTYPE_p_aubio_mfft_t;
import aubio.SWIGTYPE_p_cvec_t;
import aubio.SWIGTYPE_p_fvec_t;
import aubio.aubiowrapper;
import mms.Parameters;
import mms.processing.Process;

public class Aubio_FFT extends Process {

	private final String PARAM_SIZE 		= "size";
	private final String PARAM_SAMPLE_RATE	= "sample_rate";
	private final String PARAM_OUTPUT_TYPE 	= "output_type";
	private final String PARAM_INVERSE		= "inverse";
	
	// FFT
	private SWIGTYPE_p_aubio_mfft_t mfft_t;
	private int default_fft_size = 512;
	private int fft_size;
	private double Fs;
	private String fft_output;
	private boolean inverse;

	@Override
	public boolean init() {
		
		// Validate arguments
		fft_size = Integer.valueOf(arguments.get(PARAM_SIZE, "512"));
		Fs = Double.valueOf(arguments.get(PARAM_SAMPLE_RATE, "44100"));
		fft_output = arguments.get(PARAM_OUTPUT_TYPE, "polar"); // real, complex, polar
		inverse = Boolean.parseBoolean(arguments.get(PARAM_INVERSE, "false"));

		// Initialize FFT plan
		mfft_t = aubiowrapper.new_aubio_mfft(default_fft_size, 1);
		System.out.println("FFT plan created - size = " + default_fft_size);
		
		return true;
		
	}

	@Override
	public Object process(Parameters arguments, Object in) {

		double[] out = null;
		
		// Valide input data
		if (!(in instanceof double[])) {
			System.out.println("FFT: input must be a double[]");
			return null;
		}
		double[] chunk = (double[])in;
		
		// Creates fft plan only if needed
		if (fft_size != default_fft_size) {
			mfft_t = aubiowrapper.new_aubio_mfft(fft_size, 1);
			default_fft_size = fft_size;
			System.out.println("FFT plan recreated - size = " + default_fft_size);
		}
		
		// Forward FFT
		if (!inverse) {
	
			// Creates the input vector
			SWIGTYPE_p_fvec_t in_fvec = aubiowrapper.new_fvec(fft_size, 1);
			for (int i = 0; i < fft_size; i++) {
				aubiowrapper.fvec_write_sample(in_fvec, (float)chunk[i], 0, i);
			}
	      
			// Creates the output vector
			SWIGTYPE_p_cvec_t out_cvec = aubiowrapper.new_cvec(fft_size, 1);
	      
			// FFT
			aubiowrapper.aubio_mfft_do(mfft_t, in_fvec, out_cvec);
	      
			// Results
			if (fft_output.equals("real")) {
				out = new double[fft_size/2+1];
				for (int i = 0; i < fft_size/2+1; i++) {
					float norm = aubiowrapper.cvec_read_norm(out_cvec, 0, i);
					float phas = aubiowrapper.cvec_read_phas(out_cvec, 0, i);
	//				System.out.println(norm + " |_ " + phas);
					double mag = norm/*Math.sqrt((norm * norm) + (phas * phas))*/;
					out[i] = mag;
				}
			} else if (fft_output.equals("complex")) {
				
			} else if (fft_output.equals("polar")) {
				out = new double[fft_size + 2];
				for (int i = 0; i < fft_size/2+1; i++) {
					out[(i*2)] = aubiowrapper.cvec_read_norm(out_cvec, 0, i);
					out[(i*2)+1] = aubiowrapper.cvec_read_phas(out_cvec, 0, i);
				}
			}

		// Inverse FFT
		} else {
			// Creates the input vector
			SWIGTYPE_p_cvec_t in_cvec = aubiowrapper.new_cvec(fft_size, 1);
			for (int i = 0; i < fft_size/2+1; i++) {
				aubiowrapper.cvec_write_norm(in_cvec, (float)chunk[i*2], 0, i);
				aubiowrapper.cvec_write_phas(in_cvec, (float)chunk[i*2+1], 0, i);
			}
	      
			// Creates the output vector
			SWIGTYPE_p_fvec_t out_fvec = aubiowrapper.new_fvec(fft_size, 1);
	      
			// FFT
			aubiowrapper.aubio_mfft_rdo(mfft_t, in_cvec, out_fvec);
	      
			// Results
			out = new double[fft_size];
			for (int i = 0; i < fft_size; i++) {
				out[i] = aubiowrapper.fvec_read_sample(out_fvec, 0, i);
			}
			
		}
		
		return out;

	}

	@Override
	public boolean fini() {
		return true;
	}
	    
}
