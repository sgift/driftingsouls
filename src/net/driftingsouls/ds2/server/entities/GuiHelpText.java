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
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * Repraesentiert einen Hilfeeintrag zu einer Seite in DS.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="gui_help")
public class GuiHelpText {
	@Id
	private String page;
	@Lob
	private String text;
	
	/**
	 * Konstruktor.
	 *
	 */
	public GuiHelpText() {
		// EMPTY
	}

	/**
	 * Gibt den Namen der Seite zurueck.
	 * @return Der Seitenname
	 */
	public String getPage() {
		return page;
	}

	/**
	 * Gibt den Hilfetext zurueck.
	 * @return Der Hilfetext
	 */
	public String getText() {
		return text;
	}

	/**
	 * Setzt den Namen der Seite. Der Name fungiert gleichzeitig als ID.
	 * @param page Der Name der Seite
	 */
	public void setPage(String page)
	{
		this.page = page;
	}

	/**
	 * Setzt den Hilfetext.
	 * @param text Der Hilfetext
	 */
	public void setText(String text)
	{
		this.text = text;
	}
}
