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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

/**
 * Eine einfache Waffe in DS. Basisklasse fuer alle Waffen
 * @author Christopher Jung
 *
 */
public class Weapon {
	/**
	 * Waffenflags
	 */
	public enum Flags {
		/**
		 * Spezial-Waffe (eigene Auswahlbox unter sonstiges)
		 */
		SPECIAL(1),
		/**
		 * Nach dem Abfeuern das Schiff zerstoeren
		 */
		DESTROY_AFTER(2),
		/**
		 * Ammo-Auswahl fuer diese Waffe zulassen
		 */
		AMMO_SELECT(4),
		/**
		 * Area-Damage ueber die Distanz nicht reduzieren
		 */
		AD_FULL(8),
		/**
		 * Weitreichende Waffen koennen aus der zweiten Reihe heraus abgefeuert werden
		 */
		LONG_RANGE(16);	
		
		private int bit;
		private Flags(int bit) {
			this.bit = bit;
		}
		
		/**
		 * Gibt das zum Flag gehoerende Bitmuster zurueck
		 * @return Das Bitmuster
		 */
		public int getBits() {
			return this.bit;
		}
		
	}
	
	/**
	 * Waffenflags
	 */
	public enum AmmoFlags {
		/**
		 * Area-Damage ueber die Distanz nicht reduzieren
		 */
		AD_FULL(1);
		
		private int bit;
		private AmmoFlags(int bit) {
			this.bit = bit;
		}
		
		/**
		 * Gibt das zum Flag gehoerende Bitmuster zurueck
		 * @return Das Bitmuster
		 */
		public int getBits() {
			return this.bit;
		}
		
	}
	
	private String name = "";
	
	private int defTrefferWS = 50;
	private int defSmallTrefferWS = 0;
	private double defTorpTrefferWS = 0;
	private int defSubWS = 0;
	
	private int apCost = 1;
	private int eCost = 1;
	
	private int baseDamage = 0;
	private int shieldDamage = 0;
	private int areaDamage = 0;
	private int subDamage = 0;
	
	private String munition = "none";
	private int singleshots = 1;
	private boolean destroyable = false;
	
	private int flags = 0;
	
	/**
	 * Konstruktor
	 * @param node Der zu landende XML-Knoten
	 * @throws Exception
	 */
	public Weapon(Node node) throws Exception {
		this.name = XMLUtils.getStringByXPath(node, "name/text()");
		
		Node trefferws = XMLUtils.firstNodeByTagName(node, "treffer-ws");
		if( trefferws != null ) {
			String defTrefferWS = XMLUtils.getStringAttribute(trefferws, "default");
			if( defTrefferWS != null ) {
				this.defTrefferWS = Integer.parseInt(defTrefferWS);
			}
			
			String smallTrefferWS = XMLUtils.getStringAttribute(trefferws, "small");
			if( smallTrefferWS != null ) {
				this.defSmallTrefferWS = Integer.parseInt(smallTrefferWS);
			}
			
			String torpTrefferWS = XMLUtils.getStringAttribute(trefferws, "torpedo");
			if( torpTrefferWS != null ) {
				this.defTorpTrefferWS = Double.parseDouble(torpTrefferWS);
			}
			
			String subTrefferWS = XMLUtils.getStringAttribute(trefferws, "sub");
			if( subTrefferWS != null ) {
				this.defSubWS = Integer.parseInt(subTrefferWS);
			}
		}
		
		Node cost = XMLUtils.firstNodeByTagName(node, "cost");
		if( cost != null ) {
			String apCost = XMLUtils.getStringAttribute(cost, "ap");
			if( apCost != null ) {
				this.apCost = Integer.parseInt(apCost);
			}
			
			String eCost = XMLUtils.getStringAttribute(cost, "e");
			if( eCost != null ) {
				this.eCost = Integer.parseInt(eCost);
			}
		}
		
		Node damage = XMLUtils.firstNodeByTagName(node, "damage");
		if( damage != null ) {
			String hull = XMLUtils.getStringAttribute(damage, "hull");
			if( hull != null ) {
				this.baseDamage = Integer.parseInt(hull);
			}
			
			String shields = XMLUtils.getStringAttribute(damage, "shields");
			if( shields != null ) {
				this.shieldDamage = Integer.parseInt(shields);
			}
			
			String area = XMLUtils.getStringAttribute(damage, "area");
			if( area != null ) {
				this.areaDamage = Integer.parseInt(area);
			}
			
			String sub = XMLUtils.getStringAttribute(damage, "sub");
			if( sub != null ) {
				this.subDamage = Integer.parseInt(sub);
			}
		}
		
		Node shots = XMLUtils.firstNodeByTagName(node, "shots");
		if( shots != null ) {
			String single = XMLUtils.getStringAttribute(shots, "single");
			if( single != null ) {
				this.singleshots = Integer.parseInt(single);
			}
			
			String munition = XMLUtils.getStringAttribute(shots, "ammo");
			if( munition != null ) {
				this.munition = munition;
			}
			
			String destroyable = XMLUtils.getStringAttribute(shots, "destroyable");
			if( destroyable != null ) {
				this.destroyable = Boolean.parseBoolean(destroyable);
			}
		}
		
		Node flags = XMLUtils.firstNodeByTagName(node, "flags");
		if( flags != null ) {
			NodeList flagList = flags.getChildNodes();
			for( int i=0; i < flagList.getLength(); i++ ) {
				if( flagList.item(i).getNodeType() != Node.ELEMENT_NODE ) {
					continue;
				}
				if( !flagList.item(i).getNodeName().equals("flag") ) {
					continue;
				}
				
				this.flags |= Flags.valueOf( XMLUtils.getStringAttribute(flagList.item(i), "id") ).getBits(); 
			}
		}
	}
	
