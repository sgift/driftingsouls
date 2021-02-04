package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.services.BattleService;
import net.driftingsouls.ds2.server.services.DismantlingService;
import net.driftingsouls.ds2.server.services.FleetMgmtService;
import net.driftingsouls.ds2.server.services.LocationService;
import net.driftingsouls.ds2.server.services.NebulaService;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.services.ShipActionService;
import net.driftingsouls.ds2.server.services.UserService;
import org.springframework.stereotype.Service;

import org.springframework.lang.NonNull;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service zum Fliegen von Schiffen.
 */
@Service
public class SchiffFlugService
{
	@PersistenceContext
	private EntityManager em;

	private final BattleService battleService;
	private final ConfigService configService;
	private final UserService userService;
	private final ShipService shipService;
	private final NebulaService nebulaService;
	private final LocationService locationService;
	private final FleetMgmtService fleetMgmtService;
	private final DismantlingService dismantlingService;
	private final ShipActionService shipActionService;

	public SchiffFlugService(BattleService battleService, ConfigService configService, UserService userService, ShipService shipService, NebulaService nebulaService, LocationService locationService, FleetMgmtService fleetMgmtService, DismantlingService dismantlingService, ShipActionService shipActionService)
	{
		this.battleService = battleService;
		this.configService = configService;
		this.userService = userService;
		this.shipService = shipService;
		this.nebulaService = nebulaService;
		this.locationService = locationService;
		this.fleetMgmtService = fleetMgmtService;
		this.dismantlingService = dismantlingService;
		this.shipActionService = shipActionService;
	}

	/**
	 * Die verschiedenen Zustaende, die zum Ende eines Fluges gefuehrt haben koennen.
	 */
	public enum FlugStatus
	{
		/**
		 * Der Flug war Erfolgreich.
		 */
		SUCCESS,
		/**
		 * Der Flug wurde an einem EMP-Nebel abgebrochen.
		 */
		BLOCKED_BY_EMP,
		/**
		 * Der Flug wurde vor einem Feld mit rotem Alarm abgebrochen.
		 */
		BLOCKED_BY_ALERT,
		/**
		 * Das Schiff konnte nicht mehr weiterfliegen.
		 */
		SHIP_FAILURE,
		/**
		 * Der Flug wurde an einem Schadensnebel abgebrochen.
		 */
		BLOCKED_BY_DAMAGE_NEBULA,
	}

	private static class MovementResult
	{
		final int distance;
		final boolean moved;
		final FlugStatus status;

		MovementResult(int distance, boolean moved, FlugStatus status)
		{
			this.distance = distance;
			this.moved = moved;
			this.status = status;
		}
	}



