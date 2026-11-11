package net.driftingsouls.ds2.server.discord;

/**
 * Fehler bei der Kommunikation mit Discord. Die Nachricht ist fuer
 * die Anzeige beim Spieler gedacht.
 */
public class DiscordException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public DiscordException(String message)
	{
		super(message);
	}

	public DiscordException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
