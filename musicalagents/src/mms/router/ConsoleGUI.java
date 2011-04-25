package mms.router;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLCodec.CodecException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.StringACLCodec;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JTextArea;

import mms.Command;

public class ConsoleGUI extends JFrame implements RouterClient {

    private String myAddress = "/console/gui";
    
//    public RouterHelper router;

	private JPanel contentPane;
	private JTextField txtCommand;
	private JTextArea textArea;
	private JButton btnNewButton;

	private RouterAgent routerAgent;
	private JTextField txtRecipient;
	
	/**
	 * Create the frame.
	 */
	public ConsoleGUI(RouterAgent routerAgent) {
		this.routerAgent = routerAgent;
		
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 642, 415);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		txtCommand = new JTextField();
		txtCommand.setBounds(10, 308, 606, 20);
		contentPane.add(txtCommand);
		txtCommand.setColumns(10);
		
		txtRecipient = new JTextField();
		txtRecipient.setColumns(10);
		txtRecipient.setBounds(10, 277, 606, 20);
		contentPane.add(txtRecipient);
		
		textArea = new JTextArea();
		textArea.setBounds(10, 11, 606, 255);
		contentPane.add(textArea);
		textArea.setEditable(false);
		
		btnNewButton = new JButton("Send Command");
		txtRecipient.setText("/mms/ENVIRONMENT");
		txtCommand.setText("TESTE :key valeu :ke2 value composed");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
		        // Verifica o endereço
		        String recipient = txtRecipient.getText();
	        	Command cmd = Command.parse(txtCommand.getText());
	        	cmd.addParameter("recipient", recipient);
		        // Se tivermos um endereço e algum comando após
		        if (cmd == null || recipient == null) {
		        	textArea.append("[ConsoleGUI] Malformed address and/or command\n");
		        } else {
	            	textArea.append("[ConsoleGUI] Sending to '" + recipient + "' command '" + cmd + "'\n");
	        		sendCommand(cmd);
		        }
//		        txtCommand.setText("");
			}
		});
		btnNewButton.setBounds(467, 339, 149, 26);
		contentPane.add(btnNewButton);
		
	}

	@Override
	public String getAddress() {
		return myAddress;
	}

	@Override
	public void receiveCommand(Command cmd) {
    	textArea.append("[ConsoleGUI] Command received from : " + cmd + "\n");
	}

	@Override
	public void sendCommand(Command cmd) {
		routerAgent.processCommand(cmd);
	}

	@Override
	public void processCommand(Command cmd) {
		
	}
}
