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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

/**
 * Implementiert das Response-Interface fuer HTTP-Antworten
 * @author Christopher Jung
 *
 */
public class HttpResponse implements Response {
	private String contentType;
	private StringBuffer content;
	private String charSet;
	private int contentLength;
	private HttpServletResponse response;
	private boolean send;
	private boolean manualSend = false;
	
	/**
	 * Konstruktor
	 * @param response Die HttpServletResponse, welche die gesendeten Daten erhalten soll
	 */
	public HttpResponse(HttpServletResponse response) {
		contentType = "text/html";
		charSet = "UTF-8";
		content = new StringBuffer(500);
		contentLength = 0;
		this.response = response;
		send = false;
	}

	/**
	 * @return Returns the content.
	 */
	public StringBuffer getContent() {
		return content;
	}

	public void resetContent() {
		this.content = new StringBuffer(500);
	}

	/**
	 * @return Returns the contentType.
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * @param contentType The contentType to set.
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * @return Returns the charSet.
	 */
	public String getCharSet() {
		return charSet;
	}

	/**
	 * @param charSet The charSet to set.
	 */
	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}

	public void setContentLength(int length) {
		contentLength = length;
	}

	public OutputStream getOutputStream() throws IOException {
		if( contentLength != 0 ) {
			response.setContentLength(contentLength);
		}
		response.setHeader("Content-Type", contentType+"; charset="+charSet);

		return response.getOutputStream();
	}

	public void setStatus(int status) {
		response.setStatus(status);
	}

	public void send() throws IOException {
		if( !manualSend ) {
			if( !send ) {
				if( contentLength != 0 ) {
					response.setContentLength(contentLength);
				}
				response.setHeader("Content-Type", contentType+"; charset="+charSet);
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
				
				send = true;
			}
			else {
				throw new IOException("Response already sent");
			}
		}
	}
	
	private static final Pattern[] URL_PATTERNS = new Pattern[] {
		Pattern.compile("href\\=\"([^\"]*)\""),
		Pattern.compile("src\\=\"([^\"]*)\""),
		Pattern.compile("action\\=\"([^\"]*)\"")
	};
	
	private String prepareContentForSend() {
		String str = this.content.toString();
		
		for( int i=0; i < URL_PATTERNS.length; i++ ) {
			str = encodeUrlsWithPattern(str, URL_PATTERNS[i]);
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

	public void setContent(String content) {
		this.content = new StringBuffer(content);
	}

	public void setHeader(String name, String value) {
		response.setHeader(name, value);
	}

	public void setManualSendStatus() {
		this.manualSend = true;
	}
}
