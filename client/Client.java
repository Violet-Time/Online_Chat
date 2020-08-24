package chat.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client implements Runnable{

    private static final String ADDRESS = "127.0.0.1";
    private static final int PORT = 23456;

    public static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {

        Client  client =  new Client();
        client.connect();

    }

    public void connect() {
        try (
            Socket socket = new Socket(InetAddress.getByName(ADDRESS), PORT);
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {

            System.out.println("Client started!");


            new Thread(() -> {  //Streaming text from the server to the console
                try {
                    while (socket.isConnected()) {
                        System.out.println(input.readUTF());
                    }
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }).start();

            String msg;

            while (true) {  //Entering text from console and sending to server

                msg = scanner.nextLine();

                switch (msg) {
                    case "/exit":
                        output.writeUTF(msg);
                        socket.close();
                        break;
                    default:
                        output.writeUTF(msg);
                        break;
                }

                if (socket.isClosed()) {
                    break;
                }
            }

        } catch (IOException e) {
            //e.printStackTrace();
        }

    }

    @Override
    public void run() {
        connect();
    }
}
