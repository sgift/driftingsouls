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
package net.driftingsouls.ds2.server.modules.stats;

import java.io.IOException;
import java.io.Writer;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import org.apache.commons.lang.StringUtils;

/**
 * Zeigt die Liste hoechsten Gebote (welche zur Ersteigerung fuehrten) in der GTU.
 * @author Christopher Jung
 *
 */
public class StatGtuPrice extends AbstractStatistic implements Statistic {
	/**
	 * Konstruktor.
	 * 
	 */
	public StatGtuPrice() {
		// EMPTY
	}
	
	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		String url = getUserURL();
	
		Writer echo = getContext().getResponse().getWriter();
		
		echo.append("<table class=\"noBorderX\" cellspacing=\"1\" cellpadding=\"1\" width=\"100%\">\n");
		echo.append("<tr><td class=\"noBorderX\" colspan=\"5\" align=\"left\">Die h&ouml;chsten Gebote:</td></tr>\n");
	
		int a = 1;
	
		SQLQuery gebot = db.query("SELECT username,userid,preis,type,mtype " +
				"FROM stats_gtu " +
				"ORDER BY preis DESC LIMIT 0,"+size);

		while( gebot.next() ) {
			String name = null;
			
			if( gebot.getInt("mtype") == 1 ) {
				ShipTypeData shiptype = Ship.getShipType(gebot.getInt("type"));
				name = "<a class=\"forschinfo\" href=\"./ds?module=schiffinfo&ship="+gebot.getInt("type")+"\">"+shiptype.getNickname()+"</a>";
			}
			else if( gebot.getInt("mtype") == 2 ) {
				Cargo mycargo = new Cargo( Cargo.Type.STRING, gebot.getString("type") );
				ResourceEntry resource = mycargo.getResourceList().iterator().next();
					
				name = ( resource.getCount1() > 1 ? resource.getCount1()+"x " : "" )+Cargo.getResourceName(resource.getId());
			}
	
	   		echo.append("<tr><td class=\"noBorderX\" style=\"width:40px\">"+a+".</td>\n");
	   		echo.append("<td class=\"noBorderX\"><a class=\"profile\" href=\""+url+gebot.getInt("userid")+"\">"+Common._title(gebot.getString("username"))+" ("+gebot.getInt("userid")+")</td>");
			echo.append("<td class=\"noBorderX\">&nbsp;-&nbsp;</td>\n");
			echo.append("<td class=\"noBorderX\">"+name+"</td>\n");
			echo.append("<td class=\"noBorderX\">&nbsp;-&nbsp;</td>\n");
			echo.append("<td class=\"noBorderX\"><span class=\"nobr\">"+Common.ln(gebot.getLong("preis"))+" RE</span></td></tr>\n");
	
			a++;
		}
		gebot.free();
		
		echo.append("</table><br /><br />\n");
	}
}
