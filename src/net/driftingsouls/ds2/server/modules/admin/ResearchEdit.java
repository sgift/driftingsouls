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

import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;

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
		
		//Database db = context.getDatabase();
		org.hibernate.Session db = context.getDB();
		
		if( (changedata != 0) && (techid > 0) ) {
			Forschung research = (Forschung)db.get(Forschung.class, techid);
			research.setRace(context.getRequest().getParameterInt("race"));
			research.setReq1(context.getRequest().getParameterInt("req1"));
			research.setReq2(context.getRequest().getParameterInt("req2"));
			research.setReq3(context.getRequest().getParameterInt("req3"));
			research.setVisibility( context.getRequest().getParameterInt("visibility"));
			research.setName(context.getRequest().getParameterString("name"));
			research.setDescription(context.getRequest().getParameterString("descrip"));
			
		}

		//Spezialforschungen ignorieren
		if( techid <= 0 ) {
			echo.append(Common.tableBegin( 450, "left" ));
			
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("Tech: <select name=\"techid\" size=\"1\" style=\"width:250px\">\n");
			
			List<?> researches = db.createQuery("from Forschung").list();
			for( Iterator<?> iter=researches.iterator(); iter.hasNext(); ) {
				Forschung research = (Forschung)iter.next();
				echo.append("<option value=\""+research.getID()+"\""+(research.getID() == techid ? " selected=\"selected\"" : "")+">"+research.getName()+"</option>\n");
			}

			echo.append("</select>\n");
			echo.append("<input type=\"submit\" value=\"bearbeiten\" />\n");
			echo.append("</form>\n");
			
			echo.append(Common.tableEnd());	
		}
		else {
			Forschung research = (Forschung)db.get(Forschung.class, techid);
			
			echo.append(Common.tableBegin( 900, "left" ));
			echo.append("<form action=\"ds\" method=\"post\">\n");
			echo.append("<table class=\"noBorderX\" cellpadding=\"2\" cellspacing=\"2\" width=\"100%\">\n");
			
			echo.append("<tr><td width=\"200\" colspan=\"2\" class=\"noBorderX\">");
			echo.append("<input type=\"text\" name=\"name\" value=\""+Common._plaintitle(research.getName())+"\" size=\"20\" />\n");
			echo.append(" ("+techid+")</td>\n");;

			echo.append("<td class=\"noBorderX\">Vorraussetzungen</td>\n");
			echo.append("<td width=\"140\" class=\"noBorderX\">Kosten</td>\n");
			echo.append("<td class=\"noBorderX\">Erm&ouml;glicht</td>\n");
			echo.append("</tr>");
			
			echo.append("<tr>");
			echo.append("<td class=\"noBorderX\"><img src=\""+Configuration.getSetting("URL")+"data/tech/"+techid+".gif\" alt=\"Kein Bild vorhanden\" /></td>");
				
			echo.append("<td class=\"noBorderX\">\n");
			
			echo.append("<select name=\"race\" size=\"1\" style=\"width:100px\">\n");
			echo.append("<option value=\"-1\" "+(research.getRace() == -1 ? "selected=\"selected\"" : "")+">Alle</option>\n");
			for( Rasse rasse : Rassen.get() ) {
				echo.append("<option value=\""+rasse.getID()+"\" "+(research.getRace() == rasse.getID() ? "selected=\"selected\"" : "")+">"+rasse.getName()+"</option>\n");
			}		
			echo.append("</select>\n");
			
			echo.append("</td>");
		
			echo.append("<td class=\"noBorderX\">");
			
			for( int i=1; i <= 3; i++ ) {
				if( i > 1 ) echo.append("<br />");
				
				echo.append("<select name=\"req"+i+"\" size=\"1\" style=\"width:200px\">\n");
				List<?> researches = db.createQuery("from Forschung").list();
				for( Iterator<?> iter=researches.iterator(); iter.hasNext(); ) {
					Forschung requirement = (Forschung)iter.next();
					echo.append("<option value=\""+research.getID()+" "+(requirement.getID() == techid ? "selected=\"selected\"" : "")+" \">"+requirement.getName()+"</option>\n");
				}
				echo.append("</select>\n");
			}
			
			echo.append("</td>\n");
			echo.append("<td class=\"noBorderX\">\n");
			
			//
			// TODO: Resourcen muessen auch editierbar sein
			//
			
			echo.append("<img style=\"vertical-align:middle\" src=\""+Configuration.getSetting("URL")+"data/interface/time.gif\" alt=\"Dauer\" />"+research.getTime());
				
			Cargo costs = new Cargo(research.getCosts());
			costs.setOption( Cargo.Option.SHOWMASS, false );
				
			ResourceList reslist = costs.getResourceList();
			for( ResourceEntry res : reslist ) {
				echo.append(" <img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1());
			}
				
			echo.append("</td><td class=\"noBorderX\" valign=\"top\">");
				
			boolean entry = false;
			List<?> requirements = db.createQuery("from Forschung where id=? or id=? or id=?")
				.setInteger(0, research.getRequiredResearch(1))
				.setInteger(1, research.getRequiredResearch(2))
				.setInteger(2, research.getRequiredResearch(3))
				.list();
			for( Iterator<?> iter=requirements.iterator(); iter.hasNext(); ) {
				Forschung requirement = (Forschung)iter.next();
				
				if(entry) {
					echo.append(",<br />\n");
				}
				
				if(requirement.hasVisibility(Forschung.Visibility.NEVER)) {
					echo.append("<span class=\"smallfont\"><a class=\"error\" style=\"font-style:italic\" href=\"./ds?module=admin&sess="+context.getSession()+"&page="+page+"&act="+action+"&techid="+requirement.getID()+"\">["+Common._title(requirement.getName())+"]</a></span>\n");
					entry = true;
				}
				else {
					echo.append("<a class=\"forschinfo\" href=\"./ds?module=admin&sess="+context.getSession()+"&page="+page+"&act="+action+"&techid="+requirement.getID()+"\">"+Common._title(requirement.getName())+"</a>\n");
					entry = true;
				}
			}
				
			if( !entry ) {
				echo.append("&nbsp;");
			}
			echo.append("</td></tr>");
			
			// Beschreibung
			echo.append("<tr><td class=\"noBorderX\" colspan=\"5\">");
			echo.append("<hr noshade=\noshade\" size=\"1\" style=\"color:#cccccc\" />");
			echo.append("<textarea rows=\"5\" cols=\"90\" name=\"descrip\">\n");
			echo.append(Common._plaintitle(research.getDescription())+"</textarea>");
			echo.append("</td></tr>");
			
			// Sonstiges		
			echo.append("<tr><td class=\"noBorderX\" colspan=\"5\">\n");
			echo.append("<hr noshade=\"noshade\" size=\"1\" style=\"color:#cccccc\" />\n");
			echo.append("<select name=\"visibility\" size=\"1\" style=\"width:200px\">\n");
			for(Forschung.Visibility visibility: Forschung.Visibility.values()) {
				echo.append("<option value=\""+visibility.getBits()+" "+(research.hasVisibility(visibility) ? "selected=\"selected\"" : "")+" \">"+visibility.toString()+"</option>\n");
			}
			echo.append("</select>");
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
