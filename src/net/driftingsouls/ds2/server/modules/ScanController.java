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
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.config.Rassen;
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

/**
 * Zeigt die LRS-Scanner an
 * @author Christopher Jung
 * 
 * @urlparam Integer ship Die ID des anzuzeigenden Schiffes
 * @urlparam Integer admin Falls != 0 und AccessLevel >= 30 kann via range und base* die Position und Scanreichweite eingestellt werden
 * @urlparam Integer range (Falls Admin-Modus) Die Scan-Reichweite
 * @urlparam Integer baseloc (Falls Admin-Modus) Die Koordinate, von der aus gescannt werden soll
 *
 */
public class ScanController extends TemplateGenerator {
	private SQLResultRow ship = null;
	private int range = 0;
	private boolean admin = false;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public ScanController(Context context) {
		super(context);

		parameterNumber("ship");
		parameterNumber("admin");
		parameterNumber("range");
		parameterString("baseloc");
		
		setTemplate("scan.html");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		Database db = getDatabase();
		admin = getInteger("admin") != 0 && getUser().getAccessLevel() >= 30;
		int shipID = -1;
		
		if( !admin ) {
			shipID = getInteger("ship");
			
			SQLResultRow ship = db.first("SELECT id,x,y,system,sensors,crew,type,status FROM ships WHERE owner='",getUser().getId(),"' AND id>0 AND id=",shipID);
	
			if( ship.isEmpty() ) {
				addError("Das angegebene Schiff existiert nicht oder geh&ouml;rt ihnen nicht", Common.buildUrl("default", "module", "schiffe") );
				
				return false;
			}
			
			SQLResultRow shiptype = ShipTypes.getShipType( ship );
			int range = shiptype.getInt("sensorrange");
	
			// Sollte das Schiff in einem Nebel stehen -> halbe Scannerreichweite
			SQLResultRow nebel = db.first("SELECT * FROM nebel WHERE x=",ship.getInt("x")," AND y=",ship.getInt("y")," AND system=",ship.getInt("system"));
			if( !nebel.isEmpty() ) {
				switch( nebel.getInt("type") ) {
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
	
			if( ship.getInt("crew") < shiptype.getInt("crew")/3 ) {
				addError("Es werden mindestens "+shiptype.getInt("crew")/3+" Crewmitglieder ben&ouml;tigt", Common.buildUrl("default", "module", "schiff", "ship", shipID));
				
				return false;
			}
			
			range = (int)Math.round(range*(ship.getInt("sensors")/100d));
			
			this.range = range;
			this.ship = ship;
		}
		else {
			this.range = getInteger("range");
			Location loc = Location.fromString(getString("baseloc"));
			this.ship = new SQLResultRow();
			this.ship.put("x", loc.getX());
			this.ship.put("y", loc.getY());
			this.ship.put("system", loc.getSystem());
			
			this.getTemplateEngine().setVar(	
					"global.admin",	1,
					"global.baseloc", loc.toString(),
					"global.baserange", range);
		}
		
		this.getTemplateEngine().setVar(	"global.ship.id",	shipID,
											"global.range",		this.range+1,
											"global.scan.x",	ship.getInt("x"),
											"global.scan.y",	ship.getInt("y") );
		
		return true;	
	}
	
	/**
	 * Zeigt den Inhalt eines Sektors innerhalb der LRS-Reichweite an
	 * @urlparam Integer scanx Die X-Koordinate des Sektors
	 * @urlparam Integer scany Die Y-Koordinate des Sektors
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void scanAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = (User)getUser();
		
		parameterNumber("scanx");
		parameterNumber("scany");
		
		int scanx = getInteger("scanx");
		int scany = getInteger("scany");
		
		final int system = this.ship.getInt("system");
		
		t.setVar("global.scansector",1);
		
		if( (scany < 1) || (scany > Systems.get().system(system).getHeight()) ) {
			return;
		}
		
		if( (scanx < 1) || (scanx > Systems.get().system(system).getWidth()) ) {
			return;
		}
		
		if( Math.round(Math.sqrt(Math.pow(scany-this.ship.getInt("y"),2)+Math.pow(scanx-this.ship.getInt("x"),2))) > this.range ) {
			return;
		}
		
		t.setVar("sector.isscanable",1);
		
		boolean scanableNebel = false;
	
		SQLResultRow nebel = db.first("SELECT * FROM nebel WHERE x=",scanx," AND y=",scany," AND system="+system);
		if( !this.admin && !nebel.isEmpty() && ((nebel.getInt("type") < 3) || (nebel.getInt("type") > 5)) ) {
			SQLQuery nebelship = db.query("SELECT id,status,type,sensors,crew FROM ships WHERE id>0 AND x=",scanx," AND y=",scany," AND system=",system," AND owner=",user.getId()," AND sensors > 30");
			while( nebelship.next() ) {
				SQLResultRow ownshiptype = ShipTypes.getShipType( nebelship.getRow() );	
				if( nebelship.getInt("crew") >= ownshiptype.getInt("crew")/4 ) {
					scanableNebel = true;
					break;
				}
			}
			nebelship.free();
		}
		// Im Admin-Modus sind alle Nebel scanbar
		else if( this.admin ) {
			scanableNebel = true;
		}
	
		/*
			Nebel
		*/

		if( !nebel.isEmpty() && !scanableNebel ) {
			t.setVar(	"sector.nebel",			1,
						"sector.nebel.id",		"Nebel",
						"sector.nebel.type",	nebel.getInt("type") );
		}
		else {
			if( !nebel.isEmpty() ) {
				t.setVar(	"sector.nebel",			1,
							"sector.nebel.id",		"Nebel",
							"sector.nebel.type",	nebel.getInt("type") );
			}
			/*
				Basen
			*/
			t.setBlock("_SCAN", "bases.listitem", "bases.list");
			
			SQLQuery datan = db.query("SELECT b.id,b.owner,b.klasse,b.name,b.size,u.name username " +
					"FROM bases b JOIN users u ON b.owner=u.id " +
					"WHERE b.system=",system," AND FLOOR(SQRT(POW(",scanx,"-b.x,2)+POW(",scany,"-b.y,2))) <= b.size " +
					"ORDER BY b.id");
			while( datan.next() ) {
				t.start_record();
				
				t.setVar(	"base.id",			datan.getInt("id"),
							"base.isown",		(datan.getInt("owner") == user.getId()),
							"base.owner.id",	datan.getInt("owner"),
							"base.owner.name",	Common._title(datan.getString("username")),
							"base.name",		Common._plaintitle(datan.getString("name")),
							"base.size",		datan.getInt("size"),
							"base.klasse",		datan.getInt("klasse") );
				
				if( (datan.getInt("owner") != 0) && (datan.getInt("owner") != user.getId()) ) {
					t.setVar("base.owner.link", 1);
				}
				
				t.parse("bases.list", "bases.listitem", true);
				t.stop_record();
				t.clear_record();
			}
			datan.free();
	
			/*
				Jumpnodes
			*/	
			SQLResultRow node = db.first("SELECT * FROM jumpnodes WHERE x=",scanx," AND y=",scany," AND system=",system);
			if( !node.isEmpty() ) {
				String blocked = "";
				if( node.getBoolean("gcpcolonistblock") && Rassen.get().rasse(user.getRace()).isMemberIn( 0 ) ) {
					blocked = "<br />-Blockiert-";
				}
				
				t.setVar(	"sector.jumpnode",	1,
							"jumpnode.id",		node.getInt("id"),
							"jumpnode.name",	node.getString("name"),
							"jumpnode.blocked",	blocked );
			}
	
			/*
				Schlachten
			*/
			t.setBlock("_SCAN", "battles.listitem", "battles.list");
			
			SQLQuery battle = db.query("SELECT * FROM battles WHERE x=",scanx," AND y=",scany," AND system=",system);
			while( battle.next() ) {
				boolean questbattle = false;
				
				if( (battle.getString("visibility") != null) && (battle.getString("visibility").length() != 0) ) {
					Integer[] visibility = Common.explodeToInteger(",",battle.getString("visibility"));
					if( !Common.inArray(user.getId(),visibility) ) {
						questbattle = true;
					}
				}

				String party1 = "";
				if( battle.getInt("ally1") == 0 ) {
					User com1 = (User)getDB().get(User.class, battle.getInt("commander1"));
					party1 = Common._title(com1.getName());
				} 
				else {
					party1 = Common._title(db.first("SELECT name FROM ally WHERE id=",battle.getInt("ally1")).getString("name"));
				}
	
				String party2 = "";
				if( battle.getInt("ally2") == 0 ) {
					User com2 = (User)getDB().get(User.class, battle.getInt("commander2"));
					party2 = Common._title(com2.getName());
				} 
				else {
					party2 = Common._title(db.first("SELECT name FROM ally WHERE id=",battle.getInt("ally2")).getString("name"));
				}
					
				int shipcount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND battle=",battle.getInt("id")).getInt("count");
				
				t.setVar(	"battle.id",			battle.getInt("id"),
							"battle.isquestbattle",	questbattle,
							"battle.party1",		party1,
							"battle.party2",		party2,
							"battle.shipcount",		shipcount ); 
				
				t.parse("battles.list", "battles.listitem", true);
			}
			battle.free();

			/*
				Subraumspalten (durch Sprungantriebe)
			*/

			SQLResultRow jumps = db.first("SELECT count(*) count FROM jumps WHERE x=",scanx," AND y=",scany," AND system=",system);
			if( !jumps.isEmpty() ) {
				t.setVar("sector.subraumspalten", jumps.getInt("count"));
			}

			/*
				Schiffe
			*/
		
			t.setBlock("_SCAN", "ships.listitem", "ships.list");
		
			List<Integer> verysmallshiptypes = new ArrayList<Integer>();
			verysmallshiptypes.add(0); // Ein dummy-Wert, damit es keine SQL-Fehler gibt
			
			// Falls nicht im Admin-Modus und nicht das aktuelle Feld gescannt wird: Liste der kleinen Schiffe generieren
			if( !this.admin && (scanx != this.ship.getInt("x")) || (scany != this.ship.getInt("y")) ) {
				SQLQuery stid = db.query("SELECT id FROM ship_types WHERE LOCATE('",ShipTypes.SF_SEHR_KLEIN,"',flags)");
				while( stid.next() ) {
					verysmallshiptypes.add(stid.getInt("id"));
				}
				stid.free();
			}
			
			SQLQuery datas = db.query("SELECT s.id,s.owner,s.name,s.type,s.docked,s.status,u.name username,s.battle " ,
					"FROM ships s JOIN users u ON s.owner=u.id " ,
					"WHERE s.id>0 AND s.x=",scanx," AND s.y=",scany," AND s.system=",system," AND s.battle is null AND " ,
						"(!(s.type IN (",Common.implode(",",verysmallshiptypes),")) OR LOCATE('tblmodules',s.status)) AND " ,
						"(s.visibility IS NULL OR s.visibility='",user.getId(),"') AND !LOCATE('l ',s.docked) " ,
						"ORDER BY s.id");
						
			while( datas.next() ) {
				boolean disableIFF = (datas.getString("status").indexOf("disable_iff") > -1);
				SQLResultRow shiptype = ShipTypes.getShipType( datas.getRow() );
				
				// Falls nicht im Admin-Modus: Nur sehr kleine Schiffe im Feld des scannenden Schiffes anzeigen
				if( !this.admin && ((scanx != this.ship.getInt("x")) || (scany != this.ship.getInt("y"))) &&
					ShipTypes.hasShipTypeFlag(shiptype, ShipTypes.SF_SEHR_KLEIN) ) {
					continue;	
				}
				
				t.setVar(	"ship.id",				datas.getInt("id"),
							"ship.isown",			(datas.getInt("owner") == user.getId()),
							"ship.owner.id",		datas.getInt("owner"),
							"ship.name",			Common._plaintitle(datas.getString("name")),
							"ship.owner.name",		Common._title(datas.getString("username")),
							"ship.ownerlink",		(datas.getInt("owner") != user.getId()),
							"ship.battle",			datas.getInt("battle"),
							"ship.type.name",		shiptype.getString("nickname"),
							"ship.type",			datas.getInt("type"),
							"ship.type.picture",	shiptype.getString("picture") );
	
				if( disableIFF ) {
					t.setVar(	"ship.owner.name",	"Unbekannt",
								"ship.ownerlink",	0 );
				}
				t.parse("ships.list", "ships.listitem", true);
			}
			datas.free();
		}
	}

	private static class BaseEntry {
		BaseEntry() {
			// EMPTY
		}
		int id;
		int owner;
		int ally;
		int klasse;
		int size;
		int imgcount;
	}

	/**
	 * Zeigt die LRS-Karte an
	 */
	@Action(ActionType.DEFAULT)
	@Override
	public void defaultAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		/*
			Alle Objekte zusammensuchen, die fuer uns in Frage kommen
		*/
		
		String rangesql = "system="+this.ship.getInt("system")+" AND " +
					"(x BETWEEN "+(this.ship.getInt("x")-this.range)+" AND "+(this.ship.getInt("x")+this.range)+") AND " +
					"(y BETWEEN "+(this.ship.getInt("y")-this.range)+" AND "+(this.ship.getInt("y")+this.range)+")";

		// Nebel
		Map<Location,Integer> nebelmap = new HashMap<Location,Integer>();

		SQLQuery nebelRow = db.query("SELECT * FROM nebel WHERE ",rangesql);
		while( nebelRow.next() ) {
			nebelmap.put(new Location(ship.getInt("system"), nebelRow.getInt("x"), nebelRow.getInt("y")), nebelRow.getInt("type"));
		}
		nebelRow.free();
		
		// Jumpnodes
		Map<Location,Boolean> nodemap = new HashMap<Location,Boolean>();

		SQLQuery nodeRow = db.query("SELECT id,x,y FROM jumpnodes WHERE ",rangesql);
		while( nodeRow.next() ) {
			nodemap.put(new Location(ship.getInt("system"), nodeRow.getInt("x"), nodeRow.getInt("y")), true);
		}
		nodeRow.free();

		// Schiffe
		List<Integer> verysmallshiptypes = new ArrayList<Integer>();
		verysmallshiptypes.add(0); // Ein dummy-Wert, damit es keine SQL-Fehler gibt
		
		// Im Admin-Modus sind alle Schiffe sichtbar
		if( !this.admin ) {
			SQLQuery stid = db.query("SELECT id FROM ship_types WHERE LOCATE('",ShipTypes.SF_SEHR_KLEIN,"',flags)");
			while( stid.next() ) {
				verysmallshiptypes.add(stid.getInt("id"));
			}
			stid.free();
		}
		
		Map<Location,List<SQLResultRow>> shipmap = new HashMap<Location,List<SQLResultRow>>();
		Map<Location,Boolean> ownshipmap = new HashMap<Location,Boolean>();

		SQLQuery sRow = db.query("SELECT t1.id,t1.x,t1.y,t2.ally,t1.owner,t1.docked,t1.crew,t1.sensors,t1.type,t1.status " ,
								"FROM ships t1 JOIN users t2 ON t1.owner=t2.id " ,
								"WHERE t1.id>0 AND ",rangesql," AND (t1.visibility IS NULL OR t1.visibility='",user.getId(),"') AND " ,
										"(!(t1.type IN (",Common.implode(",",verysmallshiptypes),")) OR LOCATE('tblmodules',t1.status))");
		while( sRow.next() ) {
			SQLResultRow st = ShipTypes.getShipType(sRow.getRow());
			
			// Im Admin-Modus sind alle Schiffe sichtbar
			if( !this.admin && ShipTypes.hasShipTypeFlag(st, ShipTypes.SF_SEHR_KLEIN) ) {
				continue;	
			}
			
			Location loc = new Location(ship.getInt("system"), sRow.getInt("x"), sRow.getInt("y"));
			if( !shipmap.containsKey(loc) ) {
				shipmap.put(loc, new ArrayList<SQLResultRow>());
			}
			shipmap.get(loc).add(sRow.getRow());

			if( (sRow.getInt("owner") == user.getId()) && (sRow.getInt("sensors")>30) && !ownshipmap.containsKey(loc) ) {			
				if( sRow.getInt("crew") >= st.getInt("crew")/4 ) {
					ownshipmap.put(loc, true);
				}
			}
		}
		sRow.free();
		
		// Basen
		Map<Location,BaseEntry> basemap = new HashMap<Location,BaseEntry>();

		SQLQuery bRow = db.query("SELECT t1.id,t1.owner,t1.x,t1.y,t1.klasse,t1.size,t2.ally " ,
								"FROM bases t1 JOIN users t2 ON t1.owner=t2.id " ,
								"WHERE t1.system=",this.ship.getInt("system")," AND " ,
									"(FLOOR(SQRT(POW(t1.x-",this.ship.getInt("x"),",2)+POW(t1.y-",this.ship.getInt("y"),",2)))-CAST(t1.size AS SIGNED) <= ",this.range,") AND t1.owner=t2.id " ,
								"ORDER BY size");
						
		while( bRow.next() ) {
			int imgcount = 0;
			Location centerLoc = new Location(this.ship.getInt("system"), bRow.getInt("x"), bRow.getInt("y"));
			for( int by=bRow.getInt("y")-bRow.getInt("size"); by <= bRow.getInt("y")+bRow.getInt("size"); by++ ) {
				for( int bx=bRow.getInt("x")-bRow.getInt("size"); bx <= bRow.getInt("x")+bRow.getInt("size"); bx++ ) {
					Location loc = new Location(this.ship.getInt("system"), bx, by);
					
					if( !centerLoc.sameSector( 0, loc, bRow.getInt("size") ) ) {
						continue;	
					}
					BaseEntry entry = new BaseEntry();
					entry.id = bRow.getInt("id");
					entry.owner = bRow.getInt("owner");
					entry.ally = bRow.getInt("ally");
					entry.klasse = bRow.getInt("klasse");
					entry.size = bRow.getInt("size");
					if( bRow.getInt("size") > 0 ) {
						entry.imgcount = imgcount;
						imgcount++;
					}
					basemap.put(loc, entry);
				}
			}
		}
		bRow.free();
		
		// Obere/Untere Koordinatenreihe
		
		t.setBlock("_SCAN", "mapborder.listitem", "mapborder.list");
		
		for( int x = this.ship.getInt("x")-this.range; x <= this.ship.getInt("x")+this.range; x++ ) {
			if( (x > 0) && (x <= Systems.get().system(this.ship.getInt("system")).getWidth()) ) {
				t.setVar("mapborder.x", x);
				t.parse("mapborder.list", "mapborder.listitem", true);
			}
		}
		
		/*
			Ausgabe der Karte
		*/
		
		t.setBlock("_SCAN", "map.rowitem", "map.rowlist");
		t.setBlock("map.rowitem", "map.listitem", "map.list");
		
		for( int y = this.ship.getInt("y")-this.range; y <= this.ship.getInt("y")+this.range; y++ ) {
			if( (y < 1) || (y > Systems.get().system(this.ship.getInt("system")).getHeight()) ) {
				continue;
			}
			
			t.setVar(	"map.border.y",	y,
						"map.list",		"" );
	
			// Einen einzelnen Sektor ausgeben
			for( int x = this.ship.getInt("x")-this.range; x <= this.ship.getInt("x")+this.range; x++ ) {
				if( (x < 1) || (x > Systems.get().system(this.ship.getInt("system")).getWidth()) ) {
					continue;
				}
				Location loc = new Location(this.ship.getInt("system"), x, y);
				
				t.start_record();
	
				String cssClass = "";
				
				if( (x != this.ship.getInt("x")) || (y != this.ship.getInt("y")) ) {
					cssClass = "class=\"starmap\"";
				}
				
				if( Math.round(Math.sqrt(Math.pow(y-this.ship.getInt("y"),2)+Math.pow(x-this.ship.getInt("x"),2))) <= this.range ) {				
					t.setVar(	"map.x",			x,
								"map.y",			y,
								"map.linkclass",	cssClass,
								"map.showsector",	1 );
	
					// Nebel
					if( nebelmap.containsKey(loc) && 
							(!ownshipmap.containsKey(loc) || ((nebelmap.get(loc) >= 3) && (nebelmap.get(loc) <= 5)) ) ) {
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
							List<SQLResultRow> myships = shipmap.get(loc);
							for( int i=0; i < myships.size(); i++ ) {
								SQLResultRow myship = myships.get(i);
								
								if( myship.getInt("owner") == user.getId() ) {
									if( (myship.getString("docked").length() == 0) || (myship.getString("docked").charAt(0) != 'l') ) {
										if( own == 0 ) {
											fleet[0] = "_fo";
										}	
										own++;
									}
								}
								else if( (myship.getInt("owner") != user.getId()) && (user.getAlly() != null) && (myship.getInt("ally") == user.getAlly().getId()) ) {
									if( (myship.getString("docked").length() == 0) || (myship.getString("docked").charAt(0) != 'l') ) {
										if( ally == 0 ) {
											fleet[1] = "_fa";
										}	
										ally++;
									}
								}
								else if( (myship.getInt("owner") != user.getId()) && ( (user.getAlly() == null) || ((user.getAlly() != null) && (myship.getInt("ally") != user.getAlly().getId()) ) )  ) {
									if( (myship.getString("docked").length() == 0) || (myship.getString("docked").charAt(0) != 'l') ) {
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
						if( own+ally+enemy > 0 ) {
							tooltip = "onmouseover=\"return overlib('<span class=\\'smallfont\\'>"+(own != 0 ? "Eigene: "+own+"<br />":"")+(ally != 0 ? "Ally: "+ally+"<br />":"")+(enemy != 0 ? "Feindliche: "+enemy+"<br />":"")+"</span>',TIMEOUT,0,DELAY,400,WIDTH,120);\" onmouseout=\"return nd();\"";
							fleetStr = Common.implode("", fleet);
						}
					
						t.setVar(	"map.tooltip",	tooltip,
									"map.fleet",	fleetStr );
					
						// Nebel, Basen, Sprungpunkte
						if( nebelmap.containsKey(loc) ) {
							t.setVar(	"map.image",		"fog"+nebelmap.get(loc)+"/fog"+nebelmap.get(loc),
										"map.image.name",	"Nebel" );
						}
						else if( basemap.containsKey(loc) ) {
							BaseEntry entry = basemap.get(loc);
							if( entry.size > 0 ) {
								t.setVar(	"map.image",		"kolonie"+entry.klasse+"_lrs/kolonie"+entry.klasse+"_lrs"+entry.imgcount,
											"map.image.name",	"Asteroid" );
							}
							else if( entry.owner == user.getId() ) {
								t.setVar(	"map.image",		"asti_own/asti_own",
											"map.image.name",	"Eigener Asteroid" );
							}
							else if( (entry.owner != 0) && (user.getAlly() != null) && (entry.ally == user.getAlly().getId()) ) {
								t.setVar(	"map.image",		"asti_ally/asti_ally",
											"map.image.name",	"Ally Asteroid" );
							}
							else if( (entry.owner != 0) ) {
								t.setVar(	"map.image",		"asti_enemy/asti_enemy",
											"map.image.name",	"Feindlicher Asteroid" );
							}
							else {
								String astiimg = "kolonie"+entry.klasse+"_lrs";
								
								t.setVar(	"map.image",		astiimg+"/"+astiimg,
											"map.image.name",	"Asteroid" );
							}
						}
						else if( nodemap.containsKey(loc) ) {
							t.setVar(	"map.image",		"jumpnode/jumpnode",
										"map.image.name",	"Jumpnode" );
						}
						else {
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
