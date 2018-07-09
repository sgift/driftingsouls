package net.driftingsouls.ds2.server.map;

import com.github.jaiimageio.impl.common.PaletteBuilder;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verwaltung fuer Kacheln der Sternenkarte.
 */
public class TileCache
{
	private static final Logger LOG = LogManager.getLogger(TileCache.class);
	private static final int TILE_SIZE = 20;
	private static final int SECTOR_IMAGE_SIZE = 25;

	/**
	 * Gibt eine Instanz des TileCaches fuer das angegebene Sternensystem zurueck.
	 * @param system Das System
	 * @return Die Instanz
	 */
	public static TileCache forSystem(StarSystem system)
	{
		return new TileCache(system);
	}

	/**
	 * Gibt eine Instanz des TileCaches fuer das angegebene Sternensystem zurueck.
	 * @param system Die ID des Systems
	 * @return Die Instanz
	 */
	public static TileCache forSystem(int system)
	{
		StarSystem sys = (StarSystem)ContextMap.getContext().getDB().get(StarSystem.class, system);
		return new TileCache(sys);
	}

	private static final class CustomPaletteBuilder extends PaletteBuilder
	{

		protected CustomPaletteBuilder(RenderedImage src, int size)
		{
			super(src, size);
		}

		public static RenderedImage createIndexedImage(RenderedImage img, int colors)
		{
			CustomPaletteBuilder pb = new CustomPaletteBuilder(img, colors);
			pb.buildPalette();
			return pb.getIndexedImage();
		}
	}

	private Map<String,BufferedImage> imageCache = new HashMap<>();
	private StarSystem system;

	private TileCache(StarSystem system)
	{
		this.system = system;
	}

	private BufferedImage loadImage(String name) throws IOException
	{
		if( imageCache.containsKey(name) )
		{
			return imageCache.get(name);
		}
		File path = new File(Configuration.getAbsolutePath() + name);
		if( !path.canRead() )
		{
			throw new FileNotFoundException(path.getAbsolutePath());
		}
		BufferedImage sectorImage = ImageIO.read(path);

		imageCache.put(name, sectorImage);
		return sectorImage;
	}

	private void createTile(File tileCacheFile, int tileX, int tileY) throws IOException
	{
		PublicStarmap content = new PublicStarmap(this.system, null);

		BufferedImage img = new BufferedImage(TILE_SIZE*25,TILE_SIZE*25,BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		try
		{
			for(int y = 0; y < TILE_SIZE; y++)
			{
				for(int x = 0; x < TILE_SIZE; x++)
				{
					Location position = new Location(this.system.getID(), tileX*TILE_SIZE+x+1, tileY*TILE_SIZE+y+1);
					List<RenderedSectorImage> sectorImageName = content.getSectorBaseImage(position);

					for (RenderedSectorImage renderOp : sectorImageName)
					{
						BufferedImage sectorImage = loadImage(renderOp.getImage());

						sectorImage = enhanceImage(sectorImage, renderOp);

						g.drawImage(sectorImage,
								   x*SECTOR_IMAGE_SIZE,y*SECTOR_IMAGE_SIZE,
								   (x+1)*SECTOR_IMAGE_SIZE, (y+1)*SECTOR_IMAGE_SIZE,

								   -renderOp.getX()*25,
								   -renderOp.getY()*25,
								   -renderOp.getX()*25+25,
								   -renderOp.getY()*25+25,
								   null);
					}
				}
			}
		}
		finally
		{
			g.dispose();
		}

		try (OutputStream outputStream = new FileOutputStream(tileCacheFile))
		{
			ImageIO.write(CustomPaletteBuilder.createIndexedImage(img, 192), "png", outputStream);

			outputStream.flush();
		}
	}

	private BufferedImage enhanceImage(BufferedImage image, RenderedSectorImage sectorImage)
	{
		if( sectorImage.getAlphaMask()[0] == 1 &&
			sectorImage.getAlphaMask()[1] == 1 &&
			sectorImage.getAlphaMask()[2] == 1 &&
			sectorImage.getAlphaMask()[3] == 1 &&
			sectorImage.getAlphaMask()[4] == 1 &&
			sectorImage.getAlphaMask()[5] == 1 &&
			sectorImage.getAlphaMask()[6] == 1 &&
			sectorImage.getAlphaMask()[7] == 1 &&
			sectorImage.getAlphaMask()[8] == 1 ) {
			return image;
		}

		BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

		final int width = image.getWidth();
		int[] imgData = new int[width];

		for (int y = 0; y < image.getHeight(); y++) {
			// fetch a line of data from each image
			image.getRGB(0, y, width, 1, imgData, 0, 1);

			// apply the mask
			for (int x = 0; x < width; x++) {
				int color = imgData[x] & 0x00FFFFFF; // mask away any alpha present
				double maskFactor = 0d;
				for( int i=0; i < 9; i++ ) {
					maskFactor += distanceFactor(i, x, y, width, image.getHeight())* sectorImage.getAlphaMask()[i];
				}
				int maskColor = ((int)(Math.min(maskFactor,1)*255)) << 24;
				color |= maskColor;
				imgData[x] = color;
			}
			// replace the data
			newImage.setRGB(0, y, width, 1, imgData, 0, 1);
		}
		return newImage;
	}

	private double distanceFactor(int edge, int x, int y, int w, int h)
	{
		double[] edgePos = {((edge%3)/2d)*w, ((edge/3)/2d)*h};

		double distance = Math.sqrt(Math.pow(edgePos[0]-x, 2)+Math.pow(edgePos[1]-y, 2));
		double maxDistance = Math.sqrt(Math.pow(w, 2)+Math.pow(h, 2))/(edge%2==1 ? 3d : 2.5d);

		return Math.max(1-distance/maxDistance, 0);
	}

	/**
	 * Liefert die Kachel mit den angegebenen Kachelkoordinaten zurueck.
	 * @param tileX Die X-Kachel
	 * @param tileY Die Y-Kachel
	 * @return Die Datei mit den Kacheldaten
	 * @throws IOException Bei IO-Fehlern
	 */
	public File getTile(int tileX, int tileY) throws IOException
	{
		if( this.system == null )
		{
			throw new IOException("Es kann keine Kachel für ein unbekanntes Sternensystem erzeugt werden");
		}
		final File cacheDir = new File(getTilePath());
		if( !cacheDir.isDirectory() ) {
			cacheDir.mkdir();
		}

		File tileCacheFile = new File(getTilePath()+this.system.getID()+"_"+tileX+"_"+tileY+".png");
		if( !tileCacheFile.isFile() ) {
			createTile(tileCacheFile, tileX, tileY);
		}

		return tileCacheFile;
	}

	private String getTilePath()
	{
		return Configuration.getAbsolutePath()+"data/starmap/_tilecache/";
	}

	/**
	 * Entfernt alle erzeugten Kacheln aus dem Cache.
	 */
	public void resetCache()
	{
		if( this.system == null )
		{
			return;
		}

		final File cacheDir = new File(getTilePath());
		if( !cacheDir.isDirectory() ) {
			LOG.error("Konnte TileCache-Verzeichnis nicht finden: "+cacheDir.getAbsolutePath());
			return;
		}

		File[] files = cacheDir.listFiles(pathname -> pathname.isFile() && pathname.getName().startsWith(TileCache.this.system.getID() + "_"));

		LOG.info("System "+this.system.getID()+": Loesche "+files.length+" Tiles aus dem Cache");

		for( File file : files )
		{
			if( !file.delete() ) {
				LOG.warn("Konnte Tile "+file.getName()+" nicht loeschen");
			}
		}
	}
}
