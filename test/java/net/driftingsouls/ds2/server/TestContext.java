package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.framework.BasicContext;
import net.driftingsouls.ds2.server.framework.EmptyPermissionResolver;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;
import org.hibernate.Session;
import org.springframework.context.ApplicationContext;

public class TestContext extends BasicContext
{
	/**
	 * Konstruktor.
	 * @param db Die Datenbankverbindung
	 * @param request Das Request-Objekt
	 * @param response Das Response-Objekt
	 * @param applicationContext Der zu verwendende Spring {@link ApplicationContext}
	 */
	public TestContext(Session db, Request request, Response response, ApplicationContext applicationContext)
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
