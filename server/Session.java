package chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class Session implements Runnable {

    private final Socket socket;
    private final Chat chat;
    private User myUser;
    private User companion;
    private Output output;

    public Session(Socket socketForClient, Chat chat) {
        this.socket = socketForClient;
        this.chat = chat;
        chat.connected();
        //System.out.println("Client " + numSession + " connected!");
    }

    public void run() {
        try (
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())
        ) {



            output = new Output(outputStream);
            output.print("Server: authorize or register");

            while (socket.isConnected()) {
                try {
                    readMsg(inputStream.readUTF());
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

    private void authorization(String login, String password) {
        try {
            myUser = chat.authorizationUser(login, password, this);
            output.print("Server: you are authorized successfully!");
        } catch (ChatException e) {
            output.print(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registration(String login, String password) {
        try {
            myUser = chat.registrationUser(login, password, this);
            output.print("Server: you are registered successfully!");
        } catch (ChatException e) {
            output.print(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
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
                String[] strings = chat.getListUsersOnline();
                if (strings.length > 1) {
                    StringBuilder builder = new StringBuilder();
                    for (String str : strings) {
                        if (!str.equals(myUser.getLogin())) {
                            builder.append(str).append(" ");
                        }
                    }
                    output.print("Server: online: " + builder.toString());
                } else {
                    output.print("Server: no one online");
                }
                break;
            case "/chat":
                companion = chat.getOnlineUser(command[1]);
                if (companion != null) {
                    for (Message message : chat.getLastMessages(myUser, companion)) {
                        output.printMessage(message);
                    }
                } else {
                    output.print("Server: the user is not online!");
                }
                break;
            case "/kick":
                try {
                    chat.kick(myUser, chat.getUser(command[1]));
                    output.print("Server: " + command[1] + " was kicked!");
                } catch (ChatException e) {
                    output.print(e.getMessage());
                }
                break;
            case "/grant":
                try {
                    chat.grant(myUser, chat.getUser(command[1]));
                    output.print("Server: " + command[1] + " is the new moderator!");
                } catch (ChatException e) {
                    output.print(e.getMessage());
                }
                break;
            case "/revoke":
                try {
                    chat.revoke(myUser, chat.getUser(command[1]));
                    output.print("Server: " + command[1] + " is no longer a moderator!");
                } catch (ChatException e) {
                    output.print(e.getMessage());
                }
                break;
            case "/unread":
                try {
                    String[] users = chat.getUnread(myUser);
                    StringBuilder builder = new StringBuilder();
                    for (String str : users) {
                        builder.append(str).append(" ");
                    }
                    output.print("Server: unread from: " + builder.toString());
                } catch (ChatException e) {
                    output.print(e.getMessage());
                }
                break;
            case "/stats":
                output.print(chat.statistics(myUser, companion));
                break;
            case "/history":
                output.print(chat.history(myUser, Integer.parseInt(command[1])));
                break;
            case "/exit":
                exit();
                break;
            default:
                output.print("Server: incorrect command!");
                break;
        }
    }

    public void exit() {
        try {
            chat.leaveChat(myUser);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void kick() {
        chat.leaveChat(myUser);
        myUser = null;
        companion = null;
    }

    private void readMsg(String msg) {
        if (msg.length() > 1 && msg.charAt(0) == '/') {
            readCommand(msg.split("\\s"));
        } else {
            if (myUser == null) {
                output.print("Server: you are not in the chat!");
            } else if (companion == null) {
                output.print("Server: use /list command to choose a user to text!");
            } else {
                chat.sendMessage(new Message(myUser, companion, msg));
            }
        }

    }

    public Output getOutput() {
        return output;
    }

    public User getUser() {
        return myUser;
    }

    class Output {

        DataOutputStream output;

        Output(DataOutputStream output) {
            this.output = output;
        }

        public void printMessage(Message message) {
            if (message.getSender().getId() == myUser.getId()) {
                try {
                    output.writeUTF(message.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (companion != null && message.getSender().getId() == companion.getId()) {
                try {
                    output.writeUTF(message.toString());
                    message.setIsRead((byte) 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void print(String str) {
            try {
                output.writeUTF(str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
