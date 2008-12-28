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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlagschiffLocation;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.werften.ShipWerft;

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
		this.requireOwnShipReady(true);
	}
	
	@Override
	public int validate(Battle battle) {
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();
		
		if( (ownShip.getAction() & Battle.BS_SECONDROW) != 0 ||
			(enemyShip.getAction() & Battle.BS_SECONDROW) != 0 ) {
			return RESULT_ERROR;
		}
		
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		
		if( (ownShip.getShip().getWeapons() == 0) || (ownShip.getShip().getEngine() == 0) || 
			(ownShip.getCrew() <= 0) || (ownShip.getAction() & Battle.BS_FLUCHT) != 0 ||
			(ownShip.getAction() & Battle.BS_JOIN) != 0 || (enemyShip.getAction() & Battle.BS_FLUCHT) != 0 ||
			(enemyShip.getAction() & Battle.BS_JOIN) != 0 || (enemyShip.getAction() & Battle.BS_DESTROYED) != 0 ) {
			return RESULT_ERROR;
		}
		
		ShipTypeData enemyShipType = enemyShip.getTypeData();
	
//		 Geschuetze sind nicht kaperbar
		if( (enemyShipType.getShipClass() == ShipClasses.GESCHUETZ.ordinal() ) || 
			((enemyShipType.getCost() != 0) && (enemyShip.getShip().getEngine() != 0) && (enemyShip.getCrew() != 0)) ||
			(ownShip.getCrew() == 0) || enemyShipType.hasFlag(ShipTypes.SF_NICHT_KAPERBAR) ) {
			return RESULT_ERROR;
		}
		
		if( enemyShipType.getCrew() == 0 ) {
			return RESULT_ERROR;
		}
	
		if( enemyShip.getDocked().length() > 0 ) {
			if( enemyShip.getDocked().charAt(0) == 'l' ) {
				return RESULT_ERROR;
			} 

			Ship mastership = (Ship)db.get(Ship.class, Integer.parseInt(enemyShip.getDocked()));
			if( (mastership.getEngine() != 0) && (mastership.getCrew() != 0) ) {
				return RESULT_ERROR;
			}
		}
	
		// IFF-Stoersender
		boolean disableIFF = enemyShip.getShip().getStatus().indexOf("disable_iff") > -1;	
		
		if( disableIFF ) {
			return RESULT_ERROR;
		}
	
		//Flagschiff?
		User ownuser = (User)context.getActiveUser();
		User enemyuser = enemyShip.getOwner();
	
		UserFlagschiffLocation flagschiffstatus = enemyuser.getFlagschiff();
		
		if( !ownuser.hasFlagschiffSpace() && (flagschiffstatus != null) && 
			(flagschiffstatus.getID() == enemyShip.getId()) ) {
			return RESULT_ERROR;
		}
		
		return RESULT_OK;
	}

	@Override
	public int execute(Battle battle) throws IOException {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();
		
		org.hibernate.Session db = context.getDB();
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();

		if( this.validate(battle) != RESULT_OK ) {
			battle.logme( "Sie k&ouml;nnen dieses Schiff nicht kapern" );
			return RESULT_ERROR;
		}
	
		ShipTypeData enemyShipType = enemyShip.getTypeData();
		
		User euser = enemyShip.getOwner();
			
		int savecrew = (int)Math.round(ownShip.getCrew()/10d);
		if( savecrew <= 0 ) {
			savecrew = 1;
		}
		int acrew = ownShip.getCrew() - savecrew;
		int dcrew = enemyShip.getCrew();
	
		boolean ok = false;

		String msg = "";
		if( (acrew != 0) && (dcrew != 0) ) {
			battle.logme("Die Crew st&uuml;rmt das Schiff\n");
			msg = "Die Crew der "+Battle.log_shiplink(ownShip.getShip())+" st&uuml;rmt die "+Battle.log_shiplink(enemyShip.getShip())+"\n";
			
			int defmulti = 1;
			
			Offizier offizier = Offizier.getOffizierByDest('s', enemyShip.getId());
			if( offizier != null ) {
				defmulti = (int)Math.round(offizier.getAbility(Offizier.Ability.SEC)/25d)+1;
			}
	
			if( acrew >= dcrew*3*defmulti ) {
				ok = true;
				battle.logme("Die Crew gibt das Schiff kampflos auf und l&auml;uft &uuml;ber\n");
				msg += "Die Crew gibt das Schiff kampflos auf l&auml;uft &uuml;ber.\n";
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
			msg += "Das Schiff "+Battle.log_shiplink(enemyShip.getShip())+" wird an die "+Battle.log_shiplink(ownShip.getShip())+" &uuml;bergeben\n";
		}
			
		if( acrew != 0 ) {
			battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
			battle.logenemy(msg);
			battle.logenemy("]]></action>\n");
			
			ownShip.getShip().setCrew(acrew+savecrew);
			ownShip.getShip().setBattleAction(true);
		
			enemyShip.getShip().setCrew(dcrew);
		}
			
		// Wurde das Schiff gekapert?
		if( ok ) {
			// Unbekannte Items bekannt machen
			Cargo cargo = enemyShip.getCargo();
			
			List<ItemCargoEntry> itemlist = cargo.getItems();
			for( int i=0; i < itemlist.size(); i++ ) {
				ItemCargoEntry item = itemlist.get(i);
				
				Item itemobject = item.getItemObject();
				if( itemobject.isUnknownItem() ) {
					user.addKnownItem(item.getItemID());
				}
			}
			
			// Schiff leicht reparieren
			if( enemyShip.getShip().getEngine() <= 20 ) {
				enemyShip.getShip().setEngine(20);
				enemyShip.setEngine(20);
			}
			if( enemyShip.getShip().getWeapons() <= 20 ) {
				enemyShip.getShip().setWeapons(20);
				enemyShip.setWeapons(20);
			}
				
			// Angreifer (falls ueberlebende vorhanden) auf dem Schiff stationieren
			int newshipcrew = enemyShip.getShip().getCrew();
			if( (acrew != 0) && (dcrew == 0) ) {
				newshipcrew = acrew/2;
				if( newshipcrew > enemyShipType.getCrew() ) {
					newshipcrew = enemyShipType.getCrew();
				}
				acrew = acrew -newshipcrew;		
				
				ownShip.getShip().setCrew(acrew);
				
				battle.logme( (newshipcrew)+" Crewmitglieder werden auf dem gekaperten Schiff stationiert\n" );
			}
			
			String currentTime = Common.getIngameTime(context.get(ContextCommon.class).getTick());
			
			enemyShip.getShip().setHistory(enemyShip.getShip().getHistory()+"Im Kampf gekapert am "+currentTime+" durch "+user.getName()+" ("+user.getId()+")\n");
			
			enemyShip.getShip().removeFromFleet();
			enemyShip.getShip().setOwner(user);
			enemyShip.getShip().setBattleAction(true);
			enemyShip.getShip().setCrew(newshipcrew);
			enemyShip.setSide(battle.getOwnSide());
			
			List<Integer> kaperlist = new ArrayList<Integer>();
			kaperlist.add(enemyShip.getId());
			
			List<Ship> docked = Common.cast(db.createQuery("from Ship where id>0 and docked in (?,?)")
				.setString(0, Integer.toString(enemyShip.getId()))
				.setString(1, "l "+enemyShip.getId())
				.list());
			for( Ship dockShip : docked )
			{
				dockShip.removeFromFleet();
				dockShip.setOwner(user);
				dockShip.setBattleAction(true);
				
				BattleShip bDockShip = (BattleShip)db.get(BattleShip.class, dockShip.getId());
				bDockShip.setSide(battle.getOwnSide());
				
				db.createQuery("update Offizier set userid=? where dest=?")
					.setEntity(0, user)
					.setString(1, "s "+dockShip.getId())
					.executeUpdate();
				if( dockShip.getTypeData().getWerft() != 0 ) {
					ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where ship=?")
						.setEntity(0, dockShip)
						.uniqueResult();
							
					if( werft.getKomplex() != null ) {
						werft.removeFromKomplex();
					}
					werft.setLink(null);
				}
				
				kaperlist.add(bDockShip.getId());
			}
			
			db.createQuery("update Offizier set userid=? where dest=?")
				.setEntity(0, user)
				.setString(1, "s "+enemyShip.getId())
				.executeUpdate();
			if( enemyShipType.getWerft() != 0 ) {
				ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where ship=?")
					.setEntity(0, enemyShip)
					.uniqueResult();
					
				if( werft.getKomplex() != null ) {
					werft.removeFromKomplex();
				}
				werft.setLink(null);
			}
			
			// Flagschiffeintraege aktuallisieren?
			UserFlagschiffLocation flagschiffstatus = euser.getFlagschiff();
	
			if( (flagschiffstatus != null) && (flagschiffstatus.getType() == UserFlagschiffLocation.Type.SHIP) && 
				(enemyShip.getId() == flagschiffstatus.getID()) ) {
				euser.setFlagschiff(null);
				user.setFlagschiff(enemyShip.getId());
			}
			
			Common.dblog("kapern", Integer.toString(ownShip.getId()), Integer.toString(enemyShip.getId()), 
					"battle",	Integer.toString(battle.getId()),
					"owner",	Integer.toString(enemyShip.getOwner().getId()),
					"pos",		enemyShip.getShip().getLocation().toString(),
					"shiptype",	Integer.toString(enemyShip.getShip().getType()) );
				
			// TODO: Das Entfernen eines Schiffes aus der Liste sollte in Battle 
			// durchgefuehrt werden und den Zielindex automatisch anpassen
			// (durch das Entfernen von Schiffen kann der Zielindex ungueltig geworden sein)
			
			// Ein neues Ziel auswaehlen
			//battle.setEnemyShipIndex(battle.getNewTargetIndex());
			
			List<BattleShip> enemyShips = battle.getEnemyShips();
			for( int i=0; i < enemyShips.size(); i++ ) {
				BattleShip eship = enemyShips.get(i);
				
				if( kaperlist.contains(eship.getId()) ) {
					enemyShips.remove(i);
					i--;
					battle.getOwnShips().add(eship);
				}
			}
	
			if( enemyShips.size() < 1 ) {		
				battle.endBattle(1, 0, true);
				
				User commander = battle.getCommander(battle.getOwnSide());
				
				context.getResponse().getWriter().append("Du hast das letzte gegnerische Schiff gekapert und somit die Schlacht gewonnen!");
				PM.send(commander, battle.getCommander(battle.getEnemySide()).getId(), "Schlacht verloren", "Du hast die Schlacht bei "+battle.getLocation()+" gegen "+user.getName()+" verloren, da dein letztes Schiff gekapert wurde!");
				
				return RESULT_HALT;
			}
			
			if( !battle.isValidTarget() ) {
				battle.setEnemyShipIndex(battle.getNewTargetIndex());
			}
			
			enemyShip.getShip().recalculateShipStatus();
			
			enemyShip = battle.getEnemyShip();
		} 
		// Das Schiff konnte offenbar nicht gekapert werden....
		else {
			enemyShip.getShip().recalculateShipStatus();
		}
			
		ownShip.getShip().recalculateShipStatus();
		
		battle.resetInactivity();
		
		return RESULT_OK;
	}
}
