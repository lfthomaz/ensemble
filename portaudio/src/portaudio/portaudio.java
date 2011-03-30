package portaudio;

public class portaudio {

	// Loads the portaudio JNI interface
	static {
		// TODO Vai ter que verificar aqui qual sistema est‡ rodando
		System.out.println("OS = " + System.getProperty("os.name"));
		System.out.println("OS = " + System.getProperty("os.arch"));
		System.out.println("OS = " + System.getProperty("sun.arch.data.model"));
				
		System.loadLibrary("mmsportaudio");
	}
	
  public static int Pa_GetVersion() {
    return portaudioJNI.Pa_GetVersion();
  }

  public static String Pa_GetVersionText() {
    return portaudioJNI.Pa_GetVersionText();
  }

  public static String Pa_GetErrorText(int errorCode) {
    return portaudioJNI.Pa_GetErrorText(errorCode);
  }

  public static int Pa_Initialize() {
    return portaudioJNI.Pa_Initialize();
  }

  public static int Pa_Terminate() {
    return portaudioJNI.Pa_Terminate();
  }

  public static int Pa_GetHostApiCount() {
    return portaudioJNI.Pa_GetHostApiCount();
  }

  public static int Pa_GetDefaultHostApi() {
    return portaudioJNI.Pa_GetDefaultHostApi();
  }

  public static PaHostApiInfo Pa_GetHostApiInfo(int hostApi) {
    long cPtr = portaudioJNI.Pa_GetHostApiInfo(hostApi);
    return (cPtr == 0) ? null : new PaHostApiInfo(cPtr, false);
  }

  public static int Pa_HostApiTypeIdToHostApiIndex(SWIGTYPE_p_PaHostApiTypeId type) {
    return portaudioJNI.Pa_HostApiTypeIdToHostApiIndex(SWIGTYPE_p_PaHostApiTypeId.getCPtr(type));
  }

  public static int Pa_HostApiDeviceIndexToDeviceIndex(int hostApi, int hostApiDeviceIndex) {
    return portaudioJNI.Pa_HostApiDeviceIndexToDeviceIndex(hostApi, hostApiDeviceIndex);
  }

  public static PaHostErrorInfo Pa_GetLastHostErrorInfo() {
    long cPtr = portaudioJNI.Pa_GetLastHostErrorInfo();
    return (cPtr == 0) ? null : new PaHostErrorInfo(cPtr, false);
  }

  public static int Pa_GetDeviceCount() {
    return portaudioJNI.Pa_GetDeviceCount();
  }

  public static int Pa_GetDefaultInputDevice() {
    return portaudioJNI.Pa_GetDefaultInputDevice();
  }

  public static int Pa_GetDefaultOutputDevice() {
    return portaudioJNI.Pa_GetDefaultOutputDevice();
  }

  public static PaDeviceInfo Pa_GetDeviceInfo(int device) {
    long cPtr = portaudioJNI.Pa_GetDeviceInfo(device);
    return (cPtr == 0) ? null : new PaDeviceInfo(cPtr, false);
  }

  public static int Pa_IsFormatSupported(PaStreamParameters inputParameters, PaStreamParameters outputParameters, double sampleRate) {
    return portaudioJNI.Pa_IsFormatSupported(PaStreamParameters.getCPtr(inputParameters), inputParameters, PaStreamParameters.getCPtr(outputParameters), outputParameters, sampleRate);
  }

  public static long Pa_OpenStream(PaStreamParameters inputParameters, PaStreamParameters outputParameters, double sampleRate, long framesPerBuffer, long streamFlags, Object streamCallback) {
    return portaudioJNI.Pa_OpenStream(PaStreamParameters.getCPtr(inputParameters), inputParameters, PaStreamParameters.getCPtr(outputParameters), outputParameters, sampleRate, framesPerBuffer, streamFlags, streamCallback);
  }

  public static long Pa_OpenDefaultStream(int numInputChannels, int numOutputChannels, long sampleFormat, double sampleRate, long framesPerBuffer, Object streamCallback) {
    return portaudioJNI.Pa_OpenDefaultStream(numInputChannels, numOutputChannels, sampleFormat, sampleRate, framesPerBuffer, streamCallback);
  }

  public static int Pa_CloseStream(long stream) {
    return portaudioJNI.Pa_CloseStream(stream);
  }

  public static int Pa_StartStream(long stream) {
    return portaudioJNI.Pa_StartStream(stream);
  }

  public static int Pa_StopStream(long stream) {
    return portaudioJNI.Pa_StopStream(stream);
  }

  public static int Pa_AbortStream(long stream) {
    return portaudioJNI.Pa_AbortStream(stream);
  }

  public static int Pa_IsStreamStopped(long stream) {
    return portaudioJNI.Pa_IsStreamStopped(stream);
  }

  public static int Pa_IsStreamActive(long stream) {
    return portaudioJNI.Pa_IsStreamActive(stream);
  }

  public static PaStreamInfo Pa_GetStreamInfo(long stream) {
    long cPtr = portaudioJNI.Pa_GetStreamInfo(stream);
    return (cPtr == 0) ? null : new PaStreamInfo(cPtr, false);
  }

  public static double Pa_GetStreamTime(long stream) {
    return portaudioJNI.Pa_GetStreamTime(stream);
  }

  public static double Pa_GetStreamCpuLoad(long stream) {
    return portaudioJNI.Pa_GetStreamCpuLoad(stream);
  }

  public static int Pa_ReadStream(long stream, SWIGTYPE_p_void buffer, long frames) {
    return portaudioJNI.Pa_ReadStream(stream, SWIGTYPE_p_void.getCPtr(buffer), frames);
  }

  public static int Pa_WriteStream(long stream, SWIGTYPE_p_void buffer, long frames) {
    return portaudioJNI.Pa_WriteStream(stream, SWIGTYPE_p_void.getCPtr(buffer), frames);
  }

  public static int Pa_GetStreamReadAvailable(long stream) {
    return portaudioJNI.Pa_GetStreamReadAvailable(stream);
  }

  public static int Pa_GetStreamWriteAvailable(long stream) {
    return portaudioJNI.Pa_GetStreamWriteAvailable(stream);
  }

  public static int Pa_GetSampleSize(long format) {
    return portaudioJNI.Pa_GetSampleSize(format);
  }

  public static void Pa_Sleep(int msec) {
    portaudioJNI.Pa_Sleep(msec);
  }

}
