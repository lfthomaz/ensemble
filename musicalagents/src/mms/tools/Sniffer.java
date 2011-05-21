package mms.tools;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

import mms.Command;
import mms.Parameters;
import mms.router.RouterAgent;
import mms.router.RouterClient;

import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.tree.TreeModel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.border.EtchedBorder;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Sniffer extends Agent implements RouterClient {

	HashMap<String, AgentInfo> 	agents;
	
	JFrame frame = new JFrame();
	
	DefaultMutableTreeNode 	selectedNode;

	DefaultMutableTreeNode 	rootNode;
	DefaultTreeModel 		treeModel;
	private JTextField txtCommand;
	private JTextField txtName;
	private JTextField txtClass;
	private JTextField txtState;
	private JTree 		tree;
	private JTextField txtType;
	private JTextField txtEvent;
	
	private JLabel lblName;
	private JLabel lblState;
	private JLabel lblClass;
	private JLabel lblType;
	private JLabel lblEvent;
	
	private JButton btnAddComponent;
	private JButton btnDestroyAgent;
	private JButton btnStartSimulation;
	private JButton btnStopSimulation;
	private JButton btnSendCommand;
	private JButton btnRemoveComponent;
	private JButton btnCreateAgent;

	/**
	 * Create the application.
	 * @wbp.parser.entryPoint
	 */
	protected void setup() {
		
		// GUI
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		initialize();
		frame.setVisible(true);
		
        // Receive messages
		this.addBehaviour(new ReceiveMessages(this));
		
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {

		frame.setBounds(100, 100, 583, 410);
		frame.getContentPane().setLayout(null);
		
		JPanel infoPanel = new JPanel();
		infoPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		infoPanel.setBounds(274, 6, 303, 259);
		frame.getContentPane().add(infoPanel);
		infoPanel.setLayout(null);
		
		lblName = new JLabel("name");
		lblName.setBounds(6, 12, 37, 16);
		infoPanel.add(lblName);
		
		lblClass = new JLabel("class");
		lblClass.setBounds(6, 46, 51, 16);
		infoPanel.add(lblClass);
		
		txtName = new JTextField();
		txtName.setEditable(false);
		txtName.setBounds(44, 6, 253, 28);
		infoPanel.add(txtName);
		txtName.setColumns(10);
		
		txtClass = new JTextField();
		txtClass.setEditable(false);
		txtClass.setColumns(10);
		txtClass.setBounds(44, 40, 253, 28);
		infoPanel.add(txtClass);
		
		lblState = new JLabel("state");
		lblState.setBounds(6, 80, 51, 16);
		infoPanel.add(lblState);
		
		txtState = new JTextField();
		txtState.setEditable(false);
		txtState.setColumns(10);
		txtState.setBounds(44, 74, 253, 28);
		infoPanel.add(txtState);
		
		lblType = new JLabel("type");
		lblType.setBounds(6, 114, 51, 16);
		infoPanel.add(lblType);
		
		txtType = new JTextField();
		txtType.setEditable(false);
		txtType.setColumns(10);
		txtType.setBounds(44, 108, 253, 28);
		infoPanel.add(txtType);
		
		lblEvent = new JLabel("event");
		lblEvent.setBounds(6, 148, 51, 16);
		infoPanel.add(lblEvent);
		
		txtEvent = new JTextField();
		txtEvent.setEditable(false);
		txtEvent.setColumns(10);
		txtEvent.setBounds(44, 142, 253, 28);
		infoPanel.add(txtEvent);
		
		btnAddComponent = new JButton("Add Component...");
		btnAddComponent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		btnAddComponent.setBounds(139, 195, 158, 29);
		infoPanel.add(btnAddComponent);
		
		btnDestroyAgent = new JButton("Destroy Agent");
		btnDestroyAgent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (selectedNode != null) {
					Command cmd = new Command(getAddress(), 
							"/mms/"+selectedNode.toString(), 
							"DESTROY_AGENT");
					cmd.addParameter("NAME", selectedNode.toString());
					sendCommand(cmd);
				}
			}
		});
		btnDestroyAgent.setBounds(139, 224, 158, 29);
		infoPanel.add(btnDestroyAgent);
		
		btnRemoveComponent = new JButton("Remove Component");
		btnRemoveComponent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (selectedNode != null) {
					Command cmd = new Command(getAddress(), 
							"/mms/"+selectedNode.getParent().toString(), 
							"REMOVE_COMPONENT");
					cmd.addParameter("NAME", selectedNode.toString());
					sendCommand(cmd);
				}
			}
		});
		btnRemoveComponent.setBounds(139, 224, 158, 29);
		infoPanel.add(btnRemoveComponent);
		
		btnCreateAgent = new JButton("Create Agent...");
		btnCreateAgent.setBounds(139, 224, 158, 29);
		infoPanel.add(btnCreateAgent);
		
		JPanel listPanel = new JPanel();
		listPanel.setBounds(6, 6, 256, 259);
		frame.getContentPane().add(listPanel);
		listPanel.setLayout(null);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(0, 0, 256, 259);
		listPanel.add(scrollPane);
		
		rootNode = new DefaultMutableTreeNode("Ensemble");
		treeModel = new DefaultTreeModel(rootNode);

		tree = new JTree(treeModel);
		tree.setShowsRootHandles(true);
		scrollPane.setViewportView(tree);
		
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(new MyTreeSelectionListener());
		
		JPanel commandPanel = new JPanel();
		commandPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		commandPanel.setBounds(6, 273, 571, 71);
		frame.getContentPane().add(commandPanel);
		commandPanel.setLayout(null);
		
		JLabel lblCustomCommand = new JLabel("Command");
		lblCustomCommand.setBounds(6, 12, 69, 16);
		commandPanel.add(lblCustomCommand);
		
		txtCommand = new JTextField();
		txtCommand.setBounds(74, 6, 491, 28);
		commandPanel.add(txtCommand);
		txtCommand.setColumns(10);
		
		btnSendCommand = new JButton("Send");
		btnSendCommand.setBounds(448, 36, 117, 29);
		commandPanel.add(btnSendCommand);
		
		btnStartSimulation = new JButton("Start Simulation");
		btnStartSimulation.setBounds(6, 348, 150, 29);
		frame.getContentPane().add(btnStartSimulation);
		
		btnStopSimulation = new JButton("Stop Simulation");
		btnStopSimulation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		btnStopSimulation.setBounds(162, 348, 150, 29);
		frame.getContentPane().add(btnStopSimulation);
		
		lblName.setVisible(false);
		txtName.setVisible(false);
		lblClass.setVisible(false);
		txtClass.setVisible(false);
		lblState.setVisible(false);
		txtState.setVisible(false);
		lblType.setVisible(false);
		txtType.setVisible(false);
		lblEvent.setVisible(false);
		txtEvent.setVisible(false);
		btnAddComponent.setVisible(false);
		btnDestroyAgent.setVisible(false);
		btnRemoveComponent.setVisible(false);
	}
	
	class MyTreeSelectionListener implements TreeSelectionListener {
		@Override
		public void valueChanged(TreeSelectionEvent e) {
			selectedNode = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
			if (selectedNode == null) {
				return;
			}	
			Object nodeInfo = selectedNode.getUserObject();
			if (nodeInfo instanceof AgentInfo) {
				AgentInfo info = (AgentInfo)nodeInfo;
				txtName.setText(info.name);
				txtClass.setText(info.className);
				txtState.setText(info.state);
				lblName.setVisible(true);
				txtName.setVisible(true);
				lblClass.setVisible(true);
				txtClass.setVisible(true);
				lblState.setVisible(true);
				txtState.setVisible(true);
				lblType.setVisible(false);
				txtType.setVisible(false);
				lblEvent.setVisible(false);
				txtEvent.setVisible(false);
				btnAddComponent.setVisible(true);
				btnDestroyAgent.setVisible(true);
				btnRemoveComponent.setVisible(false);
				btnCreateAgent.setVisible(false);
			} else if (nodeInfo instanceof ComponentInfo) {
				ComponentInfo info = (ComponentInfo)nodeInfo;
				txtName.setText(info.name);
				txtClass.setText(info.className);
				txtState.setText(info.state);
				txtType.setText(info.type);
				txtEvent.setText(info.evt_type);
				lblName.setVisible(true);
				txtName.setVisible(true);
				lblClass.setVisible(true);
				txtClass.setVisible(true);
				lblState.setVisible(true);
				txtState.setVisible(true);
				lblType.setVisible(true);
				txtType.setVisible(true);
				if (info.type.equals("SENSOR") || info.type.equals("ACTUATOR")) {
					lblEvent.setVisible(true);
					txtEvent.setVisible(true);
				} else {
					lblEvent.setVisible(false);
					txtEvent.setVisible(false);
				}
				btnAddComponent.setVisible(false);
				btnDestroyAgent.setVisible(false);
				btnRemoveComponent.setVisible(true);
				btnCreateAgent.setVisible(false);
			} else {
				lblName.setVisible(false);
				txtName.setVisible(false);
				lblClass.setVisible(false);
				txtClass.setVisible(false);
				lblState.setVisible(false);
				txtState.setVisible(false);
				lblType.setVisible(false);
				txtType.setVisible(false);
				lblEvent.setVisible(false);
				txtEvent.setVisible(false);
				btnAddComponent.setVisible(false);
				btnDestroyAgent.setVisible(false);
				btnRemoveComponent.setVisible(false);
				btnCreateAgent.setVisible(true);
			}
		}
	}
	
