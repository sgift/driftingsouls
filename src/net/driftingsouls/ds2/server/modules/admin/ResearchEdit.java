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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.modules.admin.AdminMenuEntry;
import net.driftingsouls.ds2.server.modules.admin.AdminPlugin;

/**
 * Ermoeglicht das Bearbeiten von Forschungen
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Techs", name="Bearbeiten")
public class ResearchEdit implements AdminPlugin {

	public void output(AdminController controller, String page, int action) {
		Context context = ContextMap.getContext();
		StringBuffer echo = context.getResponse().getContent();
		
		int techid = context.getRequest().getParameterInt("techid");
		int changedata = context.getRequest().getParameterInt("changedata");
		
		Database db = context.getDatabase();
		
		if( (changedata != 0) && (techid != 0) ) {
			// Name
			db.prepare("UPDATE forschungen SET name=? WHERE id= ?")
				.update(context.getRequest().getParameterString("name"), techid);		
			
			// Voraussetzungen
			int req1 = context.getRequest().getParameterInt("req1");
			int req2 = context.getRequest().getParameterInt("req2");
			int req3 = context.getRequest().getParameterInt("req3");
			int race = context.getRequest().getParameterInt("race");
			db.update("UPDATE forschungen SET req1="+req1+",req2="+req2+",req3="+req3+",race="+race+" WHERE id="+techid);
			
			// Sonstiges (BeschreibungSichtbarkeit u.a.)
			int visibility = context.getRequest().getParameterInt("visibility");
			String descip = context.getRequest().getParameterString("descrip");
			db.prepare("UPDATE forschungen SET descrip= ? ,visibility= ? WHERE id= ? ")
				.update(descip, visibility, techid);
		}

		if( techid == 0 ) {
			echo.append(Common.tableBegin( 450, "left" ));
			
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("Tech: <select name=\"techid\" size=\"1\" style=\"width:250px\">\n");

			SQLQuery forschung = db.query("SELECT id,name,visibility FROM forschungen ORDER BY name");
			while( forschung.next() ) {
				echo.append("<option value=\""+forschung.getInt("id")+"\">"+(forschung.getInt("visibility") != 0 ? "" : "# ")+forschung.getString("name")+"</option>\n");
			}
			forschung.free();

			echo.append("</select>\n");
			echo.append("<input type=\"submit\" value=\"bearbeiten\" />\n");
			echo.append("</form>\n");
			
			echo.append(Common.tableEnd());	
		}
		else {
			SQLResultRow data = db.first("SELECT * FROM forschungen WHERE id="+techid);
			
			echo.append(Common.tableBegin( 900, "left" ));
			echo.append("<form action=\"ds\" method=\"post\">\n");
			echo.append("<table class=\"noBorderX\" cellpadding=\"2\" cellspacing=\"2\" width=\"100%\">\n");
			
			echo.append("<tr><td width=\"200\" colspan=\"2\" class=\"noBorderX\">");
			echo.append("<input type=\"text\" name=\"name\" value=\""+Common._plaintitle(data.getString("name"))+"\" size=\"20\" />\n");
			echo.append(" ("+techid+")</td>\n");;

			echo.append("<td class=\"noBorderX\">Vorraussetzungen</td>\n");
			echo.append("<td width=\"140\" class=\"noBorderX\">Kosten</td>\n");
			echo.append("<td class=\"noBorderX\">Erm&ouml;glicht</td>\n");
			echo.append("</tr>");
			
			echo.append("<tr>");
			echo.append("<td class=\"noBorderX\"><img src=\""+Configuration.getSetting("URL")+"data/tech/"+techid+".gif\" alt=\"Kein Bild vorhanden\" /></td>");
				
			echo.append("<td class=\"noBorderX\">\n");
			
			echo.append("<select name=\"race\" size=\"1\" style=\"width:100px\">\n");
			echo.append("<option value=\"-1\" "+(data.getInt("race") == -1 ? "selected=\"selected\"" : "")+">Alle</option>\n");
			for( Rasse rasse : Rassen.get() ) {
				echo.append("<option value=\""+rasse.getID()+"\" "+(data.getInt("race") == rasse.getID() ? "selected=\"selected\"" : "")+">"+rasse.getName()+"</option>\n");
			}		
			echo.append("</select>\n");
			
			echo.append("</td>");
		
			echo.append("<td class=\"noBorderX\">");
			
			for( int i=1; i <= 3; i++ ) {
				if( i > 1 ) echo.append("<br />");
				
				echo.append("<select name=\"req"+i+"\" size=\"1\" style=\"width:200px\">\n");
				echo.append("<option value=\"-1\" "+(data.getInt("req"+i) == -1 ? "selected=\"selected\"" : "")+">### Nicht erf&uuml;lbar ###</option>\n");
				echo.append("<option value=\"0\" "+(data.getInt("req"+i) == 0 ? "selected=\"selected\"" : "")+">Keine Voraussetzung</option>\n");
				SQLQuery forschung = db.query("SELECT id,name FROM forschungen ORDER BY name");
				while( forschung.next() ) {
					echo.append("<option value=\""+forschung.getInt("id")+"\" "+(data.getInt("req"+i) == forschung.getInt("id") ? "selected=\"selected\"" : "")+">"+forschung.getString("name")+"</option>\n");
				}
				forschung.free();
				echo.append("</select>\n");
			}
			
			echo.append("</td>\n");
			echo.append("<td class=\"noBorderX\">\n");
			
			//
			// TODO: Resourcen muessen auch editierbar sein
			//
			
			echo.append("<img style=\"vertical-align:middle\" src=\""+Configuration.getSetting("URL")+"data/interface/time.gif\" alt=\"Dauer\" />"+data.getInt("time"));
				
			Cargo costs = new Cargo( Cargo.Type.STRING, data.getString("costs") );
			costs.setOption( Cargo.Option.SHOWMASS, false );
				
			ResourceList reslist = costs.getResourceList();
			for( ResourceEntry res : reslist ) {
				echo.append(" <img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1());
			}
				
			echo.append("</td><td class=\"noBorderX\" valign=\"top\">");
				
			boolean entry = false;
			SQLQuery result = db.query( "SELECT id,name,req1,req2,req3,visibility FROM forschungen WHERE req1="+techid+" OR req2="+techid+" OR req3="+techid);
			while( result.next() ) {
				if( result.getInt("visibility") != 0 ) {
					if( entry ) {
						echo.append(",<br />\n");
					}
					echo.append("<a class=\"forschinfo\" href=\"./ds?module=admin&sess="+context.getSession()+"&page="+page+"&act="+action+"&techid="+result.getInt("id")+"\">"+Common._title(result.getString("name"))+"</a>\n");
					entry = true;
				}
				else {
					if( entry ) {
						echo.append(",<br />\n");
					}
					echo.append("<span class=\"smallfont\"><a class=\"error\" style=\"font-style:italic\" href=\"./ds?module=admin&sess="+context.getSession()+"&page="+page+"&act="+action+"&techid="+result.getInt("id")+"\">["+Common._title(result.getString("name"))+"]</a></span>\n");
					entry = true;
				}
			}
			result.free();
				
			if( !entry ) {
				echo.append("&nbsp;");
			}
			echo.append("</td></tr>");
			
			// Beschreibung
			echo.append("<tr><td class=\"noBorderX\" colspan=\"5\">");
			echo.append("<hr noshade=\noshade\" size=\"1\" style=\"color:#cccccc\" />");
			echo.append("<textarea rows=\"5\" cols=\"90\" name=\"form[descrip]\">\n");
			echo.append(Common._plaintitle(data.getString("descrip"))+"</textarea>");
			echo.append("</td></tr>");
			
			// Sonstiges		
			echo.append("<tr><td class=\"noBorderX\" colspan=\"5\">\n");
			echo.append("<hr noshade=\"noshade\" size=\"1\" style=\"color:#cccccc\" />\n");
			echo.append("<input type=\"checkbox\" name=\"visible\" id=\"visible\" value=\"1\" "+(data.getInt("visibility") != 0 ? "checked=\"checked\"" : "")+" /><label for=\"visible\">Sichtbare Forschung</label><br />\n"); 
			echo.append("<input type=\"hidden\" name=\"changedata\" value=\"1\" />\n");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"techid\" value=\""+techid+"\" />\n");
			echo.append("<div align=\"center\"><input type=\"submit\" value=\"&Auml;nderungen speichern\" />\n");
			echo.append("</td></tr>\n");
		
			echo.append("</table>\n");
			echo.append("</form>\n");
			echo.append(Common.tableEnd());		
		}
	}
}
