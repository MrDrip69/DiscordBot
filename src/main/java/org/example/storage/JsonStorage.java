package org.example.storage;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;

public class JsonStorage {

    private static Connection connection;

    // Initialize connection
    public static void initialize() {
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String host = dotenv.get("DB_HOST", "localhost");
            String port = dotenv.get("DB_PORT", "5432");
            String db = dotenv.get("DB_NAME", "bankbot");
            String user = dotenv.get("DB_USER", "postgres");
            String pass = dotenv.get("DB_PASS", "");

            String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
            connection = DriverManager.getConnection(url, user, pass);
            System.out.println("[PostgresStorage] Connected to database successfully.");
        } catch (SQLException e) {
            System.out.println("[PostgresStorage] Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Get balance
    public static int getBalance(long id) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT balance FROM users WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("balance");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Get rank
    public static int getRank(long id) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT rank FROM users WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("rank");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    // Save or update user
    public static void saveUser(long id, int balance, int rank) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO users (id, balance, rank) VALUES (?, ?, ?) " +
                "ON CONFLICT (id) DO UPDATE SET balance = EXCLUDED.balance, rank = EXCLUDED.rank"
            );
            ps.setLong(1, id);
            ps.setInt(2, balance);
            ps.setInt(3, rank);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
