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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.driftingsouls.ds2.server.framework.Loggable;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Implementiert das Request-Interface fuer HTTP-Requests
 * @author Christopher Jung
 *
 */
public class HttpRequest implements Request,Loggable {
	private HttpServletRequest request = null;
	private Map<String,String> parameters = new HashMap<String,String>();
	private boolean isMultipart = false;
	private List uploadedFiles = null;
	
	/**
	 * Konstruktor
	 * @param request Die Servlet-Request
	 */
	public HttpRequest(HttpServletRequest request) {
		this.request = request;
		
		// Standard-Encoding ist UTF-8
		if( request.getCharacterEncoding() == null ) {
			try {
				request.setCharacterEncoding("UTF-8");
			}
			catch( UnsupportedEncodingException e1 ) {
				e1.printStackTrace();
			}
		}
		
		isMultipart = ServletFileUpload.isMultipartContent(request);
		if( isMultipart ) {
			FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);

			try {
				uploadedFiles = upload.parseRequest(request);
				for( int i=0; i < uploadedFiles.size(); i++ ) {
					FileItem item = (FileItem)uploadedFiles.get(i);
					if( !item.isFormField() ) {
						continue;
					}
				    parameters.put(item.getFieldName(), item.getString());
				}
			}
			catch( FileUploadException e ) {
				LOG.error(e);
			}
		}
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
		if( request.getPathInfo() != null ) {
			return request.getServletPath()+request.getPathInfo();
		}
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

	public List<FileItem> getUploadedFiles() {
		if( !isMultipart ) {
			return new ArrayList<FileItem>();
		}
		
		List<FileItem> result = new ArrayList<FileItem>();
		List items = uploadedFiles;
		for( int i=0; i < items.size(); i++ ) {
			if( items.get(i) instanceof FileItem ) {
				FileItem item = (FileItem)items.get(i);
				if( item.isFormField() ) {
					continue;
				}
				result.add((FileItem)items.get(i));
			}
		}
			
		return result;
	}
}
