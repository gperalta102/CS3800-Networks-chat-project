import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.JScrollPane;  
import javax.swing.JTextPane;  
import javax.swing.text.BadLocationException;  
import javax.swing.text.StyledDocument;  
import javax.swing.text.SimpleAttributeSet;  
import javax.swing.text.StyleConstants;  
import java.time.LocalDateTime;

/**
 * Creates a User Interface that connects with an instance of ChatClient
 * in order to provide functionality with ChatServer
 *
 * @author Juan Vera
 * @since 04-10-2020
 */
public class UI extends JFrame implements KeyListener, Runnable{
	
	//initialize UI objects
	private static final long serialVersionUID = 1L;
	private Container pane;
	private JPanel bottomPanel;
	private JTextField messageBox;
	private JButton sendButton;
	private JTextPane chatBox;
	private StyledDocument doc;
	private Thread runner;

	//local UI vars
	private String user;
	private Color color;
	private ChatClient client;
	private HashSet<String> members;
	private boolean isInitialized = false;
	
	//text styles for UI
	private SimpleAttributeSet messageStyle = new SimpleAttributeSet();
	private SimpleAttributeSet infoStyle = new SimpleAttributeSet();
	private SimpleAttributeSet userStyle = new SimpleAttributeSet();
	private SimpleAttributeSet eventStyle = new SimpleAttributeSet();	
	
	/**
	 * Main method to initialize UI and ChatClient
	 * @param arg[0] username for Client, default = Guest####
	 * @param arg[1] preferred color for UI chat, default = blue
	 * @param arg[2] global ip address, default = localhost
	 * @param arg[3] port, default = 8080
	 */
	public static void main(String[] args) {
		String name = "Guest" + Integer.toString((int)(Math.random()*9999)+1);
		Color color = Color.BLUE;
		String ip = "localhost";
		int port = 8080;
		
		if(args.length > 0) name = args[0];
		if(args.length > 1) {
			try {
				color = (Color)Color.class.getField(args[1].toUpperCase()).get(null);
			}
			catch(Exception e) {
				System.out.println(e.getMessage());
			}
		}
		if(args.length > 2) ip = args[2];
		if(args.length > 3) port = Integer.valueOf(args[3]);
		
		UI chat = null;
		try {
			chat = new UI(name, color, ip, port);
			chat.addEvent(true, name);
		}
		catch(Exception e) {
			System.out.println(e);
		}
	}
	
