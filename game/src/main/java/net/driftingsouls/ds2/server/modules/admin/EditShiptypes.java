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
package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.config.ConfigFelsbrocken;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.config.items.Schiffsbauplan;
import net.driftingsouls.ds2.server.config.items.Schiffsverbot;
import net.driftingsouls.ds2.server.entities.npcorders.OrderShip;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableShip;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.services.ShipActionService;
import net.driftingsouls.ds2.server.ships.*;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aktualisierungstool fuer die Werte von Schiffstypen.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Schiffe", name = "Typ", permission = WellKnownAdminPermission.EDIT_SHIPTYPES)
@Component
public class EditShiptypes implements EntityEditor<ShipType>
{
	@PersistenceContext
	private EntityManager em;

	private final ShipService shipService;
	private final ShipActionService shipActionService;

	public EditShiptypes(ShipService shipService, ShipActionService shipActionService) {
		this.shipService = shipService;
		this.shipActionService = shipActionService;
	}

	@Override
	public Class<ShipType> getEntityType()
	{
		return ShipType.class;
	}

	@Override
	public void configureFor(@NonNull EditorForm8<ShipType> form)
	{
		form.allowAdd();
		form.allowDelete(this::isDeleteAllowed);
		form.ifUpdating().label("Anzahl vorhandener Schiffe", ship -> em.createQuery("select count(*) from Ship s where s.shiptype=:type", Long.class)
				.setParameter("type", ship)
				.getSingleResult());
		form.field("Name", String.class, ShipType::getNickname, ShipType::setNickname);
		form.picture("Bild", ShipType::getPicture);
		form.field("Uranreaktor", Integer.class, ShipType::getRu, ShipType::setRu);
		form.field("Deuteriumreaktor", Integer.class, ShipType::getRd, ShipType::setRd);
		form.field("Antimateriereaktor", Integer.class, ShipType::getRa, ShipType::setRa);
		form.field("Reaktor maximal", Integer.class, ShipType::getRm, ShipType::setRm);
		form.field("EPS", Integer.class, ShipType::getEps, ShipType::setEps);
		form.field("Flugkosten", Integer.class, ShipType::getCost, ShipType::setCost);
		form.field("Hülle", Integer.class, ShipType::getHull, ShipType::setHull);
		form.field("Panzerung", Integer.class, ShipType::getPanzerung, ShipType::setPanzerung);
		form.field("Cargo", Long.class, ShipType::getCargo, ShipType::setCargo);
		form.field("Nahrungsspeicher", Long.class, ShipType::getNahrungCargo, ShipType::setNahrungCargo);
		form.field("Hitze", Integer.class, ShipType::getHeat, ShipType::setHeat);
		form.field("Crew", Integer.class, ShipType::getCrew, ShipType::setCrew);
		form.field("maximale Größe für Einheiten", Integer.class, ShipType::getMaxUnitSize, ShipType::setMaxUnitSize);
		form.field("Laderaum für Einheiten", Integer.class, ShipType::getUnitSpace, ShipType::setUnitSpace);
		form.field("Waffen", String.class, st -> Weapons.packWeaponList(st.getWeapons()), (st,s) -> st.setWeapons(Weapons.parseWeaponList(s)));
		form.field("Maximale Hitze", String.class, st -> Weapons.packWeaponList(st.getMaxHeat()), (st,s) -> st.setMaxHeat(Weapons.parseWeaponList(s)));
		form.field("Torpedoabwehr", Integer.class, ShipType::getTorpedoDef, ShipType::setTorpedoDef);
		form.field("Schilde", Integer.class, ShipType::getShields, ShipType::setShields);
		form.field("Größe", Integer.class, ShipType::getSize, ShipType::setSize);
		form.field("Jäger- / Bomberdocks", Integer.class, ShipType::getJDocks, ShipType::setJDocks);
		form.field("externe Docks", Integer.class, ShipType::getADocks, ShipType::setADocks);
		form.field("Sensorreichweite", Integer.class, ShipType::getSensorRange, ShipType::setSensorRange);
		form.field("Hydros", Integer.class, ShipType::getHydro, ShipType::setHydro);
		form.field("RE-Kosten", Integer.class, ShipType::getReCost, ShipType::setReCost);
		form.textArea("Beschreibung", ShipType::getDescrip, ShipType::setDescrip);
		form.field("Deuteriumsammeln", Integer.class, ShipType::getDeutFactor, ShipType::setDeutFactor);

		Map<ShipClasses, String> shipClasses = Arrays.stream(ShipClasses.values()).collect(Collectors.toMap(sc -> sc, ShipClasses::getSingular));
		form.field("Schiffsklasse", ShipClasses.class, ShipType::getShipClass, ShipType::setShipClass).withOptions(shipClasses).dbColumn(ShipType_.shipClass);
		form.multiSelection("Flags", ShipTypeFlag.class, ShipType::getFlags, (st, flags) -> st.setFlags(flags.stream().map(ShipTypeFlag::getFlag).collect(Collectors.joining(" "))))
			.withOptions(Arrays.stream(ShipTypeFlag.values()).collect(Collectors.toMap(f -> f, ShipTypeFlag::getLabel)));
		form.field("Groupwrap", Integer.class, ShipType::getGroupwrap, ShipType::setGroupwrap);
		form.field("Werft (Slots)", Integer.class, ShipType::getWerft, ShipType::setWerft);
		form.field("Einmalwerft", ShipType.class, ShipType::getOneWayWerft, ShipType::setOneWayWerft).withNullOption("Deaktiviert");
		form.field("Loot-Chance", Integer.class, ShipType::getChance4Loot, ShipType::setChance4Loot);
		form.field("Module", String.class, ShipType::getModules, ShipType::setModules);
		form.field("Verstecken", Boolean.class, ShipType::isHide, ShipType::setHide);
		form.field("ablative Panzerung", Integer.class, ShipType::getAblativeArmor, ShipType::setAblativeArmor);
		form.field("besitzt SRS", Boolean.class, ShipType::hasSrs, ShipType::setSrs);
		form.field("Mindest-Crew", Integer.class, ShipType::getMinCrew, ShipType::setMinCrew);
		form.field("EMP verfliegen", Double.class, ShipType::getLostInEmpChance, ShipType::setLostInEmpChance);
		form.field("Kopfgeld", BigInteger.class, ShipType::getBounty, ShipType::setBounty);

		form.postUpdateTask("Schiffe aktualisieren",
				(ShipType shiptype) -> em.createQuery("select s.id from Ship s where s.shiptype= :type", Integer.class)
						.setParameter("type", shiptype)
						.getResultList(),
				(ShipType oldShiptype, ShipType shipType, Integer shipId) -> aktualisiereSchiff(oldShiptype, shipId)
		);
	}

