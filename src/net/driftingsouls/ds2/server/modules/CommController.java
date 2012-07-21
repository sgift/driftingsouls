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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.bbcodes.TagIntrnlConfTask;
import net.driftingsouls.ds2.server.comm.Ordner;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.bbcode.Smilie;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Die PM-Verwaltung.
 * @author Christopher Jung
 * @author Christian Peltz
 *
 */
@Configurable
@Module(name="comm")
public class CommController extends TemplateGenerator {
	private static final Log log = LogFactory.getLog(CommController.class);

	private Configuration config;

    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config)
    {
    	this.config = config;
    }

	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public CommController(Context context) {
		super(context);

		setTemplate("comm.html");

		setPageTitle("PMs");
		addPageMenuEntry("Neue Nachricht", Common.buildUrl("default", "to", 0));
		addPageMenuEntry("Posteingang", Common.buildUrl("showInbox"));
		addPageMenuEntry("Postausgang", Common.buildUrl("showOutbox"));
	}

	@Override
	protected boolean validateAndPrepare(String action) {
		if( action.equals("showPm") ) {
			addBodyParameter("style","background-image: url('"+config.get("URL")+"data/interface/border/border_background.gif');");
			setDisableDebugOutput(true);
		}
		else {
			getTemplateEngine().setVar("show.menu", 1);
		}
		return true;
	}

	/**
	 * Markiert alle PMs in einem Ordner als gelesen.
	 * @urlparam Integer ordner Die ID des Ordners
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void readAllAction() {
		TemplateEngine t = getTemplateEngine();

		parameterNumber("ordner");
		Ordner ordner = Ordner.getOrdnerByID(getInteger("ordner"), (User)getUser());

		ordner.markAllAsRead();

		t.setVar("show.message", "<span style=\"color:red\">Alle Nachrichten als gelesen markiert</span>");

		redirect("showInbox");
	}

	/**
	 * Loescht alle PMs in einem Ordner.
	 * @urlparam Integer ordner Der Ordner, dessen PMs geloescht werden sollen
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void deleteAllAction() {
		TemplateEngine t = getTemplateEngine();

		parameterNumber("ordner");
		Ordner ordner = Ordner.getOrdnerByID(getInteger("ordner"), (User)getUser());
		ordner.deleteAllPms();

		t.setVar("show.message", "<span style=\"color:red\">Alle Nachrichten gel&ouml;scht</span>");

		redirect("showInbox");
	}

	/**
	 * Loescht einen Ordner/eine PM.
	 * @urlparam Integer delete Falls eine PM zu loeschen ist, dann enthaelt dies die ID der PM. Andernfalls 0
	 * @urlparam Integer delord Die ID des zu loeschenden Ordners, andernfalls 0.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void deleteAction() {
		org.hibernate.Session db = getContext().getDB();
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();

		parameterNumber("delete");
		parameterNumber("delord");
		int delete = getInteger("delete");
		Ordner ordner = Ordner.getOrdnerByID(getInteger("delord"), user);

		int result = 0;
		if( (ordner != null) && (delete == 0) ) {
			result = ordner.deleteOrdner();
			db.flush();
		}
		else {
			PM pm = (PM)db.get(PM.class, delete);
			if( pm == null )
			{
				t.setVar("show.message", "<span style=\"color:red\">Die angegebene Nachricht existiert nicht</span>");
				redirect("showInbox");
				return;
			}
			if( pm.getEmpfaenger() == user ) {
				result = pm.delete();
				db.flush();
			}
		}

		switch ( result ){
			case 0:
				t.setVar("show.message", "<span style=\"color:red\">"+(delete != 0 ? "Nachricht" : "Ordner")+" gel&ouml;scht</span>");
				break;
			case 1:
				t.setVar("show.message", "<span style=\"color:red\">Sie m&uuml;ssen diese Nachricht erst lesen</span>");
				break;
			case 2:
				addError("Fehler: L&ouml;schen "+(delete != 0 ? "der PM" : "des Ordners")+" ist fehlgeschlagen");
				break;
		}

		redirect("showInbox");
	}

	/**
	 * Erstellt einen neuen Ordner.
	 * @urlparam Integer ordner Der Basisordner
	 * @urlparam String ordnername Der Name des neuen Ordners
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void newOrdnerAction() {
		User user = (User)getUser();
		parameterString("ordnername");
		parameterNumber("ordner");
		String name = getString("ordnername");
		Ordner parent = Ordner.getOrdnerByID(getInteger("ordner"), user);

		if( parent == null ) {
			redirect("showInbox");
			return;
		}

		Ordner.createNewOrdner(name, parent, user);

		redirect("showInbox");
	}

	/**
	 * Verschiebt alle PMs von einem Ordner in einen anderen.
	 * @urlparam Integer ordner Der Ausgangsordner
	 * @urlparam Integer moveto Der Zielordner
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void moveAllAction() {
		User user = (User)getUser();

		parameterNumber("moveto");
		parameterNumber("ordner");

		Ordner moveto = Ordner.getOrdnerByID(getInteger("moveto"), user);
		Ordner ordner = Ordner.getOrdnerByID(getInteger("ordner"), user);

		if( (moveto != null) && (ordner != null) ) {
			PM.moveAllToOrdner( ordner, moveto, user );
		}

		redirect("showInbox");
	}

	/**
	 * Benennt einen Ordner um.
	 * @urlparam String ordnername Der neue Name des Ordners
	 * @urlparam Integer subject Die ID des Ordners
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void renameAction() {
		User user = (User)getUser();

		parameterString("ordnername");
		parameterNumber("subject");
		String newname = getString("ordnername");
		int subject = getInteger("subject");

		Ordner ordner = Ordner.getOrdnerByID( subject, user );
		ordner.setName( newname );

		redirect("showInbox");
	}

	/**
	 * Loescht alle PMs von einem bestimmten Spieler in einem Ordner.
	 * @urlparam Integer playerid Die ID des Spielers
	 * @urlparam Integer ordner Die ID des Ordners
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void deletePlayerAction() {
		TemplateEngine t = getTemplateEngine();

		parameterNumber("playerid");
		parameterNumber("ordner");
		Ordner ordner = Ordner.getOrdnerByID(getInteger("ordner"), (User)getUser());

		User auser = (User)getContext().getDB().get(User.class, getInteger("playerid"));

		if( auser != null ) {
			ordner.deletePmsByUser(auser);

			t.setVar("show.message", "<span style=\"color:red\">Alle Nachrichten von "+Common._title(auser.getName())+" gel&ouml;scht</span>");
		}
		else {
			t.setVar("show.message", "<span style=\"color:red\">Der angegebene Spieler existiert nicht</span>");
		}

		redirect("showInbox");
	}

	/**
	 * Markiert die ausgewaehlten Nachrichten als gelesen.
	 * @urlparam Integer pm_$pmid Die ID einer als gelesen zu markierenden PM ($pmid gibt diese ebenfalls an)
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void readSelectedAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();

		List<?> pmList = db.createQuery("from PM where empfaenger=:user and gelesen < 1")
			.setEntity("user", user)
			.list();
		for( Iterator<?> iter=pmList.iterator(); iter.hasNext(); ) {
			PM pm = (PM)iter.next();

			parameterNumber("pm_"+pm.getId());
			int pmParam = getInteger("pm_"+pm.getId());

			if( (pmParam == pm.getId()) && !pm.hasFlag(PM.FLAGS_IMPORTANT) ) {
				pm.setGelesen(1);
			}
		}

		t.setVar("show.message", "<span style=\"color:red\">Nachrichten als gelesen markiert</span>");

		redirect("showInbox");
	}

	/**
	 * Verschiebt die ausgewaehlten Nachrichten/Ordner von einem Basisordner in einen anderen.
	 * @throws IOException
	 * @urlparam Integer ordner Der Basisordner
	 * @urlparam Integer moveto Die ID des Zielordners
	 * @urlparam Integer ordner_$ordnerid Die ID eines zu verschiebenden Ordners ($ordnerid gibt diese ebenfalls an)
	 * @urlparam Integer pm_$pmid Die ID einer zu verschiebenden PM ($pmid gibt diese ebenfalls an)
	 *
	 */
	@Action(ActionType.AJAX)
	public void moveAjaxAct() throws IOException {
		User user = (User)getUser();
		parameterNumber("moveto");
		parameterNumber("ordner");

		Ordner moveto = Ordner.getOrdnerByID( getInteger("moveto"), user );
		Ordner trash = Ordner.getTrash( user );
		Ordner source = Ordner.getOrdnerByID(getInteger("ordner"), user);

		if( moveto == null || source == null ) {
			getContext().getResponse().getWriter().append("Der angegebene Ordner existiert nicht");
			return;
		}

		if( trash == moveto ) {
			getContext().getResponse().getWriter().append("ERROR: Es duerfen keine Nachrichten/Ordner in den Papierkorb verschoben werden");
			return;
		}

		List<PM> pms = source.getPms();
		List<Ordner> ordners = source.getChildren();

		int counter = 0;
		for(int i = 0; i < ordners.size(); i++ ) {
			if( ordners.get(i).hasFlag(Ordner.FLAG_TRASH) ) {
				continue;
			}

			parameterNumber("ordner_"+ordners.get(i).getId());
			final int ordnerId = getInteger("ordner_"+ordners.get(i).getId());
			if( ordnerId == 0 ) {
				continue;
			}

			Ordner tomove = Ordner.getOrdnerByID(ordnerId, user);
			if( tomove == null ) {
				continue;
			}

			if( tomove.getAllChildren().contains(moveto)) {
				getContext().getResponse().getWriter().append("ERROR: Es duerfen keine Ordner in ihre eignen Unterordner verschoben werden");
				return;
			}


			if( tomove.getId() == ordners.get(i).getId() ){
				counter++;
				tomove.setParent(moveto);
			}
		}

		for( int i = 0; i < pms.size(); i++ ) {
			parameterNumber("pm_"+pms.get(i).getId());
			int pm = getInteger("pm_"+pms.get(i).getId());
			if( pm == pms.get(i).getId() ) {
				counter++;
				pms.get(i).setOrdner(moveto.getId());
			}
		}

		getContext().getResponse().getWriter().append(Integer.toString(counter));
		return;
	}

	/**
	 * Verschiebt die ausgewaehlten Nachrichten/Ordner von einem Basisordner in einen anderen.
	 * @urlparam Integer ordner Der Basisordner
	 * @urlparam Integer moveto Die ID des Zielordners
	 * @urlparam Integer ordner_$ordnerid Die ID eines zu verschiebenden Ordners ($ordnerid gibt diese ebenfalls an)
	 * @urlparam Integer pm_$pmid Die ID einer zu verschiebenden PM ($pmid gibt diese ebenfalls an)
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void moveSelectedAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();

		parameterNumber("moveto");
		parameterNumber("ordner");

		Ordner moveto = Ordner.getOrdnerByID( getInteger("moveto"), user );
		Ordner trash = Ordner.getTrash( user );
		Ordner source = Ordner.getOrdnerByID(getInteger("ordner"), user);

		if( moveto == null || source == null ) {
			t.setVar("show.message", "<span style=\"color:red\">Der angegebene Ordner existiert nicht</span>");
			redirect("showInbox");
			return;
		}

		if( trash.getId() == moveto.getId()){
			t.setVar("show.message", "<span style=\"color:red\">Es d&uuml;rfen keine Nachrichten/Ordner in den Papierkorb verschoben werden.</span>");
			redirect("showInbox");
			return;
		}

		List<PM> pms = source.getPms();
		List<Ordner> ordners = source.getChildren();

		int counter = 0;
		for(int i = 0; i < ordners.size(); i++ ) {
			if( ordners.get(i).hasFlag(Ordner.FLAG_TRASH) ) {
				continue;
			}

			parameterNumber("ordner_"+ordners.get(i).getId());
			final int ordnerId = getInteger("ordner_"+ordners.get(i).getId());
			if( ordnerId == 0 ) {
				continue;
			}

			Ordner tomove = Ordner.getOrdnerByID(ordnerId, user);
			if( tomove == null ) {
				continue;
			}

			if(tomove.equals(ordners.get(i)))
			{
				continue;
			}

			if( tomove.getAllChildren().contains(moveto)) {
				t.setVar("show.message", "<span style=\"color:red\">Es d&uuml;rfen keine Ordner in ihre eignen Unterordner verschoben werden.</span>");
				redirect("showInbox");
				return ;
			}


			if( tomove.getId() == ordners.get(i).getId() ){
				counter++;
				tomove.setParent(moveto);
			}
		}

		for( int i = 0; i < pms.size(); i++ ) {
			parameterNumber("pm_"+pms.get(i).getId());
			int pm = getInteger("pm_"+pms.get(i).getId());
			if( pm == pms.get(i).getId() ) {
				counter++;
				pms.get(i).setOrdner(moveto.getId());
			}
		}

		redirect("showInbox");
	}

	/**
	 * Loescht die ausgewaehlten Nachrichten/Ordner in einem Basisordner.
	 * @urlparam Integer ordner Der Basisordner
	 * @urlparam Integer ordner_$ordnerid Die ID eines zu loeschenden Ordners ($ordnerid gibt diese ebenfalls an)
	 * @urlparam Integer pm_$pmid Die ID einer zu loeschenden PM ($pmid gibt diese ebenfalls an)
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void deleteSelectedAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();

		parameterNumber("ordner");
		Ordner ordner = Ordner.getOrdnerByID(getInteger("ordner"), user);

		List<PM> pms = ordner.getPms();
		List<Ordner> ordners = ordner.getChildren();

		for( int i = 0; i < ordners.size(); i++ ) {
			if( ordners.get(i).hasFlag(Ordner.FLAG_TRASH) ) {
				continue;
			}

			parameterNumber("ordner_"+ordners.get(i).getId());
			final int ordnerId = getInteger("ordner_"+ordners.get(i).getId());
			if( ordnerId == 0 ) {
				continue;
			}

			Ordner delordner = Ordner.getOrdnerByID(ordnerId, user);

			if( delordner.getId() == ordners.get(i).getId() ) {
				delordner.deleteOrdner();
			}
		}

		for( int i = 0; i < pms.size(); i++ ) {
			parameterNumber("pm_"+pms.get(i).getId() );
			int pm_id = getInteger("pm_"+pms.get(i).getId());

			if( pm_id == pms.get(i).getId() ) {
				if( pms.get(i).getEmpfaenger() != user ) {
					continue;
				}
				pms.get(i).delete();
			}
		}

		t.setVar("show.message", "<span style=\"color:red\">Nachrichten gel&ouml;scht</span>");

		redirect("showInbox");
	}

	/**
	 * Versendet eine Nachricht.
	 * @urlparam String to Der Empfaenger (Eine ID oder "task" oder "ally")
	 * @urlparam Integer reply Falls != 0, dann die ID der Nachricht auf die geantwortet wird (Titel wird dann generiert)
	 * @urlparam String msg Der Text der Nachricht
	 * @urlparam String title Falls es sich nicht um eine Antwort handelt, dann der Titel der Nachricht
	 * @urlparam String special Falls es sich nicht um eine Antwort handelt, dann das Spezialflag der Nachricht
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void sendAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();

		parameterString("to");
		parameterNumber("reply");
		parameterString("msg");

		String to = getString("to").trim();
		int reply = getInteger("reply");
		String msg = getString("msg");

		String title = null;
		String special = null;

		if( reply > 0 ) {
			PM pm = (PM)db.get(PM.class, reply);
			if( (pm.getEmpfaenger().equals(user) || pm.getSender().equals(user)) && (pm.getGelesen() < 2) ) {
				User iTo = pm.getSender();
				if( iTo.equals(user) ) {
					iTo = pm.getEmpfaenger();
				}
				to = Integer.toString(iTo.getId());
				title = "RE: "+Common._plaintitle(pm.getTitle());
				special = "";
			}
		}
		else {
			parameterString("title");
			parameterString("special");

			title = getString("title");
			special = getString("special");
		}

		if( title.length() > 60 ) {
			title = title.substring(0,60);
		}

		if( special.equals("admin") && !hasPermission("comm", "adminPM") ) {
			special = "";
		}
		if( special.equals("official") && !Rassen.get().rasse(user.getRace()).isHead(user.getId()) ) {
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
			t.setVar("show.message", "<span style=\"color:#00ff55\">Antwort verarbeitet</span>");

			PM.send(user, PM.TASK, title, msg, flags );
		}
		else if( to.equals("ally") ) {
			if( user.getAlly() == null ) {
				t.setVar("show.message", "<span style=\"color:red; font-weight:bold\">Sie sind in keiner Allianz Mitglied</span>");

				return;
			}

			t.setVar("show.message",
					"<span style=\"color:#00ff55\">Nachricht versendet an</span> "+Common._title(user.getAlly().getName()));

			PM.sendToAlly(user, user.getAlly(), title, msg, flags );
		}
		else {
			if( (to.length() == 0) || (Integer.parseInt(to) == 0) ) {
				t.setVar("show.message", "<span style=\"color:#ff0000\">Sie m&uuml;ssen einen Empf&auml;nger angeben</span>");
				return;
			}

			int iTo = Integer.parseInt(to);

			User auser = (User)getContext().getDB().get(User.class, iTo);
			if( auser == null ) {
				t.setVar("show.message", "<span style=\"color:#ff0000\">Der angegebene Empf&auml;ger ist ung&uuml;ltig</span>");
				return;
			}
			t.setVar("show.message", "<span style=\"color:#00ff55\">Nachricht versendet an</span> "+Common._title(auser.getName()));

			PM.send(user, iTo, title, msg, flags );
		}
	}

	/**
	 * Zeigt eine empfangene/gesendete PM an.
	 * @urlparam Integer pmid Die ID der Nachricht
	 * @urlparam Integer ordner Die ID des Ordners, in dem sich die Nachricht befindet
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void showPmAction() {
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		BBCodeParser bbcodeparser = BBCodeParser.getNewInstance();

		parameterNumber("pmid");
		parameterNumber("ordner");
		int pmid = getInteger("pmid");
		int parent_id = getInteger("ordner");

		t.setVar("show.pm", 1);

		if( pmid == 0 ) {
			return;
		}

		PM pm = (PM)db.get(PM.class, pmid);
		if( (pm == null) || (!user.equals(pm.getEmpfaenger()) && !user.equals(pm.getSender())) ) {
			return;
		}

		User sender = null;

		if( user.equals(pm.getSender()) ) {
			try {
				bbcodeparser.registerHandler( "_intrnlConfTask", 2, "<div style=\"text-align:center\"><table class=\"noBorderX\" width=\"500\"><tr><td class=\"BorderX\" align=\"center\">Entscheidungsm&ouml;glichkeit in der Orginal-PM</td></tr></table></div>");
			}
			catch( Exception e ) {
				log.error("Register _intrnlConfTask failed", e);
				addError("Fehler beim Darstellen der PM");
			}

			if( user.equals(pm.getEmpfaenger()) && (pm.getGelesen() == 0) ) {
				pm.setGelesen(1);
			}

			User empfaenger = pm.getEmpfaenger();
			sender = user;
			if(empfaenger != null) {
				t.setVar(	"pm.empfaenger",		empfaenger.getId(),
							"pm.empfaenger.name",	Common._title(empfaenger.getName()));
			}
			else {
				t.setVar(	"pm.empfaenger",		"-",
						"pm.empfaenger.name", 		"Unbekannt");
			}
		}
		else {
			try {
				bbcodeparser.registerHandler( "_intrnlConfTask", 2, new TagIntrnlConfTask());
			}
			catch( Exception e ) {
				log.error("Register _intrnlConfTask failed", e);
				addError("Fehler beim Darstellen der PM");
			}

			if( pm.getGelesen() == 0 ) {
				pm.setGelesen(1);
			}

			sender = pm.getSender();

			if(sender != null)
			{
				t.setVar(	"pm.sender",		sender.getId(),
							"pm.sender.name", 	Common._title(sender.getName()),
							"ordner.parent",	parent_id);
			}
			else
			{
				t.setVar(	"pm.sender",	"-",
							"pm.sender.name", 	"Unbekannt",
							"ordner.parent",	parent_id);
			}
		}

		String bgimg = "";

		if( pm.hasFlag(PM.FLAGS_ADMIN) ) {
			bgimg = "pm_adminbg.png";
		}
		else if( pm.hasFlag(PM.FLAGS_OFFICIAL) ) {
			bgimg = "pm_"+Rassen.get().rasse(sender.getRace()).getName()+"bg.png";
		}

		String text = pm.getInhalt();
		text = bbcodeparser.parse(text);

		text = StringUtils.replace(text, "\r\n", "<br />");
		text = StringUtils.replace(text, "\n", "<br />");

		t.setVar(	"pm.id",			pm.getId(),
					"pm.title",			Common._plaintitle(pm.getTitle()),
					"pm.flags.admin", 	pm.hasFlag(PM.FLAGS_ADMIN),
					"pm.highlight",	pm.hasFlag(PM.FLAGS_ADMIN) || pm.hasFlag(PM.FLAGS_OFFICIAL),
					"pm.bgimage", 		bgimg,
					"pm.time", 			Common.date("j.n.Y G:i",pm.getTime()),
					"pm.text", 			Smilie.parseSmilies(text),
					"pm.kommentar", 	Smilie.parseSmilies(Common._text(pm.getKommentar())) );
	}

	/**
	 * Stellt eine Nachricht aus dem Papierkorb wieder her.
	 * @urlparam Integer recover Die wiederherzustellende Nachricht
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void recoverAction() {
		User user = (User)getUser();

		parameterNumber("recover");
		PM pm = (PM)getDB().get(PM.class, getInteger("recover"));

		if( pm.getEmpfaenger() == user ) {
			pm.recover();
		}

		redirect("showInbox");
	}

	/**
	 * Stellt alle geloeschten Nachrichten aus dem Papierkorb wieder her.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void recoverAllAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();

		PM.recoverAll( user );

		t.setVar("show.message", "<span style=\"color:red\">Nachrichten wiederhergestellt</span>");

		redirect("showInbox");
	}

	/**
	 * Zeigt die Liste aller empfangenen Nachrichten an.
	 * @urlparam Integer Der anzuzeigende Ordner (0 ist die oberste Ebene)
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void showInboxAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

		parameterNumber("ordner");
		final Ordner ordner = Ordner.getOrdnerByID(getInteger("ordner"), user);

		t.setVar(
				"show.inbox", 1,
				"currentordner.id", ordner.getId());

		t.setBlock("_COMM", "pms.listitem", "pms.list");
		t.setBlock("_COMM", "ordner.listitem", "ordner.list");
		t.setBlock("_COMM", "availordner.listitem", "availordner.list");

		// Liste aller vorhandenen Ordner generieren

		t.setVar(	"availordner.id",	0,
					"availordner.name",	"Hauptverzeichnis" );

		t.parse("availordner.list", "availordner.listitem", true);

		List<?> ordnerList = db.createQuery("from Ordner where owner= :user order by name asc")
			.setEntity("user", user)
			.list();
		for( Iterator<?> iter=ordnerList.iterator(); iter.hasNext(); ) {
			Ordner aOrdner = (Ordner)iter.next();

			t.setVar(	"availordner.id",	aOrdner.getId(),
						"availordner.name",	aOrdner.getName() );

			t.parse("availordner.list", "availordner.listitem", true);
		}

		// Link zum uebergeordneten Ordner erstellen
		if( ordner.getId() != 0 ) {
			t.setVar(	"ordner.id",			ordner.getParent().getId(),
						"ordner.name",			"..",
						"ordner.parent",		ordner.getId(),
						"ordner.pms",			ordner.getParent().getPmCount(),
						"ordner.flags.up",		1,
						"ordner.flags.trash",	(ordner.getFlags() & Ordner.FLAG_TRASH),
						"ordner.name.real",		ordner.getName());

			t.parse("ordner.list", "ordner.listitem", true);
		}

		Map<Ordner,Integer> ordners = ordner.getPmCountPerSubOrdner();

		// Ordnerliste im aktuellen Ordner ausgeben
		List<Ordner> children = ordner.getChildren();
		for( Ordner aOrdner : children ) {
			Integer count = ordners.get(aOrdner);

			t.setVar(	"ordner.id",			aOrdner.getId(),
						"ordner.name",			aOrdner.getName(),
						"ordner.parent",		aOrdner.getParent().getId(),
						"ordner.pms",			count != null ? count.intValue() : 0,
						"ordner.flags.up",		0,
						"ordner.flags.trash",	aOrdner.hasFlag(Ordner.FLAG_TRASH) );

			t.parse("ordner.list", "ordner.listitem", true);
		}

		// PMs im aktuellen Ordner ausgeben
		List<PM> pms = ordner.getPms();
		for( PM pm : pms ) {
			t.setVar(	"pm.id",			pm.getId(),
						"pm.new",			pm.getGelesen() == 0,
						"pm.flags.admin",	pm.hasFlag(PM.FLAGS_ADMIN),
						"pm.highlight",	pm.hasFlag(PM.FLAGS_ADMIN) || pm.hasFlag(PM.FLAGS_OFFICIAL),
						"pm.title",			Common._plaintitle(pm.getTitle()),
						"pm.sender.name",	Common._title(pm.getSender().getName()),
						"pm.sender.id",		pm.getSender().getId(),
						"pm.time",			Common.date("j.n.Y G:i",pm.getTime()),
						"pm.trash",			(pm.getGelesen() > 1) ? 1 : 0,
						"pm.kommentar",		pm.getKommentar());

			t.parse("pms.list", "pms.listitem", true);
		}
	}

	/**
	 * Zeigt die Liste aller versendeten und noch nicht geloeschten PMs.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void showOutboxAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

		t.setVar("show.outbox", 1);
		t.setBlock("_COMM", "pms.out.listitem", "pms.out.list");

		List<?> pms = db.createQuery("from PM as pm inner join fetch pm.empfaenger " +
				"where pm.sender= :user order by pm.id desc")
			.setEntity("user", user)
			.list();
		for( Iterator<?> iter=pms.iterator(); iter.hasNext(); ) {
			PM pm = (PM)iter.next();

			t.setVar(	"pm.id",				pm.getId(),
						"pm.flags.admin",		pm.hasFlag(PM.FLAGS_ADMIN),
						"pm.highlight",	pm.hasFlag(PM.FLAGS_ADMIN) || pm.hasFlag(PM.FLAGS_OFFICIAL),
						"pm.title",				Common._plaintitle(pm.getTitle()),
						"pm.empfaenger.name",	Common._title(pm.getEmpfaenger().getName()),
						"pm.time",				Common.date("j.n.Y G:i",pm.getTime()),
						"pm.empfaenger",		pm.getEmpfaenger().getId() );

			t.parse("pms.out.list", "pms.out.listitem", true);
		}
	}

	/**
	 * Zeigt eine Preview einer geschriebenen Nachricht an.
	 * @urlparam String msg Die Nachricht
	 * @urlparam String to Der Empfaenger der Nachricht
	 * @urlparam String title Der Titel der Nachricht
	 * @urlparam String special Spezialflag der Nachricht
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void previewAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

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
		if( hasPermission("comm", "adminPM") ) {
			specialuilist.put("admin", "admin");
		}
		if( Rassen.get().rasse(user.getRace()).isHead(user.getId()) ) {
			specialuilist.put("official", "Offizielle PM");
		}

		if( !specialuilist.containsKey(special) )
		{
			special = "";
		}

		t.setBlock("_COMM", "write.specialui.listitem", "write.specialui.list");
		for( Map.Entry<String, String> entry: specialuilist.entrySet() ) {
			t.setVar(	"specialui.name",	entry.getValue(),
						"specialui.value",	entry.getKey(),
						"specialui.selected",	special.equals(entry.getKey()) ? true : false);

			t.parse("write.specialui.list", "write.specialui.listitem", true);
		}

		String bgimg = "";

		if( "admin".equals(special) ) {
			bgimg = "pm_adminbg.png";
		}
		else if( "official".equals(special) ) {
			bgimg = "pm_"+Rassen.get().rasse(user.getRace()).getName()+"bg.png";
		}

		t.setVar(	"pm.text",			Smilie.parseSmilies(Common._text(msg)),
					"pm.title",			title,
					"pm.sender",		user.getId(),
					"pm.sender.name",	user.getName(),
					"pm.time",			Common.date("j.n.Y G:i", Common.time()),
					"pm.bgimage",		bgimg,
					"write.to",			to,
					"write.title",		title,
					"write.message",	msg,
					"show.preview",		1,
					"show.write",		1 );
	}

	/**
	 * Zeigt die GUI zum anlegen/bearbeiten eines Kommentars zu einer Nachricht an.
	 * @urlparam Integer pm Die Nachricht
	 * @urlparam Integer ordner Der Ordner, in dem sich die Nachricht befindet
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void editCommentAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

		parameterNumber("pm");
		parameterNumber("ordner");
		int pmid = getInteger("pm");
		int ordner = getInteger("ordner");

		PM pm = (PM)db.get(PM.class, pmid);
		if( (pm != null) && pm.getEmpfaenger().equals(user) ) {
			pm.setGelesen(1);

			t.setVar("show.comment", 1);
			t.setVar("comment.text", pm.getKommentar());
			t.setVar("pm.id", pmid);
			t.setVar("ordner.id", ordner);
			t.setVar("pm.title", pm.getTitle());
			t.setVar("pm.empfaenger.name", Common._title(pm.getEmpfaenger().getName()));
			t.setVar("pm.sender.name", Common._title(pm.getSender().getName()));
			t.setVar("pm.text", Smilie.parseSmilies(Common._text(pm.getInhalt())));
			t.setVar("system.time", Common.getIngameTime(getContext().get(ContextCommon.class).getTick()));
			t.setVar("user.signature", user.getUserValue("PMS/signatur") );
		}
	}

	/**
	 * Speichert einen Kommentar zu einer Nachricht.
	 * @urlparam Integer pmid Die ID der Nachricht
	 * @urlparam String msg Der Kommentar
	 */
	@Action(ActionType.DEFAULT)
	public void sendCommentAction() {
		org.hibernate.Session db = getDB();
		User user = (User)getUser();

		parameterNumber("pmid");
		parameterString("msg");
		int pmid = getInteger("pmid");
		String msg = getString("msg");

		PM pm = (PM)db.get(PM.class, pmid);
		if( (pm != null) && pm.getEmpfaenger().equals(user) ) {
			pm.setKommentar(msg);
		}

		redirect("showInbox");
	}

	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User)getUser();

		parameterString("to");
		parameterNumber("reply");
		parameterString("msg");

		String toStr = getString("to");
		int reply = getInteger("reply");
		String msg = getString("msg");

		String title = "";
		String special = "";

		if( reply != 0) {
			PM pm = (PM)db.get(PM.class, reply);

			if( (pm != null) && (pm.getEmpfaenger().equals(user) || pm.getSender().equals(user)) ) {
				User to = pm.getSender();
				if( to.equals(user) ) {
					to = pm.getEmpfaenger();
				}
				title = "RE: "+Common._plaintitle(pm.getTitle());
				special = "";

				msg = "(Nachricht am "+Common.date("j.n.Y G:i",pm.getTime())+" empfangen.)\n";

				// Fuehrende > entfernen
				msg += Pattern.compile("/\n>*/").matcher(pm.getInhalt()).replaceAll("\n");

				// Wegen der Einrueckung eingefuegte Umbrueche entfernen
				msg = msg.replaceAll("\t\r\n", " ");

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

				toStr = Integer.toString(to.getId());
			}
		}
		else {
			parameterString("title");
			parameterString("special");

			title = getString("title");
			special = getString("special");
		}

		if( title.length() > 60 ) {
			title = title.substring(0,60);
		}

		if( special.equals("admin") && !hasPermission("comm", "adminPM") ) {
			special = "";
		}
		if( special.equals("official") && !Rassen.get().rasse(user.getRace()).isHead(user.getId()) ) {
			special = "";
		}

		Map<String,String> specialuilist = new HashMap<String,String>();
		specialuilist.put("nichts", "");
		if( hasPermission("comm", "adminPM") ) {
			specialuilist.put("admin", "admin");
		}
		if( Rassen.get().rasse(user.getRace()).isHead(user.getId()) ) {
			specialuilist.put("Offizielle PM", "official");
		}

		t.setVar(	"show.write", 1,
					"write.title", title,
					"write.message", msg,
					"write.to", toStr,
					"system.time", Common.getIngameTime(getContext().get(ContextCommon.class).getTick()),
					"user.signature", user.getUserValue("PMS/signature") );

		t.setBlock("_COMM", "write.specialui.listitem", "write.specialui.list");
		if( specialuilist.size() > 1 )
		{
			for( Map.Entry<String, String> entry: specialuilist.entrySet() ) {
				t.setVar(	"specialui.name", entry.getKey(),
							"specialui.value", entry.getValue() );

				t.parse("write.specialui.list", "write.specialui.listitem", true);
			}
		}
	}
}