	/**
	 * Default Constructor for UI chat
	 * @param user username to use to connect to server
	 * @param color chat box color
	 * @param ip ip used to connected to server
	 * @param port port used for server connection
	 */
	public UI(String user, Color color, String ip, int port) {
		
		//initialize UI
		if(ip.equals("localhost") && port == 8080) client = new ChatClient(user);
		else client = new ChatClient(user, ip, port);
		this.user = user;
		this.color = color;
		members = new HashSet<String>();
		
		pane = this.getContentPane();
		bottomPanel = new JPanel();
		
		//add layout to UI
		pane.add(BorderLayout.SOUTH, bottomPanel);
		bottomPanel.setLayout(new GridBagLayout());
		
		//sets chat message styles
		StyleConstants.setForeground(infoStyle, Color.GRAY);
		StyleConstants.setFontSize(infoStyle, 13);
		StyleConstants.setForeground(messageStyle, Color.WHITE);
		StyleConstants.setFontSize(messageStyle, 20);
		StyleConstants.setAlignment(userStyle, StyleConstants.ALIGN_RIGHT);
		StyleConstants.setAlignment(eventStyle, StyleConstants.ALIGN_CENTER);
		StyleConstants.setAlignment(messageStyle, StyleConstants.ALIGN_LEFT);
		
		//create main chat box
		chatBox = new JTextPane();			
		chatBox.setEditable(false);
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(chatBox);
		JScrollPane scrollPane = new JScrollPane(panel);
		scrollPane.setViewportView(chatBox);
		pane.add(BorderLayout.CENTER, scrollPane);				
		doc = chatBox.getStyledDocument();  		
        
		//create message sending system
		messageBox = new JTextField(30);
		messageBox.addKeyListener(this);
        sendButton = new JButton("Send");
		sendButton.addActionListener(new ActionListener(){  
			public void actionPerformed(ActionEvent e){
				if(!messageBox.getText().equals(""))sendMessage();
			}	  
		});
 
		//format message sending system display
		GridBagConstraints left = new GridBagConstraints();
		left.anchor = GridBagConstraints.WEST;
		GridBagConstraints right = new GridBagConstraints();
		right.anchor = GridBagConstraints.EAST;
		right.weightx = 2.0;
		bottomPanel.add(messageBox, left);
		bottomPanel.add(sendButton, right);

		//set JFrame options
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				client.sendMessage(".");
				exit();
			}
		});
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
		this.setSize(470, 300);
		this.setTitle("Chat Client");
		
		runner = new Thread(this);
		runner.start();
		
		
	}
	/**
	 * Standby method to run when client is done updating chat
	 */
	public void run() {
		while(true) {
			String[] incomingMessage = client.readMessageFromServer();
			if(!incomingMessage[0].equals(".")) {
				try {
					addMessage(incomingMessage[1], incomingMessage[0]);				
				}
				catch(Exception e) {System.out.println(e);}
			}
			else if(!isInitialized) {
				initialize(incomingMessage[1]);
				isInitialized = true;
			}
			
		}
	}
	
	//adds previous users to members list
	private void initialize(String users) {
		int i = 0;
		while(users.charAt(i) != '[') {
			users = users.substring(1);
		}
		users = users.substring(1);
		i = 0;
		String user = "";
		while(users.charAt(i) != ']') {
			if(users.charAt(i) == ',') {
				members.add(user);
				user = "";
				i += 2;
			}
			else {
				user += users.charAt(i);				
				i++;
			}
		}
		members.add(user);
		int start = doc.getLength();
		try {
			doc.insertString(doc.getLength(), "Active Users: " + members + "\n", infoStyle);  
			doc.setParagraphAttributes(start, doc.getLength(), eventStyle, false);
		}
		catch(Exception e) {
			System.out.println("Error: " + e);
		}
	}

	/**
	 * Sends message from text box to client and chat box
	 */
	private void sendMessage() {
		String messageToSend = messageBox.getText();
		String timeToSend = getTime();
		try {
			client.sendMessage(messageToSend);
			//if no error after sendMessage, then Server OK for adding/exiting
			if(!messageToSend.equals(".")) addMessage(messageToSend, user);
			else exit();
		}
		catch(Exception e) {
			System.out.println("Error: " + e);
		}
		messageBox.setText("");
	}
	
	private void exit() {
		System.exit(0);
	}
	/**
	 * Updates chat box with new message
	 * Adds/removes logon/logoff users
	 * @param message new message to add to chat box
	 * @param user user sending message
	 */
	private void addMessage(String message, String user) throws BadLocationException {
		System.out.println(user + ": " + message);
		if(message.equals(".")) {
			if(!members.contains(user)) {
				members.add(user);
				addEvent(true, user);
			}
			else {
				members.remove(user);
				addEvent(false, user);
			}
		}
		else {
			int start = doc.getLength();
			doc.insertString(doc.getLength(), user + " - " + getTime() + "\n", infoStyle);  
			StyleConstants.setBackground(messageStyle, user.equals(this.user) ? color : Color.GRAY);
			doc.insertString(doc.getLength(), message + "\n", messageStyle); 
			doc.setParagraphAttributes(start, doc.getLength(), user.equals(this.user) ? userStyle : messageStyle, false);
			chatBox.setDocument(doc);
			chatBox.scrollRectToVisible(new Rectangle(0,chatBox.getHeight()+30,1,1));
		}
	}
	
	/**
	 * Adds logon/logoff event to the chat box
	 * @param isLogIn true if is login event, false for logoff
	 * @param user user to logon/logoff
	 */
	private void addEvent(boolean isLogIn, String user) throws BadLocationException {
		int start = doc.getLength();
		String message = String.format("%s has logged %s (%s)", user, (isLogIn?"in":"out"), getTime());
		doc.insertString(doc.getLength(), message + "\n", infoStyle);  
		doc.setParagraphAttributes(start, doc.getLength(), eventStyle, false);
		chatBox.setDocument(doc);
		chatBox.scrollRectToVisible(new Rectangle(0,chatBox.getHeight()+30,1,1));
	}
	
	
	/**
	 * Calculates time in MM:SS:mm format
	 * @return returns time as string
	 */
	private String getTime() {
		return java.time.LocalTime.now().toString().substring(0,8);
	}
	
	/**
	 * Detects enter key to send message
	 * @param e key that was typed
	 */
	public void keyTyped(KeyEvent e) {
		if(e.getKeyChar() == KeyEvent.VK_ENTER && !messageBox.getText().equals("")) sendMessage();
    }

	public void keyPressed(KeyEvent e) {
		
    }

	public void keyReleased(KeyEvent e) {
		
    }


}