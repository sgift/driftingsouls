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

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.FactoryEntry;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import javax.annotation.Nonnull;
import java.math.BigDecimal;

/**
 * Aktualisierungstool fuer die Werte eines Schiffes.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Items", name = "Fabrikeintrag", permission = WellKnownAdminPermission.EDIT_FACTORY_ENTRY)
public class EditFactoryEntry implements EntityEditor<FactoryEntry>
{
	@Override
	public Class<FactoryEntry> getEntityType()
	{
		return FactoryEntry.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<FactoryEntry> form)
	{
		form.allowAdd();
		form.field("Name", String.class, FactoryEntry::getName, FactoryEntry::setName);
		form.field("Baukosten", Cargo.class, FactoryEntry::getBuildCosts, FactoryEntry::setBuildCosts);
		form.field("Forschung 1", Forschung.class, FactoryEntry::getRes1, FactoryEntry::setRes1).withNullOption("[keine]");
		form.field("Forschung 2", Forschung.class, FactoryEntry::getRes2, FactoryEntry::setRes2).withNullOption("[keine]");
		form.field("Forschung 3", Forschung.class, FactoryEntry::getRes3, FactoryEntry::setRes3).withNullOption("[keine]");
		form.field("Produktion", Cargo.class, FactoryEntry::getProduce, FactoryEntry::setProduce);
		form.field("Dauer", BigDecimal.class, FactoryEntry::getDauer, FactoryEntry::setDauer);
		form.field("BuildingIDs", String.class, FactoryEntry::getBuildingIdString, FactoryEntry::setBuildingIdString);
	}
}
