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
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.config.Item;
import net.driftingsouls.ds2.server.config.ItemEffect;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.ks.BasicKSAction;
import net.driftingsouls.ds2.server.modules.ks.BasicKSMenuAction;
import net.driftingsouls.ds2.server.modules.ks.KSAttackAction;
import net.driftingsouls.ds2.server.modules.ks.KSCheatAPAction;
import net.driftingsouls.ds2.server.modules.ks.KSCheatRegenerateEnemyAction;
import net.driftingsouls.ds2.server.modules.ks.KSCheatRegenerateOwnAction;
import net.driftingsouls.ds2.server.modules.ks.KSDischargeBatteriesAllAction;
import net.driftingsouls.ds2.server.modules.ks.KSDischargeBatteriesClassAction;
import net.driftingsouls.ds2.server.modules.ks.KSDischargeBatteriesSingleAction;
import net.driftingsouls.ds2.server.modules.ks.KSEndBattleCivilAction;
import net.driftingsouls.ds2.server.modules.ks.KSEndBattleEqualAction;
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
import net.driftingsouls.ds2.server.ships.Ships;

import org.apache.commons.lang.StringUtils;

/**
 * Das UI fuer Schlachten
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
public class AngriffController extends DSGenerator implements Loggable {
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
		ACTIONS.put("endbattleequal", KSEndBattleEqualAction.class);
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
			ACTIONS.put("cheat_ap", KSCheatAPAction.class);
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
	 * Konstruktor
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
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		return true;
	}
	
	private void showInfo(String tag, SQLResultRow ship, boolean enemy, String jscriptid, boolean show) {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		if( ship.isEmpty() ) {
			addError("FATAL ERROR: Kein gueltiges Schiff vorhanden");
			return;	
		}

		SQLResultRow shipType = Ships.getShipType(ship);
		SQLResultRow battle_ship = db.first("SELECT hull,shields,engine,weapons,comm,sensors,newcount FROM battles_ships WHERE shipid=",ship.getInt("id"));

		t.start_record();
		t.set_var(	"shipinfo.jscriptid",		jscriptid,
					"shipinfo.show",			show,
					"shipinfo.enemy",			enemy,
					"shipinfo.type",			ship.getInt("type"),
					"shipinfo.type.name",		shipType.getString("nickname"),
					"shipinfo.type.image",		shipType.getString("picture"),
					"shipinfo.type.hull",		Common.ln(shipType.getInt("hull")),
					"shipinfo.type.shields",	Common.ln(shipType.getInt("shields")),
					"shipinfo.type.cost",		shipType.getInt("cost"),
					"shipinfo.type.maxenergy",	shipType.getInt("eps"),
					"shipinfo.type.maxcrew",	shipType.getInt("crew"),
					"shipinfo.type.weapons",	shipType.getInt("military"),
					"shipinfo.hull",			Common.ln(ship.getInt("hull")),
					"shipinfo.panzerung",		(int)Math.round(shipType.getInt("panzerung")*ship.getInt("hull")/(double)shipType.getInt("hull")),
					"shipinfo.shields",			Common.ln(ship.getInt("shields")),
					"shipinfo.nocrew",			(ship.getInt("crew") == 0) && (shipType.getInt("crew")>0),
					"shipinfo.crew",			(ship.getInt("crew") > 0) && (shipType.getInt("crew")>0),
					"shipinfo.heat",			ship.getInt("s"),
					"shipinfo.energy",			ship.getInt("e"),
					"shipinfo.crew",			ship.getInt("crew"),
					"shipinfo.engine",			ship.getInt("engine"),
					"shipinfo.weapons",			ship.getInt("weapons"),
					"shipinfo.comm",			ship.getInt("comm"),
					"shipinfo.sensors",			ship.getInt("sensors"),
					"shipinfo.showtmp",			(ship.getInt("action") & Battle.BS_HIT) != 0 || (ship.getInt("action") & Battle.BS_DESTROYED) != 0,
					"shipinfo.tmp.hull",		((ship.getInt("action") & Battle.BS_DESTROYED) != 0 ? 0 : Common.ln(battle_ship.getInt("hull")) ),
					"shipinfo.tmp.shields",		((ship.getInt("action") & Battle.BS_DESTROYED) != 0 ? 0 : Common.ln(battle_ship.getInt("shields"))),
					"shipinfo.tmp.engine",		battle_ship.getInt("engine"),
					"shipinfo.tmp.weapons",		battle_ship.getInt("weapons"),
					"shipinfo.tmp.comm",		battle_ship.getInt("comm"),
					"shipinfo.tmp.sensors",		battle_ship.getInt("sensors"),
					"shipinfo.tmp.panzerung",	(int)Math.round(shipType.getInt("panzerung")*battle_ship.getInt("hull")/(double)shipType.getInt("hull")) );

		// Anzahl
		if( shipType.getInt("shipcount") > 1 ) {
			t.set_var(	"shipinfo.count",		ship.getInt("count"),
						"shipinfo.tmp.count",	battle_ship.getInt("count") );
		}

		// Huelle
		if( ship.getInt("hull") < shipType.getInt("hull")/2 ) {
			t.set_var("shipinfo.hull.bad",1);
		}
		else if( ship.getInt("hull") < shipType.getInt("hull") ) {
			t.set_var("shipinfo.hull.normal",1);
		}
		else {
			t.set_var("shipinfo.hull.good",1);
		}

		// Schilde
		if( shipType.getInt("shields") > 0 ) {
			if( ship.getInt("shields") < shipType.getInt("shields")/2 ) {
				t.set_var("shipinfo.shields.bad",1);
			}
			else if( ship.getInt("shields") < shipType.getInt("shields") ) {
				t.set_var("shipinfo.shields.normal",1);
			}
			else {
				t.set_var("shipinfo.shields.good",1);
			}
		}

		// Antrieb
		if( shipType.getInt("cost") > 0 ) {
			if( ship.getInt("engine") < 50 ) {
				t.set_var("shipinfo.engine.bad",1);
			}
			else if( ship.getInt("engine") < 100 ) {
				t.set_var("shipinfo.engine.normal",1);
			}
			else {
				t.set_var("shipinfo.engine.good",1);
			}
		}

		// Waffen
		if( shipType.getInt("military") > 0 ) {
			if( ship.getInt("weapons") < 50 ) {
				t.set_var("shipinfo.weapons.bad",1);
			}
			else if( ship.getInt("weapons") < 100 ) {
				t.set_var("shipinfo.weapons.normal",1);
			}
			else {
				t.set_var("shipinfo.weapons.good",1);
			}
		}

		// Kommunikation
		if( ship.getInt("comm") < 50 ) {
			t.set_var("shipinfo.comm.bad",1);
		}
		else if( ship.getInt("comm") < 100 ) {
			t.set_var("shipinfo.comm.normal",1);
		}
		else {
			t.set_var("shipinfo.comm.good",1);
		}

		// Sensoren
		if( ship.getInt("sensors") < 50 ) {
			t.set_var("shipinfo.sensors.bad",1);
		}
		else if( ship.getInt("sensors") < 100 ) {
			t.set_var("shipinfo.sensors.normal",1);
		}
		else {
			t.set_var("shipinfo.sensors.good",1);
		}

		if( !enemy ) {
			// Eigene Crew
			if( (ship.getInt("crew") > 0) && (shipType.getInt("crew")>0) ) {
				if( ship.getInt("crew") < shipType.getInt("crew")/2 ) {
					t.set_var("shipinfo.crew.bad",1);
				}
				else if( ship.getInt("crew") < shipType.getInt("crew") ) {
					t.set_var("shipinfo.crew.normal",1);
				}
				else {
					t.set_var("shipinfo.crew.good",1);
				}
			}

			// Energie
			if( ship.getInt("e") < shipType.getInt("eps") / 4 ) {
				t.set_var("shipinfo.energy.bad",1);
			}
			else if( ship.getInt("e") < shipType.getInt("eps") ) {
				t.set_var("shipinfo.energy.normal",1);
			}
			else {
				t.set_var("shipinfo.energy.good",1);
			}
		}

		// Offiziere
		Offizier offizier = Offizier.getOffizierByDest('s', ship.getInt("id"));
		if( offizier != null ) {
			t.set_var(	"offizier.rang",	offizier.getRang(),
						"offizier.id",		offizier.getID(),
						"offizier.name",	Common._plaintitle(offizier.getName()) );
		}

		if( !enemy ) {
			// Waffen
			Map<String,String> weapons = Weapons.parseWeaponList(shipType.getString("weapons"));
			Map<String,String> heat = Weapons.parseWeaponList(ship.getString("heat"));
			Map<String,String> maxheat = Weapons.parseWeaponList(shipType.getString("maxheat"));

			for( String weaponName : weapons.keySet() ) {
				if( shipType.getInt("shipcount") > ship.getInt("count") ) {
					maxheat.put(weaponName, Integer.toString(
							(int)(Integer.parseInt(maxheat.get(weaponName))*ship.getInt("count")/(double)shipType.getInt("shipcount"))
							));
				}

				t.set_var(	"shipinfo.weapon.name",		Weapons.get().weapon(weaponName).getName(),
							"shipinfo.weapon.heat",		heat.containsKey(weaponName) ? Integer.parseInt(heat.get(weaponName)) : 0,
							"shipinfo.weapon.maxheat",	maxheat.get(weaponName) );

				t.parse("shipinfo.weapons.list","shipinfo.weapons.listitem",true);
			}

			// Munition
			Cargo mycargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
			
			t.set_var("shipinfo.ammo.list","");
			List<ItemCargoEntry> itemlist = mycargo.getItemsWithEffect( ItemEffect.Type.AMMO );
			for( int i=0; i < itemlist.size(); i++ ) {
				ItemCargoEntry item = itemlist.get(i);
				
				if( item.getCount() > 0 ) {
					Item itemobject = item.getItemObject();
					
					t.set_var(	"ammo.image",	itemobject.getPicture(),
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
	
	private boolean showMenu( Battle battle, StringBuilder action ) {
		User user = this.getUser();
		TemplateEngine t = this.getTemplateEngine();		
		
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow enemyShip = battle.getEnemyShip();
		
		// TODO: evt sollte das hier in ne eigene Action ausgelagert werden?
		if( action.toString().equals("showbattlelog") && (battle.getComMessageBuffer(battle.getOwnSide()).length() > 0) && battle.isCommander(user.getID(),battle.getOwnSide()) ) {
			BBCodeParser bbcodeparser = BBCodeParser.getNewInstance();
			try {
				bbcodeparser.registerHandler( "tooltip", 2, "<a onmouseover=\"return overlib('$2',TIMEOUT,0,DELAY,400,WIDTH,100,TEXTFONTCLASS,'smallTooltip');\" onmouseout=\"return nd();\" class=\"aloglink\" href=\"#\">$1</a>" );
			}
			catch( Exception e ) {
				LOG.warn("Registrierung des BBCode-Handlers tooltip gescheitert", e);
			}
		
			String msgbuffer = battle.getComMessageBuffer(battle.getOwnSide());
			msgbuffer = StringUtils.replace(msgbuffer, "<![CDATA[", "");
			msgbuffer = StringUtils.replace(msgbuffer, "]]>","");
			msgbuffer = Common._stripHTML(msgbuffer);
			
			t.set_var( "battle.msg", StringUtils.replace(bbcodeparser.parse(msgbuffer), "\n", "<br />") );
		
			battle.clearComMessageBuffer(battle.getOwnSide());
			
			t.set_var(	"global.ksaction",	"",
						"battle.msginfo",	"" );
			action.setLength(0);
		}
		else if( battle.getOwnLog(true).length() == 0 ) {
			if( battle.isCommander(user.getID(),battle.getOwnSide()) ) {
				if( battle.getTakeCommand(battle.getOwnSide()) != 0 ) {
					User auser = getContext().createUserObject(battle.getTakeCommand(battle.getOwnSide()));
		
					t.set_var(	"battle.takecommand.ask",	1,
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
					t.set_block("_ANGRIFF","menu.entry","menu");
					t.set_block("_ANGRIFF","menu.entry.ask","none");
					t.set_var("global.showmenu", 1);
		
					// Ist das gegnerische Schiff zerstoert? Falls ja, dass Angriffsmenue deaktivieren
					if( ( action.equals("attack") ) &&
						((enemyShip.getInt("action") & Battle.BS_DESTROYED) != 0 || (ownShip.getInt("action") & Battle.BS_FLUCHT) != 0 || (ownShip.getString("docked").length() > 0 && ownShip.getString("docked").charAt(0) == 'l')) ) {
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
							LOG.error("Darstellung des KS-Menues "+action+" fehlgeschlagen", e);
							
							Common.mailThrowable(e, "KS-Menu-Error Schlacht "+battle.getID(), "Action: "+action+"\nownShip: "+ownShip.getInt("id")+"\nenemyShip: "+enemyShip.getInt("id"));
						}
					}
				}
			}
			else {
				KSMenuHistoryAction historyobj = new KSMenuHistoryAction();

				historyobj.setController(this);
				
				if( (battle.getAlly(battle.getOwnSide()) != 0) && (battle.getTakeCommand(battle.getOwnSide()) == 0) && (battle.getAlly(battle.getOwnSide()) == user.getAlly()) ) {
					User auser = getContext().createUserObject(battle.getCommander(battle.getOwnSide()));
					if( auser.getInactivity() > 0 ) {
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
				LOG.warn("Registrierung des BBCode-Handlers tooltip gescheitert", e);
			}
			
			t.set_var("battle.logoutput", bbcodeparser.parse(battle.getOwnLog(false)));
		}
		
		return true;
	}
	
	private String modifyShipImg( SQLResultRow shiptype, int count ) {
		if( shiptype.getInt("shipcount") > 1 ) {
			return StringUtils.replace(shiptype.getString("picture"), ".png","$"+count+".png");
		}
		return shiptype.getString("picture");
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
	 * Zeigt die GUI an
	 * 
	 * @urlparam String ownshipgroup Die eigene ausgewaehlte Schiffsgruppe
	 * @urlparam String enemyshipgroup Die gegnerische ausgewaehlte Schiffsgruppe
	 * @urlparam String weapon Die ID der gerade ausgewaehlten Waffe
	 */
	@Override
	public void defaultAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();

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
		
		Battle battle = new Battle();
		if( battleID == 0 ) {
			if( !battle.create( user.getID(), ownShipID, enemyShipID) ) {
				this.setTemplate("");
				
				return;
			}
			battleID = battle.getID();
			battleCreated = true;
		}
		
		if( forcejoin != 0 ) {
			SQLResultRow jship = db.first("SELECT id FROM ships WHERE id>0 AND owner=",user.getID()," AND id=",addShipID);
			if( jship.isEmpty() ) forcejoin = 0;
		}
		
		if( !battle.load(battleID, user.getID(), ownShipID, enemyShipID, forcejoin) ) {
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
			if( !battle.addShip( user.getID(), addShipID ) ) {
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
				t.set_var("global.ksaction","");
			}
			catch( Exception e ) {
				addError("Kann Aktion nicht ausfuehren: "+e);
				LOG.error("Ausfuehrung der KS-Aktion "+action+" fehlgeschlagen", e);
				
				Common.mailThrowable(e, "KS-Action-Error Schlacht "+battle.getID(), "Action: "+action+"\nownShip: "+battle.getOwnShip().getInt("id")+"\nenemyShip: "+battle.getEnemyShip().getInt("id"));
			}
		}
		
		SQLResultRow enemyShip = battle.getEnemyShip();
		SQLResultRow enemyShipType = Ships.getShipType( enemyShip );
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow ownShipType = Ships.getShipType( ownShip );
		
		t.set_var(	"global.ksaction",			action,
					"global.scan",				scan,
					"global.ownshipgroup",		battle.getOwnShipGroup(),
					"global.enemyshipgroup",	battle.getEnemyShipGroup(),
					"global.weapon",			getString("weapon"),
					"battle.msginfo",			(battle.getComMessageBuffer(battle.getOwnSide()).length() > 0 && battle.isCommander(user.getID(),battle.getOwnSide())),
					"battle.id",				battle.getID(),
					"ownside.secondrow.stable",	battle.isSecondRowStable(battle.getOwnSide(), null),
					"enemyside.secondrow.stable",	battle.isSecondRowStable(battle.getEnemySide(), null),
					"ownship.id",				ownShip.getInt("id"),
					"ownship.name",				ownShip.getString("name"),
					"ownship.type",				ownShip.getInt("type"),
					"ownship.system",			ownShip.getInt("system"),
					"ownship.x",				ownShip.getInt("x"),
					"ownship.y",				ownShip.getInt("y"),
					"ownship.type.name",		ownShipType.getString("nickname"),
					"ownship.type.image",		modifyShipImg(ownShipType,ownShip.getInt("count")),
					"ownship.owner.name",		Common._title(getContext().createUserObject(ownShip.getInt("owner")).getName()),
					"ownship.owner.id",			ownShip.getInt("owner"),
					"ownship.action.hit",		ownShip.getInt("action") & Battle.BS_HIT,
					"ownship.action.flucht",	ownShip.getInt("action") & Battle.BS_FLUCHT,
					"ownship.action.destroyed",	ownShip.getInt("action") & Battle.BS_DESTROYED,
					"ownship.action.shot",		(!battle.isGuest() ? ownShip.getInt("action") & Battle.BS_SHOT : 0),
					"ownship.action.join",		(ownShip.getInt("action") & Battle.BS_JOIN) != 0,
					"ownship.action.secondrow",	(ownShip.getInt("action") & Battle.BS_SECONDROW) != 0,
					"ownship.action.fluchtnext",	(!battle.isGuest() ? ownShip.getInt("action") & Battle.BS_FLUCHTNEXT : 0),
					"ownship.mangelnahrung",		(!battle.isGuest() ? ownShip.getString("status").indexOf("mangel_nahrung") > -1 : 0),
					"ownship.mangelreaktor",	(!battle.isGuest() ? ownShip.getString("status").indexOf("mangel_reaktor") > -1 : 0),
					"enemyship.id",				enemyShip.getInt("id"),
					"enemyship.name",			enemyShip.getString("name"),
					"enemyship.type",			enemyShip.getInt("type"),
					"enemyship.type.name",		enemyShipType.getString("nickname"),
					"enemyship.type.image",		modifyShipImg(enemyShipType,enemyShip.getInt("count")),
					"enemyship.owner.name",		Common._title(getContext().createUserObject(enemyShip.getInt("owner")).getName()),
					"enemyship.owner.id",		enemyShip.getInt("owner"),
					"enemyship.action.hit",		enemyShip.getInt("action") & Battle.BS_HIT,
					"enemyship.action.flucht",	enemyShip.getInt("action") & Battle.BS_FLUCHT,
					"enemyship.action.destroyed",	enemyShip.getInt("action") & Battle.BS_DESTROYED,
					"enemyship.action.join",		enemyShip.getInt("action") & Battle.BS_JOIN,
					"enemyship.action.secondrow",	enemyShip.getInt("action") & Battle.BS_SECONDROW );
		
		if( (battle.getOwnSide() == 0) && battle.hasFlag( Battle.FLAG_BLOCK_SECONDROW_0) ) {
			t.set_var("ownside.secondrow.blocked", 1);
		}
		else if( (battle.getOwnSide() == 1) && battle.hasFlag( Battle.FLAG_BLOCK_SECONDROW_1) ) {
			t.set_var("ownside.secondrow.blocked", 1);
		}
		
		if( (battle.getEnemySide() == 0) && battle.hasFlag( Battle.FLAG_BLOCK_SECONDROW_0) ) {
			t.set_var("enemyside.secondrow.blocked", 1);
		}
		else if( (battle.getEnemySide() == 1) && battle.hasFlag( Battle.FLAG_BLOCK_SECONDROW_1) ) {
			t.set_var("enemyside.secondrow.blocked", 1);
		}
		
		/*
			Das eigene ausgewaehlte Schiff
		*/
		String energy = "";
		if( !battle.isGuest() ) {
			//Energieanzeige im ersten eigenene Schiff
			if( ownShip.getInt("e") < ownShipType.getInt("eps")/4 ) {
				energy = "<br />E: <span style=\'color:#ff0000\'>";
			} 
			else if( ownShip.getInt("e") < ownShipType.getInt("eps") ) {
		  		energy = "<br />E: <span style=\'color:#ffff00\'>";
			} 
			else {
				energy = "<br />E: <span style=\'color:#00ff00\'>";
			}
		
			energy += ownShip.getInt("e")+"/"+ownShipType.getInt("eps")+"</span>";
		}
		t.set_var("ownship.energy",energy);
		
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
		
		t.set_block("_ANGRIFF","ship.info","none");
		t.set_block("ship.info","shipinfo.ammo.listitem","shipinfo.ammo.list");
		t.set_block("ship.info","shipinfo.weapons.listitem","shipinfo.weapons.list");
		showInfo("ownship.info", ownShip, (!battle.isGuest() ? false:true), "Own", scan.equals("own") );
		showInfo("enemyship.info", enemyShip, true, "Enemy", scan.equals("enemy") );
		
		if( scan.equals("own") ) {
			t.set_var("infobox.active", "Own");
		}
		else {
			t.set_var("infobox.active", "Enemy");
		}
		
		/*---------------------------------------------------------
		
			Schiffsliste
		
		-----------------------------------------------------------*/
		
		t.set_block("_ANGRIFF","ships.typelist.item","ships.typelist.none");
		t.set_block("_ANGRIFF","ownShips.listitem","ownShips.list");
		t.set_block("_ANGRIFF","enemyShips.listitem","enemyShips.list");
		
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
		
			List<SQLResultRow> ownShips = battle.getOwnShips();
			for( int i=0; i < ownShips.size(); i++ ) {
				SQLResultRow aship = ownShips.get(i);
				
				t.start_record();
		
				if( !showgroups && (aship.getInt("id") == ownShip.getInt("id")) ) {
					continue;
				}
				if( showgroups && (aship.getInt("type") != grouptype) ) {
					continue;
				}
				if( (aship.getString("docked").length() > 0)  && battle.isGuest() && (aship.getString("docked").charAt(0) == 'l') ) {
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
		
				if( !aship.isEmpty() ) {
					SQLResultRow aShipType = Ships.getShipType( aship );
					pos++;
		
					// Energiestatus anzeigen, wenn der User kein Gast ist
					if( !battle.isGuest() ) {
						if( aship.getInt("e") < aShipType.getInt("eps")/4 ) {
							energy = "<br />E: <span style=\'color:#ff0000\'>";
						}
						else if( aship.getInt("e") < aShipType.getInt("eps") ) {
							energy =  "<br />E: <span style=\'color:#ffff00\'>";
						}
						else {
							energy =  "<br />E: <span style=\'color:#00ff00\'>";
						}
		
						energy += aship.getInt("e")+"/"+aShipType.getInt("eps")+"</span>";
					}
		
					// Ist das Schiff gedockt?
					if( aship.getString("docked").length() > 0  && (!battle.isGuest() || (aship.getString("docked").charAt(0) != 'l') ) ) {
						String[] docked = StringUtils.split(aship.getString("docked"), ' ');
						
						int shipid = 0;
						if( docked.length > 1 ) {
							shipid = Integer.parseInt(docked[1]);
						}
						else {
							shipid = Integer.parseInt(docked[0]);
						}
		
						for( int j=0; j < ownShips.size(); j++) {
							if( ownShips.get(j).getInt("id") == shipid ) {
								t.set_var(	"ship.docked.name",	ownShips.get(j).getString("name"),
											"ship.docked.id",	shipid );
								
								break;
							}
						}
		
						
					}
		
					t.set_var(	"ship.id",				aship.getInt("id"),
								"ship.name",			aship.getString("name"),
								"ship.type",			aship.getInt("type"),
								"ship.type.name",		aShipType.getString("nickname"),
								"ship.type.image",		modifyShipImg(aShipType,aship.getInt("count")),
								"ship.owner.name",		Common._title(getContext().createUserObject(aship.getInt("owner")).getName()),
								"ship.owner.id",		aship.getInt("owner"),
								"ship.energy",			energy,
								"ship.active",			(aship.getInt("id") == ownShip.getInt("id")),
								"ship.action.hit",		aship.getInt("action") & Battle.BS_HIT,
								"ship.action.flucht",	aship.getInt("action") & Battle.BS_FLUCHT,
								"ship.action.destroyed",	aship.getInt("action") & Battle.BS_DESTROYED,
								"ship.action.join",			aship.getInt("action") & Battle.BS_JOIN,
								"ship.action.secondrow",	aship.getInt("action") & Battle.BS_SECONDROW,
								"ship.action.fluchtnext",	(!battle.isGuest() ? aship.getInt("action") & Battle.BS_FLUCHTNEXT : 0),
								"ship.action.shot",			(!battle.isGuest() ? aship.getInt("action") & Battle.BS_SHOT : 0),
								"ship.mangelnahrung",		(!battle.isGuest() ? aship.getString("status").indexOf("mangel_nahrung") > -1 : false),
								"ship.mangelreaktor",		(!battle.isGuest() ? aship.getString("status").indexOf("mangel_reaktor") > -1 : false) );
		
					if( firstEntry ) {
						firstEntry = false;
					}
					else {
						t.set_var("ship.addline",1);
					}
		
					if( showgroups && ((pos >= battle.getOwnShipTypeCount(grouptype)) || (pos == groupoffset+SHIPGROUPSIZE)) ) {
						t.set_var("ship.showback",1);
					}
		
					t.parse("ownShips.list","ownShips.listitem",true);
				}
		
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
			
			List<SQLResultRow> ownShips = battle.getOwnShips();
			for( int i=0; i < ownShips.size(); i++ ) {
				SQLResultRow aship = ownShips.get(i);
				
				Common.safeIntInc(shiptypegroupcount, aship.getInt("type"));
				
				groupoffset = (shiptypegroupcount.get(aship.getInt("type"))-1) / SHIPGROUPSIZE;
				
				final String key = aship.getInt("type")+":"+groupoffset;
				int shipAction = aship.getInt("action");
				
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
					if( aship.getString("status").indexOf("mangel_nahrung") > -1 )  {
						data.mangelnahrungcount++;
					}
					if( aship.getString("status").indexOf("mangel_reaktor") > -1 )  {
						data.mangelreaktorcount++;
					}
				}
			}
			
			Map<Integer,Integer> shipTypes = battle.getShipTypeCount(battle.getOwnSide());
			for( Integer stid : shipTypes.keySet() ) {
				if( shipTypes.get(stid) <= 0 ) { 
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
					
					SQLResultRow shiptype = Ships.getShipType(stid, false);
					GroupEntry data = groupdata.get(key);
					
					t.set_var(	"shiptypelist.count",		count,
								"shiptypelist.name",		shiptype.getString("nickname"),
								"shiptypelist.groupid",		key,
								"shiptypelist.id",			stid,
								"shiptypelist.image",		shiptype.getString("picture"),
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
						t.set_var("shiptypelist.addline",1);
					}
		
					t.parse("ownShips.list","ships.typelist.item",true);
				}
			}
		}
		
		t.set_var(	"shiptypelist.addline",				0,
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
		
			List<SQLResultRow> enemyShips = battle.getEnemyShips();
			for( int i=0; i < enemyShips.size(); i++ ) {
				SQLResultRow aship = enemyShips.get(i);
				
				t.start_record();
		
				if( !showgroups && (aship.getInt("id") == enemyShip.getInt("id")) ) {
					continue;
				}
				if( showgroups && (aship.getInt("type") != grouptype) ) {
					continue;
				}
				
				// Gelandete Schiffe nicht anzeigen
				if( aship.getString("docked").length() > 0 && (aship.getString("docked").charAt(0) == 'l') ) {
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
		
				if( !aship.isEmpty() ) {
					SQLResultRow aShipType = Ships.getShipType( aship );
					
					pos++;
		
					// Ist das Schiff gedockt?
					if( (aship.getString("docked").length() > 0) && (aship.getString("docked").charAt(0) != 'l') ) {
						int shipid = Integer.parseInt(aship.getString("docked"));
		
						for( int j=0; j < enemyShips.size(); j++ ) {
							if( enemyShips.get(j).getInt("id") == shipid ) {
								t.set_var("ship.docked.name", enemyShips.get(j).getString("name"));
								
								break;
							}
						}						
					}
		
					t.set_var(	"ship.id",				aship.getInt("id"),
								"ship.name",			aship.getString("name"),
								"ship.type",			aship.getInt("type"),
								"ship.type.name",		aShipType.getString("nickname"),
								"ship.type.image",		modifyShipImg(aShipType,aship.getInt("count")),
								"ship.owner.name",		Common._title(getContext().createUserObject(aship.getInt("owner")).getName()),
								"ship.owner.id",		aship.getInt("owner"),
								"ship.active",			(aship.getInt("id") == enemyShip.getInt("id")),
								"ship.action.hit",		aship.getInt("action") & Battle.BS_HIT,
								"ship.action.flucht",	aship.getInt("action") & Battle.BS_FLUCHT,
								"ship.action.join",		aship.getInt("action") & Battle.BS_JOIN,
								"ship.action.secondrow",	aship.getInt("action") & Battle.BS_SECONDROW,
								"ship.action.destroyed",	aship.getInt("action") & Battle.BS_DESTROYED );
		
					if( firstEntry ) {
						firstEntry = false;
					}
					else {
						t.set_var("ship.addline",1);
					}
		
					if( showgroups && ((pos >= battle.getEnemyShipTypeCount(grouptype)) || (pos == groupoffset+SHIPGROUPSIZE)) ) {
						t.set_var("ship.showback",1);
					}
					
					t.parse("enemyShips.list", "enemyShips.listitem", true);
				}
				
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
		
			List<SQLResultRow> enemyShips = battle.getEnemyShips();
			for( int i=0; i < enemyShips.size(); i++ ) {
				SQLResultRow aship = enemyShips.get(i);
				
				if( (aship.getString("docked").length() > 0) && (aship.getString("docked").charAt(0) == 'l') ) {
					continue;
				}
				
				Common.safeIntInc(shiptypegroupcount, aship.getInt("type"));
				
				groupoffset = (shiptypegroupcount.get(aship.getInt("type"))-1) / SHIPGROUPSIZE;
			
				final String key = aship.getInt("type")+":"+groupoffset;
				int shipAction = aship.getInt("action");
				
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
			for( Integer stid : shipTypes.keySet() ) {
				if( shipTypes.get(stid) <= 0 ) { 
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
					SQLResultRow shiptype = Ships.getShipType(stid, false);
					
					GroupEntry data = groupdata.get(key);
					
					t.set_var(	"shiptypelist.count",		count,
								"shiptypelist.name",		shiptype.getString("nickname"),
								"shiptypelist.id",			stid,
								"shiptypelist.groupid",		stid+":"+i,
								"shiptypelist.image",		Ships.getShipType(stid, false).getString("picture"),
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
						t.set_var("shiptypelist.addline",1);
					}
		
					t.parse("enemyShips.list","ships.typelist.item",true);
				}
			}
		}
		
		/*
			Infos (APs, Runde, Gegnerischer Kommandant)
		*/
		
		if( !battle.isCommander(user.getID(),battle.getOwnSide()) ) {
			User auser = getContext().createUserObject(battle.getCommander(battle.getOwnSide()));
			t.set_var(	"user.commander",		0,
						"battle.owncom.name",	auser.getProfileLink(),
						"battle.owncom.ready",	battle.isReady(battle.getOwnSide()) );
		} 
		else {
			t.set_var(	"user.commander",		1,
						"battle.points.own",	battle.getPoints(battle.getOwnSide()) );
		}
		if( !battle.isReady(battle.getOwnSide()) ) {
			t.set_var("battle.turn.own",1);
		}
		
		int enemySide = battle.getOwnSide() == 1 ? 0 : 1;
		User auser = getContext().createUserObject(battle.getCommander(enemySide));
		t.set_var(	"battle.enemycom.name",		auser.getProfileLink(),
					"battle.enemycom.ready",	battle.isReady(battle.getEnemySide()) );
	}
}
