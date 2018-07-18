package net.driftingsouls.ds2.server.battles;

/**
 * Die Flags einer Schlacht.
 */
public enum BattleFlag
{
	/**
	 * Erste Runde.
	 */
	FIRSTROUND(1),
	/**
	 * Entfernt die zweite Reihe auf Seite 0.
	 */
	DROP_SECONDROW_0(2),
	/**
	 * Entfernt die zweite Reihe auf Seite 1.
	 */
	DROP_SECONDROW_1(4),
	/**
	 * Blockiert die zweite Reihe auf Seite 0.
	 */
	BLOCK_SECONDROW_0(8),
	/**
	 * Blockiert die zweite Reihe auf Seite 1.
	 */
	BLOCK_SECONDROW_1(16);

	private final int bit;

	BattleFlag(final int bit)
	{
		this.bit = bit;
	}

	/**
	 * Gibt den Bitwert dieses Flags zurueck.
	 * @return Der Bitwert
	 */
	public int getBit()
	{
		return this.bit;
	}
}
