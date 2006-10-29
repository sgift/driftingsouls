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
package net.driftingsouls.ds2.server.config;

/**
 * Repraesentiert einen Item-Typ in DS
 * 
 * @author Christopher Jung
 *
 */
public class Item {
	/**
	 * Enthaelt die moeglichen Qualitaetsstufen eines Items
	 * @author Christopher Jung
	 *
	 */
	public enum Quality {
		/**
		 * Gewoehnliches Item
		 */
		COMMON(0,""),
		/**
		 * Seltenes Item
		 */
		RARE(1,"#3CB371"),
		/**
		 * Sehr seltenes Item
		 */
		ULTRA_RARE(2,"#BA55D3"),
		/**
		 * Episches (noch selteners) Item
		 */
		EPIC(3,"#FF8C00"),
		/**
		 * Artefakt (am seltensten) oder Admin-Item
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
		 * @param quality Der String, welcher den Qualitaetswert als Text enthaelt
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
	}
	
	private int id = 0;
	protected String name = "noname";
	protected String picture = "open.gif";
	protected String largepicture = "none";
	protected String description = null;
	protected ItemEffect effect = null;
	protected long cargo = 1;
	protected boolean handel = false;	// Soll das Item im Handel angezeigt werden?
	protected int accesslevel = 0;
	protected Quality quality = Quality.COMMON;
	protected boolean unknownItem = false;

	protected Item(int id, String name) {
		this.name = name;
		this.id = id;
	}
	
	protected Item(int id, String name, String picture) {
		this(id, name);
		this.picture = picture;
	}
	
	/**
	 * Gibt die ID des Item-Typs zurueck
	 * @return die ID
	 */
	public int getID() {
		return this.id;
	}
	
	/**
	 * Gibt den Namen des Item-Typs zurueck
	 * @return der Name
	 */
	public String getName() {
		return this.name;	
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

	protected void setDescription(String text) {
		this.description = text;
	}
	
	/**
	 * Gibt den verbrauchten Cargo einer Einheit dieses Item-Typs zurueck
	 * @return der Cargo
	 */
	public long getCargo() {
		return this.cargo;	
	}

	protected void setEffect(ItemEffect effect) {
		this.effect = effect;
	}
	
	/**
	 * Gibt den mit dem Item assoziierten Effekt zurueck.
	 * 
	 * @return der Effekt des Items
	 */
	public ItemEffect getEffect() {
		return this.effect;	
	}
	
	/**
	 * Gibt <code>true</code> zurueck, falls dieses Item in Menues explizit
	 * angezeigt werden soll, wo Items normalerweise nicht gelistet werden (z.B. im Handel)
	 * 
	 * @return <code>true</code>, falls es explizit angezeigt werden soll
	 */
	public boolean getHandel() {
		return this.handel;	
	}
	
	/**
	 * Gibt den Qualitaetswert des Items zurueck
	 * @return der Qualitaetswert
	 */
	public Quality getQuality() {
		return this.quality;	
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
	 * Gibt das Bild des Items als grosse Version zurueck, sofern eines vorhanden
	 * ist. Das Bild verfuegt bereits ueber einen gueltigen Bild-Pfad.
	 * Falls kein Bild vorhanden ist, wird <code>null</code> zurueckgegeben.
	 * 
	 * @return Das grosse Bild inkl. Bild-Pfad oder <code>null</code>
	 */
	public String getLargePicture() {
		if( !this.largepicture.equals("none") ) {
			return this.largepicture;
		}
		return null;
	}
	
	/**
	 * Gibt das fuer die Benutzung/Ansicht erforderliche Access-Level des Benutzers zurueck.
	 * @return Das notwendige Access-Level
	 */
	public int getAccessLevel() {
		return this.accesslevel;	
	}
	
	/**
	 * Gibt zurueck, falls das Item ein per default unbekanntes Item ist und erst nach seiner
	 * Entdeckung durch den Benutzer angezeigt wird
	 * @return <code>true</code>, falls es ein unbekanntes Item ist
	 */
	public boolean isUnknownItem() {
		return this.unknownItem;
	}
}