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
import net.driftingsouls.ds2.server.entities.Munitionsdefinition;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import javax.annotation.Nonnull;

/**
 * Adminpanel zum Bearbeiten der Munitionswerte.
 * @author Sebastian Gift
 *
 */
@AdminMenuEntry(category = "Items", name = "Munition", permission = WellKnownAdminPermission.EDIT_AMMO)
public class EditAmmo implements EntityEditor<Munitionsdefinition>
{
	@Override
	public Class<Munitionsdefinition> getEntityType()
	{
		return Munitionsdefinition.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<Munitionsdefinition> form)
	{
		form.allowAdd();
		form.field("Name", String.class, Munitionsdefinition::getName, Munitionsdefinition::setName);
		form.field("Typ", String.class, Munitionsdefinition::getType, Munitionsdefinition::setType);
		form.field("Treffer-WS", Integer.class, Munitionsdefinition::getTrefferWS, Munitionsdefinition::setTrefferWS);
		form.field("Small Treffer-WS", Integer.class, Munitionsdefinition::getSmallTrefferWS, Munitionsdefinition::setSmallTrefferWS);
		form.field("Torp Treffer-WS", Integer.class, Munitionsdefinition::getTorpTrefferWS, Munitionsdefinition::setTorpTrefferWS);
		form.field("Subsystem Treffer-WS", Integer.class, Munitionsdefinition::getSubWS, Munitionsdefinition::setSubWS);
		form.field("Schaden", Integer.class, Munitionsdefinition::getDamage, Munitionsdefinition::setDamage);
		form.field("Schildschaden", Integer.class, Munitionsdefinition::getShieldDamage, Munitionsdefinition::setShieldDamage);
		form.field("Subsystemschaden", Integer.class, Munitionsdefinition::getSubDamage, Munitionsdefinition::setSubDamage);
		form.field("Zerstoerbar", Double.class, Munitionsdefinition::getDestroyable, Munitionsdefinition::setDestroyable);
		form.multiSelection("Flags", Munitionsdefinition.Flag.class, Munitionsdefinition::getFlags, Munitionsdefinition::setFlags);
		form.field("Schüsse pro Schuss", Integer.class, Munitionsdefinition::getShotsPerShot, Munitionsdefinition::setShotsPerShot);
		form.field("Flächenschaden", Integer.class, Munitionsdefinition::getAreaDamage, Munitionsdefinition::setAreaDamage);
	}
}
