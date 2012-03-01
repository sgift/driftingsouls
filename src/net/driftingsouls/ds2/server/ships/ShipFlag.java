package net.driftingsouls.ds2.server.ships;

import org.hibernate.annotations.Fetch;

import javax.persistence.*;

/**
 * Ein Schiffsflag, dass an einem bestimmten Schiff haengt.
 * Schiffsflags koennen entweder zeitlich begrenzt oder endlos sein.
 */
@Entity
@Table(name="ship_flags")
public class ShipFlag
{
    @Id
    private int id;

    /* One of the flag types defined in Ship.java */
    private int flagType;

    /* How many ticks until the flag is removed? -1 means infinite */
    private int remaining;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="ship", nullable = false)
    private Ship ship;
    private int version;

    public ShipFlag()
    {
        //Hibernate
    }

    public ShipFlag(int flagType, Ship ship, int remaining)
    {
        this.flagType = flagType;
        this.remaining = remaining;
        this.ship = ship;
    }

    /**
     * @return Ticks, bevor das Flag entfernt wird. -1 bedeutet, dass das Flag nie entfernt wird.
     */
    public int getRemaining()
    {
        return remaining;
    }

    /**
     * @return Der Typ des Flags.
     */
    public int getFlagType()
    {
        return flagType;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof ShipFlag))
        {
            return false;
        }

        ShipFlag shipFlag = (ShipFlag) o;

        if (flagType != shipFlag.flagType)
        {
            return false;
        }

        return true;
    }

    /**
     * @param remaining Wieviele Ticks das Flag erhalten bleiben soll.
     */
    public void setRemaining(int remaining)
    {
        this.remaining = remaining;
    }

    @Override
    public int hashCode()
    {
        return flagType;
    }
}
