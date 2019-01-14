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

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleFlag;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.battles.BattleShipFlag;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.config.items.Munition;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.*;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.modules.ks.*;
import net.driftingsouls.ds2.server.services.SchlachtErstellenService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.units.UnitCargo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Das UI fuer Schlachten.
 * @author Christopher Jung
 *
 */
@Module(name="angriff")
public class AngriffController extends Controller
{
	private static final Log log = LogFactory.getLog(AngriffController.class);
	
	private static final int SHIPGROUPSIZE = 50;
	
	private final Map<String,Class<? extends BasicKSAction>> actions = new HashMap<>();
	private final Map<String,Class<? extends BasicKSMenuAction>> menuActions = new HashMap<>();

	private TemplateViewResultFactory templateViewResultFactory;
	private SchlachtErstellenService schlachtErstellenService;

	@Autowired
	public AngriffController(ConfigService configService,
			TemplateViewResultFactory templateViewResultFactory,
			SchlachtErstellenService schlachtErstellenService) {
		this.templateViewResultFactory = templateViewResultFactory;
		this.schlachtErstellenService = schlachtErstellenService;

		setPageTitle("Schlacht");

		menuActions.put("attack", KSMenuAttackAction.class);
		menuActions.put("attack_select", KSMenuAttackMuniSelectAction.class);
        menuActions.put("groupattack", KSMenuGroupAttackAction.class);
        menuActions.put("groupattack_select", KSMenuGroupAttackMuniSelectAction.class);
		//menuActions.put("batterien", KSMenuBatteriesAction.class);
		menuActions.put("default", KSMenuDefaultAction.class);
		menuActions.put("flucht", KSMenuFluchtAction.class);
		menuActions.put("history", KSMenuHistoryAction.class);
		menuActions.put("new_commander", KSMenuBattleConsignAction.class);
		menuActions.put("other", KSMenuOtherAction.class);
		menuActions.put("shields", KSMenuShieldsAction.class);
        menuActions.put("undock", KSMenuUndockAction.class);
		if( configService.getValue(WellKnownConfigValue.ENABLE_CHEATS) ) {
			menuActions.put("cheats", KSMenuCheatsAction.class);
		}

		actions.put("flucht_single", KSFluchtSingleAction.class);
		actions.put("flucht_all", KSFluchtAllAction.class);
		actions.put("flucht_class", KSFluchtClassAction.class);
        actions.put("undock_single", KSUndockAction.class);
        actions.put("undock_class", KSUndockClassAction.class);
        actions.put("undock_all", KSUndockAllAction.class);
        actions.put("groupattack2", KSGroupAttackAction.class);
		actions.put("attack2", KSAttackAction.class);
		actions.put("kapern", KSKapernAction.class);
		actions.put("secondrow", KSSecondRowAction.class);
		actions.put("leavesecondrow", KSLeaveSecondRowAction.class);
		actions.put("secondrowengage", KSSecondRowEngageAction.class);
		actions.put("secondrowattack", KSSecondRowAttackAction.class);
		actions.put("endbattleequal", KSEndBattleEqualAction.class);
		actions.put("endturn", KSEndTurnAction.class);
        actions.put("activatear", KSActivateAR.class);
        actions.put("deactivatear", KSDeactivateAR.class);
		actions.put("new_commander2", KSNewCommanderAction.class);
		actions.put("take_command", KSTakeCommandAction.class);
		actions.put("stop_take_command", KSStopTakeCommandAction.class);
		actions.put("endbattle", KSEndBattleCivilAction.class);
		actions.put("shields_single", KSRegenerateShieldsSingleAction.class);
		actions.put("shields_all", KSRegenerateShieldsAllAction.class);
		actions.put("shields_class", KSRegenerateShieldsClassAction.class);
		//actions.put("batterien_single", KSDischargeBatteriesSingleAction.class);
		//actions.put("batterien_all", KSDischargeBatteriesAllAction.class);
		//actions.put("batterien_class", KSDischargeBatteriesClassAction.class);
		if( configService.getValue(WellKnownConfigValue.ENABLE_CHEATS) ) {
			actions.put("cheat_regenerate", KSCheatRegenerateOwnAction.class);
			actions.put("cheat_regenerateenemy", KSCheatRegenerateEnemyAction.class);
		}
	}

	@Override
	protected boolean validateAndPrepare() {
		return true;
	}

