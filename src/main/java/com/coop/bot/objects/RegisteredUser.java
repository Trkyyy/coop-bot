package com.coop.bot.objects;

public class RegisteredUser {
    private String minecraftUsername;
    private String discordId;
    private String discordName;
    private String avatarUrl;
    
    // Visibility settings (default to true for backward compatibility)
    private boolean showJoinLeave = true;
    private boolean showChat = true;
    private boolean showDeaths = true;

    // Gson needs a no-arg constructor
    public RegisteredUser() {}

    public RegisteredUser(String minecraftUsername, String discordId, String discordName, String avatarUrl) {
        this.minecraftUsername = minecraftUsername;
        this.discordId = discordId;
        this.discordName = discordName;
        this.avatarUrl = avatarUrl;
        // Visibility defaults to true
        this.showJoinLeave = true;
        this.showChat = true;
        this.showDeaths = true;
    }

    public String getMinecraftUsername() { return minecraftUsername; }
    public String getDiscordId() { return discordId; }
    public String getDiscordName() { return discordName; }
    public String getAvatarUrl() { return avatarUrl; }
    public boolean isShowJoinLeave() { return showJoinLeave; }
    public boolean isShowChat() { return showChat; }
    public boolean isShowDeaths() { return showDeaths; }

    public void setMinecraftUsername(String minecraftUsername) { this.minecraftUsername = minecraftUsername; }
    public void setDiscordId(String discordId) { this.discordId = discordId; }
    public void setDiscordName(String discordName) { this.discordName = discordName; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setShowJoinLeave(boolean showJoinLeave) { this.showJoinLeave = showJoinLeave; }
    public void setShowChat(boolean showChat) { this.showChat = showChat; }
    public void setShowDeaths(boolean showDeaths) { this.showDeaths = showDeaths; }
}
