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

import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

import java.util.ArrayList;
import java.util.List;

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
	 * Nebeltyp.
	 */
	public enum Typ
	{
		/**
		 * Normaler Deutnebel.
		 */
		MEDIUM_DEUT(0, 7, false, 0),
		/**
		 * Schwacher Deutnebel.
		 */
		LOW_DEUT(1, 5, false, -1),
		/**
		 * Dichter Deutnebel.
		 */
		STRONG_DEUT(2, 11, false, 1),
		/**
		 * Schwacher EMP-Nebel.
		 */
		LOW_EMP(3, Integer.MAX_VALUE, true, Integer.MIN_VALUE),
		/**
		 * Normaler EMP-Nebel.
		 */
		MEDIUM_EMP(4, Integer.MAX_VALUE, true, Integer.MIN_VALUE),
		/**
		 * Dichter EMP-Nebel.
		 */
		STRONG_EMP(5, Integer.MAX_VALUE, true, Integer.MIN_VALUE),
		/**
		 * Schadensnebel.
		 */
		DAMAGE(6, 7, false, Integer.MIN_VALUE);

		private final int code;
		private final int minScansize;
		private final boolean emp;
		private final int deutfaktor;
		
		private Typ(int code, int minScansize, boolean emp, int deutfaktor)
		{
			this.code = code;
			this.minScansize = minScansize;
			this.emp = emp;
			this.deutfaktor = deutfaktor;
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
			List<Typ> result = new ArrayList<Typ>();
			for( Typ typ : values() )
			{
				if( typ.isEmp() )
				{
					result.add(typ);
				}
			}
			return result;
		}
	}

	@Id
	private MutableLocation loc;
	@Enumerated
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
		return !isEmp();
	}

	/**
	 * Gibt das Bild des Nebels zurueck (als Pfad).
	 * Der Pfad ist relativ zum data-Verzeichnis.
	 * 
	 * @return Das Bild des Nebels als Pfad.
	 */
	public String getImage()
	{
		return "fog"+type.ordinal()+"/fog"+type.ordinal();
	}

	/**
	 * @return <code>true</code>, wenn in dem Feld ein EMP-Nebel ist, sonst <code>false</code>
	 */
	public boolean isEmp()
	{
		Typ nebula = this.type;
		if(nebula == Typ.LOW_EMP || nebula == Typ.MEDIUM_EMP || nebula == Typ.STRONG_EMP)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * @return <code>true</code>, wenn es ein Schadensnebel ist, sonst <code>false</code>.
	 */
	public boolean isDamage()
	{
		return Typ.DAMAGE == this.type;
	}
	
	/**
	 * Gibt die Mindestgroesse eines Schiffs zurueck, ab der es via LRS im Nebel geortet werden kann.
	 * @return Die Mindestgroesse
	 */
	public int getMinScanableShipSize()
	{
		Typ nebula = this.type;;
		
		if (nebula == Typ.LOW_DEUT )
		{
			return 5;
		}
		else if (nebula == Typ.MEDIUM_DEUT )
		{
			return 7;
		}
		else if (nebula == Typ.STRONG_DEUT)
		{
			return 11;
		}
		else if (nebula == Typ.DAMAGE)
		{
			return 9;
		}
		return 0;
	}
}
