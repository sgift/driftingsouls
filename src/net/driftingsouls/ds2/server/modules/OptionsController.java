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
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateController;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.namegenerator.PersonenNamenGenerator;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.List;

/**
 * Aendern der Einstellungen eines Benutzers durch den Benutzer selbst.
 *
 * @author Christopher Jung
 */
@Module(name = "options")
public class OptionsController extends TemplateController
{
	private static final Log log = LogFactory.getLog(OptionsController.class);

	/**
	 * Konstruktor.
	 *
	 * @param context Der zu verwendende Kontext
	 */
	public OptionsController(Context context)
	{
		super(context);
	}

	/**
	 * Aendert den Namen und das Passwort des Benutzers.
	 *
	 * @param name Der neue Benutzername
	 * @param pw Das neue Passwort
	 * @param pw2 Die Wiederholung des neuen Passworts
	 */
	@Action(ActionType.DEFAULT)
	public void changeNamePassAction(String name, String pw, String pw2)
	{
		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();

		String changemsg = "";

		if ((name.length() != 0) && !name.equals(user.getNickname()))
		{
			boolean addhistory = false;

			BBCodeParser bbcodeparser = BBCodeParser.getInstance();
			if (!bbcodeparser.parse(user.getNickname(), new String[]{"all"}).trim().equals(bbcodeparser.parse(name, new String[]{"all"}).trim()))
			{
				addhistory = true;
			}

			String newname = name;
			if (user.getAlly() != null)
			{
				newname = user.getAlly().getAllyTag();
				newname = StringUtils.replace(newname, "[name]", name);
			}

			changemsg += "<span style=\"color:green\">Der Ingame-Namen <span style=\"color:white\">" + Common._title(user.getNickname()) + "</span> wurde in <span style=\"color:white\">" + Common._title(name) + "</span> ge&auml;ndert</span><br />\n";

			Common.writeLog("login.log", Common.date("j.m.Y H:i:s") + ": <" + getContext().getRequest().getRemoteAddress() + "> (" + user.getId() + ") <" + user.getUN() + "> Namensaenderung: Ingame-Namen <" + user.getNickname() + "> in <" + name + "> Browser <" + getContext().getRequest().getUserAgent() + ">\n");

			if (addhistory)
			{
				user.addHistory(Common.getIngameTime(getContext().get(ContextCommon.class).getTick()) + ": Umbenennung in " + newname);
			}

			user.setName(newname);
			user.setNickname(name);
		}

		if ((pw.length() != 0) && pw.equals(pw2))
		{
			String enc_pw = Common.md5(pw);

			user.setPassword(enc_pw);
			changemsg += "<span style=\"color:red\">Das Password wurde ge&auml;ndert</span><br />\n";

			String subject = "Drifting Souls - Passwortaenderung";
			String message = Common.trimLines(Configuration.getSetting("PWCHANGE_EMAIL"));
			message = StringUtils.replace(message, "{username}", user.getUN());
			message = StringUtils.replace(message, "{date}", Common.date("H:i j.m.Y"));

			Common.mail(user.getEmail(), subject, message);

			Common.writeLog("login.log", Common.date("j.m.Y H:i:s") + ": <" + getContext().getRequest().getRemoteAddress() + "> (" + user.getId() + ") <" + user.getUN() + "> Passwortaenderung Browser <" + getContext().getRequest().getUserAgent() + "> \n");
		}
		else if (pw.length() != 0)
		{
			changemsg += "<span style=\"color:red\">Die beiden eingegebenen Passw&ouml;rter stimmen nicht &uuml;berein</span><br />\n";
		}

		t.setVar("options.changenamepwd", 1,
				"options.changenamepwd.nickname", Common._plaintitle(user.getNickname()),
				"options.message", changemsg);
	}

	/**
	 * Sendet die Löschanfrage des Spielers.
	 *
	 * @param del Der Interaktionsschritt. Bei 0 wird das Eingabeformular angezeigt. Andernfalls wird versucht die Anfrage zu senden
	 * @param reason Die schluessige Begruendung. Muss mindestens die Laenge 5 haben
	 */
	@Action(ActionType.DEFAULT)
	public void delAccountAction(int del, String reason)
	{
		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();

		if (del == 0)
		{
			t.setVar("options.delaccountform", 1);

		}
		else if (reason.length() < 5)
		{
			t.setVar("options.message", "Bitte geben sie Gr&uuml;nde f&uuml;r die L&ouml;schung an!<br />\n",
					"options.delaccountform", 1);

		}
		else
		{
			StringBuilder msg = new StringBuilder(100);
			msg.append("PLZ DELETE ME!!!\nMY ID IS: [userprofile=");
			msg.append(user.getId());
			msg.append("]");
			msg.append(user.getId());
			msg.append("[/userprofile]\n");
			msg.append("MY UN IS: ");
			msg.append(user.getUN());
			msg.append("\n");
			msg.append("MY CURRENT NAME IS: ");
			msg.append(user.getName());
			msg.append("\n");
			msg.append("MY REASONS:\n");
			msg.append(reason);
			PM.sendToAdmins(user, "Account l&ouml;schen", msg.toString(), 0);

			t.setVar("options.delaccountresp", 1,
					"delaccountresp.admins", Configuration.getSetting("ADMIN_PMS_ACCOUNT"));

		}
	}

