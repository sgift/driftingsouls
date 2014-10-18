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

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nonnull;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementiert das Request-Interface fuer HTTP-Requests.
 * @author Christopher Jung
 *
 */
public class HttpRequest implements Request {
	private static final Log log = LogFactory.getLog(HttpRequest.class);
	private HttpServletRequest request = null;
	private Map<String,String[]> parameters = new HashMap<>();
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
			parseMultipartRequest(request);
		}
		else
		{
			this.parameters.putAll(request.getParameterMap());
		}
	}

	private void parseMultipartRequest(HttpServletRequest request)
	{
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);

		try {
			uploadedFiles = upload.parseRequest(request);
			for (Object uploadedFile : uploadedFiles)
			{
				FileItem item = (FileItem) uploadedFile;
				if (!item.isFormField())
				{
					continue;
				}
				String[] vals = parameters.get(item.getFieldName());
				if( vals != null )
				{
					String[] newVals = new String[vals.length+1];
					System.arraycopy(vals, 0, newVals, 0, vals.length);
					newVals[newVals.length-1] = item.getString("UTF-8");
					parameters.put(item.getFieldName(), newVals);
				}
				else
				{
					parameters.put(item.getFieldName(), new String[]{item.getString("UTF-8")});
				}
			}
		}
		catch( FileUploadException | UnsupportedEncodingException e ) {
			log.error(e);
		}
	}

	@Nonnull
	@Override
	public String[] getParameterValues(@Nonnull String parameter)
	{
		String[] values = this.parameters.get(parameter);
		if( values == null )
		{
			return new String[0];
		}
		return values.clone();
	}

	@Override
	public String getParameter(String parameter) {
		String[] vals = parameters.get(parameter);
		if( vals == null || vals.length == 0 )
		{
			return null;
		}
		return vals[0];
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
		parameters.put(parameter, new String[] {value});
	}

	@Override
	public String getHeader(String header) {
		return request.getHeader(header);
	}

	@Override
	public String getRemoteAddress() {
		if( request.getHeader("x-forwarded-for") != null )
		{
			return request.getHeader("x-forwarded-for");
		}
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
	public Map<String,String> getParameterMap()
	{
		return this.parameters.entrySet().stream()
				.filter((e) -> e.getValue() != null && e.getValue().length > 0)
				.collect(Collectors.toMap(Map.Entry<String, String[]>::getKey, (e) -> e.getValue()[0]));
	}

	@Override
	public List<FileItem> getUploadedFiles() {
		if( !isMultipart ) {
			return new ArrayList<>();
		}

		List<FileItem> result = new ArrayList<>();
		List<?> items = uploadedFiles;
		for (Object item1 : items)
		{
			if (item1 instanceof FileItem)
			{
				FileItem item = (FileItem) item1;
				if (item.isFormField())
				{
					continue;
				}
				result.add((FileItem) item1);
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
			catch( ReflectiveOperationException e ) {
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
