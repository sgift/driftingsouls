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


import net.driftingsouls.ds2.server.config.DynamicJumpNodeConfig;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Repraesentiert einen aktiven Dynamischen Sprungpunkt.
 */
@Entity
@Table(name="dynamic_jumpnode")
public class DynamicJumpNode {
    @Id
    @GeneratedValue
    private int id;
    @ManyToOne
    @ForeignKey(name = "dynamic_jn_fk_jumpnodes")
    private JumpNode jumpnode;
    private int remainingLiveTime;
    private int remainingTicksUntilMove;
    @OneToOne
    @ForeignKey(name = "dynamic_jn_fk_dynamicjnconfig")
    private DynamicJumpNodeConfig config;
    private int initialTicksUntilMove;

    /**
     * Konstruktor.
     */
    public DynamicJumpNode() {
        // EMPTY
    }

    /**
     * Erstellt einen neuen aktiven Dynamischen Sprungpunkt.
     *
     * @param config                     Die zugehoerige Konfiguration
     * @param remainingLiveTime          Die Dauer die der Sprungpunkt existieren soll
     * @param initialTicksUntilMove      Die Abstaende in denen sich der JN bewegt
     */
    public DynamicJumpNode(DynamicJumpNodeConfig config, int remainingLiveTime, int initialTicksUntilMove) {
        this.config = config;
        this.jumpnode = new JumpNode(this.config.getInitialStart(), this.config.getInitialTarget(), this.config.getName());
        this.jumpnode.setHidden(true);
        this.remainingLiveTime = remainingLiveTime;
        this.remainingTicksUntilMove = initialTicksUntilMove;
        this.initialTicksUntilMove = initialTicksUntilMove;
    }

    /**
     * Gibt die ID der Jumpnode zurueck.
     *
     * @return Die ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gibt den tatsächlichen JumpNode dieses dynamischen Jumpnodes zurück.
     *
     * @return er tatsächliche JumpNode
     */
    public JumpNode getJumpNode() {
        return jumpnode;
    }

    /**
     * Gibt die Anzahl an Ticks zurück, die dieser JumpNode noch existiert.
     *
     * @return die Anzahl an Ticks die der Jumpnode noch existiert
     */
    public int getRemainingLiveTime() {
        return remainingLiveTime;
    }

    /**
     * Setzt die Anzahl an Ticks, die dieser JumpNode noch existiert.
     *
     * @param remainingLiveTime die neue Anzahl an Ticks, die der JumpNode noch existiert
     */
    public void setRemainingLiveTime(int remainingLiveTime) {
        this.remainingLiveTime = remainingLiveTime;
    }

    /**
     * Gibt die Zeit, wann sich der JN das naechste Mal bewegt zurueck.
     *
     * @return Die Zeit, wann sich der JN das naechste Mal bewegt
     */
    public int getRemainingTicksUntilMove() {
        return this.remainingTicksUntilMove;
    }

    /**
     * Setzt die Zeit, wann sich der JN das naechste Mal bewegt.
     *
     * @param remainingTicksUntilMove Die neue Zeit, wann sich der JN bewegt
     */
    public void setRemainingTicksUntilMove(int remainingTicksUntilMove) {
        this.remainingTicksUntilMove = remainingTicksUntilMove;
    }


    /**
     * Zerstört diesen dynamischen JumpNode.
     */
    public void destroy() {
        ContextMap.getContext().getDB().delete(this.jumpnode);
        ContextMap.getContext().getDB().delete(this);
    }

    /**
     * Bewegt diesen dynamischen JumpNode innerhalb seiner Möglichkeiten.
     */
    public void move() {
        org.hibernate.Session db = ContextMap.getContext().getDB();
        // Bewege Eingang
        if (config.getMaxDistanceToInitialStart() > 0) {
            StarSystem system = (StarSystem) db.get(StarSystem.class, jumpnode.getSystem());
            movePosition(system, config.getInitialStart().getX(), config.getInitialStart().getY(), config.getMaxDistanceToInitialStart(), jumpnode::setX, jumpnode::setY);
        }

        // Bewege Ausgang
        if (config.getMaxDistanceToInitialTarget() > 0) {
            StarSystem system = (StarSystem) db.get(StarSystem.class, jumpnode.getSystemOut());
            movePosition(system, config.getInitialTarget().getX(), config.getInitialTarget().getY(), config.getMaxDistanceToInitialTarget(), jumpnode::setXOut, jumpnode::setYOut);
        }

        this.setRemainingTicksUntilMove(initialTicksUntilMove);
    }

    private void movePosition(StarSystem system, int startX, int startY, int maxDistance, Consumer<Integer> xSetter, Consumer<Integer> ySetter) {
        int rnd = ThreadLocalRandom.current().nextInt(2 * maxDistance + 1);
        int newX = startX + rnd - maxDistance;
        rnd = ThreadLocalRandom.current().nextInt(2 * maxDistance +1);
        int newY = startY + rnd - maxDistance;
        if (newX <= 1) {
            newX = 1;
        }
        if (newX > system.getWidth()) {
            newX = system.getWidth();
        }
        if (newY <= 1) {
            newY = 1;
        }
        if (newY > system.getHeight()) {
            newY = system.getHeight();
        }

        xSetter.accept(newX);
        ySetter.accept(newY);
    }
}