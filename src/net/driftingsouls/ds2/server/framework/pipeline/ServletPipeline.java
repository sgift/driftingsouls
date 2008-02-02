/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
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

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import net.driftingsouls.ds2.server.framework.Context;

import org.w3c.dom.Node;

/**
 * Eine Pipeline die die Filterchain weiter abarbeitet (also nachgelagerte Servlets ausfuehrt)
 * @author Christopher Jung
 *
 */
public class ServletPipeline implements Pipeline {
	/**
	 * Konstruktor
	 */
	public ServletPipeline() {
		// EMPTY
	}

	public void execute(Context context) throws Exception {
		ServletRequest req = (ServletRequest)context.getVariable(HttpServlet.class, "request");
		ServletResponse resp = (ServletResponse)context.getVariable(HttpServlet.class, "response");
		FilterChain chain = (FilterChain)context.getVariable(HttpServlet.class, "chain");
		
		context.getResponse().setManualSendStatus();
		
		chain.doFilter(req, resp);
	}

	public void setConfiguration(Node node) {
		// EMPTY
	}
}
