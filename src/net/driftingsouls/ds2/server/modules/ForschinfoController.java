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

import net.driftingsouls.ds2.server.Forschung;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.Weapon;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Zeigt Details zu einer Forschung an 
 * @author Christopher Jung
 *
 * @urlparam Integer res Die ID der anzuzeigenden Forschung
 */
public class ForschinfoController extends DSGenerator {
	private Forschung research = null;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public ForschinfoController(Context context) {
		super(context);
		
		setTemplate("forschinfo.html");
		
		parameterNumber("res");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = getUser();
		
		int researchid = getInteger("res");
		
		Forschung data = Forschung.getInstance(researchid);

		if( data == null ) {
			addError("&Uuml;ber diese Forschung liegen aktuell keine Informationen vor");
			
			return false;
		}
		
		if( !data.isVisibile() && 
			!((user.hasResearched(data.getRequiredResearch(1)) && user.hasResearched(data.getRequiredResearch(2)) && user.hasResearched(data.getRequiredResearch(3))) || 
			user.hasResearched(researchid) ) && (user.getAccessLevel() < 20) ) {
			addError("&Uuml;ber diese Forschung liegen aktuell keine Informationen vor");
			
			return false;
		}
		
		this.research = data;

		return true;
	}

	@Override
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		// Name und Bild
		t.setVar(	"tech.name",			Common._plaintitle(research.getName()),
					"tech.race.notall",		(research.getRace() != -1),
					"tech.id",				research.getID(),
					"tech.time",			research.getTime() );
					
		// Rasse 
		if( this.research.getRace() != -1 ) {
			String rasse = "???";
			if( Rassen.get().rasse(research.getRace()) != null ) {
				rasse = Rassen.get().rasse(research.getRace()).getName();
			}

			if( !Rassen.get().rasse(user.getRace()).isMemberIn( this.research.getRace() ) ) {
				rasse = "<span style=\"color:red\">"+rasse+"</span>";
			}
	
			t.setVar("tech.race.name",rasse);
		}

		// Voraussetzungen
		t.setBlock("_FORSCHINFO","tech.needs.listitem","tech.needs.list");
		for( int i = 1; i <= 3; i++ ) {
			t.start_record();
	
			if( (i > 1) && ((this.research.getRequiredResearch(i) > 0) || (this.research.getRequiredResearch(i) == -1)) ) {	
				t.setVar("tech.needs.item.break",true);
			}
	
			if( this.research.getRequiredResearch(i) > 0 ) {
				Forschung dat = Forschung.getInstance(this.research.getRequiredResearch(i));

				t.setVar(	"tech.needs.item.researchable",	true,
							"tech.needs.item.id",			this.research.getRequiredResearch(i),
							"tech.needs.item.name",			Common._plaintitle(dat.getName()) );
							
				t.parse("tech.needs.list","tech.needs.listitem",true);
			}
			else if( this.research.getRequiredResearch(i) == -1 ) {
				t.setVar("tech.needs.item.researchable",false);
				
				t.parse("tech.needs.list","tech.needs.listitem",true);
			}
	
			t.stop_record();
			t.clear_record();
		}
		
		// Kosten
		Cargo costs = new Cargo( Cargo.Type.STRING, this.research.getCosts() );
		costs.setOption( Cargo.Option.SHOWMASS, false );

		t.setBlock("_FORSCHINFO","tech.res.listitem","tech.res.list");
		
		ResourceList reslist = costs.getResourceList();
		for( ResourceEntry res : reslist ) {
			t.setVar(	"tech.res.item.image",	res.getImage(),
						"tech.res.item.cargo",	res.getCargo1() );
								
			t.parse("tech.res.list","tech.res.listitem",true);
		}

		// Ermoeglicht
		t.setBlock("_FORSCHINFO","tech.allows.listitem","tech.allows.list");

