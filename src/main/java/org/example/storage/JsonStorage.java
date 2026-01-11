package org.example.storage;

import java.sql.*;

public class JsonStorage {

    public static Connection connection;

    // Initialize the database connection
    public static void initialize() {
        try {
            String host = System.getenv().getOrDefault("DB_HOST", "localhost");
            String port = System.getenv().getOrDefault("DB_PORT", "5432");
            String db   = System.getenv().getOrDefault("DB_NAME", "bankbot");
            String user = System.getenv().getOrDefault("DB_USER", "postgres");
            String pass = System.getenv().getOrDefault("DB_PASS", "password");

            String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
            connection = DriverManager.getConnection(url, user, pass);

            System.out.println("[INFO] Database connected successfully!");

            // Create table if it doesn't exist
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
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[ERROR] Failed to connect to the database!");
        }
    }

    // Get user's balance
    public static int getBalance(long userId) {
        return getInt(userId, "balance", 0);
    }

    // Get user's points
    public static int getPoints(long userId) {
        return getInt(userId, "points", 0);
    }

    // Get user's rank
    public static int getRank(long userId) {
        return getInt(userId, "rank", 1);
    }
    public static void addBalance(long userId, int amount) {
        if (connection == null) return;

        String sql = """
        INSERT INTO users (id, balance, rank, points)
        VALUES (?, ?, 1, 0)
        ON CONFLICT (id)
        DO UPDATE SET balance = users.balance + EXCLUDED.balance;
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void addPoints(long userId, int amount) {
        if (connection == null) return;

        String sql = """
        INSERT INTO users (id, balance, rank, points)
        VALUES (?, 0, 1, ?)
        ON CONFLICT (id)
        DO UPDATE SET points = users.points + EXCLUDED.points;
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Generic helper
    private static int getInt(long userId, String column, int def) {
        if (connection == null) return def;

        try (PreparedStatement ps =
                     connection.prepareStatement("SELECT " + column + " FROM users WHERE id=?")) {

            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getInt(column);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return def;
    }

    // Save or update a user
    public static void saveUser(long userId, int balance, int rank, int points) {
        if (connection == null) return;

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
        }
    }
}
