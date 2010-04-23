package net.driftingsouls.ds2.server.framework;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import net.driftingsouls.ds2.server.framework.authentication.TickInProgressException;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;

/**
 * Filter, um alle Zugriffe auf DS waehrend des Ticks zu blockieren ausser auf das Portal.
 * 
 * @author Drifting-Souls Team
 */
public class TickFilter implements Filter 
{
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException 
	{
		org.hibernate.Session db = HibernateUtil.getSessionFactory().getCurrentSession();
		ConfigValue tick = (ConfigValue)db.get(ConfigValue.class, "tick");
	    int tickState = Integer.valueOf(tick.getValue());
	    boolean isTick = tickState == 1;
	    if(isTick)
	    {
	    	String module = request.getParameter("module");
	    	if(module != null && !module.equals("portal"))
	    	{
	    		throw new TickInProgressException();
	    	}
	    }
	    
	    chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig config) throws ServletException 
	{}
	
	@Override
	public void destroy() 
	{}
}
