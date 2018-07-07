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

import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.units.UnitType;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Zeigt die insgesamt vorkommenden sowie die eigenen Einheiten an.
 *
 */
public class StatEinheiten implements Statistic {
    @Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		User user = (User)context.getActiveUser();

		Writer echo = context.getResponse().getWriter();


		List<UnitType> unitlist = Common.cast(db.createQuery("from UnitType").list());

		// Ausgabe des Tabellenkopfs
		echo.append("<table cellspacing=\"1\" cellpadding=\"1\">\n");
		echo.append("<tr><td align=\"left\" colspan=\"3\">Einheiten:</td></tr>\n");
		echo.append("<tr><td align=\"left\" width=\"200\">&nbsp;</td>\n");
		echo.append("<td>Alle</td>\n");
		echo.append("<td width=\"15\">&nbsp;</td>\n");
		echo.append("<td>Eigene</td>\n");
		echo.append("</tr>\n");

		// Einheitenliste durchlaufen
		for( UnitType unittype : unitlist ) {

			long baseunits = 0;
			long shipunits = 0;
			long baseunitsuser = 0;
			long shipunitsuser = 0;

			Object baseunitsobject = db.createQuery("select sum(e.amount) from BaseUnitCargoEntry as e where e.unittype=:unittype and e.basis.owner.id > 0")
										.setInteger("unittype", unittype.getId())
										.iterate()
										.next();

			if( baseunitsobject != null)
			{
				baseunits = (Long)baseunitsobject;
			}

			Object shipunitsobject = db.createQuery("select sum(e.amount) from ShipUnitCargoEntry as e where e.unittype=:unittype and e.schiff.owner.id > 0")
										.setInteger("unittype", unittype.getId())
										.iterate()
										.next();

			if( shipunitsobject != null)
			{
				shipunits = (Long)shipunitsobject;
			}

			Object baseunitsuserobject = db.createQuery("select sum(e.amount) from BaseUnitCargoEntry as e where e.unittype=:unittype and e.basis.owner=:user")
											.setInteger("unittype", unittype.getId())
											.setEntity("user", user)
											.iterate()
											.next();

			if( baseunitsuserobject != null)
			{
				baseunitsuser = (Long)baseunitsuserobject;
			}

			Object shipunitsuserobject = db.createQuery("select sum(e.amount) from ShipUnitCargoEntry as e where e.unittype=:unittype and e.schiff.owner=:user")
											.setInteger("unittype", unittype.getId())
											.setEntity("user", user)
											.iterate()
											.next();

			if( shipunitsuserobject != null)
			{
				shipunitsuser = (Long)shipunitsuserobject;
			}

			// Daten ausgeben, wenn der Spieler sehen darf oder selber welche besitzt
			if( !unittype.isHidden() || context.hasPermission(WellKnownPermission.UNIT_VERSTECKTE_SICHTBAR) || baseunitsuser + shipunitsuser > 0)
			{
				// Daten zur Einheit ausgeben
	      		echo.append("<tr>\n");
	      		echo.append("<td style=\"white-space:nowrap\"><img style=\"vertical-align:middle\" src=\"").append(unittype.getPicture()).append("\" alt=\"\"><a href=\"").append(Common.buildUrl("default", "module", "unitinfo", "unit", unittype.getId())).append("\" >").append(unittype.getName()).append("</a>");
	      		if( unittype.isHidden() && context.hasPermission(WellKnownPermission.UNIT_VERSTECKTE_SICHTBAR))
	      		{
	      			echo.append("[hidden]");
	      		}
	      		echo.append("</td>\n");
	      		echo.append("<td>").append(Long.toString(baseunits + shipunits)).append("</td>\n");
	      		echo.append("<td>&nbsp;</td>\n");
	      		echo.append("<td>").append(Long.toString(baseunitsuser + shipunitsuser)).append("</td>\n");
	      		echo.append("<td>&nbsp;</td>\n");
			}

		} // Ende: Einheitenliste
		echo.append("</table><br /><br />\n");
	}
}
