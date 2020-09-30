package chat.server;

import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class DataBase {

    String url;
    Connection conn;

    public DataBase(String path) {
        url = "jdbc:sqlite:".concat(path);
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        createNewTable();
    }

    public void createNewTable() {
        String sql1 = "CREATE TABLE IF NOT EXISTS users (\n"
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "login TEXT NOT NULL UNIQUE,\n"
                + "password TEXT NOT NULL,\n"
                + "access_level INTEGER DEFAULT 0\n"
                + ");\n";
        String sql2 = "CREATE TABLE IF NOT EXISTS messages (\n"
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "message TEXT NOT NULL,\n"
                + "is_read INTEGER NOT NULL,\n"
                + "sender_id INTEGER NOT NULL,\n"
                + "recipient_id INTEGER DEFAULT NULL,\n"
                + "FOREIGN KEY (sender_id) REFERENCES users (id)\n"
                + "ON UPDATE CASCADE ON DELETE CASCADE,\n"
                + "FOREIGN KEY (recipient_id) REFERENCES users (id)\n"
                + "ON UPDATE CASCADE ON DELETE CASCADE\n"
                + ");";
        String sql3 = "CREATE TABLE IF NOT EXISTS banned (\n"
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "id_banned_user INTEGER NOT NULL,\n"
                + "id_banned_by INTEGER NOT NULL,\n"
                + "ban_date INTEGER NOT NULL,\n"
                + "unban_date INTEGER NOT NULL,\n"
                + "FOREIGN KEY (id_banned_user) REFERENCES users (id),\n"
                + "FOREIGN KEY (id_banned_by) REFERENCES users (id)\n"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql1);
            stmt.execute(sql2);
            stmt.execute(sql3);
            addNewUser("admin", Chat.hashing("12345678"), 2);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ChatException e) {

        }
    }

    public User addNewUser(String login, String password) throws ChatException {
        return addNewUser(login, password, 0);
    }

    public User addNewUser(String login, String password, int accessLevel) throws ChatException {
        String sql = "INSERT INTO users (login, password, access_level) VALUES (?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            pstmt.setString(2, password);
            pstmt.setInt(3, accessLevel);
            if (pstmt.executeUpdate() == 1) {
                return getUser(login, password);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        throw new ChatException("Server: this login is already taken! Choose another one.");
    }

    public User getUser(String login, String password) throws ChatException {
        String sql = "SELECT id, login, password, access_level FROM users WHERE login = ?;";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            ResultSet rs  = pstmt.executeQuery();
            while (rs.next()) {
                if (login.equals(rs.getString("login"))) {
                    if (!rs.getString("password").equals(password)) {
                        throw new ChatException("Server: incorrect password!");
                    }
                    if (isKicked(rs.getInt("id"))) {
                        throw new ChatException("Server: you are banned!");
                    }

                    return new User(rs.getInt("id"), rs.getString("login"), rs.getInt("access_level"));
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        throw new ChatException("Server: incorrect login!");
    }

    public User getUser(String login) throws ChatException {
        String sql = "SELECT id, login, access_level FROM users WHERE login = ?;";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            ResultSet rs  = pstmt.executeQuery();
            while (rs.next()) {
                if (login.equals(rs.getString("login"))) {
                    return new User(rs.getInt("id"), rs.getString("login"), rs.getInt("access_level"));
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        throw new ChatException("Server: incorrect login!");
    }

    public void saveMessage(Message message) {
        String sql = "INSERT INTO messages (sender_id, recipient_id, message, is_read) VALUES (?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, message.getSender().getId());
            pstmt.setInt(2, message.getRecipient().getId());
            pstmt.setString(3, message.getMessage());
            pstmt.setByte(4, message.getIsRead());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public Message[] getLastMessages(User user, int count) {
        count = count > 25 ? 25 : Math.abs(count);
        String sql = "SELECT login, sender_id, message,  FROM messages INNER JOIN users ON " +
                "users.id = messages.sender_id WHERE recipient_id = ? " +
                "ORDER BY id DESC LIMIT ?";

        ArrayList<Message> messages = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, user.getId());
            pstmt.setInt(2, count);
            ResultSet rs = pstmt.executeQuery();
            rs.getFetchSize();

            //messages = new Message[10];

            while (rs.next()) {
                messages.add(new Message(new User(rs.getInt("sender_id"), rs.getString("login"), 0), user,
                        rs.getString("message"),
                        (byte) rs.getInt("is_read"),
                        rs.getInt("is_read") == 0));
            }
            Collections.reverse(messages);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return messages.toArray(Message[]::new);
    }

    public Message[] getLastMessages(User firstUser, User secondUser) {
        String sql = "SELECT id, message, sender_id, recipient_id, is_read FROM messages " +
                     "WHERE sender_id = ? AND recipient_id = ? OR recipient_id = ? AND sender_id = ? AND is_read = 1 " +
                     "ORDER BY id DESC LIMIT 10";
        String sql2 = "SELECT id, message, sender_id, recipient_id, is_read FROM messages " +
                      "WHERE sender_id = ? AND recipient_id = ? AND is_read = 0 " +
                      "ORDER BY id DESC LIMIT 15";
        //Message[] messages = null;
        ArrayList<Message> messages = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             PreparedStatement pstmt2 = conn.prepareStatement(sql2)) {

            pstmt.setInt(1, firstUser.getId());
            pstmt.setInt(2, secondUser.getId());
            pstmt.setInt(3, firstUser.getId());
            pstmt.setInt(4, secondUser.getId());
            ResultSet rs = pstmt.executeQuery();
            rs.getFetchSize();

            //messages = new Message[10];

            while (rs.next()) {
                messages.add(new Message(rs.getInt("sender_id") == firstUser.getId() ? firstUser : secondUser,
                                         rs.getInt("recipient_id")  == firstUser.getId() ? firstUser : secondUser,
                                         rs.getString("message"),
                                         (byte) 1,
                                   false));
            }
            Collections.reverse(messages);
            pstmt2.setInt(1, secondUser.getId());
            pstmt2.setInt(2, firstUser.getId());
            rs = pstmt2.executeQuery();
            rs.getFetchSize();

            ArrayList<Message> messages2 = new ArrayList<>();

            while (rs.next()) {
                messages2.add(new Message(rs.getInt("sender_id") == firstUser.getId() ? firstUser : secondUser,
                        rs.getInt("recipient_id")  == firstUser.getId() ? firstUser : secondUser,
                        rs.getString("message"),
                        (byte) 1,
                        true));
                if (rs.getInt("is_read") == 0) {
                    readMessage(rs.getInt("sender_id"), rs.getInt("recipient_id"));
                }
            }
            Collections.reverse(messages2);
            messages.addAll(messages2);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return messages.toArray(Message[]::new);
    }

    public void readMessage(int sender_id, int recipient_id) {
        String sql = "UPDATE messages SET is_read = 1 WHERE sender_id = ? AND recipient_id = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sender_id);
            pstmt.setInt(2, recipient_id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void accessLevelUpdate(int id, int accessLevel) {
        String sql = "UPDATE users SET access_level = ? WHERE id = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accessLevel);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean isKicked(int id) {
        String sql = "SELECT * FROM banned WHERE id_banned_user = ? AND unban_date > strftime('%s','now');";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs  = pstmt.executeQuery();
            while (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
        return false;
    }

    public void kick(User firstUser, User secondUser) {
        String sql = "INSERT INTO banned (id_banned_user, id_banned_by, ban_date, unban_date) VALUES (?, ?, strftime('%s','now'), strftime('%s','now') + 300);";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, secondUser.getId());
            pstmt.setInt(2, firstUser.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public String getStatistics(User firstUser, User secondUser) {

        String sql = "SELECT COUNT(*) AS \"all\", " +
                "(SELECT COUNT(*) FROM messages WHERE sender_id = ? AND recipient_id = ?)  AS \"send\", " +
                "(SELECT COUNT(*) FROM messages WHERE sender_id = ? AND recipient_id = ?) AS \"rec\" FROM messages " +
                "WHERE sender_id = ? AND recipient_id = ? OR recipient_id = ? AND sender_id = ?";

        StringBuilder builder = new StringBuilder();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, firstUser.getId());
            pstmt.setInt(2, secondUser.getId());
            pstmt.setInt(3, secondUser.getId());
            pstmt.setInt(4, firstUser.getId());
            pstmt.setInt(5, firstUser.getId());
            pstmt.setInt(6, secondUser.getId());
            pstmt.setInt(7, firstUser.getId());
            pstmt.setInt(8, secondUser.getId());
            ResultSet rs = pstmt.executeQuery();
            rs.getFetchSize();
            builder.append("Server:\n").append("Statistics with ").append(secondUser.getLogin()).append(":\n");

            while (rs.next()) {
                builder.append("Total messages: ").append(rs.getInt("all")).append("\n");
                builder.append("Messages from ").append(firstUser.getLogin()).append(": ").append(rs.getInt("send")).append("\n");
                builder.append("Messages from ").append(secondUser.getLogin()).append(": ").append(rs.getInt("rec"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return builder.toString();
    }

    public String[] getUnreadUsers(User user) throws ChatException {
        String sql = "SELECT login FROM messages INNER JOIN users ON " +
                    "users.id = messages.sender_id WHERE is_read = 0 " +
                    "AND recipient_id = ? GROUP BY sender_id;";
        ArrayList<String> users = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, user.getId());
            ResultSet rs  = pstmt.executeQuery();
            while (rs.next()) {
                users.add(rs.getString("login"));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        if (users.isEmpty()) {
            throw new ChatException("Server: no one unread");
        }
        users.sort(Comparator.naturalOrder());
        return users.toArray(String[]::new);
    }

    public void closeConnection() {
        try {
            conn.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

}