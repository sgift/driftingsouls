package net.driftingsouls.ds2.server.ships;

/**
 * Die verschiedenen Alarmstufen eines Schiffes.
 *
 * @author Sebastian Gift
 */
public enum Alarmstufe
{
	/**
	 * Keine Reaktion.
	 */
	GREEN("grün", 0, 1.0, 0.0),
	/**
	 * Reaktion bei feindlichen Schiffen.
	 */
	YELLOW("gelb", 1, 1.1, 0.5),
	/**
	 * Reaktion bei Schiffen, die nicht freundlich eingestellt sind.
	 */
	RED("rot", 2, 1.1, 0.75);

	private final String name;
	private final double scaleFactor;
	private final double energyCostFactor;
	private final int code;

	Alarmstufe(String name, int code, double scaleFactor, double energyCostFactor)
	{
		this.name = name;
		this.code = code;
		this.scaleFactor = scaleFactor;
		this.energyCostFactor = energyCostFactor;
	}

	/**
	 * Gibt den Anzeigenamen der Alarmstufe zurueck.
	 * @return Der Anzeigename
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * @return Der zugehoerige Code als int.
	 */
	public int getCode()
	{
		return this.code;
	}


	/**
	 * Gibt den Skalierungsfaktor fuer aktiven Alarm zurueck.
	 *
	 * @return Skalierungsfaktor, je nach Alarmstufe.
	 */
	public double getAlertScaleFactor()
	{
		return this.scaleFactor;
	}

	/**
	 * Gibt den Faktor fuer die Energiekosten bei dieser Alarmstufe zurueck.
	 * @return Der Faktor
	 */
	public double getEnergiekostenFaktor()
	{
		return energyCostFactor;
	}
}
