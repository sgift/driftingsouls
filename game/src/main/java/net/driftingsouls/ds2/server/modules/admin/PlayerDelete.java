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
import net.driftingsouls.ds2.server.framework.Common;
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
import java.nio.file.Files;
import java.nio.file.Path;
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
	public void output(StringBuilder echo) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		int userid = context.getRequest().getParameterInt("userid");
		String conf = context.getRequest().getParameterString("conf");

		if( userid == 0 )
		{
			echo.append("<div class='gfxbox' style='width:440px;text-align:center'>");
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorderX\">\n");
			echo.append("<tr><td class=\"noBorderX\" width=\"60\">Userid:</td><td class=\"noBorderX\">");
			echo.append("<input type=\"text\" name=\"userid\" size=\"6\" />");
			echo.append("</td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\" colspan=\"2\" align=\"center\">");
			echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"conf\" value=\"false\" />\n");
			echo.append("<input type=\"submit\" value=\"l&ouml;schen\" style=\"width:100px\"/></td></tr>");
			echo.append("</table>\n");
			echo.append("</form>");
			echo.append("</div>");

			return;
		}



		echo.append("<div class='gfxbox' style='width:540px'>");
		User user = (User)db.get(User.class, userid);
		if( user == null ) {
			echo.append("Der Spieler existiert nicht.<br />\n");
			echo.append("</div>");

			return;
		}
		if( user.isNPC() || user.isAdmin() ) {
			echo.append("Der NPC/Admin kann nicht gelöscht werden.<br />\n");
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
			echo.append("Der Spieler ").append(userid).append(" ist Anführer einer Allianz.<br />\n");
			echo.append("Die Allianz muss zuerst gelöscht werden oder einen anderen Anführer bekommen, bevor der Spieler gelöscht werden kann.<br />\n");
			echo.append("</div>");

			return;
		}

		//Sicherheitsabfrage
		if(!conf.equals("ok"))
		{

			echo.append("<div class='gfxbox' style='width:440px;text-align:center'>");
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorderX\">\n");
			echo.append("<tr><td class=\"noBorderX\" width=\"60\">Spieler:</td><td class=\"noBorderX\">"+ user.getName() + "("+user.getId()+")"+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\" width=\"60\">Allianz:</td><td class=\"noBorderX\">"+ (user.getAlly()!=null?user.getAlly().getName():"keine")+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\" width=\"60\">Login:</td><td class=\"noBorderX\">"+ user.getUN()+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\" width=\"60\">E-Mail:</td><td class=\"noBorderX\">"+ user.getEmail()+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\" width=\"60\">Angemeldet seit:</td><td class=\"noBorderX\">"+  Common.date("d.m.Y H:i:s", user.getSignup()) +"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\" width=\"60\">Inaktiv seit:</td><td class=\"noBorderX\">"+ user.getInactivity() +" Ticks</td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\" colspan=\"2\" align=\"center\">");
			echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"conf\" value=\"ok\" />\n");
			echo.append("<input type=\"hidden\" name=\"userid\" value=\""+userid+"\" />\n");
			echo.append("<input type=\"submit\" value=\"l&ouml;schen\" style=\"width:100px\"/></td></tr>");
			echo.append("</table>\n");
			echo.append("</form>");
			echo.append("</div>");
			return;
		}

		deleteUser(userid);
		echo.append("<br />Spieler ").append(userid).append(" gelöscht!<br />\n");
		echo.append("</div>");
	}

	public void deleteUser(int userId) {
		try {
			log.info(userId + ": Delete player");


			var context = ContextMap.getContext();
			var db = context.getEM();
			User user = db.find(User.class, userId);
			if (user.getAlly() != null) {
				log.info(userId + ": Check if ally still has enough members after player is deleted");
				Ally ally = user.getAlly();

				// Allianzen mit einem NPC als Praesidenten koennen auch mit 1 oder 2 Membern existieren
				if (ally.getPresident().getId() > 0) {
					long count = ally.getMemberCount() - 1;
					if (count < 2) {
						log.info(userId + ": Not enough players left, starting ally low warning");
						Taskmanager.getInstance().addTask(Taskmanager.Types.ALLY_LOW_MEMBER, 21,
								Integer.toString(ally.getId()), "", "");

						final User sourceUser = db.find(User.class, 0);

						AllianzService allianzService = context.getBean(AllianzService.class, null);
						List<User> supermembers = allianzService.getAllianzfuehrung(ally);
						for (User supermember : supermembers) {
							if (supermember.getId() == userId) {
								continue;
							}

							PM.send(
									sourceUser,
									supermember.getId(),
									"Drohende Allianzauflösung",
									"[Automatische Nachricht]\nAchtung!\n"
											+ "Durch das Löschen eines Allianzmitglieds hat Deine Allianz zu wenig Mitglieder, "
											+ "um weiterhin zu bestehen. Du hast nun 21 Ticks Zeit, diesen Zustand zu ändern. "
											+ "Anderenfalls wird die Allianz aufgelöst.");
						}
					}
				}
			}

			log.info(userId + ": Deleting gtu stash");
			db.createQuery("delete from GtuZwischenlager where user1=:user or user2=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Deleting user values");
			db.createQuery("delete from UserValue where user=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Deleting comnet visits");
			db.createQuery("delete from ComNetVisit where user=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Setting owner of comnet posts to 0");
			db.createQuery("update ComNetEntry set user=0 where user=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Removing user ranks");
			db.createQuery("delete from UserRank where userRankKey.owner=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Removing faction notes");
			db.createQuery("delete from FraktionAktionsMeldung where gemeldetVon=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Deleting ships");
			var ships = db.createQuery("from Ship where owner=:user", Ship.class)
					.setParameter("user", user)
					.getResultList();
			for (Ship ship : ships) {
				ship.destroy();
			}

			log.info(userId + ": Vacating bases");
			var baseList = db.createQuery("from Base where owner=:user", Base.class)
					.setParameter("user", user)
					.getResultList();

			User nullUser = db.find(User.class, 0);

			for (Base base : baseList) {
				Integer[] bebauung = base.getBebauung();
				for (int i = 0; i < bebauung.length; i++) {
					if (bebauung[i] == 0) {
						continue;
					}

					Building building = Building.getBuilding(bebauung[i]);
					building.cleanup(context, base, bebauung[i]);
					bebauung[i] = 0;
				}
				base.setBebauung(bebauung);

				base.setOwner(nullUser);
				base.setName("Verlassener Asteroid");
				base.setActive(new Integer[]{0});
				base.setCore(null);
				base.setCoreActive(false);
				base.setEnergy(0);
				base.setBewohner(0);
				base.setArbeiter(0);
				base.setCargo(new Cargo());
				base.setAutoGTUActs(new ArrayList<>());
			}

			log.info(userId + ": Deleting trade entries");
			db.createQuery("delete from Handel where who=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Transfer auction bids to GTU");
			db.createQuery("update Versteigerung set bieter=-2 where bieter=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Transfer auctioneer position to GTU");
			db.createQuery("update Versteigerung set owner=-2 where owner=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Delete PMs");
			db.createQuery("delete from PM where empfaenger = :user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Remove Player as PM sender");
			db.createQuery("update PM set sender=0 where sender = :user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Remove PM folders");
			db.createQuery("delete from Ordner where owner=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Remove relation entries");
			db.createQuery("delete from UserRelation where user=:user or target=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Remove bank transfer logs");
			db.createQuery("delete from UserMoneyTransfer umt where umt.from= :user or umt.to = :user")
					.setParameter("user", user)
					.executeUpdate();

			try {
				Files.delete(Path.of(Configuration.getAbsolutePath() + "data/logos/user/" + userId + ".gif"));
			} catch (IOException ignored) {
				// Nothing to do
			}

			log.info(userId + ": Remove officers");
			db.createQuery("delete from Offizier where owner=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Remove faction shop orders");
			db.createQuery("delete from FactionShopOrder where user=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Remove item statistics");
			db.createQuery("delete from StatItemLocations where user=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Remove user cargo statistics");
			db.createQuery("delete from StatUserCargo where user=:user")
					.setParameter("user", user)
					.executeUpdate();

			log.info(userId + ": Remove session entries");
			db.createQuery("delete from PermanentSession where user=:user")
					.setParameter("user", user)
					.executeUpdate();

			db.flush();
			db.remove(user);
			log.info(userId + ": Player deleted");
		} catch (Exception e) {
			log.error(userId + ": Unable to delete user", e);
			throw new RuntimeException(e);
		}
	}
}
