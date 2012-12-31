package net.driftingsouls.ds2.server.map;

import com.sun.imageio.plugins.common.PaletteBuilder;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.ContextMap;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Verwaltung fuer Kacheln der Sternenkarte.
 */
public class TileCache
{
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

	private StarSystem system;

	private TileCache(StarSystem system)
	{
		this.system = system;
	}

	private void createTile(File tileCacheFile, int tileX, int tileY) throws IOException
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();

		PublicStarmap content = new PublicStarmap(db, this.system);

		BufferedImage img = new BufferedImage(TILE_SIZE*25,TILE_SIZE*25,BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		try
		{
			for(int y = 0; y < TILE_SIZE; y++)
			{
				for(int x = 0; x < TILE_SIZE; x++)
				{
					Location position = new Location(this.system.getID(), tileX*TILE_SIZE+x+1, tileY*TILE_SIZE+y+1);
					String sectorImageName = content.getSectorBaseImage(position);

					BufferedImage sectorImage = ImageIO.read(new File(Configuration.getSetting("ABSOLUTE_PATH") + "data/starmap/" + sectorImageName));
					g.drawImage(sectorImage,
							x*SECTOR_IMAGE_SIZE, y*SECTOR_IMAGE_SIZE,
							SECTOR_IMAGE_SIZE, SECTOR_IMAGE_SIZE,
							null);
				}
			}
		}
		finally
		{
			g.dispose();
		}

		final OutputStream outputStream = new FileOutputStream(tileCacheFile);
		try
		{
			ImageIO.write(CustomPaletteBuilder.createIndexedImage(img, 64), "png", outputStream);

			outputStream.flush();
		}
		finally
		{
			outputStream.close();
		}
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
		return Configuration.getSetting("ABSOLUTE_PATH")+"data/starmap/_tilecache/";
	}

	/**
	 * Entfernt alle erzeugten Kacheln aus dem Cache.
	 */
	public void resetCache()
	{
		final File cacheDir = new File(getTilePath());
		if( !cacheDir.isDirectory() ) {
			return;
		}

		File[] files = cacheDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname)
			{
				if( !pathname.isFile() )
				{
					return false;
				}
				return pathname.getName().startsWith(TileCache.this.system.getID()+"_");
			}
		});

		for( File file : files )
		{
			file.delete();
		}
	}
}
