package net.driftingsouls.ds2.server.battles;

import javax.annotation.Nonnull;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import java.util.Date;

/**
 * Basisklasse fuer Logeintraege zu einer Schlacht.
 */
@Entity
public abstract class SchlachtLogEintrag implements Comparable<SchlachtLogEintrag>
{
	@Id
	@GeneratedValue
	private Long id;
	@Version
	private Integer version;

	@Temporal(TemporalType.TIMESTAMP)
	private Date zeitpunkt;
	private int tick;

	protected SchlachtLogEintrag()
	{
		this.zeitpunkt = new Date();
	}

	/**
	 * Setzt den Tick an dem die Aktion durchgefuehrt wurde.
	 * @param tick Der Tick
	 */
	public void setTick(int tick)
	{
		this.tick = tick;
	}

	/**
	 * Gibt den Zeitpunkt zurueck, an dem der Logeintrag erzeugt wurde.
	 * @return Der Zeitpunkt
	 */
	public Date getZeitpunkt()
	{
		return zeitpunkt;
	}

	/**
	 * Gibt den Tick zurueck, in dem der Eintrag erzeugt wurde.
	 * @return Der Tick
	 */
	public int getTick()
	{
		return tick;
	}

	@Override
	public int compareTo(@Nonnull SchlachtLogEintrag o)
	{
		int diff = this.zeitpunkt.compareTo(o.zeitpunkt);
		if( diff != 0 )
		{
			return diff;
		}
		if( this.id != null )
		{
			return o.id == null ? -1 : this.id.compareTo(o.id);
		}
		return o.id == null ? 0 : 1;
	}
}
