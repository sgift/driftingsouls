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

import javax.persistence.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
    @AttributeOverrides( {
            @AttributeOverride(name="x", column = @Column(name="initialStartX") ),
            @AttributeOverride(name="y", column = @Column(name="initialStartY") ),
            @AttributeOverride(name="system", column = @Column(name="initialStartSystem"))
    })
    private Location initialStart = null;
    @AttributeOverrides( {
            @AttributeOverride(name="x", column = @Column(name="initialTargetX") ),
            @AttributeOverride(name="y", column = @Column(name="initialTargetY") ),
            @AttributeOverride(name="system", column = @Column(name="initialTargetSystem"))
    })
    private Location initialTarget = null;
    private int maxDistanceToInitialStart;
    private int maxDistanceToInitialTarget;
    private int minLifetime;
    private int maxLifetime;
    private int minNextMovementDelay;
    private int maxNextMovementDelay;

    /**
     * Konstruktor.
     */
    public DynamicJumpNodeConfig() {
        // EMPTY
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
    public Location getInitialStart()
    {
        return initialStart;
    }

    public void setInitialStart(Location initialStart)
    {
        this.initialStart = initialStart;
    }

    public Location getInitialTarget()
    {
        return initialTarget;
    }

    public void setInitialTarget(Location initialTarget)
    {
        this.initialTarget = initialTarget;
    }

    public int getMaxDistanceToInitialStart()
    {
        return maxDistanceToInitialStart;
    }

    /**
     * Setzt, wie weit der Eingang des JumpNodes maximal pro Tick wandert.
     * @param maxDistanceToInitialStart die neue Maximale Reichweite des Eingangs
     */
    public void setMaxDistanceToInitialStart(int maxDistanceToInitialStart)
    {
        this.maxDistanceToInitialStart = maxDistanceToInitialStart;
    }

    /**
     * Gibt die maximale Reichweite des Ausgangsportals zurueck.
     * @return Die maximale Reichweite des Ausgangsportals
     */
    public int getMaxDistanceToInitialTarget()
    {
        return maxDistanceToInitialTarget;
    }

    /**
     * Setzt die maximale Reichweite des Ausgangsportals.
     * @param maxDistanceToInitialTarget Die neue maximale Reichweite des Ausgangsportals
     */
    public void getMaxDistanceToInitialTarget(int maxDistanceToInitialTarget)
    {
        this.maxDistanceToInitialTarget = maxDistanceToInitialTarget;
    }

    /**
     * Gibt die minimale Dauer des JN zurueck.
     * @return Die minimale Dauer des JN
     */
    public int getMinLifetime()
    {
        return minLifetime;
    }

    /**
     * Setzt die minimale Dauer des JN.
     * @param minLifetime Die neue minimale Dauer des JN
     */
    public void setMinLifetime(int minLifetime)
    {
        this.minLifetime = minLifetime;
    }

    /**
     * Gibt die maximale Dauer des JN zurueck.
     * @return Die maximale Dauer des JN
     */
    public int getMaxLifetime()
    {
        return maxLifetime;
    }

    /**
     * Setzt die maximale Dauer des JN.
     * @param maxLifetime Die neue maximale Dauer des JN
     */
    public void setMaxLifetime(int maxLifetime)
    {
        this.maxLifetime = maxLifetime;
    }

    /**
     * Gibt die minimale Zeit bis zur naechsten Bewegung zurueck.
     * @return Die minimale Zeit bis zur naechsten Bewegung
     */
    public int getMinNextMovementDelay()
    {
        return minNextMovementDelay;
    }

    /**
     * Setzt die minimale Zeit bis zur naechsten Bewegung.
     * @param minNextMovementDelay Die neue minimale Zeit bis zur naechsten Bewegung
     */
    public void setMinNextMovementDelay(int minNextMovementDelay) { this.minNextMovementDelay = minNextMovementDelay; }

    /**
     * Gibt die maximale Zeit bis zur naechsten Bewegung zurueck.
     * @return Die maximale Zeit bis zur naechsten Bewegung
     */
    public int getMaxNextMovementDelay()
    {
        return maxNextMovementDelay;
    }

    /**
     * Setzt die maximale Zeit bis zur naechsten Bewegung.
     * @param maxnextmovement Die neue maximale Zeit bis zur naechsten Bewegung
     */
    public void setMaxNextMovement(int maxnextmovement) { this.maxNextMovementDelay = maxnextmovement; }

    public String getName() {
        org.hibernate.Session db = ContextMap.getContext().getDB();
        StarSystem startSystem = (StarSystem)db.get(StarSystem.class, initialStart.getSystem());
        StarSystem targetSystem = (StarSystem)db.get(StarSystem.class, initialTarget.getSystem());
        return startSystem.getName()+"->"+targetSystem.getName();
    }

    /**
     * Spawnt einen dynamischen Sprungpunkt von dieser Konfiguration.
     */
    public void spawnJumpNode()
    {
        Location defaultLocation = new Location(0, 0, 0);
        if(initialStart == null || initialStart.equals(defaultLocation))
        {
            throw new IllegalArgumentException("Start position not set");
        }
        if(initialTarget == null || initialTarget.equals(defaultLocation))
        {
            throw new IllegalArgumentException("Target position not set");
        }

        if(minLifetime > maxLifetime) {
            throw new IllegalArgumentException("minLifetime > maxLifetime");
        }

        if(minNextMovementDelay > maxNextMovementDelay) {
            throw new IllegalArgumentException("minNextMovementDelay > maxNextMovementDelay");
        }

        int timeUntilDeath = -1;
        if(getMinLifetime() > 0 && getMaxLifetime() > 0) {
            timeUntilDeath = ThreadLocalRandom.current().nextInt(getMinLifetime(), getMaxLifetime());
        } else if(getMinLifetime() > 0) {
            timeUntilDeath = ThreadLocalRandom.current().nextInt(getMinLifetime(), Integer.MAX_VALUE);
        } else if(getMaxLifetime() > 0) {
            timeUntilDeath = ThreadLocalRandom.current().nextInt(1, getMaxLifetime());
        }

        org.hibernate.Session db = ContextMap.getContext().getDB();

        int movementDelay = 0;
        if(getMinNextMovementDelay() > 0 && getMaxNextMovementDelay() > 0) {
            movementDelay = ThreadLocalRandom.current().nextInt(getMinNextMovementDelay(), getMaxNextMovementDelay());
        } else if(getMinNextMovementDelay() > 0) {
            movementDelay = ThreadLocalRandom.current().nextInt(getMinNextMovementDelay(), Integer.MAX_VALUE);
        } else if(getMaxNextMovementDelay() > 0) {
            movementDelay = ThreadLocalRandom.current().nextInt(getMaxNextMovementDelay());
        }

        DynamicJumpNode jump = new DynamicJumpNode(this, timeUntilDeath, movementDelay);
        db.persist(jump.getJumpNode());
        db.persist(jump);
    }

    public void removeJumpNode() {
        org.hibernate.Session db = ContextMap.getContext().getDB();
        @SuppressWarnings("unchecked")
        List<DynamicJumpNode> dynamicJumpNodes = db.createQuery("from DynamicJumpNode j where config = :config")
                .setParameter("config", this)
                .list();

        if(dynamicJumpNodes.isEmpty()) {
            return;
        }

        DynamicJumpNode dynamicJumpNode = dynamicJumpNodes.get(0);

        db.delete(dynamicJumpNode.getJumpNode());
        db.delete(dynamicJumpNode);
    }
}
