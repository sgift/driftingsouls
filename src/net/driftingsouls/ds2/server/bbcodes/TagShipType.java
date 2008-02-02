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

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeFunction;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * BBCode fuer Schiffstypen
 * @author Christopher Jung
 *
 */
public class TagShipType implements BBCodeFunction {

	public String handleMatch(String content, String... values) {
		Context context = ContextMap.getContext();
		String url = Configuration.getSetting("URL");
	
		url += "ds?module=schiffinfo&sess="+context.getSession()+"&ship="+content;
	
		SQLResultRow shiptype = ShipTypes.getShipType(Integer.parseInt(content), false);

	 	return "<a target=\"main\" onmouseover=\"return overlib('"+shiptype.getString("nickname")+"',TIMEOUT,0,DELAY,400,WIDTH,150);\" onmouseout=\"return nd();\" class=\"noborder\" href=\""+url+"\"><img align=\"middle\" border=\"0\" src=\""+shiptype.getString("picture")+"\" alt=\"\" /></a>";
	}

}
