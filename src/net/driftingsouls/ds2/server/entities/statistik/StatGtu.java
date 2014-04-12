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

import net.driftingsouls.ds2.server.entities.fraktionsgui.Versteigerung;
import net.driftingsouls.ds2.server.entities.fraktionsgui.VersteigerungResource;
import net.driftingsouls.ds2.server.entities.fraktionsgui.VersteigerungSchiff;
import org.hibernate.annotations.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Ein Statistikeintrag fuer ein in der Gtu versteigertes Objekt.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="stats_gtu")
public class StatGtu {
	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String username;
	@Column(name="userid", nullable = false)
	private int userId;
	@Column(name="mtype", nullable = false)
	private int mType;
	@Lob
	@Column(nullable = false)
	private String type;
	@Index(name = "preis")
	private long preis;
	private int owner;
	@Column(nullable = false)
	private String ownername;
	@Column(name="gtugew", nullable = false)
	private double gtuGew;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	protected StatGtu() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Statistikeintrag fuer die angegebene Versteigerung.
	 * @param entry Die Versteigerung
	 * @param gtuGewinn Der Gewinn der GTU in Prozent (0-100)
	 */
	public StatGtu(Versteigerung entry, int gtuGewinn) {
		this.username = entry.getBieter().getName();
		this.userId = entry.getBieter().getId();
		this.preis = entry.getPreis();
		this.owner = entry.getOwner().getId();
		this.ownername = entry.getOwner().getName();
		this.gtuGew = gtuGewinn;
		
		if( entry instanceof VersteigerungSchiff) {
			this.mType = 1;
			this.type = Integer.toString(((VersteigerungSchiff)entry).getShipType().getId());
		}
		else if( entry instanceof VersteigerungResource) {
			this.mType = 2;
			this.type = ((VersteigerungResource)entry).getCargo().save();
		}
		else {
			throw new UnsupportedOperationException("Die Gtu-Statistik unterstuetzt "+entry.getClass().getName()+" nicht");
		}
	}

	/**
	 * Gibt den Gewinn der Gtu in Prozent (0-100) zurueck.
	 * @return Der Gewinn
	 */
	public double getGtuGew() {
		return gtuGew;
	}

	/**
	 * Setzt den Gewinn der Gtu in Prozent (0-100).
	 * @param gtuGew Der Gewinn (0-100)
	 */
	public void setGtuGew(double gtuGew) {
		this.gtuGew = gtuGew;
	}

	/**
	 * Gibt die ID des Eintrags zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt den Obertyp des versteigerten Objekts zurueck.
	 * @return Der Obertyp
	 */
	public int getMType() {
		return mType;
	}

	/**
	 * Setzt den Obertyp des versteigerten Objekts.
	 * @param type Der Obertyp
	 */
	public void setMType(int type) {
		mType = type;
	}

	/**
	 * Gibt den Besitzer der Versteigerung zurueck.
	 * @return Der Besitzer
	 */
	public int getOwner() {
		return owner;
	}

	/**
	 * Setzt den Besitzer der Versteigerung.
	 * @param owner Der Besitzer
	 */
	public void setOwner(int owner) {
		this.owner = owner;
	}

	/**
	 * Gibt den Namen des Besitzers zurueck.
	 * @return Der Name
	 */
	public String getOwnername() {
		return ownername;
	}

	/**
	 * Setzt den Namen des Besitzers.
	 * @param ownername Der Name
	 */
	public void setOwnername(String ownername) {
		this.ownername = ownername;
	}

	/**
	 * Gibt den Versteigerungspreis zurueck.
	 * @return Der Preis
	 */
	public long getPrice() {
		return preis;
	}

	/**
	 * Setzt den Versteigrungspreis.
	 * @param price Der Preis
	 */
	public void setPrice(long price) {
		this.preis = price;
	}

	/**
	 * Gibt den Untertyp des versteigerten Objekts zurueck.
	 * @return Der Untertyp
	 */
	public String getType() {
		return type;
	}

	/**
	 * Setzt den Untertyp des versteigerten Objekts.
	 * @param type Der Untertyp
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Gibt den User zurueck, der das Objekt ersteigert hat.
	 * @return Der User
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * Setzt den User, der das Objekt ersteigert hat.
	 * @param userId Der User
	 */
	public void setUserId(int userId) {
		this.userId = userId;
	}

	/**
	 * Gibt den Namen des Users zurueck, der das Objekt ersteigert hat.
	 * @return Der Name
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Setzt den Namen des Users, der das Objekt ersteigert hat.
	 * @param username Der Name
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
