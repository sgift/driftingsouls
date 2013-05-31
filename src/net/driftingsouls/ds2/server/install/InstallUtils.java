package net.driftingsouls.ds2.server.install;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Allgemeine Hilfsfunktionen fuer das Installationsprogramm.
 */
public final class InstallUtils
{
	private InstallUtils()
	{
		// EMPTY
	}

	/**
	 * Erstellt ein Verzeichnis, sofern dieses noch nicht vorhanden ist. Sollte
	 * der Pfad zum Verzeichnis ebenfalls nicht existieren wird auch dieser erstellt.
	 * @param directory Das zu erstellende Verzeichnis
	 * @throws IOException Falls keine Berechtigung zum Erstellen von Verzeichnissen vorliegt
	 */
	public static void createDirectory(File directory) throws IOException
	{
		if( directory.getParentFile() != null && !directory.getParentFile().isDirectory() )
		{
			createDirectory(directory.getParentFile());
		}

		if( !directory.isDirectory() )
		{
			System.out.println("Erstelle Verzeichnis "+directory.getAbsolutePath());
			if( !directory.mkdir() )
			{
				throw new IOException("Konnte Verzeichnis "+ directory +" nicht erstellen");
			}
		}
	}

	/**
	 * (De)aktiviert alle Foreign Key-Constraints fuer die gegebene Datenbankverbindung
	 * @param con Die Datenbankverbindung
	 * @param on <code>true</code> falls die Constraints aktiviert werden sollen
	 * @throws SQLException Bei SQL-Fehlern
	 */
	public static void toggleForeignKeyChecks(Connection con, boolean on) throws SQLException
	{
		try (PreparedStatement stmt = con.prepareStatement("SET FOREIGN_KEY_CHECKS=" + (on ? 1 : 0)))
		{
			stmt.executeUpdate();
		}
	}

	/**
	 * Fuehrt das angegebene SQL-Script aus.
	 * @param con Die Datenbankverbindung auf der das Script ausgefuehrt werden soll
	 * @param file Das auszufuehrende Script
	 * @throws SQLException Bei SQL-Fehlern
	 */
	public static void installSqlFile(Connection con, File file) throws SQLException
	{
		try
		{
			String delimiter = ";";
			String str;
			try (FileInputStream in = new FileInputStream(file))
			{
				str = IOUtils.toString(in, "UTF-8");
			}

			StringBuilder statement = new StringBuilder();
			List<String> statements = new ArrayList<>();
			for( String line : str.split("\n") )
			{
				line = line.trim();
				if( line.isEmpty() || line.startsWith("--") || line.startsWith("//") )
				{
					continue;
				}
				if( line.toUpperCase().startsWith("DELIMITER ") )
				{
					delimiter = line.substring("DELIMITER ".length()).trim();
					continue;
				}
				statement.append(line);
				statement.append(" ");
				if( line.endsWith(delimiter) )
				{
					statements.add(statement.toString().trim());
					statement.setLength(0);
				}
			}
			if( !statement.toString().trim().isEmpty() )
			{
				statements.add(statement.toString().trim());
			}

			for (String s : statements)
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

		}
		catch (IOException e)
		{
			throw new SQLException(e);
		}
	}
}
