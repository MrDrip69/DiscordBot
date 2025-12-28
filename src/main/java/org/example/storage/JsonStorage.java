package org.example.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JsonStorage {
    private static final String DATA_FILE = "user_data.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    private static Map<Long, UserInfo> userData = new HashMap<>();
    
    public static class UserInfo {
        public long userId;
        public int balance;
        public int rank;
        
        public UserInfo(long userId, int balance, int rank) {
            this.userId = userId;
            this.balance = balance;
            this.rank = rank;
        }
    }
    
    public static void initialize() {
        lock.writeLock().lock();
        try {
            File file = new File(DATA_FILE);
            if (file.exists()) {
                try {
                    String content = new String(Files.readAllBytes(file.toPath()));
                    userData = gson.fromJson(content, new TypeToken<Map<Long, UserInfo>>(){}.getType());
                    if (userData == null) userData = new HashMap<>();
                    System.out.println("[Storage] Loaded " + userData.size() + " users from JSON");
                } catch (Exception e) {
                    System.out.println("[Storage] Error loading JSON: " + e.getMessage());
                    userData = new HashMap<>();
                }
            } else {
                userData = new HashMap<>();
                save();
                System.out.println("[Storage] Created new user_data.json");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public static void saveUser(long userId, int balance, int rank) {
        lock.writeLock().lock();
        try {
            userData.put(userId, new UserInfo(userId, balance, rank));
            save();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public static int getBalance(long userId) {
        lock.readLock().lock();
        try {
            UserInfo info = userData.get(userId);
            return info != null ? info.balance : 0;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public static int getRank(long userId) {
        lock.readLock().lock();
        try {
            UserInfo info = userData.get(userId);
            return info != null ? info.rank : 1;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public static Map<Long, UserInfo> getAllUsers() {
        lock.readLock().lock();
        try {
            return new HashMap<>(userData);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public static void deleteUser(long userId) {
        lock.writeLock().lock();
        try {
            userData.remove(userId);
            save();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private static void save() {
        try {
            String json = gson.toJson(userData);
            Files.write(Paths.get(DATA_FILE), json.getBytes());
        } catch (IOException e) {
            System.out.println("[Storage] Error saving JSON: " + e.getMessage());
        }
    }
}
