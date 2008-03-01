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
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

/**
 * Schiffsmodul fuer die Anzeige des Schiffscargos
 * @author Christopher Jung
 *
 */
public class CargoDefault implements SchiffPlugin {

	public String action(Parameters caller) {
		Ship ship = caller.ship;
		SchiffController controller = caller.controller;
		
		String output = "";

		controller.parameterString("max");
		controller.parameterString("act");
		controller.parameterNumber("unload");
		controller.parameterNumber("load");
		controller.parameterNumber("setautodeut");
		controller.parameterNumber("setstartfighter");
		
		String act = controller.getString("act");
		String max = controller.getString("max");
		long load = controller.getInteger("load");
		long unload = controller.getInteger("unload");
		int setautodeut = controller.getInteger("setautodeut");
		int setstartfighter = controller.getInteger("setstartfighter");
		
		if( act.equals("load") ) {
			if( !max.equals("") ) {
				load = 10000;
			}
			
			Cargo cargo = ship.getCargo();

			int e = ship.getEnergy();
			if( load > e ) {
				load = e;
			}
			if( load > cargo.getResourceCount( Resources.LBATTERIEN ) ) {
				load = cargo.getResourceCount( Resources.LBATTERIEN );
			}
			if( load < 0 ) {
				load = 0;
			}

			output += Common._plaintitle(ship.getName())+" l&auml;dt "+load+" "+Cargo.getResourceName(Resources.BATTERIEN)+" auf<br /><br />\n";
			cargo.addResource( Resources.BATTERIEN, load );
			cargo.substractResource( Resources.LBATTERIEN, load );
			
			ship.setEnergy((int)(ship.getEnergy() - load));
			ship.setCargo(cargo);
		}
		else if( act.equals("unload") ) {
			if( !max.equals("") ) {
				unload = 10000;
			}

			int maxeps = caller.shiptype.getEps();
			
			Cargo cargo = ship.getCargo();
			
			int e = ship.getEnergy();
			if( (unload + e) > maxeps ) {
				unload = maxeps - e;
			}
			if( unload > cargo.getResourceCount( Resources.BATTERIEN ) ) {
				unload = cargo.getResourceCount( Resources.BATTERIEN );
			}
			if( unload < 0 ) {
				unload = 0;
			}

			output += ship.getName()+" entl&auml;dt "+unload+" "+Cargo.getResourceName(Resources.BATTERIEN)+"<br /><br />\n";
			cargo.substractResource( Resources.BATTERIEN, unload );
			cargo.addResource( Resources.LBATTERIEN, unload );
			
			ship.setEnergy((int)(ship.getEnergy() + unload));
			ship.setCargo(cargo);
		}
		else if( setautodeut != 0 ) {
			if( caller.shiptype.getDeutFactor() <= 0 ) {
				output += "<span style=\"color:red\">Nur Tanker k&ouml;nnen automatisch Deuterium sammeln</span><br />\n";
				return output;
			}
			controller.parameterNumber("autodeut");
			int autodeut = controller.getInteger("autodeut");
			
			ship.setAutoDeut(autodeut != 0 ? true : false);
			
			output += "Automatisches Deuteriumsammeln "+(autodeut != 0 ? "":"de")+"aktiviert<br />\n";
		}
		else if(setstartfighter != 0)
		{
			controller.parameterNumber("startfighter");
			int startfighter = controller.getInteger("startfighter");
			
			ship.setStartFighters(startfighter != 0 ? true : false);
			
			output += "Automatisches Starten von Jägern "+(startfighter != 0 ? "":"de")+"aktiviert<br />\n";
		}
		
		return output;
	}

	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		Ship ship = caller.ship;
		ShipTypeData shiptype = caller.shiptype;

		TemplateEngine t = caller.controller.getTemplateEngine();
		t.setFile("_PLUGIN_"+pluginid, "schiff.cargo.default.html");

		Cargo cargo = ship.getCargo();
		cargo.setOption(Cargo.Option.LINKCLASS,"schiffwaren");

		t.setBlock("_CARGO","schiff.cargo.reslist.listitem","schiff.cargo.reslist.list");
		ResourceList reslist = cargo.getResourceList();
		Resources.echoResList( t, reslist, "schiff.cargo.reslist.list" );
		
		t.setVar(	"schiff.cargo.empty",					Common.ln(shiptype.getCargo()-cargo.getMass()),
					"global.pluginid",						pluginid,
					"ship.id",								ship.getId(),
					"schiff.cargo.batterien",				cargo.hasResource( Resources.BATTERIEN ),
					"schiff.cargo.lbatterien",				cargo.hasResource( Resources.LBATTERIEN ),
					"schiff.cargo.tanker",					shiptype.getDeutFactor(),
					"schiff.cargo.tanker.autodeut",			ship.getAutoDeut(),
					"schiff.cargo.traeger",					shiptype.getJDocks() > 0 ? 1 : 0,
					"schiff.cargo.traeger.startfighter",	ship.startFighters(),
					"resource.RES_DEUTERIUM.image",			Cargo.getResourceImage(Resources.DEUTERIUM) );
		
		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
