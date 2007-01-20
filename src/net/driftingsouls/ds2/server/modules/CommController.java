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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.comm.Ordner;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.bbcode.TagIntrnlConfTask;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Die PM-Verwaltung
 * @author Christopher Jung
 * @author Christian Peltz
 *
 */
public class CommController extends DSGenerator implements Loggable {

	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public CommController(Context context) {
		super(context);
		
		setTemplate("comm.html");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		if( action.equals("showPm") ) {
			addBodyParameter("style","background-image: url('"+Configuration.getSetting("URL")+"data/interface/border/border_background.gif');");
			setDisableDebugOutput(true);	
		}
		else {
			getTemplateEngine().set_var("show.menu", 1);
		}
		return true;
	}
	
	/**
	 * Markiert alle PMs in einem Ordner als gelesen
	 * @urlparam Integer ordner Die ID des Ordners
	 *
	 */
	public void readAllAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		parameterNumber("ordner");
		int ordner = getInteger("ordner");

		db.update("UPDATE transmissionen SET gelesen=1 WHERE empfaenger='",user.getID(),"' AND ordner='",ordner,"' AND (gelesen=0 AND !(flags & ",PM.FLAGS_IMPORTANT,"))");
		t.set_var("show.message", "<span style=\"color:red\">Alle Nachrichten als gelesen markiert</span>");
	
