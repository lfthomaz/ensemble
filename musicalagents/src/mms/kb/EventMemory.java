package mms.kb;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import mms.Parameters;
import mms.clock.TimeUnit;

public class EventMemory extends Memory {

	/**
	 *  Lock
	 */
	private Lock lock = new ReentrantLock();
	
	EventSlot head = null;

	public void init(Parameters parameters) {

	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getFirstInstant() {
		if (head != null) {
			return head.instant;
		} else {
			return -1;
		}
	}

	@Override
	public double getLastInstant() {
		if (head != null) {
			EventSlot ptr = head;
			while (true) {
				if (ptr.next == null) {
					System.out.println("las instant = " + ptr);
					return ptr.instant;
				}
				ptr = ptr.next;
			}
		} else {
			return -1;
		}
	}

	/**
	 * Cleans memory beyond time horizon (past and future parameters)
	 */
	public void updateMemory() {
		
		double horizon = ((double)clock.getCurrentTime()/1000) - past;
//		System.out.println("[" + name + "] horizon = " + horizon);
		EventSlot ptr = head;
		while (ptr != null) {
			if (ptr.instant >= horizon) {
				head = ptr;
				break;
			}
//			System.out.println("[" + name + "] Limpei - " + ptr.instant);
			ptr = ptr.next;
		}
		
	}
	
	@Override
	public Object readMemory(double instant, TimeUnit unit) {

		// TODO Sincronizar o acesso a fila!
//		updateMemory();
		
		if (head == null) {
			
			System.err.println("[ERROR] Memory has no state!");
			return null;

		} else {

			EventSlot ptr = head;

			while (ptr.next != null) {
				
				if (ptr.next.instant > instant) {
					break;
				}
				ptr = ptr.next;

			}
			
			return ptr.object;

		}
		
	}

	@Override
	public Object readMemory(double instant, double duration, TimeUnit unit) {
		return readMemory(instant, unit);
	}

	@Override
	public void resetMemory() {
		head = null;
	}

	@Override
	public void writeMemory(Object object, double instant, double duration,
			TimeUnit unit) throws MemoryException {

//		System.out.println("writeMemory = " + instant);
		
		updateMemory();

		// TODO Garantir que a unidade (unit) é a mesma
		
		// Creates a new slot to store the object
		EventSlot evt = new EventSlot();
		evt.instant = instant;
		evt.object = object;
		
		// Searches the right place to insert the slot
		if (head == null) {
			head = evt;
		} else {
			EventSlot ptr = head;
			EventSlot ptr_prev = null;
			while (ptr != null) {
				if (instant < ptr.instant) {
					break;
				}
				ptr_prev = ptr;
				ptr = ptr.next;
			}
			if (ptr_prev != null) {
				ptr_prev.next = evt;
			}
			if (ptr != null) {
				evt.next = ptr;
			}
		}
		
	}
	
	public void writeMemory(Object object) throws MemoryException {
		
		double instant = (double)clock.getCurrentTime() / 1000;
		writeMemory(object, instant, 0, TimeUnit.SECONDS);
		
	}

	class EventSlot {
		public double 		instant;
		public Object 		object;
		public EventSlot 	next;
	}

}