	private void showInfo(TemplateEngine t, String tag, BattleShip ship, boolean enemy, String jscriptid, boolean show) {
		if( ship == null ) {
			addError("FATAL ERROR: Kein gueltiges Schiff vorhanden");
			return;
		}

		ShipTypeData shipType = ship.getTypeData();

		t.start_record();
		t.setVar(	"shipinfo.jscriptid",		jscriptid,
					"shipinfo.show",			show,
					"shipinfo.enemy",			enemy,
					"shipinfo.type",			ship.getShip().getType(),
					"shipinfo.type.name",		shipType.getNickname(),
					"shipinfo.type.image",		shipType.getPicture(),
					"shipinfo.type.hull",		Common.ln(shipType.getHull()),
					"shipinfo.type.ablativeArmor", Common.ln(shipType.getAblativeArmor()),
					"shipinfo.type.shields",	Common.ln(shipType.getShields()),
					"shipinfo.type.cost",		shipType.getCost(),
					"shipinfo.type.maxenergy",	shipType.getEps(),
					"shipinfo.type.maxcrew",	shipType.getCrew(),
					"shipinfo.type.weapons",	shipType.isMilitary(),
					"shipinfo.hull",			Common.ln(ship.getShip().getHull()),
					"shipinfo.ablativeArmor", 	Common.ln(ship.getShip().getAblativeArmor()),
					"shipinfo.panzerung",		(int)Math.round(shipType.getPanzerung()*ship.getShip().getHull()/(double)shipType.getHull()),
					"shipinfo.shields",			Common.ln(ship.getShip().getShields()),
					"shipinfo.nocrew",			(ship.getShip().getCrew() == 0) && (shipType.getCrew()>0),
					"shipinfo.heat",			ship.getShip().getHeat(),
					"shipinfo.energy",			ship.getShip().getEnergy(),
					"shipinfo.crew",			ship.getShip().getCrew(),
					"shipinfo.engine",			ship.getShip().getEngine(),
					"shipinfo.weapons",			ship.getShip().getWeapons(),
					"shipinfo.comm",			ship.getShip().getComm(),
					"shipinfo.sensors",			ship.getShip().getSensors(),
					"shipinfo.showtmp",			ship.hasFlag(BattleShipFlag.HIT) || ship.hasFlag(BattleShipFlag.DESTROYED),
					"shipinfo.tmp.hull",		ship.hasFlag(BattleShipFlag.DESTROYED) ? 0 : Common.ln(ship.getHull()),
					"shipinfo.tmp.shields",		ship.hasFlag(BattleShipFlag.DESTROYED) ? 0 : Common.ln(ship.getShields()),
					"shipinfo.tmp.engine",		ship.getEngine(),
					"shipinfo.tmp.weapons",		ship.getWeapons(),
					"shipinfo.tmp.comm",		ship.getComm(),
					"shipinfo.tmp.sensors",		ship.getSensors(),
					"shipinfo.tmp.panzerung",	(int)Math.round(shipType.getPanzerung()*ship.getHull()/(double)shipType.getHull()), 
					"shipinfo.tmp.ablativeArmor", ship.hasFlag(BattleShipFlag.DESTROYED) ? 0 : Common.ln(ship.getAblativeArmor()));

		// Ablative Panzerung
		if(ship.getShip().getAblativeArmor() < shipType.getAblativeArmor()/2)
		{
			t.setVar("shipinfo.ablativeArmor.bad", 1);
		}
		else if(ship.getShip().getAblativeArmor() < shipType.getAblativeArmor()) 
		{
			t.setVar("shipinfo.ablativeArmor.normal",1);
		}
		else 
		{
			t.setVar("shipinfo.ablativeArmor.good",1);
		}

		// Huelle
		if( ship.getShip().getHull() < shipType.getHull()/2 ) {
			t.setVar("shipinfo.hull.bad",1);
		}
		else if( ship.getShip().getHull() < shipType.getHull() ) {
			t.setVar("shipinfo.hull.normal",1);
		}
		else {
			t.setVar("shipinfo.hull.good",1);
		}

		// Schilde
		if( shipType.getShields() > 0 ) {
			if( ship.getShip().getShields() < shipType.getShields()/2 ) {
				t.setVar("shipinfo.shields.bad",1);
			}
			else if( ship.getShip().getShields() < shipType.getShields() ) {
				t.setVar("shipinfo.shields.normal",1);
			}
			else {
				t.setVar("shipinfo.shields.good",1);
			}
		}

		// Antrieb
		if( shipType.getCost() > 0 ) {
			if( ship.getShip().getEngine() < 50 ) {
				t.setVar("shipinfo.engine.bad",1);
			}
			else if( ship.getShip().getEngine() < 100 ) {
				t.setVar("shipinfo.engine.normal",1);
			}
			else {
				t.setVar("shipinfo.engine.good",1);
			}
		}

		// Waffen
		if( shipType.isMilitary() ) {
			if( ship.getShip().getWeapons() < 50 ) {
				t.setVar("shipinfo.weapons.bad",1);
			}
			else if( ship.getShip().getWeapons() < 100 ) {
				t.setVar("shipinfo.weapons.normal",1);
			}
			else {
				t.setVar("shipinfo.weapons.good",1);
			}
		}

		// Kommunikation
		if( ship.getShip().getComm() < 50 ) {
			t.setVar("shipinfo.comm.bad",1);
		}
		else if( ship.getShip().getComm() < 100 ) {
			t.setVar("shipinfo.comm.normal",1);
		}
		else {
			t.setVar("shipinfo.comm.good",1);
		}

		// Sensoren
		if( ship.getShip().getSensors() < 50 ) {
			t.setVar("shipinfo.sensors.bad",1);
		}
		else if( ship.getShip().getSensors() < 100 ) {
			t.setVar("shipinfo.sensors.normal",1);
		}
		else {
			t.setVar("shipinfo.sensors.good",1);
		}

		if( !enemy ) {
			// Eigene Crew
			if( (ship.getShip().getCrew() > 0) && (shipType.getCrew()>0) ) {
				if( ship.getShip().getCrew() < shipType.getCrew()/2 ) {
					t.setVar("shipinfo.crew.bad",1);
				}
				else if( ship.getShip().getCrew() < shipType.getCrew() ) {
					t.setVar("shipinfo.crew.normal",1);
				}
				else {
					t.setVar("shipinfo.crew.good",1);
				}
			}

			// Energie
			if( ship.getShip().getEnergy() < shipType.getEps() / 4 ) {
				t.setVar("shipinfo.energy.bad",1);
			}
			else if( ship.getShip().getEnergy() < shipType.getEps() ) {
				t.setVar("shipinfo.energy.normal",1);
			}
			else {
				t.setVar("shipinfo.energy.good",1);
			}
		}

		// Offiziere
		Offizier offizier = ship.getShip().getOffizier();
		if( offizier != null ) {
			t.setVar(	"offizier.rang",	offizier.getRang(),
						"offizier.id",		offizier.getID(),
						"offizier.name",	Common._plaintitle(offizier.getName()) );
		}

		if( !enemy ) {
			// Waffen
			Map<String,Integer> weapons = shipType.getWeapons();
			Map<String,Integer> heat = ship.getWeaponHeat();
			Map<String,Integer> maxheat = shipType.getMaxHeat();

			for( String weaponName : weapons.keySet() ) {
				t.setVar(	"shipinfo.weapon.name",		Weapons.get().weapon(weaponName).getName(),
							"shipinfo.weapon.heat",     heat.getOrDefault(weaponName, 0),
							"shipinfo.weapon.maxheat",	maxheat.get(weaponName) );

				t.parse("shipinfo.weapons.list","shipinfo.weapons.listitem",true);
			}

			// Munition
			Cargo mycargo = ship.getCargo();
			
			t.setVar("shipinfo.ammo.list","");
			List<ItemCargoEntry<Munition>> itemlist = mycargo.getItemsOfType(Munition.class);
            for (ItemCargoEntry<Munition> item : itemlist) {
                if (item.getCount() > 0) {
					Munition itemobject = item.getItem();

                    t.setVar("ammo.image", itemobject.getPicture(),
                            "ammo.name", itemobject.getName(),
                            "ammo.count", item.getCount());
                    t.parse("shipinfo.ammo.list", "shipinfo.ammo.listitem", true);
                }
            }
			/*if(mycargo.hasResource(Resources.BATTERIEN))
			{
				t.setVar( "ammo.image", Cargo.getResourceImage(Resources.BATTERIEN),
						  "ammo.name",  Cargo.getResourceName(Resources.BATTERIEN),
						  "ammo.count", mycargo.getResourceCount(Resources.BATTERIEN));
				t.parse("shipinfo.ammo.list", "shipinfo.ammo.listitem", true);
			}*/

			// Einheiten
			UnitCargo unitcargo = ship.getUnits();

			unitcargo.echoUnitList(t, "shipinfo.units.list", "shipinfo.units.listitem");
		}

		t.parse(tag, "ship.info");
		t.stop_record();
		t.clear_record();
	}
	
