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
import net.driftingsouls.ds2.server.bases.Base;
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
public class TCController extends TemplateGenerator {
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
		
		t.setVar( "global.shipid", shipId );

		ship = db.first("SELECT * FROM ships WHERE id>0 AND owner='",this.getUser().getId(),"' AND id='",shipId,"'");

		if( ship.isEmpty() ) {
			addError("Das angegebene Schiff existiert nicht oder geh&ouml;rt ihnen nicht",  Common.buildUrl("default", "module", "schiffe") );
			
			return false;
		}
		
		if( ship.getInt("battle") != 0 ) {
			addError("Das angegebene Schiff befindet sich in einer Schlacht", Common.buildUrl("default", "module", "schiffe" ) );
			
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

		t.setVar(	"tc.selectoffizier",	1,
					"tc.mode",				mode );
				
		t.setBlock( "_TC", "tc.offiziere.listitem", "tc.offiziere.list" );
		
		List<Offizier> offiList = getContext().query("from Offizier where dest='"+dest+" "+destid+"'", Offizier.class);
		for( Offizier offi : offiList ) {
			t.setVar(	"tc.offizier.picture",		offi.getPicture(),
						"tc.offizier.id",			offi.getID(),
						"tc.offizier.name",			Common._plaintitle(offi.getName()),
						"tc.offizier.ability.ing",	offi.getAbility( Offizier.Ability.ING ),
						"tc.offizier.ability.nav",	offi.getAbility( Offizier.Ability.NAV ),
						"tc.offizier.ability.waf",	offi.getAbility( Offizier.Ability.WAF ),
						"tc.offizier.ability.sec",	offi.getAbility( Offizier.Ability.SEC ),
						"tc.offizier.ability.com",	offi.getAbility( Offizier.Ability.COM ),
						"tc.offizier.special",		offi.getSpecial().getName() );
				
			t.parse( "tc.offiziere.list", "tc.offiziere.listitem", true );
		}
	}