//	class MyTreeModelListener implements TreeModelListener {
//	    public void treeNodesChanged(TreeModelEvent e) {
//	        DefaultMutableTreeNode node;
//	        node = (DefaultMutableTreeNode)
//	                 (e.getTreePath().getLastPathComponent());
//
//	        /*
//	         * If the event lists children, then the changed
//	         * node is the child of the node we have already
//	         * gotten.  Otherwise, the changed node and the
//	         * specified node are the same.
//	         */
//	        try {
//	            int index = e.getChildIndices()[0];
//	            node = (DefaultMutableTreeNode)
//	                   (node.getChildAt(index));
//	        } catch (NullPointerException exc) {}
//
//	        System.out.println("The user has finished editing the node.");
//	        System.out.println("New value: " + node.getUserObject());
//	    }
//	    public void treeNodesInserted(TreeModelEvent e) {
//	    }
//	    public void treeNodesRemoved(TreeModelEvent e) {
//	    }
//	    public void treeStructureChanged(TreeModelEvent e) {
//	    }
//	}
	
//	/**
//	 * Launch the application.
//	 */
//	public static void main(String[] args) {
//		EventQueue.invokeLater(new Runnable() {
//			public void run() {
//				try {
//					Sniffer window = new Sniffer();
//					window.frame.setVisible(true);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
//	}

	//--------------------------------------------------------------------------------
	// JADE Message Control 
	//--------------------------------------------------------------------------------

	private final class ReceiveMessages extends CyclicBehaviour {

		MessageTemplate mt;
		
		public ReceiveMessages(Agent a) {
			super(a);
			mt = MessageTemplate.MatchConversationId("CommandRouter");
		}
		
		public void action() {
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				if (msg.getPerformative() != ACLMessage.FAILURE) {
					String sender = msg.getSender().getLocalName();
					Command cmd = Command.parse(msg.getContent());
					if (cmd != null) {
						receiveCommand(cmd);
					}
				}
			}
			else {
				block();
			}
		}
	
	}
	
	@Override
	public String getAddress() {
		return "/console/Sniffer";
	}

	@Override
	public void processCommand(Command cmd) {
	}

	@Override
	public void receiveCommand(Command cmd) {
		System.out.println("SNIFFER: Recebi mensagem - " + cmd.toString());
		if (cmd.getCommand().equals("CREATE")) {
			String agentName = cmd.getParameter("AGENT");
			// It is a component
			if (cmd.containsParameter("COMPONENT")) {
				// Searches for agent's node
				String compName = cmd.getParameter("COMPONENT");
				for (int i = 0; i < rootNode.getChildCount(); i++) {
					DefaultMutableTreeNode agentNode = (DefaultMutableTreeNode)rootNode.getChildAt(i); 
					if (agentNode.toString().equals(agentName)) {
						ComponentInfo compInfo = new ComponentInfo();
						compInfo.agent = agentName;
						compInfo.name = compName;
						compInfo.state = "CREATED";
						String className = cmd.getParameter("CLASS");
						compInfo.className = className.substring(6); 
						compInfo.type = cmd.getParameter("TYPE");
						compInfo.evt_type = cmd.getParameter("EVT_TYPE");
//						compInfo.parameters = cmd.getParameters("PARAMETERS");
						DefaultMutableTreeNode compNode = new DefaultMutableTreeNode(compInfo);
						treeModel.insertNodeInto(compNode, agentNode, agentNode.getChildCount());
					}
				}
			} 
			// It is an agent
			else {
				AgentInfo agentInfo = new AgentInfo();
				agentInfo.name = agentName;
				String className = cmd.getParameter("CLASS");
				agentInfo.className = className.substring(6); 
				agentInfo.state = "CREATED";
//				agentInfo.parameters = cmd.getParameters("PARAMETERS");
				DefaultMutableTreeNode agentNode = new DefaultMutableTreeNode(agentInfo);
				treeModel.insertNodeInto(agentNode, rootNode, rootNode.getChildCount());
			}
		}
		else if (cmd.getCommand().equals("UPDATE")) {
			// Searches for the node
			DefaultMutableTreeNode agentNode = null;
			String agentName = cmd.getParameter("AGENT");
			for (int i = 0; i < rootNode.getChildCount(); i++) {
				agentNode = (DefaultMutableTreeNode)rootNode.getChildAt(i);
				if (agentNode.toString().equals(agentName)) {
					break;
				}
				agentNode = null;
			}
			if (agentNode == null) {
				System.err.println("[Sniffer] ERROR: agent does not exist!");
				return;
			}
			// If it is a component
			if (cmd.containsParameter("COMPONENT")) {
				// Searches for comp's node
				DefaultMutableTreeNode compNode = null;
				String compName = cmd.getParameter("COMPONENT");
				for (int i = 0; i < agentNode.getChildCount(); i++) {
					compNode = (DefaultMutableTreeNode)agentNode.getChildAt(i);
					if (compNode.toString().equals(compName)) {
						break;
					}
					compNode = null;
				}
				if (compNode == null) {
					System.err.println("[Sniffer] ERROR: component does not exist!");
					return;
				}
				if (cmd.getParameter("NAME").equals("STATE")) {
					((ComponentInfo)compNode.getUserObject()).state = cmd.getParameter("VALUE");
				}
			}
			// If it is an agent
			else {
				if (cmd.getParameter("NAME").equals("STATE")) {
					((AgentInfo)agentNode.getUserObject()).state = cmd.getParameter("VALUE");
				}
			}
		}
		else if (cmd.getCommand().equals("DESTROY")) {
			DefaultMutableTreeNode agentNode = null;
			String agentName = cmd.getParameter("AGENT");
			for (int i = 0; i < rootNode.getChildCount(); i++) {
				agentNode = (DefaultMutableTreeNode)rootNode.getChildAt(i);
				if (agentNode.toString().equals(agentName)) {
					break;
				}
				agentNode = null;
			}
			// If it is a component
			if (cmd.containsParameter("COMPONENT")) {
				// Searches for comp's node
				DefaultMutableTreeNode compNode = null;
				String compName = cmd.getParameter("COMPONENT");
				for (int i = 0; i < agentNode.getChildCount(); i++) {
					compNode = (DefaultMutableTreeNode)agentNode.getChildAt(i);
					if (compNode.toString().equals(compName)) {
						treeModel.removeNodeFromParent(compNode);
						return;
					}
					compNode = null;
				}
			} 
			// If it is an agent
			else {
				treeModel.removeNodeFromParent(agentNode);
			}
		}
	}

	@Override
	public void sendCommand(Command cmd) {
		System.out.println("[Sniffer] sendCommand(): " + cmd);
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.addReceiver(new AID("Router", AID.ISLOCALNAME));
		msg.setConversationId("CommandRouter");
		msg.setContent(cmd.toString());
    	send(msg);
	}
	
}

class AgentInfo {
	String 							name;
	String 							className;
	String 							state;
	Parameters 						parameters;
	
	@Override
	public String toString() {
		return name;
	}
}

class ComponentInfo {
	String 		agent;
	String 		name;
	String 		className;
	String 		type;
	String 		state;
	String 		evt_type;
	Parameters 	parameters;

	@Override
	public String toString() {
		return name;
	}
}

//class EnvironmentInfo {
//	String 		name;
//	
//}