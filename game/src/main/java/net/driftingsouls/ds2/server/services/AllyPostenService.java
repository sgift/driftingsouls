package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.ally.AllyPosten;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

/**
 * Service fuer Allianzposten.
 */
@Service
public class AllyPostenService
{
	private static final double MAX_POSTENCOUNT = 0.3;

	/**
	 * Gibt die momentane Anzahl an eingerichteten Posten einer Allianz zurueck.
	 *
	 * @param ally Die Allianz
	 * @return Die momentane Anzahl an eingerichteten Posten
	 */
	public int getAnzahlPostenDerAllianz(@Nonnull Ally ally)
	{
		return ally.getPosten().size();
	}

	/**
	 * Gibt die maximale Anzahl an Posten einer Allianz zurueck.
	 *
	 * @param allianz Die Allianz
	 * @return Die maximale Anzahl an Posten
	 */
	public int getMaxPostenDerAllianz(@Nonnull Ally allianz)
	{
		long membercount = allianz.getMemberCount();

		int maxposten = (int) Math.round(membercount * MAX_POSTENCOUNT);
		if (maxposten < 2)
		{
			maxposten = 2;
		}
		return maxposten;
	}

	/**
	 * Loescht den angegebenen Allianzposten.
	 * @param posten Der Posten
	 */
	public void loesche(@Nonnull AllyPosten posten)
	{
		Session db = ContextMap.getContext().getDB();

		if (posten.getUser() != null)
		{
			posten.getUser().setAllyPosten(null);
		}
		db.delete(posten);
		posten.getAlly().getPosten().remove(posten);
	}

	/**
	 * Erstellt einen neuen Allianzposten fuer die angegebene Allianz.
	 * @param allianz Die Allianz
	 * @param name Der Name des neuen Postens
	 * @return Der neue Posten
	 */
	public @Nonnull AllyPosten erstelle(@Nonnull Ally allianz, @Nonnull String name) {
		Session db = ContextMap.getContext().getDB();

		AllyPosten posten = new AllyPosten(allianz, name);
		db.persist(posten);
		allianz.getPosten().add(posten);
		return posten;
	}
}
