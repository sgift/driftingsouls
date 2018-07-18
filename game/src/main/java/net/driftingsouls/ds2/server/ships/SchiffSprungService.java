package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service zum Springen von Schiffen ueber verschiedene Arten von Sprungpunkten.
 */
@Service
public class SchiffSprungService
{
	/**
	 * Das Ergebnis eines Sprungbefehls.
	 */
	public static class SprungErgebnis {
		private final String meldungen;
		private final boolean erfolgreich;

		public SprungErgebnis(@Nonnull String meldungen, boolean erfolgreich)
		{
			this.meldungen = meldungen;
			this.erfolgreich = erfolgreich;
		}

		/**
		 * Gibt alle Meldungen des Sprungs zurueck.
		 * @return Die Meldungen
		 */
		public @Nonnull String getMeldungen()
		{
			return meldungen;
		}

		/**
		 * Gibt zurueck, ob der Sprung erfolgreich war.
		 * @return <code>true</code>, falls er erfolgreich war
		 */
		public boolean isErfolgreich()
		{
			return erfolgreich;
		}
	}
	/**
	 * <p>Laesst das Schiff durch einen Sprungpunkt springen.
	 * Der Sprungpunkt ist dabei ein normaler Sprungpunkt</p>
	 *
	 * @param node Der Sprungpunkt
	 * @return <code>true</code>, falls ein Fehler aufgetreten ist
	 */
	public SprungErgebnis sprungViaSprungpunkt(Ship schiff, JumpNode node)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		StringBuilder outputbuffer = new StringBuilder();

		User user = schiff.getOwner();

		if (schiff.getBattle() != null)
		{
			outputbuffer.append("Fehler: Sie k&ouml;nnen nicht mit einem Schiff springen, dass in einem Kampf ist.<br />\n");
			return new SprungErgebnis(outputbuffer.toString(), false);
		}

		//
		// Daten der Sprungpunkte laden
		//
		String nodetypename = "Der Sprungpunkt";

		if (node == null)
		{
			outputbuffer.append("Fehler: Der angegebene Sprungpunkt existiert nicht<br />\n");
			return new SprungErgebnis(outputbuffer.toString(), false);
		}

		String nodetarget = node.getName() + " (" + node.getSystemOut() + ")";

		if ((user.getId() > 0) && node.isGcpColonistBlock() && Rassen.get().rasse(user.getRace()).isMemberIn(0) && !user.hasFlag(UserFlag.NO_JUMPNODE_BLOCK))
		{
			outputbuffer.append("<span style=\"color:red\">Die GCP hat diesen Sprungpunkt f&uuml;r Kolonisten gesperrt</span><br />\n");
			return new SprungErgebnis(outputbuffer.toString(), false);
		}

		Location nodeLoc = node.getLocation();
		Location outLoc = new Location(node.getSystemOut(), node.getXOut(), node.getYOut());

		Location shipLoc = new Location(schiff.getSystem(), schiff.getX(), schiff.getY());

		if (!shipLoc.sameSector(0, nodeLoc, 0))
		{
			outputbuffer.append("<span style=\"color:red\">Fehler: ").append(nodetypename).append(" befindet sich nicht im selben Sektor wie das Schiff</span><br />\n");
			return new SprungErgebnis(outputbuffer.toString(), false);
		}

		//
		// Liste der Schiffe ermitteln, welche springen sollen
		//

		List<Ship> shiplist = new ArrayList<>();
		// Falls vorhanden die Schiffe der Flotte einfuegen
		if (schiff.getFleet() != null)
		{
			shiplist.addAll(ermittleSprungfaehigeSchiffeDerFlotte(db, schiff));
		}
		// Keine Flotte -> nur das aktuelle Schiff einfuegen
		else
		{
			shiplist.add(schiff);
		}

