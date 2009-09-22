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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Ally;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Zeigt die LRS-Scanner an.
 * @author Christopher Jung
 * 
 * @urlparam Integer ship Die ID des anzuzeigenden Schiffes
 * @urlparam Integer admin Falls != 0 und AccessLevel >= 30 kann via range und base* die Position und Scanreichweite eingestellt werden
 * @urlparam Integer range (Falls Admin-Modus) Die Scan-Reichweite
 * @urlparam Integer baseloc (Falls Admin-Modus) Die Koordinate, von der aus gescannt werden soll
 *
 */
public class ScanController extends TemplateGenerator {
	private Ship ship = null;
	private int range = 0;
	private boolean admin = false;
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public ScanController(Context context) {
		super(context);

		parameterNumber("ship");
		parameterNumber("admin");
		parameterNumber("range");
		parameterString("baseloc");
		
		setTemplate("scan.html");
		
		setPageTitle("LRS");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		org.hibernate.Session db = getDB();
		admin = getInteger("admin") != 0 && getUser().getAccessLevel() >= 30;
		int shipID = -1;
		
		if( !admin ) {
			shipID = getInteger("ship");
			
			Ship ship = (Ship)db.get(Ship.class, shipID);
	
			if( (ship == null) || (ship.getOwner().getId() != getUser().getId()) || (ship.getId() < 0) ) {
				addError("Das angegebene Schiff existiert nicht oder geh&ouml;rt ihnen nicht", Common.buildUrl("default", "module", "schiffe") );
				
				return false;
			}
			
			ShipTypeData shiptype = ship.getTypeData();
			
			if( shiptype.getScanCost() > ship.getEnergy()) {
				addError("Nicht genug Energie vorhanden zum Scannen.");
				return false;
			}
			
			int range = shiptype.getSensorRange();
	
			// Sollte das Schiff in einem Nebel stehen -> halbe Scannerreichweite
			Nebel nebel = (Nebel)db.get(Nebel.class, new MutableLocation(ship));
			if( nebel != null ) {
				switch( nebel.getType() ) {
				// Norm. Deut, DMG
				case 0:
				case 6: 
					range = (int)Math.round(range/2d);
					break;
					
				// L. Deut
				case 1:
					range = (int)Math.round(range*0.75d);
					break;
					
				// H. Deut
				case 2:
					range = (int)Math.round(range/3d);
					break;
					
				default:
					addError("Der Nebel verhindert den Einsatz von Langstreckensensoren", Common.buildUrl("default", "module", "schiff", "ship", shipID));
				
					return false;
				}
			}
	
			if( ship.getCrew() < shiptype.getCrew()/3 ) {
				addError("Es werden mindestens "+shiptype.getCrew()/3+" Crewmitglieder ben&ouml;tigt", Common.buildUrl("default", "module", "schiff", "ship", shipID));
				
				return false;
			}
			
			range = (int)Math.round(range*(ship.getSensors()/100d));
			
			this.range = range;
			this.ship = ship;
		}
		else {
			this.range = getInteger("range");
			Location loc = Location.fromString(getString("baseloc"));
			
			// Schiffstyp 1 ist zufaellig gewaehlt - wichtig ist nur, dass ein in der 
			// DB vorhandener Typ hier verwendet wird (sonst gibts Exceptions).
			ShipType type = (ShipType)db.get(ShipType.class, 1);
			this.ship = new Ship((User)getUser(), type, loc.getSystem(), loc.getX(), loc.getY());
			this.ship.setEnergy(type.getPickingCost()*10);

			this.getTemplateEngine().setVar(	
					"global.admin",	1,
					"global.baseloc", loc.toString(),
					"global.baserange", range);
		}
		
		this.getTemplateEngine().setVar(	"global.ship.id",	shipID,
											"global.range",		this.range+1,
											"global.scan.x",	ship.getX(),
											"global.scan.y",	ship.getY() );
		
		return true;	
	}
	
