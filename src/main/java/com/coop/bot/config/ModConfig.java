package com.coop.bot.config;  // Notice the .config subpackage

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class ModConfig {
    // Configuration values with defaults
    private String webhookUrl = "";
    private String joinMessageFormat = "🟢 {player} joined the game. Everyone say 'Hello there, {player}'";
    private String leaveMessageFormat = "🔴 {player} left the game. Everyone say 'Goodbye there, {player}'";
    private String deathMessageFormat = "💀 {message}";
    private String chatMessageFormat = "\uD83D\uDC72 {player}: {message}";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", "coop-bot.json");

    public static ModConfig load() {
        File configFile = CONFIG_PATH.toFile();
        ModConfig config;

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = GSON.fromJson(reader, ModConfig.class);
                System.out.println("[CoopBot] Loaded config from " + CONFIG_PATH);
            } catch (IOException e) {
                System.err.println("[CoopBot] Failed to load config, using defaults: " + e.getMessage());
                config = new ModConfig();
            }
        } else {
            config = new ModConfig();
            System.out.println("[CoopBot] Config file not found, using defaults");
        }

        config.save(); // Creates file if it doesn't exist
        return config;
    }

    public void save() {
        try {
            File configFile = CONFIG_PATH.toFile();
            configFile.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("[CoopBot] Failed to save config: " + e.getMessage());
        }
    }

    // Getters
    public String getWebhookUrl() { return webhookUrl; }
    public String getJoinMessageFormat() { return joinMessageFormat; }
    public String getLeaveMessageFormat() { return leaveMessageFormat; }
    public String getDeathMessageFormat() { return deathMessageFormat; }
    public String getChatMessageFormat() { return chatMessageFormat; }

    // Setters (optional)
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        save();
    }
}