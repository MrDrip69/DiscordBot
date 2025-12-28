package org.example.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class JsonStorage {

    private static final String FILE_PATH = "balances.json"; // file to store user balances
    private static Map<Long, UserData> users = new HashMap<>();
    private static final Gson gson = new Gson();

    // Load the JSON file at startup
    static {
        load();
    }

    // ================= USER DATA CLASS =================
    public static class UserData {
        private int balance;
        private int rank;

        public UserData(int balance, int rank) {
            this.balance = balance;
            this.rank = rank;
        }

        public int getBalance() {
            return balance;
        }

        public void setBalance(int balance) {
            this.balance = balance;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }
    }

    // ================= SAVE USER =================
    public static void saveUser(long userId, int balance, int rank) {
        users.put(userId, new UserData(balance, rank));
        save();
    }

    // ================= GET BALANCE =================
    public static int getBalance(long userId) {
        UserData data = users.get(userId);
        return data != null ? data.getBalance() : 0;
    }

    // ================= GET RANK =================
    public static int getRank(long userId) {
        UserData data = users.get(userId);
        return data != null ? data.getRank() : 1;
    }

    // ================= SAVE TO FILE =================
    private static void save() {
        try (Writer writer = new FileWriter(FILE_PATH)) {
            gson.toJson(users, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= LOAD FROM FILE =================
    private static void load() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (Reader reader = new FileReader(FILE_PATH)) {
            Type type = new TypeToken<Map<Long, UserData>>() {}.getType();
            users = gson.fromJson(reader, type);
            if (users == null) users = new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= RESET ALL =================
    public static void resetUser(long userId) {
        saveUser(userId, 0, 1);
    }
}
