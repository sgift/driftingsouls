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

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Permission;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Aktualisierungstool fuer die Permissions eines Spielers.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Spieler", name = "Berechtigungen")
public class EditUserPermissions implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int userid = context.getRequest().getParameterInt("userid");
		String paction = context.getRequest().getParameterString("paction");
		String pcategory = context.getRequest().getParameterString("pcategory");

		// Update values?
		boolean add = context.getRequest().getParameterString("change").equals("hinzufügen");
		boolean delete = context.getRequest().getParameterString("change").equals("löschen");

		echo.append("<div class='gfxbox' style='width:400px'>");
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("User: <input type=\"text\" name=\"userid\" value=\""+ userid +"\" />\n");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form>");
		echo.append("</div>");


		if(delete && userid != 0)
		{
			User user = (User)db.get(User.class, userid);

			for( Permission p : user.getPermissions() )
			{
				if( pcategory.equals(p.getCategory()) && paction.equals(p.getAction()) )
				{
					db.delete(p);
					user.getPermissions().remove(p);

					echo.append("<p>Delete abgeschlossen.</p>");

					break;
				}
			}
		}
		else if( add && userid != 0 )
		{
			User user = (User)db.get(User.class, userid);
			Permission p = new Permission(user, pcategory, paction);
			if( user.getPermissions().add(p) )
			{
				db.persist(p);
				echo.append("<p>Add abgeschlossen.</p>");
			}
		}

		if(userid != 0)
		{
			User user = (User)db.get(User.class, userid);

			if(user == null)
			{
				return;
			}

			echo.append("<div class='gfxbox' style='width:650px'>");
			echo.append("<table width=\"100%\">");
			echo.append("<thead><tr><th>Kategorie</th><th>Aktion</th><th></th></tr><thead>");
			echo.append("<tbody>");
			for( Permission p : user.getPermissions() )
			{
				echo.append("<tr><td>"+p.getCategory()+"</td><td>"+p.getAction()+"</td>");
				echo.append("<td>");
				echo.append("<form action=\"./ds\" method=\"post\">");
				echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
				echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
				echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
				echo.append("<input type=\"hidden\" name=\"userid\" value=\"" + userid + "\" />\n");
				echo.append("<input type=\"hidden\" name=\"paction\" value=\"" + p.getAction() + "\" />\n");
				echo.append("<input type=\"hidden\" name=\"pcategory\" value=\"" + p.getCategory() + "\" />\n");
				echo.append("<input type='submit' name='change' value='löschen' />");
				echo.append("</form>");
				echo.append("</td></tr>");
			}
			echo.append("</tbody>");
			echo.append("<tfoot>");
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<tr><td>");
			echo.append("<input type='text' name='pcategory' value='' />");
			echo.append("</td>");
			echo.append("<td>");
			echo.append("<input type='text' name='paction' value='' />");
			echo.append("</td>");
			echo.append("<td>");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"userid\" value=\"" + userid + "\" />\n");
			echo.append("<input type='submit' name='change' value='hinzufügen' />");
			echo.append("</form>\n");
			echo.append("</td></tr></tfoot></table>");
			echo.append("</div>");
		}
	}
}
