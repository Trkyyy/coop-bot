# Coop Bot

**Coop Bot** integrates a Minecraft Fabric server with a Discord server to relay messages and events between them.

**Note**: This plugin is designed specifically for my and my friends discord server, and uses emojis specific to that server. These emojis should still work on your server, but note they are not part of Discord's standard emoji library.

## Features
- Relay server chat to Discord
- Relay Discord messages into Minecraft chat (including replies)
- Player join/leave notifications
- Death notifications (sent to a configurable "mob" channel)
- Farming detection: detects repeated mob kills and sends a farming notification and summary, suppressing per-death spam
- `/info` slash command that returns server status and player list
- Optional webhook for server shutdown messages


## Requirements
- Java 21
- Minecraft ~1.21.11 (see `fabric.mod.json`)
- A Minecraft server running Fabric Loader and Fabric API
- A Discord bot token and a Discord channel for messages


## Quick start (server admin)
1. Build the mod:

   ```bash
   ./gradlew build
   ```

2. Copy `build/libs/coop-bot-<version>.jar` into your server's `mods/` folder.
3. Start the server as usual (ensure it runs with Java 21).
4. Configure the mod (see Configuration below).

> Tip: During development you can use Loom's `runServer` task to start a dev server: `./gradlew runServer`.


## Configuration
The mod writes/reads a JSON config file at `config/coop-bot.json`. The following fields are available (defaults shown):

```json
{
  "discordBotToken": "",
  "joinMessageFormat": "🔥 {player} joined the server.",
  "leaveMessageFormat": "🔴 {player} left the server.",
  "deathMessageFormat": "💀 {message}",
  "chatMessageFormat": "👲 {player}: {message}",
  "discordChannelId": "",
  "discordMobChannelId": "",
  "discordToMinecraftFormat": "[Discord] {user}: {message}",
  "discordWebhookUrl": ""
}
```

- `discordBotToken` (required) — Bot token from the Discord Developer Portal.
- `discordChannelId` — Channel ID used for general messages and slash commands.
- `discordMobChannelId` — Channel ID used for death / mob notifications (farming messages posted here).
- `discordWebhookUrl` (optional) — Webhook URL to post a server shutdown message.
  - You can generate this as follows:
    - Navigate to your server's settings > Integrations > Create Webhook
    - Change the name and channel of the webhook as desired
    - Copy Webhook URL
- Formatting strings support placeholders:
  - `{player}` — player name
  - `{message}` — death message or chat message
  - `{user}` — Discord username (used for messages relayed to Minecraft)


## Discord Bot setup
1. Create a bot in the Discord Developer Portal.
2. Enable the following intents for the bot:
   - **MESSAGE_CONTENT** 
   - **GUILD_MESSAGES**
3. Invite your bot to the server with appropriate permissions to read and send messages.
   - Recommended OAuth2 scopes: `bot` and `applications.commands`.
   - Required permissions: `Send Messages`, `Read Message History`, `View Channels`.
4. Put the bot token in `config/coop-bot.json` (`discordBotToken`) and set the channel IDs.

Note: Slash commands are registered to the guild associated with the `discordChannelId` when possible.


## How it works (high-level)
- The mod listens for player join/leave, chat messages, and entity death events.
- Messages are formatted using the values in `ModConfig` and sent to Discord via JDA.
- Deaths are recorded; repeated kills by the same player on the same mob are tracked as a farming session. When the session reaches a threshold (default: 10 kills), a farming notification is sent and individual death messages are suppressed until the session ends; then a summary is posted.
- Discord messages can be relayed back into Minecraft chat. Replies are supported and include hover text showing the referenced message.


## License
This project is licensed as **CC0-1.0** (see `fabric.mod.json`).

