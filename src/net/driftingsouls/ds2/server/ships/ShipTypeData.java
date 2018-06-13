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

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.Map;

/**
 * Interface fuer Schiffstypendaten eines Schiffes.
 * @author Christopher Jung
 *
 */
public interface ShipTypeData extends Cloneable {

    /**
     * @return Wieviel Kopfgeld erhaelt ein Spieler dafuer diesen Schiffstyp zu kapern?
     */
    BigInteger getBounty();

	/**
	 * Gibt die Anzahl an externen Docks zurueck.
	 * @return Die externen Docks
	 */
    int getADocks();
	
	/**
	 * Gibt die Cargogroesse zurueck.
	 * @return Die Cargogroesse
	 */
    long getCargo();
	
	/**
	 * Gibt die NahrungsCargogroesze zurueck.
	 * @return Die Nahrungscargogroesze
	 */
    long getNahrungCargo();
	
	/**
	 * Git die Chance auf Loot zurueck.
	 * @return Die Lootchance
	 */
    int getChance4Loot();
	
	/**
	 * Gibt die Flugkosten in Energie zurueck.
	 * @return Die Flugkosten
	 */
    int getCost();
	
	/**
	 * Gibt die Maximalbesatzung zurueck.
	 * @return Die Besatzung
	 */
    int getCrew();
	
	/**
	 * Gibt die Schiffstypenbeschreibung zurueck.
	 * @return Die Beschreibung
	 */
    String getDescrip();

	/**
	 * Gibt die Menge an Deuterium zurueck, die pro Energieeinheit in Nebeln gesammelt werden kann.
	 * @return Der Deuteriumfaktor
	 */
    int getDeutFactor();

	/**
	 * Gibt die Groesse der Energiespeicher zurueck.
	 * @return Die EPS
	 */
    int getEps();

	/**
	 * Gibt die Flags zurueck.
	 * @return Die Flags
	 */
    EnumSet<ShipTypeFlag> getFlags();

	/**
	 * Prueft, ob das angegebene Flag gesetzt ist.
	 * @param flag Das ShipType-Flag
	 * @return <code>true</code>, falls das Flag gesetzt ist
	 */
    boolean hasFlag(@Nonnull ShipTypeFlag flag);

	/**
	 * Gibt zurueck, ab wievielen Schiffen in der Anzeige Gruppen gebildet werden sollen.
	 * @return Der Gruppierungsfaktor
	 */
    int getGroupwrap();

	/**
	 * Gibt die pro geflogenem Feld generierte Hitze zurueck.
	 * @return Die pro Feld generierte Hitze
	 */
    int getHeat();

	/**
	 * Gibt zurueck, ob der Schiffstypeneintrag versteckt ist.
	 * @return <code>true</code>, wenn der Schiffstypeneintrag unsichtbar ist
	 */
    boolean isHide();

	/**
	 * Gibt die Anzahl an Huellenpunkten zurueck.
	 * @return Die Huellenpunkte
	 */
    int getHull();

	/**
	 * Gibt die Nahrungsproduktion pro Tick zurueck.
	 * @return Die Nahrungsproduktion pro Tick
	 */
    int getHydro();

	/**
	 * Gibt die ID des Schifftyps zurueck.
	 * @return Die Schiffstypen-ID
	 */
    int getTypeId();
	
	/**
	 * Gibt die Typendaten des zu Grunde liegenden Schifftyps zurueck.
	 * @return Die Typendaten
	 */
    ShipTypeData getType();

	/**
	 * Gibt die Anzahl an Jaegerdocks zurueck.
	 * @return Die Anzahl an Jaegerdocks
	 */
    int getJDocks();

	/**
	 * Gibt die maximale Hitze der einzelnen Waffen zurueck.
	 * @return Die max. Hitze der Waffen
	 */
    Map<String, Integer> getMaxHeat();

	/**
	 * Gibt die verfuegbaren Modulsteckplaetze zurueck.
	 * @return Die verfuegbaren Modulsteckplaetze
	 */
    String getTypeModules();

	/**
	 * Gibt den Namen des Schiffstypen zurueck.
	 * @return Der Name
	 */
    String getNickname();

	/**
	 * Gibt die Einweg-Werftdaten zurueck.
	 * @return Die Einweg-Werftdaten
	 */
    ShipType getOneWayWerft();

