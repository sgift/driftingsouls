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

import java.util.HashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.items.Items;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.StatsController;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Zeigt die insgesamt vorkommenden sowie die eigenen Waren an. Bei Items werden zudem,
 * falls vorhanden, die Aufenthaltsorte angezeigt
 * @author Christopher Jung
 *
 */
public class StatWaren implements Statistic {
	public void show(StatsController contr, int size) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		User user = (User)context.getActiveUser();

		StringBuffer echo = context.getResponse().getContent();
	
		Cargo cargo = new Cargo(Cargo.Type.STRING, db.first("SELECT cargo FROM stats_cargo ORDER BY tick DESC LIMIT 1").getString("cargo"));
		
		SQLResultRow userCargo = db.first("SELECT cargo FROM stats_user_cargo WHERE user_id=",user.getId());
		Cargo owncargo = null;
		if( !userCargo.isEmpty() ) {
			owncargo = new Cargo(Cargo.Type.STRING, userCargo.getString("cargo"));
		}
		else {
			owncargo = new Cargo();
		}

		// Ausgabe des Tabellenkopfs
		echo.append("<table class=\"noBorderX\" cellspacing=\"1\" cellpadding=\"1\">\n");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"3\">Waren:</td></tr>\n");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\" width=\"200\">&nsbp;</td>\n");
		echo.append("<td class=\"noBorderX\">Alle</td>\n");
		echo.append("<td class=\"noBorderX\" width=\"15\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\">Eigene</td>\n");
		echo.append("<td class=\"noBorderX\" width=\"15\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\">&nbsp;</td>\n");
		echo.append("</tr>\n");
		
		// Itempositionen auslesen
		Map<Integer,String[]> reslocationlist = new HashMap<Integer,String[]>();
		SQLQuery amodule = db.query("SELECT item_id,locations FROM stats_module_locations WHERE user_id=",user.getId());
		while( amodule.next() ) {
			reslocationlist.put(amodule.getInt("item_id"), StringUtils.split(amodule.getString("locations"), ';'));
		}
		amodule.free();
		
		// Caches fuer Schiffe und Basen
		Map<Integer,SQLResultRow> basecache = new HashMap<Integer,SQLResultRow>();
		Map<Integer,String> shipnamecache = new HashMap<Integer,String>();
		
		// Diese Grafiken kennzeichen bei Itempositionen den Typ der Position
		final String shipimage = "<td class='noBorderX' style='text-align:right'><img style='vertical-align:middle' src='"+Configuration.getSetting("URL")+"data/interface/schiffe/"+user.getRace()+"/icon_schiff.gif' alt='' title='Schiff' /></td>";
		final String baseimage = "<td class='noBorderX' style='text-align:right'><img style='vertical-align:middle;width:15px;height:15px' src='"+Configuration.getSetting("URL")+"data/starmap/asti/asti.png' alt='' title='Asteroid' /></td>";
	
		// Resourcenliste durchlaufen
		ResourceList reslist = cargo.compare(owncargo, false);
		for( ResourceEntry res : reslist ) {
			// Wenn die Resource ein Item ist, dann pruefen, ob dieses angezeigt werden darf
			if( res.getId().isItem() ) {
				int itemid = res.getId().getItemID();
				if( Items.get().item(itemid) == null ) {
					continue;
				}
				if( Items.get().item(itemid).getAccessLevel() > user.getAccessLevel() ) {
					continue;
				}
				if( Items.get().item(itemid).isUnknownItem() && !user.isKnownItem(itemid) && (user.getAccessLevel() < 15) ) {
					continue;
				}
			}
			
			// Daten zur Resource ausgeben
      		echo.append("<tr>\n");
      		echo.append("<td class=\"noBorderX\" style=\"white-space:nowrap\"><img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\">"+res.getName()+"</td>\n");
      		echo.append("<td class=\"noBorderX\">"+res.getCargo1()+"</td>\n");
      		echo.append("<td class=\"noBorderX\">&nbsp;</td>\n");
      		echo.append("<td class=\"noBorderX\">"+res.getCargo2()+"</td>\n");
      		echo.append("<td class=\"noBorderX\">&nbsp;</td>\n");
      		echo.append("<td class=\"noBorderX\">\n");
			
      		// Wenn es sich um ein Item handelt und einige Positionsangaben fuer dieses Item beim Spieler
      		// vorliegen -> diese anzeigen!
			if( res.getId().isItem() && reslocationlist.containsKey(res.getId().getItemID()) ) {
				// Die Darstellung erfolgt als Tooltip
				StringBuilder tooltip = new StringBuilder();
				tooltip.append(StringUtils.replaceChars(Common.tableBegin(350, "left"), '"', '\''));
				tooltip.append("<table class='noBorderX'>");
				
				// Alle Positionen durchgehen
				String[] locations = reslocationlist.get(res.getId().getItemID());
				for( int i=0; i < locations.length; i++ ) {
					String alocation = locations[i]; 
					
					// Das erste Zeichen ist der Typ der Position. Der Rest ist die ID
					int objectid = Integer.parseInt(alocation.substring(1));
					
					tooltip.append("<tr>");
					switch( alocation.charAt(0) ) {
					// Positionstyp Schiff
					case 's':
						if( !shipnamecache.containsKey(objectid) ) {
							SQLResultRow ship = db.first("SELECT name FROM ships WHERE id=",objectid);
							if( ship.isEmpty() ) {
								tooltip.append("</tr>");
								continue;
							}
							shipnamecache.put(objectid, Common._plaintitle(ship.getString("name")));
						}
						tooltip.append(shipimage+"<td class='noBorderX'>" +
								"<a style='font-size:14px' class='forschinfo' " +
								"href='"+Common.buildUrl("default", "module", "schiff", "ship", objectid)+"'>"+
								shipnamecache.get(objectid)+" ("+objectid+")</a></td>");
						break;

					// Positionstyp Basis 
					case 'b':
						if( !basecache.containsKey(objectid) ) {
							basecache.put(objectid, db.first("SELECT name,x,y,system FROM bases WHERE id=",objectid));
						}
						tooltip.append(baseimage+"<td class='noBorderX'>" +
								"<a style='font-size:14px' class='forschinfo' " +
								"href='"+Common.buildUrl("default", "module", "base", "col", objectid)+"'>"+
								Common._plaintitle(basecache.get(objectid).getString("name"))+" - "+
								Location.fromResult(basecache.get(objectid))+
								"</a></td>");
						break;
					
					// Positionstyp Gtu-Zwischenlager
					case 'g':
						if( !shipnamecache.containsKey(objectid) ) {
							shipnamecache.put(objectid, Common._plaintitle(db.first("SELECT name FROM ships WHERE id=",objectid).getString("name")));
						}
						tooltip.append("<td colspan='2' class='noBorderX' style='font-size:14px'>"+
								shipnamecache.get(objectid)+"</td>");
						break;
					
					// Falls der Typ unbekannt ist: Warnmeldung ausgeben
					default:
						tooltip.append("<td colspan='2' class='noBorderX' style='font-size:14px'>Unbekanntes Objekt "+alocation+"</td>");
					}
					
					tooltip.append("</tr>");
				}
				tooltip.append("</table>");
				tooltip.append(StringUtils.replaceChars(Common.tableEnd(), '"', '\''));
				String tooltipStr = StringEscapeUtils.escapeJavaScript(StringUtils.replace(StringUtils.replace(tooltip.toString(), "<", "&lt;"), ">", "&gt;"));

				// Linkt mit Tooltip ausgeben
				echo.append("<a style=\"forschinfo\" name=\"module"+res.getId().getItemID()+"_popup\" " +
						"id=\"module"+res.getId().getItemID()+"_popup\" class=\"forschinfo\" " +
						"onmouseover=\"javascript:overlib('"+tooltipStr+"', REF,'module"+res.getId().getItemID()+"_popup', REFY,22,REFX,-150,FGCLASS,'gfxtooltip',BGCLASS,'gfxclass',TEXTFONTCLASS,'gfxclass',TIMEOUT,0,NOCLOSE,STICKY);\" " +
						"onmouseout=\"return nd();\" href=\"#\">Wo?</a>\n");
				
			} // Ende: Itempositionen
			
			echo.append("</td>");
			echo.append("</tr>\n");
			
		} // Ende: Resourcenliste
		echo.append("</table><br /><br />\n");
	}

	public boolean generateAllyData() {
		return false;
	}
	
	public int getRequiredData() {
		return 0;
	}
}
