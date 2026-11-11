package net.driftingsouls.ds2.server.discord;

import net.driftingsouls.ds2.server.entities.User;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ein mit dem Discord-Server synchronisierter Kanal und wer ihn im Ingame-Chat nutzen darf.
 * Neu synchronisierte Kanaele sind gesperrt, bis ein Admin sie freigibt.
 * <p>
 * Die Freigabe gilt fuer die Sichtbarkeit, die der Kanal in Discord beim Freigeben hatte
 * (siehe {@link DiscordService#permissionSignature}). Aendert sie sich dort, etwa weil der Kanal
 * privat wird, ist er im Spiel gesperrt, bis ein Admin die Rechte erneut speichert.
 */
@Entity
@Table(name = "discord_channel")
public class DiscordChannel
{
	@Id
	@GeneratedValue
	private int id;
	@Column(name = "guild_id", nullable = false)
	private String guildId;
	@Column(name = "channel_id", nullable = false)
	private String channelId;
	@Column(nullable = false)
	private String name;
	private String category;
	@Column(nullable = false)
	private int position;
	@Enumerated(EnumType.STRING)
	@Column(name = "read_access", nullable = false)
	private DiscordAccess readAccess = DiscordAccess.BLOCKED;
	@Enumerated(EnumType.STRING)
	@Column(name = "write_access", nullable = false)
	private DiscordAccess writeAccess = DiscordAccess.BLOCKED;
	@Column(name = "default_channel", nullable = false)
	private boolean defaultChannel;
	@Column(name = "bot_notice")
	private String botNotice;
	@Column(name = "visible_to_everyone", nullable = false)
	private boolean visibleToEveryone;
	@Column(name = "discord_signature")
	private String discordSignature;
	@Column(name = "approved_signature")
	private String approvedSignature;
	@Version
	private int version;

	protected DiscordChannel()
	{
		// JPA
	}

	public DiscordChannel(String guildId, String channelId)
	{
		this.guildId = guildId;
		this.channelId = channelId;
	}

	/**
	 * Gibt alle Kanaele eines Discord-Servers in der Reihenfolge von Discord zurueck.
	 * @param em Der EntityManager
	 * @param guildId Die ID des Discord-Servers
	 * @return Die Kanaele
	 */
	public static List<DiscordChannel> all(EntityManager em, String guildId)
	{
		return em.createQuery("from DiscordChannel where guildId=:guild order by position, name", DiscordChannel.class)
				.setParameter("guild", guildId != null ? guildId : "")
				.getResultList();
	}

	/**
	 * Gibt die Kanaele zurueck, die der Spieler im Ingame-Chat lesen darf. Ist der Chat nicht
	 * eingerichtet, ist die Liste leer. Kanaele, deren Sichtbarkeit in Discord sich seit der
	 * Freigabe geaendert hat, fehlen. Alle Zugriffe auf Kanaele muessen hierueber laufen.
	 * @param em Der EntityManager
	 * @param configuration Die Bot-Einstellungen
	 * @param user Der Spieler
	 * @return Die lesbaren Kanaele
	 */
	public static List<DiscordChannel> readable(EntityManager em, DiscordConfiguration configuration, User user)
	{
		if (!configuration.isChatConfigured())
		{
			return List.of();
		}
		return all(em, configuration.getGuildId()).stream()
				.filter(DiscordChannel::isApprovalCurrent)
				.filter(channel -> channel.mayRead(user))
				.collect(Collectors.toList());
	}

	/**
	 * Gibt zurueck, ob die Freigabe fuer die Sichtbarkeit gilt, die der Kanal beim letzten Synchronisieren in Discord hatte.
	 * @return <code>true</code>, falls dem so ist
	 */
	public boolean isApprovalCurrent()
	{
		return approvedSignature != null && approvedSignature.equals(discordSignature);
	}

	/**
	 * Gibt die Freigabe fuer die aktuelle Sichtbarkeit in Discord, z.B. wenn ein Admin die Kanalrechte speichert.
	 */
	public void approve()
	{
		approvedSignature = discordSignature;
	}

	public boolean mayRead(User user)
	{
		return readAccess.allowed(user);
	}

	/**
	 * Schreiben setzt immer auch das Leserecht voraus.
	 * @param user Der Spieler
	 * @return <code>true</code>, falls der Spieler in diesen Kanal schreiben darf
	 */
	public boolean mayWrite(User user)
	{
		return mayRead(user) && writeAccess.allowed(user);
	}

	public int getId()
	{
		return id;
	}

	public String getGuildId()
	{
		return guildId;
	}

	public String getChannelId()
	{
		return channelId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getCategory()
	{
		return category;
	}

	public void setCategory(String category)
	{
		this.category = category;
	}

	public int getPosition()
	{
		return position;
	}

	public void setPosition(int position)
	{
		this.position = position;
	}

	public DiscordAccess getReadAccess()
	{
		return readAccess;
	}

	public void setReadAccess(DiscordAccess readAccess)
	{
		this.readAccess = readAccess;
	}

	public DiscordAccess getWriteAccess()
	{
		return writeAccess;
	}

	public void setWriteAccess(DiscordAccess writeAccess)
	{
		this.writeAccess = writeAccess;
	}

	public boolean isDefaultChannel()
	{
		return defaultChannel;
	}

	public void setDefaultChannel(boolean defaultChannel)
	{
		this.defaultChannel = defaultChannel;
	}

	/**
	 * Gibt die beim letzten Synchronisieren festgestellten fehlenden Rechte des Bots zurueck.
	 * @return Der Hinweis oder <code>null</code>, falls alles passt
	 */
	public String getBotNotice()
	{
		return botNotice;
	}

	public void setBotNotice(String botNotice)
	{
		this.botNotice = botNotice;
	}

	/**
	 * Gibt zurueck, ob @everyone den Kanal in Discord sehen darf.
	 * @return <code>true</code>, falls dem so ist
	 */
	public boolean isVisibleToEveryone()
	{
		return visibleToEveryone;
	}

	public void setVisibleToEveryone(boolean visibleToEveryone)
	{
		this.visibleToEveryone = visibleToEveryone;
	}

	/**
	 * Gibt die Signatur der Sichtbarkeit beim letzten Synchronisieren zurueck.
	 * @return Die Signatur oder <code>null</code>
	 */
	public String getDiscordSignature()
	{
		return discordSignature;
	}

	public void setDiscordSignature(String discordSignature)
	{
		this.discordSignature = discordSignature;
	}

	/**
	 * Gibt die Signatur der Sichtbarkeit zurueck, fuer die die Kanalrechte freigegeben wurden.
	 * @return Die Signatur oder <code>null</code>, falls nie freigegeben
	 */
	public String getApprovedSignature()
	{
		return approvedSignature;
	}
}
