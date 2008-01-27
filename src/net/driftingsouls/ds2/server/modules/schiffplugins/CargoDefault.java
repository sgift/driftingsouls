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
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;

/**
 * Schiffsmodul fuer die Anzeige des Schiffscargos
 * @author Christopher Jung
 *
 */
public class CargoDefault implements SchiffPlugin {

	public String action(Parameters caller) {
		SQLResultRow ship = caller.ship;
		SchiffController controller = caller.controller;
		
		Database db = controller.getDatabase();
		
		String output = "";

		controller.parameterString("max");
		controller.parameterString("act");
		controller.parameterNumber("unload");
		controller.parameterNumber("load");
		controller.parameterNumber("setautodeut");
		
		String act = controller.getString("act");
		String max = controller.getString("max");
		long load = controller.getInteger("load");
		long unload = controller.getInteger("unload");
		int setautodeut = controller.getInteger("setautodeut");
		
		if( act.equals("load") ) {
			if( !max.equals("") ) {
				load = 10000;
			}
			
			Cargo cargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );

			int e = ship.getInt("e");
			if( load > e ) {
				load = e;
			}
			if( load > cargo.getResourceCount( Resources.LBATTERIEN ) ) {
				load = cargo.getResourceCount( Resources.LBATTERIEN );
			}
			if( load < 0 ) {
				load = 0;
			}

			output += Common._plaintitle(ship.getString("name"))+" l&auml;dt "+load+" "+Cargo.getResourceName(Resources.BATTERIEN)+" auf<br /><br />\n";
			cargo.addResource( Resources.BATTERIEN, load );
			cargo.substractResource( Resources.LBATTERIEN, load );
			ship.put("e", ship.getInt("e") - load);
			ship.put("cargo", cargo.save());
			
			db.update("UPDATE ships SET e=",ship.getInt("e"),",cargo='",cargo.save(),"' WHERE id=",ship.getInt("id")," AND cargo='",cargo.save(true),"' AND e='",e,"'");
		}
		else if( act.equals("unload") ) {
			if( !max.equals("") ) {
				unload = 10000;
			}

			int maxeps = caller.shiptype.getInt("eps");
			
			Cargo cargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
			
			int e = ship.getInt("e");
			if( (unload + e) > maxeps ) {
				unload = maxeps - e;
			}
			if( unload > cargo.getResourceCount( Resources.BATTERIEN ) ) {
				unload = cargo.getResourceCount( Resources.BATTERIEN );
			}
			if( unload < 0 ) {
				unload = 0;
			}

			output += ship.getString("name")+" entl&auml;dt "+unload+" "+Cargo.getResourceName(Resources.BATTERIEN)+"<br /><br />\n";
			cargo.substractResource( Resources.BATTERIEN, unload );
			cargo.addResource( Resources.LBATTERIEN, unload );
			
			ship.put("e", ship.getInt("e") + unload);
			ship.put("cargo", cargo.save());

			db.update("UPDATE ships SET e=",ship.getInt("e"),",cargo='",cargo.save(),"' WHERE id=",ship.getInt("id")," AND cargo='",cargo.save(true),"' AND e='",e,"'");
		}
		else if( setautodeut != 0 ) {
			if( caller.shiptype.getInt("deutfactor") <= 0 ) {
				output += "<span style=\"color:red\">Nur Tanker k&ouml;nnen automatisch Deuterium sammeln</span><br />\n";
				return output;
			}
			controller.parameterNumber("autodeut");
			int autodeut = controller.getInteger("autodeut");
			
			db.update("UPDATE ships SET autodeut='",autodeut,"' WHERE id=",ship.getInt("id"));
			
			output += "Automatisches Deuteriumsammeln "+(autodeut != 0 ? "":"de")+"aktiviert<br />\n";
		}
		
		return output;
	}

	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		SQLResultRow ship = caller.ship;
		SQLResultRow shiptype = caller.shiptype;

		TemplateEngine t = caller.controller.getTemplateEngine();
		t.setFile("_PLUGIN_"+pluginid, "schiff.cargo.default.html");

		Cargo cargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
		cargo.setOption(Cargo.Option.LINKCLASS,"schiffwaren");

		t.setBlock("_CARGO","schiff.cargo.reslist.listitem","schiff.cargo.reslist.list");
		ResourceList reslist = cargo.getResourceList();
		Resources.echoResList( t, reslist, "schiff.cargo.reslist.list" );
		
		t.setVar(	"schiff.cargo.empty",			Common.ln(shiptype.getLong("cargo")-cargo.getMass()),
					"global.pluginid",				pluginid,
					"ship.id",						ship.getInt("id"),
					"schiff.cargo.batterien",		cargo.hasResource( Resources.BATTERIEN ),
					"schiff.cargo.lbatterien",		cargo.hasResource( Resources.LBATTERIEN ),
					"schiff.cargo.tanker",			shiptype.getInt("deutfactor"),
					"schiff.cargo.tanker.autodeut",	ship.getInt("autodeut"),
					"resource.RES_DEUTERIUM.image",	Cargo.getResourceImage(Resources.DEUTERIUM) );
		
		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
