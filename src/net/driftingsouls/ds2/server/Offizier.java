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
package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DSObject;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

import org.apache.commons.lang.StringUtils;

/**
 * Repraesentiert einen Offizier in DS
 * @author Christopher Jung
 *
 */
public class Offizier extends DSObject {
	/**
	 * Die Attribute eines Offiziers
	 * @author Christopher Jung
	 *
	 */
	public enum Ability {
		/**
		 * Der Navigationsskill
		 */
		NAV,
		/**
		 * Der Ingenieursskill/Technikskill
		 */
		ING,
		/**
		 * Der Waffenskill
		 */
		WAF,
		/**
		 * Der Sicherheitsskill
		 */
		SEC,
		/**
		 * Der Kommandoskill
		 */
		COM
	}
	
	/**
	 * Die Spezialfaehigkeiten der Offiziere.
	 * Jeder Offizier besitzt eine Spezialfaehigkeit
	 * @author Christopher Jung
	 *
	 */
	public enum Special {
		/**
		 * Keine Spezialfaehigkeit
		 */
		NONE("Nichts"),
		/**
		 * Motivationskuenstler
		 */
		MOTIVATIONSKUENSTLER("Motivationsk&uuml;nstler"),
		/**
		 * Schnellmerker
		 */
		SCHNELLMERKER("Schnellmerker"),
		/**
		 * Technikfreak
		 */
		TECHNIKFREAK("Technikfreak"),
		/**
		 * Waffennarr
		 */
		WAFFENNARR("Waffennarr"),
		/**
		 * Bleifuss
		 */
		BLEIFUSS("Bleifuss"),
		/**
		 * Verrueckter Diktator
		 */
		VERRUECKTER_DIKTATOR("Verr&uuml;ckter Diktator");
		
		private String name;
		private Special(String name) {
			this.name = name;
		}
		
		/**
		 * Gibt den Namen der Spezialfaehigkeit zurueck (entspricht nicht 
		 * zwangslaeufig der Konstante!)
		 * @return Der Name
		 */
		public String getName() {
			return name;
		}
	}
	
	private int id;
	private String name;
	private int rang;
	private int owner;
	private String dest;
	private int ing;
	private int waf;
	private int nav;
	private int sec;
	private int com;
	private int spec;
	private int ingu;
	private int navu;
	private int wafu;
	private int secu;
	private int comu;
	private boolean changed = false;;
	
	/**
	 * Erstellt eine neue Instanz aus einer SQL-Ergebniszeile. Die
	 * Zeile muss alle Daten aus der Offizierstabelle enthalten
	 * @param data die SQL-Ergebniszeile
	 */
	public Offizier( SQLResultRow data ) {
		id = data.getInt("id");
		name = data.getString("name");
		rang = data.getInt("rang");
		owner = data.getInt("userid");
		dest = data.getString("dest");
		ing = data.getInt("ing");
		waf = data.getInt("waf");
		nav = data.getInt("nav");
		sec = data.getInt("sec");
		com = data.getInt("com");
		spec = data.getInt("spec");
		ingu = data.getInt("ingu");
		navu = data.getInt("navu");
		wafu = data.getInt("wafu");
		secu = data.getInt("secu");
		comu = data.getInt("comu");	
	}
	
	/**
	 * Gibt den Namen des Offiziers zurueck
	 * @return Der Name
	 */
	public String getName() {
		return name;	
	}
	
	/**
	 * Setzt den Namen des Offiziers
	 * @param name der neue Name
	 */
	public void setName( String name ) {
		this.name = name;
		
		changed = true;	
	}
	
	/**
	 * Gibt die ID des Offiziers zurueck
	 * @return die ID
	 */
	public int getID() {
		return id;	
	}
	
	/**
	 * Gibt den Rang des Offiziers zurueck
	 * @return der Rang
	 */
	public int getRang() {
		return rang;	
	}
	
	/**
	 * Gibt den Pfad des zum Offizier gehoerenden Bilds zurueck.
	 * Der Pfad ist bereits eine vollstaendige URL.
	 * @return Der Pfad des Bildes
	 */
	public String getPicture() {
		return Configuration.getSetting("URL")+"data/interface/offiziere/off"+getRang()+".png";
	}
	
	/**
	 * Gibt den Aufenthaltsort des Offiziers als Array der Laenge 2 zurueck.
	 * Das erste Element kennzeichnet den Typ des Aufenthaltsortes (<code>s</code> bei Schiffen,
	 * <code>b</code> bei Basen und <code>t</code> bei aktuell laufender Ausbildung auf einer Basis).
	 * Das zweite Feld enthaelt die ID des Aufenthaltsortes
	 * @return Der Aufenthaltsort
	 */
	public String[] getDest() {
		return StringUtils.split(dest, ' ');
	}
	