	/**
	 * Transferiert einen Offizier von einem Schiff zu einem Schiff
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void shipToShipAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		String conf = getString("conf");
		int off = getInteger("off");
		
		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getInt("id"));
		
		SQLResultRow tarShip = db.first("SELECT * FROM ships WHERE id>0 AND id=",getInteger("target"));
		if( tarShip.isEmpty() ) {
			addError("Das angegebene Zielschiff existiert nicht", errorurl);
			setTemplate("");
			
			return;
		}
	
		t.setVar( 	"tc.ship",			ship.getInt("id"),
					"tc.target",		tarShip.getInt("id"),
					"tc.target.name",	tarShip.getString("name"),
					"tc.target.isown",	(tarShip.getInt("owner") == user.getId()),
					"tc.stos",			1,
					"tc.mode",			"shipToShip" );
	
		if( !Location.fromResult(ship).sameSector(0, Location.fromResult(tarShip), 0) ) {
			addError( "Die beiden Schiffe befinden sich nicht im selben Sektor", errorurl );
			setTemplate("");
			
			return;
		}
		
		if( tarShip.getInt("battle") != 0 ) {
			addError("Das Zielschiff befindet sich in einer Schlacht", Common.buildUrl("default","module", "schiffe" ) );
			
			return;
		}
	
		int officount = db.first("SELECT count(*) count FROM offiziere WHERE dest='s ",ship.getInt("id"),"' AND userid=",user.getId()).getInt("count");
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

		SQLResultRow tarShipType = ShipTypes.getShipType(tarShip);
		
		// Schiff gross genug?
		if( tarShipType.getInt("size") <= 3 ) {
			addError("Das Schiff ist zu klein f&uuml;r einen Offizier", errorurl);
			setTemplate("");
			
			return;
		}

		// Check ob noch fuer einen weiteren Offi platz ist
		int maxoffis = 1;
		if( ShipTypes.hasShipTypeFlag(tarShipType, ShipTypes.SF_OFFITRANSPORT) ) {
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
		
		if( (offizier == null) || (offizier.getOwner() != user) ) {
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
	
		t.setVar( "tc.offizier.name", Common._plaintitle(offizier.getName()) );
		
		// Confirm?
		if( (tarShip.getInt("owner") != user.getId()) && !conf.equals("ok") ) {
			t.setVar("tc.confirm",1);
			
			return;
		}
	
		// Transfer!
		offizier.setDest( "s", tarShip.getInt("id") );
		offizier.setOwner( (User)getDB().get(User.class, tarShip.getInt("owner")) );
	
		Ships.recalculateShipStatus( ship.getInt("id") );
		Ships.recalculateShipStatus( tarShip.getInt("id") );
	}
	
	/**
	 * Transferiert einen Offizier von einem Schiff zu einer Basis
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void shipToBaseAction() {
		Database database = getDatabase();
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		String conf = getString("conf");
		int off = getInteger("off");
		
		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getInt("id"));
		
		Base tarBase = (Base)db.get(Base.class, getInteger("target"));
	
		t.setVar(	"tc.ship",			ship.getInt("id"),
					"tc.target",		tarBase.getId(),
					"tc.target.name",	tarBase.getName(),
					"tc.target.isown",	(tarBase.getOwner() == user),
					"tc.stob",			1,
					"tc.mode",			"shipToBase" );
	
		if( !Location.fromResult(ship).sameSector(0, tarBase.getLocation(), tarBase.getSize()) ) {
			addError( "Schiff und Basis befinden sich nicht im selben Sektor", errorurl );
			setTemplate("");
			
			return;
		}
	
		int officount = database.first("SELECT count(*) count FROM offiziere WHERE dest='s ",ship.getInt("id"),"' AND userid=",user.getId()).getInt("count");
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

		if( (offizier == null) || (offizier.getOwner().getId() != user.getId()) ) {
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
		
		t.setVar( "tc.offizier.name", Common._plaintitle(offizier.getName()) );
	
		// Confirm ?
		if( !user.equals(tarBase.getOwner()) && (conf != "ok") ) {
			t.setVar( "tc.confirm", 1 );
			
			return;
		}

		// Transfer !
		offizier.setDest( "b", tarBase.getId() );
		offizier.setOwner( tarBase.getOwner() );

		Ships.recalculateShipStatus( ship.getInt("id") );
	}
	
	/**
	 * Transfieriert Offiziere (sofern genug vorhanden) Offiziere von einer Basis
	 * zu einer Flotte
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void baseToFleetAction() {
		org.hibernate.Session db = getDB();
		Database database = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getInt("id"));
		
		t.setVar( "tc.ship", ship.getInt("id") );
	
		Base upBase = (Base)db.get(Base.class, getInteger("target"));
		if( (upBase == null) || (upBase.getOwner() != user) ) {
			addError("Die angegebene Basis existiert nicht oder geh&ouml;rt nicht ihnen", errorurl);
			setTemplate("");
			
			return;
		}
		
		if( !Location.fromResult(ship).sameSector(0, upBase.getLocation(), upBase.getSize()) ) {
			addError("Schiff und Basis befinden sich nicht im selben Sektor", errorurl);
			setTemplate("");
			
			return;
		}
	
		t.setVar(	"tc.captainzuweisen",	1,
					"tc.ship",				ship.getInt("id"),
					"tc.target",			upBase.getId() );
							
		List<Offizier> offilist = getContext().query("from Offizier where dest='b "+upBase.getId()+"'", Offizier.class);

		int shipcount = 0;
	
		SQLQuery ship = database.query("SELECT id,type,status FROM ships WHERE fleet='",this.ship.getInt("fleet"),"' AND owner='",user.getId(),"' AND system='",this.ship.getInt("system"),"' AND x='",this.ship.getInt("x"),"' AND y='",this.ship.getInt("y"),"' AND !LOCATE('offizier',status) LIMIT ",offilist.size());
		while( ship.next() ) {
			SQLResultRow shipType = ShipTypes.getShipType(ship.getRow());
			if( shipType.getInt("size") <= 3 ) {
				continue;
			}
			
			Offizier offi = offilist.remove(0);
			offi.setDest( "s", ship.getInt("id") );

			Ships.recalculateShipStatus( ship.getInt("id") );
			
			shipcount++;
		}
		ship.free();
		
		t.setVar("tc.message", shipcount+" Offiziere wurden transferiert" );
	}
	
	/**
	 * Transfieriert Offiziere von einer Basis zu einem Schiff
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void baseToShipAction() {
		Database database = getDatabase();
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		int off = getInteger("off");
		
		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getInt("id"));
		
		t.setVar( "tc.ship", ship.getInt("id") );
	
		Base upBase = (Base)db.get(Base.class,getInteger("target"));
		if( (upBase == null) || (upBase.getOwner() != user) ) {
			addError("Die angegebene Basis existiert nicht oder geh&ouml;rt nicht ihnen");
			setTemplate("");
			
			return;
		}
		
		if( !Location.fromResult(ship).sameSector(0, upBase.getLocation(), upBase.getSize()) ) {
			addError("Schiff und Basis befinden sich nicht im selben Sektor", errorurl);
			setTemplate("");
			
			return;
		}
		
		SQLResultRow shipType = ShipTypes.getShipType(ship);
		if( shipType.getInt("size") < 3 ) {
			addError("Das Schiff ist zu klein f&uuml;r einen Offizier", errorurl);
			setTemplate("");
			
			return;
		}
	
		t.setVar(	"tc.captainzuweisen",	1,
					"tc.offizier",			off,
					"tc.ship",				ship.getInt("id"),
					"tc.target",			upBase.getId() );

		// Wenn noch kein Offizier ausgewaehlt wurde -> Liste der Offiziere in der Basis anzeigen
		if( off == 0 ) {
			echoOffiList("baseToShip", "b", upBase.getId());
			
			if( ship.getInt("fleet") != 0 ) {
				int count = database.first("SELECT count(*) count FROM ships WHERE fleet='",ship.getInt("fleet"),"' AND owner='",user.getId(),"' AND system='",ship.getInt("system"),"' AND x='",ship.getInt("x"),"' AND y='",ship.getInt("y"),"' AND !LOCATE('offizier',status)").getInt("count");
				if( count > 1 ) {
					t.setVar(	"show.fleetupload",	1,
								"tc.fleetmode",		"baseToFleet");
				}	
			}
		} 
		//ein Offizier wurde ausgewaehlt -> transferieren
		else {
			Offizier offizier = (Offizier)getDB().get(Offizier.class, off);
			if( offizier == null ) {
				addError("Der angegebene Offizier existiert nicht", errorurl);
				setTemplate("");
				
				return;
			}
			
			String[] dest = offizier.getDest();
			if( !dest[0].equals("b") || (Integer.parseInt(dest[1]) != upBase.getId()) ) {
				addError("Der angegebene Offizier ist nicht in der Basis stationiert", errorurl);
				setTemplate("");
				
				return;
			}
			
			// Check ob noch fuer einen weiteren Offi platz ist
			SQLResultRow tarShipType = ShipTypes.getShipType(ship);
	
			long offi = ((Number)getDB().createQuery("select count(*) from Offizier where dest=?")
					.setString(0, "s "+ship.getInt("id"))
					.iterate().next()
				).longValue();
			
			int maxoffis = 1;
			if( ShipTypes.hasShipTypeFlag(tarShipType, ShipTypes.SF_OFFITRANSPORT) ) {
				maxoffis = tarShipType.getInt("crew");
			}
			
			if( offi >= maxoffis ) {
				addError("Das Schiff hat bereits die maximale Anzahl Offiziere an Bord", errorurl );
				setTemplate("");
			
				return;
			}
			
			t.setVar("tc.offizier.name",Common._plaintitle(offizier.getName()));
				
			offizier.setDest( "s", ship.getInt("id") );
			
			Ships.recalculateShipStatus( ship.getInt("id") );
		}
	}
	
	@Override
	public void defaultAction() {
		// EMPTY
	}
}
