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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.ks.BasicKSAction;
import net.driftingsouls.ds2.server.modules.ks.BasicKSMenuAction;
import net.driftingsouls.ds2.server.modules.ks.KSAttackAction;
import net.driftingsouls.ds2.server.modules.ks.KSCheatRegenerateEnemyAction;
import net.driftingsouls.ds2.server.modules.ks.KSCheatRegenerateOwnAction;
import net.driftingsouls.ds2.server.modules.ks.KSDischargeBatteriesAllAction;
import net.driftingsouls.ds2.server.modules.ks.KSDischargeBatteriesClassAction;
import net.driftingsouls.ds2.server.modules.ks.KSDischargeBatteriesSingleAction;
import net.driftingsouls.ds2.server.modules.ks.KSEndBattleCivilAction;
import net.driftingsouls.ds2.server.modules.ks.KSEndTurnAction;
import net.driftingsouls.ds2.server.modules.ks.KSFluchtAllAction;
import net.driftingsouls.ds2.server.modules.ks.KSFluchtClassAction;
import net.driftingsouls.ds2.server.modules.ks.KSFluchtSingleAction;
import net.driftingsouls.ds2.server.modules.ks.KSKapernAction;
import net.driftingsouls.ds2.server.modules.ks.KSLeaveSecondRowAction;
import net.driftingsouls.ds2.server.modules.ks.KSMenuAttackAction;
import net.driftingsouls.ds2.server.modules.ks.KSMenuAttackMuniSelectAction;
import net.driftingsouls.ds2.server.modules.ks.KSMenuBatteriesAction;
import net.driftingsouls.ds2.server.modules.ks.KSMenuBattleConsignAction;
import net.driftingsouls.ds2.server.modules.ks.KSMenuCheatsAction;
import net.driftingsouls.ds2.server.modules.ks.KSMenuDefaultAction;
import net.driftingsouls.ds2.server.modules.ks.KSMenuFluchtAction;
import net.driftingsouls.ds2.server.modules.ks.KSMenuHistoryAction;
import net.driftingsouls.ds2.server.modules.ks.KSMenuOtherAction;
import net.driftingsouls.ds2.server.modules.ks.KSMenuShieldsAction;
import net.driftingsouls.ds2.server.modules.ks.KSNewCommanderAction;
import net.driftingsouls.ds2.server.modules.ks.KSRegenerateShieldsAllAction;
import net.driftingsouls.ds2.server.modules.ks.KSRegenerateShieldsClassAction;
import net.driftingsouls.ds2.server.modules.ks.KSRegenerateShieldsSingleAction;
import net.driftingsouls.ds2.server.modules.ks.KSSecondRowAction;
import net.driftingsouls.ds2.server.modules.ks.KSSecondRowAttackAction;
import net.driftingsouls.ds2.server.modules.ks.KSSecondRowEngageAction;
import net.driftingsouls.ds2.server.modules.ks.KSStopTakeCommandAction;
import net.driftingsouls.ds2.server.modules.ks.KSTakeCommandAction;
import net.driftingsouls.ds2.server.modules.ks.KSUndockAllAction;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Das UI fuer Schlachten.
 * @author Christopher Jung
 * 
 * @urlparam Integer ship Die ID eines eigenen ausgewaehlten Schiffes (Pflicht bei der Erstellung einer neuen Schlacht)
 * @urlparam Integer addship Die ID eines Schiffes, welches beitreten soll
 * @urlparam Integer attack Die ID eines Schiffes, welches angegriffen werden soll (Pflicht bei der Erstellung einer neuen Schlacht)
 * @urlparam String ksaction Die auszufuehrende KS-Aktion
 * @urlparam Integer battle Die ID der Schlacht. Wenn keine angegeben ist wird eine neue erstellt
 * @urlparam Integer forcejoin Die Seite auf der ein Beitritt "erzwungen" werden soll
 * @urlparam String scan Die im Scan anzuzeigende Seite (<code>own</code> oder <code>enemy</code>)
 *
 */
public class AngriffController extends TemplateGenerator {
	private static final Log log = LogFactory.getLog(AngriffController.class);
	
	private static final int SHIPGROUPSIZE = 50;
	
