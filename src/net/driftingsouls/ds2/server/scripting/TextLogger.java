/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.scripting;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;

/**
 * Logger, welcher die Ausgabe in die Standardausgabe umleitet.
 * @author Christopher Jung
 *
 */
public class TextLogger extends Writer {
	private boolean first = true;
	
	@Override
	public void close() throws IOException {
		System.out.print("#########################ENDE#############################\n");
		
		first = true;
	}

	@Override
	public void flush() {
		// EMPTY
	}

	@Override
	public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
		if( first ) {
			System.out.print("###################Scriptengine [Debug]###################\n");
			first = false;
		}
		System.out.print(new String(cbuf, off, len));
	}
}
