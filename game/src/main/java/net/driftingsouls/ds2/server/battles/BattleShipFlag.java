package net.driftingsouls.ds2.server.battles;

/**
 * Die Flags die ein einzelnes Schiff in einer Schlacht haben kann.
 */
public enum BattleShipFlag
{
	/**
	 * Schiff wird am Rundenende geloescht.
	 */
	DESTROYED(1),
	/**
	 * Schiff verlaesst am Rundenende die Schlacht.
	 */
	FLUCHT(2),
	/**
	 * Schiff ist getroffen (Wertabgleich ships und battles_ships!).
	 */
	HIT(4),
	/**
	 * Das Schiff hat gefeuernt.
	 */
	SHOT(8),
	/**
	 * Schiff tritt der Schlacht bei.
	 */
	JOIN(16),
	/**
	 * Schiff befindet sich in der zweiten Reihe.
	 */
	SECONDROW(32),
	/**
	 * Schiff flieht naechste Runde.
	 */
	FLUCHTNEXT(64),
	/**
	 *  Schiff hat bereits eine zweite Reihe aktion in der Runde ausgefuehrt.
	 */
	SECONDROW_BLOCKED(128),
	/**
	 * Waffen sind bis zum Rundenende blockiert.
	 */
	BLOCK_WEAPONS(256),
	/**
	 * Waffen sind bis zum Kampfende blockiert.
	 */
	DISABLE_WEAPONS(512);

	private final int bit;

	BattleShipFlag(int bit)
	{
		this.bit = bit;
	}

	/**
	 * Gibt den Bitwert des Flags zurueck.
	 * @return Der Bitwert
	 */
	public int getBit()
	{
		return bit;
	}
}