	private MovementResult moveSingle(Ship ship, ShipTypeData shiptype, Offizier offizier, int direction, int distance, long adocked, boolean forceLowHeat, StringBuilder out) {
		boolean moved = false;
		FlugStatus status = FlugStatus.SUCCESS;

		if( ship.getEngine() <= 0 ) {
			distance = 0;

			return new MovementResult(distance, false, FlugStatus.SHIP_FAILURE);
		}

		int newe = ship.getEnergy() - shiptype.getCost();
		int news = ship.getHeat() + shiptype.getHeat();

		newe -= adocked;
		if( shiptype.getMinCrew() > ship.getCrew() ) {
			newe--;
		}

		// Antrieb teilweise beschaedigt?
		if( ship.getEngine() < 20 ) {
			newe -= 4;
		}
		else if( ship.getEngine() < 40 ) {
			newe -= 2;
		}
		else if( ship.getEngine() < 60 ) {
			newe -= 1;
		}

		if( newe < 0 ) {
			out.append(ship.getName()).append(" (").append(ship.getId()).append("): ");
			out.append("<span style=\"color:#ff0000\">Keine Energie. Stoppe bei ").append(locationService.displayCoordinates(ship.getLocation(), true)).append("</span><br />\n");
			distance = 0;

			return new MovementResult(distance, false, FlugStatus.SHIP_FAILURE);
		}

		if( offizier != null ) {
			// Flugkosten
			int success = offizier.useAbility( Offizier.Ability.NAV, 200 );
			if( success > 0 ) {
				newe += 2;
				if( newe > ship.getEnergy()-1 ) {
					newe = ship.getEnergy() - 1;
				}
			}
			// Ueberhitzung
			success = offizier.useAbility( Offizier.Ability.ING, 200 );
			if( success > 0 ) {
				news -= 1;
				if( news < ship.getHeat()+2 ) {
					news = ship.getHeat()+2;
				}
			}
		}

		// Grillen wir uns bei dem Flug eventuell den Antrieb?
		if( news > 100 )  {
			if(forceLowHeat && distance > 0) {
				out.append(ship.getName()).append(" (").append(ship.getId()).append("): ");
				out.append("<span style=\"color:#ff0000\">Triebwerk w&uuml;rde &uuml;berhitzen</span><br />\n");

				out.append("<span style=\"color:#ff0000\">Autopilot bricht ab bei ").append(locationService.displayCoordinates(ship.getLocation(), true)).append("</span><br />\n");
				out.append("</span></td></tr>\n");
				distance = 0;
				return new MovementResult(distance, false, FlugStatus.SHIP_FAILURE);
			}
		}

		int x = ship.getX();
		int y = ship.getY();

		if( direction == 1 ) { x--; y--; }
		else if( direction == 2 ) { y--; }
		else if( direction == 3 ) { x++; y--; }
		else if( direction == 4 ) { x--; }
		else if( direction == 6 ) { x++; }
		else if( direction == 7 ) { x--; y++; }
		else if( direction == 8 ) { y++; }
		else if( direction == 9 ) { x++; y++; }

		StarSystem sys = em.find(StarSystem.class, ship.getSystem());

		if( x > sys.getWidth()) {
			x = sys.getWidth();
			distance = 0;
		}
		if( y > sys.getHeight()) {
			y = sys.getHeight();
			distance = 0;
		}
		if( x < 1 ) {
			x = 1;
			distance = 0;
		}
		if( y < 1 ) {
			y = 1;
			distance = 0;
		}

		if( (ship.getX() != x) || (ship.getY() != y) ) {
			moved = true;

			if( ship.getHeat() >= 100 ) {
				out.append(ship.getName()).append(" (").append(ship.getId()).append("): ");
				out.append("<span style=\"color:#ff0000\">Triebwerke &uuml;berhitzt</span><br />\n");

				if( (ThreadLocalRandom.current().nextInt(101)) < 3*(news-100) ) {
					int dmg = (int)( (2*(ThreadLocalRandom.current().nextInt(101)/100d)) + 1 ) * (news-100);
					out.append("<span style=\"color:#ff0000\">Triebwerke nehmen ").append(dmg).append(" Schaden</span><br />\n");
					ship.setEngine(ship.getEngine()-dmg);
					if( ship.getEngine() < 0 ) {
						ship.setEngine(0);
					}
					if( distance > 0 ) {
						out.append("<span style=\"color:#ff0000\">Autopilot bricht ab bei ").append(locationService.displayCoordinates(ship.getLocation(), true)).append("</span><br />\n");
						status = FlugStatus.SHIP_FAILURE;
						distance = 0;
					}
				}
			}

			Nebel.Typ nebula = nebulaService.getNebula(new Location(ship.getSystem(), x, y));
			if(nebula != null) {
				nebula.damageShip(ship, configService, shipService, dismantlingService);
			}

			ship.setX(x);
			ship.setY(y);
			ship.setEnergy(newe);
			ship.setHeat(news);

			if(ship.isDestroyed()) {
				out.append("<span style=\"color:#ff0000\">Das Schiff wurde beim Einflug in den Sektor ").append(locationService.displayCoordinates(ship.getLocation(), true)).append(" durch ein Raumphänomen zerstört</span><br />\n");
				status = FlugStatus.SHIP_FAILURE;
				distance = 0;
			} else if(ship.getEngine() == 0) {
				out.append("<span style=\"color:#ff0000\">Die Triebwerke sind zerstört</span><br />\n");
				out.append("<span style=\"color:#ff0000\">Autopilot bricht ab bei ").append(locationService.displayCoordinates(ship.getLocation(), true)).append("</span><br />\n");
				status = FlugStatus.SHIP_FAILURE;
				distance = 0;
			}
		}

		return new MovementResult(distance, moved, status);
	}

	/**
	 * Enthaelt die Daten der Schiffe in einer Flotte, welche sich gerade bewegt.
	 *
	 */
	private static class FleetMovementData {
		FleetMovementData() {
			// EMPTY
		}

