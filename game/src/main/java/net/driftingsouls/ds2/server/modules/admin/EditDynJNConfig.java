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

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.config.DynamicJumpNodeConfig;
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
		form.field("Start (Format: system:x/y", String.class, (c -> c.getInitialStart() != null ? c.getInitialStart().asString() : ""), (s, v) -> s.setInitialStart(!"".equals(v) ? Location.fromString(v) : new Location(0, 0, 0)));
		form.field("Ziel (Format: system:x/y", String.class, (c -> c.getInitialTarget() != null ? c.getInitialTarget().asString() : ""), (s, v) -> s.setInitialTarget(!"".equals(v) ? Location.fromString(v) : new Location(0, 0, 0)));
        form.field("Maximale Distanz zum Start", Integer.class, DynamicJumpNodeConfig::getMaxDistanceToInitialStart, DynamicJumpNodeConfig::setMaxDistanceToInitialStart);
        form.field("Maximale Distanz zum Ziel", Integer.class, DynamicJumpNodeConfig::getMaxDistanceToInitialTarget, DynamicJumpNodeConfig::getMaxDistanceToInitialTarget);
        form.field("Minimale Laufzeit", Integer.class, DynamicJumpNodeConfig::getMinLifetime, DynamicJumpNodeConfig::setMinLifetime);
        form.field("Maximale Laufzeit", Integer.class, DynamicJumpNodeConfig::getMaxLifetime, DynamicJumpNodeConfig::setMaxLifetime);
        form.field("Minimale Dauer an einem Punkt", Integer.class, DynamicJumpNodeConfig::getMinNextMovementDelay, DynamicJumpNodeConfig::setMinNextMovementDelay);
        form.field("Maximale Dauer an einem Punkt", Integer.class, DynamicJumpNodeConfig::getMaxNextMovementDelay, DynamicJumpNodeConfig::setMaxNextMovement);

        form.postAddTask("Richte neuen dynamischen JN ein", DynamicJumpNodeConfig::spawnJumpNode);
        form.preDeleteTask("Entferne dynamischen JN", DynamicJumpNodeConfig::removeJumpNode);
        form.postUpdateTask("Aktualisiere dynamischen Jumpnode", (oldDynamicJumpNodeConfig, dynamicJumpNodeConfig) -> {
			dynamicJumpNodeConfig.removeJumpNode();
			dynamicJumpNodeConfig.spawnJumpNode();
		});
	}
}
