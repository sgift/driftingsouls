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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;

import net.driftingsouls.ds2.server.framework.pipeline.Request;

/**
 * Repraesentiert eine Request auf Basis von Kommandozeilenparametern
 * @author Christopher Jung
 *
 */
public class CmdLineRequest implements Request {
	private Map<String,String> params = new HashMap<String,String>();
	
	/**
	 * Erstellt ein neues Request-Objekt
	 * @param args Die Kommandozeilenparameter
	 */
	public CmdLineRequest(String[] args) {
		for( int i=0; i < args.length-1; i+=2 ) {
			if( args[i].startsWith("--") ) {
				params.put(args[i].substring(2), args[i+1]);
			}
		}
	}
	
	public String getCharacterEncoding() {
		return "UTF-8";
	}

	public int getContentLength() {
		return 0;
	}

	public String getContentType() {
		return "text";
	}

	public String getHeader(String header) {
		return null;
	}

	public InputStream getInputStream() throws IOException {
		return null;
	}

	public String getParameter(String parameter) {
		return params.get(parameter);
	}

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

	public String getParameterString(String parameter) {
		String param = params.get(parameter);
		if( param == null ) {
			return "";
		}
		return param;
	}

	public String getPath() {
		return System.getProperty("user.dir");
	}

	public String getQueryString() {
		return "";
	}

	public String getRemoteAddress() {
		return "localhost";
	}

	public String getRequestURL() {
		return "./java";
	}

	public String getUserAgent() {
		return "Command Line";
	}

	public void setParameter(String parameter, String value) {
		params.put(parameter, value);
	}

	public List<FileItem> getUploadedFiles() {
		return new ArrayList<FileItem>();
	}

}
