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

import java.math.BigInteger;
import java.util.Map;

import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Aktualisierungstool fuer die Werte eines Spielers
 * 
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Spieler", name = "Spieler editieren")
public class EditUser implements AdminPlugin
{
	public void output(AdminController controller, String page, int action)
	{
		Context context = ContextMap.getContext();
		StringBuffer echo = context.getResponse().getContent();
		org.hibernate.Session db = context.getDB();
		
		int userid = context.getRequest().getParameterInt("userid");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");

		echo.append(Common.tableBegin(350,"left"));
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"sess\" value=\"" + context.getSession() + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("User: <input type=\"text\" name=\"userid\" value=\""+ userid +"\" />\n");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\"");
		echo.append("</form>");
		echo.append(Common.tableEnd());
		
		if(update && userid != 0)
		{
			User user = (User)db.get(User.class, userid);
			
			boolean disableAccount = context.getRequest().getParameterInt("blockuser") == 1 ? true : false;

			user.setDisabled(disableAccount);
			
			user.setNickname(context.getRequest().getParameterString("name"));
			user.setRace(context.getRequest().getParameterInt("race"));
			user.setVacationCount(context.getRequest().getParameterInt("vacation"));
			user.setWait4VacationCount(context.getRequest().getParameterInt("wait4vac"));
			user.setKonto(new BigInteger(context.getRequest().getParameterString("account")));
			user.setFlags(context.getRequest().getParameterString("flags"));
			user.setFoodpooldegeneration(Double.valueOf(context.getRequest().getParameterString("foodpooldegeneration")));
			user.setRang(Byte.valueOf(context.getRequest().getParameterString("rank")));
			user.setHistory(context.getRequest().getParameterString("history"));
			user.setNpcPunkte(context.getRequest().getParameterInt("npcpoints"));
			user.setMedals(context.getRequest().getParameterString("medals"));
			
			echo.append("<p>Update abgeschlossen.</p>");
		}
		
		if(userid != 0)
		{
			User user = (User)db.get(User.class, userid);
			
			if(user == null)
			{
				return;
			}

			echo.append(Common.tableBegin(650,"left"));
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorderX\" width=\"100%\">");
			echo.append("<input type=\"hidden\" name=\"sess\" value=\"" + context.getSession() + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"userid\" value=\"" + userid + "\" />\n");
			echo.append("<tr><td class=\"noBorderX\">Name: </td><td><input type=\"text\" size=\"40\" name=\"name\" value=\"" + user.getNickname() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Rasse: </td><td><select size=\"1\" name=\"race\" \">");
			for(Rasse race: Rassen.get())
			{
				echo.append("<option value=\""+ race.getID() +"\" " + (race.getID() == user.getRace() ? "selected=\"selected\"" : "") + " />"+race.getName()+"</option>");
			}
			echo.append("</select></td></tr>\n");	
			echo.append("<tr><td class=\"noBorderX\">Vacation: </td><td><input type=\"text\" size=\"40\" name=\"vacation\" value=\"" + user.getVacationCount() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Wait4Vac: </td><td><input type=\"text\" size=\"40\" name=\"wait4vac\" value=\"" + user.getWait4VacationCount() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Konto: </td><td><input type=\"text\" size=\"40\" name=\"account\" value=\"" + user.getKonto() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Flags: </td><td><input type=\"text\" size=\"40\" name=\"flags\" value=\"" + user.getFlags() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Nahrungspool-Verfall (Offset): </td><td><input type=\"text\" size=\"40\" name=\"foodpooldegeneration\" value=\"" + user.getFoodpooldegeneration()+ "\"></td></tr>\n");		
			echo.append("<tr><td class=\"noBorderX\">Rang: </td><td><select size=\"1\" name=\"rank\" \">");
			for(Map.Entry<Integer, Rang> rank: Medals.get().raenge().entrySet())
			{
				echo.append("<option value=\""+ rank.getValue().getID() +"\" " + (rank.getValue().getID() == user.getRang() ? "selected=\"selected\"" : "") + " />"+rank.getValue().getName()+"</option>");
			}
			echo.append("</select></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">History: </td><td><textarea name=\"history\" rows=\"3\" cols=\"40\">" + user.getHistory() + "</textarea></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">NPC-Punkte: </td><td><input type=\"text\" size=\"40\" name=\"npcpoints\" value=\"" + user.getNpcPunkte() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Medaillen: </td><td><input type=\"text\" size=\"40\" name=\"medals\" value=\"" + user.getMedals()+ "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Zugang sperren: </td><td><input type=\"checkbox\" name=\"blockuser\" value=\"1\" "+ (user.getDisabled() ? " checked " : "")+"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\" colspan=\"2\">Vorhandene Medallien:<br />");
			echo.append("<ul>");
			for(Map.Entry<Integer, Medal> medal: Medals.get().medals().entrySet())
			{
				echo.append("<li>"+ medal.getValue().getID() +" = " + medal.getValue().getName() + "</li>");
			}
			echo.append("</ul>");
			echo.append("</td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
			echo.append(Common.tableEnd());
		}
	}
}
