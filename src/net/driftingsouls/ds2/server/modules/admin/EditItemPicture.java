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

import org.apache.commons.fileupload.FileItem;

/**
 * Aktualisierungstool fuer Itemgrafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Items", name = "Itemgrafik editieren")
public class EditItemPicture implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int itemid = context.getRequest().getParameterInt("itemid");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");

		echo.append("<div class='gfxbox'><form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select name=\"itemid\" size='1'>\n");
		List<Item> items = Common.cast(db.createQuery("from Item order by id").list());
		for( Item item : items )
		{
			echo.append("<option value='"+item.getID()+"' "+
					(item.getID()==itemid?"selected='selected'":"")+">"+
					item.getName()+" ("+item.getID()+")</option>");
		}
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form></div>");

		if(update && itemid != 0)
		{
			Item item = (Item)db.get(Item.class, itemid);

			if(item != null) {
				for( FileItem file : context.getRequest().getUploadedFiles() )
				{
					if( "picture".equals(file.getFieldName()) && file.getSize() > 0 )
					{
						String oldImg = item.getPicture();
						item.setPicture("data/dynamicContent/"+DynamicContentManager.add(file));
						if( oldImg.startsWith("data/dynamicContent/") )
						{
							DynamicContentManager.remove(oldImg);
						}
					}
					if( "largepicture".equals(file.getFieldName()) && file.getSize() > 0 )
					{
                        String oldImg = item.getLargePicture();
						item.setLargePicture("data/dynamicContent/"+DynamicContentManager.add(file));
						if( oldImg != null && oldImg.startsWith("data/dynamicContent/") )
						{
							DynamicContentManager.remove(oldImg);
						}
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

			echo.append("<div class='gfxbox' style='width:500px'>");
			echo.append("<form action=\"./ds\" method=\"post\" enctype='multipart/form-data'>");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"itemid\" value=\"" + itemid + "\" />\n");

			echo.append("<table width=\"100%\">");
			echo.append("<tr><td >Name: </td>" +
					"<td></td>"+
					"<td>"+item.getName()+"</td></tr>\n");
			echo.append("<tr><td>Bild: </td>" +
					"<td><img src='"+item.getPicture()+"' /></td>" +
					"<td><input type=\"file\" name=\"picture\"\"></td></tr>\n");

			echo.append("<tr><td>Bild (gro&szlig;): </td>");
			if( item.getLargePicture() != null )
			{
				echo.append("<td><img src='"+item.getLargePicture()+"' /></td>");
			}
			else
			{
				echo.append("<td></td>");
			}
			echo.append("<td><input type=\"file\" name=\"largepicture\" ></td></tr>\n");

			echo.append("<tr><td></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
			echo.append("</div>");
		}
	}
}
