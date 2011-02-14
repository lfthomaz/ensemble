package mms.world.law;

import mms.movement.MovementState;
import mms.world.EntityState;
import mms.world.Vector;
import mms.world.World;

public class MovementLaw extends Law {

	// Physical constants of the World
	// TODO Pode estar no mundo também
	private double gravity = 10.0;
	private double friction_coefficient = 0.0;
	
	// temporary variable
	private Vector frictionAcceleration;

	// time
	public long time_1 = 0;
	public long time_2 = 0;
	public long time_3 = 0;
	public long time_4 = 0;
	public long time_5 = 0;
	public long time_6 = 0;
	
	@Override
	public boolean configure() {
		setType("MOVEMENT");
		frictionAcceleration = new Vector(world.dimensions);
		// TODO Pegar os parâmetros de gravidade/fricção
		warmup();
		return true;
	}
	
	public void warmup() {
		
		MovementState prevState = new MovementState(world.dimensions);
		prevState.instant = 0;
		prevState.velocity.setValue(0, 10);
		prevState.angularVelocity.setValue(0, 1);

		MovementState newState = new MovementState(world.dimensions);

		for (int j = 0; j < 20000; j++) {
			changeState(prevState, 10.0, newState);
		}
		
		prevState.velocity.zero();
		prevState.velocity.setValue(0, 10);
		prevState.acceleration.zero();
		prevState.acceleration.setValue(0, 10);

		for (int j = 0; j < 20000; j++) {
			changeState(prevState, 10.0, newState);
		}
		
		prevState.velocity.zero();
		prevState.acceleration.zero();
		prevState.acceleration.setValue(0, 50);
	
		for (int j = 0; j < 20000; j++) {
			changeState(prevState, 10.0, newState);
		}

	}
	
