/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.tools;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.DSApplication;
import net.driftingsouls.ds2.server.ships.ShipType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFilter;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Required;

/**
 * Kommandozeilentool zur Erstellung von Grafikpaks.<br>
 * ACHTUNG: Dieses Programm funktioniert nur in einer Kommandozeile, wenn tar vorhanden ist!
 * @author Christopher Jung
 *
 */
@Configurable
public class GfxPak extends DSApplication {
	private Configuration config;

	/**
	 * Konstruktor.
	 * @param args Die Kommandozeilenargumente
	 * @throws Exception
	 */
	public GfxPak(String[] args) throws Exception {
		super(args);
	}
	
	private void printHelp() {
		log("DS GFXPak Generator");
		log("Erzeugt Grafikpaks");
		log("");
		log("java "+getClass().getName()+" --config $configpfad [--help]");
		log(" * --config Der Pfad zum DS2-Konfigurationsverzeichnis");
		log(" * [optional] --help Zeigt diese Hilfe an");
	}
	
	private String dirname(String file) {
		int pos = file.lastIndexOf('/');
		if( pos != -1 ) {
			return file.substring(0,pos);
		}
		return null;
	}
	
	/**
	 * Injiziert die DS-Konfiguration.
	 * @param config Die DS-Konfiguration
	 */
	@Autowired @Required
	public void setConfiguration(Configuration config) {
		this.config = config;
	}
	
	/**
	 * Startet die Ausfuehrung.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void execute() throws IOException, InterruptedException {
		if( getContext().getRequest().getParameter("help") != null ) {
			printHelp();
			return;
		}
		
		final String datadir = config.get("ABSOLUTE_PATH")+"data";
		
		log("\nBeginne:");

		String rnd = "";
		do {
			rnd = Common.md5(Integer.toString(RandomUtils.nextInt(Integer.MAX_VALUE)));
		} 
		while( new File(rnd).exists() );
		
		rnd = new File(rnd).getAbsolutePath();

		new File(rnd).mkdir();
		new File(rnd+"/data").mkdir();
		new File(rnd+"/data/schiffe").mkdir();
		new File(rnd+"/data/interface").mkdir();
		new File(rnd+"/data/interface/border").mkdir();
		new File(rnd+"/data/interface/menubar").mkdir();
		new File(rnd+"/data/javascript").mkdir();
		new File(rnd+"/data/css").mkdir();
		new File(rnd+"/data/resources").mkdir();

		// Resourcen kopieren
		log("Kopiere Resourcen");
		FileUtils.copyDirectory(new File(datadir+"/resources/"), new File(rnd+"/data/resources/"));

		// Javascript kopieren
		log("Kopiere Javascript");
		FileUtils.copyDirectory(new File(datadir+"/javascript/"), new File(rnd+"/data/javascript/"));
		
		// CSS kopieren
		log("Kopiere CSS");
		FileUtils.copyDirectory(new File(datadir+"/css/"), new File(rnd+"/data/css/"));
		
		// Kopiere Menubar-Pfeil
		log("Kopiere Menubar-Pfeil");
		FileUtils.copyFileToDirectory(new File(datadir+"/interface/menubar/menubar_kriegsschiffe_arrow_l.gif"), 
				new File(rnd+"/data/interface/menubar/"));
		
		// Kopiere Borders
		log("Kopiere Borders");
		FileUtils.copyDirectory(new File(datadir+"/interface/border/"), 
				new File(rnd+"/data/interface/border/"));

		// Schiffsbilder kopieren
		log("Kopiere Schiffsbilder");
		org.hibernate.Session db = getDB();
		List<ShipType> types = Common.cast(db.createQuery("from ShipType where hide=0").list());
		for( ShipType type : types )
		{
			String dirname = dirname(type.getPicture());
			String filename = type.getPicture().substring(dirname.length()+1);
			new File(rnd+"/"+dirname).mkdirs();
			
			String name = filename.substring(0, filename.length() - "png".length()-1);
			
			File[] files = new File(config.get("ABSOLUTE_PATH")+dirname).listFiles((FileFilter)new WildcardFilter(name+"*.png"));
			for( int i=0; i < files.length; i++ ) {
				FileUtils.copyFile(files[i], new File(rnd+"/"+dirname+"/"+FilenameUtils.getName(files[i].getName())));
			}
		}

		// GFXPak-Version speichern (erst jetzt, um eine ggf mitkopierte Version der Datei zu ueberschreiben)
		log("Versionierung...");
		FileWriter ver = new FileWriter(rnd+"/data/javascript/gfxpakversion.js");
		try {
			ver.write("// DS GFXPAK VERSION\n");
			ver.write("var _GFXPAKVERSION="+config.get("GFXPAK_VERSION")+";\n");
		}
		finally {
			ver.close();
		}

		// Archiv erzeugen
		log("tar...");

		Process p = Runtime.getRuntime().exec("tar cfj ../gfxpak"+Common.date("Ymd")+".tar.bz2 data", null, new File(rnd+"/"));
		p.waitFor();
		IOUtils.copy(p.getErrorStream(), System.err);
		IOUtils.copy(p.getInputStream(), System.err);

		log("aufraeumen");
		FileUtils.deleteDirectory(new File(rnd));
	}

	/**
	 * Main.
	 * @param args Die Argumente
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		GfxPak rs = new GfxPak(args);
		rs.addLogTarget("_ds2_gfxpak.log", false);
		rs.execute();
		rs.dispose();
	}

}
