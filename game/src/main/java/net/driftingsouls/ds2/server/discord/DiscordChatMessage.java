package net.driftingsouls.ds2.server.discord;

import java.util.List;

/**
 * Eine fuer die Anzeige im Ingame-Chat aufbereitete Discord-Nachricht.
 * Der Text ist reiner Text (kein HTML) und muss beim Anzeigen escaped werden.
 */
public class DiscordChatMessage
{
	public static final String TYPE_IMAGE = "image";
	public static final String TYPE_VIDEO = "video";
	public static final String TYPE_FILE = "file";

	/**
	 * Ein Anhang oder ein von Discord eingebettetes Bild bzw. Video.
	 */
	public static class Attachment
	{
		/**
		 * {@link #TYPE_IMAGE} und {@link #TYPE_VIDEO} werden direkt angezeigt, {@link #TYPE_FILE} als Link.
		 */
		public final String type;
		public final String name;
		public final String url;
		/**
		 * Die Abmessungen in Pixeln, falls Discord sie kennt, sonst 0.
		 */
		public final int width;
		public final int height;

		public Attachment(String type, String name, String url, int width, int height)
		{
			this.type = type;
			this.name = name;
			this.url = url;
			this.width = width;
			this.height = height;
		}
	}

	public final String id;
	public final String author;
	public final String avatarUrl;
	public final String text;
	/**
	 * Der Zeitpunkt im ISO-8601-Format.
	 */
	public final String timestamp;
	/**
	 * <code>true</code>, falls die Nachricht ueber einen Webhook kam - aus dem Spiel oder von einer anderen Integration.
	 */
	public final boolean webhook;
	/**
	 * <code>true</code>, falls die Nachricht nachweislich aus dem Spiel heraus gesendet wurde
	 * (siehe {@link DiscordSentMessage}). Das setzt erst der Controller.
	 */
	public final boolean ingame;
	public final List<Attachment> attachments;
	/**
	 * Die Rollenfarbe des Verfassers als <code>#rrggbb</code> oder <code>null</code>. Bei Nachrichten
	 * aus dem Spiel setzt sie erst der Controller, der den Verfasser kennt.
	 */
	public final String color;

	public DiscordChatMessage(String id, String author, String avatarUrl, String text, String timestamp, boolean webhook, List<Attachment> attachments, String color)
	{
		this(id, author, avatarUrl, text, timestamp, webhook, false, attachments, color);
	}

	private DiscordChatMessage(String id, String author, String avatarUrl, String text, String timestamp, boolean webhook, boolean ingame, List<Attachment> attachments, String color)
	{
		this.id = id;
		this.author = author;
		this.avatarUrl = avatarUrl;
		this.text = text;
		this.timestamp = timestamp;
		this.webhook = webhook;
		this.ingame = ingame;
		this.attachments = attachments;
		this.color = color;
	}

	/**
	 * Gibt eine Kopie dieser Nachricht zurueck, die als aus dem Spiel gesendet markiert ist.
	 * Die zwischengespeicherte Nachricht selbst bleibt unveraendert.
	 * @param color Die Rollenfarbe des Verfassers oder <code>null</code>
	 * @return Die Kopie
	 */
	public DiscordChatMessage asIngame(String color)
	{
		return new DiscordChatMessage(id, author, avatarUrl, text, timestamp, webhook, true, attachments, color);
	}
}
