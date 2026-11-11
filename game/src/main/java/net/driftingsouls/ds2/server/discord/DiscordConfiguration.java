package net.driftingsouls.ds2.server.discord;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Die Einstellungen des Discord-Bots fuer den Ingame-Chat. Es gibt genau einen Datensatz,
 * der im Adminbereich unter "Discord-Bot" bearbeitet wird.
 * <p>
 * Bot-Token und Client-Secret lassen sich statt in der Datenbank auch in den Umgebungsvariablen
 * {@link #ENV_BOT_TOKEN} und {@link #ENV_CLIENT_SECRET} hinterlegen. Gesetzte Umgebungsvariablen
 * haben Vorrang, so landen die Geheimnisse nicht in Datenbank-Dumps.
 */
@Entity
@Table(name = "discord_configuration")
public class DiscordConfiguration
{
	private static final int ID = 1;
	public static final String ENV_BOT_TOKEN = "DS_DISCORD_BOT_TOKEN";
	public static final String ENV_CLIENT_SECRET = "DS_DISCORD_CLIENT_SECRET";
	/**
	 * Nur echte Einladungen auf Discord, damit der Link weder auf fremde Seiten noch auf <code>javascript:</code> zeigt.
	 */
	private static final Pattern INVITE = Pattern.compile("https://(discord\\.gg|discord\\.com/invite|discordapp\\.com/invite)/[A-Za-z0-9-]{2,32}");

	/**
	 * Zum Ersetzen in Tests.
	 */
	static Function<String, String> environment = System::getenv;

	@Id
	private int id = ID;
	@Column(nullable = false)
	private boolean active;
	@Column(name = "bot_token")
	private String botToken;
	@Column(name = "client_id")
	private String clientId;
	@Column(name = "client_secret")
	private String clientSecret;
	@Column(name = "redirect_uri")
	private String redirectUri;
	@Column(name = "guild_id")
	private String guildId;
	@Column(name = "invite_url")
	private String inviteUrl;
	@Version
	private int version;

	/**
	 * Laedt die Einstellungen. Fehlt der Datensatz, wird ein nicht gespeicherter, inaktiver geliefert.
	 * @param em Der EntityManager
	 * @return Die Einstellungen
	 */
	public static DiscordConfiguration load(EntityManager em)
	{
		DiscordConfiguration configuration = em.find(DiscordConfiguration.class, ID);
		return configuration != null ? configuration : new DiscordConfiguration();
	}

	/**
	 * Gibt zurueck, ob der Chat benutzt werden kann (aktiv, Bot-Token und Server gesetzt).
	 * @return <code>true</code>, falls dem so ist
	 */
	public boolean isChatConfigured()
	{
		return active && isSet(getBotToken()) && isSet(guildId);
	}

	/**
	 * Gibt zurueck, ob Spieler ihren Account per OAuth2 verknuepfen koennen.
	 * @return <code>true</code>, falls zusaetzlich Client-ID, Client-Secret und Redirect-URI gesetzt sind
	 */
	public boolean isLoginConfigured()
	{
		return isChatConfigured() && isSet(clientId) && isSet(getClientSecret()) && isSet(redirectUri);
	}

	/**
	 * Schlaegt eine Redirect-URI fuer den OAuth2-Login vor, abgeleitet aus der URL der aktuellen Anfrage.
	 * Verwendet wird sie nie: Die URL der Anfrage haengt vom Host-Header ab, den der Aufrufer bestimmt.
	 * @param requestUrl Die URL der aktuellen Anfrage (z.B. <code>http://localhost:8080/ds</code>)
	 * @return Der Vorschlag
	 */
	public static String suggestRedirectUri(String requestUrl)
	{
		String base = requestUrl;
		int query = base.indexOf('?');
		if (query != -1)
		{
			base = base.substring(0, query);
		}
		return base.substring(0, base.lastIndexOf('/') + 1) + "ds?module=options&action=discordCallback";
	}

	/**
	 * Prueft, ob ein Link eine Einladung auf einen Discord-Server ist.
	 * @param url Der Link
	 * @return <code>true</code>, falls dem so ist
	 */
	public static boolean isValidInvite(String url)
	{
		return url != null && INVITE.matcher(url).matches();
	}

	public static boolean isSet(String value)
	{
		return value != null && !value.trim().isEmpty();
	}

	public boolean isActive()
	{
		return active;
	}

	public void setActive(boolean active)
	{
		this.active = active;
	}

	/**
	 * Gibt das Bot-Token zurueck, vorrangig aus der Umgebungsvariable {@link #ENV_BOT_TOKEN}.
	 * @return Das Token
	 */
	public String getBotToken()
	{
		return isBotTokenFromEnvironment() ? environment.apply(ENV_BOT_TOKEN).trim() : botToken;
	}

	public boolean isBotTokenFromEnvironment()
	{
		return isSet(environment.apply(ENV_BOT_TOKEN));
	}

	/**
	 * Gibt zurueck, ob in der Datenbank ein Bot-Token steht, auch wenn die Umgebungsvariable Vorrang hat.
	 * @return <code>true</code>, falls dem so ist
	 */
	public boolean isBotTokenStored()
	{
		return isSet(botToken);
	}

	public void setBotToken(String botToken)
	{
		this.botToken = botToken;
	}

	public String getClientId()
	{
		return clientId;
	}

	public void setClientId(String clientId)
	{
		this.clientId = clientId;
	}

	/**
	 * Gibt das Client-Secret zurueck, vorrangig aus der Umgebungsvariable {@link #ENV_CLIENT_SECRET}.
	 * @return Das Secret
	 */
	public String getClientSecret()
	{
		return isClientSecretFromEnvironment() ? environment.apply(ENV_CLIENT_SECRET).trim() : clientSecret;
	}

	public boolean isClientSecretFromEnvironment()
	{
		return isSet(environment.apply(ENV_CLIENT_SECRET));
	}

	/**
	 * Gibt zurueck, ob in der Datenbank ein Client-Secret steht, auch wenn die Umgebungsvariable Vorrang hat.
	 * @return <code>true</code>, falls dem so ist
	 */
	public boolean isClientSecretStored()
	{
		return isSet(clientSecret);
	}

	public void setClientSecret(String clientSecret)
	{
		this.clientSecret = clientSecret;
	}

	/**
	 * Gibt die im Developer Portal eingetragene Redirect-URI fuer den OAuth2-Login zurueck.
	 * @return Die Redirect-URI oder <code>null</code>
	 */
	public String getRedirectUri()
	{
		return redirectUri != null ? redirectUri.trim() : null;
	}

	public void setRedirectUri(String redirectUri)
	{
		this.redirectUri = redirectUri;
	}

	public String getGuildId()
	{
		return guildId;
	}

	public void setGuildId(String guildId)
	{
		this.guildId = guildId;
	}

	public String getInviteUrl()
	{
		return inviteUrl;
	}

	/**
	 * Gibt den Einladungslink zurueck, aber nur, falls er wirklich auf eine Discord-Einladung zeigt.
	 * Fuer jede Ausgabe an Spieler.
	 * @return Der Link oder <code>null</code>
	 */
	public String getSafeInviteUrl()
	{
		return isValidInvite(inviteUrl) ? inviteUrl : null;
	}

	public void setInviteUrl(String inviteUrl)
	{
		this.inviteUrl = inviteUrl;
	}
}
