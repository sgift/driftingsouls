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

import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.werften.ShipWerft;

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
		
		Database db = controller.getDatabase();

		SQLResultRow werftRow = db.first("SELECT * FROM werften WHERE shipid=",ship.getInt("id"));
		ShipWerft werft = new ShipWerft(werftRow,shiptype.getString("werft"),ship.getInt("system"),ship.getInt("owner"),ship.getInt("id"));
		werft.setOneWayFlag(shiptype.getInt("ow_werft"));

		TemplateEngine t = controller.getTemplateEngine();
		t.setFile("_PLUGIN_"+pluginid, "schiff.werft.default.html");

		if( werft.isBuilding() ) {
			SQLResultRow type = werft.getBuildShipType();
			
			t.setVar(	"global.pluginid",			pluginid,
						"ship.id",					ship.getInt("id"),
						"schiff.werft.prod.dauer",	werft.getRemainingTime(),
						"schiff.werft.prod.type",	type.getInt("id"),
						"schiff.werft.prod.name",	type.getString("nickname"),
						"schiff.werft.prod.picture",	type.getString("picture") );
			
			if( werft.getRequiredItem() != -1 ) {
				t.setVar(	"schiff.werft.prod.item",			werft.getRequiredItem(),
							"schiff.werft.prod.item.name",		Items.get().item(werft.getRequiredItem()).getName(),
							"schiff.werft.prod.item.picture",	Items.get().item(werft.getRequiredItem()).getPicture(),
							"schiff.werft.prod.item.available",	werft.isBuildContPossible() );
			}
		}
		
		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
