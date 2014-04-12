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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.UnmodifiableCargo;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

//TODO: Warum Verbrauch/Produktion unterscheiden?
/**
 * Basisklasse fuer alle Coretypen.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="cores")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("1")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public abstract class Core {
	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String name;
	@ManyToOne(optional = false)
	@JoinColumn(nullable = false, name="astitype")
	@ForeignKey(name="core_fk_basetype")
	private BaseType astiType;
	@Type(type="cargo")
	@Column(nullable = false)
	private Cargo buildcosts;
	@Type(type="cargo")
	@Column(nullable = false)
	private Cargo produces;
	@Type(type="cargo")
	@Column(nullable = false)
	private Cargo consumes;
	private int arbeiter;
	@Column(name="ever", nullable = false)
	private int eVerbrauch;
	@Column(name="eprodu", nullable = false)
	private int eProduktion;
	private int bewohner;
	@ForeignKey(name="core_fk_forschung")
	@ManyToOne
	@JoinColumn
	private Forschung techReq;
	private int eps;	
	private boolean shutdown;
	
	/**
	 * Konstruktor.
	 *
	 */
	public Core() {
		this.name = "";
		this.buildcosts = new Cargo();
		this.produces = new Cargo();
		this.consumes = new Cargo();
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
	public BaseType getAstiType() {
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
	 * Gibt zurueck, ob dieser Core bei unzureichenden Voraussetzungen abschaltet.
	 * @return <code>true</code>, wenn dieses Gebaeude abschaltet
	 */
	public boolean isShutDown() {
		return shutdown;
	}
	
	/**
	 * Setzt, ob dieser Core bei unzureichenden Voraussetzungen abschaltet.
	 * @param shutdown <code>true</code>, wenn der Core abschalten soll
	 */
	public void setShutDown(boolean shutdown) {
		this.shutdown = shutdown;
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
	public Forschung getTechRequired() {
		return techReq;
	}
	
	/**
	 * Unbekannt (?????) - Wird aber auch nicht verwendet.
	 * @return ????
	 */
	public int getEPS() {
		return eps;
	}
	
	/**
	 * Setzt den Namen.
	 * 
	 * @param name Name
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Setzt den Astityp.
	 * 
	 * @param astiType Astityp
	 */
	public void setAstiType(BaseType astiType)
	{
		this.astiType = astiType;
	}

	/**
	 * Setzt die Baukosten.
	 * 
	 * @param buildcosts Baukosten
	 */
	public void setBuildcosts(Cargo buildcosts)
	{
		this.buildcosts = buildcosts;
	}

	/**
	 * Setzt die Produktion.
	 * 
	 * @param produces Produktion
	 */
	public void setProduces(Cargo produces)
	{
		this.produces = produces;
	}

	/**
	 * Setzt den Verbrauch.
	 * 
	 * @param consumes Verbrauch
	 */
	public void setConsumes(Cargo consumes)
	{
		this.consumes = consumes;
	}

	/**
	 * Setzt die notwendigen Arbeiter.
	 * 
	 * @param arbeiter Arbeiter
	 */
	public void setArbeiter(int arbeiter)
	{
		this.arbeiter = arbeiter;
	}

	/**
	 * Setzt den Energieverbrauch.
	 * 
	 * @param verbrauch Verbrauch
	 */
	public void setEVerbrauch(int verbrauch)
	{
		eVerbrauch = verbrauch;
	}

	/**
	 * Setzt die Energieproduktion.
	 * 
	 * @param produktion Energieproduktion
	 */
	public void setEProduktion(int produktion)
	{
		eProduktion = produktion;
	}

	/**
	 * Setzt den Wohnraum.
	 * 
	 * @param bewohner Wohnraum
	 */
	public void setBewohner(int bewohner)
	{
		this.bewohner = bewohner;
	}

	/**
	 * Setzt die notwendige Forschung.
	 * 
	 * @param techReq Forschung
	 */
	public void setTechReq(Forschung techReq)
	{
		this.techReq = techReq;
	}

	/**
	 * Setzt den Energiespeicher.
	 * 
	 * @param eps Energiespeicher
	 */
	public void setEps(int eps)
	{
		this.eps = eps;
	}
}
