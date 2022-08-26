package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;

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
	List<StationaryObjectData> getBases();

	/**
	 * Gibt alle sichtbaren Schiffe gruppiert nach Besitzer und Typ zurueck.
	 * @return Die Schiffe
	 */
	Map<UserData, Map<ShipTypeData, List<ShipData>>> getShips();

	/**
	 * Gibt alle sichtbaren Spungpunkte zurueck.
	 * @return Die Sprungpunkte
	 */
	List<NodeData> getJumpNodes();

	/**
	 * Gibt alle Brocken zurueck.
	 * @return Die Brocken
	 */
	List<StationaryObjectData> getBrocken();

	/**
	 * Gibt zurück, ob ein Sprung in dieses System stattfindet.
	 */
	int getJumpCount();

	/**
	 * Gibt, sofern an der Stelle vorhanden, den entsprechenden Nebel zurueck.
	 * @return Der Nebel oder <code>null</code>
	 */
	Nebel.Typ getNebel();

	/**
	 * Gibt alle sichtbaren Schlachten zurueck.
	 * @return Die Schlachten
	 */
	List<BattleData> getBattles();

	/**
	 * Gibt zurueck, ob im Sektor feindliche Schiffe auf rotem (bzw. gelben) Alarm
	 * gesetzt sind, als ein Einflug vmtl in einem Angriff endet.
	 * @return <code>true</code>, falls dem so ist
	 */
	boolean isRoterAlarm();

	Location getLocation();
}
