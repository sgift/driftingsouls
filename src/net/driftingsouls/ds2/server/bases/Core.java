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
package net.driftingsouls.ds2.server.bases;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.UnmodifiableCargo;
import net.driftingsouls.ds2.server.framework.ContextMap;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

//TODO: Warum Verbrauch/Produktion unterscheiden?
/**
 * Basisklasse fuer alle Coretypen.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="cores")
@Immutable
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("1")
@Cache(usage=CacheConcurrencyStrategy.READ_ONLY)
public abstract class Core {
	@Id
	private int id;
	private String name;
	@Column(name="astitype")
	private int astiType;
	@Type(type="cargo")
	private Cargo buildcosts;
	@Type(type="cargo")
	private Cargo produces;
	@Type(type="cargo")
	private Cargo consumes;
	private int arbeiter;
	@Column(name="ever")
	private int eVerbrauch;
	@Column(name="eprodu")
	private int eProduktion;
	private int bewohner;
	@Column(name="techreq")
	private int techReq;
	private int eps;	
	
	/**
	 * Konstruktor.
	 *
	 */
	public Core() {
		// EMPTY
	}
	
	/**
	 * Gibt eine Instanz der Coreklasse des angegebenen Coretyps zurueck.
	 * Sollte kein passender Coretyp existieren, wird <code>null</code> zurueckgegeben.
	 * 
	 * @param id Die ID des Coretyps
	 * @return Eine Instanz der zugehoerigen Coreklasse
	 */
	public static Core getCore(int id) {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		return (Core)db.get(Core.class, id);
	}
	
	/**
	 * Die ID des Coretyps.
	 * @return die ID
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Der Name der Core.
	 * @return der Name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gibt den Basis-Typ, in den die Core passt, zurueck.
	 * @return der Basistyp
	 * @see Base#getKlasse()
	 */
	public int getAstiType() {
		return astiType;
	}
	
	/**
	 * Gibt die Baukosten, welche zum errichten der Core notwendig sind, zurueck.
	 * @return die Baukosten
	 */
	public Cargo getBuildCosts() {
		return new UnmodifiableCargo(buildcosts);
	}
	
	/**
	 * Gibt die Produktion pro Tick der Core zurueck.
	 * @return die Produktion pro Tick
	 */
	public Cargo getProduces() {
		return new UnmodifiableCargo(produces);
	}
	
	/**
	 * Gibt den Verbrauch pro Tick der Core zurueck.
	 * @return der Verbrauch pro Tick
	 */
	public Cargo getConsumes() {
		return new UnmodifiableCargo(consumes);
	}
	
	/**
	 * Gibt die Anzahl der zum Betrieb der Core notwendigen Arbeiter zurueck.
	 * @return die benoetigten Arbeiter
	 */
	public int getArbeiter() {
		return arbeiter;
	}
	
	/**
	 * Gibt den Energieverbrauch der Core pro Tick zurueck.
	 * @return der Energieverbrauch pro Tick
	 */
	public int getEVerbrauch() {
		return eVerbrauch;
	}
	
	/**
	 * Gibt die Energieproduktion der Core pro Tick zurueck.
	 * @return Die Energieproduktion pro Tick
	 */
	public int getEProduktion() {
		return eProduktion;
	}
	
	/**
	 * Gibt den durch die Core bereitgestellten Wohnraum zurueck.
	 * @return Der Wohnraum
	 */
	public int getBewohner() {
		return bewohner;
	}
	
	/**
	 * Gibt die ID der Forschung zurueck, welche zum errichten der Core benoetigt wird.
	 * @return Die ID der benoetigten Forschung
	 */
	public int getTechRequired() {
		return techReq;
	}
	
	/**
	 * Unbekannt (?????) - Wird aber auch nicht verwendet.
	 * @return ????
	 */
	public int getEPS() {
		return eps;
	}
}
