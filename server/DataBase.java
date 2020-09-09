package chat.server;

import java.sql.*;
import java.util.ArrayList;

public class DataBase {

    String url;

    public DataBase(String path) {
        url = "jdbc:sqlite:".concat(path);
        createNewTable();
    }

    public void createNewTable() {
        String sql1 = "CREATE TABLE IF NOT EXISTS users (\n"
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "login TEXT NOT NULL UNIQUE,\n"
                + "password TEXT NOT NULL\n"
                + ");\n";
        String sql2 = "CREATE TABLE IF NOT EXISTS messages (\n"
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "message TEXT NOT NULL,\n"
                + "sender_id INTEGER NOT NULL,\n"
                + "recipient_id INTEGER DEFAULT NULL,\n"
                + "FOREIGN KEY (sender_id) REFERENCES users (id)\n"
                + "ON UPDATE CASCADE ON DELETE CASCADE,\n"
                + "FOREIGN KEY (recipient_id) REFERENCES users (id)\n"
                + "ON UPDATE CASCADE ON DELETE CASCADE\n"
                + ");";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql1);
            stmt.execute(sql2);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public User addNewUser(String name, String password) {
        String sql = "INSERT INTO users (login, password) VALUES (?, ?);";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, password);
            if (pstmt.executeUpdate() == 1) {
                return new User(name,password);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public User getUser(String name) {
        String sql = "SELECT id, login, password FROM users WHERE login = ?;";

        User user = null;

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            ResultSet rs  = pstmt.executeQuery();
            while (rs.next()) {
                user = new User(rs.getString("login"), rs.getString("password"));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return user;
    }

    public void saveMessage(Message message) {
        String sql = "INSERT INTO messages (sender_id, recipient_id, message)\n" +
                    "SELECT (SELECT id FROM users WHERE login = ?), id, ? FROM users WHERE login = ?;";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, message.getSender());
            pstmt.setString(2, message.getMessage());
            pstmt.setString(3, message.getRecipient());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public Message[] getLastMessages(String firstLogin, String secondLogin) {
        String sql = "SELECT (SELECT login FROM users WHERE sender_id = id) AS sender,\n" +
                    "(SELECT login FROM users WHERE recipient_id = id) AS recipient,\n" +
                    "message\n" +
                    "FROM messages \n" +
                    "JOIN users ON (login = ? OR login = ?) AND \n" +
                    "messages.sender_id = users.id\n" +
                    "WHERE messages.recipient_id IN (SELECT id FROM users WHERE login = ? OR login = ?)\n" +
                    "ORDER BY messages.id DESC LIMIT 10";

        //Message[] messages = null;
        ArrayList<Message> messages = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, firstLogin);
            pstmt.setString(2, secondLogin);
            pstmt.setString(3, firstLogin);
            pstmt.setString(4, secondLogin);
            ResultSet rs  = pstmt.executeQuery();
            rs.getFetchSize();

            //messages = new Message[10];

            while (rs.next()) {
                messages.add(new Message(rs.getString("sender"), rs.getString("recipient"), rs.getString("message")));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return messages.toArray(Message[]::new);
    }

}
