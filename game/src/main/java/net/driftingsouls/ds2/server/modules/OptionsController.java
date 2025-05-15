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
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.RedirectViewResult;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.namegenerator.PersonenNamenGenerator;
import net.driftingsouls.ds2.server.namegenerator.SchiffsKlassenNamenGenerator;
import net.driftingsouls.ds2.server.namegenerator.SchiffsNamenGenerator;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.notification.Notifier;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Aendern der Einstellungen eines Benutzers durch den Benutzer selbst.
 *
 * @author Christopher Jung
 */
@Module(name = "options")
public class OptionsController extends Controller
{
	private static final Log log = LogFactory.getLog(OptionsController.class);
	private final TemplateViewResultFactory templateViewResultFactory;
	private final ConfigService configService;

	@Autowired
	public OptionsController(TemplateViewResultFactory templateViewResultFactory, ConfigService configService)
	{
		this.templateViewResultFactory = templateViewResultFactory;
		this.configService = configService;
	}

	/**
	 * Aendert den Namen und das Passwort des Benutzers.
	 *  @param name Der neue Benutzername
	 * @param pw Das neue Passwort
	 * @param pw2 Die Wiederholung des neuen Passworts
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine changeNamePassAction(String name, String pw, String pw2)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
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
				newname = newname.replace("[name]", name);
			}

			changemsg += "<span style=\"color:green\">Der Ingame-Name <span style=\"color:white\">" + Common._title(user.getNickname()) + "</span> wurde in <span style=\"color:white\">" + Common._title(name) + "</span> geändert</span><br />\n";

			Common.writeLog("login.log", Common.date("j.m.Y H:i:s") + ": <" + getContext().getRequest().getRemoteAddress() + "> (" + user.getId() + ") <" + user.getUN() + "> Namensänderung: Ingame-Name <" + user.getNickname() + "> in <" + name + "> Browser <" + getContext().getRequest().getUserAgent() + ">\n");

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
			changemsg += "<span style=\"color:red\">Das Passwort wurde geändert</span><br />\n";

			String subject = "Drifting Souls - Passwortänderung";
			String message = "Hallo {username},\n" +
					"Du hast Dein Password geändert. Dein Passwort wird kodiert gespeichert. Wenn es verloren geht, musst Du Dir über die \"neues Password zuteilen\"-Funktion der Login-Seite ein neues erstellen lassen.\n" +
					"Das Admin-Team wünscht viel Spaß mit Drifting Souls 2!\n" +
					"Gruß Guzman\n" +
					"Admin\n" +
					"{date} Serverzeit";
			message = message.replace("{username}", user.getUN());
			message = message.replace("{date}", Common.date("H:i j.m.Y"));

			Common.mail(user.getEmail(), subject, message);

			Common.writeLog("login.log", Common.date("j.m.Y H:i:s") + ": <" + getContext().getRequest().getRemoteAddress() + "> (" + user.getId() + ") <" + user.getUN() + "> Passwortaenderung Browser <" + getContext().getRequest().getUserAgent() + "> \n");
		}
		else if (pw.length() != 0)
		{
			changemsg += "<span style=\"color:red\">Die beiden eingegebenen Passwörter stimmen nicht überein</span><br />\n";
		}

		t.setVar("options.changenamepwd", 1,
				"options.changenamepwd.nickname", Common._plaintitle(user.getNickname()),
				"options.message", changemsg);

		return t;
	}

	/**
	 * Sendet die Löschanfrage des Spielers.
	 *  @param del Der Interaktionsschritt. Bei 0 wird das Eingabeformular angezeigt. Andernfalls wird versucht die Anfrage zu senden
	 * @param reason Die schluessige Begruendung. Muss mindestens die Laenge 5 haben
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine delAccountAction(int del, String reason)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();

		if (del == 0)
		{
			t.setVar("options.delaccountform", 1);

		}
		else if (reason.length() < 5)
		{
			t.setVar("options.message", "Bitte geben Sie Gründe für die Löschung an!<br />\n",
					"options.delaccountform", 1);

		}
		else
		{
			String msg = "PLZ DELETE ME!!!\nMY ID IS: [userprofile=" +
					user.getId() +
					"]" +
					user.getId() +
					"[/userprofile]\n" +
					"MY UN IS: " +
					user.getUN() +
					"\n" +
					"MY CURRENT NAME IS: " +
					user.getName() +
					"\n" +
					"MY REASONS:\n" +
					reason;
			PM.sendToAdmins(user, "Account löschen", msg, 0, getEM());

			t.setVar("options.delaccountresp", 1,
					"delaccountresp.admins", configService.getValue(WellKnownConfigValue.ADMIN_PMS_ACCOUNT));

		}

		return t;
	}

	/**
	 * Aendert die erweiterten Einstellungen des Spielers.
	 *  @param shipgroupmulti der neue Schiffsgruppierungswert
	 * @param inttutorial Die Seite des Tutorials in der Uebersicht (0 = deaktiviert)
	 * @param scriptdebug Ist fuer den Spieler die Option zum Debugging
 * von (ScriptParser-)Scripten sichtbar gewesen?
	 * @param scriptdebugstatus Bei <code>true</code> wird das ScriptDebugging aktiviert
	 * @param defrelation Die Default-Beziehung zu anderen Spielern (1 = feindlich, 2 = freundlich, sonst neutral)
	 * @param personenNamenGenerator Der zu verwendende {@link net.driftingsouls.ds2.server.namegenerator.PersonenNamenGenerator}
	 * @param schiffsKlassenNamenGenerator Der zu verwendende {@link net.driftingsouls.ds2.server.namegenerator.SchiffsKlassenNamenGenerator}
	 * @param schiffsNamenGenerator Der zu verwendende {@link net.driftingsouls.ds2.server.namegenerator.SchiffsNamenGenerator}
	 * @param apikey Der ApiKey fuer die Push-Benachrichtigungen
	 * @param auktion_pm Angabe, ob fuer neue Auktionen PMs verschickt werden sollen
	 * @param handel_pm Angabe, ob fuer neue Handelsinserate PMs verschickt werden sollen
	 * @param sounds_mute Einstellungen, ob Sounds gemutet werden sollen
	 * @param sounds_volume Lautstaerke fuer die Sounds
	 * @param handelsposten_pm Angabe, ob fuer Transaktionen am HP PMs verschickt werden sollen
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult changeXtraAction(int shipgroupmulti, int inttutorial, int scriptdebug, boolean scriptdebugstatus, boolean battle_pm, boolean research_pm, boolean ship_build_pm, boolean base_down_pm, boolean officer_build_pm, boolean unit_build_pm, User.Relation defrelation,
											   PersonenNamenGenerator personenNamenGenerator,
											   SchiffsKlassenNamenGenerator schiffsKlassenNamenGenerator,
											   SchiffsNamenGenerator schiffsNamenGenerator,
											   String apikey,
											   boolean auktion_pm,
												 boolean handel_pm,
												 boolean sounds_mute,
												 int sounds_volume,
												 boolean handelsposten_pm)
	{
		User user = (User) getUser();

		String changemsg = "";

		if (shipgroupmulti != user.getUserValue(WellKnownUserValue.TBLORDER_SCHIFF_WRAPFACTOR))
		{
			changemsg += "Neuer Schiffsgruppenmultiplikator gespeichert...<br />\n";

			user.setUserValue(WellKnownUserValue.TBLORDER_SCHIFF_WRAPFACTOR, shipgroupmulti);
		}

		if ((scriptdebug != 0) && hasPermission(WellKnownPermission.SCHIFF_SCRIPT))
		{
			if (scriptdebugstatus != user.hasFlag(UserFlag.SCRIPT_DEBUGGING))
			{
				user.setFlag(UserFlag.SCRIPT_DEBUGGING, scriptdebugstatus);

				changemsg += "Scriptdebugging " + (scriptdebugstatus ? "" : "de") + "aktiviert<br />\n";
			}
		}

		if (inttutorial != user.getUserValue(WellKnownUserValue.TBLORDER_UEBERSICHT_INTTUTORIAL))
		{
			if (inttutorial != 0)
			{
				changemsg += "Tutorial aktiviert...<br />\n";
			}
			else
			{
				changemsg += "Tutorial deaktiviert...<br />\n";
			}
			user.setUserValue(WellKnownUserValue.TBLORDER_UEBERSICHT_INTTUTORIAL, inttutorial);
		}

		if (defrelation != user.getRelation(null))
		{
			changemsg += "Diplomatieeinstellung geändert...<br />\n";

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
		if (!apikey.equals(user.getApiKey()))
		{
			if (apikey.length()==25||apikey.length()==0)//Die ApiKeys sind alle 25 Zeichen lang
			{
				user.setUserValue(WellKnownUserValue.APIKEY, apikey);
					new Notifier(apikey).sendMessage("'Drifting Souls 2'-Push-Benachrichtigungen", user.getPlainname()+", Du hast die Push-Benachrichtigungen erfolgreich aktiviert...");
			}
			else
			{
				changemsg += "Ungültigen API Key eingegeben... zum Entfernen das Feld leeren<br />\\n";
			}
		}

		user.setPersonenNamenGenerator(personenNamenGenerator);
		user.setSchiffsKlassenNamenGenerator(schiffsKlassenNamenGenerator);
		user.setSchiffsNamenGenerator(schiffsNamenGenerator);
        user.setUserValue(WellKnownUserValue.GAMEPLAY_USER_BATTLE_PM, battle_pm);
        user.setUserValue(WellKnownUserValue.GAMEPLAY_USER_RESEARCH_PM, research_pm);
        user.setUserValue(WellKnownUserValue.GAMEPLAY_USER_SHIP_BUILD_PM, ship_build_pm);
        user.setUserValue(WellKnownUserValue.GAMEPLAY_USER_BASE_DOWN_PM, base_down_pm);
        user.setUserValue(WellKnownUserValue.GAMEPLAY_USER_OFFICER_BUILD_PM, officer_build_pm);
        user.setUserValue(WellKnownUserValue.GAMEPLAY_USER_UNIT_BUILD_PM, unit_build_pm);
        user.setUserValue(WellKnownUserValue.GAMEPLAY_USER_AUKTION_PM,auktion_pm);
				user.setUserValue(WellKnownUserValue.GAMEPLAY_USER_HANDEL_PM,handel_pm);
				user.setUserValue(WellKnownUserValue.GAMEPLAY_USER_SOUNDS_MUTE,sounds_mute);
				user.setUserValue(WellKnownUserValue.GAMEPLAY_USER_SOUNDS_VOLUME,sounds_volume);
				user.setUserValue(WellKnownUserValue.GAMEPLAY_USER_HANDELSPOSTEN_PM,handelsposten_pm);

		return new RedirectViewResult("xtra").withMessage(changemsg);
	}

	@ViewModel
	public static class GenerierePersonenNamenBeispieleViewModel
	{
		public final List<String> namen = new ArrayList<>();
	}

	@Action(ActionType.AJAX)
	public GenerierePersonenNamenBeispieleViewModel generierePersonenNamenBeispiele(PersonenNamenGenerator generator)
	{
		GenerierePersonenNamenBeispieleViewModel result = new GenerierePersonenNamenBeispieleViewModel();
		if (generator == null)
		{
			return result;
		}

		for (int i = 0; i < 5; i++)
		{
			result.namen.add(generator.generiere());
		}
		return result;
	}

	public static class SchiffsKlasseNameBeispiel {
		private String klasse;
		private String name;

		public SchiffsKlasseNameBeispiel(String klasse, String name)
		{
			this.klasse = klasse;
			this.name = name;
		}

		public String getKlasse()
		{
			return klasse;
		}

		public void setKlasse(String klasse)
		{
			this.klasse = klasse;
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}
	}

	@ViewModel
	public static class GeneriereSchiffsNamenBeispieleViewModel
	{
		final List<SchiffsKlasseNameBeispiel> namen = new ArrayList<>();
	}

	@Action(ActionType.AJAX)
	public GeneriereSchiffsNamenBeispieleViewModel generiereSchiffsNamenBeispiele(
			SchiffsKlassenNamenGenerator schiffsKlassenNamenGenerator,
			SchiffsNamenGenerator schiffsNamenGenerator)
	{
		GeneriereSchiffsNamenBeispieleViewModel result = new GeneriereSchiffsNamenBeispieleViewModel();
		if (schiffsKlassenNamenGenerator == null || schiffsNamenGenerator == null)
		{
			return result;
		}

		org.hibernate.Session db = getDB();
		for (ShipClasses cls : ShipClasses.values())
		{
			ShipTypeData std = (ShipTypeData)db.createQuery("from ShipType where hide=false and shipClass=:cls")
				.setParameter("cls", cls)
				.setMaxResults(1)
				.uniqueResult();

			if( std == null )
			{
				continue;
			}

			String name = (schiffsKlassenNamenGenerator.generiere(cls) + " " + schiffsNamenGenerator.generiere(std)).trim();
			if( name.isEmpty() ) {
				continue;
			}
			result.namen.add(new SchiffsKlasseNameBeispiel(cls.getSingular(), name));
		}

		return result;
	}

	/**
	 * Zeigt die erweiterten Einstellungen des Spielers.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine xtraAction(RedirectViewResult redirect)
	{
		User user = (User) getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);

		t.setVar("options.message", redirect != null ? redirect.getMessage() : null);

		t.setVar("options.xtra", 1,
				"user.wrapfactor", user.getUserValue(WellKnownUserValue.TBLORDER_SCHIFF_WRAPFACTOR),
				"user.inttutorial", user.getUserValue(WellKnownUserValue.TBLORDER_UEBERSICHT_INTTUTORIAL),
				"user.showScriptDebug", hasPermission(WellKnownPermission.SCHIFF_SCRIPT),
				"user.scriptdebug", user.hasFlag(UserFlag.SCRIPT_DEBUGGING),
				"user.defrelation", user.getRelation(null).ordinal(),
                "user.battlepm", user.getUserValue(WellKnownUserValue.GAMEPLAY_USER_BATTLE_PM),
                "user.researchpm", user.getUserValue(WellKnownUserValue.GAMEPLAY_USER_RESEARCH_PM),
                "user.shipbuildpm", user.getUserValue(WellKnownUserValue.GAMEPLAY_USER_SHIP_BUILD_PM),
                "user.basedownpm", user.getUserValue(WellKnownUserValue.GAMEPLAY_USER_BASE_DOWN_PM),
                "user.officerbuildpm", user.getUserValue(WellKnownUserValue.GAMEPLAY_USER_OFFICER_BUILD_PM),
                "user.unitbuildpm", user.getUserValue(WellKnownUserValue.GAMEPLAY_USER_UNIT_BUILD_PM),
				"user.apikey", user.getUserValue(WellKnownUserValue.APIKEY),
				"user.handelpm", user.getUserValue(WellKnownUserValue.GAMEPLAY_USER_HANDEL_PM),
				"user.auktionpm", user.getUserValue(WellKnownUserValue.GAMEPLAY_USER_AUKTION_PM),
				"user.soundsmute", user.getUserValue(WellKnownUserValue.GAMEPLAY_USER_SOUNDS_MUTE),
				"user.soundsvolume", user.getUserValue(WellKnownUserValue.GAMEPLAY_USER_SOUNDS_VOLUME),
				"user.handelspostenpm", user.getUserValue(WellKnownUserValue.GAMEPLAY_USER_HANDELSPOSTEN_PM));

		t.setBlock("_OPTIONS", "personenNamenGenerator.listitem", "personenNamenGenerator.list");
		for (PersonenNamenGenerator png : PersonenNamenGenerator.values())
		{
			t.setVar("personenNamenGenerator.name", png.name(),
					"personenNamenGenerator.label", png.getLabel(),
					"personenNamenGenerator.selected", png == user.getPersonenNamenGenerator());
			t.parse("personenNamenGenerator.list", "personenNamenGenerator.listitem", true);
		}

		t.setBlock("_OPTIONS", "schiffsKlassenNamenGenerator.listitem", "schiffsKlassenNamenGenerator.list");
		for (SchiffsKlassenNamenGenerator skng : SchiffsKlassenNamenGenerator.values())
		{
			t.setVar("schiffsKlassenNamenGenerator.name", skng.name(),
					"schiffsKlassenNamenGenerator.label", skng.getLabel(),
					"schiffsKlassenNamenGenerator.selected", skng == user.getSchiffsKlassenNamenGenerator());
			t.parse("schiffsKlassenNamenGenerator.list", "schiffsKlassenNamenGenerator.listitem", true);
		}

		t.setBlock("_OPTIONS", "schiffsNamenGenerator.listitem", "schiffsNamenGenerator.list");
		for (SchiffsNamenGenerator skng : SchiffsNamenGenerator.values())
		{
			t.setVar("schiffsNamenGenerator.name", skng.name(),
					"schiffsNamenGenerator.label", skng.getLabel(),
					"schiffsNamenGenerator.selected", skng == user.getSchiffsNamenGenerator());
			t.parse("schiffsNamenGenerator.list", "schiffsNamenGenerator.listitem", true);
		}

		return t;
	}

	private static final int MAX_UPLOAD_SIZE = 307200;

	/**
	 * Aendert das Logo des Spielers.
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult logoAction()
	{
		List<FileItem> list = getContext().getRequest().getUploadedFiles();
		if (list.size() == 0)
		{
			return new RedirectViewResult("default");
		}

		if (list.get(0).getSize() > MAX_UPLOAD_SIZE)
		{
			return new RedirectViewResult("default").withMessage("Das Logo ist leider zu groß. Bitte wähle eine Datei mit einer Größe von maximal 300 kB.<br />");
		}

		String message;
		String uploaddir = Configuration.getAbsolutePath() + "data/logos/user/";
		try
		{
			File uploadedFile = new File(uploaddir + getUser().getId() + ".gif");
			list.get(0).write(uploadedFile);
			message = "Das neue Logo wurde auf dem Server gespeichert<br />";
		}
		catch (Exception e)
		{
			message = "Offenbar ging beim Upload etwas schief (ist die Datei evtl. zu groß?)<br />";
			log.warn(e);
		}
		return new RedirectViewResult("default").withMessage(message);
	}

	/**
	 * Aktiviert den Vac-Mode fuer den Spieler.
	 *
	 * @param vacdays Die Anzahl der Tage im Vacationmodus
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult vacModeAction(int vacdays)
	{
		User user = (User) getUser();

		int vacTicks = Common.daysToTicks(vacdays);

		if (!user.checkVacationRequest(vacTicks))
		{
			return new RedirectViewResult("default").withMessage("Dein Urlaubskonto reicht nicht aus für soviele Tage Urlaub.");
		}

		user.activateVacation(vacTicks);
		return new RedirectViewResult("default").withMessage("Der Vorlauf für deinen Urlaub wurde gestartet.");
	}

	/**
	 * Speichert die neuen Optionen.
	 *  @param showtooltip Falls != 0 werden die Hilfstooltips aktiviert
	 * @param wrapfactor Der neue Schiffsgruppierungsfaktor (0 = keine Gruppierung)
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult saveOptionsAction(boolean showtooltip, int wrapfactor)
	{
		User user = (User) getUser();

		String changemsg = "";

		if (showtooltip == (user.getUserValue(WellKnownUserValue.TBLORDER_SCHIFF_TOOLTIPS) == 0))
		{
			user.setUserValue(WellKnownUserValue.TBLORDER_SCHIFF_TOOLTIPS, showtooltip ? 1 : 0);

			changemsg += "Anzeige der Tooltips " + (showtooltip ? "" : "de") + "aktiviert<br />\n";
		}

		if (wrapfactor != user.getUserValue(WellKnownUserValue.TBLORDER_SCHIFF_WRAPFACTOR))
		{
			user.setUserValue(WellKnownUserValue.TBLORDER_SCHIFF_WRAPFACTOR, wrapfactor);

			changemsg += "Schiffsgruppierungen " + (wrapfactor != 0 ? "aktiviert" : "deaktiviert") + "<br />\n";
		}

		return new RedirectViewResult("default").withMessage(changemsg);
	}

	/**
	 * Deaktiviert den Noob-Schutz des Spielers.
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult dropNoobProtectionAction()
	{
		User user = (User) getUser();

		String message = null;
		if (user.isNoob())
		{
			user.setFlag(UserFlag.NOOB, false);
			message = "Der Neuspielerschutz (GCP-Schutz) wurde vorzeitig aufgehoben.<br />";
		}

		return new RedirectViewResult("default").withMessage(message);
	}

	/**
	 * Uebersicht ueber die Einstellungen.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(RedirectViewResult redirect)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();

		t.setVar("options.message", redirect != null ? redirect.getMessage() : null);
		t.setVar("options.general", 1,
				"user.wrapfactor", user.getUserValue(WellKnownUserValue.TBLORDER_SCHIFF_WRAPFACTOR),
				"user.tooltip", user.getUserValue(WellKnownUserValue.TBLORDER_SCHIFF_TOOLTIPS),
				"user.noob", user.isNoob(),
				"vacation.maxtime", Common.ticks2DaysInDays(user.maxVacTicks()));

		return t;
	}
}
