package net.driftingsouls.ds2.server.tick;

import net.driftingsouls.ds2.server.framework.BasicContext;
import net.driftingsouls.ds2.server.framework.EmptyPermissionResolver;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;
import org.hibernate.Session;
import org.springframework.context.ApplicationContext;

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
	 * @param db Die fuer den Tick verwendete Session.
	 * @param request Das Requestobjekt.
	 * @param response Das Responseobjekt.
	 * @param applicationContext Der zu verwendende Spring {@link ApplicationContext}.
	 */
	public TickContext(Session db, Request request, Response response, ApplicationContext applicationContext)
	{
		super(request, response, new EmptyPermissionResolver(), applicationContext);
		this.db = db;
	}

	@Override
	public Session getDB()
	{
		return db;
	}

	private final Session db;
}
