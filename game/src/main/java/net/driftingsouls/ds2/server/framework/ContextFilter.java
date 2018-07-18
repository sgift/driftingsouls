package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.framework.pipeline.HttpRequest;
import net.driftingsouls.ds2.server.framework.pipeline.HttpResponse;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter to initialize the drifting-souls context.
 * This filter should be called before any ds specific action happens, as most of
 * them use the context.
 * Note: This filter does <strong>NOT</strong> fully configure the context.
 * Especially chain, request and response are not set here but in DefaultServletRequestFilter.
 * Still, the context set here can be used to access the database and perform checks,
 * before a page is shown or an action is executed.
 *
 * @author Drifting-Souls Team
 */
public class ContextFilter extends DSFilter
{
	@Override
	public void destroy()
	{}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		BasicContext context = null;
		try
		{
			WebApplicationContext springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext());

			HttpServletRequest httpRequest = (HttpServletRequest)request;
			HttpServletResponse httpResponse = (HttpServletResponse)response;
			Request dsRequest = new HttpRequest(httpRequest);
			Response dsResponse = new HttpResponse(httpRequest, httpResponse);
			context = new BasicContext(dsRequest, dsResponse, new EmptyPermissionResolver(), springContext);
			ContextMap.addContext(context);

			chain.doFilter(request, response);
		}
		finally
		{
			if(context != null)
			{
				context.free();
			}
		}
	}
}
