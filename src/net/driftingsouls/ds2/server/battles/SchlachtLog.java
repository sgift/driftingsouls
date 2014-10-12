package net.driftingsouls.ds2.server.battles;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

@Entity
public class SchlachtLog implements Locatable
{
	@Id
	@GeneratedValue
	private Long id;
	@Version
	private Integer version;
	private int system;
	private int x;
	private int y;
	private int startTick;
	@Temporal(TemporalType.TIMESTAMP)
	private Date startZeitpunkt;
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "schlachtlog_id")
	@OrderBy("zeitpunkt,id")
	@Sort(type = SortType.NATURAL)
	@ForeignKey(name="schlachtlogeintrag_fk_schlachtlog")
	private SortedSet<SchlachtLogEintrag> eintraege = new TreeSet<>();

	/**
	 * Konstruktor.
	 */
	protected SchlachtLog()
	{
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param battle Die Schlacht zu der das Log gehoert.
	 * @param tick Der Tick an dem die Schlacht begonnen hat.
	 */
	public SchlachtLog(Battle battle, int tick)
	{
		this.system = battle.getSystem();
		this.x = battle.getX();
		this.y = battle.getY();
		this.startTick = tick;
		this.startZeitpunkt = new Date();
	}


	@Override
	public Location getLocation()
	{
		return new Location(system, x, y);
	}

	/**
	 * Gibt den Tick zurueck, an dem die Schlacht begonnen wurde.
	 * @return Der Tick
	 */
	public int getStartTick()
	{
		return startTick;
	}

	/**
	 * Gibt den Zeitpunkt zurueck, an dem die Schlacht begonnen wurde.
	 * @return Der Zeitpunkt
	 */
	public Date getStartZeitpunkt()
	{
		return new Date(startZeitpunkt.getTime());
	}

	/**
	 * Gibt die Logeintraege im Log in sortierter Reihenfolge zurueck.
	 * @return Die Logeintraege
	 */
	public SortedSet<SchlachtLogEintrag> getEintraege()
	{
		return eintraege;
	}

	/**
	 * Fuegt einen Eintrag zum Schlachtlog hinzu.
	 * @param schlachtLogEintrag Der Eintrag
	 */
	public void add(SchlachtLogEintrag schlachtLogEintrag)
	{
		this.eintraege.add(schlachtLogEintrag);
	}
}
