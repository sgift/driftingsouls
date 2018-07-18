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

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.statistik.StatItemLocations;
import net.driftingsouls.ds2.server.entities.statistik.StatUserCargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.ships.Ship;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Zeigt die insgesamt vorkommenden sowie die eigenen Waren an. Bei Items werden zudem,
 * falls vorhanden, die Aufenthaltsorte angezeigt.
 * @author Christopher Jung
 *
 */
public class StatWaren implements Statistic {

    @Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		User user = (User)context.getActiveUser();

		Writer echo = context.getResponse().getWriter();

		Iterator iterator = db.createQuery("SELECT cargo FROM StatCargo ORDER BY tick DESC").setMaxResults(1).iterate();
		if( !iterator.hasNext() )
		{
			echo.append("Keine Datenbasis vorhanden");
			return;
		}
		Cargo cargo = new Cargo((Cargo) iterator.next());

		StatUserCargo userCargo = (StatUserCargo) db.createQuery("from StatUserCargo where user=:user")
				.setEntity("user", user)
				.uniqueResult();
		Cargo ownCargo = null;
		if( userCargo != null )
		{
			ownCargo = userCargo.getCargo();
		}
		if( ownCargo == null ) {
			ownCargo = new Cargo();
		}

		// Ausgabe des Tabellenkopfs
		echo.append("<table class=\"noBorderX\" cellspacing=\"1\" cellpadding=\"1\">\n");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"3\">Waren:</td></tr>\n");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\" width=\"200\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\">Alle</td>\n");
		echo.append("<td class=\"noBorderX\" width=\"15\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\">Eigene</td>\n");
		echo.append("<td class=\"noBorderX\" width=\"15\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\">&nbsp;</td>\n");
		echo.append("</tr>\n");

		// Itempositionen auslesen
		Map<Integer,String[]> reslocationlist = new HashMap<>();
		List<StatItemLocations> modules = Common.cast(db.createQuery("FROM StatItemLocations WHERE user=:user").setEntity("user",user).list());
		for( StatItemLocations amodule : modules ) {
			reslocationlist.put(amodule.getUser().getId(), StringUtils.split(amodule.getLocations(), ';'));
		}

		// Caches fuer Schiffe und Basen
		Map<Integer,Base> basecache = new HashMap<>();
		Map<Integer,String> shipnamecache = new HashMap<>();

		// Diese Grafiken kennzeichen bei Itempositionen den Typ der Position
		final String shipimage = "<td class='noBorderX' style='text-align:right'><img style='vertical-align:middle' src='./data/interface/schiffe/"+user.getRace()+"/icon_schiff.gif' alt='' title='Schiff' /></td>";
		final String baseimage = "<td class='noBorderX' style='text-align:right'><img style='vertical-align:middle;width:15px;height:15px' src='./data/starmap/asti/asti.png' alt='' title='Asteroid' /></td>";

		// Resourcenliste durchlaufen
		ResourceList reslist = cargo.compare(ownCargo, false);
		for( ResourceEntry res : reslist ) {
			// Wenn die Resource ein Item ist, dann pruefen, ob dieses angezeigt werden darf
			int itemid = res.getId().getItemID();
			Item item = (Item)db.get(Item.class, itemid);
			if( item == null || !user.canSeeItem(item) ) {
				continue;
			}

			// Daten zur Resource ausgeben
      		echo.append("<tr>\n");
      		echo.append("<td class=\"noBorderX\" style=\"white-space:nowrap\"><img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\">").append(res.getName()).append("</td>\n");
      		echo.append("<td class=\"noBorderX\">").append(res.getCargo1()).append("</td>\n");
      		echo.append("<td class=\"noBorderX\">&nbsp;</td>\n");
      		echo.append("<td class=\"noBorderX\">").append(res.getCargo2()).append("</td>\n");
      		echo.append("<td class=\"noBorderX\">&nbsp;</td>\n");
      		echo.append("<td class=\"noBorderX\">\n");

      		// Wenn es sich um ein Item handelt und einige Positionsangaben fuer dieses Item beim Spieler
      		// vorliegen -> diese anzeigen!
			if( reslocationlist.containsKey(res.getId().getItemID()) ) {
				// Die Darstellung erfolgt als Tooltip
				StringBuilder tooltip = new StringBuilder();
				tooltip.append("<table class='noBorderX'>");

				// Alle Positionen durchgehen
				String[] locations = reslocationlist.get(res.getId().getItemID());
				for (String alocation : locations)
				{
					// Das erste Zeichen ist der Typ der Position. Der Rest ist die ID
					int objectid = Integer.parseInt(alocation.substring(1));

					tooltip.append("<tr>");
					switch (alocation.charAt(0))
					{
						// Positionstyp Schiff
						case 's':
							if (!shipnamecache.containsKey(objectid))
							{
								Ship ship = (Ship) db.get(Ship.class, objectid);
								if (ship == null)
								{
									tooltip.append("</tr>");
									continue;
								}
								shipnamecache.put(objectid, Common._plaintitle(ship.getName()));
							}
							tooltip.append(shipimage).append("<td class='noBorderX'>").append("<a style='font-size:14px' class='forschinfo' ").append("href='").append(Common.buildUrl("default", "module", "schiff", "ship", objectid)).append("'>").append(shipnamecache.get(objectid)).append(" (").append(objectid).append(")</a></td>");
							break;

						// Positionstyp Basis
						case 'b':
							if (!basecache.containsKey(objectid))
							{
								Base base = (Base) db.get(Base.class, objectid);
								if (base != null)
								{
									basecache.put(objectid, base);
								}
							}
							tooltip.append(baseimage + "<td class='noBorderX'>" + "<a style='font-size:14px' class='forschinfo' " + "href='").append(Common.buildUrl("default", "module", "base", "col", objectid)).append("'>").append(Common._plaintitle(basecache.get(objectid).getName())).append(" - ").append(basecache.get(objectid).getLocation().displayCoordinates(false)).append("</a></td>");
							break;

						// Positionstyp Gtu-Zwischenlager
						case 'g':
							if (!shipnamecache.containsKey(objectid))
							{
								Ship ship = (Ship) db.get(Ship.class, objectid);
								if (ship != null)
								{
									shipnamecache.put(objectid, Common._plaintitle(ship.getName()));
								}
							}
							tooltip.append("<td colspan='2' class='noBorderX' style='font-size:14px'>").append(shipnamecache.get(objectid)).append("</td>");
							break;

						// Falls der Typ unbekannt ist: Warnmeldung ausgeben
						default:
							tooltip.append("<td colspan='2' class='noBorderX' style='font-size:14px'>Unbekanntes Objekt ").append(alocation).append("</td>");
					}

					tooltip.append("</tr>");
				}
				tooltip.append("</table>");


				// Linkt mit Tooltip ausgeben
				echo.append("<a class=\"forschinfo tooltip\" href=\"#\">Wo?<span class='ttcontent'>").append(tooltip).append("</span></a>\n");

			} // Ende: Itempositionen

			echo.append("</td>");
			echo.append("</tr>\n");

		} // Ende: Resourcenliste
		echo.append("</table><br /><br />\n");
	}
}
