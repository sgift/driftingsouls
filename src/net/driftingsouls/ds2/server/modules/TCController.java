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

import java.util.ArrayList;
import java.util.List;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Transferiert Offiziere von und zu Schiffen/Basen
 * 
 * @author Christopher Jung
 * 
 * @urlparam Integer ship Die ID des Schiffes, das Ausgangspunkt des Transfers ist.
 * @urlparam Integer target Die ID des Ziels des Transfers
 * @urlparam String conf "ok", falls eine Sicherheitsabfrage bestaetigt werden soll
 * @urlparam Integer off Die ID des zu transferierenden Offiziers, falls mehr als ein Offizier zur Auswahl steht
 *
 */
public class TCController extends DSGenerator {
	private SQLResultRow ship = null;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public TCController(Context context) {
		super(context);
		
		setTemplate("tc.html");	
		
		parameterNumber("ship");
		parameterNumber("target");
		parameterString("conf");
		parameterNumber("off");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		int shipId = getInteger("ship");
		
		t.set_var( "global.shipid", shipId );

		ship = db.first("SELECT * FROM ships WHERE id>0 AND owner='",this.getUser().getID(),"' AND id='",shipId,"'");

		if( ship.isEmpty() ) {
			addError("Das angegebene Schiff existiert nicht oder geh&ouml;rt ihnen nicht",  Common.buildUrl(getContext(), "default", "module", "schiffe") );
			
			return false;
		}
		
		if( ship.getInt("battle") != 0 ) {
			addError("Das angegebene Schiff befindet sich in einer Schlacht", Common.buildUrl(getContext(), "default", "module", "schiffe" ) );
			
			return false;
		}

		return true;
	}
	
	/**
	 * Offiziersliste eines Objekts ausgeben
	 * 
	 * @param mode	Transfermodus (shipToShip, baseToShip usw)
	 * @param dest	Typ des Aufenthaltsort der Offiziere (s,b usw)
	 * @param destid ID des Aufenthaltsortes
	 */
	private void echoOffiList( String mode, String  dest, int destid ) {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		t.set_var(	"tc.selectoffizier",	1,
					"tc.mode",				mode );
				
		t.set_block( "_TC", "tc.offiziere.listitem", "tc.offiziere.list" );
		
		SQLQuery offiRow = db.query("SELECT * FROM offiziere WHERE dest='",dest," ",destid,"'");
		while( offiRow.next() ) {
			Offizier offi = new Offizier( offiRow.getRow() );
			t.set_var(	"tc.offizier.picture",		offi.getPicture(),
						"tc.offizier.id",			offi.getID(),
						"tc.offizier.name",			Common._plaintitle(offi.getName()),
						"tc.offizier.ability.ing",	offi.getAbility( Offizier.Ability.ING ),
						"tc.offizier.ability.nav",	offi.getAbility( Offizier.Ability.NAV ),
						"tc.offizier.ability.waf",	offi.getAbility( Offizier.Ability.WAF ),
						"tc.offizier.ability.sec",	offi.getAbility( Offizier.Ability.SEC ),
						"tc.offizier.ability.com",	offi.getAbility( Offizier.Ability.COM ),
						"tc.offizier.special",		offi.getSpecial() );
				
			t.parse( "tc.offiziere.list", "tc.offiziere.listitem", true );
		}
		offiRow.free();
	}

	/**
	 * Transferiert einen Offizier von einem Schiff zu einem Schiff
	 *
	 */
	public void shipToShipAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		String conf = getString("conf");
		int off = getInteger("off");
		
		String errorurl = Common.buildUrl(getContext(), "default", "module", "schiff", "ship", ship.getInt("id"));
		
		SQLResultRow tarShip = db.first("SELECT * FROM ships WHERE id>0 AND id=",getInteger("target"));
	
		t.set_var( 	"tc.ship",			ship.getInt("id"),
					"tc.target",		tarShip.getInt("id"),
					"tc.target.name",	tarShip.getString("name"),
					"tc.target.isown",	(tarShip.getInt("owner") == user.getID()),
					"tc.stos",			1,
					"tc.mode",			"shipToShip" );
	
