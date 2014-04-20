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

import org.hibernate.annotations.ForeignKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * Eine Tutorialseite.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="inttutorial")
public class IntTutorial {
	@Id @GeneratedValue
	private int id;
	private boolean reqBase;
	private boolean reqShip;
	private boolean reqName;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="inttutorial_fk_inttutorial")
	private IntTutorial benoetigteSeite;
	@Column(name="headimg", nullable = false)
	private String headImg;
	@Lob
	@Column(nullable = false)
	private String text;
	@OneToMany(mappedBy = "benoetigteSeite")
	private Set<IntTutorial> abhaengigeSeiten = new HashSet<>();
	
	/**
	 * Konstruktor.
	 *
	 */
	public IntTutorial() {
		// EMPTY
	}

	/**
	 * Gibt die anzuzeigene Kopfgrafik zurueck.
	 * @return Die Grafik
	 */
	public String getHeadImg() {
		return headImg;
	}

	/**
	 * Die ID des Tutorialeintrags.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt zurueck, ob diese Tutorialseite eine Basis erfordert.
	 * @return <code>true</code>, falls eine Basis benoetigt wird
	 */
	public boolean isReqBase() {
		return reqBase;
	}

	/**
	 * Gibt zurueck, ob diese Tutorialseite erfordert, dass der Spieler
	 * sich einen Namen gegeben hat.
	 * @return <code>true</code>, falls ein Name erforderlich ist
	 */
	public boolean isReqName() {
		return reqName;
	}

	/**
	 * Gibt die Tutorialseite zurueck, die vorher eingeblendet werden muss.
	 * @return Die Tutorialseite
	 */
	public IntTutorial getBenoetigteSeite() {
		return benoetigteSeite;
	}

	/**
	 * Gibt zurueck, ob fuer diese Tutorialseite ein Schiff benoetigt wird.
	 * @return <code>true</code>, falls ein Schiff erforderlich ist
	 */
	public boolean isReqShip() {
		return reqShip;
	}

	/**
	 * Gibt den Text der Tutorialseite zurueck.
	 * @return Der Text
	 */
	public String getText() {
		return text;
	}

	/**
	 * Setzt, ob diese Tutorialseite eine Basis erfordert.
	 * @param reqBase <code>true</code>, falls eine Basis benoetigt wird
	 */
	public void setReqBase(boolean reqBase)
	{
		this.reqBase = reqBase;
	}

	/**
	 * Setzt, ob fuer diese Tutorialseite ein Schiff benoetigt wird.
	 * @param reqShip <code>true</code>, falls ein Schiff erforderlich ist
	 */
	public void setReqShip(boolean reqShip)
	{
		this.reqShip = reqShip;
	}

	/**
	 * Setzt, ob diese Tutorialseite erfordert, dass der Spieler
	 * sich einen Namen gegeben hat.
	 * @param reqName <code>true</code>, falls ein Name erforderlich ist
	 */
	public void setReqName(boolean reqName)
	{
		this.reqName = reqName;
	}

	/**
	 * Setzt die Tutorialseite, die vorher eingeblendet werden muss.
	 * @param reqSheet Die Tutorialseite
	 */
	public void setBenoetigteSeite(IntTutorial reqSheet)
	{
		this.benoetigteSeite = reqSheet;
	}

	/**
	 * Setzt die anzuzeigene Kopfgrafik.
	 * @param headImg Die Grafik
	 */
	public void setHeadImg(String headImg)
	{
		this.headImg = headImg;
	}

	/**
	 * Setzt den Text der Tutorialseite.
	 * @param text Der Text
	 */
	public void setText(String text)
	{
		this.text = text;
	}

	/**
	 * Gibt alle von dieser Tutorialseite abhaengigen Seiten zurueck.
	 * @return Die Menge der Tutorialseiten
	 */
	public Set<IntTutorial> getAbhaengigeSeiten()
	{
		return abhaengigeSeiten;
	}
}