		boolean entry = false;
		SQLQuery result = db.query( "SELECT id FROM forschungen WHERE req1=",this.research.getID()," OR req2=",this.research.getID()," OR req3=",this.research.getID());
		while( result.next() ) {
			Forschung res = Forschung.getInstance(result.getInt("id"));
			
			if( res.isVisibile() || 
				(!res.isVisibile() && user.hasResearched(res.getRequiredResearch(1)) && user.hasResearched(res.getRequiredResearch(2)) && user.hasResearched(res.getRequiredResearch(3))) ) {
				t.setVar(	"tech.allows.item.break",	entry,
							"tech.allows.item.id",		res.getID(),
							"tech.allows.item.name",	Common._plaintitle(res.getName()),
							"tech.allows.item.hidden",	false );
				entry = true;
				
				t.parse("tech.allows.list","tech.allows.listitem",true);
			}	
			else if( (user.getAccessLevel() > 20) && !res.isVisibile() ) {
				t.setVar(	"tech.allows.item.break",	entry,
							"tech.allows.item.id",		res.getID(),
							"tech.allows.item.name",	Common._plaintitle(res.getName()),
							"tech.allows.item.hidden",	true );
				entry = true;
				
				t.parse("tech.allows.list","tech.allows.listitem",true);
			}	
		}
		result.free();

		// Beschreibung
		if( this.research.getDescription().length() > 0 ) {
			int colspan = 4;
			if( this.research.getRace() != -1 ) {
				colspan = 5;
			}
			t.setVar(	"tech.descrip",			Common._text(this.research.getDescription()),
						"tech.descrip.colspan",	colspan );
		}	
		
		//
		// Gebaeude
		//
		t.setBlock("_FORSCHINFO","tech.buildings.listitem","tech.buildings.list");
		t.setBlock("tech.buildings.listitem","tech.building.buildcosts.listitem","tech.building.buildcosts.list");
		t.setBlock("tech.buildings.listitem","tech.building.produces.listitem","tech.building.produces.list");
		t.setBlock("tech.buildings.listitem","tech.building.consumes.listitem","tech.building.consumes.list");
		t.setVar("tech.buildings.list","");

		boolean firstentry = true;

		SQLQuery buildingRow = db.query("SELECT id FROM buildings WHERE techreq=",this.research.getID());
		while( buildingRow.next() ) {
			Building building = Building.getBuilding(db, buildingRow.getInt("id"));
			
			t.start_record();
	
			t.setVar(	"tech.building.hr",			!firstentry,
						"tech.building.picture",	building.getPicture(),
						"tech.building.name",		Common._plaintitle(building.getName()),
						"tech.building.arbeiter",	building.getArbeiter(),
						"tech.building.bewohner",	building.getBewohner() );
	
			if( firstentry ) {
				firstentry = false;
			}
	
			reslist = building.getBuildCosts().getResourceList();
			Resources.echoResList( t, reslist, "tech.building.buildcosts.list" );
	
			reslist = building.getConsumes().getResourceList();
			Resources.echoResList( t, reslist, "tech.building.consumes.list" );
	
			if( building.getEVerbrauch() > 0 ) {
				t.setVar(	"res.image",	Configuration.getSetting("URL")+"data/interface/energie.gif",
							"res.cargo",	building.getEVerbrauch() );
							
				t.parse("tech.building.consumes.list","tech.building.consumes.listitem",true);
			}	

			reslist = building.getProduces().getResourceList();
			Resources.echoResList( t, reslist, "tech.building.produces.list" );

			if( building.getEProduktion() > 0 ) {
				t.setVar(	"res.image",	Configuration.getSetting("URL")+"data/interface/energie.gif",
							"res.cargo",	building.getEProduktion() );
									
				t.parse("tech.building.produces.list","tech.building.produces.listitem",true);
			}

			t.parse("tech.buildings.list","tech.buildings.listitem",true);

			t.stop_record();
			t.clear_record();
		}
		buildingRow.free();

		//
		// Cores
		//
		t.setBlock("_FORSCHINFO","tech.cores.listitem","tech.cores.list");
		t.setBlock("tech.cores.listitem","tech.core.buildcosts.listitem","tech.core.buildcosts.list");
		t.setBlock("tech.cores.listitem","tech.core.consumes.listitem","tech.core.consumes.list");
		t.setBlock("tech.cores.listitem","tech.core.produces.listitem","tech.core.produces.list");
		t.setVar("tech.cores.list","");

