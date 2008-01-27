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

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.Forschung;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.ModuleSlots;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.Weapon;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.NoSuchShipTypeException;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Die Schiffstypen-Infos
 * @author Christopher Jung
 *
 */
public class SchiffInfoController extends DSGenerator {
	private int shipID = 0;
	private SQLResultRow ship = null;
	private SQLResultRow shipBuildData = null;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public SchiffInfoController(Context context) {
		super(context);
		
		parameterNumber("ship");

		requireValidSession(false);
		
		setTemplate("schiffinfo.html");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		int ship = getInteger("ship");
		
		if( ship == 0 ) {
			addError("Keine Schiffstypen-ID angegeben");
			
			return false;
		}
		
		t.setVar( "global.login", (getUser() != null) );
					
		SQLResultRow data = null;
		try {
			data = ShipTypes.getShipType(ship, false);
		}
		catch(NoSuchShipTypeException e) {
			// EMPTY
		}

		//Dummydata, falls eine ungueltige Schiffs-ID eingegeben wurde
		if( (data == null) || 
			(data.getBoolean("hide") && (user != null) && (user.getAccessLevel() < 10) ) || 
			(data.getBoolean("hide") && user == null) ) {
			
			if( data != null ) {
				data.clear();
			}
			else {
				data = new SQLResultRow();
			}
			data.put( "id",ship );
			data.put( "nickname","Unbekanntes Schiff" );
			data.put( "ru", 0 );
			data.put( "rd", 0 );
			data.put( "ra", 0 );
			data.put( "rm", 0 );
			data.put( "eps", 0 );
			data.put( "cost", 0 );
			data.put( "hull", 0 );
			data.put( "cargo", 0 );
			data.put( "heat", 0 );
			data.put( "crew", 0 );
			data.put( "weapons","" );
			data.put( "maxheat","" );
			data.put( "torpedodef" , 0 );
			data.put( "shields", 0 );
			data.put( "size", 0 );
			data.put( "jdocks", 0 );
			data.put( "flags" , "" );
			data.put( "adocks", 0 );
			data.put( "sensorrage", 0 );
			data.put( "hydro", 0 );
			data.put( "recost" , 0 );
			data.put( "deutfactor", 0 );
			data.put( "class",ShipClasses.UNBEKANNT.ordinal() );
			data.put( "descrip", "&uuml;ber diesen Schiffstyp liegen leider keine Daten vor");
			data.put( "panzerung", 0 );
			data.put( "sensorrange", 0 );
			data.put( "picture", "" );
			data.put( "modules", "" );
			
			ship = 0;
		}
		else if( data.getBoolean("hide") && (user != null) && (user.getAccessLevel() >= 10) ) {
			t.setVar("shiptype.showinvisible",1);
		}
		
		SQLResultRow sw = null;
		
		if( ship != 0 ) {	
			//Daten fuer baubare Schiffe laden
			sw = db.first("SELECT * FROM ships_baubar WHERE type=",ship);
		}
		
		if( sw == null) {
			sw = new SQLResultRow();	
		}
		
		this.shipID = ship;
		this.ship = data;
		this.shipBuildData = sw;	
				
		return true;
	}

	private void outPrerequisites() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();

		if( getUser() != null ) {
			for( int i=1; i <= 3; i++ ) {
				if( shipBuildData.getInt("tr"+i) != 0 ) {
					SQLResultRow dat = db.first("SELECT f.name, uf.r",shipBuildData.getInt("tr"+i)," AS research " +
							"FROM forschungen f JOIN user_f uf " +
							"WHERE f.id=",shipBuildData.get("tr"+i)," AND uf.id=",getUser().getID());
					String cssClass = "error";
					if( !dat.isEmpty() && dat.getBoolean("research") ) {
						cssClass = "ok";
					} 	

					t.setVar(	"shiptype.tr"+i, shipBuildData.getInt("tr"+i),
								"shiptype.tr"+i+".name"	, Common._title(dat.getString("name")),
								"shiptype.tr"+i+".status", cssClass );
				}
			}	
   		} 
		else {
			for( int i=1; i <= 3; i++ ) {
				if( shipBuildData.getInt("tr"+i) != 0 ) {
					Forschung f = Forschung.getInstance(shipBuildData.getInt("tr"+i));

					t.setVar(	"shiptype.tr"+i, shipBuildData.getInt("tr"+i),
								"shiptype.tr"+i+".name", Common._title(f.getName()) );
				}
			}
		}
		String race = "???";
		if( shipBuildData.getInt("race") == -1 ) {
			race = "Alle";
		} 
		else {
			race = Rassen.get().rasse(shipBuildData.getInt("race")).getName();
		}