		/**
		 * Die Schiffe in der Flotte.
		 */
        final Map<Integer,Ship> ships = new HashMap<>();
		/**
		 * Die Offiziere auf den Schiffen der Flotte.
		 */
        final Map<Integer,Offizier> offiziere = new HashMap<>();
		/**
		 * Die Anzahl der gedockten/gelandeten Schiffe.
		 */
        final Map<Integer,Long> dockedCount = new HashMap<>();
		/**
		 * Die Anzahl der extern gedocketen Schiffe.
		 */
        final Map<Integer,Long> aDockedCount = new HashMap<>();
	}


	private boolean initFleetData(Ship schiff, StringBuilder out) {
		Context context = ContextMap.getContext();
		boolean error = false;

		FleetMovementData fleetdata = (FleetMovementData)context.getVariable(Ship.class, "fleetdata");
		if( fleetdata != null ) {
			return false;
		}

		fleetdata = new FleetMovementData();

		context.putVariable(Ship.class, "fleetdata", fleetdata);

		List<Ship> fleetships = em.createQuery("from Ship s left join fetch s.modules " +
				"where s.id>0 and s.fleet=:fleet and s.x=:x and s.y=:y and s.system=:sys and s.owner=:owner and " +
				"s.docked='' and s.id!=:id and s.e>0 and s.battle is null", Ship.class)
				.setParameter("fleet", schiff.getFleet())
				.setParameter("x", schiff.getX())
				.setParameter("y", schiff.getY())
				.setParameter("sys", schiff.getSystem())
				.setParameter("owner", schiff.getOwner())
				.setParameter("id", schiff.getId())
				.getResultList();

		for (Ship fleetship : fleetships)
		{
			ShipTypeData shiptype = fleetship.getTypeData();

			StringBuilder outpb = new StringBuilder();

			if (shiptype.getCost() == 0)
			{
				outpb.append("<span style=\"color:red\">Das Objekt kann nicht fliegen, da es keinen Antieb hat</span><br />");
				error = true;
			}

			if ((fleetship.getCrew() == 0) && (shiptype.getCrew() > 0))
			{
				outpb.append("<span style=\"color:red\">Fehler: Sie haben keine Crew auf dem Schiff</span><br />");
				error = true;
			}

			if (outpb.length() != 0)
			{
				out.append("<tr>\n");
				out.append("<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange; font-size:12px\"> ").append(fleetship.getName()).append(" (").append(fleetship.getId()).append("):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n");
				out.append(outpb);
				out.append("</span></td></tr>\n");
			}
			else
			{
				long dockedcount = 0;
				long adockedcount = 0;
				if ((shiptype.getJDocks() > 0) || (shiptype.getADocks() > 0))
				{

					dockedcount = shipService.getAnzahlGedockterUndGelandeterSchiffe(fleetship);
					if (shiptype.getADocks() > 0)
					{
						adockedcount = shipService.getDockedCount(fleetship);
					}
				}

				if (fleetship.getStatus().contains("offizier"))
				{
					fleetdata.offiziere.put(fleetship.getId(), fleetship.getOffizier());
				}

				fleetdata.dockedCount.put(fleetship.getId(), dockedcount);
				fleetdata.aDockedCount.put(fleetship.getId(), adockedcount);

				fleetdata.ships.put(fleetship.getId(), fleetship);
			}
		}

		return error;
	}

	private FlugStatus moveFleet(int direction, boolean forceLowHeat, StringBuilder out)  {
		FlugStatus status = FlugStatus.SUCCESS;

		boolean firstEntry = true;
		Context context = ContextMap.getContext();
		FleetMovementData fleetdata = (FleetMovementData)context.getVariable(Ship.class, "fleetdata");

		for( Ship fleetship : fleetdata.ships.values() ) {
			Offizier offizierf = fleetdata.offiziere.get(fleetship.getId());

			ShipTypeData shiptype = fleetship.getTypeData();

			MovementResult result = moveSingle(fleetship, shiptype, offizierf, direction, 1, fleetdata.aDockedCount.get(fleetship.getId()), forceLowHeat, out);

			//Einen einmal gesetzten Fehlerstatus nicht wieder aufheben
			if( status == FlugStatus.SUCCESS ) {
				status = result.status;
			}
		}

		if( !firstEntry )
		{
			out.append("</table>\n");
		}

		return status;
	}

