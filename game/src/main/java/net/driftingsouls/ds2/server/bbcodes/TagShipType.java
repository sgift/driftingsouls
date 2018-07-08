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

import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeFunction;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * BBCode fuer Schiffstypen.
 *
 * @author Christopher Jung
 */
public class TagShipType implements BBCodeFunction
{
	@Override
	public String handleMatch(String content, String... values)
	{
		String url = "./ds?module=schiffinfo&ship=" + content;

		if (!NumberUtils.isDigits(content))
		{
			return "Unbekannter Schiffstyp (" + content + ")";
		}

		ShipTypeData shiptype = Ship.getShipType(Integer.parseInt(content));

		User user = (User) ContextMap.getContext().getActiveUser();

		if ((shiptype == null) ||
				(shiptype.isHide() && ((user == null) || !ContextMap.getContext().hasPermission(WellKnownPermission.SCHIFFSTYP_VERSTECKTE_SICHTBAR))))
		{

			return "Unbekannter Schiffstyp (" + content + ")";
		}

		return "<a target=\"main\" title=\"" + shiptype.getNickname() + "\" class=\"noborder\" " +
				"onclick='ShiptypeBox.show(" + shiptype.getTypeId() + ");return false;' " +
				"href=\"" + url + "\"><img align=\"middle\" border=\"0\" src=\"" + shiptype.getPicture()
				+ "\" alt=\"\" /></a>";
	}

}
