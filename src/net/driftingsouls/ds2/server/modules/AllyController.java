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
import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.Ally;
import net.driftingsouls.ds2.server.entities.AllyPosten;
import net.driftingsouls.ds2.server.entities.ComNetChannel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipLost;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Zeigt die Allianzseite an
 * @author Christopher Jung
 */
public class AllyController extends TemplateGenerator {
	private static final Log log = LogFactory.getLog(AllyController.class);
	private static final double MAX_POSTENCOUNT = 0.3;
	
	private Ally ally = null;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public AllyController(Context context) {
		super(context);
		
		setTemplate("ally.html");
		
		parameterString("show");
		
		setPageTitle("Allianz");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		
		this.ally = user.getAlly();

		t.setVar(	"ally",	user.getAlly() != null ? user.getAlly().getId() : 0,
					"show",	this.getString("show") );
		
		if( this.ally != null ) {
			t.setVar(
					"ally.name",		Common._title( this.ally.getName() ),
					"user.president",	(user.getId() == this.ally.getPresident().getId()),
					"ally.id",			this.ally.getId() );
			
			addPageMenuEntry("Allgemeines", Common.buildUrl("default"));
			addPageMenuEntry("Mitglieder", Common.buildUrl("showMembers"));
			if( user.getId() == this.ally.getPresident().getId() ) {
				addPageMenuEntry("Einstellungen", Common.buildUrl("showAllySettings"));
				addPageMenuEntry("Posten", Common.buildUrl("showPosten"));
			}
			addPageMenuEntry("Kaempfe", Common.buildUrl("showBattles"));
			addPageMenuEntry("Allianzen auflisten", Common.buildUrl("default", "module", "allylist"));
			addPageMenuEntry("Austreten", Common.buildUrl("part"));
		}
		else {
			addPageMenuEntry("Allianz beitreten", Common.buildUrl("defaultNoAlly"));
			addPageMenuEntry("Allianz gruenden", Common.buildUrl("showCreateAlly"));
			addPageMenuEntry("Allianzen auflisten", Common.buildUrl("default", "module", "allylist"));
		}
					
		return true;	
	}
	
