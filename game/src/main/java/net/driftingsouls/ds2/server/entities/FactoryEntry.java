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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.UnmodifiableCargo;
import net.driftingsouls.ds2.server.framework.Common;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Ein Eintrag der Fabriken.
 *
 */
@Entity
@Table(name="items_build")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FactoryEntry {
	
	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String name;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="factoryentry_fk_forschung1")
	private Forschung res1;
	@ManyToOne
	@ForeignKey(name="factoryentry_fk_forschung2")
	private Forschung res2;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="factoryentry_fk_forschung3")
	private Forschung res3;
	@Column(nullable = false, precision = 19, scale = 5)
	private BigDecimal dauer;
	@Column(name="buildcosts", nullable = false)
	@Type(type="cargo")
	private Cargo buildCosts;
	@Column(nullable = false)
	private String buildingid;
	@Type(type="cargo")
	@Column(nullable = false)
	private Cargo produce;
	
	/**
	 * Konstruktor.
	 *
	 */
	public FactoryEntry() {
		this.buildCosts = new Cargo();
		this.produce = new Cargo();
		this.name = "";
		this.dauer = BigDecimal.ONE;
		this.buildingid = "";
	}

	/**
	 * Gibt die Baukosten zurueck.
	 * @return Die Baukosten
	 */
	public Cargo getBuildCosts() {
		return new UnmodifiableCargo(buildCosts);
	}

	/**
	 * Gibt die Waffenfabrikauslastung zurueck.
	 * @return Die Auslastung der Waffenfabrik
	 */
	public BigDecimal getDauer() {
		return dauer;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt den Cargo zurueck den der Eintrag ausspucken soll.
	 * @return Der Cargo
	 */
	public Cargo getProduce()
	{
		return (Cargo)produce.clone();
	}
	
	/**
	 * Setzt den Cargo, den der Eintrag ausspucken soll.
	 * @param cargo Der Cargo
	 */
	public void setProduce(Cargo cargo)
	{
		this.produce = cargo;
	}

	/**
	 * Prueft, ob dieser Eintrag in dem Gebaeude gebaut werden darf.
	 * @param buildingid Die Gebaeudeid
	 * @return <code>true</code>, falls dieser Eintrag gebaut werden darf
	 */
	public boolean hasBuildingId(int buildingid)
	{
		String buildingids = this.buildingid;
		if(buildingids == null || buildingids.equals(""))
		{
			return false;
		}
		int[] ids = Common.explodeToInt(";", buildingids);
		for(int id : ids)
		{
			if(id == buildingid)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Gibt den Namen der Ammo zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gibt die erste benoetigte Forschung zurueck.
	 * @return Die erste benoetigte Forschung
	 */
	public Forschung getRes1() {
		return res1;
	}

	/**
	 * Gibt die zweite benoetigte Forschung zurueck.
	 * @return Die zweite benoetigte Forschung
	 */
	public Forschung getRes2() {
		return res2;
	}

	/**
	 * Gibt die dritte benoetigte Forschung zurueck.
	 * @return Die erste benoetigte Forschung
	 */
	public Forschung getRes3() {
		return res3;
	}

	/**
	 * Gibt alle benoetigten Forschungen zurueck.
	 * @return Die benoetigten Forschungen
	 */
	public Set<Forschung> getBenoetigteForschungen()
	{
		Set<Forschung> result = new HashSet<>();
		if( this.res1 != null )
		{
			result.add(this.res1);
		}
		if( this.res2 != null )
		{
			result.add(this.res2);
		}
		if( this.res3 != null )
		{
			result.add(this.res3);
		}

		return result;
	}

	/**
	 * Gibt alle Building-IDs als String zurueck, wo dieser Eintrag gebaut werden darf.
	 * @return Die IDs
	 */
	public String getBuildingIdString()
	{
		return buildingid;
	}

	/**
	 * Setzt die Baukosten pro Einheit.
	 * @param buildCosts Die Kosten
	 */
	public void setBuildCosts(Cargo buildCosts) {
		this.buildCosts = buildCosts;
	}

	/**
	 * Setzt die beim Bau belegte Produktionskapazitaet.
	 * @param dauer Die notwendige Kapazitaet
	 */
	public void setDauer(BigDecimal dauer) {
		this.dauer = dauer;
	}

	/**
	 * Setzt den Namen der Munition.
	 * @param name Der Name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Setzt die Gebaeudeids als String, wo dieser Eintrag gebaut werden darf.
	 * @param buildingids die Ids
	 */
	public void setBuildingIdString(String buildingids)
	{
		this.buildingid = buildingids;
	}
	
	/**
	 * Setzt die erste benoetigte Forschung.
	 * @param res1 Die Forschung
	 */
	public void setRes1(Forschung res1) {
		this.res1 = res1;
	}

	/**
	 * Setzt die zweite benoetigte Forschung.
	 * @param res2 Die Forschung
	 */
	public void setRes2(Forschung res2) {
		this.res2 = res2;
	}

	/**
	 * Setzt die dritte benoetigte Forschung.
	 * @param res3 Die Forschung
	 */
	public void setRes3(Forschung res3) {
		this.res3 = res3;
	}
}
