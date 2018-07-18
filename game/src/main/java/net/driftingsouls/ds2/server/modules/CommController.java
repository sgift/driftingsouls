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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.bbcodes.TagIntrnlConfTask;
import net.driftingsouls.ds2.server.comm.Ordner;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.bbcode.Smilie;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.*;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Die PM-Verwaltung.
 *
 * @author Christopher Jung
 * @author Christian Peltz
 */
@Module(name = "comm")
public class CommController extends Controller
{
	private static final Log log = LogFactory.getLog(CommController.class);

	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public CommController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;

		setPageTitle("PMs");
		addPageMenuEntry("Neue Nachricht", Common.buildUrl("default", "to", 0));
		addPageMenuEntry("Posteingang", Common.buildUrl("showInbox"));
		addPageMenuEntry("Postausgang", Common.buildUrl("showOutbox"));
	}

	/**
	 * Markiert alle PMs in einem Ordner als gelesen.
	 *
	 * @param ordner Die ID des Ordners
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult readAllAction(Ordner ordner)
	{
		ordner.markAllAsRead();

		return new RedirectViewResult("showInbox").withMessage("<span style=\"color:red\">Alle Nachrichten als gelesen markiert</span>");
	}

	/**
	 * Loescht alle PMs in einem Ordner.
	 *
	 * @param ordner Der Ordner, dessen PMs geloescht werden sollen
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult deleteAllAction(Ordner ordner)
	{
		ordner.deleteAllPms();

		return new RedirectViewResult("showInbox").withMessage("<span style=\"color:red\">Alle Nachrichten gelöscht</span>");
	}

	/**
	 * Loescht einen Ordner/eine PM.
	 *  @param delete Falls eine PM zu loeschen ist, dann enthaelt dies die ID der PM. Andernfalls 0
	 * @param delord Die ID des zu loeschenden Ordners, andernfalls 0.
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult deleteAction(int delete, Ordner delord)
	{
		org.hibernate.Session db = getContext().getDB();
		User user = (User) getUser();

		int result = 0;
		if ((delord != null) && (delete == 0))
		{
			result = delord.deleteOrdner();
			db.flush();
		}
		else
		{
			PM pm = (PM) db.get(PM.class, delete);
			if (pm == null)
			{
				return new RedirectViewResult("showInbox").withMessage("<span style=\"color:red\">Die angegebene Nachricht existiert nicht</span>");
			}
			if (pm.getEmpfaenger() == user)
			{
				result = pm.delete();
				db.flush();
			}
		}

		String message = null;
		switch (result)
		{
			case 0:
				message = "<span style=\"color:red\">" + (delete != 0 ? "Nachricht" : "Ordner") + " gelöscht</span>";
				break;
			case 1:
				message = "<span style=\"color:red\">Sie müssen diese Nachricht erst lesen</span>";
				break;
			case 2:
				addError("Fehler: L&ouml;schen " + (delete != 0 ? "der PM" : "des Ordners") + " ist fehlgeschlagen");
				break;
		}

		return new RedirectViewResult("showInbox").withMessage(message);
	}

	/**
	 * Erstellt einen neuen Ordner.
	 *
	 * @param ordnername Der Name des neuen Ordners
	 * @param ordner Der Basisordner
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult newOrdnerAction(String ordnername, Ordner ordner)
	{
		User user = (User) getUser();

		if (ordner == null)
		{
			return new RedirectViewResult("showInbox");
		}

		Ordner.createNewOrdner(ordnername, ordner, user);

		return new RedirectViewResult("showInbox");
	}

	/**
	 * Verschiebt alle PMs von einem Ordner in einen anderen.
	 *
	 * @param moveto Der Zielordner
	 * @param ordner Der Ausgangsordner
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult moveAllAction(Ordner moveto, Ordner ordner)
	{
		User user = (User) getUser();

		if ((moveto != null) && (ordner != null))
		{
			PM.moveAllToOrdner(ordner, moveto, user);
		}

		return new RedirectViewResult("showInbox");
	}

	/**
	 * Benennt einen Ordner um.
	 *  @param ordnername Der neue Name des Ordners
	 * @param ordner Die ID des Ordners
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult renameAction(String ordnername, @UrlParam(name = "subject") Ordner ordner)
	{
		ordner.setName(ordnername);

		return new RedirectViewResult("showInbox");
	}

	/**
	 * Loescht alle PMs von einem bestimmten Spieler in einem Ordner.
	 *  @param player Die ID des Spielers
	 * @param ordner Die ID des Ordners
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult deletePlayerAction(@UrlParam(name = "playerid") User player, Ordner ordner)
	{
		String message;
		if (player != null)
		{
			ordner.deletePmsByUser(player);

			message = "<span style=\"color:red\">Alle Nachrichten von " + Common._title(player.getName()) + " gelöscht</span>";
		}
		else
		{
			message = "<span style=\"color:red\">Der angegebene Spieler existiert nicht</span>";
		}

		return new RedirectViewResult("showInbox").withMessage(message);
	}

	/**
	 * Markiert die ausgewaehlten Nachrichten als gelesen.
	 *
	 * @param pms Die IDs aller aös gelesen zu markierenden PMs
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult readSelectedAction(@UrlParam(name = "pm_#") Map<Integer, Integer> pms)
	{
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		List<?> pmList = db.createQuery("from PM where empfaenger=:user and gelesen < 1")
				.setEntity("user", user)
				.list();
		for (Object aPmList : pmList)
		{
			PM pm = (PM) aPmList;

			Integer pmParam = pms.get(pm.getId());

			if (pmParam != null && (pmParam == pm.getId()) && !pm.hasFlag(PM.FLAGS_IMPORTANT))
			{
				pm.setGelesen(1);
			}
		}

		return new RedirectViewResult("showInbox").withMessage("<span style=\"color:red\">Nachrichten als gelesen markiert</span>");
	}

	/**
	 * Verschiebt die ausgewaehlten Nachrichten/Ordner von einem Basisordner in einen anderen.
	 *
	 * @param source Der Basisordner
	 * @param moveto Die ID des Zielordners
	 * @param ordnerMap Die IDs der zu verschiebenden Ordner
	 * @param pmMap Die IDs der zu verschiebenden PMs
	 * @throws IOException
	 */
	@Action(ActionType.AJAX)
	public String moveAjaxAct(Ordner moveto, @UrlParam(name = "ordner") Ordner source, @UrlParam(name = "ordner_#") Map<Integer, Ordner> ordnerMap, @UrlParam(name = "pm_#") Map<Integer, Integer> pmMap) throws IOException
	{
		User user = (User) getUser();

		Ordner trash = Ordner.getTrash(user);

		if (moveto == null || source == null)
		{
			return "Der angegebene Ordner existiert nicht";
		}

		if (trash == moveto)
		{
			return "ERROR: Es duerfen keine Nachrichten/Ordner in den Papierkorb verschoben werden";
		}

		List<PM> pms = source.getPms();
		List<Ordner> ordners = source.getChildren();

		int counter = 0;
		for (Ordner ordner : ordners)
		{
			if (ordner.hasFlag(Ordner.FLAG_TRASH))
			{
				continue;
			}

			Ordner tomove = ordnerMap.get(ordner.getId());
			if (tomove == null)
			{
				continue;
			}

			if (tomove.getAllChildren().contains(moveto))
			{
				return "ERROR: Es duerfen keine Ordner in ihre eignen Unterordner verschoben werden";
			}


			if (tomove.getId() == ordner.getId())
			{
				counter++;
				tomove.setParent(moveto);
			}
		}

		for (PM pm1 : pms)
		{
			Integer pm = pmMap.get(pm1.getId());
			if (pm != null && pm == pm1.getId())
			{
				counter++;
				pm1.setOrdner(moveto.getId());
			}
		}

		return Integer.toString(counter);
	}

	/**
	 * Verschiebt die ausgewaehlten Nachrichten/Ordner von einem Basisordner in einen anderen.
	 *  @param moveto Die ID des Zielordners
	 * @param source Der Basisordner
	 * @param ordnerMap Die IDs der zu verschiebenden Ordner
	 * @param pmMap Die IDs der zu verschiebenden PMs
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult moveSelectedAction(Ordner moveto, @UrlParam(name = "ordner") Ordner source, @UrlParam(name = "ordner_#") Map<Integer, Ordner> ordnerMap, @UrlParam(name = "pm_#") Map<Integer, Integer> pmMap)
	{
		User user = (User) getUser();

		Ordner trash = Ordner.getTrash(user);

		if (moveto == null || source == null)
		{
			return new RedirectViewResult("showInbox").withMessage("<span style=\"color:red\">Der angegebene Ordner existiert nicht</span>");
		}

		if (trash.getId() == moveto.getId())
		{
			return new RedirectViewResult("showInbox").withMessage("<span style=\"color:red\">Es dürfen keine Nachrichten/Ordner in den Papierkorb verschoben werden.</span>");
		}

		List<PM> pms = source.getPms();
		List<Ordner> ordners = source.getChildren();

		for (Ordner ordner : ordners)
		{
			if (ordner.hasFlag(Ordner.FLAG_TRASH))
			{
				continue;
			}

			Ordner tomove = ordnerMap.get(ordner.getId());
			if (tomove == null)
			{
				continue;
			}

			if (tomove.equals(ordner))
			{
				continue;
			}

			if (tomove.getAllChildren().contains(moveto))
			{
				return new RedirectViewResult("showInbox").withMessage("<span style=\"color:red\">Es dürfen keine Ordner in ihre eignen Unterordner verschoben werden.</span>");
			}


			if (tomove.getId() == ordner.getId())
			{
				tomove.setParent(moveto);
			}
		}

		for (PM pm1 : pms)
		{
			Integer pm = pmMap.get(pm1.getId());
			if (pm != null && pm == pm1.getId())
			{
				pm1.setOrdner(moveto.getId());
			}
		}

		return new RedirectViewResult("showInbox");
	}

	/**
	 * Loescht die ausgewaehlten Nachrichten/Ordner in einem Basisordner.
	 *  @param ordner Der Basisordner
	 * @param ordnerMap Die IDd der zu loeschenden Ordner
	 * @param pmMap Die IDs der zu loeschenden PMs
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult deleteSelectedAction(Ordner ordner, @UrlParam(name = "ordner_#") Map<Integer, Ordner> ordnerMap, @UrlParam(name = "pm_#") Map<Integer, Integer> pmMap)
	{
		User user = (User) getUser();

		List<PM> pms = ordner.getPms();
		List<Ordner> ordners = ordner.getChildren();

		for (Ordner ordner1 : ordners)
		{
			if (ordner1.hasFlag(Ordner.FLAG_TRASH))
			{
				continue;
			}

			Ordner delordner = ordnerMap.get(ordner1.getId());

			if (delordner != null && delordner.getId() == ordner1.getId())
			{
				delordner.deleteOrdner();
			}
		}

		for (PM pm : pms)
		{
			Integer pmId = pmMap.get(pm.getId());

			if (pmId != null && pmId == pm.getId())
			{
				if (pm.getEmpfaenger() != user)
				{
					continue;
				}
				pm.delete();
			}
		}

		return new RedirectViewResult("showInbox").withMessage("<span style=\"color:red\">Nachrichten gelöscht</span>");
	}

	/**
	 * Versendet eine Nachricht.
	 *  @param to Der Empfaenger (Eine ID oder "task" oder "ally")
	 * @param reply Falls != 0, dann die ID der Nachricht auf die geantwortet wird (Titel wird dann generiert)
	 * @param msg Der Text der Nachricht
	 * @param title Falls es sich nicht um eine Antwort handelt, dann der Titel der Nachricht
	 * @param special Falls es sich nicht um eine Antwort handelt, dann das Spezialflag der Nachricht
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine sendAction(String to, PM reply, String msg, String sendeziel, String title, String special)
	{
		User user = (User) getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);

		if (reply != null)
		{
			if ((reply.getEmpfaenger().equals(user) || reply.getSender().equals(user)) && (reply.getGelesen() < 2))
			{
				User iTo = reply.getSender();
				if (iTo.equals(user))
				{
					iTo = reply.getEmpfaenger();
				}
				to = Integer.toString(iTo.getId());
				title = "RE: " + Common._plaintitle(reply.getTitle());
				special = "";
			}
		}

		if (title.length() > 60)
		{
			title = title.substring(0, 60);
		}

		if (special.equals("admin") && !hasPermission(WellKnownPermission.COMM_ADMIN_PM))
		{
			special = "";
		}
		if (special.equals("official") && !hasPermission(WellKnownPermission.COMM_OFFIZIELLE_PM))
		{
			special = "";
		}

		int flags = 0;

		if (special.equals("admin"))
		{
			flags |= PM.FLAGS_ADMIN;
			flags |= PM.FLAGS_IMPORTANT;
		}
		else if (special.equals("official"))
		{
			flags |= PM.FLAGS_OFFICIAL;
		}

		if ("task".equals(to))
		{
			t.setVar("show.message", "<span style=\"color:#00ff55\">Antwort verarbeitet</span>");

			PM.send(user, PM.TASK, title, msg, flags);
		}
		else if ("ally".equals(to) || "ally".equals(sendeziel))
		{
			if (user.getAlly() == null)
			{
				t.setVar("show.message", "<span style=\"color:red; font-weight:bold\">Sie sind in keiner Allianz Mitglied</span>");

				return t;
			}

			t.setVar("show.message",
					"<span style=\"color:#00ff55\">Nachricht versendet an</span> " + Common._title(user.getAlly().getName()));

			PM.sendToAlly(user, user.getAlly(), title, msg, flags);
		}
		else
		{
			User auser = User.lookupByIdentifier(to);
			if (auser == null)
			{
				t.setVar("show.message", "<span style=\"color:#ff0000\">Sie m&uuml;ssen einen gülten Empf&auml;nger angeben</span>");
				return t;
			}
			t.setVar("show.message", "<span style=\"color:#00ff55\">Nachricht versendet an</span> " + Common._title(auser.getName()));

			PM.send(user, auser.getId(), title, msg, flags);
		}

		return t;
	}

	/**
	 * Zeigt eine empfangene/gesendete PM an.
	 *  @param pm Die ID der Nachricht
	 * @param ordner Die ID des Ordners, in dem sich die Nachricht befindet
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine showPmAction(@UrlParam(name = "pmid") PM pm, Ordner ordner)
	{
		User user = (User) getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);

		BBCodeParser bbcodeparser = BBCodeParser.getNewInstance();

		t.setVar("show.pm", 1);

		if ((pm == null) || (!user.equals(pm.getEmpfaenger()) && !user.equals(pm.getSender())))
		{
			return t;
		}

		User sender;

		if (user.equals(pm.getSender()))
		{
			try
			{
				bbcodeparser.registerHandler("_intrnlConfTask", 2, "<div style=\"text-align:center\"><table class=\"noBorderX\" width=\"500\"><tr><td class=\"BorderX\" align=\"center\">Entscheidungsm&ouml;glichkeit in der Orginal-PM</td></tr></table></div>");
			}
			catch (Exception e)
			{
				log.error("Register _intrnlConfTask failed", e);
				addError("Fehler beim Darstellen der PM");
			}

			if (user.equals(pm.getEmpfaenger()) && (pm.getGelesen() == 0))
			{
				pm.setGelesen(1);
			}

			User empfaenger = pm.getEmpfaenger();
			sender = user;
			if (empfaenger != null)
			{
				t.setVar("pm.empfaenger", empfaenger.getId(),
						"pm.empfaenger.name", Common._title(empfaenger.getName()));
			}
			else
			{
				t.setVar("pm.empfaenger", "-",
						"pm.empfaenger.name", "Unbekannt");
			}
		}
		else
		{
			try
			{
				bbcodeparser.registerHandler("_intrnlConfTask", 2, new TagIntrnlConfTask());
			}
			catch (Exception e)
			{
				log.error("Register _intrnlConfTask failed", e);
				addError("Fehler beim Darstellen der PM");
			}

			if (pm.getGelesen() == 0)
			{
				pm.setGelesen(1);
			}

			sender = pm.getSender();

			if (sender != null)
			{
				t.setVar("pm.sender", sender.getId(),
						"pm.sender.name", Common._title(sender.getName()),
						"ordner.parent", ordner != null ? ordner.getId() : 0);
			}
			else
			{
				t.setVar("pm.sender", "-",
						"pm.sender.name", "Unbekannt",
						"ordner.parent", ordner != null ? ordner.getId() : 0);
			}
		}

		String bgimg = "";

		if (pm.hasFlag(PM.FLAGS_ADMIN))
		{
			bgimg = "pm_adminbg.png";
		}
		else if (sender != null && pm.hasFlag(PM.FLAGS_OFFICIAL))
		{
			bgimg = "pm_" + Rassen.get().rasse(sender.getRace()).getName() + "bg.png";
		}

		String text = pm.getInhalt();
		text = Common.escapeHTML(text);
		text = bbcodeparser.parse(text);

		text = text.replace("\r\n", "<br />");
		text = text.replace("\n", "<br />");

		t.setVar("pm.id", pm.getId(),
				"pm.title", Common._plaintitle(pm.getTitle()),
				"pm.flags.admin", pm.hasFlag(PM.FLAGS_ADMIN),
				"pm.highlight", pm.hasFlag(PM.FLAGS_ADMIN) || pm.hasFlag(PM.FLAGS_OFFICIAL),
				"pm.bgimage", bgimg,
				"pm.time", Common.date("j.n.Y G:i", pm.getTime()),
				"pm.text", Smilie.parseSmilies(text),
				"pm.kommentar", Smilie.parseSmilies(Common._text(pm.getKommentar())));

		return t;
	}

	/**
	 * Stellt eine Nachricht aus dem Papierkorb wieder her.
	 *
	 * @param recover Die wiederherzustellende Nachricht
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult recoverAction(PM recover)
	{
		User user = (User) getUser();

		if (recover != null && recover.getEmpfaenger() == user)
		{
			recover.recover();
		}

		return new RedirectViewResult("showInbox");
	}

	/**
	 * Stellt alle geloeschten Nachrichten aus dem Papierkorb wieder her.
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult recoverAllAction()
	{
		User user = (User) getUser();

		PM.recoverAll(user);

		return new RedirectViewResult("showInbox").withMessage("<span style=\"color:red\">Nachrichten wiederhergestellt</span>");
	}

	/**
	 * Zeigt die Liste aller empfangenen Nachrichten an.
	 *
	 * @param ordner Der anzuzeigende Ordner (0 ist die oberste Ebene)
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine showInboxAction(Ordner ordner, RedirectViewResult redirect)
	{
		org.hibernate.Session db = getDB();
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();

		t.setVar(
				"show.inbox", 1,
				"currentordner.id", ordner.getId(),
				"show.message", redirect != null ? redirect.getMessage() : null);

		t.setBlock("_COMM", "pms.listitem", "pms.list");
		t.setBlock("_COMM", "ordner.listitem", "ordner.list");
		t.setBlock("_COMM", "availordner.listitem", "availordner.list");

		// Liste aller vorhandenen Ordner generieren

		t.setVar("availordner.id", 0,
				"availordner.name", "Hauptverzeichnis");

		t.parse("availordner.list", "availordner.listitem", true);

		List<?> ordnerList = db.createQuery("from Ordner where owner= :user order by name asc")
				.setEntity("user", user)
				.list();
		for (Object anOrdnerList : ordnerList)
		{
			Ordner aOrdner = (Ordner) anOrdnerList;

			t.setVar("availordner.id", aOrdner.getId(),
					"availordner.name", aOrdner.getName());

			t.parse("availordner.list", "availordner.listitem", true);
		}

		// Link zum uebergeordneten Ordner erstellen
		if (ordner.getId() != 0)
		{
			t.setVar("ordner.id", ordner.getParent().getId(),
					"ordner.name", "..",
					"ordner.parent", ordner.getId(),
					"ordner.pms", ordner.getParent().getPmCount(),
					"ordner.flags.up", 1,
					"ordner.flags.trash", (ordner.getFlags() & Ordner.FLAG_TRASH),
					"ordner.name.real", ordner.getName());

			t.parse("ordner.list", "ordner.listitem", true);
		}

		Map<Ordner, Integer> ordners = ordner.getPmCountPerSubOrdner();

		// Ordnerliste im aktuellen Ordner ausgeben
		List<Ordner> children = ordner.getChildren();
		for (Ordner aOrdner : children)
		{
			Integer count = ordners.get(aOrdner);

			t.setVar("ordner.id", aOrdner.getId(),
					"ordner.name", aOrdner.getName(),
					"ordner.parent", aOrdner.getParent().getId(),
					"ordner.pms", count != null ? count : 0,
					"ordner.flags.up", 0,
					"ordner.flags.trash", aOrdner.hasFlag(Ordner.FLAG_TRASH));

			t.parse("ordner.list", "ordner.listitem", true);
		}

		// PMs im aktuellen Ordner ausgeben
		List<PM> pms = ordner.getPms();
		for (PM pm : pms)
		{
			String title = pm.getTitle();
			if (title == null || title.trim().isEmpty())
			{
				title = "<Kein Betreff>";
			}
			t.setVar("pm.id", pm.getId(),
					"pm.new", pm.getGelesen() == 0,
					"pm.flags.admin", pm.hasFlag(PM.FLAGS_ADMIN),
					"pm.highlight", pm.hasFlag(PM.FLAGS_ADMIN) || pm.hasFlag(PM.FLAGS_OFFICIAL),
					"pm.title", Common._plaintitle(title),
					"pm.sender.name", Common._title(pm.getSender().getName()),
					"pm.sender.id", pm.getSender().getId(),
					"pm.time", Common.date("j.n.Y G:i", pm.getTime()),
					"pm.trash", (pm.getGelesen() > 1) ? 1 : 0,
					"pm.kommentar", pm.getKommentar());

			t.parse("pms.list", "pms.listitem", true);
		}

		return t;
	}

	/**
	 * Zeigt die Liste aller versendeten und noch nicht geloeschten PMs.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine showOutboxAction()
	{
		org.hibernate.Session db = getDB();
		TemplateEngine t = templateViewResultFactory.createFor(this);

		User user = (User) getUser();

		t.setVar("show.outbox", 1);
		t.setBlock("_COMM", "pms.out.listitem", "pms.out.list");

		List<?> pms = db.createQuery("from PM as pm inner join fetch pm.empfaenger " +
				"where pm.sender= :user order by pm.id desc")
				.setEntity("user", user)
				.list();
		for (Object pm1 : pms)
		{
			PM pm = (PM) pm1;

			String title = pm.getTitle();
			if (title == null || title.trim().isEmpty())
			{
				title = "<Kein Betreff>";
			}

			t.setVar("pm.id", pm.getId(),
					"pm.flags.admin", pm.hasFlag(PM.FLAGS_ADMIN),
					"pm.highlight", pm.hasFlag(PM.FLAGS_ADMIN) || pm.hasFlag(PM.FLAGS_OFFICIAL),
					"pm.title", Common._plaintitle(title),
					"pm.empfaenger.name", Common._title(pm.getEmpfaenger().getName()),
					"pm.time", Common.date("j.n.Y G:i", pm.getTime()),
					"pm.empfaenger", pm.getEmpfaenger().getId());

			t.parse("pms.out.list", "pms.out.listitem", true);
		}

		return t;
	}

	/**
	 * Zeigt eine Preview einer geschriebenen Nachricht an.
	 *  @param msg Die Nachricht
	 * @param to Der Empfaenger der Nachricht
	 * @param title Der Titel der Nachricht
	 * @param special Spezialflag der Nachricht
	 * @param sendeziel Der Empfaengertyp der Nachricht
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine previewAction(String msg, String to, String title, String special, String sendeziel)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);

		User user = (User) getUser();

		Map<String, String> specialuilist = new LinkedHashMap<>();
		specialuilist.put("nichts", "");
		if (hasPermission(WellKnownPermission.COMM_ADMIN_PM))
		{
			specialuilist.put("admin", "admin");
		}
		if (hasPermission(WellKnownPermission.COMM_OFFIZIELLE_PM))
		{
			specialuilist.put("official", "Offizielle PM");
		}

		if (!specialuilist.containsKey(special))
		{
			special = "";
		}

		t.setBlock("_COMM", "write.specialui.listitem", "write.specialui.list");
		for (Map.Entry<String, String> entry : specialuilist.entrySet())
		{
			t.setVar("specialui.name", entry.getValue(),
					"specialui.value", entry.getKey(),
					"specialui.selected", special.equals(entry.getKey()));

			t.parse("write.specialui.list", "write.specialui.listitem", true);
		}

		String bgimg = "";

		if ("admin".equals(special))
		{
			bgimg = "pm_adminbg.png";
		}
		else if ("official".equals(special))
		{
			bgimg = "pm_" + Rassen.get().rasse(user.getRace()).getName() + "bg.png";
		}

		t.setVar("pm.text", Smilie.parseSmilies(Common._text(msg)),
				"pm.title", title,
				"pm.sender", user.getId(),
				"pm.sender.name", Common._title(user.getName()),
				"pm.time", Common.date("j.n.Y G:i", Common.time()),
				"pm.bgimage", bgimg,
				"write.to", to,
				"write.title", title,
				"write.message", msg,
				"sendeziel.ally", "ally".equals(sendeziel),
				"show.preview", 1,
				"show.write", 1,
				"system.time", Common.getIngameTime(getContext().get(ContextCommon.class).getTick()));

		return t;
	}

	/**
	 * Zeigt die GUI zum anlegen/bearbeiten eines Kommentars zu einer Nachricht an.
	 *  @param pm Die Nachricht
	 * @param ordner Der Ordner, in dem sich die Nachricht befindet
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine editCommentAction(PM pm, Ordner ordner)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);

		User user = (User) getUser();

		if ((pm != null) && pm.getEmpfaenger().equals(user))
		{
			pm.setGelesen(1);

			t.setVar("show.comment", 1);
			t.setVar("comment.text", pm.getKommentar());
			t.setVar("pm.id", pm.getId());
			t.setVar("ordner.id", ordner != null ? ordner.getId() : 0);
			t.setVar("pm.title", pm.getTitle());
			t.setVar("pm.empfaenger.name", Common._title(pm.getEmpfaenger().getName()));
			t.setVar("pm.sender.name", Common._title(pm.getSender().getName()));
			t.setVar("pm.text", Smilie.parseSmilies(Common._text(pm.getInhalt())));
			t.setVar("system.time", Common.getIngameTime(getContext().get(ContextCommon.class).getTick()));
			t.setVar("user.signature", user.getUserValue(WellKnownUserValue.PMS_SIGNATURE));
		}

		return t;
	}

	/**
	 * Speichert einen Kommentar zu einer Nachricht.
	 *
	 * @param pm Die ID der Nachricht
	 * @param msg Der Kommentar
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult sendCommentAction(@UrlParam(name = "pmid") PM pm, String msg)
	{
		User user = (User) getUser();

		if ((pm != null) && pm.getEmpfaenger().equals(user))
		{
			pm.setKommentar(msg);
		}

		return new RedirectViewResult("showInbox");
	}

	/**
	 * Zeigt die GUI zum Versenden einer PM.
	 *  @param toStr Der Empfaenger der neuen PM
	 * @param reply Die ID der PM, auf die geantwortet wird
	 * @param msg Der Text der PM
	 * @param title Der Titel der PM
	 * @param special Die Spezialmarkierung (admin, official)
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(@UrlParam(name = "to") String toStr, PM reply, String msg, String title, String special)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);

		User user = (User) getUser();

		if (reply != null && (reply.getEmpfaenger().equals(user) || reply.getSender().equals(user)))
		{
			User to = reply.getSender();
			if (to.equals(user))
			{
				to = reply.getEmpfaenger();
			}
			title = "RE: " + Common._plaintitle(reply.getTitle());
			special = "";

			msg = "(Nachricht am " + Common.date("j.n.Y G:i", reply.getTime()) + " empfangen.)\n";

			// Fuehrende > entfernen
			msg += Pattern.compile("/\n>*/").matcher(reply.getInhalt()).replaceAll("\n");

			// Wegen der Einrueckung eingefuegte Umbrueche entfernen
			msg = msg.replaceAll("\t\r\n", " ");

			// Reply-Verschachtelungstiefe ermitteln
			int depth = 0;
			Matcher match = Pattern.compile("/\\(Nachricht am \\d{1,2}\\.\\d{1,2}\\.\\d{4,4} \\d{1,2}:\\d{2,2} empfangen\\.\\)/").matcher(msg);
			while (match.find())
			{
				depth++;
			}

			String[] msg_lines = StringUtils.split(msg, '\n'); //Text Zeilenweise auftrennen
			for (int i = 0; i < msg_lines.length; i++)
			{
				msg_lines[i] = Common.wordwrap(msg_lines[i], 65 - depth, "\t\n");    //Zeilen umbrechen

				if (Pattern.compile("/\\(Nachricht am \\d{1,2}\\.\\d{1,2}\\.\\d{4,4} \\d{1,2}:\\d{2,2} empfangen\\.\\)/").matcher(msg_lines[i]).find())
				{
					//beginn einer neuen Verschachtelung
					for (int j = i + 1; j < msg_lines.length; j++)
					{
						//in Jede zeile ein ">" am Anfang einfuegen
						msg_lines[j] = ">" + msg_lines[j];
					}
				}
			}
			msg = Common.implode("\n", msg_lines); //Text wieder zusammenfuegen
			msg += "\n\n"; // Zwei Leerzeilen koennen am Ende nicht schaden...

			toStr = Integer.toString(to.getId());
		}

		if (title.length() > 60)
		{
			title = title.substring(0, 60);
		}

		if ("admin".equals(special) && !hasPermission(WellKnownPermission.COMM_ADMIN_PM))
		{
			special = "";
		}
		if ("official".equals(special) && !hasPermission(WellKnownPermission.COMM_OFFIZIELLE_PM))
		{
			special = "";
		}

		Map<String, String> specialuilist = new LinkedHashMap<>();
		specialuilist.put("nichts", "");
		if (hasPermission(WellKnownPermission.COMM_ADMIN_PM))
		{
			specialuilist.put("admin", "admin");
		}
		if (hasPermission(WellKnownPermission.COMM_OFFIZIELLE_PM))
		{
			specialuilist.put("Offizielle PM", "official");
		}

		t.setVar("show.write", 1,
				"write.title", title,
				"write.message", msg,
				"write.to", toStr,
				"system.time", Common.getIngameTime(getContext().get(ContextCommon.class).getTick()),
				"user.signature", user.getUserValue(WellKnownUserValue.PMS_SIGNATURE));

		t.setBlock("_COMM", "write.specialui.listitem", "write.specialui.list");
		if (specialuilist.size() > 1)
		{
			for (Map.Entry<String, String> entry : specialuilist.entrySet())
			{
				t.setVar("specialui.name", entry.getKey(),
						"specialui.value", entry.getValue(),
						"specialui.selected", entry.getKey().equals(special));

				t.parse("write.specialui.list", "write.specialui.listitem", true);
			}
		}

		return t;
	}
}
