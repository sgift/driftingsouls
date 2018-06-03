package net.driftingsouls.ds2.server.install;

import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.IntTutorial;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.ally.AllyRangDescriptor;
import net.driftingsouls.ds2.server.framework.DriftingSouls;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.units.UnitType;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.driftingsouls.ds2.server.install.InstallUtils.*;

/**
 * Installationstool fuer DS. Erstellt alle notwendigen Verzeichnisse und Konfigurationsdateien,
 * spielt den Referenzdatensatz in die Datenbank ein und laedt alle identifizierbaren nicht
 * eingecheckten Grafiken nach.
 */
public class Install
{
	public static void main(String[] args) throws IOException
	{
		// Unnoetige Logmeldungen unterdruecken
		Logger.getRootLogger().setLevel(Level.ERROR);
		Logger.getLogger("net.driftingsouls.ds2.server.install").setLevel(Level.INFO);

		Set<String> params = new HashSet<>(Arrays.asList(args));
		final boolean skipDatabase = params.contains("--skip-database");
		final boolean skipConfiguraiton = params.contains("--skip-configuration");

		System.out.println("\n\nDrifting Souls 2 - Installationsprogramm\n\n");

		System.out.println("Lade Datenbanktreiber");
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
		}
		catch (ClassNotFoundException e)
		{
			System.err.println("Der MySQl-Treiber konnte nicht gefunden werden");
			return;
		}

		if (pruefeVerzeichnisse())
		{
			return;
		}

		System.out.println();

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		System.out.print("Adresse des MySQL-Servers [localhost:3306]: ");
		String dblocation = reader.readLine();
		if( dblocation == null || dblocation.trim().isEmpty() )
		{
			dblocation = "localhost:3306";
		}

		System.out.print("Name der zu verwendenden MySQL-Datenbank [ds2]: ");
		String db = reader.readLine();
		if( db == null || db.trim().isEmpty() )
		{
			db = "ds2";
		}

		System.out.print("MySQL-Benutzername [root]: ");
		String user = reader.readLine();
		if( user == null || user.trim().isEmpty() )
		{
			user = "root";
		}

		System.out.print("MySQL-Passwort: ");
		String pw = reader.readLine();

		System.out.println();

		erstelleVerzeichnisse();

