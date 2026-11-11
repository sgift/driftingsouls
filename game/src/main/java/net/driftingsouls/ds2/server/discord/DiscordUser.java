package net.driftingsouls.ds2.server.discord;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Ein ueber OAuth2 verknuepfter Discord-Account.
 */
public class DiscordUser
{
	private static final Pattern AVATAR_HASH = Pattern.compile("(a_)?[0-9a-f]{32}");
	private static final Pattern ID = Pattern.compile("\\d{1,25}");
	/**
	 * So lange gilt eine Verknuepfung, danach muss der Spieler sie bei Discord erneut bestaetigen.
	 * DS speichert keine Tokens und merkt sonst nicht, wenn jemand der Anwendung in Discord die Berechtigung entzieht.
	 */
	public static final int LINK_VALID_DAYS = 30;

	public final String id;
	public final String displayName;
	public final String avatar;

	public DiscordUser(String id, String displayName, String avatar)
	{
		this.id = id;
		this.displayName = displayName;
		this.avatar = avatar;
	}

	/**
	 * Gibt die URL des Avatars eines Discord-Accounts zurueck. Ohne eigenen Avatar
	 * wird einer der Standardavatare von Discord verwendet.
	 * @param userId Die ID des Accounts (oder Webhooks)
	 * @param avatar Der Hash des Avatars oder <code>null</code>
	 * @return Die URL
	 */
	public static String avatarUrl(String userId, String avatar)
	{
		if (isAvatarHash(avatar) && userId != null && ID.matcher(userId).matches())
		{
			return "https://cdn.discordapp.com/avatars/" + userId + "/" + avatar + ".png?size=64";
		}
		long index;
		try
		{
			index = (Long.parseUnsignedLong(userId) >>> 22) % 6;
		}
		catch (NumberFormatException e)
		{
			index = 0;
		}
		return "https://cdn.discordapp.com/embed/avatars/" + index + ".png";
	}

	/**
	 * Gibt zurueck, ob eine Verknuepfung noch gilt (siehe {@link #LINK_VALID_DAYS}).
	 * @param linkedAt Wann sie zuletzt bei Discord bestaetigt wurde (Millisekunden) oder <code>null</code>
	 * @return <code>true</code>, falls sie noch gilt
	 */
	public static boolean isLinkCurrent(Long linkedAt)
	{
		return linkedAt != null && System.currentTimeMillis() - linkedAt < TimeUnit.DAYS.toMillis(LINK_VALID_DAYS);
	}

	/**
	 * Prueft, ob ein Wert ein Avatar-Hash von Discord ist, damit nur saubere CDN-URLs entstehen.
	 * @param avatar Der Wert
	 * @return <code>true</code>, falls dem so ist
	 */
	static boolean isAvatarHash(String avatar)
	{
		return avatar != null && AVATAR_HASH.matcher(avatar).matches();
	}
}
