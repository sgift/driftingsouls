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

import net.driftingsouls.ds2.server.framework.utils.StringBufferWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementiert das Response-Interface fuer HTTP-Antworten.
 * @author Christopher Jung
 *
 */
public class HttpResponse implements Response {
	private Log log = LogFactory.getLog(HttpResponse.class);
	
	private String charSet;
	private StringBuffer content;
	private Writer writer;
	private HttpServletResponse response;
	private HttpServletRequest request;
	private boolean send;
	private boolean manualSend = false;
	private boolean cacheOutput = false;
	
	/**
	 * Konstruktor.
	 * @param request Die HttpServletRequest
	 * @param response Die HttpServletResponse, welche die gesendeten Daten erhalten soll
	 */
	public HttpResponse(HttpServletRequest request, HttpServletResponse response) {
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		this.charSet = "UTF-8";
		this.cacheOutput = request.isRequestedSessionIdFromURL();
		if( this.cacheOutput ) {
			this.content = new StringBuffer(500);
		}
		this.request = request;
		this.response = response;
		this.send = false;
	}

	@Override
	public void setContentType(String contentType) {
		response.setContentType(contentType);
	}

	@Override
	public void setContentType(String contentType, String charSet) {
		this.charSet = charSet;
		response.setContentType(contentType);
		response.setCharacterEncoding(charSet);
	}

	@Override
	public void setContentLength(int length) {
		response.setContentLength(length);
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return response.getOutputStream();
	}

	@Override
	public void setStatus(int status) {
		response.setStatus(status);
	}

	@Override
	public void send() throws IOException {
		if( !manualSend ) {
			if( !send ) {
				if( this.cacheOutput ) {
					try {
						OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), charSet);
						if( content.length() > 0 ) {
							writer.append(prepareContentForSend());
						}
						writer.flush();
						
						writer.close();
					}
					catch( IOException e ) {
						// Ignorieren, da es sich vmtl um einen Browser handelt, der
						// die Leitung zu frueh dicht gemacht hat
					}
				}
				else if( this.writer != null ) {
					this.writer.flush();
					this.writer.close();
				}
				
				send = true;
			}
			else {
				throw new IOException("Response already sent");
			}
		}
	}
	
	private static final Pattern[] URL_PATTERNS = new Pattern[] {
		Pattern.compile("href=\"([^\"]*)\""),
		Pattern.compile("src=\"([^\"]*)\""),
		Pattern.compile("action=\"([^\"]*)\"")
	};
	
	private String prepareContentForSend() {
		String str = this.content.toString();
		
		if( this.request.isRequestedSessionIdFromCookie()) {
			return str;
		}

		for (Pattern URL_PATTERN : URL_PATTERNS)
		{
			str = encodeUrlsWithPattern(str, URL_PATTERN);
		}
		
		return str;
	}

	private String encodeUrlsWithPattern(String str, Pattern pattern) {
		Matcher matcher = pattern.matcher(str);
		
		int offset = 0;
		while( matcher.find() ) {
			String group = matcher.group(1);
			
			if( !group.equals("#") && !group.startsWith("http://") && 
					!group.startsWith("javascript") && !group.contains("(") && 
					!group.contains(")") ) {
				int oldlength = str.length();
				str = str.substring(0,matcher.start(1)+offset)+
					this.response.encodeURL(group)+
					str.substring(matcher.end(1)+offset);
				
				offset += str.length() - oldlength;
			}
		}
		return str;
	}

	@Override
	public void setHeader(String name, String value) {
		response.setHeader(name, value);
	}

	@Override
	public void setManualSendStatus() {
		this.manualSend = true;
	}
	
	@Override
	public void redirectTo(String url) {
		this.response.setStatus(HttpServletResponse.SC_FOUND);
		try
		{
			this.response.sendRedirect(this.response.encodeRedirectURL(url));
		}
		catch( IOException e )
		{
			log.error("Redirect konnte nicht durchgefuehrt werden", e);
		}
		
		this.manualSend = true;
	}

	@Override
	public Writer getWriter() throws IOException
	{
		if( !this.cacheOutput ) {
			if( this.writer == null ) {
				this.writer = this.response.getWriter();
			}
			return this.writer;
		}
		return new StringBufferWriter(this.content);
	}

	@Override
	public void activateOutputCache() throws IllegalStateException
	{
		if( this.writer != null ) {
			throw new IllegalStateException("Ausgabe bereits geschrieben");
		}
		
		this.cacheOutput = true;
		if( this.content == null ) {
			this.content = new StringBuffer(500);
		}
	}
	
	@Override
	public void setCookie(String name, String value, int expiry) 
	{
		Cookie cookie = new Cookie(name, value);
		cookie.setMaxAge(expiry);
		response.addCookie(cookie);
	}
}
