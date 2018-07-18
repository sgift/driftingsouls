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

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.HibernateCargoType;
import net.driftingsouls.ds2.server.cargo.HibernateLargeCargoType;
import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DSObject;
import net.driftingsouls.ds2.server.ships.Ship;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Repraesentiert einen Offizier in DS.
 * @author Christopher Jung
 *
 */
@TypeDefs(
	{
		@TypeDef(
				name="cargo",
				typeClass = HibernateCargoType.class
		),
		@TypeDef(
				name="largeCargo",
				typeClass = HibernateLargeCargoType.class
		)
	}
)
@Entity
@Table(name="offiziere")
@BatchSize(size=50)
public class Offizier extends DSObject {
	/**
	 * Die Attribute eines Offiziers.
	 * @author Christopher Jung
	 *
	 */
	public enum Ability {
		/**
		 * Der Navigationsskill.
		 */
		NAV,
		/**
		 * Der Ingenieursskill/Technikskill.
		 */
		ING,
		/**
		 * Der Waffenskill.
		 */
		WAF,
		/**
		 * Der Sicherheitsskill.
		 */
		SEC,
		/**
		 * Der Kommandoskill.
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
		 * Keine Spezialfaehigkeit.
		 */
		NONE("Nichts"),
		/**
		 * Motivationskuenstler.
		 */
		MOTIVATIONSKUENSTLER("Motivationsk&uuml;nstler"),
		/**
		 * Schnellmerker.
		 */
		SCHNELLMERKER("Schnellmerker"),
		/**
		 * Technikfreak.
		 */
		TECHNIKFREAK("Technikfreak"),
		/**
		 * Waffennarr.
		 */
		WAFFENNARR("Waffennarr"),
		/**
		 * Bleifuss.
		 */
		BLEIFUSS("Bleifuss"),
		/**
		 * Verrueckter Diktator.
		 */
		VERRUECKTER_DIKTATOR("Verr&uuml;ckter Diktator");

		private String name;
		Special(String name) {
			this.name = name;
		}

