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
package net.driftingsouls.ds2.server.entities;

import net.driftingsouls.ds2.server.ships.ShipTypeData;
import org.hibernate.annotations.ForeignKey;

import javax.annotation.Nonnull;
import javax.persistence.CollectionTable;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.HashSet;
import java.util.Set;

/**
 * Eine einfache Waffe in DS. Basisklasse fuer alle Waffen.
 * @author Christopher Jung
 *
 */
@Entity
@DiscriminatorColumn(name = "implementierung")
@DiscriminatorValue("Weapon")
public class Weapon {
	@Id
	private String id;
	private String name = "";
	private int defTrefferWS = 50;
	private int defSmallTrefferWS = 0;
	private double defTorpTrefferWS = 0;
	private int defSubWS = 0;
	private int apCost = 1;
	private int eCost = 1;
	private int baseDamage = 0;
	private int shieldDamage = 0;
	private int areaDamage = 0;
	private int subDamage = 0;
	@ElementCollection
	@CollectionTable
	@ForeignKey(name="weapon_munition_fk_weapon")
	private Set<String> munition = new HashSet<>();
	private int singleshots = 1;
	private boolean destroyable = false;
	@ElementCollection
	@CollectionTable
	@ForeignKey(name="weapon_flags_fk_weapon")
	private Set<Flags> flags = new HashSet<>();