	/**
	 * Zeigt den Inhalt eines Sektors innerhalb der LRS-Reichweite an.
	 * @urlparam Integer scanx Die X-Koordinate des Sektors
	 * @urlparam Integer scany Die Y-Koordinate des Sektors
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void scanAction() {
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		
		if(ship.getTypeData().getPickingCost() > ship.getEnergy())
		{
			return;
		}
		
		ship.setEnergy(ship.getEnergy() - ship.getTypeData().getPickingCost());
		
		parameterNumber("scanx");
		parameterNumber("scany");
		
		int scanx = getInteger("scanx");
		int scany = getInteger("scany");
		
		final int system = this.ship.getSystem();
		
		t.setVar("global.scansector",1);
		
		StarSystem thissystem = (StarSystem)db.get(StarSystem.class, system);
		
		if( (scany < 1) || (scany > thissystem.getHeight()) ) {
			return;
		}
		
		if( (scanx < 1) || (scanx > thissystem.getWidth()) ) {
			return;
		}
		
		if( Math.round(Math.sqrt(Math.pow(scany-this.ship.getY(),2)+Math.pow(scanx-this.ship.getX(),2))) > this.range ) {
			return;
		}
		
		final Location scanLoc = new Location(system, scanx, scany);
		
		t.setVar("sector.isscanable",1);
		
		boolean scanableNebel = false;
	
		Nebel nebel = (Nebel)db.get(Nebel.class, new MutableLocation(scanLoc));
		if( !this.admin && (nebel != null) && ((nebel.getType() < 3) || (nebel.getType() > 5)) ) 
		{
			// Wenn kein EMP-Nebel, dann kann man ihn scannen
			scanableNebel = true;
		}
		// Im Admin-Modus sind alle Nebel scanbar
		else if( this.admin ) {
			scanableNebel = true;
		}
	
		/*
			Nebel
		*/

