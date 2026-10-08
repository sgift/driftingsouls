package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.ally.AllyPosten;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import javax.annotation.Nonnull;

/**
 * Service fuer Allianzposten.
 */
@Service
@RequestScope
public class AlliancePostService
{
	private static final double MAX_POST_COUNT = 0.3;

	/**
	 * Gibt die momentane Anzahl an eingerichteten Posten einer Allianz zurueck.
	 *
	 * @param ally Die Allianz
	 * @return Die momentane Anzahl an eingerichteten Posten
	 */
	public int getPostCountOfAlliance(@Nonnull Ally ally)
	{
		return ally.getPosten().size();
	}

	/**
	 * Gibt die maximale Anzahl an Posten einer Allianz zurueck.
	 *
	 * @param alliance Die Allianz
	 * @return Die maximale Anzahl an Posten
	 */
	public int getMaxPostCountOfAlliance(@Nonnull Ally alliance)
	{
		long membercount = alliance.getMemberCount();

		int maxPosts = (int) Math.round(membercount * MAX_POST_COUNT);
		if (maxPosts < 2)
		{
			maxPosts = 2;
		}
		return maxPosts;
	}

	/**
	 * Loescht den angegebenen Allianzposten.
	 * @param post Der Posten
	 */
	public void delete(@Nonnull AllyPosten post)
	{
		Session db = ContextMap.getContext().getDB();

		if (post.getUser() != null)
		{
			post.getUser().setAllyPosten(null);
		}
		db.delete(post);
		post.getAlly().getPosten().remove(post);
	}

	/**
	 * Erstellt einen neuen Allianzposten fuer die angegebene Allianz.
	 * @param alliance Die Allianz
	 * @param name Der Name des neuen Postens
	 * @return Der neue Posten
	 */
	public @Nonnull AllyPosten create(@Nonnull Ally alliance, @Nonnull String name) {
		Session db = ContextMap.getContext().getDB();

		AllyPosten post = new AllyPosten(alliance, name);
		db.persist(post);
		alliance.getPosten().add(post);
		return post;
	}
}
