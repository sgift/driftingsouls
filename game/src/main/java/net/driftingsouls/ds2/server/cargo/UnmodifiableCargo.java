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

import net.driftingsouls.ds2.server.config.items.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>Wrapper-Klasse fuer <code>Cargo</code>-Objekte.</h1>
 * Die Wrapper-Klasse faengt alle Funktionsaufrufe ab, welche das Cargo-Objekt
 * veraendern wuerden und wirft jeweils eine <code>UnsupportedOperationException</code>.
 * Alle anderen Aufrufe werden an das Cargo-Objekt weitergeleitet.
 * 
 * @author Christopher Jung
 *
 */
public class UnmodifiableCargo extends Cargo {
	private Cargo innerCargo;
	
	/**
	 * Erstellt eine neue Instanz um das angegebene Cargo-Objekt.
	 * @param cargo Das zu kapselnde Cargo-Objekt
	 */
	public UnmodifiableCargo(Cargo cargo) {
		super();
		this.innerCargo = cargo;
	}

	@Override
	public void addCargo(Cargo addcargo) {
		throw new UnsupportedOperationException("addCargo nicht erlaubt");
	}

	@Override
	public void addResource(ResourceID resourceid, long count) {
		throw new UnsupportedOperationException("addResource nicht erlaubt");
	}

	@Override
	public void addItem(ItemCargoEntry item) {
		throw new UnsupportedOperationException("addResourceObject nicht erlaubt");
	}

	@Override
	public Object clone() {
		return innerCargo.clone();
	}

	@Override
	public ResourceList compare(Cargo cargoObj, boolean echoBothSides) {
		return innerCargo.compare(cargoObj, echoBothSides);
	}
  
  @Override
  public ResourceList compare(Cargo cargoObj, boolean echoBothSides, boolean baukosten) {
    return innerCargo.compare(cargoObj, echoBothSides, false, baukosten);
  }

	@Override
	public Cargo cutCargo(long mass) {
		throw new UnsupportedOperationException("cutCargo nicht erlaubt");
	}

	@Override
	public List<ItemCargoEntry> getItem(int itemid) {
		return innerCargo.getItem(itemid);
	}

	@Override
	public <T extends Item> ItemCargoEntry<T> getItemOfType(Class<T> itemType)
	{
		return innerCargo.getItemOfType(itemType);
	}

	@Override
	public <T extends Item> List<ItemCargoEntry<T>> getItemsOfType(Class<T> itemType)
	{
		return innerCargo.getItemsOfType(itemType);
	}

	@Override
	public long getMass() {
		return innerCargo.getMass();
	}

	@Override
	public long getResourceCount(ResourceID resourceid) {
		return innerCargo.getResourceCount(resourceid);
	}

	@Override
	public ResourceList getResourceList() {
		return innerCargo.getResourceList();
	}

	@Override
	public List<ItemCargoEntry> getItems() {
		return innerCargo.getItems();
	}

	@Override
	public boolean hasResource(ResourceID resourceid, long count) {
		return innerCargo.hasResource(resourceid, count);
	}

	@Override
	public boolean hasResource(ResourceID resourceid) {
		return innerCargo.hasResource(resourceid);
	}

	@Override
	public boolean isEmpty() {
		return innerCargo.isEmpty();
	}

	@Override
	public void multiply(double factor, Round round) {
		throw new UnsupportedOperationException("multiply nicht erlaubt");
	}

	@Override
	public String save() {
		return innerCargo.save();
	}

	@Override
	public String save(boolean orginalCargo) {
		return innerCargo.save(orginalCargo);
	}

	@Override
	public void setOption(Option option, Object data) {
		throw new UnsupportedOperationException("setOption nicht erlaubt");
	}

	@Override
	public void setResource(ResourceID resourceid, long count) {
		throw new UnsupportedOperationException("setResource nicht erlaubt");
	}

	@Override
	public void substractCargo(Cargo subcargo) {
		throw new UnsupportedOperationException("substractCargo nicht erlaubt");
	}

	@Override
	public void substractResource(ResourceID resourceid, long count) {
		throw new UnsupportedOperationException("substractResource nicht erlaubt");
	}

	@Override
	public void substractItem(ItemCargoEntry item) {
		throw new UnsupportedOperationException("substractResourceObject nicht erlaubt");
	}

	@Override
	protected List<Long[]> getItemArray() {
		return new ArrayList<>(innerCargo.getItemArray());
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public Object getOption(Option option) {
		return innerCargo.getOption(option);
	}

	@Override
	public int hashCode() {
		return innerCargo.hashCode();
	}

	@Override
	public String toString() {
		return innerCargo.toString();
	}
}
