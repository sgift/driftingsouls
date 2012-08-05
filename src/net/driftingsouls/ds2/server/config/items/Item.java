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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffectFactory;
import net.driftingsouls.ds2.server.framework.JSONSupport;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Repraesentiert einen Item-Typ in DS.
 *
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="items")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class Item {
	/**
	 * Enthaelt die moeglichen Qualitaetsstufen eines Items.
	 * @author Christopher Jung
	 *
	 */
	public enum Quality implements JSONSupport {
		/**
		 * Gewoehnliches Item.
		 */
		COMMON(0,""),
		/**
		 * Seltenes Item.
		 */
		RARE(1,"#3CB371"),
		/**
		 * Sehr seltenes Item.
		 */
		ULTRA_RARE(2,"#BA55D3"),
		/**
		 * Episches (noch selteners) Item.
		 */
		EPIC(3,"#FF8C00"),
		/**
		 * Artefakt (am seltensten) oder Admin-Item.
		 */
		ARTIFACT(4,"#DC143C");

		private int id = 0;
		private String color = null;

		private Quality( int id, String color ) {
			this.id = id;
			this.color = color;
		}

		/**
		 * Gibt die ID der Qualitaetsstufe zurueck.
		 * Niederige Qualitaetsstufen haben eine niederige ID.
		 * Hohe eine hohe ID.
		 * @return die ID
		 */
		public int id() {
			return this.id;
		}

		/**
		 * <p>Gibt den Farbcode in Hex-Zahlen zurueck, welcher mit dieser Qualitaetsstufe
		 * assoziiert ist.</p>
		 * <p>Items sollten grundsaetzlich im Farbcode ihrer Farbstufe angezeigt werden.</p>
		 *
		 * @return Der Farbcode als Hexzahl
		 */
		public String color() {
			return this.color;
		}

		/**
		 * Konvertiert ein String in einen Qualitaetswert. Sollte kein passender
		 * Qualitaetswert existieren wird <code>COMMON</code> zurueckgegeben
		 * @param quality Der String, welcher den Qualitaetswert als Text enthaelt.
		 * @return Der Qualitaetswert
		 */
		public static Quality fromString(String quality) {
			if( quality == null ) {
				return COMMON;
			}
			if( quality.equalsIgnoreCase("common") ) {
				return COMMON;
			}
			if( quality.equalsIgnoreCase("rare") ) {
				return RARE;
			}
			if( quality.equalsIgnoreCase("ultra-rare") || quality.equalsIgnoreCase("urare") ) {
				return ULTRA_RARE;
			}
			if( quality.equalsIgnoreCase("epic") ) {
				return EPIC;
			}
			if( quality.equalsIgnoreCase("artifact") ) {
				return ARTIFACT;
			}
			return COMMON;
		}

		/**
		 * Gibt die Qualitaet als String zurueck.
		 * @return Die Qualitaet als String
		 */
		@Override
		public String toString() {
			if( id() == 0) {
				return "common";
			}
			else if ( id() == 1) {
				return "rare";
			}
			else if ( id() == 2) {
				return "ultra-rare";
			}
			else if ( id() == 3) {
				return "epic";
			}
			else if ( id() == 4) {
				return "artifact";
			}
			return "common";
		}

		@Override
		public JSON toJSON()
		{
			JSONObject obj = new JSONObject();
			obj.accumulate("name", toString());
			obj.accumulate("color", this.color);
			return obj;
		}
	}

	@Id
	private int id;
	private String name;
	private String picture = "open.gif";
	private String largepicture = "none";
	private String description = null;
	private String effect = "";
	private long cargo = 1;
	private boolean handel = false;	// Soll das Item im Handel angezeigt werden?
	private int accesslevel = 0;
	private String quality = null;
	private boolean unknownItem = false;
	private boolean isspawnable = false;

	/**
	 * Leerer Konstruktor.
	 */
	public Item() {
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
	 * Setzt den Effekt des Item-Typs.
	 * @param effect Der neue Effekt
	 */
	public void setEffect(ItemEffect effect) {
		this.effect = effect.toString();
	}

	/**
	 * Gibt den mit dem Item assoziierten Effekt zurueck.
	 * @return der Effekt des Items
	 */
	public ItemEffect getEffect() {
		try {
			return ItemEffectFactory.fromString(this.effect);
		}
		catch (Exception e) {
			throw new IllegalStateException("Konnte keinen Effekt fuer Item "+id+" ermitteln",e);
		}
	}

	/**
	 * Gibt <code>true</code> zurueck, falls dieses Item in Menues explizit
	 * angezeigt werden soll, wo Items normalerweise nicht gelistet werden (z.B. im Handel).
	 *
	 * @return <code>true</code>, falls es explizit angezeigt werden soll
	 */
	public boolean getHandel() {
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
