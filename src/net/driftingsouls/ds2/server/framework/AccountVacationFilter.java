package net.driftingsouls.ds2.server.framework;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.user.authentication.AccountInVacationModeException;

/**
 * Filter to block account access, if the user is in vacation.
 * Administration accounts are not blocked.
 * 
 * @author Drifting-Souls Team
 */
public class AccountVacationFilter extends SessionBasedFilter
{
	@Override
	public void destroy() 
	{}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException 
	{
		User user = (User)ContextMap.getContext().getActiveUser();
		if(!isStaticRequest(request) && isSessionNeededByModule(request))
		{
			if(!user.isAdmin())
			{
				if(user.isInVacation())
				{
					throw new AccountInVacationModeException(user.getVacationCount());
				}
			}
		}
		
		chain.doFilter(request, response);
	}
}
