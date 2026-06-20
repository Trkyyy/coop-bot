package com.coop.bot;

import com.coop.bot.objects.DeathRecord;
import com.coop.bot.objects.RegisteredUser;
import com.coop.bot.config.ModConfig;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

import net.minecraft.world.damagesource.DamageSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class DeathTracking {
    private final List<DeathRecord> deathHistory = new ArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger("Chicken-Bot-Death-Tracking");
    private static DiscordBotManager discordBot;
    private static ModConfig config;
    private final Map<String, FarmingSession> activeFarmingSessions = new ConcurrentHashMap<>();
    private final Set<String> recentlyNotifiedFarms = ConcurrentHashMap.newKeySet();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final long FARMING_CHECK_INTERVAL = 5; // Check every 5 seconds
    private static final long FARMING_INACTIVITY_THRESHOLD = 60; // 60 seconds

    // This is the best I can do for mob xp atm
    private static final Map<EntityType<?>, Integer> XP_VALUES = new IdentityHashMap<>();

    static {
        // Hostile Mobs (5 XP)
        XP_VALUES.put(EntityTypes.ZOMBIE, 5);
        XP_VALUES.put(EntityTypes.HUSK, 5);
        XP_VALUES.put(EntityTypes.DROWNED, 5);
        XP_VALUES.put(EntityTypes.ZOMBIFIED_PIGLIN, 5);
        XP_VALUES.put(EntityTypes.ZOGLIN, 5);
        XP_VALUES.put(EntityTypes.SKELETON, 5);
        XP_VALUES.put(EntityTypes.STRAY, 5);
        XP_VALUES.put(EntityTypes.WITHER_SKELETON, 5);
        XP_VALUES.put(EntityTypes.CREEPER, 5);
        XP_VALUES.put(EntityTypes.SPIDER, 5);
        XP_VALUES.put(EntityTypes.CAVE_SPIDER, 5);
        XP_VALUES.put(EntityTypes.ENDERMAN, 5);
        XP_VALUES.put(EntityTypes.GHAST, 5);
        XP_VALUES.put(EntityTypes.PIGLIN, 5);
        XP_VALUES.put(EntityTypes.HOGLIN, 5);
        XP_VALUES.put(EntityTypes.VINDICATOR, 5);
        XP_VALUES.put(EntityTypes.PILLAGER, 5);
        XP_VALUES.put(EntityTypes.WITCH, 5);
        XP_VALUES.put(EntityTypes.PHANTOM, 5);
        XP_VALUES.put(EntityTypes.SHULKER, 5);

        // 10 XP mobs
        XP_VALUES.put(EntityTypes.BLAZE, 10);
        XP_VALUES.put(EntityTypes.GUARDIAN, 10);
        XP_VALUES.put(EntityTypes.ELDER_GUARDIAN, 10);
        XP_VALUES.put(EntityTypes.EVOKER, 10);

        // Special XP values
        XP_VALUES.put(EntityTypes.PIGLIN_BRUTE, 20);
        XP_VALUES.put(EntityTypes.ENDERMITE, 3);
        XP_VALUES.put(EntityTypes.SILVERFISH, 5);
        XP_VALUES.put(EntityTypes.RAVAGER, 20);
        XP_VALUES.put(EntityTypes.WITHER, 50);
        XP_VALUES.put(EntityTypes.ENDER_DRAGON, 12000);
        XP_VALUES.put(EntityTypes.WARDEN, 5);
        XP_VALUES.put(EntityTypes.MAGMA_CUBE, 2);
        XP_VALUES.put(EntityTypes.SLIME, 2);
        XP_VALUES.put(EntityTypes.SULFUR_CUBE, 5);

        // Note: Size-based mobs (Slime, Magma Cube) need special handling
        // These values are just placeholders
    }

    public DeathTracking(DiscordBotManager discordBot, ModConfig config) {
        this.discordBot = discordBot;
        this.config = config;
        startFarmingMonitor();
    }

    // Boolean return = if message should send
    public boolean recordDeath(DeathRecord death) {
        //logDeath(death); // Commenting this because it is spamming console a LOT 18/12/25
        deathHistory.add(death);

        // Check for farming
        boolean shouldSendDeathMessage = checkAndHandleFarming(death);

        // Clean up old sessions occasionally (every 1000 deaths)
        // TODO: calculate a reasonable number for this (how much memory do these objects take up)
        if (deathHistory.size() % 1000 == 0) {
            cleanupOldSessions();
        }

        return shouldSendDeathMessage;
    }
    
    public static DeathRecord createDeathRecord(LivingEntity entity, DamageSource damageSource) {
        DeathRecord.Builder builder = new DeathRecord.Builder()
                .entityUUID(entity.getUUID())
                .entityName(entity.getName().getString())
                .entityType(entity.getType())
                .deathMessage(getDeathMessage(entity, damageSource))
                .damageSource(damageSource.type().msgId());

        // Get coords
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        String dimension;

        if (entity.level() != null) {
            dimension = entity.level().dimension().toString();
            // Remove namespace if you want just the name
            if (dimension.startsWith("minecraft:")) {
                dimension = dimension.substring(10);
            }
            builder.deathLocation(x, y, z, dimension);
        } else {
            builder.deathLocation(x, y, z);
        }

        // Handle killer information
        Entity attacker = damageSource.getEntity();
        if (attacker != null) {
            builder.killerName(attacker.getName().getString());

            if (attacker.getUUID() != null) {
                builder.killerUUID(attacker.getUUID());
            }

            builder.killerType(attacker.getType());

        } else if (damageSource.getDirectEntity() != null) {
            // For indirect damage sources (like projectiles, explosions)
            builder.killerName(damageSource.getDirectEntity().getName().getString());
            builder.killerType(damageSource.getDirectEntity().getType());
        }

        return builder.build();
    }

    public List<DeathRecord> getDeathsOfEntity(UUID id) {
        List<DeathRecord> result = new ArrayList<>();
        for (DeathRecord death : deathHistory) {
            if (death.getEntityUUID().equals(id)) {
                result.add(death);
            }
        }
        return result;
    }

    public List<DeathRecord> getRecentDeaths(int count) {
        int start = Math.max(0, deathHistory.size() - count);
        return deathHistory.subList(start, deathHistory.size());
    }

    public boolean isFarming(DeathRecord record) {
        if (deathHistory.isEmpty() || record == null) {
            return false;
        }

        long currentTime = Instant.now().getEpochSecond();
        long thirtySecondsAgo = currentTime - 30;

        long count = deathHistory.stream()
                .filter(dr -> dr.getTimestamp() >= thirtySecondsAgo)
                .filter(dr -> Objects.equals(dr.getEntityName(), record.getEntityName()))
                .filter(dr -> Objects.equals(dr.getKillerName(), record.getKillerName()))
                .limit(10) // Optimization: stop counting after 10 matches
                .count();

        return count >= 10;
    }

    public static String getDeathMessage(LivingEntity entity, DamageSource source){
        // Get death message through combat tracker
        Component deathText = entity.getCombatTracker().getDeathMessage();
        return deathText != null ? deathText.getString() : "died";
    }

    private void logDeath(DeathRecord death) {
        LOGGER.info("[DeathTracker] Storing death: " + death);
    }


    // ----------
    // Farming sessions
    // ----------

    public static class FarmingSession {
        private final String killerName;
        private final String entityName;
        private final EntityType<?> entityType;
        private final UUID killerUUID;
        private final long startTime;
        private int killCount;
        private int xpDropped;
        private long lastKillTime;
        private boolean notificationSent;

        public FarmingSession(String killerName, UUID killerUUID, String entityName,  EntityType<?> entityType) {
            this.killerName = killerName;
            this.killerUUID = killerUUID;
            this.entityName = entityName;
            this.entityType = entityType;
            this.startTime = Instant.now().getEpochSecond();
            this.lastKillTime = this.startTime;
            this.killCount = 1;
            this.notificationSent = false;
        }

        public void recordKill() {
            this.killCount++;
            this.lastKillTime = Instant.now().getEpochSecond();
        }

        public boolean isActive() {
            long currentTime = Instant.now().getEpochSecond();
            return (currentTime - lastKillTime) <= 60; // 60 seconds since last kill
        }

        public boolean shouldNotify() {
            return killCount >= 10 && !notificationSent;
        }

        public void markNotificationSent() {
            this.notificationSent = true;
        }

        public String getSessionKey() {
            return (killerUUID != null ? killerUUID.toString() : killerName) + ":" + entityName;
        }

        public int getKillCount() {
            return killCount;
        }

        public String getKillerName() {
            return killerName;
        }

        public String getEntityName() {
            return entityName;
        }

        public boolean isNotificationSent() {
            return notificationSent;
        }

        public String getFormattedDuration() {
            long duration = lastKillTime - startTime;
            long minutes = duration / 60;
            long seconds = duration % 60;
            if (minutes > 0) {
                return String.format("%dm %ds", minutes, seconds);
            }
            return String.format("%ds", seconds);
        }

        public String getFormattedSummary() {
            long killsPerMinute = (killCount * 60) / Math.max(1, (lastKillTime - startTime));
            int mobXpValue = getMobXpValue(entityType) * killCount;
            return String.format(
                    "**%s** farmed **%s**\n" +
                            "• Total Kills: %d\n" +
                            "• Experience dropped: %d\n" +
                            "• Duration: %s\n" +
                            "• Rate: ~%d kills/min\n" +
                            "• Started: <t:%d:R>\n" +
                            "• Ended: <t:%d:R>",
                    killerName, entityName, killCount, mobXpValue, getFormattedDuration(),
                    killsPerMinute, startTime, lastKillTime
            );
        }
    }

    private boolean checkAndHandleFarming(DeathRecord death) {
        if (death == null || death.getKillerName() == null) {
            return true; // Send death message if no killer
        }

        String killerId = death.getKillerUUID() != null ?
                death.getKillerUUID().toString() : death.getKillerName();
        String sessionKey = killerId + ":" + death.getEntityName();

        // Get or create farming session
        FarmingSession session = activeFarmingSessions.computeIfAbsent(
                sessionKey,
                k -> new FarmingSession(death.getKillerName(), death.getKillerUUID(), death.getEntityName(), death.getEntityType())
        );

        // Update session with new kill
        session.recordKill();

        boolean shouldSendDeathMessage = true;

        // Check if farming threshold reached and we haven't sent notification yet
        if (session.shouldNotify() && !session.isNotificationSent()) {
            // Mark that we've sent the notification
            session.markNotificationSent();

            // Add to recently notified set for suppression tracking
            recentlyNotifiedFarms.add(sessionKey);

            // Send farming notification to Discord
            sendFarmingNotification(session);
        }

        // Check if we should suppress death messages for this farming session
        if (recentlyNotifiedFarms.contains(sessionKey) && session.isActive()) {
            // Don't send death message while actively farming
            shouldSendDeathMessage = false;
            LOGGER.debug("Suppressing death message for farming session: {}", sessionKey);
        }

        return shouldSendDeathMessage;
    }

    // I have long pondered if this should sit in DiscordBotManager
    private void sendFarmingNotification(FarmingSession session) {
        String message = String.format(
                "<:brasovpog:1411341162111045632> It seems %s is farming %ss",
                session.getKillerName(),
                session.getEntityName()
        );

        try {
            discordBot.sendToDiscord(message, true); // Send to mob channel
            LOGGER.info("Farming notification sent: {} farming {}",
                    session.getKillerName(), session.getEntityName());
        } catch (Exception e) {
            LOGGER.error("Failed to send farming notification: " + e.getMessage());
        }
    }

    private void startFarmingMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndCompleteFarmingSessions();
            } catch (Exception e) {
                LOGGER.error("Error in farming monitor: " + e.getMessage(), e);
            }
        }, FARMING_CHECK_INTERVAL, FARMING_CHECK_INTERVAL, TimeUnit.SECONDS);

        LOGGER.info("Farming monitor started with {}s check interval", FARMING_CHECK_INTERVAL);
    }

    private void checkAndCompleteFarmingSessions() {
        long currentTime = Instant.now().getEpochSecond();
        Iterator<Map.Entry<String, FarmingSession>> iterator = activeFarmingSessions.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, FarmingSession> entry = iterator.next();
            FarmingSession session = entry.getValue();

            // Check if session is inactive for more than threshold
            if ((currentTime - session.lastKillTime) > FARMING_INACTIVITY_THRESHOLD) {
                iterator.remove();

                // Remove from suppression tracking
                recentlyNotifiedFarms.remove(entry.getKey());

                // Only send summary if farming was detected
                if (session.isNotificationSent()) {
                    sendFarmingSummary(session);
                }

                LOGGER.debug("Completed farming session: {} ({} kills in {})",
                        entry.getKey(), session.getKillCount(), session.getFormattedDuration());
            }
        }
    }

    // TODO: Make this commit to a db
    public void cleanupOldSessions() {
        Iterator<Map.Entry<String, FarmingSession>> iterator = activeFarmingSessions.entrySet().iterator();
        long currentTime = Instant.now().getEpochSecond();

        while (iterator.hasNext()) {
            Map.Entry<String, FarmingSession> entry = iterator.next();
            FarmingSession session = entry.getValue();

            // Remove if inactive for over 60 seconds
            if ((currentTime - session.lastKillTime) > 60) {
                iterator.remove();
                recentlyNotifiedFarms.remove(entry.getKey());
                LOGGER.debug("Cleaned up inactive farming session: {}", entry.getKey());
            }
        }
    }

    private void sendFarmingSummary(FarmingSession session) {
        // Check visibility preferences for registered players
        String killerName = session.getKillerName();
        RegisteredUser reg = RegistrationStore.getInstance().getByMinecraft(killerName);
        if (reg != null && !reg.isShowDeaths()) {
            LOGGER.debug("Suppressed farming summary for " + killerName + " (visibility setting)");
            return;
        }
        
        String message = String.format(
                "<:brasovpog:1411341162111045632> **Farming Complete**\n%s",
                session.getFormattedSummary()
        );

        try {
            discordBot.sendToDiscord(message, true);
            LOGGER.info("Farming summary sent: {} completed farming {} ({} total kills)",
                    session.getKillerName(), session.getEntityName(), session.getKillCount());
        } catch (Exception e) {
            LOGGER.error("Failed to send farming summary: " + e.getMessage());
        }
    }

    private static int getMobXpValue(EntityType<?> entityType) {
        return XP_VALUES.getOrDefault(entityType, 0);
    }

}