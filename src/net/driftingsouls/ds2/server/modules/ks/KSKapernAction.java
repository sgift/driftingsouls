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
package net.driftingsouls.ds2.server.modules.ks;

import java.util.ArrayList;
import java.util.List;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Item;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserFlagschiffLocation;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.ships.ShipClasses;

/**
 * Laesst das aktuell ausgewaehlte Schiff versuchen das aktuell ausgewaehlte Zielschiff zu kapern 
 * @author Christopher Jung
 *
 */
public class KSKapernAction extends BasicKSAction {
	/**
	 * Konstruktor
	 *
	 */
	public KSKapernAction() {
		this.requireAP(5);
		this.requireOwnShipReady(true);
	}
	
	@Override
	public int validate(Battle battle) {
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow enemyShip = battle.getEnemyShip();
		
		if( (ownShip.getInt("action") & Battle.BS_SECONDROW) != 0 ||
			(enemyShip.getInt("action") & Battle.BS_SECONDROW) != 0 ) {
			return RESULT_ERROR;
		}
		
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		if( (ownShip.getInt("weapons") == 0) || (ownShip.getInt("engine") == 0) || 
			(ownShip.getInt("crew") <= 0) || (ownShip.getInt("action") & Battle.BS_FLUCHT) != 0 ||
			(ownShip.getInt("action") & Battle.BS_JOIN) != 0 || (enemyShip.getInt("action") & Battle.BS_FLUCHT) != 0 ||
			(enemyShip.getInt("action") & Battle.BS_JOIN) != 0 || (enemyShip.getInt("action") & Battle.BS_DESTROYED) != 0 ) {
			return RESULT_ERROR;
		}
		
		SQLResultRow enemyShipType = Ships.getShipType( enemyShip );
	
		// Geschuetze sind nicht kaperbar
		if( (enemyShipType.getInt("class") == ShipClasses.GESCHUETZ.ordinal() ) || 
			((enemyShipType.getInt("cost") != 0) && (enemyShip.getInt("engine") != 0) && (enemyShip.getInt("crew") != 0)) ||
			(ownShip.getInt("crew") == 0) || Ships.hasShipTypeFlag(enemyShipType, Ships.SF_NICHT_KAPERBAR) ) {
			return RESULT_ERROR;
		}
		
		if( enemyShipType.getInt("crew") == 0 ) {
			return RESULT_ERROR;
		}
	
		if( enemyShip.getString("docked").length() > 0 ) {
			if( enemyShip.getString("docked").charAt(0) == 'l' ) {
				return RESULT_ERROR;
			} 

			SQLResultRow mastership = db.first("SELECT engine,crew FROM ships WHERE id>0 AND id=",enemyShip.getString("docked"));
			if( (mastership.getInt("engine") != 0) && (mastership.getInt("crew") != 0) ) {
				return RESULT_ERROR;
			}
		}
	
		// IFF-Stoersender
		boolean disableIFF = enemyShip.getString("status").indexOf("disable_iff") > -1;	
		
		if( disableIFF ) {
			return RESULT_ERROR;
		}
	
		//Flagschiff?
		User ownuser = context.getActiveUser();
		User enemyuser = context.createUserObject(enemyShip.getInt("owner"));
	
		UserFlagschiffLocation flagschiffstatus = enemyuser.getFlagschiff();
		
		if( !ownuser.hasFlagschiffSpace() && (flagschiffstatus != null) && 
			(flagschiffstatus.getID() == enemyShip.getInt("id")) ) {
			return RESULT_ERROR;
		}
		
		return RESULT_OK;
	}