	private boolean isUserInAllyFoundBlock(User user) {
		Taskmanager taskmanager = Taskmanager.getInstance();

		Task[] tasks = taskmanager.getTasksByData( Taskmanager.Types.ALLY_FOUND, "*", "*", "*" );
		if( tasks.length > 0 ) {
			for( int i=0; i < tasks.length; i++ ) {
				int[] users = Common.explodeToInt(",", tasks[i].getData3());
				for( int j=0; j < users.length; j++ ) {
					if( users[j] == user.getId() ) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Leitet die Gruendung einer Allianz ein. Die Aktion erstellt
	 * die notwendigen Tasks und benachrichtigt die Unterstuetzer der Gruendung
	 * @urlparam String name Der Name der neuen Allianz
	 * @urlparam Integer confuser1 Die User-ID des ersten Unterstuetzers
	 * @urlparam Integer confuser2 Die User-ID des zweiten Unterstuetzers
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void foundAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
	
		if( user.getAlly() != null ) {
			t.setVar( "ally.message", "Fehler: Sie sind bereits Mitglied in einer Allianz und k&ouml;nnen daher keine neue Allianz gr&uuml;nden" );
			
			redirect();
			return;
		}
	
		Taskmanager taskmanager = Taskmanager.getInstance();
	
		Task[] tasks = taskmanager.getTasksByData( Taskmanager.Types.ALLY_NEW_MEMBER, "*", Integer.toString(user.getId()), "*" );
		if( tasks.length > 0 ) {
			t.setVar( "ally.message", "Fehler: Sie haben bereits einen Aufnahmeantrag bei einer Allianz gestellt" );
			
			redirect("defaultNoAlly");
			return;	
		}
		
		parameterString("name");
		parameterNumber("confuser1");
		parameterNumber("confuser2");
		String name = getString("name");
		int confuser1ID = getInteger("confuser1");
		int confuser2ID = getInteger("confuser2");
		
		if( confuser1ID == confuser2ID ) {
			t.setVar("ally.statusmessage", "<span style=\"color:red\">Einer der angegebenen Unterst&uuml;tzer ist ung&uuml;ltig</span>\n");
			
			redirect("showCreateAlly");
			return;
		}
		
		User confuser1 = (User)getContext().getDB().get(User.class, confuser1ID);
		User confuser2 = (User)getContext().getDB().get(User.class, confuser2ID);
		
		if( (confuser1 == null) || (confuser1.getAlly() != null) || 
				(confuser2 == null) || (confuser2.getAlly() != null) ) {
			t.setVar("ally.statusmessage", "<span style=\"color:red\">Einer der angegebenen Unterst&uuml;tzer ist ung&uuml;ltig</span>\n");
			
			redirect("showCreateAlly");
			return;
		}
		
		if( isUserInAllyFoundBlock(confuser1) ) {
			confuser1 = null;	
		}
		else {
			tasks = taskmanager.getTasksByData( Taskmanager.Types.ALLY_NEW_MEMBER, "*", 
					Integer.toString(confuser1.getId()), "*" );
			if( tasks.length > 0 ) {
				confuser1 = null;
			}
		}
	
		if( isUserInAllyFoundBlock(confuser2) ) {
			confuser2 = null;
		}
		else {
			tasks = taskmanager.getTasksByData( Taskmanager.Types.ALLY_NEW_MEMBER, "*", 
					Integer.toString(confuser2.getId()), "*" );
			if( tasks.length > 0 ) {
				confuser2 = null;
			}
		}
	
		if( (confuser1 == null) || (confuser2 == null) ) {
			t.setVar("ally.statusmessage", "<span style=\"color:red\">Einer der angegebenen Unterst&uuml;tzer ist versucht bereits in einer anderen Allianz Mitglied zu werden</span>\n");
			
			redirect("showCreateAlly");
			return; 	
		}
	
		String mastertaskid = taskmanager.addTask(
				Taskmanager.Types.ALLY_FOUND, 21, 
				"2", name, user.getId()+","+confuser1.getId()+","+confuser2.getId() );
		String conf1taskid = taskmanager.addTask(
				Taskmanager.Types.ALLY_FOUND_CONFIRM, 21, 
				mastertaskid, Integer.toString(confuser1.getId()), "" );
		String conf2taskid = taskmanager.addTask(
				Taskmanager.Types.ALLY_FOUND_CONFIRM, 21, 
				mastertaskid, Integer.toString(confuser2.getId()), "" );
	
		PM.send( user, confuser1.getId(), "Allianzgr&uuml;ndung", "[automatische Nachricht]\nIch habe vor die Allianz "+name+" zu gr&uuml;nden. Da zwei Spieler dieses vorhaben unterst&uuml;tzen m&uuml;ssen habe ich mich an dich gewendet.\nAchtung: Durch die Unterst&uuml;tzung wirst du automatisch Mitglied!\n\n[_intrnlConfTask="+conf1taskid+"]Willst du die Allianzgr&uuml;ndung unterst&uuml;tzen?[/_intrnlConfTask]", PM.FLAGS_IMPORTANT);
		PM.send( user, confuser2.getId(), "Allianzgr&uuml;ndung", "[automatische Nachricht]\nIch habe vor die Allianz "+name+" zu gr&uuml;nden. Da zwei Spieler dieses vorhaben unterst&uuml;tzen m&uuml;ssen habe ich mich an dich gewendet.\nAchtung: Durch die Unterst&uuml;tzung wirst du automatisch Mitglied!\n\n[_intrnlConfTask="+conf2taskid+"]Willst du die Allianzgr&uuml;ndung unterst&uuml;tzen?[/_intrnlConfTask]", PM.FLAGS_IMPORTANT);
	
		t.setVar( "ally.statusmessage", "Die beiden angegebenen Spieler wurden via PM benachrichtigt. Sollten sich beide zur Unterst&uuml;tzung entschlossen haben, wird die Allianz augenblicklich gegr&uuml;ndet. Du wirst au&szlig;erdem via PM benachrichtigt." );
		
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
	@Action(ActionType.DEFAULT)
	public void joinAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
	
		parameterString("conf");
		parameterNumber("join");
		
		String conf = getString("conf");
		int join = getInteger("join");
	
		if( user.getAlly() != null ) {
			t.setVar( "ally.message", "Sie sind bereits in einer Allianz. Sie m&uuml;ssen diese erst verlassen um in eine andere Allianz eintreten zu k&ouml;nnen!" );
			
			redirect("defaultNoAlly");
			return;	
		}
		
		Ally al = (Ally)getContext().getDB().get(Ally.class, join);
		if( al == null ) {
			t.setVar( "ally.message", "Die angegebene Allianz existiert nicht" );
			
			redirect("defaultNoAlly");
			return;	
		}
	
		if( isUserInAllyFoundBlock(user) ) {
			t.setVar( "ally.message", "Es gibt eine oder mehrere Anfragen an sie zwecks Unterst&uuml;tzung einer Allianzgr&uuml;ndung. Sie m&uuml;ssen diese Anfragen erst bearbeiten bevor sie einer Allianz beitreten k&ouml;nnen." );
			
			redirect("defaultNoAlly");
			return;
		}
		
		Taskmanager taskmanager = Taskmanager.getInstance();
		
		Task[] tasks = taskmanager.getTasksByData( Taskmanager.Types.ALLY_NEW_MEMBER, "*", Integer.toString(user.getId()), "*" );
		if( tasks.length > 0 ) {
			t.setVar( "ally.message", "Fehler: Sie haben bereits einen Aufnahmeantrag bei einer Allianz gestellt" );
			
			redirect("defaultNoAlly");
			return;	
		}
	
		if( !conf.equals("ok") ) {	
			t.setVar(	"ally.statusmessage",			"Wollen sie der Allianz &gt;"+Common._title(al.getName())+"&lt; wirklich beitreten?",
						"ally.statusmessage.ask.url1",	"&amp;action=join&amp;join="+join+"&amp;conf=ok",
						"ally.statusmessage.ask.url2",	"" );
								
			redirect("defaultNoAlly");
			return;
		}
		
		String taskid = taskmanager.addTask(Taskmanager.Types.ALLY_NEW_MEMBER, 35, Integer.toString(join), Integer.toString(user.getId()), "");
		
		List<User> supermembers = al.getSuperMembers();
		for( User supermember : supermembers ) {
			PM.send(user, supermember.getId(), 
					"Aufnahmeantrag", "[Automatische Nachricht]\nHiermit beantrage ich die Aufnahme in die Allianz.\n\n[_intrnlConfTask="+taskid+"]Wollen sie dem Aufnahmeantrag zustimmen?[/_intrnlConfTask]", PM.FLAGS_IMPORTANT);
		}
	
		t.setVar( "ally.statusmessage", "Der Aufnahmeantrag wurde weitergeleitet. Die Bearbeitung kann jedoch abh&auml;ngig von der Allianz l&auml;ngere Zeit in anspruch nehmen. Sollten sie aufgenommen werden, wird automatisch eine PM an sie gesendet." );
			
		return;
	}
	
	/**
	 * Loescht einen Allianz-Posten
	 * @urlparam Integer postenid Die ID des zu loeschenden Postens
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void deletePostenAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

		if( this.ally.getPresident().getId() != user.getId()) {
			t.setVar("ally.message","Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren");
			redirect("showPosten");
			
			return;
		}
		
		parameterNumber("postenid");
		int postenid = getInteger("postenid");
		
		AllyPosten posten = (AllyPosten)getDB().get(AllyPosten.class, postenid);
		if( posten == null ) {
  			t.setVar( "ally.message", "Fehler: Der angegebene Posten ist ungueltig" );
  			redirect("showPosten");
  			
			return;
		}
		
		if( posten.getUser() != null ) {
			posten.getUser().setAllyPosten(null);
		}
		getDB().delete(posten);
	
		t.setVar( "ally.statusmessage", "Posten gel&ouml;scht");
		
		redirect("showPosten");
	}
	
	/**
	 * Weisst einem Posten einen neuen Users zu
	 * @urlparam Integer user Die ID des neuen Inhabers des Postens
	 * @urlparam Integer id Die ID des zu besetzenden Postens
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void editPostenAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

		if( this.ally.getPresident().getId() != user.getId()) {
			t.setVar("ally.message","Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren");
			redirect("showPosten");
			
			return;
		}
		
		parameterNumber("user");
		parameterNumber("id");
		int userid = getInteger("user");
		int postenid = getInteger("id");
		
		if( userid == 0 ) {
			t.setVar( "ally.message", "Fehler: Sie m&uuml;ssen den Posten jemandem zuweisen" );
			redirect("showPosten");
			
			return;
		}

		User formuser = (User)getDB().get(User.class, userid);
		if( formuser.getAllyPosten() != null ) {
  			t.setVar( "ally.message", "Fehler: Jedem Mitglied darf maximal ein Posten zugewiesen werden" );
  			redirect("showPosten");
  			
			return;
		}
		
		AllyPosten posten = (AllyPosten)getDB().get(AllyPosten.class, postenid);
		if( posten == null ) {
  			t.setVar( "ally.message", "Fehler: Der angegebene Posten ist ungueltig" );
  			redirect("showPosten");
  			
			return;
		}

		if( posten.getUser() != null ) {
			posten.getUser().setAllyPosten(null);
		}
		formuser.setAllyPosten(posten);

		t.setVar( "ally.statusmessage", "&Auml;nderungen gespeichert" );
		redirect("showPosten");
	}
	
	/**
	 * Erstellt einen neuen Posten
	 * @urlparam String name Der Name des neuen Postens
	 * @urlparam Integer user Die ID des Benutzers, der den Posten innehaben soll
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void addPostenAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

		if( this.ally.getPresident().getId() != user.getId()) {
			t.setVar("ally.message","Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren");
			redirect("showPosten");
			
			return;
		}
		
		parameterString("name");
		parameterNumber("user");
		String name = getString("name");
		int userid = getInteger("user");

		if( name.length() == 0 ) {
			t.setVar( "ally.message", "Fehler: Sie m&uuml;ssen dem Posten einen Namen geben" );
			this.redirect("showPosten");
			return;
		}

		User formuser = (User)getContext().getDB().get(User.class, userid);
		if( formuser.getAllyPosten() != null ) {
  			t.setVar( "ally.message", "Fehler: Jedem Mitglied darf maximal ein Posten zugewiesen werden" );
  			redirect("showPosten");
			return;
		}

		long postencount = (Long)getDB()
			.createQuery("select count(*) from AllyPosten where ally="+this.ally.getId()).iterate().next();
		long membercount = this.ally.getMemberCount();

		int maxposten = (int)Math.round(membercount*MAX_POSTENCOUNT);
		if( maxposten < 2 ) {
			maxposten = 2;
		}

		if( maxposten <= postencount ) {
			t.setVar( "ally.message", "Fehler: Sie haben bereits die maximale Anzahl an Posten erreicht" );
			redirect("showPosten");
			return;
		}

		AllyPosten posten = new AllyPosten(this.ally, name);
		getDB().persist(posten);
		formuser.setAllyPosten(posten);

		t.setVar( "ally.statusmessage", "Der Posten "+Common._plaintitle(name)+" wurde erstellt und zugewiesen" );
		redirect("showPosten");
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
	@Action(ActionType.DEFAULT)
	public void createChannelAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		
		if( this.ally.getPresident().getId() != user.getId()) {
			t.setVar( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect("showAllySettings");
			return;
		}
		
		int count = ((Number)db.createQuery("select count(*) from ComNetChannel where allyOwner=?")
			.setInteger(0, this.ally.getId())
			.iterate().next()).intValue();
		
		if( count >= 2 ) {
			t.setVar( "ally.message", "Fehler: Ihre Allianz besitzt bereits zwei Frequenzen" );
			redirect("showAllySettings");
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
			t.setVar( "ally.message", "Fehler: Sie haben keinen Namen f&uuml;r die Frequenz eingegeben" );
			redirect("showAllySettings");
			return;
		}
		
		ComNetChannel channel = new ComNetChannel(name);
		channel.setAllyOwner(this.ally.getId());
		
		if( read.equals("all") ) {
			channel.setReadAll(true);
		}
		else if( read.equals("ally") ) {
			channel.setReadAlly(this.ally.getId());
		}
		else if( read.equals("player") ) {
			readids = Common.implode(",", Common.explodeToInteger(",",readids));
			channel.setReadPlayer(readids);
		}

		if( write.equals("all") ) {
			channel.setWriteAll(true);
		}
		else if( write.equals("ally") ) {
			channel.setWriteAlly(this.ally.getId());
		}
		else if( write.equals("player") ) {
			writeids = Common.implode(",", Common.explodeToInteger(",",writeids));
			channel.setWritePlayer(writeids);
		}
		db.persist(channel);

		t.setVar( "ally.statusmessage", "Frequenz "+Common._title(name)+" hinzugef&uuml;gt" );
		redirect("showAllySettings");	
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
	@Action(ActionType.DEFAULT)
	public void editChannelAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		
		if( this.ally.getPresident().getId() != user.getId()) {
			t.setVar( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect("showAllySettings");
			return;
		}
		
		this.parameterNumber("edit");
		int edit = getInteger("edit");

		ComNetChannel channel = (ComNetChannel)db.get(ComNetChannel.class, edit);
		if( (channel == null) || (channel.getAllyOwner() != this.ally.getId()) ) {
			t.setVar( "ally.message", "Fehler: Diese Frequenz geh&ouml;rt nicht ihrer Allianz" );
			redirect("showAllySettings");
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
			t.setVar( "ally.message", "Fehler: Sie haben keinen Namen f&uuml;r die Frequenz eingegeben" );
			redirect("showAllySettings");
			return;
		}
		
		channel.setName(name);
		channel.setReadAll(false);
		channel.setReadAlly(0);
		channel.setReadPlayer("");
		channel.setWriteAll(false);
		channel.setWriteAlly(0);
		channel.setWritePlayer("");
		
		if( read.equals("all") ) {
			channel.setReadAll(true);
		}
		else if( read.equals("ally") ) {
			channel.setReadAlly(this.ally.getId());
		}
		else if( read.equals("player") ) {
			readids = Common.implode(",", Common.explodeToInteger(",",readids));
			channel.setReadPlayer(readids);
		}

		if( write.equals("all") ) {
			channel.setWriteAll(true);
		}
		else if( write.equals("ally") ) {
			channel.setWriteAlly(this.ally.getId());
		}
		else if( write.equals("player") ) {
			writeids = Common.implode(",", Common.explodeToInteger(",",writeids));
			channel.setWritePlayer(writeids);
		}
		
		t.setVar( "ally.statusmessage", "Frequenz "+Common._plaintitle(name)+" ge&auml;ndert" );
		redirect("showAllySettings");	
	}
	
	/**
	 * Loescht einen Comnet-Kanal der Allianz
	 * @urlparam Integer channel Die ID des zu loeschenden Kanals
	 * @urlparam String conf Die Bestaetigung des Vorgangs. <code>ok</code>, falls der Vorgang durchgefuehrt werden soll
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void deleteChannelAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		
		if( this.ally.getPresident().getId() != user.getId()) {
			t.setVar( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect("showAllySettings");
			return;
		}
		
		this.parameterNumber("channel");
		int channelID = getInteger("channel");
	
		ComNetChannel channel = (ComNetChannel)db.get(ComNetChannel.class, channelID);
		if( (channel == null) || (channel.getAllyOwner() != this.ally.getId()) ) {
			t.setVar( "ally.message", "Fehler: Diese Frequenz geh&ouml;rt nicht ihrer Allianz" );
			redirect("showAllySettings");
			return;
		}
	
		this.parameterString("conf");
		String conf = getString("conf");
		String show = getString("show");
	
		if( !conf.equals("ok") ) {
			t.setVar(	"ally.statusmessage",			"Wollen sie die Frequenz \""+Common._title(channel.getName())+"\" wirklich l&ouml;schen?",
						"ally.statusmessage.ask.url1",	"&amp;action=deleteChannel&amp;channel="+channel.getId()+"&amp;conf=ok&amp;show="+show,
						"ally.statusmessage.ask.url2",	"&amp;show="+show );
			return;
		} 

		db.createQuery("delete from ComNetVisit where channel=?")
			.setEntity(0, channel)
			.executeUpdate();
		
		db.createQuery("delete from ComNetEntry where channel=?")
			.setEntity(0, channel)
			.executeUpdate();
		
		db.delete(channel);
		
		t.setVar( "ally.statusmessage", "Die Frequenz wurde gel&ouml;scht" );
		redirect("showAllySettings");
	}
	
	private static final int MAX_UPLOAD_SIZE = 307200;
	
	/**
	 * Laedt das neue Logo der Allianz auf den Server
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void uploadLogoAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		if( this.ally.getPresident().getId() != user.getId()) {
			t.setVar( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect("showAllySettings");
			return;
		}

		List<FileItem> list = getContext().getRequest().getUploadedFiles();
		if( list.size() == 0 ) {
			redirect("showAllySettings");
			return;
		}
		
		if( list.get(0).getSize() > MAX_UPLOAD_SIZE ) {
			t.setVar("options.message","Das Logo ist leider zu gro&szlig;. Bitte w&auml;hle eine Datei mit maximal 300kB Gr&ouml;&stlig;e<br />");
			redirect("showAllySettings");
			return;
		}
		
		String uploaddir = Configuration.getSetting("ABSOLUTE_PATH")+"data/logos/ally/";
		try {
			File uploadedFile = new File(uploaddir+this.ally.getId()+".gif");
			list.get(0).write(uploadedFile);
			t.setVar("options.message","Das neue Logo wurde auf dem Server gespeichert<br />");
		}
		catch( Exception e ) {
			t.setVar("options.message","Offenbar ging beim Upload etwas schief (Ist die Datei evt. zu gro&szlig;?)<br />");
			log.warn("",e);
		}
		
		redirect("showAllySettings");
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
	@Action(ActionType.DEFAULT)
	public void changeSettingsAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

		if( this.ally.getPresident().getId() != user.getId()) {
			t.setVar( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect("showAllySettings");
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
		String allytag = this.getString("allytag");
		String praesi = this.getString("praesi");

		// Wurde der [name]-Tag vergessen?
		if( allytag.indexOf("[name]") == -1 ) {
			t.setVar( "ally.message", "Warnung: Der [name]-tag wurde vergessen. Dieser wird nun automatisch angeh&auml;ngt!" );
			allytag += "[name]";
		}
	
		if( name.length() == 0 ) {
			t.setVar( "ally.message", "Fehler: Sie m&uuml;ssen einen Allianznamen angeben" );
			redirect("showAllySettings");
			return;	
		}
	
		if( praesi.length() == 0 ) {
			t.setVar( "ally.message", "Fehler: Sie m&uuml;ssen dem Pr&auml;sidentenamt einen Namen geben" );
			redirect("showAllySettings");
			return;	
		}

		this.ally.setName(name);
		this.ally.setDescription(this.getString("desc"));
		this.ally.setHp(this.getString("hp"));
		this.ally.setAllyTag(allytag);
		this.ally.setShowAstis(this.getInteger("showastis") != 0 ? true : false);
		this.ally.setShowGtuBieter(this.getInteger("showGtuBieter") != 0);
		this.ally.setPname(praesi);
		this.ally.setShowLrs(this.getInteger("showlrs") != 0);
		
		//Benutzernamen aktualisieren
		List<User> allyusers = this.ally.getMembers();
		for( User auser : allyusers ) {
			String newname = StringUtils.replace(allytag, "[name]", auser.getNickname());

			if( !newname.equals(auser.getName()) ) {
				auser.setName(newname);
			}
		}
		
		t.setVar( "ally.statusmessage", "Neue Daten gespeichert..." );

		redirect("showAllySettings");	
	}

	/**
	 * Laesst den aktuellen Spieler aus der Allianz austreten
	 * @urlparam String conf Falls <code>ok</code> wird der Austritt vollzogen
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void partAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
	
		if( this.ally.getPresident().getId() == user.getId()) {
			t.setVar( "ally.message", "<span style=\"color:red\">Sie k&ouml;nnen erst austreten, wenn ein anderer Pr&auml;sident bestimmt wurde" );
			redirect();
			return;
		}
	
		parameterString("conf");
		String conf = getString("conf");
	
		if( !conf.equals("ok") ) {
			String show = getString("show");
		
			t.setVar(	"ally.statusmessage",				"Wollen sie wirklich aus der Allianz austreten?",
						"ally.statusmessage.ask.url1",		"&amp;action=part&amp;conf=ok&amp;show="+show,
						"ally.statusmessage.ask.url2",		"&amp;show="+show );
			return;
		}
		
		PM.send(user, this.ally.getPresident().getId(), "Allianz verlassen", 
				"Ich habe die Allianz verlassen");
		
		ally.removeUser(user);
		
		t.setVar( "ally.showmessage", "Allianz verlassen" );
		
		this.ally = null;
		t.setVar("ally", 0);
		
		redirect("defaultNoAlly");
	}
	
	/**
	 * Loest einen Allianz auf
	 * @urlparam String conf Bestaetigt die Aufloesung, wenn der Wert <code>ok</code> ist 
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void killAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

		if( this.ally.getPresident().getId() != user.getId()) {
			t.setVar( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect();
			return;
		}
	
		this.parameterString("conf");
		String conf = this.getString("conf");
	
		if( !conf.equals("ok") ) {
			String show = getString("show");
			
			t.setVar(	"ally.statusmessage",			"Wollen sie die Allianz wirklich aufl&ouml;sen?",
						"ally.statusmessage.ask.url1",	"&amp;action=kill&amp;conf=ok&amp;show="+show,
						"ally.statusmessage.ask.url2",	"&amp;show="+show );
		} 
		else {
			PM.sendToAlly(user, this.ally, "Allianz aufgel&ouml;st", "Die Allianz wurde mit sofortiger Wirkung aufgel&ouml;st");
	
			this.ally.destroy();
		
			t.setVar( "ally.statusmessage", "Die Allianz wurde aufgel&ouml;st" );
			
			this.ally = null;
			t.setVar("ally", 0);
			
			redirect("defaultNoAlly");
		}
	}
	
	/**
	 * Befoerdert einen Spieler zum Praesidenten
	 * @urlparam Integer presn Die ID des neuen Praesidenten der Allianz
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void newPraesiAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

		if( this.ally.getPresident() != user) {
			t.setVar( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect("showMembers");
			return;
		}
	
		parameterNumber("presn");
		int presn = getInteger("presn");
		
		User presnuser = (User)getContext().getDB().get(User.class, presn);
		if( presnuser.getAlly() != this.ally ) {
			t.setVar( "ally.message", "Dieser Spieler ist nicht Mitglied ihrer Allianz" );
			redirect("showMembers");
			return;	
		}

		this.ally.setPresident(presnuser);
		t.setVar( "ally.statusmessage", presnuser.getProfileLink()+" zum Pr&auml;sidenten ernannt" );
	
		PM.send(this.ally.getPresident(), presnuser.getId(), "Zum Pr&auml;sidenten ernannt", "Ich habe dich zum Pr&auml;sidenten der Allianz ernannt");

		redirect("showMembers");
	}

	/**
	 * Wirft einen Spieler aus der Allianz
	 * @urlparam Integer kick Die ID des aus der Allianz zu werfenden Spielers
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void kickAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		if( this.ally.getPresident().getId() != user.getId()) {
			t.setVar( "ally.message", "Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchf&uuml;hren" );
			redirect("showMembers");
			return;
		}
		
		parameterNumber("kick");
		int kick = getInteger("kick");
		
		if( kick == user.getId() ) {
			t.setVar( "ally.message", "Sie k&ouml;nnen sich nicht selber aus der Allianz werfen" );
			redirect("showMembers");
			return;
		}
	
		User kickuser = (User)getContext().getDB().get(User.class, kick);
		if( !this.ally.equals(kickuser.getAlly()) ) {
			t.setVar( "ally.message", "Dieser Spieler ist nicht Mitglied ihrer Allianz" );
			redirect("showMembers");
			return;
		}

		this.ally.removeUser(kickuser);

		t.setVar( "ally.statusmessage", Common._title(kickuser.getName())+" aus der Allianz geworfen" );

		PM.send(this.ally.getPresident(), kickuser.getId(), "Aus der Allianz geworfen", "Ich habe dich aus der Allianz geworfen.");
		
		redirect("showMembers");
	}
	
	/**
	 * Zeigt die Liste der Allianzen fuer einen Allianzbeitritt an
	 */
	@Action(ActionType.DEFAULT)
	public void defaultNoAllyAction() {
		TemplateEngine t = getTemplateEngine();

		t.setVar( "show.join", 1 );
		t.setBlock( "_ALLY", "show.join.allylist.listitem", "show.join.allylist.list" );
		
		List<?> al = getDB().createQuery("from Ally order by founded").list();
		for( Iterator<?> iter=al.iterator(); iter.hasNext(); ) {
			Ally aAlly = (Ally)iter.next();
			
			t.setVar(	"show.join.allylist.allyid",	aAlly.getId(),
						"show.join.allylist.name",		Common._title(aAlly.getName()) );
								
			t.parse( "show.join.allylist.list", "show.join.allylist.listitem", true );
		}
	}
	
	/**
	 * Zeigt die GUI zum Gruenden einer Allianz an
	 */
	@Action(ActionType.DEFAULT)
	public void showCreateAllyAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		if( Common.time() - user.getSignup() < 60*60*24*3 ) {
			t.setVar("ally.message","Sie m&uuml;ssen seit mindestens 3 Tage dabei sein um eine Allianz gr&uuml;nden zu k&ouml;nnen");
		}
		else {
			t.setVar("show.create",1);
		}
	}

	/**
	 * Zeigt die Postenliste der Allianz an
	 * 
	 * @urlparam Integer destpos Offset fuer die Liste der zerstoerten Schiffe
	 * @urlparam Integer lostpos Offset fuer die Liste der verlorenen Schiffe
	 */
	@Action(ActionType.DEFAULT)
	public void showPostenAction() {
		if( this.ally == null ) {
			this.redirect("defaultNoAlly");
			return;	
		}
		
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		List<User> allymember = this.ally.getMembers();

		long postencount = (Long)db.createQuery("select count(*) from AllyPosten where ally="+this.ally.getId()).iterate().next();

		int membercount = allymember.size();
		int maxposten = (int)Math.round(membercount*MAX_POSTENCOUNT);
		if( maxposten < 2 ) {
			maxposten = 2;
		}
		
		t.setVar(	"show.posten",				1,
					"show.posten.count",		postencount,
					"show.posten.maxcount",		maxposten,
					"show.posten.addposten",	(maxposten > postencount),
					"show.posten.modify.list",	"" );
							
		t.setBlock( "_ALLY", "show.posten.modify.listitem", "show.posten.modify.list" );
		t.setBlock( "show.posten.modify.listitem", "show.posten.modify.userlist.listitem", "show.posten.modify.userlist.list" );
		
		List<?> posten = db.createQuery("from AllyPosten as ap left join fetch ap.user where ap.ally= :ally")
			.setEntity("ally", this.ally)
			.list();
		for( Iterator<?> iter=posten.iterator(); iter.hasNext(); ) {
			AllyPosten aposten = (AllyPosten)iter.next();
			
			t.setVar(	"show.posten.modify.name",			Common._plaintitle(aposten.getName()),
						"show.posten.modify.id",			aposten.getId(),
						"show.posten.modify.userlist.list",	"" );
			
			if( aposten.getUser() == null ) {
				t.setVar(	"show.posten.modify.userlist.id",		"",
							"show.posten.modify.userlist.name",		"KEINER",
							"show.posten.modify.userlist.selected",	1 );
									
				t.parse( "show.posten.modify.userlist.list", "show.posten.modify.userlist.listitem", true );
			}
			
			for( int i=0; i < allymember.size(); i++ ) {	
				User member = allymember.get(i);
				t.setVar(	"show.posten.modify.userlist.id",		member.getId(),
							"show.posten.modify.userlist.name",		Common._title(member.getNickname()),
							"show.posten.modify.userlist.selected",	aposten.getUser() != null && (aposten.getUser().getId() == member.getId()) );
									
				t.parse( "show.posten.modify.userlist.list", "show.posten.modify.userlist.listitem", true );
			}
			
			t.parse( "show.posten.modify.list", "show.posten.modify.listitem", true );
		}
		
		if( maxposten > postencount ) {
			t.setBlock( "_ALLY", "show.posten.addposten.userlist.listitem", "show.posten.addposten.userlist.list" );
			
			for( int i=0; i < allymember.size(); i++ ) {	
				User member = allymember.get(i);
				t.setVar(	"show.posten.addposten.userlist.id",	member.getId(),
							"show.posten.addposten.userlist.name",	Common._title(member.getNickname()) );
									
				t.parse( "show.posten.addposten.userlist.list", "show.posten.addposten.userlist.listitem", true );
			}
		}
	}
	
	/**
	 * Zeigt die Liste der zerstoerten und verlorenen Schiffe der Allianz
	 * 
	 * @urlparam Integer destpos Offset fuer die Liste der zerstoerten Schiffe
	 * @urlparam Integer lostpos Offset fuer die Liste der verlorenen Schiffe
	 */
	@Action(ActionType.DEFAULT)
	public void showBattlesAction() {
		if( this.ally == null ) {
			this.redirect("defaultNoAlly");
			return;	
		}
		
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		/////////////////////////////
		// Zerstoerte Schiffe
		/////////////////////////////

		int counter = 0;

		this.parameterNumber("destpos");
		long destpos = getInteger("destpos");
			
		long destcount = (Long)db.createQuery("select count(*) from ShipLost where destAlly=?")
			.setInteger(0, this.ally.getId())
			.iterate().next();
		if( destpos > destcount ) {
			destpos = destcount - 10;
		}

		if( destpos < 0 ) {
			destpos = 0;
		}
		
		t.setBlock( "_ALLY", "show.destships.listitem", "show.destships.list" );
		t.setBlock( "_ALLY", "show.destships.linefiller.listitem", "show.destships.linefiller.list" );
		t.setBlock( "_ALLY", "show.lostships.listitem", "show.lostships.list" );
		t.setBlock( "_ALLY", "show.lostships.linefiller.listitem", "show.lostships.linefiller.list" );
		
		t.setVar(	"show.battles",						1,
					"show.destships.list",				"",
					"show.destships.linefiller.list",	"",
					"show.lostships.list",				"",
					"show.lostships.linefiller.list",	"",
					"show.destpos.back",				destpos-10,
					"show.destpos.forward",				destpos+10 );

		List<?> sList = db.createQuery("from ShipLost where destAlly=? order by time desc")
			.setInteger(0, this.ally.getId())
			.setMaxResults(10)
			.setFirstResult((int)destpos)
			.list();
		for( Iterator<?> iter=sList.iterator(); iter.hasNext(); ) {
			ShipLost s = (ShipLost)iter.next();
			ShipTypeData shiptype = Ship.getShipType( s.getType() );
			
			counter++;

			User auser = (User)getContext().getDB().get(User.class, s.getOwner());
			String ownername = null;
			if( auser != null ) {
				ownername = auser.getName();
			}
			else {
				ownername = "Unbekannter Spieler ("+s.getOwner()+")";
			}

			t.setVar(	"show.destships.name",			s.getName(),
						"show.destships.type.name",		shiptype.getNickname(),
						"show.destships.type",			s.getType(),
						"show.destships.type.picture",	shiptype.getPicture(),
						"show.destships.owner",			Common._title(ownername),
						"show.destships.time",			Common.date("d.m.Y H:i:s",s.getTime()),
						"show.destships.newrow",		(counter % 5) == 0 );
			
			t.parse( "show.destships.list", "show.destships.listitem", true );
		}
		
		while( counter % 5 != 0 ) {
			t.parse( "show.destships.linefiller.list", "show.destships.linefiller.listitem", true );
			counter++;
		}

		/////////////////////////////
		// Verlorene Schiffe
		/////////////////////////////

		counter = 0;

		parameterNumber("lostpos");
		long lostpos = getInteger("lostpos");

		long lostcount = (Long)db.createQuery("select count(*) from ShipLost where ally=?")
			.setInteger(0, this.ally.getId())
			.iterate().next();
		if( lostpos > lostcount ) {
			lostpos = lostcount - 10;
		}

		if( lostpos < 0 ) {
			lostpos = 0;
		}
		
		t.setVar(	"show.lostpos.back",	lostpos-10,
					"show.lostpos.forward",	lostpos+10 );

		sList = db.createQuery("from ShipLost where destally=? order by time desc")
			.setInteger(0, this.ally.getId())
			.setMaxResults(10)
			.setFirstResult((int)lostpos)
			.list();
		for( Iterator<?> iter=sList.iterator(); iter.hasNext(); ) {
			ShipLost s = (ShipLost)iter.next();
			ShipTypeData shiptype = Ship.getShipType( s.getType() );
			
			counter++;
			
			User destowner = (User)getContext().getDB().get(User.class, s.getDestOwner());
			User owner = (User)getContext().getDB().get(User.class, s.getOwner());
			
			String ownername = null;
			if( owner != null ) {
				ownername = owner.getName();
			}
			else {
				ownername = "Unbekannter Spieler ("+s.getOwner()+")";
			}
			
			String destownername = null;
			if( destowner != null ) {
				destownername = destowner.getName();
			}
			else {
				destownername = "Unbekannter Spieler ("+s.getDestOwner()+")";
			}
						
			t.setVar(	"show.lostships.name",			s.getName(),
						"show.lostships.type.name",		shiptype.getNickname(),
						"show.lostships.type",			s.getType(),
						"show.lostships.type.picture",	shiptype.getPicture(),
						"show.lostships.owner",			Common._title(destownername),
						"show.lostships.destroyer",		Common._title(ownername),
						"show.lostships.time",			Common.date("d.m.Y H:i:s",s.getTime()),
						"show.lostships.newrow",		(counter % 5) == 0 );
			
			t.parse( "show.lostships.list", "show.lostships.listitem", true );
		}

		while( counter % 5 != 0 ) {
			t.parse( "show.lostships.linefiller.list", "show.lostships.linefiller.listitem", true );
			counter++;
		}
	}
	
	/**
	 * Zeigt die Allianzeinstellungen an
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void showAllySettingsAction() {
		if( this.ally == null ) {
			this.redirect("defaultNoAlly");
			return;	
		}
		
		User user = (User)getUser();
		
		if( user.getId() != this.ally.getPresident().getId() ) {
			redirect();
			return;
		}
		
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		t.setVar(	"show.einstellungen",	1,
					"ally.plainname",		this.ally.getName(),
					"ally.description",		this.ally.getDescription(),
					"ally.hp",				this.ally.getHp(),
					"ally.allytag",			this.ally.getAllyTag(),
					"ally.pname",			this.ally.getPname(),
					"ally.showastis",		this.ally.getShowAstis(),
					"ally.showgtubieter",	this.ally.getShowGtuBieter(),
					"ally.showlrs",			this.ally.getShowLrs(),
					"show.einstellungen.channels.list",	"" );
		
		// Zuerst alle vorhandenen Channels dieser Allianz auslesen (max 2)
		List<ComNetChannel> channels = new ArrayList<ComNetChannel>();
		List<?> channelList = db.createQuery("from ComNetChannel where allyOwner=?")
			.setInteger(0, this.ally.getId())
			.setMaxResults(2)
			.list();
		for( Iterator<?> iter=channelList.iterator(); iter.hasNext(); ) {
			channels.add((ComNetChannel)iter.next());
		}
		channels.add(null);
		
		t.setBlock( "_ALLY", "show.einstellungen.channels.listitem", "show.einstellungen.channels.list" );
	
		// Nun die vorhandenen Channels anzeigen und ggf. eine Eingabemaske fuer neue Channels anzeigen
		for( int i=0; i<=1; i++ ) {
			t.start_record();
			t.setVar(	"show.einstellungen.channels.id",		channels.get(i) == null ? 0 : channels.get(i).getId(),
						"show.einstellungen.channels.index",	i+1 );
			
			if( channels.get(i) != null ) {
				t.setVar(	"show.einstellungen.channels.name",			Common._plaintitle(channels.get(i).getName()),
							"show.einstellungen.channels.readall",		channels.get(i).isReadAll(),
							"show.einstellungen.channels.writeall",		channels.get(i).isWriteAll(),
							"show.einstellungen.channels.readally",		channels.get(i).getReadAlly(),
							"show.einstellungen.channels.writeally",	channels.get(i).getWriteAlly(),
							"show.einstellungen.channels.readids",		channels.get(i).getReadPlayer(),
							"show.einstellungen.channels.writeids",		channels.get(i).getWritePlayer() );
			}
			else {
				t.setVar(	"show.einstellungen.channels.name",		"",
							"show.einstellungen.channels.readall",	1,
							"show.einstellungen.channels.writeall",	1 );
			}
				
			t.parse( "show.einstellungen.channels.list", "show.einstellungen.channels.listitem", true );
				
			t.stop_record();
			t.clear_record();
			
			// Maximal eine Eingabemaske anzeigen
			if( channels.get(i) == null ) {
				break;
			}
		}
	}
	
	/**
	 * Zeigt die Mitgliederliste an
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void showMembersAction() {
		if( this.ally == null ) {
			this.redirect("defaultNoAlly");
			return;	
		}
		
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		t.setVar(	"show.members",		1,
					"user.president",	(user.getId() == this.ally.getPresident().getId()) );
							
		t.setBlock( "_ALLY", "show.members.listitem", "show.members.list" );
		
		//Mitglieder auflisten
		List<?> memberList = db.createQuery("from User where ally=? order by name")
			.setEntity(0, this.ally)
			.list();
		for( Iterator<?> iter=memberList.iterator(); iter.hasNext(); ) {
			User member = (User)iter.next();
			
			t.setVar(	"show.members.name",	Common._title( member.getName() ),
						"show.members.id",		member.getId() );
										
			if( user.getId() == this.ally.getPresident().getId() ) {
				String inakt_status = "";
				int inakt = member.getInactivity();
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
				
				t.setVar( "show.members.inaktstatus", inakt_status );
			}
			
			t.parse( "show.members.list", "show.members.listitem", true );
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
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
				
		if( this.ally == null ) {
			this.redirect("defaultNoAlly");
			return;	
		}
		
		/*
			Allgemeines
		*/
	
		User presi = this.ally.getPresident();
		long membercount = this.ally.getMemberCount();
		
		t.setVar(	"show.allgemein",		1,
					"ally.description",		Common._text(this.ally.getDescription()),
					"ally.founded",			this.ally.getFounded().toString(),
					"ally.wonBattles",		this.ally.getWonBattles(),
					"ally.lostBattles",		this.ally.getLostBattles(),
					"ally.destroyedShips",	this.ally.getDestroyedShips(),
					"ally.lostShips",		this.ally.getLostShips(),
					"ally.membercount",		membercount,
					"ally.pname",			Common._plaintitle(this.ally.getPname()),
					"ally.president.id",	this.ally.getPresident().getId(),
					"ally.president.name",	Common._title(presi.getName()),
					"ally.posten.list",		"" );
	
		if( ally.getItems().length() > 0 ) {
			t.setBlock( "_ALLY", "ally.items.listitem", "ally.items.list" );
			Cargo itemlist = new Cargo( Cargo.Type.ITEMSTRING, this.ally.getItems() );
			ResourceList reslist = itemlist.getResourceList();
			
			Resources.echoResList( t, reslist, "ally.items.list" );
		}
	
		t.setBlock( "_ALLY", "ally.posten.listitem", "ally.posten.list" );
		
		List<?> posten = getDB().createQuery("from AllyPosten as ap left join fetch ap.user " +
				"where ap.ally= :ally")
			.setEntity("ally", this.ally)
			.list();
		for( Iterator<?> iter=posten.iterator(); iter.hasNext(); ) {
			AllyPosten aposten = (AllyPosten)iter.next();
			if( aposten.getUser() == null ) {
				continue;
			}
			
			t.setVar(	"ally.posten.name",			Common._title(aposten.getName()),
						"ally.posten.user.name",	Common._title(aposten.getUser().getName()),
						"ally.posten.user.id",		aposten.getUser().getId());
			
			t.parse( "ally.posten.list", "ally.posten.listitem", true );
		}
		
		List<?> allymembers = getDB().createQuery("from User " +
				"where ally= :ally and " +
						"id!= :presidentId and " +
						"allyposten is null")
			.setEntity("ally", this.ally)
			.setInteger("presidentId", this.ally.getPresident().getId())
			.list();
		if( allymembers.size() > 0 ) {
			t.setVar( "ally.addmembers.list", "" );
			t.setBlock( "_ALLY", "ally.addmembers.listitem", "ally.addmembers.list" );
			
			for( Iterator<?> iter=allymembers.iterator(); iter.hasNext(); ) {
				User allymember = (User)iter.next();
				t.setVar(	"ally.addmembers.name",	Common._title(allymember.getName()),
							"ally.addmembers.id",	allymember.getId() );
				
				t.parse( "ally.addmembers.list", "ally.addmembers.listitem", true );
			}
		}
	}
}