		t.setVar("shiptype.race",race);
	}
	
	private void outShipCost() {
		TemplateEngine t = getTemplateEngine();

		t.setVar(	"shiptype.cost.energie",		shipBuildData.getInt("ekosten"),
					"shiptype.cost.crew",			shipBuildData.getInt("crew"),
					"shiptype.cost.dauer",			shipBuildData.getInt("dauer"),
					"shiptype.cost.linfactor",		shipBuildData.getInt("linfactor")*100 );

		t.setBlock("_SCHIFFINFO", "shiptype.werften.listitem", "shiptype.werften.list" );
		String[] werftlist = StringUtils.split(shipBuildData.getString("werftreq"), " ");
		
		for( int i=0; i < werftlist.length; i++ ) {
			String name = "";
			if( werftlist[i].equals("ganymed") ) {
				name = "Ganymed";	
			}
			else if( werftlist[i].equals("pwerft") ) {
				name = "Werft";	
			}
			t.setVar("werft.name", name);
			t.parse("shiptype.werften.list", "shiptype.werften.listitem", true);
		}

		t.setBlock("_SCHIFFINFO","res.listitem","res.list");
	
		Cargo costs = new Cargo( Cargo.Type.STRING, shipBuildData.getString("costs") );
		ResourceList reslist = costs.getResourceList();
		for( ResourceEntry res  : reslist ) {
			t.setVar(	"res.name", res.getName(),
						"res.image", res.getImage(),
						"res.count", res.getCargo1() );
			t.parse("res.list","res.listitem",true);
		}
	}
	
	@Override
	public void defaultAction() {
		Database db = getDatabase();
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		//Kann der User sehen, dass das Schiff baubar ist?
		int visible = -1;

		if( !shipBuildData.isEmpty() ) {
			for( int i=1; i <= 3; i++ ) {
				if( shipBuildData.getInt("tr"+i) != 0 ) {
					SQLResultRow research = db.first("SELECT visibility,req1,req2,req3 FROM forschungen WHERE id=",shipBuildData.get("tr"+i));
					
					if( (research.getInt("visibility") != 1) && 
						(user == null || !user.hasResearched(research.getInt("req1")) || 
						 !user.hasResearched(research.getInt("req2")) || !user.hasResearched(research.getInt("req3")) ) ) {
						 	
						visible = shipBuildData.getInt("tr"+i);
					}
				}
			}
		}

		if( visible > 0 ) {
			shipBuildData.clear();
			
			if( (user != null) && user.getAccessLevel() >= 10 ) {
				t.setVar(	"shiptype.showbuildable",	1,
							"shiptype.visibletech",		visible);
			}	
		}

		if( (user != null) && (user.getAccessLevel() >= 10) ) {
			SQLResultRow order = db.first("SELECT cost FROM orders_ships WHERE type=",shipID);
			
			if( !order.isEmpty() ) {
				t.setVar(	"shiptype.showorderable",	1,
							"shiptype.ordercost",		order.getInt("cost") );
			}
		}

		Map<String,String> weapons = Weapons.parseWeaponList(ship.getString("weapons"));
		Map<String,String> maxheat = Weapons.parseWeaponList(ship.getString("maxheat"));
		
		t.setBlock("_SCHIFFINFO","shiptype.weapons.listitem","shiptype.weapons.list");
		for( String weapon : weapons.keySet() ) {
			int count = Integer.parseInt(weapons.get(weapon));
			if( Weapons.get().weapon(weapon) == null ) {
				t.setVar(	"shiptype.weapon.name",			"<span style=\"color:red\">UNKNOWN: weapon</span>",
							"shiptype.weapon.count",		count,
							"shiptype.weapon.description",	"" );

				t.parse("shiptype.weapons.list","shiptype.weapons.listitem",true);
				
				continue;
			}

			StringBuilder descrip = new StringBuilder(100);
			descrip.append("<span style=\\'font-size:12px\\'>");

			descrip.append("AP-Kosten: ");
			descrip.append(Weapons.get().weapon(weapon).getAPCost());
			descrip.append("<br />");
			descrip.append("Energie-Kosten: ");
			descrip.append(Common.ln(Weapons.get().weapon(weapon).getECost()));
			descrip.append("<br />");

			if( Weapons.get().weapon(weapon).getSingleShots() > 1 ) {
				descrip.append("Sch&uuml;sse: ");
				descrip.append(Weapons.get().weapon(weapon).getSingleShots());
				descrip.append("<br />");
			}

			descrip.append("Max. &Uuml;berhitzung: ");
			descrip.append(maxheat.get(weapon));
			descrip.append("<br />");

			descrip.append("Schaden (H/S/Sub): ");
			if( !Weapons.get().weapon(weapon).getAmmoType().equals("none") ) {
				descrip.append("Munition<br />");
			}
			else {
				descrip.append(Common.ln(Weapons.get().weapon(weapon).getBaseDamage(ship)));
				descrip.append("/");
				descrip.append(Common.ln(Weapons.get().weapon(weapon).getShieldDamage(ship)));
				descrip.append("/");
				descrip.append(Common.ln(Weapons.get().weapon(weapon).getSubDamage(ship)));
				descrip.append("<br />");
			}

			descrip.append("Trefferws (C/J/Torp): ");
			if( !Weapons.get().weapon(weapon).getAmmoType().equals("none") ) {
				descrip.append("Munition<br />");
			}
			else {
				descrip.append(Weapons.get().weapon(weapon).getDefTrefferWS());
				descrip.append("/");
				descrip.append(Weapons.get().weapon(weapon).getDefSmallTrefferWS());
				descrip.append("/");
				descrip.append(Weapons.get().weapon(weapon).getTorpTrefferWS());
				descrip.append("<br />");
			}
			
			if( Weapons.get().weapon(weapon).getAreaDamage() != 0 ) {
				descrip.append("Areadamage: ");
				descrip.append(Weapons.get().weapon(weapon).getAreaDamage());
				descrip.append("<br />");	
			}
			if( Weapons.get().weapon(weapon).getDestroyable() ) {
				descrip.append("Durch Abwehrfeuer zerst&ouml;rbar<br />");	
			}
			if( Weapons.get().weapon(weapon).hasFlag(Weapon.Flags.DESTROY_AFTER) ) {
				descrip.append("Beim Angriff zerst&ouml;rt<br />");	
			}
			if( Weapons.get().weapon(weapon).hasFlag(Weapon.Flags.LONG_RANGE) ) {
				descrip.append("Gro&szlig;e Reichweite<br />");	
			}
	
			descrip.append("</span>");

			t.setVar(	"shiptype.weapon.name",			Weapons.get().weapon(weapon).getName(),
						"shiptype.weapon.count",		count,
						"shiptype.weapon.description",	descrip );

			t.parse("shiptype.weapons.list","shiptype.weapons.listitem",true);
		}
		
		if( weapons.isEmpty() ) {
			t.setVar("shiptype.noweapons",1);
		}
		
		// Flags auflisten
		t.setBlock("_SCHIFFINFO", "shiptypeflags.listitem", "shiptypeflags.list");
		t.setVar("shiptypeflags.list","");

		String[] flaglist = ShipTypes.getShipTypeFlagList(this.ship);
		Arrays.sort(flaglist);
		for( int i=0; i < flaglist.length; i++ ) {
			if( flaglist[i].length() == 0 ) {
				continue;	
			}
			t.setVar(	"shiptypeflag.name", 			ShipTypes.getShipTypeFlagName(flaglist[i]),
						"shiptypeflag.description",		ShipTypes.getShipTypeFlagDescription(flaglist[i]) );
								
			t.parse("shiptypeflags.list","shiptypeflags.listitem",true);
		}

		if( ship.getString("descrip").length() == 0 ) {
			ship.put( "descrip", "Keine Beschreibung verf&uuml;gbar");
		}
		
		// Module
		StringBuilder moduletooltip = new StringBuilder();
		String[] modulelist = new String[0];
		if( ship.getString("modules").length() != 0 ) {
			modulelist = StringUtils.split( ship.getString("modules"), ';' );
			
			for( int i=0; i < modulelist.length; i++ ) {
				String[] amodule = StringUtils.split(modulelist[i],':');
				if( ModuleSlots.get().slot(amodule[1]) != null ) {
					moduletooltip.append(ModuleSlots.get().slot(amodule[1]).getName());
					moduletooltip.append("<br />");
				}
				else {
					moduletooltip.append("<span style='color:red'>UNGUELTIGER SLOTTYP ");
					moduletooltip.append(amodule[1]);
					moduletooltip.append("</span><br />");
				}
			}
		}

		t.setVar(	"shiptype.nickname",	ship.getString("nickname"),
					"shiptype.id",			ship.getInt("id"),
					"shiptype.class",		ShipTypes.getShipClass(ship.getInt("class")).getSingular(),
					"shiptype.image",		ship.getString("picture"),
					"shiptype.ru",			ship.getInt("ru"),
					"shiptype.rd",			ship.getInt("rd"),
					"shiptype.ra",			ship.getInt("ra"),
					"shiptype.rm",			ship.getInt("rm"),
					"nahrung.image",		Cargo.getResourceImage(Resources.NAHRUNG),
					"uran.image",			Cargo.getResourceImage(Resources.URAN),
					"deuterium.image",		Cargo.getResourceImage(Resources.DEUTERIUM),
					"antimaterie.image",	Cargo.getResourceImage(Resources.ANTIMATERIE),
					"shiptype.buildable",	shipBuildData.isEmpty() ? 0:1,
					"shiptype.description",	Common._text(ship.getString("descrip")),
					"shiptype.cost",		ship.getInt("cost"),
					"shiptype.heat",		ship.getInt("heat"),
					"shiptype.size",		ship.getInt("size"),
					"shiptype.sensorrange",	ship.getInt("sensorrange") + 1,
					"shiptype.eps",			ship.getInt("eps"),
					"shiptype.cargo",		ship.getInt("cargo"),
					"shiptype.crew",		ship.getInt("crew"),
					"shiptype.jdocks",		ship.getInt("jdocks"),
					"shiptype.adocks",		ship.getInt("adocks"),
					"shiptype.hull",		Common.ln(ship.getInt("hull")),
					"shiptype.panzerung",	ship.getInt("panzerung"),
					"shiptype.shields",		Common.ln(ship.getInt("shields")),
					"shiptype.deutfactor",	ship.getInt("deutfactor"),
					"shiptype.hydro",		ship.getInt("hydro"),
					"shiptype.flagschiff",	!shipBuildData.isEmpty() && shipBuildData.getBoolean("flagschiff"),
					"shiptype.recost",		ship.getInt("recost"),
					"shiptype.torpedodef",	ship.getInt("torpedodef"),
					"shiptype.moduleslots",	modulelist.length,
					"shiptype.moduleslots.desc",	moduletooltip,
					"shiptype.count",		ship.get("shipcount") );

		if( !shipBuildData.isEmpty() ) {
			outPrerequisites();
		}

		//Produktionskosten anzeigen, sofern das Schiff baubar ist
		if( !shipBuildData.isEmpty() ) {
			outShipCost();
		}
		
	}
}
