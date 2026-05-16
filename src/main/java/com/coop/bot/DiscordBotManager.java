package com.coop.bot;

import com.coop.bot.config.ModConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.coop.bot.objects.RegisteredUser;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscordBotManager extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger("Chicken-Bot");
    private JDA jda;
    private final ModConfig config;
    private MinecraftServer minecraftServer;
    private String generalChannelId;
    private String mobChannelId;
    private static long serverStartTime;
    // Executor for background HTTP/webhook work so we don't block the server thread
    // Set a reasonable limit on threads to avoid resource exhaustion
    private final ExecutorService webhookExecutor = Executors.newFixedThreadPool(4);

    // Intialise class
    public DiscordBotManager(ModConfig config) {
        this.config = config;
        this.generalChannelId = config.getDiscordChannelId();
        this.mobChannelId = config.getDiscordMobChannelId();
        this.serverStartTime = System.currentTimeMillis();;
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
            case "registrations":
                handleListRegistrationsCommand(event);
                break;
            case "register":
                handleRegisterCommand(event);
                break;
            case "unregister":
                handleUnregisterCommand(event);
                break;
            case "visibility":
                handleVisibilityCommand(event);
                break;
            default:
                event.reply("Unknown command").setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        
        if (buttonId.startsWith("visibility_")) {
            handleVisibilityButton(event);
        }
    }

    private void registerSlashCommands(JDA jda) {
        if (generalChannelId != null) {
            TextChannel channel = jda.getTextChannelById(generalChannelId);
            if (channel != null) {
                channel.getGuild().updateCommands()
                        .addCommands(
                                Commands.slash("info", "Show server information"),
                                Commands.slash("registrations", "List all registered Minecraft usernames"),
                                Commands.slash("register", "Register your Minecraft username").addOption(OptionType.STRING, "minecraft_username", "Your Minecraft username", true),
                                Commands.slash("unregister", "Unregister your Minecraft username").addOption(OptionType.STRING, "minecraft_username", "Your Minecraft username", true),
                                Commands.slash("visibility", "Control your visibility settings for Discord posting")
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
    // mob = false by default
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

        Message discordMessage = event.getMessage();
        String message = discordMessage.getContentDisplay();

        // Use the member's nickname for display in Minecraft if present; otherwise use the effective name or username
        String author;
        if (event.getMember() != null) {
            String nick = event.getMember().getNickname();
            author = (nick != null && !nick.isEmpty()) ? nick : event.getMember().getEffectiveName();
        } else {
            author = event.getAuthor().getName();
        }

        // Check if this message is a reply
        String referencedMessage = null;
        String referencedAuthor = null;

        if (discordMessage.getMessageReference() != null) {
            try {
                Message referencedMsg = discordMessage.getMessageReference().resolve().complete();
                if (referencedMsg != null) {
                    referencedMessage = referencedMsg.getContentDisplay();
                    if (referencedMsg.getMember() != null) {
                        String nick = referencedMsg.getMember().getNickname();
                        referencedAuthor = (nick != null && !nick.isEmpty()) ? nick : referencedMsg.getMember().getEffectiveName();
                    } else {
                        referencedAuthor = referencedMsg.getAuthor().getName();
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to resolve referenced message: " + e.getMessage());
            }
        }

        sendToMinecraft(author, message, referencedAuthor, referencedMessage);
    }

    private void sendToMinecraft(String author, String message, String referencedAuthor, String referencedMessage) {
        if (minecraftServer == null) {
            LOGGER.error("Minecraft server not set!");
            return;
        }

        String formatted;

        if (referencedMessage != null && referencedAuthor != null) {
            // Format for replies
            String truncatedRefMessage = referencedMessage;
            if (truncatedRefMessage.length() > 50) {
                truncatedRefMessage = truncatedRefMessage.substring(0, 47) + "...";
            }

            // Create the base text
            formatted = String.format("§9[Discord] §7%s §8(↩ %s)§7: §f%s",
                    author, referencedAuthor, message);


            // Create hover text showing the full referenced message
            // (hover text will only appear if the targeted message is moused over (the entire message))
            Text hoverText = Text.literal("Replying to " + referencedAuthor + ": " + truncatedRefMessage)
                    .formatted(Formatting.GRAY);

            HoverEvent hoverEvent = new HoverEvent.ShowText(hoverText);

            // Apply the hover event to the text
            MutableText mainText = Text.literal(formatted)
                    .styled(style -> style.withHoverEvent(hoverEvent));

            minecraftServer.getPlayerManager().broadcast(mainText, false);

        } else {
            formatted = String.format("§9[Discord] §7%s: §f%s", author, message);
            minecraftServer.getPlayerManager().broadcast(
                    Text.literal(formatted),
                    false
            );
        }

        LOGGER.info("Discord -> Minecraft: " + author + ": " + message +
                (referencedAuthor != null ? " (replying to " + referencedAuthor + ")" : ""));
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

        // Defer the reply immediately to extend the timeout
        event.deferReply().queue();

        // Now do your processing
        String info = formatServerInfo();

        // Send the actual reply
        event.getHook().editOriginal(info).queue();

        LOGGER.info("Completed processing info command.");
    }

    private void handleRegisterCommand(SlashCommandInteractionEvent event) {
        String mcUsername = event.getOption("minecraft_username").getAsString();
        String discordId = event.getUser().getId();
        // Persist the user's display name (effective name) rather than raw username or nickname
        String discordName = (event.getMember() != null) ? event.getMember().getEffectiveName() : event.getUser().getName();
        String avatarUrl = event.getUser().getEffectiveAvatarUrl();

        // Basic validation for Minecraft username
        if (!mcUsername.matches("^[A-Za-z0-9_]{1,16}$")) {
            event.reply("❌ Invalid Minecraft username. Use 1-16 chars: letters, numbers and underscores only.").setEphemeral(true).queue();
            return;
        }

        event.deferReply().setEphemeral(true).queue();

        boolean ok = RegistrationStore.getInstance().register(mcUsername, discordId, discordName, avatarUrl);
        if (!ok) {
            event.getHook().editOriginal("❌ That Minecraft username is already registered to another Discord account.").queue();
            return;
        }

        event.getHook().editOriginal("✅ Registered **" + mcUsername + "** to you (" + discordName + ")").queue();
        LOGGER.info("Registered Minecraft user " + mcUsername + " to Discord user " + discordName + " (" + discordId + ")");
    }

    private void handleUnregisterCommand(SlashCommandInteractionEvent event) {
        String mcUsername = event.getOption("minecraft_username").getAsString();
        String discordId = event.getUser().getId();

        event.deferReply().setEphemeral(true).queue();
        boolean ok = RegistrationStore.getInstance().unregister(mcUsername, discordId);
        if (!ok) {
            event.getHook().editOriginal("❌ Could not unregister. Either it wasn't registered, or it is registered to a different Discord account.").queue();
            return;
        }
        event.getHook().editOriginal("✅ Unregistered **" + mcUsername + "**").queue();
        LOGGER.info("Unregistered Minecraft user " + mcUsername + " by Discord user " + discordId);
    }

    private void handleListRegistrationsCommand(SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();

        Map<String, RegisteredUser> regs = RegistrationStore.getInstance().listAll();
        if (regs.isEmpty()) {
            event.getHook().editOriginal("No registrations found.").queue();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**Registered Minecraft Users:**\n\n");
        regs.values().forEach(r -> {
            sb.append(String.format("- **%s** → %s (`%s`)\n", r.getMinecraftUsername(), r.getDiscordName(), r.getDiscordId()));
        });

        event.getHook().editOriginal(sb.toString()).queue();
    }

    private void handleVisibilityCommand(SlashCommandInteractionEvent event) {
        String discordId = event.getUser().getId();
        event.deferReply().setEphemeral(true).queue();

        RegisteredUser reg = RegistrationStore.getInstance().getByDiscordId(discordId);
        if (reg == null) {
            event.getHook().editOriginal("❌ You must be registered to use visibility settings. Use `/register` first.").queue();
            return;
        }

        String message = buildVisibilityMessage(reg);
        List<Button> buttons = buildVisibilityButtons(reg);
        
        event.getHook().editOriginal(message)
                .setActionRow(buttons.get(0), buttons.get(1), buttons.get(2))
                .queue();
    }

    private void handleVisibilityButton(ButtonInteractionEvent event) {
        String discordId = event.getUser().getId();
        String buttonId = event.getComponentId();

        RegisteredUser reg = RegistrationStore.getInstance().getByDiscordId(discordId);
        if (reg == null) {
            event.reply("❌ Registration not found.").setEphemeral(true).queue();
            return;
        }

        // Toggle the appropriate setting
        boolean newValue = false;
        switch (buttonId) {
            case "visibility_joinleave":
                newValue = !reg.isShowJoinLeave();
                reg.setShowJoinLeave(newValue);
                break;
            case "visibility_chat":
                newValue = !reg.isShowChat();
                reg.setShowChat(newValue);
                break;
            case "visibility_deaths":
                newValue = !reg.isShowDeaths();
                reg.setShowDeaths(newValue);
                break;
            default:
                event.reply("❌ Unknown button.").setEphemeral(true).queue();
                return;
        }

        // Save the updated registration
        RegistrationStore.getInstance().updateUser(reg);

        // Update the message with new button states
        String message = buildVisibilityMessage(reg);
        List<Button> buttons = buildVisibilityButtons(reg);
        
        event.editMessage(message)
                .setActionRow(buttons.get(0), buttons.get(1), buttons.get(2))
                .queue();
        
        LOGGER.info("Updated visibility settings for " + reg.getMinecraftUsername() + " (" + reg.getDiscordName() + ")");
    }

    private String buildVisibilityMessage(RegisteredUser reg) {
        return String.format("**Visibility Settings for %s**\n\n" +
                "Control what gets posted to Discord:\n" +
                "• **Join/Leave**: %s\n" +
                "• **Chat Messages**: %s\n" +
                "• **Deaths/Mobs**: %s\n\n" +
                "Click a button to toggle.",
                reg.getMinecraftUsername(),
                reg.isShowJoinLeave() ? "🟢 Visible" : "🔴 Hidden",
                reg.isShowChat() ? "🟢 Visible" : "🔴 Hidden",
                reg.isShowDeaths() ? "🟢 Visible" : "🔴 Hidden"
        );
    }

    private List<Button> buildVisibilityButtons(RegisteredUser reg) {
        Button joinLeaveBtn = reg.isShowJoinLeave() 
                ? Button.success("visibility_joinleave", "Join/Leave: ON")
                : Button.danger("visibility_joinleave", "Join/Leave: OFF");
        
        Button chatBtn = reg.isShowChat()
                ? Button.success("visibility_chat", "Chat: ON")
                : Button.danger("visibility_chat", "Chat: OFF");
        
        Button deathsBtn = reg.isShowDeaths()
                ? Button.success("visibility_deaths", "Deaths: ON")
                : Button.danger("visibility_deaths", "Deaths: OFF");
        
        return List.of(joinLeaveBtn, chatBtn, deathsBtn);
    }

    public void sendMinecraftChatToDiscord(ServerPlayerEntity player, String messageBody) {
        if (jda == null) return;

        String playerName = player.getName().getString();
        RegisteredUser reg = RegistrationStore.getInstance().getByMinecraft(playerName);

        // Check visibility preferences for registered players
        if (reg != null && !reg.isShowChat()) {
            LOGGER.debug("Suppressed chat message for " + playerName + " (visibility setting)");
            return;
        }

        // If registered and webhook configured, use webhook to impersonate but do it off-thread
        if (reg != null && config.getDiscordWebhookUrl() != null && !config.getDiscordWebhookUrl().isEmpty()) {
            // Submit the webhook send to the background executor so the server thread isn't blocked
            webhookExecutor.submit(() -> {
                try {
                    DiscordWebhook webhook = new DiscordWebhook(config.getDiscordWebhookUrl());
                    webhook.sendMessage(messageBody, reg.getDiscordName(), reg.getAvatarUrl());
                } catch (Exception e) {
                    LOGGER.error("Failed to send Minecraft chat via webhook", e);
                }
            });
            // Return immediately to avoid blocking
            return;
        }

        // Otherwise, if registered but no webhook, fall back to sending as bot but use registered name
        try {
            String formatted = config.getChatMessageFormat();
            if (reg != null) {
                formatted = formatted.replace("{player}", reg.getDiscordName()).replace("{message}", messageBody);
            } else {
                formatted = formatted.replace("{player}", playerName).replace("{message}", messageBody);
            }

            sendToDiscord(formatted);
        } catch (Exception e) {
            LOGGER.error("Failed to send Minecraft chat to Discord: " + e.getMessage());
        }
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

        // Don't escape Discord timestamp format: <t:timestamp:R>
        // Don't escape custom emoji format: <:name:id>
        // Don't escape animated emoji format: <a:name:id>

        // Use regex to find Discord special tags and protect them
        StringBuilder result = new StringBuilder();
        Pattern pattern = Pattern.compile("(<[tT]:\\d+:[A-Za-z]>|<a?:\\w+:\\d+>)");
        Matcher matcher = pattern.matcher(text);

        int lastIndex = 0;
        while (matcher.find()) {
            // Escape text before the Discord tag
            result.append(escapePlainMarkdown(text.substring(lastIndex, matcher.start())));
            // Add the Discord tag without escaping
            result.append(matcher.group());
            lastIndex = matcher.end();
        }

        // Escape any remaining text
        result.append(escapePlainMarkdown(text.substring(lastIndex)));

        return result.toString();
    }

    private static String escapePlainMarkdown(String text) {
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
        try {
            webhookExecutor.shutdownNow();
        } catch (Exception e) {
            LOGGER.warn("Error shutting down webhook executor: " + e.getMessage());
        }
    }
}