		firstentry = true;
		SQLQuery coreRow = db.query("SELECT id FROM cores WHERE techreq=",this.research.getID());
		while( coreRow.next() ) {
			Core core = Core.getCore(db, coreRow.getInt("id"));
			
			t.start_record();
		
			t.setVar(	"tech.core.astitype",	core.getAstiType(),
						"tech.core.name",		Common._plaintitle(core.getName()),
						"tech.core.hr",			!firstentry,
						"tech.core.arbeiter",	core.getArbeiter(),
						"tech.core.bewohner",	core.getBewohner() );
							
			if( firstentry ) {
				firstentry = false;
			}

			reslist = core.getBuildCosts().getResourceList();
			Resources.echoResList( t, reslist, "tech.core.buildcosts.list" );
	
			reslist = core.getConsumes().getResourceList();
			Resources.echoResList( t, reslist, "tech.core.consumes.list" );

			if( core.getEVerbrauch() > 0 ) {
				t.setVar(	"res.image",	Configuration.getSetting("URL")+"data/interface/energie.gif",
							"res.cargo",	core.getEVerbrauch() );
							
				t.parse("tech.core.consumes.list","tech.core.consumes.listitem",true);
			}

			reslist = core.getProduces().getResourceList();
			Resources.echoResList( t, reslist, "tech.core.produces.list" );

			if( core.getEProduktion() > 0 ) {
				t.setVar(	"res.image",	Configuration.getSetting("URL")+"data/interface/energie.gif",
							"res.cargo",	core.getEProduktion() );
							
				t.parse("tech.core.produces.list","tech.core.produces.listitem",true);
			}
	
			t.parse("tech.cores.list","tech.cores.listitem",true);
	
			t.stop_record();
			t.clear_record();
		}
		coreRow.free();
				
				
		//
		// Schiffe
		//
		t.setBlock("_FORSCHINFO","tech.ships.listitem","tech.ships.list");
		t.setBlock("tech.ships.listitem","tech.ship.costs.listitem","tech.ship.costs.list");
		t.setBlock("tech.ships.listitem","tech.ship.techs.listitem","tech.ship.techs.list");
		t.setVar("tech.ships.list","");

		firstentry = true;
		SQLQuery ship = db.query("SELECT t1.*,t2.nickname,t2.picture " +
				"FROM ships_baubar t1 JOIN ship_types t2 ON t1.type=t2.id " +
				"WHERE t1.type=t2.id AND (t1.tr1=",this.research.getID()," OR t1.tr2=",this.research.getID()," OR t1.tr3=",this.research.getID(),")");
		while( ship.next() ) {
			boolean show = true;

			//Schiff sichtbar???
			for( int i=0; i < 3; i++ ) {
				if( ship.getInt("tr"+(i+1)) > 0 ) {
					Forschung tmpres = Forschung.getInstance(ship.getInt("tr"+(i+1)));
					if( !tmpres.isVisibile() && 
						(!user.hasResearched(ship.getInt("tr"+(i+1))) || !user.hasResearched(tmpres.getRequiredResearch(1)) || 
								!user.hasResearched(tmpres.getRequiredResearch(2)) || !user.hasResearched(tmpres.getRequiredResearch(3)) 
						) ) {
						show = false;
				 		break;
					}
				}
			}
			
			if( !show ) {
				continue;
			}
	
			t.start_record();

			SQLResultRow shiptype = ShipTypes.getShipType(ship.getInt("type"), false);
			
			t.setVar(	"tech.ship.id",			ship.getInt("type"),
						"tech.ship.name",		Common._plaintitle(ship.getString("nickname")),
						"tech.ship.picture",	shiptype.getString("picture"),
						"tech.ship.hr",			!firstentry,
						"tech.ship.dauer",		ship.getInt("dauer"),
						"tech.ship.ekosten",	ship.get("ekosten"),
						"tech.ship.crew",		ship.get("crew") );
		
			if( firstentry ) {
				firstentry = false;
			}
		
			costs = new Cargo(Cargo.Type.STRING, ship.getString("costs"));
		
			reslist = costs.getResourceList();
			Resources.echoResList( t, reslist, "tech.ship.costs.list" );
		
			//Benoetigt dieses Schiff noch weitere Forschungen???
			if( ((ship.getInt("tr1") != 0) && (ship.getInt("tr1") != this.research.getID())) || 
				((ship.getInt("tr2") != 0) && (ship.getInt("tr2") != this.research.getID())) || 
				((ship.getInt("tr3") != 0) && (ship.getInt("tr3") != this.research.getID())) ) {
				firstentry = true;
				
				//Es benoetigt weitere!
				t.setVar("tech.ship.techs.list","");
		      	for( int b = 1; b <= 3; b++ ) {
         			if( (ship.getInt("tr"+b) != 0) && (ship.getInt("tr"+b) != this.research.getID()) ) {
         				t.setVar(	"tech.ship.tech.break",	!firstentry,
         							"tech.ship.tech.id",	ship.getInt("tr"+b),
         							"tech.ship.tech.name",	Forschung.getInstance(ship.getInt("tr"+b)).getName() );
         							
						if( firstentry ) {
							firstentry = false;
						}
					
						t.parse( "tech.ship.techs.list", "tech.ship.techs.listitem", true );
	         		}
    	  		}
			} 	
		
			t.parse( "tech.ships.list", "tech.ships.listitem", true );
		
			t.stop_record();
			t.clear_record();
		}
		ship.free();