	private void saveFleetShips() {
		Context context = ContextMap.getContext();
		FleetMovementData fleetdata = (FleetMovementData)context.getVariable(Ship.class, "fleetdata");

		if( fleetdata != null ) {

			Map<Location,List<String>> shipDockIds = new HashMap<>();
			for( Ship fleetship : fleetdata.ships.values() ) {
				if( fleetdata.dockedCount.get(fleetship.getId()) > 0 ) {
					List<String> posIds = shipDockIds.computeIfAbsent(fleetship.getLocation(), k -> new ArrayList<>());
					posIds.add("l "+fleetship.getId());
					posIds.add(Integer.toString(fleetship.getId()));
				}

			}

			for( Map.Entry<Location, List<String>> entry : shipDockIds.entrySet() ) {
				final Location loc = entry.getKey();
				List<Ship> dockedList = em.createQuery("from Ship s left join fetch s.modules where s.id>0 and s.docked in :dockedIds", Ship.class)
						.setParameter("dockedIds", entry.getValue())
						.getResultList();
				for (Ship dockedShip: dockedList)
				{
					dockedShip.setLocation(loc);
				}
			}
		}
		context.putVariable(Ship.class, "fleetships", null);
		context.putVariable(Ship.class, "fleetoffiziere", null);
	}

	/**
	 * Das Ergebnis einer Flugbewegung eines oder mehrerer Schiffe.
	 */
	public static class FlugErgebnis {
		private final FlugStatus status;
		private final String messages;

		public FlugErgebnis(FlugStatus status, @NonNull String messages)
		{
			this.status = status;
			this.messages = messages;
		}

		/**
		 * Das Ergebnis der Flugbewegung.
		 * @return Das Ergebnis
		 */
		public FlugStatus getStatus()
		{
			return status;
		}

		/**
		 * Die Statusmeldungen der Flugbewegung.
		 * @return Die Statusmeldungen
		 */
		public @NonNull String getMeldungen()
		{
			return messages;
		}
	}

