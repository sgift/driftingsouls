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
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeInfo;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeType;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import javax.annotation.Nonnull;

/**
 * Aktualisierungstool fuer die Werte der Asteroiden Ausbauten.
 *
 */
@AdminMenuEntry(category = "Basis-Ausbau", name = "Werte editieren", permission = WellKnownAdminPermission.EDIT_DI)
public class EditUpgradeInfo implements EntityEditor<UpgradeInfo>
{
    @Override
    public Class<UpgradeInfo> getEntityType()
    {
        return UpgradeInfo.class;
    }

    @Override
    public void configureFor(@Nonnull EditorForm8<UpgradeInfo> form)
    {
        form.allowAdd();
        form.field("Basis-Klasse:", BaseType.class, UpgradeInfo::getType, UpgradeInfo::setType);
        form.field("Ausbau-Typ:", UpgradeType.class, UpgradeInfo::getUpgradeType, UpgradeInfo::setUpgradeType);
        form.field("Modifizierer:", Integer.class, UpgradeInfo::getModWert, UpgradeInfo::setModWert);
        form.field("RE-Kosten:", Integer.class, UpgradeInfo::getPrice, UpgradeInfo::setPrice);
        form.field("BBS-Kosten:", Integer.class, UpgradeInfo::getMiningExplosive, UpgradeInfo::setMiningExplosive);
        form.field("Erz-Kosten:", Integer.class, UpgradeInfo::getOre, UpgradeInfo::setOre);
        form.field("Mindestdauer:", Integer.class, UpgradeInfo::getMinTicks, UpgradeInfo::setMinTicks);
        form.field("Maximaldauer:", Integer.class, UpgradeInfo::getMaxTicks,  (u, maxticks) -> u.setMaxTicks(Math.max(maxticks, u.getMinTicks())));
    }
}
