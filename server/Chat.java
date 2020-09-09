package chat.server;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class for storing information about online users and the history of recent messages,
 * sends new messages to listeners
 */

public class Chat {

    private Map<String, Session.Output> users;
    private DataBase dataBase;
    private int countSessions = 0;
    Logger logger = Logger.getLogger(Chat.class.getName());

    public Chat() {
        this.users = new HashMap<>();
        this.dataBase = new DataBase("D:\\Project\\Online Chat\\Online Chat\\task\\src\\chat\\server\\test.db");
    }
    public synchronized User registrationUser(String login, String password, Session.Output output) throws Exception {
        if (password.length() < 8) {
            throw new Exception("Server: the password is too short!");
        }
        User user = dataBase.addNewUser(login, User.hashing(password));
        if (user == null) {
            throw new Exception("Server: this login is already taken! Choose another one.");
        }
        users.put(login,output);
        return user;
    }
    public synchronized User authorizationUser(String login, String password, Session.Output output) throws Exception {
        User user = dataBase.getUser(login);
        if (user == null) {
            throw new Exception("Server: incorrect login!");
        }
        if (!user.checkPassword(User.hashing(password))){
            throw new Exception("Server: incorrect password!");
        }
        users.put(login,output);
        return user;
    }
    public String[] getListUsersOnline() {
        return users.keySet().toArray(String[]::new);
    }

    public Message[] getLastMessages(String firstLogin, String secondLogin) {
        return dataBase.getLastMessages(firstLogin,secondLogin);
    }

    public void putMessage(Message message) {
        dataBase.saveMessage(message);
        try {
            //logger.log(Level.WARNING, message.getSender());
            users.get(message.getRecipient()).printMessage(message);//output.writeUTF(message.getSender() + ": "+ message.getMessage());
            users.get(message.getSender()).printMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public User getUser(String name) {
        return dataBase.getUser(name);
    }

    public boolean getOnlineUser(String name) {
        return users.containsKey(name);
    }
    public synchronized void leaveChat(String name) {
        logger.log(Level.WARNING, String.valueOf(countSessions));
        if (name != null) {
            users.remove(name);
        }
        countSessions--;
    }

    public int getCountSessions() {
        return countSessions;
    }
    public void connected() {
        countSessions++;
    }
}
