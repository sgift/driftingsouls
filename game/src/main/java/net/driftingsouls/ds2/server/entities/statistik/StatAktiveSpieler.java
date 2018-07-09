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
package net.driftingsouls.ds2.server.entities.statistik;

import net.driftingsouls.ds2.server.entities.User;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Ein Stats-Eintrag, der die Anzahl der aktiven Spieler zu einem
 * Zeitpunkt (Tick) festhaelt.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="stat_aktive_spieler")
public class StatAktiveSpieler
{
	@Id
	private int tick;
	private int sehrAktiv;
	private int aktiv;
	private int teilweiseAktiv;
	private int wenigAktiv;
	private int inaktiv;
	private int vacation;
	private int gesamtanzahl;
	private int registrierungen;
	private int maxUserId;

	@Version
	private int version;

	/**
	 * Konstruktor.
	 *
	 */
	public StatAktiveSpieler() {
		// EMPTY
	}

	/**
	 * Erstellt einen neuen Eintrag.
	 * @param tick Der Tick
	 */
	public StatAktiveSpieler(int tick) {
		this.tick = tick;
	}

	/**
	 * Gibt die Anzahl der sehr aktiven Spieler (<= 7 Ticks) zurueck.
	 * @return Die Anzahl
	 */
	public int getSehrAktiv()
	{
		return sehrAktiv;
	}

	/**
	 * Gibt die Anzahl der aktiven Spieler (<=49 Ticks) zurueck.
	 * @return Die Anzahl
	 */
	public int getAktiv()
	{
		return aktiv;
	}

	/**
	 * Gibt die Anzahl der teilweise aktiven Spieler zurueck (<= 96 Ticks)
	 * @return Die Anzahl
	 */
	public int getTeilweiseAktiv()
	{
		return teilweiseAktiv;
	}

	/**
	 * Gibt die Anzahl der wenig aktiven Spieler zurueck (<= 299 Ticks)
	 * @return Die Anzahl
	 */
	public int getWenigAktiv()
	{
		return wenigAktiv;
	}

	/**
	 * Gibt die Anzahl der inaktiven Spieler zurueck (>= 300 Ticks)
	 * @return Die Anzahl
	 */
	public int getInaktiv()
	{
		return inaktiv;
	}

	/**
	 * Gibt die Anzahl der Spieler im Vacation-Modus zurueck.
	 * @return Die Anzahl
	 */
	public int getVacation()
	{
		return vacation;
	}

	/**
	 * Gibt die Anzahl der Neuanmeldungen zurueck.
	 * @return Die Anzahl
	 */
	public int getRegistrierungen()
	{
		return registrierungen;
	}

	/**
	 * Gibt die maximale User-ID zurueck, bis zu der
	 * der Statistikeintrag erzeugt wurde
	 * @return Die User-ID
	 */
	public int getMaxUserId()
	{
		return maxUserId;
	}

	/**
	 * Gibt die Gesamtanzahl der erfassten Spieler zurueck.
	 * @return Die Anzahl
	 */
	public int getGesamtanzahl()
	{
		return gesamtanzahl;
	}

	/**
	 * Erfasst einen Spieler als Neuregistrierung fuer
	 * die Statistik.
	 * @param user Der zu erfassende Spieler
	 */
	public void erfasseRegistrierung(User user)
	{
		this.registrierungen++;
	}

	/**
	 * Erfasst die (In)Aktivitaet eines Spielers fuer
	 * die Statistik.
	 * @param user Der zu erfassende Spieler
	 */
	public void erfasseSpieler(User user)
	{
		if( this.maxUserId < user.getId() )
		{
			this.maxUserId = user.getId();
		}
		if( user.isNPC() || user.isAdmin() )
		{
			return;
		}
		this.gesamtanzahl++;
		if( user.isInVacation() )
		{
			this.vacation++;
		}
		else if( user.getInactivity() <= 7 )
		{
			this.sehrAktiv++;
		}
		else if( user.getInactivity() <= 49 )
		{
			this.aktiv++;
		}
		else if( user.getInactivity() <= 98 )
		{
			this.teilweiseAktiv++;
		}
		else if( user.getInactivity() <= 299 )
		{
			this.wenigAktiv++;
		}
		else
		{
			this.inaktiv++;
		}
	}

	/**
	 * Gibt den Tick zurueck.
	 * @return Der Tick
	 */
	public int getTick() {
		return tick;
	}

	/**
	 * Setzt den Tick.
	 * @param tick Der Tick
	 */
	public void setTick(int tick) {
		this.tick = tick;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
