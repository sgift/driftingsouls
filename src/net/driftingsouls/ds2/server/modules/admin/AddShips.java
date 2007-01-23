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
package net.driftingsouls.ds2.server.modules.admin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.modules.Modules;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.ships.Ships;

import org.apache.commons.lang.math.RandomUtils;

/**
 * Ermoeglicht das Erstellen von Schiffen
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Schiffe", name="hinzuf&uuml;gen")
public class AddShips implements AdminPlugin {

	public void output(AdminController controller, String page, int action) {
		Context context = ContextMap.getContext();
		StringBuffer echo = context.getResponse().getContent();
		Database db = context.getDatabase();
		User user = context.getActiveUser();
		
		int ship = context.getRequest().getParameterInt("ship");
		int count = context.getRequest().getParameterInt("count");

		if( ship == 0 ) {
			List<SQLResultRow> stlist = new ArrayList<SQLResultRow>();
			
			echo.append("<script type=\"text/javascript\">\n");
			echo.append("<!--\n");
			echo.append("var shipdata = new Array();\n");
			
			Set<String> knownwpntypes = new HashSet<String>();
			
			SQLQuery st = db.query("SELECT nickname,id,jdocks,weapons FROM ship_types");
			while( st.next() ) {
				stlist.add(st.getRow());
				echo.append("shipdata["+st.getInt("id")+"] = Array();\n");
				echo.append("shipdata["+st.getInt("id")+"][0] = "+st.getInt("jdocks")+";\n");
				echo.append("shipdata["+st.getInt("id")+"][1] = Array();\n");
				
				Set<String> thisammolist = new HashSet<String>();
				int i = 0;
				Map<String,String> weapons = Weapons.parseWeaponList(st.getString("weapons"));
				for( String weapon : weapons.keySet() ) {
					if( Weapons.get().weapon(weapon) == null ) {
						continue;	
					}
					
					if( Weapons.get().weapon(weapon).getAmmoType().equals("none") ) {
						continue;	
					}
					
					knownwpntypes.add(Weapons.get().weapon(weapon).getAmmoType());
					if( !thisammolist.contains(Weapons.get().weapon(weapon).getAmmoType()) ) {
						thisammolist.add(Weapons.get().weapon(weapon).getAmmoType());
						echo.append("shipdata["+st.getInt("id")+"][1]["+(i++)+"] = \""+Weapons.get().weapon(weapon).getAmmoType()+"\";\n");
					}
				}
			}
			st.free();
			echo.append("var ammodata = Array();\n");
			int i = 0;
			for( String ammo : knownwpntypes ) {
				echo.append("ammodata["+(i++)+"] = \""+ammo+"\"\n");
			}
			
			echo.append("-->\n");
			echo.append("</script>\n");
			echo.append("<script src=\""+Configuration.getSetting("URL")+"data/javascript/admin.addships.js\" type=\"text/javascript\"></script>\n");
			
			echo.append(Common.tableBegin(520,"left"));
			echo.append("<form action=\"./main.php\" method=\"post\">\n");
			echo.append("<table class=\"noBorderX\" width=\"100%\">\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\" style=\"width:80px\">Schifftyp:</td>");
			echo.append("<td class=\"noBorderX\">");
			echo.append("<select name=\"ship\" size=\"1\" onchange=\"shipSelectChange(this.options[this.options.selectedIndex].value)\">");
			for( i=0; i < stlist.size(); i++ ) {
				echo.append("<option value=\""+stlist.get(i).getInt("id")+"\">"+Common._plaintitle(stlist.get(i).getString("nickname"))+" ("+stlist.get(i).getInt("id")+")</option>\n");
			}
			echo.append("</select>\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\">Spieler:</td>\n");
			echo.append("<td class=\"noBorderX\"><input type=\"text\" name=\"owner\" size=\"6\" /></td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\">Pos:</td>\n");
			echo.append("<td class=\"noBorderX\">\n");
			echo.append("<input type=\"text\" name=\"system\" size=\"3\" />:<input type=\"text\" name=\"x\" size=\"3\" />/<input type=\"text\" name=\"y\" size=\"3\" />\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\">Menge:</td>\n");
			echo.append("<td class=\"noBorderX\"><input type=\"text\" name=\"count\" size=\"20\" value=\"1\" /></td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\">Name:</td>\n");
			echo.append("<td class=\"noBorderX\"><input type=\"text\" name=\"name\" size=\"20\" value=\"noname\" /></td>\n");
			echo.append("</tr>\n");
			echo.append("<tr id=\"tbl_ammo\">\n");
			echo.append("<td class=\"noBorderX\" style=\"vertial-align:top\">Munition:</td>\n");
			echo.append("<td class=\"noBorderX\">\n");
			echo.append("<div id=\"tbl_ammo_div\" style=\"margin:0px;border:0px;padding:0px;position:relative;top:0px;left:0px;\">\n");
			for( String ammo : knownwpntypes ) {
				echo.append("<div style=\"margin:0px;border:0px;padding:0px;display:inline;position:absolute;top:0px;left:0px\" id=\"select_ammo_"+ammo+"\">\n");
				echo.append("<select id=\"select_ammo_"+ammo+"_element\" name=\"ammo_"+ammo+"\" size=\"1\">\n");
				echo.append("<option id=\"0\">[Nichts]</option>\n");
				SQLQuery ammoRow = db.query("SELECT name,itemid FROM ammo WHERE type='"+ammo+"'");
				while( ammoRow.next() ) {
					echo.append("<option value=\""+ammoRow.getInt("itemid")+"\">"+Common._plaintitle(ammoRow.getString("name"))+"</option>\n");
				}
				ammoRow.free();
				echo.append("</select>\n");
				echo.append("</div>\n");
			}
			echo.append("</div>\n");	
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\">Offizier (Typ,Name):</td>\n");
			echo.append("<td class=\"noBorderX\">\n");
			echo.append("<select name=\"offitype\" size=\"1\">\n");
			echo.append("<option value=\"-1\" selected=\"selected\">keiner</option>\n");
			SQLQuery offi = db.query("SELECT id,name FROM orders_offiziere");
			while( offi.next() ) {
				echo.append("<option value=\""+offi.getInt("id")+"\">"+offi.getString("name")+"</option>\n");
			}
			offi.free();
			echo.append("</select>\n");
			echo.append("<input type=\"text\" name=\"offiname\" value=\"Captain\" />\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr id=\"tbl_jaeger\" style=\"visibility:visible\">\n");
			echo.append("<td class=\"noBorderX\">J&auml;ger:</td>\n");
			echo.append("<td class=\"noBorderX\">\n");
			echo.append("<select name=\"jaeger\" size=\"1\" onchange=\"jaegerSelectChange(this.options[this.options.selectedIndex].value)\">\n");
			echo.append("<option id=\"0\">[Nichts]</option>\n");
			st = db.query("SELECT nickname,id FROM ship_types WHERE LOCATE('"+Ships.SF_JAEGER+"',flags)");
			while( st.next() ) {
				echo.append("<option value=\""+st.getInt("id")+"\">"+Common._plaintitle(st.getString("nickname"))+" ("+st.getInt("id")+")</option>\n");
			}
			st.free();
			echo.append("</select>\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr id=\"tbl_jaeger_ammo\" style=\"visibility:hidden\">\n");
			echo.append("<td class=\"noBorderX\" style=\"vertial-align:top\">Munition [J&auml;ger]:</td>\n");
			echo.append("<td class=\"noBorderX\">\n");
			echo.append("<div id=\"tbl_jaeger_ammo_div\" style=\"margin:0px;border:0px;padding:0px;position:relative;top:0px;left:0px;\">\n");
			for( String ammo : knownwpntypes ) {
				echo.append("<div style=\"margin:0px;border:0px;padding:0px;display:inline;position:absolute;top:0px;left:0px\" id=\"select_jaeger_ammo_"+ammo+"\">\n");
				echo.append("<select name=\"jaeger_ammo_"+ammo+"\" size=\"1\">\n");
				echo.append("<option id=\"0\">[Nichts]</option>\n");
				SQLQuery ammoRow = db.query("SELECT name,itemid FROM ammo WHERE type='"+ammo+"'");
				while( ammoRow.next() ) {
					echo.append("<option value=\""+ammoRow.getInt("itemid")+"\">"+Common._plaintitle(ammoRow.getString("name"))+"</option>\n");
				}
				ammoRow.free();
				echo.append("</select>\n");
				echo.append("</div>\n");
			}
			echo.append("</div>\n");	
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\" colspan=\"2\">\n");
			echo.append("<input type=\"checkbox\" name=\"inteliid\" id=\"form_inteliid\" value=\"1\" /><label for=\"form_inteliid\">Erste freie ID verwenden</label>\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\" colspan=\"2\">\n");
			echo.append("<input type=\"checkbox\" name=\"lowid\" id=\"form_lowid\" value=\"1\" /><label for=\"form_lowid\">Low ID (&lt;10000 - nur wenn es WIRKLICH notwendig ist)</label>\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\" colspan=\"2\" id=\"tbl_jaeger_lowid\" style=\"visibility:hidden\">\n");
			echo.append("<input type=\"checkbox\" name=\"jlowid\" id=\"form_jlowid\" value=\"1\" /><label for=\"form_jlowid\">Low ID f&uuml;r J&auml;ger (&lt;10000 - s.o.)</label>\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\" colspan=\"2\">\n");
			echo.append("<input type=\"checkbox\" name=\"replikator\" id=\"form_replikator\" value=\"1\" /><label for=\"form_replikator\">Replikator (Nicht f&uuml;r J&auml;ger)</label>\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr><td class=\"noBorderX\" colspan=\"2\" align=\"center\">\n");
			echo.append("<input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<br /><input type=\"submit\" value=\"senden\" style=\"width:200px\" />\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("</table>\n");
			echo.append("</form>\n");
			echo.append("<script type=\"text/javascript\">\n");
			echo.append("<!--\n");
			echo.append("shipSelectChange(1);\n");
			echo.append("-->\n");
			echo.append("</script>\n");
			echo.append(Common.tableEnd());
		}
		else if( count > 0 ) {
			int owner = context.getRequest().getParameterInt("owner");
			int system = context.getRequest().getParameterInt("system");
			int x = context.getRequest().getParameterInt("x");
			int y = context.getRequest().getParameterInt("y");
			String name = context.getRequest().getParameterString("name");
			int offitype = context.getRequest().getParameterInt("offitype");
			String offiname = context.getRequest().getParameterString("offiname");
			int jaeger = context.getRequest().getParameterInt("jaeger");
			int inteliid = context.getRequest().getParameterInt("inteliid");
			int lowid = context.getRequest().getParameterInt("lowid");
			int jlowid = context.getRequest().getParameterInt("jlowid");
			int replikator = context.getRequest().getParameterInt("replikator");
			
			db.tBegin();
			
			String currentTime = Common.getIngameTime(context.get(ContextCommon.class).getTick());
			
			SQLResultRow shiptype = Ships.getShipType(ship, false);
			Cargo cargo = new Cargo();
			cargo.addResource( Resources.DEUTERIUM, shiptype.getInt("rd")*10 );
			cargo.addResource( Resources.URAN, shiptype.getInt("ru")*10 );
			cargo.addResource( Resources.ANTIMATERIE, shiptype.getInt("ra")*10 );
			
			Map<String,String> weapons = Weapons.parseWeaponList(shiptype.getString("weapons"));
			for( String weapon : weapons.keySet() ) {
				if( Weapons.get().weapon(weapon) != null && !Weapons.get().weapon(weapon).getAmmoType().equals("none") ) {
					String ammotype = Weapons.get().weapon(weapon).getAmmoType();
					if( context.getRequest().getParameterInt("ammo_"+ammotype) > 0 )	{
						cargo.addResource( 
								new ItemID(context.getRequest().getParameterInt("ammo_"+ammotype)), 
								Integer.parseInt(weapons.get(weapon))*10 );
					}
				}
			}
			for( int i=0; i < count; i++ ) {
				User auser = context.createUserObject(owner);	
				String history = "Indienststellung am "+currentTime+" durch "+auser.getName()+" ("+auser.getID()+") [hide]Admin: "+user.getID()+"[/hide]\n";
				
				// Schiff erstellen
				PreparedQuery query = null;
				if( inteliid != 0 ) {
					int shouldId = 10000;
					if( lowid != 0 ) {
						shouldId = 0;
					}
					
					query = db.prepare("INSERT INTO ships (id,name,type,owner,x,y,system,hull,e,crew,shields,cargo,history) "+ 
								"VALUES (newIntelliShipID( ? ), ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? )");
					query.update(shouldId, name, ship, owner, x, y, system, shiptype.getInt("hull"), shiptype.getInt("eps"), shiptype.getInt("crew"), shiptype.getInt("shields"), cargo.save(), history);
				}
				else {
					query = db.prepare("INSERT INTO ships (name,type,owner,x,y,system,hull,e,crew,shields,cargo,history) "+ 
								"VALUES (? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? )");
					query.update(name, ship, owner, x, y, system, shiptype.getInt("hull"), shiptype.getInt("eps"), shiptype.getInt("crew"), shiptype.getInt("shields"), cargo.save(), history);
				}
				int shipid = query.insertID();
				query.close();
				
				if( shiptype.getString("werft").length() > 0 ) {
					db.update("INSERT INTO werften (shipid) VALUES ("+shipid+")");
				}

				echo.append("Schiff ("+shipid+") hinzugef&uuml;gt<br />");
				
				// Offizier einfuegen
				if( (offitype > 0) && (offiname.length() > 0) ) {
					SQLResultRow offi = db.first("SELECT * FROM orders_offiziere WHERE id="+offitype);
					if( !offi.isEmpty() ) {
						db.prepare("INSERT INTO offiziere (name,rang,ing,waf,nav,sec,com,dest,userid,spec) "+
									"VALUES ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? )")
							.update(offiname, offi.getInt("rang"), offi.getInt("ing"), offi.getInt("waf"), 
									offi.getInt("nav"), offi.getInt("sec"), offi.getInt("com"), "s "+shipid, 
									owner, RandomUtils.nextInt(6));
						
						echo.append("Offizier '"+offiname+"' hinzugef&uuml;gt<br />\n");
					}
				}
				
				Ships.recalculateShipStatus(shipid);
				
				if( replikator != 0 ) {
					SQLResultRow shipRow = db.first("SELECT * FROM ships WHERE id="+shipid);
					Ships.addModule(shipRow, 1, Modules.MODULE_ITEMMODULE, "304");
				}
				
				// Jaeger einfuegen
				if( (jaeger > 0) && (shiptype.getInt("jdocks")>0) ) {
					echo.append("F&uuml;ge J&auml;ger ein:<br />\n");
					SQLResultRow jshiptype = Ships.getShipType(jaeger, false);

					query = db.prepare("INSERT INTO ship_fleets (name) VALUES ( ? )");
					query.update(name+"-Staffel");
					int fleetid = query.insertID();
					query.close();
				
					Cargo jcargo = new Cargo();
					jcargo.addResource( Resources.DEUTERIUM, jshiptype.getInt("rd")*10 );
					jcargo.addResource( Resources.URAN, jshiptype.getInt("ru")*10 );
					jcargo.addResource( Resources.ANTIMATERIE, jshiptype.getInt("ra")*10 );
					
					weapons = Weapons.parseWeaponList(jshiptype.getString("weapons"));
					for( String weapon : weapons.keySet() ) {
						if( Weapons.get().weapon(weapon) != null && !Weapons.get().weapon(weapon).getAmmoType().equals("none") ) {
							String ammotype = Weapons.get().weapon(weapon).getAmmoType();
							if( context.getRequest().getParameterInt("jaeger_ammo_"+ammotype) > 0 )	{
								jcargo.addResource( 
										new ItemID(context.getRequest().getParameterInt("jaeger_ammo_"+ammotype)), 
										Integer.parseInt(weapons.get(weapon))*10 );
							}
						}
					}

					for( int j=1; j <= shiptype.getInt("jdocks"); j++ ) {
						history = "Indienststellung am "+currentTime+" durch "+auser.getName()+" ("+auser.getID()+") [hide]Admin: "+user.getID()+"[/hide]\n";
						
						// Jaeger erstellen
						query = null;
						if( inteliid != 0 ) {
							int shouldId = 10000;
							if( jlowid != 0 ) {
								shouldId = 0;
							}
							
							query = db.prepare("INSERT INTO ships (id,name,type,owner,x,y,system,hull,e,crew,shields,docked,fleet,cargo,history) "+ 
										"VALUES (newIntelliShipID( ? ), ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ?, ?, ? )");
							query.update(shouldId, name+" "+j, jaeger, owner, x, y, system, jshiptype.getInt("hull"), jshiptype.getInt("eps"), jshiptype.getInt("crew"), jshiptype.getInt("shields"), "l "+shipid, fleetid, jcargo.save(), history);
						}
						else {
							query = db.prepare("INSERT INTO ships (name,type,owner,x,y,system,hull,e,crew,shields,docked,fleet,cargo,history) "+ 
										"VALUES (newIntelliShipID( ? ), ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ?, ?, ? )");
							query.update(name+" "+j, jaeger, owner, x, y, system, jshiptype.getInt("hull"), jshiptype.getInt("eps"), jshiptype.getInt("crew"), jshiptype.getInt("shields"), "l "+shipid, fleetid, jcargo.save(), history);
						}
						int insid = query.insertID();
						query.close();
						
						Ships.recalculateShipStatus(insid);
					
						echo.append("J&auml;ger ("+insid+") hinzugef&uuml;gt<br />");
					} // For jdocks
				} // if Jaeger
				
			} // For Schiffe
			
			db.tCommit();			
		} // if Schiffe erstellen
	}
}