	private boolean showMenu(TemplateEngine t, Battle battle, StringBuilder action ) throws IOException {
		User user = (User)this.getUser();
		
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();
		
		// TODO: evt sollte das hier in ne eigene Action ausgelagert werden?
		if( battle.getOwnLog(true).length() == 0 ) {
			if( battle.isCommander(user,battle.getOwnSide()) ) {
				if( battle.getTakeCommand(battle.getOwnSide()) != 0 ) {
					User auser = (User)getContext().getDB().get(User.class, battle.getTakeCommand(battle.getOwnSide()));
		
					t.setVar(	"battle.takecommand.ask",	1,
								"battle.takecommand.id",	battle.getTakeCommand(battle.getOwnSide()),
								"battle.takecommand.name",	"<span style=\"color:#000050\">"+auser.getProfileLink(Common._titleNoFormat(auser.getName()))+"</span>");
				}
				else if( battle.isReady(battle.getOwnSide()) ) {
					KSMenuHistoryAction historyobj = new KSMenuHistoryAction();

					historyobj.setController(this);
					
					historyobj.setText("<div style=\"text-align:center\"><span class=\"smallfont\" style=\"color:black\">Zug beendet</span></div><br /> "+
								 "Bitte warten sie bis ihr Gegner ebenfalls seinen Zug beendet hat. Die Runde wird dann automatisch beendet.");
					historyobj.showOK(false);
					
					historyobj.execute(t, battle);
				}
				else {
					t.setBlock("_ANGRIFF","menu.entry","menu");
					t.setBlock("_ANGRIFF","menu.entry.ask","none");
					t.setVar("global.showmenu", 1);
		
					// Ist das gegnerische Schiff zerstoert? Falls ja, dass Angriffsmenue deaktivieren
					if( ( action.toString().equals("attack") || action.toString().equals("groupattack") ) &&
						(enemyShip.hasFlag(BattleShipFlag.DESTROYED) || ownShip.hasFlag(BattleShipFlag.FLUCHT) || ownShip.getShip().isLanded()) ) {
						action.setLength(0);
					}
					
					if( action.length() == 0 ) {
						action.append("default");
					}
					
					if( menuActions.containsKey(action.toString()) ) {
						try {
							BasicKSMenuAction actionobj = menuActions.get(action.toString()).getDeclaredConstructor().newInstance();
							actionobj.setController(this);
							
							BasicKSAction.Result result = actionobj.execute(t, battle);
							if( result == BasicKSAction.Result.HALT ) {
								return false;
							}
							else if( result == BasicKSAction.Result.ERROR ) {
								action.setLength(0);
							}
						}
						catch( Exception e ) {
							addError("Kann Menue nicht aufrufen: "+e);
							log.error("Darstellung des KS-Menues "+action+" fehlgeschlagen", e);
							
							Common.mailThrowable(e, "KS-Menu-Error Schlacht "+battle.getId(), "Action: "+action+"\nownShip: "+ownShip.getId()+"\nenemyShip: "+enemyShip.getId());
						}
					}
				}
			}
			else {
				KSMenuHistoryAction historyobj = new KSMenuHistoryAction();

				historyobj.setController(this);
				
				if( (battle.getAlly(battle.getOwnSide()) != 0) && (battle.getTakeCommand(battle.getOwnSide()) == 0) && 
					(user.getAlly() != null) && (battle.getAlly(battle.getOwnSide()) == user.getAlly().getId()) ) {
					User auser = battle.getCommander(battle.getOwnSide());
					if( auser.getInactivity() >= 0 ) {
						historyobj.showTakeCommand(true);
					}
				}

                if(user.hasFlag(UserFlag.KS_TAKE_BATTLES) && (battle.getTakeCommand(battle.getOwnSide()) == 0))
                {
                    historyobj.showTakeCommand(true);
                }
					
				historyobj.setText("<div style=\"text-align:center\">Nur der Oberkommandierende einer Seite kann Befehle erteilen.</div>\n");
				historyobj.showOK(false);
					
				if( historyobj.execute(t, battle) == BasicKSAction.Result.HALT ) {
					return false;
				}
			}
		}
		
		if( battle.getOwnLog(true).length() > 0 ) {
			BBCodeParser bbcodeparser = BBCodeParser.getNewInstance();
			try {
				bbcodeparser.registerHandler( "tooltip", 2, "<a class=\"aloglink tooltip\" href=\"#\">$1<span class='ttcontent'>$2</span></a>" );
			}
			catch( Exception e ) {
				log.warn("Registrierung des BBCode-Handlers tooltip gescheitert", e);
			}
			
			t.setVar("battle.logoutput", bbcodeparser.parse(battle.getOwnLog(false)));
		}
		
		return true;
	}

