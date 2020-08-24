package chat.server;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;

/**
 * The class for storing information about online users and the history of recent messages,
 * sends new messages to listeners
 */

public class Chat {

    private LinkedList<Message> history;
    private Set<String> users;
    private PropertyChangeSupport support;

    public Chat() {
        this.history = new LinkedList<>();
        this.users = new HashSet<>();
        this.support = new PropertyChangeSupport(this);
    }

    public boolean addUser(String name) {
        return users.add(name);
    }

    public boolean removeUser(String name) {
        return users.remove(name);
    }

    public Message getLastMessage() {
        return history.getLast();
    }

    public Message[] getLastMessages() {
        return history.toArray(Message[]::new);
    }

    public void putMessage(Message message) {
        history.add(message);
        if(history.size() > 10) {
            history.removeFirst();
        }
        support.firePropertyChange("message","", message);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

}
