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
import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.driftingsouls.ds2.server.framework.BasicContext;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.pipeline.configuration.PipelineConfig;

/**
 * Der Standard-Listener fuer Servlet-Requests. Initalisiert die DS-Umgebung soweit wie moeglich.
 * @author Christopher Jung
 *
 */
public class DefaultServletRequestFilter implements Filter, Loggable {
	private ServletContext context;
	
	public void destroy() {
		// EMPTY
	}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		if( !(req instanceof HttpServletRequest) ) {
			chain.doFilter(req, resp);
			LOG.error(this.getClass().getName()+" konnte Request nicht verarbeiten");
			return;
		}
		
		HttpServletRequest httpRequest = (HttpServletRequest)req;
		HttpServletResponse httpResponse = (HttpServletResponse)resp;
		
		try {
			// Refferer verstecken, um Abgreifen der sessid zu vermeiden
			httpResponse.setHeader("Referer", "http://ds.drifting-souls.net");
			
			Request request = new HttpRequest(httpRequest);
			Response response = new HttpResponse(httpResponse);
			
			BasicContext context = null;
			
			try {
				context = new BasicContext(request, response);
				context.putVariable(HttpServlet.class, "response", httpResponse);
				context.putVariable(HttpServlet.class, "request", httpRequest);
				context.putVariable(HttpServlet.class, "context", this.context);
				context.putVariable(HttpServlet.class, "chain", chain);
				
				context.revalidate();
				
				try {
					Pipeline pipeline = PipelineConfig.getPipelineForContext(context);
					
					if( pipeline != null ) {
						pipeline.execute(context);
					}
					else {
						throw new Exception("Unable to find a suitable rule for URL '"+context.getRequest().getRequestURL()+(context.getRequest().getQueryString() != null ? "?"+context.getRequest().getQueryString() : "")+"'");
					}
					
					context.getResponse().send();
				}
				catch( Throwable e ) {
					context.rollback();
					
					mailThrowable(httpRequest, context, e);
					
					e.printStackTrace();
					httpResponse.setContentType("text/html");
					PrintWriter writer = httpResponse.getWriter();
					writer.append("<html><head><title>Drifting Souls Server Framework</title></head>");
					writer.append("<body>");
					writer.append("<table border=\"0\"><tr><td>\n");
					writer.append("<div align=\"center\">\n");
					writer.append("<h1>Drifting Souls Server Framework</h1>");
					writer.append("Unhandled Exception "+e.getClass().getName()+" during pipeline execution detected<br />\n");
					writer.append("Reason: "+e.getMessage()+"</div>\n");
					writer.append("<hr style=\"height:1px; border:0px; background-color:#606060; color:#606060\" />");
					StackTraceElement[] st = e.getStackTrace();
					for( int i=0; i < st.length; i++ ) {
						writer.append(st[i].toString()+"<br />\n");
					}
					writer.append("</td></tr></table></body></html>");
				}
			}
			finally {
				if( context != null ) {
					context.free();
				}
			}
		}
		catch( Throwable e ) {
			mailThrowable(httpRequest, null, e);
		}
	}

	private void mailThrowable(HttpServletRequest httpRequest, BasicContext context, Throwable t) {
		StringBuilder msg = new StringBuilder(100);
		msg.append("Time: "+new Date()+"\n");
		msg.append("URI: "+httpRequest.getRequestURI()+"\n");
		msg.append("PARAMS:\n");
		for( Enumeration e=httpRequest.getParameterNames(); e.hasMoreElements(); ) {
			String key = (String)e.nextElement();
			msg.append("\t* "+key+" = "+httpRequest.getParameter(key)+"\n");
		}
		
		msg.append("QUERY_STRING: "+httpRequest.getQueryString()+"\n");
		msg.append("Session: "+httpRequest.getParameter("sess")+"\n");
		
		if( context != null ) {
			msg.append("User: "+(context.getActiveUser() != null ? context.getActiveUser().getId() : "none")+"\n");
		}
		
		Common.mailThrowable(t, (context == null ? "Fatal " : "")+"Framework Exception", msg.toString());
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		context = filterConfig.getServletContext();
		
		// Pipeline lesen
		LOG.info("Reading "+Configuration.getSetting("configdir")+"pipeline.xml");
		try {
			PipelineConfig.readConfiguration();
		}
		catch( Exception e ) {
			LOG.fatal(e, e);
			throw new ServletException(e);
		}
	}
}
