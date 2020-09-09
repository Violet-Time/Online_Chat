package chat.server;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

public class Session implements Runnable {

    private final Socket socket;
    private User user;
    private String friend;
    private Output myOutput;
    private Chat chat;

    public Session(Socket socketForClient, Chat chat) {
        this.socket = socketForClient;
        this.chat = chat;
        chat.connected();
        //System.out.println("Client " + numSession + " connected!");
    }

    public void run() {
        try (
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            identifyUser(input, output);

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

    private void identifyUser(DataInputStream input, DataOutputStream output) throws IOException {
        output.writeUTF("Server: authorize or register");
    }

    private void authorization(String login, String password) {
        try {
            user = chat.authorizationUser(login, password, myOutput);
            if (user != null) {
                myOutput.print("Server: you are authorized successfully!");
            }
        } catch (Exception e) {
            try {
                myOutput.print(e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private void registration(String login, String password) {
        try {
            user = chat.registrationUser(login, password, myOutput);
            if (user != null) {
                myOutput.print("Server: you are registered successfully!");
            }
        } catch (Exception e) {
            try {
                myOutput.print(e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private void readCommand(String[] command) {
        switch (command[0]) {
            case "/auth":
                authorization(command[1], command[2]);
                break;
            case "/registration":
                registration(command[1], command[2]);
                break;
            case "/list":
                try {
                    String[] strings = chat.getListUsersOnline();
                    if (strings.length > 1) {
                        StringBuilder builder = new StringBuilder();
                        for (String str : strings) {
                            if (!str.equals(user.getName())) {
                                builder.append(str).append(" ");
                            }
                        }
                        myOutput.print("Server: online: " + builder.toString());
                    } else {
                        myOutput.print("Server: no one online");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "/chat":
                if(chat.getOnlineUser(command[1])) {
                    friend = command[1];
                    for (Message message : chat.getLastMessages(user.getName(), friend)) {
                        try {
                            myOutput.printMessage(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    try {
                        myOutput.print("Server: the user is not online!");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case "/exit":
                //System.out.println("Client " +  numSession + " disconnected!");

                try {
                    chat.leaveChat(user == null ? null : user.getName());
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                try {
                    myOutput.print("Server: incorrect command!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //System.out.println("Client " + numSession + " sent: " + msg);
                    /*
                    sent = String.valueOf(Arrays.stream(msg.split("\\s"))
                            .count());*/
                //System.out.println("Sent to client " + numSession + ": Count is " + sent);
                //output.writeUTF("Count is " + sent);
                break;
        }
    }

    private void readMsg(String msg) throws IOException {
        if (msg.length() > 1 && msg.charAt(0) == '/') {
            readCommand(msg.split("\\s"));
        } else {
            if (user == null) {
                myOutput.print("Server: you are not in the chat!");
            } else if (friend == null) {
                myOutput.print("Server: use /list command to choose a user to text!");
            } else {
                chat.putMessage(new Message(user.getName(), friend, msg));
                //myOutput.print(user.getName() + ": " + msg);
                //myOutput.printMessage();
            }
        }

    }

    class Output implements PropertyChangeListener {

        DataOutputStream output;

        Output(DataOutputStream output) {
            this.output = output;
            //chat.addPropertyChangeListener(this);
            //writeLastMessages();
        }

        public void printMessage(Message message) throws IOException {
            if (message.getSender().equals(user.getName()) || message.getSender().equals(friend)) {
                output.writeUTF(message.getSender() + ": " + message.getMessage());
            }
        }

        public void print(String str) throws IOException {
            output.writeUTF(str);
        }

        /*
        private void writeLastMessages() {
            for (Message message : chat.getLastMessages()) {
                try {
                    output.writeUTF(message.getName() + ": " + message.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
*/
        @Override
        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            try {
                output.writeUTF(((Message) propertyChangeEvent.getNewValue()).getSender() + ": " + ((Message) propertyChangeEvent.getNewValue()).getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
