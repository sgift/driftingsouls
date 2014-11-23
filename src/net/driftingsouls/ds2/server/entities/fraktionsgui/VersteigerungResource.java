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
package net.driftingsouls.ds2.server.entities.fraktionsgui;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;

/**
 * Versteigerung einer Resource in einer bestimmten Menge.
 * Es wird davon ausgegangen, das der Cargo auch immer nur exakt
 * <b>eine</b> Resource enthaelt. Sollten weitere Resourcen enthalten sein,
 * so koennen diese ignoriert werden.
 * @author Christopher Jung
 *
 */
@Entity
@DiscriminatorValue("2")
public class VersteigerungResource extends Versteigerung {
	private String type;
	
	/**
	 * Konstruktor.
	 *
	 */
	public VersteigerungResource() {
		// EMPTY
	}

	/**
	 * Erstellt einen neuen Versteigerungseintrag fuer die angegebene Resource.
	 * @param owner Der Besitzer und zugleich default-Bieter
	 * @param res Der Cargo der die Resource enthaelt. Der Cargo darf nicht leer sein und muss
	 * exakt eine Resource enthalten.
	 * @param price Der Startpreis
	 */
	public VersteigerungResource(User owner, Cargo res, long price) {
		super(owner, price);
		
		if( res.isEmpty() ) {
			throw new IllegalArgumentException("Der Cargo darf nicht leer sein");
		}
		
		this.type = res.save();
	}
	
	/**
	 * Gibt den Cargo zurueck.
	 * @return Der Cargo
	 */
	public Cargo getCargo() {
		return new Cargo(Cargo.Type.AUTO, type);
	}
	
	/**
	 * Setzt den Cargo.
	 * @param res Der Cargo der die Resource enthaelt. Der Cargo darf nicht leer sein und muss
	 * exakt eine Resource enthalten.
	 */
	public void setCargo(Cargo res) {
		if( res.isEmpty() ) {
			throw new IllegalArgumentException("Der Cargo darf nicht leer sein");
		}
		this.type = res.save();
	}
	
	private ResourceEntry getEntry() {
		Cargo cargo = new Cargo(Cargo.Type.AUTO, type);
		cargo.setOption( Cargo.Option.SHOWMASS, false );
		cargo.setOption( Cargo.Option.LARGEIMAGES, true );
		ResourceList reslist = cargo.getResourceList();
		return reslist.iterator().next();
	}

	@Override
	public long getObjectCount() {
		return getEntry().getCount1();
	}

	@Override
	public String getObjectName() {
		return getEntry().getPlainName();
	}

	@Override
	public String getObjectPicture() {
		return getEntry().getImage();
	}

	@Override
	public String getObjectUrl() {
		ResourceID res = getEntry().getId();
		return Common.buildUrl("details", "module", "iteminfo", "item", res.getItemID() );
	}

	@Override
	public boolean isObjectFixedImageSize() {
        return !getEntry().showLargeImages();
    }
}
