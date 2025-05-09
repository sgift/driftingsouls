package net.driftingsouls.ds2.server.tick;

import net.driftingsouls.ds2.server.framework.BasicContext;
import net.driftingsouls.ds2.server.framework.EmptyPermissionResolver;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;
import org.hibernate.Session;
import org.springframework.context.ApplicationContext;

import javax.persistence.EntityManager;

/**
 * A tick specific context, which does not use the default database session handling.
 * This class does neither open nor close a database connection.
 * This must be handled by the caller.
 */
public class TickContext extends BasicContext
{
	/**
	 * Initialisiert den Tick-Context.
	 *
	 * @param em Die fuer den Tick verwendete EntityManager.
	 * @param request Das Requestobjekt.
	 * @param response Das Responseobjekt.
	 * @param applicationContext Der zu verwendende Spring {@link ApplicationContext}.
	 */
	public TickContext(EntityManager em, Request request, Response response, ApplicationContext applicationContext)
	{
		super(request, response, new EmptyPermissionResolver(), applicationContext);
		this.em = em;
	}

	@Override
	public EntityManager getEM()
	{
		return em;
	}

	@Override
	public Session getDB()
	{
		return (Session) em.getDelegate();
	}

	private final EntityManager em;
}
