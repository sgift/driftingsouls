package net.driftingsouls.ds2.server.framework;

import java.io.IOException;

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
public class UserAuthenticationFilter extends SessionBasedFilter
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
			if(isSessionNeededByModule(request))
			{
				throw new NotLoggedInException();
			}
			
			chain.doFilter(request, response);
		}
	}

	@Override
	public void init(FilterConfig config) throws ServletException 
	{
		super.init(config);
		this.manager = new DefaultAuthenticationManager();
	}
	
	private AuthenticationManager manager = null;
}
