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
package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.framework.pipeline.Request;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repraesentiert eine Request auf Basis von Kommandozeilenparametern.
 * @author Christopher Jung
 *
 */
public class CmdLineRequest implements Request {
	private Log log = LogFactory.getLog(CmdLineRequest.class);
	
	private Map<String,String> params = new HashMap<>();
	
	/**
	 * Erstellt ein neues Request-Objekt.
	 * @param args Die Kommandozeilenparameter
	 */
	public CmdLineRequest(String[] args) {
		for( int i=0; i < args.length; i++ ) {
			if( args[i].startsWith("--") ) {
				String arg = args[i].substring(2);
				String value = "true";
				
				if( (i < args.length - 1) && !args[i+1].startsWith("--") ) {
					value = args[i+1];
					i++;
				}
				params.put(arg, value);
			}
		}
	}
	
	@Override
	public String getCharacterEncoding() {
		return "UTF-8";
	}

	@Override
	public int getContentLength() {
		return 0;
	}

	@Override
	public String getContentType() {
		return "text";
	}

	@Override
	public String getHeader(String header) {
		return null;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Nonnull
	@Override
	public String[] getParameterValues(@Nonnull String parameter)
	{
		String val = params.get(parameter);
		if( val == null )
		{
			return new String[0];
		}
		return new String[] {val};
	}

	@Override
	public String getParameter(String parameter) {
		return params.get(parameter);
	}

	@Override
	public int getParameterInt(String parameter) {
		String param = params.get(parameter);
		if( param == null ) {
			return 0;
		}
		try {
			return Integer.parseInt(param);
		}
		catch( NumberFormatException e ) {
			// EMPTY
		}
		return 0;
	}

	@Override
	public String getParameterString(String parameter) {
		String param = params.get(parameter);
		if( param == null ) {
			return "";
		}
		return param;
	}
	
	@Override
	public String getPath() {
		return System.getProperty("user.dir");
	}

	@Override
	public String getQueryString() {
		return "";
	}

	@Override
	public String getRemoteAddress() {
		return "localhost";
	}

	@Override
	public String getRequestURL() {
		return "./java";
	}

	@Override
	public String getUserAgent() {
		return "Command Line";
	}

	@Override
	public void setParameter(String parameter, String value) {
		params.put(parameter, value);
	}

	@Override
	public List<FileItem> getUploadedFiles() {
		return new ArrayList<>();
	}

	@Override
	public <T> T getFromSession(Class<T> cls) {
		log.error("getFromSession not supported");
		
		return null;
	}

	@Override
	public void removeFromSession(Class<?> cls) {
		log.error("removeFromSession not supported");
	}

	@Override
	public String getCookie(String name) {
		return null;
	}

	@Override
	public Map<String, String> getParameterMap()
	{
		return Collections.unmodifiableMap(this.params);
	}
}
