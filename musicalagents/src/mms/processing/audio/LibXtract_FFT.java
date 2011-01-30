package mms.processing.audio;

import xtract.core.floatArray;
import xtract.core.xtract;
import xtract.core.xtract_features_;
import xtract.core.xtract_spectrum_;
import mms.Parameters;
import mms.processing.Process;

public class LibXtract_FFT extends Process {

	// FFT
	private static int default_fft_size = 512;

	static {
		xtract.xtract_init_fft(default_fft_size, xtract_features_.XTRACT_SPECTRUM.swigValue());
	}
	
	@Override
	public Object process(Parameters arguments, Object in) {

		// Validate arguments
		int fft_size = Integer.valueOf(arguments.get("fft_size", "512"));
		double Fs = Double.valueOf(arguments.get("Fs", "44100"));
		String fft_type = "r2c";
		String fft_output = arguments.get("fft_output", "real"); // real, complex, polar
		
		// Valide input data
		if (!(in instanceof double[])) {
			System.out.println("SPECTRUM: input must be a double[]");
			return null;
		}
		double[] chunk = (double[])in;

		// Creates fft plan only if needed
		if (fft_size != default_fft_size) {
			xtract.xtract_init_fft(fft_size, xtract_features_.XTRACT_SPECTRUM.swigValue());
			default_fft_size = fft_size;
		}

		// Input vector
		floatArray vector = new floatArray(fft_size);
		for (int i = 0; i < chunk.length; i++) {
			vector.setitem(i, (float)chunk[i]);
		}

		// Spectrum
		floatArray argf = new floatArray(4);
		argf.setitem(0, (float)Fs/fft_size);
		argf.setitem(1, xtract_spectrum_.XTRACT_MAGNITUDE_SPECTRUM.swigValue());
		argf.setitem(2, 0.0f);
		argf.setitem(3, 0.0f);
		floatArray spectrum = new floatArray(fft_size);
		xtract.xtract_spectrum(vector.cast(), fft_size, argf.cast().getVoidPointer(), spectrum.cast());

		// Results
		double[] out = new double[fft_size/2];
		for (int i = 0; i < fft_size/2; i++) {
			out[i] = spectrum.getitem(i);
		}
		
		return out;

	}

}
