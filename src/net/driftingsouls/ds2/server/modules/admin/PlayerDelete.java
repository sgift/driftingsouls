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
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.services.AllianzService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ermoeglicht das Einloggen in einen anderen Account ohne Passwort.
 *
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category = "Spieler", name = "Spieler löschen", permission = WellKnownAdminPermission.PLAYER_DELETE)
public class PlayerDelete implements AdminPlugin
{
	private static final Log log = LogFactory.getLog(PlayerDelete.class);

	@Override
	public void output(StringBuilder echo) throws IOException
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		int userid = context.getRequest().getParameterInt("userid");

		if( userid == 0 )
		{
			echo.append("<div class='gfxbox' style='width:440px;text-align:center'>");
			echo.append("Hinweis: Es gibt KEINE Sicherheitsabfrage!<br />\n");
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorderX\">\n");
			echo.append("<tr><td class=\"noBorderX\" width=\"60\">Userid:</td><td class=\"noBorderX\">");
			echo.append("<input type=\"text\" name=\"userid\" size=\"6\" />");
			echo.append("</td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\" colspan=\"2\" align=\"center\">");
			echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"l&ouml;schen\" style=\"width:100px\"/></td></tr>");
			echo.append("</table>\n");
			echo.append("</form>");
			echo.append("</div>");

			return;
		}

		log.info("Loesche Spieler "+userid);

		echo.append("<div class='gfxbox' style='width:540px'>");
		User user = (User)db.get(User.class, userid);
		if( user == null ) {
			echo.append("Der Spieler existiert nicht.<br />\n");
			echo.append("</div>");

			return;
		}
		if( user.isNPC() || user.isAdmin() ) {
			echo.append("Der NPCs/Admins können nicht gelöscht werden.<br />\n");
			echo.append("</div>");

			return;
		}
		if( user.getAccessLevel() >= context.getActiveUser().getAccessLevel() ) {
			echo.append("Du hast nicht das notwendige Berechtigungslevel.<br />\n");
			echo.append("</div>");

			return;
		}
		if( (user.isInVacation() || user.getInactivity() <= 7*14) &&
				!context.hasPermission(WellKnownAdminPermission.PLAYER_DELETE_ACTIVE) ) {
			echo.append("Du hast nicht die Berechtigung einen aktiven Spieler zu löschen.<br />\n");
			echo.append("</div>");

			return;
		}

		if( (user.getAlly() != null) && (user.getAlly().getPresident() == user) )
		{
			echo.append("Der Spieler ").append(userid).append(" ist Pr&auml;sident einer Allianz.<br />\n");
			echo.append("Die Allianz muss zuerst gel&ouml;scht werden oder einen anderen Pr&auml;sidenten bekommen, bevor der Spieler gel&ouml;scht werden kann.<br />\n");
			echo.append("</div>");

			return;
		}

		long count;

		if( user.getAlly() != null )
		{
			echo.append("Stelle fest ob die Ally jetzt zu wenig Member hat\n");

			Ally ally = user.getAlly();

			// Allianzen mit einem NPC als Praesidenten koennen auch mit 1 oder 2 Membern existieren
			if( ally.getPresident().getId() > 0 )
			{
				count = ally.getMemberCount() - 1;
				if( count < 3 )
				{
					Taskmanager.getInstance().addTask(Taskmanager.Types.ALLY_LOW_MEMBER, 21,
							Integer.toString(ally.getId()), "", "");

					final User sourceUser = (User)db.get(User.class, 0);

					AllianzService allianzService = context.getBean(AllianzService.class, null);
					List<User> supermembers = allianzService.getAllianzfuehrung(ally);
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
		db.createQuery("delete from GtuZwischenlager where user1=:user")
			.setInteger("user", userid)
			.executeUpdate();
		db.createQuery("delete from GtuZwischenlager where user2=:user")
			.setInteger("user", userid)
			.executeUpdate();

		echo.append("Entferne User-Values...");
		count = db.createQuery("delete from UserValue where user=:user")
			.setInteger("user", userid)
			.executeUpdate();
		echo.append(count).append("<br />\n");

		echo.append("Entferne comnet-visits...");
		count = db.createQuery("delete from ComNetVisit where user=:user")
			.setInteger("user", userid)
			.executeUpdate();
		echo.append(count).append("<br />\n");

		echo.append("Ordne Comnet-Posts ID 0 zu...");
		count = db.createQuery("update ComNetEntry set user=0 where user=:user")
			.setInteger("user", userid)
			.executeUpdate();
		echo.append(count).append("<br />\n");

		echo.append("Entferne user-raenge...");
		count = db.createQuery("delete from UserRank where userRankKey.owner=:user")
				.setInteger("user", userid)
				.executeUpdate();
		echo.append(count).append("<br />\n");

		echo.append("Entferne Fraktionsaktionsmeldungen...");
		count = db.createQuery("delete from FraktionAktionsMeldung where gemeldetVon=:user")
				.setInteger("user", userid)
				.executeUpdate();
		echo.append(count).append("<br />\n");

		// Schiffe
		echo.append("Entferne Schiffe...\n");

		List<?> ships = db.createQuery("from Ship where owner=:user")
			.setEntity("user", user)
			.list();
		for (Object ship : ships)
		{
			Ship aship = (Ship) ship;
			aship.destroy();
			count++;
		}

		echo.append(count).append(" Schiffe entfernt<br />\n");

		// Basen
		List<Base> baselist = new ArrayList<>();

		List<?> baseList = db.createQuery("from Base where owner=:user")
			.setInteger("user", userid)
			.list();
		for (Object aBaseList : baseList)
		{
			Base base = (Base) aBaseList;
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
				building.cleanup(context, base, bebauung[i]);
				bebauung[i] = 0;
			}
			base.setBebauung(bebauung);

			base.setOwner(nullUser);
			base.setName("Verlassener Asteroid");
			base.setActive(new Integer[] { 0 });
			base.setCore(null);
			base.setCoreActive(false);
			base.setEnergy(0);
			base.setBewohner(0);
			base.setArbeiter(0);
			base.setCargo(new Cargo());
			base.setAutoGTUActs(new ArrayList<>());
		}

		echo.append(baselist.size()).append(" Basen bearbeitet<br />\n");

		echo.append("Entferne Handelseintr&auml;ge...");
		count = db.createQuery("delete from Handel where who=:user")
			.setInteger("user", userid)
			.executeUpdate();
		echo.append(count).append("<br />\n");

		echo.append("&Uuml;berstelle GTU-Gebote an die GTU (-2)...<br />");
		db.createQuery("update Versteigerung set bieter=-2 where bieter=:user")
			.setInteger("user", userid)
			.executeUpdate();

		db.createQuery("update Versteigerung set owner=-2 where owner=:user")
			.setInteger("user", userid)
			.executeUpdate();

		echo.append("L&ouml;sche PM's...");
		count = db.createQuery("delete from PM where empfaenger = :user")
			.setEntity("user", user)
			.executeUpdate();
		echo.append(count).append("<br />\n");

		db.createQuery("update PM set sender=0 where sender = :user")
			.setEntity("user", user)
			.executeUpdate();

		echo.append("L&ouml;sche PM-Ordner...");
		count = db.createQuery("delete from Ordner where owner=:user")
			.setInteger("user", userid)
			.executeUpdate();
		echo.append(count).append("<br />");

		echo.append("L&ouml;sche Diplomatieeintr&auml;ge...");
		count = db.createQuery("delete from UserRelation where user=:user1 or target=:user2")
			.setInteger("user1", userid)
			.setInteger("user2", userid)
			.executeUpdate();
		echo.append(count).append("<br />\n");

		echo.append("L&ouml;sche Kontobewegungen...<br />\n");
		db.createQuery("delete from UserMoneyTransfer umt where umt.from= :user or umt.to = :user")
			.setEntity("user", user)
			.executeUpdate();

		echo.append("L&ouml;sche Userlogo...<br />\n");
		new File(Configuration.getAbsolutePath() + "data/logos/user/" + userid + ".gif")
				.delete();

		echo.append("L&ouml;sche Offiziere...");
		count = db.createQuery("delete from Offizier where owner=:user")
			.setInteger("user", userid)
			.executeUpdate();
		echo.append(count).append("<br />");

		echo.append("L&ouml;sche Shop-Auftraege...");
		count = db.createQuery("delete from FactionShopOrder where user=:user")
			.setInteger("user", userid)
			.executeUpdate();
		echo.append(count).append("<br />\n");

		echo.append("L&ouml;sche Statistik 'Item-Locations'...");
		count = db.createQuery("delete from StatItemLocations where user=:user")
			.setInteger("user", userid)
			.executeUpdate();
		echo.append(count).append("<br />\n");

		echo.append("L&ouml;sche Statistik 'User-Cargo'...");
		count = db.createQuery("delete from StatUserCargo where user=:user")
			.setInteger("user", userid)
			.executeUpdate();
		echo.append(count).append("<br />\n");

		echo.append("L&ouml;sche Sessioneintrag...");
		count = db.createQuery("delete from PermanentSession where user=:user")
			.setEntity("user", user)
			.executeUpdate();
		echo.append(count).append("<br />\n");

		echo.append("L&ouml;sche Usereintrag...<br />\n");
		db.flush();
		db.delete(user);

		echo.append("<br />Spieler ").append(userid).append(" gel&ouml;scht!<br />\n");

		echo.append("</div>");
	}

}
