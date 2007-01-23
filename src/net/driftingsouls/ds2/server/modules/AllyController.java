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
package net.driftingsouls.ds2.server.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserIterator;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;

/**
 * Zeigt die Allianzseite an
 * @author Christopher Jung
 *
 * @urlparam String show Die anzuzeigende Unterseite
 */
public class AllyController extends DSGenerator implements Loggable {
	private static final double MAX_POSTENCOUNT = 0.3;
	
	private SQLResultRow ally = null;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public AllyController(Context context) {
		super(context);
		
		setTemplate("ally.html");
		
		parameterString("show");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		if( user.getAlly() > 0 ) {
			this.ally = db.first("SELECT * FROM ally WHERE id=",user.getAlly());
		}

		t.set_var(	"ally",	user.getAlly(),
					"show",	this.getString("show") );
					
		return true;	
	}
	
	private void checkLowMember() {
		Database db = getDatabase();
		
		// Ist der Praesident kein NPC (negative ID) ?
		if( this.ally.getInt("president") > 0 ) {
			int count = db.first("SELECT count(*) count FROM users WHERE ally=",this.ally.getInt("id")).getInt("count");
			if( count < 3 ) {
				Taskmanager.getInstance().addTask(Taskmanager.Types.ALLY_LOW_MEMBER, 21, Integer.toString(this.ally.getInt("id")), "", "" );
			
				SQLQuery supermemberid = db.query("SELECT DISTINCT id FROM users WHERE ally=",this.ally.getInt("id")," AND (allyposten!=0 OR id=",this.ally.getInt("president"),")");
				while( supermemberid.next() ) {
					PM.send(getContext(), 0, supermemberid.getInt("id"), "Drohende Allianzaufl&oum;sung", "[Automatische Nachricht]\nAchtung!\nDurch den j&uuml;ngsten Weggang eines Allianzmitglieds hat deine Allianz zu wenig Mitglieder um weiterhin zu bestehen. Du hast nun 21 Ticks Zeit diesen Zustand zu &auml;ndern. Andernfalls wird die Allianz aufgel&ouml;&szlig;t.");
				}
				supermemberid.free();				
			}
		}
	}
	
	/**
	 * Leitet die Gruendung einer Allianz ein. Die Aktion erstellt
	 * die notwendigen Tasks und benachrichtigt die Unterstuetzer der Gruendung
	 * @urlparam String name Der Name der neuen Allianz
	 * @urlparam Integer confuser1 Die User-ID des ersten Unterstuetzers
	 * @urlparam Integer confuser2 Die User-ID des zweiten Unterstuetzers
	 *
	 */
	public void foundAction() {
		Database db = getDatabase();
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
	
		if( user.getAlly() > 0 ) {
			t.set_var( "ally.message", "Fehler: Sie sind bereits Mitglied in einer Allianz und k&ouml;nnen daher keine neue Allianz gr&uuml;nden" );
			
			redirect();
			return;
		}
	
		Taskmanager taskmanager = Taskmanager.getInstance();
	
		Task[] tasks = taskmanager.getTasksByData( Taskmanager.Types.ALLY_NEW_MEMBER, "*", Integer.toString(user.getID()), "*" );
		if( tasks.length > 0 ) {
			t.set_var( "ally.message", "Fehler: Sie haben bereits einen Aufnahmeantrag bei einer Allianz gestellt" );
			
			redirect("defaultNoAlly");
			return;	
		}
		
		parameterString("name");
		parameterNumber("confuser1");
		parameterNumber("confuser2");
		String name = getString("name");
		int confuser1 = getInteger("confuser1");
		int confuser2 = getInteger("confuser2");
		
		confuser1 = db.first("SELECT id FROM users WHERE id=",confuser1," AND id NOT IN (",user.getID(),",",confuser2,") AND ally=0").getInt("id");
		confuser2 = db.first("SELECT id FROM users WHERE id=",confuser2," AND id NOT IN (",user.getID(),",",confuser2,") AND ally=0").getInt("id");
	
		if( (confuser1 == 0) || (confuser2 == 0) ) {
			t.set_var("ally.statusmessage", "<span style=\"color:red\">Einer der angegebenen Unterst&uuml;tzer ist ung&uuml;ltig</span>\n");
			
			redirect("defaultNoAlly");
			return; 	
		}
		
		tasks = taskmanager.getTasksByData( Taskmanager.Types.ALLY_FOUND_CONFIRM, "*", Integer.toString(confuser1), "*" );
		if( tasks.length > 0 ) {
			confuser1 = 0;	
		}
		else {
			tasks = taskmanager.getTasksByData( Taskmanager.Types.ALLY_NEW_MEMBER, "*", Integer.toString(confuser1), "*" );
			if( tasks.length > 0 ) {
				confuser1 = 0;	
			}
		}
	
		tasks = taskmanager.getTasksByData( Taskmanager.Types.ALLY_FOUND_CONFIRM, "*", Integer.toString(confuser2), "*" );
		if( tasks.length > 0 ) {
			confuser2 = 0;	
		}
		else {
			tasks = taskmanager.getTasksByData( Taskmanager.Types.ALLY_NEW_MEMBER, "*", Integer.toString(confuser2), "*" );
			if( tasks.length > 0 ) {
				confuser2 = 0;	
			}
		}
	
		if( (confuser1 == 0) || (confuser2 == 0) ) {
			t.set_var("ally.statusmessage", "<span style=\"color:red\">Einer der angegebenen Unterst&uuml;tzer ist ung&uuml;ltig</span>\n");
			
			redirect("defaultNoAlly");
			return; 	
		}
	
		String mastertaskid = taskmanager.addTask(Taskmanager.Types.ALLY_FOUND, 21, "2", name, user.getID()+","+confuser1+","+confuser2 );
		String conf1taskid = taskmanager.addTask(Taskmanager.Types.ALLY_FOUND_CONFIRM, 21, mastertaskid, Integer.toString(confuser1), "" );
		String conf2taskid = taskmanager.addTask(Taskmanager.Types.ALLY_FOUND_CONFIRM, 21, mastertaskid, Integer.toString(confuser2), "" );
	
		PM.send( getContext(), user.getID(), confuser1, "Allianzgr&uuml;ndung", "[automatische Nachricht]\nIch habe vor die Allianz "+name+" zu gr&uuml;nden. Da zwei Spieler dieses vorhaben unterst&uuml;tzen m&uuml;ssen habe ich mich an dich gewendet.\nAchtung: Durch die Unterst&uuml;tzung wirst du automatisch Mitglied!\n\n[_intrnlConfTask="+conf1taskid+"]Willst du die Allianzgr&uuml;ndung unterst&uuml;tzen?[/_intrnlConfTask]", false, PM.FLAGS_IMPORTANT);
		PM.send( getContext(), user.getID(), confuser2, "Allianzgr&uuml;ndung", "[automatische Nachricht]\nIch habe vor die Allianz "+name+" zu gr&uuml;nden. Da zwei Spieler dieses vorhaben unterst&uuml;tzen m&uuml;ssen habe ich mich an dich gewendet.\nAchtung: Durch die Unterst&uuml;tzung wirst du automatisch Mitglied!\n\n[_intrnlConfTask="+conf2taskid+"]Willst du die Allianzgr&uuml;ndung unterst&uuml;tzen?[/_intrnlConfTask]", false, PM.FLAGS_IMPORTANT);
	
		user.setAlly(-1);
	
		t.set_var( "ally.statusmessage", "Die beiden angegebenen Spieler wurden via PM benachrichtigt. Sollten sich beide zur Unterst&uuml;tzung entschlossen haben, wird die Allianz augenblicklich gegr&uuml;ndet. Du wirst au&szlig;erdem via PM benachrichtigt." );
		
		return;
	}
	
