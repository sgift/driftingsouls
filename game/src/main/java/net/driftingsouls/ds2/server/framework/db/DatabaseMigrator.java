package net.driftingsouls.ds2.server.framework.db;

import com.googlecode.flyway.core.Flyway;
import com.googlecode.flyway.core.api.MigrationVersion;
import com.googlecode.flyway.core.util.Resource;
import com.googlecode.flyway.core.util.scanner.classpath.ClassPathScanner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Migrationsprogramm fuer die DS-Datenbank. Aktualisiert die Datenbank auf die jeweils letzte Version.
 */
public class DatabaseMigrator
{
	private static final Log LOG = LogFactory.getLog(DatabaseMigrator.class);

	/**
	 * Fuehrt die Aktualisierung durch.
	 * @param dbUrl Die JDBC-Url zur Datenbank
	 * @param dbUser Der DB-user
	 * @param dbPassword Das DB-Passwort
	 * @param emptyMeansInitial <code>true</code> falls die initiale DB-Version angenommen werden soll
	 * wenn noch keine Update-Informationen vorhanden sind. Andernfalls wird angenommen, dass die DB aktuell ist.
	 * @return Anzahl der angewendeten Updates
	 * @throws IOException
	 */
	public int upgradeDatabase(String dbUrl, String dbUser, String dbPassword, boolean emptyMeansInitial) throws IOException
	{
		LOG.info("Aktualisiere Datenbank");
		Flyway flyway = new Flyway();
		flyway.setDataSource(dbUrl, dbUser, dbPassword);
		flyway.setInitOnMigrate(true);
		flyway.setSqlMigrationPrefix("");
		flyway.setLocations("db/migration");
		if( !emptyMeansInitial )
		{
			flyway.setInitVersion(detectLatestDbVersion(flyway));
		}
		return flyway.migrate();
	}

	private MigrationVersion detectLatestDbVersion(Flyway flyway) throws IOException
	{
		MigrationVersion version = MigrationVersion.EMPTY;
		ClassPathScanner scanner = new ClassPathScanner();
		Resource[] resources = scanner.scanForResources("db/migration", flyway.getSqlMigrationPrefix(), flyway.getSqlMigrationSuffix());
		for (Resource res : resources)
		{
			String filename = res.getFilename();
			MigrationVersion thisVersion = MigrationVersion.fromVersion(filename.substring(0, filename.indexOf("__")));
			if( thisVersion.compareTo(version) > 0 )
			{
				version = thisVersion;
			}
		}
		return version;
	}
}