	@Override
	public void changeState(final State prevState, double instant, State newState) {
		
//		long start = System.nanoTime();
		// If not the right kind of State, returns
		if (!(prevState instanceof MovementState) && !(newState instanceof MovementState)) {
			System.err.println("[MovementLaw] Not the right kind of state!");
			return;
		}
		
		MovementState movPrevState = (MovementState)prevState;
		MovementState movNewState = (MovementState)newState;
//		time_1 = time_1 + (System.nanoTime() - start);

//		start = System.nanoTime();
		// Copies the prevState into newState
		movPrevState.copy(movNewState);
		movNewState.instant = instant;
//		time_2 = time_2 + (System.nanoTime() - start);

		// Does any necessary calculation
		double interval = instant - movPrevState.instant;
		if (interval > 0) {
			
			double acc = movPrevState.acceleration.magnitude;
			double vel = movPrevState.velocity.magnitude;
			// Se o corpo não está em movimento e não está sendo acelerado
			if (acc == 0 && vel == 0) {
				return;
			}
			// Se o corpo está em movimento, mas sem acelerar
			else if (acc == 0 && vel > 0) {
//				start = System.nanoTime();
				// Calcular a aceleração devido ao atrito
				movNewState.velocity.copy(frictionAcceleration);
				frictionAcceleration.normalizeVectorInverse();
				frictionAcceleration.product(gravity * friction_coefficient);
//				time_3 = time_3 + (System.nanoTime() - start);

//				start = System.nanoTime();
				// Se velocidade chegar em zero durante esse intervalo, calcular 
				double t_vel_zero = movPrevState.instant + (movPrevState.velocity.magnitude / frictionAcceleration.magnitude);
	    		if (t_vel_zero < instant) {
	    			double new_interval = t_vel_zero - movPrevState.instant;
	    			// S = S0 + V0t + 1/2atˆ2
	    			movNewState.position.add(movPrevState.velocity, new_interval);
	    			movNewState.position.add(frictionAcceleration, (0.5 * new_interval * new_interval));
					movNewState.velocity.update(0, 0, 0);
	    		} else {
	    			// S = S0 + V0t + 1/2atˆ2
	    			movNewState.position.add(movPrevState.velocity, interval);
	    			movNewState.position.add(frictionAcceleration, (0.5 * interval * interval));
	    			// V = V0 + at
	    			movNewState.velocity.add(frictionAcceleration, interval);
	    		}
//				time_4 = time_4 + (System.nanoTime() - start);
			}
			// Se o corpo foi acelerado mas ainda não está em movimento
			else if (acc > 0 && vel == 0) {
				// Verificar que tem aceleração suficiente para começar a andar
				movPrevState.acceleration.copy(frictionAcceleration);
				frictionAcceleration.normalizeVectorInverse();
				frictionAcceleration.product(gravity * friction_coefficient);
				if (acc > frictionAcceleration.magnitude) {
					frictionAcceleration.add(movNewState.acceleration);
	    			// S = S0 + V0t + 1/2atˆ2
	    			movNewState.position.add(movPrevState.velocity, interval);
	    			movNewState.position.add(frictionAcceleration, (0.5 * interval * interval));
	    			// V = V0 + at
	    			movNewState.velocity.add(frictionAcceleration, interval);

				}
			}
			// Se o corpo está em movimento e está sendo acelerado
			else {
				movPrevState.acceleration.copy(frictionAcceleration);
				frictionAcceleration.normalizeVectorInverse();
				frictionAcceleration.product(gravity * friction_coefficient);
				frictionAcceleration.add(movNewState.acceleration);
    			// S = S0 + V0t + 1/2atˆ2
    			movNewState.position.add(movPrevState.velocity, interval);
    			movNewState.position.add(frictionAcceleration, (0.5 * interval * interval));
    			// V = V0 + at
    			movNewState.velocity.add(frictionAcceleration, interval);
			}
			
			// Se o loop estiver habilitado, modifica a posição
			if (world != null && world.form_loop) {
				for (int i = 0; i < movNewState.position.dimensions; i++) {
					double value = movNewState.position.getValue(i);
					if (Math.abs(value) > world.form_size_half) {
						value = value > 0 ? value - world.form_size : value + world.form_size;
					}
					movNewState.position.setValue(i, value);
				}
				movNewState.position.updateMagnitude();
			}
			
//			start = System.nanoTime();
			// Atualizar a orientação
			if (movPrevState.angularVelocity.magnitude != 0) {
				movNewState.orientation.add(movPrevState.angularVelocity, interval);
				// TODO E se tiver duas dimensões?!
				for (int i = 0; i < movNewState.orientation.dimensions; i++) {
					movNewState.orientation.setValue(i, movNewState.orientation.getValue(i) % (2*Math.PI));
				}
				movNewState.orientation.updateMagnitude();
			}
//			time_5 = time_5 + (System.nanoTime() - start);
			
		}
		
		return;
		
	}
	
//	public static void main(String[] args) {
//	
//		MovementLaw law = new MovementLaw();
//		law.configure();
//		
//		MovementState prevState = new MovementState(3);
//		prevState.instant = 0;
//		prevState.velocity.update(10,0,0);
//
//		MovementState newState = new MovementState(3);
//
//		for (int j = 0; j < 10; j++) {
//			long start_time = System.nanoTime();
//			// Simulando um chunk_size de 250 ms
//			for (int i = 0; i < 44000; i++) {
//				law.changeState(prevState, 10.0, newState);
//			}
//			long elapsed_time = System.nanoTime() - start_time;
//			System.out.println("elapsed time = " + elapsed_time);
////			System.out.println("time_1 = " + law.time_1);
////			System.out.println("time_2 = " + law.time_2);
////			System.out.println("time_3 = " + law.time_3);
////			System.out.println("time_4 = " + law.time_4);
////			System.out.println("time_5 = " + law.time_5);
//			elapsed_time = 0;
//			law.time_1 = 0;
//			law.time_2 = 0;
//			law.time_3 = 0;
//			law.time_4 = 0;
//			law.time_5 = 0;
//		}
//		
////		System.out.println("ins = " + newState.instant);
////		System.out.println("pos = " + newState.position);
////		System.out.println("vel = " + newState.velocity);
////		System.out.println("acc = " + newState.acceleration);
////		System.out.println("angVel = " + newState.angularVelocity);
////		System.out.println("ori = " + newState.orientation);
//		
//	}

}
