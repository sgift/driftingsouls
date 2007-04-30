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

import java.util.HashMap;

import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserIterator;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;

/**
 * Die Spielerliste
 * @author Christopher Jung
 *
 */
public class PlayerList {
	/**
	 * Gibt die Spielerliste im angegebenen Kontext aus
	 * @param context Der Kontext
	 */
	public static void draw(Context context) {
		String ord = context.getRequest().getParameter("ord");
		
		int comPopup = context.getRequest().getParameter("compopup") != null ? 
				Integer.parseInt(context.getRequest().getParameter("compopup")) : 
				0;
				
		User user = context.getActiveUser();
		Database db = context.getDatabase();
		
		String show = "";
		if( context.getRequest().getParameter("show") != null ) {
			show = "&show="+context.getRequest().getParameter("show");
		}
		
		String url = Configuration.getSetting("URL")+"ds";
		if( context.getRequest().getParameter("module") != null ) {
			url += "?module="+context.getRequest().getParameter("module");
		}
		url += "&sess="+context.getSession();
		
		if( context.getRequest().getParameter("action") != null ) {
			url += "&action="+context.getRequest().getParameter("action");
		}
		url += show;
		
		StringBuffer echo = context.getResponse().getContent();
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
		
		HashMap<Integer,String> allys = new HashMap<Integer,String>();
		SQLQuery allyQuery = db.query( "SELECT id,name FROM ally" );
		while( allyQuery.next() ) {
			allys.put(allyQuery.getInt("id"), allyQuery.getString("name"));
		}
		allyQuery.free();
		
		HashMap<Integer,Integer> asticount = null;
		HashMap<Integer,Integer> shipcount = null;
		
		String query = "";
		if( (user == null) || user.getAccessLevel() <= 20 ) {
			query = "SELECT t1.id,t1.name,t1.race,t1.signup,t1.ally,t1.inakt,t1.flags FROM users t1 WHERE !LOCATE('hide',t1.flags) ORDER BY ";
		}
		else {
			// Asteroiden/Schiffe zaehlen
			asticount = new HashMap<Integer,Integer>();
			shipcount = new HashMap<Integer,Integer>();
			
			SQLQuery basecount = db.query("SELECT owner,count(*) basecount FROM bases GROUP BY owner");
			while( basecount.next() ) {
				asticount.put(basecount.getInt("owner"), basecount.getInt("basecount"));
			}
			basecount.free();
			
			SQLQuery scount = db.query("SELECT owner,count(*) basecount FROM ships GROUP BY owner");
			while( scount.next() ) {
				shipcount.put(scount.getInt("owner"), scount.getInt("basecount"));
			}
			scount.free();
			
			query = "SELECT t1.id,t1.name,t1.race,t1.signup,t1.ally,t1.inakt,t1.flags FROM users t1 ORDER BY ";
		}
		
		if( (ord == null) || "".equals(ord) ) {
			query += "t1.id";
		} 
		else if( "id".equals(ord) || "name".equals("ord") || "race".equals(ord) || "signup".equals(ord) || "ally".equals(ord) ) {
			query += "t1."+ord;
		} 
		else if( "inakt".equals(ord) && (user != null) && (user.getAccessLevel() > 20) ) {
			query += "t1.inakt";
		} 
		else {
			query += "t1.id";
		}
		
		User.Relations relationlist = null;
		if( user != null ) {
			relationlist = user.getRelations();
		}
		
		UserIterator iter = context.createUserIterator(query);
		for( User aUser : iter ) {
			String race = "???";
			if( Rassen.get().rasse(aUser.getRace()) != null ) {
				race = Rassen.get().rasse(aUser.getRace()).getName();
			} 

			String ally = "&nbsp;";
			if( aUser.getAlly() != 0 ) {
				if( user != null ) {
					ally = "<a class=\"profile\" href=\""+Common.buildUrl(context, "details", "module", "allylist", "details", aUser.getAlly()) +"\">"+Common._title(allys.get(aUser.getAlly()))+"</a>";
				}
				else {
					ally = Common._title(allys.get(aUser.getAlly()));
				}
			} 
			
			echo.append("<tr>\n");
			
			// ID
			echo.append("<td class=\"noBorderX\">"+aUser.getID()+"</td>\n");
			
			if( comPopup == 0 ) {
				// Diplomatie
				echo.append("<td class=\"noBorderX\"><span class=\"nobr\">\n");
				
				if( (user != null) && (aUser.getID() != user.getID()) ) {
					if( !relationlist.toOther.containsKey(aUser.getID()) ) {
						relationlist.toOther.put(aUser.getID(), relationlist.toOther.get(0));	
					}
					
					if( relationlist.toOther.get(aUser.getID()) == User.Relation.ENEMY ) {
						echo.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/diplomacy/enemy1.png\" alt=\"\" title=\"Feindlich\" />");
					}
					else if( relationlist.toOther.get(aUser.getID()) == User.Relation.NEUTRAL ) {
						echo.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/diplomacy/neutral1.png\" alt=\"\" />");
					}
					else if( relationlist.toOther.get(aUser.getID()) == User.Relation.FRIEND ) {
						echo.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/diplomacy/friend1.png\" alt=\"\" title=\"Feundlich\" />");
					}
					
					if( relationlist.fromOther.get(aUser.getID()) == User.Relation.ENEMY ) {
						echo.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/diplomacy/enemy2.png\" alt=\"\" title=\"Feindlich\" />");
					}
					else if( relationlist.fromOther.get(aUser.getID()) == User.Relation.NEUTRAL ) {
						echo.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/diplomacy/neutral2.png\" alt=\"\" />");
					}
					else if( relationlist.fromOther.get(aUser.getID()) == User.Relation.FRIEND ) {
						echo.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/diplomacy/friend2.png\" alt=\"\" title=\"Feundlich\" />");
					}
				}
				
				echo.append("</td>\n");
				
				// Spielername
				if( context.getActiveUser() != null ) {
					echo.append("<td class=\"noBorderX\"><a class=\"profile\" href=\""+Common.buildUrl(context, "default", "module", "userprofile", "user", aUser.getID())+"\">"+Common._title(aUser.getName())+"</a>");
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
					if( !asticount.containsKey(aUser.getID()) ) {
						asticount.put(aUser.getID(), 0);
					}
					echo.append("<td class=\"noBorderX\" style=\"text-align:center\">"+asticount.get(aUser.getID())+"</td>\n");
					
					if( !shipcount.containsKey(aUser.getID()) ) {
						shipcount.put(aUser.getID(), 0);
					}
					echo.append("<td class=\"noBorderX\" style=\"text-align:center\">"+Common.ln(shipcount.get(aUser.getID()))+"</td>\n");
				}
			}
			else {
				echo.append("<td class=\"noBorderX\"><a style=\"font-size:14px;color:#c7c7c7\" href=\"javascript:playerPM("+aUser.getID()+");\">"+Common._title(aUser.getName())+"</a></td>\n");
			}
			
			echo.append("</tr>\n");
		}
		iter.free();
		
		echo.append("</table>\n");
	}
}
