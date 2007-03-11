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
import java.util.List;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.scripting.ScriptParser;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.ships.Ships;
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
		Database db = context.getDatabase();
		
		int userid = context.getRequest().getParameterInt("userid");
		
		String sess = context.getSession();
		
		if( userid == 0 ) {
			echo.append(Common.tableBegin(400, "center"));
			echo.append("Hinweis: Es gibt KEINE Sicherheitsabfrage!<br />\n");
			echo.append("<form action=\"./main.php\" method=\"post\">");
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
		
		SQLResultRow presi = db.first("SELECT a.president FROM users u JOIN ally a ON u.ally=a.id WHERE u.id="+userid);
		if( !presi.isEmpty() && (presi.getInt("president") == userid) ) {
			echo.append("Der Spieler "+userid+" ist Pr&auml;sident einer Allianz.<br />\n");
			echo.append("Die Allianz muss zuerst gel&ouml;scht werden oder einen anderen Pr&auml;sidenten bekommen, bevor der Spieler gel&ouml;scht werden kann.<br />\n");
			echo.append(Common.tableEnd());
			
			return;
		}
		User user = context.createUserObject(userid);
			
		ScriptParser scriptparser = context.get(ContextCommon.class).getScriptParser(ScriptParser.NameSpace.QUEST);
			
		db.tBegin();
		
		echo.append("Beende Quests....<br />\n");
		SQLQuery rquest = db.query("SELECT * FROM quests_running WHERE userid=",userid);
		while( rquest.next() ) {
			scriptparser.cleanup();
			try {
				scriptparser.setContext(
						ScriptParserContext.fromStream(rquest.getBlob("execdata").getBinaryStream())
				);
				scriptparser.setRegister("USER", userid);
				scriptparser.setRegister("QUEST", "r"+rquest.getInt("id"));
				scriptparser.executeScript(db, ":0\n!ENDQUEST\n!QUIT","0");
			}
			catch( Exception e ) {
				echo.append("Fehler beim Beenden des Quests "+rquest.getInt("questid")+": "+e);
				LOG.error(e,e);
				echo.append(Common.tableEnd());
				
				db.tRollback();
				
				return;
			}
		}
		rquest.free();
		
		echo.append("Loesche 'Abgeschlossen'-Status bei Quests<br />\n");
		db.update("DELETE FROM quests_completed WHERE userid="+userid);
		
		if( user.getAlly() != 0 ) {
			echo.append("Stelle fest ob die Ally jetzt zu wenig Member hat\n");
				
			SQLResultRow ally = db.first("SELECT * FROM ally WHERE id="+user.getAlly());
				
			// Allianzen mit einem NPC als Praesidenten koennen auch mit 1 oder 2 Membern existieren
			if( ally.getInt("president") > 0 ) {
				int count = db.first("SELECT count(*)-1 as count FROM users WHERE ally="+ally.getInt("id")).getInt("count");
				if( count < 3 ) {
					Taskmanager.getInstance().addTask(Taskmanager.Types.ALLY_LOW_MEMBER, 21, Integer.toString(ally.getInt("id")), "", "" );
	
					SQLQuery supermemberid = db.query("SELECT DISTINCT id FROM users " +
							"WHERE ally="+ally.getInt("id")+" AND " +
									"(allyposten!=0 OR id="+ally.getInt("president")+") " +
									"AND id!="+userid);
					while( supermemberid.next() ) {
						PM.send( context, 0, supermemberid.getInt("id"), "Drohende Allianzaufl&oum;sung", 
								"[Automatische Nachricht]\nAchtung!\n" +
								"Durch das L&ouml;schen eines Allianzmitglieds hat deine Allianz zu wenig Mitglieder " +
								"um weiterhin zu bestehen. Du hast nun 21 Ticks Zeit diesen Zustand zu &auml;ndern. " +
								"Andernfalls wird die Allianz aufgel&ouml;&szlig;t.");
					}
					supermemberid.free();

					echo.append("....sie hat jetzt zu wenig");
				}
			}
			echo.append("<br />\n");
		}
		
		echo.append("Entferne User-Values...<br />\n");
		db.update("DELETE FROM user_values WHERE user_id="+userid);

		echo.append("Entferne comnet-visits...<br />\n");
		db.update("DELETE FROM skn_visits WHERE user="+userid);

		echo.append("Entferne user-forschungen...<br />\n");
		db.update("DELETE FROM user_f WHERE id="+userid);

		//Schiffe
		echo.append("Entferne Schiffe...\n");
		int count = 0;
		
		SQLQuery sid = db.query("SELECT id FROM ships WHERE owner="+userid);
		while( sid.next() ) {
			Ships.destroy(sid.getInt("id"));
			count++;
		}
		sid.free();
		
		echo.append(count+" Schiffe entfernt<br />\n");

		//Basen
		List<Integer> baselist = new ArrayList<Integer>();
		
		SQLQuery bid = db.query("SELECT id FROM bases WHERE owner="+userid);
		while( bid.next() ) {
			baselist.add(bid.getInt("id"));
		}
		bid.free();
		
		if( context.getRequest().getParameterInt("preset") == 0 ) {
			echo.append("&Uuml;bereigne Bases an Spieler 0...\n");

			db.update("UPDATE bases " +
					"SET owner=0,name='Verlassener Asteroid',e=0,bewohner=0,arbeiter=0,autogtuacts='' " +
					"WHERE owner="+userid);

			if( baselist.size() > 0 ) {
				db.update("UPDATE fz SET forschung=0,dauer=0 WHERE col IN ("+Common.implode(",",baselist)+")");
				db.update("UPDATE academy SET train=0,remain=0,`upgrade`='' WHERE col IN ("+Common.implode(",",baselist)+")");
				db.update("UPDATE werften SET remaining=0,building=0 WHERE col IN ("+Common.implode(",",baselist)+")");
				db.update("UPDATE weaponfactory SET produces='' WHERE col IN ("+Common.implode(",",baselist)+")");
			}
		} 
		else {
			echo.append("&Uuml;bereigne Basen an Spieler 0 (+ reset)...\n");

			Cargo emptycargo = new Cargo();

			db.update("UPDATE bases " +
					"SET owner=0,bebauung='0',active='0',core='0',coreactive='0',name='Leerer Asteroid'," +
						"e=0,bewohner=0,arbeiter=0,cargo='"+emptycargo.save()+"',autogtuacts='' " +
					"WHERE owner="+userid);

			if( baselist.size() > 0 ) {
				db.update("DELETE FROM fz WHERE col IN ("+Common.implode(",",baselist)+")");
				db.update("DELETE FROM academy WHERE col IN ("+Common.implode(",",baselist)+")");
				db.update("DELETE FROM werften WHERE col IN ("+Common.implode(",",baselist)+")");
				db.update("DELETE FROM weaponfactory WHERE col IN ("+Common.implode(",",baselist)+")");
			}
		}
		echo.append(baselist.size()+" Basen bearbeitet<br />\n");

		echo.append("Entferne Handelseintr&auml;ge...<br />\n");
		db.update("DELETE FROM handel WHERE who="+userid);

		echo.append("&Uuml;berstelle GTU-Gebote an die GTU (-2)...<br />");
		db.update("UPDATE versteigerungen SET bieter=-2 WHERE bieter="+userid);
		db.update("UPDATE versteigerungen_pakete SET bieter=-2 WHERE bieter="+userid);

		echo.append("L&ouml;sche PM's...\n");
		db.update("DELETE FROM transmissionen WHERE empfaenger="+userid);
		echo.append(db.affectedRows()+" gel&ouml;scht<br />\n");
		db.update("UPDATE transmissionen SET sender=0 WHERE sender="+userid);

		echo.append("L&ouml;sche Usereintrag...<br />\n");
		db.update("DELETE FROM users WHERE id="+userid);

		echo.append("L&ouml;sche Diplomatieeintr&auml;ge...<br />\n");
		db.update("DELETE FROM user_relations WHERE user_id="+userid+" OR target_id="+userid);

		echo.append("L&ouml;sche Kontobewegungen...<br />\n");
		db.update("DELETE FROM user_moneytransfer WHERE `from`=",userid," OR `to`=",userid);

		echo.append("L&ouml;sche Userlogo...<br />\n");
		new File(Configuration.getSetting("ABSOLUTE_PATH")+"data/logos/user/"+userid+".gif").delete();
		
		echo.append("L&ouml;sche Offiziere...<br />\n");
		db.update("DELETE FROM offiziere WHERE userid="+userid);
		
		echo.append("L&ouml;sche PM-Ordner...<br />\n");
		db.update("DELETE FROM ordner WHERE playerid="+userid);

		db.tCommit();
		
		echo.append("<br />Spieler "+userid+" gel&ouml;scht!<br />\n");		
		
		echo.append(Common.tableEnd());
	}

}
