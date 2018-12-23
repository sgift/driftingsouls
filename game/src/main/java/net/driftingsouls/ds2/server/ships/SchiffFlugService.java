package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.services.SchlachtErstellenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service zum Fliegen von Schiffen.
 */
@Service
public class SchiffFlugService
{
	private SchlachtErstellenService schlachtErstellenService;

	@Autowired
	public SchiffFlugService(SchlachtErstellenService schlachtErstellenService)
	{
		this.schlachtErstellenService = schlachtErstellenService;
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
		SHIP_FAILURE
	}

	private static class MovementResult
	{
		int distance;
		boolean moved;
		FlugStatus status;

		MovementResult(int distance, boolean moved, FlugStatus status)
		{
			this.distance = distance;
			this.moved = moved;
			this.status = status;
		}
	}



	private static MovementResult moveSingle(Ship ship, ShipTypeData shiptype, Offizier offizier, int direction, int distance, long adocked, boolean forceLowHeat, boolean verbose, StringBuilder out) {
		boolean moved = false;
		FlugStatus status = FlugStatus.SUCCESS;
		org.hibernate.Session db = ContextMap.getContext().getDB();

		if( ship.getEngine() <= 0 ) {
			if(verbose) {
				out.append("<span style=\"color:#ff0000\">Antrieb defekt</span><br />\n");
			}
			distance = 0;

			return new MovementResult(distance, moved, FlugStatus.SHIP_FAILURE);
		}

		int newe = ship.getEnergy() - shiptype.getCost();
		int news = ship.getHeat() + shiptype.getHeat();

		newe -= adocked;
		if( shiptype.getMinCrew() > ship.getCrew() ) {
			newe--;
			if(verbose) {
				out.append("<span style=\"color:red\">Geringe Besatzung erh&ouml;ht Flugkosten</span><br />\n");
			}
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
			if(!verbose)
			{
				out.append(ship.getName()).append(" (").append(ship.getId()).append("): ");
			}
			out.append("<span style=\"color:#ff0000\">Keine Energie. Stoppe bei ").append(ship.getLocation().displayCoordinates(true)).append("</span><br />\n");
			distance = 0;

			return new MovementResult(distance, moved, FlugStatus.SHIP_FAILURE);
		}

		if( offizier != null ) {
			// Flugkosten
			int success = offizier.useAbility( Offizier.Ability.NAV, 200 );
			if( success > 0 ) {
				newe += 2;
				if( newe > ship.getEnergy()-1 ) {
					newe = ship.getEnergy() - 1;
				}
				if(verbose) {
					out.append(offizier.getName()).append(" verringert Flugkosten<br />\n");
				}
			}
			// Ueberhitzung
			success = offizier.useAbility( Offizier.Ability.ING, 200 );
			if( success > 0 ) {
				news -= 1;
				if( news < ship.getHeat()+2 ) {
					news = ship.getHeat()+2;
				}
				if( verbose ) {
					out.append(offizier.getName()).append(" verringert &Uuml;berhitzung<br />\n");
				}
			}
			if( verbose ) {
				out.append(offizier.MESSAGE.getMessage().replace("\n", "<br />"));
			}
		}

		// Grillen wir uns bei dem Flug eventuell den Antrieb?
		if( news > 100 )  {
			if(forceLowHeat && distance > 0) {
				if( !verbose ) {
					out.append(ship.getName()).append(" (").append(ship.getId()).append("): ");
				}
				out.append("<span style=\"color:#ff0000\">Triebwerk w&uuml;rde &uuml;berhitzen</span><br />\n");

				out.append("<span style=\"color:#ff0000\">Autopilot bricht ab bei ").append(ship.getLocation().displayCoordinates(true)).append("</span><br />\n");
				out.append("</span></td></tr>\n");
				distance = 0;
				return new MovementResult(distance, moved, FlugStatus.SHIP_FAILURE);
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

		StarSystem sys = (StarSystem)db.get(StarSystem.class, ship.getSystem());

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
				if( !verbose ) {
					out.append(ship.getName()).append(" (").append(ship.getId()).append("): ");
				}
				out.append("<span style=\"color:#ff0000\">Triebwerke &uuml;berhitzt</span><br />\n");

				if( (ThreadLocalRandom.current().nextInt(101)) < 3*(news-100) ) {
					int dmg = (int)( (2*(ThreadLocalRandom.current().nextInt(101)/100d)) + 1 ) * (news-100);
					out.append("<span style=\"color:#ff0000\">Triebwerke nehmen ").append(dmg).append(" Schaden</span><br />\n");
					ship.setEngine(ship.getEngine()-dmg);
					if( ship.getEngine() < 0 ) {
						ship.setEngine(0);
					}
					if( distance > 0 ) {
						out.append("<span style=\"color:#ff0000\">Autopilot bricht ab bei ").append(ship.getLocation().displayCoordinates(true)).append("</span><br />\n");
						status = FlugStatus.SHIP_FAILURE;
						distance = 0;
					}
				}
			}

			ship.setX(x);
			ship.setY(y);
			ship.setEnergy(newe);
			ship.setHeat(news);
			if( verbose ) {
				out.append(ship.getName()).append(" fliegt in ").append(ship.getLocation().displayCoordinates(true)).append(" ein<br />\n");
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
		Map<Integer,Ship> ships = new HashMap<>();
		/**
		 * Die Offiziere auf den Schiffen der Flotte.
		 */
		Map<Integer,Offizier> offiziere = new HashMap<>();
		/**
		 * Die Anzahl der gedockten/gelandeten Schiffe.
		 */
		Map<Integer,Long> dockedCount = new HashMap<>();
		/**
		 * Die Anzahl der extern gedocketen Schiffe.
		 */
		Map<Integer,Long> aDockedCount = new HashMap<>();
	}


	private boolean initFleetData(Ship schiff, boolean verbose, StringBuilder out) {
		Context context = ContextMap.getContext();
		boolean error = false;
		boolean firstEntry = true;

		FleetMovementData fleetdata = (FleetMovementData)context.getVariable(Ships.class, "fleetdata");
		if( fleetdata != null ) {
			return false;
		}

		fleetdata = new FleetMovementData();

		context.putVariable(Ships.class, "fleetdata", fleetdata);

		org.hibernate.Session db = context.getDB();

		List<?> fleetships = db.createQuery("from Ship s left join fetch s.modules " +
				"where s.id>0 and s.fleet=:fleet and s.x=:x and s.y=:y and s.system=:sys and s.owner=:owner and " +
				"s.docked='' and s.id!=:id and s.e>0 and s.battle is null")
				.setEntity("fleet", schiff.getFleet())
				.setInteger("x", schiff.getX())
				.setInteger("y", schiff.getY())
				.setInteger("sys", schiff.getSystem())
				.setEntity("owner", schiff.getOwner())
				.setInteger("id", schiff.getId())
				.list();

		for (Object fleetship1 : fleetships)
		{
			if (verbose && firstEntry)
			{
				firstEntry = false;
				out.append("<table class=\"noBorder\">\n");
			}
			Ship fleetship = (Ship) fleetship1;
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

					dockedcount = fleetship.getAnzahlGedockterUndGelandeterSchiffe();
					if (shiptype.getADocks() > 0)
					{
						adockedcount = fleetship.getDockedCount();
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

	private FlugStatus moveFleet(int direction, boolean forceLowHeat, boolean verbose, StringBuilder out)  {
		FlugStatus status = FlugStatus.SUCCESS;

		boolean firstEntry = true;
		Context context = ContextMap.getContext();
		FleetMovementData fleetdata = (FleetMovementData)context.getVariable(Ships.class, "fleetdata");

		for( Ship fleetship : fleetdata.ships.values() ) {
			if( verbose && firstEntry ) {
				firstEntry = false;
				out.append("<table class=\"noBorder\">\n");
			}

			if(verbose) {
				out.append("<tr>\n");
				out.append("<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange; font-size:12px\"> ").append(fleetship.getName()).append(" (").append(fleetship.getId()).append("):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n");
			}
			Offizier offizierf = fleetdata.offiziere.get(fleetship.getId());

			ShipTypeData shiptype = fleetship.getTypeData();

			MovementResult result = moveSingle(fleetship, shiptype, offizierf, direction, 1, fleetdata.aDockedCount.get(fleetship.getId()), forceLowHeat, verbose, out);

			//Einen einmal gesetzten Fehlerstatus nicht wieder aufheben
			if( status == FlugStatus.SUCCESS ) {
				status = result.status;
			}

			if(verbose) {
				out.append("</span></td></tr>\n");
			}
		}

		if( !firstEntry )
		{
			out.append("</table>\n");
		}

		return status;
	}

	private static void saveFleetShips() {
		Context context = ContextMap.getContext();
		FleetMovementData fleetdata = (FleetMovementData)context.getVariable(Ships.class, "fleetdata");

		if( fleetdata != null ) {
			org.hibernate.Session db = context.getDB();

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
				List<?> dockedList = db.createQuery("from Ship s left join fetch s.modules where s.id>0 and s.docked in (:dockedIds)")
						.setParameterList("dockedIds", entry.getValue())
						.list();
				for (Object aDockedList : dockedList)
				{
					Ship dockedShip = (Ship) aDockedList;
					dockedShip.setLocation(loc);
				}
			}
		}
		context.putVariable(Ships.class, "fleetships", null);
		context.putVariable(Ships.class, "fleetoffiziere", null);
	}

	/**
	 * Das Ergebnis einer Flugbewegung eines oder mehrerer Schiffe.
	 */
	public static class FlugErgebnis {
		private final FlugStatus status;
		private final String messages;

		public FlugErgebnis(FlugStatus status, @Nonnull String messages)
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
		public @Nonnull String getMeldungen()
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

		org.hibernate.Session db = ContextMap.getContext().getDB();

		//We want to fly the slowest ship first, so the fleet cannot be seperated
		if(schiff.getFleet() != null && route.size() > 1)
		{
			Ship moving = schiff;

			List<Ship> ships = schiff.getFleet().getShips();
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
			docked = schiff.getAnzahlGedockterUndGelandeterSchiffe();

			if( shiptype.getADocks() > 0 ) {
				adocked = (int)schiff.getDockedCount();
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

			List<Ship> sectorList = Common.cast(db.createQuery("from Ship " +
					"where owner!=:owner and system=:system and x between :lowerx and :upperx and y between :lowery and :uppery")
					.setEntity("owner", schiff.getOwner())
					.setInteger("system", schiff.getSystem())
					.setInteger("lowerx", (waypoint.direction - 1) % 3 == 0 ? schiff.getX() - waypoint.distance : schiff.getX())
					.setInteger("upperx", (waypoint.direction) % 3 == 0 ? schiff.getX() + waypoint.distance : schiff.getX())
					.setInteger("lowery", waypoint.direction <= 3 ? schiff.getY() - waypoint.distance : schiff.getY())
					.setInteger("uppery", waypoint.direction >= 7 ? schiff.getY() + waypoint.distance : schiff.getY())
					.list());

			Map<Location, List<Ship>> alertList = Ship.alertCheck(schiff.getOwner(), sectorList.stream().map(Ship::getLocation).toArray(Location[]::new));

			// Alle potentiell relevanten Sektoren mit EMP-Nebeln (ok..und ein wenig ueberfluessiges Zeug bei schraegen Bewegungen) auslesen
			Map<Location,Boolean> nebulaemplist = new HashMap<>();
			List<Nebel> sectorNebelList = Common.cast(db.createQuery("from Nebel " +
					"where type in (:emptypes) and loc.system=:system and loc.x between :lowerx and :upperx and loc.y between :lowery and :uppery")
					.setInteger("system", schiff.getSystem())
					.setInteger("lowerx", (waypoint.direction - 1) % 3 == 0 ? schiff.getX() - waypoint.distance : schiff.getX())
					.setInteger("upperx", (waypoint.direction) % 3 == 0 ? schiff.getX() + waypoint.distance : schiff.getX())
					.setInteger("lowery", waypoint.direction <= 3 ? schiff.getY() - waypoint.distance : schiff.getY())
					.setInteger("uppery", waypoint.direction >= 7 ? schiff.getY() + waypoint.distance : schiff.getY())
					.setParameterList("emptypes", Nebel.Typ.getEmpNebel())
					.list());

			for (Nebel nebel : sectorNebelList)
			{
				nebulaemplist.put(nebel.getLocation(), Boolean.TRUE);
			}

			if( (waypoint.distance > 1) && nebulaemplist.containsKey(schiff.getLocation()) ) {
				out.append("<span style=\"color:#ff0000\">Der Autopilot funktioniert in EMP-Nebeln nicht</span><br />\n");
				return new FlugErgebnis(FlugStatus.BLOCKED_BY_EMP, out.toString());
			}

			if( schiff.getFleet() != null ) {
				initFleetData(schiff, false, out);
			}

			long starttime = System.currentTimeMillis();

			int startdistance = waypoint.distance;

			// Und nun fliegen wir mal ne Runde....
			while( waypoint.distance > 0 ) {
				final Location nextLocation = new Location(schiff.getSystem(),schiff.getX()+xoffset, schiff.getY()+yoffset);

				if(alertList.containsKey(nextLocation) && user.isNoob()){
					List<Ship> attackers = Ship.alertCheck(user, nextLocation)
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
					List<Ship> attackers = Ship.alertCheck(user, nextLocation)
							.values().iterator().next();
					if( !attackers.isEmpty() ) {
						out.append("<span style=\"color:#ff0000\">Feindliche Schiffe in Alarmbereitschaft im n&auml;chsten Sektor geortet</span><br />\n");
						out.append("<span style=\"color:#ff0000\">Autopilot bricht ab</span><br />\n");
						status = FlugStatus.BLOCKED_BY_ALERT;
						waypoint.distance = 0;
						break;
					}
				}

				if( (startdistance > 1) && nebulaemplist.containsKey(nextLocation) ) {
					out.append("<span style=\"color:#ff0000\">EMP-Nebel im n&auml;chsten Sektor geortet</span><br />\n");
					out.append("<span style=\"color:#ff0000\">Autopilot bricht ab</span><br />\n");
					status = FlugStatus.BLOCKED_BY_EMP;
					waypoint.distance = 0;
					break;
				}

				int olddirection = waypoint.direction;

				// ACHTUNG: Ob das ganze hier noch sinnvoll funktioniert, wenn distance > 1 ist, ist mehr als fraglich...
				if( nebulaemplist.containsKey(nextLocation) &&
						(ThreadLocalRandom.current().nextDouble() < schiff.getTypeData().getLostInEmpChance()) ) {
					Nebel.Typ nebel = Nebel.getNebula(schiff.getLocation());
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

					alertList.putAll(Ship.alertCheck(schiff.getOwner(), new Location(schiff.getSystem(), schiff.getX() + tmpxoff, schiff.getY() + tmpyoff)));
				}

				waypoint.distance--;

				MovementResult result = moveSingle(schiff, shiptype, offizier, waypoint.direction, waypoint.distance, adocked, forceLowHeat, false, out);
				status = result.status;
				waypoint.distance = result.distance;

				if( result.moved ) {
					// Jetzt, da sich unser Schiff korrekt bewegt hat, fliegen wir auch die Flotte ein stueck weiter
					if( schiff.getFleet() != null ) {
						FlugStatus fleetResult = moveFleet(waypoint.direction, forceLowHeat, false, out);
						if( fleetResult != FlugStatus.SUCCESS  ) {
							status = fleetResult;
							waypoint.distance = 0;
						}
					}

					moved = true;

					if( alertList.containsKey(schiff.getLocation()) ) {
						schiff.setDocked("");
						if( docked != 0 ) {
							for( Ship dship : schiff.getDockedShips() )
							{
								dship.setLocation(schiff);
							}
							for( Ship dship : schiff.getLandedShips() )
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
			out.append("Ankunft bei ").append(schiff.getLocation().displayCoordinates(true)).append("<br />\n");

			schiff.setDocked("");
			if( docked != 0 ) {
				for (Ship dockedShip : schiff.getGedockteUndGelandeteSchiffe())
				{
					dockedShip.setLocation(schiff);
				}
			}
            if(schiff.getFleet() != null)
            {
                for(Ship ship : schiff.getFleet().getShips())
                {
                    ship.recalculateShipStatus(false);
                }
            }
            else
            {
                schiff.recalculateShipStatus(false);
            }
		}
		saveFleetShips();

		return new FlugErgebnis(status, out.toString());
	}

	private void handleAlert(Ship schiff)
	{
		User owner = schiff.getOwner();
		List<Ship> attackShips = Ship.alertCheck(owner, schiff.getLocation()).values().iterator().next();

		if(attackShips.isEmpty())
		{
			return;
		}

		for(Ship ship: attackShips)
		{
			if(ship.getBattle() != null)
			{
				org.hibernate.Session db = ContextMap.getContext().getDB();
				BattleShip bship = (BattleShip)db.get(BattleShip.class, ship.getId());
				int oside = (bship.getSide() + 1) % 2 + 1;
				Battle battle = ship.getBattle();
				battle.load(schiff.getOwner(), null, null, oside);

				for(Ship aship: schiff.getDockedShips())
				{
					battle.addShip(schiff.getOwner().getId(), aship.getId());
				}
				battle.addShip(schiff.getOwner().getId(), schiff.getId());

				return;
			}
		}

		Ship ship = attackShips.get(0); //Take some ship .. no special mechanism here.
		schlachtErstellenService.erstelle(ship.getOwner(), ship, schiff, true);
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