	/**
	 * <p>Fliegt eine Flugroute entlang. Falls das Schiff einer Flotte angehoert, fliegt
	 * diese ebenfalls n Felder in diese Richtung.</p>
	 * <p>Der Flug wird abgebrochen sobald eines der Schiffe nicht mehr weiterfliegen kann</p>
	 * Die Flugrouteninformationen werden waehrend des Fluges modifiziert.
	 *
	 * @param route Die Flugroute
	 * @param forceLowHeat Soll bei Ueberhitzung sofort abgebrochen werden?
	 * @return Der Status, der zum Ende des Fluges gefuehrt hat
	 */
	public FlugErgebnis fliege(Ship schiff, List<Waypoint> route, boolean forceLowHeat) {
		StringBuilder out = new StringBuilder();

		//We want to fly the slowest ship first, so the fleet cannot be seperated
		if(schiff.getFleet() != null && route.size() > 1)
		{
			Ship moving = schiff;

			List<Ship> ships = fleetMgmtService.getShips(schiff.getFleet());
			for(Ship ship: ships)
			{
				if(getSafeTravelDistance(ship) < getSafeTravelDistance(moving))
				{
					moving = ship;
				}
			}

			//We have used references instead of copies, so we can compare by != here
			if(moving != schiff)
			{
				//Maximum distance is safe travel distance
				if(route.size() > getSafeTravelDistance(moving))
				{
					route = route.subList(0, getSafeTravelDistance(moving));
				}

				return fliege(moving, route, forceLowHeat);
			}
		}

		User user = schiff.getOwner();

		ShipTypeData shiptype = schiff.getTypeData();
		Offizier offizier = schiff.getOffizier();

		//Das Schiff soll sich offenbar bewegen
		if( schiff.isDocked() ) {
			out.append("Fehler: Sie k&ouml;nnen nicht mit dem Schiff fliegen, da es geladet/angedockt ist\n");
			return new FlugErgebnis(FlugStatus.SHIP_FAILURE, out.toString());
		}

		if( shiptype.getCost() == 0 ) {
			out.append("Fehler: Das Objekt kann nicht fliegen, da es keinen Antrieb hat\n");
			return new FlugErgebnis(FlugStatus.SHIP_FAILURE, out.toString());
		}

		if( schiff.getBattle() != null ) {
			out.append("Fehler: Das Schiff ist in einen Kampf verwickelt\n");
			return new FlugErgebnis(FlugStatus.SHIP_FAILURE, out.toString());
		}

		if( (schiff.getCrew() <= 0) && (shiptype.getCrew() > 0) ) {
			out.append("<span style=\"color:#ff0000\">Das Schiff verf&uuml;gt &uuml;ber keine Crew</span><br />\n");
			return new FlugErgebnis(FlugStatus.SHIP_FAILURE, out.toString());
		}

		long docked = 0;
		long adocked = 0;
		FlugStatus status = FlugStatus.SUCCESS;

		if( (shiptype.getJDocks() > 0) || (shiptype.getADocks() > 0) ) {
			docked = shipService.getAnzahlGedockterUndGelandeterSchiffe(schiff);

			if( shiptype.getADocks() > 0 ) {
				adocked = (int)shipService.getDockedCount(schiff);
			}
		}

		boolean inAlarmRotEinfliegen = route.size() == 1 && route.get(0).distance == 1;
		boolean moved = false;

		while( (status == FlugStatus.SUCCESS) && route.size() > 0 ) {
			Waypoint waypoint = route.remove(0);

			if( waypoint.type != Waypoint.Type.MOVEMENT ) {
				throw new RuntimeException("Es wird nur "+Waypoint.Type.MOVEMENT+" als Wegpunkt unterstuetzt");
			}

			if( waypoint.direction == 5 ) {
				continue;
			}

			// Zielkoordinaten/Bewegungsrichtung berechnen
			int xoffset = 0;
			int yoffset = 0;
			if( waypoint.direction <= 3 ) {
				yoffset--;
			}
			else if( waypoint.direction >= 7 ) {
				yoffset++;
			}

			if( (waypoint.direction-1) % 3 == 0 ) {
				xoffset--;
			}
			else if( waypoint.direction % 3 == 0 ) {
				xoffset++;
			}

			List<Ship> sectorList = em.createQuery("from Ship " +
					"where owner!=:owner and system=:system and x between :lowerx and :upperx and y between :lowery and :uppery", Ship.class)
					.setParameter("owner", schiff.getOwner())
					.setParameter("system", schiff.getSystem())
					.setParameter("lowerx", (waypoint.direction - 1) % 3 == 0 ? schiff.getX() - waypoint.distance : schiff.getX())
					.setParameter("upperx", (waypoint.direction) % 3 == 0 ? schiff.getX() + waypoint.distance : schiff.getX())
					.setParameter("lowery", waypoint.direction <= 3 ? schiff.getY() - waypoint.distance : schiff.getY())
					.setParameter("uppery", waypoint.direction >= 7 ? schiff.getY() + waypoint.distance : schiff.getY())
					.getResultList();

			Map<Location, List<Ship>> alertList = userService.alertCheck(schiff.getOwner(), sectorList.stream().map(Ship::getLocation).toArray(Location[]::new));

			// Alle potentiell relevanten Sektoren mit EMP- oder Schadensnebeln (ok..und ein wenig ueberfluessiges Zeug bei schraegen Bewegungen) auslesen
			Set<Nebel.Typ> nebulaTypes = new HashSet<>(Nebel.Typ.getEmpNebula());
			nebulaTypes.addAll(Nebel.Typ.getDamageNebula());
			List<Nebel> sectorNebelList = em.createQuery("from Nebel " +
					"where type in :nebulaTypes and loc.system=:system and loc.x between :lowerx and :upperx and loc.y between :lowery and :uppery", Nebel.class)
					.setParameter("system", schiff.getSystem())
					.setParameter("lowerx", (waypoint.direction - 1) % 3 == 0 ? schiff.getX() - waypoint.distance : schiff.getX())
					.setParameter("upperx", (waypoint.direction) % 3 == 0 ? schiff.getX() + waypoint.distance : schiff.getX())
					.setParameter("lowery", waypoint.direction <= 3 ? schiff.getY() - waypoint.distance : schiff.getY())
					.setParameter("uppery", waypoint.direction >= 7 ? schiff.getY() + waypoint.distance : schiff.getY())
					.setParameter("nebulaTypes", nebulaTypes)
					.getResultList();

			Set<Location> empLocations = new HashSet<>();
			Set<Location> damageLocations = new HashSet<>();
			for (Nebel nebel : sectorNebelList)
			{
				if(nebel.getType().isEmp()) {
					empLocations.add(nebel.getLocation());
				} else {
					damageLocations.add(nebel.getLocation());
				}
			}

			if( (waypoint.distance > 1) && empLocations.contains(schiff.getLocation()) ) {
				out.append("<span style=\"color:#ff0000\">Der Autopilot funktioniert in EMP-Nebeln nicht</span><br />\n");
				return new FlugErgebnis(FlugStatus.BLOCKED_BY_EMP, out.toString());
			}

			if( schiff.getFleet() != null ) {
				initFleetData(schiff, out);
			}

			long starttime = System.currentTimeMillis();

			int startdistance = waypoint.distance;

			// Und nun fliegen wir mal ne Runde....
			while( waypoint.distance > 0 ) {
				final Location nextLocation = new Location(schiff.getSystem(),schiff.getX()+xoffset, schiff.getY()+yoffset);

				if(alertList.containsKey(nextLocation) && userService.isNoob(user)){
					List<Ship> attackers = userService.alertCheck(user, nextLocation)
							.values().iterator().next();
					if( !attackers.isEmpty() ) {
						out.append("<span style=\"color:#ff0000\">Sie stehen unter dem Schutz des Commonwealth.</span><br />Ihnen ist der Einflug in gesicherte Gebiete untersagt<br />\n");
						status = FlugStatus.BLOCKED_BY_ALERT;
						waypoint.distance = 0;
						break;
					}
				}

				// Schauen wir mal ob wir vor Alarm warnen muessen
				if( !inAlarmRotEinfliegen && alertList.containsKey(nextLocation) ) {
					List<Ship> attackers = userService.alertCheck(user, nextLocation)
							.values().iterator().next();
					if( !attackers.isEmpty() ) {
						out.append("<span style=\"color:#ff0000\">Feindliche Schiffe in Alarmbereitschaft im n&auml;chsten Sektor geortet</span><br />\n");
						out.append("<span style=\"color:#ff0000\">Autopilot bricht ab</span><br />\n");
						status = FlugStatus.BLOCKED_BY_ALERT;
						waypoint.distance = 0;
						break;
					}
				}

				if( (startdistance > 1) && empLocations.contains(nextLocation) ) {
					out.append("<span style=\"color:#ff0000\">EMP-Nebel im n&auml;chsten Sektor geortet</span><br />\n");
					out.append("<span style=\"color:#ff0000\">Autopilot bricht ab</span><br />\n");
					status = FlugStatus.BLOCKED_BY_EMP;
					waypoint.distance = 0;
					break;
				}

				if( (startdistance > 1) && damageLocations.contains(nextLocation) ) {
					out.append("<span style=\"color:#ff0000\">Schadensnebel im n&auml;chsten Sektor geortet</span><br />\n");
					out.append("<span style=\"color:#ff0000\">Autopilot bricht ab</span><br />\n");
					status = FlugStatus.BLOCKED_BY_DAMAGE_NEBULA;
					waypoint.distance = 0;
					break;
				}

				int olddirection = waypoint.direction;

				// ACHTUNG: Ob das ganze hier noch sinnvoll funktioniert, wenn distance > 1 ist, ist mehr als fraglich...
				if( empLocations.contains(nextLocation) &&
						(ThreadLocalRandom.current().nextDouble() < schiff.getTypeData().getLostInEmpChance()) ) {
					Nebel.Typ nebel = nebulaService.getNebula(schiff.getLocation());
					if( nebel == Nebel.Typ.STRONG_EMP ) {
						waypoint.direction = ThreadLocalRandom.current().nextInt(1,10);
					}
				}

				// Nun muessen wir noch die Caches fuellen
				if( waypoint.direction != olddirection ) {
					int tmpxoff = 0;
					int tmpyoff = 0;

					if( waypoint.direction <= 3 ) {
						tmpyoff--;
					}
					else if( waypoint.direction >= 7 ) {
						tmpyoff++;
					}

					if( (waypoint.direction-1) % 3 == 0 ) {
						tmpxoff--;
					}
					else if( waypoint.direction % 3 == 0 ) {
						tmpxoff++;
					}

					alertList.putAll(userService.alertCheck(schiff.getOwner(), new Location(schiff.getSystem(), schiff.getX() + tmpxoff, schiff.getY() + tmpyoff)));
				}

				waypoint.distance--;
				MovementResult result = moveSingle(schiff, shiptype, offizier, waypoint.direction, waypoint.distance, adocked, forceLowHeat, out);
				status = result.status;
				waypoint.distance = result.distance;

				if( result.moved ) {
					// Jetzt, da sich unser Schiff korrekt bewegt hat, fliegen wir auch die Flotte ein stueck weiter
					if( schiff.getFleet() != null ) {
						FlugStatus fleetResult = moveFleet(waypoint.direction, forceLowHeat, out);
						if( fleetResult != FlugStatus.SUCCESS  ) {
							status = fleetResult;
							waypoint.distance = 0;
						}
					}

					moved = true;

					if( alertList.containsKey(schiff.getLocation()) ) {
						schiff.setDocked("");
						if( docked != 0 ) {
							for( Ship dship : shipService.getDockedShips(schiff) )
							{
								dship.setLocation(schiff);
							}
							for( Ship dship : shipService.getLandedShips(schiff) )
							{
								dship.setLocation(schiff);
							}
						}
						saveFleetShips();


						handleAlert(schiff);
					}
				}

				// Wenn wir laenger als 25 Sekunden fuers fliegen gebraucht haben -> abbrechen!
				if( System.currentTimeMillis() - starttime > 25000 ) {
					out.append("<span style=\"color:#ff0000\">Flug dauert zu lange</span><br />\n");
					out.append("<span style=\"color:#ff0000\">Autopilot bricht ab</span><br />\n");
					waypoint.distance = 0;
					status = FlugStatus.SHIP_FAILURE;
				}
			}  // while distance > 0

		} // while !error && route.size() > 0

		if( moved ) {
			out.append("Ankunft bei ").append(locationService.displayCoordinates(schiff.getLocation(), true)).append("<br />\n");

			schiff.setDocked("");
			if( docked != 0 ) {
				for (Ship dockedShip : shipService.getGedockteUndGelandeteSchiffe(schiff))
				{
					dockedShip.setLocation(schiff);
				}
			}
            if(schiff.getFleet() != null)
            {
                for(Ship ship : fleetMgmtService.getShips(schiff.getFleet()))
                {
                    shipActionService.recalculateShipStatus(ship, false);
                }
            }
            else
            {
				shipActionService.recalculateShipStatus(schiff,false);
            }
		}
		saveFleetShips();

		return new FlugErgebnis(status, out.toString());
	}

