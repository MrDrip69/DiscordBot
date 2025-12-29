package org.example.storage;

import java.sql.*;

public class JsonStorage {

    private static Connection connection;

    // ================= INIT =================
    public static void initialize() {
        try {
            String dbUrl = System.getenv("DATABASE_URL");

            if (dbUrl == null) {
                throw new RuntimeException("DATABASE_URL not found");
            }

            connection = DriverManager.getConnection(dbUrl);
            createTable();
            System.out.println("✅ Connected to PostgreSQL");

        } catch (Exception e) {
            throw new RuntimeException("Failed to connect DB", e);
        }
    }

    // ================= TABLE =================
    private static void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                user_id BIGINT PRIMARY KEY,
                balance INT NOT NULL,
                rank INT NOT NULL
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    // ================= GET BALANCE =================
    public static int getBalance(long userId) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT balance FROM users WHERE user_id = ?"
            );
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getInt("balance");

            createUser(userId);
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ================= GET RANK =================
    public static int getRank(long userId) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT rank FROM users WHERE user_id = ?"
            );
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getInt("rank");

            createUser(userId);
            return 1;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ================= SAVE USER =================
    public static void saveUser(long userId, int balance, int rank) {
        try {
            PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO users (user_id, balance, rank)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id)
                DO UPDATE SET balance = EXCLUDED.balance, rank = EXCLUDED.rank
            """);

            ps.setLong(1, userId);
            ps.setInt(2, balance);
            ps.setInt(3, rank);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ================= CREATE USER =================
    private static void createUser(long userId) {
        saveUser(userId, 0, 1);
    }
}
