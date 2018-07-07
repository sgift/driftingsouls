package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import org.hibernate.Session;
import org.junit.*;

import javax.persistence.EntityManager;

/**
 * Variante des {@link DBTest}, in dem die Transaktionsverwaltung automatisch erfolgt
 * und pro Testfall eine eigene Transaktion gestartet wird. <b>Achtung:</b> die Testfaelle
 * duerfen ausdruecklich <b>nicht</b> auf die Transaktionssteuerung zugreifen.
 */
public class DBSingleTransactionTest
{
	@BeforeClass
	public static void setUpDatabase()
	{
		DBTestUtils.ladeHibernateKonfiguration();
		DBTestUtils.startDerby();
		DBTestUtils.erzeugeDbSchema();
		DBTestUtils.starteHibernate();
		DBTestUtils.erzeugeContext();
	}

	@Before
	public void startTransaction()
	{
		getEM().getTransaction().begin();
	}

	@After
	public void stoppeTransaktion() throws Exception
	{
		getEM().getTransaction().rollback();
		getEM().clear();
	}

	@AfterClass
	public static void tearDownDB() throws Exception
	{
		DBTestUtils.stoppeEntityManager();
		DBTestUtils.stoppeContext();
		DBTestUtils.stoppeHibernate();
		DBTestUtils.stopDerby();
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

	/**
	 * Gibt, sofern vorhanden, den im Context aktiven Benutzer zurueck.
	 * @return Der User
	 */
	protected User getUser()
	{
		return (User) getContext().getActiveUser();
	}

	/**
	 * Persistiert die angegebene Instanz und gibt sie anschiessend direkt wieder zurueck.
	 * @param instance Die zu persistierende Instanz
	 * @param <T> Der Typ der Entity
	 * @return Die Instanz
	 */
	protected <T> T persist(T instance)
	{
		getEM().persist(instance);
		return instance;
	}
}
