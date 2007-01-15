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

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Zeigt die Liste hoechsten Gebote (welche zur Ersteigerung fuehrten) in der GTU
 * @author Christopher Jung
 *
 */
public class StatGtuPrice extends AbstractStatistic implements Statistic {
	/**
	 * Konstruktor
	 * 
	 */
	public StatGtuPrice() {
		// EMPTY
	}
	
	public void show(StatsController contr, int size) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		String url = getUserURL();
	
		StringBuffer echo = getContext().getResponse().getContent();
		
		echo.append("<table class=\"noBorderX\" cellspacing=\"1\" cellpadding=\"1\" width=\"100%\">\n");
		echo.append("<tr><td class=\"noBorderX\" colspan=\"5\" align=\"left\">Die h&ouml;chsten Gebote:</td></tr>\n");
	
		int a = 1;
	
		SQLQuery gebot = db.query("SELECT username,userid,preis,type,mtype " +
				"FROM stats_gtu " +
				"ORDER BY preis DESC LIMIT 0,"+size);

		while( gebot.next() ) {
			String name = null;
			
			if( gebot.getInt("mtype") == 1 ) {
				SQLResultRow shiptype = Ships.getShipType(gebot.getInt("type"), false);
				name = "<a class=\"forschinfo\" href=\"./main.php?module=schiffinfo&sess="+context.getSession()+"&ship="+gebot.getInt("type")+"\">"+shiptype.getString("nickname")+"</a>";
			}
			else if( gebot.getInt("mtype") == 2 ) {
				Cargo mycargo = new Cargo( Cargo.Type.STRING, gebot.getString("type") );
				ResourceEntry resource = mycargo.getResourceList().iterator().next();
					
				name = ( resource.getCount1() > 1 ? resource.getCount1()+"x " : "" )+Cargo.getResourceName(resource.getId());
			}
			else if( gebot.getInt("mtype") == 3 ) {
				String[] type = StringUtils.split(gebot.getString("type"), '/');
				int[] ships = Common.explodeToInt("|", type[1]);
				
				Cargo cargo = new Cargo( Cargo.Type.STRING, type[0] );
				
				StringBuilder text = new StringBuilder();
				
				ResourceList reslist = cargo.getResourceList();
				for( ResourceEntry res : reslist ) {
					if( res.getCount1() > 1 ) {
						text.append(res.getCount1()+"x ");	
					}
					text.append(Cargo.getResourceName( res.getId() )+"<br />");
				}
	
				for( int i=0; i < ships.length; i++ ) {
					SQLResultRow shiptype = Ships.getShipType(ships[i], false);
					text.append(shiptype.getString("nickname")+"<br />");
				}
	
				name = "<a class=\"forschinfo\" href=\"#\" onmouseover=\"return overlib('"+text+"');\" onmouseout=\"return nd();\">GTU-Paket</a>";
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
