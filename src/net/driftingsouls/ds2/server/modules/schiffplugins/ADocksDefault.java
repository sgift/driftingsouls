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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Schiffsmodul fuer die Anzeige von externen Docks.
 * @author Christopher Jung
 *
 */
@Component
public class ADocksDefault implements SchiffPlugin {
	@Action(ActionType.DEFAULT)
	public String action(Parameters caller, String act) {
		Ship ship = caller.ship;
		ShipTypeData shiptype = caller.shiptype;

		String output = "";

		if( !act.equals("") ) {
			output += "Entlade gedockte Schiffe<br />\n";
			Cargo cargo = ship.getCargo();

			long cargocount = cargo.getMass();

			for( Ship dship : ship.getDockedShips() ) {
				Cargo dcargo = dship.getCargo();
				long dcargocount = dcargo.getMass();

				if( cargocount + dcargocount > shiptype.getCargo() ) {
					output += "Kann einige Schiffe nicht entladen - nicht genug Frachtraum<br />\n";
					break;
				}

				cargo.addCargo( dcargo );

				cargocount += dcargocount;

				dship.setCargo(new Cargo());
			}

			ship.setCargo(cargo);
		}

		return output;
	}

	@Action(ActionType.DEFAULT)
	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		Ship ship = caller.ship;
		ShipTypeData shiptype = caller.shiptype;

		TemplateEngine t = caller.t;
		t.setFile("_PLUGIN_"+pluginid, "schiff.adocks.default.html");

		List<Ship> dockedShips = new ArrayList<>();
		List<Integer> dockedids = new ArrayList<>();

		for( Ship aship : ship.getDockedShips() ) {
			dockedShips.add(aship);
			dockedids.add(aship.getId());
		}

		String idlist = "";
		if( dockedShips.size() > 0 ) {
			idlist = Common.implode("|",dockedids);
		}

		t.setVar(	"global.pluginid",		pluginid,
					"ship.id",				ship.getId(),
					"ship.docklist",		idlist,
					"ship.adocks",			shiptype.getADocks(),
					"docks.width",			100/(shiptype.getADocks()>4 ? 4 : shiptype.getADocks()) );

		t.setBlock("_PLUGIN_"+pluginid,"adocks.listitem","adocks.list");
		for( int j = 0; j < shiptype.getADocks(); j++ ) {
			t.start_record();
			if( (j > 0) && (j % 4 == 0) )
			{
				t.setVar("docks.endrow",1);
			}

			if( dockedShips.size() > j ) {
				Ship aship = dockedShips.get(j);
				t.setVar(	"docks.entry.id",		aship.getId(),
							"docks.entry.name",		aship.getName(),
							"docks.entry.type",		aship.getType(),
							"docks.entry.image",	aship.getTypeData().getPicture() );
			}

			t.parse("adocks.list","adocks.listitem",true);
			t.stop_record();
			t.clear_record();
		}

		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
