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
import net.driftingsouls.ds2.server.config.items.Schiffsbauplan;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipType;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Item-Effekt "Schiffsbauplan".
 * @author Christopher Jung
 *
 */
public class IEDraftShip extends ItemEffect {
	private Schiffsbauplan schiffsbauplan;
	
	public IEDraftShip(Schiffsbauplan schiffsbauplan) {
		super(ItemEffect.Type.DRAFT_SHIP, schiffsbauplan.isAllianzEffekt());
		this.schiffsbauplan = schiffsbauplan;
	}
	
	/**
	 * Gibt den Typ des baubaren Schiffs zurueck.
	 * @return Der Typ
	 */
	public int getShipType() {
		return schiffsbauplan.getSchiffstyp().getId();
	}
	
	/**
	 * Gibt die Rasse zurueck, fuer die das Schiff baubar ist.
	 * @return Die Rasse
	 */
	public int getRace() {
		return schiffsbauplan.getRasse().getId();
	}
	
	/**
	 * Gibt die Baukosten zurueck.
	 * @return Die Baukosten
	 */
	public Cargo getBuildCosts() {
		return schiffsbauplan.getBaukosten();
	}
	
	/**
	 * Gibt die zum Bau benoetigte Crew zurueck.
	 * @return Die benoetigte Crew
	 */
	public int getCrew() {
		return schiffsbauplan.getCrew();
	}
	
	/**
	 * Gibt die zum Bau benoetigte Energie zurueck.
	 * @return die benoetigte Energie
	 */
	public int getE() {
		return schiffsbauplan.getEnergiekosten();
	}
	
	/**
	 * Gibt zurueck, ob ein Flagschiff gebaut wird.
	 * @return <code>true</code>, wenn es ein Flagschiff ist
	 */
	public boolean isFlagschiff() {
		return schiffsbauplan.isFlagschiff();
	}
	
	/**
	 * Gibt die Baudauer zurueck.
	 * @return Die Baudauer
	 */
	public int getDauer() {
		return schiffsbauplan.getDauer();
	}

	/**
	 * Gibt alle benoetigten Forschungen zurueck.
	 * @return Die Forschungen
	 */
	public Set<Forschung> getBenoetigteForschungen()
	{
		return schiffsbauplan.getBenoetigteForschungen();
	}
	
	/**
	 * Gibt die Werftslots zurueck, die zum Bau des Schiffes erforderlich sind.
	 * @return Die Werftslots
	 */
	public int getWerftSlots() {
		return schiffsbauplan.getWerftSlots();
	}

	/**
	 * Konvertiert die Baudaten des Itemeffekts in ein {@link ShipBaubar}-Objekt.
	 * @return Das neu erstellte Objekt
	 */
	public ShipBaubar toShipBaubar()
	{
		ShipType type = schiffsbauplan.getSchiffstyp();

		ShipBaubar baudaten = new ShipBaubar(type);
		baudaten.setCosts(this.schiffsbauplan.getBaukosten());
		baudaten.setCrew(this.schiffsbauplan.getCrew());
		baudaten.setDauer(this.schiffsbauplan.getDauer());
		baudaten.setEKosten(this.schiffsbauplan.getEnergiekosten());
		baudaten.setRace(this.schiffsbauplan.getRasse().getId());
		Iterator<Forschung> iter = this.schiffsbauplan.getBenoetigteForschungen().iterator();
		if( iter.hasNext() )
		{
			baudaten.setRes1(iter.next());
		}
		if( iter.hasNext() )
		{
			baudaten.setRes2(iter.next());
		}
		if( iter.hasNext() )
		{
			baudaten.setRes3(iter.next());
		}
		baudaten.setWerftSlots(this.schiffsbauplan.getWerftSlots());
		baudaten.setFlagschiff(this.schiffsbauplan.isFlagschiff());

		return baudaten;
	}
	
	/**
	 * Gibt den Itemeffect als String aus.
	 * @return der Effect als String
	 */
	@Override
	public String toString() {
		String itemstring = "draft-ship:" + getShipType() + "&" + getRace() + "&" + isFlagschiff() + "&" + getCrew() + "&" + getE() + "&" + getDauer() + "&" + getWerftSlots() + "&" + getBuildCosts().save();
		String techs = this.schiffsbauplan.getBenoetigteForschungen().stream().map(f -> Integer.toString(f.getID())).collect(Collectors.joining(","));
		if( techs.equals("")) {
			techs = ",";
		}
		itemstring = itemstring + "&" + techs;
		
		return itemstring;
	}
}
