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
package net.driftingsouls.ds2.server.modules.schiffplugins;

import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.werften.WerftObject;
import net.driftingsouls.ds2.server.werften.WerftQueueEntry;

/**
 * Schiffsmodul fuer die Anzeige des Werftstatus
 * @author Christopher Jung
 *
 */
public class WerftDefault implements SchiffPlugin {

	public String action(Parameters parameters) {
		return "";
	}

	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		SchiffController controller = caller.controller;
		SQLResultRow ship = caller.ship;
		SQLResultRow shiptype = caller.shiptype;
		
		Database database = controller.getDatabase();
		org.hibernate.Session db = controller.getDB();

		WerftObject werft = (WerftObject)db.createQuery("from ShipWerft where ship=?")
			.setEntity(0, Ships.getAsObject(ship))
			.uniqueResult();
				
		if( werft != null ) {
			TemplateEngine t = controller.getTemplateEngine();
			t.setFile("_PLUGIN_"+pluginid, "schiff.werft.default.html");
	
			if( werft.getKomplex() != null ) {
				werft = werft.getKomplex();
			}
			
			final WerftQueueEntry[] entries = werft.getBuildQueue();
			final int totalSlots = werft.getWerftSlots();
			int usedSlots = 0;
			int buildingCount = 0;
			for( int i=0; i < entries.length; i++ ) {
				if( entries[i].isScheduled() ) {
					usedSlots += entries[i].getSlots();
					buildingCount++;
				}
			}
			
			t.setVar(	"global.pluginid",			pluginid,
						"ship.id",					ship.getInt("id"),
						"schiff.werft.usedslots",	usedSlots,
						"schiff.werft.totalslots",	totalSlots,
						"schiff.werft.scheduled",	buildingCount,
						"schiff.werft.waiting",		entries.length-buildingCount
						);
			
			t.parse(caller.target,"_PLUGIN_"+pluginid);
		}
	}

}
