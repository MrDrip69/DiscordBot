package org.example.storage;

import java.sql.*;

public class JsonStorage {

    public static Connection connection;

    // Initialize the database connection
    public static void initialize() {
        try {
            String host = System.getenv("DB_HOST");
            String port = System.getenv("DB_PORT");
            String db = System.getenv("DB_NAME");
            String user = System.getenv("DB_USER");
            String pass = System.getenv("DB_PASS");

            if (host == null) host = "localhost";
            if (port == null) port = "5432";
            if (db == null) db = "bankbot";
            if (user == null) user = "postgres";
            if (pass == null) pass = "password";

            String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;

            connection = DriverManager.getConnection(url, user, pass);
            System.out.println("[INFO] Database connected successfully!");

            // Create table if it doesn't exist
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id BIGINT PRIMARY KEY," +
                    "balance INT NOT NULL," +
                    "rank INT NOT NULL" +
                    ");";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[ERROR] Failed to connect to the database!");
        }
    }

    // Get user's balance
    public static int getBalance(long userId) {
        if (connection == null) return 0;
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT balance FROM users WHERE id=?");
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("balance");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Get user's rank
    public static int getRank(long userId) {
        if (connection == null) return 1;
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT rank FROM users WHERE id=?");
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("rank");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    // Save or update a user
    public static void saveUser(long userId, int balance, int rank) {
        if (connection == null) return;
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (id, balance, rank) VALUES (?, ?, ?) " +
                            "ON CONFLICT (id) DO UPDATE SET balance = EXCLUDED.balance, rank = EXCLUDED.rank;"
            );
            ps.setLong(1, userId);
            ps.setInt(2, balance);
            ps.setInt(3, rank);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
