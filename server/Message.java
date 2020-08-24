package chat.server;

import java.time.LocalDateTime;

/**
 * The class stores the sent message
 */

public class Message {

    private String name;
    private String message;
    private LocalDateTime time;

    public Message(String name, String message) {
        this.name = name;
        this.message = message;
        this.time = LocalDateTime.now();
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