	/**
	 * Gibt die Panzerung zurueck.
	 * @return Die Panzerung
	 */
    int getPanzerung();

	/**
	 * Gibt das Bild zurueck.
	 * @return Das Bild
	 */
    String getPicture();

	/**
	 * Gibt die Reaktoreffizienz bei Antimaterie zurueck.
	 * @return Die Reaktoreffizienz
	 */
    int getRa();

	/**
	 * Gibt die Reaktoreffizienz bei Deuterium zurueck.
	 * @return Die Reaktoreffizienz
	 */
    int getRd();

	/**
	 * Gibt die Wartungskosten zurueck.
	 * @return Die Wartungskosten
	 */
    int getReCost();

	/**
	 * Gibt die Gesamtenergieproduktion pro Tick zurueck.
	 * @return Die Gesamtenergieproduktion
	 */
    int getRm();

	/**
	 * Gibt die Reaktoreffizienz bei Uran zurueck.
	 * @return Die Reaktoreffizienz
	 */
    int getRu();

	/**
	 * Gibt die Reichweite der LRS zurueck.
	 * @return Die LRS-Reichweite
	 */
    int getSensorRange();

	/**
	 * Gibt die Schildstaerke zurueck.
	 * @return Die Schildstaerke
	 */
    int getShields();

	/**
	 * Gibt die Schiffsklasse zurueck.
	 * @return Die Schiffsklasse
	 */
    ShipClasses getShipClass();

	/**
	 * Gibt die Schiffsgroesse zurueck.
	 * @return Die Groesse
	 */
    int getSize();

	/**
	 * Gibt den Verteidigungswert gegen Torpedos zurueck.
	 * @return Der Verteidigungswert
	 */
    int getTorpedoDef();
	
	/**
	 * Gibt die Waffen des Schiffes zurueck.
	 * @return Die Waffen
	 */
    Map<String, Integer> getWeapons();

	/**
	 * Gibt die Anzahl der verfuegbaren Werftslots zurueck.
	 * @return Die Werftslots
	 */
    int getWerft();
	
	/**
	 * Gibt die maximale Groesze der Einheiten auf diesem Schiff zurueck.
	 * @return Die maximale Groesze
	 */
    int getMaxUnitSize();
	
	/**
	 * Gibt den Laderaum fuer die Einheiten zurueck.
	 * @return Der Laderaum
	 */
    int getUnitSpace();
	
	/**
	 * Gibt zurueck, ob es sich um ein bewaffnetes Schiff handelt.
	 * @return <code>true</code>, falls das Schiff Waffen hat
	 */
    boolean isMilitary();
	
	/**
	 * Gibt zurueck, ob es sich um einen Versorger handelt.
	 * @return <code>true</code>, falls das Schiff ein Versorger ist
	 */
    boolean isVersorger();
	
	/**
	 * Gibt die ablative Panzerung des Schifftypes zurueck.
	 * @return Ablative Panzerung
	 */
    int getAblativeArmor();
	
	/**
	 * Gibt zurueck, ob ein Schiff ein SRS besitzt.
	 * 
	 * @return <code>True</code>, wenn es ein SRS hat, <code>false</code> ansonsten.
	 */
    boolean hasSrs();

	/**
	 * @return Crewwert bei dem das Schiff noch normal funktioniert.
	 */
    int getMinCrew();
	
	/**
	 * Wahrscheinlichkeit, dass das Schiff sich in einem EMP-Nebel verfliegt.
	 * 
	 * @return Wahrscheinlichkeit als Anteil von 0 bis 1. Kann auch groesser oder kleienr als 0 oder 1 sein (Module!)
	 */
    double getLostInEmpChance();
	
	/**
	 * Zusaetzlich zu den Bedingungen von {@link Object#clone()} gilt folgende Bedingung:
	 * Eine Klasse soll nur dann eine <code>CloneNotSupportedException</code> werfen,
	 * wenn sie Immutable ist, also sich die Werte der Instanz nie aendern (und sich die Klone
	 * folglich auch nie unterscheiden wuerden).
	 * @return Das geklonte Objekt
	 * @throws CloneNotSupportedException Falls das Objekt Immutable ist und klonen nicht unterstuetzt wird
	 */
    Object clone() throws CloneNotSupportedException;
}
