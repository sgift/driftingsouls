package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsGuiEintrag;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Serviceklasse fuer {@link net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsGuiEintrag}.
 */
@Service
public class FraktionsGuiEintragService
{
	/**
	 * Findet alle GUI-Daten fuer die Fraktionsmaske.
	 * @return Die Liste der GUI-Daten
	 */
	public List<FraktionsGuiEintrag> findeAlle()
	{
		return Common.cast(ContextMap.getContext().getDB().createCriteria(FraktionsGuiEintrag.class).list());
	}

	/**
	 * Findet, sofern vorhanden, die GUI-Daten zur Fraktionsmaske fuer den angegebenen Benutzer.
	 * @param user Der Benutzer
	 * @return Die GUI-Daten oder <code>null</code>
	 */
	public @Nullable FraktionsGuiEintrag findeNachUser(@Nonnull User user)
	{
		Session db = ContextMap.getContext().getDB();
		return (FraktionsGuiEintrag)db.createQuery("from FraktionsGuiEintrag where user=:user")
				.setParameter("user", user)
				.uniqueResult();
	}
}