	private static final Map<String,Class<? extends BasicKSAction>> ACTIONS = new HashMap<String,Class<? extends BasicKSAction>>();
	static {
		ACTIONS.put("flucht_single", KSFluchtSingleAction.class);
		ACTIONS.put("flucht_all", KSFluchtAllAction.class);
		ACTIONS.put("flucht_class", KSFluchtClassAction.class);
		ACTIONS.put("attack2", KSAttackAction.class);
		ACTIONS.put("kapern", KSKapernAction.class);
		ACTIONS.put("secondrow", KSSecondRowAction.class);
		ACTIONS.put("leavesecondrow", KSLeaveSecondRowAction.class);
		ACTIONS.put("secondrowengage", KSSecondRowEngageAction.class);
		ACTIONS.put("secondrowattack", KSSecondRowAttackAction.class);
		//ACTIONS.put("endbattleequal", KSEndBattleEqualAction.class);
		ACTIONS.put("endturn", KSEndTurnAction.class);
		ACTIONS.put("alleabdocken", KSUndockAllAction.class);
		ACTIONS.put("new_commander2", KSNewCommanderAction.class);
		ACTIONS.put("take_command", KSTakeCommandAction.class);
		ACTIONS.put("stop_take_command", KSStopTakeCommandAction.class);
		ACTIONS.put("endbattle", KSEndBattleCivilAction.class);
		ACTIONS.put("shields_single", KSRegenerateShieldsSingleAction.class);
		ACTIONS.put("shields_all", KSRegenerateShieldsAllAction.class);
		ACTIONS.put("shields_class", KSRegenerateShieldsClassAction.class);
		ACTIONS.put("batterien_single", KSDischargeBatteriesSingleAction.class);
		ACTIONS.put("batterien_all", KSDischargeBatteriesAllAction.class);
		ACTIONS.put("batterien_class", KSDischargeBatteriesClassAction.class);
		if( Configuration.getIntSetting("ENABLE_CHEATS") != 0 ) {
			ACTIONS.put("cheat_regenerate", KSCheatRegenerateOwnAction.class);
			ACTIONS.put("cheat_regenerateenemy", KSCheatRegenerateEnemyAction.class);
		}
	}
	
	private static final Map<String,Class<? extends BasicKSMenuAction>> MENUACTIONS = new HashMap<String,Class<? extends BasicKSMenuAction>>();
	static {
		MENUACTIONS.put("attack", KSMenuAttackAction.class);
		MENUACTIONS.put("attack_select", KSMenuAttackMuniSelectAction.class);
		MENUACTIONS.put("batterien", KSMenuBatteriesAction.class);
		MENUACTIONS.put("default", KSMenuDefaultAction.class);
		MENUACTIONS.put("flucht", KSMenuFluchtAction.class);
		MENUACTIONS.put("history", KSMenuHistoryAction.class);
		MENUACTIONS.put("new_commander", KSMenuBattleConsignAction.class);
		MENUACTIONS.put("other", KSMenuOtherAction.class);
		MENUACTIONS.put("shields", KSMenuShieldsAction.class);
		if( Configuration.getIntSetting("ENABLE_CHEATS") != 0 ) {
			MENUACTIONS.put("cheats", KSMenuCheatsAction.class);
		}
	}
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public AngriffController(Context context) {
		super(context);
		
		parameterNumber("ship");
		parameterNumber("addship");
		parameterNumber("attack");
		parameterString("ksaction");
		parameterNumber("battle");
		parameterNumber("forcejoin");
		parameterString("scan");
		
		setTemplate("angriff.html");
		
		setPageTitle("Schlacht");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		return true;
	}
	
