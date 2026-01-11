package org.example.storage;

import java.sql.*;

public class JsonStorage {

    private static Connection connection;

    // Initialize the database connection
    public static void initialize() {
        connect();
        createTableIfNotExists();
    }

    /** Ensures the connection is alive; reconnects if needed */
    private static void connect() {
        try {
            if (connection != null && !connection.isClosed() && connection.isValid(2)) return;

            String host = System.getenv().getOrDefault("DB_HOST", "localhost");
            String port = System.getenv().getOrDefault("DB_PORT", "5432");
            String db   = System.getenv().getOrDefault("DB_NAME", "bankbot");
            String user = System.getenv().getOrDefault("DB_USER", "postgres");
            String pass = System.getenv().getOrDefault("DB_PASS", "password");

            String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
            connection = DriverManager.getConnection(url, user, pass);
            System.out.println("[INFO] Database connected successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[ERROR] Failed to connect to the database!");
        }
    }

    private static void createTableIfNotExists() {
        connect();
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT PRIMARY KEY,
                    balance INT NOT NULL,
                    rank INT NOT NULL,
                    points INT NOT NULL
                );
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[ERROR] Failed to create table!");
        }
    }

    // ========== GETTERS ==========
    public static int getBalance(long userId) {
        return getInt(userId, "balance", 0);
    }

    public static int getPoints(long userId) {
        return getInt(userId, "points", 0);
    }

    public static int getRank(long userId) {
        return getInt(userId, "rank", 1);
    }

    // ========== ADDERS ==========
    public static void addBalance(long userId, int amount) {
        connect();
        int balance = getBalance(userId) + amount;
        int rank = getRank(userId);
        int points = getPoints(userId);
        saveUser(userId, balance, rank, points);
    }

    public static void addPoints(long userId, int amount) {
        connect();
        int balance = getBalance(userId);
        int rank = getRank(userId);
        int points = getPoints(userId) + amount;
        saveUser(userId, balance, rank, points);
    }

    // ========== GENERIC GET ==========
    private static int getInt(long userId, String column, int def) {
        connect();
        String sql = "SELECT " + column + " FROM users WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(column);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return def;
    }

    // ========== SAVE/UPDATE ==========
    public static void saveUser(long userId, int balance, int rank, int points) {
        connect();
        String sql = """
            INSERT INTO users (id, balance, rank, points)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (id)
            DO UPDATE SET
                balance = EXCLUDED.balance,
                rank    = EXCLUDED.rank,
                points  = EXCLUDED.points;
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, balance);
            ps.setInt(3, rank);
            ps.setInt(4, points);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[ERROR] Failed to save user " + userId);
        }
    }
}
