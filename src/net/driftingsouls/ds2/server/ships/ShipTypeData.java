/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.ships;

/**
 * Interface fuer Schiffstypendaten eines Schiffes
 * @author Christopher Jung
 *
 */
public interface ShipTypeData extends Cloneable {
	/**
	 * Gibt die Anzahl an externen Docks zurueck
	 * @return Die externen Docks
	 */
	public int getADocks();
	
	/**
	 * Gibt die Cargogroesse zurueck
	 * @return Die Cargogroesse
	 */
	public long getCargo();
	
	/**
	 * Git die Chance auf Loot zurueck
	 * @return Die Lootchance
	 */
	public int getChance4Loot();
	
	/**
	 * Gibt die Flugkosten in Energie zurueck
	 * @return Die Flugkosten
	 */
	public int getCost();
	
	/**
	 * Gibt die Maximalbesatzung zurueck
	 * @return Die Besatzung
	 */
	public int getCrew();
	
	/**
	 * Gibt die Maximalbesetzung an Marines zurueck
	 * @return Die Marines
	 */
	public int getMarines();
	
	/**
	 * Gibt die Schiffstypenbeschreibung zurueck
	 * @return Die Beschreibung
	 */
	public String getDescrip();

	/**
	 * Gibt die Menge an Deuterium zurueck, die pro Energieeinheit in Nebeln gesammelt werden kann
	 * @return Der Deuteriumfaktor
	 */
	public int getDeutFactor();

	/**
	 * Gibt die Groesse der Energiespeicher zurueck
	 * @return Die EPS
	 */
	public int getEps();

	/**
	 * Gibt die Flags zurueck
	 * @return Die Flags
	 */
	public String getFlags();
	
	/**
	 * Prueft, ob das angegebene Flag gesetzt ist
	 * @param flag Das ShipType-Flag
	 * @return <code>true</code>, falls das Flag gesetzt ist
	 */
	public boolean hasFlag(String flag);

	/**
	 * Gibt zurueck, ab wievielen Schiffen in der Anzeige Gruppen gebildet werden sollen
	 * @return Der Gruppierungsfaktor
	 */
	public int getGroupwrap();

	/**
	 * Gibt die pro geflogenem Feld generierte Hitze zurueck
	 * @return Die pro Feld generierte Hitze
	 */
	public int getHeat();

	/**
	 * Gibt zurueck, ob der Schiffstypeneintrag versteckt ist
	 * @return <code>true</code>, wenn der Schiffstypeneintrag unsichtbar ist
	 */
	public boolean isHide();

	/**
	 * Gibt die Anzahl an Huellenpunkten zurueck
	 * @return Die Huellenpunkte
	 */
	public int getHull();

	/**
	 * Gibt die Nahrungsproduktion pro Tick zurueck
	 * @return Die Nahrungsproduktion pro Tick
	 */
	public int getHydro();

	/**
	 * Gibt die ID des Schifftyps zurueck
	 * @return Die Schiffstypen-ID
	 */
	public int getTypeId();
	
	/**
	 * Gibt die Typendaten des zu Grunde liegenden Schifftyps zurueck
	 * @return Die Typendaten
	 */
	public ShipTypeData getType();

	/**
	 * Gibt die Anzahl an Jaegerdocks zurueck
	 * @return Die Anzahl an Jaegerdocks
	 */
	public int getJDocks();

	/**
	 * Gibt die maximale Hitze der einzelnen Waffen zurueck
	 * @return Die max. Hitze der Waffen
	 */
	public String getMaxHeat();

	/**
	 * Gibt die verfuegbaren Modulsteckplaetze zurueck 
	 * @return Die verfuegbaren Modulsteckplaetze
	 */
	public String getTypeModules();

	/**
	 * Gibt den Namen des Schiffstypen zurueck
	 * @return Der Name
	 */
	public String getNickname();

