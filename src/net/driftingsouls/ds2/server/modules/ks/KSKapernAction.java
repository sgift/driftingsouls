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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.battles.BattleShipFlag;
import net.driftingsouls.ds2.server.battles.SchlachtLogAktion;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import net.driftingsouls.ds2.server.units.TransientUnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargo.Crew;
import net.driftingsouls.ds2.server.units.UnitType;
import net.driftingsouls.ds2.server.werften.ShipWerft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Laesst das aktuell ausgewaehlte Schiff versuchen das aktuell ausgewaehlte Zielschiff zu kapern.
 * @author Christopher Jung
 *
 */
public class KSKapernAction extends BasicKSAction {
	/**
	 * Konstruktor.
	 *
	 */
	public KSKapernAction() {
		this.requireOwnShipReady(true);
	}

	@Override
	public Result validate(Battle battle) {
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();

		if( ownShip.hasFlag(BattleShipFlag.SECONDROW) || enemyShip.hasFlag(BattleShipFlag.SECONDROW) ) {
			return Result.ERROR;
		}

		if( (ownShip.getShip().getWeapons() == 0) || (ownShip.getShip().getEngine() == 0) ||
				(ownShip.getCrew() <= 0) || ownShip.hasFlag(BattleShipFlag.FLUCHT) ||
				ownShip.hasFlag(BattleShipFlag.JOIN) || enemyShip.hasFlag(BattleShipFlag.FLUCHT) ||
				enemyShip.hasFlag(BattleShipFlag.JOIN) || enemyShip.hasFlag(BattleShipFlag.DESTROYED) ) {
			return Result.ERROR;
		}

		ShipTypeData enemyShipType = enemyShip.getTypeData();

		//		 Geschuetze sind nicht kaperbar
		if(!enemyShipType.getShipClass().isKaperbar() ||
				((enemyShipType.getCost() != 0) && (enemyShip.getShip().getEngine() != 0) && (enemyShip.getCrew() != 0)) ||
				enemyShipType.hasFlag(ShipTypeFlag.NICHT_KAPERBAR)) {
			return Result.ERROR;
		}

		if( enemyShipType.getCrew() == 0 ) {
			return Result.ERROR;
		}

		if(enemyShip.getShip().isDocked() || enemyShip.getShip().isLanded())
		{
			if(enemyShip.getShip().isLanded())
			{
				return Result.ERROR;
			}

			Ship mastership = enemyShip.getShip().getBaseShip();
			if( (mastership.getEngine() != 0) && (mastership.getCrew() != 0) ) {
				return Result.ERROR;
			}
		}

		// IFF-Stoersender
		boolean disableIFF = enemyShip.getShip().getStatus().contains("disable_iff");

		if( disableIFF ) {
			return Result.ERROR;
		}

		return Result.OK;
	}

	@Override
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();

		org.hibernate.Session db = context.getDB();
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();

		if( this.validate(battle) != Result.OK ) {
			battle.logme( "Sie k&ouml;nnen dieses Schiff nicht kapern" );
			return Result.ERROR;
		}

		ShipTypeData enemyShipType = enemyShip.getTypeData();

		Crew dcrew = new UnitCargo.Crew(enemyShip.getCrew());
		UnitCargo ownUnits = ownShip.getUnits();
		UnitCargo enemyUnits = enemyShip.getUnits();

		UnitCargo saveunits = ownUnits.trimToMaxSize(enemyShipType.getMaxUnitSize());

		boolean ok = false;


		int attmulti = 1;
		int defmulti = 1;

		Offizier defoffizier = enemyShip.getShip().getOffizier();
		if( defoffizier != null ) {
			defmulti = defoffizier.getKaperMulti(true);
		}
		Offizier attoffizier = ownShip.getShip().getOffizier();
		if( attoffizier != null)
		{
			attmulti = attoffizier.getKaperMulti(false);
		}

