package net.driftingsouls.ds2.server.framework;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;

import javax.servlet.ServletRequest;
import java.util.HashSet;
import java.util.Set;

/**
 * Base filter for filters, which need to check user sessions.
 * 
 * @author Drifting-Souls Team
 */
public abstract class SessionBasedFilter extends DSFilter
{
	@Override
	protected void initBeanWrapper(BeanWrapper bw) throws BeansException
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
		return module != null && !sessionFreeModules.contains(module);

	}
	
	private Set<String> sessionFreeModules = new HashSet<>();
}
