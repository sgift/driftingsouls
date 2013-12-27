package net.driftingsouls.ds2.server.entities;

import net.driftingsouls.ds2.server.framework.ContextMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Hilfsmethoden zum ComNet.
 */
public class ComNetService
{
	/**
	 * Gibt zurueck, ob der momentan aktive User ungelesene Comnet-Nachrichten hat.
	 * @return <code>true</code>, falls dem so ist
	 */
	public boolean hatAktiverUserUngeleseneComNetNachrichten() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		User user = (User)ContextMap.getContext().getActiveUser();

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
