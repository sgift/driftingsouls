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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

/**
 * Schiffsmodul fuer die Anzeige der Jaegerdocks
 * @author Christopher Jung
 *
 */
public class JDocksDefault implements SchiffPlugin {

	public String action(Parameters caller) {
		Ship ship = caller.ship;
		ShipTypeData shiptype = caller.shiptype;
		SchiffController controller = caller.controller;
		
		String output = "";
		
		org.hibernate.Session db = controller.getDB();
		
		controller.parameterString("act");
		String act = controller.getString("act");

		if( !act.equals("") ) {
			output += "Entlade J&auml;ger<br />\n";
			Cargo cargo = ship.getCargo();

			long cargocount = cargo.getMass();

			List<?> lships = db.createQuery("from Ship where id>0 and docked=?")
				.setString(0, "l "+ship.getId())
				.list();
			for( Iterator<?> iter=lships.iterator(); iter.hasNext(); ) {
				Ship lship = (Ship)iter.next();
				
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

	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		Ship data = caller.ship;
		ShipTypeData datatype = caller.shiptype;
		SchiffController controller = caller.controller;

		org.hibernate.Session db = controller.getDB();

		TemplateEngine t = controller.getTemplateEngine();
		t.setFile("_PLUGIN_"+pluginid, "schiff.jdocks.default.html");

		boolean nofleet = true;
		List<Integer> jdockedid = new ArrayList<Integer>();
		List<Ship> jdockedShip = new ArrayList<Ship>();

		List<?> lships = db.createQuery("from Ship where id>0 and docked=? order by id")
			.setString(0, "l "+data.getId())
			.list();
		
		for( Iterator<?> iter=lships.iterator(); iter.hasNext(); ) {
			Ship aship = (Ship)iter.next();
			
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
					"docks.width",			100/(datatype.getJDocks()>4 ? 4 : datatype.getJDocks() ),
					"ship.docklist.nofleet",	nofleet );

		t.setBlock("_PLUGIN_"+pluginid,"jdocks.listitem","jdocks.list");
		for( int j = 0; j < datatype.getJDocks(); j++ ) {
			t.start_record();
			if( (j > 0) && (j % 4 == 0) )
				t.setVar("docks.endrow",1);

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