		redirect("showInbox");
	}
	
	/**
	 * Loescht alle PMs in einem Ordner
	 * @urlparam Integer ordner Der Ordner, dessen PMs geloescht werden sollen
	 *
	 */
	public void deleteAllAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		parameterNumber("ordner");
		int ordner = getInteger("ordner");

		int trash = Ordner.getTrash( user.getID() ).getID();

		db.update("UPDATE transmissionen SET gelesen=2, ordner='",trash,"' WHERE empfaenger='",user.getID(),"' AND ordner='",ordner,"' AND (gelesen=1 OR !(flags & ",PM.FLAGS_IMPORTANT,"))");
		t.set_var("show.message", "<span style=\"color:red\">Alle Nachrichten gel&ouml;scht</span>");
	
		redirect("showInbox");
	}
	
	/**
	 * Loescht einen Ordner/eine PM
	 * @urlparam Integer delete Falls eine PM zu loeschen ist, dann enthaelt dies die ID der PM. Andernfalls 0
	 * @urlparam Integer delord Die ID des zu loeschenden Ordners, andernfalls 0.
	 *
	 */
	public void deleteAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("delete");
		parameterNumber("delord");
		int delete = getInteger("delete");
		int ordner = getInteger("delord");

		int result = 0;
		if( (ordner != 0) && (delete == 0) ) {
			result = Ordner.deleteOrdnerByID( ordner, user.getID() );
		} else {
			result = PM.deleteByID( delete, user.getID() );
		}

		switch ( result ){
			case 0:
				t.set_var("show.message", "<span style=\"color:red\">Nachricht gel&ouml;scht</span>");
				break;
			case 1:
				t.set_var("show.message", "<span style=\"color:red\">Sie m&uuml;ssen diese Nachricht erst lesen</span>");
				break;
			case 2:
				addError("Fehler: L&ouml;schen "+(delete != 0 ? "der PM" : "des Ordners")+" ist fehlgeschlagen");
				break;
		}
	
		redirect("showInbox");
	}
	
	/**
	 * Erstellt einen neuen Ordner
	 * @urlparam Integer ordner Der Basisordner
	 * @urlparam String ordnername Der Name des neuen Ordners
	 *
	 */
	public void newOrdnerAction() {
		parameterString("ordnername");
		parameterNumber("ordner");
		String name = getString("ordnername");
		int parent = getInteger("ordner");

		Ordner.createNewOrdner(name, parent, getUser().getID());

		redirect("showInbox");
	}
	
	/**
	 * Verschiebt alle PMs von einem Ordner in einen anderen
	 * @urlparam Integer ordner Der Ausgangsordner
	 * @urlparam Integer moveto Der Zielordner
	 *
	 */
	public void moveAllAction() {
		User user = getUser();

		parameterNumber("moveto");
		parameterNumber("ordner");

		int moveto = getInteger("moveto");
		int ordner = getInteger("ordner");

		if( Ordner.existsOrdnerWithID( moveto, user.getID() ) ){
			PM.moveAllToOrdner( ordner, moveto, user.getID() );
		}

		redirect("showInbox");
	}
	
	/**
	 * Benennt einen Ordner um
	 * @urlparam String ordnername Der neue Name des Ordners
	 * @urlparam Integer subject Die ID des Ordners
	 *
	 */
	public void renameAction() {
		User user = getUser();
		Database db = getDatabase();
		
		parameterString("ordnername");
		parameterNumber("subject");
		String newname = getString("ordnername");
		int subject = getInteger("subject");

		newname = db.prepareString(newname);

		Ordner ordner = Ordner.getOrdnerByID( subject, user.getID() );

		ordner.setName( newname );

		redirect("showInbox");
	}
	
	/**
	 * Loescht alle PMs von einem bestimmten Spieler in einem Ordner
	 * @urlparam Integer playerid Die ID des Spielers
	 * @urlparam Integer ordner Die ID des Ordners
	 *
	 */
	public void deletePlayerAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		parameterNumber("playerid");
		parameterNumber("ordner");
		int playerid = getInteger("playerid");
		int ordner = getInteger("ordner");
		
		User auser = createUserObject(playerid);
		
		if( auser.getID() != 0 ) {
			db.update("UPDATE transmissionen SET gelesen=2 WHERE empfaenger='",user.getID(),"' AND sender='",user.getID(),"' AND ordner='",ordner,"' AND (gelesen=1 OR !(flags & ",PM.FLAGS_IMPORTANT,"))");
			t.set_var("show.message", "<span style=\"color:red\">Alle Nachrichten von "+Common._title(user.getName())+" gel&ouml;scht</span>");
		}
		else {
			t.set_var("show.message", "<span style=\"color:red\">Der angegebene Spieler existiert nicht</span>");
		}
	
		redirect("showInbox");
	}
	
	/**
	 * Markiert die ausgewaehlten Nachrichten als gelesen
	 * @urlparam Integer pm_$pmid Die ID einer als gelesen zu markierenden PM ($pmid gibt diese ebenfalls an)
	 *
	 */
	public void readSelectedAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		List<Integer> pms = new ArrayList<Integer>();
		SQLQuery all_pm = db.query("SELECT id FROM transmissionen WHERE empfaenger='",user.getID(),"' AND gelesen < 2");
		while( all_pm.next() ){
			pms.add(all_pm.getInt("id"));
		}
		all_pm.free();

		long count_pm = pms.size();

		for( int i = 0; i < count_pm; i++ ){
			parameterString("pm_"+pms.get(i));
			int pm = getInteger("pm_"+pms.get(i));

			if( pm == pms.get(i)){
				db.update("UPDATE transmissionen SET gelesen=1 WHERE id='",pms.get(i),"' AND empfaenger='",user.getID(),"' AND (gelesen=0 AND !(flags & ",PM.FLAGS_IMPORTANT,"))");
			}
		}

		t.set_var("show.message", "<span style=\"color:red\">Nachrichten als gelesen markiert</span>");
				
		redirect("showInbox");
	}
	
	/**
	 * Verschiebt die ausgewaehlten Nachrichten/Ordner von einem Basisordner in einen anderen
	 * @urlparam Integer ordner Der Basisordner
	 * @urlparam Integer moveto Die ID des Zielordners
	 * @urlparam Integer ordner_$ordnerid Die ID eines zu verschiebenden Ordners ($ordnerid gibt diese ebenfalls an)
	 * @urlparam Integer pm_$pmid Die ID einer zu verschiebenden PM ($pmid gibt diese ebenfalls an)
	 *
	 */
	public void moveAjaxAct() {
		User user = getUser();
		Database db = getDatabase();

		parameterNumber("moveto");
		parameterNumber("ordner");
		int movetoID = getInteger("moveto");
		int ordner = getInteger("ordner");
		
		Ordner moveto = Ordner.getOrdnerByID( movetoID, user.getID() );
		Ordner trash = Ordner.getTrash( user.getID() );

		if( moveto == null ) {
			getContext().getResponse().getContent().append("Der angegebene Ordner existiert nicht");
			return;
		}

		if( trash.getID() == moveto.getID()){
			getContext().getResponse().getContent().append("ERROR: Es duerfen keine Nachrichten/Ordner in den Papierkorb verschoben werden");
			return;
		}

		SQLQuery all_pm = db.query("SELECT id FROM transmissionen WHERE empfaenger='",user.getID(),"' AND gelesen < 2 AND ordner='",ordner,"'");
		SQLQuery all_ordner = db.query("SELECT id FROM ordner WHERE playerid='",user.getID(),"' AND !(flags=",Ordner.FLAG_TRASH,") AND parent='",ordner,"'");

		List<Integer> pms = new ArrayList<Integer>();
		while( all_pm.next() ){
			pms.add(all_pm.getInt("id"));
		}
		all_pm.free();

		List<Integer> ordners = new ArrayList<Integer>();
		while( all_ordner.next() ){
			ordners.add(all_ordner.getInt("id"));
		}
		all_ordner.free();

		int count_pm = pms.size();
		int count_ordner = ordners.size();
		int counter = 0;
		
		for(int i = 0; i < count_ordner; i++ ) {
			parameterNumber("ordner_"+ordners.get(i));
			int tomove = getInteger("ordner_"+ordners.get(i));
			if( (tomove != 0) && Ordner.getAllChildIDs(tomove, user.getID()).contains(moveto.getID())) {
				getContext().getResponse().getContent().append("ERROR: Es duerfen keine Ordner in ihre eignen Unterordner verschoben werden");
				return ;
			}


			if( tomove == ordners.get(i) ){
				counter++;
				db.update("UPDATE ordner SET parent='",moveto.getID(),"' WHERE id='",tomove,"'");
			}
		}
		
		for( int i = 0; i < count_pm; i++ ) {
			parameterNumber("pm_"+pms.get(i));
			int pm = getInteger("pm_"+pms.get(i));
			if( pm == pms.get(i) ) {
				counter++;
				db.update("UPDATE transmissionen SET ordner='",moveto.getID(),"' WHERE id='",pm,"'");
			}
		}
	
		getContext().getResponse().getContent().append(counter);
		return;
	}
	
	/**
	 * Verschiebt die ausgewaehlten Nachrichten/Ordner von einem Basisordner in einen anderen
	 * @urlparam Integer ordner Der Basisordner
	 * @urlparam Integer moveto Die ID des Zielordners
	 * @urlparam Integer ordner_$ordnerid Die ID eines zu verschiebenden Ordners ($ordnerid gibt diese ebenfalls an)
	 * @urlparam Integer pm_$pmid Die ID einer zu verschiebenden PM ($pmid gibt diese ebenfalls an)
	 *
	 */
	public void moveSelectedAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();

		parameterNumber("moveto");
		parameterNumber("ordner");
		int movetoID = getInteger("moveto");
		int ordner = getInteger("ordner");
		
		Ordner moveto = Ordner.getOrdnerByID( movetoID, user.getID() );
		Ordner trash = Ordner.getTrash( user.getID() );

		if( moveto == null ) {
			t.set_var("show.message", "<span style=\"color:red\">Der angegebene Ordner existiert nicht</span>");
			redirect("showInbox");
			return;
		}
		
		if( trash.getID() == moveto.getID()){
			t.set_var("show.message", "<span style=\"color:red\">Es d&uuml;rfen keine Nachrichten/Ordner in den Papierkorb verschoben werden.</span>");
			redirect("showInbox");
			return;
		}

		SQLQuery all_pm = db.query("SELECT id FROM transmissionen WHERE empfaenger='",user.getID(),"' AND gelesen < 2 AND ordner='",ordner,"'");
		SQLQuery all_ordner = db.query("SELECT id FROM ordner WHERE playerid='",user.getID(),"' AND !(flags=",Ordner.FLAG_TRASH,") AND parent='",ordner,"'");

		List<Integer> pms = new ArrayList<Integer>();
		while( all_pm.next() ){
			pms.add(all_pm.getInt("id"));
		}
		all_pm.free();

		List<Integer> ordners = new ArrayList<Integer>();
		while( all_ordner.next() ){
			ordners.add(all_ordner.getInt("id"));
		}
		all_ordner.free();

		int count_pm = pms.size();
		int count_ordner = ordners.size();
		
		for(int i = 0; i < count_ordner; i++ ) {
			parameterNumber("ordner_"+ordners.get(i));
			int tomove = getInteger("ordner_"+ordners.get(i));
			if( (tomove != 0) && Ordner.getAllChildIDs(tomove, user.getID()).contains(moveto.getID())) {
				t.set_var("show.message", "<span style=\"color:red\">Es d&uuml;rfen keine Ordner in ihre eignen Unterordner verschoben werden.</span>");
				redirect("showInbox");
				return ;
			}


			if( tomove == ordners.get(i) ){
				db.update("UPDATE ordner SET parent='",moveto.getID(),"' WHERE id='",tomove,"'");
			}
		}

		for( int i = 0; i < count_pm; i++ ) {
			parameterNumber("pm_"+pms.get(i));
			int pm = getInteger("pm_"+pms.get(i));
			if( pm == pms.get(i) ) {
				db.update("UPDATE transmissionen SET ordner='",moveto.getID(),"' WHERE id='",pm,"'");
			}
		}
	
		redirect("showInbox");
	}

	/**
	 * Loescht die ausgewaehlten Nachrichten/Ordner in einem Basisordner
	 * @urlparam Integer ordner Der Basisordner
	 * @urlparam Integer ordner_$ordnerid Die ID eines zu loeschenden Ordners ($ordnerid gibt diese ebenfalls an)
	 * @urlparam Integer pm_$pmid Die ID einer zu loeschenden PM ($pmid gibt diese ebenfalls an)
	 *
	 */
	public void deleteSelectedAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();

		parameterNumber("ordner");
		int ordner = getInteger("ordner");

		SQLQuery all_pm = db.query("SELECT id FROM transmissionen WHERE empfaenger='",user.getID(),"' AND gelesen < 2");
		SQLQuery all_ordner = db.query("SELECT id FROM ordner WHERE playerid='",user.getID(),"' AND !flags=",Ordner.FLAG_TRASH," AND parent='",ordner,"'");

		List<Integer> pms = new ArrayList<Integer>();
		while( all_pm.next() ){
			pms.add(all_pm.getInt("id"));
		}

		List<Integer> ordners = new ArrayList<Integer>();
		while( all_ordner.next() ) {
			ordners.add(all_ordner.getInt("id"));
		}

		int count_pm = pms.size();
		int count_ordner = ordners.size();
		
		for( int i = 0; i < count_ordner; i++ ) {
			parameterNumber("ordner_"+ordners.get(i));
			int delordner = getInteger("ordner_"+ordners.get(i));

			if( delordner == ordners.get(i) ) {
				Ordner.deleteOrdnerByID( delordner, user.getID() );
			}
		}

		for( int i = 0; i < count_pm; i++ ) {
			parameterNumber("pm_"+pms.get(i) );
			int pm_id = getInteger("pm_"+pms.get(i));

			if( pm_id == pms.get(i) ) {
				PM.deleteByID( pm_id, user.getID() );
			}
		}
		
		t.set_var("show.message", "<span style=\"color:red\">Nachrichten gel&ouml;scht</span>");

		redirect("showInbox");
	}
	
	/**
	 * Versendet eine Nachricht
	 * @urlparam String to Der Empfaenger (Eine ID oder "task" oder "ally")
	 * @urlparam Integer reply Falls != 0, dann die ID der Nachricht auf die geantwortet wird (Titel wird dann generiert)
	 * @urlparam String msg Der Text der Nachricht
	 * @urlparam String title Falls es sich nicht um eine Antwort handelt, dann der Titel der Nachricht
	 * @urlparam String special Falls es sich nicht um eine Antwort handelt, dann das Spezialflag der Nachricht 
	 *
	 */
	public void sendAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		parameterString("to");
		parameterNumber("reply");
		parameterString("msg");
		
		String to = getString("to");
		int reply = getInteger("reply");
		String msg = getString("msg");
		
		String title = null;
		String special = null;
		
		if( reply > 0 ) {
			SQLResultRow pm = db.first("SELECT * FROM transmissionen " +
					"WHERE id="+reply+" AND " +
						"(empfaenger="+user.getID()+" OR sender="+user.getID()+") " +
						"AND gelesen < 2");
			int iTo = pm.getInt("sender");
			if( iTo == user.getID() ) {
				iTo = pm.getInt("empfaenger");	
			}
			to = Integer.toString(iTo);
			title = "RE: "+Common._plaintitle(pm.getString("title"));
			special = "";
		}
		else {
			parameterString("title");
			parameterString("special");
			
			title = getString("title");
			special = getString("special");
		}
		
		if( special.equals("admin") && (user.getAccessLevel() < 30) ) {
			special = "";
		}
		if( special.equals("official") && !Rassen.get().rasse(user.getRace()).isHead(user.getID()) ) {
			special = "";	
		}
		
		int flags = 0;
		
		if( special.equals("admin") ) {
			flags |= PM.FLAGS_ADMIN;
			flags |= PM.FLAGS_IMPORTANT;	
		}
		else if( special.equals("official") ) {
			flags |= PM.FLAGS_OFFICIAL;	
		}

		if( to.equals("task") ) {
			t.set_var("show.message", "<span style=\"color:#00ff55\">Antwort verarbeitet</span>");
			
			PM.send(getContext(), user.getID(), PM.TASK, title, msg, false, flags );
		} 
		else if( to.equals("ally") ) {
			if( user.getAlly() <= 0 ) {
				t.set_var("show.message", "<span style=\"color:red; font-weight:bold\">Sie sind in keiner Allianz Mitglied</span>");
				
				return;
			}
			
			String nameto = db.first("SELECT name FROM ally WHERE id="+user.getAlly()).getString("name");
			t.set_var("show.message", "<span style=\"color:#00ff55\">Nachricht versendet an</span> "+Common._title(nameto));

			PM.send(getContext(), user.getID(), user.getAlly(), title, msg, true, flags );
		}
		else {			
			if( (to.length() == 0) || (Integer.parseInt(to) == 0) ) {
				t.set_var("show.message", "<span style=\"color:#ff0000\">Sie m&uuml;ssen einen Empf&auml;nger angeben</span>");
				return;
			}
			
			int iTo = Integer.parseInt(to);
		
			User auser = getContext().createUserObject(iTo);
			t.set_var("show.message", "<span style=\"color:#00ff55\">Nachricht versendet an</span> "+Common._title(auser.getName()));

			PM.send(getContext(), user.getID(), iTo, title, msg, false, flags );
		}
	}
	
	/**
	 * Zeigt eine empfangene/gesendete PM an
	 * @urlparam Integer pmid Die ID der Nachricht
	 * @urlparam Integer ordner Die ID des Ordners, in dem sich die Nachricht befindet
	 *
	 */
	public void showPmAction() {
		Database db = getDatabase();
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		BBCodeParser bbcodeparser = BBCodeParser.getNewInstance();
		
		parameterNumber("pmid");
		parameterNumber("ordner");
		int pmid = getInteger("pmid");
		int parent_id = getInteger("ordner");
		
		t.set_var("show.pm", 1);
	
		if( pmid == 0 ) {			
			return;	
		}
	
		SQLResultRow pm = db.first("SELECT * FROM transmissionen WHERE id='",pmid,"' AND (empfaenger='",user.getID(),"' OR sender='",user.getID(),"')");
		if( pm.isEmpty() ) {			
			return;	
		}
		
		User sender = null;
		
		if( pm.getInt("sender") == user.getID() ) {
			try {
				bbcodeparser.registerHandler( "_intrnlConfTask", 2, "<div style=\"text-align:center\"><table class=\"noBorderX\" width=\"500\"><tr><td class=\"BorderX\" align=\"center\">Entscheidungsm&ouml;glichkeit in der Orginal-PM</td></tr></table></div>");
			}
			catch( Exception e ) {
				LOG.error("Register _intrnlConfTask failed", e);
				addError("Fehler beim Darstellen der PM");
			}
			
			if( (pm.getInt("empfaenger") == user.getID()) && (pm.getInt("gelesen") == 0) ) {
				db.update("UPDATE transmissionen SET gelesen=1 WHERE id='",pmid,"'");	
			}
	
			User empfaenger = createUserObject(pm.getInt("empfaenger"));
			sender = user;
			
			t.set_var(	"pm.empfaenger",		empfaenger.getID(),
						"pm.empfaenger.name",	(empfaenger.getID() != 0 ? Common._title(empfaenger.getName()) : "Unbekannt" ));
		}
		else {
			if( pm.getInt("gelesen") >= 2 ) {
				return;	
			}
			try {
				bbcodeparser.registerHandler( "_intrnlConfTask", 2, new TagIntrnlConfTask());
			}
			catch( Exception e ) {
				LOG.error("Register _intrnlConfTask failed", e);
				addError("Fehler beim Darstellen der PM");
			}
			
			if( pm.getInt("gelesen") == 0 ) {
				db.update("UPDATE transmissionen SET gelesen=1 WHERE id='",pmid,"'");	
			}
			
			sender = createUserObject(pm.getInt("sender"));
			
			t.set_var(	"pm.sender",		sender.getID(),
						"pm.sender.name", 	(sender.getID() != 0? Common._title(sender.getName()) : "Unbekannt"),
						"ordner.parent",	parent_id);
		}
	
		String bgimg = "";

		if( (pm.getInt("flags") & PM.FLAGS_ADMIN) != 0 ) {
			bgimg = "pm_adminbg.png";	
		}
		else if( (pm.getInt("flags") & PM.FLAGS_OFFICIAL) != 0 ) {
			bgimg = "pm_"+Rassen.get().rasse(sender.getRace()).getName().toLowerCase()+"bg.png";	
		}
		
		t.set_var(	"pm.id",			pm.getInt("id"),
					"pm.title",			Common._plaintitle(pm.getString("title")),
					"pm.flags.admin", 	(pm.getInt("flags") & PM.FLAGS_ADMIN),
					"pm.bgimage", 		bgimg,
					"pm.time", 			Common.date("j.n.Y G:i",pm.getInt("time")),
					"pm.text", 			Common.smiliesParse(Common._text(pm.getString("inhalt"))),
					"pm.kommentar", 	Common.smiliesParse(Common._text(pm.getString("kommentar"))));
	}
	
	/**
	 * Stellt eine Nachricht aus dem Papierkorb wieder her
	 * @urlparam Integer recover Die wiederherzustellende Nachricht
	 *
	 */
	public void recoverAction() {
		User user = getUser();

		parameterNumber("recover");
		int recover = getInteger("recover");
	
		PM.recoverByID( recover, user.getID() );
		
		redirect("showInbox");
	}
	
	/**
	 * Stellt alle geloeschten Nachrichten aus dem Papierkorb wieder her
	 *
	 */
	public void recoverAllAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
	
		PM.recoverAll( user.getID() );
		
		t.set_var("show.message", "<span style=\"color:red\">Nachrichten wiederhergestellt</span>");

		redirect("showInbox");
	}
	
	/**
	 * Zeigt die Liste aller empfangenen Nachrichten an
	 * @urlparam Integer Der anzuzeigende Ordner (0 ist die oberste Ebene)
	 *
	 */
	public void showInboxAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		parameterNumber("ordner");
		int current_ordner = getInteger("ordner");

		int gelesen = 2;
		
		t.set_var("show.inbox", 1);
		t.set_block("_COMM", "pms.listitem", "pms.list");
		t.set_block("_COMM", "ordner.listitem", "ordner.list");
		t.set_block("_COMM", "availordner.listitem", "availordner.list");
		
		// Liste aller vorhandenen Ordner generieren
		SQLQuery ordner = db.query("SELECT * FROM ordner WHERE playerid=",user.getID()," ORDER BY name ASC");

		t.set_var(	"availordner.id",	0,
					"availordner.name",	"Hauptverzeichnis" );
			
		t.parse("availordner.list", "availordner.listitem", true);

		while( ordner.next() ){
			t.set_var(	"availordner.id",	ordner.getInt("id"),
						"availordner.name",	ordner.getString("name") );
			
			t.parse("availordner.list", "availordner.listitem", true);
		}
		ordner.free();

		// Link zum uebergeordneten Ordner erstellen
		Map<Integer,Integer> ordners = Ordner.countPMInAllOrdner( current_ordner, user.getID() );

		if( current_ordner != 0 ) {
			SQLResultRow ordnerRow = db.first("SELECT * FROM ordner WHERE playerid=",user.getID()," AND id=",current_ordner,"");
			t.set_var(	"ordner.id",			ordnerRow.getInt("parent"),
						"ordner.name",			"..",
						"ordner.parent",		ordnerRow.getInt("id"),
						"ordner.pms",			Ordner.countPMInOrdner(ordnerRow.getInt("parent"), user.getID()),
						"ordner.flags.up",		1,
						"ordner.flags.trash",	(ordnerRow.getInt("flags") & Ordner.FLAG_TRASH),
						"ordner.name.real",		ordnerRow.getString("name"));
			
			t.parse("ordner.list", "ordner.listitem", true);
			if( (ordnerRow.getInt("flags") & Ordner.FLAG_TRASH) != 0){
				gelesen = 10;
			}
		}

		// Ordnerliste im aktuellen Ordner ausgeben
		ordner = db.query("SELECT * FROM ordner WHERE playerid=",user.getID()," AND parent=",current_ordner," ORDER BY name ASC");

		while( ordner.next() ){
			t.set_var(	"ordner.id",			ordner.getInt("id"),
						"ordner.name",			ordner.getString("name"),
						"ordner.parent",		ordner.getInt("parent"),
						"ordner.pms",			ordners.get(ordner.getInt("id")),
						"ordner.flags.up",		0,
						"ordner.flags.trash",	(ordner.getInt("flags") & Ordner.FLAG_TRASH) != 0 );
			
			t.parse("ordner.list", "ordner.listitem", true);
		}
		ordner.free();

		// PMs im aktuellen Ordner ausgeben
		SQLQuery pm = db.query("SELECT t1.id,t1.sender,t1.gelesen,t1.time,t1.title,t1.flags,t1.kommentar,t2.name AS sender_name " +
				"FROM transmissionen AS t1,users AS t2 " +
				"WHERE t1.empfaenger=",user.getID()," AND t1.gelesen<",gelesen," AND t1.sender=t2.id AND ordner=",current_ordner," "+
				"ORDER BY t1.id DESC");

		while( pm.next() ) {	
			t.set_var(	"pm.id",			pm.getInt("id"),
						"pm.new",			pm.getInt("gelesen") == 0,
						"pm.flags.admin",	(pm.getInt("flags") & PM.FLAGS_ADMIN) != 0,
						"pm.title",			Common._plaintitle(pm.getString("title")),
						"pm.sender.name",	Common._title(pm.getString("sender_name")),
						"pm.sender.id",		pm.getInt("sender"),
						"pm.time",			Common.date("j.n.Y G:i",pm.getInt("time")),
						"pm.trash",			(pm.getInt("gelesen") > 1) ? 1 : 0,
						"pm.kommentar",		pm.getString("kommentar"));
			
			t.parse("pms.list", "pms.listitem", true);
		}
		pm.free();
	}
	
	/**
	 * Zeigt die Liste aller versendeten und noch nicht geloeschten PMs
	 *
	 */
	public void showOutboxAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		t.set_var("show.outbox", 1);
		t.set_block("_COMM", "pms.out.listitem", "pms.out.list");
		
		SQLQuery pm = db.query("SELECT t1.id,t1.empfaenger,t1.time,t1.title,t1.flags,t2.name AS empfaenger_name " ,
				"FROM transmissionen AS t1,users AS t2 " ,
				"WHERE t1.sender='",user.getID(),"' AND t1.empfaenger=t2.id " ,
				"ORDER BY t1.id DESC");
				
		while( pm.next() ) {	
			t.set_var(	"pm.id",				pm.getInt("id"),
						"pm.flags.admin",		(pm.getInt("flags") & PM.FLAGS_ADMIN),
						"pm.title",				Common._plaintitle(pm.getString("title")),
						"pm.empfaenger.name",	Common._title(pm.getString("empfaenger_name")),
						"pm.time",				Common.date("j.n.Y G:i",pm.getInt("time")),
						"pm.empfaenger",		pm.getInt("empfaenger") );
			
			t.parse("pms.out.list", "pms.out.listitem", true);
		}
		pm.free();
	}

	/**
	 * Zeigt eine Preview einer geschriebenen Nachricht an
	 * @urlparam String msg Die Nachricht
	 * @urlparam String to Der Empfaenger der Nachricht
	 * @urlparam String title Der Titel der Nachricht
	 * @urlparam String special Spezialflag der Nachricht
	 *
	 */
	public void previewAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
	
		parameterString("msg");
		parameterString("to");
		parameterString("title");
		parameterString("special");

		String msg = getString("msg");
		String to = getString("to");
		String title = getString("title");
		String special = getString("special");
		
		Map<String,String> specialuilist = new HashMap<String,String>();
		specialuilist.put("nichts", "");
		if( user.getAccessLevel() >= 30 ) {
			specialuilist.put("admin", "admin");
		}
		if( Rassen.get().rasse(user.getRace()).isHead(user.getID()) ) {
			specialuilist.put("Offizielle PM", "official");
		}

		String special_key = specialuilist.get(special);
		if( special_key == null ) {
			special = "";
		}
		
		specialuilist.remove(special_key);
		specialuilist.put(special_key, special);
			
		t.set_block("_COMM", "write.specialui.listitem", "write.specialui.list");
		for( String uiname : specialuilist.keySet() ) {
			t.set_var(	"specialui.name",	uiname,
						"specialui.value",	specialuilist.get(uiname),
						"specialui.selected",	special.equals(specialuilist.get(uiname)) ? true : false);
								
			t.parse("write.specialui.list", "write.specialui.listitem", true);
		}
			
		String bgimg = "";

		if( special.equals("admin") ) {
			bgimg = "pm_adminbg.png";	
		}
		else if( special.equals("official") ) {
			bgimg = "pm_"+Rassen.get().rasse(user.getRace()).getName()+"bg.png";	
		}
			
		t.set_var(	"pm.text",			Common.smiliesParse(Common._text(msg)),
					"pm.title",			title,
					"pm.sender",		user.getID(),
					"pm.sender.name",	(user.getID() != 0 ? Common._title(user.getName()) : "Unbekannt"),
					"pm.time",			Common.date("j.n.Y G:i", Common.time()),
					"pm.bgimage",		bgimg,
					"write.to",			to,
					"write.title",		title,
					"write.message",	msg,
					"show.preview",		1,
					"show.write",		1 );
	}
	
	/**
	 * Zeigt die GUI zum anlegen/bearbeiten eines Kommentars zu einer Nachricht an
	 * @urlparam Integer pm Die Nachricht
	 * @urlparam Integer ordner Der Ordner, in dem sich die Nachricht befindet
	 *
	 */
	public void editCommentAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
	
		parameterNumber("pm");
		parameterNumber("ordner");
		int pmid = getInteger("pm");
		int ordner = getInteger("ordner");

		SQLResultRow pm = db.first("SELECT * FROM transmissionen WHERE id='",pmid,"' AND empfaenger='",user.getID(),"'");
		db.update("UPDATE transmissionen SET gelesen=1 WHERE id=",pmid," AND gelesen=0 AND empfaenger=",user.getID());

		t.set_var("show.comment", 1);
		t.set_var("comment.text", pm.getString("kommentar"));
		t.set_var("pm.id", pmid);
		t.set_var("ordner.id", ordner);
		t.set_var("pm.title", pm.getString("title"));
		t.set_var("pm.empfaenger.name", Common._title(db.first("SELECT name FROM users WHERE id='",pm.getInt("empfaenger"),"'").getString("name")));
		t.set_var("pm.sender.name", Common._title(db.first("SELECT name FROM users WHERE id='",pm.getInt("sender"),"'").getString("name")));
		t.set_var("pm.text", Common.smiliesParse(Common._text(pm.getString("inhalt"))));
		t.set_var("system.time", Common.getIngameTime(getContext().get(ContextCommon.class).getTick()));
		t.set_var("user.signature", user.getUserValue("PMS/signatur") );
	}
	
	/**
	 * Speichert einen Kommentar zu einer Nachricht
	 * @urlparam Integer pmid Die ID der Nachricht
	 * @urlparam String msg Der Kommentar
	 */
	public void sendCommentAction() {
		Database db = getDatabase();
		User user = getUser();
	
		parameterNumber("pmid");
		parameterString("msg");
		int pmid = getInteger("pmid");
		String msg = getString("msg");

		db.prepare("UPDATE transmissionen SET kommentar= ? WHERE id= ? AND empfaenger= ?")
			.update(msg, pmid, user.getID());

		redirect("showInbox");
	}

	@Override
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		parameterNumber("to");
		parameterNumber("reply");
		parameterString("msg");
		
		int to = getInteger("to");
		int reply = getInteger("reply");
		String msg = getString("msg");
		
		String title = "";
		String special = "";
		
		if( reply != 0) {
			SQLResultRow pm = db.first("SELECT * FROM transmissionen WHERE id='",reply,"' AND (empfaenger='",user.getID(),"' OR sender='",user.getID(),"') AND gelesen < 10");
			to = pm.getInt("sender");
			if( to == user.getID() ) {
				to = pm.getInt("empfaenger");
			}
			title = "RE: "+Common._plaintitle(pm.getString("title"));
			special = "";
			
			msg = "(Nachricht am "+Common.date("j.n.Y G:i",pm.getInt("time"))+" empfangen.)\n"+Pattern.compile("/\n>*/").matcher(pm.getString("inhalt")).replaceAll("\n"); //Fuehrende > entfernen
			msg = msg.replaceAll("\t\r\n", " "); //Wegen der Einrueckung eingefuegte Umbrueche entfernen
			
			// Reply-Verschachtelungstiefe ermitteln
			int depth = 0;
			Matcher match = Pattern.compile("/\\(Nachricht am \\d{1,2}\\.\\d{1,2}\\.\\d{4,4} \\d{1,2}:\\d{2,2} empfangen\\.\\)/").matcher(msg);
			while(match.find()) {
				depth++;
			}

			String[] msg_lines = StringUtils.split(msg, '\n'); //Text Zeilenweise auftrennen
			for( int i=0; i < msg_lines.length; i++ ){
				msg_lines[i] = Common.wordwrap(msg_lines[i], 65 - depth, "\t\n");	//Zeilen umbrechen
				
				if(Pattern.compile("/\\(Nachricht am \\d{1,2}\\.\\d{1,2}\\.\\d{4,4} \\d{1,2}:\\d{2,2} empfangen\\.\\)/").matcher(msg_lines[i]).find() ){ //beginn einer neuen Verschachtelung
					for( int j = i + 1; j < msg_lines.length; j++){	//in Jede zeile ein ">" am Anfang einfuegen
						msg_lines[j] = ">"+msg_lines[j];
					}
				}
			}
			msg = Common.implode("\n", msg_lines); //Text wieder zusammenfuegen
			msg += "\n\n"; // Zwei Leerzeilen koennen am Ende nicht schaden...
			
			
		}
		else {
			parameterString("title");
			parameterString("special");
			
			title = getString("title");
			special = getString("special");
		}
		
		if( special.equals("admin") && (user.getAccessLevel() < 30) ) {
			special = "";	
		}
		if( special.equals("official") && !Rassen.get().rasse(user.getRace()).isHead(user.getID()) ) {
			special = "";	
		}
		
		Map<String,String> specialuilist = new HashMap<String,String>();
		specialuilist.put("nichts", "");
		if( user.getAccessLevel() >= 30 ) {
			specialuilist.put("admin", "admin");
		}
		if( Rassen.get().rasse(user.getRace()).isHead(user.getID()) ) {
			specialuilist.put("Offizielle PM", "official");
		}
			
		t.set_var(	"show.write", 1,
					"write.title", title,
					"write.message", msg,
					"write.to", to,
					"system.time", Common.getIngameTime(getContext().get(ContextCommon.class).getTick()),
					"user.signature", user.getUserValue("PMS/signature") );
		
		t.set_block("_COMM", "write.specialui.listitem", "write.specialui.list");
		for( String uiname : specialuilist.keySet() ) {
			t.set_var(	"specialui.name", uiname,
						"specialui.value", specialuilist.get(uiname) );
								
			t.parse("write.specialui.list", "write.specialui.listitem", true);
		}
	}
}
