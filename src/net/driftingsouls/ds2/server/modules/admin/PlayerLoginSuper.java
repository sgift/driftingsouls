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

import org.apache.commons.lang.math.RandomUtils;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Ermoeglicht das Einloggen in einen anderen Account ohne Passwort
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Spieler", name="Masterlogin")
public class PlayerLoginSuper implements AdminPlugin {

	public void output(AdminController controller, String page, int action) {
		Context context = ContextMap.getContext();
		StringBuffer echo = context.getResponse().getContent();
		
		int user = context.getRequest().getParameterInt("user");
		int usesessid = context.getRequest().getParameterInt("usesessid");
		
		Database db = context.getDatabase();
		String sess = context.getSession();

		if( user == 0 ) {
			echo.append("<form action=\"./main.php\" method=\"post\">");
			echo.append("ID: <input type=\"text\" name=\"user\" size=\"10\" value=\"0\" />\n");
			echo.append("<br /><br />\n");
			echo.append("<input type=\"checkbox\" name=\"usesessid\" id=\"form[usesessid]\" value=\"1\" /><label for=\"usesessid\">Rechte vererben?</label><br /><br />\n");
			echo.append("<input type=\"hidden\" name=\"sess\" value=\""+sess+"\" />");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"Login\" style=\"width:100px\" />");
			echo.append("</form>");
		}
		else {
			int uid = user;
			User userObj = context.createUserObject(uid);
			if( userObj.getID() == 0 ) {
				echo.append("<span style=\"color:red\">Der angegebene Spieler existiert nicht</span>");
				return;
			}
			
			String usess = null;
			if( usesessid == 0 ) {
				usess = Common.md5(""+RandomUtils.nextInt(Integer.MAX_VALUE));
				
	  			db.update("INSERT INTO sessions (id,session,ip,lastaction,usegfxpak) VALUES('",uid,"','",usess,"','<",context.getRequest().getRemoteAddress(),">','",Common.time(),"','0')");
			} 
			else {
				usess = Common.md5(""+RandomUtils.nextInt(Integer.MAX_VALUE));
				User currentUser = context.getActiveUser();
				SQLResultRow ownsess = db.first("SELECT session FROM sessions WHERE id='",currentUser.getID(),"' AND attach IS NULL");
				
	  			db.update("INSERT INTO sessions (id,session,ip,lastaction,usegfxpak,attach) VALUES('",uid,"','",usess,"','<"+context.getRequest().getRemoteAddress()+">','"+Common.time()+"','0','",ownsess.getString("sess")+"')");
			}
			echo.append("<a class=\"ok\" target=\"_blank\" href=\"./main.php?sess="+usess+"&module=main\">Zum Account</a>\n");
		}
	}

}
