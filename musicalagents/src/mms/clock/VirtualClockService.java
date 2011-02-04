package mms.clock;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import mms.MusicalAgent;
import mms.clock.TimeUnit;

import jade.core.Agent;
import jade.core.BaseService;
import jade.core.ServiceException;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.util.Logger;

public class VirtualClockService extends BaseService {

	//----------------------------------------------------------
	// Log
	public static Logger logger = Logger.getMyLogger(MusicalAgent.class.getName());

	//----------------------------------------------------------
	// Service name
	public static final String NAME = "VirtualClock";
	
	private long referenceStartTime;
	private long referenceNanoStartTime;
	
	private final static ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(20);
	
	@Override
	public String getName() {
		return NAME;
	}

	public void boot(jade.core.Profile p) throws ServiceException {
		super.boot(p);

		referenceStartTime = System.currentTimeMillis();
		referenceNanoStartTime = System.nanoTime();
		MusicalAgent.logger.info("[" + NAME + "] " + "Reference Time = " + referenceStartTime);
	}
	
	public VirtualClockHelper getHelper(Agent a) {
		return new VirtualClockHelperImp();
	}
	
	public class VirtualClockHelperImp implements VirtualClockHelper {

		public void init(Agent a) {
		}
		
		public double getCurrentTime(TimeUnit unit) {
			double ret = 0.0;
			long now = System.currentTimeMillis() - referenceStartTime;
			switch (unit) {
			case SECONDS:
				ret = ((double)now)/1000;
				break;
			case MILLISECONDS:
				ret = (double)now;
				break;
			}
			return ret;
		}
		
		public void schedule(Agent a, Runnable b, long wakeupTime) {
			long wakeupMili = wakeupTime;
			long now = (long)getCurrentTime(TimeUnit.MILLISECONDS);
			if (wakeupMili > now) {
				scheduler.schedule(b, wakeupMili - now, java.util.concurrent.TimeUnit.MILLISECONDS);
			} else {
				//System.out.println("ERRO!");
				scheduler.execute(b);
			}
//			System.out.println("(" + now + ") " + a.getLocalName() + " vai acordar em \t" + (wakeupMili - now) + "\t(" + System.currentTimeMillis() + ")");
		}

		@Override
		public void execute(Agent a, Runnable b) {
			scheduler.execute(b);
		}

		@Override
		public void updateClock(long units) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void updateClock() {
			// TODO Auto-generated method stub
			
		}

	}
		
}
