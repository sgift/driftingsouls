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

import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.Ammo;
import net.driftingsouls.ds2.server.modules.admin.editoren.AbstractEditPlugin8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;

import javax.annotation.Nonnull;

/**
 * Adminpanel zum Bearbeiten der Munitionswerte.
 * @author Sebastian Gift
 *
 */
@AdminMenuEntry(category = "Items", name = "Munition")
public class EditAmmo extends AbstractEditPlugin8<Ammo>
{
	public EditAmmo()
	{
		super(Ammo.class);
	}

	@Override
	protected void configureFor(@Nonnull EditorForm8<Ammo> form)
	{
		form.allowAdd();
		form.field("Name", String.class, Ammo::getName, Ammo::setName);
		form.field("Typ", String.class, Ammo::getType, Ammo::setType);
		form.field("Treffer-WS", Integer.class, Ammo::getTrefferWS, Ammo::setTrefferWS);
		form.field("Small Treffer-WS", Integer.class, Ammo::getSmallTrefferWS, Ammo::setSmallTrefferWS);
		form.field("Torp Treffer-WS", Integer.class, Ammo::getTorpTrefferWS, Ammo::setTorpTrefferWS);
		form.field("Subsystem Treffer-WS", Integer.class, Ammo::getSubWS, Ammo::setSubWS);
		form.field("Schaden", Integer.class, Ammo::getDamage, Ammo::setDamage);
		form.field("Schildschaden", Integer.class, Ammo::getShieldDamage, Ammo::setShieldDamage);
		form.field("Subsystemschaden", Integer.class, Ammo::getSubDamage, Ammo::setSubDamage);
		form.field("Zerstoerbar", Double.class, Ammo::getDestroyable, Ammo::setDestroyable);
		form.field("Flags", Integer.class, Ammo::getFlags, Ammo::setFlags);
		form.field("Schüsse pro Schuss", Integer.class, Ammo::getShotsPerShot, Ammo::setShotsPerShot);
		form.field("Flächenschaden", Integer.class, Ammo::getAreaDamage, Ammo::setAreaDamage);
		form.field("Zugehoeriges Item", Item.class, Integer.class, Ammo::getItemId, Ammo::setItemId);
	}
}
