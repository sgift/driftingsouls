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

import org.apache.commons.fileupload.FileItem;

/**
 * Aktualisierungstool fuer Gebaeudegrafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Asteroiden", name = "Gebäudegrafiken editieren")
public class EditBuildingPicture implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int buildingid = context.getRequest().getParameterInt("buildingid");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");
		boolean delete = !context.getRequest().getParameterString("reset").isEmpty();

		echo.append("<div class='gfxbox'><form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select name=\"buildingid\" size='1'>\n");
		List<Building> buildings = Common.cast(db.createQuery("from Building order by id").list());
		for( Building building : buildings )
		{
			echo.append("<option value='"+building.getId()+"' "+
					(building.getId()==buildingid?"selected='selected'":"")+">"+
					building.getName()+" ("+building.getId()+")</option>");
		}
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form></div>");

		if(update && buildingid != 0)
		{
			Building building = (Building)db.get(Building.class, buildingid);

			if(building != null) {
				Map<Integer,String> altBilder = building.getAlternativeBilder();

				for( FileItem file : context.getRequest().getUploadedFiles() )
				{
					if( "picture".equals(file.getFieldName()) && file.getSize() > 0 )
					{
						if( building.getDefaultPicture().startsWith("data/dynamicContent/") )
						{
							DynamicContentManager.remove(building.getDefaultPicture());
						}
						building.setDefaultPicture("data/dynamicContent/"+DynamicContentManager.add(file));
					}
					for( Rasse rasse : Rassen.get() )
					{
						String key = "rasse"+rasse.getID()+"_picture";
						if( key.equals(file.getFieldName()) && file.getSize() > 0 )
						{
							String curPicture = altBilder.get(rasse.getID());
							if( curPicture != null && curPicture.startsWith("data/dynamicContent/"))
							{
								DynamicContentManager.remove(curPicture);
							}
							altBilder.put(rasse.getID(), "data/dynamicContent/"+DynamicContentManager.add(file));
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
		else if( delete && buildingid != 0 )
		{
			int rasse = context.getRequest().getParameterInt("reset");
			Building building = (Building)db.get(Building.class, buildingid);
			if(building != null) {
				building.getAlternativeBilder().remove(rasse);

				echo.append("<p>Update abgeschlossen.</p>");
			}
			else
			{
				echo.append("<p>Kein Gebäude gefunden.</p>");
			}
		}

		if(buildingid != 0)
		{
			Building building = (Building)db.get(Building.class, buildingid);

			if(building == null)
			{
				return;
			}

			echo.append("<div class='gfxbox' style='width:500px'>");
			echo.append("<form action=\"./ds\" method=\"post\" enctype='multipart/form-data'>");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"buildingid\" value=\"" + buildingid + "\" />\n");

			echo.append("<table width=\"100%\">");
			echo.append("<tr><td>Name:</td>" +
					"<td></td>"+
					"<td>"+building.getName()+"</td></tr>\n");
			echo.append("<tr><td>Bild: </td>" +
					"<td><img src='"+building.getDefaultPicture()+"' /></td>" +
					"<td><input type=\"file\" name=\"picture\"\"></td></tr>\n");

			Map<Integer,String> altBilder = building.getAlternativeBilder();
			for( Rasse rasse : Rassen.get() )
			{
				echo.append("<tr><td>"+rasse.getName()+": </td>");
				if( altBilder.containsKey(rasse.getID())  )
				{
					echo.append("<td><img src='"+altBilder.get(rasse.getID())+"' /></td>");
				}
				else
				{
					echo.append("<td></td>");
				}
				echo.append("<td><input type=\"file\" name=\"rasse"+rasse.getID()+"_picture\" ></td>");
				echo.append("<td><a title='entfernen' href='./ds?module=admin&amp;page="+page+"&amp;act="+action+"&amp;buildingid="+buildingid+"&reset="+rasse.getID()+"'>X</a>");
				echo.append("</tr>\n");
			}


			echo.append("<tr><td></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
			echo.append("</div>");
		}
	}
}
