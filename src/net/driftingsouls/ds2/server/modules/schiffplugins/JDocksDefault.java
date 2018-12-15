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
 * Schiffsmodul fuer die Anzeige der Jaegerdocks.
 * @author Christopher Jung
 *
 */
@Component
public class JDocksDefault implements SchiffPlugin {
	@Action(ActionType.DEFAULT)
	public String action(Parameters caller, String act) {
		Ship ship = caller.ship;
		ShipTypeData shiptype = caller.shiptype;

		String output = "";

		if( !act.equals("") ) {
			output += "Entlade J&auml;ger<br />\n";
			Cargo cargo = ship.getCargo();

			long cargocount = cargo.getMass();

			for( Ship lship : ship.getLandedShips() ) {
				Cargo dcargo = lship.getCargo();
				long dcargocount = dcargo.getMass();

				if( cargocount + dcargocount > shiptype.getCargo() ) {
					output += "Kann einige Schiffe nicht entladen - nicht genug Frachtraum<br />\n";
					break;
				}

				cargo.addCargo( dcargo );

				cargocount += dcargocount;

				lship.setCargo(new Cargo());
			}

			ship.setCargo(cargo);
		}

		return output;
	}

	@Action(ActionType.DEFAULT)
	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		Ship data = caller.ship;
		ShipTypeData datatype = caller.shiptype;

		TemplateEngine t = caller.t;
		t.setFile("_PLUGIN_"+pluginid, "schiff.jdocks.default.html");

		boolean nofleet = true;
		List<Integer> jdockedid = new ArrayList<>();
		List<Ship> jdockedShip = new ArrayList<>();

		for( Ship aship : data.getLandedShips() ) {
			jdockedid.add(aship.getId());
			jdockedShip.add(aship);
			if( aship.getFleet() != null ) {
				nofleet = false;
			}
		}

		String idlist = "";
		if( jdockedid.size() > 0 ) {
			idlist = Common.implode("|",jdockedid);
		}

		t.setVar(	"global.pluginid",		pluginid,
					"ship.id",				data.getId(),
					"ship.docklist",		idlist,
					"ship.jdocks",			datatype.getJDocks(),
					"docks.width",			100/(datatype.getJDocks()>5 ? 5 : datatype.getJDocks() ),
					"ship.docklist.nofleet",	nofleet );

		t.setBlock("_PLUGIN_"+pluginid,"jdocks.listitem","jdocks.list");
		for( int j = 0; j < datatype.getJDocks(); j++ ) {
			t.start_record();
			if( (j > 0) && (j % 5 == 0) )
			{
				t.setVar("docks.endrow",1);
			}

			if( jdockedShip.size() > j ) {
				Ship aship = jdockedShip.get(j);

				t.setVar(	"docks.entry.id",		aship.getId(),
							"docks.entry.name",		aship.getName(),
							"docks.entry.type",		aship.getType(),
							"docks.entry.image",	aship.getTypeData().getPicture() );
			}
			t.parse("jdocks.list","jdocks.listitem",true);
			t.stop_record();
			t.clear_record();
		}

		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
