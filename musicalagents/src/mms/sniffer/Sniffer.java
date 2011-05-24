package mms.sniffer;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.swing.AbstractCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import mms.Constants;
import mms.Command;
import mms.Parameters;
import mms.router.RouterClient;

import javax.swing.JPanel;
import javax.swing.JLabel;

import java.util.HashMap;

import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.border.EtchedBorder;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.border.TitledBorder;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Component;

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
	
	private JPanel pnlParameters;
	
	private JButton btnAddComponent;
	private JButton btnDestroyAgent;
	private JButton btnStartSimulation;
	private JButton btnStopSimulation;
	private JButton btnSendCommand;
	private JButton btnRemoveComponent;
	private JButton btnCreateAgent;
	private JButton btnFacts;
	private DefaultTableModel tblParametersModel;
	private JTable tblParameters;
	private JScrollPane scrollPane_1;

	/**
	 * Create the application.
	 * @wbp.parser.entryPoint
	 */
	protected void setup() {
		
		// GUI
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		frame.setTitle("Ensemble Sniffer");
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

		frame.setBounds(100, 100, 638, 552);
		frame.getContentPane().setLayout(null);
		
		JPanel infoPanel = new JPanel();
		infoPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		infoPanel.setBounds(274, 6, 348, 379);
		frame.getContentPane().add(infoPanel);
		infoPanel.setLayout(null);
		
		lblName = new JLabel("NAME");
		lblName.setBounds(6, 12, 37, 16);
		infoPanel.add(lblName);
		
		lblClass = new JLabel("CLASS");
		lblClass.setBounds(6, 46, 51, 16);
		infoPanel.add(lblClass);
		
		txtName = new JTextField();
		txtName.setEditable(false);
		txtName.setBounds(44, 6, 294, 28);
		infoPanel.add(txtName);
		txtName.setColumns(10);
		
		txtClass = new JTextField();
		txtClass.setEditable(false);
		txtClass.setColumns(10);
		txtClass.setBounds(44, 40, 294, 28);
		infoPanel.add(txtClass);
		
		lblState = new JLabel("STATE");
		lblState.setBounds(6, 80, 51, 16);
		infoPanel.add(lblState);
		
		txtState = new JTextField();
		txtState.setEditable(false);
		txtState.setColumns(10);
		txtState.setBounds(44, 74, 294, 28);
		infoPanel.add(txtState);
		
		lblType = new JLabel("TYPE");
		lblType.setBounds(6, 114, 51, 16);
		infoPanel.add(lblType);
		
		txtType = new JTextField();
		txtType.setEditable(false);
		txtType.setColumns(10);
		txtType.setBounds(44, 108, 294, 28);
		infoPanel.add(txtType);
		
		lblEvent = new JLabel("EVENT");
		lblEvent.setBounds(6, 148, 51, 16);
		infoPanel.add(lblEvent);
		
		txtEvent = new JTextField();
		txtEvent.setEditable(false);
		txtEvent.setColumns(10);
		txtEvent.setBounds(44, 142, 294, 28);
		infoPanel.add(txtEvent);
		
		btnDestroyAgent = new JButton("Destroy Agent");
		btnDestroyAgent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (selectedNode != null) {
					Command cmd = new Command(getAddress(), 
							"/" + Constants.FRAMEWORK_NAME + "/"+selectedNode.toString(), 
							"DESTROY_AGENT");
					cmd.addParameter("NAME", selectedNode.toString());
					sendCommand(cmd);
				}
			}
		});
		btnDestroyAgent.setBounds(180, 339, 158, 29);
		infoPanel.add(btnDestroyAgent);
		
		btnRemoveComponent = new JButton("Remove Component");
		btnRemoveComponent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (selectedNode != null) {
					Command cmd = new Command(getAddress(), 
							"/" + Constants.FRAMEWORK_NAME + "/"+selectedNode.getParent().toString(), 
							"REMOVE_COMPONENT");
					cmd.addParameter("NAME", selectedNode.toString());
					sendCommand(cmd);
				}
			}
		});
		btnRemoveComponent.setBounds(180, 339, 158, 29);
		infoPanel.add(btnRemoveComponent);
		
		btnCreateAgent = new JButton("Create Agent...");
		btnCreateAgent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				SnifferDialog dialog = new SnifferDialog(0);
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setResizable(false);
				dialog.setVisible(true);
				if (dialog.result) {
					Command cmd = new Command(getAddress(), 
							"/" + Constants.FRAMEWORK_NAME + "/" + Constants.ENVIRONMENT_AGENT,
							"CREATE_AGENT");
					// Returns the command string
					cmd.addParameter("NAME", dialog.txtName.getText());
					cmd.addParameter("CLASS", dialog.txtClass.getText());
					String parameters = "{";
					for (int i = 0; i < dialog.tableModel.getRowCount(); i++) {
						parameters += dialog.tableModel.getValueAt(i, 0) + "=" + dialog.tableModel.getValueAt(i, 1) + ";";
					}
					parameters += "}";
					cmd.addParameter("PARAMETERS", parameters);
					sendCommand(cmd);
				}
			}
		});
		btnCreateAgent.setBounds(180, 339, 158, 29);
		infoPanel.add(btnCreateAgent);
		
		pnlParameters = new JPanel();
		pnlParameters.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Parameters", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlParameters.setBounds(6, 180, 332, 148);
		infoPanel.add(pnlParameters);
		pnlParameters.setLayout(null);
		
		scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(10, 22, 312, 115);
		pnlParameters.add(scrollPane_1);
		
		tblParameters = new JTable();
		scrollPane_1.setViewportView(tblParameters);
		tblParameters.setRowSelectionAllowed(false);
		tblParametersModel = new DefaultTableModel(
				new Object[][] {},
				new String[] {
					"NAME", "VALUE"
				}) {
			boolean[] columnEditables = new boolean[] {
					false, true
				};
				public boolean isCellEditable(int row, int column) {
					return columnEditables[column];
				}
			};
		tblParameters.setModel(tblParametersModel);
