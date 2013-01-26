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
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Aktualisierungstool fuer Itemgrafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Items", name = "Itemgrafik editieren")
public class EditItemPicture extends AbstractEditPlugin implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int itemid = context.getRequest().getParameterInt("entityId");

		this.beginSelectionBox(echo, page, action);
		List<Item> items = Common.cast(db.createQuery("from Item order by id").list());
		for( Item item : items )
		{
			this.addSelectionOption(echo, item.getID(), item.getName()+"( "+item.getID()+")");
		}
		this.endSelectionBox(echo);

		if(isUpdateExecuted() && itemid != 0)
		{
			Item item = (Item)db.get(Item.class, itemid);

			if(item != null) {
				String newimg = this.processDynamicContent("picture", item.getPicture());
				if( newimg != null )
				{
					String oldImg = item.getPicture();
					item.setPicture("data/dynamicContent/"+newimg);
					if( oldImg.startsWith("data/dynamicContent/") )
					{
						DynamicContentManager.remove(oldImg);
					}
				}
				String newlargeimg = this.processDynamicContent("largepicture", item.getLargePicture());
				if( newlargeimg != null )
				{
					String oldImg = item.getLargePicture();
					item.setLargePicture("data/dynamicContent/"+newlargeimg);
					if( oldImg != null && oldImg.startsWith("data/dynamicContent/") )
					{
						DynamicContentManager.remove(oldImg);
					}
				}

				echo.append("<p>Update abgeschlossen.</p>");
			}
			else {
				echo.append("<p>Kein Item gefunden.</p>");
			}

		}

		if(itemid != 0)
		{
			Item item = (Item)db.get(Item.class, itemid);

			if(item == null)
			{
				return;
			}

			this.beginEditorTable(echo, page, action, item.getID());
			this.editLabel(echo, "Name", item.getName());
			this.editDynamicContentField(echo, "Bild", "picture", item.getPicture());
			this.editDynamicContentField(echo, "Bild (groß)", "largepicture", item.getLargePicture());
			this.endEditorTable(echo);
		}
	}
}
