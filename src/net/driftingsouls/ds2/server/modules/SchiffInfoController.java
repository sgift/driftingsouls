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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.ModuleSlots;
import net.driftingsouls.ds2.server.config.NoSuchSlotException;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.Weapon;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.OrderShip;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.NoSuchShipTypeException;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

import org.apache.commons.lang.StringUtils;

/**
 * Die Schiffstypen-Infos
 * @author Christopher Jung
 *
 */
public class SchiffInfoController extends TemplateGenerator {
	private int shipID = 0;
	private ShipTypeData ship = null;
	private ShipBaubar shipBuildData = null;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public SchiffInfoController(Context context) {
		super(context);
		
		parameterNumber("ship");

		requireValidSession(false);
		
		setTemplate("schiffinfo.html");
		
		setPageTitle("Schiffstyp");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		int ship = getInteger("ship");
		
		if( ship == 0 ) {
			addError("Keine Schiffstypen-ID angegeben");
			
			return false;
		}
		
		t.setVar( "global.login", (getUser() != null) );
					
		ShipTypeData data = null;
		try {
			data = Ship.getShipType(ship);
		}
		catch(NoSuchShipTypeException e) {
			// EMPTY
		}

		//Dummydata, falls eine ungueltige Schiffs-ID eingegeben wurde
		if( (data == null) || 
			(data.isHide() && ((user == null) || (user.getAccessLevel() < 10)) ) ) {
			
			addError("&Uuml;ber diesen Schiffstyp liegen leider keine Daten vor");
			
			return false;
		}
		else if( data.isHide() && (user != null) && (user.getAccessLevel() >= 10) ) {
			t.setVar("shiptype.showinvisible",1);
		}
		
		ShipBaubar sw = null;
		
		if( ship != 0 ) {
			//Daten fuer baubare Schiffe laden
			sw = (ShipBaubar)db.createQuery("from ShipBaubar where type=?")
				.setInteger(0, ship)
				.setMaxResults(1)
				.uniqueResult();
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
				if( shipBuildData.getRes(i) != 0 ) {
					SQLResultRow dat = db.first("SELECT f.name, uf.r",shipBuildData.getRes(i)," AS research " +
							"FROM forschungen f JOIN user_f uf " +
							"WHERE f.id=",shipBuildData.getRes(i)," AND uf.id=",getUser().getId());
					String cssClass = "error";
					if( !dat.isEmpty() && dat.getBoolean("research") ) {
						cssClass = "ok";
					} 	

					t.setVar(	"shiptype.tr"+i, shipBuildData.getRes(i),
								"shiptype.tr"+i+".name"	, Common._title(dat.getString("name")),
								"shiptype.tr"+i+".status", cssClass );
				}
			}	
   		} 
		else {
			for( int i=1; i <= 3; i++ ) {
				if( shipBuildData.getRes(i) != 0 ) {
					Forschung f = Forschung.getInstance(shipBuildData.getRes(i));

					t.setVar(	"shiptype.tr"+i, shipBuildData.getRes(i),
								"shiptype.tr"+i+".name", Common._title(f.getName()) );
				}
			}
		}
		String race = "???";
		if( shipBuildData.getRace() == -1 ) {
			race = "Alle";
		} 
		else {
			race = Rassen.get().rasse(shipBuildData.getRace()).getName();
		}

