package com.coop.bot;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.coop.bot.config.ModConfig;

public class CoopBot implements DedicatedServerModInitializer {
    public static final String MOD_ID = "coop-bot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static DiscordWebhook discordWebhook;
    private static ModConfig config;

    @Override
    public void onInitializeServer() {
        LOGGER.info("Initializing CoopBot");

        // Load configuration
        config = ModConfig.load();

        // Initialize Discord webhook sender
        discordWebhook = new DiscordWebhook(config.getWebhookUrl());

        // Register event listeners
        EventListener.registerEvents(discordWebhook, config);

        // Register server start/stop events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                discordWebhook.sendMessage("🟢 **Minecraft Server has started!** \n yahoo \n ┌( ͝° ͜ʖ͡°)=ε/̵͇̿̿/’̿’̿ ̿");
            } catch (Exception e) {
                LOGGER.error("Failed to send server start message: " + e.getMessage());
            }

        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            try {
                discordWebhook.sendMessage("🔴 **Minecraft Server is stopping...** O.o");
            } catch (Exception e) {
                LOGGER.error("Failed to send server stop message: " + e.getMessage());
            }
        });

        LOGGER.info("CoopBot mod initialized successfully \t( 0 _ 0 )");
    }

    public static DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }

    public static ModConfig getConfig() {
        return config;
    }
}