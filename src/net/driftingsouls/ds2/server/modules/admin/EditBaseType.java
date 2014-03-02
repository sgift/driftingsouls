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

import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import java.io.IOException;

/**
 * Aktualisierungstool fuer die Basis-Klassen.
 * 
 */
@AdminMenuEntry(category = "Asteroiden", name = "Basis-Klasse editieren")
public class EditBaseType extends AbstractEditPlugin<BaseType> implements AdminPlugin
{
	public EditBaseType()
	{
		super(BaseType.class);
	}

	@Override
	protected void update(StatusWriter writer, BaseType type) throws IOException
	{
		Context context = ContextMap.getContext();
		type.setName(context.getRequest().getParameterString("name"));
		type.setEnergy(context.getRequest().getParameterInt("energie"));
		type.setCargo(context.getRequest().getParameterInt("cargo"));
		type.setWidth(context.getRequest().getParameterInt("width"));
		type.setHeight(context.getRequest().getParameterInt("height"));
		type.setMaxTiles(context.getRequest().getParameterInt("maxtiles"));
		type.setSize(context.getRequest().getParameterInt("size"));
		type.setTerrain(Common.explodeToInteger(";", context.getRequest().getParameterString("terrain")));
		type.setSpawnableRess(context.getRequest().getParameterString("spawnableress"));
	}

	@Override
	protected void edit(EditorForm form, BaseType type)
	{
		form.editField("Name", "name", String.class, type.getName());
		form.editField("Energie", "energie", Integer.class, type.getEnergy());
		form.editField("Cargo", "cargo", Integer.class, type.getCargo());
		form.editField("Breite", "width", Integer.class, type.getWidth());
		form.editField("Höhe", "height", Integer.class, type.getHeight());
		form.editField("Max. Feldanzahl", "maxtiles", Integer.class, type.getMaxTiles());
		form.editField("Radius", "size", Integer.class, type.getSize());
		form.editField("Terrain", "terrain", String.class, (type.getTerrain() == null ? "" : Common.implode(";", type.getTerrain())));
		form.editField("Zum Spawn freigegebene Ressourcen", "spawnableress", String.class, type.getSpawnableRess());
	}
}
