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
	 * Gibt den Inhalt des Ausgabepuffers zurueck
	 * @return Der Inhalt des Ausgabepuffers
	 */
	public StringBuffer getContent()
	{
		return buffer;
	}

	public OutputStream getOutputStream() throws IOException
	{
		throw new IOException("Kein Stream unterstuetzt");
	}

	public void send() throws IOException
	{
		throw new IOException("Send not possible");
	}

	/**
	 * Setzt den Inhalt des Ausgabepuffers
	 * @param content Der Inhalt
	 */
	public void setContent(String content)
	{
		buffer = new StringBuffer(content);
	}

	public void setContentLength(int length)
	{
		// EMPTY
	}

	public void setContentType(String contentType)
	{
		// EMPTY
	}

	public void setHeader(String name, String value)
	{
		// EMPTY
	}

	public void setStatus(int status)
	{
		// EMPTY
	}

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
}
