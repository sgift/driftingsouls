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
import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

import org.apache.commons.lang.StringUtils;

/**
 * Die Flottenverwaltung
 * @author Christopher Jung
 * 
 * @urlparam Integer fleet Die ID der Flotte, falls schon eine existiert
 * @urlparam String shiplist Eine mit | separierte Liste von Schiffs-IDs oder eine mit , separierte Liste mit Koordinaten, Schiffstyp und  Mengenangabe
 * @urlparam Integer sector Die ID eines Schiffs, dessen Sektor "geparst" werden soll
 * @urlparam Integer type Die Typen-ID der zu "parsenden" Schiffe
 * @urlparam Integer count Die Anzahl der maximal zu ermittelnden Schiffe
 *
 */
public class FleetMgntController extends TemplateGenerator {
	private ShipFleet fleet = null;

	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public FleetMgntController(Context context) {
		super(context);
		
		setTemplate("fleetmgnt.html");
		
		addOnLoadFunction("reloadMainpage('"+this.getString("sess")+"')");
		
		parameterNumber("fleet");
		parameterString("shiplist");
		parameterNumber("sector");
		parameterNumber("type");
		parameterNumber("count");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		
		Integer[] shiplist = null;
		
		// Zuerst shiplist verarbeiten
		String shiplistStr = getString("shiplist");
		if( (shiplistStr.length() == 0) || (shiplistStr.charAt(0) != 'g') ) {
			shiplist = Common.explodeToInteger("|", shiplistStr);
		}
		else {
			String[] tmp = StringUtils.split(shiplistStr, ",");
			int sector = Integer.parseInt(tmp[1]);
			int type = Integer.parseInt(tmp[2]);
			
			Ship sectorShip = (Ship)db.get(Ship.class, sector);
			Ship matchingShip = (Ship)db.createQuery("from Ship " +
					"where owner= :user and system= :sys and " +
							"x= :x and y= :y and shiptype= :type and docked=''")
				.setEntity("user", user)
				.setInteger("sys", sectorShip.getSystem())
				.setInteger("x", sectorShip.getX())
				.setInteger("y", sectorShip.getY())
				.setInteger("type", type)
				.iterate().next();
			
			shiplist = new Integer[] {matchingShip.getId()};	
		}
		
		// Evt haben wir bereits eine Flotten-ID uebergeben bekommen -> checken
		int fleetID = getInteger("fleet");
		if( fleetID != 0 ) {
			this.fleet = (ShipFleet)db.get(ShipFleet.class, fleetID);
			if( this.fleet == null ) {
				addError("Die angegebene Flotte existiert nicht");
				
				return false;
			}
			User owner = (User)db.createQuery("select owner from Ship where id>0 and fleet=?")
				.setEntity(0, fleet)
				.iterate().next();

			if( user.getId() != owner.getId() ) {
				this.fleet = null;
				addError("Diese Flotte geh&ouml;rt einem anderen Spieler");
			
				return false;
			}
			
			// Falls sich doch ein Schiff eines anderen Spielers eingeschlichen hat
			db.createQuery("update Ship set fleet=null where fleet= :fleet and owner!= :user")
				.setEntity("fleet", this.fleet)
				.setEntity("user", user)
				.executeUpdate();
		}
		
		if( (this.fleet == null) && !action.equals("createFromSRSGroup") && 
			!action.equals("create") && !action.equals("create2") ) {
			addError("Die angegebene Flotte existiert nicht");
			
			return false;
		}

		int shipid = 0;
		// Nun brauchen wir die ID eines der Schiffe aus der Flotte fuer den javascript-code....
		if( (shiplist == null || shiplist.length == 0) && (fleetID != 0) ) {
			Ship aship = (Ship)db.createQuery("from Ship where id>0 and owner= :user and fleet= :fleet")
				.setEntity("user", user)
				.setEntity("fleet", this.fleet)
				.setMaxResults(1)
				.uniqueResult();
			
			shipid = aship.getId();
		} 
		else if( (shiplist != null) && (shiplist.length > 0) ){
			shipid = shiplist[0];
		}
		
		t.setVar(	"jscript.reloadmain.ship",	shipid,
					"fleet.id",					(fleet != null ? fleet.getId() : 0) );
		
		return true;	
	}
	
