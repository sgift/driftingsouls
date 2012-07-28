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

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Zeigt die insgesamt vorkommenden sowie die eigenen Einheiten an.
 *
 */
@Configurable
public class StatEinheiten implements Statistic {
	@SuppressWarnings("unused")
	private Configuration config;

    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config)
    {
    	this.config = config;
    }

    @Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		User user = (User)context.getActiveUser();

		Writer echo = context.getResponse().getWriter();


		List<UnitType> unitlist = Common.cast(db.createQuery("from UnitType").list());

		// Ausgabe des Tabellenkopfs
		echo.append("<table class=\"noBorderX\" cellspacing=\"1\" cellpadding=\"1\">\n");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"3\">Einheiten:</td></tr>\n");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\" width=\"200\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\">Alle</td>\n");
		echo.append("<td class=\"noBorderX\" width=\"15\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\">Eigene</td>\n");
		echo.append("</tr>\n");

		// Einheitenliste durchlaufen
		for( UnitType unittype : unitlist ) {

			long baseunits = 0;
			long shipunits = 0;
			long baseunitsuser = 0;
			long shipunitsuser = 0;

			Object baseunitsobject = db.createQuery("select sum(e.amount) from UnitCargoEntry as e, Base as b where e.key.type=:type and e.key.unittype=:unittype and e.key.destid = b.id and b.owner > 0")
										.setInteger("type", UnitCargo.CARGO_ENTRY_BASE)
										.setInteger("unittype", unittype.getId())
										.iterate()
										.next();

			if( baseunitsobject != null)
			{
				baseunits = (Long)baseunitsobject;
			}

			Object shipunitsobject = db.createQuery("select sum(e.amount) from UnitCargoEntry as e, Ship as s where e.key.type=:type and e.key.unittype=:unittype and e.key.destid = s.id and s.owner > 0")
										.setInteger("type", UnitCargo.CARGO_ENTRY_SHIP)
										.setInteger("unittype", unittype.getId())
										.iterate()
										.next();

			if( shipunitsobject != null)
			{
				shipunits = (Long)shipunitsobject;
			}

			Object baseunitsuserobject = db.createQuery("select sum(e.amount) from UnitCargoEntry as e, Base as b where e.key.type=:type and e.key.unittype=:unittype and e.key.destid = b.id and b.owner=:user")
											.setInteger("type", UnitCargo.CARGO_ENTRY_BASE)
											.setInteger("unittype", unittype.getId())
											.setEntity("user", user)
											.iterate()
											.next();

			if( baseunitsuserobject != null)
			{
				baseunitsuser = (Long)baseunitsuserobject;
			}

			Object shipunitsuserobject = db.createQuery("select sum(e.amount) from UnitCargoEntry as e, Ship as s where e.key.type=:type and e.key.unittype=:unittype and e.key.destid = s.id and s.owner=:user")
											.setInteger("type", UnitCargo.CARGO_ENTRY_SHIP)
											.setInteger("unittype", unittype.getId())
											.setEntity("user", user)
											.iterate()
											.next();

			if( shipunitsuserobject != null)
			{
				shipunitsuser = (Long)shipunitsuserobject;
			}

			// Daten ausgeben, wenn der Spieler sehen darf oder selber welche besitzt
			if( !unittype.isHidden() || context.hasPermission("unit", "versteckteSichtbar") || baseunitsuser + shipunitsuser > 0)
			{
				// Daten zur Einheit ausgeben
	      		echo.append("<tr>\n");
	      		echo.append("<td class=\"noBorderX\" style=\"white-space:nowrap\"><img style=\"vertical-align:middle\" src=\""+unittype.getPicture()+"\" alt=\"\"><a href=\""+Common.buildUrl("default", "module", "unitinfo", "unit", unittype.getId())+"\" >"+unittype.getName()+"</a>");
	      		if( unittype.isHidden() && context.hasPermission("unit", "versteckteSichtbar"))
	      		{
	      			echo.append("[hidden]");
	      		}
	      		echo.append("</td>\n");
	      		echo.append("<td class=\"noBorderX\">"+(baseunits+shipunits)+"</td>\n");
	      		echo.append("<td class=\"noBorderX\">&nbsp;</td>\n");
	      		echo.append("<td class=\"noBorderX\">"+(baseunitsuser+shipunitsuser)+"</td>\n");
	      		echo.append("<td class=\"noBorderX\">&nbsp;</td>\n");
			}

		} // Ende: Einheitenliste
		echo.append("</table><br /><br />\n");
	}
}
