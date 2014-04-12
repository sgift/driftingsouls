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
package net.driftingsouls.ds2.server.entities.fraktionsgui;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import org.hibernate.annotations.ForeignKey;

/**
 * Eine Bestellung in einem Fraktionsshop.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="factions_shop_orders")
public class FactionShopOrder {
	@Id @GeneratedValue
	private int id;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="shopentry_id", nullable=false)
	@ForeignKey(name="factions_shop_orders_fk_factions_shop_entries")
	private FactionShopEntry shopEntry;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="user_id", nullable=false)
	@ForeignKey(name="factions_shop_orders_fk_users")
	private User user;
	private long price;
	private long lpKosten;
	private int count;
	private int status;
	private long date;
	@Column(name="adddata")
	@Lob
	private String addData;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public FactionShopOrder() {
		// EMPTY
	}

	/**
	 * <p>Konstruktor.</p>
	 * Erstellt eine neue Bestellung fuer den aktuellen Zeitpunkt.
	 * @param entry Der Shopeintrag, der gekauft wurde
	 * @param user Der User, der den Kauf getaetigt hat
	 */
	public FactionShopOrder(FactionShopEntry entry, User user) {
		setShopEntry(entry);
		setUser(user);
		setDate(Common.time());
	}

	/**
	 * Gibt die LP-Kosten fuer den Auftrag zurueck.
	 * @return Die LP-Kosten
	 */
	public long getLpKosten()
	{
		return lpKosten;
	}

	/**
	 * Setzt die LP-Kosten fuer den Auftrag.
	 * @param lpKosten Die LP-Kosten
	 */
	public void setLpKosten(long lpKosten)
	{
		this.lpKosten = lpKosten;
	}

	/**
	 * Gibt weitere Daten zurueck.
	 * @return Weitere Daten
	 */
	public String getAddData() {
		return addData;
	}

	/**
	 * Setzt weitere Daten.
	 * @param adddata weitere Daten
	 */
	public void setAddData(String adddata) {
		this.addData = adddata;
	}

	/**
	 * Gibt zurueck, wie oft der Eintrag gekauft wurde.
	 * @return Die Anzahl
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Setzt, wie oft der Eintrag gekauft wurde.
	 * @param count Die Anzahl
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * Gibt das Datum des Kaufs zurueck.
	 * @return Das Datum
	 */
	public long getDate() {
		return date;
	}

	/**
	 * Setzt das Datum des Kaufs.
	 * @param date Das Datum
	 */
	public final void setDate(final long date) {
		this.date = date;
	}

	/**
	 * Gibt den Kaufpreis in RE zurueck.
	 * @return Der Kaufpreis
	 */
	public long getPrice() {
		return price;
	}

	/**
	 * Setzt den Kaufpreis in RE.
	 * @param price Der Kaufpreis
	 */
	public void setPrice(long price) {
		this.price = price;
	}

	/**
	 * Gibt den Shopeintrag zurueck, der gekauft wurde.
	 * @return Der Shopeintrag
	 */
	public FactionShopEntry getShopEntry() {
		return shopEntry;
	}

	/**
	 * Setzt den Shopeintrag, der gekauft wurde.
	 * @param shopEntry Der Shopeintrag
	 */
	public final void setShopEntry(final FactionShopEntry shopEntry) {
		this.shopEntry = shopEntry;
	}

	/**
	 * Gibt den Bearbeitungsstatus zurueck.
	 * @return Der Bearbeitungsstatus
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Setzt den Bearbeitungsstatus.
	 * @param status Der neue Status
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * Gibt den Kaeufer zurueck.
	 * @return Der Kaeufer
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Setzt den Kaeufer.
	 * @param user Der Kaeufer
	 */
	public final void setUser(final User user) {
		this.user = user;
	}

	/**
	 * Gibt die ID der Bestellung zurueck.
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
