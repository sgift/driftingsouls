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
package net.driftingsouls.ds2.server.entities;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.framework.Common;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;

/**
 * Eine Fabrik auf einer Basis.
 * 
 */

@Entity
@Table(name="factory",
	  uniqueConstraints = {@UniqueConstraint(name = "col_buildingid_idx", columnNames = {"col", "buildingid"})})
public class Factory {
	/**
	 * Ein Auftrag in einer Waffenfabrik.
	 * @author Christopher Jung
	 *
	 */
	public static class Task {
		private int id;
		private int count;
		
		protected Task(String task) {
			
			String[] tmp = StringUtils.split(task,'=');
			this.id = Integer.parseInt(tmp[0]);
			this.count = Integer.parseInt(tmp[1]);
		}
		
		/**
		 * Erstellt einen neuen Auftrag.
		 * @param id Die ID des Eintrages der produziert werden soll
		 * @param count Die zu produzierende Menge
		 */
		public Task(int id, int count) {
			this.id = id;
			this.count= count;
		}
		
		/**
		 * Gibt die ID zurueck, die zum Auftrag gehoert.
		 * @return Die ID
		 */
		public int getId() {
			return this.id;
		}
		
		/**
		 * Gibt die zu produzierende Menge an Ammo zurueck.
		 * @return Die Menge
		 */
		public int getCount() {
			return this.count;
		}
		
		@Override
		public String toString() {
			return this.id+"="+this.count;
		}
	}
	
	@SuppressWarnings("unused")
	@Id @GeneratedValue
	private int id;
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="col")
	@ForeignKey(name="factory_fk_bases")
	private Base base;
	private int count;
	@Lob
	@Column(nullable=false)
	private String produces;
	private int buildingid;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	protected Factory() {
		// EMPTY
	}
	
	/**
	 * Erstellt eine neue Fabrik auf der Basis.
	 * Die Waffenfabrik hat eine maximale Auslastung von 1.
	 * @param base Die Basis
	 * @param buildingid Die ID des Gebauedes
	 */
	public Factory(Base base, int buildingid) {
		this.base = base;
		this.count = 1;
		this.produces = "";
		this.buildingid = buildingid;
	}

	/**
	 * Gibt die Basis zurueck.
	 * @return Die Basis
	 */
	public Base getBase() {
		return base;
	}

	/**
	 * Setzt die Basis.
	 * @param base Die Basis
	 */
	public void setBase(Base base) {
		this.base = base;
	}

	/**
	 * Gibt die maximale Auslastung zurueck.
	 * @return Die maximale Auslastung
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Setzt die maximale Auslastung.
	 * @param count Die maximale Auslastung
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * Gibt die aktuellen Produktionsdaten zurueck.
	 * @return Die Produktionsdaten
	 */
	public Task[] getProduces() {
		String[] taskString = StringUtils.split(this.produces, ';');
		Task[] tasks = new Task[taskString.length];
		
		for( int i=0; i < taskString.length; i++ ) {
			tasks[i] = new Task(taskString[i]);
		}
		
		return tasks;
	}

	/**
	 * Setzt die aktuellen Produktionsdaten.
	 * @param produces Die Produktionsdaten
	 */
	public void setProduces(Task[] produces) {
		this.produces = Common.implode(";", produces);
	}

	/**
	 * Gibt die Id des Gebaeudes zurueck.
	 * @return Die Gebaeude-ID
	 * 
	 */
	public int getBuildingID()
	{
		return this.buildingid;
	}
	
	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