	/**
	 * Konstruktor.
	 */
	protected Weapon()
	{
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 */
	public Weapon(String id)
	{
		this.id = id;
	}

	/**
	 * Gibt alle Flags der Waffe zurueck.
	 * @return Die Flags
	 */
	public Set<Flags> getFlags()
	{
		return new HashSet<>(flags);
	}

	/**
	 * Setzt alle Flags der Waffe.
	 * @param flags Die Flags
	 */
	public void setFlags(Set<Flags> flags)
	{
		this.flags.clear();
		this.flags.addAll(flags);
	}

	/**
	 * Gibt den Namen der Waffe zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Setzt den Anzeigenamen der Waffe.
	 * @param name Der Name
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Gibt die ID der Waffe zurueck.
	 * @return Die ID
	 */
    public String getId()
    {
        return this.id;
    }

	/**
	 * Setzt die ID der Waffe.
	 * @param id Die ID
	 */
	public void setId(String id)
	{
		this.id = id;
	}

	/**
	 * Gibt die zum Abfeuern benoetigten AP zurueck.
	 * @return Die AP-Kosten
	 */
	public int getApCost() {
		return this.apCost;
	}

	/**
	 * Setzt die zum Abfeuern benoetigten AP.
	 * @param apCost Die AP-Kosten
	 */
	public void setApCost(int apCost)
	{
		this.apCost = apCost;
	}

	/**
	 * Gibt die zum Abfeuern benoetigte Energie zurueck.
	 * @return Die Energie-Kosten
	 */
	public int getECost() {
		return this.eCost;
	}

	/**
	 * Setzt die zum Abfeuern benoetigte Energie.
	 * @param eCost Die Energie-Kosten
	 */
	public void setECost(int eCost)
	{
		this.eCost = eCost;
	}

	/**
	 * Gibt den Schaden der Waffe gegenueber der Schiffshuelle zurueck.
	 * @return Der Schaden an der Huelle
	 */
	public int getBaseDamage() {
		return this.baseDamage;
	}

	/**
	 * Setzt den Schaden der Waffe gegenueber der Schiffshuelle ohne Modifikationen.
	 * @param baseDamage Der Schaden an der Huelle
	 */
	public void setBaseDamage(int baseDamage)
	{
		this.baseDamage = baseDamage;
	}

	/**
	 * Gibt den Multiplikationsfaktor fuer den Schaden in Abhaengigkeit vom getroffenen Schiffstyp zurueck.
	 * @param enemyShipType Der Typ des Schiffes, auf welches gefeuert werden soll
	 * @return Der Multiplikationsfaktor
	 */
	public int getBaseDamageModifier(ShipTypeData enemyShipType) {
		return 1;
	}
	
	/**
	 * Gibt den Schaden der Waffe gegenueber den Schilden zurueck.
	 * @return Der Schaden an den Schilden
	 */
	public int getShieldDamage() {
		return this.shieldDamage;
	}

	/**
	 * Setzt den Schaden der Waffe gegenueber den Schilden ohne Modifikationen.
	 * @param shieldDamage Der Schaden an den Schilden
	 */
	public void setShieldDamage(int shieldDamage)
	{
		this.shieldDamage = shieldDamage;
	}
    
	/**
	 * Gibt den Schaden der Waffe gegenueber den Subsystemen zurueck.
	 * @return Der Schaden an den Subsystemen
	 */
	public int getSubDamage() {
		return this.subDamage;
	}

	/**
	 * Setzt den Schaden der Waffe gegenueber den Subsystemen ohne Modifikationen.
	 * @param subDamage Der Schaden an den Subsystemen
	 */
	public void setSubDamage(int subDamage)
	{
		this.subDamage = subDamage;
	}
	
	/**
	 * Gibt die Trefferwahrscheinlichkeit gegenueber normalen Schiffen zurueck.
	 * @return Die Trefferwahrscheinlichkeit gegenueber normalen Schiffen
	 */
	public int getDefTrefferWS() {
		return this.defTrefferWS;
	}

	/**
	 * Setzt die Trefferwahrscheinlichkeit gegenueber normalen Schiffen.
	 * @param defTrefferWS Die Trefferwahrscheinlichkeit gegenueber normalen Schiffen
	 */
	public void setDefTrefferWS(int defTrefferWS)
	{
		this.defTrefferWS = defTrefferWS;
	}
	
	/**
	 * Gibt die Trefferwahrscheinlichkeit gegenueber kleinen Schiffen zurueck.
	 * @return Die Trefferwahrscheinlichkeit gegenueber kleinen Schiffen
	 */
	public int getDefSmallTrefferWS() {
		return this.defSmallTrefferWS;
	}

	/**
	 * Setzt die Trefferwahrscheinlichkeit gegenueber kleinen Schiffen.
	 * @param defSmallTrefferWS Die Trefferwahrscheinlichkeit gegenueber kleinen Schiffen
	 */
	public void setDefSmallTrefferWS(int defSmallTrefferWS)
	{
		this.defSmallTrefferWS = defSmallTrefferWS;
	}
	
	/**
	 * Gibt die Trefferwahrscheinlichkeit gegenueber anfliegenden Torpedos (und anderen zerstoerbaren Waffen) zurueck.
	 * @return Die Trefferwahrscheinlichkeit gegenueber Torpedos (und anderen zerstoerbaren Waffen)
	 */
	public double getTorpTrefferWS() {
		return this.defTorpTrefferWS;
	}

	/**
	 * Setzt die Trefferwahrscheinlichkeit gegenueber anfliegenden Torpedos (und anderen zerstoerbaren Waffen).
	 * @param defTorpTrefferWS Die Trefferwahrscheinlichkeit gegenueber Torpedos (und anderen zerstoerbaren Waffen)
	 */
	public void setTorpTrefferWS(double defTorpTrefferWS)
	{
		this.defTorpTrefferWS = defTorpTrefferWS;
	}

	/**
	 * Gibt die Trefferwahrscheinlichkeit gegenueber Subsystemen zurueck.
	 * @return Die Trefferwahrscheinlichkeit gegenueber Subsystemen
	 */
	public int getDefSubWS() {
		return this.defSubWS;
	}

	/**
	 * Setzt die Trefferwahrscheinlichkeit gegenueber Subsystemen.
	 * @param defSubWS Die Trefferwahrscheinlichkeit gegenueber Subsystemen
	 */
	public void setDefSubWS(int defSubWS)
	{
		this.defSubWS = defSubWS;
	}
	
	/**
	 * Berechnet Aenderungen am Schiffstyp des feuernden Schiffes.
	 * @param ownShipType Der Typ des feuernden Schiffes
	 * @param enemyShipType Der Typ des getroffenen Schiffes
	 * @return Wurden Aenderungen vorgenommen (<code>true</code>)
	 */
	public ShipTypeData calcOwnShipType(ShipTypeData ownShipType, ShipTypeData enemyShipType) {
		return ownShipType;
	}
	
	/**
	 * Berechnet Aenderungen am Schiffstyp des getroffenen Schiffes.
	 * @param ownShipType Der Typ des feuernden Schiffes
	 * @param enemyShipType Der Typ des getroffenen Schiffes
	 * @return Wurden Aenderungen vorgenommen (<code>true</code>)
	 */
	public ShipTypeData calcEnemyShipType(ShipTypeData ownShipType, ShipTypeData enemyShipType) {
		return enemyShipType;
	}
	
	/**
	 * Gibt die benoetigten Munitionstypen zurueck. Falls keine Munition verwendet wird, so wird ein leeres Set zurueckgegeben.
	 * @return Die benoetigten Munitionstypen
	 */
	public @Nonnull Set<String> getMunitionstypen() {
		return new HashSet<>(this.munition);
	}

	/**
	 * Setzt die benoetigten Munitionstypen. Falls keine Munition verwendet wird, so wird leeres Set erwartet.
	 * @param munition Die benoetigten Munitionstypen
	 */
	public void setMunitionstypen(@Nonnull Set<String> munition)
	{
		this.munition.clear();
		this.munition.addAll(munition);
	}
	
	/**
	 * Gibt die Anzahl der Einzelschuesse pro abgefeuertem Schuss zurueck.
	 * @return Die Anzahl der Einzelschuesse pro abgefeuertem Schiff
	 */
	public int getSingleShots() {
		return this.singleshots;
	}

	/**
	 * Setzt die Anzahl der Einzelschuesse pro abgefeuertem Schuss.
	 * @param singleshots Die Anzahl der Einzelschuesse pro abgefeuertem Schiff
	 */
	public void setSingleShots(int singleshots)
	{
		this.singleshots = singleshots;
	}
	
	/**
	 * Gibt die Reichweite des Schadens gegenueber der Umgebung des getroffenen Schiffes zurueck.
	 * @return Der Umgebungsschaden
	 */
	public int getAreaDamage() {
		return this.areaDamage;
	}

	/**
	 * Setzt die Reichweite des Schadens gegenueber der Umgebung des getroffenen Schiffes.
	 * @param areaDamage Der Umgebungsschaden
	 */
	public void setAreaDamage(int areaDamage)
	{
		this.areaDamage = areaDamage;
	}
	
	/**
	 * Gibt zurueck, ob das Geschoss durch Abwehrfeuer zerstoerbar ist.
	 * @return <code>true</code>, falls das Geschoss durch Abwehrfeuer zerstoerbar ist
	 */
	public boolean getDestroyable() {
		return this.destroyable;
	}

	/**
	 * Setzt, ob das Geschoss durch Abwehrfeuer zerstoerbar ist.
	 * @param destroyable <code>true</code>, falls das Geschoss durch Abwehrfeuer zerstoerbar ist
	 */
	public void setDestroyable(boolean destroyable)
	{
		this.destroyable = destroyable;
	}
	
	/**
	 * Prueft, ob die Waffe ueber das angegebene Flag verfuegt.
	 * @param flag Das Flag
	 * @return <code>true</code>, falls die Waffe das Flag besitzt
	 */
	public boolean hasFlag(Flags flag) {
		return this.flags.contains(flag);
	}

	/**
	 * Waffenflags.
	 */
	public enum Flags {
		/**
		 * Spezial-Waffe (eigene Auswahlbox unter sonstiges).
		 */
		SPECIAL,
		/**
		 * Nach dem Abfeuern das Schiff zerstoeren.
		 */
		DESTROY_AFTER,
		/**
		 * Ammo-Auswahl fuer diese Waffe zulassen.
		 */
		AMMO_SELECT,
		/**
		 * Area-Damage ueber die Distanz nicht reduzieren.
		 */
		AD_FULL,
		/**
		 * Weitreichende Waffen koennen aus der zweiten Reihe heraus abgefeuert werden.
		 */
		LONG_RANGE,
		/**
		 * Sehr weitreichende Waffen koennen aus der zweiten Reihe heraus abgefeuert und auch in die
		 * zweite Reihe des Gegners feuern.
		 */
		VERY_LONG_RANGE
	}
}
