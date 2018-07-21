package net.driftingsouls.ds2.server.install;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.commons.io.IOUtils;
import org.hibernate.Session;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Funktionen zum identifizieren und nachladen von Grafiken fuer die DS-Installation.
 */
public class ImageDownloader
{
	/**
	 * Laedt das angegebene Set von Grafiken vom DS-Server und speichert es im lokalen
	 * Dateisystem ab. Die Methode ist fehlertollerant, d.h. bei einem IO-Fehler bei einem Bild
	 * wird nur dieses nicht verarbeitet. Sollte die Grafik bereits vorhanden sein wird
	 * diese nicht ueberschrieben.
	 * @param imgs Das zu bearbeitende Grafikset
	 */
	public void store(Set<String> imgs)
	{
		store(imgs, null, null);
	}

	/**
	 * Laedt das angegebene Set von Grafiken vom DS-Server und speichert es im lokalen
	 * Dateisystem ab. Jeder Grafik kann dabei optional ein Prefix und ein Suffix mitgegeben
	 * werden. Die Methode ist fehlertollerant, d.h. bei einem IO-Fehler bei einem Bild
	 * wird nur dieses nicht verarbeitet. Sollte die Grafik bereits vorhanden sein wird
	 * diese nicht ueberschrieben.
	 * @param imgs Das zu bearbeitende Grafikset
	 * @param prefix Das fuer alle Grafiken zu verwendende Prefix (optional)
	 * @param suffix Das fuer alle Grafiken zu verwendende Suffix (optional)
	 */
	public void store(Set<String> imgs, String prefix, String suffix)
	{
		final String dsUrl = "https://ds2.drifting-souls.net/";

		for (String img : imgs)
		{
			if( prefix != null )
			{
				img = prefix + img;
			}
			if( suffix != null )
			{
				img += suffix;
			}

			File localFile = new File("web/"+img);
			if( localFile.isFile() )
			{
				continue;
			}

			try
			{
				if( !localFile.getParentFile().isDirectory() )
				{
					InstallUtils.createDirectory(localFile.getParentFile());
				}

				System.out.print("Lade [" + img + "]...");

				storeUrlOnDisc(dsUrl+img, localFile);

				System.out.println("fertig");
			}
			catch( IOException e )
			{
				System.out.println(e.getClass().getSimpleName()+" - "+e.getMessage());
			}
		}
	}

	private void storeUrlOnDisc(String urlString, File localFile) throws IOException
	{
		URL url = new URL(urlString);
		HttpURLConnection urlcon = (HttpURLConnection)url.openConnection();
		urlcon.setRequestProperty("User-agent", "DS Installationsprogramm");
		urlcon.setReadTimeout(60*1000);

		try (InputStream in = urlcon.getInputStream())
		{
			try (FileOutputStream out = new FileOutputStream(localFile))
			{
				IOUtils.copy(in, out);
			}
		}
	}