	private void handleAlert(Ship schiff)
	{
		User owner = schiff.getOwner();
		List<Ship> attackShips = userService.alertCheck(owner, schiff.getLocation()).values().iterator().next();

		if(attackShips.isEmpty())
		{
			return;
		}

		for(Ship ship: attackShips)
		{
			if(ship.getBattle() != null)
			{
				BattleShip bship = em.find(BattleShip.class, ship.getId());
				int oside = (bship.getSide() + 1) % 2 + 1;
				Battle battle = ship.getBattle();
				battleService.load(battle, schiff.getOwner(), null, null, oside);

				for(Ship aship: shipService.getDockedShips(schiff))
				{
					battleService.addShip(battle, schiff.getOwner().getId(), aship.getId());
				}
				battleService.addShip(battle, schiff.getOwner().getId(), schiff.getId());

				return;
			}
		}

		Ship ship = attackShips.get(0); //Take some ship .. no special mechanism here.
		battleService.erstelle(ship.getOwner(), ship, schiff, true);
	}

	/**
	 * @return Die Felder, die das Schiff zuruecklegen kann ohne zu ueberhitzen / keine Energie mehr zu haben.
	 */
	public int getSafeTravelDistance(Ship ship)
	{

		int energy = ship.getEnergy();
		int heat = ship.getHeat();

		ShipTypeData typeData = ship.getTypeData();
		int consumption = typeData.getCost();
		int heatBuildup = typeData.getHeat();

		if(consumption == 0 || heatBuildup == 0)
		{
			if(ship.isDocked() || ship.isLanded())
			{
				return Integer.MAX_VALUE;
			}
			return 0;
		}

		int distance = Math.min(energy/consumption, (100-heat)/heatBuildup);
		if(distance < 0)
		{
			distance = 0;
		}

		return distance;
	}
}
