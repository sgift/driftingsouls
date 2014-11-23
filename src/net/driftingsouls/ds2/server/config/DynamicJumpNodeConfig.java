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
package net.driftingsouls.ds2.server.config;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.DynamicJumpNode;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.commons.lang.math.RandomUtils;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Repraesentiert eine mögliche Konfiguration eines dynamischen JumpNodes.
 */
@Entity
@Table(name="dynamic_jn_config")
public class DynamicJumpNodeConfig
{
    @Id
    @GeneratedValue
    private int id;
    @ManyToMany
    @ForeignKey(name="dynamic_jn_config_fk_startsystems", inverseName="dynamic_jn_config_startsystems_fk_systems")
    @JoinTable(name="dynamic_jn_config_startsystems")
    private Set<StarSystem> startsystems = new HashSet<>();
    @ManyToMany
    @ForeignKey(name="dynamic_jn_config_fk_zielsystems", inverseName="dynamic_jn_config_zielsystems_fk_systems")
    @JoinTable(name="dynamic_jn_config_zielsystems")
    private Set<StarSystem> zielsystems = new HashSet<>();
    private int inrange;
    private int outrange;
    private int mindauer;
    private int maxdauer;
    private int minnextmovement;
    private int maxnextmovement;

    /**
     * Konstruktor.
     */
    public DynamicJumpNodeConfig() {
        // EMPTY
    }

    /**
     * Konstruktor.
     * @param startsystems Liste der möglihen Startsysteme
     * @param zielsystems Liste der möglichen Zielsysteme
     * @param inrange Maximale Reichweite des Eingangs
     * @param outrange Maximale Reichweite des Ausgangs
     * @param mindauer Mindestdauer, die der JumpNode geöffnet ist
     * @param maxdauer Maximaldauer, die der JumpNode geöffnet ist
     */
    public DynamicJumpNodeConfig(Set<StarSystem> startsystems, Set<StarSystem> zielsystems, int inrange, int outrange, int mindauer, int maxdauer, int minnextmovement, int maxnextmovement)
    {
        this.startsystems = startsystems;
        this.zielsystems = zielsystems;
        this.inrange = inrange;
        this.outrange = outrange;
        this.mindauer = mindauer;
        this.maxdauer = maxdauer;
        this.minnextmovement = minnextmovement;
        this.maxnextmovement = maxnextmovement;
    }

    /**
     * Gibt die ID zurueck.
     * @return die ID
     */
    public int getId()
    {
        return id;
    }

    /**
     * Gibt die Liste der Startsysteme zurück.
     * @return die Liste der Startsysteme
     */
    public Set<StarSystem> getStartSystems()
    {
        return startsystems;
    }

    /**
     * Setzt die Liste der Startsysteme
     * @param startsystems die neue Liste der Startsysteme
     */
    public void setStartSystems(Set<StarSystem> startsystems)
    {
        this.startsystems = startsystems;
    }

    /**
     * Gibt die Liste der Zielsysteme zurück.
     * @return die Liste der Zielsysteme
     */
    public Set<StarSystem> getZielSystems()
    {
        return zielsystems;
    }

    /**
     * Setzt die Liste der Zielsysteme.
     * @param zielsystems die neue Liste der Zielsysteme
     */
    public void setZielSystems(Set<StarSystem> zielsystems)
    {
        this.zielsystems = zielsystems;
    }

    /**
     * Gibt zurück, wie weit der Eingang des JumpNodes maximal pro Tick wandert
     * @return die Maximale Reichweite des Eingangs
     */
    public int getInRange()
    {
        return inrange >= 0 ? inrange : 0;
    }

    /**
     * Setzt, wie weit der Eingang des JumpNodes maximal pro Tick wandert.
     * @param inrange die neue Maximale Reichweite des Eingangs
     */
    public void setInRange(int inrange)
    {
        this.inrange = inrange;
    }

    /**
     * Gibt die maximale Reichweite des Ausgangsportals zurueck.
     * @return Die maximale Reichweite des Ausgangsportals
     */
    public int getOutRange()
    {
        return outrange >= 0 ? outrange : 0;
    }