	private static class GroupEntry {
		GroupEntry() {
			//EMPTY
		}
		
		int destcount;
		int hitcount;
		int fluchtcount;
		int joincount;
		int shotcount;
		int srcount;
		int fluchtnextcount;
		int mangelnahrungcount;
		int mangelreaktorcount;
	}

	/**
	 * Zeigt die GUI an.
	 * @throws IOException 
	 *
	 * @param addShipID Die ID eines Schiffes, dass der Schlacht beitreten soll (falls die Schlacht schon laeuft)
	 * @param enemyShipID Die ID des anzugreifenden Schiffs (Pflicht bei der Erstellung einer neuen Schlacht)
	 * @param forcejoin Die ID der Seite auf der der Schlachtbeitritt erfolgen soll (1 oder 2, 0 = automatische Seitenwahl; nur bei bereits laufender Schlacht)
	 * @param ksaction Die auszufuehrende Aktion in der Schlacht
	 * @param ownShipID Die ID des eigenen Schiffs (Pflicht bei der Erstellung einer neuen Schlacht)
	 * @param scan <code>own</code> oder Leerstring, falls die Scandaten eines eigenen Schiffes angezeigt werden sollen, sonst wird das gegnerische Schiff dargestellt
	 * @param ownshipgroup Die eigene ausgewaehlte Schiffsgruppe
	 * @param battleID Die ID der Schlacht (falls diese bereits laeuft)
	 * @param enemyshipgroup Die gegnerische ausgewaehlte Schiffsgruppe
	 * @param weapon Die ID der gerade ausgewaehlten Waffe
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(@UrlParam(name="ship") int ownShipID,
			@UrlParam(name="addship") int addShipID,
			@UrlParam(name="attack") int enemyShipID,
			String ksaction,
			@UrlParam(name="battle") int battleID,
			int forcejoin,
			String scan,
			String ownshipgroup,
			String enemyshipgroup,
			String weapon) throws IOException {
		User user = (User)getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);
		org.hibernate.Session db = getDB();

		if( ownShipID < 0 ) {
			ownShipID = 0;	
		}

		if( addShipID < 0 ) {
			addShipID = 0;	
		}

		if( enemyShipID < 0 ) {
			enemyShipID = 0;	
		}
		
		if( scan == null || scan.length() == 0 ) {
			scan = "own";
		}
		
		/*--------------------------------------------------------------
		
			Schlacht laden bzw. erstellen
		
		----------------------------------------------------------------*/
		
		boolean battleCreated = false;
		
		Battle battle;
		if( battleID == 0 ) {
			try {
				battle = schlachtErstellenService.erstelle(user, ownShipID, enemyShipID);
			}
			catch( IllegalArgumentException e ) {
				throw new ValidierungException(e.getMessage());
			}
			battleCreated = true;
		}
		else {
			battle = (Battle)db.get(Battle.class, battleID);
		}
		
		if( forcejoin != 0 ) {
			Ship jship = (Ship)db.get(Ship.class, addShipID);
			if( (jship == null) || (jship.getId() < 0) || (jship.getOwner() != user) ) {
				forcejoin = 0;
			}
		}
		
		if( (battle == null) || !battle.load(user, (Ship)db.get(Ship.class, ownShipID), (Ship)db.get(Ship.class, enemyShipID), forcejoin) ) {
			return null;
		}
		
		battle.setOwnShipGroup(ownshipgroup);
		battle.setEnemyShipGroup(enemyshipgroup);
		
		//
		// Schiff zur Schlacht hinzufgen
		//
		if( (!battleCreated) && addShipID != 0 && !battle.isGuest() ) {
			if( !battle.addShip( user.getId(), addShipID ) ) {
				// Wenn das Schiff offenbar von jemandem ausserhalb der Kriegfhrenden Allys stammt - Laden abbrechen bei fehlschlag
				if( forcejoin != 0 ) {
					return null;
				}
			}
		}

		/*--------------------------------------------------------------
			
			Actions
		
		----------------------------------------------------------------*/

		if( (ksaction.length() > 0) && actions.containsKey(ksaction) ) {
			try {
				BasicKSAction actionobj = actions.get(ksaction).getDeclaredConstructor().newInstance();
				actionobj.setController(this);
				
				if( actionobj.execute(t, battle) == BasicKSAction.Result.HALT ) {
					return null;
				}
				
				ksaction = "";
				t.setVar("global.ksaction","");
			}
			catch( Exception e ) {
				addError("Kann Aktion nicht ausfuehren: "+e);
				log.error("Ausfuehrung der KS-Aktion "+ksaction+" fehlgeschlagen", e);
				int curOwnShipID = -1;
				int curEnemyShipID = -1;
				try {
					curOwnShipID = battle.getOwnShip().getId();
				}
				catch( IndexOutOfBoundsException e2 ) {
					// EMPTY
				}
				
				try {
					curEnemyShipID = battle.getEnemyShip().getId();
				}
				catch( IndexOutOfBoundsException e2 ) {
					// EMPTY
				}
				
				Common.mailThrowable(e, "KS-Action-Error Schlacht "+battle.getId(), "Action: "+ksaction+"\nownShip: "+curOwnShipID+"\nenemyShip: "+curEnemyShipID);
			}
		}
		
		BattleShip enemyShip = battle.getEnemyShip();
		ShipTypeData enemyShipType = enemyShip.getTypeData();
		BattleShip ownShip = battle.getOwnShip();
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		User oUser = ownShip.getOwner();
		User eUser = enemyShip.getOwner();
		