		if( (nebel != null) && !scanableNebel ) {
			t.setVar(	"sector.nebel",			1,
						"sector.nebel.type",	nebel.getType() );
		}
		else {
			if( nebel != null ) {
				t.setVar(	"sector.nebel",			1,
							"sector.nebel.type",	nebel.getType() );
			}
			/*
				Basen
			*/
			t.setBlock("_SCAN", "bases.listitem", "bases.list");
			
			List<?> bases = db.createQuery("from Base b inner join fetch b.owner " +
					"where b.system= :sys and floor(sqrt(pow( :x - b.x,2)+pow( :y - b.y,2))) <= b.size " +
					"order by b.id")
				.setInteger("x", scanLoc.getX())
				.setInteger("y", scanLoc.getY())
				.setInteger("sys", scanLoc.getSystem())
				.list();
			for( Iterator<?> iter=bases.iterator(); iter.hasNext(); ) {
				Base base = (Base)iter.next();
				
				t.start_record();
				
				t.setVar(	"base.id",			base.getId(),
							"base.isown",		(base.getOwner().getId() == user.getId()),
							"base.owner.id",	base.getOwner().getId(),
							"base.owner.name",	Common._title(base.getOwner().getName()),
							"base.name",		Common._plaintitle(base.getName()),
							"base.size",		base.getSize(),
							"base.klasse",		base.getKlasse() );
				
				if( (base.getOwner().getId() != 0) && (base.getOwner().getId() != user.getId()) ) {
					t.setVar("base.owner.link", 1);
				}
				
				t.parse("bases.list", "bases.listitem", true);
				t.stop_record();
				t.clear_record();
			}
	
			/*
				Jumpnodes
			*/	
			JumpNode node = (JumpNode)db.createQuery("from JumpNode where x= :x and y= :y and system= :sys")
				.setInteger("x", scanLoc.getX())
				.setInteger("y", scanLoc.getY())
				.setInteger("sys", scanLoc.getSystem())
				.setMaxResults(1)
				.uniqueResult();
			if( node != null ) {
				String blocked = "";
				if( node.isGcpColonistBlock() && Rassen.get().rasse(user.getRace()).isMemberIn( 0 ) ) {
					blocked = "<br />-Blockiert-";
				}
				
				t.setVar(	"sector.jumpnode",	1,
							"jumpnode.id",		node.getId(),
							"jumpnode.name",	node.getName(),
							"jumpnode.blocked",	blocked );
			}
	
			/*
				Schlachten
			*/
			t.setBlock("_SCAN", "battles.listitem", "battles.list");
			List<?> battleList = db.createQuery("from Battle where x= :x and y= :y and system= :sys")
				.setInteger("x", scanLoc.getX())
				.setInteger("y", scanLoc.getY())
				.setInteger("sys", scanLoc.getSystem())
				.list();
			for( Iterator<?> iter=battleList.iterator(); iter.hasNext(); ) {
				Battle battle = (Battle)iter.next();
				
				boolean questbattle = false;
				
				if( !battle.isVisibleToUser(user) ) {
					questbattle = true;
				}

				String party1 = "";
				if( battle.getAlly(0) == 0 ) {
					final User commander1 = battle.getCommander(0);
					party1 = Common._title(commander1.getName());
				} 
				else {
					Ally ally = (Ally)db.get(Ally.class, battle.getAlly(0));
					party1 = Common._title(ally.getName());
				}
	
				String party2 = "";
				if( battle.getAlly(1) == 0 ) {
					final User commander2 = battle.getCommander(1);
					party2 = Common._title(commander2.getName());
				} 
				else {
					Ally ally = (Ally)db.get(Ally.class, battle.getAlly(1));
					party2 = Common._title(ally.getName());
				}
					
				long shipcount = (Long)db.createQuery("select count(*) from Ship where id>0 and battle= :battle")
					.setEntity("battle", battle)
					.iterate().next();
				
				t.setVar(	"battle.id",			battle.getId(),
							"battle.isquestbattle",	questbattle,
							"battle.party1",		party1,
							"battle.party2",		party2,
							"battle.shipcount",		shipcount ); 
				
				t.parse("battles.list", "battles.listitem", true);
			}

			/*
				Subraumspalten (durch Sprungantriebe)
			*/

			long jumps = (Long)db.createQuery("select count(*) from Jump where x= :x and y= :y and system= :sys")
				.setInteger("x", scanLoc.getX())
				.setInteger("y", scanLoc.getY())
				.setInteger("sys", scanLoc.getSystem())
				.iterate().next();
			if( jumps > 0 ) {
				t.setVar("sector.subraumspalten", jumps);
			}

			/*
				Schiffe
			*/
		
			t.setBlock("_SCAN", "ships.listitem", "ships.list");
		
			List<Integer> verysmallshiptypes = new ArrayList<Integer>();
			verysmallshiptypes.add(0); // Ein dummy-Wert, damit es keine SQL-Fehler gibt
			
			// Falls nicht im Admin-Modus und nicht das aktuelle Feld gescannt wird: Liste der kleinen Schiffe generieren
			if( !this.admin && (scanx != this.ship.getX()) || (scany != this.ship.getY()) ) {
				final Iterator<?> typeIter = db.createQuery("from ShipType where locate(:flag,flags)!=0")
					.setString("flag", ShipTypes.SF_SEHR_KLEIN)
					.iterate();
				while( typeIter.hasNext() ) {
					ShipType type = (ShipType)typeIter.next();
					verysmallshiptypes.add(type.getId());
				}
			}
			
			List<?> shiplist = db.createQuery("from Ship s inner join fetch s.owner " +
					"where s.id>0 and s.x= :x and s.y= :y and s.system= :sys and s.battle is null and " +
						"((s.shiptype not in ("+Common.implode(",",verysmallshiptypes)+")) or s.modules is not null) and " +
						"(s.visibility is null or s.visibility= :user) and locate('l ',s.docked)=0 " +
						"order by s.id")
					.setInteger("x", scanLoc.getX())
					.setInteger("y", scanLoc.getY())
					.setInteger("sys", scanLoc.getSystem())
					.setInteger("user", user.getId())
					.list();
						
			for( Iterator<?> iter=shiplist.iterator(); iter.hasNext(); ) {
				Ship ship = (Ship)iter.next();
				
				boolean disableIFF = ship.getStatus().contains("disable_iff");
				ShipTypeData shiptype = ship.getTypeData();
				
				// Falls nicht im Admin-Modus: Nur sehr kleine Schiffe im Feld des scannenden Schiffes anzeigen
				if( !this.admin && ((scanx != this.ship.getX()) || (scany != this.ship.getY())) &&
					shiptype.hasFlag(ShipTypes.SF_SEHR_KLEIN) ) {
					continue;	
				}
				
				boolean scanable = false;
				if(nebel != null){
					int nebeltype = nebel.getType();
					if( nebeltype == 1 && shiptype.getSize() > 4 ) // leichter Deutnebel
					{
						scanable = true;
					}
					else if( nebeltype == 0 && shiptype.getSize() > 6 ) // mittlerer Deutnebel
					{
						scanable = true;
					}
					else if( nebeltype == 2 && shiptype.getSize() > 10 ) // schwerer Deutnebel
					{
						scanable = true;
					}
					else if( nebeltype == 6 && shiptype.getSize() > 8 ) // Schadensnebel
					{
						scanable = true;
					}
					else if (ship.getOwner().getId() == user.getId())
					{
						scanable = true;
					}
				}
				else
				// kein nebel
				{
					scanable = true;
				}
				
				if (scanable){
					t.setVar(	"ship.id",				ship.getId(),
								"ship.isown",			(ship.getOwner().getId() == user.getId()),
								"ship.owner.id",		ship.getOwner().getId(),
								"ship.name",			Common._plaintitle(ship.getName()),
								"ship.owner.name",		Common._title(ship.getOwner().getName()),
								"ship.ownerlink",		(ship.getOwner().getId() != user.getId()),
								"ship.battle",			ship.getBattle() != null ? ship.getBattle().getId() : 0,
								"ship.type.name",		shiptype.getNickname(),
								"ship.type",			ship.getType(),
								"ship.type.picture",	shiptype.getPicture() );
				}
	
				if( disableIFF ) {
					t.setVar(	"ship.owner.name",	"Unbekannt",
								"ship.ownerlink",	0 );
				}
				if (scanable){
				t.parse("ships.list", "ships.listitem", true);
				}
			}
		}
	}

	private static class BaseEntry {
		BaseEntry() {
			// EMPTY
		}
		Base base;
		int imgcount;
	}

	/**
	 * Zeigt die LRS-Karte an.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		ship.setEnergy(ship.getEnergy() - ship.getTypeData().getScanCost());
		
		/*
			Alle Objekte zusammensuchen, die fuer uns in Frage kommen
		*/
		
		// Nebel
		Map<Location,Integer> nebelmap = new HashMap<Location,Integer>();

		List<?> nebelList = db.createQuery("from Nebel where system=:system and " +
				"x between :xmin and :xmax and " +
				"y between :ymin and :ymax")
			.setInteger("system", this.ship.getSystem())
			.setInteger("xmin", this.ship.getX()-this.range)
			.setInteger("xmax", this.ship.getX()+this.range)
			.setInteger("ymin", this.ship.getY()-this.range)
			.setInteger("ymax", this.ship.getY()+this.range)
			.list();
		for( Iterator<?> iter=nebelList.iterator(); iter.hasNext(); ) {
			Nebel nebel = (Nebel)iter.next();
			nebelmap.put(nebel.getLocation(), nebel.getType());
		}
		
		// Jumpnodes
		Map<Location,Boolean> nodemap = new HashMap<Location,Boolean>();

		List<?> nodeList = db.createQuery("from JumpNode where  system=:system and " +
				"x between :xmin and :xmax and " +
				"y between :ymin and :ymax")
			.setInteger("system", this.ship.getSystem())
			.setInteger("xmin", this.ship.getX()-this.range)
			.setInteger("xmax", this.ship.getX()+this.range)
			.setInteger("ymin", this.ship.getY()-this.range)
			.setInteger("ymax", this.ship.getY()+this.range)
			.list();
		for( Iterator<?> iter=nodeList.iterator(); iter.hasNext(); ) {
			JumpNode node = (JumpNode)iter.next();
			nodemap.put(node.getLocation(), true);
		}

		// Schiffe
		List<Integer> verysmallshiptypes = new ArrayList<Integer>();
		verysmallshiptypes.add(0); // Ein dummy-Wert, damit es keine SQL-Fehler gibt
		
		// Im Admin-Modus sind alle Schiffe sichtbar
		if( !this.admin ) {
			final Iterator<?> typeIter = db.createQuery("from ShipType where locate(:flag,flags)!=0")
				.setString("flag", ShipTypes.SF_SEHR_KLEIN)
				.iterate();
			while( typeIter.hasNext() ) {
				ShipType type = (ShipType)typeIter.next();
				verysmallshiptypes.add(type.getId());
			}
		}
		
		Map<Location,List<Ship>> shipmap = new HashMap<Location,List<Ship>>();
		Map<Location,Boolean> ownshipmap = new HashMap<Location,Boolean>();

		List<?> shipList = db.createQuery("from Ship s inner join fetch s.owner " +
				"where s.id>0 and system=:system and " +
				"x between :xmin and :xmax and " +
				"y between :ymin and :ymax and (s.visibility is null or s.visibility= :user ) and " +
				"((s.shiptype not in ("+Common.implode(",",verysmallshiptypes)+")) or s.modules is not null)")
			.setInteger("user", user.getId())
			.setInteger("system", this.ship.getSystem())
			.setInteger("xmin", this.ship.getX()-this.range)
			.setInteger("xmax", this.ship.getX()+this.range)
			.setInteger("ymin", this.ship.getY()-this.range)
			.setInteger("ymax", this.ship.getY()+this.range)
			.list();
		for( Iterator<?> iter=shipList.iterator(); iter.hasNext(); ) {
			Ship ship = (Ship)iter.next();
			
			ShipTypeData st = ship.getTypeData();
			
			// Im Admin-Modus sind alle Schiffe sichtbar
			if( !this.admin && st.hasFlag(ShipTypes.SF_SEHR_KLEIN) ) {
				continue;	
			}
			
			Location loc = ship.getLocation();
			if( !shipmap.containsKey(loc) ) {
				shipmap.put(loc, new ArrayList<Ship>());
			}
			shipmap.get(loc).add(ship);
				
			if( (ship.getOwner().getId() == user.getId()) && (ship.getSensors()>30) && !ownshipmap.containsKey(loc) ) {			
				if( ship.getCrew() >= st.getCrew()/4 ) {
					ownshipmap.put(loc, true);
				}
			}
		}
		
		// Basen
		Map<Location,BaseEntry> basemap = new HashMap<Location,BaseEntry>();

		List<?> baseList = db.createQuery("from Base b inner join fetch b.owner " +
				"where b.system= :sys and " +
					"(floor(sqrt(pow(b.x - :x,2)+pow(b.y - :y,2))) <= :range+b.size) " +
				"order by b.size")
			.setInteger("sys", this.ship.getSystem())
			.setInteger("x", this.ship.getX())
			.setInteger("y", this.ship.getY())
			.setInteger("range", this.range)
			.list();
						
		for( Iterator<?> iter=baseList.iterator(); iter.hasNext(); ) {
			Base base = (Base)iter.next();
			
			int imgcount = 0;
			Location centerLoc = base.getLocation();
			for( int by=base.getY()-base.getSize(); by <= base.getY()+base.getSize(); by++ ) {
				for( int bx=base.getX()-base.getSize(); bx <= base.getX()+base.getSize(); bx++ ) {
					Location loc = new Location(this.ship.getSystem(), bx, by);
					
					if( !centerLoc.sameSector( 0, loc, base.getSize() ) ) {
						continue;	
					}
					BaseEntry entry = new BaseEntry();
					entry.base = base;
					if( base.getSize() > 0 ) {
						entry.imgcount = imgcount;
						imgcount++;
					}
					basemap.put(loc, entry);
				}
			}
		}
		
		// Obere/Untere Koordinatenreihe
		
		t.setBlock("_SCAN", "mapborder.listitem", "mapborder.list");
		
		StarSystem system = (StarSystem)db.get(StarSystem.class, this.ship.getSystem());
		
		for( int x = this.ship.getX()-this.range; x <= this.ship.getX()+this.range; x++ ) {
			if( (x > 0) && (x <= system.getWidth()) ) {
				t.setVar("mapborder.x", x);
				t.parse("mapborder.list", "mapborder.listitem", true);
			}
		}
		
		/*
			Ausgabe der Karte
		*/
		
		t.setBlock("_SCAN", "map.rowitem", "map.rowlist");
		t.setBlock("map.rowitem", "map.listitem", "map.list");
		
		for( int y = this.ship.getY()-this.range; y <= this.ship.getY()+this.range; y++ ) {
			if( (y < 1) || (y > system.getHeight()) ) {
				continue;
			}
			
			t.setVar(	"map.border.y",	y,
						"map.list",		"" );
	
			// Einen einzelnen Sektor ausgeben
			for( int x = this.ship.getX()-this.range; x <= this.ship.getX()+this.range; x++ ) {
				if( (x < 1) || (x > system.getWidth()) ) {
					continue;
				}
				Location loc = new Location(this.ship.getSystem(), x, y);
				
				t.start_record();
	
				String cssClass = "";
				
				if( (x != this.ship.getX()) || (y != this.ship.getY()) ) {
					cssClass = "class=\"starmap\"";
				}
				
				if( Math.round(Math.sqrt(Math.pow(y-this.ship.getY(),2)+Math.pow(x-this.ship.getX(),2))) <= this.range ) {				
					t.setVar(	"map.x",			x,
								"map.y",			y,
								"map.linkclass",	cssClass,
								"map.showsector",	1 );
	
					// Nebel
					if (nebelmap.containsKey(loc) && ((nebelmap.get(loc) >=3) && (nebelmap.get(loc) <= 5)))
					{
						t.setVar(	"map.image",		"fog"+nebelmap.get(loc)+"/fog"+nebelmap.get(loc),
								"map.image.name",	"Nebel" );
					}
					else {
						int own = 0;
						int enemy = 0;
						int ally = 0;
						
						// Schiffe
						String[] fleet = new String[] {"", "", ""};
						if( shipmap.containsKey(loc) ) {
							List<Ship> myships = shipmap.get(loc);
							for( int i=0; i < myships.size(); i++ ) {
								Ship myship = myships.get(i);
								
								if( myship.getOwner().getId() == user.getId() ) {
									if( (myship.getDocked().length() == 0) || (myship.getDocked().charAt(0) != 'l') ) {
										if( own == 0 ) {
											fleet[0] = "_fo";
										}	
										own++;
									}
								}
								else if( (myship.getOwner().getId() != user.getId()) && (user.getAlly() != null) && (myship.getOwner().getAlly() == user.getAlly()) )
								{
									if( (myship.getDocked().length() == 0) || (myship.getDocked().charAt(0) != 'l') )
									{
										if( ally == 0 )
										{
											fleet[1] = "_fa";
										}	
										ally++;
									}
								}
								else if( (myship.getOwner().getId() != user.getId()) && ( (user.getAlly() == null) || ((user.getAlly() != null) && (myship.getOwner().getAlly() != user.getAlly()) ) )  )
								{
									boolean scan = false;
									if (nebelmap.containsKey(loc))
									{
										if (nebelmap.get(loc) == 1 && myship.getTypeData().getSize() > 4) // leichter Deutnebel
										{
											scan = true;
										}
										else if (nebelmap.get(loc) == 0 && myship.getTypeData().getSize() > 6) // mittlerer Deutnebel
										{
											scan = true;
										}
										else if (nebelmap.get(loc) == 2 && myship.getTypeData().getSize() > 10) // schwerer Deutnebel
										{
											scan = true;
										}
										else if (nebelmap.get(loc) == 6 && myship.getTypeData().getSize() > 8) // Schadensnebel
										{
											scan = true;
										}
									}
									else
									{
										scan = true;
									}
									
									if( ((myship.getDocked().length() == 0) || (myship.getDocked().charAt(0) != 'l')) && scan == true )
									{
										if( enemy == 0 ) {
											fleet[2] = "_fe";
										}	
										enemy++;
									}
								}
							}
						}
	
						String tooltip = "";
						String fleetStr = "";
						if( own+ally+enemy > 0 )
						{
							tooltip = "onmouseover=\"return overlib('<span class=\\'smallfont\\'>"+(own != 0 ? "Eigene: "+own+"<br />":"")+(ally != 0 ? "Ally: "+ally+"<br />":"")+(enemy != 0 ? "Feindliche: "+enemy+"<br />":"")+"</span>',TIMEOUT,0,DELAY,400,WIDTH,120);\" onmouseout=\"return nd();\"";
							fleetStr = Common.implode("", fleet);
						}
					
						t.setVar(	"map.tooltip",	tooltip,
									"map.fleet",	fleetStr );
					
						// Nebel, Basen, Sprungpunkte
						if( nebelmap.containsKey(loc) )
						{
							t.setVar(	"map.image",		"fog"+nebelmap.get(loc)+"/fog"+nebelmap.get(loc),
										"map.image.name",	"Nebel" );
						}
						else if( basemap.containsKey(loc) )
						{
							BaseEntry entry = basemap.get(loc);
							if( entry.base.getSize() > 0 )
							{
								t.setVar(	"map.image",		"kolonie"+entry.base.getKlasse()+"_lrs/kolonie"+entry.base.getKlasse()+"_lrs"+entry.imgcount,
											"map.image.name",	"Asteroid" );
							}
							else if( entry.base.getOwner().getId() == user.getId() )
							{
								t.setVar(	"map.image",		"asti_own/asti_own",
											"map.image.name",	"Eigener Asteroid" );
							}
							else if( (entry.base.getOwner().getId() != 0) && (user.getAlly() != null) && (entry.base.getOwner().getAlly() == user.getAlly()) )
							{
								t.setVar(	"map.image",		"asti_ally/asti_ally",
											"map.image.name",	"Ally Asteroid" );
							}
							else if( (entry.base.getOwner().getId() != 0) )
							{
								t.setVar(	"map.image",		"asti_enemy/asti_enemy",
											"map.image.name",	"Feindlicher Asteroid" );
							}
							else
							{
								String astiimg = "kolonie"+entry.base.getKlasse()+"_lrs";
								
								t.setVar(	"map.image",		astiimg+"/"+astiimg,
											"map.image.name",	"Asteroid" );
							}
						}
						else if( nodemap.containsKey(loc) )
						{
							t.setVar(	"map.image",		"jumpnode/jumpnode",
										"map.image.name",	"Jumpnode" );
						}
						else
						{
							t.setVar(	"map.image",		"space/space",
										"map.image.name",	"Leer" );
						}
					}
				}
				t.parse("map.list", "map.listitem", true);
				
				t.stop_record();
				t.clear_record();
			}
			
			t.parse("map.rowlist", "map.rowitem", true);
		}
	}
}
