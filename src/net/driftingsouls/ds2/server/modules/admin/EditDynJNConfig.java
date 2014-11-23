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
import net.driftingsouls.ds2.server.config.DynamicJumpNodeConfig;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import javax.annotation.Nonnull;

/**
 * Aktualisierungstool fuer die Konfiguration der dynamischen JNs.
 *
 */
@AdminMenuEntry(category = "Dyn JN", name = "Config", permission = WellKnownAdminPermission.EDIT_DYN_JN)
public class EditDynJNConfig implements EntityEditor<DynamicJumpNodeConfig>
{
	@Override
	public Class<DynamicJumpNodeConfig> getEntityType()
	{
		return DynamicJumpNodeConfig.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<DynamicJumpNodeConfig> form)
	{
        form.allowAdd();
        form.allowDelete();
		form.multiSelection("Startsysteme", StarSystem.class, DynamicJumpNodeConfig::getStartSystems, DynamicJumpNodeConfig::setStartSystems);
        form.multiSelection("Zielsysteme", StarSystem.class, DynamicJumpNodeConfig::getZielSystems, DynamicJumpNodeConfig::setZielSystems);
        form.field("Reichweite des Einganges", Integer.class, DynamicJumpNodeConfig::getInRange, DynamicJumpNodeConfig::setInRange);
        form.field("Reichweite des Ausganges", Integer.class, DynamicJumpNodeConfig::getOutRange, DynamicJumpNodeConfig::setOutRange);
        form.field("Minimale Laufzeit", Integer.class, DynamicJumpNodeConfig::getMinDauer, DynamicJumpNodeConfig::setMinDauer);
        form.field("Maximale Laufzeit", Integer.class, DynamicJumpNodeConfig::getMaxDauer, DynamicJumpNodeConfig::setMaxDauer);
        form.field("Minimale Dauer an einem Punkt", Integer.class, DynamicJumpNodeConfig::getMinNextMovement, DynamicJumpNodeConfig::setMinNextMovement);
        form.field("Maximale Dauer an einem Punkt", Integer.class, DynamicJumpNodeConfig::getMaxNextMovement, DynamicJumpNodeConfig::setMaxNextMovement);
	}


}
