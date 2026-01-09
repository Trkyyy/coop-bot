# Coop Bot - AI Coding Instructions

## Project Overview
**Coop Bot** is a Minecraft Fabric mod (Java 21, 1.21.11) that bridges Minecraft and Discord servers. It relays chat, player join/leave events, deaths, and detects farming behavior. All code is in `src/main/java/com/coop/bot/`.

## Architecture & Key Components

### Core Lifecycle: `CoopBot.java`
Entry point implementing `DedicatedServerModInitializer`. On server startup:
1. Loads JSON config from `config/coop-bot.json`
2. Initializes `DiscordBotManager` (manages JDA bot + slash commands)
3. Initializes `DeathTracking` (tracks deaths & farming patterns)
4. Registers `EventListener` for Minecraft events

Static accessors: `getDiscordBotManager()`, `getConfig()` for global access.

### Event Routing: `EventListener.java`
Registers Fabric event handlers for:
- **Player join/leave** → calls `DiscordBotManager.sendToDiscord()` with formatted message
- **Entity death** → routes to `DeathTracking.onEntityDeath()` for processing
- **Chat messages** → calls `DiscordBotManager.handleChatMessage()` with webhook impersonation if registered

Config format strings use `{player}`, `{message}`, `{user}`, `{coords}` placeholders (see `deathMessageFormat`).

### Discord Integration: `DiscordBotManager.java`
Manages JDA (Discord API lib v5.0.0-beta.20). Key methods:
- `startBot()` → initializes JDA with intents (MESSAGE_CONTENT, GUILD_MESSAGES)
- `sendToDiscord(message)` → sends to general channel (`discordChannelId`)
- `sendToMobChannel(message)` → sends to mob/farming channel (`discordMobChannelId`)
- Slash commands: `/register`, `/unregister`, `/registrations` (stored in `RegistrationStore`)
- Webhook posting: Uses `discordWebhookUrl` for avatar/username impersonation of registered players

**Critical pattern**: JDA callbacks run async; wrap Minecraft server access in `minecraftServer.submit(() -> {})` to ensure main thread execution.

### Death & Farming Detection: `DeathTracking.java`
Tracks repeated mob kills to suppress spam:
- Maintains `deathHistory` list and `activeFarmingSessions` (ConcurrentHashMap)
- **FarmingSession**: detects when player kills same mob type repeatedly in ~60 second windows
- Suppresses individual death messages if farming detected; sends summary instead
- Scheduled executor checks every 5 seconds for farming inactivity
- XP values hardcoded for ~40 mob types (see `XP_VALUES` static map)

Example flow: Player kills 5 zombies → FarmingSession created → next kills suppressed → after 60s inactivity → farming summary sent.

### Player Statistics: `PlayerStatsManager.java` & `PlayerStats.java`
Daily stat aggregation (resets at midnight):
- `PlayerStats` (one per player/day): mob kills by type, player deaths, playtime seconds
- `PlayerStatsManager` (singleton): in-memory map `currentDayStats` (UUID → PlayerStats)
- Scheduled nightly task persists stats to disk via `PlayerStatsPersistence`
- Memory efficient: only current day in memory; old stats archived

Key methods: `recordMobKill()`, `recordPlayerDeath()`, `recordPlayerLogin()`, `recordPlayerLogout()`.

### Configuration: `ModConfig.java`
JSON at `config/coop-bot.json`. Loaded on init; auto-creates if missing. All fields have defaults:
- **Discord**: `discordBotToken`, `discordChannelId`, `discordMobChannelId`, `discordWebhookUrl`
- **Format strings** (support placeholders): `joinMessageFormat`, `leaveMessageFormat`, `deathMessageFormat`, `chatMessageFormat`, `discordToMinecraftFormat`

Uses Gson for serialization with pretty-printing. Config is static field accessible via `CoopBot.getConfig()`.

## Build & Development

### Build Command
```bash
./gradlew build
```
Produces: `build/libs/coop-bot-<version>.jar`

**Key build setup** (in `build.gradle`):
- Fabric Loom (remapping + decompilation)
- JDA 5.0.0-beta.20 (included in JAR)
- OkHttp 4.12.0 (included)
- GSON 2.10.1, SLF4J 2.0.13 (included)

### Development Server
```bash
./gradlew runServer
```
Starts local test server with mod loaded.

## Testing
- **Player Stats**: Kill mobs, die, stay logged in. Check `[CoopBot-PlayerStats]` console logs.
- **Farming Detection**: Kill same mob repeatedly (30+ in 5 sec window) → should see farming summary.
- **Discord**: Verify config has valid token & channel IDs; messages appear in Discord.

See `TESTING_GUIDE.md` for detailed testing procedures.

## Code Patterns & Conventions

### Logging
Use project-wide logger: `LoggerFactory.getLogger("CoopBot")` or component-specific: `LoggerFactory.getLogger("CoopBot-DeathTracking")`.

### Thread Safety
- `ConcurrentHashMap` for shared state (e.g., `currentDayStats`, `activeFarmingSessions`)
- Minecraft callbacks executed async; use `minecraftServer.submit()` for safety
- `ScheduledExecutorService` for delayed/recurring tasks (farming check, nightly persist)

### Null Safety
Death messages include optional `{coords}` — only included if coordinates available. Always check before formatting.

### Placeholders in Format Strings
When processing config format strings, use `.replace("{key}", value)`. Supported keys:
- Chat/join/leave: `{player}`, `{message}`, `{user}`
- Deaths: `{message}`, `{coords}` (coords may be empty)

### JSON Persistence
`PlayerStatsPersistence` persists `PlayerStats` objects daily. Use Gson's no-arg constructor for deserialization.

## Key Files Reference
- **Core**: `CoopBot.java`, `EventListener.java`, `DiscordBotManager.java`
- **Features**: `DeathTracking.java`, `PlayerStatsManager.java`, `PlayerStatsPersistence.java`
- **Data Models**: `PlayerStats.java`, `DeathRecord.java`, `RegisteredUser.java`
- **Config**: `ModConfig.java` (design: static singleton, auto-save)
- **Build**: `build.gradle`, `gradle.properties` (Minecraft 1.21.11, Fabric Loader 0.18.2)

## When Modifying
1. **Config changes**: Update `ModConfig.java` fields and regenerate JSON defaults
2. **New events**: Register in `EventListener.java` and route to appropriate manager
3. **Discord features**: Add to `DiscordBotManager` or create new manager, register slash commands in `startBot()`
4. **Stats tracking**: Extend `PlayerStats` properties and update `PlayerStatsManager` record methods
5. **Async code**: Always wrap Minecraft API calls in `minecraftServer.submit()`
