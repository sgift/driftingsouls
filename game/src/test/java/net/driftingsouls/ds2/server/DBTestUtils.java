package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.framework.BasicContext;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.EmptyPermissionResolver;
import net.driftingsouls.ds2.server.framework.SimpleResponse;
import net.driftingsouls.ds2.server.framework.TestRequest;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.persistence.EntityManager;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;

import static org.junit.Assert.*;

/**
 * Hilfsmethoden fuer DB-Tests.
 */
final class DBTestUtils
{
	private static final Logger LOG = LogManager.getLogger(DBTest.class);
	private static SchemaExport schema;

	private DBTestUtils() {
		// EMPTY
	}

	public static void erzeugeContext()
	{
		ApplicationContext springContext = new AnnotationConfigApplicationContext(TestAppConfig.class);

		BasicContext context = new BasicContext(new TestRequest(), new SimpleResponse(), new EmptyPermissionResolver(), springContext);
		ContextMap.addContext(context);
	}

	public static void starteHibernate()
	{
		try
		{
			LOG.info("Starte Hibernate-Instanz");
			HibernateUtil.createFactories();
		}
		catch (Exception ex)
		{
			LOG.fatal("", ex);
			fail("Konnte Hibernate-Instanz nicht starten");
		}
	}

	public static void erzeugeDbSchema()
	{
		try
		{
			LOG.info("Erzeuge DB-Schema");
			schema.execute(false, true, false, true);
		}
		catch (Exception ex)
		{
			LOG.fatal("", ex);
			fail("Konnte DB-Schema nicht erzeugen");
		}
	}

	public static void startDerby()
	{
		try
		{
			LOG.info("Starte Derby");
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
			DriverManager.getConnection("jdbc:derby:memory:tests;create=true").close();
		}
		catch (Exception ex)
		{
			LOG.fatal("", ex);
			fail("Konnte Derby nicht starten.");
		}
	}

	public static void stoppeHibernate()
	{
		LOG.info("Stoppe Hibernate");
		HibernateUtil.shutdown();
	}

	public static void stoppeContext()
	{
		BasicContext context = (BasicContext) ContextMap.getContext();
		if( context != null )
		{
			context.free();
		}
	}

	public static void stoppeEntityManager()
	{
		EntityManager em = HibernateUtil.getCurrentEntityManager();
		if( em.getTransaction().isActive() )
		{
			em.getTransaction().rollback();
		}
		HibernateUtil.removeCurrentEntityManager();
		em.close();
	}

	public static void stopDerby() throws SQLException
	{
		LOG.info("Beende Derby");
		try
		{
			DriverManager.getConnection("jdbc:derby:memory:tests;drop=true").close();
		}
		catch (SQLNonTransientConnectionException ex)
		{
			if (ex.getErrorCode() != 45000)
			{
				throw ex;
			}
		}
	}

	public static void ladeHibernateKonfiguration()
	{
		LOG.info("Initialisiere Hibernate");
		HibernateUtil.initConfiguration("src/test/cfg/hibernate.xml", "jdbc:derby:memory:tests", "", "");
		schema = new SchemaExport(HibernateUtil.getConfiguration());
	}

	/**
	 * Fuehrt den angegebenen Code (Lambda) in einer Transaktion aus.
	 * @param handler Der auszufuehrende Code
	 */
	public static void mitTransaktion(Runnable handler)
	{
		EntityManager em = HibernateUtil.getCurrentEntityManager();
		try {
			em.getTransaction().begin();

			// Call the next filter (continue request processing)
			handler.run();

			// Commit and cleanup
			LOG.debug("Committing the database transaction");

			if( em.getTransaction().isActive() )
			{
				em.getTransaction().commit();
			}
		}
		catch( AssertionError ex )
		{
			try
			{
				if (em.getTransaction().isActive())
				{
					LOG.debug("Trying to rollback database transaction after assertion error");
					em.getTransaction().rollback();
				}
			}
			catch (Throwable rbEx)
			{
				LOG.error("Could not rollback transaction after assertion error!", rbEx);
			}

			throw ex;
		}
		catch (Throwable ex)
		{
			try
			{
				if (em.getTransaction().isActive())
				{
					LOG.debug("Trying to rollback database transaction after exception");
					em.getTransaction().rollback();
				}
			}
			catch (Throwable rbEx)
			{
				LOG.error("Could not rollback transaction after exception!", rbEx);
			}

			LOG.error("", ex);
			fail("Transaktionsfehler");
		}
		finally
		{
			HibernateUtil.removeCurrentEntityManager();
			em.close();
		}
	}
}
