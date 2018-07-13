package net.driftingsouls.ds2.server.entities;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Optional;

public enum UserFlag
{
	/**
	 * Der Spieler taucht in der Spielerliste nicht auf.
	 */
	HIDE("hide"),
	/**
	 * Der Spieler kann auch in entmilitarisierte Systeme mit Militaerschiffen springen.
	 */
	MILITARY_JUMPS("miljumps"),
	/**
	 * Der Spieler hat Zugriff auf das NPC-Menue.
	 */
	ORDER_MENU("ordermenu"),
	/**
	 * Der Spieler kann auch NPC-Systeme sehen.
	 */
	VIEW_SYSTEMS("viewsystems"),
	/**
	 * Der Spieler kann sowohl Admin- als auch NPC-Systeme sehen.
	 */
	VIEW_ALL_SYSTEMS("viewallsystems"),
	/**
	 * Der Spieler kann Questschlachten leiten (und uebernehmen).
	 */
	QUEST_BATTLES("questbattles"),
	/**
	 * Der Spieler sieht den Debug-Output des Scriptparsers.
	 */
	SCRIPT_DEBUGGING("scriptdebug"),
	/**
	 * Der Spieler sieht zusaetzliche Anzeigen der TWs im Kampf.
	 */
	KS_DEBUG("ks_debug"),
	/**
	 * Dem Spieler koennen keine Schiffe uebergeben werden.
	 */
	NO_SHIP_CONSIGN("noshipconsign"),
	/**
	 * Der Spieler kann mit Schiffen jederzeit ins System 99 springen.
	 */
	NPC_ISLAND("npc_island"),
	/**
	 * Sprungpunkte sind fuer den Spieler immer passierbar.
	 */
	NO_JUMPNODE_BLOCK("nojnblock"),
	/**
	 * Der Spieler kann jedes Schiff, egal welcher Besitzer und wie Gross andocken.
	 */
	SUPER_DOCK("superdock"),
	/**
	 * Der Spieler ist ein Noob.
	 */
	NOOB("noob"),
	/**
	 * Die Schiffe des Spielers werden nicht beschaedigt, wenn sie zu wenig Crew haben.
	 */
	NO_HULL_DECAY("nohulldecay"),
	/**
	 * Die Schiffe des Spielers laufen nicht zur Ratte ueber, wenn zu wenig Geld auf dem Konto ist.
	 */
	NO_DESERTEUR("nodeserteur"),
	/**
	 * Kann alle Kaempfe uebernehmen, egal wer sie gerade kommandiert.
	 */
	KS_TAKE_BATTLES("cantakeallbattles"),
	/**
	 * Dieser Spieler setzt nie automatisch Kopfgeld aus.
	 */
	NO_AUTO_BOUNTY("noautobounty"),
	/**
	 * Dieser Spieler braucht keine Nahrung.
	 */
	NO_FOOD_CONSUMPTION("nofoodconsumption");

	private final String flag;

	UserFlag(String flag)
	{
		this.flag = flag;
	}

	/**
	 * Gibt die ID des internen Flags in der Datenbank zurueck.
	 * @return Das Flag
	 */
	public String getFlag()
	{
		return flag;
	}

	/**
	 * Gibt das Flag mit der angebenen Datenbank-ID zurueck.
	 * @param flag Die Datenbank-ID des Flags
	 * @return Das Flag
	 * @throws IllegalArgumentException Falls die ID unbekannt ist
	 * @see #getFlag()
	 */
	public static Optional<UserFlag> byFlag(@Nonnull String flag) throws IllegalArgumentException
	{
		for (UserFlag userFlag : values())
		{
			if( userFlag.getFlag().equals(flag) )
			{
				return Optional.of(userFlag);
			}
		}

		return Optional.empty();
	}

	/**
	 * Parst einen mit Leerzeichen separierten String von Datenbank-IDs von Flags und
	 * gibt die zugehoerigen Objekte als Set zurueck.
	 * @param flagString Der String
	 * @return Die Enum-Objekte
	 * @throws java.lang.IllegalArgumentException Falls die ID eines Flags unbekannt ist
	 */
	public static @Nonnull EnumSet<UserFlag> parseFlags(@Nonnull String flagString) throws IllegalArgumentException
	{
		EnumSet<UserFlag> flagSet = EnumSet.noneOf(UserFlag.class);

		String[] flagArray = StringUtils.split(flagString, ' ');
		for (String aflag : flagArray)
		{
			if (aflag.trim().isEmpty())
			{
				continue;
			}

			UserFlag.byFlag(aflag).ifPresent(flagSet::add);
		}
		return flagSet;
	}
}
