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
package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * Repraesentiert einen Item-Typ in DS.
 *
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="items")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "typ", length=255)
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public abstract class Item {
	@Id @GeneratedValue(generator="ds-itemid")
	@GenericGenerator(name="ds-itemid", strategy = "net.driftingsouls.ds2.server.config.items.ItemIdGenerator")
	private int id;
	@Column(nullable = false)
	private String name;
	@Lob
	private String picture = "open.gif";
	@Lob
	private String largepicture = "none";
	@Lob
	private String description = null;
	private long cargo = 1;
	private boolean handel = false;	// Soll das Item im Handel angezeigt werden?
	private int accesslevel = 0;
	private String quality = null;
	private boolean unknownItem = false;
	private boolean isspawnable = false;

	/**
	 * Leerer Konstruktor.
	 */
	protected Item() {
		//Empty
	}

	/**
	 * Konstruktor fuer ein Item.
	 * @param id die id des Items
	 * @param name der Name des Items
	 */
	public Item(int id, String name) {
		this.name = name;
		this.id = id;
	}

	/**
	 * Konstruktor fuer ein Item.
	 * @param id Die id des Items
	 * @param name Der Name des Items
	 * @param picture Das Bild des Items
	 */
	public Item(int id, String name, String picture) {
		this(id, name);
		this.picture = picture;
	}

	/**
	 * Gibt die ID des Item-Typs zurueck.
	 * @return die ID
	 */
	public int getID() {
		return this.id;
	}

	/**
	 * Gibt den Namen des Item-Typs zurueck.
	 * @return der Name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Setzt den Namen des Item-Typs.
	 * @param name der neue Name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gibt die Beschreibung des Item-Typs zurueck.
	 * Falls keine Beschreibung vorliegt, wird <code>null</code>
	 * zurueckgegeben.
	 * @return die Beschreibung oder <code>null</code>
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Setzt die Berschreibung des Item-Typs.
	 * @param text Die neue Beschreibung
	 */
	public void setDescription(String text) {
		this.description = text;
	}

	/**
	 * Gibt den verbrauchten Cargo einer Einheit dieses Item-Typs zurueck.
	 * @return der Cargo
	 */
	public long getCargo() {
		return this.cargo;
	}

	/**
	 * Setzt den verbrauchten Cargo einer Einheit dieses Item-Typs.
	 * @param cargo der verbrauchte Cargo
	 */
	public void setCargo(long cargo) {
		this.cargo = cargo;
	}

	/**
	 * Gibt den mit dem Item assoziierten Effekt zurueck.
	 * @return der Effekt des Items
	 */
	public abstract ItemEffect getEffect();

	/**
	 * Gibt <code>true</code> zurueck, falls dieses Item in Menues explizit
	 * angezeigt werden soll, wo Items normalerweise nicht gelistet werden (z.B. im Handel).
	 *
	 * @return <code>true</code>, falls es explizit angezeigt werden soll
	 */
	public boolean isHandel() {
		return this.handel;
	}

	/**
	 * Setzt, ob dieses Item in Menues explizit angezeigt werden soll.
	 * @param handel <code>true</code> falls es explizit angezeigt werden soll
	 */
	public void setHandel(boolean handel) {
		this.handel = handel;
	}

	/**
	 * Gibt den Qualitaetswert des Items zurueck.
	 * @return der Qualitaetswert
	 */
	public Quality getQuality() {
		return Quality.fromString(quality);
	}

	/**
	 * Setzt den Qualitaetswert der Items.
	 * @param quality der Qualitaetswert
	 */
	public void setQuality(Quality quality) {
		this.quality = quality.toString();
	}

	/**
	 * Gibt das Bild des Items zurueck. Das Bild verfuegt bereits
	 * ueber einen gueltigen Bild-Pfad.
	 *
	 * @return das Bild inkl. Bild-Pfad
	 */
	public String getPicture() {
		return this.picture;
	}

	/**
	 * Setzt das Bild des Items.
	 * @param picture Der Bild-Pfad
	 */
	public void setPicture(String picture) {
		this.picture = picture;
	}

	/**
	 * Gibt das Bild des Items als grosse Version zurueck, sofern eines vorhanden
	 * ist. Das Bild verfuegt bereits ueber einen gueltigen Bild-Pfad.
	 * Falls kein Bild vorhanden ist, wird <code>null</code> zurueckgegeben.
	 *
	 * @return Das grosse Bild inkl. Bild-Pfad oder <code>null</code>
	 */
	public String getLargePicture() {
		if( !(this.largepicture == null) && !this.largepicture.equals("none") ) {
			return this.largepicture;
		}
		return null;
	}

	/**
	 * Setzt den Bildpfad fuer die grosse Version des Bildes.
	 * @param largepicture Der Bildpfad, "none", wenn es kein grosses Bild gibt.
	 */
	public void setLargePicture(String largepicture) {
		if( !largepicture.equals(""))
		{
			this.largepicture = largepicture;
		}
		else
		{
			this.largepicture = null;
		}
	}

	/**
	 * Gibt das fuer die Benutzung/Ansicht erforderliche Access-Level des Benutzers zurueck.
	 * @return Das notwendige Access-Level
	 */
	public int getAccessLevel() {
		return this.accesslevel;
	}

	/**
	 * Setzt das fuer die Benutzung/Ansicht erforderliche Access-Level des Benutzers.
	 * @param accesslevel Das notwendige Access-Level
	 */
	public void setAccessLevel(int accesslevel) {
		this.accesslevel = accesslevel;
	}

	/**
	 * Gibt zurueck, falls das Item ein per default unbekanntes Item ist und erst nach seiner
	 * Entdeckung durch den Benutzer angezeigt wird.
	 * @return <code>true</code>, falls es ein unbekanntes Item ist
	 */
	public boolean isUnknownItem() {
		return this.unknownItem;
	}

	/**
	 * Setzt, ob es sich bei dem Item um ein default-maessig unbekanntes Item handelt.
	 * @param unknownitem <code>true</code>, falls es ein unbekanntes Item ist
	 */
	public void setUnknownItem(boolean unknownitem) {
		this.unknownItem = unknownitem;
	}

	/**
	 * Gibt zurueck, ob dieses Item eine Spawnable Ressource ist.
	 * @return <code>true</code>, falls es eine Spawn-Ressource ist
	 */
	public boolean isSpawnableRess() {
		return this.isspawnable;
	}

	/**
	 * Setzt, ob es sich bei dem Item um eine Spawn-Ressource handelt.
	 * @param spawnableress <code>true</code>, falls es eine Spawn-Ressource sein soll
	 */
	public void setSpawnableRess(boolean spawnableress) {
		this.isspawnable = spawnableress;
	}
}
