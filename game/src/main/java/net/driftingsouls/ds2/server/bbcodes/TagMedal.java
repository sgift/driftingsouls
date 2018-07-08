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

import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeFunction;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * BBCode fuer Orden.
 *
 * @author Christopher Jung
 */
public class TagMedal implements BBCodeFunction
{
	@Override
	public String handleMatch(String content, String... values)
	{
		if (!NumberUtils.isDigits(content))
		{
			return "Unbekannter Orden (" + content + ")";
		}

		Medal medal = Medals.get().medal(Integer.parseInt(content));

		if (medal == null)
		{

			return "Unbekannter Orden (" + content + ")";
		}

		return "<img src=\""+medal.getImageSmall()+"\" alt=\"\"/>"+medal.getName();
	}

}
