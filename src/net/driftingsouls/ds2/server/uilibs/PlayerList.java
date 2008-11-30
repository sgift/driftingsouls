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
package net.driftingsouls.ds2.server.uilibs;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;

/**
 * Die Spielerliste
 * @author Christopher Jung
 *
 */
public class PlayerList {
	/**
	 * Gibt die Spielerliste im angegebenen Kontext aus
	 * @param context Der Kontext
	 * @throws IOException 
	 */
	public static void draw(Context context) throws IOException {
		String ord = context.getRequest().getParameter("ord");
		
		int comPopup = context.getRequest().getParameter("compopup") != null ? 
				Integer.parseInt(context.getRequest().getParameter("compopup")) : 
				0;
				
		User user = (User)context.getActiveUser();
		org.hibernate.Session db = context.getDB();
		
		String show = "";
		if( context.getRequest().getParameter("show") != null ) {
			show = "&show="+context.getRequest().getParameter("show");
		}
		
		String url = Configuration.getSetting("URL")+"ds";
		if( context.getRequest().getParameter("module") != null ) {
			url += "?module="+context.getRequest().getParameter("module");
		}
		
		if( context.getRequest().getParameter("action") != null ) {
			url += "&action="+context.getRequest().getParameter("action");
		}
		url += show;
		
		Writer echo = context.getResponse().getWriter();
		echo.append("<table class=\"noBorderX\" cellpadding=\"2\" cellspacing=\"2\" width=\"100%\">\n");
		
		if( comPopup == 0 ) {
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\""+url+"&ord=id\">ID</a></td>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"></td>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\""+url+"&ord=id\">Name</a></td>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\""+url+"&ord=race\">Rasse</a></td>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\""+url+"&ord=signup\">Dabei seit</a></td>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\""+url+"&ord=ally\">Allianz</b></a></td>");
		}
		// Sollen wir nen Popup sein?
		else {
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\""+url+"&ord=id\">ID</a></td>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\""+url+"&ord=id\">Name</a></td>\n");
		}
		
		// Ein Admin bekommt mehr zu sehen...
		if( (comPopup == 0) && (user != null) && user.getAccessLevel() > 20 ) {
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\""+url+"&ord=inakt\">Inaktiv</a></td>");
			echo.append("<td class=\"noBorderX\" align=\"center\">Astis</td>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\">Schiffe</td>\n");
		}
		echo.append("</tr>\n");
			
		HashMap<Integer,Integer> asticount = null;
		HashMap<Integer,Integer> shipcount = null;
		
		String query = "";
		if( (user == null) || user.getAccessLevel() <= 20 ) {
			query = "select u from User u left join fetch u.ally a where locate('hide',u.flags)=0 order by ";
		}
		else {
			// Asteroiden/Schiffe zaehlen
			asticount = new HashMap<Integer,Integer>();
			shipcount = new HashMap<Integer,Integer>();
			
			List<?> basecounts = db.createQuery("select owner.id,count(*) from Base group by owner.id").list();
			for( Iterator<?> iter=basecounts.iterator(); iter.hasNext(); ) {
				final Object[] data = (Object[])iter.next();
				final Integer owner = (Integer)data[0];
				final Number count = (Number)data[1];
				asticount.put(owner, count.intValue());
			}
			
			List<?> shipcounts = db.createQuery("select owner.id,count(*) from Ship group by owner.id").list();
			for( Iterator<?> iter=shipcounts.iterator(); iter.hasNext(); ) {
				final Object[] data = (Object[])iter.next();
				final Integer owner = (Integer)data[0];
				final Number count = (Number)data[1];
				shipcount.put(owner, count.intValue());
			}
			
			query = "select u from User u left join fetch u.ally a order by ";
		}
		
		if( (ord == null) || "".equals(ord) ) {
			query += "u.id";
		} 
		else if( "id".equals(ord) || "name".equals("ord") || "race".equals(ord) || "signup".equals(ord) || "ally".equals(ord) ) {
			query += "u."+ord;
		} 
		else if( "inakt".equals(ord) && (user != null) && (user.getAccessLevel() > 20) ) {
			query += "u.inakt";
		} 
		else {
			query += "u.id";
		}
		
		User.Relations relationlist = null;
		if( user != null ) {
			relationlist = user.getRelations();
		}
		
		List<User> userlist = context.query(query, User.class);
		for( User aUser : userlist ) {
			String race = "???";
			if( Rassen.get().rasse(aUser.getRace()) != null ) {
				race = Rassen.get().rasse(aUser.getRace()).getName();
			} 

			String ally = "&nbsp;";
			if( aUser.getAlly() != null ) {
				if( user != null ) {
					ally = "<a class=\"profile\" href=\""+Common.buildUrl("details", "module", "allylist", "details", aUser.getAlly().getId()) +"\">"+Common._title(aUser.getAlly().getName())+"</a>";
				}
				else {
					ally = Common._title(aUser.getAlly().getName());
				}
			} 
			
			echo.append("<tr>\n");
			
			// ID
			echo.append("<td class=\"noBorderX\">"+aUser.getId()+"</td>\n");
			
			if( comPopup == 0 ) {
				// Diplomatie
				echo.append("<td class=\"noBorderX\"><span class=\"nobr\">\n");
				
				if( (user != null) && (aUser.getId() != user.getId()) ) {
					if( !relationlist.toOther.containsKey(aUser.getId()) ) {
						relationlist.toOther.put(aUser.getId(), relationlist.toOther.get(0));	
					}
					
					if( relationlist.toOther.get(aUser.getId()) == User.Relation.ENEMY ) {
						echo.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/diplomacy/enemy1.png\" alt=\"\" title=\"Feindlich\" />");
					}
					else if( relationlist.toOther.get(aUser.getId()) == User.Relation.NEUTRAL ) {
						echo.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/diplomacy/neutral1.png\" alt=\"\" />");
					}
					else if( relationlist.toOther.get(aUser.getId()) == User.Relation.FRIEND ) {
						echo.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/diplomacy/friend1.png\" alt=\"\" title=\"Feundlich\" />");
					}
					
					if( relationlist.fromOther.get(aUser.getId()) == User.Relation.ENEMY ) {
						echo.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/diplomacy/enemy2.png\" alt=\"\" title=\"Feindlich\" />");
					}
					else if( relationlist.fromOther.get(aUser.getId()) == User.Relation.NEUTRAL ) {
						echo.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/diplomacy/neutral2.png\" alt=\"\" />");
					}
					else if( relationlist.fromOther.get(aUser.getId()) == User.Relation.FRIEND ) {
						echo.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/diplomacy/friend2.png\" alt=\"\" title=\"Feundlich\" />");
					}
				}
				
				echo.append("</td>\n");
				
				// Spielername
				if( context.getActiveUser() != null ) {
					echo.append("<td class=\"noBorderX\"><a class=\"profile\" href=\""+Common.buildUrl("default", "module", "userprofile", "user", aUser.getId())+"\">"+Common._title(aUser.getName())+"</a>");
				}
				else {
					echo.append("<td class=\"noBorderX\">"+Common._title(aUser.getName()));
				}
				if( aUser.hasFlag(User.FLAG_HIDE) ) {
					echo.append(" <span style=\"color:red;font-style:italic\">[h]</span>");
				}
				if( (user != null) && (user.getAccessLevel() > 20 ) && aUser.hasFlag(User.FLAG_VIEW_BATTLES) ) {
					echo.append(" <span style=\"color:red;font-style:italic\">[vb]</span>");
				}
				if( (user != null) && (user.getAccessLevel() > 20 ) && aUser.hasFlag(User.FLAG_ORDER_MENU) ) {
					echo.append(" <span style=\"color:red;font-style:italic\">[om]</span>");
				}
				if( (user != null) && (user.getAccessLevel() > 20 ) && aUser.hasFlag(User.FLAG_EXEC_NOTES) ) {
					echo.append(" <span style=\"color:red;font-style:italic\">[en]</span>");
				}
				echo.append("</span></td>\n");
				
				// Rasse
				echo.append("<td class=\"noBorderX\">"+race+"</td>\n");
				
				// Signup
				if( aUser.getSignup() != 0 ) {
					echo.append("<td class=\"noBorderX\" align=\"center\">"+Common.date("j.n.Y H:i",aUser.getSignup())+"</td>\n");
				}
				else {
					echo.append("<td class=\"noBorderX\" align=\"center\">-</td>\n");
				}
				
				// Ally
				echo.append("<td class=\"noBorderX\">"+ally+"</td>\n");
				
				// Die Spezial-Admin-Infos anzeigen
				if( (user != null) && (user.getAccessLevel() > 20) ) {
					echo.append("<td class=\"noBorderX\">"+aUser.getInactivity()+"</td>\n");
					if( !asticount.containsKey(aUser.getId()) ) {
						asticount.put(aUser.getId(), 0);
					}
					echo.append("<td class=\"noBorderX\" style=\"text-align:center\">"+asticount.get(aUser.getId())+"</td>\n");
					
					if( !shipcount.containsKey(aUser.getId()) ) {
						shipcount.put(aUser.getId(), 0);
					}
					echo.append("<td class=\"noBorderX\" style=\"text-align:center\">"+Common.ln(shipcount.get(aUser.getId()))+"</td>\n");
				}
			}
			else {
				echo.append("<td class=\"noBorderX\"><a style=\"font-size:14px;color:#c7c7c7\" href=\"javascript:playerPM("+aUser.getId()+");\">"+Common._title(aUser.getName())+"</a></td>\n");
			}
			
			echo.append("</tr>\n");
		}
		
		echo.append("</table>\n");
	}
}
