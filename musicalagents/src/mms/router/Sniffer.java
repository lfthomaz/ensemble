package mms.router;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

import mms.Command;

public class Sniffer extends JFrame implements RouterClient {

	RouterAgent 			router;
	
	DefaultMutableTreeNode 	rootNode;
	DefaultTreeModel 		treeModel;

	/**
	 * Create the application.
	 */
	public Sniffer(RouterAgent router) {
		this.router = router;
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		this.setBounds(100, 100, 450, 300);
		this.setDefaultCloseOperation(JFrame.NORMAL);
		this.getContentPane().setLayout(null);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(6, 6, 256, 266);
		this.getContentPane().add(scrollPane);
		
		rootNode = new DefaultMutableTreeNode("Ensemble");
		treeModel = new DefaultTreeModel(rootNode);
//		treeModel.addTreeModelListener(new MyTreeModelListener());
//	    createNodes(rootNode);
	    
		JTree tree = new JTree(treeModel);
		scrollPane.setViewportView(tree);
		tree.setEditable(true);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setShowsRootHandles(true);
	}
	
//	private void createNodes(DefaultMutableTreeNode top) {
//	    DefaultMutableTreeNode category = null;
//	    
//	    category = new DefaultMutableTreeNode("Dummy");
//	    top.add(category);
//	    
//	    category.add(new DefaultMutableTreeNode("Environment"));
//	    category.add(new DefaultMutableTreeNode("Dummy_1"));
//
//	    category = new DefaultMutableTreeNode("Clapping Music");
//	    top.add(category);
//	    
//	    category.add(new DefaultMutableTreeNode("Environment"));
//	    category.add(new DefaultMutableTreeNode("Listener"));
//	    category.add(new DefaultMutableTreeNode("Leader"));
//	    category.add(new DefaultMutableTreeNode("Follower_1"));
//	    category.add(new DefaultMutableTreeNode("Follower_2"));
//
//	}
	
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

	@Override
	public String getAddress() {
		return "/console/sniffer";
	}

	@Override
	public void processCommand(Command cmd) {
	}

	@Override
	public void receiveCommand(Command cmd) {
//		System.out.println("SNIFFER: Recebi mensagem - " + cmd.toString());
		if (cmd.getCommand().equals("CREATE")) {
			// Creates a new node
			String agentName = cmd.getParameter("AGENT");
			if (cmd.containsParameter("COMPONENT")) {
				// It is a component
				String compName = cmd.getParameter("COMPONENT");
				for (int i = 0; i < rootNode.getChildCount(); i++) {
					TreeNode agentNode = rootNode.getChildAt(i); 
					if (agentNode.toString().equals(agentName)) {
						DefaultMutableTreeNode compNode = new DefaultMutableTreeNode(compName);
						treeModel.insertNodeInto(compNode, (DefaultMutableTreeNode)agentNode, agentNode.getChildCount());
					}
				}
			} else {
				// It is an agent
				DefaultMutableTreeNode agentNode = new DefaultMutableTreeNode(agentName);
				treeModel.insertNodeInto(agentNode, rootNode, rootNode.getChildCount());
			}
		}
		else if (cmd.getCommand().equals("UPDATE")) {
			// Searches for the node
		}
	}

	@Override
	public void sendCommand(Command cmd) {
		
	}
	
}
