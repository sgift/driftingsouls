# Local instance

Serves the game on http://localhost:8080 against a MySQL container. The WAR must be built first —
**with `clean`**, or `maven-war-plugin` packages a stale exploded webapp and deleted assets reappear:

```bash
./mvnw -pl game -am clean package -DskipTests
docker compose -f docker/docker-compose.yml build app
docker compose -f docker/docker-compose.yml up -d
```

`db-init/` is auto-loaded into a *fresh* data volume: drop a `.sql`/`.sql.gz` dump in there before the
first `up`. Flyway then migrates whatever is in the database up to what the current code expects, and
runs once per `up`.

## Discord chat on the overview page

The chat box on the overview page shows channels of a Discord server. Players link their Discord
account under *Einstellungen* via OAuth2 — they log in at Discord, the game never sees their password.
The game reads the channels through a bot and posts player messages through a webhook named
"Drifting Souls", which it creates per channel. Messages carry the player's server alias and avatar,
as if written in Discord directly.
For what to do when merging and rolling out the chat, see [`docs/discord-chat-merge.md`](../docs/discord-chat-merge.md).

Everything is configured in the admin area under *Sonstiges → Discord-Bot* (admin permission
`Discord`): bot token, client ID and secret, server ID (our server `471610776126226432` is preset;
enter another one to test), redirect URI, and per channel who may read and who may write.
*Verbindung prüfen* checks the token, the Message Content Intent and the bot's permissions;
*Kanäle synchronisieren* fetches the server's text channels. New channels stay blocked until an admin
allows them. The page opens with a step-by-step guide for the first setup in Discord and in the game,
which collapses once the chat is active.

The bot token and client secret can instead come from the environment variables
`DS_DISCORD_BOT_TOKEN` and `DS_DISCORD_CLIENT_SECRET` (passed through by `docker-compose.yml`); they
take precedence over the database and keep the secrets out of database dumps.

To set up a bot:

1. In the [Discord Developer Portal](https://discord.com/developers/applications), create an application.
2. Under *Bot*, reset and copy the token, and enable the *Message Content Intent*. Without it, the
   messages arrive without text.
3. Under *OAuth2*, note the client ID and client secret, and add a redirect URI such as
   `http://localhost:8080/ds?module=options&action=discordCallback`. Enter exactly the same URI on the
   admin page — it is required, the game never derives it from the request.
4. Enter the values on the admin page and invite the bot with the link shown there. It asks for
   *View Channels*, *Read Message History* and *Create Instant Invite* (permission integer `66561`).
   Grant *Manage Webhooks* only in the channels the chat should serve, not server-wide, and never make
   the bot an administrator. The bot also needs *View Channels* and *Read Message History* in every
   private channel it should serve, e.g. via a role.

What the game enforces:

- **Read access** is granted for the visibility a channel has in Discord when an admin saves the channel
  rights. If Discord moderators later change who may see it (e.g. make it private), the channel is
  blocked in the game until an admin saves the channel rights again.
- **Writing** additionally needs the linked account's Discord rights: it must be a member that accepted
  the server rules, must not be timed out, needs *View Channel* and *Send Messages* in the channel, and
  is subject to the slowmode. Masked links `[text](url)` are posted as `text (url)`; mentions never ping.
  Each Discord account may send one message every 2 seconds and 10 per minute, each channel 25 per minute.
- **Re-confirmation:** a link holds for 30 days, then the player confirms it at Discord again. The game
  stores no tokens and would otherwise not notice a revoked authorization.
- **Privacy:** the browser loads avatars, emojis and images straight from Discord's CDN, so the chat only
  does so after the player agreed (in the chat box or by linking an account); the consent can be revoked
  under *Einstellungen*. Media from other hosts are never shown. Which Discord account wrote a message
  sent from the game is kept for 30 days and deleted when the link is removed or the player is deleted.
  The privacy policy should mention the Discord chat, and the Discord server rules should say that its
  public channels can be read in the game.
- **Request forgery:** sending, consenting, unlinking and every change in the admin page only work as
  POST requests (sending and consenting from the page's own script), so an image URL in a message
  (`[img]`) cannot trigger them.

## A second stack in parallel

Nothing in the compose file is named or bound absolutely, so a second checkout — typically a git
worktree for a hotfix running alongside longer-lived work — can bring up its own stack. In the second
checkout only:

```bash
git worktree add ../ds2-hotfix master        # from the main checkout
cd ../ds2-hotfix
cp docker/.env.sample docker/.env            # sets project name + ports; edit if 8081/3307 are taken
cp ../driftingsouls/docker/db-init/dump.sql.gz docker/db-init/   # optional, for real data
./mvnw -pl game -am clean package -DskipTests
docker compose -f docker/docker-compose.yml up -d --build
```

That stack gets its own containers, its own `db-data` volume and its own ports; the two share only the
Docker daemon and the images. Loading the dump again takes a while and costs a few GB of disk, so skip
it if an empty migrated schema is enough for what you are fixing.

**Do not create a `docker/.env` in the main checkout.** Without one the project name defaults to the
directory name (`docker`), which is what the existing `docker_db-data` volume — the one holding your
loaded dump — belongs to. Setting `COMPOSE_PROJECT_NAME` there would silently start from an empty
database.

Container names follow the project name, so commands that used to name containers directly become:

```bash
docker compose -f docker/docker-compose.yml exec db mysql -uroot -pds ds
docker compose -f docker/docker-compose.yml logs -f app
```

Run them from the checkout you mean, or pass `-p <project>` from anywhere.

## Tearing down

```bash
docker compose -f docker/docker-compose.yml down          # stop, keep the database
docker compose -f docker/docker-compose.yml down -v       # ...and delete its data volume
git worktree remove ../ds2-hotfix                         # once the branch is merged
```

`down -v` deletes the data volume of *that* project. Run from the wrong checkout it will happily drop
the main instance's loaded dump, so check `docker compose ls` first if in doubt.
