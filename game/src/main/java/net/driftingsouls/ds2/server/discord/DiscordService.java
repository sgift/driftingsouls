package net.driftingsouls.ds2.server.discord;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Anbindung des Ingame-Chats an den Discord-Server.
 * <p>
 * Spieler verknuepfen ihren Discord-Account per OAuth2 (Scopes <code>identify</code> und
 * <code>guilds.join</code>), ihr Passwort sieht DS dabei nie. Gelesen wird ueber einen Bot,
 * gesendet ueber einen Webhook je Kanal, der den Server-Alias und den Server-Avatar des
 * Spielers traegt - wie bei einer direkt in Discord geschriebenen Nachricht. Vor jedem Senden
 * wird wie in Discord geprueft, ob der Account im Kanal schreiben darf (Kanalrechte, Timeout,
 * Regelbestaetigung, Slowmode). Damit nicht jeder Spieler Discord einzeln abfragt, werden die
 * Nachrichten je Kanal kurz zwischengespeichert.
 * <p>
 * Welche Kanaele ein Spieler nutzen darf, prueft der Aufrufer ueber {@link DiscordChannel}.
 * Dieser Service selbst kennt keine Spielerrechte.
 */
@Service
public class DiscordService
{
	private static final Logger log = LoggerFactory.getLogger(DiscordService.class);

	private static final String API = "https://discord.com/api/v10";
	private static final String USER_AGENT = "DiscordBot (https://ds.drifting-souls.net, 1.0)";
	private static final String WEBHOOK_NAME = "Drifting Souls";
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration WRITE_TIMEOUT = Duration.ofSeconds(10);
	private static final long MESSAGE_CACHE_MILLIS = 3000;
	private static final long MEMBER_CACHE_MILLIS = 5 * 60 * 1000;
	private static final long SERVER_CACHE_MILLIS = 60 * 1000;
	/**
	 * So lange wird nach einer fehlgeschlagenen Anfrage nicht erneut gefragt.
	 */
	private static final long FAILURE_CACHE_MILLIS = 30 * 1000;
	/**
	 * Hoechstens so viele noch unbekannte Verfasser werden je Abruf eines Kanals nachgeladen, die
	 * uebrigen bei den naechsten Abrufen. So bleibt ein Abruf auch bei leerem Cache kurz.
	 */
	private static final int MAX_NEW_MEMBERS_PER_FETCH = 5;
	private static final int MESSAGE_LIMIT = 50;
	public static final int MAX_MESSAGE_LENGTH = 2000;

	static final long SEND_INTERVAL_MILLIS = 2000;
	static final long SEND_WINDOW_MILLIS = 60 * 1000;
	/**
	 * Nachrichten je Discord-Account und Minute.
	 */
	static final int MAX_MESSAGES_PER_ACCOUNT = 10;
	/**
	 * Nachrichten je Kanal und Minute ueber alle Spieler. Discord erlaubt einem Webhook etwa 30 je Minute.
	 */
	static final int MAX_MESSAGES_PER_CHANNEL = 25;

	/**
	 * Discord sperrt eine IP, die in 10 Minuten 10.000 ungueltige Anfragen (401, 403, 429) stellt.
	 * Lange vorher stellt DS die Anfragen fuer den Rest des Zeitraums ein.
	 */
	private static final long INVALID_WINDOW_MILLIS = 10 * 60 * 1000;
	private static final int MAX_INVALID_REQUESTS = 2500;

	static final long PERMISSION_CREATE_INVITE = 1L;
	static final long PERMISSION_ADMINISTRATOR = 1L << 3;
	static final long PERMISSION_MANAGE_CHANNELS = 1L << 4;
	static final long PERMISSION_VIEW_CHANNEL = 1L << 10;
	static final long PERMISSION_SEND_MESSAGES = 1L << 11;
	static final long PERMISSION_MANAGE_MESSAGES = 1L << 13;
	static final long PERMISSION_READ_MESSAGE_HISTORY = 1L << 16;
	static final long PERMISSION_MANAGE_WEBHOOKS = 1L << 29;
	static final long ALL_PERMISSIONS = -1L;
	/**
	 * Die Rechte, die der Bot auf dem ganzen Server braucht. Als Zahl fuer den Einladungslink des Bots.
	 * Webhooks verwalten darf er nur in den angebundenen Kanaelen, siehe {@link #CHANNEL_PERMISSIONS}.
	 */
	public static final long REQUIRED_PERMISSIONS = PERMISSION_CREATE_INVITE | PERMISSION_VIEW_CHANNEL | PERMISSION_READ_MESSAGE_HISTORY;
	/**
	 * Die Rechte, die der Bot in jedem angebundenen Kanal braucht.
	 */
	public static final long CHANNEL_PERMISSIONS = PERMISSION_VIEW_CHANNEL | PERMISSION_READ_MESSAGE_HISTORY | PERMISSION_MANAGE_WEBHOOKS;
	/**
	 * Die Rechte, die bestimmen, wer einen Kanal mitlesen kann. Nur sie gehen in die {@link #permissionSignature} ein.
	 */
	static final long VISIBILITY_PERMISSIONS = PERMISSION_VIEW_CHANNEL | PERMISSION_READ_MESSAGE_HISTORY;

	private static final Map<Long, String> PERMISSION_NAMES = new LinkedHashMap<>();
	static
	{
		PERMISSION_NAMES.put(PERMISSION_VIEW_CHANNEL, "Kanäle ansehen");
		PERMISSION_NAMES.put(PERMISSION_READ_MESSAGE_HISTORY, "Nachrichtenverlauf lesen");
		PERMISSION_NAMES.put(PERMISSION_MANAGE_WEBHOOKS, "Webhooks verwalten");
		PERMISSION_NAMES.put(PERMISSION_CREATE_INVITE, "Einladung erstellen");
	}

	private static final long FLAG_MESSAGE_CONTENT = 1L << 19;
	private static final long FLAG_MESSAGE_CONTENT_LIMITED = 1L << 18;

	private static final String UNREACHABLE = "Discord ist gerade nicht erreichbar. Bitte versuche es später erneut.";

	private static final Pattern SNOWFLAKE = Pattern.compile("\\d{1,25}");
	private static final Pattern USER_MENTION = Pattern.compile("<@!?(\\d+)>");
	private static final Pattern ROLE_MENTION = Pattern.compile("<@&(\\d+)>");
	private static final Pattern CHANNEL_MENTION = Pattern.compile("<#(\\d+)>");
	/**
	 * Eine Kurzform wie <code>:thumbsup:</code>, aber nicht der Name in einem Server-Emoji <code>&lt;:name:id&gt;</code>.
	 */
	private static final Pattern SHORTCODE = Pattern.compile("(?<![<\\w]):([A-Za-z0-9_+\\-]{2,40}):");
	private static final Pattern TIMESTAMP = Pattern.compile("<t:(-?\\d+)(?::[a-zA-Z])?>");
	/**
	 * Ein maskierter Link <code>[Text](URL)</code>, auch mit <code>&lt;URL&gt;</code> und Titel.
	 */
	private static final Pattern MASKED_LINK = Pattern.compile("\\[([^\\]\\n]*)\\]\\(\\s*<?([^\\s)>]+)>?(?:\\s+\"[^\"]*\")?\\s*\\)");
	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Berlin"));

	private static class ChannelCache
	{
		final ReentrantLock lock = new ReentrantLock();
		volatile List<DiscordChatMessage> messages = Collections.emptyList();
		volatile long fetchedAt;
		volatile long blockedUntil;
		/**
		 * Die Fehlermeldung, die bis <code>blockedUntil</code> geliefert wird, oder <code>null</code>.
		 */
		volatile String error;

		List<DiscordChatMessage> cachedResult()
		{
			String message = error;
			if (message != null && System.currentTimeMillis() < blockedUntil)
			{
				throw new DiscordException(message);
			}
			return messages;
		}
	}

	private static class Webhook
	{
		final String id;
		final String token;

		Webhook(String id, String token)
		{
			this.id = id;
			this.token = token;
		}
	}

	/**
	 * Ein Account aus Sicht des Discord-Servers: Server-Alias (sonst Anzeigename) und Server-Avatar
	 * (sonst Account-Avatar). <code>mitglied == false</code>, falls der Account nicht auf dem Server ist.
	 */
	private static class Member
	{
		static Member failed()
		{
			Member member = new Member(false, null, null, null, Collections.emptySet(), false, 0);
			member.failed = true;
			return member;
		}

