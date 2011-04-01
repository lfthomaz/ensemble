package mms.processing.audio;

import xtract.core.floatArray;
import xtract.core.xtract;
import xtract.core.xtract_features_;
import xtract.core.xtract_spectrum_;
import mms.Parameters;
import mms.processing.Process;

public class LibXtract_FFT extends Process {

	private final String PARAM_SIZE 		= "size";
	private final String PARAM_SAMPLE_RATE	= "sample_rate";
	private final String PARAM_OUTPUT_TYPE 	= "output_type";
	private final String PARAM_INVERSE		= "inverse";

	// FFT
	private static int default_fft_size = 512;
	private int fft_size;
	private double Fs;
	private String fft_output;
	private boolean inverse;

	private floatArray vector;
	private	floatArray argf = new floatArray(4);
	private floatArray spectrum;

	@Override
	public boolean init() {
		
		// Validate arguments
		fft_size = Integer.valueOf(arguments.get(PARAM_SIZE, "512"));
		Fs = Double.valueOf(arguments.get(PARAM_SAMPLE_RATE, "44100"));
		fft_output = arguments.get(PARAM_OUTPUT_TYPE, "polar"); // real, complex, polar
		inverse = Boolean.parseBoolean(arguments.get(PARAM_INVERSE, "false"));

		vector = new floatArray(fft_size);
		spectrum = new floatArray(fft_size);
		
		xtract.xtract_init_fft(fft_size, xtract_features_.XTRACT_SPECTRUM.swigValue());
		return true;
		
	}

	@Override
	public boolean fini() {
		
		xtract.xtract_free_fft();
		return true;
		
	}

	@Override
	public Object process(Parameters arguments, Object in) {

		// Valide input data
		if (!(in instanceof double[])) {
			System.out.println("SPECTRUM: input must be a double[]");
			return null;
		}
		double[] chunk = (double[])in;

		// Creates fft plan only if needed
		if (fft_size != default_fft_size) {
			xtract.xtract_init_fft(fft_size, xtract_features_.XTRACT_SPECTRUM.swigValue());
			vector = new floatArray(fft_size);
			spectrum = new floatArray(fft_size);
			default_fft_size = fft_size;
		}

		// Input vector
		for (int i = 0; i < chunk.length; i++) {
			vector.setitem(i, (float)chunk[i]);
		}

		// Spectrum
		argf.setitem(0, (float)Fs/fft_size);
		argf.setitem(1, xtract_spectrum_.XTRACT_MAGNITUDE_SPECTRUM.swigValue());
		argf.setitem(2, 0.0f);
		argf.setitem(3, 0.0f);
		xtract.xtract_spectrum(vector.cast(), fft_size, argf.cast().getVoidPointer(), spectrum.cast());

		// Results
		double[] out = new double[fft_size/2];
		for (int i = 0; i < fft_size/2; i++) {
			out[i] = spectrum.getitem(i);
		}
		
		return out;

	}

}