		t.setVar(	"global.ksaction",			ksaction,
					"global.scan",				scan,
					"global.ownshipgroup",		battle.getOwnShipGroup(),
					"global.enemyshipgroup",	battle.getEnemyShipGroup(),
					"global.weapon",			weapon,
					"battle.id",				battle.getId(),
					"ownside.secondrow.stable",	battle.isSecondRowStable(battle.getOwnSide()),
					"enemyside.secondrow.stable",	battle.isSecondRowStable(battle.getEnemySide()),
					"ownship.id",				ownShip.getId(),
					"ownship.name",				ownShip.getName(),
					"ownship.type",				ownShip.getShip().getType(),
					"ownship.coordinates",		ownShip.getShip().getLocation().displayCoordinates(false),
					"ownship.type.name",		ownShipType.getNickname(),
					"ownship.type.image",		ownShipType.getPicture(),
					"ownship.owner.name",		Common._title(oUser.getName()),
					"ownship.owner.id",			ownShip.getOwner().getId(),
					"ownship.action.hit",		ownShip.hasFlag(BattleShipFlag.HIT),
					"ownship.action.flucht",	ownShip.hasFlag(BattleShipFlag.FLUCHT),
					"ownship.action.destroyed",	ownShip.hasFlag(BattleShipFlag.DESTROYED),
					"ownship.action.shot",		!battle.isGuest() && ownShip.hasFlag(BattleShipFlag.SHOT),
					"ownship.action.join",		ownShip.hasFlag(BattleShipFlag.JOIN),
					"ownship.action.secondrow",	ownShip.hasFlag(BattleShipFlag.SECONDROW),
					"ownship.action.fluchtnext", !battle.isGuest() && ownShip.hasFlag(BattleShipFlag.FLUCHTNEXT),
					"ownship.mangelnahrung", !battle.isGuest() ? ownShip.getShip().getStatus().contains("mangel_nahrung") : 0,
					"ownship.mangelreaktor",	(!battle.isGuest() ? ownShip.getShip().getStatus().contains("mangel_reaktor") : 0),
					"enemyship.id",				enemyShip.getId(),
					"enemyship.name",			enemyShip.getName(),
					"enemyship.type",			enemyShip.getShip().getType(),
					"enemyship.type.name",		enemyShipType.getNickname(),
					"enemyship.type.image",		enemyShipType.getPicture(),
					"enemyship.owner.name",		Common._title(eUser.getName()),
					"enemyship.owner.id",		enemyShip.getOwner().getId(),
					"enemyship.action.hit",		enemyShip.hasFlag(BattleShipFlag.HIT),
					"enemyship.action.flucht",	enemyShip.hasFlag(BattleShipFlag.FLUCHT),
					"enemyship.action.destroyed",	enemyShip.hasFlag(BattleShipFlag.DESTROYED),
					"enemyship.action.join",		enemyShip.hasFlag(BattleShipFlag.JOIN),
					"enemyship.action.secondrow",	enemyShip.hasFlag(BattleShipFlag.SECONDROW) );

		Nebel.Typ nebula = Nebel.getNebula(ownShip.getShip().getLocation());
		if( nebula == null || nebula.allowsScan() )
		{
			Location loc = ownShip.getShip().getLocation();
			t.setVar("ownship.location.x", loc.getX(),
					"ownship.location.y", loc.getY(),
					"ownship.location.system", loc.getSystem());
		}
		
		if( (battle.getOwnSide() == 0) && battle.hasFlag(BattleFlag.BLOCK_SECONDROW_0) ) {
			t.setVar("ownside.secondrow.blocked", 1);
		}
		else if( (battle.getOwnSide() == 1) && battle.hasFlag(BattleFlag.BLOCK_SECONDROW_1) ) {
			t.setVar("ownside.secondrow.blocked", 1);
		}
		
		if( (battle.getEnemySide() == 0) && battle.hasFlag(BattleFlag.BLOCK_SECONDROW_0) ) {
			t.setVar("enemyside.secondrow.blocked", 1);
		}
		else if( (battle.getEnemySide() == 1) && battle.hasFlag(BattleFlag.BLOCK_SECONDROW_1) ) {
			t.setVar("enemyside.secondrow.blocked", 1);
		}
		
		/*
			Das eigene ausgewaehlte Schiff
		*/
		String energy = "";
		if( !battle.isGuest() ) {
			//Energieanzeige im ersten eigenene Schiff
			if( ownShip.getShip().getEnergy() < ownShipType.getEps()/4 ) {
				energy = "<br />E: <span style=\'color:#ff0000\'>";
			} 
			else if( ownShip.getShip().getEnergy() < ownShipType.getEps() ) {
		  		energy = "<br />E: <span style=\'color:#ffff00\'>";
			} 
			else {
				energy = "<br />E: <span style=\'color:#00ff00\'>";
			}
		
			energy += ownShip.getShip().getEnergy()+"/"+ownShipType.getEps()+"</span>";
		}
		t.setVar("ownship.energy",energy);
		
		/*---------------------------------------------------------
		
			Menue
		
		-----------------------------------------------------------*/

		StringBuilder actionBuilder = new StringBuilder(ksaction);
		if( !this.showMenu( t, battle, actionBuilder ) ) {
			return null;
		}
		
		ksaction = actionBuilder.toString();
		t.setVar("global.ksaction", ksaction);
		
		t.setBlock("_ANGRIFF","ship.info","none");
		t.setBlock("ship.info","shipinfo.ammo.listitem","shipinfo.ammo.list");
		t.setBlock("ship.info","shipinfo.weapons.listitem","shipinfo.weapons.list");
		t.setBlock("ship.info","shipinfo.units.listitem","shipinfo.units.list");
		showInfo(t, "ownship.info", ownShip, battle.isGuest(), "Own", scan.equals("own") );
		showInfo(t, "enemyship.info", enemyShip, true, "Enemy", scan.equals("enemy") );
		
		if( scan.equals("own") ) {
			t.setVar("infobox.active", "Own");
		}
		else {
			t.setVar("infobox.active", "Enemy");
		}
		
		/*---------------------------------------------------------
		
			Schiffsliste
		
		-----------------------------------------------------------*/
		
