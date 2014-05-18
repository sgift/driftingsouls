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
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeMaxValues;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeType;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import javax.annotation.Nonnull;

/**
 * Aktualisierungstool fuer die Maximalwerte der Asteroiden Ausbauten.
 *
 */
@AdminMenuEntry(category = "Basis-Ausbau", name = "Maximalwerte", permission = WellKnownAdminPermission.EDIT_DI)
public class EditUpgradeMaxValue implements EntityEditor<UpgradeMaxValues>
{
    @Override
    public Class<UpgradeMaxValues> getEntityType()
    {
        return UpgradeMaxValues.class;
    }

    @Override
    public void configureFor(@Nonnull EditorForm8<UpgradeMaxValues> form)
    {
        form.allowAdd();
        form.allowDelete();
        form.field("Basis-Klasse:", BaseType.class, UpgradeMaxValues::getType, UpgradeMaxValues::setType);
        form.field("Ausbau-Typ:", UpgradeType.class, UpgradeMaxValues::getUpgradeType, UpgradeMaxValues::setUpgradeType);
		form.field("Maximalwert:", Integer.class , UpgradeMaxValues::getMaximalwert, UpgradeMaxValues::setMaximalwert);
    }
}
