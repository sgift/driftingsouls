package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.entities.ComNetChannel;
import net.driftingsouls.ds2.server.entities.ComNetEntry;
import net.driftingsouls.ds2.server.entities.ComNetVisit;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.Query;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Hilfsmethoden zum ComNet.
 */
public class ComNetService
{
	public enum Suchmodus
	{
		Titel,
		Inhalt,
		UserId
	}

	/**
	 * Markiert den angegebenen ComNet-Kanal fuer den angegebenen Benutzer als (in diesem Moment) gelesen.
	 * @param kanal Der Kanal
	 * @param user Der Benutzer
	 */
	public void markiereKanalAlsGelesen(ComNetChannel kanal, User user)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();

		ComNetVisit visit = (ComNetVisit) db.createQuery("from ComNetVisit where user=:user and channel=:channel")
										  .setParameter("user", user)
										  .setParameter("channel", kanal)
										  .uniqueResult();
		if (visit != null)
		{
			visit.setTime(Common.time());
		}
		else
		{
			visit = new ComNetVisit(user, kanal);
			visit.setTime(0);
			db.persist(visit);
		}
	}

	/**
	 * Durchsucht einen ComNet-Kanal nach einem gegebenen Muster auf die angegebene Weise. Es wird nur eine Untermenge
	 * an gefundenen Eintraegen zurueckgegeben, spezifiziert durch eine Startnummer und der Anzahl der Eintraege.
	 * Die Eintraege werden in Reihenfolge ihres Datums zurueckgegeben, beginnend mit dem Neusten.
	 *
	 * @param kanal Der zu durchsuchende Kanal
	 * @param suchtext_inhalt Der Inhalt, nach dem gesucht werden soll
 	 * @param suchtext_titel Der Titel, nach dem gesucht werden soll
	 * @param suchid_sender Die SenderID, nach dem gesucht werden soll
	 * @param offset Die Startnummer des ersten zu findenden Eintrags (relativ zum ersten insgesamt gefundenen Eintrag)
	 * @param limit Die maximale Anzahl an zu findenden Eintraegen
	 * @return Die Liste der gefundenen Eintraege
	 * @throws java.lang.IllegalArgumentException Falls die Argumente ungueltig sind
	 */
	public List<ComNetEntry> durchsucheKanal(ComNetChannel kanal, String suchtext_inhalt, String suchtext_titel, Integer suchid_sender, int offset, int limit) throws IllegalArgumentException
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();

		Query query;
		query = db.createQuery("from ComNetEntry entry where entry.head like :searchtitel and entry.text like :searchinhalt "+(suchid_sender != null? " and entry.pic=:suchid_sender":"")+ " and channel=:channel order by entry.post desc");
		if(suchid_sender != null)
		{
			return Common.cast(query.setParameter("searchtitel", "%" + suchtext_titel + "%")
					.setParameter("searchinhalt", "%" + suchtext_inhalt + "%")
					.setParameter("channel", kanal)
					.setParameter("suchid_sender",suchid_sender)
					.setFirstResult(offset)
					.setMaxResults(limit)
					.list());
		}
		else {
			return Common.cast(query.setParameter("searchtitel", "%" + suchtext_titel + "%")
					.setParameter("searchinhalt", "%" + suchtext_inhalt + "%")
					.setParameter("channel", kanal)
					.setFirstResult(offset)
					.setMaxResults(limit)
					.list());
		}
	}

	/**
	 * Gibt zurueck, ob der momentan aktive User ungelesene Comnet-Nachrichten hat.
	 *
	 * @return <code>true</code>, falls dem so ist
	 */
	public boolean hatAktiverUserUngeleseneComNetNachrichten()
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		User user = (User) ContextMap.getContext().getActiveUser();

		Map<ComNetChannel, ComNetVisit> visits = new HashMap<>();

		List<?> visitList = db.createQuery("from ComNetVisit where user= :user")
							.setEntity("user", user)
							.list();
		for (Object aVisitList : visitList)
		{
			ComNetVisit avisit = (ComNetVisit) aVisitList;
			visits.put(avisit.getChannel(), avisit);
		}

		Iterator<?> chnlIter = db.createQuery("from ComNetChannel order by allyOwner").iterate();
		while (chnlIter.hasNext())
		{
			ComNetChannel achannel = (ComNetChannel) chnlIter.next();

			if (!achannel.isReadable(user, ContextMap.getContext()))
			{
				continue;
			}

			ComNetVisit visit = visits.get(achannel);

			if (visit == null)
			{
				visit = new ComNetVisit(user, achannel);
				visit.setTime(0);
				db.persist(visit);
			}

			Long lastpost = (Long) db.createQuery("select max(time) from ComNetEntry where channel= :channel")
								   .setEntity("channel", achannel)
								   .iterate().next();

			if (lastpost == null)
			{
				lastpost = 0L;
			}

			if (lastpost > visit.getTime())
			{
				return true;
			}
		}
		return false;
	}
}
