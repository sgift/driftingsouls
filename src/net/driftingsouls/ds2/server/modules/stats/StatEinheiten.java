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
import java.util.Map.Entry;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Zeigt die insgesamt vorkommenden sowie die eigenen Waren an. Bei Items werden zudem,
 * falls vorhanden, die Aufenthaltsorte angezeigt.
 * @author Christopher Jung
 *
 */
@Configurable
public class StatEinheiten implements Statistic {
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
		Database db = context.getDatabase();
		org.hibernate.Session database = context.getDB();
		User user = (User)context.getActiveUser();

		Writer echo = context.getResponse().getWriter();
	
		UnitCargo unitcargo = new UnitCargo(db.first("SELECT unitcargo FROM stats_unitcargo ORDER BY tick DESC LIMIT 1").getString("unitcargo"));
		
		SQLResultRow userUnitCargo = db.first("SELECT unitcargo FROM stats_user_unitcargo WHERE user_id=",user.getId());
		UnitCargo owncargo = null;
		if( !userUnitCargo.isEmpty() ) {
			owncargo = new UnitCargo(userUnitCargo.getString("unitcargo"));
		}
		else {
			owncargo = new UnitCargo();
		}

		// Ausgabe des Tabellenkopfs
		echo.append("<table class=\"noBorderX\" cellspacing=\"1\" cellpadding=\"1\">\n");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"3\">Einheiten:</td></tr>\n");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\" width=\"200\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\">Alle</td>\n");
		echo.append("<td class=\"noBorderX\" width=\"15\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\">Eigene</td>\n");
		echo.append("</tr>\n");
		
		// Einheitenliste durchlaufen
		for( Entry<Integer,Long[]> unit : unitcargo.compare(owncargo).entrySet() ) {
			
			UnitType type = (UnitType)database.get(UnitType.class, unit.getKey());
			// Daten zur Einheit ausgeben
      		echo.append("<tr>\n");
      		echo.append("<td class=\"noBorderX\" style=\"white-space:nowrap\"><img style=\"vertical-align:middle\" src=\""+type.getPicture()+"\" alt=\"\"><a href=\""+Common.buildUrl("default", "module", "unitinfo", "unit", type.getId())+"\" >"+type.getName()+"</a></td>\n");
      		echo.append("<td class=\"noBorderX\">"+unit.getValue()[0]+"</td>\n");
      		echo.append("<td class=\"noBorderX\">&nbsp;</td>\n");
      		echo.append("<td class=\"noBorderX\">"+unit.getValue()[1]+"</td>\n");
      		echo.append("<td class=\"noBorderX\">&nbsp;</td>\n");
      		
		} // Ende: Resourcenliste
		echo.append("</table><br /><br />\n");
	}

    @Override
	public boolean generateAllyData() {
		return false;
	}
	
    @Override
	public int getRequiredData() {
		return 0;
	}
}
