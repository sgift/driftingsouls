/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.modules.admin.editoren.AbstractEditPlugin8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Aktualisierungstool fuer Schiffstypen-Grafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Schiffe", name = "Typengrafik")
public class EditShiptypePicture extends AbstractEditPlugin8<ShipType> implements AdminPlugin
{
	public EditShiptypePicture()
	{
		super(ShipType.class);
	}

	private List<Integer> liefereZuAktualisierendeSchiffe(ShipType shipType)
	{
		return Common.cast(getDB().createQuery("select s.id from Ship s where s.shiptype=:type and s.modules is not null")
								   .setEntity("type", shipType)
								   .list());
	}

	private void aktualisiereSchiff(ShipType oldshipType, ShipType shipType, Integer schiffsId)
	{
		Ship ship = (Ship)getDB().get(Ship.class, schiffsId);
		ship.recalculateModules();
	}

	@Override
	protected void configureFor(@Nonnull EditorForm8<ShipType> form)
	{
		form.label("Name", ShipType::getNickname);
		form.dynamicContentField("Bild", ShipType::getPicture, ShipType::setPicture);

		form.postUpdateTask("Schiffe mit Modulen neu berechnen", this::liefereZuAktualisierendeSchiffe, this::aktualisiereSchiff);
	}
}