	/**
	 * Aendert die erweiterten Einstellungen des Spielers.
	 *
	 * @param shipgroupmulti der neue Schiffsgruppierungswert
	 * @param inttutorial Die Seite des Tutorials in der Uebersicht (0 = deaktiviert)
	 * @param scriptdebug Ist fuer den Spieler die Option zum Debugging
	 * von (ScriptParser-)Scripten sichtbar gewesen?
	 * @param scriptdebugstatus Bei <code>true</code> wird das ScriptDebugging aktiviert
	 * @param defrelation Die Default-Beziehung zu anderen Spielern (1 = feindlich, 2 = freundlich, sonst neutral)
	 * @param personenNamenGenerator Der zu verwendende {@link PersonenNamenGenerator}
	 */
	@Action(ActionType.DEFAULT)
	public void changeXtraAction(int shipgroupmulti, int inttutorial, int scriptdebug, boolean scriptdebugstatus, User.Relation defrelation, PersonenNamenGenerator personenNamenGenerator)
	{
		User user = (User) getUser();
		TemplateEngine t = getTemplateEngine();

		String changemsg = "";

		if (shipgroupmulti != Integer.parseInt(user.getUserValue("TBLORDER/schiff/wrapfactor")))
		{
			changemsg += "Neuer Schiffsgruppenmultiplikator gespeichert...<br />\n";

			user.setUserValue("TBLORDER/schiff/wrapfactor", Integer.toString(shipgroupmulti));
		}

		if ((scriptdebug != 0) && hasPermission("schiff", "script"))
		{
			if (scriptdebugstatus != user.hasFlag(User.FLAG_SCRIPT_DEBUGGING))
			{
				user.setFlag(User.FLAG_SCRIPT_DEBUGGING, scriptdebugstatus);

				changemsg += "Scriptdebugging " + (scriptdebugstatus ? "" : "de") + "aktiviert<br />\n";
			}
		}

		if (inttutorial != Integer.parseInt(user.getUserValue("TBLORDER/uebersicht/inttutorial")))
		{
			if (inttutorial != 0)
			{
				changemsg += "Tutorial aktiviert...<br />\n";
			}
			else
			{
				changemsg += "Tutorial deaktiviert...<br />\n";
			}
			user.setUserValue("TBLORDER/uebersicht/inttutorial", Integer.toString(inttutorial));
		}

		if (defrelation != user.getRelation(0))
		{
			changemsg += "Diplomatiehaltung ge&auml;ndert...<br />\n";

			user.setRelation(0, defrelation);
			if (user.getAlly() != null)
			{
				for (User auser : user.getAlly().getMembers())
				{
					if (auser.getId() == user.getId())
					{
						continue;
					}
					user.setRelation(auser.getId(), User.Relation.FRIEND);
					auser.setRelation(user.getId(), User.Relation.FRIEND);
				}
			}
		}

		user.setPersonenNamenGenerator(personenNamenGenerator);

		t.setVar("options.message", changemsg);

		redirect("xtra");
	}

	/**
	 * Zeigt die erweiterten Einstellungen des Spielers.
	 */
	@Action(ActionType.DEFAULT)
	public void xtraAction()
	{
		User user = (User) getUser();
		TemplateEngine t = getTemplateEngine();

		t.setVar("options.xtra", 1,
				"user.wrapfactor", user.getUserValue("TBLORDER/schiff/wrapfactor"),
				"user.inttutorial", user.getUserValue("TBLORDER/uebersicht/inttutorial"),
				"user.showScriptDebug", hasPermission("schiff", "script"),
				"user.scriptdebug", user.hasFlag(User.FLAG_SCRIPT_DEBUGGING),
				"user.defrelation", user.getRelation(0).ordinal());

		t.setBlock("_OPTIONS", "personenNamenGenerator.listitem", "personenNamenGenerator.list");
		for(PersonenNamenGenerator png : PersonenNamenGenerator.values())
		{
			t.setVar("personenNamenGenerator.name", png.name(),
				"personenNamenGenerator.label", png.getLabel(),
				"personenNamenGenerator.selected", png == user.getPersonenNamenGenerator());
			t.parse("personenNamenGenerator.list", "personenNamenGenerator.listitem", true);
		}
	}

