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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Kolonisieren eines Asteroiden mittels eines Colonizers (Schiff)
 * @author Christopher Jung
 * 
 * @urlparam Integer ship Die Schiffs-ID des Colonizers
 * @urlparam Integer col Die Basis-ID des zu kolonisierenden Asteroiden
 *
 */
public class ColonizeController extends TemplateGenerator {
	private Ship ship;
	private Base base;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public ColonizeController(Context context) {
		super(context);
		
		setTemplate("colonize.html");
		
		parameterNumber("ship");
		parameterNumber("col");
		
		setPageTitle("Kolonisieren");
	}
	
	@Override
	protected boolean validateAndPrepare( String action ) {
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		
		int shipId = getInteger("ship");
		int col = getInteger("col");
		
		Ship ship = (Ship)db.get(Ship.class, shipId);
		if( (ship == null) || (ship.getOwner() != user) ) {
			addError("Fehler: Das angegebene Schiff existiert nicht oder geh&ouml;rt ihnen nicht", Common.buildUrl("default", "module", "schiffe") );
			
			return false;
		}
		
		ShipTypeData shiptype = ship.getTypeData();
		if( !shiptype.hasFlag(ShipTypes.SF_COLONIZER) ) {
			addError("Fehler: Das angegebene Schiff kann keine Planeten kolonisieren", Common.buildUrl("default", "module", "schiff" , "ship", shipId) );
			
			return false;
		}

		Base base = (Base)getDB().get(Base.class, col);
		if( (base == null) || (base.getOwner().getId() != 0) ) {
			addError("Fehler: Der angegebene Asteroid existiert nicht oder geh&ouml;rt bereits einem anderen Spieler", Common.buildUrl("default", "module", "schiff" , "ship", shipId) );
			
			return false;
		}

		if( !ship.getLocation().sameSector(0, base.getLocation(), base.getSize()) ) {
			addError("Fehler: Der Asteroid befindet sich nicht im selben Sektor wie das Schiff", Common.buildUrl("default", "module", "schiff", "ship", shipId) );
			
			return false;
		}
		
		this.base = base;
		this.ship = ship;
		
		t.setVar( "ship.id", ship.getId() );
		
		return true;	
	}
	
	/**
	 * Der Kolonisiervorgang
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		
		Integer[] bebauung = base.getBebauung();
		Integer[] bebon = base.getActive();
		
		Map<Integer,Integer> bases = new HashMap<Integer,Integer>();
		bases.put(base.getSystem(), 0);
		int basecount = 0;
		
		final List<?> baseList = db.createQuery("from Base where owner=?")
			.setEntity(0, user)
			.list();
		for( Iterator<?> iter=baseList.iterator(); iter.hasNext(); ) {
			Base aBase = (Base)iter.next();
			
			final int system = aBase.getSystem();
			Common.safeIntInc(bases, system);
			basecount += aBase.getMaxTiles();
		}
		basecount += this.base.getMaxTiles();
		
		if( basecount > Integer.parseInt(user.getUserValue("GAMEPLAY/bases/maxtiles")) ) {
			t.setVar("colonize.message", "<span style=\"color:#ff0000; font-weight:bold\">Kolonisierung unzul&auml;ssig, da dies die Gesamtzahl an zul&auml;ssigen Oberfl&auml;chenfeldern "+user.getUserValue("GAMEPLAY/bases/maxtiles")+" &uuml;bersteigen w&uuml;rde.</span>");
	
			return;
		}

		if( (Systems.get().system(base.getSystem()).getMaxColonies() >= 0) && 
			(bases.get(base.getSystem()) >= Systems.get().system(base.getSystem()).getMaxColonies()) ) {
			t.setVar("colonize.message", "<span style=\"color:#ff0000\">Sie d&uuml;rfen lediglich "+Systems.get().system(base.getSystem()).getMaxColonies()+" Asteroiden in "+Systems.get().system(base.getSystem()).getName()+" ("+base.getSystem()+") kolonisieren" );
			
			return;
		}
		
		int crew = ship.getCrew();
		int e = ship.getEnergy();
		
		/* 
		 * 
		 * Evt muessen einige Gebaeude entfernt werden, wenn der betreffende Spieler sonst zu viele haette
		 * 
		 */
		//Anzahl der Gebaeude pro Spieler berechnen
		Map<Integer,Integer> ownerBuildingCount = new HashMap<Integer,Integer>();
		
		for( Iterator<?> iter=baseList.iterator(); iter.hasNext(); ) {
			Base aBase = (Base)iter.next();
			Integer[] abeb = aBase.getBebauung();
			for( int i=0; i < abeb.length; i++ ) {
				if( ownerBuildingCount.containsKey(abeb[i]) ) {
					ownerBuildingCount.put( abeb[i], ownerBuildingCount.get(abeb[i])+1 );
				}
				else {
					ownerBuildingCount.put( abeb[i], 1 );
				}
			}
		}
		
		// Problematische Gebaeude ermitteln
		Map<Integer,Integer> problematicBuildings = new HashMap<Integer,Integer>();
		Iterator<?> buildingIter = db.createQuery("from Building where perOwner>0").iterate();
		for( ; buildingIter.hasNext(); ) {
			Building aBuilding = (Building)buildingIter.next();
			problematicBuildings.put(aBuilding.getId(), aBuilding.getPerUserCount());
		}
		
		// Nun die Gebaeude auf dem Asti durchlaufen und bei Bedarf einige entfernen
		for( int index=0; index < bebauung.length; index++ ) {
			final Integer building = bebauung[index];
			if( problematicBuildings.containsKey(building) && ownerBuildingCount.containsKey(building) && 
				(ownerBuildingCount.get(building) + 1 > problematicBuildings.get(building)) ) {
				bebauung[index] = 0;
				bebon[index] = 0;
			}
			if( !ownerBuildingCount.containsKey(building) ) {
				ownerBuildingCount.put(building, 0);
			}
			ownerBuildingCount.put(building, ownerBuildingCount.get(building) + 1 );
		}

		/*
		 * 
		 * Nun den Asteroiden kolonisieren
		 * 
		 */

		Cargo cargo = ship.getCargo();
		
		t.setBlock("_COLONIZE", "res.listitem", "res.list");
		
		ResourceList reslist = cargo.getResourceList();
		for( ResourceEntry res : reslist ) {
			t.setVar(	"res.image",	res.getImage(),
						"res.name",		res.getName(),
						"res.cargo",	res.getCargo1() );
			t.parse("res.list", "res.listitem", true);
		}
		
		Cargo cargo2 = base.getCargo();
		cargo.addCargo( cargo2 );

		db.createQuery("update Offizier set dest=? where dest=?")
			.setString(0, "b "+base.getId())
			.setString(1, "s "+ship.getId())
			.executeUpdate();
		
		ship.destroy();

		// Die Kommandozentrale setzen
		bebauung[0] = 1;
		bebon[0] = 1;

		base.setBebauung(bebauung);
		base.setActive(bebon);
		base.setCargo(cargo);
		base.setOwner(user);
		base.setBewohner(crew);
		base.setEnergy(e);
		
		db.createQuery("update Offizier set userid=? where dest in (?,?)")
			.setEntity(0, user)
			.setString(1, "b "+base.getId())
			.setString(2, "t "+base.getId())
			.executeUpdate();
		
		t.setVar("base.id", base.getId());		
	}

}
