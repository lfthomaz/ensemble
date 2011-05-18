package mms.world;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import mms.EnvironmentAgent;
import mms.MusicalAgent;
import mms.Parameters;
import mms.clock.VirtualClockHelper;
import mms.world.law.Law;

/**
 * Represents the actual state of the world, with all its entities.
 */
//TODO Criar métodos genéricos para obter o estado do agente no mundo, posição etc...
public class World {
	
	/**
	 * Locks
	 */
	protected Lock lock = new ReentrantLock();
	
	/**
	 * Parameters
	 */
	Parameters parameters = null;

	/**
	 * Environment Agent
	 */
	protected EnvironmentAgent envAgent;
	
	/**
	 * World definition
	 */
	public int 		dimensions;
	public String 	structure;
	public String 	form_type;
	public double 	form_size;
	public double 	form_size_half;
	public boolean 	form_loop;
	
	/**
	 * Laws
	 */
	private HashMap<String,Law> laws = new HashMap<String,Law>();
	
	/**
	 * Table with entities' state
	 */
	// TODO Na hora da criação, fine tune no tamanho e no load factor
    protected HashMap<String, EntityState> entities = new HashMap<String, EntityState>();

    /**
     * World GUI
     */
    // TODO permitir inserir qualquer tipo de GUI (?)
    protected WorldGUI gui;
    
    /**
     * Virtual Clock
     */
    protected VirtualClockHelper clock;
    
    // Performance
	public int calls = 0;
	public long time_1 = 0;
	public long time_2 = 0;

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}
	
	public Parameters getParameters() {
		return this.parameters;
	}

	/**
     * Constructor
     */
    public void start(EnvironmentAgent envAgent) {

    	this.envAgent = envAgent;
    	this.clock = envAgent.getClock();
    	
    	// Laws
    	if (getParameters().containsKey("LAW")) {
	    	String[] laws = getParameters().get("LAW").split(" ");
	    	for (int i = 0; i < laws.length; i++) {
				addLaw(laws[i], null); 
			}
    	}
    	
    	try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
//		System.out.println("[WORLD] " + "Initialized");
		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":WORLD] " + "Initialized");
	
    }
    
    /**
     * Add an entity to the virtual World (agent, obstacle etc.)
     * @param entityName
     * @param state
     */
    public final boolean addEntity(String entityName, Parameters parameters) {

    	boolean result = false;

    	lock.lock();
    	try {
    		if (!entities.containsKey(entityName)) {
    			
    			// Creates a new EntityState for the Entity
    			EntityState state = new EntityState();
		    	entities.put(entityName, state);

		    	// Checks for defaults attributes for an entity
		    	// TODO ISSO NÃO VALE PARA O LM!!!
		    	Vector position = null;
		    	if (parameters.containsKey("POSITION")) {
		    		position = Vector.parse(parameters.get("POSITION"));
		    	} else {
		    		position = new Vector(dimensions);
		    	}
	    		state.attributes.put("POSITION", position);
	    		
	    		// Calls user implemented method
	    		entityAdded(entityName);
	    		
		    	result = true;
		    	
    		}			
    	} finally {
    		lock.unlock();
    	}
		
		return result;
    	
    }
    
    /**
     * Remove an entity from the Virtual World
     * @param entityName
     */
    public final void removeEntity(String entityName) {
    	
		// Calls user implemented method
		entityRemoved(entityName);

		// Removes entity from the world
		entities.remove(entityName);
    	
    }
    
    public final Set<String> getEntityList() {
    	
    	return entities.keySet();
    	
    }
    
    //--------------------------------------------------------------------------------
	// Laws
	//--------------------------------------------------------------------------------

    public final void addLaw(String className, Parameters arguments) {
		try {
			// Creates a Law instance
			Class lawClass = Class.forName(className);
			Law law = (Law)lawClass.newInstance();
			// Configures this Law
			law.setWorld(this);
			law.setParameters(arguments);
			law.configure();
			// Adicionar na tabela
			if (laws.containsKey(law.getType())) {
				System.err.println("ERROR: There is already a Law with type " + law.getType());
				return;
			} else {
				laws.put(law.getType(), law);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.err.println("ERROR: Not possible to create an instance of " + className);
		} catch (InstantiationException e) {
			e.printStackTrace();
			System.err.println("ERROR: Not possible to create an instance of " + className);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			System.err.println("ERROR: Not possible to create an instance of " + className);
		}
	}
		
    public final void removeLaw(String type) {
    	if (laws.containsKey(type)) {
			laws.remove(type);
		} else {
			System.err.println("["+envAgent.getAgentName()+"] Law " + type + " does not exist.");
		}
    }
    
    /**
     * Changes a state based in a law 
     * @param type
     * @param parameters
     * @param oldState
     * @return
     */
    public final Law getLaw(String type) {
    	
    	return laws.get(type);
    	
    }

    //--------------------------------------------------------------------------------
	// GUI
	//--------------------------------------------------------------------------------

    public final WorldGUI getWorldGUI() {
    	
    	return gui;
    	
    }
    
    public final void setWorldGUI(WorldGUI gui) {
    	
    	this.gui = gui;
    	
    }
    
    //--------------------------------------------------------------------------------
	// State Management methods
	//--------------------------------------------------------------------------------

    /**
     * Retorna o estado atual de uma entidade
     * @return variável do estado de uma entidade
     */
    public Object getEntityStateAttribute(String entityName, String attribute) {

    	return (entities.get(entityName)).getEntityStateAttribute(attribute);
    	
    }

    public void addEntityStateAttribute(String entityName, String attribute, Object value) {

    	EntityState entity = entities.get(entityName);
    	if (entity != null) {
        	entity.attributes.put(attribute, value);
    	} else {
    		System.err.println("[ERROR] EntityState does not exist!");
    	}
    
    }
    
    public Object removeEntityStateAttribute(String entityName, String attribute) {

    	return entities.get(entityName).attributes.remove(attribute);
    
    }
    
    //--------------------------------------------------------------------------------
	// User implemented methods
	//--------------------------------------------------------------------------------
    
	/**
	 * 
	 */
	public void configure() {
		this.dimensions = Integer.valueOf(parameters.get("dimensions", "3"));
		this.structure	= parameters.get("structure", "continuous");
    	String form = getParameters().get("form");
    	if (form != null) {
    		String[] str = form.split(":");
    		if (str[0].equals("cube") && str.length == 3) {
        		this.form_type 		= str[0];
    			this.form_size 		= Double.valueOf(str[1]); 
    			this.form_size_half = form_size / 2;
    			this.form_loop 		= str[2].equals("loop") ? true : false;
    		}
    	} else {
        		this.form_type 		= "infinite";
    			this.form_size 		= Double.MAX_VALUE; 
    			this.form_size_half = Double.MAX_VALUE / 2;
    			this.form_loop 		= false;
    	}
	}

	/**
	 * User initialization method
	 * @param parameters
	 * @throws Exception
	 */
	protected void init() throws Exception {
//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "init()");
	}
	
	/**
	 * Called when an entity is added from the World. Must be overrided by the user.
	 * @param entityName
	 */
	protected void entityAdded(String entityName) {
//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "entityAdded()");
	}

	/**
	 * Called when an entity is removed from the World. Must be overrided by the user.
	 * @param entityName
	 */
	protected void entityRemoved(String entityName) {
//		MusicalAgent.logger.info("[" + envAgent.getAgentName() + ":" + getEventType() + "] " + "entityRemoved()");
	}

}
