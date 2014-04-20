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
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import net.driftingsouls.ds2.server.units.UnitType;

import javax.annotation.Nonnull;

/**
 * Aktualisierungstool fuer Einheitengrafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Einheiten", name = "Einheitengrafik", permission = WellKnownAdminPermission.EDIT_UNIT_PICTURE)
public class EditUnitPicture implements EntityEditor<UnitType>
{
	@Override
	public Class<UnitType> getEntityType()
	{
		return UnitType.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<UnitType> form)
	{
		form.label("Name", UnitType::getName);
		form.dynamicContentField("Bild", UnitType::getPicture, UnitType::setPicture);
	}
}
