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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Version;

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;

/**
 * Eine Waffenfabrik auf einer Basis
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="weaponfactory")
public class WeaponFactory {
	/**
	 * Ein Auftrag in einer Waffenfabrik
	 * @author Christopher Jung
	 *
	 */
	public static class Task {
		private Ammo ammo;
		private int count;
		
		protected Task(String task) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			String[] tmp = StringUtils.split(task,'=');
			this.ammo = (Ammo)db.get(Ammo.class, Integer.parseInt(tmp[0]));
			this.count = Integer.parseInt(tmp[1]);
		}
		
		/**
		 * Erstellt einen neuen Auftrag
		 * @param ammo Die Ammo die produziert werden soll
		 * @param count Die zu produzierende Menge
		 */
		public Task(Ammo ammo, int count) {
			this.ammo = ammo;
			this.count= count;
		}
		
		/**
		 * Gibt die Ammo zurueck, die zum Auftrag gehoert
		 * @return Die Ammo
		 */
		public Ammo getAmmo() {
			return this.ammo;
		}
		
		/**
		 * Gibt die zu produzierende Menge an Ammo zurueck
		 * @return Die Menge
		 */
		public int getCount() {
			return this.count;
		}
		
		@Override
		public String toString() {
			return this.ammo.getId()+"="+this.count;
		}
	}
	
	@Id
	private int col;
	@OneToOne(fetch=FetchType.LAZY)
	@PrimaryKeyJoinColumn
	private Base base;
	private int count;
	private String produces;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor
	 *
	 */
	public WeaponFactory() {
		// EMPTY
	}
	
	/**
	 * Erstellt eine neue Waffenfabrik auf der Basis.
	 * Die Waffenfabrik hat eine maximale Auslastung von 1.
	 * @param base Die Basis
	 */
	public WeaponFactory(Base base) {
		this.col = base.getId();
		this.base = base;
		this.count = 1;
		this.produces = "";
	}

	/**
	 * Gibt die Basis zurueck
	 * @return Die Basis
	 */
	public Base getBase() {
		return base;
	}

	/**
	 * Setzt die Basis
	 * @param base Die Basis
	 */
	public void setBase(Base base) {
		this.base = base;
	}

	/**
	 * Gibt die maximale Auslastung zurueck
	 * @return Die maximale Auslastung
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Setzt die maximale Auslastung
	 * @param count Die maximale Auslastung
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * Gibt die aktuellen Produktionsdaten zurueck
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
	 * Setzt die aktuellen Produktionsdaten
	 * @param produces Die Produktionsdaten
	 */
	public void setProduces(Task[] produces) {
		this.produces = Common.implode(";", produces);
	}

	/**
	 * Gibt die ID der Basis zurueck
	 * @return Die ID der Basis
	 */
	public int getId() {
		return col;
	}

	/**
	 * Gibt die Versionsnummer zurueck
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