	private static final int MAX_UPLOAD_SIZE = 307200;

	/**
	 * Aendert das Logo des Spielers.
	 */
	@Action(ActionType.DEFAULT)
	public void logoAction()
	{
		TemplateEngine t = getTemplateEngine();

		List<FileItem> list = getContext().getRequest().getUploadedFiles();
		if (list.size() == 0)
		{
			redirect();
			return;
		}

		if (list.get(0).getSize() > MAX_UPLOAD_SIZE)
		{
			t.setVar("options.message", "Das Logo ist leider zu gro&szlig;. Bitte w&auml;hle eine Datei mit maximal 300kB Gr&ouml;&stlig;e<br />");
			redirect();
			return;
		}

		String uploaddir = Configuration.getSetting("ABSOLUTE_PATH") + "data/logos/user/";
		try
		{
			File uploadedFile = new File(uploaddir + getUser().getId() + ".gif");
			list.get(0).write(uploadedFile);
			t.setVar("options.message", "Das neue Logo wurde auf dem Server gespeichert<br />");
		}
		catch (Exception e)
		{
			t.setVar("options.message", "Offenbar ging beim Upload etwas schief (Ist die Datei evt. zu gro&szlig;?)<br />");
			log.warn(e);
		}
		redirect();
	}

	/**
	 * Aktiviert den Vac-Mode fuer den Spieler.
	 *
	 * @param vacdays Die Anzahl der Tage im Vacationmodus
	 */
	@Action(ActionType.DEFAULT)
	public void vacModeAction(int vacdays)
	{
		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();

		int vacTicks = Common.daysToTicks(vacdays);

		if (!user.checkVacationRequest(vacTicks))
		{
			t.setVar("options.message", "Dein Urlaubskonto reicht nicht aus f&uuml;r soviele Tage Urlaub.");
			redirect();
			return;
		}

		user.activateVacation(vacTicks);
		t.setVar("options.message", "Der Vorlauf f&uuml;r deinen Urlaub wurde gestartet.");
		redirect();
	}

	/**
	 * Speichert die neuen Optionen.
	 *
	 * @param showtooltip Falls != 0 werden die Hilfstooltips aktiviert
	 * @param wrapfactor Der neue Schiffsgruppierungsfaktor (0 = keine Gruppierung)
	 */
	@Action(ActionType.DEFAULT)
	public void saveOptionsAction(boolean showtooltip, int wrapfactor)
	{
		User user = (User) getUser();
		TemplateEngine t = getTemplateEngine();

		String changemsg = "";

		if (showtooltip == (Integer.parseInt(user.getUserValue("TBLORDER/schiff/tooltips")) == 0))
		{
			user.setUserValue("TBLORDER/schiff/tooltips", showtooltip ? "1" : "0");

			changemsg += "Anzeige der Tooltips " + (showtooltip ? "" : "de") + "aktiviert<br />\n";
		}

		if (wrapfactor != Integer.parseInt(user.getUserValue("TBLORDER/schiff/wrapfactor")))
		{
			user.setUserValue("TBLORDER/schiff/wrapfactor", Integer.toString(wrapfactor));

			changemsg += "Schiffsgruppierungen " + (wrapfactor != 0 ? "aktiviert" : "deaktiviert") + "<br />\n";
		}

		t.setVar("options.message", changemsg);

		redirect();
	}

	/**
	 * Deaktiviert den Noob-Schutz des Spielers.
	 */
	@Action(ActionType.DEFAULT)
	public void dropNoobProtectionAction()
	{
		User user = (User) getUser();
		TemplateEngine t = getTemplateEngine();

		if (user.isNoob())
		{
			user.setFlag(User.FLAG_NOOB, false);
			t.setVar("options.message", "GCP-Schutz wurde vorzeitig aufgehoben.<br />");
		}

		redirect();
	}

	/**
	 * Uebersicht ueber die Einstellungen.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction()
	{
		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();

		String imagepath = user.getUserImagePath();

		if (BasicUser.getDefaultImagePath().equals(imagepath))
		{
			imagepath = "";
		}

		t.setVar("options.general", 1,
				"user.wrapfactor", user.getUserValue("TBLORDER/schiff/wrapfactor"),
				"user.tooltip", user.getUserValue("TBLORDER/schiff/tooltips"),
				"user.imgpath", imagepath,
				"user.noob", user.isNoob(),
				"vacation.maxtime", Common.ticks2DaysInDays(user.maxVacTicks()));
	}
}
