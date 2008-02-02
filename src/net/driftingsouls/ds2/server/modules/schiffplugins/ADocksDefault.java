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
import java.util.List;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Schiffsmodul fuer die Anzeige von externen Docks
 * @author Christopher Jung
 *
 */
public class ADocksDefault implements SchiffPlugin {

	public String action(Parameters caller) {
		SQLResultRow ship = caller.ship;
		SQLResultRow shiptype = caller.shiptype;
		SchiffController controller = caller.controller;
		
		String output = "";
		
		Database db = ContextMap.getContext().getDatabase();
		
		controller.parameterString("act");
		String act = controller.getString("act");

		if( !act.equals("") ) {
			db.tBegin();
			
			output += "Entlade gedockte Schiffe<br />\n";
			Cargo cargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );

			long cargocount = cargo.getMass();

			SQLQuery dship = db.query("SELECT id,cargo FROM ships WHERE id>0 AND docked='",ship.getInt("id"),"' ORDER BY id");
			while( dship.next() ) {
				int did = dship.getInt("id");
				Cargo dcargo = new Cargo( Cargo.Type.STRING, dship.getString("cargo") );
				long dcargocount = dcargo.getMass();

				if( cargocount + dcargocount > shiptype.getInt("cargo") ) {
					output += "Kann einige Schiffe nicht entladen - nicht genug Frachtraum<br />\n";
					break;
				}
				
				cargo.addCargo( dcargo );

				cargocount += dcargocount;

				db.tUpdate(1,"UPDATE ships SET cargo='",new Cargo().save(),"' WHERE id>0 AND id=",did," AND cargo='",dcargo.save(),"'");
			}
			dship.free();

			db.tUpdate(1,"UPDATE ships SET cargo='",cargo.save(),"' WHERE id>0 AND id=",ship.getInt("id")," AND cargo='",cargo.save(true),"'");
			
			if( !db.tCommit() ) {
				output = "Entladen der gedockten Schiffe nicht erfolgreich - Bitte versuchen sie es sp&auml;ter nocheinmal<br />\n";
			}
			else {
				ship.put("cargo", cargo.save());
			}
		}
		
		return output;
	}

	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		SQLResultRow data = caller.ship;
		SQLResultRow datatype = caller.shiptype;
		SchiffController controller = caller.controller;

		Database db = controller.getDatabase();

		TemplateEngine t = controller.getTemplateEngine();
		t.setFile("_PLUGIN_"+pluginid, "schiff.adocks.default.html");

		List<Integer> dockedid = new ArrayList<Integer>();
		List<Integer> dockedtype = new ArrayList<Integer>();
		List<String> dockedname = new ArrayList<String>();
		List<String> dockedpicture = new ArrayList<String>();
		
		SQLQuery line = db.query("SELECT name,type,id,status FROM ships WHERE id>0 AND docked=",data.getInt("id")," ORDER BY id");
		while( line.next() ) {
			dockedid.add(line.getInt("id"));
			dockedtype.add(line.getInt("type"));
			dockedname.add(line.getString("name"));
			SQLResultRow dockedshiptype = ShipTypes.getShipType( line.getRow() );
			dockedpicture.add(dockedshiptype.getString("picture"));
		}
		line.free();

		String idlist = "";
		if( dockedid.size() > 0 ) {
			idlist = Common.implode("|",dockedid);
		}

		t.setVar(	"global.pluginid",		pluginid,
					"ship.id",				data.getInt("id"),
					"ship.docklist",		idlist,
					"ship.adocks",			datatype.getInt("adocks"),
					"docks.width",			100/(datatype.getInt("adocks")>4 ? 4 : datatype.getInt("adocks")) );

		t.setBlock("_PLUGIN_"+pluginid,"adocks.listitem","adocks.list");
		for( int j = 0; j < datatype.getInt("adocks"); j++ ) {
			t.start_record();
			if( (j > 0) && (j % 4 == 0) )
				t.setVar("docks.endrow",1);

			if( dockedid.size() > j ) {
				t.setVar(	"docks.entry.id",		dockedid.get(j),
							"docks.entry.name",		dockedname.get(j),
							"docks.entry.type",		dockedtype.get(j),
							"docks.entry.image",	dockedpicture.get(j) );
			}

			t.parse("adocks.list","adocks.listitem",true);
			t.stop_record();
			t.clear_record();
		}

		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
