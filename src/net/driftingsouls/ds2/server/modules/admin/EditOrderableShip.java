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
package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableShip;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableShip_;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import net.driftingsouls.ds2.server.ships.ShipType;

import javax.annotation.Nonnull;

/**
 * Aktualisierungstool fuer die Systeme.
 *
 */
@AdminMenuEntry(category = "Schiffe", name = "NPC-Schiffsbestellungen", permission = WellKnownAdminPermission.EDIT_ORDERABLE_SHIPS)
public class EditOrderableShip implements EntityEditor<OrderableShip>
{
	@Override
	public Class<OrderableShip> getEntityType()
	{
		return OrderableShip.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<OrderableShip> form)
	{
		form.allowAdd();
		form.allowDelete();
		form.field("Schiffstyp", ShipType.class, OrderableShip::getShipType, OrderableShip::setShipType);
		form.field("Für Rasse bestellbar", Rasse.class, OrderableShip::getRasse, OrderableShip::setRasse).dbColumn(OrderableShip_.rasse);
		form.field("Kosten", Integer.class, OrderableShip::getCost, OrderableShip::setCost);
	}
}
