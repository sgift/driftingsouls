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
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

// TODO: Warum Verbrauch/Produktion unterscheiden?
/**
 * Basisklasse fuer alle Gebaeudetypen
 * 
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="buildings")
@Immutable
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("module")
@Cache(usage=CacheConcurrencyStrategy.READ_ONLY)
public abstract class Building {
	/**
	 * Die ID des Kommandozentralen-Gebaeudes
	 */
	public static final int KOMMANDOZENTRALE = 1;
	
	/**
	 * Gibt eine Instanz der Gebaudeklasse des angegebenen Gebaeudetyps zurueck.
	 * Sollte kein passender Gebaeudetyp existieren, wird <code>null</code> zurueckgegeben.
	 * 
	 * @param id Die ID des Gebaudetyps
	 * @return Eine Instanz der zugehoerigen Gebaeudeklasse
	 */
	public static Building getBuilding(int id) {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		return (Building)db.get(Building.class, id);
	}
	
	@Id
	private int id;
	@SuppressWarnings("unused")
	private String module;
	private int bewohner;
	private int arbeiter;
	private String name;
	private String picture;
	@Type(type="cargo")
	@Column(name="buildcosts")
	private Cargo buildCosts;
	@Type(type="cargo")
	private Cargo produces;
	@Type(type="cargo")
	private Cargo consumes;
	@Column(name="ever")
	private int eVerbrauch;
	@Column(name="eprodu")
	private int eProduktion;
	@Column(name="techreq")
	private int techReq;
	private int eps;
	@Column(name="perplanet")
	private int perPlanet;
	@Column(name="perowner")
	private int perOwner;
	private int category;
	private int ucomplex;
	private boolean deakable;
	
	/**
	 * Konstruktor
	 *
	 */
	public Building() {
		// EMPTY
	}

	/**
	 * Gibt die ID des Gebaeudetyps zurueck
	 * @return Die ID des Gebaeudetyps
	 */
	public int getId() {
		return this.id;
	}
	
	/**
	 * Gibt den Namen des Gebaeudetyps zurueck
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gibt das zum Gebaeudetyp gehoerende Bild zurueck
	 * @return Das Bild
	 */
	public String getPicture() {
		return picture;
	}
	
	/**
	 * Gibt die fuer das Gebaeude anfallenden Baukosten zurueck
	 * @return Die Baukosten
	 */
	public Cargo getBuildCosts() {
		return new UnmodifiableCargo(buildCosts);
	}
	
	/**
	 * Gibt die Produktion des Gebaeudes pro Tick zurueck
	 * @return Die Produktion
	 */
	public Cargo getProduces() {
		return new UnmodifiableCargo(produces);
	}
	
	/**
	 * Gibt den Verbrauch des Gebaeudes pro Tick zurueck
	 * @return Der Verbrauch
	 */
	public Cargo getConsumes() {
		return new UnmodifiableCargo(consumes);
	}
	
	/**
	 * Gibt die Anzahl Wohnraum zurueck, die das Gebaeude schafft
	 * @return Der Wohnraum
	 */
	public int getBewohner() {
		return bewohner;
	}
	
	/**
	 * Gibt die Anzahl der Arbeiter zurueck, die das Gebaeude fuer den Betrieb benoetigt
	 * @return Die Anzahl der Arbeiter
	 */
	public int getArbeiter() {
		return arbeiter;
	}
	
	/**
	 * Gibt den Energieverbrauch des Gebaeudes pro Tick zurueck
	 * @return Der Energieverbrauch
	 */
	public int getEVerbrauch() {
		return eVerbrauch;
	}
	
	/**
	 * Gibt die Energieproduktion des Gebaeudes pro Tick zurueck
	 * @return die Energieproduktion
	 */
	public int getEProduktion() {
		return eProduktion;
	}
	
	/**
	 * Gibt die ID der zum Bau benoetigten Forschung zurueck
	 * @return die benoetigte Forschung
	 */
	public int getTechRequired() {
		return techReq;
	}
	
	/**
	 * Unbekannt (?????) - Wird aber auch nicht verwendet
	 * @return ????
	 */
	public int getEPS() {
		return eps;
	}
	
	/**
	 * Gibt die maximale Anzahl des Gebaeudes pro Basis zurueck.
	 * Sollte es keine Beschraenkung geben, so wird 0 zurueckgegeben.
	 * @return Die max. Anzahl pro Basis
	 */
	public int getPerPlanetCount() {
		return perPlanet;
	}
	
	/**
	 * Gibt die maximale Anzahl des Gebaeudes pro Benutzer zurueck.
	 * Sollte es keine Beschraenkung geben, so wird 0 zurueckgegeben.
	 * @return Die max. Anzahl pro Benutzer
	 */
	public int getPerUserCount() {
		return perOwner;
	}
	
	/**
	 * Gibt die ID der Kategorie des Gebaeudes zurueck
	 * @return die ID der Kategorie
	 */
	public int getCategory() {
		return category;
	}
	
	/**
	 * Gibt <code>true</code> zurueck, falls das Gebaeude ein unterirdischer Komplex ist
	 * @return <code>true</code>, falls es ein unterirdischer Komplex ist
	 */
	public boolean isUComplex() {
		return ucomplex == 1;
	}
	
	/**
	 * Gibt <code>true</code> zurueck, falls das Gebaeude deaktivierbar ist
	 * @return <code>true</code>, falls das Gebaeude deaktivierbar ist
	 */
	public boolean isDeakAble() {
		return deakable;
	}

	/**
	 * Wird aufgerufen, wenn das Gebaeude auf einer Basis gebaut wurde
	 * @param base Die Basis
	 */
	public abstract void build( Base base );
	
	/**
	 * Wird aufgerufen, wenn das Gebaeude auf einer Basis abgerissen wurde
	 * @param context Der aktive Kontext
	 * @param base Die Basis
	 */
	public abstract void cleanup( Context context, Base base );
	
	/**
	 * Modifiziert das stats-objekt der Basis um die Stats dieses Gebaeudes
	 * @param base Die Basis
	 * @param stats Ein Cargo-Objekt mit dem aktuellen Stats
	 * @return Warnungen fuer den Benutzer/Fuers Log
	 */
	public abstract String modifyStats( Base base, Cargo stats );
	
	/**
	 * Gibt <code>true</code> zurueck, wenn das Gebaeude aktiv ist
	 * @param base Die Basis
	 * @param status der aktuelle Aktivierungsstatus (0 oder 1)
	 * @param field Das Feld, auf dem das Gebaeude steht
	 * @return <code>true</code>, falls das Gebaeude aktiv ist
	 */
	public abstract boolean isActive( Base base, int status, int field );
	
	/**
	 * Generiert einen Shortcut-Link (String) sofern notwendig. Sollte das Gebaeude keinen haben 
	 * wird ein leerer String zurueckgegeben
	 * @param context der aktive Kontext
	 * @param base Die Basis
	 * @param field Das Feld, auf dem das Gebaeude steht
	 * @param building die ID des Gebaeudetyps
	 * @return Ein HTML-String, welcher den Shortcut enthaelt
	 */
	public abstract String echoShortcut( Context context, Base base, int field, int building );
	
	/**
	 * Gibt <code>true</code> zurueck, wenn eine Kopfzeile ausgegeben werden soll (Enthaelt den Namen des Gebaeudes und ggf dessen Bild.
	 * Dies ist jedoch von {@link #classicDesign()} abhaengig)
	 * @return <code>true</code>, falls der Header ausgegeben werden soll
	 */
	public abstract boolean printHeader();
	
	/**
	 * Gibt <code>true</code> zurueck, wenn das klassische Design fuer die Gebaeudeseite verwendet werden soll
	 * @return <code>true</code>,falls das klassische Design verwendet werden soll
	 */
	public abstract boolean classicDesign();
	
	/**
	 * Gibt die eigendliche UI des Gebaeudes aus
	 * @param context Der aktive Kontext
	 * @param t Eine Instanz des zu verwendenden TemplateEngines
	 * @param base Die ID der Basis
	 * @param field Das Feld, auf dem das Gebaeude steht
	 * @param building die ID des Gebaeudetyps
	 * @return Ein HTML-String, der die Gebaeudeseite einhaelt
	 */
	public abstract String output( Context context, TemplateEngine t, Base base, int field, int building );
}