	private void showInfo(String tag, BattleShip ship, boolean enemy, String jscriptid, boolean show) {
		TemplateEngine t = getTemplateEngine();
		
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
					"shipinfo.type.maxmarines",	shipType.getMarines(),
					"shipinfo.type.weapons",	shipType.isMilitary(),
					"shipinfo.hull",			Common.ln(ship.getShip().getHull()),
					"shipinfo.ablativeArmor", 	Common.ln(ship.getShip().getAblativeArmor()),
					"shipinfo.panzerung",		(int)Math.round(shipType.getPanzerung()*ship.getShip().getHull()/(double)shipType.getHull()),
					"shipinfo.shields",			Common.ln(ship.getShip().getShields()),
					"shipinfo.nocrew",			(ship.getShip().getCrew() == 0) && (shipType.getCrew()>0),
					"shipinfo.heat",			ship.getShip().getHeat(),
					"shipinfo.energy",			ship.getShip().getEnergy(),
					"shipinfo.crew",			ship.getShip().getCrew(),
					"shipinfo.marines",			ship.getShip().getMarines(),
					"shipinfo.engine",			ship.getShip().getEngine(),
					"shipinfo.weapons",			ship.getShip().getWeapons(),
					"shipinfo.comm",			ship.getShip().getComm(),
					"shipinfo.sensors",			ship.getShip().getSensors(),
					"shipinfo.showtmp",			(ship.getAction() & Battle.BS_HIT) != 0 || (ship.getAction() & Battle.BS_DESTROYED) != 0,
					"shipinfo.tmp.hull",		((ship.getAction() & Battle.BS_DESTROYED) != 0 ? 0 : Common.ln(ship.getHull()) ),
					"shipinfo.tmp.shields",		((ship.getAction() & Battle.BS_DESTROYED) != 0 ? 0 : Common.ln(ship.getShields())),
					"shipinfo.tmp.engine",		ship.getEngine(),
					"shipinfo.tmp.weapons",		ship.getWeapons(),
					"shipinfo.tmp.comm",		ship.getComm(),
					"shipinfo.tmp.sensors",		ship.getSensors(),
					"shipinfo.tmp.panzerung",	(int)Math.round(shipType.getPanzerung()*ship.getHull()/(double)shipType.getHull()), 
					"shipinfo.tmp.ablativeArmor", ((ship.getAction() & Battle.BS_DESTROYED) != 0 ? 0 : Common.ln(ship.getAblativeArmor())));

		// Anzahl
		if( shipType.getShipCount() > 1 ) {
			t.setVar(	"shipinfo.count",		ship.getCount(),
						"shipinfo.tmp.count",	ship.getNewCount() );
		}
		
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
		Offizier offizier = Offizier.getOffizierByDest('s', ship.getId());
		if( offizier != null ) {
			t.setVar(	"offizier.rang",	offizier.getRang(),
						"offizier.id",		offizier.getID(),
						"offizier.name",	Common._plaintitle(offizier.getName()) );
		}

		if( !enemy ) {
			// Waffen
			Map<String,String> weapons = Weapons.parseWeaponList(shipType.getWeapons());
			Map<String,String> heat = Weapons.parseWeaponList(ship.getWeaponHeat());
			Map<String,String> maxheat = Weapons.parseWeaponList(shipType.getMaxHeat());

			for( String weaponName : weapons.keySet() ) {
				if( shipType.getShipCount() > ship.getCount() ) {
					maxheat.put(weaponName, Integer.toString(
							(int)(Integer.parseInt(maxheat.get(weaponName))*ship.getCount()/(double)shipType.getShipCount())
							));
				}

				t.setVar(	"shipinfo.weapon.name",		Weapons.get().weapon(weaponName).getName(),
							"shipinfo.weapon.heat",		heat.containsKey(weaponName) ? Integer.parseInt(heat.get(weaponName)) : 0,
							"shipinfo.weapon.maxheat",	maxheat.get(weaponName) );

				t.parse("shipinfo.weapons.list","shipinfo.weapons.listitem",true);
			}

			// Munition
			Cargo mycargo = ship.getCargo();
			
			t.setVar("shipinfo.ammo.list","");
			List<ItemCargoEntry> itemlist = mycargo.getItemsWithEffect( ItemEffect.Type.AMMO );
			for( int i=0; i < itemlist.size(); i++ ) {
				ItemCargoEntry item = itemlist.get(i);
				
				if( item.getCount() > 0 ) {
					Item itemobject = item.getItemObject();
					
					t.setVar(	"ammo.image",	itemobject.getPicture(),
								"ammo.name",	itemobject.getName(),
								"ammo.count",	item.getCount() );
					t.parse("shipinfo.ammo.list","shipinfo.ammo.listitem",true);
				}
			}
		}