    /**
     * Setzt die maximale Reichweite des Ausgangsportals.
     * @param outrange Die neue maximale Reichweite des Ausgangsportals
     */
    public void setOutRange(int outrange)
    {
        this.outrange = outrange;
    }

    /**
     * Gibt die minimale Dauer des JN zurueck.
     * @return Die minimale Dauer des JN
     */
    public int getMinDauer()
    {
        return mindauer >= 1 ? mindauer : 1;
    }

    /**
     * Setzt die minimale Dauer des JN.
     * @param mindauer Die neue minimale Dauer des JN
     */
    public void setMinDauer(int mindauer)
    {
        this.mindauer = mindauer;
    }

    /**
     * Gibt die maximale Dauer des JN zurueck.
     * @return Die maximale Dauer des JN
     */
    public int getMaxDauer()
    {
        return maxdauer >= getMinDauer() ? maxdauer : getMinDauer();
    }

    /**
     * Setzt die maximale Dauer des JN.
     * @param maxdauer Die neue maximale Dauer des JN
     */
    public void setMaxDauer(int maxdauer)
    {
        this.maxdauer = maxdauer;
    }

    /**
     * Gibt die minimale Zeit bis zur naechsten Bewegung zurueck.
     * @return Die minimale Zeit bis zur naechsten Bewegung
     */
    public int getMinNextMovement()
    {
        return minnextmovement >= 1 ? minnextmovement : 1;
    }

    /**
     * Setzt die minimale Zeit bis zur naechsten Bewegung.
     * @param minnextmovement Die neue minimale Zeit bis zur naechsten Bewegung
     */
    public void setMinNextMovement(int minnextmovement) { this.minnextmovement = minnextmovement; }

    /**
     * Gibt die maximale Zeit bis zur naechsten Bewegung zurueck.
     * @return Die maximale Zeit bis zur naechsten Bewegung
     */
    public int getMaxNextMovement()
    {
        return maxnextmovement >= getMinNextMovement() ? maxnextmovement : getMinNextMovement();
    }

    /**
     * Setzt die maximale Zeit bis zur naechsten Bewegung.
     * @param maxnextmovement Die neue maximale Zeit bis zur naechsten Bewegung
     */
    public void setMaxNextMovement(int maxnextmovement) { this.maxnextmovement = maxnextmovement; }

    /**
     * Spawnt einen dynamischen Sprungpunkt von dieser Konfiguration.
     */
    public void spawnJumpNode()
    {
        if(startsystems == null || startsystems.isEmpty())
        {
            return;
        }
        if(zielsystems == null || zielsystems.isEmpty())
        {
            return;
        }
        org.hibernate.Session db = ContextMap.getContext().getDB();
        int startrnd = RandomUtils.nextInt(startsystems.size());
        StarSystem startsystem = (StarSystem)startsystems.toArray()[startrnd];
        int zielrnd = RandomUtils.nextInt(zielsystems.size());
        StarSystem zielsystem = (StarSystem)zielsystems.toArray()[zielrnd];

        int startx = RandomUtils.nextInt(startsystem.getWidth())+1;
        int starty = RandomUtils.nextInt(startsystem.getHeight())+1;
        Location startloc = new Location(startsystem.getID(), startx, starty);

        int zielx = RandomUtils.nextInt(zielsystem.getWidth())+1;
        int ziely = RandomUtils.nextInt(zielsystem.getHeight())+1;
        Location zielloc = new Location(zielsystem.getID(), zielx, ziely);

        int dauer = RandomUtils.nextInt(getMaxDauer()-getMinDauer()+1)+getMinDauer();
        int move = RandomUtils.nextInt(getMaxNextMovement()-getMinNextMovement()+1)+getMinNextMovement();

        DynamicJumpNode jump = new DynamicJumpNode(startloc, zielloc, startsystem.getName()+"->"+zielsystem.getName(), dauer, getInRange(), getOutRange(), move);
        db.persist(jump.getJumpNode());
        db.persist(jump);
    }
}
