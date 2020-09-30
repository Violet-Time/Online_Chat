package chat.server;

import java.time.LocalDateTime;

/**
 * The class stores the sent message
 */

public class Message {

    private User sender;
    private User recipient;
    private String message;
    private byte isRead;
    private boolean isNew = false;

    public Message(User sender, User recipient, String message) {
        this(sender, recipient, message, (byte) 0, false);
    }

    public Message(User sender, User recipient, String message, byte isRead, boolean isNew) {
        this.sender = sender;
        this.recipient = recipient;
        this.message = message;
        this.isRead = isRead;
        this.isNew = isNew;
    }

    public User getSender() {
        return sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    public byte getIsRead() {
        return isRead;
    }

    public void setIsRead(byte isRead) {
        this.isRead = isRead;
    }

    @Override
    public String toString() {
        return (isNew ? "(new) " : "") + sender.getLogin() + ": "+ message;
    }
}
