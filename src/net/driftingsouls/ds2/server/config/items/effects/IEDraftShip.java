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
package net.driftingsouls.ds2.server.config.items.effects;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.UnmodifiableCargo;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;
import net.driftingsouls.ds2.server.ships.ShipType;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Item-Effekt "Schiffsbauplan".
 * @author Christopher Jung
 *
 */
public class IEDraftShip extends ItemEffect {
	private int shiptype = 0;
	private int race = 0;
	private boolean systemReq = false;
	private boolean flagschiff = false;
	private Cargo buildcosts = null;
	private int crew = 0;
	private int e = 0;
	private int dauer = 0;
	private int[] techs = null;
	private int werftslots = 1;
	
	protected IEDraftShip(boolean allyEffect) {
		super(ItemEffect.Type.DRAFT_SHIP, allyEffect);
	}
	
	/**
	 * Gibt den Typ des baubaren Schiffs zurueck.
	 * @return Der Typ
	 */
	public int getShipType() {
		return shiptype;
	}
	
	/**
	 * Gibt die Rasse zurueck, fuer die das Schiff baubar ist.
	 * @return Die Rasse
	 */
	public int getRace() {
		return race;
	}
	
	/**
	 * Gibt zurueck, ob das Schiff nur in militaerischen Systemen gebaut werden kann.
	 * @return Die System-Voraussetzung
	 */
	public boolean hasSystemReq() {
		return systemReq;
	}
	
	/**
	 * Gibt die Baukosten zurueck.
	 * @return Die Baukosten
	 */
	public Cargo getBuildCosts() {
		return buildcosts;
	}
	
	/**
	 * Gibt die zum Bau benoetigte Crew zurueck.
	 * @return Die benoetigte Crew
	 */
	public int getCrew() {
		return crew;
	}
	
	/**
	 * Gibt die zum Bau benoetigte Energie zurueck.
	 * @return die benoetigte Energie
	 */
	public int getE() {
		return e;
	}
	
	/**
	 * Gibt zurueck, ob ein Flagschiff gebaut wird.
	 * @return <code>true</code>, wenn es ein Flagschiff ist
	 */
	public boolean isFlagschiff() {
		return flagschiff;
	}
	
	/**
	 * Gibt die Baudauer zurueck.
	 * @return Die Baudauer
	 */
	public int getDauer() {
		return dauer;
	}
	
	/**
	 * Gibt die benoetigte Tech mit dem Index zurueck.
	 * @param index Der Index (1-3)
	 * @return Die Tech
	 */
	public int getTechReq(int index) {
		index--;
		if( (index < 0) || (index >= techs.length) ) {
			return 0;
		}
		return techs[index];
	}
	
	/**
	 * Gibt die Werftslots zurueck, die zum Bau des Schiffes erforderlich sind.
	 * @return Die Werftslots
	 */
	public int getWerftSlots() {
		return werftslots;
	}
	
	protected static ItemEffect fromXML(Node effectNode) throws Exception {
		IEDraftShip draft = new IEDraftShip(false);
		
		draft.shiptype = (int)XMLUtils.getLongAttribute(effectNode, "shiptype");
		
		org.hibernate.Session db = ContextMap.getContext().getDB();
		ShipType shipType = (ShipType)db.get(ShipType.class, draft.shiptype);
		if( shipType == null ) {
			throw new Exception("Illegaler Schiffstyp '"+draft.shiptype+"' im Item-Effekt 'Schiffsbauplan'");
		}
		
		draft.race = (int)XMLUtils.getLongAttribute(effectNode, "race");
		draft.systemReq = XMLUtils.getBooleanByXPath(effectNode, "system-req");
		draft.flagschiff = XMLUtils.getBooleanByXPath(effectNode, "flagschiff");
		draft.crew = XMLUtils.getNumberByXPath(effectNode, "buildcosts/crew/@count").intValue();
		draft.e = XMLUtils.getNumberByXPath(effectNode, "buildcosts/e/@count").intValue();
		draft.dauer = XMLUtils.getNumberByXPath(effectNode, "dauer/@count").intValue();
		
		NodeList technodes = XMLUtils.getNodesByXPath(effectNode, "tech-req");
		draft.techs = new int[technodes.getLength()];
		for( int i=0; i < technodes.getLength(); i++ ) {
			draft.techs[i] = (int)XMLUtils.getLongAttribute(technodes.item(i), "id");
		}
	
		draft.werftslots = XMLUtils.getNumberByXPath(effectNode, "werft-slots/@count").intValue();
		
		draft.buildcosts = new UnmodifiableCargo(new Cargo(XMLUtils.getNodeByXPath(effectNode, "buildcosts")));
		
		return draft;
	}
}