	/**
	 * Leitet den Beitritt zu einer Allianz ein.
	 * Die Aktion erstellt die notwendige Task und benachrichtigt
	 * die "Minister" und den Praesident der Zielallianz
	 *
	 * @urlparam Integer join Die ID der Allianz, der der Benuzter beitreten moechte
	 * @urlparam String conf Bestaetigt den Aufnahmewunsch falls der Wert "ok" ist
	 */
	public void joinAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
	
		parameterString("conf");
		parameterNumber("join");
		
		String conf = getString("conf");
		int join = getInteger("join");
	
		if( user.getAlly() > 0 ) {
			t.set_var( "ally.message", "Sie sind bereits in einer Allianz. Sie m&uuml;ssen diese erst verlassen um in eine andere Allianz eintreten zu k&ouml;nnen!" );
			
			redirect("defaultNoAlly");
			return;	
		}
		
		SQLResultRow al = db.first("SELECT name,id,president FROM ally WHERE id=",join);
		if( al.isEmpty() ) {
			t.set_var( "ally.message", "Die angegebene Allianz existiert nicht" );
			
			redirect("defaultNoAlly");
			return;	
		}
	
		Taskmanager taskmanager = Taskmanager.getInstance();
		Task[] tasks = taskmanager.getTasksByData( Taskmanager.Types.ALLY_FOUND_CONFIRM, "*", Integer.toString(user.getID()), "*" );
		if( tasks.length > 0 ) {
			t.set_var( "ally.message", "Es gibt eine oder mehrere Anfragen an sie zwecks Unterst&uuml;tzung einer Allianzgr&uuml;ndung. Sie m&uuml;ssen diese Anfragen erst bearbeiten bevor sie einer Allianz beitreten k&ouml;nnen." );
			
			redirect("defaultNoAlly");
			return;	
		}
		
		tasks = taskmanager.getTasksByData( Taskmanager.Types.ALLY_NEW_MEMBER, "*", Integer.toString(user.getID()), "*" );
		if( tasks.length > 0 ) {
			t.set_var( "ally.message", "Fehler: Sie haben bereits einen Aufnahmeantrag bei einer Allianz gestellt" );
			
			redirect("defaultNoAlly");
			return;	
		}
	
		if( !conf.equals("ok") ) {	
			t.set_var(	"ally.statusmessage",			"Wollen sie der Allianz &gt;"+Common._title(al.getString("name"))+"&lt; wirklich beitreten?",
						"ally.statusmessage.ask.url1",	"&amp;action=join&amp;join=$join&amp;conf=ok",
						"ally.statusmessage.ask.url2",	"" );
								
			redirect("defaultNoAlly");
			return;
		}
		
		String taskid = taskmanager.addTask(Taskmanager.Types.ALLY_NEW_MEMBER, 35, Integer.toString(join), Integer.toString(user.getID()), "");
		
		SQLQuery supermemberid = db.query("SELECT DISTINCT u.id FROM users u JOIN ally a ON u.ally=a.id WHERE u.ally=",join," AND (u.allyposten!=0 OR u.id=a.president)");
		while( supermemberid.next() ) {
			PM.send(getContext(), 0, supermemberid.getInt("id"), "Aufnahmeantrag", "[Automatische Nachricht]\nHiermit beantrage ich die Aufnahme in die Allianz.\n\n[_intrnlConfTask="+taskid+"]Wollen sie dem Aufnahmeantrag zustimmen?[/_intrnlConfTask]", false, PM.FLAGS_IMPORTANT);
		}
		supermemberid.free();	
	
		t.set_var( "ally.statusmessage", "Der Aufnahmeantrag wurde weitergeleitet. Die Bearbeitung kann jedoch abh&auml;ngig von der Allianz l&auml;ngere Zeit in anspruch nehmen. Sollten sie aufgenommen werden, wird automatisch eine PM an sie gesendet." );
			
