package com.coop.bot;

import com.coop.bot.config.ModConfig;
import  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("CoopBot-EventListener");
    private static DiscordBotManager discordBotManager;
    private static ModConfig config;

    public static void registerEvents(DiscordBotManager manager, ModConfig cfg) {
        discordBotManager = manager;
        config = cfg;

        // Register player join event
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            onPlayerJoin(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            onPlayerLeave(handler.player);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register(EventListener::onEntityDeath);

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, typeKey) -> {
           onChatMessage(message, sender);
        });

        LOGGER.info("EventListener registered successfully");
    }

    private static void onPlayerJoin(ServerPlayerEntity player) {
        String playerName = player.getName().getString();
        String message = config.getJoinMessageFormat()
                .replace("{player}", playerName);

        try {
            discordBotManager.sendToDiscord(message);
            LOGGER.info("Sent join message for player: " + playerName);
        } catch (Exception e) {
            LOGGER.error("Failed to send join message: " + e.getMessage());
        }
    }

    private static void onPlayerLeave(ServerPlayerEntity player) {
        String playerName = player.getName().getString();
        String message = config.getLeaveMessageFormat()
                .replace("{player}", playerName);

        try {
            discordBotManager.sendToDiscord(message);
            LOGGER.info("Sent leave message for player: " + playerName);
        } catch (Exception e) {
            LOGGER.error("Failed to send leave message: " + e.getMessage());
        }
    }

    public static void onEntityDeath(LivingEntity entity, DamageSource source) {
        String playerName = entity.getName().getString();

        // Get the death message from the game
        Text deathText = source.getDeathMessage(entity);
        String deathMessage = deathText != null ? deathText.getString() : "died";

        // Format the message
        String message = config.getDeathMessageFormat()
                .replace("{message}", deathMessage);

        try {
            discordBotManager.sendToDiscord(message, true);
            LOGGER.info("Sent death message for player: " + playerName);
        } catch (Exception e) {
            LOGGER.error("Failed to send death message: " + e.getMessage());
        }
    }

    public static void onChatMessage(SignedMessage message, ServerPlayerEntity sender){
        String messageBody = message.getContent().getString();
        String playerName = sender.getName().getString();

        String sentMessage =     config.getChatMessageFormat()
                .replace("{player}", playerName)
                .replace("{message}", messageBody);
        try {
            discordBotManager.sendToDiscord(sentMessage);
            LOGGER.info("Sent chat message for player: " + playerName);
        } catch (Exception e) {
            LOGGER.error("Failed to send chat message: " + e.getMessage());
        }
    }
}