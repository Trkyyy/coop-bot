package com.coop.bot;

import com.coop.bot.objects.RegisteredUser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RegistrationStore {
    private static final Path REG_PATH = Path.of("config", "coop-bot-registrations.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static RegistrationStore instance;

    private Map<String, RegisteredUser> registrations = new HashMap<>();

    private RegistrationStore() {
        load();
    }

    public static synchronized RegistrationStore getInstance() {
        if (instance == null) instance = new RegistrationStore();
        return instance;
    }

    private void load() {
        File f = REG_PATH.toFile();
        if (!f.exists()) {
            save();
            return;
        }

        try (FileReader r = new FileReader(f)) {
            Type type = new TypeToken<Map<String, RegisteredUser>>(){}.getType();
            Map<String, RegisteredUser> map = GSON.fromJson(r, type);
            if (map != null) registrations = map;
        } catch (Exception e) {
            System.err.println("[CoopBot] Failed to load registrations: " + e.getMessage());
            registrations = new HashMap<>();
        }
    }

    public synchronized void save() {
        try {
            File f = REG_PATH.toFile();
            f.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(f)) {
                GSON.toJson(registrations, w);
            }
        } catch (Exception e) {
            System.err.println("[CoopBot] Failed to save registrations: " + e.getMessage());
        }
    }

    public synchronized boolean register(String minecraftUsername, String discordId, String discordName, String avatarUrl) {
        String key = minecraftUsername.toLowerCase();
        RegisteredUser existing = registrations.get(key);
        if (existing != null && !existing.getDiscordId().equals(discordId)) {
            return false; // Already owned by another user
        }

        RegisteredUser user = new RegisteredUser(minecraftUsername, discordId, discordName, avatarUrl);
        registrations.put(key, user);
        save();
        return true;
    }

    public synchronized boolean unregister(String minecraftUsername, String discordId) {
        String key = minecraftUsername.toLowerCase();
        RegisteredUser existing = registrations.get(key);
        if (existing == null) return false;
        if (!existing.getDiscordId().equals(discordId)) return false;
        registrations.remove(key);
        save();
        return true;
    }

    public RegisteredUser getByMinecraft(String minecraftUsername) {
        if (minecraftUsername == null) return null;
        return registrations.get(minecraftUsername.toLowerCase());
    }

    public synchronized Map<String, RegisteredUser> listAll() {
        // Return a snapshot to avoid concurrent modification risks for callers iterating the map
        return Collections.unmodifiableMap(new HashMap<>(registrations));
    }
}