	/**
	 * Gibt den Namen der Waffe zurueck
	 * @return Der Name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Gibt die zum Abfeuern benoetigten AP zurueck
	 * @return Die AP-Kosten
	 */
	public int getAPCost() {
		return this.apCost;
	}
	
	/**
	 * Gibt die zum Abfeuern benoetigte Energie zurueck 
	 * @return Die Energie-Kosten
	 */
	public int getECost() {
		return this.eCost;
	}
	
	/**
	 * Gibt den Schaden der Waffe gegenueber der Schiffshuelle zurueck
	 * @param ownShipType Der Schiffstyp des feuernden Schiffes
	 * @return Der Schaden an der Huelle
	 */
	public int getBaseDamage(SQLResultRow ownShipType) {
		return this.baseDamage;
	}
	
	/**
	 * Gibt den Multiplikationsfaktor fuer den Schaden in Abhaengigkeit vom getroffenen Schiffstyp zurueck
	 * @param enemyShipType Der Typ des Schiffes, auf welches gefeuert werden soll
	 * @return Der Multiplikationsfaktor
	 */
	public int getBaseDamageModifier(SQLResultRow enemyShipType) {
		return 1;
	}
	
	/**
	 * Gibt den Schaden der Waffe gegenueber den Schilden zurueck
	 * @param ownShipType Der Schiffstyp des feuernden Schiffes
	 * @return Der Schaden an den Schilden
	 */
	public int getShieldDamage(SQLResultRow ownShipType) {
		return this.shieldDamage;
	}
	
	/**
	 * Gibt den Schaden der Waffe gegenueber den Subsystemen zurueck
	 * @param ownShipType Der Schiffstyp des feuernden Schiffes
	 * @return Der Schaden an den Subsystemen
	 */
	public int getSubDamage(SQLResultRow ownShipType) {
		return this.subDamage;
	}
	
	/**
	 * Gibt die Trefferwahrscheinlichkeit gegenueber normalen Schiffen zurueck
	 * @return Die Trefferwahrscheinlichkeit gegenueber normalen Schiffen
	 */
	public int getDefTrefferWS() {
		return this.defTrefferWS;
	}
	
	/**
	 * Gibt die Trefferwahrscheinlichkeit gegenueber kleinen Schiffen zurueck
	 * @return Die Trefferwahrscheinlichkeit gegenueber kleinen Schiffen
	 */
	public int getDefSmallTrefferWS() {
		return this.defSmallTrefferWS;
	}
	
	/**
	 * Gibt die Trefferwahrscheinlichkeit gegenueber anfliegenden Torpedos (und anderen zerstoerbaren Waffen) zurueck
	 * @return Die Trefferwahrscheinlichkeit gegenueber Torpedos (und anderen zerstoerbaren Waffen)
	 */
	public double getTorpTrefferWS() {
		return this.defTorpTrefferWS;
	}
	
	/**
	 * Gibt die Trefferwahrscheinlichkeit gegenueber Subsystemen zurueck
	 * @return Die Trefferwahrscheinlichkeit gegenueber Subsystemen
	 */
	public int getDefSubWS() {
		return this.defSubWS;
	}
	
	/**
	 * Berechnet Aenderungen an den Schiffstypen
	 * @param ownShipType Der Typ des feuernden Schiffes
	 * @param enemyShipType Der Typ des getroffenen Schiffes
	 * @return Wurden Aenderungen vorgenommen (<code>true</code>)
	 */
	public boolean calcShipTypes(SQLResultRow ownShipType, SQLResultRow enemyShipType) {
		return false;
	}
	
	/**
	 * Gibt den benoetigten Munitionstyp zurueck. Falls keine Munition verwendet wird, so wird <code>none</code>
	 * zurueckgegeben.
	 * @return Der benoetigte Munitionstyp oder <code>none</code>
	 */
	public String getAmmoType() {
		return this.munition;
	}
	
	/**
	 * Gibt die Anzahl der Einzelschuesse pro abgefeuertem Schuss zurueck
	 * @return Die Anzahl der Einzelschuesse pro abgefeuertem Schiff 
	 */
	public int getSingleShots() {
		return this.singleshots;
	}
	
	/**
	 * Gibt die Reichweite des Schadens gegenueber der Umgebung des getroffenen Schiffes zurueck
	 * @return Der Umgebungsschaden
	 */
	public int getAreaDamage() {
		return this.areaDamage;
	}
	
	/**
	 * Gibt zurueck, ob das Geschoss durch Abwehrfeuer zerstoerbar ist
	 * @return <code>true</code>, falls das Geschoss durch Abwehrfeuer zerstoerbar ist
	 */
	public boolean getDestroyable() {
		return this.destroyable;
	}
	
	/**
	 * Prueft, ob die Waffe ueber das angegebene Flag verfuegt
	 * @param flag Das Flag
	 * @return <code>true</code>, falls die Waffe das Flag besitzt
	 */
	public boolean hasFlag(Flags flag) {
		return (this.flags & flag.getBits()) != 0;
	}
}
