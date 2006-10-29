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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Implementiert das Request-Interface fuer HTTP-Requests
 * @author Christopher Jung
 *
 */
public class HttpRequest implements Request {
	private HttpServletRequest request = null;
	private Map<String,String> parameters = new HashMap<String,String>();
	
	public HttpRequest(HttpServletRequest request) {
		this.request = request;
	}

	public String getParameter(String parameter) {
		if( parameters.containsKey(parameter) ) {
			return parameters.get(parameter);
		}
		return request.getParameter(parameter);
	}

	public String getContentType() {
		return request.getContentType();
	}

	public InputStream getInputStream() throws IOException {
		return request.getInputStream();
	}

	public String getQueryString() {
		return request.getQueryString();
	}

	public String getPath() {
		return request.getServletPath();
	}

	public String getCharacterEncoding() {
		return request.getCharacterEncoding();
	}

	public int getContentLength() {
		return request.getContentLength();
	}

	public void setParameter(String parameter, String value) {
		parameters.put(parameter, value);
	}
	
	public String getHeader(String header) {
		return request.getHeader(header);
	}
	
	public String getRemoteAddress() {
		return request.getRemoteAddr();
	}
	
	public String getRequestURL() {
		return request.getRequestURL().toString();
	}
	
	public String getUserAgent() {
		return request.getHeader("user-agent");
	}

	public int getParameterInt(String parameter) {
		String str = getParameter(parameter);
		if( str == null || str.equals("") ) {
			return 0;
		}
		try {
			return Integer.parseInt(str);
		}
		catch( NumberFormatException e ) {
			return 0;
		}
	}

	public String getParameterString(String parameter) {
		String str = getParameter(parameter);
		if( str == null ) {
			return "";
		}
		return str;
	}
}
