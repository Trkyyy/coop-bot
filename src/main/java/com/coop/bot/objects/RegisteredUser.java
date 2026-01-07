package com.coop.bot.objects;

public class RegisteredUser {
    private String minecraftUsername;
    private String discordId;
    private String discordName;
    private String avatarUrl;

    // Gson needs a no-arg constructor
    public RegisteredUser() {}

    public RegisteredUser(String minecraftUsername, String discordId, String discordName, String avatarUrl) {
        this.minecraftUsername = minecraftUsername;
        this.discordId = discordId;
        this.discordName = discordName;
        this.avatarUrl = avatarUrl;
    }

    public String getMinecraftUsername() { return minecraftUsername; }
    public String getDiscordId() { return discordId; }
    public String getDiscordName() { return discordName; }
    public String getAvatarUrl() { return avatarUrl; }

    public void setMinecraftUsername(String minecraftUsername) { this.minecraftUsername = minecraftUsername; }
    public void setDiscordId(String discordId) { this.discordId = discordId; }
    public void setDiscordName(String discordName) { this.discordName = discordName; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
