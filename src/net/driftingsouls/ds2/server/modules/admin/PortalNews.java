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

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.modules.admin.AdminMenuEntry;
import net.driftingsouls.ds2.server.modules.admin.AdminPlugin;

/**
 * Ermoeglicht das Verfassen von neuen News im Portal 
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Portal", name="News schreiben")
public class PortalNews implements AdminPlugin {

	public void output(AdminController controller, String page, int action) {
		Context context = ContextMap.getContext();
		StringBuffer echo = context.getResponse().getContent();
		
		String news = context.getRequest().getParameterString("news");
		String title = context.getRequest().getParameterString("title");
		
		Database db = context.getDatabase();
		
		if( (news.length() == 0) || (title.length() == 0) ) {
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("Titel: <input type=\"text\" name=\"title\" size=\"50\" /><br />");
			echo.append("<textarea name=\"news\" rows=\"20\" cols=\"50\"></textarea><br />");
			echo.append("<input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<center><input type=\"submit\" value=\"senden\" style=\"width:200px\" /></center>");
			echo.append("</form>");
		}
		else {
			String username = Common._title(context.getActiveUser().getName());
			long timestamp = Common.time();
			db.prepare("INSERT INTO portal_news (title,author,date,txt) " +
					"VALUES ( ?, ?, ?, ?)")
				.update(title, username, timestamp, news);
			echo.append("News hinzugef&uuml;gt<br />");
		}
	}
}
