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
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ships;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ein Nebel.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="nebel")
@Immutable
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
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
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		// Hibernate cachet nur Ergebnisse, die nicht leer waren.
		// Da es jedoch viele Positionen ohne Nebel gibt wuerden viele Abfragen
		// mehrfach durchgefuehrt. Daher wird in der Session vermerkt, welche
		// Positionen bereits geprueft wurden

		Map<Location,Boolean> map = (Map<Location,Boolean>)context.getVariable(Ships.class, "getNebula(Location)#Nebel");
		if( map == null ) {
			map = new HashMap<>();
			context.putVariable(Ships.class, "getNebula(Location)#Nebel", map);
		}
		if( !map.containsKey(loc) ) {
			Nebel nebel = (Nebel)db.get(Nebel.class, new MutableLocation(loc));
			if( nebel == null ) {
				map.put(loc, Boolean.FALSE);
				return null;
			}

			map.put(loc, Boolean.TRUE);
			return nebel.getType();
		}

		Boolean val = map.get(loc);
		if(val) {
			Nebel nebel = (Nebel)db.get(Nebel.class, new MutableLocation(loc));
			return nebel.getType();
		}

		return null;
	}

	/**
	 * Nebeltyp.
	 */
	public enum Typ
	{
		/**
		 * Normaler Deutnebel.
		 */
		MEDIUM_DEUT(0, 7, false, 0, 7),
		/**
		 * Schwacher Deutnebel.
		 */
		LOW_DEUT(1, 5, false, -1, 5),
		/**
		 * Dichter Deutnebel.
		 */
		STRONG_DEUT(2, 11, false, 1, 11),
		/**
		 * Schwacher EMP-Nebel.
		 */
		LOW_EMP(3, Integer.MAX_VALUE, true, Integer.MIN_VALUE, 0),
		/**
		 * Normaler EMP-Nebel.
		 */
		MEDIUM_EMP(4, Integer.MAX_VALUE, true, Integer.MIN_VALUE, 0),
		/**
		 * Dichter EMP-Nebel.
		 */
		STRONG_EMP(5, Integer.MAX_VALUE, true, Integer.MIN_VALUE, 0),
		/**
		 * Schadensnebel.
		 */
		DAMAGE(6, 7, false, Integer.MIN_VALUE, 9);

		private final int code;
		private final int minScansize;
		private final boolean emp;
		private final int deutfaktor;
		private final int minScanbareSchiffsgroesse;
		
		Typ(int code, int minScansize, boolean emp, int deutfaktor, int minScanbareSchiffsgroesse)
		{
			this.code = code;
			this.minScansize = minScansize;
			this.emp = emp;
			this.deutfaktor = deutfaktor;
			this.minScanbareSchiffsgroesse = minScanbareSchiffsgroesse;
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
		public static List<Typ> getEmpNebel()
		{
			List<Typ> result = new ArrayList<>();
			for( Typ typ : values() )
			{
				if( typ.isEmp() )
				{
					result.add(typ);
				}
			}
			return result;
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
	}

	@Id
	private MutableLocation loc;
	@Index(name="idx_nebulatype")
	@Enumerated
	@Column(nullable = false)
	private Typ type;
	
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
