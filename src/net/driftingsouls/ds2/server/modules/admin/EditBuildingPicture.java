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
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;

import java.io.IOException;
import java.util.Map;

/**
 * Aktualisierungstool fuer Gebaeudegrafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Asteroiden", name = "Gebäudegrafiken editieren", permission = WellKnownAdminPermission.EDIT_BUILDING_PICTURE)
public class EditBuildingPicture extends AbstractEditPlugin<Building> implements AdminPlugin
{
	public EditBuildingPicture()
	{
		super(Building.class);
	}

	@Override
	protected void reset(StatusWriter writer, Building building) throws IOException
	{
		for( Rasse rasse : Rassen.get() )
		{
			if( isResetted("rasse"+rasse.getId()) )
			{
				String value = building.getAlternativeBilder().remove(rasse.getId());
				if(value != null)
				{
					DynamicContentManager.remove(value);
				}

				break;
			}
		}
	}

	@Override
	protected void update(StatusWriter writer, Building building) throws IOException
	{
		Map<Integer,String> altBilder = building.getAlternativeBilder();

		String buildingImg = processDynamicContent("picture", building.getDefaultPicture());
		if( buildingImg != null )
		{
			String oldImg = building.getDefaultPicture();
			building.setDefaultPicture("data/dynamicContent/"+buildingImg);
			if( oldImg.startsWith("data/dynamicContent/") )
			{
				DynamicContentManager.remove(oldImg);
			}
		}
		for( Rasse rasse : Rassen.get() )
		{
			String rasseImg = processDynamicContent("rasse"+rasse.getId(), altBilder.get(rasse.getId()));
			if( rasseImg != null )
			{
				String curPicture = altBilder.get(rasse.getId());
				altBilder.put(rasse.getId(), "data/dynamicContent/"+rasseImg);
				if( curPicture != null && curPicture.startsWith("data/dynamicContent/"))
				{
					DynamicContentManager.remove(curPicture);
				}
			}
		}
	}

	@Override
	protected void edit(EditorForm form, Building building)
	{
		form.label("Name", building.getName());
		form.dynamicContentField("Bild", "picture", building.getDefaultPicture());

		Map<Integer,String> altBilder = building.getAlternativeBilder();
		for( Rasse rasse : Rassen.get() )
		{
			form.dynamicContentField(rasse.getName(), "rasse" + rasse.getId(), altBilder.get(rasse.getId())).withRemove();
		}
	}
}
