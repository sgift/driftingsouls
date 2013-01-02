package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;

import java.util.List;
import java.util.Map;

/**
 * Eine benutzerspezifische Sicht auf ein Feld der Sternenkarte.
 */
public interface FieldView
{
	/**
	 * Gibt alle sichtbaren Basen zurueck.
	 * @return Die Liste der Basen
	 */
	List<Base> getBases();

	/**
	 * Gibt alle sichtbaren Schiffe gruppiert nach Besitzer und Typ zurueck.
	 * @return Die Schiffe
	 */
	Map<User, Map<ShipType, List<Ship>>> getShips();
}
