import java.io.*;
import java.net.*;
import java.util.*;

//Created By Gustavo Peralta


public class ChatServer{
    public class UThread extends Thread{
        private Socket uSocket;
        private ChatServer server;
        private PrintWriter twriter;

        //constructor
        public UThread(ChatServer cServer, Socket tsocket){
            this.uSocket = tsocket;
            this.server = cServer;
        }


        public void run(){
            try {
                //setup input
                BufferedReader infromclient = new BufferedReader(new InputStreamReader(uSocket.getInputStream()));
                
                //output
                twriter = new PrintWriter(uSocket.getOutputStream(),true);

                

                //get info from client
                //0 is the username and 1 is the message
                String[] clientPair = new String[2];
                
                //get username
                clientPair[0] = infromclient.readLine();
				clientPair[1] = infromclient.readLine();
                System.out.println(clientPair[0]+" Connected, Adding to active users");
                activeUsers.add(clientPair[0]);
                printUsers();
                twriter.flush();
                synchronized(this){messagQueue.add(clientPair);}
				server.sendToAll(this);

                System.out.println("Current Users: "+activeUsers);

                do {
                    clientPair[0] = infromclient.readLine();
                    clientPair[1] = infromclient.readLine();
                    System.out.println("Message recieved from " + clientPair[0]+": "+clientPair[1]);
                    synchronized(this){messagQueue.add(clientPair);}
                    for (String[] string : messagQueue) {
                        System.out.println("Messages in queue"+string[0]+string[1]);    
                    }
                    server.sendToAll(this);
                }while (!clientPair[1].equals("."));
                server.removeUser(clientPair[0], this);
				twriter.println("OK");
                uSocket.close();





                //get the username of the client that is joining
                
            } catch (Exception e) {
                //TODO: handle exception
            }

        }

        void printUsers(){
            if (server.currentUsers()) {
                twriter.println(".\n"+server.getActiveUsers());
                
            }else{
                twriter.println("None");
            }
        }
        //sends the message in the two parts username\n message\n
        void sendMessage(String mess[]){
            twriter.println(mess[0]);
            twriter.println(mess[1]);
            twriter.flush();
        }
        
    }


    //port
    private int port;
    //using sets since order does not matter and we can not have repetetive users
    private Set<UThread> uThreads = new HashSet<>();
    private Set<String> activeUsers = new HashSet<>();
    //queue of messages
    private Queue<String[]>messagQueue = new LinkedList<String[]>();


    //constructor
    public ChatServer(){
        this.port = 8080;
    }
	
	public ChatServer(int port) {
		this.port = port;
	}


    //create a new conection and thread
    public void newClientConn() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("listening on port " + port + "...");
            while (true) {
                Socket welcomeSocket = serverSocket.accept();
                System.out.println("Connected");
                UThread User = new UThread(this, welcomeSocket);
                //keep track of the users
                uThreads.add(User);
                User.start();
                
            }



        } catch (Exception e) {
            //TODO: handle exception
            e.printStackTrace();
        }

    }
    
    //send the message to all the connected clients except the original one
    public void sendToAll(UThread u){
        for (String string[] : messagQueue) {
            String[] s;
            synchronized(this){s = messagQueue.poll();}
             
            for (UThread x : uThreads) {
                    if(x != u){
                    x.sendMessage(s);

                    }

            }
    
        }
        
    
    }

    //remove the user from the active user list so messages dont get sent to no one
    public void removeUser(String name, UThread uT){
        //remove user from the active user list
        boolean removed = activeUsers.remove(name);

        if (removed) {
            uThreads.remove(uT);
            System.out.println(name+" User was removed");
        }

    }
    
    //get currently logged in users
    Set<String> getActiveUsers(){
        return this.activeUsers;
    }

    //check if there is anyone logged on
    boolean currentUsers(){
        return !this.activeUsers.isEmpty();
    }

    
    public static void main(String[] args) {
		ChatServer cServer = null;
        if(args.length > 0) cServer = new ChatServer(Integer.valueOf(args[0]));
		else cServer = new ChatServer();
        cServer.newClientConn();

        
    }

    

}
