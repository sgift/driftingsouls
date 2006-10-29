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
package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Die Liste aller baubaren Gebaeude und Cores
 * @author Christopher Jung
 *
 */
public class BuildingsController extends DSGenerator {
	public BuildingsController(Context context) {
		super(context);
		
		setTemplate("buildings.html");
		
		parameterNumber("col");	
		parameterNumber("field");	
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		Database db = getDatabase();
		User user = getUser();
		
		int col = getInteger("col");
		
		if( col != 0 ) {
			SQLResultRow chk = db.first("SELECT id FROM bases WHERE owner="+user.getID()+" AND id="+col);

			if( chk.isEmpty() ) {
				addError("Die angegebene Kolonie existiert nicht");
				
				return false;
			}
		}
		
		return true;
	}

	@Override
	public void defaultAction() {
		Database db = getDatabase();
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		t.set_block("_BUILDINGS", "buildings.listitem", "buildings.list");
		t.set_block("buildings.listitem", "building.buildcosts.listitem", "building.buildcosts.list");
		t.set_block("buildings.listitem", "building.produces.listitem", "building.produces.list");
		t.set_block("buildings.listitem", "building.consumes.listitem", "building.consumes.list");
		
		SQLQuery buildingID = db.query("SELECT id FROM buildings ORDER BY name" );
		while( buildingID.next() ) {
			Building building = Building.getBuilding(db, buildingID.getInt("id"));
			if( !user.hasResearched(building.getTechRequired()) ) {
				continue;
			}
			
			t.set_var(	"building.name",	Common._plaintitle(building.getName()),
						"building.picture",	building.getPicture(),
						"building.arbeiter",	building.getArbeiter(),
						"building.bewohner",	building.getBewohner() );
				
			ResourceList reslist = building.getBuildCosts().getResourceList();
			Resources.echoResList( t, reslist, "building.buildcosts.list" );
		
			reslist = building.getConsumes().getResourceList();
			Resources.echoResList( t, reslist, "building.consumes.list" );
	
			if( building.getEVerbrauch() > 0 ) {
				t.set_var(	"res.image",	Configuration.getSetting("URL")+"data/interface/energie.gif",
							"res.cargo",	building.getEVerbrauch(),
							"res.plainname",	"Energie" );
							
				t.parse("building.consumes.list","building.consumes.listitem",true);
			}

			reslist = building.getProduces().getResourceList();
			Resources.echoResList( t, reslist, "building.produces.list" );

			if( building.getEProduktion() > 0 ) {
				t.set_var(	"res.image",		Configuration.getSetting("URL")+"data/interface/energie.gif",
							"res.cargo",		building.getEProduktion(),
							"res.plainname",	"Energie" );
									
				t.parse("building.produces.list","building.produces.listitem",true);
			}
			
			// Weitere Infos generieren (max per Basis/Acc & UComplex)
			StringBuilder addinfo = new StringBuilder(20);
			if( building.isUComplex() ) {
				addinfo.append("Unterirdischer Komplex");	
			}
			
			if( building.getPerPlanetCount() != 0 ) {
				if( addinfo.length() != 0 ) {
					addinfo.append(", ");	
				}
				addinfo.append("Max. "+building.getPerPlanetCount()+"x pro Basis");
			}
			
			if( building.getPerUserCount() != 0 ) {
				if( addinfo.length() != 0 ) {
					addinfo.append(", ");	
				}
				addinfo.append("Max. "+building.getPerUserCount()+"x pro Account");
			}
			
			t.set_var("building.addinfo", addinfo);
			
			t.parse("buildings.list", "buildings.listitem", true);
		}
		buildingID.free();
		
		//
		// Cores
		//
		
		t.set_block("_BUILDINGS", "cores.listitem", "cores.list");
		t.set_block("cores.listitem", "core.buildcosts.listitem", "core.buildcosts.list");
		t.set_block("cores.listitem", "core.produces.listitem", "core.produces.list");
		t.set_block("cores.listitem", "core.consumes.listitem", "core.consumes.list");
		

		SQLQuery coreID = db.query("SELECT id FROM cores ORDER BY name,astitype");
		while( coreID.next() ) {
			Core core = Core.getCore(db, coreID.getInt("id"));
			if( !user.hasResearched(core.getTechRequired()) ) {
				continue;
			}
			
			t.set_var(	"core.astitype",	core.getAstiType(),
						"core.name",		Common._plaintitle(core.getName()),
						"core.arbeiter",	core.getArbeiter(),
						"core.bewohner",	core.getBewohner() );
			
			ResourceList reslist = core.getBuildCosts().getResourceList();
			Resources.echoResList( t, reslist, "core.buildcosts.list" );
		
			reslist = core.getConsumes().getResourceList();
			Resources.echoResList( t, reslist, "core.consumes.list" );
	
			if( core.getEVerbrauch() > 0 ) {
				t.set_var(	"res.image",	Configuration.getSetting("URL")+"data/interface/energie.gif",
							"res.cargo",	core.getEVerbrauch(),
							"res.plainname",	"Energie" );
							
				t.parse("core.consumes.list","core.consumes.listitem",true);
			}

			reslist = core.getProduces().getResourceList();
			Resources.echoResList( t, reslist, "core.produces.list" );

			if( core.getEProduktion() > 0 ) {
				t.set_var(	"res.image",		Configuration.getSetting("URL")+"data/interface/energie.gif",
							"res.cargo",		core.getEProduktion(),
							"res.plainname",	"Energie" );
									
				t.parse("core.produces.list","core.produces.listitem",true);
			}
	
			t.parse("cores.list","cores.listitem",true);
		}
		coreID.free();
		
		t.set_var(	"base.id",		getInteger("col"),
					"base.field",	getInteger("field") );
	}
}
