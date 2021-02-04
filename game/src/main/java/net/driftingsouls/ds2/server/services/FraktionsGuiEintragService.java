package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsGuiEintrag;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Serviceklasse fuer {@link net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsGuiEintrag}.
 */
@Service
public class FraktionsGuiEintragService
{
	@PersistenceContext
	private EntityManager em;

	/**
	 * Findet alle GUI-Daten fuer die Fraktionsmaske.
	 * @return Die Liste der GUI-Daten
	 */
	public List<FraktionsGuiEintrag> findeAlle() {
		return em.createQuery("from FraktionsGuiEintrag", FraktionsGuiEintrag.class).getResultList();
	}

	/**
	 * Findet, sofern vorhanden, die GUI-Daten zur Fraktionsmaske fuer den angegebenen Benutzer.
	 * @param user Der Benutzer
	 * @return Die GUI-Daten oder <code>null</code>
	 */
	public @Nullable FraktionsGuiEintrag findeNachUser(@NonNull User user) {
		var result = em.createQuery("from FraktionsGuiEintrag where user=:user", FraktionsGuiEintrag.class)
				.setParameter("user", user)
				.getResultStream();

		return result.findFirst().orElse(null);
	}
}
