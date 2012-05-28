package net.driftingsouls.ds2.server.entities.npcorders;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Eine NPC-Bestellung.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="orders")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="ordertype")
public abstract class Order {
	@Id @GeneratedValue
	private int id;
	private int tick;
	private int user;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public Order() {
		// EMPTY
	}
	
	/**
	 * Konstruktor.
	 * @param user Der User, fuer den der Auftrag abgewickelt werden soll
	 */
	public Order(int user) {
		setUser(user);
	}

	/**
	 * Gibt den Tick zurueck, an dem der Auftrag abgewickelt werden soll.
	 * @return Der Tick
	 */
	public int getTick() {
		return tick;
	}

	/**
	 * Setzt den Tick, am den der Auftrag abgewickelt werden soll.
	 * @param tick Der Tick
	 */
	public void setTick(int tick) {
		this.tick = tick;
	}

	/**
	 * Gibt den User zurueck, fuer den der Auftrag abgewickelt werden soll.
	 * @return Der User
	 */
	public int getUser() {
		return user;
	}

	/**
	 * Setzt den User fuer den der Auftrag abgewickelt werden soll.
	 * @param user Der User
	 */
	public final void setUser(final int user) {
		this.user = user;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
