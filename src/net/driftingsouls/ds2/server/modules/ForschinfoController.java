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

import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.entities.FactoryEntry;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Zeigt Details zu einer Forschung an .
 * @author Christopher Jung
 *
 * @urlparam Integer res Die ID der anzuzeigenden Forschung
 */
@Configurable
public class ForschinfoController extends TemplateGenerator {
	private Forschung research = null;
	
	private Configuration config;
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public ForschinfoController(Context context) {
		super(context);
		
		setTemplate("forschinfo.html");
		
		parameterNumber("res");
		
		setPageTitle("Forschung");
	}
	
    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
    }
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
		
		int researchid = getInteger("res");
		
		Forschung data = Forschung.getInstance(researchid);

		if( data == null ) {
			addError("&Uuml;ber diese Forschung liegen aktuell keine Informationen vor");
			
			return false;
		}
		
		if( !data.isVisibile(user) && (user.getAccessLevel() < 20) ) {
			addError("&Uuml;ber diese Forschung liegen aktuell keine Informationen vor");
			
			return false;
		}
		
		this.research = data;

		return true;
	}
	
	/**
	 * Wirft eine Forschung - mit allen davon abhaengigen Forschungen - weg.
	 */
	@Action(ActionType.DEFAULT)
	public void dropAction()
	{
		User user = (User)getUser();
		if(user.getUserResearch(this.research) == null)
		{
			return;
		}
		
		user.dropResearch(this.research);
		redirect();
	}

	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		
		// Name und Bild
		t.setVar(	"tech.name",			Common._plaintitle(research.getName()),
					"tech.race.notall",		(research.getRace() != -1),
					"tech.id",				research.getID(),
					"tech.time",			research.getTime(),
					"tech.speccosts",		research.getSpecializationCosts());
					
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
		Cargo costs = this.research.getCosts();
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
		List<?> results = db.createQuery("from Forschung where req1= :fid or req2= :fid or req3= :fid")
			.setInteger("fid", this.research.getID())
			.list();
		for( Iterator<?> iter=results.iterator(); iter.hasNext(); ) {
			Forschung res = (Forschung)iter.next();
			
			if( res.isVisibile(user) || 
				(!res.isVisibile(user) && user.hasResearched(res.getRequiredResearch(1)) && user.hasResearched(res.getRequiredResearch(2)) && user.hasResearched(res.getRequiredResearch(3))) ) {
				t.setVar(	"tech.allows.item.break",	entry,
							"tech.allows.item.id",		res.getID(),
							"tech.allows.item.name",	Common._plaintitle(res.getName()),
							"tech.allows.item.hidden",	false );
				entry = true;
				
				t.parse("tech.allows.list","tech.allows.listitem",true);
			}	
			else if( (user.getAccessLevel() > 20) && !res.isVisibile(user) ) {
				t.setVar(	"tech.allows.item.break",	entry,
							"tech.allows.item.id",		res.getID(),
							"tech.allows.item.name",	Common._plaintitle(res.getName()),
							"tech.allows.item.hidden",	true );
				entry = true;
				
				t.parse("tech.allows.list","tech.allows.listitem",true);
			}	
		}

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

		Iterator<?> buildingIter = db.createQuery("from Building where techReq=?")
			.setInteger(0, this.research.getID())
			.iterate();
		for( ; buildingIter.hasNext(); ) {
			Building building = (Building)buildingIter.next();
			
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
				t.setVar(	"res.image",	config.get("URL")+"data/interface/energie.gif",
							"res.cargo",	building.getEVerbrauch() );
							
				t.parse("tech.building.consumes.list","tech.building.consumes.listitem",true);
			}	

			reslist = building.getAllProduces().getResourceList();
			Resources.echoResList( t, reslist, "tech.building.produces.list" );

			if( building.getEProduktion() > 0 ) {
				t.setVar(	"res.image",	config.get("URL")+"data/interface/energie.gif",
							"res.cargo",	building.getEProduktion() );
									
				t.parse("tech.building.produces.list","tech.building.produces.listitem",true);
			}

			t.parse("tech.buildings.list","tech.buildings.listitem",true);

			t.stop_record();
			t.clear_record();
		}

		//
		// Cores
		//
		t.setBlock("_FORSCHINFO","tech.cores.listitem","tech.cores.list");
		t.setBlock("tech.cores.listitem","tech.core.buildcosts.listitem","tech.core.buildcosts.list");
		t.setBlock("tech.cores.listitem","tech.core.consumes.listitem","tech.core.consumes.list");
		t.setBlock("tech.cores.listitem","tech.core.produces.listitem","tech.core.produces.list");
		t.setVar("tech.cores.list","");

		firstentry = true;
		Iterator<?> coreIter = db.createQuery("from Core where techReq=?")
			.setInteger(0, this.research.getID())
			.iterate();
		for( ; coreIter.hasNext(); ) {
			Core core = (Core)coreIter.next();
			
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
				t.setVar(	"res.image",	config.get("URL")+"data/interface/energie.gif",
							"res.cargo",	core.getEVerbrauch() );
							
				t.parse("tech.core.consumes.list","tech.core.consumes.listitem",true);
			}

			reslist = core.getProduces().getResourceList();
			Resources.echoResList( t, reslist, "tech.core.produces.list" );

			if( core.getEProduktion() > 0 ) {
				t.setVar(	"res.image",	config.get("URL")+"data/interface/energie.gif",
							"res.cargo",	core.getEProduktion() );
							
				t.parse("tech.core.produces.list","tech.core.produces.listitem",true);
			}
	
			t.parse("tech.cores.list","tech.cores.listitem",true);
	
			t.stop_record();
			t.clear_record();
		}
				
				
		//
		// Schiffe
		//
		t.setBlock("_FORSCHINFO","tech.ships.listitem","tech.ships.list");
		t.setBlock("tech.ships.listitem","tech.ship.costs.listitem","tech.ship.costs.list");
		t.setBlock("tech.ships.listitem","tech.ship.techs.listitem","tech.ship.techs.list");
		t.setVar("tech.ships.list","");

		firstentry = true;
		List<?> ships = db.createQuery("from ShipBaubar " +
				"where tr1= :fid or tr2= :fid or tr3= :fid")
				.setInteger("fid", this.research.getID())
				.list();
		for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
			ShipBaubar ship = (ShipBaubar)iter.next();
			
			boolean show = true;

			//Schiff sichtbar???
			for( int i=1; i <= 3; i++ ) {
				if( ship.getRes(i) > 0 ) {
					Forschung tmpres = Forschung.getInstance(ship.getRes(i));
					if( !tmpres.isVisibile(user) && 
						(!user.hasResearched(ship.getRes(i)) || !user.hasResearched(tmpres.getRequiredResearch(1)) || 
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

			ShipTypeData shiptype = ship.getType();
			
			t.setVar(	"tech.ship.id",			shiptype.getTypeId(),
						"tech.ship.name",		Common._plaintitle(shiptype.getNickname()),
						"tech.ship.picture",	shiptype.getPicture(),
						"tech.ship.hr",			!firstentry,
						"tech.ship.dauer",		ship.getDauer(),
						"tech.ship.ekosten",	ship.getEKosten(),
						"tech.ship.crew",		ship.getCrew() );
		
			if( firstentry ) {
				firstentry = false;
			}
		
			costs = ship.getCosts();
		
			reslist = costs.getResourceList();
			Resources.echoResList( t, reslist, "tech.ship.costs.list" );
		
			//Benoetigt dieses Schiff noch weitere Forschungen???
			if( ((ship.getRes(1) != 0) && (ship.getRes(1) != this.research.getID())) || 
				((ship.getRes(2) != 0) && (ship.getRes(2) != this.research.getID())) || 
				((ship.getRes(3) != 0) && (ship.getRes(3) != this.research.getID())) ) {
				firstentry = true;
				
				//Es benoetigt weitere!
				t.setVar("tech.ship.techs.list","");
		      	for( int b = 1; b <= 3; b++ ) {
         			if( (ship.getRes(b) != 0) && (ship.getRes(b) != this.research.getID()) ) {
         				t.setVar(	"tech.ship.tech.break",	!firstentry,
         							"tech.ship.tech.id",	ship.getRes(b),
         							"tech.ship.tech.name",	Forschung.getInstance(ship.getRes(b)).getName() );
         							
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

		//
		// Munition
		//
		t.setBlock("_FORSCHINFO","tech.ammo.listitem","tech.ammo.list");
		t.setBlock("tech.ammo.listitem","tech.ammo.buildcosts.listitem","tech.ammo.buildcosts.list");
		t.setVar("tech.ammo.list","");

		firstentry = true;

		List<?> entryList = db.createQuery("from FactoryEntry " +
				"where res1= :fid or res2= :fid or res3= :fid")
				.setInteger("fid", this.research.getID())
				.list();
		
		for( Iterator<?> iter=entryList.iterator(); iter.hasNext(); ) {
			FactoryEntry facentry = (FactoryEntry)iter.next();
			t.start_record();
	
			t.setVar(	"tech.ammo.hr",			!firstentry,
						"tech.ammo.name",		Common._plaintitle(facentry.getName()),
						"tech.ammo.picture",	facentry.getPicture(),
						"tech.ammo.description",	Common._text(facentry.getDescription()),
						"tech.ammo.itemid",		facentry.getItemId(),
						"tech.ammo.dauer",		facentry.getDauer() );
	
			if( firstentry ) {
				firstentry = false;
			}
			
			Cargo buildcosts = facentry.getBuildCosts();

			// Produktionskosten	
			reslist = buildcosts.getResourceList();
			Resources.echoResList( t, reslist, "tech.ammo.buildcosts.list" );
				
			t.parse( "tech.ammo.list", "tech.ammo.listitem", true );
		
			t.stop_record();
			t.clear_record();
		}
		
		if(this.research.getSpecializationCosts() > 0 && user.getUserResearch(this.research) != null)
		{
			t.setVar("tech.dropable", 1);
		}
	}
}