	/**
	 * Gibt die Einweg-Werftdaten zurueck
	 * @return Die Einweg-Werftdaten
	 */
	public int getOneWayWerft();

	/**
	 * Gibt die Panzerung zurueck
	 * @return Die Panzerung
	 */
	public int getPanzerung();

	/**
	 * Gibt das Bild zurueck
	 * @return Das Bild
	 */
	public String getPicture();

	/**
	 * Gibt die Reaktoreffizienz bei Antimaterie zurueck
	 * @return Die Reaktoreffizienz
	 */
	public int getRa();

	/**
	 * Gibt die Reaktoreffizienz bei Deuterium zurueck
	 * @return Die Reaktoreffizienz
	 */
	public int getRd();

	/**
	 * Gibt die Wartungskosten zurueck
	 * @return Die Wartungskosten
	 */
	public int getReCost();

	/**
	 * Gibt die Gesamtenergieproduktion pro Tick zurueck
	 * @return Die Gesamtenergieproduktion
	 */
	public int getRm();

	/**
	 * Gibt die Reaktoreffizienz bei Uran zurueck
	 * @return Die Reaktoreffizienz
	 */
	public int getRu();

	/**
	 * Gibt die Reichweite der LRS zurueck
	 * @return Die LRS-Reichweite
	 */
	public int getSensorRange();

	/**
	 * Gibt die Schildstaerke zurueck
	 * @return Die Schildstaerke
	 */
	public int getShields();

	/**
	 * Gibt die Schiffsklasse zurueck
	 * @return Die Schiffsklasse
	 */
	public int getShipClass();

	/**
	 * Gibt die Anzahl an Schiffen pro Schiff zurueck
	 * @return Die Schiffsanzahl
	 */
	public int getShipCount();
	
	/**
	 * Gibt die Schiffsgroesse zurueck
	 * @return Die Groesse
	 */
	public int getSize();

	/**
	 * Gibt den Verteidigungswert gegen Torpedos zurueck
	 * @return Der Verteidigungswert
	 */
	public int getTorpedoDef();
	
	/**
	 * Gibt die Waffen des Schiffes zurueck
	 * @return Die Waffen
	 */
	public String getWeapons();

	/**
	 * Gibt den Werttyp zurueck
	 * @return Der Werfttyp
	 */
	public String getWerft();
	
	/**
	 * Gibt zurueck, ob es sich um ein bewaffnetes Schiff handelt
	 * @return <code>true</code>, falls das Schiff Waffen hat
	 */
	public boolean isMilitary();
	
	/**
	 * Gibt die ablative Panzerung des Schifftypes zurueck.
	 * @return Ablative Panzerung
	 */
	public int getAblativeArmor();
	
	/**
	 * Gibt zurueck, ob ein Schiff ein SRS besitzt.
	 * 
	 * @return <code>True</code>, wenn es ein SRS hat, <code>false</code> ansonsten.
	 */
	public boolean hasSrs();
	
	/**
	 * Gibt die Energiekosten pro LRS-Scan (Gesamtuebersicht) zurueck
	 * @return Die Energiekosten
	 */
	public int getScanCost();
	
	/**
	 * Gibt die Energiekosten pro LRS-Sektorscan (Scannen des Inhalts eines Sektors) zurueck
	 * @return Die Energiekosten
	 */
	public int getPickingCost();
	
	/**
	 * Zusaetzlich zu den Bedingungen von {@link Object#clone()} gilt folgende Bedingung:
	 * Eine Klasse soll nur dann eine <code>CloneNotSupportedException</code> werfen,
	 * wenn sie Immutable ist, also sich die Werte der Instanz nie aendern (und sich die Klone
	 * folglich auch nie unterscheiden wuerden)
	 * @return Das geklonte Objekt
	 * @throws CloneNotSupportedException Falls das Objekt Immutable ist und klonen nicht unterstuetzt wird
	 */
	public Object clone() throws CloneNotSupportedException;
}
