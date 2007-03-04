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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

// TODO: Warum Verbrauch/Produktion unterscheiden?
/**
 * Basisklasse fuer alle Gebaeudetypen
 * 
 * @author Christopher Jung
 *
 */
public abstract class Building implements Loggable {
	/**
	 * Die ID des Kommandozentralen-Gebaeudes
	 */
	public static final int KOMMANDOZENTRALE = 1;
	
	private static Map<Integer,Building> buildingCache = new HashMap<Integer,Building>();
	
	// TODO: remove me
	private static String fixPhpClassNames(String module) {
		String prefix = Building.class.getPackage().getName()+".";
		if( module.equals("default") ) {
			return prefix+"DefaultBuilding";
		}
		if( module.equals("kommandozentrale") ) {
			return prefix+"Kommandozentrale";
		}
		if( module.equals("forschungszentrum") ) {
			return prefix+"Forschungszentrum";
		}
		if( module.equals("academy") ) {
			return prefix+"Academy";
		}
		if( module.equals("werft") ) {
			return prefix+"Werft";
		}
		if( module.equals("waffenfabrik") ) {
			return prefix+"Waffenfabrik";
		}
		return prefix+"DefaultBuilding";
	}
	
	/**
	 * Gibt eine Instanz der Gebaudeklasse des angegebenen Gebaeudetyps zurueck.
	 * Sollte kein passender Gebaeudetyp existieren, wird <code>null</code> zurueckgegeben.
	 * 
	 * @param db Eine Datenbankverbindung
	 * @param id Die ID des Gebaudetyps
	 * @return Eine Instanz der zugehoerigen Gebaeudeklasse
	 */
	public static synchronized Building getBuilding(Database db, int id) {
		if( !buildingCache.containsKey(id) ) {
			SQLResultRow row = db.first("SELECT * FROM buildings WHERE id='",id,"'");
			if( row.isEmpty() ) {
				return null;
			}
			String module = row.getString("module");
			try {
				Class<?> cls = Class.forName(fixPhpClassNames(module));
				Class<? extends Building> buildingCls = cls.asSubclass(Building.class);
				Constructor<? extends Building> constr = buildingCls.getConstructor(SQLResultRow.class);
				buildingCache.put(id, constr.newInstance(row));
			}
			catch( Exception e ) {
				LOG.fatal(e, e);
				return null;
			}
		}
		return buildingCache.get(id);
	}

	/**
	 * Gibt die ID des Gebaeudetyps zurueck
	 * @return Die ID des Gebaeudetyps
	 */
	public abstract int getID();
	
	/**
	 * Gibt den Namen des Gebaeudetyps zurueck
	 * @return Der Name
	 */
	public abstract String getName();
	
	/**
	 * Gibt das zum Gebaeudetyp gehoerende Bild zurueck
	 * @return Das Bild
	 */
	public abstract String getPicture();
	
	/**
	 * Gibt die fuer das Gebaeude anfallenden Baukosten zurueck
	 * @return Die Baukosten
	 */
	public abstract Cargo getBuildCosts();
	
	/**
	 * Gibt die Produktion des Gebaeudes pro Tick zurueck
	 * @return Die Produktion
	 */
	public abstract Cargo getProduces();
	
	/**
	 * Gibt den Verbrauch des Gebaeudes pro Tick zurueck
	 * @return Der Verbrauch
	 */
	public abstract Cargo getConsumes();
	
	/**
	 * Gibt die Anzahl Wohnraum zurueck, die das Gebaeude schafft
	 * @return Der Wohnraum
	 */
	public abstract int getBewohner();
	
	/**
	 * Gibt die Anzahl der Arbeiter zurueck, die das Gebaeude fuer den Betrieb benoetigt
	 * @return Die Anzahl der Arbeiter
	 */
	public abstract int getArbeiter();
	
	/**
	 * Gibt den Energieverbrauch des Gebaeudes pro Tick zurueck
	 * @return Der Energieverbrauch
	 */
	public abstract int getEVerbrauch();
	
	/**
	 * Gibt die Energieproduktion des Gebaeudes pro Tick zurueck
	 * @return die Energieproduktion
	 */
	public abstract int getEProduktion();
	
	/**
	 * Gibt die ID der zum Bau benoetigten Forschung zurueck
	 * @return die benoetigte Forschung
	 */
	public abstract int getTechRequired();
	
	/**
	 * Unbekannt (?????) - Wird aber auch nicht verwendet
	 * @return ????
	 */
	public abstract int getEPS();
	
	/**
	 * Gibt die maximale Anzahl des Gebaeudes pro Basis zurueck.
	 * Sollte es keine Beschraenkung geben, so wird 0 zurueckgegeben.
	 * @return Die max. Anzahl pro Basis
	 */
	public abstract int getPerPlanetCount();
	
	/**
	 * Gibt die maximale Anzahl des Gebaeudes pro Benutzer zurueck.
	 * Sollte es keine Beschraenkung geben, so wird 0 zurueckgegeben.
	 * @return Die max. Anzahl pro Benutzer
	 */
	public abstract int getPerUserCount();
	
	/**
	 * Gibt die ID der Kategorie des Gebaeudes zurueck
	 * @return die ID der Kategorie
	 */
	public abstract int getCategory();
	
	/**
	 * Gibt <code>true</code> zurueck, falls das Gebaeude ein unterirdischer Komplex ist
	 * @return <code>true</code>, falls es ein unterirdischer Komplex ist
	 */
	public abstract boolean isUComplex();
	
	/**
	 * Gibt <code>true</code> zurueck, falls das Gebaeude deaktivierbar ist
	 * @return <code>true</code>, falls das Gebaeude deaktivierbar ist
	 */
	public abstract boolean isDeakAble();

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
	 * @param col Die ID der Basis
	 * @param field Das Feld, auf dem das Gebaeude steht
	 * @param building die ID des Gebaeudetyps
	 * @return Ein HTML-String, der die Gebaeudeseite einhaelt
	 */
	public abstract String output( Context context, TemplateEngine t, int col, int field, int building );
}