	@Override
	public int execute(Battle battle) {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		User user = context.getActiveUser();
		
		Database db = context.getDatabase();
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow enemyShip = battle.getEnemyShip();

		if( this.validate(battle) != RESULT_OK ) {
			battle.logme( "Sie k&ouml;nnen dieses Schiff nicht kapern" );
			return RESULT_ERROR;
		}
	
		SQLResultRow enemyShipType = Ships.getShipType( enemyShip );
		
		User euser = context.createUserObject(enemyShip.getInt("owner"));
			
		int savecrew = (int)Math.round(ownShip.getInt("crew")/10d);
		if( savecrew <= 0 ) {
			savecrew = 1;
		}
		int acrew = ownShip.getInt("crew") - savecrew;
		int dcrew = enemyShip.getInt("crew");
	
		boolean ok = false;
		int keepcrew = 0;
	
		String msg = "";
		if( (acrew != 0) && (dcrew != 0) ) {
			battle.logme("Die Crew st&uuml;rmt das Schiff\n");
			msg = "Die Crew der "+Battle.log_shiplink(ownShip)+" st&uuml;rmt die "+Battle.log_shiplink(enemyShip)+"\n";
			
			int defmulti = 1;
			
			Offizier offizier = Offizier.getOffizierByDest('s', enemyShip.getInt("id"));
			if( offizier != null ) {
				defmulti = (int)Math.round(offizier.getAbility(Offizier.Ability.SEC)/25d)+1;
			}
	
			if( acrew >= dcrew*3*defmulti ) {
				ok = true;
				battle.logme("Die Crew gibt das Schiff kampflos auf und l&auml;uft &uuml;ber\n");
				msg += "Die Crew gibt das Schiff kampflos auf l&auml;uft &uuml;ber.\n";
				keepcrew = dcrew;
			}
			else {
				//$dcrew = round(($dcrew*$defmulti - $acrew)/$defmulti);
				if( Math.round(dcrew*defmulti - acrew) > 0 ) {
					int oldacrew = acrew;
					int olddcrew = dcrew;
					acrew = (int)Math.round(acrew * 0.1);
					if( acrew < 1 ) {
						acrew = 1;
					}
	
					dcrew = (int)Math.round((dcrew*defmulti - oldacrew+acrew)/(double)defmulti);
					battle.logme((oldacrew-acrew)+" Crewmitglieder fallen. "+(olddcrew-dcrew)+" Feinde get&ouml;tet.\nAngriff abgebrochen\n");
					msg += (oldacrew-acrew)+" Angreifer erschossen. "+(olddcrew-dcrew)+" Crewmitglieder sind gefallen.\nDer Angreifer flieht\n";
					ok = false;
	
				} 
				else {
					battle.logme(Math.round(dcrew*defmulti)+" Crewmitglieder fallen. "+dcrew+" Feinde get&ouml;tet.\n[color=red]Das Schiff wurde erobert[/color]\n");
					msg += Math.round(dcrew*defmulti)+" Angreifer erschossen. "+dcrew+" Crewmitglieder sind gefallen.\n[color=red]Das Schiff ist verloren[/color]\n";
					acrew = acrew - Math.round(dcrew*defmulti);
					dcrew = 0;
					ok = true;
				}
			}
		} 
		else if( acrew != 0 ) {
			ok = true;
			battle.logme("Schiff wird widerstandslos &uuml;bernommen\n");
			msg += "Das Schiff "+Battle.log_shiplink(enemyShip)+" wird an die "+Battle.log_shiplink(ownShip)+" &uuml;bergeben\n";
		}
			
		if( acrew != 0 ) {
			battle.setPoints(battle.getOwnSide(), battle.getPoints(battle.getOwnSide()) - 5);
			
			battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
			battle.logenemy(msg);
			battle.logenemy("]]></action>\n");
			
			db.update("UPDATE ships SET crew='"+(acrew+savecrew)+"',battleAction='1' WHERE id>0 AND id=",ownShip.getInt("id"));
			db.update("UPDATE ships SET crew='",dcrew,"' WHERE id>0 AND id=",enemyShip.getInt("id"));
			
			ownShip.put("crew", acrew+savecrew);
			enemyShip.put("crew", dcrew);
		}
			
		// Wurde das Schiff gekapert?
		if( ok ) {
			// Ein neues Ziel auswaehlen
			battle.setEnemyShipIndex(battle.getNewTargetIndex());
			
			// Unbekannte Items bekannt machen
			Cargo cargo = new Cargo( Cargo.Type.STRING, enemyShip.getString("cargo") );
			
			List<ItemCargoEntry> itemlist = cargo.getItems();
			for( int i=0; i < itemlist.size(); i++ ) {
				ItemCargoEntry item = itemlist.get(i);
				
				Item itemobject = item.getItemObject();
				if( itemobject.isUnknownItem() ) {
					user.addKnownItem(item.getItemID());
				}
			}
			
			// Schiff leicht reparieren
			if( enemyShip.getInt("engine") <= 20 ) {
				enemyShip.put("engine", 20);
			}
			if( enemyShip.getInt("weapons") <= 20 ) {
				enemyShip.put("weapons", 20);
			}
				
			// Angreifer (falls ueberlebende vorhanden) auf dem Schiff stationieren
			int newshipcrew = 0;
			if( (acrew != 0) && (dcrew == 0) ) {
				newshipcrew = acrew;
				if( newshipcrew > enemyShipType.getInt("crew") ) {
					newshipcrew = enemyShipType.getInt("crew");
				}
				acrew -= newshipcrew;		
				db.update("UPDATE ships SET crew=",(acrew+savecrew),",battleAction=1 WHERE id>0 AND id=",ownShip.getInt("id"));				
				battle.logme( (newshipcrew+keepcrew)+" Crewmitglieder werden auf dem gekaperten Schiff stationiert\n" );
			}
			newshipcrew += keepcrew;
			
			String currentTime = Common.getIngameTime(context.get(ContextCommon.class).getTick());
			
			enemyShip.put("history", enemyShip.getString("history") + "Im Kampf gekapert am "+currentTime+" durch "+user.getName()+" ("+user.getID()+")\n");
			
			db.update("UPDATE ships SET owner=",user.getID(),",fleet=0,battleAction=1,engine=",enemyShip.getInt("engine"),",weapons=",enemyShip.getInt("weapons"),",crew=",newshipcrew," WHERE id>0 AND id=",enemyShip.getInt("id"));
			db.update("UPDATE battles_ships SET side=",battle.getOwnSide(),",engine=",enemyShip.getInt("engine"),",weapons=",enemyShip.getInt("weapons")," WHERE shipid=",enemyShip.getInt("id"));
				
			db.update("UPDATE ships SET owner=",user.getID(),",fleet=0,battleAction=1 WHERE id>0 AND docked IN ('",enemyShip.getInt("id"),"','l ",enemyShip.getInt("id"),"')");
				
			db.update("UPDATE offiziere SET userid=",user.getID()," WHERE dest='s ",enemyShip.getInt("id"),"'");
			if( enemyShipType.getString("werft").length() > 0 ) {
				db.update("UPDATE werften SET linked=0 WHERE shipid=",enemyShip.getInt("id"));
			}
				
			// Flagschiffeintraege aktuallisieren?
			UserFlagschiffLocation flagschiffstatus = euser.getFlagschiff();
	
			if( (flagschiffstatus != null) && (flagschiffstatus.getType() == UserFlagschiffLocation.Type.SHIP) && 
				(enemyShip.getInt("id") == flagschiffstatus.getID()) ) {
				euser.setFlagschiff(0);
				user.setFlagschiff(enemyShip.getInt("id"));
			}
			
			Common.dblog("kapern", Integer.toString(ownShip.getInt("id")), Integer.toString(enemyShip.getInt("id")), 
					"battle",	Integer.toString(battle.getID()),
					"owner",	Integer.toString(enemyShip.getInt("owner")),
					"pos",		Location.fromResult(enemyShip).toString(),
					"shiptype",	Integer.toString(enemyShip.getInt("type")) );
				
			List<Integer> kaperlist = new ArrayList<Integer>();
			kaperlist.add(enemyShip.getInt("id"));
			SQLQuery sid = db.query("SELECT id FROM ships WHERE id>0 AND docked IN ('",enemyShip.getInt("id"),"','l ",enemyShip.getInt("id"),"')");
			while( sid.next() ) {
				kaperlist.add(sid.getInt("id"));
				db.update("UPDATE battles_ships SET side=",battle.getOwnSide()," WHERE shipid=",sid.getInt("id"));
			}
			sid.free();
	
			List<SQLResultRow> enemyShips = battle.getEnemyShips();
			for( int i=0; i < enemyShips.size(); i++ ) {
				SQLResultRow eship = enemyShips.get(i);
				
				if( kaperlist.contains(eship.getInt("id")) ) {
					enemyShips.remove(i);
					i--;
					battle.getOwnShips().add(eship);
				}
			}
	
			if( enemyShips.size() < 1 ) {		
				battle.endBattle(1, 0, true);
				context.getResponse().getContent().append("Du hast das letzte gegnerische Schiff gekapert und somit die Schlacht gewonnen!");
				PM.send(context, battle.getCommander(battle.getOwnSide()), battle.getCommander(battle.getEnemySide()), "Schlacht verloren", "Du hast die Schlacht bei "+battle.getSystem()+" : "+battle.getX()+"/"+battle.getY()+" gegen "+user.getName()+" verloren, da dein letztes Schiff gekapert wurde!");
				
				return RESULT_HALT;
			}
				
			Ships.recalculateShipStatus(enemyShip.getInt("id"));
			
			enemyShip = battle.getEnemyShip();
		} 
		// Das Schiff konnte offenbar nicht gekapert werden....
		else {
			enemyShip.put("status", Ships.recalculateShipStatus(enemyShip.getInt("id")));
		}
			
		ownShip.put("status", Ships.recalculateShipStatus(ownShip.getInt("id")));
		
		battle.save(false);
		
		return RESULT_OK;
	}
}
