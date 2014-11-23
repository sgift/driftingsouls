/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server.framework.utils;

import org.jetbrains.annotations.NotNull;

import java.io.Writer;

/**
 * Adapter von einem StringBuffer auf einen Writer.
 * @author Christopher Jung
 *
 */
public final class StringBufferWriter extends Writer {
	private StringBuffer buffer;
	
	/**
	 * Konstruktor.
	 * @param buffer Der zu wrappende StringBuffer 
	 */
	public StringBufferWriter(StringBuffer buffer) {
		this.buffer = buffer;
	}
	
	@Override
	public void close()
	{
		// EMPTY
	}

	@Override
	public void flush()
	{
		// EMPTY
	}

	@Override
	public void write(@NotNull char[] cbuf, int off, int len)
	{
		this.buffer.append(cbuf, off, len);
	}
}