//		tblParameters.setDefaultEditor(String.class, new MyTableEditor());
		tblParameters.getColumnModel().getColumn(0).setMinWidth(30);
		tblParameters.getColumnModel().getColumn(1).setCellEditor(new MyTableCellEditor());
		tblParameters.setBorder(new LineBorder(Color.LIGHT_GRAY));
		
		btnFacts = new JButton("Facts...");
		btnFacts.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SnifferFactsDialog dialog = new SnifferFactsDialog();
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setLocationRelativeTo(frame);
				dialog.setResizable(false);
				// Populates facts table
				Parameters facts = ((ComponentInfo)selectedNode.getUserObject()).facts;
				for (String key : facts.keySet()) {
					dialog.tableModel.addRow(new String[] {key, facts.get(key)});
				}
				dialog.setVisible(true);
				if (dialog.result) {
					Command cmd = new Command(getAddress(), 
							"/" + Constants.FRAMEWORK_NAME + "/" + selectedNode.getParent().toString() + "/" + selectedNode.toString(), 
							"UPDATE_FACTS");
					// Returns the command string
					String parameters = "{";
					for (int i = 0; i < dialog.tableModel.getRowCount(); i++) {
						parameters += dialog.tableModel.getValueAt(i, 0) + "=" + dialog.tableModel.getValueAt(i, 1) + ";";
					}
					parameters += "}";
					cmd.addParameter("FACTS", parameters);
					sendCommand(cmd);
				}
			}
		});
		btnFacts.setBounds(16, 339, 158, 28);
		infoPanel.add(btnFacts);
		
				btnAddComponent = new JButton("Add Component...");
				btnAddComponent.setBounds(16, 339, 158, 29);
				infoPanel.add(btnAddComponent);
				btnAddComponent.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						SnifferDialog dialog = new SnifferDialog(1);
						dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
						dialog.setResizable(false);
						dialog.setVisible(true);
						if (dialog.result) {
							Command cmd = new Command(getAddress(), 
									"/" + Constants.FRAMEWORK_NAME + "/" + selectedNode.toString(),
									"ADD_COMPONENT");
							// Returns the command string
							cmd.addParameter("NAME", dialog.txtName.getText());
							cmd.addParameter("CLASS", dialog.txtClass.getText());
							String parameters = "{";
							for (int i = 0; i < dialog.tableModel.getRowCount(); i++) {
								parameters += dialog.tableModel.getValueAt(i, 0) + "=" + dialog.tableModel.getValueAt(i, 1) + ";";
							}
							if (!dialog.txtEvtType.equals("")) {
								parameters += "EVT_TYPE=" + dialog.txtEvtType.getText();
							}
							parameters += "}";
							cmd.addParameter("PARAMETERS", parameters);
							sendCommand(cmd);
						}
					}
				});
				btnAddComponent.setVisible(false);
		
		rootNode = new DefaultMutableTreeNode("Ensemble");
		treeModel = new DefaultTreeModel(rootNode);
		
		JPanel listPanel = new JPanel();
		listPanel.setBounds(6, 6, 256, 379);
		frame.getContentPane().add(listPanel);
		listPanel.setLayout(null);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(0, 0, 256, 378);
		listPanel.add(scrollPane);
		
		tree = new JTree(treeModel);
		tree.setShowsRootHandles(true);
		tree.setCellRenderer(new MyTreeCellRenderer());
		scrollPane.setViewportView(tree);
		
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(new MyTreeSelectionListener());

		JPanel commandPanel = new JPanel();
		commandPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		commandPanel.setBounds(6, 396, 616, 81);
		frame.getContentPane().add(commandPanel);
		commandPanel.setLayout(null);
		
		JLabel lblCustomCommand = new JLabel("Command");
		lblCustomCommand.setBounds(6, 12, 69, 16);
		commandPanel.add(lblCustomCommand);
		
		txtCommand = new JTextField();
		txtCommand.setBounds(74, 6, 532, 28);
		commandPanel.add(txtCommand);
		txtCommand.setColumns(10);
		
		btnSendCommand = new JButton("Send");
		btnSendCommand.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Command cmd = Command.parse(txtCommand.getText());
				cmd.setSender("/console/Sniffer");
				if (selectedNode.getDepth() == 1) {
					cmd.setRecipient("/" + Constants.FRAMEWORK_NAME + "/" + selectedNode);
				} else {
					cmd.setRecipient("/" + Constants.FRAMEWORK_NAME + "/" + selectedNode.getParent() + "/" + selectedNode);
				}
				System.out.println(cmd);
				sendCommand(cmd);
			}
		});
		btnSendCommand.setBounds(489, 41, 117, 29);
		btnSendCommand.setEnabled(false);
		commandPanel.add(btnSendCommand);
		
		btnStartSimulation = new JButton("Start Simulation");
		btnStartSimulation.setBounds(6, 484, 150, 29);
		frame.getContentPane().add(btnStartSimulation);
		
		btnStopSimulation = new JButton("Stop Simulation");
		btnStopSimulation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Command cmd = new Command(getAddress(), 
						"/" + Constants.FRAMEWORK_NAME + "/" + Constants.ENVIRONMENT_AGENT,
						"STOP_SIMULATION");
				sendCommand(cmd);
			}
		});
		btnStopSimulation.setBounds(166, 484, 150, 29);
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
		pnlParameters.setVisible(false);
		btnDestroyAgent.setVisible(false);
		btnRemoveComponent.setVisible(false);
		btnFacts.setVisible(false);
	}
	
	class MyTreeCellRenderer extends DefaultTreeCellRenderer {
	    
		ImageIcon maIcon;
	    ImageIcon eaIcon;
	    ImageIcon compIcon;
	 
	    public MyTreeCellRenderer() {
	        eaIcon = new ImageIcon("media/world.png");
	        maIcon = new ImageIcon("media/eva.png");
	        compIcon = new ImageIcon("media/gear.png");
	    }
	 
	    public Component getTreeCellRendererComponent(JTree tree,
	      Object value,boolean sel,boolean expanded,boolean leaf,
	      int row,boolean hasFocus) {
	 
	        super.getTreeCellRendererComponent(tree, value, sel, 
	          expanded, leaf, row, hasFocus);
	 
	        Object nodeObj = ((DefaultMutableTreeNode)value).getUserObject();
	        // check whatever you need to on the node user object
	        if (nodeObj instanceof AgentInfo) {
	            setIcon(maIcon);
	        } else if (nodeObj instanceof ComponentInfo) {
	            setIcon(compIcon);
	        } else if (nodeObj instanceof EnvironmentInfo) {
	            setIcon(eaIcon);
	        } 
	        return this;
	    }
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
				while (tblParametersModel.getRowCount() > 0) {
					tblParametersModel.removeRow(0);
				}
				for (String key : info.parameters.keySet()) {
					tblParametersModel.addRow(new String[] {key, info.parameters.get(key)});
				}
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
				pnlParameters.setVisible(true);
				btnAddComponent.setVisible(true);
				btnDestroyAgent.setVisible(true);
				btnRemoveComponent.setVisible(false);
				btnCreateAgent.setVisible(false);
				btnSendCommand.setEnabled(true);
				btnFacts.setVisible(false);
			} else if (nodeInfo instanceof ComponentInfo) {
				ComponentInfo info = (ComponentInfo)nodeInfo;
				txtName.setText(info.name);
				txtClass.setText(info.className);
				txtState.setText(info.state);
				txtType.setText(info.type);
				txtEvent.setText(info.evt_type);
				while (tblParametersModel.getRowCount() > 0) {
					tblParametersModel.removeRow(0);
				}
				for (String key : info.parameters.keySet()) {
					tblParametersModel.addRow(new String[] {key, info.parameters.get(key)});
				}
				lblName.setVisible(true);
				txtName.setVisible(true);
				lblClass.setVisible(true);
				txtClass.setVisible(true);
				lblState.setVisible(true);
				txtState.setVisible(true);
				lblType.setVisible(true);
				txtType.setVisible(true);
				if (info.type.equals(Constants.COMP_SENSOR) || info.type.equals(Constants.COMP_ACTUATOR)) {
					lblEvent.setVisible(true);
					txtEvent.setVisible(true);
				} else {
					lblEvent.setVisible(false);
					txtEvent.setVisible(false);
				}
				pnlParameters.setVisible(true);
				btnAddComponent.setVisible(false);
				btnDestroyAgent.setVisible(false);
				btnRemoveComponent.setVisible(true);
				btnCreateAgent.setVisible(false);
				btnSendCommand.setEnabled(true);
				btnFacts.setVisible(info.type.equals(Constants.COMP_KB));
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
				pnlParameters.setVisible(false);
				btnAddComponent.setVisible(false);
				btnDestroyAgent.setVisible(false);
				btnRemoveComponent.setVisible(false);
				btnCreateAgent.setVisible(true);
				btnFacts.setVisible(false);
				btnSendCommand.setEnabled(false);
			}
		}
	}
	
	class MyTableCellEditor extends AbstractCellEditor 
						implements TableCellEditor {

		JTextField component = new JTextField();
		int row, column;
		
		@Override
		public Object getCellEditorValue() {
			String recipient;
			if (selectedNode.getDepth() == 1) {
				recipient = "/" + Constants.FRAMEWORK_NAME + "/" + selectedNode.toString(); 
			} else {
				recipient = "/" + Constants.FRAMEWORK_NAME + "/" + selectedNode.getParent().toString() + "/" + selectedNode.toString(); 
			}
			Command cmd = new Command(getAddress(), 
						recipient, 
						Constants.CMD_PARAMETER);
			cmd.addParameter("NAME", (String)tblParametersModel.getValueAt(row, column-1));
			cmd.addParameter("VALUE", ((JTextField)component).getText());
			sendCommand(cmd);
			return ((JTextField)component).getText();
		}

		@Override
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {

			((JTextField)component).setText((String)value);
			this.row = row;
			this.column = column;
			
			return component;
		}
		
	}
	
	
	
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
						compInfo.parameters = Parameters.parse(cmd.getParameter("PARAMETERS"));
						if (cmd.containsParameter("FACTS")) {
							compInfo.facts = Parameters.parse(cmd.getParameter("FACTS"));
						}
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
				agentInfo.parameters = Parameters.parse(cmd.getParameter("PARAMETERS"));
				DefaultMutableTreeNode agentNode = new DefaultMutableTreeNode(agentInfo);
				treeModel.insertNodeInto(agentNode, rootNode, rootNode.getChildCount());
				tree.expandRow(0);
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
				} else {
					((ComponentInfo)compNode.getUserObject()).parameters.put(cmd.getParameter("NAME"), cmd.getParameter("VALUE"));
				}
			}
			// If it is an agent
			else {
				if (cmd.getParameter("NAME").equals("STATE")) {
					((AgentInfo)agentNode.getUserObject()).state = cmd.getParameter("VALUE");
				} else {
					((AgentInfo)agentNode.getUserObject()).parameters.put(cmd.getParameter("NAME"), cmd.getParameter("VALUE"));
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
						tree.setSelectionPath(new TreePath(agentNode.getPath()));
						return;
					}
					compNode = null;
				}
			} 
			// If it is an agent
			else {
				treeModel.removeNodeFromParent(agentNode);
				tree.setSelectionPath(new TreePath(rootNode.getPath()));
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
	String 		name;
	String 		className;
	String 		state;
	Parameters 	parameters;
	
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
	Parameters 	facts;

	@Override
	public String toString() {
		return name;
	}
}

class EnvironmentInfo {
	String 		className;
	String		state;
	Parameters 	parameters;

	@Override
	public String toString() {
		return Constants.ENVIRONMENT_AGENT;
	}
}

class EventServerInfo {
	String 		evt_type;
	String		state;
	String 		className;
	Parameters	parameters;
	
	@Override
	public String toString() {
		return evt_type;
	}
}

class WorldInfo {
	String[] laws;
}