		t.setVar("shiptype.race",race);
	}
	
	private void outShipCost() {
		TemplateEngine t = getTemplateEngine();

		t.setVar(	"shiptype.cost.energie",		shipBuildData.getEKosten(),
					"shiptype.cost.crew",			shipBuildData.getCrew(),
					"shiptype.cost.dauer",			shipBuildData.getDauer(),
					"shiptype.cost.werftslots",		shipBuildData.getWerftSlots());

		t.setBlock("_SCHIFFINFO","res.listitem","res.list");
	
		Cargo costs = shipBuildData.getCosts();
		ResourceList reslist = costs.getResourceList();
		for( ResourceEntry res  : reslist ) {
			t.setVar(	"res.name", res.getName(),
						"res.image", res.getImage(),
						"res.count", res.getCargo1() );
			t.parse("res.list","res.listitem",true);
		}
	}
	
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		
		//Kann der User sehen, dass das Schiff baubar ist?
		int visible = -1;

		if( shipBuildData != null ) {
			for( int i=1; i <= 3; i++ ) {
				if( shipBuildData.getRes(i) != 0 ) {
					Forschung research = Forschung.getInstance(shipBuildData.getRes(i));
					
					if( !research.isVisibile() && 
						(user == null || !user.hasResearched(research.getRequiredResearch(1)) || 
						 !user.hasResearched(research.getRequiredResearch(2)) || 
						 !user.hasResearched(research.getRequiredResearch(3)) ) ) {
						 	
						visible = shipBuildData.getRes(i);
					}
				}
			}
		}

		if( visible > 0 ) {
			shipBuildData = null;
			
			if( (user != null) && user.getAccessLevel() >= 10 ) {
				t.setVar(	"shiptype.showbuildable",	1,
							"shiptype.visibletech",		visible);
			}	
		}

		if( (user != null) && (user.getAccessLevel() >= 10) ) {
			OrderShip order = (OrderShip)db.get(OrderShip.class, shipID);
			
			if( order != null ) {
				t.setVar(	"shiptype.showorderable",	1,
							"shiptype.ordercost",		order.getCost() );
			}
		}

		Map<String,String> weapons = Weapons.parseWeaponList(ship.getWeapons());
		Map<String,String> maxheat = Weapons.parseWeaponList(ship.getMaxHeat());
		
		t.setBlock("_SCHIFFINFO","shiptype.weapons.listitem","shiptype.weapons.list");
		for( Map.Entry<String, String> entry: weapons.entrySet() ) {
			int count = Integer.parseInt(entry.getValue());
			String weaponname = entry.getKey();
			
			Weapon weapon = null;
			weapon = Weapons.get().weapon(weaponname);
			
			if( weapon == null ) {
				t.setVar(	"shiptype.weapon.name",			"<span style=\"color:red\">UNKNOWN: weapon</span>",
							"shiptype.weapon.count",		count,
							"shiptype.weapon.description",	"" );

				t.parse("shiptype.weapons.list","shiptype.weapons.listitem",true);
				
				continue;
			}

			StringBuilder descrip = new StringBuilder(100);
			descrip.append("<span style=\\'font-size:12px\\'>");

			descrip.append("AP-Kosten: ");
			descrip.append(weapon.getAPCost());
			descrip.append("<br />");
			descrip.append("Energie-Kosten: ");
			descrip.append(Common.ln(weapon.getECost()));
			descrip.append("<br />");

			if( weapon.getSingleShots() > 1 ) {
				descrip.append("Sch&uuml;sse: ");
				descrip.append(weapon.getSingleShots());
				descrip.append("<br />");
			}

			descrip.append("Max. &Uuml;berhitzung: ");
			descrip.append(maxheat.get(weaponname));
			descrip.append("<br />");

			descrip.append("Schaden (H/S/Sub): ");
			if( !"none".equals(weapon.getAmmoType()) ) {
				descrip.append("Munition<br />");
			}
			else {
				descrip.append(Common.ln(weapon.getBaseDamage(ship)));
				descrip.append("/");
				descrip.append(Common.ln(weapon.getShieldDamage(ship)));
				descrip.append("/");
				descrip.append(Common.ln(weapon.getSubDamage(ship)));
				descrip.append("<br />");
			}

			descrip.append("Trefferws (C/J/Torp): ");
			if( !"none".equals(weapon.getAmmoType()) ) {
				descrip.append("Munition<br />");
			}
			else {
				descrip.append(weapon.getDefTrefferWS());
				descrip.append("/");
				descrip.append(weapon.getDefSmallTrefferWS());
				descrip.append("/");
				descrip.append(weapon.getTorpTrefferWS());
				descrip.append("<br />");
			}
			
			if( weapon.getAreaDamage() != 0 ) {
				descrip.append("Areadamage: ");
				descrip.append(weapon.getAreaDamage());
				descrip.append("<br />");	
			}
			if( weapon.getDestroyable() ) {
				descrip.append("Durch Abwehrfeuer zerst&ouml;rbar<br />");	
			}
			if( weapon.hasFlag(Weapon.Flags.DESTROY_AFTER) ) {
				descrip.append("Beim Angriff zerst&ouml;rt<br />");	
			}
			if( weapon.hasFlag(Weapon.Flags.LONG_RANGE) ) {
				descrip.append("Gro&szlig;e Reichweite<br />");	
			}
			if( weapon.hasFlag(Weapon.Flags.VERY_LONG_RANGE) ) {
				descrip.append("Sehr gro&szlig;e Reichweite<br />");	
			}
	
			descrip.append("</span>");

			t.setVar(	"shiptype.weapon.name",			weapon.getName(),
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

		String[] flaglist = Ship.getShipTypeFlagList(this.ship);
		Arrays.sort(flaglist);
		for( int i=0; i < flaglist.length; i++ ) {
			if( flaglist[i].length() == 0 ) {
				continue;	
			}
			t.setVar(	"shiptypeflag.name", 			ShipTypes.getShipTypeFlagName(flaglist[i]),
						"shiptypeflag.description",		ShipTypes.getShipTypeFlagDescription(flaglist[i]) );
								
			t.parse("shiptypeflags.list","shiptypeflags.listitem",true);
		}
	
		// Module
		StringBuilder moduletooltip = new StringBuilder();
		String[] modulelist = new String[0];
		if( ship.getTypeModules().length() != 0 ) {
			modulelist = StringUtils.split( ship.getTypeModules(), ';' );
			
			for( int i=0; i < modulelist.length; i++ ) {
				String[] amodule = StringUtils.split(modulelist[i],':');
				try {
					moduletooltip.append(ModuleSlots.get().slot(amodule[1]).getName());
					moduletooltip.append("<br />");
				}
				catch( NoSuchSlotException e ) {
					moduletooltip.append("<span style='color:red'>UNGUELTIGER SLOTTYP ");
					moduletooltip.append(amodule[1]);
					moduletooltip.append("</span><br />");
				}
			}
		}

		t.setVar(	"shiptype.nickname",	ship.getNickname(),
					"shiptype.id",			ship.getTypeId(),
					"shiptype.class",		ShipTypes.getShipClass(ship.getShipClass()).getSingular(),
					"shiptype.image",		ship.getPicture(),
					"shiptype.ru",			ship.getRu(),
					"shiptype.rd",			ship.getRd(),
					"shiptype.ra",			ship.getRa(),
					"shiptype.rm",			ship.getRm(),
					"nahrung.image",		Cargo.getResourceImage(Resources.NAHRUNG),
					"uran.image",			Cargo.getResourceImage(Resources.URAN),
					"deuterium.image",		Cargo.getResourceImage(Resources.DEUTERIUM),
					"antimaterie.image",	Cargo.getResourceImage(Resources.ANTIMATERIE),
					"shiptype.buildable",	shipBuildData != null,
					"shiptype.cost",		ship.getCost(),
					"shiptype.heat",		ship.getHeat(),
					"shiptype.size",		ship.getSize(),
					"shiptype.sensorrange",	ship.getSensorRange() + 1,
					"shiptype.eps",			ship.getEps(),
					"shiptype.cargo",		ship.getCargo(),
					"shiptype.crew",		ship.getCrew(),
					"shiptype.jdocks",		ship.getJDocks(),
					"shiptype.adocks",		ship.getADocks(),
					"shiptype.hull",		Common.ln(ship.getHull()),
					"shiptype.panzerung",	ship.getPanzerung(),
					"shiptype.ablativearmor",	ship.getAblativeArmor(),
					"shiptype.shields",		Common.ln(ship.getShields()),
					"shiptype.deutfactor",	ship.getDeutFactor(),
					"shiptype.hydro",		ship.getHydro(),
					"shiptype.flagschiff",	shipBuildData != null && shipBuildData.isFlagschiff(),
					"shiptype.recost",		ship.getReCost(),
					"shiptype.torpedodef",	ship.getTorpedoDef(),
					"shiptype.moduleslots",	modulelist.length,
					"shiptype.moduleslots.desc",	moduletooltip,
					"shiptype.count",		ship.getShipCount(),
					"shiptype.werftslots",	ship.getWerft() );

		if( ship.getDescrip().length() == 0 ) {
			t.setVar("shiptype.description", Common._text("Keine Beschreibung verf&uuml;gbar"));
		}
		else {
			t.setVar("shiptype.description", Common._text(ship.getDescrip()));
		}
		
		if( shipBuildData != null ) {
			outPrerequisites();
		}

		//Produktionskosten anzeigen, sofern das Schiff baubar ist
		if( shipBuildData != null ) {
			outShipCost();
		}
		
	}
}
