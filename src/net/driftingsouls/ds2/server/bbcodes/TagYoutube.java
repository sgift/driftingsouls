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
package net.driftingsouls.ds2.server.bbcodes;

import net.driftingsouls.ds2.server.framework.bbcode.BBCodeFunction;
import org.apache.commons.lang.math.NumberUtils;

/**
 * BBCode fuer Youtube-Videos.
 *
 * @author Christopher Jung
 */
public class TagYoutube implements BBCodeFunction
{
	@Override
	public String handleMatch(String content, String... values)
	{
		try {
			if( content == null || !content.matches("[A-Za-z0-9]+") )
			{
				return "[Ungueltiger Youtube-Videocode]";
			}
			if( values.length != 2 || !NumberUtils.isDigits(values[0]) || !NumberUtils.isDigits(values[1]) )
			{
				return "[Ungueltige Groessenangaben]";
			}

			return "<iframe width=\""+values[0]+"\" height=\""+values[1]+"\" src=\"//www.youtube.com/embed/"+content+"\" frameborder=\"0\" allowfullscreen></iframe>";
		}
		catch( RuntimeException e ) {
			return content;
		}
	}
}
