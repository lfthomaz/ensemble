/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.31
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package mmsjack;

class mmsjackJNI {
  public final static native String JACK_DEFAULT_AUDIO_TYPE_get();
  public final static native String JACK_DEFAULT_MIDI_TYPE_get();
  public final static native int JackPortIsInput_get();
  public final static native int JackPortIsOutput_get();
  public final static native int JackPortIsPhysical_get();
  public final static native int JackPortCanMonitor_get();
  public final static native int JackPortIsTerminal_get();
  public final static native int JackPortIsActive_get();
  public final static native int JackNullOption_get();
  public final static native int JackNoStartServer_get();
  public final static native int JackUseExactName_get();
  public final static native int JackServerName_get();
  public final static native int JackLoadName_get();
  public final static native int JackLoadInit_get();
  public final static native int jack_client_close(long jarg1);
  public final static native long jack_client_open(String jarg1, Object jarg2);
  public final static native int jack_get_sample_rate(long jarg1);
  public final static native long jack_port_register(long jarg1, String jarg2, String jarg3, long jarg4);
  public final static native int jack_port_unregister(long jarg1, long jarg2);
  public final static native int jack_activate(long jarg1);
  public final static native String[] jack_get_ports(long jarg1, String jarg2, String jarg3, long jarg4);
  public final static native int jack_connect(long jarg1, String jarg2, String jarg3);
  public final static native int jack_disconnect(long jarg1, String jarg2, String jarg3);
  public final static native String jack_port_name(long jarg1);
  public final static native Object jack_port_get_buffer(long jarg1, int jarg2);
//  public final static native int jack_set_process_callback(long jarg1, Object jarg2);
  public final static native long jack_port_by_name(long jarg1, String jarg2);
  public final static native int jack_port_get_latency(long jarg1);
  public final static native int jack_port_get_total_latency(long jarg1, long jarg2);
  public final static native int jack_frames_since_cycle_start(long jarg1);
  public final static native int jack_frame_time(long jarg1);
  public final static native int jack_last_frame_time(long jarg1);
  public final static native long jack_frames_to_time(long jarg1, int jarg2);
  public final static native int jack_time_to_frames(long jarg1, long jarg2);
  public final static native long jack_get_time();
}
