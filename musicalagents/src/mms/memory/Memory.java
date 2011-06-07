package mms.memory;

import jade.util.Logger;
import mms.MMSAgent;
import mms.Parameters;
import mms.clock.TimeUnit;
import mms.clock.VirtualClockHelper;

// usar TimeFormat(); para os instantes e amostras

// TODO se ele pedir em tempo, devolver o vetor interpolado, se pedir em amostras, devolver os valores do buffer
// TODO Reconversão entre taxas de amostragem (raciocionio trabalhando em outra taxa que o sensor)
// TODO Se não está acontecendo nada no ambiente, não enviar os chunks (ou então os chunks com zero são descartadas)
// TODO Raciocinio cutucar o outro (oscilidores/mixer)
// Reunião - terça
// TODO Não funciona para Batch (considerar turnos também) -> Já tem o tipo em TimeUnit
public abstract class Memory {

	// Log
//	public static Logger logger = Logger.getMyLogger(MMSAgent.class.getName());

	protected VirtualClockHelper clock;

	protected MMSAgent 	myAgent;
	protected String 	name;
	protected double	past;
	protected double	future;

	public void start(MMSAgent myAgent, String name, double past, double future, Parameters parameters) {

		this.myAgent = myAgent;
		this.name = name;
		this.past = past;
		this.future = future;
		
		// Obter o clock
		clock = myAgent.getClock();

		init(parameters);

	}
	
	/**
	 * Retorna o nome do componente ao qual está memória está associada
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Retorna o tipo de evento que essa memória armazena
	 * @return
	 */
	public String getType() {
		return name;
	}
	
	public double getPast() {
		return past;
	}
	
	public double getFuture() {
		return future;
	}
	
	public abstract void init(Parameters parameters);
	
	/**
	 * Retorna o instante mais antigo em que existe informação na memória
	 * @return
	 */
	public abstract double getFirstInstant();

	/**
	 * Retorna o último instante em que existe informação na memória
	 * @return
	 */
	public abstract double getLastInstant();
	
	/**
	 * Obtém a memória em um instante do tempo
	 * @param instant
	 * @return
	 */
	public abstract Object readMemory(double instant, TimeUnit unit);
	
	/**
	 * Obtém a memória em um dado intervalo de tempo
	 * @param initialInstant
	 * @param duration
	 * @return
	 */
	public abstract Object readMemory(double instant, double duration, TimeUnit unit);
	
	/**
	 * Limpa a memória
	 */
	public abstract void resetMemory();
	
	/**
	 * Escreve na memória a partir de um instante absoluto
	 * @param object
	 * @param instant
	 */
	public abstract void writeMemory(Object object, double instant, double duration, TimeUnit unit) throws MemoryException;

	/**
	 *
	 * @param object
	 * @throws MemoryException
	 */
	public abstract void writeMemory(Object object, double instant, TimeUnit unit) throws MemoryException;

	/**
	 *
	 * @param object
	 * @throws MemoryException
	 */
	public abstract void writeMemory(Object object) throws MemoryException;

}
