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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * <h1>Repraesentiert eine Forschung in DS.</h1>
 * Hinweis zu den Forschungs-IDs:<br>
 * Eine normale Forschung hat eine ID ab 1<br>
 * "Keine Forschung"/"Keine Vorbedingung" hat die ID 0<br>
 * "Nie erforschbar" hat die ID -1.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="forschungen")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Forschung {
	/**
	 * Beim Erforschen einer Forschung mit dieser Technologie, verliert
	 * der Spieler den Noob-Status.
	 */
	public static final String FLAG_DROP_NOOB_PROTECTION = "drop_noob";

	/**
	 * Sichtbarkeiten von einzelnen Forschungen.
	 */
	public enum Visibility {
		/**
		 * Erst sichtbar, wenn die Forschung auch erforschbar ist.
		 */
		IF_RESEARCHABLE("Sichtbar, wenn erforschbar"),
		/**
		 * Immer sichtbar.
		 */
		ALWAYS("Sichtbar"),
		/**
		 * Niemals sichtbar.
		 */
		NEVER("Unsichtbar");


		private String description;

		Visibility(String description) {
			this.description = description;
		}

		@Override
		public String toString() {
			return description;
		}
	}

	/**
	 * Gibt ein Forschungsobjekt fuer die angegebene Forschungs-ID zurueck.
	 * Sollte keine solche Forschung existieren, so wird <code>null</code> zurueckgegeben.
	 * @param fid Die Forschungs-ID
	 * @return Das zur ID gehoerende Forschungsobjekt oder <code>null</code>
	 */
	public static Forschung getInstance( int fid ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		return (Forschung)db.get(Forschung.class, fid);
	}

	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String name;
	private String image;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="forschung_fk_forschung1")
	private Forschung req1;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="forschung_fk_forschung2")
	private Forschung req2;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="forschung_fk_forschung3")
	private Forschung req3;
	private int time;
	@Type(type="cargo")
	@Column(nullable = false)
	private Cargo costs;
	@Lob
	@Column(nullable = false)
	private String description;
	private int race;
	@Enumerated(EnumType.ORDINAL)
	@Column(nullable = false)
	private Visibility visibility;
	@Column(nullable = false)
	private String flags;
	private int specializationCosts;

	/**
	 * Konstruktor.
	 *
	 */
	public Forschung() {
		this.flags = "";
		this.visibility = Visibility.IF_RESEARCHABLE;
	}

	/**
	 * Gibt die ID der Forschung zurueck.
	 * @return die ID
	 */
	public int getID() {
		return this.id;
	}

	/**
	 * Gibt den Namen der Forschung zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gibt alle fuer diese Forschung benoetigten Forschungen zurueck.
	 * @return Die benoetigten Forschungen
	 */
	public Set<Forschung> getBenoetigteForschungen()
	{
		Set<Forschung> result = new HashSet<>();
		if( this.req1 != null )
		{
			result.add(req1);
		}
		if( this.req2 != null )
		{
			result.add(req2);
		}
		if( this.req3 != null )
		{
			result.add(req3);
		}
		return result;
	}

	/**
	 * Gibt die fuer diese Forschung benoetigten Forschungen zurueck.
	 * @param number Die Nummer der benoetigten Forschung (1-3)
	 * @return Die Forschung oder <code>null</code>
	 */
	public Forschung getRequiredResearch( int number ) {
		switch(number) {
		case 1:
			return this.req1;
		case 2:
			return this.req2;
		case 3:
			return this.req3;
		}
		throw new RuntimeException("Ungueltiger Forschungsindex '"+number+"'");
	}

	/**
	 * Gibt die Forschungsdauer in Ticks zurueck.
	 * @return Die Forschungsdauer
	 */
	public int getTime() {
		return this.time;
	}

	/**
	 * Setzt die Forschungskosten einer Forschung.
	 * @param costs Die Forschungskosten
	 */
	public void setCosts(Cargo costs)
	{
		this.costs = costs;
	}

	/**
	 * Gibt die Forschungskosten als nicht modifizierbarer Cargo zurueck.
	 * @return Die Forschungskosten
	 */
	public Cargo getCosts() {
		return this.costs;
	}

	/**
	 * Gibt die Beschreibung der Forschung zurueck.
	 * @return Die Beschreibung
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Gibt die ID der Rasse zurueck, der die Forschung zugeordnet ist.
	 * @return Die ID der Rasse
	 */
	public int getRace() {
		return this.race;
	}

	/**
	 * Gibt zurueck, ob die Forschung die angegebene Sichtbarkeit hat.
	 * @param visibility Die Sichtbarkeit
	 * @return <code>true</code>, falls die Sichtbarkeit gegeben ist
	 */
	public boolean hasVisibility(Forschung.Visibility visibility) {
		return this.visibility == visibility;
	}

	/**
	 * Prueft, ob die Forschung fuer den Spieler sichtbar ist.
	 * Es werden nur die Werte von Forschung.Visibility beruecksichtigt.
	 * @param user Der Spieler
	 *
	 * @return <code>true</code>, falls die Forschung sichtbar ist
	 */
	public boolean isVisibile(User user) {
		if(hasVisibility(Forschung.Visibility.ALWAYS)) {
			return true;
		}

		if(hasVisibility(Forschung.Visibility.NEVER)) {
			return false;
		}

		if(user == null)
		{
			return false;
		}

		return user.hasResearched(getBenoetigteForschungen());
	}

	/**
	 * Setzte die Beschreibung der Forschung.
	 *
	 * @param description Die neue Beschreibung.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Setzt den Namen der Forschung.
	 *
	 * @param name Der neue Name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Setzt die Rasse der Forschung.
	 *
	 * @param race vergleiche <code>Rassen</code>
	 */
	public void setRace(int race) {
		this.race = race;
	}

	/**
	 * Setzt die erste benoetigte Forschung .
	 * @param req1 Die Forschung.
	 */
	public void setReq1(Forschung req1) {
		this.req1 = req1;
	}

	/**
	 * Setzt die zweite benoetigte Forschung .
	 * @param req2 Die Forschung.
	 */
	public void setReq2(Forschung req2) {
		this.req2 = req2;
	}

	/**
	 * Setzt die zweite benoetigte Forschung.
	 * @param req3 Die Forschung.
	 */
	public void setReq3(Forschung req3) {
		this.req3 = req3;
	}

	/**
	 * Setzt die Dauer in Ticks die notwendig ist um die Forschung
	 * zu erforschen.
	 * @param time Die Dauer
	 */
	public void setTime(int time) {
		this.time = time;
	}

	/**
	 * Setzt die Sichtbarkeit auf den angegebenen Wert.
	 *
	 * @param visibility Die neue Sichtbarkeitsstufe.
	 */
	public void setVisibility(Visibility visibility) {
		this.visibility = visibility;
	}

	/**
	 * Gibt die Sichtbarkeit der Forschung zurueck.
	 * @return Die Sichtbarkeitsstufe
	 */
	public Visibility getVisibility()
	{
		return visibility;
	}

	/**
	 * Prueft, ob die Forschung ein bestimmtes Flag hat.
	 * @param flag Das Flag
	 * @return <code>true</code>, falls die Forschung das Flag besitzt
	 */
	public boolean hasFlag( String flag ) {
		return this.flags.contains(flag);
	}

	/**
	 * @return Die Spezialisierungskosten dieser Forschung.
	 */
	public int getSpecializationCosts()
	{
		return this.specializationCosts;
	}

	/**
	 * @param specializationCosts Die Spezialisierungskosten dieser Forschung.
	 */
	public void setSpecializationCosts(int specializationCosts)
	{
		this.specializationCosts = specializationCosts;
	}

	/**
	 * Gibt den Pfad zur Grafik zurueck.
	 * @return Der Pfad
	 */
	public String getImage()
	{
		return image;
	}

	/**
	 * Setzt den Pfad zur Grafik.
	 * @param image Der Pfad
	 */
	public void setImage(String image)
	{
		this.image = image;
	}

	@Override
	public String toString()
	{
		return "Forschung [id="+this.id+", name="+this.name+"]";
	}
}
