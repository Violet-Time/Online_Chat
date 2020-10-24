package chat.server;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class for storing information about online users and the history of recent messages,
 * sends new messages to listeners
 */

public class Chat {

    private Map<String, Session> onlineUsers;
    private DataBase dataBase;
    private int countSessions = 0;
    Logger logger = Logger.getLogger(Chat.class.getName());

    public Chat() {
        this.onlineUsers = new HashMap<>();
        this.dataBase = new DataBase("D:\\Project\\Online Chat\\Online Chat\\task\\src\\chat\\server\\test.db");
    }

    public void sendMessage(Message message) {
            //logger.log(Level.WARNING, message.getSender());
        onlineUsers.get(message.getRecipient().getLogin()).getOutput().printMessage(message);//output.writeUTF(message.getSender() + ": "+ message.getMessage());
        onlineUsers.get(message.getSender().getLogin()).getOutput().printMessage(message);
        dataBase.saveMessage(message);
    }

    public synchronized User registrationUser(String login, String password, Session session) throws Exception {
        if (password.length() < 8) {
            throw new ChatException("Server: the password is too short!");
        }
        User user = dataBase.addNewUser(login, hashing(password));
        onlineUsers.put(login, session);
        return user;
    }

    public synchronized User authorizationUser(String login, String password, Session session) throws Exception {
        User user = dataBase.getUser(login, hashing(password));
        onlineUsers.put(login, session);
        return user;
    }

    public String[] getListUsersOnline() {
        return onlineUsers.keySet().toArray(String[]::new);
    }

    public String[] getUnread(User user) throws ChatException {
        return dataBase.getUnreadUsers(user);
    }

    public String statistics(User firstUser, User secondUser) {
        return dataBase.getStatistics(firstUser, secondUser);
    }
    public String history(User user, int count) {
        Message[] tmp = dataBase.getLastMessages(user, count);
        StringBuilder builder = new StringBuilder();
        builder.append("Server:\n");
        for (Message str : tmp) {
            builder.append(str.toString()).append("\n");
        }
        return builder.toString();
    }

    public Message[] getLastMessages(User firstUser, User secondUser) {
        return dataBase.getLastMessages(firstUser, secondUser);
    }

    public User getOnlineUser(String login) {
        Session session = onlineUsers.get(login);
        if (session != null) {
            return session.getUser();
        }
        return null;
    }
    public synchronized void leaveChat(User user) {
        logger.log(Level.WARNING, String.valueOf(countSessions));
        if (user != null) {
            onlineUsers.remove(user.getLogin());
        }
        countSessions--;
    }

    public void kick(User firstUser, User secondUser) throws ChatException {
        if (firstUser.getAccessLevel() <= secondUser.getAccessLevel()) {
            if (firstUser.getId() == secondUser.getId()) {
                throw new ChatException("Server: you can't kick yourself!");
            }
            if (firstUser.getAccessLevel() < 1) {
                throw new ChatException("Server: you are not a moderator or an admin!");
            }
        }
        dataBase.kick(firstUser, secondUser);
        Session session = onlineUsers.get(secondUser.getLogin());
        if (session != null) {
            session.getOutput().print("Server: you have been kicked out of the server!");
            session.kick();
        }
    }


    public void revoke(User firstUser, User secondUser) throws ChatException {
        if (firstUser.getAccessLevel() < 2) {
            throw new ChatException("Server: you are not an admin!");
        }
        dataBase.accessLevelUpdate(secondUser.getId(), 0);
        Session session = onlineUsers.get(secondUser.getLogin());
        if (session != null) {
            session.getUser().setAccessLevel(0);
            session.getOutput().print("Server: you are no longer a moderator!");
        }
    }

    public void grant(User firstUser, User secondUser) throws ChatException {
        if (firstUser.getAccessLevel() < 2) {
            throw new ChatException("Server: you are not an admin!");
        }
        if (secondUser.getAccessLevel() == 1) {
            throw new ChatException("Server: this user is already a moderator!");
        }
        dataBase.accessLevelUpdate(secondUser.getId(), 1);
        Session session = onlineUsers.get(secondUser.getLogin());
        if (session != null) {
            session.getUser().setAccessLevel(1);
            session.getOutput().print("Server: you are the new moderator now!");
        }
    }

    public int getCountSessions() {
        return countSessions;
    }

    public void connected() {
        countSessions++;
    }

    public User getUser(String login) throws ChatException {
        return dataBase.getUser(login);
    }

    public void close() {
        dataBase.closeConnection();
    }

    public static String hashing(String originalString) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < encodedHash.length; i++) {
            String hex = Integer.toHexString(0xff & encodedHash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
