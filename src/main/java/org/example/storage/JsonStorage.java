package org.example.storage;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class JsonStorage {

    private static JSONObject storage;
    private static final String FILE_NAME = "users.json";

    // Load data on class initialization
    static {
        loadFromFile();
    }

    // ================= LOAD FROM FILE =================
    private static void loadFromFile() {
        try {
            File file = new File(FILE_NAME);
            if (!file.exists()) {
                file.createNewFile();
                storage = new JSONObject();
                saveToFile(); // create empty file
                return;
            }

            String content = new String(Files.readAllBytes(file.toPath()));
            storage = content.isEmpty() ? new JSONObject() : new JSONObject(content);

        } catch (IOException e) {
            e.printStackTrace();
            storage = new JSONObject(); // fallback to empty
        }
    }

    // ================= SAVE TO FILE =================
    private static synchronized void saveToFile() {
        try (FileWriter file = new FileWriter(FILE_NAME)) {
            file.write(storage.toString(4)); // pretty-print JSON
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= GET BALANCE =================
    public static int getBalance(long id) {
        String key = String.valueOf(id);
        if (!storage.has(key)) return 0;
        return storage.getJSONObject(key).optInt("balance", 0);
    }

    // ================= GET RANK =================
    public static int getRank(long id) {
        String key = String.valueOf(id);
        if (!storage.has(key)) return 1;
        return storage.getJSONObject(key).optInt("rank", 1);
    }

    // ================= SAVE USER =================
    public static void saveUser(long id, int balance, int rank) {
        String key = String.valueOf(id);
        JSONObject userData = new JSONObject();
        userData.put("balance", balance);
        userData.put("rank", rank);

        storage.put(key, userData);
        saveToFile();
    }

    // ================= RESET USER =================
    public static void resetUser(long id) {
        saveUser(id, 0, 1);
    }
}