		String msg = "";
		if( !ownUnits.isEmpty() && !(enemyUnits.isEmpty() && enemyShip.getCrew() == 0) ) {
			battle.logme("Die Einheiten st&uuml;rmen das Schiff\n");
			msg = "Die Einheiten der "+Battle.log_shiplink(ownShip.getShip())+" stürmen die "+Battle.log_shiplink(enemyShip.getShip())+"\n";

			UnitCargo toteeigeneUnits = new TransientUnitCargo();
			UnitCargo totefeindlicheUnits = new TransientUnitCargo();

			if(ownUnits.kapern(enemyUnits, toteeigeneUnits, totefeindlicheUnits, dcrew, attmulti, defmulti ))
			{
				ok = true;
				if(toteeigeneUnits.isEmpty() && totefeindlicheUnits.isEmpty())
				{
					battle.logme("Angriff erfolgreich. Schiff wird widerstandslos &uuml;bernommen.\n");
					msg += "Das Schiff ist kampflos verloren.\n";
					if( attoffizier != null)
					{
						attoffizier.gainExperience(Offizier.Ability.COM, 5);
					}
				}
				else
				{
					battle.logme("Angriff erfolgreich.\n");
					msg += "Das Schiff ist verloren.\n";
					Map<UnitType, Long> ownunitlist = toteeigeneUnits.getUnitMap();
					Map<UnitType, Long> enemyunitlist = totefeindlicheUnits.getUnitMap();

					if(!ownunitlist.isEmpty())
					{
						battle.logme("Angreifer:\n");
						msg += "Angreifer:\n";
						for(Entry<UnitType, Long> unit : ownunitlist.entrySet())
						{
							UnitType unittype = unit.getKey();
							battle.logme(unit.getValue()+" "+unittype.getName()+" gefallen\n");
							msg += unit.getValue()+" "+unittype.getName()+" erschossen\n";
						}
					}

					if(!enemyunitlist.isEmpty())
					{
						battle.logme("Verteidiger:\n");
						msg += "Verteidiger:\n";
						for(Entry<UnitType, Long> unit : enemyunitlist.entrySet())
						{
							UnitType unittype = unit.getKey();
							battle.logme(unit.getValue()+" "+unittype.getName()+" erschossen\n");
							msg += unit.getValue()+" "+unittype.getName()+" gefallen\n";
						}
					}

					if( attoffizier != null)
					{
						attoffizier.gainExperience(Offizier.Ability.COM, 3);
					}
				}
			}
			else
			{
				battle.logme("Angriff abgebrochen.\n");
				msg += "Angreifer flieht.\n";
				Map<UnitType, Long> ownunitlist = toteeigeneUnits.getUnitMap();
				Map<UnitType, Long> enemyunitlist = totefeindlicheUnits.getUnitMap();

				if(!ownunitlist.isEmpty())
				{
					battle.logme("Angreifer:\n");
					msg += "Angreifer:\n";
					for(Entry<UnitType, Long> unit : ownunitlist.entrySet())
					{
						UnitType unittype = unit.getKey();
						battle.logme(unit.getValue()+" "+unittype.getName()+" gefallen\n");
						msg += unit.getValue()+" "+unittype.getName()+" erschossen\n";
					}
				}

				if(!enemyunitlist.isEmpty())
				{
					battle.logme("Verteidiger:\n");
					msg += "Verteidiger:\n";
					for(Entry<UnitType, Long> unit : enemyunitlist.entrySet())
					{
						UnitType unittype = unit.getKey();
						battle.logme(unit.getValue()+" "+unittype.getName()+" erschossen\n");
						msg += unit.getValue()+" "+unittype.getName()+" gefallen\n";
					}
				}

				if( defoffizier != null)
				{
					defoffizier.gainExperience(Offizier.Ability.SEC, 5);
				}
			}
		}
		else if( !ownUnits.isEmpty() ) {
			ok = true;
			if( attoffizier != null)
			{
				attoffizier.gainExperience(Offizier.Ability.COM, 5);
			}
			battle.logme("Schiff wird widerstandslos &uuml;bernommen\n");
			msg += "Das Schiff "+Battle.log_shiplink(enemyShip.getShip())+" wird an die "+Battle.log_shiplink(ownShip.getShip())+" übergeben\n";
		}

