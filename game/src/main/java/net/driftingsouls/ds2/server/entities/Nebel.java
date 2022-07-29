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

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.repositories.NebulaRepository;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.Ships;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Index;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

/**
 * Ein Nebel.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="nebel")
@Immutable
@BatchSize(size=50)
public class Nebel implements Locatable {
	/**
	 * Gibt den Nebeltyp an der angegebenen Position zurueck. Sollte sich an der Position kein
	 * Nebel befinden, wird <code>null</code> zurueckgegeben.
	 * @param loc Die Position
	 * @return Der Nebeltyp oder <code>null</code>
	 */
	@SuppressWarnings("unchecked")
	public static synchronized Typ getNebula(Location loc) {
		return NebulaRepository.getInstance().getNebula(loc);
	}

	/**
	 * Nebeltyp.
	 */
	public enum Typ
	{
		/**
		 * Normaler Deutnebel.
		 */
		MEDIUM_DEUT(0, 7, false, 0, 7,"normaler Deuteriumnebel"),
		/**
		 * Schwacher Deutnebel.
		 */
		LOW_DEUT(1, 5, false, -1, 5,"schwacher Deuteriumnebel"),
		/**
		 * Dichter Deutnebel.
		 */
		STRONG_DEUT(2, 11, false, 1, 11,"starker Deuteriumnebel"),
		/**
		 * Schwacher EMP-Nebel.
		 */
		LOW_EMP(3, Integer.MAX_VALUE, true, Integer.MIN_VALUE, 0,"schwacher EMP-Nebel"),
		/**
		 * Normaler EMP-Nebel.
		 */
		MEDIUM_EMP(4, Integer.MAX_VALUE, true, Integer.MIN_VALUE, 0,"normaler EMP-Nebel"),
		/**
		 * Dichter EMP-Nebel.
		 */
		STRONG_EMP(5, Integer.MAX_VALUE, true, Integer.MIN_VALUE, 0,"dichter EMP-Nebel" ),
		/**
		 * Schadensnebel.
		 */
		DAMAGE(6, 7, false, Integer.MIN_VALUE, 9, "Schadensnebel");

		private final int code;
		private final int minScansize;
		private final boolean emp;
		private final int deutfaktor;
		private final int minScanbareSchiffsgroesse;
		private final String beschreibung;

		Typ(int code, int minScansize, boolean emp, int deutfaktor, int minScanbareSchiffsgroesse, String beschreibung)
		{
			this.code = code;
			this.minScansize = minScansize;
			this.emp = emp;
			this.deutfaktor = deutfaktor;
			this.minScanbareSchiffsgroesse = minScanbareSchiffsgroesse;
			this.beschreibung = beschreibung;
		}

		/**
		 * Erzeugt aus Typenids (Datenbank) enums.
		 *
		 * @param type Typenid.
		 * @return Passendes enum.
		 */
		public static Typ getType(int type)
		{
			switch(type)
			{
				case 0: return MEDIUM_DEUT;
				case 1: return LOW_DEUT;
				case 2: return STRONG_DEUT;
				case 3: return LOW_EMP;
				case 4: return MEDIUM_EMP;
				case 5: return STRONG_EMP;
				case 6: return DAMAGE;
				default: throw new IllegalArgumentException("There's no nebula with type:" + type);
			}
		}

		/**
		 * @return Die Beschreibung des Nebels.
		 */
		public String getDescription()
		{
			return this.beschreibung;
		}

		/**
		 * @return Der Typcode des Nebels.
		 */
		public int getCode()
		{
			return this.code;
		}

		/**
		 * @return Die Groesse ab der ein Schiff sichtbar ist in dem Nebel.
		 */
		public int getMinScansize()
		{
			return this.minScansize;
		}

		/**
		 * Gibt zurueck, ob es sich um einen EMP-Nebel handelt.
		 * @return <code>true</code> falls dem so ist
		 */
		public boolean isEmp()
		{
			return emp;
		}

		/**
		 * Gibt zurueck, ob ein Nebel diesen Typs das Sammeln
		 * von Deuterium ermoeglicht.
		 * @return <code>true</code> falls dem so ist
		 */
		public boolean isDeuteriumNebel()
		{
			return this.deutfaktor > Integer.MIN_VALUE;
		}

		/**
		 * Gibt den durch die Eigenschaften des Nebels modifizierten
		 * Deuterium-Faktor beim Sammeln von Deuterium
		 * in einem Nebel diesem Typs zurueck. Falls der modifizierte
		 * Faktor <code>0</code> betraegt ist kein Sammeln moeglich.
		 * Falls es sich nicht um einen Nebel handelt,
		 * der das Sammeln von Deuterium erlaubt, wird
		 * der Faktor immer auf <code>0</code> reduziert.
		 * @param faktor Der zu modifizierende Faktor
		 * @return Der modifizierte Deuteriumfaktor
		 */
		public long modifiziereDeutFaktor(long faktor)
		{
			if( faktor <= 0 )
			{
				return 0;
			}
			long modfaktor = faktor + this.deutfaktor;
			if( modfaktor < 0 )
			{
				return 0;
			}
			return modfaktor;
		}

		/**
		 * Gibt alle Nebeltypen zurueck, die die Eigenschaft
		 * EMP haben.
		 * @return Die Liste
		 * @see #isEmp()
		 */
		public static Set<Typ> getEmpNebula()
		{
			return Arrays.stream(values())
				.filter(Typ::isEmp)
				.collect(Collectors.toCollection(HashSet::new));
		}

		public static Set<Typ> getDamageNebula() {
			Set<Typ> nebula = new HashSet<>();
			nebula.add(DAMAGE);
			return nebula;
		}

		/**
		 * Gibt die minimale mittels LRS scanbare Schiffsgroesse zurueck.
		 * Schiffe, deren Groesse kleiner als die angegebene Groesse ist,
		 * werden mittels LRS in einem Nebel diesen Typs nicht erkannt.
		 * Der Wert <code>0</code> bedeutet, das alle Schiffe mittels
		 * LRS geortet werden koennen.
		 * @return Die minimale Groesse
		 */
		public int getMinScanbareSchiffsgroesse()
		{
			return this.minScanbareSchiffsgroesse;
		}

		/**
		 * Gibt an, ob Schiffe in diesem Feld scannen duerfen.
		 *
		 * @return <code>true</code>, wenn sie scannen duerfen, sonst <code>false</code>.
		 */
		public boolean allowsScan()
		{
			return !this.emp;
		}

		/**
		 * Gibt das Bild des Nebeltyps zurueck (als Pfad).
		 * Der Pfad ist relativ zum data-Verzeichnis.
		 *
		 * @return Das Bild des Nebels als Pfad.
		 */
		public String getImage()
		{
			return "data/starmap/fog"+this.ordinal()+"/fog"+this.ordinal()+".png";
		}

		/**
		 * Damage the ship according to the nebula type.
		 *
		 * @param ship The ship to be damaged.
		 */
		public void damageShip(Ship ship, ConfigService config) {
			// Currently only damage nebula do damage and we only have one type of damage nebula
			// so no different effects
			if(this != DAMAGE) {
				return;
			}
			//gedockte Schiffe werden bereits ueber ihr Traegerschiff verarbeitet (siehe damageInternal())
			if(ship.isDocked())
			{
				return;
			}

			double shieldDamageFactor = config.getValue(WellKnownConfigValue.NEBULA_DAMAGE_SHIELD)/100.d;
			double ablativeDamageFactor = config.getValue(WellKnownConfigValue.NEBULA_DAMAGE_ABLATIVE)/100.d;
			double hullDamageFactor = config.getValue(WellKnownConfigValue.NEBULA_DAMAGE_HULL)/100.d;
			double subsystemDamageFactor = config.getValue(WellKnownConfigValue.NEBULA_DAMAGE_SUBSYSTEM)/100.d;

			damageInternal(ship, 1.0d, shieldDamageFactor, ablativeDamageFactor, hullDamageFactor, subsystemDamageFactor);
		}

		/**
		 * Damage a ship for flying into a damage nebula.
		 *
		 * @param ship Ship to damage
		 * @param globalDamageFactor Dampens initial damage to this ship (needed for docked ships, which should not take more damage than their carrier)
		 */
		private void damageInternal(Ship ship, double globalDamageFactor, double shieldDamageFactor, double ablativeDamageFactor, double hullDamageFactor, double subsystemDamageFactor) {
			/*
			Damage is applied according to the following formula ("ship type" always includes modules here):
			- Find the maximum shield of this ship type
			- Remove damage from shields
			- If ships shields are at zero find how much damage remains and apply it as percentage on the next level,
			e.g. We want to damage the shields for 1000, but only 900 shields remain, so we calculate the armor damage
			(according to the armor formula) and then only remove 10% (the remainder from shields)

			When shields are down externally attached ships (e.g. containers) start taking damage.
			 */

			double modifiedShieldDamageFactor = shieldDamageFactor * globalDamageFactor;

			ShipTypeData shipType = ship.getTypeData();
			int shieldDamage = (int)Math.floor(shipType.getShields() * modifiedShieldDamageFactor);
			int shieldsRemaining = ship.getShields() - shieldDamage;
			//Damage we wanted to do, but couldn't cause there wasn't enough shield strength left
			int shieldOverflow = 0;
			if(shieldsRemaining < 0) {
				shieldOverflow = Math.abs(shieldsRemaining);
				shieldsRemaining = 0;
			}

			ship.setShields(shieldsRemaining);

			//No damage left after shields
			if(shieldOverflow == 0 && shipType.getShields() != 0) {
				return;
			}

			double shieldOverflowFactor = 1.0d;
			if(shipType.getShields() != 0) {
				shieldOverflowFactor = (double)shieldOverflow / (double)shieldDamage;
			}
			int ablativeDamage = (int)Math.floor(shipType.getAblativeArmor() * ablativeDamageFactor * shieldOverflowFactor);
			int ablativeRemaining = ship.getAblativeArmor() - ablativeDamage;
			int ablativeOverflow = 0;
			if(ablativeRemaining < 0) {
				ablativeOverflow = Math.abs(ablativeRemaining);
				ablativeRemaining = 0;
			}

			ship.setAblativeArmor(ablativeRemaining);

			// No more shields left to save them -> also damage docked ships
			for (Ship dockedShip : ship.getDockedShips()) {
				damageInternal(dockedShip, shieldOverflowFactor, shieldDamageFactor, ablativeDamageFactor, hullDamageFactor, subsystemDamageFactor);
			}

			//No damage left after ablative armor
			if(ablativeOverflow == 0 && shipType.getAblativeArmor() != 0) {
				return;
			}

			double ablativeOverflowFactor = 1.0d;
			if(shipType.getAblativeArmor() != 0) {
				ablativeOverflowFactor = (double)ablativeOverflow / (double)ablativeDamage;
			}
			int hullDamage = (int)Math.floor(shipType.getHull() * hullDamageFactor * ablativeOverflowFactor);
			int hullRemaining = ship.getHull() - hullDamage;
			if(hullRemaining <= 0) {
				ship.destroy();
				return;
			} else {
				ship.setHull(hullRemaining);
			}

			//Damage each subsystem individually, so you don't get very good or bad results based on one roll
			damageSubsystem(subsystemDamageFactor, ship.getComm(), ship::setComm);
			damageSubsystem(subsystemDamageFactor, ship.getEngine(), ship::setEngine);
			damageSubsystem(subsystemDamageFactor, ship.getSensors(), ship::setSensors);
			damageSubsystem(subsystemDamageFactor, ship.getWeapons(), ship::setWeapons);
		}

		private void damageSubsystem(double subsystemDamageFactor, int subsystemBefore, IntConsumer subsystem) {
			double modifiedSubsystemDamageFactor = ThreadLocalRandom.current().nextDouble() * subsystemDamageFactor;
			int subsystemDamage = (int)Math.floor(100 * modifiedSubsystemDamageFactor);
			int subsystemRemaining = Math.max(0, subsystemBefore - subsystemDamage);
			subsystem.accept(subsystemRemaining);
		}
	}

	@EmbeddedId
	private MutableLocation loc;
	@Index(name="idx_nebulatype")
	@Enumerated
	@Column(nullable = false)
	private Typ type;

	public MutableLocation getLoc() {
		return loc;
	}

	public void setLoc(MutableLocation loc) {
		this.loc = loc;
	}

	/**
	 * Konstruktor.
	 *
	 */
	public Nebel() {
		// EMPTY
	}

	/**
	 * Erstellt einen neuen Nebel.
	 * @param loc Die Position des Nebels
	 * @param type Der Typ
	 */
	public Nebel(MutableLocation loc, Typ type) {
		this.loc = loc;
		this.type = type;
	}

	/**
	 * Gibt das System des Nebels zurueck.
	 * @return Das System
	 */
	public int getSystem() {
		return loc.getSystem();
	}

	/**
	 * Gibt den Typ des Nebels zurueck.
	 * @return Der Typ
	 */
	public Typ getType() {
		return this.type;
	}

	/**
	 * Gibt die X-Koordinate zurueck.
	 * @return Die X-Koordinate
	 */
	public int getX() {
		return loc.getX();
	}

	/**
	 * Gibt die Y-Koordinate zurueck.
	 * @return Die Y-Koordinate
	 */
	public int getY() {
		return loc.getY();
	}

	/**
	 * Gibt die Position des Nebels zurueck.
	 * @return Die Position
	 */
	@Override
	public Location getLocation() {
		return loc.getLocation();
	}

	/**
	 * Gibt an, ob Schiffe in diesem Feld scannen duerfen.
	 *
	 * @return <code>true</code>, wenn sie scannen duerfen, sonst <code>false</code>.
	 */
	public boolean allowsScan()
	{
		return this.type.allowsScan();
	}

	/**
	 * Gibt das Bild des Nebels zurueck (als Pfad).
	 * Der Pfad ist relativ zum data-Verzeichnis.
	 *
	 * @return Das Bild des Nebels als Pfad.
	 */
	public String getImage()
	{
		return this.type.getImage();
	}
}