		if( !Location.fromResult(ship).sameSector(0, Location.fromResult(tarShip), 0) ) {
			addError( "Die beiden Schiffe befinden sich nicht im selben Sektor", errorurl );
			setTemplate("");
			
			return;
		}
		
		if( tarShip.getInt("battle") != 0 ) {
			addError("Das Zielschiff befindet sich in einer Schlacht", Common.buildUrl(getContext(),"default", "module", "schiffe" ) );
			
			return;
		}
	
		int officount = db.first("SELECT count(*) count FROM offiziere WHERE dest='s ",ship.getInt("id"),"' AND userid=",user.getID()).getInt("count");
		if( officount == 0 ) {
			addError("Das Schiff hat keinen Offizier an Bord", errorurl );
			setTemplate("");
			
			return;
		}
	
		//IFF-Check
		boolean disableIFF = (tarShip.getString("status").indexOf("disable_iff") > -1);
		if( disableIFF ) {
			addError("Sie k&ouml;nnen keinen Offizier zu diesem Schiff transferieren", errorurl);
			setTemplate("");
			
			return;
		}

		SQLResultRow tarShipType = Ships.getShipType(tarShip);
		
		// Schiff gross genug?
		if( tarShipType.getInt("size") < 3 ) {
			addError("Das Schiff ist zu klein f&uuml;r einen Offizier", errorurl);
			setTemplate("");
			
			return;
		}

		// Check ob noch fuer einen weiteren Offi platz ist
		int maxoffis = 1;
		if( Ships.hasShipTypeFlag(tarShipType, Ships.SF_OFFITRANSPORT) ) {
			maxoffis = tarShipType.getInt("crew");
		}
		
		int tarOffiCount = db.first("SELECT count(*) count FROM offiziere WHERE dest='s ",tarShip.getInt("id"),"' AND userid=",tarShip.getInt("owner")).getInt("count");
		if( tarOffiCount >= maxoffis ) {
			addError("Das Schiff hat bereits die maximale Anzahl Offiziere an Bord", errorurl );
			setTemplate("");
			
			return;
		}
	
		// Offiziersliste bei bedarf ausgeben
		if( (officount > 1) && (off == 0) ) {
			echoOffiList("shipToShip", "s", ship.getInt("id"));
			return;
		}
		
		// Offizier laden
		Offizier offizier = null;
		if( off != 0 ) {
			offizier = Offizier.getOffizierByID(off);
		}
		else {
			offizier = Offizier.getOffizierByDest('s', ship.getInt("id"));
		}
		
		if( (offizier == null) || (offizier.getOwner() != user.getID()) ) {
			addError("Der angegebene Offizier existiert nicht oder geh&ouml;rt nicht ihnen", errorurl);
			setTemplate("");
			
			return;
		}
			
		String[] dest = offizier.getDest();
		if( !dest[0].equals("s") || (Integer.parseInt(dest[1]) != ship.getInt("id")) ) {
			addError("Der angegebene Offizier befindet sich nicht auf dem Schiff", errorurl);
			setTemplate("");
			
			return;
		}
	
		t.set_var( "tc.offizier.name", Common._plaintitle(offizier.getName()) );
		
		// Confirm?
		if( (tarShip.getInt("owner") != user.getID()) && !conf.equals("ok") ) {
			t.set_var("tc.confirm",1);
			
			return;
		}
	
		// Transfer!
		offizier.setDest( "s", tarShip.getInt("id") );
		offizier.setOwner( tarShip.getInt("owner") );
		offizier.save();
	
