package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.framework.BasicContext;
import net.driftingsouls.ds2.server.framework.EmptyPermissionResolver;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;
import org.hibernate.Session;

public class TestContext extends BasicContext
{
	public TestContext(Session db, Request request, Response response)
	{
		super(request, response, new EmptyPermissionResolver());
		this.db = db;
	}

	@Override
	public Session getDB()
	{
		return db;
	}

	private final Session db;
}
