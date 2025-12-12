package com.coop.bot;

import com.coop.bot.config.ModConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordBotManager extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger("Chicken-Bot");
    private JDA jda;
    private final ModConfig config;
    private MinecraftServer minecraftServer;
    private String targetChannelId;

    public DiscordBotManager(ModConfig config) {
        this.config = config;
        this.targetChannelId = config.getDiscordChannelId();
    }

    public void startBot() {
        if (config.getDiscordBotToken().isEmpty()) {
            LOGGER.error("No Discord bot token configured!");
            return;
        }

        try {
            jda = JDABuilder.createDefault(config.getDiscordBotToken())
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    //.setActivity(Activity.playing("Minecraft \uD83E\uDDD9\u200D♂\uFE0F"))
                    .addEventListeners(this)
                    .build();

            jda.awaitReady();
            LOGGER.info("Discord bot started successfully!");

            // Send startup message
            sendToDiscord("✅ **Minecraft Server is now online!**");

        } catch (Exception e) {
            LOGGER.error("Failed to start Discord bot: " + e.getMessage());
        }
    }

    public void setMinecraftServer(MinecraftServer server) {
        this.minecraftServer = server;
    }

    // Send message from Minecraft to Discord
    public void sendToDiscord(String message) {
        if (jda == null || targetChannelId == null) return;

        TextChannel channel = jda.getTextChannelById(targetChannelId);
        String sanitisedMessage = escapeMarkdown(message);
        if (channel != null && channel.canTalk()) {
            channel.sendMessage(sanitisedMessage).queue(
                    success -> LOGGER.debug("Message sent to Discord"),
                    error -> LOGGER.error("Failed to send to Discord: " + error.getMessage())
            );
        }
    }

    // Handle incoming Discord messages
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore messages from bots (including ourselves)
        if (event.getAuthor().isBot()) return;

        // Only listen to specific channel if configured
        if (targetChannelId != null && !event.getChannel().getId().equals(targetChannelId)) {
            return;
        }

        // Only listen to text channels (not DMs)
        if (!event.isFromType(ChannelType.TEXT)) return;

        String message = event.getMessage().getContentDisplay();
        String author = event.getAuthor().getName();

        // Send to Minecraft
        sendToMinecraft(author, message);
    }

    private void sendToMinecraft(String author, String message) {
        if (minecraftServer == null) {
            LOGGER.error("Minecraft server not set!");
            return;
        }

        // Format: "[Discord] Username: Message"
        String formatted = String.format("§9[Discord] §7%s: §f%s", author, message);

        // Broadcast to all players
        minecraftServer.getPlayerManager().broadcast(
                Text.literal(formatted),
                false
        );

        LOGGER.info("Discord -> Minecraft: " + author + ": " + message);
    }

    public static String escapeMarkdown(String text) {
        if (text == null) return "";
        return text
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("|", "\\|");
    }

    public void shutdown() {
        LOGGER.info("Shutting down Discord bot...");

        // Shutdown JDA
        if (jda != null) {
            jda.shutdown();
            jda = null;
        }

    }
}