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


import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.commons.lang.math.RandomUtils;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;

/**
 * Repraesentiert einen aktiven Dynamischen Sprungpunkt.
 */
@Entity
@Table(name="dynamic_jumpnode")
public class DynamicJumpNode
{
    @Id @GeneratedValue
    private int id;
    @ManyToOne
    @ForeignKey(name="dynamic_jn_fk_jumpnodes")
    private JumpNode jumpnode;
    private int restdauer;
    private int inrange;
    private int outrange;
    private int nextmove;
    private int move;

    /**
     * Konstruktor.
     *
     */
    public DynamicJumpNode() {
        // EMPTY
    }

    /**
     * Erstellt einen neuen aktiven Dynamischen Sprungpunkt.
     * @param source Der Ausgangspunkt
     * @param target Der Zielpunkt
     * @param name Der Name des Sprungpunkts
     * @param restdauer Die Dauer die der Sprungpunkt existieren soll
     * @param inrange Die maximale Reichweite die der JumpNode-Eingang zurücklegen kann
     * @param outrange Die maximale Reichweite die der JumpNode-Ausgang zurücklegen kann
     * @param move Die Abstaende in denen sich der JN bewegt
     */
    public DynamicJumpNode(Location source, Location target, String name, int restdauer, int inrange, int outrange, int move) {
        this.jumpnode = new JumpNode(source, target, name);
        this.jumpnode.setHidden(true);
        this.restdauer = restdauer;
        this.inrange = inrange;
        this.outrange = outrange;
        this.nextmove = move;
        this.move = move;
    }

    /**
     * Gibt die ID der Jumpnode zurueck.
     * @return Die ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gibt den tatsächlichen JumpNode dieses dynamischen Jumpnodes zurück.
     * @return er tatsächliche JumpNode
     */
    public JumpNode getJumpNode()
    {
        return jumpnode;
    }

    /**
     * Setzt den tatsächlichen JumpNode dieses dynamischen Jumpnodes.
     * @param jumpnode der neue tatsächliche JumpNode
     */
    public void setJumpNode(JumpNode jumpnode)
    {
        this.jumpnode = jumpnode;
    }

    /**
     * Gibt die Anzahl an Ticks zurück, die dieser JumpNode noch existiert.
     * @return die Anzahl an Ticks die der Jumpnode noch existiert
     */
    public int getRestdauer()
    {
        return restdauer;
    }

    /**
     * Setzt die Anzahl an Ticks, die dieser JumpNode noch existiert.
     * @param restdauer die neue Anzahl an Ticks, die der JumpNode noch existiert
     */
    public void setRestdauer(int restdauer)
    {
        this.restdauer = restdauer;
    }

    /**
     * Gibt die Reichweite die der JumpNode-Eingang sich maximal pro Tick bewegt zurück.
     * @return die Reichweite des Eingangs
     */
    public int getInRange()
    {
        return inrange;
    }

    /**
     * Setzt die Reichweite, die der JumpNode-Eingang sich maximal pro Tick bewegt.
     * @param inrange die neue Reichweite des Eingangs
     */
    public void setInRange(int inrange)
    {
        this.inrange = inrange;
    }

    /**
     * Gibt die Reichweite die der JumpNode-Ausgang sich maximal pro Tick bewegt zurück.
     * @return die Reichweite des Ausgangs
     */
    public int getOutRange()
    {
        return outrange;
    }

    /**
     * Setzt die Reichweite, die der JumpNode-Ausgang sich maximal pro Tick bewegt.
     * @param outrange die neue Reichweite des Ausgangs
     */
    public void setOutRange(int outrange)
    {
        this.outrange = outrange;
    }

    /**
     * Gibt die Zeit, wann sich der JN das naechste Mal bewegt zurueck.
     * @return Die Zeit, wann sich der JN das naechste Mal bewegt
     */
    public int getNextMove() { return this.nextmove; }

    /**
     * Setzt die Zeit, wann sich der JN das naechste Mal bewegt.
     * @param nextmove Die neue Zeit, wann sich der JN bewegt
     */
    public void setNextMove(int nextmove) { this.nextmove = nextmove; }

    /**
     * Gibt zurueck, in welchen Abstaenden sich der JN bewegt.
     * @return Gibt an, in welchen Abstaenden sich der JN bewegt
     */
    public int getMove() { return this.move; }

    /**
     * Setzt, in welchen Abstaenden sich der JN bewegt.
     * @param move In welchen Abstaenden sich der JN bewegen soll
     */
    public void setMove(int move) { this.move = move; }

    /**
     * Zerstört diesen dynamischen JumpNode.
     */
    public void destroy()
    {
        ContextMap.getContext().getDB().delete(this.jumpnode);
        ContextMap.getContext().getDB().delete(this);
    }

    /**
     * Bewegt diesen dynamischen JumpNode innerhalb seiner Möglichkeiten.
     */
    public void move()
    {
        org.hibernate.Session db = ContextMap.getContext().getDB();
        // Bewege Eingang
        if(inrange > 0)
        {
            StarSystem system = (StarSystem) db.get(StarSystem.class, this.jumpnode.getSystem());
            int x = this.jumpnode.getX();
            int y = this.jumpnode.getY();
            int rnd = RandomUtils.nextInt(2*inrange + 1);
            int newx = x+rnd-inrange;
            rnd = RandomUtils.nextInt(2*inrange+1);
            int newy = y+rnd-inrange;
            if(newx <= 1)
            {
                newx = 1;
            }
            if(newx > system.getWidth())
            {
                newx = system.getWidth();
            }
            if(newy <= 1)
            {
                newy = 1;
            }
            if(newy > system.getHeight())
            {
                newy = system.getHeight();
            }
            this.jumpnode.setX(newx);
            this.jumpnode.setY(newy);
        }

        // Bewege Ausgang
        if(outrange > 0)
        {
            StarSystem system = (StarSystem) db.get(StarSystem.class, this.jumpnode.getSystemOut());
            int x = this.jumpnode.getXOut();
            int y = this.jumpnode.getYOut();
            int rnd = RandomUtils.nextInt(2*outrange + 1);
            int newx = x+rnd-outrange;
            rnd = RandomUtils.nextInt(2*outrange+1);
            int newy = y+rnd-outrange;
            if(newx <= 1)
            {
                newx = 1;
            }
            if(newx > system.getWidth())
            {
                newx = system.getWidth();
            }
            if(newy <= 1)
            {
                newy = 1;
            }
            if(newy > system.getHeight())
            {
                newy = system.getHeight();
            }
            this.jumpnode.setXOut(newx);
            this.jumpnode.setYOut(newy);
        }
        this.setNextMove(this.getMove());
    }
}
