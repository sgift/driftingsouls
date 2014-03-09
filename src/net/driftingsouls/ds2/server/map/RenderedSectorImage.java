package net.driftingsouls.ds2.server.map;

/**
 * Beschreibung einer fuer einen Sektor zu verwendenden Grafik mit zusaetzlichen Renderinginformationen.
 */
public class RenderedSectorImage extends SectorImage
{
	public static final double[] DEFAULT_MASK = {1, 1, 1, 1, 1, 1, 1, 1, 1};
	private final double[] alphaMask;

	RenderedSectorImage(String image, int x, int y, double[] alphaMask)
	{
		super(image, x, y);
		this.alphaMask = alphaMask;
	}

	/**
	 * Die zu verwendenden Alphainformationen pro Ecke inkl. Zentrum (9 Eintraege).
	 * Aus den Eintraegen sind in Abhaengigkeit von der tatsaechlichen Bildgroesse eine
	 * entsprechende Alphawerte pro Pixel zu berechnen.
	 * @return Die Alphamaske
	 */
	public double[] getAlphaMask()
	{
		return alphaMask;
	}
}
