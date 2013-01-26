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
import net.driftingsouls.ds2.server.modules.AdminController;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Aktualisierungstool fuer die Basis-Klassen.
 * 
 */
@AdminMenuEntry(category = "Asteroiden", name = "Basis-Klasse editieren")
public class EditBaseType extends AbstractEditPlugin implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();
		
		int typeid = context.getRequest().getParameterInt("entityId");

		List<BaseType> baseTypes = Common.cast(db.createQuery("from BaseType").list());

		beginSelectionBox(echo, page, action);
		for (BaseType type: baseTypes)
		{
			addSelectionOption(echo, type.getId(), type.getName()+" ("+type.getId()+")");
		}
		endSelectionBox(echo);

		if(this.isUpdateExecuted() && typeid != 0)
		{
			BaseType type = (BaseType)db.get(BaseType.class, typeid);
			
			type.setName(context.getRequest().getParameterString("name"));
			type.setEnergy(context.getRequest().getParameterInt("energie"));
			type.setCargo(context.getRequest().getParameterInt("cargo"));
			type.setWidth(context.getRequest().getParameterInt("width"));
			type.setHeight(context.getRequest().getParameterInt("height"));
			type.setMaxTiles(context.getRequest().getParameterInt("maxtiles"));
			type.setTerrain(Common.explodeToInteger(";", context.getRequest().getParameterString("terrain")));
			type.setSpawnableRess(context.getRequest().getParameterString("spawnableress"));
			
			echo.append("<p>Update abgeschlossen.</p>");
		}
		
		if(typeid != 0)
		{
			BaseType type = (BaseType)db.get(BaseType.class, typeid);
			
			if(type == null)
			{
				return;
			}

			beginEditorTable(echo, page, action, typeid);

			editField(echo, "Name", "name", String.class, type.getName());
			editField(echo, "Energie", "energie", Integer.class, type.getEnergy());
			editField(echo, "Cargo", "cargo", Integer.class, type.getCargo());
			editField(echo, "Breite", "width", Integer.class, type.getWidth());
			editField(echo, "Höhe", "height", Integer.class, type.getHeight());
			editField(echo, "Max. Feldanzahl", "maxtiles", Integer.class, type.getMaxTiles());
			editField(echo, "Terrain", "terrain", String.class, (type.getTerrain() == null ? "" : Common.implode(";", type.getTerrain())));
			editField(echo, "Zum Spawn freigegebene Ressourcen", "spawnableress", String.class, type.getSpawnableRess());

			endEditorTable(echo);
		}
	}
}
