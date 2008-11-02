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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Ermoeglicht das Einfuegen von neuen Versteigerungen in die GTU 
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="GTU", name="Verkaufsdaten")
public class GtuVerkaeufe implements AdminPlugin {

	public void output(AdminController controller, String page, int action) throws IOException {
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		
		int system = context.getRequest().getParameterInt("system");
		String type = context.getRequest().getParameterString("type");
		
		Database db = context.getDatabase();
		
		if( (system == 0) || (type.length() == 0)  ) {
			echo.append(Common.tableBegin(400,"center"));
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorderX\" width=\"100%\">\n");
			echo.append("<tr><td class=\"noBorderX\" style=\"width:60px\">System:</td><td class=\"noBorderX\">");
			echo.append("<select name=\"system\" size=\"1\">\n");
			
			for( StarSystem sys : Systems.get() ) {
				echo.append("<option value=\""+sys.getID()+"\">"+sys.getName()+" ("+sys.getID()+")</option>\n");
			}
			
			echo.append("</select>\n");
			echo.append("</td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Verkaufsort:</td><td class=\"noBorderX\">\n");
			echo.append("<select name=\"type\" size=\"1\">\n");
			echo.append("<option value=\"asti\">Basisverkauf</option>\n");
			echo.append("<option value=\"tradepost\">Handelsposten</option>\n");
			echo.append("</select>\n");
			echo.append("</td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\" colspan=\"2\" style=\"text-align:center\">\n");
			echo.append("<input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"anzeigen\" style=\"width:100px\"/></td></tr>\n");
			echo.append("</table>\n");
			echo.append("</form>\n");
			echo.append(Common.tableEnd());
		}
		else {
			int entries = 0;
			Cargo totalcargo = new Cargo();
			Cargo cargo = new Cargo();
			final int tick = context.get(ContextCommon.class).getTick();
			
			SQLQuery entry = db.query("SELECT * FROM stats_verkaeufe WHERE system="+system+" AND place='"+type+"'");
			while( entry.next() ) {
				Cargo ecargo = new Cargo( Cargo.Type.STRING, entry.getString("stats") );
				
				totalcargo.addCargo(ecargo);
				entries++;
				
				if( (entry.getInt("tick") != tick) && (entry.getInt("tick") >= tick-7) ) {
					cargo.addCargo(ecargo);
				}
			}
			entry.free();
			
			echo.append(Common.tableBegin(300,"center"));
			echo.append("System: "+system+" - Type: "+type+"<br /><br />");
			echo.append("<table class=\"noBorderX\" width=\"100%\">\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\">Durchschnitt</td>\n");
			echo.append("<td class=\"noBorderX\">Zuletzt (7T)</td>\n");
			echo.append("</tr>\n");
			
			ResourceList reslist = totalcargo.getResourceList();
			for( ResourceEntry res : reslist ) {
				echo.append("<tr>\n");
				echo.append("<td class=\"noBorderX\">\n");
				echo.append("<img src=\""+res.getImage()+"\" alt=\"\" title=\""+res.getPlainName()+"\" />~"+Common.ln(Math.round(res.getCount1()/(double)entries))+"&nbsp;&nbsp;\n");
				echo.append("</td>\n");
				echo.append("<td class=\"noBorderX\">");
				echo.append(Common.ln(Math.round(cargo.getResourceCount(res.getId())/7d)));
				echo.append("</td></tr>\n");
			}
			echo.append("</table>");
			echo.append(Common.tableEnd());
		}
	}
}