		/**
		 * Gibt den Namen der Spezialfaehigkeit zurueck (entspricht nicht
		 * zwangslaeufig der Konstante!).
		 * @return Der Name
		 */
		public String getName() {
			return name;
		}
	}

	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String name;
	private int rang;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="userid", nullable = false)
	@ForeignKey(name="offiziere_fk_users")
	private User owner;
	@ManyToOne(fetch=FetchType.LAZY)
	@ForeignKey(name="offiziere_fk_ships")
	private Ship stationiertAufSchiff;
	@ManyToOne(fetch=FetchType.LAZY)
	@ForeignKey(name="offiziere_fk_bases")
	private Base stationiertAufBasis;
	private boolean training;
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

	/**
	 * Konstruktor.
	 *
	 */
	public Offizier() {
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param owner Der Besitzer des Offiziers
	 * @param name Der Name des Offiziers
	 */
	public Offizier(User owner, String name) {
		setOwner(owner);
		setName(name);
	}

	/**
	 * Gibt den Namen des Offiziers zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Namen des Offiziers.
	 * @param name der neue Name
	 */
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 * Gibt die ID des Offiziers zurueck.
	 * @return die ID
	 */
	public int getID() {
		return id;
	}

	/**
	 * Gibt den Rang des Offiziers zurueck.
	 * @return der Rang
	 */
	public int getRang() {
		return rang;
	}

	/**
	 * Setzt den Rang des Offiziers.
	 * @param rang Der Rang
	 */
	public void setRang(int rang) {
		this.rang = rang;
	}

	/**
	 * Gibt den Pfad des zum Offizier gehoerenden Bilds zurueck.
	 * Der Pfad ist bereits eine vollstaendige URL.
	 * @return Der Pfad des Bildes
	 */
	public String getPicture() {
		return "./data/interface/offiziere/off"+getRang()+".png";
	}

	/**
	 * Gibt zurueck, ob der Offizier momentan ausgebildet wird.
	 * @return <code>true</code> falls dem so ist
	 */
	public boolean isTraining()
	{
		return training;
	}

	/**
	 * Setzt, ob der Offizier momentan gerade ausgebildet wird.
	 * @param training <code>true</code> falls er ausgebildet wird
	 */
	public void setTraining(boolean training)
	{
		this.training = training;
	}

	/**
	 * Gibt das Zielschiff zurueck, auf dem sich der Offizier befindet.
	 * Falls der Offizier sich nicht auf einem Schiff befindet wird <code>null</code>
	 * zurueckgegeben.
	 * @return Das Schiff oder <code>null</code>
	 * @see #getStationiertAufBasis()
	 */
	public Ship getStationiertAufSchiff()
	{
		return stationiertAufSchiff;
	}

	/**
	 * Setzt das Zielschiff auf dem sich der Offizier befindet.
	 * @param schiff Das Schiff
	 */
	public void setStationiertAufSchiff(Ship schiff)
	{
		if( schiff == null )
		{
			throw new NullPointerException("Das Schiff darf nicht null sein");
		}
		if( this.stationiertAufSchiff != null )
		{
			this.stationiertAufSchiff.onOffizierEntfernt(this);
		}
		this.stationiertAufSchiff = schiff;
		this.stationiertAufBasis = null;
	}

	/**
	 * Gibt die Zielbasis zurueck, auf der sich der Offizier befindet.
	 * Falls der Offizier sich nicht auf einer Basis befindet wird <code>null</code>
	 * zurueckgegeben.
	 * @return Die Basis oder <code>null</code>
	 * @see #getStationiertAufSchiff()
	 */
	public Base getStationiertAufBasis()
	{
		return stationiertAufBasis;
	}

	/**
	 * Gibt den Besitzers des Offiziers zurueck.
	 * @return der Besitzer
	 */
	public User getOwner() {
		return owner;
	}

	/**
	 * Setzt den Besitzer des Offiziers.
	 * @param owner der neue Besitzer
	 */
	public void setOwner( User owner ) {
		this.owner = owner;
	}

	/**
	 * Gibt den aktuellen Skillwert der angegebenen Faehigkeit des Offiziers zurueck.
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
	 * Setzt den Skillwert einer Faehigkeit des offiziers.
	 * @param ability Die Faehigkeit
	 * @param value Der Skill
	 */
	public void setAbility(Ability ability, int value) {
		switch( ability ) {
		case ING:
			ing = value;
			break;
		case WAF:
			waf = value;
			break;
		case NAV:
			nav = value;
			break;
		case SEC:
			sec = value;
			break;
		case COM:
			com = value;
			break;
	}
	}

	/**
	 * Gibt dem Offizier Erfahrungspunkte fuer die Faehigkeit.
	 *
	 * @param ability Die Faehigkeit
	 * @param exp Die Anzahl der Erfahrungspunkte
	 */
	public void gainExperience( Ability ability, int exp )
	{
		boolean newrang = false;

		switch( ability ) {
			case ING: {
				this.ingu = this.ingu + exp;
				int fak = 2;
				if( this.spec == 2)
				{
					fak = 1;
				}
				if( this.ingu > this.ing * fak)
				{
					this.ing++;
					this.ingu = 0;
					newrang = true;
				}
				break;
			}
			case WAF:
				this.wafu = this.wafu + exp;
				int fak = 2;
				if( this.spec == 2)
				{
					fak = 1;
				}
				if( this.wafu > this.waf * fak)
				{
					this.waf++;
					this.wafu = 0;
					newrang = true;
				}
				break;

			case NAV: {
				this.navu = this.navu + exp;
				fak = 2;
				if( this.spec == 2)
				{
					fak = 1;
				}
				if( this.navu > this.nav * fak)
				{
					this.nav++;
					this.navu = 0;
					newrang = true;
				}
				break;
			}
			case SEC:
				this.secu = this.secu + exp;
				fak = 2;
				if( this.spec == 2)
				{
					fak = 1;
				}
				if( this.secu > this.sec * fak)
				{
					this.sec++;
					this.secu = 0;
					newrang = true;
				}
				break;

			case COM:
				this.comu = this.comu + exp;
				fak = 2;
				if( this.spec == 2)
				{
					fak = 1;
				}
				if( this.comu > this.com * fak)
				{
					this.com++;
					this.comu = 0;
					newrang = true;
				}
				break;
		}

		if( newrang ) {
			double rangf = (this.ing+this.waf+this.nav+this.sec+this.com);
			int rang = (int)(rangf/125);
			if( rang > Offiziere.MAX_RANG ) {
				rang = Offiziere.MAX_RANG;
			}

			this.rang = rang;
		}
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
		int count = 0;

		switch( ability ) {
			case ING: {
				int fak = difficulty;
				if( this.spec == 3 ) {
					fak *= 0.6;
				}
				if( this.ing > fak*(ThreadLocalRandom.current().nextInt(101)/100d) ) {
					count++;

					if( ThreadLocalRandom.current().nextInt(31) > 10 ) {
						this.ingu++;
						fak = 2;
						if( this.spec == 2) {
							fak = 1;
						}
						if( this.ingu > this.ing * fak ) {
							MESSAGE.get().append(Common._plaintitle(this.name)).append(" hat seine Ingeneursf&auml;higkeit verbessert\n");
							this.ing++;
							this.ingu = 0;
						}
					}
				}
				break;
			}
			case WAF:
				break;

			case NAV: {
				int fak = difficulty;
				if( this.spec == 5 ) {
					fak *= 0.6;
				}
				if( this.nav > fak*(ThreadLocalRandom.current().nextInt(101)/100d) ) {
					count++;

					if( ThreadLocalRandom.current().nextInt(31) > 10 ) {
						this.navu++;
						fak = 2;
						if( this.spec == 2) {
							fak = 1;
						}
						if( this.navu > this.nav * fak ) {
							MESSAGE.get().append(Common._plaintitle(this.name)).append(" hat seine Navigationsf&auml;higkeit verbessert\n");
							this.nav++;
							this.navu = 0;
						}
					}
				}
				break;
			}
			case SEC:
				break;

			case COM:
				break;
		}

		if( count != 0 ) {
			double rangf = (this.ing+this.waf+this.nav+this.sec+this.com)/5.0;
			int rang = (int)(rangf/125);
			if( rang > Offiziere.MAX_RANG ) {
				rang = Offiziere.MAX_RANG;
			}

			if( rang > this.rang ) {
				MESSAGE.get().append(this.name).append(" wurde bef&ouml;rdert\n");
				this.rang = rang;
			}
		}

		return count;
	}

	/**
	 * Gibt die Spezialfaehigkeit des Offiziers zurueck.
	 * @return Die Spezialfaehigkeit
	 */
	public Special getSpecial() {
		return Special.values()[spec];
	}

	/**
	 * Setzt die Spezialeigenschaft des Offiziers.
	 * @param special Die Spezialeigenschaft
	 */
	public void setSpecial(Special special) {
		this.spec = special.ordinal();
	}

	/**
	 * Prueft, ob der Offizier die angegebene Spezialfaehigkeit hat.
	 * @param special Die Spezialfaehigkeit
	 * @return <code>true</code>, falls der Offizier die Faehigkeit hat
	 */
	public boolean hasSpecial( Special special ) {
		return spec == special.ordinal();
	}

	/**
	 * @return Offensivskill des Offiziers.
	 */
	public int getOffensiveSkill()
	{
		int weaponSkill = getAbility(Offizier.Ability.WAF);
		int commSkill = getAbility(Offizier.Ability.COM);

		return (int)Math.round((weaponSkill + commSkill) / 2d);
	}

	/**
	 * @return Defensivskill des Offiziers.
	 */
	public int getDefensiveSkill()
	{
		return (getAbility(Offizier.Ability.NAV) + getAbility(Offizier.Ability.COM)) / 2;
	}

	/**
	 * @param verteidiger <code>true</code>, falls es der Verteidigende Offizier ist.
	 * @return Kapermultiplikator des Offiziers.
	 */
	public int getKaperMulti(boolean verteidiger)
	{
		int multi;
		if(verteidiger)
		{
			multi =  (int)Math.floor(((getAbility(Offizier.Ability.COM)+2*getAbility(Offizier.Ability.SEC))/3d) /25d)+1;
		}
		else
		{
			multi = (int)Math.floor(((2*getAbility(Offizier.Ability.COM)+getAbility(Offizier.Ability.SEC))/3d) /25d)+1;
		}
		if(hasSpecial(Special.MOTIVATIONSKUENSTLER))
		{
			multi++;
		}
		return multi;
	}

	/**
	 * Stationiert den Offizier auf dem angegebenen Zielobjekt. Es erfolgt keine sonstige
	 * Statusberechnung/korrektur auf dem bisherigen oder dem zukuenftigen Stationierungsort.
	 * @param ziel Der neue Stationierungsort
	 * @throws IllegalArgumentException Falls der Typ des Aufenthaltsorts unbekannt ist
	 */
	public void stationierenAuf(Object ziel) throws IllegalArgumentException
	{
		if( this.stationiertAufSchiff != null )
		{
			this.stationiertAufSchiff.onOffizierEntfernt(this);
		}

		if( ziel instanceof Ship )
		{
			this.stationiertAufBasis = null;
			this.stationiertAufSchiff = (Ship)ziel;
			this.stationiertAufSchiff.onOffizierStationiert(this);
		}
		else if( ziel instanceof Base )
		{
			this.stationiertAufSchiff = null;
			this.stationiertAufBasis = (Base)ziel;
		}
		else
		{
			throw new IllegalArgumentException("Unbekannter Typ von Aufenthaltsort: "+ziel);
		}
	}

	/**
	 * Gibt den Offizier mit der angegebenen ID zurueck. Sollte kein solcher Offizier
	 * existieren, so wird <code>null</code> zurueckgegeben.
	 * @param id Die ID des Offiziers
	 * @return Der Offizier oder <code>null</code>
	 */
	public static Offizier getOffizierByID(int id) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		return (Offizier)db.get(Offizier.class, id);
	}

	/**
	 * Gibt alle Offiziere am angegebenen Aufenthaltsort zurueck.
	 * @param ziel Der Aufenthaltsort
	 * @return Die Liste aller Offiziere
	 * @throws IllegalArgumentException Falls der Typ des Aufenthaltsorts unbekannt ist
	 */
	@SuppressWarnings("unchecked")
	public static List<Offizier> getOffiziereByDest(Object ziel) throws IllegalArgumentException {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		if( ziel instanceof Ship )
		{
			return db.createQuery("from Offizier where stationiertAufSchiff= :dest")
					.setEntity("dest", ziel)
					.list();
		}
		else if( ziel instanceof Base )
		{
			return db.createQuery("from Offizier where stationiertAufBasis= :dest")
					.setEntity("dest", ziel)
					.list();
		}
		throw new IllegalArgumentException("Unbekannter Typ von Aufenthaltsort: "+ziel);
	}

	@Override
	public String toString()
	{
		return "Offizier{" +
				"id=" + id +
				", name='" + name + '\'' +
				", owner=" + owner +
				'}';
	}
}