		ownUnits.addCargo(saveunits);

		battle.log(new SchlachtLogAktion(battle.getOwnSide(), msg));
		ownShip.getShip().setBattleAction(true);
		ownShip.setUnits(ownUnits);

		enemyShip.setUnits(enemyUnits);
		enemyShip.getShip().setCrew(dcrew.getValue());

		// Wurde das Schiff gekapert?
		if( ok ) {
			// Unbekannte Items bekannt machen
			Cargo cargo = enemyShip.getCargo();

			List<ItemCargoEntry> itemlist = cargo.getItems();
			for (ItemCargoEntry item : itemlist)
			{
				Item itemobject = item.getItem();
				if (itemobject.isUnknownItem())
				{
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

			String currentTime = Common.getIngameTime(context.get(ContextCommon.class).getTick());

			enemyShip.getShip().getHistory().addHistory("Im Kampf gekapert am "+currentTime+" durch "+user.getName()+" ("+user.getId()+")");

			enemyShip.getShip().removeFromFleet();
			enemyShip.getShip().setOwner(user);
			enemyShip.getShip().setBattleAction(true);
			enemyShip.setSide(battle.getOwnSide());
            if(enemyShip.getShip().isDocked())
            {
                enemyShip.getShip().setDocked("");
            }

			List<Integer> kaperlist = new ArrayList<>();
			kaperlist.add(enemyShip.getId());

			List<Ship> docked = Common.cast(db.createQuery("from Ship where id>0 and docked in (:docked,:landed)")
					.setString("docked", Integer.toString(enemyShip.getId()))
					.setString("landed", "l "+enemyShip.getId())
					.list());
			for( Ship dockShip : docked )
			{
				dockShip.removeFromFleet();
				dockShip.setOwner(user);
				dockShip.setBattleAction(true);

				BattleShip bDockShip = (BattleShip)db.get(BattleShip.class, dockShip.getId());
				bDockShip.setSide(battle.getOwnSide());

				for( Offizier offi : dockShip.getOffiziere() )
				{
					offi.setOwner(user);
				}
				if( dockShip.getTypeData().getWerft() != 0 ) {
					ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where ship=:ship")
					.setEntity("ship", dockShip)
					.uniqueResult();

					if( werft.getKomplex() != null ) {
						werft.removeFromKomplex();
					}
					werft.setLink(null);
				}

				kaperlist.add(bDockShip.getId());
			}

			for( Offizier offi : enemyShip.getShip().getOffiziere() )
			{
				offi.setOwner(user);
			}
			if( enemyShipType.getWerft() != 0 ) {
				ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where ship=:ship")
				.setEntity("ship", enemyShip)
				.uniqueResult();

				if( werft.getKomplex() != null ) {
					werft.removeFromKomplex();
				}
				werft.setLink(null);
			}

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
				battle.endBattle(1, 0);

				User commander = battle.getCommander(battle.getOwnSide());

				context.getResponse().getWriter()
						.append("Du hast das letzte gegnerische Schiff gekapert und somit die Schlacht bei <a class='forschinfo' href='./client#/map/")
						.append(battle.getLocation().urlFragment()).append("'>")
						.append(battle.getLocation().displayCoordinates(false))
						.append("</a> gewonnen!");
				PM.send(commander, battle.getCommander(battle.getEnemySide()).getId(), "Schlacht verloren", "Du hast die Schlacht bei "+battle.getLocation().displayCoordinates(false)+" gegen "+user.getName()+" verloren, da dein letztes Schiff gekapert wurde!");

				return Result.HALT;
			}

			if( !battle.isValidTarget() ) {
				int newindex = battle.getNewTargetIndex();
				if( newindex == -1)
				{
					newindex = 0;
				}
				battle.setEnemyShipIndex(newindex);
			}

			enemyShip.getShip().recalculateShipStatus();
		}
		// Das Schiff konnte offenbar nicht gekapert werden....
		else {
			enemyShip.getShip().recalculateShipStatus();
		}

		ownShip.getShip().recalculateShipStatus();

		return Result.OK;
	}
}
