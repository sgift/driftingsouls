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

import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;
import net.driftingsouls.ds2.server.modules.AdminController;

import org.apache.commons.fileupload.FileItem;

/**
 * Aktualisierungstool fuer Forschungsgrafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Techs", name = "Forschungsgrafik editieren")
public class EditResearchPicture implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int forschungid = context.getRequest().getParameterInt("forschungid");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");

		echo.append("<div class='gfxbox'><form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select name=\"forschungid\" size='1'>\n");
		List<Forschung> forschungen = Common.cast(db.createQuery("from Forschung order by id").list());
		for( Forschung f : forschungen )
		{
			echo.append("<option value='"+f.getID()+"' "+
					(f.getID()==forschungid?"selected='selected'":"")+">"+
					f.getName()+" ("+f.getID()+")</option>");
		}
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form></div>");

		if(update && forschungid != 0)
		{
			Forschung forschung = (Forschung)db.get(Forschung.class, forschungid);

			if(forschung != null) {
				for( FileItem file : context.getRequest().getUploadedFiles() )
				{
					if( "image".equals(file.getFieldName()) && file.getSize() > 0 )
					{
						if( forschung.getImage().startsWith("data/dynamicContent/") )
						{
							DynamicContentManager.remove(forschung.getImage());
						}
						forschung.setImage("data/dynamicContent/"+DynamicContentManager.add(file));
					}
				}

				echo.append("<p>Update abgeschlossen.</p>");
			}
			else {
				echo.append("<p>Keine Forschung gefunden.</p>");
			}

		}

		if(forschungid != 0)
		{
			Forschung forschung = (Forschung)db.get(Forschung.class, forschungid);

			if(forschung == null)
			{
				return;
			}

			echo.append("<div class='gfxbox' style='width:500px'>");
			echo.append("<form action=\"./ds\" method=\"post\" enctype='multipart/form-data'>");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"forschungid\" value=\"" + forschungid + "\" />\n");

			echo.append("<table width=\"100%\">");
			echo.append("<tr><td >Name: </td>" +
					"<td></td>"+
					"<td>"+forschung.getName()+"</td></tr>\n");
			echo.append("<tr><td>Bild: </td>" +
					"<td><img src='"+forschung.getImage()+"' /></td>" +
					"<td><input type=\"file\" name=\"image\"\"></td></tr>\n");

			echo.append("<tr><td></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
			echo.append("</div>");
		}
	}
}
