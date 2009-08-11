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
import java.io.OutputStream;
import java.io.Writer;

import net.driftingsouls.ds2.server.framework.pipeline.Response;
import net.driftingsouls.ds2.server.framework.utils.StringBufferWriter;

/**
 * Eine einfache Response auf Basis von StringBuffer. Streams werden nicht unterstuetzt.
 * 
 * @author Christopher Jung
 * 
 */
public class SimpleResponse implements Response
{
	private StringBuffer buffer = new StringBuffer();

	/**
	 * Gibt den Inhalt des Ausgabepuffers zurueck.
	 * @return Der Inhalt des Ausgabepuffers
	 */
	public StringBuffer getContent()
	{
		return buffer;
	}

	@Override
	public OutputStream getOutputStream() throws IOException
	{
		throw new IOException("Kein Stream unterstuetzt");
	}

	@Override
	public void send() throws IOException
	{
		throw new IOException("Send not possible");
	}

	/**
	 * Setzt den Inhalt des Ausgabepuffers.
	 * @param content Der Inhalt
	 */
	public void setContent(String content)
	{
		buffer = new StringBuffer(content);
	}

	@Override
	public void setContentLength(int length)
	{
		// EMPTY
	}

	@Override
	public void setContentType(String contentType)
	{
		// EMPTY
	}

	@Override
	public void setHeader(String name, String value)
	{
		// EMPTY
	}

	@Override
	public void setStatus(int status)
	{
		// EMPTY
	}

	@Override
	public void setManualSendStatus()
	{
		// EMPTY
	}

	@Override
	public void redirectTo(String url)
	{
		// EMPTY
	}

	@Override
	public Writer getWriter()
	{
		return new StringBufferWriter(this.buffer);
	}

	@Override
	public void setContentType(String contentType, String charSet)
	{
		// EMPTY
	}

	@Override
	public void activateOutputCache()
	{
		// EMPTY
	}

	@Override
	public void setCookie(String name, String value, int expiry) 
	{
		// EMPTY
	}
}