	/**
	 * Zeigt den Flottenerstelldialog fuer eine
	 * Flotte aus einer Koordinaten-, Mengen- und Schiffstypenangabe
	 * an
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void createFromSRSGroupAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		
		int sector = getInteger("sector");
		int type = getInteger("type");
		int count = getInteger("count");
		
		Ship sectorShip = (Ship)db.get(Ship.class, sector);

		long shipcount = (Long)db.createQuery("select count(*) from Ship " +
				"where owner= :user and system= :sys and " +
						"x= :x and y= :y and shiptype= :type and docked=''")
			.setEntity("user", user)
			.setInteger("sys", sectorShip.getSystem())
			.setInteger("x", sectorShip.getX())
			.setInteger("y", sectorShip.getY())
			.setInteger("type", type)
			.iterate().next();
		
		if( (count < 1) || (shipcount < count) ) {
			t.setVar("fleetmgnt.message", "Es gibt nicht genug Schiffe im Sektor" );
			return;
		}
			
		t.setVar(	"show.create",		1,
					"create.shiplist",	"g,"+sector+","+type+","+count );
	}
	
	/**
	 * Zeigt den Erstelldialog fuer eine neue Flotte an
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void createAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		
		Integer[] shiplist = Common.explodeToInteger("|", getString("shiplist"));
		
		if( (getString("shiplist").length() == 0) || (shiplist.length == 0) ) {
			t.setVar("fleetmgnt.message", "Sie haben keine Schiffe angegeben" );
			return;
		}
		
		boolean nonEmpty = db.createQuery("from Ship where id in ("+Common.implode(",",shiplist)+") and owner!=?")
			.setEntity(0, user)
			.iterate().hasNext();
		
		if( nonEmpty ) {
			t.setVar("fleetmgnt.message", "Alle Schiffe m&uuml;ssen ihrem Kommando unterstehen" );
		}
		else {		
			t.setVar(	"show.create",			1,
						"create.shiplist",		Common.implode("|",shiplist) );
		}
	}
	
	/**
	 * Erstellt eine Flotte aus einer Schiffsliste oder einer Koordinaten/Typen-Angabe
	 * @urlparam String fleetname der Name der neuen Flotte
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void create2Action() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		
		parameterString("fleetname");
		String fleetname = getString("fleetname");
		
		Integer[] shiplistInt = null;
		
		String shiplist = getString("shiplist");
		if( shiplist.length() == 0 ) {
			t.setVar("fleetmgnt.message", "Sie haben keine Schiffe angegeben" );
			return;
		}
		
		if( shiplist.charAt(0) != 'g' ) {
			shiplistInt = Common.explodeToInteger("|", shiplist);
			if( (getString("shiplist").length() == 0) || shiplistInt.length == 0 ) {
				t.setVar("fleetmgnt.message", "Sie haben keine Schiffe angegeben" );
				return;
			}
		
			boolean nonEmpty = db.createQuery("from Ship where id in ("+Common.implode(",",shiplistInt)+") and (owner!=? or id < 0)")
				.setEntity(0, user)
				.iterate().hasNext();
			if( nonEmpty ) {
				t.setVar("fleetmgnt.message", "Alle Schiffe m&uuml;ssen ihrem Kommando unterstehen" );
				return;
			}
		}
		else {
			String[] tmp = StringUtils.split(shiplist,",");
			int sector = Integer.parseInt(tmp[1]);
			int type = Integer.parseInt(tmp[2]);
			int count = Integer.parseInt(tmp[3]);
			Ship sectorShip = (Ship)db.get(Ship.class, sector);
			if( (sectorShip == null) || (sectorShip.getOwner() != user) || (sectorShip.getId() < 0) ) {
				t.setVar("fleetmgnt.message", "Das Schiff untersteht nicht ihrem Kommando" );
				return;
			}
			
			List ships = db.createQuery("from Ship where id>0 and owner=? and system=? and x=? and y=? and type=? and docked='' order by fleet,id asc")
				.setEntity(0, user)
				.setInteger(1, sectorShip.getSystem())
				.setInteger(2, sectorShip.getX())
				.setInteger(3, sectorShip.getY())
				.setInteger(4, type)
				.setMaxResults(count)
				.list();
			shiplistInt = new Integer[ships.size()];
			int i=0;
			for( Iterator iter=ships.iterator(); iter.hasNext(); ) {
				Ship s = (Ship)iter.next();
				
				if( s.getFleet() != null ) {
					s.removeFromFleet();	
				}
				shiplistInt[i++] = s.getId();
			}
		}
		
		if( fleetname.length() > 0 ) {
			ShipFleet fleet = new ShipFleet(fleetname);
			db.persist(fleet);
			
			for( int i=0; i < shiplistInt.length; i++ ) {
				Ship s = (Ship)db.get(Ship.class, shiplistInt[i]);
				s.setFleet(fleet);
			}
			
			t.setVar(	"fleetmgnt.message",	"Flotte "+Common._plaintitle(fleetname)+" erstellt",
						"jscript.reloadmain",	1,
						"fleet.id",				fleet.getId() );
			
			this.fleet = fleet;
			
			this.redirect();
		} 
		else {
			t.setVar("fleetmgnt.message", "Sie m&uuml;ssen einen Namen angeben" );
			redirect("create");
		}
	}
	
	/**
	 * Fuegt eine definierte Anzahl an Schiffen eines Typs aus einem Sektor zur
	 * Flotte hinzu
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void addFromSRSGroupAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		
		int sector = getInteger("sector");
		int type = getInteger("type");
		int count = getInteger("count");
		
		Ship sectorShip = (Ship)db.get(Ship.class, sector);
		if( (sectorShip == null) || (sectorShip.getId() < 0) || (sectorShip.getOwner() != user) ) {
			t.setVar("fleetmgnt.message", "Das angegebene Schiff existiert oder gehoert ihnen nicht");
			return;
		}
		
		long shipcount = (Long)db.createQuery("select count(*) from Ship where owner=? and system=? and x=? and y=? and type=? and docked=''")
			.setEntity(0, user)
			.setInteger(1, sectorShip.getSystem())
			.setInteger(2, sectorShip.getX())
			.setInteger(3, sectorShip.getY())
			.setInteger(4, type)
			.iterate().next();
		
		if( (count < 1) || (shipcount < count) ) {
			t.setVar("fleetmgnt.message", "Es gibt nicht genug Schiffe im Sektor" );
			return;
		}
		
		List<Ship> shiplist = new ArrayList<Ship>();
		List slist = db.createQuery("from Ship where owner=? and system=? and x=? and y=? and type=? and " +
				"docked='' and fleet!=? order by fleet,id asc")
				.setEntity(0, user)
				.setInteger(1, sectorShip.getSystem())
				.setInteger(2, sectorShip.getX())
				.setInteger(3, sectorShip.getY())
				.setInteger(4, type)
				.setEntity(5, this.fleet)
			.setMaxResults(count)
			.list();
		for( Iterator iter=slist.iterator(); iter.hasNext(); ) {
			Ship s = (Ship)iter.next();
			if( s.getFleet() != null ) {
				s.removeFromFleet();	
			}
			shiplist.add(s);
		}
		
		if( shiplist.isEmpty() ) {
			t.setVar("fleetmgnt.message", "Es gibt nicht genug Schiffe im Sektor" );
			return;
		}
		
		for( Ship s : shiplist ) {
			s.setFleet(this.fleet);
		}
				
		t.setVar(	"fleetmgnt.message",	count+" Schiffe der Flotte hinzugef&uuml;gt",
					"jscript.reloadmain",	1 );
	}
	
	/**
	 * Zeigt die Seite zum Umbenennen von Flotten an
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void renameAction() {
		TemplateEngine t = getTemplateEngine();
		
		t.setVar(	"show.rename",	1,
					"fleet.id",		fleet.getId(),
					"fleet.name",	Common._plaintitle(fleet.getName()) );
	}
	
	/**
	 * Benennt eine Flotte um
	 * @urlparam String fleetname Der neue Name der Flotte
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void rename2Action() {
		TemplateEngine t = getTemplateEngine();
		
		parameterString("fleetname");
		String fleetname = getString("fleetname");
		
		if( fleetname.length() > 0 ) {
			this.fleet.setName(fleetname);

			t.setVar(	"fleetmgnt.message",	"Flotte "+Common._plaintitle(fleetname)+" umbenannt",
						"jscript.reloadmain",	1 );

			redirect();
		}
		else {
			t.setVar("fleetmgnt.message", "Sie m&uuml;ssen einen Namen angeben" );
			
			redirect("rename");
		}
	}
	
	/**
	 * Zeigt die Abfrage an, ob eine Flotte aufgeloest werden soll
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void killAction() {
		TemplateEngine t = getTemplateEngine();
		
		t.setVar(	"fleet.name",	Common._plaintitle(fleet.getName()),
					"fleet.id",		fleet.getId(),
					"show.kill",	1 );
	}
	
	/**
	 * Loest eine Flotte auf
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void kill2Action() {	
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		db.createQuery("update Ship set fleet=null where fleet=?")
			.setEntity(0, this.fleet)
			.executeUpdate();
		db.delete(this.fleet);

		t.setVar(	"fleetmgnt.message",	"Die Flotte '"+fleet.getName()+"' wurde aufgel&ouml;st",
					"jscript.reloadmain",	1 );
	}

	/**
	 * Zeigt das Eingabefeld fuer die Uebergabe von Flotten an
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void newownerAction() {
		TemplateEngine t = getTemplateEngine();
		
		t.setVar(	"show.newowner",	1,
					"fleet.id",			this.fleet.getId(),
					"fleet.name",		Common._plaintitle(this.fleet.getName()) );
	}
	
	/**
	 * Zeigt die Bestaetigung fuer die Uebergabe der Flotte an
	 * @urlparam Integer ownerid Die ID des Users, an den die Flotte uebergeben werden soll
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void newowner2Action() {
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("ownerid");
		int ownerid = getInteger("ownerid");
		
		User newowner = (User)getDB().get(User.class, ownerid );
		
		if( newowner != null ) {
			t.setVar(	"show.newowner2",	1,
						"newowner.name",	Common._title(newowner.getName()),
						"newowner.id",		newowner.getId(),
						"fleet.id",			this.fleet.getId(),
						"fleet.name",		Common._plaintitle(this.fleet.getName()) );
		}
		else {
			t.setVar( "fleetmgnt.message", "Der angegebene Spieler existiert nicht");
			
			redirect("newowner");	
		}	
	}
	
	/**
	 * Uebergibt die Flotte an einen neuen Spieler
	 * @urlparam Integer ownerid Die ID des neuen Besitzers
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void newowner3Action() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)this.getUser();
		org.hibernate.Session db = getDB();
		
		parameterNumber("ownerid");
		int ownerid = getInteger("ownerid");
		
		User newowner = (User)getDB().get(User.class, ownerid);
		
		if( newowner != null ) {
			StringBuilder message = new StringBuilder(100);
			int count = 0;
			
			List<Integer> idlist = new ArrayList<Integer>();
			
			List shiplist = db.createQuery("from Ship where fleet=? and battle is null" )
				.setInteger(0, this.fleet.getId())
				.list();
			for( Iterator iter=shiplist.iterator(); iter.hasNext(); ) {
				Ship aship = (Ship)iter.next();
				boolean tmp = aship.consign(newowner, false );
			
				String msg = Ship.MESSAGE.getMessage();
				if( msg.length() > 0 ) {
					message.append(msg+"<br />");	
				}
				if( !tmp ) {
					count++;
					idlist.add(aship.getId());
					aship.setFleet(this.fleet);
				}
			}

			if( count != 0 ) {
				// Da die Schiffe beim uebergeben aus der Flotte geschmissen werden, muessen wir sie nun wieder hinein tun
				Ship coords = (Ship)db.createQuery("from Ship where owner=? and fleet=?")
					.setEntity(0, newowner)
					.setEntity(1, this.fleet)
					.iterate().next();
				
				PM.send(user, newowner.getId(), "Flotte &uuml;bergeben", "Ich habe dir die Flotte "+Common._plaintitle(this.fleet.getName())+" &uuml;bergeben. Sie steht bei "+coords.getLocation());
		
				t.setVar("fleetmgnt.message", message+"Die Flotte wurde &uuml;bergeben");
			}
			else {
				t.setVar("fleetmgnt.message", message+"Flotten&uuml;bergabe gescheitert");
			}
		}
		else {
			t.setVar( "fleetmgnt.message", "Der angegebene Spieler existiert nicht");
			
			redirect("newowner");	
		}
	}
	
	/**
	 * Laedt die Schilde aller Schiffe in der Flotte auf
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void shupAction() {
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		StringBuilder message = new StringBuilder(100);
		List ships = db.createQuery("from Ship as s left join fetch s.modules where s.fleet=? and (s.shields < s.shiptype.shields or s.shields < s.modules.shields) and s.battle is null")
			.setEntity(0, this.fleet)
			.list();
		for( Iterator iter=ships.iterator(); iter.hasNext(); ) {
			Ship s = (Ship)iter.next();
			ShipTypeData stype = s.getTypeData();
		
			int shieldfactor = 100;
			if( stype.getShields() < 1000 ) {
				shieldfactor = 10;
			}

			int shup = (int)Math.ceil((stype.getShields() - s.getShields())/(double)shieldfactor);
			if( shup > s.getEnergy() ) {
				shup = s.getEnergy();
				message.append(s.getName()+" ("+s.getId()+") - " +
						"<span style=\"color:orange\">Schilde bei "+Math.round((s.getShields()+shup*shieldfactor)/(double)stype.getShields()*100)+"%</span><br />");
			}
			s.setShields(s.getShields() + shup*shieldfactor);
			if( s.getShields() > stype.getShields() ) {
				s.setShields(stype.getShields());
			}
			s.setEnergy(s.getEnergy()-shup);
		}

		t.setVar( "fleetmgnt.message", message+" Die Schilde wurden aufgeladen" );
		
		redirect();			
	}
	
	/**
	 * Entlaedt die Batterien auf den Schiffen der Flotte, um die EPS wieder aufzuladen
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void dischargeBatteriesAction() {
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		StringBuilder message = new StringBuilder(100);
		
		List ships = db.createQuery("from Ship as s left join fetch s.modules " +
				"where s.fleet=? and (s.e < s.shiptype.eps or s.e < s.modules.eps) and s.battle is null")
			.setEntity(0, this.fleet)
			.list();
		for( Iterator iter=ships.iterator(); iter.hasNext(); ) {
			Ship s = (Ship)iter.next();
			ShipTypeData stype = s.getTypeData();

			if( s.getEnergy() >= stype.getEps() ) {
				continue;
			}
			
			Cargo cargo = s.getCargo();
		
			long unload = stype.getEps() - s.getEnergy();
			if( unload > cargo.getResourceCount( Resources.BATTERIEN ) ) {
				unload = cargo.getResourceCount( Resources.BATTERIEN ) ;
				
				message.append(s.getName()+" ("+s.getId()+") - <span style=\"color:orange\">Energie bei "+Math.round((s.getEnergy()+unload)/(double)stype.getEps()*100)+"%</span><br />");
			}
			cargo.substractResource( Resources.BATTERIEN, unload );
			cargo.addResource( Resources.LBATTERIEN, unload );
		
			s.setEnergy((int)(s.getEnergy()+unload));
			s.setCargo(cargo);
		}

		t.setVar( "fleetmgnt.message", message+"Batterien wurden entladen" );
		
		redirect();			
	}
	
	/**
	 * Laedt die Batterien auf den Schiffen der Flotte auf
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void chargeBatteriesAction() {
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		StringBuilder message = new StringBuilder(100);
		
		List ships = db.createQuery("from Ship as s WHERE s.fleet=? and s.battle is null")
			.setEntity(0, this.fleet)
			.list();
		for( Iterator iter=ships.iterator(); iter.hasNext(); ) {
			Ship s = (Ship)iter.next();

			Cargo cargo = new Cargo(s.getCargo());
			if( !cargo.hasResource(Resources.LBATTERIEN) ) {
				continue;
			}
				
			long load = cargo.getResourceCount(Resources.LBATTERIEN);
			if( load > s.getEnergy() ) {
				load = s.getEnergy();
				
				message.append(s.getName()+" ("+s.getId()+") - <span style=\"color:orange\">"+load+"/"+cargo.getResourceCount(Resources.LBATTERIEN)+" Batterien aufgeladen</span><br />");
			}
			cargo.substractResource( Resources.LBATTERIEN, load );
			cargo.addResource( Resources.BATTERIEN, load );
		
			s.setEnergy((int)(s.getEnergy()-load));
			s.setCargo(cargo);
		}

		t.setVar( "fleetmgnt.message", message+"Batterien wurden aufgeladen" );
		
		redirect();			
	}
	
	/**
	 * Exportiert die Schiffsliste der Flotte
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void exportAction() {
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		t.setVar(	"fleet.name",	Common._plaintitle(this.fleet.getName()),
					"show.export",	1 );
							
		t.setBlock("_FLEETMGNT", "exportships.listitem", "exportships.list" );
		
		List ships = db.createQuery("from Ship where id>0 and fleet=?")
			.setEntity(0, this.fleet)
			.list();
		for( Iterator iter=ships.iterator(); iter.hasNext(); ) {
			Ship aship = (Ship)iter.next();
			
			t.setVar(	"ship.id",		aship.getId(),
						"ship.name",	Common._plaintitle(aship.getName()) );
				
			t.parse("exportships.list", "exportships.listitem", true);
		}
	}
	
	/**
	 * Dockt alle Schiffe der Flotte ab
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void undockAction() {
		TemplateEngine t = getTemplateEngine();

		this.fleet.undockContainers();
		
		t.setVar(	"fleetmgnt.message",	"Alle gedockten Schiffe wurden gestartet",
					"jscript.reloadmain",	1 );

		redirect();
	}
	
	/**
	 * Sammelt alle nicht gedockten eigenen Container im Sektor auf (sofern genug Platz vorhanden ist)
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void redockAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		this.fleet.collectContainers(user);

		t.setVar(	"fleetmgnt.message",	"Container wurden aufgesammelt",
					"jscript.reloadmain",	1 );

		redirect();
	}
	
	/**
	 * Startet alle Jaeger der Flotte
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void jstartAction() {
		TemplateEngine t = getTemplateEngine();

		this.fleet.startFighters();
		
		t.setVar(	"fleetmgnt.message",	"Alle J&auml;ger sind gestartet",
					"jscript.reloadmain",	1 );

		redirect();
	}
	
	/**
	 * Sammelt alle nicht gelandeten eigenen Jaeger im Sektor auf (sofern genug Platz vorhanden ist)
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void jlandAction() {	
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		parameterNumber("jaegertype");
		int jaegertypeID = getInteger("jaegertype");
		
		this.fleet.collectFightersByType(user, jaegertypeID);

		t.setVar(	"fleetmgnt.message",	"J&auml;ger wurden aufgesammelt",
					"jscript.reloadmain",	1 );

		redirect();
	}
	
	/**
	 * Fuegt die Schiffe einer anderen Flotte der aktiven Flotte hinzu
	 * @urlparam Integer fleetcombine Die ID der Flotte, deren Schiffe zur aktiven Flotte hinzugefuegt werden sollen
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void fleetcombineAction() {
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		
		parameterNumber("fleetcombine");
		int fleetID = getInteger("fleetcombine");
		
		ShipFleet targetFleet = (ShipFleet)db.get(ShipFleet.class, fleetID);
		if( targetFleet == null ) {
			addError("Die angegebene Flotte existiert nicht!");
			this.redirect();
			return;
		}
		
		User aowner = targetFleet.getOwner();
		if( aowner == null || (aowner != user) ) {
			addError("Die angegebene Flotte geh&ouml;rt nicht ihnen!");
			this.redirect();
			return;
		}
		
		this.fleet.joinFleet(targetFleet);
		
		t.setVar(	"fleetmgnt.message",	"Alle Schiffe der Flotte '"+Common._plaintitle(targetFleet.getName())+"' sind beigetreten",
					"jscript.reloadmain",	1 );
							
		this.redirect();
	}
	
	/**
	 * Aendert die Alarmstufe der Schiffe
	 * @urlparam Integer alarm Die neue Alarmstufe
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void alarmAction() {
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("alarm");
		final int alarm = getInteger("alarm");
		
		this.fleet.setAlarm(alarm);
		
		t.setVar(	"fleetmgnt.message",	"Die Alarmstufe wurde ge&auml;ndert",
					"jscript.reloadmain",	1 );
		
		this.redirect();
	}
	
	/**
	 * Zeigt das Eingabefeld fuer das Umbenennen der Schiffe der Flotte
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void renameShipsAction() {
		TemplateEngine t = getTemplateEngine();
		
		t.setVar(	"show.renameShips",	1,
					"fleet.id",			this.fleet.getId(),
					"fleet.name",		Common._plaintitle(this.fleet.getName()) );
	}
	
	/**
	 * Teil eines Formatierungsstrings fuer Schiffsnamen
	 */
	private static interface NamePatternElement {
		/**
		 * Gibt den Text fuer das naechste Schiff zurueck
		 * @return Der Text
		 */
		public String next();
	}
	
