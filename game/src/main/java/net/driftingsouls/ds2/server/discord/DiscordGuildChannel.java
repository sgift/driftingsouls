package net.driftingsouls.ds2.server.discord;

/**
 * Ein Textkanal, wie ihn Discord beim Synchronisieren liefert.
 */
public class DiscordGuildChannel
{
	public final String id;
	public final String name;
	public final String category;
	/**
	 * Sortierschluessel ueber Kategorie und Position innerhalb der Kategorie.
	 */
	public final int position;
	/**
	 * Fehlende Rechte des Bots in diesem Kanal oder <code>null</code>, falls alles passt.
	 */
	public final String notice;
	/**
	 * <code>true</code>, falls @everyone den Kanal sehen darf.
	 */
	public final boolean visibleToEveryone;
	/**
	 * Wer den Kanal mitlesen kann, siehe {@link DiscordService#permissionSignature}.
	 */
	public final String signature;

	public DiscordGuildChannel(String id, String name, String category, int position, String notice, boolean visibleToEveryone, String signature)
	{
		this.id = id;
		this.name = name;
		this.category = category;
		this.position = position;
		this.notice = notice;
		this.visibleToEveryone = visibleToEveryone;
		this.signature = signature;
	}
}