		t.parse(tag, "ship.info");
		t.stop_record();
		t.clear_record();
	}
	
	private boolean showMenu( Battle battle, StringBuilder action ) throws IOException {
		User user = (User)this.getUser();
		TemplateEngine t = this.getTemplateEngine();		
		
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();
		
		// TODO: evt sollte das hier in ne eigene Action ausgelagert werden?
		if( action.toString().equals("showbattlelog") && (battle.getComMessageBuffer(battle.getOwnSide()).length() > 0) && battle.isCommander(user,battle.getOwnSide()) ) {
			BBCodeParser bbcodeparser = BBCodeParser.getNewInstance();
			try {
				bbcodeparser.registerHandler( "tooltip", 2, "<a onmouseover=\"return overlib('$2',TIMEOUT,0,DELAY,400,WIDTH,100,TEXTFONTCLASS,'smallTooltip');\" onmouseout=\"return nd();\" class=\"aloglink\" href=\"#\">$1</a>" );
			}
			catch( Exception e ) {
				log.warn("Registrierung des BBCode-Handlers tooltip gescheitert", e);
			}
		
			String msgbuffer = battle.getComMessageBuffer(battle.getOwnSide());
			msgbuffer = StringUtils.replace(msgbuffer, "<![CDATA[", "");
			msgbuffer = StringUtils.replace(msgbuffer, "]]>","");
			msgbuffer = StringUtils.replace(msgbuffer, "<br />", "\n");
			msgbuffer = Common._stripHTML(msgbuffer);
			
			t.setVar( "battle.msg", StringUtils.replace(bbcodeparser.parse(msgbuffer), "\n", "<br />") );
		
			battle.clearComMessageBuffer(battle.getOwnSide());
			
			t.setVar(	"global.ksaction",	"",
						"battle.msginfo",	"" );
			action.setLength(0);
		}
		else if( battle.getOwnLog(true).length() == 0 ) {
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
					
					historyobj.execute(battle);
				}
				else {
					t.setBlock("_ANGRIFF","menu.entry","menu");
					t.setBlock("_ANGRIFF","menu.entry.ask","none");
					t.setVar("global.showmenu", 1);
		
					// Ist das gegnerische Schiff zerstoert? Falls ja, dass Angriffsmenue deaktivieren
					if( ( action.toString().equals("attack") ) &&
						((enemyShip.getAction() & Battle.BS_DESTROYED) != 0 || (ownShip.getAction() & Battle.BS_FLUCHT) != 0 || (ownShip.getDocked().length() > 0 && ownShip.getDocked().charAt(0) == 'l')) ) {
						action.setLength(0);
					}
					
					if( action.length() == 0 ) {
						action.append("default");
					}
					
					if( MENUACTIONS.containsKey(action.toString()) ) {
						try {
							BasicKSMenuAction actionobj = MENUACTIONS.get(action.toString()).newInstance();
							actionobj.setController(this);
							
							int result = actionobj.execute(battle);
							if( result == BasicKSAction.RESULT_HALT ) {
								return false;
							}
							else if( result == BasicKSAction.RESULT_ERROR ) {
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
					
				historyobj.setText("<div style=\"text-align:center\">Nur der Oberkommandierende einer Seite kann Befehle erteilen.</div>\n");
				historyobj.showOK(false);
					
				if( historyobj.execute(battle) == BasicKSAction.RESULT_HALT ) {
					return false;
				}
			}
		}
		
		if( battle.getOwnLog(true).length() > 0 ) {
			BBCodeParser bbcodeparser = BBCodeParser.getNewInstance();
			try {
				bbcodeparser.registerHandler( "tooltip", 2, "<a onmouseover=\"return overlib('$2',TIMEOUT,0,DELAY,400,WIDTH,100,TEXTFONTCLASS,'smallTooltip');\" onmouseout=\"return nd();\" class=\"aloglink\" href=\"#\">$1</a>" );
			}
			catch( Exception e ) {
				log.warn("Registrierung des BBCode-Handlers tooltip gescheitert", e);
			}
			
			t.setVar("battle.logoutput", bbcodeparser.parse(battle.getOwnLog(false)));
		}
		
		return true;
	}
	
	private String modifyShipImg( ShipTypeData shiptype, int count ) {
		if( shiptype.getShipCount() > 1 ) {
			return StringUtils.replace(shiptype.getPicture(), ".png","$"+count+".png");
		}
		return shiptype.getPicture();
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
	
	@Override
	protected void printHeader(String action) throws IOException {
		TemplateEngine t = getTemplateEngine();
		
		t.setBlock("_BASE", "header", "none" );
		t.parse("__HEADER","header");
		
		super.printHeader(action);
	}
	
	/**
	 * Zeigt die GUI an.
	 * @throws IOException 
	 * 
	 * @urlparam String ownshipgroup Die eigene ausgewaehlte Schiffsgruppe
	 * @urlparam String enemyshipgroup Die gegnerische ausgewaehlte Schiffsgruppe
	 * @urlparam String weapon Die ID der gerade ausgewaehlten Waffe
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() throws IOException {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();

		int ownShipID = getInteger("ship");
		if( ownShipID < 0 ) {
			ownShipID = 0;	
		}
		
		int addShipID = getInteger("addship");
		if( addShipID < 0 ) {
			addShipID = 0;	
		}
		
		int enemyShipID = getInteger("attack");
		if( enemyShipID < 0 ) {
			enemyShipID = 0;	
		}
		
		String action = getString("ksaction");
		int battleID = getInteger("battle");
		int forcejoin = getInteger("forcejoin");
		
		String scan = getString("scan");
		if( scan.length() == 0 ) {
			scan = "own";
		}
		
		/*--------------------------------------------------------------
		
			Schlacht laden bzw. erstellen
		
		----------------------------------------------------------------*/
		
		boolean battleCreated = false;
		
		Battle battle = null;
		if( battleID == 0 ) {
			battle = Battle.create(user.getId(), ownShipID, enemyShipID);
			if( battle == null ) {
				this.setTemplate("");
				
				return;
			}
			battleID = battle.getId();
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
			this.setTemplate("");
			return;
		}
		
		parameterString("ownshipgroup");
		parameterString("enemyshipgroup");
		battle.setOwnShipGroup(getString("ownshipgroup"));
		battle.setEnemyShipGroup(getString("enemyshipgroup"));
		
		//
		// Schiff zur Schlacht hinzufgen
		//
		if( (!battleCreated) && addShipID != 0 && !battle.isGuest() ) {
			if( !battle.addShip( user.getId(), addShipID ) ) {
				addShipID = 0;
		
				// Wenn das Schiff offenbar von jemandem ausserhalb der Kriegfhrenden Allys stammt - Laden abbrechen bei fehlschlag
				if( forcejoin != 0 ) {
					this.setTemplate("");
					return;
				}
			}
		}
		
		parameterString("weapon");
		
		/*--------------------------------------------------------------
			
			Actions
		
		----------------------------------------------------------------*/

		if( (action.length() > 0) && ACTIONS.containsKey(action) ) {
			try {
				BasicKSAction actionobj = ACTIONS.get(action).newInstance();
				actionobj.setController(this);
				
				if( actionobj.execute(battle) == BasicKSAction.RESULT_HALT ) {
					this.setTemplate("");
					return;	
				}
				
				action = "";
				t.setVar("global.ksaction","");
			}
			catch( Exception e ) {
				addError("Kann Aktion nicht ausfuehren: "+e);
				log.error("Ausfuehrung der KS-Aktion "+action+" fehlgeschlagen", e);
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
				
				Common.mailThrowable(e, "KS-Action-Error Schlacht "+battle.getId(), "Action: "+action+"\nownShip: "+curOwnShipID+"\nenemyShip: "+curEnemyShipID);
			}
		}
		
		BattleShip enemyShip = battle.getEnemyShip();
		ShipTypeData enemyShipType = enemyShip.getTypeData();
		BattleShip ownShip = battle.getOwnShip();
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		User oUser = ownShip.getOwner();
		User eUser = enemyShip.getOwner();
		
		t.setVar(	"global.ksaction",			action,
					"global.scan",				scan,
					"global.ownshipgroup",		battle.getOwnShipGroup(),
					"global.enemyshipgroup",	battle.getEnemyShipGroup(),
					"global.weapon",			getString("weapon"),
					"battle.msginfo",			(battle.getComMessageBuffer(battle.getOwnSide()).length() > 0 && battle.isCommander(user,battle.getOwnSide())),
					"battle.id",				battle.getId(),
					"ownside.secondrow.stable",	battle.isSecondRowStable(battle.getOwnSide()),
					"enemyside.secondrow.stable",	battle.isSecondRowStable(battle.getEnemySide()),
					"ownship.id",				ownShip.getId(),
					"ownship.name",				ownShip.getName(),
					"ownship.type",				ownShip.getShip().getType(),
					"ownship.coordinates",		ownShip.getShip().getLocation().displayCoordinates(false),
					"ownship.type.name",		ownShipType.getNickname(),
					"ownship.type.image",		modifyShipImg(ownShipType,ownShip.getCount()),
					"ownship.owner.name",		Common._title(oUser.getName()),
					"ownship.owner.id",			ownShip.getOwner().getId(),
					"ownship.action.hit",		ownShip.getAction() & Battle.BS_HIT,
					"ownship.action.flucht",	ownShip.getAction() & Battle.BS_FLUCHT,
					"ownship.action.destroyed",	ownShip.getAction() & Battle.BS_DESTROYED,
					"ownship.action.shot",		(!battle.isGuest() ? ownShip.getAction() & Battle.BS_SHOT : 0),
					"ownship.action.join",		(ownShip.getAction() & Battle.BS_JOIN) != 0,
					"ownship.action.secondrow",	(ownShip.getAction() & Battle.BS_SECONDROW) != 0,
					"ownship.action.fluchtnext",	(!battle.isGuest() ? ownShip.getAction() & Battle.BS_FLUCHTNEXT : 0),
					"ownship.mangelnahrung",		(!battle.isGuest() ? ownShip.getShip().getStatus().indexOf("mangel_nahrung") > -1 : 0),
					"ownship.mangelreaktor",	(!battle.isGuest() ? ownShip.getShip().getStatus().indexOf("mangel_reaktor") > -1 : 0),
					"enemyship.id",				enemyShip.getId(),
					"enemyship.name",			enemyShip.getName(),
					"enemyship.type",			enemyShip.getShip().getType(),
					"enemyship.type.name",		enemyShipType.getNickname(),
					"enemyship.type.image",		modifyShipImg(enemyShipType,enemyShip.getCount()),
					"enemyship.owner.name",		Common._title(eUser.getName()),
					"enemyship.owner.id",		enemyShip.getOwner().getId(),
					"enemyship.action.hit",		enemyShip.getAction() & Battle.BS_HIT,
					"enemyship.action.flucht",	enemyShip.getAction() & Battle.BS_FLUCHT,
					"enemyship.action.destroyed",	enemyShip.getAction() & Battle.BS_DESTROYED,
					"enemyship.action.join",		enemyShip.getAction() & Battle.BS_JOIN,
					"enemyship.action.secondrow",	enemyShip.getAction() & Battle.BS_SECONDROW );
		
		if( (battle.getOwnSide() == 0) && battle.hasFlag( Battle.FLAG_BLOCK_SECONDROW_0) ) {
			t.setVar("ownside.secondrow.blocked", 1);
		}
		else if( (battle.getOwnSide() == 1) && battle.hasFlag( Battle.FLAG_BLOCK_SECONDROW_1) ) {
			t.setVar("ownside.secondrow.blocked", 1);
		}
		
		if( (battle.getEnemySide() == 0) && battle.hasFlag( Battle.FLAG_BLOCK_SECONDROW_0) ) {
			t.setVar("enemyside.secondrow.blocked", 1);
		}
		else if( (battle.getEnemySide() == 1) && battle.hasFlag( Battle.FLAG_BLOCK_SECONDROW_1) ) {
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
		
		if( battle.getEnemyLog(true).length() > 0 ) {
			battle.writeLog();
		}
		
		StringBuilder actionBuilder = new StringBuilder(action);
		if( !this.showMenu( battle, actionBuilder ) ) {
			this.setTemplate("");
			return;
		}
		
		action = actionBuilder.toString();
		t.setVar("global.ksaction", action);
		
		t.setBlock("_ANGRIFF","ship.info","none");
		t.setBlock("ship.info","shipinfo.ammo.listitem","shipinfo.ammo.list");
		t.setBlock("ship.info","shipinfo.weapons.listitem","shipinfo.weapons.list");
		showInfo("ownship.info", ownShip, (!battle.isGuest() ? false:true), "Own", scan.equals("own") );
		showInfo("enemyship.info", enemyShip, true, "Enemy", scan.equals("enemy") );
		
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
				if( (aship.getDocked().length() > 0)  && battle.isGuest() && (aship.getDocked().charAt(0) == 'l') ) {
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
				if( aship.getDocked().length() > 0  && (!battle.isGuest() || (aship.getDocked().charAt(0) != 'l') ) ) {
					String[] docked = StringUtils.split(aship.getDocked(), ' ');
					
					int shipid = 0;
					if( docked.length > 1 ) {
						shipid = Integer.parseInt(docked[1]);
					}
					else {
						shipid = Integer.parseInt(docked[0]);
					}
	
					for( int j=0; j < ownShips.size(); j++) {
						if( ownShips.get(j).getId() == shipid ) {
							t.setVar(	"ship.docked.name",	ownShips.get(j).getName(),
										"ship.docked.id",	shipid );
							
							break;
						}
					}
	
					
				}
	
				User aUser = aship.getOwner();
				
				t.setVar(	"ship.id",				aship.getId(),
							"ship.name",			aship.getName(),
							"ship.type",			aship.getShip().getType(),
							"ship.type.name",		aShipType.getNickname(),
							"ship.type.image",		modifyShipImg(aShipType,aship.getCount()),
							"ship.owner.name",		Common._title(aUser.getName()),
							"ship.owner.id",		aship.getOwner().getId(),
							"ship.energy",			energy,
							"ship.active",			(aship == ownShip),
							"ship.action.hit",		aship.getAction() & Battle.BS_HIT,
							"ship.action.flucht",	aship.getAction() & Battle.BS_FLUCHT,
							"ship.action.destroyed",	aship.getAction() & Battle.BS_DESTROYED,
							"ship.action.join",			aship.getAction() & Battle.BS_JOIN,
							"ship.action.secondrow",	aship.getAction() & Battle.BS_SECONDROW,
							"ship.action.fluchtnext",	(!battle.isGuest() ? aship.getAction() & Battle.BS_FLUCHTNEXT : 0),
							"ship.action.shot",			(!battle.isGuest() ? aship.getAction() & Battle.BS_SHOT : 0),
							"ship.mangelnahrung",		(!battle.isGuest() ? aship.getShip().getStatus().indexOf("mangel_nahrung") > -1 : false),
							"ship.mangelreaktor",		(!battle.isGuest() ? aship.getShip().getStatus().indexOf("mangel_reaktor") > -1 : false) );
	
				if( firstEntry ) {
					firstEntry = false;
				}
				else {
					t.setVar("ship.addline",1);
				}
	
				if( showgroups && ((pos >= battle.getOwnShipTypeCount(grouptype)) || (pos == groupoffset+SHIPGROUPSIZE)) ) {
					t.setVar("ship.showback",1);
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
		
			Map<String,GroupEntry> groupdata = new HashMap<String,GroupEntry>();
			Map<Integer,Integer> shiptypegroupcount = new HashMap<Integer,Integer>();
			
			List<BattleShip> ownShips = battle.getOwnShips();
			for( int i=0; i < ownShips.size(); i++ ) {
				BattleShip aship = ownShips.get(i);
				
				Common.safeIntInc(shiptypegroupcount, aship.getShip().getType());
				
				groupoffset = (shiptypegroupcount.get(aship.getShip().getType())-1) / SHIPGROUPSIZE;
				
				final String key = aship.getShip().getType()+":"+groupoffset;
				int shipAction = aship.getAction();
				
				GroupEntry data = null;
				if( !groupdata.containsKey(key) ) {
					data = new GroupEntry();
					groupdata.put(key, data);
				}
				else {
					data = groupdata.get(key);
				}
				
				if( (shipAction & Battle.BS_DESTROYED) != 0 ) {
					data.destcount++;
				}
				if( (shipAction & Battle.BS_HIT) != 0 ) {
					data.hitcount++;
				}
				if( (shipAction & Battle.BS_FLUCHT) != 0 ) {
					data.fluchtcount++;
				}
				if( (shipAction & Battle.BS_JOIN) != 0 ) {
					data.joincount++;
				}
				if( (shipAction & Battle.BS_SECONDROW) != 0 ) {
					data.srcount++;
				}
				if( !battle.isGuest() ) {
					if( (shipAction & Battle.BS_SHOT) != 0 )  {
						data.shotcount++;
					}
					if( (shipAction & Battle.BS_FLUCHTNEXT) != 0 )  {
						data.fluchtnextcount++;
					}
					if( aship.getShip().getStatus().indexOf("mangel_nahrung") > -1 )  {
						data.mangelnahrungcount++;
					}
					if( aship.getShip().getStatus().indexOf("mangel_reaktor") > -1 )  {
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
				if( aship.getDocked().length() > 0 && (aship.getDocked().charAt(0) == 'l') ) {
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
				if( (aship.getDocked().length() > 0) && (aship.getDocked().charAt(0) != 'l') ) {
					int shipid = Integer.parseInt(aship.getDocked());
	
					for( int j=0; j < enemyShips.size(); j++ ) {
						if( enemyShips.get(j).getId() == shipid ) {
							t.setVar("ship.docked.name", enemyShips.get(j).getName());
							
							break;
						}
					}						
				}
				
				User aUser = aship.getOwner();
	
				t.setVar(	"ship.id",				aship.getId(),
							"ship.name",			aship.getName(),
							"ship.type",			aship.getShip().getType(),
							"ship.type.name",		aShipType.getNickname(),
							"ship.type.image",		modifyShipImg(aShipType,aship.getCount()),
							"ship.owner.name",		Common._title(aUser.getName()),
							"ship.owner.id",		aship.getOwner().getId(),
							"ship.active",			(aship == enemyShip),
							"ship.action.hit",		aship.getAction() & Battle.BS_HIT,
							"ship.action.flucht",	aship.getAction() & Battle.BS_FLUCHT,
							"ship.action.join",		aship.getAction() & Battle.BS_JOIN,
							"ship.action.secondrow",	aship.getAction() & Battle.BS_SECONDROW,
							"ship.action.destroyed",	aship.getAction() & Battle.BS_DESTROYED );
	
				if( firstEntry ) {
					firstEntry = false;
				}
				else {
					t.setVar("ship.addline",1);
				}
	
				if( showgroups && ((pos >= battle.getEnemyShipTypeCount(grouptype)) || (pos == groupoffset+SHIPGROUPSIZE)) ) {
					t.setVar("ship.showback",1);
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
		
			Map<String,GroupEntry> groupdata = new HashMap<String,GroupEntry>();
		
			Map<Integer,Integer> shiptypegroupcount = new HashMap<Integer,Integer>();
		
			List<BattleShip> enemyShips = battle.getEnemyShips();
			for( int i=0; i < enemyShips.size(); i++ ) {
				BattleShip aship = enemyShips.get(i);
				
				if( (aship.getDocked().length() > 0) && (aship.getDocked().charAt(0) == 'l') ) {
					continue;
				}
				
				Common.safeIntInc(shiptypegroupcount, aship.getShip().getType());
				
				groupoffset = (shiptypegroupcount.get(aship.getShip().getType())-1) / SHIPGROUPSIZE;
			
				final String key = aship.getShip().getType()+":"+groupoffset;
				int shipAction = aship.getAction();
				
				GroupEntry data = null;
				if( !groupdata.containsKey(key) ) {
					data = new GroupEntry();
					groupdata.put(key, data);
				}
				else {
					data = groupdata.get(key);
				}
				
				if( (shipAction & Battle.BS_DESTROYED) != 0 ) {
					data.destcount++;
				}
				if( (shipAction & Battle.BS_HIT) != 0 ) {
					data.hitcount++;
				}
				if( (shipAction & Battle.BS_FLUCHT) != 0 ) {
					data.fluchtcount++;
				}
				if( (shipAction & Battle.BS_JOIN) != 0 ) {
					data.joincount++;
				}
				if( (shipAction & Battle.BS_SECONDROW) != 0 ) {
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
	}
}