		//
		// Jedes Schiff in der Liste springen lassen
		//
		for (Ship ship : shiplist)
		{
			ShipTypeData shiptype = ship.getTypeData();

			// Liste der gedockten Schiffe laden
			List<Ship> docked = ship.getGedockteUndGelandeteSchiffe();

			if (node.isWeaponBlock() && !user.hasFlag(UserFlag.MILITARY_JUMPS))
			{
				//Schiff ueberprfen
				if (shiptype.isMilitary())
				{
					outputbuffer.append("<span style=\"color:red\">").append(ship.getName()).append(" (").append(ship.getId()).append("): Die GCP verwehrt ihrem Kriegsschiff den Einflug nach ").append(node.getName()).append("</span><br />\n");
					return new SprungErgebnis(outputbuffer.toString(), false);
				}

				//Angedockte Schiffe ueberprfen
				if (docked.stream().anyMatch(d -> d.getTypeData().isMilitary()))
				{
					outputbuffer.append("<span style=\"color:red\">").append(ship.getName()).append(" (").append(ship.getId()).append("): Die GCP verwehrt einem/mehreren ihrer angedockten Kriegsschiffe den Einflug nach ").append(node.getName()).append("</span><br />\n");
					return new SprungErgebnis(outputbuffer.toString(), false);
				}
			}

			if (ship.getEnergy() < 5)
			{
				outputbuffer.append("<span style=\"color:red\">").append(ship.getName()).append(" (").append(ship.getId()).append("): Zuwenig Energie zum Springen</span><br />\n");
				return new SprungErgebnis(outputbuffer.toString(), false);
			}

			outputbuffer.append(ship.getName()).append(" (").append(ship.getId()).append(") springt nach ").append(nodetarget).append("<br />\n");
			ship.setLocation(outLoc);
			ship.setEnergy(ship.getEnergy() - 5);

			for (Ship aship : docked)
			{
				aship.setLocation(outLoc);
			}
		}

