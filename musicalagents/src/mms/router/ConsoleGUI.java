package mms.router;

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

public class ConsoleGUI extends JFrame implements CommandClientInterface {

    private String myAddress = "/console/gui";
    
    public RouterHelper router;

	private JPanel contentPane;
	private JTextField textField;
	private JTextArea textArea;
	private JButton btnNewButton;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ConsoleGUI frame = new ConsoleGUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public ConsoleGUI() {
		setResizable(false);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 642, 415);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		textField = new JTextField();
		textField.setBounds(10, 308, 606, 20);
		contentPane.add(textField);
		textField.setColumns(10);
		
		textArea = new JTextArea();
		textArea.setBounds(10, 11, 606, 286);
		contentPane.add(textArea);
		textArea.setEditable(false);
		
		btnNewButton = new JButton("Send Command");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
		        // Verifica o endereço
		        String[] str = textField.getText().split(" ");
		        String recipient = str[0];
		        // Se tivermos um endereço e algum comando após
		        if (recipient.length() > 0 && str.length > 1) {
			        String message = str[1];
                    if (router != null) {
                    	textArea.append("[ConsoleGUI] Sending to '" + recipient + "' command '" + str[1] + "'\n");
                		router.sendCommand(myAddress, recipient, message);
                    } else {
                    	textArea.append("[ConsoleGUI] There is no router connected\n");
                    }
		        } else {
		        	textArea.append("[ConsoleGUI] Malformed address and/or command\n");
		        }
		        textField.setText("");
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
	public void receiveCommand(String sender, String recipient, String msg) {
    	textArea.append("[ConsoleGUI] Command received from '" + sender + "': " + msg + "\n");
	}

	@Override
	public void sendCommand(String sender, String recipient, String msg) {
	}

	@Override
	public void processCommand(String sender, String recipient, String msg) {
		
	}
}
