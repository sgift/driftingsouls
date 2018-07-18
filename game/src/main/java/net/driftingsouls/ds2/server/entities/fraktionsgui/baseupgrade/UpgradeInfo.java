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
package net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade;

import net.driftingsouls.ds2.server.bases.BaseType;
import org.hibernate.annotations.ForeignKey;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Daten zu moeglichen Basis-Ausbauten.
 * @author Christoph Peltz
 *
 */
@Entity
@Table(name="upgrade_info")
public class UpgradeInfo implements Comparable<UpgradeInfo> {


	@Id @GeneratedValue
	private int id;
	@ManyToOne(optional = false)
	@JoinColumn(nullable = false)
	@ForeignKey(name="upgrade_info_fk_basetype")
	private BaseType type;
	private int modWert;
	@Column(nullable = false)
	private UpgradeType upgradetype;
	private int price;
	@Column(name="miningexplosive", nullable = false)
	private int miningExplosive;
	private int ore;
    private int minticks;
    private int maxticks;

	/**
	 * Konstruktor.
	 *
	 */
	protected UpgradeInfo()
	{
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param type Der Basistyp
	 * @param upgradetype Die Art des Ausbaus
	 */
	public UpgradeInfo(BaseType type, UpgradeType upgradetype)
	{
		this.type = type;
		this.upgradetype = upgradetype;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}	

	/**
	 * Setzt die ID.
	 * @param id Die neue ID
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Gibt die Klasse der Asteroiden zurueck, fuer die der Ausbau zutrifft.
	 * @return Die Klasse des Asteroiden
	 */
	public BaseType getType() {
		return type;
	}

	/**
	 * Setzt die Klasse.
	 * @param type Die neue Klasse des Asteroiden
	 */
	public void setType(BaseType type) {
		this.type = type;
	}

    /**
     * Gibt den UpgradeType zurueck.
     * @return der UpgradeType oder <code>null</code> falls nicht vorhanden
     */
    public UpgradeType getUpgradeType()
    {
        return upgradetype;
    }

    /**
     * Setzt den UpgradeType.
     * @param upgradetype der neue UpgradeType
     */
    public void setUpgradeType(UpgradeType upgradetype)
    {
        this.upgradetype = upgradetype;
    }

	/**
	 * Gibt den Zahlenwert der Modifikation zurueck.
	 * @return Zahlenwert der Modifikation
	 */
	public int getModWert() {
		return modWert;
	}

	/**
	 * Setzt den Zahlenwert der Modifikation.
	 * @param mod Zahlenwert der Modifikation
	 */
	public void setModWert(int mod) {
		this.modWert = mod;
	}

	/**
	 * Gibt den Preis zurueck.
	 * @return Der Preis
	 */
	public int getPrice() {
		return price;
	}

	/**
	 * Setzt den Preis.
	 * @param price Preis
	 */
	public void setPrice(int price) {
		this.price = price;
	}

	/**
	 * Gibt die Anzahl noetigen BergBauSprengstoffes zurueck.
	 * @return Anzahl BBS
	 */
	public int getMiningExplosive() {
		return miningExplosive;
	}

	/**
	 * Setzt die noetige Menge BBS.
	 * @param miningExplosive Anzahl BBS
	 */
	public void setMiningExplosive(int miningExplosive) {
		this.miningExplosive = miningExplosive;
	}

	/**
	 * Gibt die Anzahl noetigen Erzes zurueck.
	 * @return Anzahl Erz
	 */
	public int getOre() {
		return ore;
	}

	/**
	 * Setzt die Menge des benoetigten Erzes.
	 * @param ore Anzahl Erz
	 */
	public void setOre(int ore) {
		this.ore = ore;
	}

    /**
     * Gibt die Mindestanzahl die dieser Auftarg zum bearbeiten braucht zurueck.
     * @return die Mindestanzahl der Ticks
     */
    public int getMinTicks() { return minticks; }

    /**
     * Setzt die Mindestanzahl die dieser Auftrag zum bearbeiten braucht.
     * @param minticks die neue Mindestanzahl der Ticks
     */
    public void setMinTicks(int minticks) { this.minticks = minticks; }

    /**
     * Gibt die Maximalanzahl die dieser Auftarg zum bearbeiten braucht zurueck.
     * @return die Maximalanzahl der Ticks
     */
    public int getMaxTicks() { return maxticks; }

    /**
     * Setzt dieMaximalanzahl die dieser Auftrag zum bearbeiten braucht.
     * @param maxticks die neue Maximalanzahl der Ticks
     */
    public void setMaxTicks(int maxticks) { this.maxticks = maxticks; }

    @Override
    public boolean equals(Object other)
    {
        if(!(other instanceof UpgradeInfo)) { return false; }
        UpgradeInfo o = (UpgradeInfo)other;
        return o.getId() == this.getId();
    }

	@Override
	public int compareTo(@Nonnull UpgradeInfo o)
	{
		int diff = this.upgradetype.compareTo(o.getUpgradeType());
		if( diff != 0 )
		{
			return diff;
		}
		return this.modWert - o.modWert;
	}
}
