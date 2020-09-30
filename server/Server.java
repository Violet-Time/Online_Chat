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

public class Server {

    private static final String ADDRESS = "127.0.0.1";
    private static final int PORT = 23456;
    private int countSessions = 0;
    private int numLastSession = 0;
    protected Chat chat = new Chat();

    public static void main(String[] args) {
        Server server = new Server();
        server.listenAndConnectClients();
    }

    public void listenAndConnectClients() {

        try (ServerSocket server = new ServerSocket(PORT, 50, InetAddress.getByName(ADDRESS))) {
            server.setSoTimeout(1000);
            System.out.println("Server started!");
            while (true) {
                try {
                    try {
                        new Thread(new Session(server.accept(), chat)).start();
                    } catch (SocketTimeoutException e) {
                        if (chat.getCountSessions() < 1) {
                            chat.close();
                            server.close();
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
}
