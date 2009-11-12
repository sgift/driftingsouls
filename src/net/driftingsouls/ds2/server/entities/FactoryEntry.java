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

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.UnmodifiableCargo;
import net.driftingsouls.ds2.server.framework.Common;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

/**
 * Ein Eintrag der Fabriken.
 *
 */
@Entity
@Table(name="items_build")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FactoryEntry {
	
	@Id
	private int id;
	private String name;
	private String description;
	private int res1;
	private int res2;
	private int res3;
	@Column(name="itemid")
	private int itemId;
	private BigDecimal dauer;
	@Column(name="buildcosts")
	@Type(type="cargo")
	private Cargo buildCosts;
	private String buildingid;
	private String picture;
	
	/**
	 * Konstruktor.
	 *
	 */
	public FactoryEntry() {
		// EMPTY
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
	 * Gibt die Beschreibung zurueck.
	 * @return Die Beschreibung
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt die Item-ID den zugehoerigen Ammo-Items zurueck.
	 * @return Die Item-ID
	 */
	public int getItemId() {
		return itemId;
	}

	/**
	 * Gibt den Bildpfad zurueck.
	 * @return Der Bildpfad
	 */
	public String getPicture()
	{
		return this.picture;
	}
	
	/**
	 * Gibt die Gebaeude zurueck in denen dieser Eintrag gebaut werden darf.
	 * @return Die Gebaeudeids
	 */
	public String getBuildingIds()
	{
		return buildingid;
	}
	
	/**
	 * Prueft, ob dieser Eintrag in dem Gebaeude gebaut werden darf.
	 * @param buildingid Die Gebaeudeid
	 * @return <code>true</code>, falls dieser Eintrag gebaut werden darf
	 */
	public boolean hasBuildingId(int buildingid)
	{
		String buildingids = getBuildingIds();
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
	public int getRes1() {
		return res1;
	}

	/**
	 * Gibt die zweite benoetigte Forschung zurueck.
	 * @return Die zweite benoetigte Forschung
	 */
	public int getRes2() {
		return res2;
	}

	/**
	 * Gibt die dritte benoetigte Forschung zurueck.
	 * @return Die erste benoetigte Forschung
	 */
	public int getRes3() {
		return res3;
	}
	
	/**
	 * Gibt die benoetigte Forschung zurueck.
	 * @param i Die Nummer der Forschung (1-3)
	 * @return Die Forschung
	 * @see #getRes1()
	 * @see #getRes2()
	 * @see #getRes3()
	 */
	public int getRes(int i) {
		switch(i) {
		case 1:
			return getRes1();
		case 2:
			return getRes2();
		case 3:
			return getRes3();
		default:
			throw new RuntimeException("Ungueltiger Forschungsindex '"+i+"'");
		}
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
	 * Setzt die Beschreibung.
	 * @param description Die Beschreibung
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Setzt den Namen der Munition.
	 * @param name Der Name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Setzt das Bild fuer diesen Eintrag.
	 * @param picture Der Bildpfad
	 */
	public void setPicture(String picture)
	{
		this.picture = picture;
	}
	
	/**
	 * Setzt die Gebaeudeids, wo dieser Eintrag gebaut werden darf.
	 * @param buildingids die Ids
	 */
	public void setBuildingIds(String buildingids)
	{
		this.buildingid = buildingids;
	}
	
	/**
	 * Setzt die erste benoetigte Forschung.
	 * @param res1 Die Forschung
	 */
	public void setRes1(int res1) {
		this.res1 = res1;
	}

	/**
	 * Setzt die zweite benoetigte Forschung.
	 * @param res2 Die Forschung
	 */
	public void setRes2(int res2) {
		this.res2 = res2;
	}

	/**
	 * Setzt die dritte benoetigte Forschung.
	 * @param res3 Die Forschung
	 */
	public void setRes3(int res3) {
		this.res3 = res3;
	}
}
