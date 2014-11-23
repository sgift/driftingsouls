/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.framework.pipeline;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.configuration.PipelineConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.filter.GenericFilterBean;

/**
 * Der Standard-Listener fuer Servlet-Requests. Initalisiert die DS-Umgebung soweit wie moeglich.
 * @author Christopher Jung
 *
 */
public class DefaultServletRequestFilter extends GenericFilterBean implements Filter {
	private static final Log log = LogFactory.getLog(DefaultServletRequestFilter.class);
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		if( !(req instanceof HttpServletRequest) ) 
		{
			chain.doFilter(req, resp);
			log.error(this.getClass().getName()+" konnte Request nicht verarbeiten");
			return;
		}
		
		HttpServletRequest httpRequest = (HttpServletRequest)req;
		HttpServletResponse httpResponse = (HttpServletResponse)resp;
				
		Context context;
		try 
		{
			context = ContextMap.getContext();
			context.putVariable(HttpServlet.class, "response", httpResponse);
			context.putVariable(HttpServlet.class, "request", httpRequest);
			context.putVariable(HttpServlet.class, "context", this.getServletContext());
			context.putVariable(HttpServlet.class, "chain", chain);
			
			PipelineConfig config = context.getBean(PipelineConfig.class, "pipelineConfig");
			Pipeline pipeline = config.getPipelineForContext(context);
			
			if( pipeline != null ) 
			{
				pipeline.execute(context);
			}
			else 
			{
				throw new Exception("Unable to find a suitable rule for URL '"+context.getRequest().getRequestURL()+(context.getRequest().getQueryString() != null ? "?"+context.getRequest().getQueryString() : "")+"'");
			}
			
			context.getResponse().send();
		}
		catch( Throwable e ) 
		{
			throw new RuntimeException(e);
		}
	}
}
