package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.junit.After;
import org.junit.Before;

import javax.persistence.EntityManager;

/**
 * Variante des {@link DBTest}, in dem die Transaktionsverwaltung automatisch erfolgt
 * und pro Testfall eine eigene Transaktion gestartet wird. <b>Achtung:</b> die Testfaelle
 * duerfen ausdruecklich <b>nicht</b> auf die Transaktionssteuerung zugreifen.
 */
public class DBSingleTransactionTest
{
	@Before
	public void startTransaction()
	{
		getEM().getTransaction().begin();
	}

	@After
	public void stoppeTransaktion() {
		getEM().getTransaction().rollback();
		getEM().clear();
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
