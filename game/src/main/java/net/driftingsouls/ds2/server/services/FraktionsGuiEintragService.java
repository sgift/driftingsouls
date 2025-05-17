package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsGuiEintrag;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * Serviceklasse fuer {@link net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsGuiEintrag}.
 */
@Service
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class FraktionsGuiEintragService
{
	private final EntityManager db;

    public FraktionsGuiEintragService(EntityManager db) {
        this.db = db;
    }

    /**
	 * Findet alle GUI-Daten fuer die Fraktionsmaske.
	 * @return Die Liste der GUI-Daten
	 */
	public List<FraktionsGuiEintrag> findeAlle()
	{
		return db.createQuery("from FraktionsGuiEintrag", FraktionsGuiEintrag.class).getResultList();
	}

	/**
	 * Findet, sofern vorhanden, die GUI-Daten zur Fraktionsmaske fuer den angegebenen Benutzer.
	 * @param user Der Benutzer
	 * @return Die GUI-Daten oder <code>null</code>
	 */
	public @Nullable FraktionsGuiEintrag findeNachUser(@Nonnull User user)
	{
		return db.createQuery("from FraktionsGuiEintrag where user=:user", FraktionsGuiEintrag.class)
				.setParameter("user", user)
				.getResultList().stream().findFirst().orElse(null);
	}
}