		//
		// Munition
		//
		t.setBlock("_FORSCHINFO","tech.ammo.listitem","tech.ammo.list");
		t.setBlock("tech.ammo.listitem","tech.ammo.buildcosts.listitem","tech.ammo.buildcosts.list");
		t.setVar("tech.ammo.list","");

		firstentry = true;

		SQLQuery ammo = db.query("SELECT * FROM ammo WHERE res1=",this.research.getID()," OR res2=",this.research.getID()," OR res3=",this.research.getID());
		while( ammo.next() ) {
			t.start_record();
	
			t.setVar(	"tech.ammo.hr",			!firstentry,
						"tech.ammo.name",		Common._plaintitle(ammo.getString("name")),
						"tech.ammo.picture",	ammo.getString("picture"),
						"tech.ammo.description",	Common._text(ammo.getString("description")),
						"tech.ammo.itemid",		ammo.getInt("itemid"),
						"tech.ammo.dauer",		ammo.getString("dauer") );
	
			if( firstentry ) {
				firstentry = false;
			}
			
			Cargo buildcosts = new Cargo( Cargo.Type.STRING, ammo.getString("buildcosts") );

			// Produktionskosten	
			reslist = buildcosts.getResourceList();
			Resources.echoResList( t, reslist, "tech.ammo.buildcosts.list" );

			// Diverse Daten
			StringBuilder data = new StringBuilder(50);
	
			if( ammo.getInt("shotspershot") > 1 ) {
				data.append(ammo.getInt("shotspershot")+" Salven<br />\n");
			}
			if( ammo.getInt("damage") != 0 ) {
				data.append(ammo.getInt("damage")+" Schaden<br />\n");
			}
			if( ammo.getInt("damage") != ammo.getInt("shielddamage") ) {
				data.append(ammo.getInt("shielddamage")+" Schildschaden<br />\n");
			}
			if( ammo.getInt("subdamage") != 0 ) {
				data.append(ammo.getInt("subdamage")+" Subsystemschaden<br />\n");
				data.append(ammo.getInt("subws")+"% Subsystem-Trefferws<br />\n");
			}
			data.append(ammo.getInt("smalltrefferws")+"% Trefferws (J&auml;ger)<br />\n");
			data.append(ammo.getInt("trefferws")+"% Trefferws (Capitals)\n");
			if( ammo.getInt("torptrefferws") != 0 ) {
				data.append("<br />"+ammo.getInt("torptrefferws")+"% Trefferws (Torpedos)\n");
			}
			if( ammo.getInt("areadamage") != 0 ) {
				data.append("<br />Umgebungsschaden ("+ammo.getInt("areadamage")+")\n");
			}
			if( ammo.getInt("destroyable") > 0 ) {
				data.append("<br />Durch Abwehrfeuer zerst&ouml;rbar\n");
			}
			if( ammo.getInt("replaces") != 0 ) {
				SQLResultRow replammo = db.first("SELECT itemid,name FROM ammo WHERE id=",ammo.getInt("replaces"));
				data.append("<br />Ersetzt <a style=\"font-size:14px\" class=\"forschinfo\" href=\""+Common.buildUrl(getContext(), "details", "module", "iteminfo", "item", replammo.getInt("itemid"))+"\">"+replammo.getString("name")+"</a>\n");
			}
		
			/*
				Mit welchen Waffen ist das ganze abfeuerbar?
			*/
			StringBuilder weapons = new StringBuilder(50);
			for( Weapon weapon : Weapons.get() ) {
				if( !weapon.getAmmoType().equals(ammo.get("type")) ) {
					continue;
				}
		
				if( weapons.length() == 0 ) {
					weapons.append(weapon.getName());
				}
				else {
					weapons.append(",<br />\n"+weapon.getName());
				}
			}

			t.setVar(	"tech.ammo.data",		data,
						"tech.ammo.weapons",	weapons );
								
			t.parse( "tech.ammo.list", "tech.ammo.listitem", true );
		
			t.stop_record();
			t.clear_record();
		}
		ammo.free();
	}
}
