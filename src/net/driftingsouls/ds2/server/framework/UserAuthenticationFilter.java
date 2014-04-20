package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.framework.authentication.AuthenticationManager;
import net.driftingsouls.ds2.server.framework.authentication.DefaultAuthenticationManager;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Checks, if the current user is authenticated.
 * PermanentSessions (remember-me function) are honored.
 * 
 * @author Drifting-Souls Team
 */
public class UserAuthenticationFilter extends SessionBasedFilter
{
	private AuthenticationManager manager = null;

	@Override
	public void destroy()
	{}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		boolean authenticatedUser = false;
        if(!isStaticRequest(request))
        {
            if(manager != null)
            {
				authenticatedUser = manager.authenticateCurrentSession();
            }
        }
        else
        {
            authenticatedUser = true;
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
	protected void initFilterBean() throws ServletException
	{
		this.manager = new DefaultAuthenticationManager();
	}
}