	private boolean isDeleteAllowed(ShipType st)
	{
		long count = em.createQuery("select count(*) from Ship s where s.shiptype=:type", Long.class)
				.setParameter("type", st)
				.getSingleResult();

		if( count > 0 )
		{
			return false;
		}

		var shipBaubar = em.createQuery("from ShipBaubar where type=:type", ShipBaubar.class)
				.setMaxResults(1)
				.setParameter("type", st)
				.getSingleResult();
		if( shipBaubar != null )
		{
			return false;
		}

		var configFelsBrocken = em.createQuery("from ConfigFelsbrocken where shiptype=:type", ConfigFelsbrocken.class)
				.setMaxResults(1)
				.setParameter("type", st)
				.getSingleResult();
		if( configFelsBrocken != null )
		{
			return false;
		}

		var blueprint = em.createQuery("from Schiffsbauplan where schiffstyp=:type", Schiffsbauplan.class)
				.setMaxResults(1)
				.setParameter("type", st)
				.getSingleResult();
		if( blueprint != null )
		{
			return false;
		}

		var shipBan = em.createQuery("from Schiffsverbot where schiffstyp=:type", Schiffsverbot.class)
				.setMaxResults(1)
				.setParameter("type", st)
				.getSingleResult();
		if( shipBan != null )
		{
			return false;
		}

		var orderableShip = em.createQuery("from OrderableShip where shipType=:type", OrderableShip.class)
				.setMaxResults(1)
				.setParameter("type", st)
				.getSingleResult();
		if( orderableShip != null )
		{
			return false;
		}

		var orderShip = em.createQuery("from OrderShip where shipType=:type", OrderShip.class)
				.setMaxResults(1)
				.setParameter("type", st)
				.getSingleResult();
		if( orderShip != null )
		{
			return false;
		}

		var shipType = em.createQuery("from ShipType where oneWayWerft=:type", ShipType.class)
				.setMaxResults(1)
				.setParameter("type", st)
				.getSingleResult();
		if( shipType != null )
		{
			return false;
		}

		var shipModules = em.createQuery("from ShipModules where oneWayWerft=:type", ShipModules.class)
				.setMaxResults(1)
				.setParameter("type", st)
				.getResultList();
		return shipModules == null;
	}

