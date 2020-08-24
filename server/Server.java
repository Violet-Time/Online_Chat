package chat.server;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

public class Server implements Runnable {

    private static final String ADDRESS = "127.0.0.1";
    private static final int PORT = 23456;
    private int countSessions = 0;
    private int numLastSession = 0;
    protected Chat chat = new Chat();

    public static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }

    public void run() {

        try (ServerSocket server = new ServerSocket(PORT, 50, InetAddress.getByName(ADDRESS))) {
            server.setSoTimeout(1000);
            System.out.println("Server started!");
            //Socket socket = null;
            while (true) {
                try {
                    try {
                        new Thread(new Session(server.accept())).start();
                    } catch (SocketTimeoutException e) {

                        if (countSessions < 1) {
                            //e.printStackTrace();
                            break;
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class Session implements Runnable {

        private final Socket socket;
        private Output myOutput;
        private int numSession;
        private String name;

        public Session(Socket socketForClient) {
            this.socket = socketForClient;
            numSession = ++numLastSession;
            countSessions++;
            System.out.println("Client " + numSession + " connected!");

        }

        void readMsg(String msg) throws IOException{
            switch (msg) {
                case "/exit":
                    System.out.println("Client " +  numSession + " disconnected!");
                    chat.removePropertyChangeListener(myOutput);
                    countSessions--;
                    socket.close();
                    break;
                default:
                    chat.putMessage(new Message(name, msg));
                    //System.out.println("Client " + numSession + " sent: " + msg);
                    /*
                    sent = String.valueOf(Arrays.stream(msg.split("\\s"))
                            .count());*/
                    //System.out.println("Sent to client " + numSession + ": Count is " + sent);
                    //output.writeUTF("Count is " + sent);
                    break;
            }
        }

        public void run() {
            try (
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream())
            ) {


                output.writeUTF("Server: write your name");

                while (true) {
                    name = input.readUTF();
                    if (chat.addUser(name)) {
                        break;
                    }
                    output.writeUTF("Server: This name is already taken! Choose another one.");
                }

                myOutput = new Output(output);

                while (socket.isConnected()) {
                    try {
                        readMsg(input.readUTF());
                    } catch (SocketException e) {
                        break;
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                }

                while (socket.isConnected()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        class Output implements PropertyChangeListener {

            DataOutputStream output;

            Output(DataOutputStream output) {
                this.output = output;
                chat.addPropertyChangeListener(this);
                writeLastMessages();
            }

            private void writeLastMessages() {
                for (Message message : chat.getLastMessages()) {
                    try {
                        output.writeUTF(message.getName() + ": " + message.getMessage());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                try {
                    output.writeUTF(((Message) propertyChangeEvent.getNewValue()).getName() + ": " + ((Message) propertyChangeEvent.getNewValue()).getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
