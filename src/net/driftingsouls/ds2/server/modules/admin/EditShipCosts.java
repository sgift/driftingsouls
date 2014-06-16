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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipType;

import javax.annotation.Nonnull;

/**
 * Aktualisierungstool fuer die Kosten von Schiffstypen.
 */
@AdminMenuEntry(category = "Schiffe", name = "Baukosten", permission = WellKnownAdminPermission.EDIT_SHIP_COSTS)
public class EditShipCosts implements EntityEditor<ShipBaubar>
{
	@Override
	public Class<ShipBaubar> getEntityType()
	{
		return ShipBaubar.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<ShipBaubar> form)
	{
		form.allowAdd();
		form.allowDelete();
		form.ifAdding().field("Schiffstyp", ShipType.class, ShipBaubar::getType, ShipBaubar::setType);
		form.ifUpdating().label("Schiffstyp", ShipBaubar::getType);
		form.field("Energiekosten", Integer.class, ShipBaubar::getEKosten, ShipBaubar::setEKosten);
		form.field("Crew", Integer.class, ShipBaubar::getCrew, ShipBaubar::setCrew);
		form.field("Dauer", Integer.class, ShipBaubar::getDauer, ShipBaubar::setDauer);
		form.field("Rasse", Rasse.class, Integer.class, ShipBaubar::getRace, ShipBaubar::setRace);
		form.field("Benötigtige Werftslots", Integer.class, ShipBaubar::getWerftSlots, ShipBaubar::setWerftSlots);
		form.field("Flagschiff", Boolean.class, ShipBaubar::isFlagschiff, ShipBaubar::setFlagschiff);
		form.field("Forschung 1", Forschung.class, (sb) -> sb.getRes(1), ShipBaubar::setRes1).withNullOption("[keine]");
		form.field("Forschung 2", Forschung.class, (sb) -> sb.getRes(2), ShipBaubar::setRes2).withNullOption("[keine]");
		form.field("Forschung 3", Forschung.class, (sb) -> sb.getRes(3), ShipBaubar::setRes3).withNullOption("[keine]");
		form.field("Baukosten", Cargo.class, ShipBaubar::getCosts, ShipBaubar::setCosts);
	}
}
