package net.driftingsouls.ds2.server.framework;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;

/**
 * Base filter for filters, which need to check user sessions.
 * 
 * @author Drifting-Souls Team
 */
public abstract class SessionBasedFilter implements Filter 
{
	@Override
	public void init(FilterConfig arg0) throws ServletException 
	{
		//TODO: Use different urls/servlets for modules which don't need a session.
		//Then we can set this filter for modules which need sessions and 
		//don't need to check the module here
		sessionFreeModules.add("portal");
		sessionFreeModules.add("news");
		sessionFreeModules.add("schiffinfo");
		sessionFreeModules.add("newsdetail");
	}
	
	protected boolean isSessionNeededByModule(ServletRequest request)
	{
		String module = request.getParameter("module");
		if(module != null && !sessionFreeModules.contains(module))
		{
			return true;
		}
		
		return false;
	}
	
	private Set<String> sessionFreeModules = new HashSet<String>();
}
