package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.services.ShipActionService;
import net.driftingsouls.ds2.server.services.UserService;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Component
public class SchiffHinzufuegenService
{
	@PersistenceContext
	private EntityManager em;

	private final UserService userService;
	private final ShipActionService shipActionService;

	public SchiffHinzufuegenService(UserService userService, ShipActionService shipActionService) {
		this.userService = userService;
		this.shipActionService = shipActionService;
	}

	public @NonNull Ship erstelle(@NonNull User auser, @NonNull ShipType shiptype, @NonNull Location loc) {
		return erstelle(auser, shiptype, loc, "");
	}

	public @NonNull Ship erstelle(@NonNull User auser, @NonNull ShipType shiptype, @NonNull Location loc, @NonNull String historiendaten) {
		Context context = ContextMap.getContext();

		String currentTime = Common.getIngameTime(new ConfigService().getValue(WellKnownConfigValue.TICKS));
		String history = "Indienststellung am "+currentTime+" durch "+auser.getName()+" ("+auser.getId()+")"+(historiendaten.isEmpty() ? "" : "" + historiendaten);

		Ship ship = new Ship(auser, shiptype, loc.getSystem(), loc.getX(), loc.getY());
		ship.getHistory().addHistory(history);
		ship.setCrew(shiptype.getCrew());
		ship.setHull(shiptype.getHull());
		ship.setCargo(new Cargo());
		ship.setEnergy(shiptype.getEps());
		ship.setName((userService.getSchiffsKlassenNamenGenerator(auser).generiere(shiptype.getShipClass()) + " " + userService.getSchiffsNamenGenerator(auser).generiere(shiptype)).trim());
		ship.setEngine(100);
		ship.setWeapons(100);
		ship.setComm(100);
		ship.setSensors(100);
		ship.setAblativeArmor(shiptype.getAblativeArmor());

		em.persist(ship);

		if( shiptype.getWerft() != 0 ) {
			ShipWerft awerft = new ShipWerft(ship);
			em.persist(awerft);
		}

		shipActionService.recalculateShipStatus(ship);

		return ship;
	}
}
