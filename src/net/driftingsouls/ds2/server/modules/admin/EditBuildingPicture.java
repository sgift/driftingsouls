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

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Aktualisierungstool fuer Gebaeudegrafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Asteroiden", name = "Gebäudegrafiken editieren")
public class EditBuildingPicture extends AbstractEditPlugin implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int buildingid = context.getRequest().getParameterInt("entityId");

		// Update values?

		this.beginSelectionBox(echo, page, action);
		List<Building> buildings = Common.cast(db.createQuery("from Building order by id").list());
		for( Building building : buildings )
		{
			this.addSelectionOption(echo, building.getId(), building.getName()+" ("+building.getId()+")");
		}
		this.endSelectionBox(echo);

		if(this.isUpdateExecuted() && buildingid != 0)
		{
			Building building = (Building)db.get(Building.class, buildingid);

			if(building != null) {
				Map<Integer,String> altBilder = building.getAlternativeBilder();

				String buildingImg = processDynamicContent("picture", building.getDefaultPicture());
				if( buildingImg != null )
				{
					String oldImg = building.getDefaultPicture();
					building.setDefaultPicture("data/dynamicContent/"+buildingImg);
					if( oldImg.startsWith("data/dynamicContent/") )
					{
						DynamicContentManager.remove(building.getDefaultPicture());
					}
				}
				for( Rasse rasse : Rassen.get() )
				{
					String rasseImg = processDynamicContent("rasse"+rasse.getID(), altBilder.get(rasse.getID()));
					if( rasseImg != null )
					{
						String curPicture = altBilder.get(rasse.getID());
						altBilder.put(rasse.getID(), "data/dynamicContent/"+rasseImg);
						if( curPicture != null && curPicture.startsWith("data/dynamicContent/"))
						{
							DynamicContentManager.remove(curPicture);
						}
					}
				}

				echo.append("<p>Update abgeschlossen.</p>");
			}
			else
			{
				echo.append("<p>Kein Gebäude gefunden.</p>");
			}
		}
		else if( isResetExecuted() && buildingid != 0 )
		{
			Building building = (Building)db.get(Building.class, buildingid);
			for( Rasse rasse : Rassen.get() )
			{
				if( isResetted("rasse"+rasse.getID()) )
				{
					String value = building.getAlternativeBilder().remove(rasse.getID());
					if(value != null)
					{
						DynamicContentManager.remove(value);
					}

					echo.append("<p>Update abgeschlossen.</p>");
					break;
				}
			}
		}

		if(buildingid != 0)
		{
			Building building = (Building)db.get(Building.class, buildingid);

			if(building == null)
			{
				return;
			}

			this.beginEditorTable(echo, page, action, buildingid);

			this.editLabel(echo, "Name", building.getName());
			this.editDynamicContentField(echo, "Bild", "picture", building.getDefaultPicture());

			Map<Integer,String> altBilder = building.getAlternativeBilder();
			for( Rasse rasse : Rassen.get() )
			{
				this.editDynamicContentFieldWithRemove(echo, rasse.getName(), "rasse"+rasse.getID(), altBilder.get(rasse.getID()));
			}

			this.endEditorTable(echo);
		}
	}
}
