package net.driftingsouls.ds2.server.install;

import net.driftingsouls.ds2.server.framework.BasicContext;
import net.driftingsouls.ds2.server.framework.CmdLineRequest;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.EmptyPermissionResolver;
import net.driftingsouls.ds2.server.framework.SimpleResponse;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.framework.db.batch.SingleUnitOfWork;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;
import org.quartz.impl.StdScheduler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Allgemeine Hilfsfunktionen fuer das Installationsprogramm.
 */
public final class InstallUtils
{
	private static final Logger LOG = LogManager.getLogger(InstallUtils.class);

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

	public static <T> void mitTransaktion(final String name, final Supplier<? extends Collection<T>> jobDataGenerator, final Consumer<T> job)
	{
		LOG.info(name);
		new EvictableUnitOfWork<T>(name) {
			@Override
			public void doWork(T object) throws Exception
			{
				job.accept(object);
			}
		}
				.setFlushSize(1).setErrorReporter((uow, fo, e) -> LOG.error("Fehler bei "+uow.getName()+": Objekte "+fo+" fehlgeschlagen", e)).executeFor(jobDataGenerator.get());
	}

	public static void mitTransaktion(final String name, final Runnable job)
	{
		LOG.info(name);
		new SingleUnitOfWork(name) {
			@Override
			public void doWork() throws Exception
			{
				job.run();
			}
		}.setErrorReporter((uow, fo, e) -> LOG.error("Fehler bei "+uow.getName(), e)).execute();
	}

	public static void mitContext(Consumer<Context> contextConsumer)
	{
		try
		{
			ApplicationContext springContext = new FileSystemXmlApplicationContext("web/WEB-INF/cfg/spring.xml");

			// Ticks provisorisch deaktivieren
			StdScheduler quartzSchedulerFactory = (StdScheduler) springContext.getBean("quartzSchedulerFactory");
			quartzSchedulerFactory.shutdown();

			BasicContext context = new BasicContext(new CmdLineRequest(new String[0]), new SimpleResponse(), new EmptyPermissionResolver(), springContext);
			try
			{
				ContextMap.addContext(context);

				contextConsumer.accept(context);
			}
			finally
			{
				context.free();
			}
		}
		catch (Exception e)
		{
			LOG.error("Konnte Content nicht generieren", e);
		}
	}

	public static void mitContextUndSession(Consumer<Context> handler)
	{
		mitHibernateSession((session) -> mitContext(handler));
	}

	public static void mitHibernateSession(Consumer<Session> handler)
	{
		SessionFactory sf = HibernateUtil.getSessionFactory();
		Session session = sf.openSession();
		try
		{
			ManagedSessionContext.bind(session);

			// Call the next filter (continue request processing)
			handler.accept(session);

			// Commit and cleanup
			LOG.debug("Committing the database transaction");

			ManagedSessionContext.unbind(sf);

		}
		catch (RuntimeException e)
		{
			LOG.error("", e);
		}
		finally
		{
			session.close();
		}
	}
}
