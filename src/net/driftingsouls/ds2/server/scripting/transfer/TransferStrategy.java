/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Sebastian Gift
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
package net.driftingsouls.ds2.server.scripting.transfer; 

import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Transfering;

/**
 * Basisklasse fuer Transferstrategien. Eine Transferstrategie
 * erlaubt den Transfer von Resourcen von einem Objekt eines Typs
 * zu einem anderen Objekt eines anderen oder des selben Typs.
 * 
 * @author Sebastian Gift
 *
 */
public abstract class TransferStrategy {
	private Transfering from;
	private Transfering to;
	private boolean forceSameOwner;

	/**
	 * Konstruktor.
	 *
	 */
	protected TransferStrategy() {
		// EMPTY
	}

	/**
	 * Setzt, ob Quelle und Ziel den selben Besitzer haben muessen.
	 * @param forceSameOwner <code>true</code>, falls sie den selben Besitzer haben muessen
	 */
	protected final void setForceSameOwner(boolean forceSameOwner) {
		this.forceSameOwner = forceSameOwner;
	}
	
	/**
	 * Setzt das Quellobjekt.
	 * @param from Das Quellobjekt
	 */
	protected final void setFrom(Transfering from) {
		if(from == null) {
			throw new IllegalArgumentException("From and to may not be null");
		}
		
		this.from = from;
	}
	
	/**
	 * Setzt das Zielobjekt.
	 * @param to Das Zielobjekt
	 */
	protected final void setTo(Transfering to) {
		if( to == null ) {
			throw new IllegalArgumentException("From and to may not be null");
		}
		
		this.to = to;
	}
	
	/**
	 * Transferiert die angegebene Resourcenmenge von Quellobjekt zum Zielobjekt.
	 * @param resource Die Resource
	 * @param count Die Menge die zu transferieren ist
	 * @return Ein Meldungstext
	 */
	public final String transfer(ResourceID resource, long count) {
		if( (from == null) || (to == null) ) {
			return "";
		}
		
		if( forceSameOwner && !from.getOwner().equals(to.getOwner()) ) {
			return "Verschiedene Besitzer - Konnte nicht transferieren.\n";
		}
		
		return getFrom().transfer(getTo(), resource, count);
	}

	private Transfering getFrom() {
		return from;
	}

	private Transfering getTo() {
		return to;
	}
	
	/**
	 * Wird der selbe Besitzer fuer den Warentransfer erzwungen?
	 * @return <code>true</code>, falls der Besitzer gleich sein muss
	 */
	public final boolean isForceSameOwner() {
		return forceSameOwner;
	}
}
