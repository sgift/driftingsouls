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
package net.driftingsouls.ds2.server.modules.admin;

import java.io.IOException;
import java.io.Writer;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Zeigt an, was wie oft versteigert wurde und welche Durchschnittspreise erziehlt wurden.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="GTU", name="Preisliste")
public class GtuPrices implements AdminPlugin {

	@Override
	public void output(AdminController controller, String page, int action) throws IOException {
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		
		Database db = context.getDatabase();
		
		echo.append("<table class=\"noBorder\">\n");
		echo.append("<tr><td class=\"noBorder\" width=\"230\">Typ</td><td class=\"noBorder\" width=\"20\">Menge</td><td class=\"noBorder\" width=\"180\">Durchschnittspreis</td></tr>\n");

		// Resourcen
		echo.append("<tr><td class=\"borderS\" colspan=\"4\">Artefakte</td></tr>\n");
		
		SQLQuery rt = db.query("SELECT type,sum(preis)/count(preis) as avprice, count(preis) as menge " +
					"FROM stats_gtu " +
					"WHERE mtype=2 " +
					"GROUP BY type ORDER BY avprice DESC" );
		while( rt.next() ) {
			echo.append("<tr><td class=\"noBorderS\">");
			Cargo cargo = new Cargo( Cargo.Type.STRING, rt.getString("type"));
			cargo.setOption( Cargo.Option.SHOWMASS, false );
			ResourceEntry res = cargo.getResourceList().iterator().next();
				
			echo.append("<tr><td class=\"noBorderS\">");
			echo.append(res.getCount1()+"x <img src=\""+res.getImage()+"\" alt=\"\" />"+res.getName());
			echo.append("</td>\n");
			echo.append("<td class=\"noBorderS\" width=\"180\">"+rt.getInt("menge")+"x</td>\n");
			echo.append("<td class=\"noBorderS\" width=\"180\">"+Common.ln(rt.getDouble("avprice"))+" RE\n");
			if( res.getCount1() > 1 ) {
				echo.append("&nbsp;&nbsp;~"+Common.ln(rt.getDouble("avprice")/res.getCount1())+"/E\n");
			}
			echo.append("</td></tr>\n");
		}
		rt.free();

		//Schiffe
		echo.append("<tr><td class=\"borderS\" colspan=\"4\">Schiffe</td></tr>\n");
		SQLQuery st = db.query("SELECT st.nickname,sg.type,sum(sg.preis)/count(sg.preis) as avprice, count(sg.preis) as menge "+
					"FROM ship_types st JOIN stats_gtu sg ON st.id=sg.type "+
					"WHERE sg.mtype=1 "+
					"GROUP BY sg.type ORDER BY avprice DESC" );
		while( st.next() ) {
			echo.append("<tr><td class=\"noBorderS\"><a class=\"forschinfo\" " +
					"href=\"./ds?module=schiffinfo&sess=$sess&ship="+st.getInt("type")+"\">"+
					st.getString("nickname")+"</a> ("+st.getInt("type")+")</td>\n");
			echo.append("<td class=\"noBorderS\" width=\"180\">"+st.getInt("menge")+"x</td>\n");
			echo.append("<td class=\"noBorderS\" width=\"180\">"+Common.ln(st.getDouble("avprice"))+" RE</td></tr>\n");
		}
		st.free();
		
		echo.append("</table>");
	}
}