		return new SprungErgebnis(outputbuffer.toString(), true);
	}

	private List<Ship> ermittleSprungfaehigeSchiffeDerFlotte(Session db, Ship schiff)
	{
		return Common.cast(db.createQuery("from Ship where id>0 and fleet=:fleet AND x=:x AND y=:y AND system=:sys and docked='' AND battle is null")
				.setEntity("fleet", schiff.getFleet())
				.setInteger("x", schiff.getX())
				.setInteger("y", schiff.getY())
				.setInteger("sys", schiff.getSystem())
				.list());
	}

	/**
	 * <p>Laesst das Schiff durch einen Sprungpunkt springen.
	 * Der Sprungpunkt ist dabei ein "Knossos"-Sprungpunkt (also ein mit einem Schiff verbundener
	 * Sprungpunkt) sein. Der Sprung kann dabei scheitern, wenn keine Sprungberechtigung
	 * vorliegt.</p>
	 *
	 * @param schiff Das Schiff, welches springen soll
	 * @param node Das Schiff mit dem Sprungpunkt
	 * @return <code>true</code>, falls ein Fehler aufgetreten ist
	 */
	public SprungErgebnis sprungViaSchiff(Ship schiff, Ship node)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		StringBuilder outputbuffer = new StringBuilder();

		User user = schiff.getOwner();

		if (schiff.getBattle() != null)
		{
			outputbuffer.append("Fehler: Sie k&ouml;nnen nicht mit einem Schiff springen, dass in einem Kampf ist.<br />\n");
			return new SprungErgebnis(outputbuffer.toString(), false);
		}

		//
		// Daten der Sprungpunkte laden
		//

		/* Behandlung Knossosportale:
		 *
		 * Ziel wird mit ships.jumptarget festgelegt - Format: art|koords/id|user/ally/gruppe
		 * Beispiele:
		 * fix|2:35/35|all:
		 * ship|id:10000|ally:1
		 * base|id:255|group:-15,455,1200
		 * fix|8:20/100|default <--- diese Einstellung entspricht der bisherigen Praxis
		 */

		if (node == null)
		{
			outputbuffer.append("Fehler: Der angegebene Sprungpunkt existiert nicht<br />\n");
			return new SprungErgebnis(outputbuffer.toString(), false);
		}


		Location outLoc;
		String nodetarget;
		String nodetypename = node.getTypeData().getNickname();

		/*
		 * Ermittlung der Zielkoordinaten
		 * geprueft wird bei Schiffen und Basen das Vorhandensein der Gegenstation
		 * existiert keine, findet kein Sprung statt
		 */

		String[] target = StringUtils.split(node.getJumpTarget(), '|');
		switch (target[0])
		{
			case "fix":
				outLoc = Location.fromString(target[1]);

				nodetarget = target[1];
				break;
			case "ship":
			{
				String[] shiptarget = StringUtils.split(target[1], ':');
				Ship jmptarget = (Ship) db.get(Ship.class, Integer.valueOf(shiptarget[1]));
				if (jmptarget == null)
				{
					outputbuffer.append("<span style=\"color:red\">Die Empfangsstation existiert nicht!</span><br />\n");
					return new SprungErgebnis(outputbuffer.toString(), false);
				}

				outLoc = new Location(jmptarget.getSystem(), jmptarget.getX(), jmptarget.getY());
				nodetarget = outLoc.getSystem() + ":" + outLoc.getX() + "/" + outLoc.getY();
				break;
			}
			case "base":
			{
				String[] shiptarget = StringUtils.split(target[1], ':');
				Base jmptarget = (Base) db.get(Base.class, Integer.valueOf(shiptarget[1]));
				if (jmptarget == null)
				{
					outputbuffer.append("<span style=\"color:red\">Die Empfangsbasis existiert nicht!</span><br />\n");
					return new SprungErgebnis(outputbuffer.toString(), false);
				}

				outLoc = jmptarget.getLocation();
				nodetarget = outLoc.toString();
				break;
			}
			default:
				throw new IllegalArgumentException("Ungueltiger Zieltyp: "+target[0]);
		}

		// Einmalig das aktuelle Schiff ueberpruefen.
		// Evt vorhandene Schiffe in einer Flotte werden spaeter separat gecheckt
		if (node.getId() == schiff.getId())
		{
			outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen nicht mit dem ").append(nodetypename).append(" durch sich selbst springen</span><br />\n");
			return new SprungErgebnis(outputbuffer.toString(), false);
		}

			/*
			 * Ermittlung der Sprungberechtigten
			 */
		String[] jmpnodeuser = StringUtils.split(target[2], ':'); // Format art:ids aufgespalten

		switch (jmpnodeuser[0])
		{
			case "all":
				// Keine Einschraenkungen
				break;
			// die alte variante
			case "default":
			case "ownally":
				if (((user.getAlly() != null) && (node.getOwner().getAlly() != user.getAlly())) ||
						(user.getAlly() == null && (node.getOwner() != user)))
				{
					outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes ").append(nodetypename).append(" benutzen - default</span><br />\n");
					return new SprungErgebnis(outputbuffer.toString(), false);
				}
				break;
			// user:$userid
			case "user":
				if (Integer.parseInt(jmpnodeuser[1]) != user.getId())
				{
					outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes ").append(nodetypename).append(" benutzen - owner</span><br />\n");
					return new SprungErgebnis(outputbuffer.toString(), false);
				}
				break;
			// ally:$allyid
			case "ally":
				if ((user.getAlly() == null) || (Integer.parseInt(jmpnodeuser[1]) != user.getAlly().getId()))
				{
					outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes ").append(nodetypename).append(" benutzen - ally</span><br />\n");
					return new SprungErgebnis(outputbuffer.toString(), false);
				}
				break;
			// group:userid1,userid2, ...,useridn
			case "group":
				Integer[] userlist = Common.explodeToInteger(",", jmpnodeuser[1]);
				if (!Common.inArray(user.getId(), userlist))
				{
					outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes ").append(nodetypename).append(" benutzen - group</span><br />\n");
					return new SprungErgebnis(outputbuffer.toString(), false);
				}
				break;
			default:
				throw new IllegalArgumentException("Ungueltiger Berechtigungstyp: "+jmpnodeuser[0]);
		}

		Location nodeLoc = new Location(node.getSystem(), node.getX(), node.getY());

		Location shipLoc = new Location(schiff.getSystem(), schiff.getX(), schiff.getY());

		if (!shipLoc.sameSector(0, nodeLoc, 0))
		{
			outputbuffer.append("<span style=\"color:red\">Fehler: ").append(nodetypename).append(" befindet sich nicht im selben Sektor wie das Schiff</span><br />\n");
			return new SprungErgebnis(outputbuffer.toString(), false);
		}

		//
		// Liste der Schiffe ermitteln, welche springen sollen
		//

		List<Ship> shiplist = new ArrayList<>();
		// Falls vorhanden die Schiffe der Flotte einfuegen
		if (schiff.getFleet() != null)
		{
			List<Ship> fleetships = ermittleSprungfaehigeSchiffeDerFlotte(db, schiff);

			// Bei Knossossprungpunkten darauf achten, dass das Portal nicht selbst mitspringt
			shiplist.addAll(fleetships.stream().filter(s -> s != node).collect(Collectors.toList()));
		}
		// Keine Flotte -> nur das aktuelle Schiff einfuegen
		else
		{
			shiplist.add(schiff);
		}

		//
		// Jedes Schiff in der Liste springen lassen
		//
		for (Ship ship : shiplist)
		{
			// Liste der gedockten Schiffe laden
			List<Ship> docked = ship.getGedockteUndGelandeteSchiffe();

			if (ship.getEnergy() < 5)
			{
				outputbuffer.append("<span style=\"color:red\">").append(ship.getName()).append(" (").append(ship.getId()).append("): Zuwenig Energie zum Springen</span><br />\n");
				return new SprungErgebnis(outputbuffer.toString(), false);
			}

			outputbuffer.append(ship.getName()).append(" (").append(ship.getId()).append(") springt nach ").append(nodetarget).append("<br />\n");
			ship.setLocation(outLoc);
			ship.setEnergy(ship.getEnergy() - 5);

			for (Ship aship : docked)
			{
				aship.setLocation(outLoc);
			}
		}

		return new SprungErgebnis(outputbuffer.toString(), true);
	}
}
