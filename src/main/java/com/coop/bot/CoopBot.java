package com.coop.bot;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.coop.bot.config.ModConfig;

public class CoopBot implements DedicatedServerModInitializer {
    public static final String MOD_ID = "coop-bot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ModConfig config;
    private static DiscordBotManager discordBotManager;
    private DiscordWebhook shutdownWebhook;

    @Override
    public void onInitializeServer() {
        LOGGER.info("Initializing CoopBot");

        // Load configuration
        config = ModConfig.load();

        // Set webhook for shutdown message
        if (!config.getDiscordWebhookUrl().isEmpty()) {
            this.shutdownWebhook = new DiscordWebhook(config.getDiscordWebhookUrl());

            ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
                try {
                    shutdownWebhook.sendMessage("🔴 **Minecraft Server is stopping...** O.o");
                } catch (Exception e) {
                    LOGGER.error("Failed to send server stop message: " + e.getMessage());
                }

                discordBotManager.shutdown();
            });
        } else {
            LOGGER.error("discordWebhookUrl constant is empty. Shutdown messages will not be sent.");
        }

        // Initialize Discord bot manager
        discordBotManager = new DiscordBotManager(config);

        // Register server lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            discordBotManager.setMinecraftServer(server);
            discordBotManager.startBot();
        });



        EventListener.registerEvents(discordBotManager, config);

        LOGGER.info("CoopBot mod initialized successfully \t( 0 _ 0 )");
    }

    public static DiscordBotManager getDiscordBotManager() {
        return discordBotManager;
    }

    public static ModConfig getConfig() {
        return config;
    }
}