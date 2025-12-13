package com.coop.bot;

import com.coop.bot.config.ModConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.util.List;
import java.util.stream.Collectors;

public class DiscordBotManager extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger("Chicken-Bot");
    private JDA jda;
    private final ModConfig config;
    private MinecraftServer minecraftServer;
    private String generalChannelId;
    private String mobChannelId;
    private static long serverStartTime;

    // Intialise class
    public DiscordBotManager(ModConfig config) {
        this.config = config;
        this.generalChannelId = config.getDiscordChannelId();
        this.mobChannelId = config.getDiscordMobChannelId();
        this.serverStartTime = serverStartTime = System.currentTimeMillis();;
    }

    // Setters
    public void setMinecraftServer(MinecraftServer server) {
        this.minecraftServer = server;
    }


    // ---------------
    // Bot startup
    // ---------------

    public void startBot() {
        if (config.getDiscordBotToken().isEmpty()) {
            LOGGER.error("No Discord bot token configured!");
            return;
        }

        try {
            jda = JDABuilder.createDefault(config.getDiscordBotToken())
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    .addEventListeners(this)
                    .build();

            jda.awaitReady();
            LOGGER.info("Discord bot started successfully!");
            sendToDiscord("✅ **Minecraft Server is now online!**", false);
        } catch (Exception e) {
            LOGGER.error("Failed to start Discord bot: " + e.getMessage());
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        LOGGER.info("Discord bot ready! Registering slash commands...");
        registerSlashCommands(event.getJDA());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // Could simplify this to if statement, but it seems likely to be expanded, so I will stick with it
        switch (event.getName()) {
            case "info":
                handleInfoCommand(event);
                break;
            default:
                event.reply("Unknown command").setEphemeral(true).queue();
        }
    }

    private void registerSlashCommands(JDA jda) {
        if (generalChannelId != null) {
            TextChannel channel = jda.getTextChannelById(generalChannelId);
            if (channel != null) {
                channel.getGuild().updateCommands()
                        .addCommands(
                                Commands.slash("info", "Show server information")
                        )
                        .queue(
                                success -> LOGGER.info("Slash commands registered successfully!"),
                                error -> LOGGER.error("Failed to register slash commands: " + error.getMessage())
                        );
                return;
            }
        }
        // Global registration
        // `/info` is hard coded to only post in generalChannelId, if that ever changes, this can be uncommented,
        //      and the above can be removed

        //jda.updateCommands()
        //        .addCommands(
        //                 Commands.slash("info", "Show server information")
        //        )
        //        .queue();
    }

    // ---------------
    // Message sending
    // ---------------

    public void sendToDiscord(String message, boolean mob) {

        if (jda == null || generalChannelId == null) return;

        TextChannel channel;
        if (mob) {
            channel = jda.getTextChannelById(mobChannelId);
        } else {
            channel = jda.getTextChannelById(generalChannelId);
        }

        String sanitisedMessage = escapeMarkdown(message);
        if (channel != null && channel.canTalk()) {
            channel.sendMessage(sanitisedMessage).queue(
                    success -> LOGGER.debug("Message sent to Discord"),
                    error -> LOGGER.error("Failed to send to Discord: " + error.getMessage())
            );
        }
    }

    public void sendToDiscord(String message) {
        sendToDiscord(message, false);
    }



    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (generalChannelId != null && !event.getChannel().getId().equals(generalChannelId)) {
            return;
        }
        if (!event.isFromType(ChannelType.TEXT)) return;
        String message = event.getMessage().getContentDisplay();
        String author = event.getAuthor().getName();
        sendToMinecraft(author, message);
    }

    private void sendToMinecraft(String author, String message) {
        if (minecraftServer == null) {
            LOGGER.error("Minecraft server not set!");
            return;
        }
        String formatted = String.format("§9[Discord] §7%s: §f%s", author, message);
        minecraftServer.getPlayerManager().broadcast(
                Text.literal(formatted),
                false
        );
        LOGGER.info("Discord -> Minecraft: " + author + ": " + message);
    }

    // ---------------
    // Discord commands
    // ---------------

    private void handleInfoCommand(SlashCommandInteractionEvent event) {
        if (minecraftServer == null) {
            event.reply("❌ **Server not available**").setEphemeral(true).queue();
            return;
        }
        LOGGER.info("Info command requested, processing");
        String info = formatServerInfo();
        event.reply(info).queue();
        LOGGER.info("Completed processing info command.");
    }

    // ---------------
    // Markdown formatting
    // ---------------

    private String formatPlayersTable(List<String> players) {
        if (players.isEmpty()) {
            return "```\n┌─────────────────┐\n│  No players    │\n│    online      │\n└─────────────────┘\n```";
        }
        StringBuilder table = new StringBuilder();
        table.append("```\n");
        table.append("┌────┬──────────────────────┬──────────────┐\n");
        table.append("│ #  │ Player Name          │ Status       │\n");
        table.append("├────┼──────────────────────┼──────────────┤\n");
        for (int i = 0; i < players.size(); i++) {
            String playerName = players.get(i);
            String status = "🟢 Online";
            if (playerName.length() > 18) {
                playerName = playerName.substring(0, 15) + "...";
            }
            table.append(String.format("│ %-2d │ %-20s │ %-12s │\n",
                    i + 1, playerName, status));
        }
        table.append("└────┴──────────────────────┴──────────────┘\n");
        table.append("```");
        return table.toString();
    }

    private String formatServerInfo() {
        if (minecraftServer == null) {
            return "❌ **Server not available**";
        }

        int playerCount = minecraftServer.getPlayerManager().getCurrentPlayerCount();
        int maxPlayers = minecraftServer.getMaxPlayerCount();

        // FIXED: Use getTicks() and convert to seconds
        String uptimeFormatted = getFormattedUptime();
        StringBuilder info = new StringBuilder();
        info.append("## Minecraft Server Info\n\n");
        info.append("### \uD83D\uDC72 Players\n");
        info.append(String.format("-# %s online\n", playerCount));
        if (playerCount > 0) {
            List<String> players = minecraftServer.getPlayerManager().getPlayerList()
                    .stream()
                    .map(p -> p.getName().getString())
                    .collect(Collectors.toList());
            players.forEach((player) -> {
                // like `   - Trko__ {new line}`
                info.append(String.format("• %s\n", player));
            });
        }
        info.append("\n\n");
        info.append("### \uD83E\uDDA4 Status\n");
        info.append(String.format("• `%s` since last unplugging\n", uptimeFormatted));
        info.append(String.format("• Version: `%s`\n",
                minecraftServer.getVersion()));

        return info.toString();
    }

    public static String escapeMarkdown(String text) {
        if (text == null) return "";
        return text
                .replace("_", "\\_")
                .replace("~", "\\~")
                .replace(">", "\\>")
                .replace("|", "\\|");
    }

    private static String formatUptime(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        if (days > 0) {
            return String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
        } else {
            return String.format("%02dm %02ds", minutes, seconds);
        }
    }

    public static String getFormattedUptime() {
        return formatUptime(System.currentTimeMillis() - serverStartTime);
    }

    // ---------------
    // Bot shutdown
    // ---------------

    public void shutdown() {
        LOGGER.info("Shutting down Discord bot...");
        if (jda != null) {
            jda.shutdown();
            jda = null;
        }
    }
}