		Ships.recalculateShipStatus( ship.getInt("id") );
		Ships.recalculateShipStatus( tarShip.getInt("id") );
	}
	
	/**
	 * Transferiert einen Offizier von einem Schiff zu einer Basis
	 *
	 */
	public void shipToBaseAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		String conf = getString("conf");
		int off = getInteger("off");
		
		String errorurl = Common.buildUrl(getContext(), "default", "module", "schiff", "ship", ship.getInt("id"));
		
		SQLResultRow tarBase = db.first("SELECT id,x,y,system,size,owner,name FROM bases WHERE id=",getInteger("target"));
	
		t.set_var(	"tc.ship",			ship.getInt("id"),
					"tc.target",		tarBase.getInt("id"),
					"tc.target.name",	tarBase.getString("name"),
					"tc.target.isown",	(tarBase.getInt("owner") == user.getID()),
					"tc.stob",			1,
					"tc.mode",			"shipToBase" );
	
		if( !Location.fromResult(ship).sameSector(0, Location.fromResult(tarBase), tarBase.getInt("size")) ) {
			addError( "Schiff und Basis befinden sich nicht im selben Sektor", errorurl );
			setTemplate("");
			
			return;
		}
	
		int officount = db.first("SELECT count(*) count FROM offiziere WHERE dest='s ",ship.getInt("id"),"' AND userid=",user.getID()).getInt("count");
		if( officount == 0 ) {
			addError("Das Schiff hat keinen Offizier an Bord", errorurl);
			setTemplate("");
			
			return;
		}
		
		// bei bedarf offiliste ausgeben
		if( (officount > 1) && (off == 0) ) {
			echoOffiList("shipToBase", "s", ship.getInt("id"));
			return;
		}
		
		// Offi laden
		Offizier offizier = null;
		if( off != 0 ) {
			offizier = Offizier.getOffizierByID(off);
		}
		else {
			offizier = Offizier.getOffizierByDest('s', ship.getInt("id"));
		}

		if( (offizier == null) || (offizier.getOwner() != user.getID()) ) {
			addError("Der angegebene Offizier existiert nicht oder geh&ouml;rt nicht ihnen", errorurl);
			setTemplate("");
			
			return;
		}
			
		String[] dest = offizier.getDest();
		if( !dest[0].equals("s") || (Integer.parseInt(dest[1]) != ship.getInt("id")) ) {
			addError("Der angegebene Offizier befindet sich nicht auf dem Schiff", errorurl);
			setTemplate("");
			
			return;
		}
		
		t.set_var( "tc.offizier.name", Common._plaintitle(offizier.getName()) );
	
		// Confirm ?
		if( (tarBase.getInt("owner") != user.getID()) && (conf != "ok") ) {
			t.set_var( "tc.confirm", 1 );
			
			return;
		}

		// Transfer !
		offizier.setDest( "b", tarBase.getInt("id") );
		offizier.setOwner( tarBase.getInt("owner") );
		offizier.save();
	
		Ships.recalculateShipStatus( ship.getInt("id") );
	}
	
	/**
	 * Transfieriert Offiziere (sofern genug vorhanden) Offiziere von einer Basis
	 * zu einer Flotte
	 *
	 */
	public void baseToFleetAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		String errorurl = Common.buildUrl(getContext(), "default", "module", "schiff", "ship", ship.getInt("id"));
		
		t.set_var( "tc.ship", ship.getInt("id") );
	
		SQLResultRow upBase = db.first("SELECT id,x,y,system,size,owner,name FROM bases WHERE id=",getInteger("target")," AND owner='",user.getID(),"'");
		
		if( !Location.fromResult(ship).sameSector(0, Location.fromResult(upBase), upBase.getInt("size")) ) {
			addError("Schiff und Basis befinden sich nicht im selben Sektor", errorurl);
			setTemplate("");
			
			return;
		}
	
		t.set_var(	"tc.captainzuweisen",	1,
					"tc.ship",				ship.getInt("id"),
					"tc.target",			upBase.getInt("id") );
							
		List<Offizier> offilist = new ArrayList<Offizier>();
	
		SQLQuery offiRow = db.query("SELECT * FROM offiziere WHERE dest='b ",upBase.getInt("id"),"'");
		while( offiRow.next() ){
			offilist.add(new Offizier(offiRow.getRow()));
		}
		offiRow.free();

		int shipcount = 0;
	
		SQLQuery ship = db.query("SELECT id FROM ships WHERE fleet='",this.ship.getInt("fleet"),"' AND owner='",user.getID(),"' AND system='",this.ship.getInt("system"),"' AND x='",this.ship.getInt("x"),"' AND y='",this.ship.getInt("y"),"' AND !LOCATE('offizier',status) LIMIT ",offilist.size());
		while( ship.next() ) {
			SQLResultRow shipType = Ships.getShipType(ship.getRow());
			if( shipType.getInt("size") < 3 ) {
				continue;
			}
			
			Offizier offi = offilist.remove(0);
			offi.setDest( "s", ship.getInt("id") );
			offi.save();
			
			Ships.recalculateShipStatus( ship.getInt("id") );
			
			shipcount++;
		}
		ship.free();
		
		t.set_var("tc.message", shipcount+" Offiziere wurden transferiert" );
	}
	
	/**
	 * Transfieriert Offiziere von einer Basis zu einem Schiff
	 *
	 */
	public void baseToShipAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		int off = getInteger("off");
		
		String errorurl = Common.buildUrl(getContext(), "default", "module", "schiff", "ship", ship.getInt("id"));
		
		t.set_var( "tc.ship", ship.getInt("id") );
	
		SQLResultRow upBase = db.first("SELECT id,x,y,system,size,owner,name FROM bases WHERE id=",getInteger("target")," AND owner='",user.getID(),"'");
		
		if( !Location.fromResult(ship).sameSector(0, Location.fromResult(upBase), upBase.getInt("size")) ) {
			addError("Schiff und Basis befinden sich nicht im selben Sektor", errorurl);
			setTemplate("");
			
			return;
		}
		
		SQLResultRow shipType = Ships.getShipType(ship);
		if( shipType.getInt("size") < 3 ) {
			addError("Das Schiff ist zu klein f&uuml;r einen Offizier", errorurl);
			setTemplate("");
			
			return;
		}
	
		t.set_var(	"tc.captainzuweisen",	1,
					"tc.offizier",			off,
					"tc.ship",				ship.getInt("id"),
					"tc.target",			upBase.getInt("id") );

		// Wenn noch kein Offizier ausgewaehlt wurde -> Liste der Offiziere in der Basis anzeigen
		if( off == 0 ) {
			echoOffiList("baseToShip", "b", upBase.getInt("id"));
			
			if( ship.getInt("fleet") != 0 ) {
				int count = db.first("SELECT count(*) count FROM ships WHERE fleet='",ship.getInt("fleet"),"' AND owner='",user.getID(),"' AND system='",ship.getInt("system"),"' AND x='",ship.getInt("x"),"' AND y='",ship.getInt("y"),"' AND !LOCATE('offizier',status)").getInt("count");
				if( count > 1 ) {
					t.set_var(	"show.fleetupload",	1,
								"tc.fleetmode",		"baseToFleet");
				}	
			}
		} 
		//ein Offizier wurde ausgewaehlt -> transferieren
		else {
			String ch = db.first("SELECT dest FROM offiziere WHERE id=",off).getString("dest");
			if( !ch.equals("b "+upBase.getInt("id")) ) {
				addError("Der angegebene Offizier ist nicht in der Basis stationiert", errorurl);
				setTemplate("");
				
				return;
			} 
			
			// Check ob noch fuer einen weiteren Offi platz ist
			SQLResultRow tarShipType = Ships.getShipType(ship);
	
			int offi = db.first("SELECT count(*) count FROM offiziere WHERE dest='s ",ship.getInt("id"),"'").getInt("count");
			int maxoffis = 1;
			if( Ships.hasShipTypeFlag(tarShipType, Ships.SF_OFFITRANSPORT) ) {
				maxoffis = tarShipType.getInt("crew");
			}
			
			if( offi >= maxoffis ) {
				addError("Das Schiff hat bereits die maximale Anzahl Offiziere an Bord", errorurl );
				setTemplate("");
			
				return;
			}
			
			SQLResultRow offizierRow = db.first("SELECT * FROM offiziere WHERE id=",off);
			Offizier offizier = new Offizier( offizierRow );
			t.set_var("tc.offizier.name",Common._plaintitle(offizier.getName()));
				
			offizier.setDest( "s", ship.getInt("id") );
			offizier.save();
			
			Ships.recalculateShipStatus( ship.getInt("id") );
		}
	}
	
	@Override
	public void defaultAction() {
		// EMPTY
	}
}
