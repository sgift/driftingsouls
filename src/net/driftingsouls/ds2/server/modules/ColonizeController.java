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
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Kolonisieren eines Asteroiden mittels eines Colonizers (Schiff)
 * @author Christopher Jung
 * 
 * @urlparam Integer ship Die Schiffs-ID des Colonizers
 * @urlparam Integer col Die Basis-ID des zu kolonisierenden Asteroiden
 *
 */
public class ColonizeController extends TemplateGenerator {
	private SQLResultRow ship;
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
	}
	
	@Override
	protected boolean validateAndPrepare( String action ) {
		Database db = getDatabase();
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		
		int shipId = getInteger("ship");
		int col = getInteger("col");
		
		SQLResultRow ship = db.first("SELECT * FROM ships WHERE id>0 AND owner='",user.getId(),"' AND id='",shipId,"'");
		if( ship.isEmpty() ) {
			addError("Fehler: Das angegebene Schiff existiert nicht oder geh&ouml;rt ihnen nicht", Common.buildUrl("default", "module", "schiffe") );
			
			return false;
		}
		
		SQLResultRow shiptype = ShipTypes.getShipType( ship );
		if( !ShipTypes.hasShipTypeFlag(shiptype, ShipTypes.SF_COLONIZER) ) {
			addError("Fehler: Das angegebene Schiff kann keine Planeten kolonisieren", Common.buildUrl("default", "module", "schiff" , "ship", shipId) );
			
			return false;
		}

		SQLResultRow base = db.first("SELECT * FROM bases WHERE owner=0 AND id='",col,"'");
		if( base.isEmpty() ) {
			addError("Fehler: Der angegebene Asteroid existiert nicht oder geh&ouml;rt bereits einem anderen Spieler", Common.buildUrl("default", "module", "schiff" , "ship", shipId) );
			
			return false;
		}

		if( !Location.fromResult(ship).sameSector(0, Location.fromResult(base), base.getInt("size")) ) {
			addError("Fehler: Der Asteroid befindet sich nicht im selben Sektor wie das Schiff", Common.buildUrl("default", "module", "schiff", "ship", shipId) );
			
			return false;
		}
		
		this.base = new Base(base);
		this.ship = ship;
		
		t.setVar( "ship.id", ship.getInt("id") );
		
		return true;	
	}
	
	/**
	 * Der Kolonisiervorgang
	 */
	@Action(ActionType.DEFAULT)
	@Override
	public void defaultAction() {
		Database db = getDatabase();
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		
		Integer[] bebauung = base.getBebauung();
		Integer[] bebon = base.getActive();
		
		Map<Integer,Integer> bases = new HashMap<Integer,Integer>();
		bases.put(base.getSystem(), 0);
		int basecount = 0;
		
		SQLQuery tmp = db.query("SELECT system,width,height,maxtiles,cargo FROM bases WHERE owner='",user.getId(),"'");
		while( tmp.next() ){
			final int system = tmp.getInt("system");
			Common.safeIntInc(bases, system);
			basecount += tmp.getInt("maxtiles");
		}
		tmp.free();
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
		
		int crew = ship.getInt("crew");
		int e = ship.getInt("e");
		
		/* 
		 * 
		 * Evt muessen einige Gebaeude entfernt werden, wenn der betreffende Spieler sonst zu viele haette
		 * 
		 */
		//Anzahl der Gebaeude pro Spieler berechnen
		Map<Integer,Integer> ownerBuildingCount = new HashMap<Integer,Integer>();
		
		SQLQuery abebQuery = db.query("SELECT bebauung FROM bases WHERE owner="+user.getId()+" AND id!="+base.getId());
		while( abebQuery.next() ) {
			Integer[] abeb = Common.explodeToInteger("|", abebQuery.getString("bebauung"));
			for( int i=0; i < abeb.length; i++ ) {
				if( ownerBuildingCount.containsKey(abeb[i]) ) {
					ownerBuildingCount.put( abeb[i], ownerBuildingCount.get(abeb[i])+1 );
				}
				else {
					ownerBuildingCount.put( abeb[i], 1 );
				}
			}
		}
		abebQuery.free();
		
		// Problematische Gebaeude ermitteln
		Map<Integer,Integer> problematicBuildings = new HashMap<Integer,Integer>();
		SQLQuery aBuilding = db.query("SELECT id,perowner FROM buildings WHERE perowner>0");
		while( aBuilding.next() ) {
			problematicBuildings.put(aBuilding.getInt("id"), aBuilding.getInt("perowner"));
		}
		aBuilding.free();
		
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

		Cargo cargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
		
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

		db.tBegin();
		db.update("UPDATE offiziere SET dest='b ",base.getId(),"' WHERE dest='s ",ship.getInt("id"),"'");
		Ships.destroy( ship.getInt("id") );

		// Die Kommandozentrale setzen
		bebauung[0] = 1;
		bebon[0] = 1;

		String bebDB = Common.implode("|",bebauung);
		String onDB = Common.implode("|",bebon);

		db.tUpdate(1, "UPDATE bases SET bebauung='",bebDB,"',active='",onDB,"',cargo='",cargo.save(),"',owner='",user.getId(),"',bewohner='",crew,"',e='",e,"' WHERE id='",base.getId(),"' AND owner=0 AND cargo='",cargo2.save(),"'");

		db.update("UPDATE offiziere SET userid=",user.getId()," WHERE dest IN ('b ",base.getId(),"','t "+base.getId(),"')");
		if( !db.tCommit() ) {
			addError("Beim kolonisieren ist ein Fehler aufgetreten. Bitte versuchen sie es sp&auml;ter erneut");
		}
		t.setVar("base.id", base.getId());		
	}

}
