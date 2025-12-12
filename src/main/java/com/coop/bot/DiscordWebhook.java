package com.coop.bot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class DiscordWebhook {
    private final String webhookUrl;
    private final HttpClient httpClient;
    private static final Gson GSON = new Gson();

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
        System.out.println("Sending this JSON to Discord: " + jsonPayload);

        // Build and send the HTTP POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Check the response status
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            System.err.println("Failed to send message to Discord. Status: " + response.statusCode());
            System.err.println("Response: " + response.body());
        }
    }
}