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
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeFunction;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * BBCode fuer Schiffstypen
 * 
 * @author Christopher Jung
 * 
 */
@Configurable
public class TagShipType implements BBCodeFunction
{
	
	private Configuration config;
	
    /**
     * Injiziert die DS-Konfiguration
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
    }

	public String handleMatch(String content, String... values)
	{
		String url = config.get("URL");

		url += "ds?module=schiffinfo&ship=" + content;

		ShipTypeData shiptype = Ship.getShipType(Integer.parseInt(content));

		return "<a target=\"main\" onmouseover=\"return overlib('"
				+ shiptype.getNickname()
				+ "',TIMEOUT,0,DELAY,400,WIDTH,150);\" onmouseout=\"return nd();\" class=\"noborder\" href=\""
				+ url + "\"><img align=\"middle\" border=\"0\" src=\"" + shiptype.getPicture()
				+ "\" alt=\"\" /></a>";
	}

}
