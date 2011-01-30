package mms.clock;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mms.MusicalAgent;

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
		
		public long getCurrentTime() {
			return (System.currentTimeMillis() - referenceStartTime);
//			return (System.nanoTime() - referenceNanoStartTime);
		}
		
		public void schedule(Agent a, Runnable b, long wakeupTime) {
			long wakeupMili = wakeupTime;
			long now = getCurrentTime();
			if (wakeupMili > now) {
				scheduler.schedule(b, wakeupMili - now, TimeUnit.MILLISECONDS);
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
