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
package net.driftingsouls.ds2.server.config.items.effects;

/**
 * Basisklasse fuer Item-Effekte in DS.
 * @author Christopher Jung
 *
 */
public abstract class ItemEffect {
	/**
	 * Liste aller Item-Effekt-Typen.
	 * @author Christopher Jung
	 *
	 */
	public enum Type {
		/**
		 * Kein Effekt.
		 */
		NONE("Ware"),
		/**
		 * Schiffsbauplan.
		 */
		DRAFT_SHIP("Schiffsbauplan"),
		//DRAFT_BUILDING,
		/**
		 * Munitionsbauplan.
		 */
		DRAFT_AMMO("Munitionsbauplan"),
		//DRAFT_CORE,
		/**
		 * Modul.
		 */
		MODULE("Modul"),
		/**
		 * Munition.
		 */
		AMMO("Munition"),
		/**
		 * Schiffsbauplan-Deaktivierer.
		 */
		DISABLE_SHIP("Schiffsverbot"),
		/**
		 * IFF-Deaktivierer.
		 */
		DISABLE_IFF("IFF-Sperre"),
		/**
		 * Modul-Set Metainformationen.
		 */
		MODULE_SET_META("Modul-Set");

		private String name = null;

		Type(String name) {
			this.name = name;
		}

		/**
		 * Gibt den umgangssprachlichen Namen der Modulklasse zurueck.
		 * @return der Name
		 */
		public String getName() {
			return name;
		}
	}

	private Type type = null;
	private boolean allyEffect = false;

	protected ItemEffect(Type type) {
		this.type = type;
	}

	protected ItemEffect(Type type, boolean allyEffect) {
		this(type);
		this.allyEffect = allyEffect;
	}

	/**
	 * Gibt den Typ des Item-Effekts zurueck.
	 * @return Der Typ des Item-Effekts
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * Gibt an, ob es sich bei dem Effekt um einen allianzweiten Effekt handelt.
	 * Allianzweite Effekte wirken sich nur dann aus, wenn das Item an die Allianz uebergeben wurde.
	 *
	 * @return <code>true</code>, falls es sich um einen Allianzeffekt handelt.
	 */
	public boolean hasAllyEffect() {
		return allyEffect;
	}

	/**
	 * Gibt den Itemeffect als String aus.
	 * @return der Effect als String
	 */
	@Override
	public String toString() {
		return null;
	}
}