	/**
	 * Liesst alle moeglichen Grafiken aus einer CSS-Datei aus.
	 * @param cssFile Die CSS-Datei
	 * @return Die identifizierten Grafiken
	 * @throws IOException Bei IO-Fehlern
	 */
	public Set<String> readFromCss(File cssFile) throws IOException
	{
		String content;
		try (FileInputStream in = new FileInputStream(cssFile))
		{
			content = IOUtils.toString(in, "UTF-8");
		}

		Set<String> imgs = new HashSet<>();

		Pattern pattern = Pattern.compile("url\\(([a-zA-Z0-9./'\"_\\-]+)\\)", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(content);
		while( matcher.find() )
		{
			String match = matcher.group(1);
			if( match.startsWith("'") || match.startsWith("\"") )
			{
				match = match.substring(1);
			}
			if( match.endsWith("'") || match.endsWith("\"") )
			{
				match = match.substring(0,match.length()-1);
			}
			if( match.startsWith("./") )
			{
				// Pfad relativ zur aufrufenden HTML-Seite
				match = match.substring(2);
			}
			else
			{
				// Pfad relativ zur aufgerufenen CSS-Datei

				String prefix = "";
				File parent = cssFile.getParentFile();
				while( parent != null )
				{
					prefix = parent.getName()+"/"+prefix;

					if( "data".equalsIgnoreCase(parent.getName()) )
					{
						break;
					}

					parent = parent.getParentFile();
				}
				match = prefix + match;
			}

			imgs.add(match);
		}
		return imgs;
	}

	/**
	 * Liesst alle moeglichen Grafiken aus einer Template-Datei aus,
	 * sofern diese eindeutig identifiziert werden koennen (d.h. der Pfad
	 * keine nicht aufloesbaren Variablen enthaelt).
	 * @param templateFile Die Template-Datei
	 * @return Die identifizierten Grafiken
	 * @throws IOException Bei IO-Fehlern
	 */
	public Set<String> readFromTemplate(File templateFile) throws IOException
	{
		String content;
		try (FileInputStream in = new FileInputStream(templateFile))
		{
			content = IOUtils.toString(in, "UTF-8");
		}

		Set<String> imgs = new HashSet<>();

		Pattern pattern = Pattern.compile("src=\"([{}a-zA-Z0-9./_\\-']+)\"", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(content);
		while( matcher.find() )
		{
			String match = matcher.group(1);
			if( match.contains("{") )
			{
				// Nicht identifizierbar
				continue;
			}

			if( match.startsWith("/") )
			{
				match = match.substring(1);
			}

			imgs.add(match);
		}
		return imgs;
	}

	/**
	 * Laedt ein Set von Grafiken (Pfaden) aus allen verfuegbaren Entities eines Typs.
	 * Es wird eine vorhandene Hibernate Session und ein vorhandener Context vorausgesetzt.
	 * @param entityClass Der Entitytyp
	 * @param getters Die Getter zum Ermitteln der Pfade auf der Entity
	 * @return Das resultierende Set an Grafikpfaden
	 */
	@SafeVarargs
	public final <T> Set<String> readMultipleFromEntity(Class<T> entityClass, Function<T, Collection<String>>... getters)
	{
		Set<String> imgs = new HashSet<>();

		Session db = ContextMap.getContext().getDB();
		List<T> list = Common.cast(db.createCriteria(entityClass).list());
		for (Function<T, Collection<String>> getter : getters)
		{
			imgs.addAll(list.stream().flatMap((e) -> getter.apply(e).stream()).collect(Collectors.toList()));
		}

		return imgs;
	}


	/**
	 * Laedt ein Set von Grafiken (Pfaden) aus allen verfuegbaren Entities eines Typs.
	 * Es wird eine vorhandene Hibernate Session und ein vorhandener Context vorausgesetzt.
	 * @param entityClass Der Entitytyp
	 * @param getters Die Getter zum Ermitteln der Pfade auf der Entity
	 * @return Das resultierende Set an Grafikpfaden
	 */
	@SafeVarargs
	public final <T> Set<String> readFromEntity(Class<T> entityClass, Function<T, String>... getters)
	{
		Set<String> imgs = new HashSet<>();

		Session db = ContextMap.getContext().getDB();
		List<T> list = Common.cast(db.createCriteria(entityClass).list());
		for (Function<T, String> getter : getters)
		{
			imgs.addAll(list.stream().map(getter::apply).collect(Collectors.toList()));
		}

		return imgs;
	}

	/**
	 * Laedt ein Set von Grafiken (Pfaden) aus der Datenbank.
	 * @param con Die zu verwendende Datenbankconnection.
	 * @param table Die Tabelle aus der die Bilder geladen werden sollen
	 * @param columns Die Spalten in der Tabelle, die Bildpfade enthalten
	 * @return Das resultierende Set an Grafikpfaden
	 * @throws SQLException Bei SQL-Fehlern
	 */
	public Set<String> readFromDb(Connection con, String table, String... columns) throws SQLException
	{
		Set<String> imgs = new HashSet<>();

		try (PreparedStatement stmt = con.prepareStatement("select " + String.join(",", columns) + " from " + table))
		{
			try (ResultSet result = stmt.executeQuery())
			{
				while (result.next())
				{
					for (String col : columns)
					{
						String img = result.getString(col);
						if (img != null && !img.trim().isEmpty())
						{
							imgs.add(img);
						}
					}
				}
			}
		}

		return imgs;
	}
}