		try
		{
			String dburl = "jdbc:mysql://" + dblocation + "/" + db;

			HibernateUtil.initConfiguration("web/WEB-INF/cfg/hibernate.xml", dburl, user, pw);
			HibernateUtil.createFactories();

			try (Connection con = DriverManager.getConnection(dburl, user, pw))
			{
				System.out.println("Datenbankverbindung hergestellt");

				if (!skipDatabase)
				{
					if (!checkDatabaseEmpty(con, reader))
					{
						return;
					}

					setupDatabase(con);
					fillDatabase(con);

					System.out.println("Datenbank installiert\n");
				}

				if (!skipConfiguraiton)
				{
					System.out.println("Erstelle config.xml");
					if (!createConfigXml(reader, dblocation, db, user, pw))
					{
						return;
					}
				}

				boot();

				System.out.println("\nDS ist nun installiert.\n");

				loadImages(reader, con);

				generateContent();
			}

			System.out.println("\n\nDrifting Souls wurde erfolgreich installiert");
			System.out.println("Du solltest es nun mittels 'ant clean compile' erneut uebersetzen.");
			System.out.println("Anschliessend kannst du DS mittels des Kommandos 'ant run' starten und im Browser unter http://localhost:8080/driftingsouls/ aufrufen.");
		}
		catch (SQLException e)
		{
			System.err.println("Fehler bei Datenbankoperation");
			e.printStackTrace();
		}
	}

	private static void boot()
	{
		try
		{
			new DriftingSouls("web/WEB-INF/cfg/", true);
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Konnte DS nicht starten", e);
		}
	}

	private static void generateContent()
	{
		ContentGenerator generator = new ContentGenerator();
		generator.generiereContent();
	}

	private static void loadImages(BufferedReader reader, Connection con) throws IOException, SQLException
	{
		System.out.println("Das Installationsprogramm kann nun versuchen noch fehlende Grafiken " +
				"insbesondere fuer den eingespielten Musterdatensatz nachzuladen. Achtung: Diese Grafiken " +
				"unterliegen moeglicherweise speziellen Lizenzen die ihre Verwendung nur im Kontext" +
				"von Drifting Souls zulassen. Moeglicherweise verbietet die jeweilige Lizenz auch eine " +
				"Weiterbearbeitung der Grafik.\n");
		System.out.print("Sollen das Installationsprogramm nun die fehlenden Grafiken nachladen (y/n)? ");

		String result = reader.readLine();

		if( !"y".equalsIgnoreCase(result) )
		{
			return;
		}

		Set<String> imgs;

		ImageDownloader inst = new ImageDownloader();
		imgs = inst.readFromDb(con, "ally", "id");
		inst.store(imgs, "data/logos/ally/", ".gif");

		imgs = inst.readFromDb(con, "users", "id");
		inst.store(imgs, "data/logos/user/", ".gif");

		File[] cssFiles = new File("web/data/css").listFiles((FileFilter) FileFilterUtils.suffixFileFilter(".css"));
		for (File cssFile : cssFiles)
		{
			imgs = inst.readFromCss(cssFile);
			inst.store(imgs);
		}

		cssFiles = new File("web/data/css/common").listFiles((FileFilter) FileFilterUtils.suffixFileFilter(".css"));
		for (File cssFile : cssFiles)
		{
			imgs = inst.readFromCss(cssFile);
			inst.store(imgs);
		}

		File[] templateFiles = new File("templates").listFiles((FileFilter) FileFilterUtils.suffixFileFilter(".html"));
		for (File templateFile : templateFiles)
		{
			imgs = inst.readFromTemplate(templateFile);
			inst.store(imgs);
		}

		imgs = new HashSet<>(Arrays.asList(
				"jumpnode/jumpnode.png",
				"space/space.png",
				"asti_own/asti_own.png",
				"asti_enemy/asti_enemy.png",
				"asti_ally/asti_ally.png"));
		inst.store(imgs, "data/starmap/", null);

		imgs = Arrays.stream(Nebel.Typ.values()).map(Nebel.Typ::getImage).collect(Collectors.toSet());
		inst.store(imgs);

		imgs = Stream.of(0,1,2,3,4,5,6,7,8,9).map((g) -> "ground"+g+".png").collect(Collectors.toSet());
		inst.store(imgs, "data/buildings/", null);

		imgs = Stream.of(0,1,2,3,4,5,6).map((g) -> "off"+g+".png").collect(Collectors.toSet());
		inst.store(imgs, "data/interface/offiziere/", null);

		mitContextUndSession((context) -> {
			inst.store(inst.readFromEntity(AllyRangDescriptor.class, AllyRangDescriptor::getImage));
			inst.store(inst.readFromEntity(Building.class, Building::getDefaultPicture));
			inst.store(inst.readMultipleFromEntity(Building.class, (b) -> b.getAlternativeBilder().values()));
			inst.store(inst.readFromEntity(Forschung.class, Forschung::getImage));
			inst.store(inst.readFromEntity(ShipType.class, ShipType::getPicture));
			inst.store(inst.readFromEntity(UnitType.class, UnitType::getPicture));
			inst.store(inst.readFromEntity(IntTutorial.class, IntTutorial::getHeadImg));
			inst.store(inst.readFromEntity(Item.class, Item::getPicture, Item::getLargePicture));
			inst.store(inst.readFromEntity(BaseType.class, BaseType::getStarmapImage, BaseType::getSmallImage, BaseType::getLargeImage));
		});

		System.out.println("Alle automatisch erkannten Grafiken wurden geladen.\n");
	}

	private static boolean checkDatabaseEmpty(Connection con, BufferedReader reader) throws SQLException, IOException
	{
		DatabaseMetaData metaData = con.getMetaData();
		try (ResultSet rs = metaData.getTables(con.getCatalog(), con.getSchema(), "%", null))
		{
			boolean drop = false;
			while (rs.next())
			{
				if (!"TABLE".equalsIgnoreCase(rs.getString("TABLE_TYPE")))
				{
					continue;
				}
				if (!drop)
				{
					System.out.print("Warnung! Es existieren bereits Tabellen in der gewaehlten Datenbank. " +
							"Fuer die Installation von DS wird aber eine leere Datenbank benoetigt.\n" +
							"Sollten die Tabellen automatisch geloescht werden (y/n)?: ");

					String result = reader.readLine();
					if (!"y".equalsIgnoreCase(result.trim()))
					{
						return false;
					}
					drop = true;

					InstallUtils.toggleForeignKeyChecks(con, false);
				}

				System.out.println("Loesche Tabelle: " + rs.getString("TABLE_NAME"));
				try (Statement stmt = con.createStatement())
				{
					stmt.executeUpdate("DROP TABLE " + rs.getString("TABLE_NAME"));
				}
			}

			if (drop)
			{
				InstallUtils.toggleForeignKeyChecks(con, true);
			}
		}

		return true;
	}

	private static void erstelleVerzeichnisse() throws IOException
	{
		InstallUtils.createDirectory(new File("lox/tick"));
		InstallUtils.createDirectory(new File("lox/raretick"));
		InstallUtils.createDirectory(new File("lox/battles"));

		InstallUtils.createDirectory(new File("quests"));

		InstallUtils.createDirectory(new File("web/data/starmap/_tilecache"));
	}

	private static boolean createConfigXml(BufferedReader reader, String dblocation, String db, String user, String pw) throws IOException
	{
		if( new File("web/WEB-INF/cfg/config.xml").canRead() )
		{
			System.err.print("Es existiert bereits eine config.xml - Soll diese ueberschrieben werden (y/n)?: ");
			String result = reader.readLine();
			if( !"y".equalsIgnoreCase(result.trim()) )
			{
				return false;
			}
		}
		try
		{
			Document doc = XMLUtils.readFile("web/WEB-INF/cfg/config.sample.xml");

			Element el = (Element)XMLUtils.getNodeByXPath(doc, "/config/setting[@name='db_url']");
			el.setAttribute("value", "jdbc:mysql://"+dblocation+"/"+db);

			el = (Element)XMLUtils.getNodeByXPath(doc, "/config/setting[@name='db_user']");
			el.setAttribute("value", user);

			el = (Element)XMLUtils.getNodeByXPath(doc, "/config/setting[@name='db_password']");
			el.setAttribute("value", pw);

			el = (Element)XMLUtils.getNodeByXPath(doc, "/config/setting[@name='ABSOLUTE_PATH']");
			el.setAttribute("value", new File("web").getAbsolutePath() + "/");

			el = (Element)XMLUtils.getNodeByXPath(doc, "/config/setting[@name='LOXPATH']");
			el.setAttribute("value", new File("lox").getAbsolutePath() + "/");

			XMLUtils.writeFile("web/WEB-INF/cfg/config.xml", doc);

			System.out.println("config.xml erstellt");
		}
		catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException | TransformerException e)
		{
			System.err.println("Konnte Konfigurationsdatei nicht erstellen");
			throw new IOException(e);
		}

		return true;
	}

	private static boolean pruefeVerzeichnisse()
	{
		System.out.println("Prüfe Verzeichnis");
		File dbDir = new File("./db");
		if( !dbDir.isDirectory() )
		{
			System.err.println("Konnte das Verzeichnis 'db' nicht finden! Bitte das Installationsscript aus dem DS-Verzeichnis heraus aufrufen!");
			return true;
		}

		File tableDir = new File("./db/tables");
		if( !tableDir.isDirectory() )
		{
			System.err.println("Konnte das Verzeichnis 'db/tables' nicht finden! Bitte das Installationsscript aus dem DS-Verzeichnis heraus aufrufen!");
			return true;
		}

		File webDir = new File("./web");
		if( !webDir.isDirectory() )
		{
			System.err.println("Konnte das Verzeichnis 'web' nicht finden! Bitte das Installationsscript aus dem DS-Verzeichnis heraus aufrufen!");
			return true;
		}

		File templatesDir = new File("./templates");
		if( !templatesDir.isDirectory() )
		{
			System.err.println("Konnte das Verzeichnis 'templates' nicht finden! Bitte das Installationsscript aus dem DS-Verzeichnis heraus aufrufen!");
			return true;
		}

		return false;
	}

	private static void setupDatabase(Connection con) throws SQLException
	{
		System.out.println("Erstelle Tabellenstruktur");
		String[] create = HibernateUtil.getConfiguration().generateSchemaCreationScript(new MySQL5InnoDBDialect());
		for (String s : create)
		{
			try (Statement stmt = con.createStatement())
			{
				stmt.executeUpdate(s);
			}
			catch (SQLException e)
			{
				System.err.println("Konnte Statement nicht ausfuehren: " + s);
				throw e;
			}
		}

		InstallUtils.installSqlFile(con, new File("db/functions.sql"));
	}

	private static void fillDatabase(Connection con) throws SQLException
	{
		try (Statement stmt = con.createStatement())
		{
			stmt.execute("SET SESSION sql_mode='NO_AUTO_VALUE_ON_ZERO'");
			stmt.execute("SET FOREIGN_KEY_CHECKS=0");
		}

		File tableDir = new File("db/tables");
		System.out.println("Schreibe Datensaetze...");

		File[] insertFiles = tableDir.listFiles((FileFilter) FileFilterUtils.suffixFileFilter("_insert.sql"));
		for (File insertFile : insertFiles)
		{
			System.out.println("Fuehre Script '"+insertFile.getName()+"' aus");
			InstallUtils.installSqlFile(con, insertFile);
		}

		try (Statement stmt = con.createStatement())
		{
			stmt.execute("SET FOREIGN_KEY_CHECKS=1");
		}
	}
}
