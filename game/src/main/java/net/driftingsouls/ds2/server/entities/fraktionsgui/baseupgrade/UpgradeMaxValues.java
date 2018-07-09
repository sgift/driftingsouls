/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Maximalwerte der einzelnen Asteroidentypen beim Basisausbau. Wenn
 * ein Asteroidentyp keine Werte hat, kann er nicht ausgebaut werden.
 * Zu jedem Asteroidentyp kann es nur einen Datensatz geben.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="upgrade_maxvalues", uniqueConstraints = {@UniqueConstraint(name="upgrade_maxvalues_upgradetype_basetype_unique", columnNames = {"type_id", "upgradetype"})})
public class UpgradeMaxValues
{
	@Id @GeneratedValue
	private int id;

	@ManyToOne(optional = false)
	@JoinColumn(nullable = false)
	@ForeignKey(name="upgrade_max_values_fk_basetype")
	private BaseType type;
	@Column(nullable=false)
    private UpgradeType upgradetype;
    private int maximalwert;
	
	/**
	 * Konstruktor.
	 */
	protected UpgradeMaxValues() {
        // Leer
	}

	/**
	 * Konstruktor.
	 * @param type Der Basistyp fuer den der Maximalwert erfasst werden soll
	 * @param upgradeType Die Art des Upgrades/Maximalwerts
	 * @param maximalwert Der Maximalwert
	 */
	public UpgradeMaxValues(BaseType type, UpgradeType upgradeType, int maximalwert)
	{
		this.type = type;
		this.upgradetype = upgradeType;
		this.maximalwert = maximalwert;
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
	 * Gibt den Asteroidentyp zurueck.
	 * @return Der Typ
	 */
	public BaseType getType()
	{
		return type;
	}

	/**
	 * Setzt den Asteroidentyp.
	 * @param type Der Typ
	 */
	public void setType(BaseType type)
	{
		this.type = type;
	}

    /**
     * Gibt eine Instanz des UpgradeTyp zurueck.
     * @return eine Instanz davon
     */
    public UpgradeType getUpgradeType()
    {
        return upgradetype;
    }

    /**
     * Setzt die Klasse des UpgradeTyp.
     * @param upgradetype die neue Klasse
     */
    public void setUpgradeType(UpgradeType upgradetype)
    {
        this.upgradetype = upgradetype;
    }

    /**
     * Gibt den Maximalwert zurueck.
     * @return der Maximalwert
     */
    public int getMaximalwert() { return maximalwert; }

    /**
     * Setzt den Maximalwert.
     * @param maxvalue der neue Maximalwert
     */
    public void setMaximalwert(int maxvalue) { this.maximalwert = maxvalue; }
}
