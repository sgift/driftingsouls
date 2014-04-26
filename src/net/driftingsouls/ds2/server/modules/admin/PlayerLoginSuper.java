/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
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
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.authentication.AuthenticationException;
import net.driftingsouls.ds2.server.framework.authentication.AuthenticationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;

/**
 * Ermoeglicht das Einloggen in einen anderen Account ohne Passwort.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Spieler", name="Masterlogin", permission = WellKnownAdminPermission.PLAYER_LOGIN_SUPER)
public class PlayerLoginSuper implements AdminPlugin {
	private AuthenticationManager authManager;
	
	/**
	 * Injiziert den DS-AuthenticationManager zum einloggen von Benutzern.
	 * @param authManager Der AuthenticationManager
	 */
	@Autowired
	@Required
	public void setAuthenticationManager(AuthenticationManager authManager) {
		this.authManager = authManager;
	}
	
	@Override
	public void output(StringBuilder echo) throws IOException {
		Context context = ContextMap.getContext();

		int user = context.getRequest().getParameterInt("user");
		int usesessid = context.getRequest().getParameterInt("usesessid");
		
		if( user == 0 ) {
			echo.append("<div class=\"gfxbox\" style=\"width:300px\">");
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("ID: <input type=\"text\" name=\"user\" size=\"10\" value=\"0\" />\n");
			echo.append("<br /><br />\n");
			echo.append("<input type=\"checkbox\" name=\"usesessid\" id=\"form[usesessid]\" value=\"1\" /><label for=\"usesessid\">Rechte vererben?</label><br /><br />\n");
			echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"Login\" style=\"width:100px\" />");
			echo.append("</form>");
			echo.append("</div>");
		}
		else {
			User userObj = (User)context.getDB().get(User.class, user);
			if( userObj == null ) {
				echo.append("<span style=\"color:red\">Der angegebene Spieler existiert nicht</span>");
				return;
			}

			int currentAccessLevel = context.getActiveUser().getAccessLevel();

			if( userObj.getAccessLevel() > currentAccessLevel )
			{
				echo.append("<span style=\"color:red\">Du hast nicht die Berechtigung dich in diesen Account einzuloggen</span>");
				return;
			}

			if( userObj.getPermissions().stream().anyMatch(p -> !context.hasPermission(p)) )
			{
				echo.append("<span style=\"color:red\">Der angegebene Benutzer hat mehr Rechte als du. Login abgelehnt.</span>");
				return;
			}
			
			try {
				this.authManager.adminLogin(userObj, usesessid != 0);
				
				echo.append("<a class=\"ok\" target=\"_blank\" href=\"./ds?module=main\">Zum Account</a>\n");
			}
			catch( AuthenticationException e ) {
				echo.append("<span style=\"color:red\">").append(e.getMessage()).append("</span>");
			}
		}
	}

}