	/**
	 * Setzt den Aufenthaltsort eines Offiziers
	 * @param dest Der Typ des Aufenthaltsortes (s, b, t)
	 * @param objectid Die ID des Aufenthaltsortes
	 * @see #getDest()
	 */
	public void setDest( String dest, int objectid ) {
		dest = dest+' '+objectid;
		changed = true;
	}
	
	/**
	 * Gibt die ID des Besitzers des Offiziers zurueck
	 * @return die ID des Besitzers
	 */
	public int getOwner() {
		return owner;	
	}
	
	/**
	 * Setzt den Besitzer des Offiziers auf die angegebene ID
	 * @param owner der neue Besitzer
	 */
	public void setOwner( int owner ) {
		this.owner = owner; 
		
		changed = true;	
	}
	
	/**
	 * Gibt den aktuellen Skillwert der angegebenen Faehigkeit des Offiziers zurueck
	 * @param ability Die Faehigkeit
	 * @return Der aktuelle Skill in dieser Faehigkeit
	 */
	public int getAbility( Ability ability ) {
		switch( ability ) {
			case ING:
				return ing;
			case WAF:
				return waf;
			case NAV:
				return nav;
			case SEC:
				return sec;
			case COM:
				return com;
		}
		return 0;
	}
	
	/**
	 * Benutzt einen Skill des Offiziers unter Beruecksichtigung 
	 * der Schwierigkeit der Aufgabe. Der Offizier kann dabei seinen
	 * Skill verbessern. Entsprechende Hinweistexte koennen via {@link DSObject#MESSAGE}
	 * erfragt werden. Zurueckgegeben wird, wie oft der Skill erfolgreich angewandt wurde.
	 * 
	 * @param ability Die Faehigkeit
	 * @param difficulty Die Schwierigkeit der Aufgabe
	 * @return Die Anzahl der erfolgreichen Anwendungen des Skills
	 */
	public int useAbility( Ability ability, int difficulty ) {
		// TODO
		throw new RuntimeException("STUB");
	}
	
	/**
	 * Gibt die Spezialfaehigkeit des Offiziers zurueck 
	 * @return Die Spezialfaehigkeit
	 */
	public Special getSpecial() {
		return Special.values()[spec];
	}
	
	/**
	 * Prueft, ob der Offizier die angegebene Spezialfaehigkeit hat
	 * @param special Die Spezialfaehigkeit
	 * @return <code>true</code>, falls der Offizier die Faehigkeit hat
	 */
	public boolean hasSpecial( Special special ) {
		return spec == special.ordinal();	
	}
	
	/**
	 * Speichert die Offiziersdaten in der Datenbank
	 *
	 */
	public void save() {
		if( changed ) {
			Database db = ContextMap.getContext().getDatabase();
			db.prepare("UPDATE offiziere ",
						"SET name=?, nav=?, navu=?, ing=?, ingu=?, ",
							"waf=?, wafu=?, sec=?, secu=?, com=?, comu=?," ,
							"rang=?,userid=?, dest=?,spec=? ",
						"WHERE id=?")
				.update(name, nav, navu, ing, ingu, 
						waf, wafu, sec, secu, com, comu, 
						rang, owner, dest, spec, 
						id);
		}			
	}
	
	/**
	 * Gibt einen Offizier am angegebenen Aufenthaltsort zurueck. Sollten mehrere
	 * Offiziere sich an diesem Ort aufhalten, so wird der beste von ihnen zurueckgegeben.
	 * Sollte sich an dem Ort kein Offizier aufhalten, so wird <code>null</code> zurueckgegeben.
	 * 
	 * @param dest Der Typ des Aufenthaltsortes (s, t, b)
	 * @param objid Die ID des Aufenthaltsortes
	 * @return Ein Offizier oder <code>null</code> 
	 * @see #getDest()
	 */
	public static Offizier getOffizierByDest(char dest, int objid) {
		Database db = ContextMap.getContext().getDatabase();
		
		SQLResultRow offizier = db.first("SELECT * FROM offiziere WHERE dest='",dest," ",objid,"' ORDER BY rang DESC,id ASC");
		if( !offizier.isEmpty() ) {
			return new Offizier(offizier);
		}
		return null;
	}
	
	/**
	 * Gibt den Offizier mit der angegebenen ID zurueck. Sollte kein solcher Offizier
	 * existieren, so wird <code>null</code> zurueckgegeben.
	 * @param id Die ID des Offiziers
	 * @return Der Offizier oder <code>null</code>
	 */
	public static Offizier getOffizierByID(int id) {
		Database db = ContextMap.getContext().getDatabase();
		
		SQLResultRow offizier = db.first("SELECT * FROM offiziere WHERE id='",id,"'");
		if( !offizier.isEmpty() ) {
			return new Offizier(offizier);
		}
		return null;
	}
}
