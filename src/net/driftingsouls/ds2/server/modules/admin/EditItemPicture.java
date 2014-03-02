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
import net.driftingsouls.ds2.server.framework.DynamicContentManager;

import java.io.IOException;

/**
 * Aktualisierungstool fuer Itemgrafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Items", name = "Itemgrafik editieren")
public class EditItemPicture extends AbstractEditPlugin<Item> implements AdminPlugin
{
	public EditItemPicture()
	{
		super(Item.class);
	}

	@Override
	protected void update(StatusWriter statusWriter, Item item) throws IOException
	{
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
	}

	@Override
	protected void edit(EditorForm form, Item item)
	{
		form.label("Name", item.getName());
		form.dynamicContentField("Bild", "picture", item.getPicture());
		form.dynamicContentField("Bild (groß)", "largepicture", item.getLargePicture());
	}
}
