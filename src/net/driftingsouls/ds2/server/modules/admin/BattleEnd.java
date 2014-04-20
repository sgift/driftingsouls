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

import net.driftingsouls.ds2.server.AdminCommands;
import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import java.io.IOException;
import java.util.List;

/**
 * Ermoeglicht das Beenden von Schlachten.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Sonstiges", name="Schlacht beenden", permission = WellKnownAdminPermission.BATTLE_END)
public class BattleEnd implements AdminPlugin 
{
	@Override
	public void output(StringBuilder echo) throws IOException
	{
		Context context = ContextMap.getContext();

		int battleid = context.getRequest().getParameterInt("battleid");
				
		if( battleid == 0 ) 
		{
			echo.append("<div class='gfxbox' style='width:540px;text-align:center'>");
			
			org.hibernate.Session db = context.getDB();
			
			echo.append("<table class=\"noBorderX\" width=\"100%\">\n");
			List<Battle> battles = Common.cast(db.createQuery("from Battle").list());
			for( Battle battle : battles )
			{
				echo.append("<tr>\n");
				echo.append("<td class=\"noBorderX\">ID ").append(battle.getId()).append("&nbsp;&nbsp;</td>\n");
				echo.append("<td class=\"noBorderX\">").append(battle.getSystem()).append(":").append(battle.getX()).append("/").append(battle.getY()).append("</td>\n");
				
				String commander1;
				String commander2;
				
				if( battle.getAlly(0) != 0 ) 
				{
					Ally ally = (Ally)db.get(Ally.class, battle.getAlly(0));
					commander1 = Common._title(ally.getName());
				}
				else 
				{
					commander1 = Common._title(battle.getCommander(0).getName());
				}
				if( battle.getAlly(1) != 0 ) 
				{
					Ally ally = (Ally)db.get(Ally.class, battle.getAlly(1));
					commander2 = Common._title(ally.getName());
				}
				else 
				{
					commander2 = Common._title(battle.getCommander(1).getName());
				} 
				echo.append("<td class=\"noBorderX\" style=\"text-align:center\">").append(commander1).append("<br />vs<br />").append(commander2).append("</td>\n");
				echo.append("</tr>\n");
			}
			echo.append("</table>\n");
			echo.append("<hr style=\"height:1px; border:0px; background-color:#606060; color:#606060\" />\n");
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("BattleID: <input type=\"text\" name=\"battleid\" value=\"0\" />\n");
			echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");		
			echo.append("<input type=\"submit\" value=\"beenden\" style=\"width:100px\"/>");
			echo.append("</form>");
			
			echo.append("</div>");
		}
		else 
		{
			new AdminCommands().executeCommand("battle end "+battleid);
					
			echo.append("Schlacht beendet<br />");
		}
	}
}
