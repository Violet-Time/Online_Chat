package chat.server;

import java.util.Objects;

public class User {
    private final int id;
    private final String login;
    private int accessLevel;

    public User(int id, String login, int accessLevel) {
        this.id = id;
        this.login = login;
        this.accessLevel = accessLevel;
    }

    public int getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(int accessLevel) {
        this.accessLevel = accessLevel;
    }
}
