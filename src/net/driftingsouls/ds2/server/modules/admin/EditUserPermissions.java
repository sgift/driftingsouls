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
import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Permission;
import net.driftingsouls.ds2.server.framework.PermissionDescriptor;
import net.driftingsouls.ds2.server.framework.PermissionResolver;
import net.driftingsouls.ds2.server.framework.authentication.AccessLevelPermissionResolver;
import net.driftingsouls.ds2.server.framework.authentication.PermissionDelegatePermissionResolver;

import java.io.IOException;

/**
 * Aktualisierungstool fuer die Permissions eines Spielers.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Spieler", name = "Berechtigungen", permission = WellKnownAdminPermission.EDIT_USER_PERMISSIONS)
public class EditUserPermissions implements AdminPlugin
{
	@Override
	public void output(StringBuilder echo) throws IOException
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		int userid = context.getRequest().getParameterInt("userid");

		// Update values?
		String type = context.getRequest().getParameterString("type");

		echo.append("<div class='gfxbox' style='width:400px'>");
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("User: <input type=\"text\" name=\"userid\" value=\"").append(userid).append("\" />\n");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form>");
		echo.append("</div>");

		User user = null;
		if( userid != 0 )
		{
			user = (User) db.get(User.class, userid);
			if( user != null )
			{
				PermissionResolver userPermissionResolver = new PermissionDelegatePermissionResolver(new AccessLevelPermissionResolver(user.getAccessLevel()), user.getPermissions());

				if (user.getAccessLevel() > context.getActiveUser().getAccessLevel())
				{
					echo.append("<p>Du bist nicht berechtigt diesen Benutzer zu bearbeiten</p>");
				}
				else if ("gameplay".equals(type))
				{
					savePermissionsFromRequest(context, user, userPermissionResolver, WellKnownPermission.values());
				}
				else if ("admin".equals(type))
				{
					savePermissionsFromRequest(context, user, userPermissionResolver, WellKnownAdminPermission.values());
				}
			}
		}

		if(userid != 0)
		{
			if(user == null)
			{
				return;
			}

			PermissionResolver userPermissionResolver = new PermissionDelegatePermissionResolver(new AccessLevelPermissionResolver(user.getAccessLevel()), user.getPermissions());

			permissionEditor(echo, "gameplay", "Normale Permissions", user, userPermissionResolver, WellKnownPermission.values());
			permissionEditor(echo, "admin", "Admin Permissions", user, userPermissionResolver, WellKnownAdminPermission.values());
		}
	}

	private void savePermissionsFromRequest(Context context, User user, PermissionResolver userPermissionResolver, PermissionDescriptor[] permissions)
	{
		for (PermissionDescriptor p : permissions)
		{
			if (!context.hasPermission(p))
			{
				continue;
			}

			String value = context.getRequest().getParameter(p.getCategory()+"_"+p.getAction());
			if( "true".equals(value) && !userPermissionResolver.hasPermission(p) )
			{
				user.getPermissions().add(new Permission(user, p.getCategory(), p.getAction()));
			}
			else if( value == null )
			{
				user.getPermissions().removeIf((perm) -> perm.getCategory().equals(p.getCategory()) && perm.getAction().equals(p.getAction()));
			}
		}
	}

	private void permissionEditor(StringBuilder echo, String mode, String label, User user, PermissionResolver userPermissionResolver, PermissionDescriptor[] permissions)
	{
		Context context = ContextMap.getContext();
		boolean canEdit = user.getAccessLevel() <= context.getActiveUser().getAccessLevel();

		echo.append("<div class='gfxbox' style='width:550px'>");
		echo.append("<h3>").append(label).append("</h3>");
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"hidden\" name=\"type\" value=\"").append(mode).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"userid\" value=\"").append(user.getId()).append("\" />\n");
		echo.append("<table width=\"100%\">");
		echo.append("<thead><tr><th></th><th>Kategorie</th><th>Aktion</th><th></th></tr><thead>");
		echo.append("<tbody>");
		for (PermissionDescriptor p : permissions)
		{
			String paramName = p.getCategory()+"_"+p.getAction();
			echo.append("<tr>");
			echo.append("<td><input type=\"checkbox\" name=\"").append(paramName).append("\" value=\"true\" ")
					.append(userPermissionResolver.hasPermission(p) ? "checked=\"checked\" " : "")
					.append(canEdit && context.hasPermission(p) ? "" : "disabled=\"disabled\" ")
					.append(" /></td>");
			echo.append("<td>").append(p.getCategory()).append("</td><td>").append(p.getAction()).append("</td>");
			echo.append("</tr>");
		}

		echo.append("</tbody>");
		echo.append("</td></tr></tfoot></table>");
		echo.append("<input type='submit' name='change' value='speichern' />");
		echo.append("</form>\n");
		echo.append("</div>");
	}
}