		final boolean member;
		final String name;
		final String avatarUrl;
		/**
		 * Die Farbe der hoechsten farbigen Rolle als <code>#rrggbb</code> oder <code>null</code>.
		 */
		final String color;
		final Set<String> roles;
		/**
		 * <code>true</code>, falls der Account die Regeln des Servers noch nicht bestaetigt hat.
		 */
		final boolean pending;
		/**
		 * Bis wann der Account stummgeschaltet ist (Timeout), in Millisekunden.
		 */
		final long mutedUntil;
		final long loaded = System.currentTimeMillis();
		/**
		 * <code>true</code>, falls Discord nicht sauber geantwortet hat. Dann wird eine Weile nicht erneut gefragt.
		 */
		boolean failed;

		Member(boolean member, String name, String avatarUrl, String color, Set<String> roles, boolean pending, long mutedUntil)
		{
			this.member = member;
			this.name = name;
			this.avatarUrl = avatarUrl;
			this.color = color;
			this.roles = roles;
			this.pending = pending;
			this.mutedUntil = mutedUntil;
		}

		boolean isCurrent(long now)
		{
			return now - loaded < (failed ? FAILURE_CACHE_MILLIS : MEMBER_CACHE_MILLIS);
		}
	}

	/**
	 * Was zu einem Server gebraucht wird: Eigentuemer, Rollen und eigene Emojis.
	 * Ist <code>fehler</code> gesetzt, konnte Discord den Server nicht liefern.
	 */
	private static class ServerData
	{
		String ownerId;
		final Map<String, Long> rolePermissions = new HashMap<>();
		final Map<String, int[]> colorAndPosition = new HashMap<>();
		/**
		 * Die eigenen Emojis als Name -&gt; Markup.
		 */
		final Map<String, String> emojis = new HashMap<>();
		String error;
		final long loaded = System.currentTimeMillis();

		boolean isCurrent(long now)
		{
			return now - loaded < (error != null ? FAILURE_CACHE_MILLIS : SERVER_CACHE_MILLIS);
		}
	}

	/**
	 * Die Kanaele eines Servers samt Rechten und Slowmode.
	 */
	private static class ChannelList
	{
		final Map<String, JsonObject> channels = new HashMap<>();
		String error;
		final long loaded = System.currentTimeMillis();

		boolean isCurrent(long now)
		{
			return now - loaded < (error != null ? FAILURE_CACHE_MILLIS : SERVER_CACHE_MILLIS);
		}
	}

	static class BotPermissions
	{
		String botId;
		String botName;
		Set<String> roles = new HashSet<>();
		Map<String, Long> rolePermissions = new HashMap<>();
		long serverPermissions;
		boolean administrator;
	}

	/**
	 * Begrenzt, wie oft gesendet wird: je Discord-Account, je Kanal und nach dem Slowmode des Kanals.
	 * Pruefen und Vermerken geschehen gemeinsam, parallele Anfragen koennen die Grenzen also nicht umgehen.
	 */
	static class SendLimits
	{
		private static final long MAX_SLOWMODE_MILLIS = 6 * 60 * 60 * 1000;

		private final Map<String, Deque<Long>> perAccount = new HashMap<>();
		private final Map<String, Deque<Long>> perChannel = new HashMap<>();
		private final Map<String, Long> lastInChannel = new HashMap<>();

		/**
		 * Prueft, ob der Account jetzt in den Kanal senden darf, und vermerkt die Nachricht.
		 * @param discordUserId Der Account
		 * @param channelId Der Kanal
		 * @param slowmodeSeconds Der Slowmode, der fuer den Account gilt, oder 0
		 * @param now Der aktuelle Zeitpunkt in Millisekunden
		 * @throws DiscordException Falls er jetzt nicht senden darf
		 */
		synchronized void checkAndRecord(String discordUserId, String channelId, int slowmodeSeconds, long now)
		{
			Deque<Long> account = recent(perAccount, discordUserId, now);
			if (!account.isEmpty() && now - account.peekLast() < SEND_INTERVAL_MILLIS)
			{
				throw new DiscordException("Bitte warte einen Moment, bevor du die nächste Nachricht sendest.");
			}
			if (account.size() >= MAX_MESSAGES_PER_ACCOUNT)
			{
				throw new DiscordException("Du hast in der letzten Minute zu viele Nachrichten gesendet. Bitte warte einen Moment.");
			}
			Deque<Long> channel = recent(perChannel, channelId, now);
			if (channel.size() >= MAX_MESSAGES_PER_CHANNEL)
			{
				throw new DiscordException("In diesem Kanal wird gerade sehr viel geschrieben. Bitte versuche es gleich noch einmal.");
			}
			String slowmodeKey = channelId + ":" + discordUserId;
			Long last = lastInChannel.get(slowmodeKey);
			if (slowmodeSeconds > 0 && last != null && now - last < slowmodeSeconds * 1000L)
			{
				long rest = (slowmodeSeconds * 1000L - (now - last) + 999) / 1000;
				throw new DiscordException("In diesem Kanal gilt ein Slowmode von " + slowmodeSeconds + " Sekunden. Bitte warte noch " + rest + " Sekunden.");
			}

			account.addLast(now);
			channel.addLast(now);
			if (lastInChannel.size() > 10000)
			{
				lastInChannel.values().removeIf(time -> now - time > MAX_SLOWMODE_MILLIS);
			}
			lastInChannel.put(slowmodeKey, now);
		}

		private static Deque<Long> recent(Map<String, Deque<Long>> times, String key, long now)
		{
			Deque<Long> result = times.computeIfAbsent(key, k -> new ArrayDeque<>());
			while (!result.isEmpty() && now - result.peekFirst() >= SEND_WINDOW_MILLIS)
			{
				result.removeFirst();
			}
			return result;
		}
	}

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private final Gson gson = new Gson();
	private final Map<String, ChannelCache> messageCache = new ConcurrentHashMap<>();
	private final Map<String, Webhook> webhooks = new ConcurrentHashMap<>();
	private final Map<String, Object> webhookLocks = new ConcurrentHashMap<>();
	private final Map<String, Member> members = new ConcurrentHashMap<>();
	private final Map<String, ServerData> server = new ConcurrentHashMap<>();
	private final Map<String, ChannelList> channelLists = new ConcurrentHashMap<>();
	private final SendLimits sendLimits = new SendLimits();

	private final Object invalidLock = new Object();
	private long invalidSince;
	private int invalidCount;
	private volatile long globallyBlockedUntil;

	/**
	 * Verwirft alle zwischengespeicherten Daten, z.B. nachdem Bot oder Server geaendert wurden.
	 */
	public void reset()
	{
		messageCache.clear();
		webhooks.clear();
		members.clear();
		server.clear();
		channelLists.clear();
	}

	/**
	 * Gibt die URL zurueck, auf der der Spieler die Verknuepfung bei Discord bestaetigt.
	 * @param configuration Die Bot-Einstellungen
	 * @param redirectUri Die Redirect-URI
	 * @param state Zufaelliger Wert gegen CSRF, der beim Callback geprueft wird
	 * @return Die URL
	 */
	public String getAuthorizeUrl(DiscordConfiguration configuration, String redirectUri, String state)
	{
		return "https://discord.com/oauth2/authorize?response_type=code"
				+ "&client_id=" + encode(configuration.getClientId().trim())
				+ "&scope=" + encode("identify guilds.join")
				+ "&redirect_uri=" + encode(redirectUri)
				+ "&state=" + encode(state)
				+ "&prompt=none";
	}

