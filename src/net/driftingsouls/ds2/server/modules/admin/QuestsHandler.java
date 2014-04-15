/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.scripting.entities.RunningQuest;
import net.driftingsouls.ds2.server.ships.Ship;
import org.hibernate.Session;

import java.io.IOException;
import java.util.List;

/**
 * Ermoeglicht das Bearbeiten von Quest-Handlern.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Quests", name="Handler")
public class QuestsHandler implements AdminPlugin {
	@Override
	public void output(StringBuilder echo) throws IOException {
		Context context = ContextMap.getContext();

		int save = context.getRequest().getParameterInt("save");
		String event = context.getRequest().getParameterString("event");
		String oid = context.getRequest().getParameterString("oid");
		String handler = context.getRequest().getParameterString("handler");

		Session db = context.getDB();

		final String URLBASE = "./ds?module=admin&namedplugin="+getClass().getName();

		if( event.length() == 0 ) {
			echo.append("<div class='gfxbox' style='width:740px;text-align:center'>");
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<select size=\"1\" name=\"event\">\n");
			echo.append("<option value=\"oncommunicate\">oncommunicate</option>\n");
			echo.append("<option value=\"ontick_rquest\">ontick (rQuest)</option>\n");
			echo.append("<option value=\"onendbattle\">onendbattle</option>\n");
			echo.append("</select>\n");
			echo.append("<input type=\"text\" name=\"oid\" value=\"object-id\" />\n");
			echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"bearbeiten\" />\n");
			echo.append("</form>\n");

			echo.append("</div>");

			echo.append("<br /><br />\n");

			echo.append("<div class='gfxbox' style='width:740px'>");

			echo.append("oncommunicate:<br />\n");
			List<?> ships = db
					.createQuery("from Ship where id>0 and oncommunicate is not null order by id")
					.list();
			for( Object obj : ships )
			{
				Ship ship = (Ship)obj;
				echo.append("* <a class=\"forschinfo\" href=\"").append(URLBASE).append("&event=oncommunicate&oid=").append(ship.getId()).append("\">");

				User owner = ship.getOwner();

				echo.append(ship.getName()).append(" (").append(ship.getId()).append(") [").append(Common._title(owner.getName())).append("]");

				echo.append("</a><br />\n");
			}

			echo.append("<br />ontick (rQuest):<br />\n");
			List<?> rquests = db
					.createQuery("from RunningQuest rq where rq.onTick is not null order by rq.user.id")
					.list();
			for( Object obj : rquests )
			{
				RunningQuest rquest = (RunningQuest)obj;
				echo.append("* <a class=\"forschinfo\" href=\"").append(URLBASE).append("&event=ontick_rquest&oid=").append(rquest.getId()).append("\">");

				User owner = rquest.getUser();

				echo.append(rquest.getQuest().getName()).append(" (").append(rquest.getId()).append(") [").append(Common._title(owner.getName())).append("]");

				echo.append("</a><br />\n");
			}

			echo.append("<br />onendbattle:<br />\n");
			List<?> battles = db
					.createQuery("from Battle where onend is not null order by system,x,y")
					.list();
			for( Object obj : battles )
			{
				Battle battle = (Battle)obj;
				echo.append("* <a class=\"forschinfo\" href=\"").append(URLBASE).append("&event=onendbattle&oid=").append(battle.getId()).append("\">");

				User com1 = battle.getCommander(0);
				User com2 = battle.getCommander(1);

				echo.append(battle.getSystem()).append(":").append(battle.getX()).append("/").append(battle.getY()).append(" (").append(battle.getId()).append(") [").append(Common._title(com1.getName())).append(" vs ").append(Common._title(com2.getName())).append("]");

				echo.append("</a><br />\n");
			}

			echo.append("</div>");
		}
		else if( save == 0 ) {
			switch (event)
			{
				case "oncommunicate":
					handler = (String) db
							.createQuery("select oncommunicate from Ship where id>0 and id=:id")
							.setInteger("id", Integer.parseInt(oid))
							.uniqueResult();
					break;
				case "ontick_rquest":
					handler = (String) db
							.createQuery("select onTick from RunningQuest where id=:id")
							.setInteger("id", Integer.parseInt(oid))
							.uniqueResult();
					break;
				case "onendbattle":
					handler = (String) db
							.createQuery("select onend from Battle where id=:id")
							.setInteger("id", Integer.parseInt(oid))
							.uniqueResult();
					break;
				default:
					echo.append("WARNUNG: Ung&uuml;ltiges Event &gt;").append(event).append("&lt; <br />\n");
					handler = "";
					event = "";
					oid = "";
					break;
			}

			echo.append("<div class='gfxbox' style='width:740px'>");

			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<input type=\"text\" name=\"handler\" size=\"50\" value=\"").append(handler).append("\" />\n");
			echo.append("<input type=\"hidden\" name=\"event\" value=\"").append(event).append("\" />\n");
			echo.append("<input type=\"hidden\" name=\"oid\" value=\"").append(oid).append("\" />\n");
			echo.append("<input type=\"hidden\" name=\"save\" value=\"1\" />\n");
			echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"bearbeiten\" />\n");
			echo.append("</form>\n");

			echo.append("</div>");
		}
		else {
			switch (event)
			{
				case "oncommunicate":
					Ship ship = (Ship) db
							.createQuery("from Ship where id>0 and id=:id")
							.setInteger("id", Integer.parseInt(oid))
							.uniqueResult();
					if (handler.length() != 0)
					{
						ship.setOnCommunicate(handler);
					}
					else
					{
						ship.setOnCommunicate(null);
					}
					break;
				case "ontick_rquest":
					RunningQuest rquest = (RunningQuest) db
							.createQuery("select onTick from RunningQuest where id=:id")
							.setInteger("id", Integer.parseInt(oid))
							.uniqueResult();
					if (handler.length() != 0)
					{
						rquest.setOnTick(Integer.parseInt(handler));
					}
					else
					{
						rquest.setOnTick(null);
					}
					break;
				case "onendbattle":
					Battle battle = (Battle) db
							.createQuery("select onend from Battle where id=:id")
							.setInteger("id", Integer.parseInt(oid))
							.uniqueResult();
					if (handler.length() != 0)
					{
						battle.setOnEnd(handler);
					}
					else
					{
						battle.setOnEnd(null);
					}
					break;
				default:
					echo.append("WARNUNG: Ung&uuml;ltiges Event &gt;").append(event).append("&lt; <br />\n");
					break;
			}
			echo.append("&Auml;nderungen durchgef&uuml;hrt<br />");
		}
	}
}
