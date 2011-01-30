package rt;

import mms.Actuator;
import mms.Constants;
import mms.EventHandler;
import mms.MusicalAgent;
import mms.Reasoning;
import mms.Sensor;
import mms.clock.TimeUnit;
import mms.kb.Memory;
import mms.kb.MemoryException;
import mms.tools.AudioInputFile;

public class RT_Reasoning extends Reasoning {

	Actuator 	mouth;
	Memory 		mouthMemory;
	Sensor 		ear;
	Memory 		earMemory;
			
	// número de samples (frame) em um chunk
	int chunk_size;
	
	private long currentChunk 	= 0;
	private long initialTime 	= System.currentTimeMillis();

	// Buffer do Agente
	private byte[] 	buffer;
	private double[] chunk;
	private int		backup = 5;

	private double 	gain = 1.0;
	
	// Desempenho
	private long 	sentChunks	= 0;
		
	// Arquivo de áudio
	AudioInputFile in;

	@Override
	public boolean init() {
		
		// Abre o arquivo de áudio para leitura
		String filename = getAgent().getKB().readFact("filename");
		try {
			in = new AudioInputFile(filename, true);
		} catch (Exception e) {
			getAgent().logger.severe("[" + getName() + "] " + "Error in opening the file " + filename);
			return false;
		}
		
		// Verifica se existe o argumento de ganho
		if (getParameters().containsKey("gain")) {
			gain = Double.valueOf(getParameter("gain"));
		}
		
		return true;
		
	}

	@Override
	protected void eventHandlerRegistered(EventHandler evtHdl) {
		
		// Checar se é um atuador de som e adicionar na lista
		if (evtHdl instanceof Actuator && evtHdl.getEventType().equals(Constants.EVT_AUDIO)) {
			mouth = (Actuator)evtHdl;
			mouth.registerListener(this);
			mouthMemory = getAgent().getKB().getMemory(mouth.getName());
			chunk_size = Integer.parseInt(mouth.getParameter(Constants.PARAM_CHUNK_SIZE, "0"));
		}

		// Checar se é um atuador de som e adicionar na lista
		if (evtHdl instanceof Sensor && evtHdl.getEventType().equals(Constants.EVT_AUDIO)) {
			ear = (Sensor)evtHdl;
			ear.registerListener(this);
			earMemory = getAgent().getKB().getMemory(ear.getName());
		}

	}

	@Override
	public void needAction(Actuator sourceActuator, double instant, double duration) {

//		System.out.println(System.currentTimeMillis() + " " + getAgent().getLocalName() + " Entrei no needAction() - instant " + instant);

		// Le o fragmento do arquivo e transformas em float
		chunk = in.readNextChunk(chunk_size);

		// Faz qualquer alteração necessária no buffer (aplica o ganho)
		for (int i = 0; i < chunk.length; i++) {
			chunk[i] = chunk[i] * gain;
		}
		
		// Escreve na Base de Conhecimento o evento a ser enviado
		// TODO Ao invés de escrever na KB, fazer diretamente no Atuador
		try {
//			mouthMemory.writeMemoryRelative(chunk, 0, duration, TimeUnit.SECONDS);
			mouthMemory.writeMemory(chunk, instant, duration, TimeUnit.SECONDS);
//			System.out.println("Guardei na memória um evento no instante " + instant + " de duração " + duration);
		} catch (MemoryException e1) {
			MusicalAgent.logger.warning("[" + getAgent().getLocalName() + ":" + getName() + "] " + "Não foi possível armazenar na memória");
		}

		// Adicionar atrasos aleatórios para ver o que acontece!!
//			try {
//				Thread.sleep(30000);
////				Thread.sleep((long) Math.floor(Math.random() * 300));
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		
		mouth.act();

//			System.out.println(System.currentTimeMillis() + " MusicalAgent: enviei chunk de tamanho " + chunk.length);
//		System.out.println(System.currentTimeMillis() + " " + getAgent().getLocalName() + " Sai do needAction() - " + num);

	}

	@Override
	public void newSense(String eventType, double instant, double duration) {

		// Reads ear's memory
//		System.out.println("Entrei no newSense()");
		double[] buf = (double[])earMemory.readMemory(instant, duration, TimeUnit.SECONDS);

		// Analisa o evento e modifica as notas escutadas
		// notes = ...
	}

	@Override
	public void process() {
	}

}