	private void aktualisiereSchiff(ShipType oldShiptype, Integer shipId)
	{
		Ship ship = em.find(Ship.class, shipId);
		boolean modules = ship.getModuleEntries().length > 0;
        // Clone bei Modulen notwendig. Sonst werden auch die gespeicherten neu berechnet.
        ShipTypeData oldType;
        try{
            oldType = modules ? (ShipTypeData)ship.getTypeData().clone() : oldShiptype;
        }
        catch(CloneNotSupportedException e)
        {
            oldType = oldShiptype;
        }

		shipService.recalculateModules(ship);
		ShipTypeData type = ship.getTypeData();

		ship.setEnergy((int) Math.floor(type.getEps() * (ship.getEnergy() / (double) oldType.getEps())));
		ship.setHull((int) Math.floor(type.getHull() * (ship.getHull() / (double) oldType.getHull())));
		ship.setCrew((int) Math.floor(type.getCrew() * (ship.getCrew() / (double)oldType.getCrew())));
		ship.setShields((int) Math.floor(type.getShields() * (ship.getShields() / (double) oldType.getShields())));
		ship.setAblativeArmor((int) Math.floor(type.getAblativeArmor() * (ship.getAblativeArmor() / (double) oldType.getAblativeArmor())));
		ship.setNahrungCargo((long) Math.floor(type.getNahrungCargo() * (ship.getNahrungCargo() / (double) oldType.getNahrungCargo())));

		int fighterDocks = ship.getTypeData().getJDocks();
		if (shipService.getLandedCount(ship) > fighterDocks)
		{
			List<Ship> fighters = shipService.getLandedShips(ship);
			long toStart = fighters.size() - fighterDocks;
			int fighterCount = 0;

			for (Iterator<Ship> iter2 = fighters.iterator(); iter2.hasNext() && fighterCount < toStart; )
			{
				Ship fighter = iter2.next();

				fighter.setDocked("");
				fighterCount++;
			}
		}

		//Docked
		int outerDocks = ship.getTypeData().getADocks();
		if (shipService.getDockedCount(ship) > outerDocks)
		{
			List<Ship> outerDocked = shipService.getDockedShips(ship);
			long toStart = outerDocked.size() - outerDocks;
			int dockedCount = 0;

			for (Iterator<?> iter2 = outerDocked.iterator(); iter2.hasNext() && dockedCount < toStart; )
			{
				Ship outer = (Ship) iter2.next();
				outer.setDocked("");

				dockedCount++;
			}
		}

		if (ship.getId() >= 0)
		{
			shipActionService.recalculateShipStatus(ship);
		}

        if(ship.getBattle() != null)
        {
            BattleShip battleShip = em.find(BattleShip.class, shipId);
            battleShip.setHull((int) Math.floor(type.getHull() * (battleShip.getHull() / (double) oldType.getHull())));
            battleShip.setShields((int) Math.floor(type.getShields() * (battleShip.getShields() / (double) oldType.getShields())));
            battleShip.setAblativeArmor((int) Math.floor(type.getAblativeArmor() * (battleShip.getAblativeArmor() / (double) oldType.getAblativeArmor())));
        }
	}
}
