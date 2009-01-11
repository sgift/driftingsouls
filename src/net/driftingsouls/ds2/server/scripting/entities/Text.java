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
package net.driftingsouls.ds2.server.scripting.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * <h1>Repraesentiert den Text in einem Questdialog.</h1>
 * <p>Einem solche, mit BBCode formatierten, Text kann jeweils ein Bild
 * zugeordnet werden, welches neben dem Text angezeigt wird. Zusaetzlich
 * besitzt ein Text einen Kommentar, welcher den Inhalt/Zweck beschreiben
 * kann und nicht angezeigt wird.</p>
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="quests_text")
public class Text {
	@Id @GeneratedValue
	private int id;
	private String text;
	private String picture;
	private String comment;
	
	/**
	 * Konstruktor.
	 *
	 */
	public Text() {
		// EMPTy
	}
	
	/**
	 * Erstellt einen neuen Text.
	 * @param text Der Text
	 */
	public Text(String text) {
		this.text = text;
		this.picture = "";
		this.comment = "";
	}

	/**
	 * Gibt den Kommentar zurueck.
	 * @return Der Kommentar
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Setzt den Kommentar.
	 * @param comment Der Kommentar
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * Gibt das anzuzeigende Bild zurueck.
	 * @return Das Bild
	 */
	public String getPicture() {
		return picture;
	}

	/**
	 * Setzt das anzuzeigende Bild.
	 * @param picture Das Bild
	 */
	public void setPicture(String picture) {
		this.picture = picture;
	}

	/**
	 * Gibt den Text zurueck.
	 * @return Der Text
	 */
	public String getText() {
		return text;
	}

	/**
	 * Setzt den Text.
	 * @param text Der Text
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}
}
