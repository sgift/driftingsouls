# Merging the Discord chat (`feature/discord-chat`)

What to do when merging this branch into `develop` and rolling it out. The bot setup itself is
described in [`docker/README.md`](../docker/README.md#discord-chat-on-the-overview-page); this file
is the checklist around it.

## What the branch adds

- A chat box on the overview page that shows channels of the Discord server. Players read through a
  bot and write through a webhook per channel, under their Discord server alias and avatar.
- Account linking under *Einstellungen* via OAuth2 (`identify`, `guilds.join`). No Discord tokens
  are stored.
- The admin page *Sonstiges → Discord-Bot* (admin permission `Discord`): credentials, server, and
  read/write access per channel.
- Hardening (commit `d7d48f5ef`): POST-only actions, Discord rights checked on every send (timeouts,
  channel rights, slowmode, accepted rules), approvals tied to the channel's visibility in Discord,
  consent before anything is loaded from Discord, a CDN allowlist, rate limits and data retention.

## Before merging

1. **Migration number.** The branch adds `V042__add_discord_tables.sql`. `feature/campaign` reserves
   V042 for it and starts at V043, so the numbers do not collide.
   - Merge this branch **before** `feature/campaign`. If campaign lands first, databases that are
     already past V043 skip V042: Flyway does not apply lower versions afterwards (`outOfOrder` is off).
     In that case run Flyway once with `-outOfOrder=true` or renumber the migration.
   - V042 was rewritten on the branch (`9ac830544`). A local database that applied an earlier version
     of it fails Flyway's checksum validation: set it up fresh, or drop the three `discord_*` tables
     and the V042 row from `flyway_schema_history`.
2. **Merge.** The branch already contains `develop` up to the English renames (`2a16673e1`), so it
   merges without conflicts. The overview controller and template are now `OverviewController` and
   `overview.html`. A test merge with `feature/campaign` (which also contains that `develop`) goes
   through without conflicts too: both touch `OverviewController`, `overview.html`, `WellKnownUserValue`
   and `WellKnownAdminPermission`, but in different places. If `develop` moves on, merge it into the
   branch again first.
3. **New dependency.** `com.vdurmont:emoji-java:5.1.1` (emoji short codes like `:thumbsup:`).
4. **Tests.** `mvnw -pl game -am test` needs Docker (Testcontainers MySQL for Flyway and jOOQ). On a
   JDK newer than 11 the Spring integration tests need
   `-DargLine="--add-opens java.base/java.lang=ALL-UNNAMED"`, independent of this branch.

## Rolling out

The chat stays off (`aktiv = 0`) until an admin switches it on, so deploying the code alone changes
nothing for players.

1. **Discord application** (Developer Portal): create it, enable the *Message Content Intent*, add the
   redirect URI `https://<host>/ds?module=options&action=discordCallback`.
2. **Secrets.** Prefer the environment variables `DS_DISCORD_BOT_TOKEN` and
   `DS_DISCORD_CLIENT_SECRET` over the admin page, so the secrets stay out of database dumps.
   `docker/docker-compose.yml` passes both through; for a plain Tomcat, set them in `setenv.sh`.
3. **Admin page** *Sonstiges → Discord-Bot*: grant the admin permission `Discord` to whoever configures
   the chat. Enter the client ID, server ID, the **redirect URI (required, exactly as in the portal)**
   and the invite link (only `https://discord.gg/…` or `https://discord.com/invite/…` is accepted).
4. **Invite the bot** with the link on the admin page (permission integer `66561`: View Channels, Read
   Message History, Create Instant Invite). Then, in Discord:
   - grant *Manage Webhooks* **only in the channels the chat serves**, not server-wide;
   - give it *View Channels* and *Read Message History* in every private channel it should serve;
   - never make it an administrator. *Verbindung prüfen* warns about both.
5. **Verbindung prüfen**: every point should be green.
6. **Kanäle synchronisieren**, then set read/write access per channel, pick a default channel readable
   by all players, and press **Kanalrechte speichern**. Saving is what approves a channel: until then,
   and again whenever Discord moderators change who may see it, the channel is blocked in the game.
   Channels marked *(privat)* are not visible to `@everyone` in Discord. Giving them read access for
   all players exposes them to every player.
7. **Switch on** *Chat aktiv*.

## Outside the code

- **Privacy policy:** mention the Discord chat. Players who agree load avatars, emojis and images
  directly from Discord's CDN, which sees their IP address. The game also keeps which Discord account
  wrote a message sent from the game, for 30 days.
- **Discord server rules:** say that the public channels can be read in the game and that messages can
  be written from it.

## Smoke test after the rollout

- [ ] Overview: the chat box asks for consent first and loads nothing from Discord before (check the
      browser's network tab).
- [ ] Linking a Discord account in *Einstellungen* works, and an account that is not on the server yet
      gets added to it.
- [ ] A message from the game appears in Discord under the server alias, and in the game with the
      *DS* mark. Messages from other webhooks show *APP*.
- [ ] A Discord account in timeout, or without *Send Messages* in the channel, cannot write from the
      game. Slowmode applies.
- [ ] `[Text](https://example.com)` arrives as `Text (https://example.com)`. `@everyone` does not ping.
- [ ] A PM containing `[img]./ds?module=ueber&action=discordSend&channel=<id>&text=test[/img]` sends
      nothing when read.
- [ ] Making a channel private in Discord blocks it in the game, and *Kanalrechte speichern* releases it.
- [ ] *Verbindung trennen* removes the link. *Keine Inhalte mehr von Discord laden* stops the chat
      from loading content.

## Running it

- **Re-confirmation:** links expire after 30 days (`DiscordUser.LINK_VALID_DAYS`). Players
  then confirm them again at Discord, usually one click. Without stored tokens, this is how a revoked
  authorization is noticed.
- **Retention:** rows in `discord_sent_message` older than 30 days are deleted on every send, and rows of
  a player are deleted when they unlink or are deleted.
- **Rate limits:** each Discord account may send one message every 2 seconds and 10 per minute, each
  channel 25 per minute. Message lists are cached for 3 seconds per channel.
- **Logs:** `Discord: … ungueltige Anfragen in 10 Minuten` (error level) means the game paused all
  Discord calls to avoid an IP ban. That usually points to a revoked token or missing bot rights.
- **Turning it off:** untick *Chat aktiv*. Tables and settings stay in place.

## Known limits

- The approval covers the channel's own permission overwrites and the `@everyone` permissions. Changing
  a role's server-wide rights or who has a role is not detected, so review channel access when roles
  change.
- POST-only protects the Discord actions. Other actions in the game that change state on a GET can
  still be triggered through `[img]` in PMs. That is a framework-wide issue outside this branch.
