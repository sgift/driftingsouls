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

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.units.UnitCargo;
import org.springframework.stereotype.Component;

/**
 * Schiffsmodul fuer die Anzeige des Unitcargos.
 *
 */
@Component
public class UnitsDefault implements SchiffPlugin {
	@Action(ActionType.DEFAULT)
	public String action(Parameters caller) {
		return "";
	}

	@Action(ActionType.DEFAULT)
	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		Ship ship = caller.ship;
		ShipTypeData shiptype = caller.shiptype;

		TemplateEngine t = caller.t;
		t.setFile("_PLUGIN_"+pluginid, "schiff.unitcargo.default.html");

		UnitCargo unitcargo = ship.getUnits();
		
		t.setBlock("_UNITS","schiff.unitcargo.reslist.listitem","schiff.unitcargo.reslist.list");

		unitcargo.echoUnitList( t, "schiff.unitcargo.reslist.list",  "schiff.unitcargo.reslist.listitem");
		
		t.setVar(	"schiff.cargo.empty",					Common.ln(shiptype.getUnitSpace()-unitcargo.getMass()),
					"global.pluginid",						pluginid,
					"ship.id",								ship.getId() );
		
		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
