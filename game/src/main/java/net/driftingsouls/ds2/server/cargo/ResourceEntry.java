/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
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
package net.driftingsouls.ds2.server.cargo;

/**
 * Ein Eintrag innerhalb einer Resourcen-Liste.
 *
 * @author Christopher Jung
 * @see ResourceList
 *
 */
public class ResourceEntry {
	private ResourceID id;
	private String name;
	private String plainName;
	private String image;
	private long count1;
	private long count2 = 0;
	private long diff = 0;
	private String cargo1;
	private String cargo2 = null;
	private boolean largeImages = false;

	/**
	 * Konstruktor.
	 * @param id Die Resourcen-ID
	 * @param name Der Name
	 * @param plainname Der Name ohne Formatierung
	 * @param image Das Bild der Resource
	 * @param cargo Die Menge (formatiert)
	 * @param count Die Menge ohne Formatierung
	 */
	public ResourceEntry(ResourceID id, String name, String plainname, String image, String cargo, long count) {
		this.id = id;
		this.name = name;
		this.plainName = plainname;
		this.image = image;
		this.count1 = count;
		this.cargo1 = cargo;
	}

	/**
	 * Konstruktor, falls zwei Cargos verglichen werden.
	 * @param id Die ID der Resource
	 * @param name Der Name
	 * @param plainname Der Name ohne Formatierung
	 * @param image Das Bild der Resource
	 * @param cargo1 Die Menge des ersten Cargos (formatiert)
	 * @param cargo2 Die Menge des zweiten Cargos (formatiert)
	 * @param count1 Die Menge des ersten Cargos
	 * @param count2 Die Menge des zweiten Cargos
	 * @param diff Die Differenz der beiden Cargos
	 */
	public ResourceEntry(ResourceID id, String name, String plainname, String image, String cargo1, String cargo2, long count1, long count2, long diff) {
		this(id, name, plainname, image, cargo1, count1);
		this.cargo2 = cargo2;
		this.count2 = count2;
		this.diff = diff;
	}

	protected void setLargeImages( boolean value ) {
		largeImages = value;
	}

	/**
	 * Gibt die Menge des ersten Cargos formatiert zurueck.
	 * @return Die Menge des ersten Cargos formatiert
	 */
	public String getCargo1() {
		return cargo1;
	}

	/**
	 * Gibt die Menge des zweiten Cargos formatiert zurueck.
	 * @return Die Menge des zweiten Cargos formatiert
	 */
	public String getCargo2() {
		return cargo2;
	}

	/**
	 * Gibt die Menge des ersten Cargos ohne Formatierung zurueck.
	 * @return Die Menge des ersten Cargos ohne Formatierung
	 */
	public long getCount1() {
		return count1;
	}

	/**
	 * Gibt die Menge des zweiten Cargos ohne Formatierung zurueck.
	 * @return Die Menge des zweiten Cargos ohne Formatierung
	 */
	public long getCount2() {
		return count2;
	}

	/**
	 * Gibt die Differenz der beiden Cargomengen zurueck.
	 * @return Die Differenz
	 */
	public long getDiff() {
		return diff;
	}

	/**
	 * Gibt die Resourcen-ID des Eintrags zurueck.
	 * @return Die Resourcen-ID
	 */
	public ResourceID getId() {
		return id;
	}

	/**
	 * Gibt das Bild des Eintrags zurueck.
	 * @return das Bild des Eintrags
	 */
	public String getImage() {
		return image;
	}

	/**
	 * Gibt den (formatierten) Namen des Eintrags zurueck.
	 * @return der (formatierte) Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gibt den unformatierten Namen des Eintrags zurueck.
	 * @return der unformatierte Name
	 */
	public String getPlainName() {
		return plainName;
	}

	/**
	 * Gibt an, ob grosse Fassungen der Bilder angezeigt werden duerfen (<code>true</code>).
	 * @return <code>true</code>, falls grosse Fassungen angezeigt werden duerfen
	 */
	public boolean showLargeImages() {
		return largeImages;
	}
}
