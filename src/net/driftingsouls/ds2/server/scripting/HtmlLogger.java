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

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;

/**
 * ScriptEngine-Logger, welcher die Ausgabe als HTML formatiert.
 * @author Christopher Jung
 *
 */
public class HtmlLogger extends Writer {
	private boolean first = true;

	@Override
	public void close() throws IOException {
		Context context = ContextMap.getContext();
		if( context == null ) {
			return;
		}
		Writer out = context.getResponse().getWriter();
		out.append("</span>\n");
		out.append("</div>");
		out.append("<br />\n");
		
		first = true;
	}

	@Override
	public void flush() {
		// EMPTY
	}

	@Override
	public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
		Context context = ContextMap.getContext();
		if( context == null ) {
			return;
		}
		
		if( first ) {
			Writer out = context.getResponse().getWriter();
			out.append("<div class='gfxbox' style='width:540px'>");
			out.append("<div align=\"center\">Scriptengine [Debug]</div><br />");
			out.append("<span style=\"font-size:11px\">\n");
			
			first = false;
		}
		
		Writer out = context.getResponse().getWriter();
		out.append(StringUtils.replace(new String(cbuf, off, len), "\n", "<br />"));
	}
}
