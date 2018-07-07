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
package net.driftingsouls.ds2.server.units;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Forschung;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Ein Einheitentyp.
 *
 */
@Entity
@Table(name="unit_types")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class UnitType {
	
	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String name;
	private int size;
	@Lob
	private String description;
	private double recost;
	private double nahrungcost;
	private int kapervalue;
	@Type(type="cargo")
	@Column(nullable = false)
	private Cargo buildcosts;
	@ManyToOne(optional = false)
	@JoinColumn(nullable = false)
	@ForeignKey(name="unitttype_fk_forschung")
	private Forschung res;
	private int dauer;
	@Column(nullable = false)
	private String picture;
	private boolean hidden;
	
	/**
	 * Konstruktor.
	 *
	 */
	public UnitType() 
	{
		this.picture = "";
	}

	/**
	 * Gibt die Beschreibung des Einheitentyps zurueck.
	 * @return Die Beschreibung
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * Gibt die ID des Einheitentyps zurueck.
	 * @return Die ID
	 */
	public int getId() 
	{
		return id;
	}

	/**
	 * Gibt den Namen des Einheitentyps zurueck.
	 * @return Der Name
	 */
	public String getName() 
	{
		return name;
	}
	
	/**
	 * Gibt die RE-Kosten des Einheitentyps zurueck.
	 * @return Die RE-Kosten
	 */
	public double getReCost() 
	{
		return recost;
	}

	/**
	 * Gibt die Nahrungskosten des Einheitentyps zurueck.
	 * @return Die Nahrungskosten
	 */
	public double getNahrungCost() 
	{
		return nahrungcost;
	}
	
	/**
	 * Gibt die Groesze des Einheitentyps zurueck.
	 * @return Die Groesze
	 */
	public int getSize() 
	{
		return size;
	}

	/**
	 * Gibt den Kaper-Wert des Einheitentyps zurueck.
	 * @return Der Kaper-Wert
	 */
	public int getKaperValue() 
	{
		return kapervalue;
	}
	
	/**
	 * Gibt die Kosten dieser Einheit zurueck.
	 * @return Die Kosten
	 */
	public Cargo getBuildCosts() 
	{
		return buildcosts;
	}
	
	/**
	 * Gibt die Forschung zu dieser Einheit zurueck.
	 * @return Die Forschung
	 */
	public Forschung getRes()
	{
		return res;
	}
	
	/**
	 * Gibt die Ausbildungsdauer dieser Einheit zurueck.
	 * @return Die Dauer
	 */
	public int getDauer() 
	{
		return dauer;
	}
	
	/**
	 * Gibt das Bild des Einheitentyps zurueck.
	 * @return Das Bild
	 */
	public String getPicture() 
	{
		return picture;
	}
	
	/**
	 * Gibt an, ob der Einheitentyp standardmaessig sichtbar ist.
	 * @return <code>true</code>, falls die Einheit unsichtbar sein soll, sonst <code>false</code>
	 */
	public boolean isHidden()
	{
		return hidden;
	}
	
	/**
	 * Setzt die Beschreibung des Einheitentyps.
	 * @param description Die Beschreibung
	 */
	public void setDescription(String description) 
	{
		this.description = description;
	}

	/**
	 * Setzt den Namen des Einheitentyps.
	 * @param nickname Der Name
	 */
	public void setName(String nickname) 
	{
		this.name = nickname;
	}

	/**
	 * Setzt die RE-Kosten.
	 * @param reCost Die Kosten
	 */
	public void setReCost(double reCost) 
	{
		this.recost = reCost;
	}

	/**
	 * Setzt die Nahrungskosten des Einheitentyps.
	 * @param nahrungcost Die Nahrungskosten
	 */
	public void setNahrungCost(double nahrungcost) 
	{
		this.nahrungcost = nahrungcost;
	}
	/**
	 * Setzt die Groesse des Einheitentyps.
	 * @param size Die Groesse
	 */
	public void setSize(int size) 
	{
		this.size = size;
	}

	/**
	 * Setzt den Kaper-Wert des Einheitentyps.
	 * @param kapervalue Der Kaper-Wert
	 */
	public void setKaperValue(int kapervalue) 
	{
		this.kapervalue = kapervalue;
	}
	
	/**
	 * Setzt die Baukosten dieser Einheit auf den uebergebenen Cargo.
	 * @param buildcosts Die neuen Baukosten
	 */
	public void setBuildCosts(Cargo buildcosts) 
	{
		this.buildcosts = buildcosts;
	}
	
	/**
	 * Setzt die benoetigte Forschung.
	 * @param resid Die Forschung
	 */
	public void setRes(Forschung resid)
	{
		this.res = resid;
	}
	
	/**
	 * Setzt die Ausbildungsdauer dieses Einheitentyps.
	 * @param dauer Die Dauer
	 */
	public void setDauer(int dauer) 
	{
		this.dauer = dauer;
	}
	
	/**
	 * Setzt das Bild des Einheitentyps.
	 * @param picture Das Bild
	 */
	public void setPicture(String picture) 
	{
		this.picture = picture;
	}
	
	/**
	 * Setzt die Sichtbarkeit des Einheitentyps.
	 * @param hidden Die Sichtbarkeit
	 */
	public void setHidden(boolean hidden)
	{
		this.hidden = hidden;
	}
}