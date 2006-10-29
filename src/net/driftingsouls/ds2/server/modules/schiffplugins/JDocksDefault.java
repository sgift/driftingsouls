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
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Schiffsmodul fuer die Anzeige der Jaegerdocks
 * @author Christopher Jung
 *
 */
public class JDocksDefault implements SchiffPlugin {

	public String action(Parameters caller) {
		SQLResultRow ship = caller.ship;
		SQLResultRow shiptype = caller.shiptype;
		SchiffController controller = caller.controller;
		
		String output = "";
		
		Database db = controller.getDatabase();
		
		controller.parameterString("act");
		String act = controller.getString("act");

		if( !act.equals("") ) {
			db.tBegin();
			
			output += "Entlade J&auml;ger<br />\n";
			Cargo cargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );

			long cargocount = cargo.getMass();

			SQLQuery lship = db.query("SELECT id,cargo FROM ships WHERE id>0 AND docked='l ",ship.getInt("id"),"' ORDER BY id");
			while( lship.next() ) {
				int did = lship.getInt("id");
				Cargo dcargo = new Cargo( Cargo.Type.STRING, lship.getString("cargo") );
				long dcargocount = dcargo.getMass();

				if( cargocount + dcargocount > shiptype.getInt("cargo") ) {
					output += "Kann einige Schiffe nicht entladen - nicht genug Frachtraum<br />\n";
					break;
				}
				
				cargo.addCargo( dcargo );

				cargocount += dcargocount;

				db.tUpdate(1,"UPDATE ships SET cargo='",new Cargo().save(),"' WHERE id>0 AND id=",did," AND cargo='",dcargo.save(),"'");
			}
			lship.free();

			db.tUpdate(1,"UPDATE ships SET cargo='",cargo.save(),"' WHERE id>0 AND id=",ship.getInt("id")," AND cargo='",cargo.save(true),"'");
			
			if( !db.tCommit() ) {
				output = "Entladen der J&auml;ger nicht erfolgreich - Bitte versuchen sie es sp&auml;ter nocheinmal<br />\n";
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
		t.set_file("_PLUGIN_"+pluginid, "schiff.jdocks.default.html");

		boolean nofleet = true;
		List<Integer> jdockedid = new ArrayList<Integer>();
		List<Integer> jdockedtype = new ArrayList<Integer>();
		List<String> jdockedname = new ArrayList<String>();
		List<String> jdockedpicture = new ArrayList<String>();
			
		SQLQuery line = db.query("SELECT name,type,id,fleet,status FROM ships WHERE id>0 AND docked='l ",data.getInt("id"),"' ORDER BY id");
		while( line.next() ) {
			jdockedid.add(line.getInt("id"));
			jdockedtype.add(line.getInt("type"));
			jdockedname.add(line.getString("name"));
			
			SQLResultRow jdockedstype = Ships.getShipType( line.getRow() );
			jdockedpicture.add(jdockedstype.getString("picture"));
			if( line.getInt("fleet") != 0 ) {
				nofleet = false;
			}
		}
		line.free();
		String idlist = "";
		if( jdockedid.size() > 0 ) {
			idlist = Common.implode("|",jdockedid);
		}

		t.set_var(	"global.pluginid",		pluginid,
					"ship.id",				data.getInt("id"),
					"ship.docklist",		idlist,
					"ship.jdocks",			datatype.getInt("jdocks"),
					"docks.width",			100/(datatype.getInt("jdocks")>4 ? 4 : datatype.getInt("jdocks") ),
					"ship.docklist.nofleet",	nofleet );

		t.set_block("_PLUGIN_"+pluginid,"jdocks.listitem","jdocks.list");
		for( int j = 0; j < datatype.getInt("jdocks"); j++ ) {
			t.start_record();
			if( (j > 0) && (j % 4 == 0) )
				t.set_var("docks.endrow",1);

			if( jdockedid.size() > j ) {
				t.set_var(	"docks.entry.id",		jdockedid.get(j),
							"docks.entry.name",		jdockedname.get(j),
							"docks.entry.type",		jdockedtype.get(j),
							"docks.entry.image",	jdockedpicture.get(j) );
			}
			t.parse("jdocks.list","jdocks.listitem",true);
			t.stop_record();
			t.clear_record();
		}

		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