	/**
	 * Tauscht den Code aus dem OAuth2-Callback gegen ein Access-Token.
	 * @param configuration Die Bot-Einstellungen
	 * @param redirectUri Die Redirect-URI, mit der der Login gestartet wurde
	 * @param code Der Code
	 * @return Das Access-Token
	 */
	public String exchangeCode(DiscordConfiguration configuration, String redirectUri, String code)
	{
		String form = "grant_type=authorization_code"
				+ "&code=" + encode(code)
				+ "&redirect_uri=" + encode(redirectUri)
				+ "&client_id=" + encode(configuration.getClientId().trim())
				+ "&client_secret=" + encode(configuration.getClientSecret().trim());
		HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(API + "/oauth2/token"))
				.timeout(WRITE_TIMEOUT)
				.header("User-Agent", USER_AGENT)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(form)));
		String accessToken = response.statusCode() == 200 ? string(JsonParser.parseString(response.body()).getAsJsonObject(), "access_token") : null;
		if (accessToken == null)
		{
			log.warn("Discord-OAuth2: Token-Austausch fehlgeschlagen ({})", response.statusCode());
			throw new DiscordException("Die Anmeldung bei Discord ist fehlgeschlagen. Bitte versuche es erneut.");
		}
		return accessToken;
	}

	/**
	 * Laedt den Discord-Account, dem das Access-Token gehoert.
	 * @param accessToken Das Access-Token
	 * @return Der Account
	 */
	public DiscordUser fetchUser(String accessToken)
	{
		HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(API + "/users/@me"))
				.timeout(READ_TIMEOUT)
				.header("User-Agent", USER_AGENT)
				.header("Authorization", "Bearer " + accessToken)
				.GET());
		if (response.statusCode() != 200)
		{
			log.warn("Discord-OAuth2: Laden des Accounts fehlgeschlagen ({}): {}", response.statusCode(), response.body());
			throw new DiscordException("Dein Discord-Account konnte nicht geladen werden. Bitte versuche es erneut.");
		}
		JsonObject user = JsonParser.parseString(response.body()).getAsJsonObject();
		String id = string(user, "id");
		if (id == null || !SNOWFLAKE.matcher(id).matches())
		{
			throw new DiscordException("Dein Discord-Account konnte nicht geladen werden. Bitte versuche es erneut.");
		}
		return new DiscordUser(id, displayName(user), string(user, "avatar"));
	}

	/**
	 * Fuegt den Account dem Discord-Server hinzu, falls er noch kein Mitglied ist.
	 * Dafuer braucht der Bot das Recht, Einladungen zu erstellen.
	 * @param configuration Die Bot-Einstellungen
	 * @param user Der Account
	 * @param accessToken Das Access-Token des Accounts mit Scope <code>guilds.join</code>
	 * @return <code>true</code>, falls der Account danach Mitglied ist
	 */
	public boolean joinGuild(DiscordConfiguration configuration, DiscordUser user, String accessToken)
	{
		JsonObject body = new JsonObject();
		body.addProperty("access_token", accessToken);
		HttpResponse<String> response = send(botRequest(configuration, "/guilds/" + configuration.getGuildId() + "/members/" + user.id, WRITE_TIMEOUT)
				.header("Content-Type", "application/json")
				.PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(body))));
		if (response.statusCode() == 201 || response.statusCode() == 204)
		{
			// Beim naechsten Zugriff mit Server-Alias und -Avatar neu laden
			members.remove(memberKey(configuration, user.id));
			return true;
		}
		log.warn("Discord: Beitritt von {} zum Server fehlgeschlagen ({}): {}", user.id, response.statusCode(), response.body());
		return false;
	}

	/**
	 * Gibt die neuesten Nachrichten eines Kanals zurueck, aelteste zuerst.
	 * Der Aufrufer muss vorher pruefen, dass der Spieler den Kanal lesen darf.
	 * <p>
	 * Laedt gerade eine andere Anfrage den Kanal neu, wird nicht gewartet, sondern der
	 * bisherige Stand geliefert. Schlaegt das Laden fehl, wird eine Weile nicht erneut gefragt.
	 * @param configuration Die Bot-Einstellungen
	 * @param channelId Die ID des Kanals
	 * @param channelNames Bekannte Kanalnamen (ID -&gt; Name) zum Aufloesen von Kanalverweisen
	 * @return Die Nachrichten
	 */
	public List<DiscordChatMessage> getMessages(DiscordConfiguration configuration, String channelId, Map<String, String> channelNames)
	{
		ChannelCache cache = messageCache.computeIfAbsent(channelId, k -> new ChannelCache());
		if (!isDueForReload(cache) || !cache.lock.tryLock())
		{
			return cache.cachedResult();
		}
		try
		{
			if (!isDueForReload(cache))
			{
				return cache.cachedResult();
			}
			long now = System.currentTimeMillis();
			cache.fetchedAt = now;

			HttpResponse<String> response;
			try
			{
				response = send(botRequest(configuration, "/channels/" + channelId + "/messages?limit=" + MESSAGE_LIMIT, READ_TIMEOUT).GET());
			}
			catch (DiscordException e)
			{
				block(cache, now, e.getMessage());
				throw e;
			}
			if (response.statusCode() == 429)
			{
				cache.blockedUntil = now + retryAfterMillis(response);
				return cache.messages;
			}
			if (response.statusCode() != 200)
			{
				log.warn("Discord: Laden der Nachrichten aus Kanal {} fehlgeschlagen ({}): {}", channelId, response.statusCode(), response.body());
				block(cache, now, "Die Nachrichten konnten nicht von Discord geladen werden.");
				return cache.cachedResult();
			}

			JsonArray messages = JsonParser.parseString(response.body()).getAsJsonArray();
			List<DiscordChatMessage> result = new ArrayList<>();
			int[] newMembers = {MAX_NEW_MEMBERS_PER_FETCH};
			// Discord liefert die neueste Nachricht zuerst
			for (int i = messages.size() - 1; i >= 0; i--)
			{
				JsonObject message = messages.get(i).getAsJsonObject();
				int type = message.get("type").getAsInt();
				// 0 = normale Nachricht, 19 = Antwort. Systemmeldungen (Beitritte, Pins, ...) auslassen.
				if (type != 0 && type != 19)
				{
					continue;
				}
				result.add(toChatMessage(configuration, message, channelNames, newMembers));
			}
			cache.messages = Collections.unmodifiableList(result);
			cache.error = null;
			return cache.messages;
		}
		finally
		{
			cache.lock.unlock();
		}
	}

	private static boolean isDueForReload(ChannelCache cache)
	{
		long now = System.currentTimeMillis();
		return now - cache.fetchedAt >= MESSAGE_CACHE_MILLIS && now >= cache.blockedUntil;
	}

	private static void block(ChannelCache cache, long now, String error)
	{
		cache.error = error;
		cache.blockedUntil = now + FAILURE_CACHE_MILLIS;
	}

	/**
	 * Sendet eine Nachricht im Namen eines Spielers in einen Kanal.
	 * Der Aufrufer muss vorher pruefen, dass der Spieler im Spiel in den Kanal schreiben darf.
	 * Hier wird zusaetzlich geprueft, ob der Discord-Account dort schreiben duerfte: Er muss
	 * Mitglied sein, die Regeln des Servers bestaetigt haben, darf nicht stummgeschaltet sein,
	 * braucht im Kanal die Rechte zum Ansehen und Schreiben und unterliegt dem Slowmode.
	 * @param configuration Die Bot-Einstellungen
	 * @param channelId Die ID des Kanals
	 * @param discordUserId Die ID des verknuepften Discord-Accounts. Name und Avatar der Nachricht
	 * sind sein Server-Alias und Server-Avatar, wie bei einer direkt in Discord geschriebenen Nachricht.
	 * @param text Der Text
	 * @return Die ID der gesendeten Nachricht
	 */
	public String sendMessage(DiscordConfiguration configuration, String channelId, String discordUserId, String text)
	{
		String content = text == null ? "" : text.trim();
		if (content.isEmpty())
		{
			throw new DiscordException("Die Nachricht ist leer.");
		}
		if (content.length() > MAX_MESSAGE_LENGTH)
		{
			throw new DiscordException("Die Nachricht ist zu lang (maximal " + MAX_MESSAGE_LENGTH + " Zeichen).");
		}
		content = prepareForSending(configuration, defuseLinks(content));
		if (content.length() > MAX_MESSAGE_LENGTH)
		{
			throw new DiscordException("Die Nachricht ist zu lang (maximal " + MAX_MESSAGE_LENGTH + " Zeichen).");
		}

		// Immer frisch laden: Ein Bann oder Timeout soll sofort wirken
		Member member = loadMember(configuration, discordUserId, true);
		if (!member.member)
		{
			throw new DiscordException("Dein Discord-Account ist kein Mitglied unseres Discord-Servers. Tritt dem Server bei und versuche es erneut.");
		}
		if (member.pending)
		{
			throw new DiscordException("Bestätige zuerst in Discord die Regeln unseres Discord-Servers.");
		}
		long now = System.currentTimeMillis();
		if (member.mutedUntil > now)
		{
			throw new DiscordException("Du bist auf unserem Discord-Server bis " + TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(member.mutedUntil)) + " stummgeschaltet.");
		}

		ServerData serverData = loadServer(configuration);
		if (serverData.error != null)
		{
			throw new DiscordException(serverData.error);
		}
		JsonObject channel = loadChannel(configuration, channelId);
		long permissions = memberPermissions(serverData.ownerId, serverData.rolePermissions, configuration.getGuildId(), discordUserId, member.roles, channel);
		if ((permissions & (PERMISSION_VIEW_CHANNEL | PERMISSION_SEND_MESSAGES)) != (PERMISSION_VIEW_CHANNEL | PERMISSION_SEND_MESSAGES))
		{
			throw new DiscordException("Dein Discord-Account darf in diesem Kanal nicht schreiben.");
		}
		boolean exemptFromSlowmode = (permissions & (PERMISSION_MANAGE_MESSAGES | PERMISSION_MANAGE_CHANNELS)) != 0;
		int slowmode = exemptFromSlowmode || !channel.has("rate_limit_per_user") ? 0 : channel.get("rate_limit_per_user").getAsInt();
		sendLimits.checkAndRecord(discordUserId, channelId, slowmode, now);

		JsonObject allowedMentions = new JsonObject();
		allowedMentions.add("parse", new JsonArray());
		JsonObject body = new JsonObject();
		body.addProperty("content", content);
		body.addProperty("username", webhookUsername(member.name));
		body.addProperty("avatar_url", member.avatarUrl);
		body.add("allowed_mentions", allowedMentions);
		String json = gson.toJson(body);

		HttpResponse<String> response = executeWebhook(configuration, channelId, json);
		if (response.statusCode() == 404)
		{
			// Webhook wurde in Discord geloescht - neu anlegen und erneut versuchen
			webhooks.remove(channelId);
			response = executeWebhook(configuration, channelId, json);
		}
		if (response.statusCode() == 429)
		{
			throw new DiscordException("Gerade werden zu viele Nachrichten gesendet. Bitte versuche es gleich noch einmal.");
		}
		if (response.statusCode() != 200)
		{
			log.warn("Discord: Senden in Kanal {} fehlgeschlagen ({}): {}", channelId, response.statusCode(), response.body());
			throw new DiscordException("Die Nachricht konnte nicht an Discord gesendet werden.");
		}

		ChannelCache cache = messageCache.get(channelId);
		if (cache != null)
		{
			cache.fetchedAt = 0;
		}
		// Dank wait=true liefert Discord die angelegte Nachricht zurueck
		return string(JsonParser.parseString(response.body()).getAsJsonObject(), "id");
	}

	/**
	 * Vergisst die zwischengespeicherte Mitgliedschaft eines Accounts, z.B. nach dem Trennen der Verknuepfung.
	 * @param discordUserId Die ID des Accounts
	 */
	public void forgetMember(String discordUserId)
	{
		members.keySet().removeIf(key -> key.endsWith(":" + discordUserId));
	}

	/**
	 * Gibt die aktuelle {@link #permissionSignature} eines Kanals zurueck (zwischengespeichert).
	 * @param configuration Die Bot-Einstellungen
	 * @param channelId Die ID des Kanals
	 * @return Die Signatur
	 * @throws DiscordException Falls Discord nicht antwortet oder es den Kanal nicht mehr gibt
	 */
	public String getPermissionSignature(DiscordConfiguration configuration, String channelId)
	{
		ServerData serverData = loadServer(configuration);
		if (serverData.error != null)
		{
			throw new DiscordException(serverData.error);
		}
		JsonObject channel = loadChannel(configuration, channelId);
		return permissionSignature(configuration.getGuildId(), serverData.rolePermissions.getOrDefault(configuration.getGuildId(), 0L), channel);
	}

	/**
	 * Laedt die Textkanaele des Servers samt fehlender Rechte des Bots je Kanal.
	 * @param configuration Die Bot-Einstellungen
	 * @return Die Kanaele in der Reihenfolge von Discord
	 */
	public List<DiscordGuildChannel> loadChannels(DiscordConfiguration configuration)
	{
		BotPermissions permissions = loadBotPermissions(configuration);

		HttpResponse<String> response = send(botRequest(configuration, "/guilds/" + configuration.getGuildId() + "/channels", READ_TIMEOUT).GET());
		if (response.statusCode() != 200)
		{
			log.warn("Discord: Laden der Kanaele fehlgeschlagen ({}): {}", response.statusCode(), response.body());
			throw new DiscordException("Die Kanäle konnten nicht geladen werden (HTTP " + response.statusCode() + ").");
		}

		JsonArray channels = JsonParser.parseString(response.body()).getAsJsonArray();
		// Gleich als aktuellen Stand fuer die Pruefungen beim Lesen und Senden uebernehmen
		channelLists.put(configuration.getGuildId(), channelList(channels));

		Map<String, JsonObject> categories = new HashMap<>();
		for (JsonElement element : channels)
		{
			JsonObject channel = element.getAsJsonObject();
			if (channel.get("type").getAsInt() == 4)
			{
				categories.put(string(channel, "id"), channel);
			}
		}

		long everyone = permissions.rolePermissions.getOrDefault(configuration.getGuildId(), 0L);
		List<DiscordGuildChannel> result = new ArrayList<>();
		for (JsonElement element : channels)
		{
			JsonObject channel = element.getAsJsonObject();
			int type = channel.get("type").getAsInt();
			// 0 = Textkanal, 5 = Ankuendigungskanal
			if (type != 0 && type != 5)
			{
				continue;
			}
			JsonObject category = categories.get(string(channel, "parent_id"));
			int categoryPosition = category != null ? category.get("position").getAsInt() + 1 : 0;
			int position = categoryPosition * 1000 + channel.get("position").getAsInt();

			result.add(new DiscordGuildChannel(
					string(channel, "id"),
					string(channel, "name"),
					category != null ? string(category, "name") : null,
					position,
					missingChannelPermissions(permissions, configuration.getGuildId(), channel),
					isVisibleToEveryone(configuration.getGuildId(), everyone, channel),
					permissionSignature(configuration.getGuildId(), everyone, channel)));
		}
		result.sort((a, b) -> Integer.compare(a.position, b.position));
		return result;
	}

	/**
	 * Prueft Bot-Token, Server, Message Content Intent und die Rechte des Bots auf dem Server.
	 * @param configuration Die Bot-Einstellungen
	 * @return Das Ergebnis
	 */
	public DiscordCheck check(DiscordConfiguration configuration)
	{
		DiscordCheck check = new DiscordCheck();
		if (!DiscordConfiguration.isSet(configuration.getBotToken()) || !DiscordConfiguration.isSet(configuration.getGuildId()))
		{
			check.error("Bot-Token und Server-ID müssen gesetzt sein.");
			return check;
		}
		if (!DiscordConfiguration.isSet(configuration.getRedirectUri()))
		{
			check.error("Die Redirect-URI ist nicht gesetzt - ohne sie können Spieler ihren Account nicht verknüpfen.");
		}

		try
		{
			HttpResponse<String> response = send(botRequest(configuration, "/applications/@me", READ_TIMEOUT).GET());
			if (response.statusCode() == 401)
			{
				check.error("Das Bot-Token ist ungültig.");
				return check;
			}
			if (response.statusCode() == 200)
			{
				JsonObject application = JsonParser.parseString(response.body()).getAsJsonObject();
				long flags = application.has("flags") ? application.get("flags").getAsLong() : 0;
				String clientId = configuration.getClientId() != null ? configuration.getClientId().trim() : "";
				if (!clientId.isEmpty() && !clientId.equals(string(application, "id")))
				{
					check.error("Die Client-ID passt nicht zur Anwendung des Bots (" + string(application, "id") + ").");
				}
				if ((flags & (FLAG_MESSAGE_CONTENT | FLAG_MESSAGE_CONTENT_LIMITED)) != 0)
				{
					check.ok("Message Content Intent ist aktiviert.");
				}
				else
				{
					check.error("Message Content Intent ist nicht aktiviert - Nachrichten kommen ohne Text an (Developer Portal → Bot).");
				}
			}

			BotPermissions permissions = loadBotPermissions(configuration);
			check.ok("Angemeldet als Bot \"" + permissions.botName + "\".");

			response = send(botRequest(configuration, "/guilds/" + configuration.getGuildId(), READ_TIMEOUT).GET());
			if (response.statusCode() == 200)
			{
				check.ok("Server \"" + string(JsonParser.parseString(response.body()).getAsJsonObject(), "name") + "\" gefunden.");
			}

			List<String> missing = missingPermissions(permissions.serverPermissions, REQUIRED_PERMISSIONS);
			if (missing.isEmpty())
			{
				check.ok("Der Bot hat auf dem Server alle benötigten Rechte.");
			}
			else
			{
				check.error("Dem Bot fehlen auf dem Server die Rechte: " + String.join(", ", missing) + ".");
			}
			if (permissions.administrator)
			{
				check.error("Der Bot ist Administrator. Gib ihm nur die benötigten Rechte - wer das Bot-Token hat, kann sonst den ganzen Server verwalten.");
			}
			else if ((permissions.serverPermissions & PERMISSION_MANAGE_WEBHOOKS) != 0)
			{
				check.error("Der Bot darf auf dem ganzen Server Webhooks verwalten. Erlaube das besser nur in den angebundenen Kanälen.");
			}
		}
		catch (DiscordException e)
		{
			check.error(e.getMessage());
		}
		return check;
	}

	private BotPermissions loadBotPermissions(DiscordConfiguration configuration)
	{
		BotPermissions permissions = new BotPermissions();

		HttpResponse<String> response = send(botRequest(configuration, "/users/@me", READ_TIMEOUT).GET());
		if (response.statusCode() == 401)
		{
			throw new DiscordException("Das Bot-Token ist ungültig.");
		}
		if (response.statusCode() != 200)
		{
			throw new DiscordException("Der Bot konnte nicht geladen werden (HTTP " + response.statusCode() + ").");
		}
		JsonObject bot = JsonParser.parseString(response.body()).getAsJsonObject();
		permissions.botId = string(bot, "id");
		permissions.botName = string(bot, "username");

		String guildId = configuration.getGuildId();
		response = send(botRequest(configuration, "/guilds/" + guildId + "/members/" + permissions.botId, READ_TIMEOUT).GET());
		if (response.statusCode() == 403 || response.statusCode() == 404)
		{
			throw new DiscordException("Der Bot \"" + permissions.botName + "\" ist nicht auf dem Server " + guildId + ". Lade ihn zuerst über den Einladungslink ein.");
		}
		if (response.statusCode() != 200)
		{
			throw new DiscordException("Die Mitgliedschaft des Bots konnte nicht geladen werden (HTTP " + response.statusCode() + ").");
		}
		for (JsonElement role : JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("roles"))
		{
			permissions.roles.add(role.getAsString());
		}

		response = send(botRequest(configuration, "/guilds/" + guildId + "/roles", READ_TIMEOUT).GET());
		if (response.statusCode() != 200)
		{
			throw new DiscordException("Die Rollen des Servers konnten nicht geladen werden (HTTP " + response.statusCode() + ").");
		}
		for (JsonElement element : JsonParser.parseString(response.body()).getAsJsonArray())
		{
			JsonObject role = element.getAsJsonObject();
			permissions.rolePermissions.put(string(role, "id"), Long.parseLong(string(role, "permissions")));
		}

		// Die @everyone-Rolle hat die ID des Servers
		long serverPermissions = permissions.rolePermissions.getOrDefault(guildId, 0L);
		for (String role : permissions.roles)
		{
			serverPermissions |= permissions.rolePermissions.getOrDefault(role, 0L);
		}
		permissions.administrator = (serverPermissions & PERMISSION_ADMINISTRATOR) != 0;
		permissions.serverPermissions = permissions.administrator ? ALL_PERMISSIONS : serverPermissions;
		return permissions;
	}

	/**
	 * Berechnet die fehlenden Rechte des Bots in einem Kanal.
	 */
	static String missingChannelPermissions(BotPermissions permissions, String guildId, JsonObject channel)
	{
		long channelPermissions = channelPermissions(permissions.serverPermissions, guildId, permissions.roles, permissions.botId, channel);
		List<String> missing = missingPermissions(channelPermissions, CHANNEL_PERMISSIONS);
		return missing.isEmpty() ? null : "Bot fehlt: " + String.join(", ", missing);
	}

	/**
	 * Berechnet die Rechte eines Mitglieds in einem Kanal nach dem Verfahren von Discord:
	 * Der Eigentuemer und Administratoren duerfen alles, sonst gelten die Rechte von @everyone und
	 * den Rollen des Mitglieds, angepasst durch die Overwrites des Kanals.
	 */
	static long memberPermissions(String ownerId, Map<String, Long> rolePermissions, String guildId, String userId, Set<String> roles, JsonObject channel)
	{
		if (userId.equals(ownerId))
		{
			return ALL_PERMISSIONS;
		}
		long base = rolePermissions.getOrDefault(guildId, 0L);
		for (String role : roles)
		{
			base |= rolePermissions.getOrDefault(role, 0L);
		}
		if ((base & PERMISSION_ADMINISTRATOR) != 0)
		{
			return ALL_PERMISSIONS;
		}
		return channelPermissions(base, guildId, roles, userId, channel);
	}

	/**
	 * Wendet die Overwrites eines Kanals auf Server-Rechte an: erst die fuer @everyone,
	 * dann die der Rollen, zuletzt die fuer den Account selbst.
	 */
	private static long channelPermissions(long serverPermissions, String guildId, Set<String> roles, String userId, JsonObject channel)
	{
		if (serverPermissions == ALL_PERMISSIONS || !channel.has("permission_overwrites"))
		{
			return serverPermissions;
		}
		long rolesAllowed = 0;
		long rolesDenied = 0;
		long everyoneAllowed = 0;
		long everyoneDenied = 0;
		long accountAllowed = 0;
		long accountDenied = 0;
		for (JsonElement element : channel.getAsJsonArray("permission_overwrites"))
		{
			JsonObject overwrite = element.getAsJsonObject();
			String id = string(overwrite, "id");
			long allowed = Long.parseLong(string(overwrite, "allow"));
			long denied = Long.parseLong(string(overwrite, "deny"));
			if (id.equals(guildId))
			{
				everyoneAllowed = allowed;
				everyoneDenied = denied;
			}
			else if (overwrite.get("type").getAsInt() == 0 && roles.contains(id))
			{
				rolesAllowed |= allowed;
				rolesDenied |= denied;
			}
			else if (overwrite.get("type").getAsInt() == 1 && id.equals(userId))
			{
				accountAllowed = allowed;
				accountDenied = denied;
			}
		}
		long channelPermissions = serverPermissions;
		channelPermissions = (channelPermissions & ~everyoneDenied) | everyoneAllowed;
		channelPermissions = (channelPermissions & ~rolesDenied) | rolesAllowed;
		channelPermissions = (channelPermissions & ~accountDenied) | accountAllowed;
		return channelPermissions;
	}

	/**
	 * Gibt zurueck, ob @everyone den Kanal sehen darf.
	 */
	static boolean isVisibleToEveryone(String guildId, long everyonePermissions, JsonObject channel)
	{
		long permissions = (everyonePermissions & PERMISSION_ADMINISTRATOR) != 0 ? ALL_PERMISSIONS : everyonePermissions;
		return (channelPermissions(permissions, guildId, Collections.emptySet(), null, channel) & PERMISSION_VIEW_CHANNEL) != 0;
	}

	/**
	 * Fasst zusammen, wer einen Kanal in Discord mitlesen kann: die Sichtbarkeitsrechte von
	 * @everyone und alle Overwrites des Kanals, die diese Rechte beruehren. Aendert sich die
	 * Signatur, wurde der Kanal in Discord fuer andere geoeffnet oder geschlossen.
	 * @param guildId Die ID des Servers
	 * @param everyonePermissions Die Server-Rechte von @everyone
	 * @param channel Der Kanal, wie Discord ihn liefert
	 * @return Die Signatur als Hex-String (64 Zeichen)
	 */
	static String permissionSignature(String guildId, long everyonePermissions, JsonObject channel)
	{
		List<String> parts = new ArrayList<>();
		if (channel.has("permission_overwrites"))
		{
			for (JsonElement element : channel.getAsJsonArray("permission_overwrites"))
			{
				JsonObject overwrite = element.getAsJsonObject();
				long allowed = Long.parseLong(string(overwrite, "allow")) & VISIBILITY_PERMISSIONS;
				long denied = Long.parseLong(string(overwrite, "deny")) & VISIBILITY_PERMISSIONS;
				if (allowed != 0 || denied != 0)
				{
					parts.add(overwrite.get("type").getAsInt() + ":" + string(overwrite, "id") + ":" + allowed + ":" + denied);
				}
			}
		}
		Collections.sort(parts);
		String text = guildId + "|" + (everyonePermissions & (VISIBILITY_PERMISSIONS | PERMISSION_ADMINISTRATOR)) + "|" + String.join(";", parts);
		try
		{
			StringBuilder hex = new StringBuilder();
			for (byte b : MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)))
			{
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private static List<String> missingPermissions(long existing, long required)
	{
		List<String> missing = new ArrayList<>();
		for (Map.Entry<Long, String> permission : PERMISSION_NAMES.entrySet())
		{
			if ((required & permission.getKey()) != 0 && (existing & permission.getKey()) == 0)
			{
				missing.add(permission.getValue());
			}
		}
		return missing;
	}

	/**
	 * Laedt einen Account als Mitglied des Servers.
	 * @param fresh <code>true</code>, um den Zwischenspeicher zu umgehen
	 * @throws DiscordException Falls Discord nicht sauber antwortet
	 */
	private Member loadMember(DiscordConfiguration configuration, String discordUserId, boolean fresh)
	{
		if (discordUserId == null || !SNOWFLAKE.matcher(discordUserId).matches())
		{
			return new Member(false, null, null, null, Collections.emptySet(), false, 0);
		}
		String key = memberKey(configuration, discordUserId);
		Member member = members.get(key);
		if (!fresh && member != null && member.isCurrent(System.currentTimeMillis()))
		{
			if (member.failed)
			{
				throw new DiscordException(UNREACHABLE);
			}
			return member;
		}

		HttpResponse<String> response;
		try
		{
			response = send(botRequest(configuration, "/guilds/" + configuration.getGuildId() + "/members/" + discordUserId, READ_TIMEOUT).GET());
		}
		catch (DiscordException e)
		{
			members.put(key, Member.failed());
			throw e;
		}
		if (response.statusCode() == 200)
		{
			JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
			JsonObject user = json.getAsJsonObject("user");
			String nick = string(json, "nick");
			String serverAvatar = string(json, "avatar");
			Set<String> roleIds = new HashSet<>();
			for (JsonElement role : json.getAsJsonArray("roles"))
			{
				roleIds.add(role.getAsString());
			}
			boolean pending = json.has("pending") && !json.get("pending").isJsonNull() && json.get("pending").getAsBoolean();
			member = new Member(true,
					nick != null && !nick.isEmpty() ? nick : displayName(user),
					DiscordUser.isAvatarHash(serverAvatar)
							? "https://cdn.discordapp.com/guilds/" + configuration.getGuildId() + "/users/" + discordUserId + "/avatars/" + serverAvatar + ".png?size=64"
							: DiscordUser.avatarUrl(discordUserId, string(user, "avatar")),
					roleColor(configuration, roleIds),
					roleIds,
					pending,
					timestampMillis(string(json, "communication_disabled_until")));
		}
		else if (response.statusCode() == 404)
		{
			member = new Member(false, null, null, null, Collections.emptySet(), false, 0);
		}
		else
		{
			log.warn("Discord: Laden des Mitglieds {} fehlgeschlagen ({}): {}", discordUserId, response.statusCode(), response.body());
			members.put(key, Member.failed());
			throw new DiscordException(UNREACHABLE);
		}
		members.put(key, member);
		return member;
	}

	private static long timestampMillis(String iso)
	{
		if (iso == null || iso.isEmpty())
		{
			return 0;
		}
		try
		{
			return OffsetDateTime.parse(iso).toInstant().toEpochMilli();
		}
		catch (DateTimeParseException e)
		{
			// Lieber als stummgeschaltet behandeln als einen Timeout zu uebergehen
			return Long.MAX_VALUE;
		}
	}

	/**
	 * Laedt Eigentuemer, Rollen und Emojis des Servers (zwischengespeichert).
	 * Schlaegt das fehl, ist <code>fehler</code> gesetzt und es wird eine Weile nicht erneut gefragt.
	 */
	private ServerData loadServer(DiscordConfiguration configuration)
	{
		ServerData loaded = server.get(configuration.getGuildId());
		if (loaded != null && loaded.isCurrent(System.currentTimeMillis()))
		{
			return loaded;
		}
		loaded = new ServerData();
		try
		{
			HttpResponse<String> response = send(botRequest(configuration, "/guilds/" + configuration.getGuildId(), READ_TIMEOUT).GET());
			if (response.statusCode() != 200)
			{
				log.warn("Discord: Laden des Servers fehlgeschlagen ({}): {}", response.statusCode(), response.body());
				loaded.error = UNREACHABLE;
			}
			else
			{
				JsonObject guild = JsonParser.parseString(response.body()).getAsJsonObject();
				loaded.ownerId = string(guild, "owner_id");
				for (JsonElement element : guild.getAsJsonArray("roles"))
				{
					JsonObject role = element.getAsJsonObject();
					loaded.rolePermissions.put(string(role, "id"), Long.parseLong(string(role, "permissions")));
					loaded.colorAndPosition.put(string(role, "id"), new int[]{role.get("color").getAsInt(), role.get("position").getAsInt()});
				}
				if (guild.has("emojis"))
				{
					for (JsonElement element : guild.getAsJsonArray("emojis"))
					{
						JsonObject emoji = element.getAsJsonObject();
						boolean animated = emoji.has("animated") && emoji.get("animated").getAsBoolean();
						loaded.emojis.put(string(emoji, "name"), "<" + (animated ? "a" : "") + ":" + string(emoji, "name") + ":" + string(emoji, "id") + ">");
					}
				}
			}
		}
		catch (DiscordException e)
		{
			loaded.error = e.getMessage();
		}
		server.put(configuration.getGuildId(), loaded);
		return loaded;
	}

	/**
	 * Laedt einen Kanal des Servers samt Rechten und Slowmode (zwischengespeichert).
	 * @throws DiscordException Falls Discord nicht antwortet oder es den Kanal nicht gibt
	 */
	private JsonObject loadChannel(DiscordConfiguration configuration, String channelId)
	{
		ChannelList list = channelLists.get(configuration.getGuildId());
		if (list == null || !list.isCurrent(System.currentTimeMillis()))
		{
			list = new ChannelList();
			try
			{
				HttpResponse<String> response = send(botRequest(configuration, "/guilds/" + configuration.getGuildId() + "/channels", READ_TIMEOUT).GET());
				if (response.statusCode() == 200)
				{
					list = channelList(JsonParser.parseString(response.body()).getAsJsonArray());
				}
				else
				{
					log.warn("Discord: Laden der Kanaele fehlgeschlagen ({}): {}", response.statusCode(), response.body());
					list.error = UNREACHABLE;
				}
			}
			catch (DiscordException e)
			{
				list.error = e.getMessage();
			}
			channelLists.put(configuration.getGuildId(), list);
		}
		if (list.error != null)
		{
			throw new DiscordException(list.error);
		}
		JsonObject channel = list.channels.get(channelId);
		if (channel == null)
		{
			throw new DiscordException("Diesen Kanal gibt es auf Discord nicht mehr.");
		}
		return channel;
	}

	private static ChannelList channelList(JsonArray channels)
	{
		ChannelList list = new ChannelList();
		for (JsonElement element : channels)
		{
			JsonObject channel = element.getAsJsonObject();
			list.channels.put(string(channel, "id"), channel);
		}
		return list;
	}

	/**
	 * Wie in Discord: Die Farbe der hoechsten Rolle, die eine Farbe hat. Ist der Server
	 * gerade nicht zu laden, bleiben die Namen eben ungefaerbt.
	 */
	private String roleColor(DiscordConfiguration configuration, Set<String> roleIds)
	{
		ServerData serverData = loadServer(configuration);
		int color = 0;
		int position = -1;
		for (String role : roleIds)
		{
			int[] data = serverData.colorAndPosition.get(role);
			if (data != null && data[0] != 0 && data[1] > position)
			{
				color = data[0];
				position = data[1];
			}
		}
		return position == -1 ? null : String.format("#%06x", color);
	}

	/**
	 * Gibt die Rollenfarben von Accounts zurueck. Wie beim Laden der Nachrichten werden je Aufruf
	 * nur wenige noch unbekannte Accounts nachgeladen, die uebrigen bleiben vorerst ungefaerbt.
	 * @param configuration Die Bot-Einstellungen
	 * @param discordUserIds Die IDs der Accounts
	 * @return ID -&gt; Farbe als <code>#rrggbb</code>, fuer Accounts ohne Farbe oder ohne Antwort von Discord fehlt der Eintrag
	 */
	public Map<String, String> getRoleColors(DiscordConfiguration configuration, Collection<String> discordUserIds)
	{
		Map<String, String> colors = new HashMap<>();
		int[] newMembers = {MAX_NEW_MEMBERS_PER_FETCH};
		for (String discordUserId : new HashSet<>(discordUserIds))
		{
			Member member = loadMemberOrNull(configuration, discordUserId, newMembers);
			if (member != null && member.color != null)
			{
				colors.put(discordUserId, member.color);
			}
		}
		return colors;
	}

	/**
	 * Wie {@link #loadMember}, wirft aber nicht: Beim Anzeigen von Nachrichten reicht notfalls der Accountname.
	 * @param newMembers Wie viele noch nicht zwischengespeicherte Accounts noch geladen werden duerfen. Wird heruntergezaehlt.
	 */
	private Member loadMemberOrNull(DiscordConfiguration configuration, String discordUserId, int[] newMembers)
	{
		Member existing = members.get(memberKey(configuration, discordUserId));
		boolean cached = existing != null && existing.isCurrent(System.currentTimeMillis());
		if (!cached)
		{
			if (newMembers[0] <= 0)
			{
				return null;
			}
			newMembers[0]--;
		}
		try
		{
			Member member = loadMember(configuration, discordUserId, false);
			return member.member ? member : null;
		}
		catch (DiscordException e)
		{
			return null;
		}
	}

	private static String memberKey(DiscordConfiguration configuration, String discordUserId)
	{
		return configuration.getGuildId() + ":" + discordUserId;
	}

	private HttpResponse<String> executeWebhook(DiscordConfiguration configuration, String channelId, String json)
	{
		Webhook webhook = getWebhook(configuration, channelId);
		return send(HttpRequest.newBuilder(URI.create(API + "/webhooks/" + webhook.id + "/" + webhook.token + "?wait=true"))
				.timeout(WRITE_TIMEOUT)
				.header("User-Agent", USER_AGENT)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json)));
	}

	private Webhook getWebhook(DiscordConfiguration configuration, String channelId)
	{
		Webhook webhook = webhooks.get(channelId);
		if (webhook != null)
		{
			return webhook;
		}

		// Je Kanal nur einmal anlegen, ohne andere Kanaele aufzuhalten
		synchronized (webhookLocks.computeIfAbsent(channelId, k -> new Object()))
		{
			webhook = webhooks.get(channelId);
			if (webhook != null)
			{
				return webhook;
			}

			HttpResponse<String> response = send(botRequest(configuration, "/channels/" + channelId + "/webhooks", READ_TIMEOUT).GET());
			if (response.statusCode() != 200)
			{
				log.warn("Discord: Laden der Webhooks von Kanal {} fehlgeschlagen ({}): {}", channelId, response.statusCode(), response.body());
				throw new DiscordException("Der Bot darf in diesem Kanal keine Webhooks verwalten.");
			}
			for (JsonElement element : JsonParser.parseString(response.body()).getAsJsonArray())
			{
				JsonObject existing = element.getAsJsonObject();
				if (WEBHOOK_NAME.equals(string(existing, "name")) && string(existing, "token") != null)
				{
					webhook = new Webhook(string(existing, "id"), string(existing, "token"));
					webhooks.put(channelId, webhook);
					return webhook;
				}
			}

			JsonObject body = new JsonObject();
			body.addProperty("name", WEBHOOK_NAME);
			response = send(botRequest(configuration, "/channels/" + channelId + "/webhooks", WRITE_TIMEOUT)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body))));
			if (response.statusCode() != 200)
			{
				log.warn("Discord: Anlegen des Webhooks in Kanal {} fehlgeschlagen ({}): {}", channelId, response.statusCode(), response.body());
				throw new DiscordException("Der Bot darf in diesem Kanal keine Webhooks anlegen.");
			}
			JsonObject created = JsonParser.parseString(response.body()).getAsJsonObject();
			webhook = new Webhook(string(created, "id"), string(created, "token"));
			webhooks.put(channelId, webhook);
			return webhook;
		}
	}

	private DiscordChatMessage toChatMessage(DiscordConfiguration configuration, JsonObject message, Map<String, String> channelNames, int[] newMembers)
	{
		JsonObject author = message.getAsJsonObject("author");
		String authorId = string(author, "id");
		boolean webhook = message.has("webhook_id") && !message.get("webhook_id").isJsonNull();

		// Nachrichten von Webhooks (auch aus dem Spiel) tragen ihren Namen schon selbst.
		// Bei echten Accounts zeigt Discord den Server-Alias an, das REST-API liefert ihn aber nicht mit.
		String authorName = displayName(author);
		String authorAvatar = DiscordUser.avatarUrl(authorId, string(author, "avatar"));
		String authorColor = null;
		if (!webhook)
		{
			Member member = loadMemberOrNull(configuration, authorId, newMembers);
			if (member != null)
			{
				authorName = member.name;
				authorAvatar = member.avatarUrl;
				authorColor = member.color;
			}
		}

		Map<String, String> mentionedUsers = new HashMap<>();
		if (message.has("mentions"))
		{
			for (JsonElement element : message.getAsJsonArray("mentions"))
			{
				JsonObject mentioned = element.getAsJsonObject();
				String nick = string(mentioned.getAsJsonObject("member"), "nick");
				mentionedUsers.put(string(mentioned, "id"), nick != null && !nick.isEmpty() ? nick : displayName(mentioned));
			}
		}

		String text = formatContent(string(message, "content"), mentionedUsers, channelNames);
		List<DiscordChatMessage.Attachment> attachments = new ArrayList<>();
		if (message.has("embeds"))
		{
			List<String> parts = new ArrayList<>();
			for (JsonElement element : message.getAsJsonArray("embeds"))
			{
				JsonObject embed = element.getAsJsonObject();
				String embedType = string(embed, "type");
				if ("image".equals(embedType))
				{
					// Eine gepostete Bild-URL: Discord zeigt dafuer das Bild an
					attachments.add(medium(DiscordChatMessage.TYPE_IMAGE, "", embed.getAsJsonObject("thumbnail")));
					continue;
				}
				if ("gifv".equals(embedType))
				{
					// Animierte GIFs (z.B. von Tenor) liefert Discord als Video
					attachments.add(medium(DiscordChatMessage.TYPE_VIDEO, "", embed.getAsJsonObject("video")));
					continue;
				}
				if (text.isEmpty())
				{
					for (String key : new String[]{"title", "description"})
					{
						String value = string(embed, key);
						if (value != null && !value.isEmpty())
						{
							parts.add(formatContent(value, mentionedUsers, channelNames));
						}
					}
				}
				if (embed.has("image"))
				{
					attachments.add(medium(DiscordChatMessage.TYPE_IMAGE, "", embed.getAsJsonObject("image")));
				}
			}
			if (text.isEmpty())
			{
				text = String.join("\n", parts);
			}
		}

		if (message.has("attachments"))
		{
			for (JsonElement element : message.getAsJsonArray("attachments"))
			{
				JsonObject attachment = element.getAsJsonObject();
				String contentType = string(attachment, "content_type");
				String type = DiscordChatMessage.TYPE_FILE;
				if (contentType != null && contentType.startsWith("image/"))
				{
					type = DiscordChatMessage.TYPE_IMAGE;
				}
				else if (contentType != null && contentType.startsWith("video/"))
				{
					type = DiscordChatMessage.TYPE_VIDEO;
				}
				attachments.add(medium(type, string(attachment, "filename"), attachment));
			}
		}
		attachments.removeIf(attachment -> attachment.url == null);

		return new DiscordChatMessage(
				string(message, "id"),
				authorName,
				authorAvatar,
				text,
				string(message, "timestamp"),
				webhook,
				attachments,
				authorColor);
	}

	/**
	 * Uebernimmt ein Medium nur, wenn es von Discord selbst ausgeliefert wird. So laedt der Browser
	 * des Spielers nie etwas von fremden Seiten, die in eine Nachricht eingebettet wurden.
	 */
	private static DiscordChatMessage.Attachment medium(String type, String name, JsonObject quelle)
	{
		if (quelle == null)
		{
			return new DiscordChatMessage.Attachment(type, name, null, 0, 0);
		}
		JsonElement width = quelle.get("width");
		JsonElement height = quelle.get("height");
		// proxy_url liefert Discord ueber das eigene CDN aus, auch fuer Bilder fremder Seiten
		String url = string(quelle, "proxy_url");
		if (!isDiscordCdn(url))
		{
			url = isDiscordCdn(string(quelle, "url")) ? string(quelle, "url") : null;
		}
		return new DiscordChatMessage.Attachment(type, name, url,
				width != null && !width.isJsonNull() ? width.getAsInt() : 0,
				height != null && !height.isJsonNull() ? height.getAsInt() : 0);
	}

	/**
	 * Prueft, ob eine URL per HTTPS auf das CDN von Discord zeigt
	 * (<code>cdn.discordapp.com</code>, <code>media.discordapp.net</code>, <code>images-ext-*.discordapp.net</code>).
	 * @param url Die URL
	 * @return <code>true</code>, falls dem so ist
	 */
	static boolean isDiscordCdn(String url)
	{
		if (url == null)
		{
			return false;
		}
		try
		{
			URI uri = new URI(url);
			String host = uri.getHost();
			return "https".equals(uri.getScheme())
					&& uri.getUserInfo() == null
					&& uri.getPort() == -1
					&& host != null
					&& (host.equals("cdn.discordapp.com") || host.endsWith(".discordapp.net"));
		}
		catch (URISyntaxException e)
		{
			return false;
		}
	}

	/**
	 * Ersetzt die Discord-Markup-Verweise (Erwaehnungen, Kanaele, Zeitstempel) durch lesbaren Text
	 * und Emoji-Kurzformen wie <code>:thumbsup:</code> durch das Emoji. Server-Emojis
	 * (<code>&lt;:name:id&gt;</code>) bleiben stehen, die zeigt der Chat als Bild an.
	 */
	static String formatContent(String content, Map<String, String> users, Map<String, String> channels)
	{
		if (content == null)
		{
			return "";
		}
		String text = replace(USER_MENTION, content, m -> "@" + users.getOrDefault(m.group(1), "unbekannt"));
		text = replace(ROLE_MENTION, text, m -> "@Rolle");
		text = replace(CHANNEL_MENTION, text, m -> "#" + channels.getOrDefault(m.group(1), "kanal"));
		text = replace(TIMESTAMP, text, m -> TIMESTAMP_FORMAT.format(Instant.ofEpochSecond(Long.parseLong(m.group(1)))));
		return replaceShortcodes(text, Collections.emptyMap());
	}

	/**
	 * Macht aus maskierten Links <code>[Text](URL)</code> sichtbare Links <code>Text (URL)</code>.
	 * Ueber den Webhook liesse sich sonst hinter harmlosem Text ein beliebiges Ziel verstecken.
	 * @param text Der Text
	 * @return Der Text ohne maskierte Links
	 */
	static String defuseLinks(String text)
	{
		String result = text;
		while (true)
		{
			// Jede Ersetzung entfernt eine eckige Klammer, die Schleife endet also
			String next = replace(MASKED_LINK, result, m -> m.group(1) + " (" + m.group(2) + ")");
			if (next.equals(result))
			{
				return result;
			}
			result = next;
		}
	}

	/**
	 * Bereitet den Text einer Nachricht aus dem Spiel fuer Discord vor. Der Discord-Client wandelt
	 * Kurzformen beim Tippen selbst um - fuer per Webhook gesendeten Text uebernimmt das DS:
	 * Namen von Server-Emojis werden zu <code>&lt;:name:id&gt;</code>, alle anderen bekannten Kurzformen zum Emoji.
	 * @param configuration Die Bot-Einstellungen
	 * @param text Der eingegebene Text
	 * @return Der Text fuer Discord
	 */
	public String prepareForSending(DiscordConfiguration configuration, String text)
	{
		if (text == null || !SHORTCODE.matcher(text).find())
		{
			return text;
		}
		// Ist der Server gerade nicht zu laden, werden eben nur die Unicode-Kurzformen umgewandelt
		return replaceShortcodes(text, loadServer(configuration).emojis);
	}

	/**
	 * Ersetzt <code>:name:</code> durch das Server-Emoji gleichen Namens oder das Unicode-Emoji.
	 * Unbekannte Namen bleiben stehen, ebenso der Name innerhalb von <code>&lt;:name:id&gt;</code>.
	 */
	static String replaceShortcodes(String text, Map<String, String> serverEmojis)
	{
		return replace(SHORTCODE, text, m -> {
			String name = m.group(1);
			String serverEmoji = serverEmojis.get(name);
			if (serverEmoji != null)
			{
				return serverEmoji;
			}
			Emoji emoji = EmojiManager.getForAlias(name);
			return emoji != null ? emoji.getUnicode() : m.group();
		});
	}

	/**
	 * Macht aus dem Spielernamen einen fuer Webhooks zulaessigen Namen.
	 * Discord lehnt Namen mit "discord" oder "clyde" ab und begrenzt die Laenge auf 80 Zeichen.
	 */
	static String webhookUsername(String name)
	{
		String result = name == null ? "" : name.replaceAll("(?i)discord|clyde", "").replaceAll("[@#:`]", "").trim();
		if (result.length() > 80)
		{
			result = result.substring(0, 80).trim();
		}
		if (result.isEmpty() || result.equalsIgnoreCase("everyone") || result.equalsIgnoreCase("here"))
		{
			result = "Spieler";
		}
		return result;
	}

	private static String replace(Pattern pattern, String text, Function<Matcher, String> replacement)
	{
		Matcher matcher = pattern.matcher(text);
		StringBuilder result = new StringBuilder();
		while (matcher.find())
		{
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.apply(matcher)));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private HttpRequest.Builder botRequest(DiscordConfiguration configuration, String path, Duration timeout)
	{
		return HttpRequest.newBuilder(URI.create(API + path))
				.timeout(timeout)
				.header("User-Agent", USER_AGENT)
				.header("Authorization", "Bot " + configuration.getBotToken().trim());
	}

	/**
	 * Stellt eine Anfrage an Discord. Waehrend Discord global bremst oder zu viele ungueltige
	 * Anfragen gestellt wurden, wird gar nicht erst gefragt.
	 */
	private HttpResponse<String> send(HttpRequest.Builder request)
	{
		if (System.currentTimeMillis() < globallyBlockedUntil)
		{
			throw new DiscordException("Discord nimmt gerade keine Anfragen an. Bitte versuche es später erneut.");
		}
		HttpResponse<String> response;
		try
		{
			response = http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			log.warn("Discord ist nicht erreichbar: {}", e.toString());
			throw new DiscordException(UNREACHABLE, e);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new DiscordException("Die Anfrage an Discord wurde abgebrochen.", e);
		}
		int status = response.statusCode();
		if (status == 401 || status == 403 || status == 429)
		{
			recordInvalid(response);
		}
		return response;
	}

	private void recordInvalid(HttpResponse<String> response)
	{
		long now = System.currentTimeMillis();
		if (response.statusCode() == 429 && (response.headers().firstValue("X-RateLimit-Global").map("true"::equalsIgnoreCase).orElse(false)
				|| response.headers().firstValue("X-RateLimit-Scope").map("global"::equals).orElse(false)))
		{
			globallyBlockedUntil = Math.max(globallyBlockedUntil, now + retryAfterMillis(response));
		}
		synchronized (invalidLock)
		{
			if (now - invalidSince >= INVALID_WINDOW_MILLIS)
			{
				invalidSince = now;
				invalidCount = 0;
			}
			invalidCount++;
			if (invalidCount == MAX_INVALID_REQUESTS)
			{
				log.error("Discord: {} ungueltige Anfragen in 10 Minuten - Anfragen werden bis zum Ende des Zeitraums eingestellt", invalidCount);
				globallyBlockedUntil = Math.max(globallyBlockedUntil, invalidSince + INVALID_WINDOW_MILLIS);
			}
		}
	}

	private static long retryAfterMillis(HttpResponse<String> response)
	{
		try
		{
			return (long) (Double.parseDouble(response.headers().firstValue("Retry-After").orElse("5")) * 1000);
		}
		catch (NumberFormatException e)
		{
			return 5000;
		}
	}

	private static String displayName(JsonObject user)
	{
		String globalName = string(user, "global_name");
		return globalName != null && !globalName.isEmpty() ? globalName : string(user, "username");
	}

	private static String string(JsonObject object, String key)
	{
		if (object == null)
		{
			return null;
		}
		JsonElement element = object.get(key);
		return element == null || element.isJsonNull() ? null : element.getAsString();
	}

	private static String encode(String value)
	{
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
