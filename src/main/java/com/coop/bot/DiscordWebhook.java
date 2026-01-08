package com.coop.bot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordWebhook {
    private final String webhookUrl;
    private final HttpClient httpClient;
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger("Chicken-Bot");

    public DiscordWebhook(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendMessage(String content) throws Exception {
        // Discord requires a non-empty "content" field
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty. Discord will reject empty messages[citation:2][citation:6][citation:7].");
        }
        // Create JSON object
        JsonObject json = new JsonObject();
        json.addProperty("content", content);

        // Convert to JSON string
        String jsonPayload = GSON.toJson(json);
        // Avoid logging full payloads which may contain sensitive user data.
        // If debug is enabled, log a redacted payload that omits content and avatar_url.
        if (LOGGER.isDebugEnabled()) {
            JsonObject redacted = new JsonObject();
            redacted.addProperty("content", "[redacted]");
            if (json.has("username")) redacted.addProperty("username", json.get("username").getAsString());
            if (json.has("avatar_url")) redacted.addProperty("avatar_url", "[redacted]");
            LOGGER.debug("Sending JSON to Discord webhook: {}", redacted.toString());
        }

        // Build and send the HTTP POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Check the response status
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            LOGGER.error("Failed to send message to Discord. Status: " + response.statusCode());
            LOGGER.error("Response: " + response.body());
        }
    }

    /**
     * Send message using the webhook, optionally setting username and avatar.
     */
    public void sendMessage(String content, String username, String avatarUrl) throws Exception {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }

        JsonObject json = new JsonObject();
        json.addProperty("content", content);
        if (username != null && !username.isEmpty()) json.addProperty("username", username);
        if (avatarUrl != null && !avatarUrl.isEmpty()) json.addProperty("avatar_url", avatarUrl);

        String jsonPayload = GSON.toJson(json);
        if (LOGGER.isDebugEnabled()) {
            JsonObject redacted = new JsonObject();
            redacted.addProperty("content", "[redacted]");
            if (json.has("username")) redacted.addProperty("username", json.get("username").getAsString());
            if (json.has("avatar_url")) redacted.addProperty("avatar_url", "[redacted]");
            LOGGER.debug("Sending JSON to Discord webhook: {}", redacted.toString());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            LOGGER.error("Failed to send message to Discord webhook. Status: " + response.statusCode());
            LOGGER.error("Response: " + response.body());
        }
    }
}