		return;
	}
	
	/**
	 * Loescht einen Allianz-Posten
	 * @urlparam Integer postenid Die ID des zu loeschenden Postens
	 *
	 */
	public void deletePostenAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		if( this.ally.getInt("president") != user.getID()) {
			t.set_var("ally.message","Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren");
			redirect();
			
			return;
		}
		
		parameterNumber("postenid");
		int postenid = getInteger("postenid");

		db.tBegin();
		db.update("UPDATE users SET allyposten=0 WHERE allyposten=",postenid);
		db.tUpdate(1, "DELETE FROM ally_posten WHERE id=",postenid);
		db.tCommit();
	
		t.set_var( "ally.statusmessage", "Posten gel&ouml;scht");
		
		redirect();
	}
	
	/**
	 * Weisst einem Posten einen neuen Users zu
	 * @urlparam Integer user Die ID des neuen Inhabers des Postens
	 * @urlparam Integer id Die ID des zu besetzenden Postens
	 *
	 */
	public void editPostenAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		if( this.ally.getInt("president") != user.getID()) {
			t.set_var("ally.message","Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren");
			redirect();
			
			return;
		}
		
		parameterNumber("user");
		parameterNumber("id");
		int userid = getInteger("id");
		int postenid = getInteger("id");
		
		if( userid == 0 ) {
			t.set_var( "ally.message", "Fehler: Sie m&uuml;ssen den Posten jemandem zuweisen" );
			redirect();
			
			return;
		}

		User formuser = getContext().createUserObject(userid);
		if( formuser.getAllyPosten() != 0 ) {
  			t.set_var( "ally.message", "Fehler: Jedem Mitglied darf maximal ein Posten zugewiesen werden" );
  			redirect();
  			
			return;
		}

		db.tBegin();
		db.tUpdate(1, "UPDATE users SET allyposten=0 WHERE allyposten=",postenid," AND ally=",this.ally.getInt("id"));
		db.tUpdate(1, "UPDATE users SET allyposten=",postenid," WHERE id=",userid," AND allyposten=0");
		db.tCommit();

		t.set_var( "ally.statusmessage", "&Auml;nderungen gespeichert" );
		redirect();
	}
	
	/**
	 * Erstellt einen neuen Posten
	 * @urlparam String name Der Name des neuen Postens
	 * @urlparam Integer user Die ID des Benutzers, der den Posten innehaben soll
	 *
	 */
	public void addPostenAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		if( this.ally.getInt("president") != user.getID()) {
			t.set_var("ally.message","Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren");
			redirect();
			
			return;
		}
		
		parameterString("name");
		parameterNumber("user");
		String name = getString("name");
		int userid = getInteger("user");

		if( name.length() == 0 ) {
			t.set_var( "ally.message", "Fehler: Sie m&uuml;ssen dem Posten einen Namen geben" );
			this.redirect();
			return;
		}

		User formuser = createUserObject(userid);
		if( formuser.getAllyPosten() != 0 ) {
  			t.set_var( "ally.message", "Fehler: Jedem Mitglied darf maximal ein Posten zugewiesen werden" );
  			redirect();
			return;
		}

		int postencount = db.first("SELECT count(*) postencount FROM ally_posten WHERE ally=",this.ally.getInt("id")).getInt("postencount");
		int membercount = db.first("SELECT count(*) membercount FROM users WHERE ally=",this.ally.getInt("id")).getInt("membercount");

		int maxposten = (int)Math.round(membercount*MAX_POSTENCOUNT);
		if( maxposten < 2 ) {
			maxposten = 2;
		}

		if( maxposten <= postencount ) {
			t.set_var( "ally.message", "Fehler: Sie haben bereits die maximale Anzahl an Posten erreicht" );
			redirect();
			return;
		}

		db.tBegin();
		PreparedQuery insert = db.prepare("INSERT INTO ally_posten (ally,name) VALUES ( ?, ? )");
		insert.tUpdate(1, this.ally.getInt("id"), name);
		formuser.setAllyPosten(insert.insertID());
		insert.close();
		db.tCommit();

		t.set_var( "ally.statusmessage", "Der Posten "+Common._plaintitle(name)+" wurde erstellt und zugewiesen" );
		redirect();
	}
	
	/**
	 * Erstellt fuer die Allianz einen neuen Comnet-Kanal
	 * @urlparam String name Der Name des neuen Kanals
	 * @urlparam String read Der Zugriffsmodus (all, ally, player)
	 * @urlparam String readids Falls der Lesemodus player ist: Die Komma-separierte Liste der Spieler-IDs
	 * @urlparam String write Der Zugriffsmodus fuer Schreibrechte (all, ally, player)
	 * @urlparam String readids Falls der Schreibmodus player ist: Die Komma-separierte Liste der Spieler-IDs
	 *
	 */
	public void createChannelAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		if( this.ally.getInt("president") != user.getID()) {
			t.set_var( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect();
			return;
		}
		
		int count = db.first("SELECT count(id) as count FROM skn_channels WHERE allyowner=",this.ally.getInt("id")).getInt("id");
		if( count >= 2 ) {
			t.set_var( "ally.message", "Fehler: Ihre Allianz besitzt bereits zwei Frequenzen" );
			redirect();
			return;
		}
		
		parameterString("name");
		parameterString("read");
		parameterString("write");
		parameterString("readids");
		parameterString("writeids");
		String name = getString("name");
		String read = getString("read");
		String write = getString("write");
		String readids = getString("readids");
		String writeids = getString("writeids");
		
		if( name.length() == 0 ) {
			t.set_var( "ally.message", "Fehler: Sie haben keinen Namen f&uuml;r die Frequenz eingegeben" );
			redirect();
			return;
		}
		
		PreparedQuery query = db.prepare("INSERT INTO skn_channels (name, readall, readnpc, readally, readplayer, writeall, writenpc, writeally, writeplayer, allyowner) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		
		query.setString(1, name);
		query.setInt(2, 0); // readall
		query.setInt(3, 0); // readnpc
		query.setInt(4, 0); // readally
		query.setString(5, ""); // readplayer
		query.setInt(6, 0); // writeall
		query.setInt(7, 0); // writenpc
		query.setInt(8, 0); // writeally
		query.setString(9, ""); // writeplayer
		query.setInt(10, this.ally.getInt("id")); // allyowner
		
		if( read.equals("all") ) {
			query.setInt(2, 1);
		}
		else if( read.equals("ally") ) {
			query.setInt(4, this.ally.getInt("id"));
		}
		else if( read.equals("player") ) {
			readids = Common.implode(",", Common.explodeToInteger(",",readids));
			query.setString(5, readids); 
		}

		if( write.equals("all") ) {
			query.setInt(6, 1);
		}
		else if( write.equals("ally") ) {
			query.setInt(8, this.ally.getInt("id"));
		}
		else if( write.equals("player") ) {
			writeids = Common.implode(",", Common.explodeToInteger(",",writeids));
			query.setString(9, writeids); 
		}
		query.update();
		query.close();

		t.set_var( "ally.statusmessage", "Frequenz "+Common._title(name)+" hinzugef&uuml;gt" );
		redirect();	
	}
	
	/**
	 * Setzt Namen und Zugriffrechte fuer einen Allianz-Comnet-Kanal
	 * @urlparam Integer edit Die ID des Comnet-Kanals
	 * @urlparam String name Der neue Name
	 * @urlparam String read Der Zugriffsmodus (all, ally, player)
	 * @urlparam String readids Falls der Lesemodus player ist: Die Komma-separierte Liste der Spieler-IDs
	 * @urlparam String write Der Zugriffsmodus fuer Schreibrechte (all, ally, player)
	 * @urlparam String readids Falls der Schreibmodus player ist: Die Komma-separierte Liste der Spieler-IDs
	 * 
	 */
	public void editChannelAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		if( this.ally.getInt("president") != user.getID()) {
			t.set_var( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect();
			return;
		}
		
		this.parameterNumber("edit");
		int edit = getInteger("edit");

		SQLResultRow allyowner = db.first("SELECT allyowner FROM skn_channels WHERE id=",edit);
		if( allyowner.isEmpty() || (allyowner.getInt("id") != this.ally.getInt("id")) ) {
			t.set_var( "ally.message", "Fehler: Diese Frequenz geh&ouml;rt nicht ihrer Allianz" );
			return;
		}
		
		parameterString("name");
		parameterString("read");
		parameterString("write");
		parameterString("readids");
		parameterString("writeids");
		String name = getString("name");
		String read = getString("read");
		String write = getString("write");
		String readids = getString("readids");
		String writeids = getString("writeids");
		
		if( name.length() == 0 ) {
			t.set_var( "ally.message", "Fehler: Sie haben keinen Namen f&uuml;r die Frequenz eingegeben" );
			redirect();
			return;
		}
		
		PreparedQuery query = db.prepare("UPDATE skn_channels SET name=?, readall= ?, readnpc= ?, readally= ?, readplayer= ?," +
			"writeall= ?, writenpc= ?, writeally= ?, writeplayer= ? WHERE id= ?");
		
		query.setString(1, name);
		query.setInt(2, 0); // readall
		query.setInt(3, 0); // readnpc
		query.setInt(4, 0); // readally
		query.setString(5, ""); // readplayer
		query.setInt(6, 0); // writeall
		query.setInt(7, 0); // writenpc
		query.setInt(8, 0); // writeally
		query.setString(9, ""); // writeplayer
		query.setInt(10, edit); // id
		
		if( read.equals("all") ) {
			query.setInt(2, 1);
		}
		else if( read.equals("ally") ) {
			query.setInt(4, this.ally.getInt("id"));
		}
		else if( read.equals("player") ) {
			readids = Common.implode(",", Common.explodeToInteger(",",readids));
			query.setString(5, readids); 
		}

		if( write.equals("all") ) {
			query.setInt(6, 1);
		}
		else if( write.equals("ally") ) {
			query.setInt(8, this.ally.getInt("id"));
		}
		else if( write.equals("player") ) {
			writeids = Common.implode(",", Common.explodeToInteger(",",writeids));
			query.setString(9, writeids); 
		}
		query.update();
		query.close();
		
		t.set_var( "ally.statusmessage", "Frequenz "+Common._plaintitle(name)+" ge&auml;ndert" );
		redirect();	
	}
	
	/**
	 * Loescht einen Comnet-Kanal der Allianz
	 * @urlparam Integer channel Die ID des zu loeschenden Kanals
	 * @urlparam String conf Die Bestaetigung des Vorgangs. <code>ok</code>, falls der Vorgang durchgefuehrt werden soll
	 *
	 */
	public void deleteChannelAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		if( this.ally.getInt("president") != user.getID()) {
			t.set_var( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect();
			return;
		}
		
		this.parameterNumber("channel");
		int channelID = getInteger("channel");
	
		SQLResultRow channel = db.first("SELECT id,name,allyowner FROM skn_channels WHERE id=",channelID);
		if( channel.isEmpty() || (channel.getInt("allyowner") != this.ally.getInt("id")) ) {
			t.set_var( "ally.message", "Fehler: Diese Frequenz geh&ouml;rt nicht ihrer Allianz" );
			redirect();
			return;
		}
	
		this.parameterString("conf");
		String conf = getString("conf");
		String show = getString("show");
	
		if( !conf.equals("ok") ) {
			t.set_var(	"ally.statusmessage",			"Wollen sie die Frequenz \""+Common._title(channel.getString("name"))+"\" wirklich l&ouml;schen?",
						"ally.statusmessage.ask.url1",	"&amp;action=deleteChannel&amp;channel="+channel.getInt("id")+"&amp;conf=ok&amp;show="+show,
						"ally.statusmessage.ask.url2",	"&amp;show="+show );
			return;
		} 

		db.tBegin();
		db.update("DELETE FROM skn WHERE channel="+channel.getInt("id"));
		db.update("DELETE FROM skn_visits WHERE channel="+channel.getInt("id"));
		db.update("DELETE FROM skn_channels WHERE id="+channel.getInt("id"));
		db.tCommit();
	
		t.set_var( "ally.statusmessage", "Die Frequenz wurde gel&ouml;scht" );
		redirect();
	}
	
	private static final int MAX_UPLOAD_SIZE = 307200;
	
	/**
	 * Laedt das neue Logo der Allianz auf den Server
	 *
	 */
	public void uploadLogoAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		if( this.ally.getInt("president") != user.getID()) {
			t.set_var( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect();
			return;
		}

		List<FileItem> list = getContext().getRequest().getUploadedFiles();
		if( list.size() == 0 ) {
			redirect();
			return;
		}
		
		if( list.get(0).getSize() > MAX_UPLOAD_SIZE ) {
			t.set_var("options.message","Das Logo ist leider zu gro&szlig;. Bitte w&auml;hle eine Datei mit maximal 300kB Gr&ouml;&stlig;e<br />");
			redirect();
			return;
		}
		
		String uploaddir = Configuration.getSetting("ABSOLUTE_PATH")+"data/logos/ally/";
		try {
			File uploadedFile = new File(uploaddir+getUser().getID()+".gif");
			list.get(0).write(uploadedFile);
			t.set_var("options.message","Das neue Logo wurde auf dem Server gespeichert<br />");
		}
		catch( Exception e ) {
			t.set_var("options.message","Offenbar ging beim Upload etwas schief (Ist die Datei evt. zu gro&szlig;?)<br />");
			LOG.warn(e);
		}
		
		redirect();
	}
	
	/**
	 * Speichert die neuen Daten der Allianz
	 * @urlparam String name Der Name der Allianz
	 * @urlparam String desc Die Allianzbeschreibung
	 * @urlparam String allytag Der Allianztag
	 * @urlparam String hp Die URL zur Homepage
	 * @urlparam String praesi Der Name des Praesidentenpostens
	 * @urlparam Integer showastis Sollen eigene Astis auf der Sternenkarte angezeigt werden (<code>1</code>) oder nicht (<code>0</code>)
	 * @urlparam Integer showGtuBieter Sollen Allymember einander bei GTU-Versteigerungen sehen koennen (<code>1</code>) oder nicht (<code>0</code>)
	 * @urlparam Integer showlrs Sollen die LRS der Awacs in der Sternenkarte innerhalb der Ally geteilt werden (<code>1</code>) oder nicht (<code>0</code>)
	 *
	 */
	public void changeSettingsAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		if( this.ally.getInt("president") != user.getID()) {
			t.set_var( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect();
			return;
		}
		
		this.parameterString("name");
		this.parameterString("desc");
		this.parameterString("allytag");
		this.parameterString("hp");
		this.parameterString("praesi");
		this.parameterNumber("showastis");
		this.parameterNumber("showGtuBieter");
		this.parameterNumber("showlrs");
		
		String name = this.getString("name");
		String desc = this.getString("desc");
		String allytag = this.getString("allytag");
		String hp = this.getString("hp");
		String praesi = this.getString("praesi");
		int showastis = this.getInteger("showastis") != 0 ? 1 : 0;
		int showGtuBieter = this.getInteger("showGtuBieter") != 0 ? 1 : 0;
		int showlrs = this.getInteger("showlrs") != 0 ? 1 : 0;

		// Wurde der [name]-Tag vergessen?
		if( allytag.indexOf("[name]") == -1 ) {
			t.set_var( "ally.message", "Warnung: Der [name]-tag wurde vergessen. Dieser wird nun automatisch angeh&auml;ngt!" );
			allytag += "[name]";
		}
	
		if( name.length() == 0 ) {
			t.set_var( "ally.message", "Fehler: Sie m&uuml;ssen einen Allianznamen angeben" );
			redirect();
			return;	
		}
	
		if( praesi.length() == 0 ) {
			t.set_var( "ally.message", "Fehler: Sie m&uuml;ssen dem Pr&auml;sidentenamt einen Namen geben" );
			redirect();
			return;	
		}

		db.tBegin();
		
		db.prepare("UPDATE ally ",
						"SET name= ?, plainname= ?, description= ?, hp= ?, allytag= ?, ",
						"	showastis= ?, showGtuBieter= ?, pname= ?, showlrs= ? ",
						"WHERE id= ? AND name= ? AND description= ? AND " +
						"	hp= ? AND allytag= ? AND showastis= ? AND ", 
						"	pname= ? AND showGtuBieter= ? AND showlrs= ?")
			.tUpdate(1, name, Common._titleNoFormat(name), desc, hp, allytag, showastis, showGtuBieter, praesi, showlrs,
					this.ally.getInt("id"), this.ally.getString("name"), this.ally.getString("description"), 
					this.ally.getString("hp"), this.ally.getString("allytag"), this.ally.getBoolean("showastis") ? 1 : 0, 
					this.ally.getString("pname"), this.ally.getInt("showGtuBieter"), this.ally.getInt("showlrs") );

		//Benutzernamen aktualisieren
		UserIterator allyusers = getContext().createUserIterator("SELECT * FROM users WHERE ally=",this.ally.getInt("id"));
		for( User auser : allyusers ) {
			String newname = StringUtils.replace(allytag, "[name]", auser.getNickname());

			if( !newname.equals(auser.getName()) ) {
				auser.setName(newname);
			}
		}
		allyusers.free();
		
		if( db.tCommit() ) {	
			t.set_var( "ally.statusmessage", "Neue Daten gespeichert..." );
			
			this.ally.put("name", name);
			this.ally.put("plainname", Common._titleNoFormat(name));
			this.ally.put("description", desc);
			this.ally.put("hp", hp);
			this.ally.put("allytag", allytag);
			this.ally.put("showastis", showastis != 0 ? true : false);
			this.ally.put("showGtuBieter", showGtuBieter);
			this.ally.put("pname", praesi);
			this.ally.put("showlrs", showlrs);
		}

		redirect();	
	}

	/**
	 * Laesst den aktuellen Spieler aus der Allianz austreten
	 * @urlparam String conf Falls <code>ok</code> wird der Austritt vollzogen
	 *
	 */
	public void partAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
	
		if( this.ally.getInt("president") != user.getID()) {
			t.set_var( "ally.message", "<span style=\"color:red\">Sie k&ouml;nnen erst austreten, wenn ein anderer Pr&auml;sident bestimmt wurde" );
			redirect();
			return;
		}
	
		parameterString("conf");
		String conf = getString("conf");
	
		if( !conf.equals("ok") ) {
			String show = getString("show");
		
			t.set_var(	"ally.statusmessage",				"Wollen sie wirklich aus der Allianz austreten?",
						"ally.statusmessage.ask.url1",		"&amp;action=part&amp;conf=ok&amp;show="+show,
						"ally.statusmessage.ask.url2",		"&amp;show="+show );
			return;
		}
		
		PM.send(getContext(), user.getID(), this.ally.getInt("president"), "Allianz verlassen", "Ich habe die Allianz verlassen");
		user.setAlly(0);
		user.setAllyPosten(0);
		user.setName(user.getNickname());
		
		db.update("UPDATE battles SET ally1=0 WHERE commander1=",user.getID()," AND ally1=",this.ally.getInt("id"));
		db.update("UPDATE battles SET ally2=0 WHERE commander2=",user.getID()," AND ally2=",this.ally.getInt("id"));
		
		int ticks = getContext().get(ContextCommon.class).getTick();		
		user.addHistory(Common.getIngameTime(ticks)+": Verlassen der Allianz "+this.ally.getString("name"));
		
		t.set_var( "ally.showmessage", "Allianz verlassen" );
		
		checkLowMember();
		
		this.ally = null;
		t.set_var("ally", 0);
		
		redirect("defaultNoAlly");
	}
	
	/**
	 * Loest einen Allianz auf
	 * @urlparam String conf Bestaetigt die Aufloesung, wenn der Wert <code>ok</code> ist 
	 *
	 */
	public void killAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		if( this.ally.getInt("president") != user.getID()) {
			t.set_var( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect();
			return;
		}
	
		this.parameterString("conf");
		String conf = this.getString("conf");
	
		if( !conf.equals("ok") ) {
			String show = getString("show");
			
			t.set_var(	"ally.statusmessage",			"Wollen sie die Allianz wirklich aufl&ouml;sen?",
						"ally.statusmessage.ask.url1",	"&amp;action=kill&amp;conf=ok&amp;show="+show,
						"ally.statusmessage.ask.url2",	"&amp;show="+show );
		} 
		else {
			db.tBegin();
			PM.send(getContext(), user.getID(), this.ally.getInt("id"), "Allianz aufgel&ouml;st", "Die Allianz wurde mit sofortiger Wirkung aufgel&ouml;st", true );
	
			SQLQuery chn = db.query("SELECT id FROM skn_channels WHERE allyowner=",this.ally.getInt("id"));
			while( chn.next() ) {
				db.query("DELETE FROM skn WHERE channel=",chn.getInt("id"));
				db.query("DELETE FROM skn_visits WHERE channel=",chn.getInt("id"));
				db.query("DELETE FROM skn_channels WHERE id=",chn.getInt("id"));
			}
			chn.free();
			
			int tick = getContext().get(ContextCommon.class).getTick();
			
			UserIterator uid = getContext().createUserIterator("SELECT * FROM users WHERE ally=",this.ally.getInt("id"));
			for( User auser : uid ) {				
				auser.addHistory(Common.getIngameTime(tick)+": Verlassen der Allianz "+this.ally.getString("name")+" im Zuge der Aufl&ouml;sung dieser Allianz");
				auser.setAlly(0);
				auser.setAllyPosten(0);
				auser.setName(auser.getNickname());
			}
			uid.free();
			
			db.query("DELETE FROM ally_posten WHERE ally=",this.ally.getInt("id"));
			db.query("DELETE FROM ally WHERE id=",this.ally.getInt("id"));
			db.tCommit();
		
			t.set_var( "ally.statusmessage", "Die Allianz wurde aufgel&ouml;st" );
			
			this.ally = null;
			t.set_var("ally", 0);
			
			redirect("defaultNoAlly");
		}
	}
	
	/**
	 * Befoerdert einen Spieler zum Praesidenten
	 * @urlparam Integer presn Die ID des neuen Praesidenten der Allianz
	 *
	 */
	public void newPraesiAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		if( this.ally.getInt("president") != user.getID()) {
			t.set_var( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect();
			return;
		}
	
		parameterNumber("presn");
		int presn = getInteger("presn");
		
		User presnuser = getContext().createUserObject(presn);
		if( presnuser.getAlly() != this.ally.getInt("id") ) {
			t.set_var( "ally.message", "Dieser Spieler ist nicht Mitglied ihrer Allianz" );
			redirect();
			return;	
		}
	
		db.update("UPDATE ally SET president=",presnuser.getID(),"' WHERE id='",this.ally.getInt("id"));
		t.set_var( "ally.statusmessage", presnuser.getProfileLink()+" zum Pr&auml;sidenten ernannt" );
	
		PM.send(getContext(), this.ally.getInt("president"), presnuser.getID(), "Zum Pr&auml;sidenten ernannt", "Ich habe dich zum Pr&auml;sidenten der Allianz ernannt");

		this.ally.put("president", presnuser.getID());
		redirect();
	}

	/**
	 * Wirft einen Spieler aus der Allianz
	 * @urlparam Integer kick Die ID des aus der Allianz zu werfenden Spielers
	 *
	 */
	public void kickAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		if( this.ally.getInt("president") != user.getID()) {
			t.set_var( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect();
			return;
		}
		
		parameterNumber("kick");
		int kick = getInteger("kick");
		
		if( kick == user.getID() ) {
			t.set_var( "ally.message", "Sie k&ouml;nnen sich nicht selber aus der Allianz werfen" );
			redirect();
			return;
		}
	
		User kickuser = getContext().createUserObject(kick);
		if( kickuser.getAlly() != this.ally.getInt("id") ) {
			t.set_var( "ally.message", "Dieser Spieler ist nicht Mitglied ihrer Allianz" );
			redirect();
			return;
		}

		kickuser.setAlly(0);
		kickuser.setAllyPosten(0);
		kickuser.setName(kickuser.getNickname());
		
		db.update("UPDATE battles SET ally1=0 WHERE commander1=",kickuser.getID()," AND ally1=",this.ally.getInt("id"));
		db.update("UPDATE battles SET ally2=0 WHERE commander2=",kickuser.getID()," AND ally2=",this.ally.getInt("id"));
		
		int tick = getContext().get(ContextCommon.class).getTick();
		kickuser.addHistory(Common.getIngameTime(tick)+": Verlassen der Allianz "+this.ally.getString("name"));

		t.set_var( "ally.statusmessage", Common._title(kickuser.getName())+" aus der Allianz geworfen" );
		
		checkLowMember();

		PM.send(getContext(), this.ally.getInt("president"), kickuser.getID(), "Aus der Allianz geworfen", "Ich habe dich aus der Allianz geworfen.");
		
		redirect();
	}
	
	/**
	 * Zeigt die GUI, spezifiziert durch den Parameter show,
	 * fuer Spieler ohne Allianz, an
	 *
	 */
	public void defaultNoAllyAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		String show = getString("show");
		
		if( show.equals("create") ) {		
			if( Common.time() - user.getSignup() < 60*60*24*3 ) {
				t.set_var("ally.message","Sie m&uuml;ssen seit mindestens 3 Tage dabei sein um eine Allianz gr&uuml;nden zu k&ouml;nnen");
			}
			else {
				t.set_var("show.create",1);
			}
		}
		else {
			t.set_var( "show.join", 1 );
			t.set_block( "_ALLY", "show.join.allylist.listitem", "show.join.allylist.list" );
			
			SQLQuery al = db.query("SELECT name,id FROM ally ORDER BY founded");
			while( al.next() ) {
				t.set_var(	"show.join.allylist.allyid",	al.getInt("id"),
							"show.join.allylist.name",		Common._title(al.getString("name")) );
									
				t.parse( "show.join.allylist.list", "show.join.allylist.listitem", true );
			}
			al.free();
		}	
	}

	/**
	 * Zeigt die GUI, spezifiziert durch den Parameter show, 
	 * fuer Spieler mit Allianz, an
	 * 
	 * @urlparam Integer destpos Offset fuer die Liste der zerstoerten Schiffe
	 * @urlparam Integer lostpos Offset fuer die Liste der verlorenen Schiffe
	 */
	@Override
	public void defaultAction() {
		Database db = getDatabase();
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		String show = getString("show");
		
		if( this.ally == null ) {
			this.redirect("defaultNoAlly");
			return;	
		}
		
		t.set_var(	"ally.name",		Common._title( this.ally.getString("name") ),
					"user.president",	(user.getID() == this.ally.getInt("president")),
					"ally.id",			this.ally.getInt("id") );
		
		/*
			Allgemeines
		*/
	
		if( (show.length() == 0) || show.equals("allgemein") ) {
			User presi = getContext().createUserObject(this.ally.getInt("president"));
			int membercount = db.first("SELECT count(*) membercount FROM users WHERE ally=",this.ally.getInt("id")).getInt("membercount");
			
			t.set_var(	"show.allgemein",		1,
						"ally.description",		Common._text(this.ally.getString("description")),
						"ally.founded",			this.ally.getString("founded"),
						"ally.wonBattles",		this.ally.getInt("wonBattles"),
						"ally.lostBattles",		this.ally.getInt("lostBattles"),
						"ally.destroyedShips",	this.ally.getInt("destroyedShips"),
						"ally.lostShips",		this.ally.getInt("lostShips"),
						"ally.membercount",		membercount,
						"ally.pname",			Common._plaintitle(this.ally.getString("pname")),
						"ally.president.id",	this.ally.getInt("president"),
						"ally.president.name",	Common._title(presi.getName()),
						"ally.posten.list",		"" );
		
			if( ally.getString("items").length() > 0 ) {
				t.set_block( "_ALLY", "ally.items.listitem", "ally.items.list" );
				Cargo itemlist = new Cargo( Cargo.Type.ITEMSTRING, this.ally.getString("items") );
				ResourceList reslist = itemlist.getResourceList();
				
				Resources.echoResList( t, reslist, "ally.items.list" );
			}
		
			t.set_block( "_ALLY", "ally.posten.listitem", "ally.posten.list" );
			
			SQLQuery posten = db.query("SELECT t2.id,t1.name,t2.name username FROM ally_posten t1 JOIN users t2 ON t1.id=t2.allyposten WHERE t1.ally=",this.ally.getInt("id")," AND t2.ally=",this.ally.getInt("id"));
			while( posten.next() ) {
				t.set_var(	"ally.posten.name",			Common._title(posten.getString("name")),
							"ally.posten.user.name",	Common._title(posten.getString("username")),
							"ally.posten.user.id",		posten.getInt("id") );
				
				t.parse( "ally.posten.list", "ally.posten.listitem", true );
			}
			posten.free();
			
			SQLQuery allymember = db.query("SELECT id,name FROM users WHERE ally=",this.ally.getInt("id")," AND id!=",this.ally.getInt("president")," AND allyposten=0");
			if( allymember.numRows() > 0 ) {
				t.set_var( "ally.addmembers.list", "" );
				t.set_block( "_ALLY", "ally.addmembers.listitem", "ally.addmembers.list" );
				
				while( allymember.next() ) {
					t.set_var(	"ally.addmembers.name",	Common._title(allymember.getString("name")),
								"ally.addmembers.id",	allymember.getInt("id") );
					
					t.parse( "ally.addmembers.list", "ally.addmembers.listitem", true );
				}
			}
			allymember.free();
		}
		
		/*
			Mitgliederliste
		*/
		else if( show.equals("members") ) {
			t.set_var(	"show.members",		1,
						"user.president",	(user.getID() == this.ally.getInt("president")) );
								
			t.set_block( "_ALLY", "show.members.listitem", "show.members.list" );
			
			//Mitglieder auflisten
			SQLQuery mem = db.query("SELECT name,id,inakt FROM users WHERE ally=",this.ally.getInt("id")," ORDER BY name");
			while( mem.next() ) {	
				t.set_var(	"show.members.name",	Common._title( mem.getString("name") ),
							"show.members.id",		mem.getInt("id") );
											
				if( user.getID() == this.ally.getInt("president") ) {
					String inakt_status = "";
					int inakt = mem.getInt("inakt");
					if( inakt <= 14 ) {
						inakt_status = "<span style=\\'color:#00FF00\\'>aktiv</span>";
					}
					else if( inakt <= 49 ) {
						inakt_status = "<span style=\\'color:#22AA22\\'>weniger aktiv</span>";
					}
					else if( inakt <= 98 ) {
						inakt_status = "<span style=\\'color:#668822\\'>selten aktiv</span>";
					}
					else if( inakt <= 196 ) {
						inakt_status = "<span style=\\'color:#884422\\'>inaktiv</span>";
					}
					else if( inakt <= 300 ) {
						inakt_status = "<span style=\\'color:#AA4422\\'>scheintot</span>";
					}
					else {
						inakt_status = "<span style=\\'color:#FF2222\\'>bald gel&ouml;scht</span>";
					}
					
					t.set_var( "show.members.inaktstatus", inakt_status );
				}
				
				t.parse( "show.members.list", "show.members.listitem", true );
			}
			mem.free();
		}
		/*
			Einstellungen
		*/
		else if( show.equals("config") && (user.getID() == this.ally.getInt("president")) ) {
			t.set_var(	"show.einstellungen",	1,
						"ally.plainname",		this.ally.getString("name"),
						"ally.description",		this.ally.getString("description"),
						"ally.hp",				this.ally.getString("hp"),
						"ally.allytag",			this.ally.getString("allytag"),
						"ally.pname",			this.ally.getString("pname"),
						"ally.showastis",		this.ally.getBoolean("showastis"),
						"ally.showgtubieter",	this.ally.getInt("showGtuBieter"),
						"ally.showlrs",			this.ally.getInt("showlrs"),
						"show.einstellungen.channels.list",	"" );
			
			// Zuerst alle vorhandenen Channels dieser Allianz auslesen (max 2)
			List<SQLResultRow> channels = new ArrayList<SQLResultRow>();
			SQLQuery channel = db.query("SELECT * FROM skn_channels WHERE allyowner=",this.ally.getInt("id")," LIMIT 2");
			while( channel.next() ) {
				channels.add(channel.getRow());
			}
			channel.free();
			channels.add(new SQLResultRow());
			
			t.set_block( "_ALLY", "show.einstellungen.channels.listitem", "show.einstellungen.channels.list" );
	
			// Nun die vorhandenen Channels anzeigen und ggf. eine Eingabemaske fuer neue Channels anzeigen
			for( int i=0; i<=1; i++ ) {
				t.start_record();
				t.set_var(	"show.einstellungen.channels.id",		channels.get(i).isEmpty() ? 0 : channels.get(i).getInt("id"),
							"show.einstellungen.channels.index",	i+1 );
				
				if( !channels.get(i).isEmpty() ) {
					t.set_var(	"show.einstellungen.channels.name",			Common._plaintitle(channels.get(i).getString("name")),
								"show.einstellungen.channels.readall",		channels.get(i).getBoolean("readall"),
								"show.einstellungen.channels.writeall",		channels.get(i).getBoolean("writeall"),
								"show.einstellungen.channels.readally",		channels.get(i).getInt("readally"),
								"show.einstellungen.channels.writeally",	channels.get(i).getInt("writeally"),
								"show.einstellungen.channels.readids",		channels.get(i).getString("readplayer"),
								"show.einstellungen.channels.writeids",		channels.get(i).getString("writeplayer") );
				}
				else {
					t.set_var(	"show.einstellungen.channels.name",		"",
								"show.einstellungen.channels.readall",	1,
								"show.einstellungen.channels.writeall",	1 );
				}
					
				t.parse( "show.einstellungen.channels.list", "show.einstellungen.channels.listitem", true );
					
				t.stop_record();
				t.clear_record();
				
				// Maximal eine Eingabemaske anzeigen
				if( channels.get(i).isEmpty() ) {
					break;
				}
			}
		}
		/*
			Posten
		*/
		else if( show.equals("posten") ) {		
			List<SQLResultRow> allymember = new ArrayList<SQLResultRow>();
			SQLQuery memberRow = db.query("SELECT id,nickname FROM  users WHERE ally=",this.ally.getInt("id"));
			while( memberRow.next() ) {
				allymember.add(memberRow.getRow());
			}
			memberRow.free();
	
			int postencount = db.first("SELECT count(*) postencount FROM ally_posten WHERE ally=",this.ally.getInt("id")).getInt("postencount");
	
			int membercount = allymember.size();
			int maxposten = (int)Math.round(membercount*MAX_POSTENCOUNT);
			if( maxposten < 2 ) {
				maxposten = 2;
			}
			
			t.set_var(	"show.posten",				1,
						"show.posten.count",		postencount,
						"show.posten.maxcount",		maxposten,
						"show.posten.addposten",	(maxposten > postencount),
						"show.posten.modify.list",	"" );
								
			t.set_block( "_ALLY", "show.posten.modify.listitem", "show.posten.modify.list" );
			t.set_block( "show.posten.modify.listitem", "show.posten.modify.userlist.listitem", "show.posten.modify.userlist.list" );
			
			SQLQuery posten = db.query("SELECT ap.id,ap.name,u.id userid " +
					"FROM ally_posten ap LEFT OUTER JOIN users u ON ap.id=u.allyposten " +
					"WHERE ap.ally=",this.ally.getInt("id"));
			while( posten.next() ) {
				t.set_var(	"show.posten.modify.name",			Common._plaintitle(posten.getString("name")),
							"show.posten.modify.id",			posten.getInt("id"),
							"show.posten.modify.userlist.list",	"" );
				
				if( posten.getInt("userid") == 0 ) {
					t.set_var(	"show.posten.modify.userlist.id",		"",
								"show.posten.modify.userlist.name",		"KEINER",
								"show.posten.modify.userlist.selected",	1 );
										
					t.parse( "show.posten.modify.userlist.list", "show.posten.modify.userlist.listitem", true );
				}
				
				for( int i=0; i < allymember.size(); i++ ) {	
					SQLResultRow member = allymember.get(i);
					t.set_var(	"show.posten.modify.userlist.id",		member.getInt("id"),
								"show.posten.modify.userlist.name",		Common._title(member.getString("nickname")),
								"show.posten.modify.userlist.selected",	posten.getInt("userid") == member.getInt("id") );
										
					t.parse( "show.posten.modify.userlist.list", "show.posten.modify.userlist.listitem", true );
				}
				
				t.parse( "show.posten.modify.list", "show.posten.modify.listitem", true );
			}
			posten.free();
			
			if( maxposten > postencount ) {
				t.set_block( "_ALLY", "show.posten.addposten.userlist.listitem", "show.posten.addposten.userlist.list" );
				
				for( int i=0; i < allymember.size(); i++ ) {	
					SQLResultRow member = allymember.get(i);
					t.set_var(	"show.posten.addposten.userlist.id",	member.getInt("id"),
								"show.posten.addposten.userlist.name",	Common._title(member.getString("nickname")) );
										
					t.parse( "show.posten.addposten.userlist.list", "show.posten.addposten.userlist.listitem", true );
				}
			}
		}
		/*
			Schlachten
		*/
		else if( show.equals("battles") ) {
			/////////////////////////////
			// Zerstoerte Schiffe
			/////////////////////////////
	
			int counter = 0;
	
			this.parameterNumber("destpos");
			int destpos = getInteger("destpos");
				
			int destcount = db.first("SELECT count(*) count FROM ships_lost WHERE destally=",this.ally.getInt("id")).getInt("count");
			if( destpos > destcount ) {
				destpos = destcount - 10;
			}
	
			if( destpos < 0 ) {
				destpos = 0;
			}
			
			t.set_block( "_ALLY", "show.destships.listitem", "show.destships.list" );
			t.set_block( "_ALLY", "show.destships.linefiller.listitem", "show.destships.linefiller.list" );
			t.set_block( "_ALLY", "show.lostships.listitem", "show.lostships.list" );
			t.set_block( "_ALLY", "show.lostships.linefiller.listitem", "show.lostships.linefiller.list" );
			
			t.set_var(	"show.battles",						1,
						"show.destships.list",				"",
						"show.destships.linefiller.list",	"",
						"show.lostships.list",				"",
						"show.lostships.linefiller.list",	"",
						"show.destpos.back",				destpos-10,
						"show.destpos.forward",				destpos+10 );
	
			SQLQuery s = db.query("SELECT name,type,time,owner FROM ships_lost WHERE destally=",this.ally.getInt("id")," ORDER BY time DESC LIMIT ",destpos,",10");
			while( s.next() ) {
				SQLResultRow shiptype = Ships.getShipType( s.getInt("type"), false );
				
				counter++;
	
				User auser = getContext().createUserObject(s.getInt("owner"));
	
				t.set_var(	"show.destships.name",			s.getString("name"),
							"show.destships.type.name",		shiptype.getString("nickname"),
							"show.destships.type",			s.getInt("type"),
							"show.destships.type.picture",	shiptype.getString("picture"),
							"show.destships.owner",			Common._title(auser.getName()),
							"show.destships.time",			Common.date("d.m.Y H:i:s",s.getLong("time")),
							"show.destships.newrow",		(counter % 5) == 0 );
				
				t.parse( "show.destships.list", "show.destships.listitem", true );
			}
			s.free();
			
			while( counter % 5 != 0 ) {
				t.parse( "show.destships.linefiller.list", "show.destships.linefiller.listitem", true );
				counter++;
			}
	
			/////////////////////////////
			// Verlorene Schiffe
			/////////////////////////////
	
			counter = 0;
	
			parameterNumber("lostpos");
			int lostpos = getInteger("lostpos");
	
			int lostcount = db.first("SELECT count(*) count FROM ships_lost WHERE ally=",this.ally.getInt("id")).getInt("count");
			if( lostpos > lostcount ) {
				lostpos = lostcount - 10;
			}
	
			if( lostpos < 0 ) {
				lostpos = 0;
			}
			
			t.set_var(	"show.lostpos.back",	lostpos-10,
						"show.lostpos.forward",	lostpos+10 );
	
			s = db.query("SELECT name,type,time,owner,destowner FROM ships_lost WHERE ally=",this.ally.getInt("id")," ORDER BY time DESC LIMIT ",lostpos,",10");
			while( s.next() ) {			
				SQLResultRow shiptype = Ships.getShipType( s.getInt("type"), false );
				
				counter++;
				
				User destowner = getContext().createUserObject(s.getInt("destowner"));
				User owner = getContext().createUserObject(s.getInt("owner"));
							
				t.set_var(	"show.lostships.name",			s.getString("name"),
							"show.lostships.type.name",		shiptype.getString("nickname"),
							"show.lostships.type",			s.getInt("type"),
							"show.lostships.type.picture",	shiptype.getString("picture"),
							"show.lostships.owner",			Common._title(destowner.getName()),
							"show.lostships.destroyer",		Common._title(owner.getName()),
							"show.lostships.time",			Common.date("d.m.Y H:i:s",s.getLong("time")),
							"show.lostships.newrow",		(counter % 5) == 0 );
				
				t.parse( "show.lostships.list", "show.lostships.listitem", true );
			}
			s.free();
			while( counter % 5 != 0 ) {
				t.parse( "show.lostships.linefiller.list", "show.lostships.linefiller.listitem", true );
				counter++;
			}
		}
	}
}
