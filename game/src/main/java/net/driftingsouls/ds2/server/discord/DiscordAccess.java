package net.driftingsouls.ds2.server.discord;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;

/**
 * Wer einen Discord-Kanal im Ingame-Chat lesen bzw. darin schreiben darf.
 * Admins duerfen alles ausser {@link #BLOCKED}.
 */
public enum DiscordAccess
{
	BLOCKED("gesperrt"),
	ALL("alle Spieler"),
	NPC("NPCs"),
	GAMEMASTER("Gamemaster"),
	ADMIN("Admins");

	private final String label;

	DiscordAccess(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}

	/**
	 * Prueft, ob der Spieler diese Zugriffsstufe erfuellt.
	 * @param user Der Spieler
	 * @return <code>true</code>, falls er Zugriff hat
	 */
	public boolean allowed(User user)
	{
		switch (this)
		{
			case ALL:
				return true;
			case NPC:
				return user.isNPC() || user.isAdmin();
			case GAMEMASTER:
				return user.hasFlag(UserFlag.GAMEMASTER) || user.isAdmin();
			case ADMIN:
				return user.isAdmin();
			default:
				return false;
		}
	}
}
