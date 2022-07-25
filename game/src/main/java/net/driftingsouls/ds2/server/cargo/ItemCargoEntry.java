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

import net.driftingsouls.ds2.server.config.items.*;
import net.driftingsouls.ds2.server.config.items.effects.IEModule;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.FactoryEntry;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.repositories.ItemRepository;

// TODO: Readonly-Version anfertigen
/**
 * Repraesentiert ein Itemeintrag innerhalb eines Cargos.
 * @author Christopher Jung
 *
 */
public class ItemCargoEntry<T extends Item> {
	private final Cargo cargo;
	private final int itemid;
	private final long count;
	private int uses;
	private final int data;

	protected ItemCargoEntry( Cargo cargo, T item, long count, int uses, int data ) {
		this(cargo, item.getID(), count, uses, data);
	}

	protected ItemCargoEntry( Cargo cargo, int itemid, long count, int uses, int data ) {
		this.cargo = cargo;
		this.itemid = itemid;
		this.count = count;
		this.uses = uses;
		this.data = data;
	}
	
	/**
	 * Benutzt ein Item. Wenn dieses nur eine begrenzte Anzahl an Benutzungen
	 * hat, wird diese verringert und geprueft ob das Item weiterhin existiert.
	 * 
	 * @return true, wenn das Item weiterhin existiert
	 * @throws UnsupportedOperationException (Falls kein CCargo zugeordnet wurde) 
	 */
	public boolean useItem() throws UnsupportedOperationException {
		if( (cargo != null) && (uses > 0) ) {
			cargo.substractResource(getResourceID(), 1);
			uses--;
			
			if( uses > 0 ) {
				cargo.addResource(getResourceID(),1);
			}
			else {
				return false;
			}
		}
		else if( cargo == null ) {
			throw new UnsupportedOperationException("Benutzung von Items nur bei vorhandenem Cargo moeglich");
		}
		return true;
	}
	
	/**
	 * Gibt die Quest-ID zurueck, die den Item zugeordnet ist.
	 * Wenn dem Item kein Quest zugeordnet ist, wird 0 zurueckgegeben.
	 * @return die Quest-ID oder 0
	 */
	public int getQuestData() {		
		return data;
	}
	
	/**
	 * Gibt die maximale Anzahl an Benutzungen des Items zurueck.
	 * Sollte die Anzahl unbegrenzt sein, so wird 0 zurueckgegeben.
	 * @return die max. Anzahl an Benutzungen oder 0
	 */
	public int getMaxUses() {
		return uses;
	}
	
	/**
	 * Gibt die ID des Itemtyps zurueck.
	 * @return die ID des Itemtyps
	 */
	public int getItemID() {		
		return itemid;
	}
	
	/**
	 * Gibt die Resourcen-ID zurueck, die den Eintrag innerhalb
	 * des Cargos identifiziert.
	 * @return Die Resourcen-ID
	 */
	public ResourceID getResourceID() {
		return new ItemID(itemid, uses, data);	
	}
	
	/**
	 * Gibt den Itemtyp als Objekt zurueck.
	 * @return Der Itemtyp
	 */
	public ItemData getItem() {
		return ItemRepository.getInstance().getItemData(itemid);
	}

	public FactoryEntry getFactoryEntry()
	{
		var itemTyp = getItem().getType();
		if(itemTyp != ItemEffect.Type.DRAFT_AMMO) {
			return null;
		}

		var db = ContextMap.getContext().getEM();
		return db.find(FactoryEntry.class, itemid);
	}

	public Munition getAmmo()
	{
		var itemTyp = getItem().getType();
		if(itemTyp != ItemEffect.Type.AMMO) {
			return null;
		}

		var db = ContextMap.getContext().getEM();
		return db.find(Munition.class, itemid);
	}

	public Schiffsmodul getShipModule()
	{
		var itemTyp = getItem().getType();
		if(itemTyp != ItemEffect.Type.MODULE) {
			return null;
		}

		var db = ContextMap.getContext().getEM();
		return db.find(Schiffsmodul.class, itemid);
	}

	public Schiffsverbot getDisableShip()
	{
		var itemTyp = getItem().getType();
		if(itemTyp != ItemEffect.Type.DISABLE_SHIP) {
			return null;
		}

		var db = ContextMap.getContext().getEM();
		return db.find(Schiffsverbot.class, itemid);
	}

	public Schiffsbauplan getShipDraft()
	{
		var itemTyp = getItem().getType();
		if(itemTyp != ItemEffect.Type.DRAFT_SHIP) {
			return null;
		}

		var db = ContextMap.getContext().getEM();
		return db.find(Schiffsbauplan.class, itemid);
	}



	/**
	 * Gibt die im Cargo vorhandene Menge des Items zurueck.
	 * @return Die Menge
	 */
	public long getCount() {
		return count;
	}
	
	/**
	 * Gibt die Masse zurueck, die die im Cargo vorhandenen Resourcen verbrauchen.
	 * @return Die Masse
	 */
	public long getMass() {
		return getItem().getCargo() * count;
	}

	/**
	 * Kopiert das Item in einen anderen Cargo.
	 * @param cargo der Cargo
	 * @return der kopierte Item-Eintrag
	 */
	public ItemCargoEntry<T> cloneItem( Cargo cargo ) {
		// TODO
		throw new RuntimeException("STUB");
	}
}
