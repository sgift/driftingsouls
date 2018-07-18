package net.driftingsouls.ds2.server.map;

/**
 * Die Beschreibung einer fuer einen Sektor zu verwendenden Grafik.
 * Die Grafik besteht aus einem Pfad sowie einem fuer die Darstellung
 * anzuwendenden Offset.
 */
public class SectorImage
{
	private final String image;
	private final int x;
	private final int y;

	SectorImage(String image, int x, int y)
	{
		this.image = image;
		this.x = x;
		this.y = y;
	}

	/**
	 * Gibt den Pfad zur Grafik zurueck.
	 * @return Der Pfad
	 */
	public String getImage()
	{
		return image;
	}

	/**
	 * Gibt den x-Offset in Sektoren der Grafik fuer die Darstellung zurueck (vgl. CSS-Sprites).
	 * @return Der x-Offset in Sektoren
	 */
	public int getX()
	{
		return x;
	}

	/**
	 * Gibt den y-Offset in Sektoren der Grafik fuer die Darstellung zurueck (vgl. CSS-Sprites).
	 * @return Der y-Offset in Sektoren
	 */
	public int getY()
	{
		return y;
	}
}
