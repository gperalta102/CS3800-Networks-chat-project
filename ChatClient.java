import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Chat client that connects to a local server and allows messages to be sent to the server.
 * @author Lam Lieu
 * */
public class ChatClient {
  private Socket socket = null;
  private BufferedReader inFromServer = null;
  private DataOutputStream outToServer = null;

  private String userName = "";

  /**
   * Makes a connection to the server and send the username to the server.
   *
   * @param userName Required to connect to the server
   * */
  public ChatClient(String userName) {
    this.userName = userName;
    connectToServer();
    sendUsernameToServer(userName);
  }

  /**
   * Makes a connection to the server and send the username to the server.
   *
   * @param userName Required to connect to the server
   * @param ip_address IP address of the server
   * @param port Port number of the server
   * */
  public ChatClient(String userName, String ip_address, int port) {
    this.userName = userName;
    connectToServer(ip_address, port);
    sendUsernameToServer(userName);
  }

  /**
   * Send a message to the server.
   *
   * @param message Message to be sent to the server
   * */
  public void sendMessage(String message) {
    try {
      outToServer.writeBytes(userName + "\n");
      outToServer.writeBytes(message + "\n");
      outToServer.flush();
    } catch (IOException e) {
      System.out.println("Error" + e.getMessage());
    }
  }

  /**
   * Reads a message sent from the server.
   *
   * @return Returns the ACK as a string
   * */
  public String[] readMessageFromServer()  {
    String[] messageComplete = new String[2];

    try {
      // Gets username
      messageComplete[0] = inFromServer.readLine();
      // Gets message
      messageComplete[1] = inFromServer.readLine();
    } catch (IOException e) {
      System.out.println("Error" + e.getMessage());
    }
    return messageComplete;
  }

  /**
   * Connects to the server using a socket and sets up the BufferedReader and DataOutputStream to
   * send messages to the server and get ACKs from the server
   * */
  private void connectToServer() {
    try {
      socket = new Socket("localhost", 8080);
      inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      outToServer = new DataOutputStream(socket.getOutputStream());
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  /**
   * Connects to the server using a socket and sets up the BufferedReader and DataOutputStream to
   * send messages to the server and get ACKs from the server
   * @param ip_address IP address of the server
   * @param portNum Port number of the server
   * */
  private void connectToServer(String ip_address, int portNum) {
    try {
      socket = new Socket(ip_address, portNum);
      inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      outToServer = new DataOutputStream(socket.getOutputStream());
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  /**
   * Sends the username as a String to the server.
   *
   * @param userName User inputted username
   * */
  private void sendUsernameToServer(String userName) {
    try {
      outToServer.writeBytes(userName + "\n" + ".\n");
      outToServer.flush();
    } catch (IOException e) {
      System.out.println("Error" + e.getMessage());
    }
  }
}
