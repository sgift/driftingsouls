package net.driftingsouls.ds2.server.tick;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.DSFilter;
import net.driftingsouls.ds2.server.framework.authentication.TickInProgressException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Filter, um alle Zugriffe auf DS waehrend des Ticks zu blockieren ausser auf das Portal.
 * 
 * @author Drifting-Souls Team
 */
public class TickFilter extends DSFilter
{
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException 
	{
        if(!isStaticRequest(request))
        {
          	int tickState = new ConfigService().getValue(WellKnownConfigValue.TICK);
            boolean isTick = tickState == 1;
            if(isTick)
            {
                String module = request.getParameter("module");
                if(module != null && !module.equals("portal"))
                {
                    throw new TickInProgressException();
                }
            }
        }
	    
	    chain.doFilter(request, response);
	}
	
	@Override
	public void destroy() 
	{}
}
