package net.driftingsouls.ds2.server.framework;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import net.driftingsouls.ds2.server.framework.authentication.AuthenticationManager;
import net.driftingsouls.ds2.server.framework.authentication.DefaultAuthenticationManager;

/**
 * Checks, if the current user is authenticated.
 * PermanentSessions (remember-me function) are honored.
 * 
 * @author Drifting-Souls Team
 */
public class UserAuthenticationFilter implements Filter 
{
	@Override
	public void destroy() 
	{}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException 
	{
		boolean authenticatedUser = false;
		if(manager != null)
		{
			String automaticAccessParameter = request.getParameter("autoAccess");
			if(automaticAccessParameter != null && automaticAccessParameter.equals("true"))
			{
				authenticatedUser = manager.authenticateCurrentSession(true);
			}
			else
			{
				authenticatedUser = manager.authenticateCurrentSession(false);
			}
		}
		
		if(authenticatedUser)
		{
			chain.doFilter(request, response);
		}
		else
		{
			String module = request.getParameter("module");
			if(module != null && !sessionFreeModules.contains(module))
			{
				throw new NotLoggedInException();
			}
			
			chain.doFilter(request, response);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException 
	{
		this.manager = new DefaultAuthenticationManager();
		//TODO: Use different urls/servlets for modules which don't need a session.
		//Then we can set this filter for modules which need sessions and 
		//don't need to check the module here
		sessionFreeModules.add("portal");
		sessionFreeModules.add("news");
		sessionFreeModules.add("schiffinfo");
		sessionFreeModules.add("newsdetail");
	}
	
	private AuthenticationManager manager = null;
	private Set<String> sessionFreeModules = new HashSet<String>();
}
