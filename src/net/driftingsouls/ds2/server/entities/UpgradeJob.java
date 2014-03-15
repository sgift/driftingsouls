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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.ships.Ship;
import org.hibernate.annotations.ForeignKey;

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

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tiles", nullable = false)
	@ForeignKey(name="upgrade_job_fk_mod_tiles")
	private UpgradeInfo tiles;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cargo", nullable = false)
	@ForeignKey(name="upgrade_job_fk_mod_cargo")
	private UpgradeInfo cargo;
	private boolean bar;
	private boolean payed;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "colonizerid", nullable = true)
	@ForeignKey(name="upgrade_job_fk_ships")
	private Ship colonizer;
	private int end;

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
	 * @param tiles Die Art der Felder Erweiterung
	 * @param cargo Die Art der Cargo Vergroesserung
	 * @param bar Wird bar bezahlt?
	 * @param colonizer Der Colonizer, der dafuer verwendet werden soll
	 */
	public UpgradeJob(Base base, User user, UpgradeInfo tiles, UpgradeInfo cargo, boolean bar, Ship colonizer)
	{
		this.base = base;
		this.user = user;
		this.tiles = tiles;
		this.cargo = cargo;
		this.bar = bar;
		this.payed = false;
		this.colonizer = colonizer;
		this.end = 0;
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
	 * Gibt die Informationen ueber die Felder-Modifikationen zurueck.
	 *
	 * @return Infos über den Felder-Ausbau
	 */
	public UpgradeInfo getTiles()
	{
		return tiles;
	}

	/**
	 * Setzt die Informationen ueber die Felder-Modifikationen.
	 *
	 * @param tiles Die neuen Infos ueber den Felder-Ausbau
	 */
	public void setTiles(UpgradeInfo tiles)
	{
		this.tiles = tiles;
	}

	/**
	 * Gibt die Informationen ueber die Cargo-Modifikationen zurueck.
	 *
	 * @return Infos ueber den Cargo-Ausbau
	 */
	public UpgradeInfo getCargo()
	{
		return cargo;
	}

	/**
	 * Setzt die Informationen ueber die Cargo-Modifikationen.
	 *
	 * @param cargo Die neuen Infos ueber den Cargo-Ausbau
	 */
	public void setCargo(UpgradeInfo cargo)
	{
		this.cargo = cargo;
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
	 * Gibt zurueck, wann der Ausbau begonnen wurde.
	 *
	 * @return Tick in dem der Ausbau begonnen wurde oder 0
	 */
	public int getEnd()
	{
		return end;
	}

	/**
	 * Setzt den Tick in dem der Ausbau enden soll.
	 *
	 * @param end Tick
	 */
	public void setEnd(int end)
	{
		this.end = end;
	}
}
