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

import net.driftingsouls.ds2.server.services.MedalService;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeFunction;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * BBCode fuer Raenge.
 *
 * @author Christopher Jung
 */
public class TagRang implements BBCodeFunction
{
	private final MedalService medalService;

	public TagRang(MedalService medalService) {
		this.medalService = medalService;
	}

	@Override
	public String handleMatch(String content, String... values)
	{
		if (!NumberUtils.isDigits(content))
		{
			return "Unbekannter Rang (" + content + ")";
		}

		Rang rang = medalService.rang(Integer.parseInt(content));

		if (rang == null)
		{

			return "Unbekannter Rang (" + content + ")";
		}

		return "<img src=\""+rang.getImage()+"\" alt=\"\"/>"+rang.getName();
	}

}
