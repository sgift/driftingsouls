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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementiert das Request-Interface fuer HTTP-Requests.
 * @author Christopher Jung
 *
 */
public class HttpRequest implements Request {
	private static final Log log = LogFactory.getLog(HttpRequest.class);
	private HttpServletRequest request = null;
	private Map<String,String> parameters = new HashMap<String,String>();
	private boolean isMultipart = false;
	private List<?> uploadedFiles = null;
	
	/**
	 * Konstruktor.
	 * @param request Die Servlet-Request
	 */
	public HttpRequest(HttpServletRequest request) {
		this.request = request;
		
		if( request.getSession(false) == null ) {
			request.getSession(true);
		}
		
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
				log.error(e);
			}
		}
	}

	@Override
	public String getParameter(String parameter) {
		if( parameters.containsKey(parameter) ) {
			return parameters.get(parameter);
		}
		return request.getParameter(parameter);
	}

	@Override
	public String getContentType() {
		return request.getContentType();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return request.getInputStream();
	}

	@Override
	public String getQueryString() {
		return request.getQueryString();
	}

	@Override
	public String getPath() {
		if( request.getPathInfo() != null ) {
			return request.getServletPath()+request.getPathInfo();
		}
		return request.getServletPath();
	}

	@Override
	public String getCharacterEncoding() {
		return request.getCharacterEncoding();
	}

	@Override
	public int getContentLength() {
		return request.getContentLength();
	}

	@Override
	public void setParameter(String parameter, String value) {
		parameters.put(parameter, value);
	}
	
	@Override
	public String getHeader(String header) {
		return request.getHeader(header);
	}
	
	@Override
	public String getRemoteAddress() {
		return request.getRemoteAddr();
	}
	
	@Override
	public String getRequestURL() {
		return request.getRequestURL().toString();
	}
	
	@Override
	public String getUserAgent() {
		return request.getHeader("user-agent");
	}

	@Override
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

	@Override
	public String getParameterString(String parameter) {
		String str = getParameter(parameter);
		if( str == null ) {
			return "";
		}
		return str;
	}
	
	@Override
	public List<FileItem> getUploadedFiles() {
		if( !isMultipart ) {
			return new ArrayList<FileItem>();
		}
		
		List<FileItem> result = new ArrayList<FileItem>();
		List<?> items = uploadedFiles;
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
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getFromSession(Class<T> cls) {
		HttpSession session = this.request.getSession(false);
		
		if( session == null ) {
			return null;
		}
		Object obj = session.getAttribute(getClass().getName()+"#"+cls.getName());
		if( obj == null ) {
			try {
				Constructor<T> constr = cls.getConstructor();
				constr.setAccessible(true);
				obj = constr.newInstance();
			}
			catch( InstantiationException e ) {
				log.error("getFromSession for "+cls.getName()+" failed", e);
				return null;
			}
			catch( IllegalAccessException e ) {
				log.error("getFromSession for "+cls.getName()+" failed", e);
				return null;
			}
			catch( InvocationTargetException e ) {
				log.error("getFromSession for "+cls.getName()+" failed", e);
				return null;
			}
			catch( NoSuchMethodException e ) {
				log.error("getFromSession for "+cls.getName()+" failed", e);
				return null;
			}
			session.setAttribute(getClass().getName()+"#"+cls.getName(), obj);
		}
		if( cls.isInstance(obj) ) {
			return (T)obj;
		}
		log.error("getFromSession for "+cls.getName()+" failed - invalid type");
		
		return null;
	}
	
	@Override
	public void removeFromSession(Class<?> cls) {
		HttpSession session = this.request.getSession(false);
		
		if( session == null ) {
			return;
		}
		session.removeAttribute(getClass().getName()+"#"+cls.getName());
	}
	
	
	@Override
	public String getCookie(String name) 
	{
		if(request != null)
		{
			Cookie[] cookies = request.getCookies();
			if(cookies != null)
			{
				for(Cookie cookie: cookies)
				{
					if(cookie.getName().equals(name))
					{
						return cookie.getValue();
					}
				}
			}
		}
		
		return null;
	}
}
