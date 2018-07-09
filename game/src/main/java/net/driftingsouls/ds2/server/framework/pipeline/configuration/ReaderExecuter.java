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
package net.driftingsouls.ds2.server.framework.pipeline.configuration;

import java.util.regex.Pattern;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Pipeline;
import net.driftingsouls.ds2.server.framework.pipeline.ReaderPipeline;
import net.driftingsouls.ds2.server.framework.pipeline.reader.Reader;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.w3c.dom.Node;

final class ReaderExecuter implements Executer
{
	private final Class<? extends Reader> readerClass;
	private final String file;
	private final Pattern pattern;

	ReaderExecuter(Node node) throws Exception
	{
		readerClass = Class.forName(XMLUtils.getStringByXPath(node, "reader/@class"))
			.asSubclass(Reader.class);
		file = XMLUtils.getStringByXPath(node, "file/@file");
		String pattern = XMLUtils.getStringByXPath(node, "file/@pattern");

		if( (pattern != null) && !"".equals(pattern.trim()) ) {
			this.pattern = Pattern.compile(pattern);
		}
		else {
			this.pattern = null;
		}
	}

	@Override
	public Pipeline execute(Context context) throws Exception
	{
		String file = this.file;
		if( pattern != null ) {
			file = pattern.matcher(context.getRequest().getPath()).replaceFirst(file);
		}

		return new ReaderPipeline(readerClass, file);
	}

}
