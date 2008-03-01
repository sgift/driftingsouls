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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.Ally;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.scripting.entities.RunningQuest;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.tasks.Taskmanager;

/**
 * Ermoeglicht das Einloggen in einen anderen Account ohne Passwort
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Spieler", name="Spieler l&ouml;schen")
public class PlayerDelete implements AdminPlugin, Loggable {

	public void output(AdminController controller, String page, int action) {
		Context context = ContextMap.getContext();
		StringBuffer echo = context.getResponse().getContent();
		Database database = context.getDatabase();
		org.hibernate.Session db = context.getDB();
		
		int userid = context.getRequest().getParameterInt("userid");
		
		String sess = context.getSession();
		
		if( userid == 0 ) {
			echo.append(Common.tableBegin(400, "center"));
			echo.append("Hinweis: Es gibt KEINE Sicherheitsabfrage!<br />\n");
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorderX\">\n");
			echo.append("<tr><td class=\"noBorderX\" width=\"60\">Userid:</td><td class=\"noBorderX\">");
			echo.append("<input type=\"text\" name=\"userid\" size=\"6\" />");
			echo.append("</td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\" colspan=\"2\"><input type=\"checkbox\" name=\"preset\" id=\"form_preset\" value=\"1\" checked=\"checked\" /><label for=\"form_preset\">Planeten reset?</label></td></tr>");
			echo.append("<tr><td class=\"noBorderX\" colspan=\"2\" align=\"center\"><input type=\"hidden\" name=\"sess\" value=\""+sess+"\" />");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"l&ouml;schen\" style=\"width:100px\"/></td></tr>");
			echo.append("</table>\n");
			echo.append("</form>");
			echo.append(Common.tableEnd());
			
			return;
		}

		echo.append(Common.tableBegin(500,"left"));
		User user = (User)db.get(User.class, userid);
		
		if( (user.getAlly() != null) && (user.getAlly().getPresident() == user) ) {
			echo.append("Der Spieler "+userid+" ist Pr&auml;sident einer Allianz.<br />\n");
			echo.append("Die Allianz muss zuerst gel&ouml;scht werden oder einen anderen Pr&auml;sidenten bekommen, bevor der Spieler gel&ouml;scht werden kann.<br />\n");
			echo.append(Common.tableEnd());
			
			return;
		}

		ScriptEngine scriptparser = context.get(ContextCommon.class).getScriptParser("DSQuestScript");
		
		echo.append("Beende Quests....<br />\n");
		List rquestList = db.createQuery("from RunningQuest where user= :user")
			.setEntity("user", user)
			.list();
		for( Iterator iter=rquestList.iterator(); iter.hasNext(); ) {
			RunningQuest rquest = (RunningQuest)iter.next();

			try {
				scriptparser.setContext(
						ScriptParserContext.fromStream(rquest.getExecData().getBinaryStream())
				);
				final Bindings engineBindings = scriptparser.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
				
				engineBindings.put("USER", userid);
				engineBindings.put("QUEST", "r"+rquest.getId());
				engineBindings.put("_PARAMETERS", "0");
				scriptparser.eval(":0\n!ENDQUEST\n!QUIT");
			}
			catch( Exception e ) {
				echo.append("Fehler beim Beenden des Quests "+rquest.getQuest().getId()+": "+e);
				LOG.error(e,e);
				echo.append(Common.tableEnd());
				
				context.rollback();
			}
		}
		
		echo.append("Loesche 'Abgeschlossen'-Status bei Quests<br />\n");
		db.createQuery("delete from CompletedQuest where user= :user")
			.setEntity("user", user)
			.executeUpdate();
		
		if( user.getAlly() != null ) {
			echo.append("Stelle fest ob die Ally jetzt zu wenig Member hat\n");
			
			Ally ally = user.getAlly();
				
			// Allianzen mit einem NPC als Praesidenten koennen auch mit 1 oder 2 Membern existieren
			if( ally.getPresident().getId() > 0 ) {
				long count = ally.getMemberCount()-1;
				if( count < 3 ) {
					Taskmanager.getInstance().addTask(Taskmanager.Types.ALLY_LOW_MEMBER, 21, Integer.toString(ally.getId()), "", "" );
	
					final User sourceUser = (User)db.get(User.class, 0);
					
					List<User> supermembers = ally.getSuperMembers();
					for( User supermember : supermembers ) {
						if( supermember.getId() == userid ) {
							continue;
						}
						
						PM.send( sourceUser, supermember.getId(), "Drohende Allianzaufl&oum;sung", "[Automatische Nachricht]\nAchtung!\n" +
						"Durch das L&ouml;schen eines Allianzmitglieds hat deine Allianz zu wenig Mitglieder " +
						"um weiterhin zu bestehen. Du hast nun 21 Ticks Zeit diesen Zustand zu &auml;ndern. " +
						"Andernfalls wird die Allianz aufgel&ouml;&szlig;t.");
					}

					echo.append("....sie hat jetzt zu wenig");
				}
			}
			echo.append("<br />\n");
		}
		
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
		database.update("DELETE FROM user_f WHERE id="+userid);

		//Schiffe
		echo.append("Entferne Schiffe...\n");
		int count = 0;
		
		List ships = db.createQuery("from Ship where owner=?")
			.setEntity(0, user)
			.list();
		for( Iterator iter=ships.iterator(); iter.hasNext(); ) {
			Ship aship = (Ship)iter.next();
			aship.destroy();
			count++;
		}
		
		echo.append(count+" Schiffe entfernt<br />\n");

		//Basen
		List<Integer> baselist = new ArrayList<Integer>();
		
		List baseList = db.createQuery("from Base where owner=?")
			.setInteger(0, userid)
			.list();
		for( Iterator iter=baseList.iterator(); iter.hasNext(); ) {
			Base base = (Base)iter.next();
			baselist.add(base.getId());
		}
		
		if( context.getRequest().getParameterInt("preset") == 0 ) {
			echo.append("&Uuml;bereigne Bases an Spieler 0...\n");

			db.createQuery("update Base " +
					"set owner=0,name='Verlassener Asteroid',e=0,bewohner=0,arbeiter=0,autoGtuActs='' " +
					"where owner=?")
					.setInteger(0, userid)
					.executeUpdate();

			if( baselist.size() > 0 ) {
				db.createQuery("update Forschungszentrum set forschung=0,dauer=0 where col in ("+Common.implode(",",baselist)+")")
					.executeUpdate();
				db.createQuery("update Academy set train=0,remain=0,upgrade='' where col in ("+Common.implode(",",baselist)+")")
					.executeUpdate();
				db.createQuery("update BaseWerft set remaining=0,building=0 where col in ("+Common.implode(",",baselist)+")")
					.executeUpdate();
				db.createQuery("update WeaponFactory set produces='' where col in ("+Common.implode(",",baselist)+")")
					.executeUpdate();
			}
		} 
		else {
			echo.append("&Uuml;bereigne Basen an Spieler 0 (+ reset)...\n");

			Cargo emptycargo = new Cargo();

			db.createQuery("update Base " +
					"set owner=0,name='Verlassener Asteroid',active=0,core=0,coreActive=0,e=0,bewohner=0,arbeiter=0,cargo=?,autoGtuActs='' " +
					"where owner=?")
					.setParameter(0, emptycargo)
					.setInteger(1, userid)
					.executeUpdate();
			
			if( baselist.size() > 0 ) {
				db.createQuery("delete from Forschungszentrum where col in ("+Common.implode(",",baselist)+")").executeUpdate();
				db.createQuery("delete from Academy where col in ("+Common.implode(",",baselist)+")").executeUpdate();
				db.createQuery("delete from BaseWerft where col in ("+Common.implode(",",baselist)+")").executeUpdate();
				db.createQuery("delete from WeaponFactory where col in ("+Common.implode(",",baselist)+")").executeUpdate();
			}
		}
		echo.append(baselist.size()+" Basen bearbeitet<br />\n");

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
		echo.append(affRows+" gel&ouml;scht<br />\n");
		
		db.createQuery("update PM set sender=0 where sender = :user")
			.setEntity("user", user)
			.executeUpdate();
		
		echo.append("L&ouml;sche PM-Ordner...<br />\n");
		db.createQuery("delete from Ordner where owner=?")
			.setInteger(0, userid)
			.executeUpdate();

		echo.append("L&ouml;sche Diplomatieeintr&auml;ge...<br />\n");
		db.createQuery("delete from UserRelation where user=? or target=?")
			.setInteger(0, userid)
			.setInteger(1, userid)
			.executeUpdate();

		echo.append("L&ouml;sche Kontobewegungen...<br />\n");
		db.createQuery("delete from UserMoneyTransfer where from= :user or to = :user")
			.setEntity("user", user)
			.executeUpdate();

		echo.append("L&ouml;sche Userlogo...<br />\n");
		new File(Configuration.getSetting("ABSOLUTE_PATH")+"data/logos/user/"+userid+".gif").delete();
		
		echo.append("L&ouml;sche Offiziere...<br />\n");
		db.createQuery("delete from Offizier where owner=?")
			.setInteger(0, userid)
			.executeUpdate();
		
		echo.append("L&ouml;sche Shop-Auftraege...<br />\n");
		db.createQuery("delete from FactionShopOrder where user=?")
			.setInteger(0, userid)
			.executeUpdate();
		
		echo.append("L&ouml;sche Sessions...<br />\n");
		db.createQuery("delete from Session where id=?")
			.setInteger(0, userid)
			.executeUpdate();
		
		echo.append("L&ouml;sche Statistik 'Item-Locations'...<br />\n");
		db.createQuery("delete from StatModuleLocation where user=?")
			.setInteger(0, userid)
			.executeUpdate();
		
		echo.append("L&ouml;sche Statistik 'User-Cargo'...<br />\n");
		db.createQuery("delete from StatUserCargo where user=?")
			.setInteger(0, userid)
			.executeUpdate();
		
		echo.append("L&ouml;sche Umfrageeintraege...<br />\n");
		database.update("DELETE FROM survey_voted WHERE user_id="+userid);
		
		echo.append("L&ouml;sche Usereintrag...<br />\n");
		db.delete(user);
		
		echo.append("<br />Spieler "+userid+" gel&ouml;scht!<br />\n");		
		
		echo.append(Common.tableEnd());
	}

}
