package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.shipyards.ShipWerft;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;

public class ShipCreationService
{
	public @Nonnull Ship create(@Nonnull User auser, @Nonnull ShipType shiptype, @Nonnull Location loc, @Nonnull EntityManager db) {
		return create(auser, shiptype, loc, "", db);
	}

	public @Nonnull Ship create(@Nonnull User auser, @Nonnull ShipType shiptype, @Nonnull Location loc, @Nonnull String historyData, @Nonnull EntityManager db) {
		Context context = ContextMap.getContext();
		String currentTime = Common.getIngameTime(context.get(ContextCommon.class).getTick());
		String history = "Indienststellung am "+currentTime+" durch "+auser.getName()+" ("+auser.getId()+")"+(historyData.isEmpty() ? "" : "" + historyData);

		Ship ship = new Ship(auser, shiptype, loc.getSystem(), loc.getX(), loc.getY());
		ship.getHistory().addHistory(history);
		ship.setCrew(shiptype.getCrew());
		ship.setHull(shiptype.getHull());
		ship.setCargo(new Cargo());
		ship.setEnergy(shiptype.getEps());
		ship.setName((auser.getSchiffsKlassenNamenGenerator().generate(shiptype.getShipClass()) + " " + auser.getSchiffsNamenGenerator().generate(shiptype)).trim());
		ship.setEngine(100);
		ship.setWeapons(100);
		ship.setComm(100);
		ship.setSensors(100);
		ship.setAblativeArmor(shiptype.getAblativeArmor());

		db.persist(ship);

		if( shiptype.getWerft() != 0 ) {
			ShipWerft awerft = new ShipWerft(ship);
			db.persist(awerft);
		}

		ship.recalculateShipStatus();

		return ship;
	}
}
