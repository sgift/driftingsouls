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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Eine Tutorialseite
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="inttutorial")
@Immutable
public class IntTutorial {
	@Id @GeneratedValue
	private int id;
	@Column(name="reqbase")
	private int reqBase;
	@Column(name="reqship")
	private int reqShip;
	@Column(name="reqname")
	private int reqName;
	@Column(name="reqsheet")
	private int reqSheet;
	@Column(name="headimg")
	private String headImg;
	private String text;
	
	/**
	 * Konstruktor
	 *
	 */
	public IntTutorial() {
		// EMPTY
	}

	/**
	 * Gibt die anzuzeigene Kopfgrafik zurueck
	 * @return Die Grafik
	 */
	public String getHeadImg() {
		return headImg;
	}

	/**
	 * Die ID des Tutorialeintrags
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt zurueck, ob diese Tutorialseite eine Basis erfordert
	 * @return <code>true</code>, falls eine Basis benoetigt wird
	 */
	public boolean getReqBase() {
		return reqBase != 0;
	}

	/**
	 * Gibt zurueck, ob diese Tutorialseite erfordert, dass der Spieler
	 * sich einen Namen gegeben hat
	 * @return <code>true</code>, falls ein Name erforderlich ist
	 */
	public int getReqName() {
		return reqName;
	}

	/**
	 * Gibt die Nummer der Tutorialseite zurueck, die vorher eingeblendet werden muss
	 * @return Die Nummer der Tutorialseite
	 */
	public int getReqSheet() {
		return reqSheet;
	}

	/**
	 * Gibt zurueck, ob fuer diese Tutorialseite ein Schiff benoetigt wird
	 * @return <code>true</code>, falls ein Schiff erforderlich ist
	 */
	public int getReqShip() {
		return reqShip;
	}

	/**
	 * Gibt den Text der Tutorialseite zurueck
	 * @return Der Text
	 */
	public String getText() {
		return text;
	}
}
