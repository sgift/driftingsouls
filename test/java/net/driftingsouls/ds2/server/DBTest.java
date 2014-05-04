package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.framework.BasicContext;
import net.driftingsouls.ds2.server.framework.CmdLineRequest;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.EmptyPermissionResolver;
import net.driftingsouls.ds2.server.framework.SimpleResponse;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import javax.persistence.EntityManager;
import java.sql.DriverManager;
import java.sql.SQLNonTransientConnectionException;

import static org.junit.Assert.*;

/**
 * Basisklasse fuer Datenbank-Tests via Hibernate. Kuemmert sich um das Starten und Stoppen von
 * Memory-Datenbank, Hibernate und des Contexts.
 */
public class DBTest
{
	private static final Logger LOG = LogManager.getLogger(DBTest.class);
	private static SchemaExport schema;

	@BeforeClass
	public static void setUpHibernateConfig()
	{
		LOG.info("Initialisiere Hibernate");
		HibernateUtil.initConfiguration("test/cfg/hibernate.xml", "jdbc:derby:memory:tests", "", "");
		schema = new SchemaExport(HibernateUtil.getConfiguration());
	}

	@Before
	public void setUpDB()
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

		try
		{
			LOG.info("Starte Hibernate-Instanz");
			HibernateUtil.createFactories();

			schema.execute(false, true, false, true);
		}
		catch (Exception ex)
		{
			LOG.fatal("", ex);
			fail("Konnte Hibernate nicht starten");
		}

		ApplicationContext springContext = new FileSystemXmlApplicationContext("test/cfg/spring.xml");

		BasicContext context = new BasicContext(new CmdLineRequest(new String[0]), new SimpleResponse(), new EmptyPermissionResolver(), springContext);
		ContextMap.addContext(context);

		HibernateUtil.getCurrentEntityManager();
	}

	@After
	public void tearDownDB() throws Exception
	{
		EntityManager em = HibernateUtil.getCurrentEntityManager();
		if( em.getTransaction().isActive() )
		{
			em.getTransaction().rollback();
		}
		HibernateUtil.removeCurrentEntityManager();
		em.close();

		BasicContext context = (BasicContext) ContextMap.getContext();
		context.free();

		LOG.info("Stoppe Hibernate");
		HibernateUtil.shutdown();

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

	/**
	 * Fuehrt den angegebenen Code (Lambda) in einer Transaktion aus.
	 * @param handler Der auszufuehrende Code
	 */
	protected void mitTransaktion(Runnable handler)
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

	/**
	 * Gibt die aktuelle Hibernate-Session zurueck.
	 * @return Die Session
	 */
	protected Session getDB()
	{
		return ContextMap.getContext().getDB();
	}

	/**
	 * Gibt den aktuellen Hibernate-EntityManager zurueck.
	 * @return Der EntityManager
	 */
	protected EntityManager getEM()
	{
		return ContextMap.getContext().getEM();
	}

	/**
	 * Gibt die aktuelle Context-Instanz zurueck.
	 * @return Die Context-Instanz
	 */
	protected Context getContext()
	{
		return ContextMap.getContext();
	}
}