		t.setBlock("_ANGRIFF","ships.typelist.item","ships.typelist.none");
		t.setBlock("_ANGRIFF","ownShips.listitem","ownShips.list");
		t.setBlock("_ANGRIFF","enemyShips.listitem","enemyShips.list");
		
		int ownGroupCount = 0;
		int enemyGroupCount = 0;
		
		for( Integer stCount : battle.getShipTypeCount(battle.getOwnSide()).values() ) {
			ownGroupCount += stCount / SHIPGROUPSIZE;
		}
		
		for( Integer stCount : battle.getShipTypeCount(battle.getEnemySide()).values() ) {
			enemyGroupCount += stCount / SHIPGROUPSIZE;
		}
		
		int grouptype = 0;
		int groupoffset = 0;
		int grouptypecount = 0;
		
		boolean showgroups = (ownGroupCount >= 2) || (battle.getOwnShips().size() >= 10);
		if( showgroups && (battle.getOwnShipGroup().length() > 0) ) {
			String[] tmp = StringUtils.split(battle.getOwnShipGroup(), ':');
			grouptype = Integer.parseInt(tmp[0]);
			groupoffset = Integer.parseInt(tmp[1]);
			
			groupoffset *= SHIPGROUPSIZE;
			if( groupoffset > battle.getOwnShipTypeCount(grouptype) ) {
				groupoffset = 0;
			}
			grouptypecount = 0;
		}
		
		/*
			Eigene Schiffe auflisten
		*/
		if( (grouptype > 0) || !showgroups ) {
			int pos = groupoffset;
			energy = "";
			boolean firstEntry = true;
		
			List<BattleShip> ownShips = battle.getOwnShips();
			for( int i=0; i < ownShips.size(); i++ ) {
				BattleShip aship = ownShips.get(i);
				
				t.start_record();
		
				if( !showgroups && (aship == ownShip) ) {
					continue;
				}
				if( showgroups && (aship.getShip().getType() != grouptype) ) {
					continue;
				}
				if(battle.isGuest() && aship.getShip().isLanded() ) {
					continue;
				}
				
				if( showgroups ) {
					grouptypecount++;
					if( grouptypecount <= groupoffset ) {
						continue;
					}
					if( grouptypecount > groupoffset+SHIPGROUPSIZE ) {
						continue;
					}
				}
		
				ShipTypeData aShipType = aship.getTypeData();
				pos++;
	
				// Energiestatus anzeigen, wenn der User kein Gast ist
				if( !battle.isGuest() ) {
					if( aship.getShip().getEnergy() < aShipType.getEps()/4 ) {
						energy = "<br />E: <span style=\'color:#ff0000\'>";
					}
					else if( aship.getShip().getEnergy() < aShipType.getEps() ) {
						energy =  "<br />E: <span style=\'color:#ffff00\'>";
					}
					else {
						energy =  "<br />E: <span style=\'color:#00ff00\'>";
					}
	
					energy += aship.getShip().getEnergy()+"/"+aShipType.getEps()+"</span>";
				}
				
				// Ist das Schiff gedockt?
				if(aship.getShip().isDocked() || aship.getShip().isLanded())
				{
					int shipid = aship.getShip().getBaseShip().getId();

                    for (BattleShip ownShip1 : ownShips) {
                        if (ownShip1.getId() == shipid) {
                            t.setVar("ship.docked.name", ownShip1.getName(),
                                    "ship.docked.id", shipid);

                            break;
                        }
                    }
				}
	
				User aUser = aship.getOwner();
				
				t.setVar(	"ship.id",				aship.getId(),
							"ship.name",			aship.getName(),
							"ship.type",			aship.getShip().getType(),
							"ship.type.name",		aShipType.getNickname(),
							"ship.type.image",		aShipType.getPicture(),
							"ship.owner.name",		Common._title(aUser.getName()),
							"ship.owner.id",		aship.getOwner().getId(),
							"ship.energy",			energy,
							"ship.active",			(aship == ownShip),
							"ship.action.hit",		aship.hasFlag(BattleShipFlag.HIT),
							"ship.action.flucht",	aship.hasFlag(BattleShipFlag.FLUCHT),
							"ship.action.destroyed",	aship.hasFlag(BattleShipFlag.DESTROYED),
							"ship.action.join",			aship.hasFlag(BattleShipFlag.JOIN),
							"ship.action.secondrow",	aship.hasFlag(BattleShipFlag.SECONDROW),
							"ship.action.fluchtnext", !battle.isGuest() && aship.hasFlag(BattleShipFlag.FLUCHTNEXT),
							"ship.action.shot", !battle.isGuest() && aship.hasFlag(BattleShipFlag.SHOT),
                            "ship.mangelnahrung",       !battle.isGuest() && (aship.getShip().getStatus().contains("mangel_nahrung")),
							"ship.mangelreaktor",       !battle.isGuest() && aship.getShip().getStatus().contains("mangel_reaktor"));
	

				if( !firstEntry && showgroups && ((pos >= battle.getOwnShipTypeCount(grouptype)) || (pos == groupoffset+SHIPGROUPSIZE)) ) {
					t.setVar("ship.showback",1);
				}

				if( firstEntry ) {
					firstEntry = false;
					if( showgroups ){
						t.setVar("ship.firstEntry",1);
					}
				}
				else {
					t.setVar("ship.addline",1);
				}
				t.parse("ownShips.list","ownShips.listitem",true);
		
				t.stop_record();
				t.clear_record();
			}
		}
		/*
			Eigene Schiffsgruppen anzeigen
		*/
		else {
			boolean firstEntry = true;
		
			Map<String,GroupEntry> groupdata = new HashMap<>();
			Map<Integer,Integer> shiptypegroupcount = new HashMap<>();
			
			List<BattleShip> ownShips = battle.getOwnShips();
            for (BattleShip aship : ownShips)
            {
                Common.safeIntInc(shiptypegroupcount, aship.getShip().getType());

                groupoffset = (shiptypegroupcount.get(aship.getShip().getType()) - 1) / SHIPGROUPSIZE;

                final String key = aship.getShip().getType() + ":" + groupoffset;

                GroupEntry data;
                if (!groupdata.containsKey(key))
                {
                    data = new GroupEntry();
                    groupdata.put(key, data);
                }
                else
                {
                    data = groupdata.get(key);
                }

                if (aship.hasFlag(BattleShipFlag.DESTROYED)) {
                    data.destcount++;
                }
                if (aship.hasFlag(BattleShipFlag.HIT)) {
                    data.hitcount++;
                }
                if (aship.hasFlag(BattleShipFlag.FLUCHT)) {
                    data.fluchtcount++;
                }
                if (aship.hasFlag(BattleShipFlag.JOIN)) {
                    data.joincount++;
                }
                if (aship.hasFlag(BattleShipFlag.SECONDROW)) {
                    data.srcount++;
                }
                if (!battle.isGuest()) {
                    if (aship.hasFlag(BattleShipFlag.SHOT)) {
                        data.shotcount++;
                    }
                    if (aship.hasFlag(BattleShipFlag.FLUCHTNEXT)) {
                        data.fluchtnextcount++;
                    }
                    if (aship.getShip().getStatus().contains("mangel_nahrung")) {
                        data.mangelnahrungcount++;
                    }
                    if (aship.getShip().getStatus().contains("mangel_reaktor")) {
                        data.mangelreaktorcount++;
                    }
                }
            }
			
			Map<Integer,Integer> shipTypes = battle.getShipTypeCount(battle.getOwnSide());
			for( Map.Entry<Integer, Integer> entry : shipTypes.entrySet() ) {
				int stid = entry.getKey();
				
				if( entry.getValue() <= 0 ) { 
					continue;
				}
				if( !shiptypegroupcount.containsKey(stid) ) {
					continue;
				}
				for( int i=0; i <= shiptypegroupcount.get(stid)/SHIPGROUPSIZE; i++ ) {
					int count = shiptypegroupcount.get(stid)-i*SHIPGROUPSIZE;
					if( count > SHIPGROUPSIZE ) {
						count = SHIPGROUPSIZE;
					}
					if( count <= 0 ) {
						continue;	
					}				
					
					final String key = stid+":"+i;
					
					ShipTypeData shiptype = Ship.getShipType(stid);
					GroupEntry data = groupdata.get(key);
					
					t.setVar(	"shiptypelist.count",		count,
								"shiptypelist.name",		shiptype.getNickname(),
								"shiptypelist.groupid",		key,
								"shiptypelist.id",			stid,
								"shiptypelist.image",		shiptype.getPicture(),
								"shiptypelist.side",		"own",
								"shiptypelist.otherside",	"enemy",
								"shiptypelist.otherside.id",	battle.getEnemyShipGroup(),
								"shiptypelist.destcount",	data.destcount,
								"shiptypelist.hitcount",	data.hitcount,
								"shiptypelist.fluchtcount",	data.fluchtcount,
								"shiptypelist.joincount",	data.joincount,
								"shiptypelist.shotcount",	data.shotcount,
								"shiptypelist.fluchtnextcount",	data.fluchtnextcount,
								"shiptypelist.secondrowcount",	data.srcount,
								"shiptypelist.secondrowstatus",	count-data.srcount,
								"shiptypelist.mangelnahrungcount",	data.mangelnahrungcount,
								"shiptypelist.mangelreaktorcount",	data.mangelreaktorcount );
										
					if( firstEntry ) {
						firstEntry = false;
					}
					else {
						t.setVar("shiptypelist.addline",1);
					}
		
					t.parse("ownShips.list","ships.typelist.item",true);
				}
			}
		}
		
