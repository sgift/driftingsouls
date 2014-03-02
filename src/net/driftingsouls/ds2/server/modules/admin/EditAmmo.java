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

import net.driftingsouls.ds2.server.entities.Ammo;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;

import java.io.IOException;

/**
 * Adminpanel zum Bearbeiten der Munitionswerte.
 * @author Sebastian Gift
 *
 */
@AdminMenuEntry(category = "Items", name = "Munition bearbeiten")
public class EditAmmo extends AbstractEditPlugin<Ammo>
{
	public EditAmmo()
	{
		super(Ammo.class);
	}

	@Override
	protected void update(StatusWriter writer, Ammo ammo) throws IOException
	{
		Context context = ContextMap.getContext();
		Request request = context.getRequest();
		ammo.setAreaDamage(request.getParameterInt("area"));
		ammo.setShotsPerShot(request.getParameterInt("shotspershot"));
		ammo.setFlags(request.getParameterInt("flags"));
		ammo.setDestroyable(Double.valueOf(request.getParameterString("destroyable")));
		ammo.setSubDamage(request.getParameterInt("subdamage"));
		ammo.setShieldDamage(request.getParameterInt("sdamage"));
		ammo.setDamage(request.getParameterInt("damage"));
		ammo.setTorpTrefferWS(request.getParameterInt("ttws"));
		ammo.setSubWS(request.getParameterInt("subtws"));
		ammo.setSmallTrefferWS(request.getParameterInt("stws"));
		ammo.setTrefferWS(request.getParameterInt("tws"));
		ammo.setPicture(request.getParameterString("picture"));
		ammo.setType(request.getParameterString("type"));
		ammo.setName(request.getParameterString("name"));
	}

	@Override
	protected void edit(EditorForm form, Ammo ammo)
	{
		form.field("Name", "name", String.class, ammo.getName());
		form.field("Bild", "picture", String.class, ammo.getPicture());
		form.field("Typ", "type", String.class, ammo.getType());
		form.field("Treffer-WS", "tws", Integer.class, ammo.getTrefferWS());
		form.field("Small Treffer-WS", "stws", Integer.class, ammo.getSmallTrefferWS());
		form.field("Torp Treffer-WS", "ttws", Integer.class, ammo.getTorpTrefferWS());
		form.field("Subsystem Treffer-WS", "subtws", Integer.class, ammo.getSubWS());
		form.field("Schaden", "damage", Integer.class, ammo.getDamage());
		form.field("Schildschaden", "sdamage", Integer.class, ammo.getShieldDamage());
		form.field("Subsystemschaden", "subdamage", Integer.class, ammo.getSubDamage());
		form.field("Zerstoerbar", "destroyable", Double.class, ammo.getDestroyable());
		form.field("Flags", "flags", Integer.class, ammo.getFlags());
		form.field("Schüsse pro Schuss", "shotspershot", Integer.class, ammo.getShotsPerShot());
		form.field("Flächenschaden", "area", Integer.class, ammo.getAreaDamage());
	}
}
