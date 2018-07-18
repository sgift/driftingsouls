package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;

import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import javax.persistence.EntityManager;

/**
 * Basisklasse fuer Datenbank-Tests via Hibernate. Kuemmert sich um das Starten und Stoppen von
 * Memory-Datenbank, Hibernate und des Contexts.
 */
public class DBTest
{
	@BeforeClass
	public static void setUpHibernateConfig()
	{
		DBTestUtils.ladeHibernateKonfiguration();
	}

	@Before
	public void setUpDB()
	{
		DBTestUtils.startDerby();
		DBTestUtils.erzeugeDbSchema();
		DBTestUtils.starteHibernate();
		DBTestUtils.erzeugeContext();

		HibernateUtil.getCurrentEntityManager();
	}

	@After
	public void tearDownDB() throws Exception
	{
		DBTestUtils.stoppeEntityManager();
		DBTestUtils.stoppeContext();
		DBTestUtils.stoppeHibernate();
		DBTestUtils.stopDerby();
	}

	/**
	 * Fuehrt den angegebenen Code (Lambda) in einer Transaktion aus.
	 * @param handler Der auszufuehrende Code
	 */
	public void mitTransaktion(Runnable handler)
	{
		DBTestUtils.mitTransaktion(handler);
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
