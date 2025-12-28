package org.example.database;

import java.sql.*;

public class UserData {
    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL driver not found: " + e.getMessage());
        }
    }
    
    private static Connection getConnection() throws SQLException {
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl == null) {
            throw new SQLException("DATABASE_URL environment variable not set");
        }
        if (!dbUrl.startsWith("jdbc:")) {
            dbUrl = "jdbc:" + dbUrl;
        }
        return DriverManager.getConnection(dbUrl);
    }

    private static boolean dbAvailable = false;
    
    public static void initDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_balances (
                    user_id BIGINT PRIMARY KEY,
                    balance INTEGER NOT NULL DEFAULT 0,
                    rank INTEGER NOT NULL DEFAULT 1
                )
            """);
            
            dbAvailable = true;
            System.out.println("[DB] Database initialized successfully");
        } catch (SQLException e) {
            dbAvailable = false;
            System.out.println("[DB] Database unavailable - running without persistence: " + e.getMessage());
        }
    }
    
    public static boolean isAvailable() {
        return dbAvailable;
    }

    public static void saveUser(long userId, int balance, int rank) {
        if (!dbAvailable) return;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO user_balances (user_id, balance, rank) VALUES (?, ?, ?) " +
                     "ON CONFLICT (user_id) DO UPDATE SET balance = ?, rank = ?")) {
            
            stmt.setLong(1, userId);
            stmt.setInt(2, balance);
            stmt.setInt(3, rank);
            stmt.setInt(4, balance);
            stmt.setInt(5, rank);
            stmt.executeUpdate();
        } catch (SQLException e) {
            dbAvailable = false;
        }
    }

    public static Integer getBalance(long userId) {
        if (!dbAvailable) return null;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT balance FROM user_balances WHERE user_id = ?")) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("balance");
            }
        } catch (SQLException e) {
            dbAvailable = false;
        }
        return null;
    }

    public static Integer getRank(long userId) {
        if (!dbAvailable) return null;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT rank FROM user_balances WHERE user_id = ?")) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("rank");
            }
        } catch (SQLException e) {
            dbAvailable = false;
        }
        return null;
    }

    public static void deleteUser(long userId) {
        if (!dbAvailable) return;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM user_balances WHERE user_id = ?")) {
            
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            dbAvailable = false;
        }
    }
}
