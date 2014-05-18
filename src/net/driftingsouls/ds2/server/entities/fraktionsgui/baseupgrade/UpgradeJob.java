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

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.ships.Ship;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * Ein Auftrag zum Basisausbau.
 *
 * @author Christoph Peltz
 */
@Entity
@Table(name = "upgrade_job")
public class UpgradeJob
{
	@Id
	@GeneratedValue
	private int id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "baseid", nullable = false)
	@ForeignKey(name="upgrade_job_fk_base")
	private Base base;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "userid", nullable = false)
	@ForeignKey(name="upgrade_job_fk_user")
	private User user;

	@ManyToMany(fetch = FetchType.LAZY)
	@ForeignKey(name="upgrade_job_fk_mod", inverseName = "upgrade_info_fk_upgrade_job")
	private Set<UpgradeInfo> upgradelist;

	private boolean bar;
	private boolean payed;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "colonizerid", nullable = true)
	@ForeignKey(name="upgrade_job_fk_ships")
	private Ship colonizer;
	private int endTick;

	/**
	 * Konstruktor.
	 */
	public UpgradeJob()
	{
		// EMPTY
	}

	/**
	 * Erstellt einen neuen Ausbau Auftrag.
	 *
	 * @param base Die Basis die ausgebaut werden soll
	 * @param user Der Benutzer, der die Erweiterung durchfuehren laesst
	 * @param bar Wird bar bezahlt?
	 * @param colonizer Der Colonizer, der dafuer verwendet werden soll
	 */
	public UpgradeJob(Base base, User user, boolean bar, Ship colonizer)
	{
		this.base = base;
		this.user = user;
        this.upgradelist = new HashSet<>();
		this.bar = bar;
		this.payed = false;
		this.colonizer = colonizer;
		this.endTick = 0;
	}

	/**
	 * Gibt die Basis zurueck.
	 *
	 * @return Die Basis
	 */
	public Base getBase()
	{
		return base;
	}

	/**
	 * Setzt die Basis, die ausgebaut werden soll.
	 *
	 * @param base Die Basis
	 */
	public void setBase(Base base)
	{
		this.base = base;
	}

	/**
	 * Gibt den User zurueck, der den Auftrag erstellt hat.
	 *
	 * @return Der Auftraggeber
	 */
	public User getUser()
	{
		return user;
	}

	/**
	 * Setzt die User, der den Auftrag erteilt hat.
	 *
	 * @param user Der neue User
	 */
	public void setUser(User user)
	{
		this.user = user;
	}

	/**
	 * Gibt die Auftrags-ID zurueck.
	 *
	 * @return Auftrags-ID
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * Setzt die Auftrags-ID.
	 *
	 * @param id Die neue ID des Auftrages
	 */
	public void setId(int id)
	{
		this.id = id;
	}

	/**
	 * Gibt die Informationen ueber die Modifikationen zurueck.
	 *
	 * @return Infos über den Ausbau
	 */
	public Set<UpgradeInfo> getUpgrades()
	{
		return upgradelist;
	}

	/**
	 * Fuegr eine Modifikation hinzu.
	 *
	 * @param upgrade Modifikation, die hinzugefuegt werden soll.
	 */
	public void addUpgrade(UpgradeInfo upgrade)
	{
	    if(upgradelist.contains(upgrade)) { return; }
        for(UpgradeInfo aupgrade : upgradelist)
        {
            if(upgrade.getUpgradeType().equals(aupgrade.getUpgradeType()))
            {
                return;
            }
        }
        upgradelist.add(upgrade);
	}

    /**
     * Entfernt eine Modifikation.
     * @param upgrade Modifikation, die entfernt werden soll.
     */
    public void removeupgrade(UpgradeInfo upgrade)
    {
        if(upgradelist.contains(upgrade)) { upgradelist.remove(upgrade); }
    }

	/**
	 * Gibt zurueck ob der Kaeufer bar bezahlen will oder nicht.
	 *
	 * @return Die Zahlungsart
	 */
	public boolean getBar()
	{
		return bar;
	}

	/**
	 * Setzt die Zahlungsmethode.
	 *
	 * @param bar Switch fuer Barzahlung
	 */
	public void setBar(boolean bar)
	{
		this.bar = bar;
	}

	/**
	 * Gibt zurueck, ob bereits bezahlt wurde (nur wenn bar == true relevant).
	 *
	 * @return Zahlungsstatus
	 */
	public boolean getPayed()
	{
		return payed;
	}

	/**
	 * Setzt den Zahlungsstatus.
	 *
	 * @param payed Der neue Zahlungsstatus
	 */
	public void setPayed(boolean payed)
	{
		this.payed = payed;
	}

	/**
	 * Gibt den fuer den Ausbau zu benutzenden Colonizer zurueck.
	 *
	 * @return Der Colonizer
	 */
	public Ship getColonizer()
	{
		return colonizer;
	}

	/**
	 * Setzt den fuer den Ausbau zu benutzenden Colonizer.
	 *
	 * @param colonizer Der Colonizer
	 */
	public void setColonizer(Ship colonizer)
	{
		this.colonizer = colonizer;
	}

	/**
	 * Gibt zurueck, wann der Ausbau enden wird.
	 *
	 * @return Tick in dem der Ausbau enden wird oder 0
	 */
	public int getEnd()
	{
		return endTick;
	}

	/**
	 * Setzt den Tick in dem der Ausbau enden soll.
	 *
	 * @param end Tick der neue Tick zu dem der Ausbau enden soll
	 */
	public void setEnd(int end)
	{
		this.endTick = end;
	}

    /**
     * Gibt die RE-Kosten dieses Auftrages zurueck.
     * @return die RE-Kosten
     */
    public int getPrice()
    {
        int price = 0;
        for(UpgradeInfo upgrade : upgradelist)
        {
            price += upgrade.getPrice();
        }
        ConfigValue value = new ConfigService().get(WellKnownConfigValue.DI_FAKTOR_RABATT);
        double factor = Double.valueOf(value.getValue());
        price = (int)(Math.pow(factor,upgradelist.size()-1) * price);
        return price;
    }

    /**
     * Gibt die BBS-Kosten dieses Auftrages zurueck.
     * @return die BBS-Kosten
     */
    public int getMiningExplosive()
    {
        int miningexplosive = 0;
        for(UpgradeInfo upgrade : upgradelist)
        {
            miningexplosive += upgrade.getMiningExplosive();
        }
        return miningexplosive;
    }

    /**
     * Gibt die Erz-Kosten dieses Auftrages zurueck.
     * @return die Erz-Kosten
     */
    public int getOre()
    {
        int ore = 0;
        for(UpgradeInfo upgrade : upgradelist)
        {
            ore += upgrade.getOre();
        }
        return ore;
    }

    /**
     * Gibt die minimale Anzahl an Ticks zurueck, die dieser Ausbau benoetigt.
     * @return die minimale Anzahl an Ticks
     */
    public int getMinTicks()
    {
        int ticks = 0;
        for(UpgradeInfo upgrade : upgradelist)
        {
            ticks += upgrade.getMinTicks();
        }
        ConfigValue value = new ConfigService().get(WellKnownConfigValue.DI_FAKTOR_ZEIT);
        double factor = Double.valueOf(value.getValue());
        ticks = (int)(Math.pow(factor,upgradelist.size()-1) * ticks);
        return ticks;
    }

    /**
     * Gibt die maximale Anzahl an Ticks zuruekc, die dieser Ausbau benoetigt.
     * @return die maximale Anzahl an Ticks
     */
    public int getMaxTicks()
    {
        int ticks = 0;
        for (UpgradeInfo upgrade : upgradelist) {
            ticks += upgrade.getMaxTicks();
        }
        ConfigValue value = new ConfigService().get(WellKnownConfigValue.DI_FAKTOR_ZEIT);
        double factor = Double.valueOf(value.getValue());
        ticks = (int)(Math.pow(factor,upgradelist.size()-1) * ticks);
        return ticks;
    }
}
