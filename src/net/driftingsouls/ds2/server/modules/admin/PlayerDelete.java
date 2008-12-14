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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.bases.AutoGTUAction;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.Ally;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.scripting.entities.RunningQuest;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.tasks.Taskmanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Ermoeglicht das Einloggen in einen anderen Account ohne Passwort
 * 
 * @author Christopher Jung
 * 
 */
@Configurable
@AdminMenuEntry(category = "Spieler", name = "Spieler l&ouml;schen")
public class PlayerDelete implements AdminPlugin
{
	private static final Log log = LogFactory.getLog(PlayerDelete.class);
	
	private Configuration config;
	
    /**
     * Injiziert die DS-Konfiguration
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
    }

	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		Database database = context.getDatabase();
		org.hibernate.Session db = context.getDB();

		int userid = context.getRequest().getParameterInt("userid");

		if( userid == 0 )
		{
			echo.append(Common.tableBegin(400, "center"));
			echo.append("Hinweis: Es gibt KEINE Sicherheitsabfrage!<br />\n");
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorderX\">\n");
			echo.append("<tr><td class=\"noBorderX\" width=\"60\">Userid:</td><td class=\"noBorderX\">");
			echo.append("<input type=\"text\" name=\"userid\" size=\"6\" />");
			echo.append("</td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\" colspan=\"2\" align=\"center\">");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"l&ouml;schen\" style=\"width:100px\"/></td></tr>");
			echo.append("</table>\n");
			echo.append("</form>");
			echo.append(Common.tableEnd());

			return;
		}
		
		log.info("Loesche Spieler "+userid);

		echo.append(Common.tableBegin(500, "left"));
		User user = (User)db.get(User.class, userid);

		if( (user.getAlly() != null) && (user.getAlly().getPresident() == user) )
		{
			echo.append("Der Spieler " + userid + " ist Pr&auml;sident einer Allianz.<br />\n");
			echo.append("Die Allianz muss zuerst gel&ouml;scht werden oder einen anderen Pr&auml;sidenten bekommen, bevor der Spieler gel&ouml;scht werden kann.<br />\n");
			echo.append(Common.tableEnd());

			return;
		}

		ScriptEngine scriptparser = context.get(ContextCommon.class).getScriptParser("DSQuestScript");

		echo.append("Beende Quests....<br />\n");
		List<?> rquestList = db.createQuery("from RunningQuest where user= :user")
			.setEntity("user",user)
			.list();
		for( Iterator<?> iter = rquestList.iterator(); iter.hasNext(); )
		{
			RunningQuest rquest = (RunningQuest)iter.next();

			try
			{
				scriptparser.setContext(ScriptParserContext.fromStream(rquest.getExecData()
						.getBinaryStream()));
				final Bindings engineBindings = scriptparser.getContext().getBindings(
						ScriptContext.ENGINE_SCOPE);

				engineBindings.put("USER", userid);
				engineBindings.put("QUEST", "r" + rquest.getId());
				engineBindings.put("_PARAMETERS", "0");
				scriptparser.eval(":0\n!ENDQUEST\n!QUIT");
			}
			catch( Exception e )
			{
				echo.append("Fehler beim Beenden des Quests " + rquest.getQuest().getId() + ": "+ e);
				log.error(e, e);
				echo.append(Common.tableEnd());

				context.rollback();
			}
		}

		echo.append("Loesche 'Abgeschlossen'-Status bei Quests<br />\n");
		db.createQuery("delete from CompletedQuest where user= :user")
			.setEntity("user", user)
			.executeUpdate();

		if( user.getAlly() != null )
		{
			echo.append("Stelle fest ob die Ally jetzt zu wenig Member hat\n");

			Ally ally = user.getAlly();

			// Allianzen mit einem NPC als Praesidenten koennen auch mit 1 oder 2 Membern existieren
			if( ally.getPresident().getId() > 0 )
			{
				long count = ally.getMemberCount() - 1;
				if( count < 3 )
				{
					Taskmanager.getInstance().addTask(Taskmanager.Types.ALLY_LOW_MEMBER, 21,
							Integer.toString(ally.getId()), "", "");

					final User sourceUser = (User)db.get(User.class, 0);

					List<User> supermembers = ally.getSuperMembers();
					for( User supermember : supermembers )
					{
						if( supermember.getId() == userid )
						{
							continue;
						}

						PM.send(
							sourceUser,
							supermember.getId(),
							"Drohende Allianzaufl&oum;sung",
							"[Automatische Nachricht]\nAchtung!\n"
									+ "Durch das L&ouml;schen eines Allianzmitglieds hat deine Allianz zu wenig Mitglieder "
									+ "um weiterhin zu bestehen. Du hast nun 21 Ticks Zeit diesen Zustand zu &auml;ndern. "
									+ "Andernfalls wird die Allianz aufgel&ouml;&szlig;t.");
					}

					echo.append("....sie hat jetzt zu wenig");
				}
			}
			echo.append("<br />\n");
		}
		echo.append("Entferne GTU Zwischenlager...<br />\n");
		db.createQuery("delete from GtuZwischenlager where user1=?")
			.setInteger(0, userid)
			.executeUpdate();
		db.createQuery("delete from GtuZwischenlager where user2=?")
			.setInteger(0, userid)
			.executeUpdate();
		
		echo.append("Entferne User-Values...<br />\n");
		db.createQuery("delete from UserValue where user=?")
			.setInteger(0, userid)
			.executeUpdate();

		echo.append("Entferne comnet-visits...<br />\n");
		db.createQuery("delete from ComNetVisit where user=?")
			.setInteger(0, userid)
			.executeUpdate();

		echo.append("Ordne Comnet-Posts ID 0 zu...<br />\n");
		db.createQuery("update ComNetEntry set user=0 where user=?")
			.setInteger(0, userid)
			.executeUpdate();

		echo.append("Entferne user-forschungen...<br />\n");
		db.createQuery("delete from UserResearch where owner=?")
			.setInteger(0, userid)
			.executeUpdate();

		// Schiffe
		echo.append("Entferne Schiffe...\n");
		int count = 0;

		List<?> ships = db.createQuery("from Ship where owner=?")
			.setEntity(0, user)
			.list();
		for( Iterator<?> iter = ships.iterator(); iter.hasNext(); )
		{
			Ship aship = (Ship)iter.next();
			aship.destroy();
			count++;
		}

		echo.append(count + " Schiffe entfernt<br />\n");

		// Basen
		List<Base> baselist = new ArrayList<Base>();

		List<?> baseList = db.createQuery("from Base where owner=?")
			.setInteger(0, userid)
			.list();
		for( Iterator<?> iter = baseList.iterator(); iter.hasNext(); )
		{
			Base base = (Base)iter.next();
			baselist.add(base);
		}

		echo.append("&Uuml;bereigne Basen an Spieler 0 (+ reset)...\n");

		User nullUser = (User)db.get(User.class, 0);

		for( Base base : baselist )
		{
			Integer[] bebauung = base.getBebauung();
			for( int i = 0; i < bebauung.length; i++ )
			{
				if( bebauung[i] == 0 )
				{
					continue;
				}

				Building building = Building.getBuilding(bebauung[i]);
				building.cleanup(context, base);
			}

			base.setOwner(nullUser);
			base.setName("Verlassener Asteroid");
			base.setActive(new Integer[] { 0 });
			base.setCore(0);
			base.setCoreActive(false);
			base.setEnergy(0);
			base.setBewohner(0);
			base.setArbeiter(0);
			base.setCargo(new Cargo());
			base.setAutoGTUActs(new ArrayList<AutoGTUAction>());
		}

		echo.append(baselist.size() + " Basen bearbeitet<br />\n");

		echo.append("Entferne Handelseintr&auml;ge...<br />\n");
		db.createQuery("delete from Handel where who=?")
			.setInteger(0, userid)
			.executeUpdate();

		echo.append("&Uuml;berstelle GTU-Gebote an die GTU (-2)...<br />");
		db.createQuery("update Versteigerung set bieter=-2 where bieter=?")
			.setInteger(0, userid)
			.executeUpdate();

		db.createQuery("update Versteigerung set owner=-2 where owner=?")
			.setInteger(0, userid)
			.executeUpdate();

		db.createQuery("update PaketVersteigerung set bieter=-2 where bieter=?")
			.setInteger(0, userid)
			.executeUpdate();

		echo.append("L&ouml;sche PM's...\n");
		int affRows = db.createQuery("delete from PM where empfaenger = :user")
			.setEntity("user", user)
			.executeUpdate();
		echo.append(affRows + " gel&ouml;scht<br />\n");

		db.createQuery("update PM set sender=0 where sender = :user")
			.setEntity("user", user)
			.executeUpdate();

		echo.append("L&ouml;sche PM-Ordner...\n");
		count = db.createQuery("delete from Ordner where owner=?")
			.setInteger(0, userid)
			.executeUpdate();
		echo.append(count + " entfernt<br />");

		echo.append("L&ouml;sche Diplomatieeintr&auml;ge...<br />\n");
		db.createQuery("delete from UserRelation where user=? or target=?")
			.setInteger(0, userid)
			.setInteger(1, userid)
			.executeUpdate();

		echo.append("L&ouml;sche Kontobewegungen...<br />\n");
		db.createQuery("delete from UserMoneyTransfer umt where umt.from= :user or umt.to = :user")
			.setEntity("user", user)
			.executeUpdate();

		echo.append("L&ouml;sche Userlogo...<br />\n");
		new File(config.get("ABSOLUTE_PATH") + "data/logos/user/" + userid + ".gif")
				.delete();

		echo.append("L&ouml;sche Offiziere...\n");
		count = db.createQuery("delete from Offizier where owner=?")
			.setInteger(0, userid)
			.executeUpdate();
		echo.append(count + " entfernt<br />");

		echo.append("L&ouml;sche Shop-Auftraege...<br />\n");
		db.createQuery("delete from FactionShopOrder where user=?")
			.setInteger(0, userid)
			.executeUpdate();

		echo.append("L&ouml;sche Sessions...<br />\n");
		db.createQuery("delete from Session where id=?")
			.setInteger(0, userid)
			.executeUpdate();

		echo.append("L&ouml;sche Statistik 'Item-Locations'...<br />\n");
		db.createQuery("delete from StatItemLocations where user=?")
			.setInteger(0, userid)
			.executeUpdate();

		echo.append("L&ouml;sche Statistik 'User-Cargo'...<br />\n");
		db.createQuery("delete from StatUserCargo where user=?")
			.setInteger(0, userid)
			.executeUpdate();

		echo.append("L&ouml;sche Umfrageeintraege...<br />\n");
		database.update("DELETE FROM survey_voted WHERE user_id=" + userid);

		echo.append("L&ouml;sche Usereintrag...<br />\n");
		db.delete(user);

		echo.append("<br />Spieler " + userid + " gel&ouml;scht!<br />\n");

		echo.append(Common.tableEnd());
	}

}
