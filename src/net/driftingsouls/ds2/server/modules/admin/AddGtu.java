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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.modules.admin.AdminMenuEntry;
import net.driftingsouls.ds2.server.modules.admin.AdminPlugin;

/**
 * Ermoeglicht das Einfuegen von neuen Versteigerungen in die GTU 
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="GTU", name="Versteigern")
public class AddGtu implements AdminPlugin {

	public void output(AdminController controller, String page, int action) {
		Context context = ContextMap.getContext();
		StringBuffer echo = context.getResponse().getContent();
		
		int ship = context.getRequest().getParameterInt("ship");
		String resource = context.getRequest().getParameterString("resource");
		int dauer = context.getRequest().getParameterInt("dauer");
		int preis = context.getRequest().getParameterInt("preis");
		int menge = context.getRequest().getParameterInt("menge");
		
		Database db = context.getDatabase();
		
		if( (ship == 0) && ((resource.length() == 0) || resource.equals("-1") ) ) {
			echo.append("Schiffe:\n");
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorder\" width=\"300\">\n");
			echo.append("<tr><td class=\"noBorderS\" width=\"60\">Schifftyp:</td><td class=\"noBorderS\">");
			echo.append("<select name=\"ship\" size=\"1\">\n");
			SQLQuery st = db.query("SELECT nickname,id FROM ship_types");
			while( st.next() ) {
				echo.append("<option value=\""+st.getInt("id")+"\">"+st.getString("nickname")+" ("+st.getInt("id")+")</option>\n");
			}
			st.free();
			echo.append("</select>\n");
			echo.append("</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Dauer:</td><td class=\"noBorderS\"><input type=\"text\" name=\"dauer\" size=\"10\" value=\"30\" /></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Gebot:</td><td class=\"noBorderS\"><input type=\"text\" name=\"preis\" size=\"10\" /></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\" colspan=\"2\" align=\"center\"><input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"einf&uuml;gen\" style=\"width:100px\"/></td></tr>\n");
			echo.append("</table>\n");
			echo.append("</form>\n");
			echo.append("<br />\n");
			
			echo.append("Resourcen:\n");
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorder\" width=\"300\">\n");
			echo.append("<tr><td class=\"noBorderS\" width=\"60\">Artefakt:</td><td class=\"noBorderS\">");
			echo.append("<select name=\"resource\" size=\"1\">\n");
			boolean wasItem = false;
			for( ResourceEntry res : Resources.RESOURCE_LIST.getResourceList() ) {
				if( !wasItem && res.getId().isItem() ) {
					wasItem = true;
					echo.append("<option value=\"-1\">--------</option>\n");
				}
				echo.append("<option value=\""+res.getId()+"\">"+res.getPlainName()+(res.getId().isItem() ? "("+res.getId().getItemID()+")" : "")+"</option>\n");
			}
			echo.append("</select>\n");
			echo.append("</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Menge:</td><td class=\"noBorderS\"><input type=\"text\" name=\"menge\" size=\"10\" value=\"1\" /></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Dauer:</td><td class=\"noBorderS\"><input type=\"text\" name=\"dauer\" size=\"10\" value=\"30\" /></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Gebot:</td><td class=\"noBorderS\"><input type=\"text\" name=\"preis\" size=\"10\" /></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\" colspan=\"2\" align=\"center\"><input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"einf&uuml;gen\" style=\"width:100px\"/></td></tr>\n");
			echo.append("</table>\n");
			echo.append("</form>\n");
		}
		else if( ship != 0 ) {
			int tick = context.get(ContextCommon.class).getTick();

			db.update("INSERT INTO versteigerungen (mtype,type,tick,preis) " +
					"VALUES ('1','"+ship+"',"+(tick+dauer)+",'"+preis+"')");

			echo.append("Schiff eingef&uuml;gt<br />");
		}
		else if( (resource.length() > 0) && !resource.equals("-1") ) {
			int tick = context.get(ContextCommon.class).getTick();

			Cargo cargo = new Cargo();
			cargo.addResource( Resources.fromString(resource), menge );

			db.update("INSERT INTO versteigerungen (mtype,type,tick,preis) " +
					"VALUES ('2','"+cargo.save()+"','"+(tick+dauer)+"','"+preis+"')");

			echo.append("Resource eingef&uuml;gt<br />");
		}	
	}
}