		t.setVar(	"shiptypelist.addline",				0,
					"shiptypelist.mangelreaktorcount",	0,
					"shiptypelist.mangelnahrungcount",	0,
					"shiptypelist.hitcount",			0,
					"shiptypelist.shotcount",			0,
					"shiptypelist.secondrowcount",		0,
					"shiptypelist.fluchtnextcount",		0,
					"ship.mangelnahrung",				0,
					"ship.mangelreaktor",				0 );

		groupoffset = 0;
		grouptypecount = 0;
		grouptype = 0;
		
		showgroups = (enemyGroupCount >= 2) || (battle.getEnemyShips().size() >= 10);
		if( showgroups && (battle.getEnemyShipGroup().length() > 0) ) {
			String[] tmp = StringUtils.split(battle.getEnemyShipGroup(), ':');
			grouptype = Integer.parseInt(tmp[0]);
			groupoffset = Integer.parseInt(tmp[1]);
			
			groupoffset *= SHIPGROUPSIZE;
			if( groupoffset > battle.getEnemyShipTypeCount(grouptype) ) {
				groupoffset = 0;
			}
			grouptypecount = 0;
		}
		
		/*
		 * Gegnerische Schiffe aufliste
		 */
		if( (grouptype > 0) || !showgroups ) {
			int pos = groupoffset;
			boolean firstEntry = true;
		
			List<BattleShip> enemyShips = battle.getEnemyShips();
			for( int i=0; i < enemyShips.size(); i++ ) {
				BattleShip aship = enemyShips.get(i);
				
				t.start_record();
		
				if( !showgroups && (aship == enemyShip) ) {
					continue;
				}
				if( showgroups && (aship.getShip().getType() != grouptype) ) {
					continue;
				}
				
				// Gelandete Schiffe nicht anzeigen
				if( aship.getShip().isLanded() ) {
					continue;
				}
				
				if( showgroups ) {
					grouptypecount++;
					if( grouptypecount <= groupoffset ) {
						continue;
					}
					if( grouptypecount > groupoffset+SHIPGROUPSIZE ) {
						continue;
					}
				}
		
				ShipTypeData aShipType = aship.getTypeData();
				
				pos++;
	
				// Ist das Schiff gedockt?
				if( aship.getShip().isDocked() ) {
					int shipid = aship.getShip().getBaseShip().getId();

                    for (BattleShip enemyShip1 : enemyShips) {
                        if (enemyShip1.getId() == shipid) {
                            t.setVar("ship.docked.name", enemyShip1.getName());

                            break;
                        }
                    }
				}
				
				User aUser = aship.getOwner();
	
				t.setVar(	"ship.id",				aship.getId(),
							"ship.name",			aship.getName(),
							"ship.type",			aship.getShip().getType(),
							"ship.type.name",		aShipType.getNickname(),
							"ship.type.image",		aShipType.getPicture(),
							"ship.owner.name",		Common._title(aUser.getName()),
							"ship.owner.id",		aship.getOwner().getId(),
							"ship.active",			(aship == enemyShip),
							"ship.action.hit",		aship.hasFlag(BattleShipFlag.HIT),
							"ship.action.flucht",	aship.hasFlag(BattleShipFlag.FLUCHT),
							"ship.action.join",		aship.hasFlag(BattleShipFlag.JOIN),
							"ship.action.secondrow",	aship.hasFlag(BattleShipFlag.SECONDROW),
							"ship.action.destroyed",	aship.hasFlag(BattleShipFlag.DESTROYED) );
	

				if( !firstEntry && showgroups && ((pos >= battle.getEnemyShipTypeCount(grouptype)) || (pos == groupoffset+SHIPGROUPSIZE)) ) {
					t.setVar("ship.showback",1);
				}

				if( firstEntry ) {
					firstEntry = false;
					if( showgroups ){
						t.setVar("ship.firstEntry",1);
					}
				}
				else {
					t.setVar("ship.addline",1);
				}
				t.parse("enemyShips.list", "enemyShips.listitem", true);

				t.stop_record();
				t.clear_record();
			}
		}
		/*
		 * Gegnerische Schiffsgruppen anzeigen
		 */
		else {
			boolean firstEntry = true;
		
			Map<String,GroupEntry> groupdata = new HashMap<>();
		
			Map<Integer,Integer> shiptypegroupcount = new HashMap<>();
		
			List<BattleShip> enemyShips = battle.getEnemyShips();
            for (BattleShip aship : enemyShips) {
                if (aship.getShip().isLanded()) {
                    continue;
                }

                Common.safeIntInc(shiptypegroupcount, aship.getShip().getType());

                groupoffset = (shiptypegroupcount.get(aship.getShip().getType()) - 1) / SHIPGROUPSIZE;

                final String key = aship.getShip().getType() + ":" + groupoffset;
                GroupEntry data;
                if (!groupdata.containsKey(key))
                {
                    data = new GroupEntry();
                    groupdata.put(key, data);
                }
                else
                {
                    data = groupdata.get(key);
                }

                if (aship.hasFlag(BattleShipFlag.DESTROYED)) {
                    data.destcount++;
                }
                if (aship.hasFlag(BattleShipFlag.HIT)) {
                    data.hitcount++;
                }
                if (aship.hasFlag(BattleShipFlag.FLUCHT)) {
                    data.fluchtcount++;
                }
                if (aship.hasFlag(BattleShipFlag.JOIN)) {
                    data.joincount++;
                }
                if (aship.hasFlag(BattleShipFlag.SECONDROW)) {
                    data.srcount++;
                }
            }
		
			Map<Integer,Integer> shipTypes = battle.getShipTypeCount(battle.getEnemySide());
			for( Map.Entry<Integer, Integer> entry: shipTypes.entrySet() ) {
				int stid = entry.getKey();
				if( entry.getValue() <= 0 ) { 
					continue;
				}
				if( !shiptypegroupcount.containsKey(stid) ) {
					continue;
				}
				for( int i=0; i <= shiptypegroupcount.get(stid)/SHIPGROUPSIZE; i++ ) {
					int count = shiptypegroupcount.get(stid)-i*SHIPGROUPSIZE;
					if( count > SHIPGROUPSIZE ) {
						count = SHIPGROUPSIZE;
					}
			
					if( count <= 0 ) {
						continue;	
					}
			
					final String key = stid+":"+i;
					ShipTypeData shiptype = Ship.getShipType(stid);
					
					GroupEntry data = groupdata.get(key);
					
					t.setVar(	"shiptypelist.count",		count,
								"shiptypelist.name",		shiptype.getNickname(),
								"shiptypelist.id",			stid,
								"shiptypelist.groupid",		stid+":"+i,
								"shiptypelist.image",		Ship.getShipType(stid).getPicture(),
								"shiptypelist.side",		"enemy",
								"shiptypelist.otherside",	"own",
								"shiptypelist.otherside.id",	battle.getOwnShipGroup(),
								"shiptypelist.destcount",		data.destcount,
								"shiptypelist.hitcount",		data.hitcount,
								"shiptypelist.joincount",		data.joincount,
								"shiptypelist.secondrowcount",	data.srcount,
								"shiptypelist.secondrowstatus",	count-data.srcount,
								"shiptypelist.fluchtcount",		data.fluchtcount );
					
					if( firstEntry ) {
						firstEntry = false;
					}
					else {
						t.setVar("shiptypelist.addline",1);
					}
		
					t.parse("enemyShips.list","ships.typelist.item",true);
				}
			}
		}
		
		/*
			Infos (APs, Runde, Gegnerischer Kommandant)
		*/
		
		if( !battle.isCommander(user,battle.getOwnSide()) ) {
			User auser = battle.getCommander(battle.getOwnSide());
			t.setVar(	"user.commander",		0,
						"battle.owncom.name",	auser.getProfileLink(),
						"battle.owncom.ready",	battle.isReady(battle.getOwnSide()) );
		} 
		else {
			t.setVar(	"user.commander",		1 );
		}
		if( !battle.isReady(battle.getOwnSide()) ) {
			t.setVar("battle.turn.own",1);
		}
		
		int enemySide = battle.getOwnSide() == 1 ? 0 : 1;
		User auser = battle.getCommander(enemySide);
		t.setVar(	"battle.enemycom.name",		auser.getProfileLink(),
					"battle.enemycom.ready",	battle.isReady(battle.getEnemySide()) );

		return t;
	}
}
