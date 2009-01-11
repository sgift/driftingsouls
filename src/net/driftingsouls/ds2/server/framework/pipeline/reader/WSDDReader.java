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
package net.driftingsouls.ds2.server.framework.pipeline.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.ReaderPipeline;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.apache.axis.transport.http.AxisBridge;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.w3c.dom.Node;

/**
 * Liesst eine WSDD-Datei ein und startet auf ihrer Basis einen Axis-Server.
 * <p>Konfigurationsparameter:<br>
 * <code>base-dir</code> - Bestimmt das Basisverzeichnis der Webservices
 * </p>
 * @author Christopher Jung
 *
 */
@Configurable
public class WSDDReader implements Reader {
	private static final Log log = LogFactory.getLog(WSDDReader.class);
	
	private Configuration config;
	
    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
    }
	
	private static class HttpRequestWrapper implements HttpServletRequest {
		private HttpServletRequest req = null;
		private Pattern fakeServletPath = null;
		private int servletPathSplit = 0;
		
		HttpRequestWrapper(HttpServletRequest req, String fakeServletPath) {
			this.req = req;
			this.fakeServletPath = Pattern.compile(fakeServletPath);
			
			Matcher match = this.fakeServletPath.matcher(this.req.getServletPath());
			match.find();
			this.servletPathSplit = match.end();
		}

		public String getAuthType() {
			return req.getAuthType();
		}

		public String getContextPath() {
			return req.getContextPath();
		}

		public Cookie[] getCookies() {
			return req.getCookies();
		}

		public long getDateHeader(String arg0) {
			return req.getDateHeader(arg0);
		}

		public String getHeader(String arg0) {
			return req.getHeader(arg0);
		}

		public Enumeration<?> getHeaderNames() {
			return req.getHeaderNames();
		}

		public Enumeration<?> getHeaders(String arg0) {
			return req.getHeaders(arg0);
		}

		public int getIntHeader(String arg0) {
			return req.getIntHeader(arg0);
		}

		public String getMethod() {
			return req.getMethod();
		}

		public String getPathInfo() {
			return req.getServletPath().substring(this.servletPathSplit);
		}

		public String getPathTranslated() {
			return req.getPathTranslated();
		}

		public String getQueryString() {
			return req.getQueryString();
		}

		public String getRemoteUser() {
			return req.getRemoteUser();
		}

		public String getRequestURI() {
			return req.getRequestURI();
		}

		public StringBuffer getRequestURL() {
			return req.getRequestURL();
		}

		public String getRequestedSessionId() {
			return req.getRequestedSessionId();
		}

		public String getServletPath() {
			return req.getServletPath().substring(0, this.servletPathSplit);
		}

		public HttpSession getSession() {
			return req.getSession();
		}

		public HttpSession getSession(boolean arg0) {
			return req.getSession(arg0);
		}

		public Principal getUserPrincipal() {
			return req.getUserPrincipal();
		}

		public boolean isRequestedSessionIdFromCookie() {
			return req.isRequestedSessionIdFromCookie();
		}

		public boolean isRequestedSessionIdFromURL() {
			return req.isRequestedSessionIdFromURL();
		}

		@Deprecated
		public boolean isRequestedSessionIdFromUrl() {
			return req.isRequestedSessionIdFromUrl();
		}

		public boolean isRequestedSessionIdValid() {
			return req.isRequestedSessionIdValid();
		}

		public boolean isUserInRole(String arg0) {
			return req.isUserInRole(arg0);
		}

		public Object getAttribute(String arg0) {
			return req.getAttribute(arg0);
		}

		public Enumeration<?> getAttributeNames() {
			return req.getAttributeNames();
		}

		public String getCharacterEncoding() {
			return req.getCharacterEncoding();
		}

		public int getContentLength() {
			return req.getContentLength();
		}

		public String getContentType() {
			return req.getContentType();
		}

		public ServletInputStream getInputStream() throws IOException {
			return req.getInputStream();
		}

		public String getLocalAddr() {
			return req.getLocalAddr();
		}

		public String getLocalName() {
			return req.getLocalName();
		}

		public int getLocalPort() {
			return req.getLocalPort();
		}

		public Locale getLocale() {
			return req.getLocale();
		}

		public Enumeration<?> getLocales() {
			return req.getLocales();
		}

		public String getParameter(String arg0) {
			return req.getParameter(arg0);
		}

		public Map<?, ?> getParameterMap() {
			return req.getParameterMap();
		}

		public Enumeration<?> getParameterNames() {
			return req.getParameterNames();
		}

		public String[] getParameterValues(String arg0) {
			return req.getParameterValues(arg0);
		}

		public String getProtocol() {
			return req.getProtocol();
		}

		public BufferedReader getReader() throws IOException {
			return req.getReader();
		}

		@Deprecated
		public String getRealPath(String arg0) {
			return req.getRealPath(arg0);
		}

		public String getRemoteAddr() {
			return req.getRemoteAddr();
		}

		public String getRemoteHost() {
			return req.getRemoteHost();
		}

		public int getRemotePort() {
			return req.getRemotePort();
		}

		public RequestDispatcher getRequestDispatcher(String arg0) {
			return req.getRequestDispatcher(arg0);
		}

		public String getScheme() {
			return req.getScheme();
		}

		public String getServerName() {
			return req.getServerName();
		}

		public int getServerPort() {
			return req.getServerPort();
		}

		public boolean isSecure() {
			return req.isSecure();
		}

		public void removeAttribute(String arg0) {
			req.removeAttribute(arg0);
		}

		public void setAttribute(String arg0, Object arg1) {
			req.setAttribute(arg0, arg1);
		}

		public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
			req.setCharacterEncoding(arg0);
		}
	}
	
	private static class DummyServletConfig implements ServletConfig {
		private ServletContext context = null;
		
		DummyServletConfig(ServletContext context) {
			this.context = context;
		}
		
		public String getInitParameter(String arg0) {
			return null;
		}

		public Enumeration<?> getInitParameterNames() {
			return new Vector<Object>().elements();
		}

		public ServletContext getServletContext() {
			return this.context;
		}

		public String getServletName() {
			return "WSDDReader";
		}
		
	}
	
	private static Map<String, AxisBridge> server = new HashMap<String, AxisBridge>();
	
	private String fakeServletPath;
	
	private void readConfig(Node config) {		
		fakeServletPath = "/";
		if( config != null ) {
			try {
				String path = XMLUtils.getStringByXPath(config, "base-path/@value");
				if( path != null ) {
					fakeServletPath = path;
				}
			}
			catch( Exception e ) {
				log.warn("Kann Soap-Config-Eintrag 'base-path' nicht lesen",e);
			}
		}
	}
	
	@Override
	public void read(Context context, ReaderPipeline pipeline) throws Exception {
		String filename = pipeline.getFile();

		String path = config.get("ABSOLUTE_PATH") + filename;
		File file = new File(path);
		if( !file.exists() ) {
			context.getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
			log.warn("Warning: WebService Deployment Descriptor file not found: '" + file + "'");
			return;
		}
		
		readConfig(pipeline.getConfiguration());
		
		context.getResponse().setManualSendStatus();

		HttpServletRequest req = (HttpServletRequest)context.getVariable(HttpServlet.class,
				"request");
		HttpServletResponse res = (HttpServletResponse)context.getVariable(HttpServlet.class,
				"response");
		ServletConfig conf = new DummyServletConfig((ServletContext)context.getVariable(HttpServlet.class, 
				"context"));

		AxisBridge axis = null;
		synchronized(server) {
			if( !server.containsKey(file.getAbsolutePath()) ) {
				axis = new AxisBridge(conf, file.getAbsolutePath());
				axis.init();
				server.put(file.getAbsolutePath(), axis);
			}
			else {
				axis = server.get(file.getAbsolutePath());
			}
		}
		
		try {
			axis.service(new HttpRequestWrapper(req,fakeServletPath), res);
		}
		catch( Exception e ) {
			res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			throw e;
		}
	}
}