	private static class StringNamePatternElement implements NamePatternElement {
		private String text;
		
		StringNamePatternElement(String text) {
			this.text = text;
		}
		
		public String next() {
			return text;
		}
	}
	
	private static class NumberNamePatternElement implements NamePatternElement {
		private int counter = 1;
		
		NumberNamePatternElement() {
			// EMPTY
		}
		
		public String next() {
			return Integer.toString(counter++);
		}
	}
	
	private static class RomanNumberNamePatternElement implements NamePatternElement {
		private static final String[] base = { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX" };
		private static final String[] tens = { "", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC" };
		private static final String[] hundreds =  { "", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM" };
		private static final String[] thousands = { "", "M", "MM", "MMM" };
		
		private int counter = 1;
	  
		RomanNumberNamePatternElement() {
			// EMPTY
		}
		
		public String next() {
			int number = counter++;
			
			if( counter == 4000 ) {
				counter = 1;
			}
			
		    return thousands[ (number / 1000) ] +
		    	hundreds[ (number / 100) % 10 ] +
				tens[ (number / 10) % 10 ] +
				base[ number % 10 ];
		}
	}
	
	/**
	 * Konvertiert das angegebene Formatierungsmuster fuer Schiffsnamen in eine Liste
	 * von <code>NamePatternElements</code>. 
	 * Die Sortierung entspricht ihrem vorkommen im String.
	 * @param name Der Formatierungsstring
	 * @return Die Liste
	 */
	private List<NamePatternElement> parseNamePattern(String name) {
		List<NamePatternElement> nameParts = new ArrayList<NamePatternElement>();
		do {
			if( name.startsWith("$(") ) {
				int end = name.indexOf(')');
				if( end == -1 ) {
					nameParts.add(new StringNamePatternElement(name));
					break;
				}
				
				String partName = name.substring(2, end);
				
				if( "number".equalsIgnoreCase(partName) ) {
					nameParts.add(new NumberNamePatternElement());
				}
				else if( "roman".equalsIgnoreCase(partName) ) {
					nameParts.add(new RomanNumberNamePatternElement());
				}
				else {
					nameParts.add(new StringNamePatternElement("$("+partName+")"));
				}
				
				if( end == name.length()+1 ) {
					break;
				}
				
				name = name.substring(end+1);
			}
			else {
				int index = name.indexOf("$(");
				if( index != -1 ) {
					nameParts.add(new StringNamePatternElement(name.substring(0,index)));
					name = name.substring(index);
				}
				else {
					nameParts.add(new StringNamePatternElement(name));
					break;
				}
			}
		}
		while( name.contains("$(") );
		
		return nameParts;
	}
	
	/**
	 * Generiert aus einer Liste von Namensteilen den Gesamtnamen fuer das naechste Schiff
	 * @param nameParts Die Namensteile
	 * @return Der Gesamtname
	 */
	private String generateNextShipName(List<NamePatternElement> nameParts) {
		StringBuilder builder = new StringBuilder();
		
		for( int i=0; i < nameParts.size(); i++ ) {
			builder.append(nameParts.get(i).next());
		}
		
		return builder.toString();
	}
	
	/**
	 * Benennt die Schiffe der Flotte um
	 * @urlparam String name Das Namensmuster
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void renameShips2Action() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		
		parameterString("name");
		final String name = getString("name");
		
		List<NamePatternElement> nameParts = parseNamePattern(name);
		
		List ships = db.createQuery("from Ship where id>0 and fleet=?" )
			.setEntity(0, this.fleet)
			.list();
		
		for( Iterator iter=ships.iterator(); iter.hasNext(); ) {
			Ship ship = (Ship)iter.next();
			
			ship.setName(generateNextShipName(nameParts));
		}
		
		t.setVar(	"fleetmgnt.message",	"Die Namen wurden ge&auml;ndert",
					"jscript.reloadmain",	1 );
		
		this.redirect();
	}
	
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();

		List<String> sectors = new ArrayList<String>();
	
		Ship aship = (Ship)db.createQuery("from Ship where id>0 and fleet=?")
			.setEntity(0, this.fleet)
			.setMaxResults(1)
			.uniqueResult();
		
		sectors.add("(s.x="+aship.getX()+" and s.y="+aship.getY()+" and s.system="+aship.getSystem()+")");

		t.setVar(	"show.view",	1,
					"fleet.name",	Common._plaintitle(this.fleet.getName()),
					"fleet.id",		this.fleet.getId() );
								
		t.setBlock("_FLEETMGNT", "ships.listitem", "ships.list");
	
		Location aloc = aship.getLocation();
		
		List ships = db.createQuery("from Ship where id>0 and owner=? and fleet=? order by id")
			.setEntity(0, user)
			.setEntity(1, this.fleet)
			.list();
		for( Iterator iter=ships.iterator(); iter.hasNext(); ) {
			Ship ship = (Ship)iter.next();
			
			ShipTypeData shiptype = ship.getTypeData();
			Location loc = ship.getLocation();
			
			t.setVar(	"ship.id",			ship.getId(),
						"ship.name",		Common._plaintitle(ship.getName()),
						"ship.type.name",	shiptype.getNickname(),
						"ship.showbattle",	ship.getBattle() != null ? ship.getBattle() : 0,
						"ship.showwarning",	!aloc.sameSector(0, loc, 0) );
			
			String sectorStr = "(s.x="+ship.getX()+" and s.y="+ship.getY()+" and s.system="+ship.getSystem()+")";
			if( !sectors.contains(sectorStr) ) {
				sectors.add(sectorStr);
			}
			
			t.parse("ships.list", "ships.listitem", true);
		}	
	
		// Jaegerliste bauen
		String sectorstring = Common.implode(" or ", sectors);
		
		t.setBlock("_FLEETMGNT", "jaegertypes.listitem", "jaegertypes.list");
		
		List shiptypes = db.createQuery("select s.shiptype,count(*) " +
				"from Ship as s " +
				"where s.owner=? and locate(?,s.shiptype.flags)!=0 and s.docked='' and ("+sectorstring+") " +
				"group by s.shiptype")
			.setEntity(0, user)
			.setString(1, ShipTypes.SF_JAEGER)
			.list();
		for( Iterator iter=shiptypes.iterator(); iter.hasNext(); ) {
			ShipType shiptype = (ShipType)((Object[])iter.next())[0];
			
			t.setVar(	"jaegertype.id",	shiptype.getId(),
						"jaegertype.name",	shiptype.getNickname() );
								
			t.parse("jaegertypes.list", "jaegertypes.listitem", true);
		}
	
		// Flottenliste bauen
		t.setBlock("_FLEETMGNT", "fleetcombine.listitem", "fleetcombine.list");
		List fleetList = db.createQuery("select distinct s.fleet from Ship as s " +
				"where s.system=? and s.x=? and s.y=? AND docked='' AND owner=? AND s.fleet!=?")
				.setInteger(0, aship.getSystem())
				.setInteger(1, aship.getX())
				.setInteger(2, aship.getY())
				.setEntity(3, user)
				.setEntity(4, this.fleet)
				.list();
		
		for( Iterator iter=fleetList.iterator(); iter.hasNext(); ) {
			ShipFleet afleet = (ShipFleet)iter.next();
			
			long count = (Long)db.createQuery("select count(*) from Ship where fleet=?")
				.setEntity(0, this.fleet)
				.iterate().next();
			
			t.setVar(	"fleetcombine.id",			afleet.getId(),
						"fleetcombine.name",		Common._plaintitle(afleet.getName()),
						"fleetcombine.shipcount",	count );
								
			t.parse("fleetcombine.list", "fleetcombine.listitem", true);
		}
	}
}
