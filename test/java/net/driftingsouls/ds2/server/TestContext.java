package net.driftingsouls.ds2.server;

import org.hibernate.Session;

import net.driftingsouls.ds2.server.framework.BasicContext;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;

public class TestContext extends BasicContext
{
	public TestContext(Session db, Request request, Response response)
	{
		super(request, response);
		this.db = db;
	}
	
	@Override
	public Database getDatabase() 
	{
		return new Database(db.connection());
	}
	
	@Override
	public Session getDB() 
	{
		return db;
	}
	
	private final Session db;
}
