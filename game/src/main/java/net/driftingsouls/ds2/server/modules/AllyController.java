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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.ComNetChannel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.ally.AllyPosten;
import net.driftingsouls.ds2.server.entities.ally.AllyRangDescriptor;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.*;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.services.AllianzService;
import net.driftingsouls.ds2.server.services.AllyPostenService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Zeigt die Allianzseite an.
 *
 * @author Christopher Jung
 */
@Module(name = "ally")
public class AllyController extends Controller
{
	private static final Log log = LogFactory.getLog(AllyController.class);

	private AllianzService allianzService;
	private TemplateViewResultFactory templateViewResultFactory;
	private AllyPostenService allyPostenService;
	private Ally ally = null;

	@Autowired
	public AllyController(AllianzService allianzService,
			TemplateViewResultFactory templateViewResultFactory,
			AllyPostenService allyPostenService)
	{
		this.allianzService = allianzService;
		this.templateViewResultFactory = templateViewResultFactory;
		this.allyPostenService = allyPostenService;

		setPageTitle("Allianz");
	}

	@Override
	protected boolean validateAndPrepare()
	{
		User user = (User) getUser();

		this.ally = user.getAlly();

		if (this.ally != null)
		{
			addPageMenuEntry("Allgemeines", Common.buildUrl("default"));
			addPageMenuEntry("Mitglieder", Common.buildUrl("showMembers"));
			if (user.getId() == this.ally.getPresident().getId())
			{
				addPageMenuEntry("Einstellungen", Common.buildUrl("showAllySettings"));
				addPageMenuEntry("Posten", Common.buildUrl("showPosten"));
			}
			addPageMenuEntry("Kaempfe", Common.buildUrl("showBattles"));
			addPageMenuEntry("Allianzen auflisten", Common.buildUrl("default", "module", "allylist"));
			addPageMenuEntry("Austreten", Common.buildUrl("part"));
		}
		else
		{
			addPageMenuEntry("Allianz beitreten", Common.buildUrl("defaultNoAlly"));
			addPageMenuEntry("Allianz gruenden", Common.buildUrl("showCreateAlly"));
			addPageMenuEntry("Allianzen auflisten", Common.buildUrl("default", "module", "allylist"));
		}

		return true;
	}

	private void setDefaultTemplateVars(TemplateEngine t)
	{
		User user = (User) getUser();
		t.setVar("ally", user.getAlly() != null ? user.getAlly().getId() : 0);
		if (this.ally != null)
		{
			t.setVar(
					"ally.name", Common._title(this.ally.getName()),
					"user.president", (user.getId() == this.ally.getPresident().getId()),
					"user.president.npc", (user.getId() == this.ally.getPresident().getId() && user.isNPC()),
					"ally.id", this.ally.getId());
		}
		else {
			t.setVar(
					"ally.name", null,
					"user.president", false,
					"user.president.npc", false,
					"ally.id", 0);
		}
	}

	/**
	 * Leitet die Gruendung einer Allianz ein. Die Aktion erstellt
	 * die notwendigen Tasks und benachrichtigt die Unterstuetzer der Gruendung.
	 *  @param name Der Name der neuen Allianz
	 * @param confuser1 Die User-ID des ersten Unterstuetzers
	 * @param confuser2 Die User-ID des zweiten Unterstuetzers
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult foundAction(String name, User confuser1, User confuser2, String show)
	{
		User user = (User) getUser();

		if (user.getAlly() != null)
		{
			return new RedirectViewResult("default").withMessage("Fehler: Sie sind bereits Mitglied in einer Allianz und können daher keine neue Allianz gründen");
		}

		Taskmanager taskmanager = Taskmanager.getInstance();

		Task[] tasks = taskmanager.getTasksByData(Taskmanager.Types.ALLY_NEW_MEMBER, "*", Integer.toString(user.getId()), "*");
		if (tasks.length > 0)
		{
			return new RedirectViewResult("defaultNoAlly").withMessage("Fehler: Sie haben bereits einen Aufnahmeantrag bei einer Allianz gestellt");
		}

		if (confuser1 == confuser2)
		{
			return new RedirectViewResult("showCreateAlly").withMessage("<span style=\"color:red\">Einer der angegebenen Unterstützer ist ungültig</span>\n");
		}

		if ((confuser1 == null) || (confuser1.getAlly() != null) ||
				(confuser2 == null) || (confuser2.getAlly() != null))
		{
			return new RedirectViewResult("showCreateAlly").withMessage("<span style=\"color:red\">Einer der angegebenen Unterstützer ist ungültig</span>\n");
		}

		if (allianzService.isUserAnAllianzgruendungBeteiligt(confuser1))
		{
			confuser1 = null;
		}
		else
		{
			tasks = taskmanager.getTasksByData(Taskmanager.Types.ALLY_NEW_MEMBER, "*",
					Integer.toString(confuser1.getId()), "*");
			if (tasks.length > 0)
			{
				confuser1 = null;
			}
		}

		if (allianzService.isUserAnAllianzgruendungBeteiligt(confuser2))
		{
			confuser2 = null;
		}
		else
		{
			tasks = taskmanager.getTasksByData(Taskmanager.Types.ALLY_NEW_MEMBER, "*",
					Integer.toString(confuser2.getId()), "*");
			if (tasks.length > 0)
			{
				confuser2 = null;
			}
		}

		if ((confuser1 == null) || (confuser2 == null))
		{
			return new RedirectViewResult("showCreateAlly").withMessage("<span style=\"color:red\">Einer der angegebenen Unterstützer ist versucht bereits in einer anderen Allianz Mitglied zu werden</span>\n");
		}

		String mastertaskid = taskmanager.addTask(
				Taskmanager.Types.ALLY_FOUND, 21,
				"2", name, user.getId() + "," + confuser1.getId() + "," + confuser2.getId());
		String conf1taskid = taskmanager.addTask(
				Taskmanager.Types.ALLY_FOUND_CONFIRM, 21,
				mastertaskid, Integer.toString(confuser1.getId()), "");
		String conf2taskid = taskmanager.addTask(
				Taskmanager.Types.ALLY_FOUND_CONFIRM, 21,
				mastertaskid, Integer.toString(confuser2.getId()), "");

		PM.send(user, confuser1.getId(), "Allianzgr&uuml;ndung", "[automatische Nachricht]\nIch habe vor die Allianz " + name + " zu gr&uuml;nden. Da zwei Spieler dieses vorhaben unterst&uuml;tzen m&uuml;ssen habe ich mich an dich gewendet.\nAchtung: Durch die Unterst&uuml;tzung wirst du automatisch Mitglied!\n\n[_intrnlConfTask=" + conf1taskid + "]Willst du die Allianzgr&uuml;ndung unterst&uuml;tzen?[/_intrnlConfTask]", PM.FLAGS_IMPORTANT);
		PM.send(user, confuser2.getId(), "Allianzgr&uuml;ndung", "[automatische Nachricht]\nIch habe vor die Allianz " + name + " zu gr&uuml;nden. Da zwei Spieler dieses vorhaben unterst&uuml;tzen m&uuml;ssen habe ich mich an dich gewendet.\nAchtung: Durch die Unterst&uuml;tzung wirst du automatisch Mitglied!\n\n[_intrnlConfTask=" + conf2taskid + "]Willst du die Allianzgr&uuml;ndung unterst&uuml;tzen?[/_intrnlConfTask]", PM.FLAGS_IMPORTANT);

		return new RedirectViewResult("defaultNoAlly").withMessage("Die beiden angegebenen Spieler wurden via PM benachrichtigt. Sollten sich beide zur Unterst&uuml;tzung entschlossen haben, wird die Allianz augenblicklich gegr&uuml;ndet. Du wirst au&szlig;erdem via PM benachrichtigt.");
	}

	/**
	 * Leitet den Beitritt zu einer Allianz ein.
	 * Die Aktion erstellt die notwendige Task und benachrichtigt
	 * die "Minister" und den Praesident der Zielallianz.
	 *  @param conf Bestaetigt den Aufnahmewunsch falls der Wert "ok" ist
	 * @param zielAllianz Die ID der Allianz, der der Benuzter beitreten moechte
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public Object joinAction(String conf, @UrlParam(name = "join") Ally zielAllianz, String show)
	{
		User user = (User) getUser();

		if (user.getAlly() != null)
		{
			return new RedirectViewResult("defaultNoAlly").withMessage("Sie sind bereits in einer Allianz. Sie müssen diese erst verlassen um in eine andere Allianz eintreten zu können!");
		}

		if (zielAllianz == null)
		{
			return new RedirectViewResult("defaultNoAlly").withMessage("Die angegebene Allianz existiert nicht");
		}

		if (allianzService.isUserAnAllianzgruendungBeteiligt(user))
		{
			return new RedirectViewResult("defaultNoAlly").withMessage("Es gibt eine oder mehrere Anfragen an sie zwecks Unterstützung einer Allianzgründung. Sie m&uuml;ssen diese Anfragen erst bearbeiten bevor sie einer Allianz beitreten können.");
		}

		Session db = getDB();
		long battlesAgainstAlly = (Long) db.createQuery("select count(battle) from Battle battle where (commander1=:user and commander2.ally=:ally) or (commander1.ally=:ally and commander2=:user)")
				.setParameter("user", user)
				.setParameter("ally", ally)
				.uniqueResult();
		if (battlesAgainstAlly > 0)
		{
			return new RedirectViewResult("defaultNoAlly").withMessage("Sie können keiner Allianz beitreten gegen die Sie kämpfen.");
		}

		Taskmanager taskmanager = Taskmanager.getInstance();

		Task[] tasks = taskmanager.getTasksByData(Taskmanager.Types.ALLY_NEW_MEMBER, "*", Integer.toString(user.getId()), "*");
		if (tasks.length > 0)
		{
			return new RedirectViewResult("defaultNoAlly").withMessage("Fehler: Sie haben bereits einen Aufnahmeantrag bei einer Allianz gestellt");
		}

		if (!conf.equals("ok"))
		{
			TemplateEngine t = templateViewResultFactory.createFor(this);
			setDefaultTemplateVars(t);
			t.setVar("show", show);
			t.setVar("ally.statusmessage", "Wollen sie der Allianz &gt;" + Common._title(zielAllianz.getName()) + "&lt; wirklich beitreten?",
					"ally.statusmessage.ask.url1", "&amp;action=join&amp;join=" + zielAllianz.getId() + "&amp;conf=ok",
					"ally.statusmessage.ask.url2", "");

			return t;
		}

		String taskid = taskmanager.addTask(Taskmanager.Types.ALLY_NEW_MEMBER, 35, Integer.toString(zielAllianz.getId()), Integer.toString(user.getId()), "");

		List<User> supermembers = allianzService.getAllianzfuehrung(zielAllianz);
		for (User supermember : supermembers)
		{
			PM.send(user, supermember.getId(),
					"Aufnahmeantrag", "[Automatische Nachricht]\nHiermit beantrage ich die Aufnahme in die Allianz.\n\n[_intrnlConfTask=" + taskid + "]Wollen sie dem Aufnahmeantrag zustimmen?[/_intrnlConfTask]", PM.FLAGS_IMPORTANT);
		}

		return new RedirectViewResult("defaultNoAlly").withMessage("Der Aufnahmeantrag wurde weitergeleitet. Die Bearbeitung kann jedoch abhängig von der Allianz längere Zeit in anspruch nehmen. Sollten sie aufgenommen werden, wird automatisch eine PM an sie gesendet.");
	}

	/**
	 * Loescht einen Rangn.
	 *  @param rangnr Die ID des zu loeschenden Rangs
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult deleteRangAction(int rangnr, String show)
	{
		User user = (User) getUser();

		if (this.ally.getPresident().getId() != user.getId() || !user.isNPC())
		{
			return new RedirectViewResult("showPosten").withMessage("Fehler: Nur der Präsident einer NPC-Allianz kann diese Aktion durchführen");
		}

		AllyRangDescriptor rang = findeAllianzRangMitNummer(rangnr);

		String message = null;
		if (rang != null)
		{
			if (rang.getCustomImg() != null)
			{
				DynamicContentManager.remove(rang.getCustomImg());
			}
			getDB().delete(rang);
			this.ally.getRangDescriptors().remove(rang);

			message = "Rang gelöscht";
		}

		return new RedirectViewResult("showRaenge").withMessage(message);
	}

	private AllyRangDescriptor findeAllianzRangMitNummer(int rangnr)
	{
		AllyRangDescriptor rang = null;
		for (AllyRangDescriptor desc : this.ally.getRangDescriptors())
		{
			if (desc.getRang() == rangnr)
			{
				rang = desc;
				break;
			}
		}
		return rang;
	}

	/**
	 * Erstellt einen neuen Rang oder modifiziert einen bereits vorhandenen (bei Gleichheit der Rangnummer).
	 *  @param rangname Der Anzeigename des Rangs
	 * @param rangnr Die Rangnummer
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult addOrEditRangAction(String rangname, int rangnr, String show)
	{
		User user = (User) getUser();

		if (this.ally.getPresident().getId() != user.getId() || !user.isNPC())
		{
			return new RedirectViewResult("showPosten").withMessage("Fehler: Nur der Präsident einer NPC-Allianz kann diese Aktion durchführen");
		}

		if (rangname.length() == 0 || rangnr < 0)
		{
			return new RedirectViewResult("showPosten").withMessage("Fehler: Sie müssen gültige Angaben machen.");
		}

		AllyRangDescriptor rang = findeAllianzRangMitNummer(rangnr);

		if (rang == null)
		{
			rang = new AllyRangDescriptor(this.ally, rangnr, rangname);
			getDB().persist(rang);
			this.ally.getRangDescriptors().add(rang);
		}
		else
		{
			rang.setName(rangname);
		}

		List<FileItem> list = getContext().getRequest().getUploadedFiles();
		if (!list.isEmpty() && list.get(0).getSize() > 0)
		{
			String oldImg = rang.getCustomImg();
			try
			{
				rang.setCustomImg(DynamicContentManager.add(list.get(0)));

				if (oldImg != null)
				{
					DynamicContentManager.remove(oldImg);
				}
			}
			catch (Exception e)
			{
				log.warn(e);
			}
		}

		return new RedirectViewResult("showRaenge").withMessage("Der Rang " + rangname + " wurde erstellt und zugewiesen");
	}

	/**
	 * Loescht einen Allianz-Posten.
	 *  @param posten Die ID des zu loeschenden Postens
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult deletePostenAction(@UrlParam(name = "postenid") AllyPosten posten, String show)
	{
		User user = (User) getUser();


		if (this.ally.getPresident().getId() != user.getId())
		{
			return new RedirectViewResult("showPosten").withMessage("Fehler: Nur der Präsident der Allianz kann diese Aktion durchführen");
		}

		if (posten == null || posten.getAlly() != this.ally)
		{
			return new RedirectViewResult("showPosten").withMessage("Fehler: Der angegebene Posten ist ungueltig");
		}

		allyPostenService.loesche(posten);

		return new RedirectViewResult("showPosten").withMessage("Posten gelöscht");
	}

	/**
	 * Weisst einem Posten einen neuen Users zu.
	 *  @param formuser Die ID des neuen Inhabers des Postens
	 * @param posten Die ID des zu besetzenden Postens
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult editPostenAction(@UrlParam(name = "user") User formuser, @UrlParam(name = "id") AllyPosten posten, String show)
	{
		User user = (User) getUser();

		if (this.ally.getPresident().getId() != user.getId())
		{
			return new RedirectViewResult("showPosten").withMessage("Fehler: Nur der Präsident der Allianz kann diese Aktion durchführen");
		}

		if (formuser == null || formuser.getAlly() == null || formuser.getAlly() != this.ally)
		{
			return new RedirectViewResult("showPosten").withMessage("Fehler: Sie müssen den Posten jemandem zuweisen");
		}

		if (formuser.getAllyPosten() != null)
		{
			return new RedirectViewResult("showPosten").withMessage("Fehler: Jedem Mitglied darf maximal ein Posten zugewiesen werden");
		}

		if (posten == null || posten.getAlly() != this.ally)
		{
			return new RedirectViewResult("showPosten").withMessage("Fehler: Der angegebene Posten ist ungültig");
		}

		if (posten.getUser() != null)
		{
			posten.getUser().setAllyPosten(null);
		}
		formuser.setAllyPosten(posten);

		return new RedirectViewResult("showPosten").withMessage("Änderungen gespeichert");
	}

	/**
	 * Erstellt einen neuen Posten.
	 *  @param name Der Name des neuen Postens
	 * @param formuser Die ID des Benutzers, der den Posten innehaben soll
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult addPostenAction(String name, @UrlParam(name = "user") User formuser, String show)
	{
		User user = (User) getUser();

		if (this.ally.getPresident().getId() != user.getId())
		{
			return new RedirectViewResult("showPosten").withMessage("Fehler: Nur der Präsident der Allianz kann diese Aktion durchführen");
		}

		if (name.length() == 0)
		{
			return new RedirectViewResult("showPosten").withMessage("Fehler: Sie müssen dem Posten einen Namen geben");
		}

		if (formuser.getAllyPosten() != null)
		{
			return new RedirectViewResult("showPosten").withMessage("Fehler: Jedem Mitglied darf maximal ein Posten zugewiesen werden");
		}

		int postencount = allyPostenService.getAnzahlPostenDerAllianz(ally);
		int maxposten = allyPostenService.getMaxPostenDerAllianz(ally);

		if (maxposten <= postencount)
		{
			return new RedirectViewResult("showPosten").withMessage("Fehler: Sie haben bereits die maximale Anzahl an Posten erreicht");
		}

		AllyPosten posten = allyPostenService.erstelle(this.ally, name);
		formuser.setAllyPosten(posten);

		return new RedirectViewResult("showPosten").withMessage("Der Posten " + Common._plaintitle(name) + " wurde erstellt und zugewiesen");
	}

	/**
	 * Erstellt fuer die Allianz einen neuen Comnet-Kanal.
	 *  @param name Der Name des neuen Kanals
	 * @param read Der Zugriffsmodus (all, ally, player)
	 * @param readids Falls der Lesemodus/Schreibmodus player ist: Die Komma-separierte Liste der Spieler-IDs
	 * @param write Der Zugriffsmodus fuer Schreibrechte (all, ally, player)
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult createChannelAction(String name, String read, String readids, String write, String writeids, String show)
	{
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		if (this.ally.getPresident().getId() != user.getId())
		{
			return new RedirectViewResult("showAllySettings").withMessage("Fehler: Nur der Pr&auml;sident der Allianz kann diese Aktion durchführen");
		}

		int count = ((Number) db.createQuery("select count(*) from ComNetChannel where allyOwner=:owner")
				.setInteger("owner", this.ally.getId())
				.iterate().next()).intValue();

		if (count >= 2)
		{
			return new RedirectViewResult("showAllySettings").withMessage("Fehler: Ihre Allianz besitzt bereits zwei Frequenzen");
		}

		if (name.length() == 0)
		{
			return new RedirectViewResult("showAllySettings").withMessage("Fehler: Sie haben keinen Namen für die Frequenz eingegeben");
		}

		ComNetChannel channel = new ComNetChannel(name);
		channel.setAllyOwner(this.ally);

		switch (read)
		{
			case "all":
				channel.setReadAll(true);
				break;
			case "ally":
				channel.setReadAlly(this.ally);
				break;
			case "player":
				channel.setReadPlayer(Common.explodeToInt(",", readids));
				break;
		}

		switch (write)
		{
			case "all":
				channel.setWriteAll(true);
				break;
			case "ally":
				channel.setWriteAlly(this.ally);
				break;
			case "player":
				channel.setWritePlayer(Common.explodeToInt(",", writeids));
				break;
		}
		db.persist(channel);

		return new RedirectViewResult("showAllySettings").withMessage("Frequenz " + Common._title(name) + " hinzugefügt");
	}

	/**
	 * Setzt Namen und Zugriffrechte fuer einen Allianz-Comnet-Kanal.
	 *  @param channel Die ID des Comnet-Kanals
	 * @param name Der neue Name
	 * @param read Der Zugriffsmodus (all, ally, player)
	 * @param write Der Zugriffsmodus fuer Schreibrechte (all, ally, player)
	 * @param readids Falls der Lesemodus/Screibmodus player ist: Die Komma-separierte Liste der Spieler-IDs
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult editChannelAction(@UrlParam(name = "edit") ComNetChannel channel, String name, String read, String write, String readids, String writeids, String show)
	{
		User user = (User) getUser();

		if (this.ally.getPresident().getId() != user.getId())
		{
			return new RedirectViewResult("showAllySettings").withMessage("Fehler: Nur der Präsident der Allianz kann diese Aktion durchführen");
		}

		if ((channel == null) || (channel.getAllyOwner() != this.ally))
		{
			return new RedirectViewResult("showAllySettings").withMessage("Fehler: Diese Frequenz gehört nicht ihrer Allianz");
		}

		if (name.length() == 0)
		{
			return new RedirectViewResult("showAllySettings").withMessage("Fehler: Sie haben keinen Namen für die Frequenz eingegeben");
		}

		channel.setName(name);
		channel.setReadAll(false);
		channel.setReadAlly(null);
		channel.setReadPlayer(new int[0]);
		channel.setWriteAll(false);
		channel.setWriteAlly(null);
		channel.setWritePlayer(new HashSet<>());

		switch (read)
		{
			case "all":
				channel.setReadAll(true);
				break;
			case "ally":
				channel.setReadAlly(this.ally);
				break;
			case "player":
				channel.setReadPlayer(Common.explodeToInt(",", readids));
				break;
		}

		switch (write)
		{
			case "all":
				channel.setWriteAll(true);
				break;
			case "ally":
				channel.setWriteAlly(this.ally);
				break;
			case "player":
				channel.setWritePlayer(Common.explodeToInt(",", writeids));
				break;
		}

		return new RedirectViewResult("showAllySettings").withMessage("Frequenz " + Common._plaintitle(name) + " geändert");
	}

	/**
	 * Loescht einen Comnet-Kanal der Allianz.
	 *  @param channel Die ID des zu loeschenden Kanals
	 * @param conf Die Bestaetigung des Vorgangs. <code>ok</code>, falls der Vorgang durchgefuehrt werden soll
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public Object deleteChannelAction(ComNetChannel channel, String conf, String show)
	{
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		if (this.ally.getPresident().getId() != user.getId())
		{
			return new RedirectViewResult("showAllySettings").withMessage("Fehler: Nur der Präsident der Allianz kann diese Aktion durchführen");
		}

		if ((channel == null) || (channel.getAllyOwner() != this.ally))
		{
			return new RedirectViewResult("showAllySettings").withMessage("Fehler: Diese Frequenz gehört nicht ihrer Allianz");
		}

		if (!conf.equals("ok"))
		{
			TemplateEngine t = templateViewResultFactory.createFor(this);
			setDefaultTemplateVars(t);
			t.setVar("show", show);
			t.setVar("ally.statusmessage", "Wollen sie die Frequenz \"" + Common._title(channel.getName()) + "\" wirklich l&ouml;schen?",
					"ally.statusmessage.ask.url1", "&amp;action=deleteChannel&amp;channel=" + channel.getId() + "&amp;conf=ok&amp;show=" + show,
					"ally.statusmessage.ask.url2", "&amp;show=" + show);
			return t;
		}

		db.createQuery("delete from ComNetVisit where channel=:channel")
				.setEntity("channel", channel)
				.executeUpdate();

		db.createQuery("delete from ComNetEntry where channel=:channel")
				.setEntity("channel", channel)
				.executeUpdate();

		db.delete(channel);

		return new RedirectViewResult("showAllySettings").withMessage("Die Frequenz wurde gelöscht");
	}

	private static final int MAX_UPLOAD_SIZE = 307200;

	/**
	 * Laedt das neue Logo der Allianz auf den Server.
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult uploadLogoAction(String show)
	{
		User user = (User) getUser();

		if (this.ally.getPresident().getId() != user.getId())
		{
			return new RedirectViewResult("showAllySettings").withMessage("Fehler: Nur der Präsident der Allianz kann diese Aktion durchführen");
		}

		List<FileItem> list = getContext().getRequest().getUploadedFiles();
		if (list.size() == 0)
		{
			return new RedirectViewResult("showAllySettings");
		}

		if (list.get(0).getSize() > MAX_UPLOAD_SIZE)
		{
			return new RedirectViewResult("showAllySettings").withMessage("Das Logo ist leider zu groß. Bitte w&auml;hle eine Datei mit maximal 300kB Größe<br />");
		}

		String message;
		String uploaddir = Configuration.getAbsolutePath() + "data/logos/ally/";
		try
		{
			File uploadedFile = new File(uploaddir + this.ally.getId() + ".gif");
			list.get(0).write(uploadedFile);
			message = "Das neue Logo wurde auf dem Server gespeichert<br />";
		}
		catch (Exception e)
		{
			message = "Offenbar ging beim Upload etwas schief (Ist die Datei evt. zu groß?)<br />";
			log.warn("", e);
		}

		return new RedirectViewResult("showAllySettings").withMessage(message);
	}

	/**
	 * Speichert die neuen Daten der Allianz.
	 *  @param name Der Name der Allianz
	 * @param desc Die Allianzbeschreibung
	 * @param allytag Der Allianztag
	 * @param hp Die URL zur Homepage
	 * @param praesi Der Name des Praesidentenpostens
	 * @param showastis Sollen eigene Astis auf der Sternenkarte angezeigt werden (<code>true</code>) oder nicht (<code>false</code>)
	 * @param showGtuBieter Sollen Allymember einander bei GTU-Versteigerungen sehen koennen (<code>true</code>) oder nicht (<code>false</code>)
	 * @param showlrs Sollen die LRS der Awacs in der Sternenkarte innerhalb der Ally geteilt werden (<code>true</code>) oder nicht (<code>false</code>)
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult changeSettingsAction(String name, String desc, String allytag, String hp, String praesi, boolean showastis, boolean showGtuBieter, boolean showlrs, String show)
	{
		User user = (User) getUser();

		if (this.ally.getPresident().getId() != user.getId())
		{
			return new RedirectViewResult("showAllySettings").withMessage("Fehler: Nur der Präsident der Allianz kann diese Aktion durchführen");
		}

		// Wurde der [name]-Tag vergessen?
		if (!allytag.contains("[name]"))
		{
			allytag += "[name]";
		}

		if (name.length() == 0)
		{
			return new RedirectViewResult("showAllySettings").withMessage("Fehler: Sie müssen einen Allianznamen angeben");
		}

		if (praesi.length() == 0)
		{
			return new RedirectViewResult("showAllySettings").withMessage("Fehler: Sie müssen dem Präsidentenamt einen Namen geben");
		}

		this.ally.setName(name);
		this.ally.setDescription(desc);
		this.ally.setHp(hp);
		this.ally.setAllyTag(allytag);
		this.ally.setShowAstis(showastis);
		this.ally.setShowGtuBieter(showGtuBieter);
		this.ally.setPname(praesi);
		this.ally.setShowLrs(showlrs);

		//Benutzernamen aktualisieren
		List<User> allyusers = this.ally.getMembers();
		for (User auser : allyusers)
		{
			String newname = allytag.replace("[name]", auser.getNickname());

			if (!newname.equals(auser.getName()))
			{
				auser.setName(newname);
			}
		}

		return new RedirectViewResult("showAllySettings").withMessage("Neue Daten gespeichert...");
	}

	/**
	 * Laesst den aktuellen Spieler aus der Allianz austreten.
	 *  @param conf Falls <code>ok</code> wird der Austritt vollzogen
	 * @param show Die nach der Bestaetigung anzuzeigende Aktion
	 */
	@Action(ActionType.DEFAULT)
	public Object partAction(String conf, String show)
	{
		User user = (User) getUser();

		if (this.ally.getPresident().getId() == user.getId())
		{
			return new RedirectViewResult("default").withMessage("<span style=\"color:red\">Sie können erst austreten, wenn ein anderer Präsident bestimmt wurde");
		}

		if (!conf.equals("ok"))
		{
			TemplateEngine t = templateViewResultFactory.createFor(this);
			setDefaultTemplateVars(t);
			t.setVar("show", show);
			t.setVar("ally.statusmessage", "Wollen sie wirklich aus der Allianz austreten?",
					"ally.statusmessage.ask.url1", "&amp;action=part&amp;conf=ok&amp;show=" + show,
					"ally.statusmessage.ask.url2", "&amp;show=" + show);
			return t;
		}

		PM.send(user, this.ally.getPresident().getId(), "Allianz verlassen",
				"Ich habe die Allianz verlassen");

		allianzService.entferneMitglied(ally, user);
		this.ally = null;

		return new RedirectViewResult("defaultNoAlly").withMessage("Allianz verlassen");
	}

	/**
	 * Loest einen Allianz auf.
	 *  @param conf Bestaetigt die Aufloesung, wenn der Wert <code>ok</code> ist
	 * @param show Die nach der Bestaetigung anzuzeigende Aktion
	 */
	@Action(ActionType.DEFAULT)
	public Object killAction(String conf, String show)
	{
		User user = (User) getUser();

		if (this.ally.getPresident().getId() != user.getId())
		{
			return new RedirectViewResult("default").withMessage("Fehler: Nur der Präsident der Allianz kann diese Aktion durchf&uuml;hren");
		}

		if (!conf.equals("ok"))
		{
			TemplateEngine t = templateViewResultFactory.createFor(this);
			setDefaultTemplateVars(t);
			t.setVar("show", show);
			t.setVar("ally.statusmessage", "Wollen sie die Allianz wirklich aufl&ouml;sen?",
					"ally.statusmessage.ask.url1", "&amp;action=kill&amp;conf=ok&amp;show=" + show,
					"ally.statusmessage.ask.url2", "&amp;show=" + show);

			return t;
		}
		else
		{
			PM.sendToAlly(user, this.ally, "Allianz aufgel&ouml;st", "Die Allianz wurde mit sofortiger Wirkung aufgel&ouml;st");

			allianzService.loeschen(this.ally);
			this.ally = null;

			return new RedirectViewResult("defaultNoAlly").withMessage("Die Allianz wurde aufgelöst");
		}
	}

	/**
	 * Befoerdert einen Spieler zum Praesidenten.
	 *  @param presn Die ID des neuen Praesidenten der Allianz
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult newPraesiAction(int presn, String show)
	{
		User user = (User) getUser();

		if (this.ally.getPresident() != user)
		{
			return new RedirectViewResult("showMembers").withMessage("Fehler: Nur der Präsident der Allianz kann diese Aktion durchführen");
		}

		User presnuser = (User) getContext().getDB().get(User.class, presn);
		if (presnuser.getAlly() != this.ally)
		{
			return new RedirectViewResult("showMembers").withMessage("Dieser Spieler ist nicht Mitglied ihrer Allianz");
		}

		this.ally.setPresident(presnuser);
		PM.send(this.ally.getPresident(), presnuser.getId(), "Zum Pr&auml;sidenten ernannt", "Ich habe dich zum Präsidenten der Allianz ernannt");

		return new RedirectViewResult("showMembers").withMessage(presnuser.getProfileLink() + " zum Präsidenten ernannt");
	}

	/**
	 * Wirft einen Spieler aus der Allianz.
	 *  @param kick Die ID des aus der Allianz zu werfenden Spielers
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult kickAction(int kick, String show)
	{
		User user = (User) getUser();

		if (this.ally.getPresident().getId() != user.getId())
		{
			return new RedirectViewResult("showMembers").withMessage("Fehler: Nur der Präsident der Allianz kann diese Aktion durchführen");
		}

		if (kick == user.getId())
		{
			return new RedirectViewResult("showMembers").withMessage("Sie können sich nicht selber aus der Allianz werfen");
		}

		User kickuser = (User) getContext().getDB().get(User.class, kick);
		if (!this.ally.equals(kickuser.getAlly()))
		{
			return new RedirectViewResult("showMembers").withMessage("Dieser Spieler ist nicht Mitglied ihrer Allianz");
		}

		this.allianzService.entferneMitglied(ally, kickuser);

		PM.send(this.ally.getPresident(), kickuser.getId(), "Aus der Allianz geworfen", "Ich habe dich aus der Allianz geworfen.");

		return new RedirectViewResult("showMembers").withMessage(Common._title(kickuser.getName()) + " aus der Allianz geworfen");
	}

	/**
	 * Zeigt die Liste der Allianzen fuer einen Allianzbeitritt an.
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultNoAllyAction(String show, RedirectViewResult redirect)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		setDefaultTemplateVars(t);
		t.setVar("ally.message", redirect != null ? redirect.getMessage() : null);

		t.setVar("show", show);

		t.setVar("show.join", 1);
		t.setBlock("_ALLY", "show.join.allylist.listitem", "show.join.allylist.list");

		List<?> al = getDB().createQuery("from Ally order by founded").list();
		for (Object anAl : al)
		{
			Ally aAlly = (Ally) anAl;

			t.setVar("show.join.allylist.allyid", aAlly.getId(),
					"show.join.allylist.name", Common._title(aAlly.getName()));

			t.parse("show.join.allylist.list", "show.join.allylist.listitem", true);
		}
		return t;
	}

	/**
	 * Zeigt die GUI zum Gruenden einer Allianz an.
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine showCreateAllyAction(String show, RedirectViewResult redirect)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		setDefaultTemplateVars(t);

		t.setVar("ally.statusmessage", redirect != null ? redirect.getMessage() : null);

		User user = (User) getUser();
		t.setVar("show", show);

		if (Common.time() - user.getSignup() < 60 * 60 * 24 * 3)
		{
			t.setVar("ally.message", "Sie m&uuml;ssen seit mindestens 3 Tage dabei sein um eine Allianz gr&uuml;nden zu k&ouml;nnen");
		}
		else
		{
			t.setVar("show.create", 1);
		}

		return t;
	}

	/**
	 * Zeigt die Rangliste der Allianz an.
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public Object showRaengeAction(String show, RedirectViewResult redirect)
	{
		if (this.ally == null)
		{
			return new RedirectViewResult("defaultNoAlly");
		}

		User user = (User) getUser();
		if (this.ally.getPresident().getId() != user.getId() || !user.isNPC())
		{
			return new RedirectViewResult("default");
		}

		TemplateEngine t = templateViewResultFactory.createFor(this);
		setDefaultTemplateVars(t);
		t.setVar("show", show);
		t.setVar("ally.message", redirect != null ? redirect.getMessage() : null);

		t.setVar("show.raenge", 1,
				"show.raenge.modify.list", "");

		t.setBlock("_ALLY", "show.raenge.modify.listitem", "show.raenge.modify.list");

		for (AllyRangDescriptor allyRang : this.ally.getRangDescriptors())
		{

			t.setVar(
					"show.raenge.modify.rangname", allyRang.getName(),
					"show.raenge.modify.rangnr", allyRang.getRang(),
					"show.raenge.modify.rangimg", allyRang.getImage());

			t.parse("show.raenge.modify.list", "show.raenge.modify.listitem", true);
		}
		return t;
	}

	/**
	 * Zeigt die Postenliste der Allianz an.
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public Object showPostenAction(String show, RedirectViewResult redirect)
	{
		if (this.ally == null)
		{
			return new RedirectViewResult("defaultNoAlly");
		}

		TemplateEngine t = templateViewResultFactory.createFor(this);
		setDefaultTemplateVars(t);
		t.setVar("show", show);
		t.setVar("ally.message", redirect != null ? redirect.getMessage() : null);

		List<User> allymember = this.ally.getMembers();

		int postencount = allyPostenService.getAnzahlPostenDerAllianz(ally);
		int maxposten = allyPostenService.getMaxPostenDerAllianz(ally);

		t.setVar("show.posten", 1,
				"show.posten.count", postencount,
				"show.posten.maxcount", maxposten,
				"show.posten.addposten", (maxposten > postencount),
				"show.posten.modify.list", "");

		t.setBlock("_ALLY", "show.posten.modify.listitem", "show.posten.modify.list");
		t.setBlock("show.posten.modify.listitem", "show.posten.modify.userlist.listitem", "show.posten.modify.userlist.list");

		for (AllyPosten aposten : ally.getPosten())
		{
			t.setVar("show.posten.modify.name", Common._plaintitle(aposten.getName()),
					"show.posten.modify.id", aposten.getId(),
					"show.posten.modify.userlist.list", "");

			if (aposten.getUser() == null)
			{
				t.setVar("show.posten.modify.userlist.id", "",
						"show.posten.modify.userlist.name", "KEINER",
						"show.posten.modify.userlist.selected", 1);

				t.parse("show.posten.modify.userlist.list", "show.posten.modify.userlist.listitem", true);
			}

			for (User member : allymember)
			{
				t.setVar("show.posten.modify.userlist.id", member.getId(),
						"show.posten.modify.userlist.name", Common._title(member.getNickname()),
						"show.posten.modify.userlist.selected", aposten.getUser() != null && (aposten.getUser().getId() == member.getId()));

				t.parse("show.posten.modify.userlist.list", "show.posten.modify.userlist.listitem", true);
			}

			t.parse("show.posten.modify.list", "show.posten.modify.listitem", true);
		}

		if (maxposten > postencount)
		{
			t.setBlock("_ALLY", "show.posten.addposten.userlist.listitem", "show.posten.addposten.userlist.list");

			for (User member : allymember)
			{
				t.setVar("show.posten.addposten.userlist.id", member.getId(),
						"show.posten.addposten.userlist.name", Common._title(member.getNickname()));

				t.parse("show.posten.addposten.userlist.list", "show.posten.addposten.userlist.listitem", true);
			}
		}
		return t;
	}

	/**
	 * Zeigt die Liste der zerstoerten und verlorenen Schiffe der Allianz.
	 *  @param destpos Offset fuer die Liste der zerstoerten Schiffe
	 * @param lostpos Offset fuer die Liste der verlorenen Schiffe
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public Object showBattlesAction(long destpos, long lostpos, String show)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		setDefaultTemplateVars(t);
		t.setVar("show", show);
		if (this.ally == null)
		{
			return new RedirectViewResult("defaultNoAlly");
		}

		org.hibernate.Session db = getDB();

		/////////////////////////////
		// Zerstoerte Schiffe
		/////////////////////////////

		int counter = 0;

		long destcount = (Long) db.createQuery("select count(distinct tick) from ShipLost where destAlly=:ally")
				.setInteger("ally", this.ally.getId())
				.iterate().next();
		if (destpos > destcount)
		{
			destpos = destcount - 10;
		}

		if (destpos < 0)
		{
			destpos = 0;
		}

		t.setBlock("_ALLY", "show.destships.listitem", "show.destships.list");
        t.setBlock("show.destships.listitem", "show.destships.ships.listitem", "show.destships.ships.list");
		t.setBlock("_ALLY", "show.destships.linefiller.listitem", "show.destships.linefiller.list");
		t.setBlock("_ALLY", "show.lostships.listitem", "show.lostships.list");
        t.setBlock("show.lostships.listitem", "show.lostships.ships.listitem", "show.lostships.ships.list");
		t.setBlock("_ALLY", "show.lostships.linefiller.listitem", "show.lostships.linefiller.list");

		t.setVar("show.battles", 1,
				"show.destships.list", "",
				"show.destships.linefiller.list", "",
				"show.lostships.list", "",
				"show.lostships.linefiller.list", "",
				"show.destpos.back", destpos - 10,
				"show.destpos.forward", destpos + 10);

        List<?> ticks = db.createQuery("SELECT distinct tick FROM ShipLost WHERE destAlly=:ally ORDER BY tick DESC")
                .setInteger("ally", this.ally.getId())
                .setMaxResults(10)
                .setFirstResult((int) destpos)
                .list();
        for( Object o : ticks )
        {
            int tick = (Integer)o;
            List<?> s = db.createQuery("SELECT distinct count(*),type,owner FROM ShipLost WHERE destAlly=:ally AND tick=:tick GROUP BY type,owner")
                    .setInteger("ally", ally.getId())
                    .setInteger("tick", tick)
                    .list();

			counter++;

            t.setVar("show.destships.time", Common.getIngameTime(tick),
                     "show.destships.newrow", (counter % 5) == 0,
                     "show.destships.ships.list", "");

            for( Object o2 : s ) {
                Object[] data = (Object[]) o2;
                ShipTypeData shiptype = Ship.getShipType((Integer) data[1]);
                User auser = (User) db.get(User.class, (Integer) data[2]);

                long count = (Long) data[0];
                String shiptypename;
                String shiptypepicture;
                String ownername;

                if (shiptype != null) {
                    shiptypename = shiptype.getNickname();
                    shiptypepicture = shiptype.getPicture();
                } else {
                    shiptypename = "Unbekannter Schiffstyp";
                    shiptypepicture = "";
                }


                if (auser != null) {
                    ownername = auser.getName();
                } else {
                    ownername = "Unbekannter Spieler (" + data[2] + ")";
                }


                t.setVar("show.destships.ships.count", count,
                        "show.destships.ships.type.name", shiptypename,
                        "show.destships.ships.type", data[1],
                        "show.destships.ships.type.picture", shiptypepicture,
                        "show.destships.ships.owner", Common._title(ownername));


                t.parse("show.destships.ships.list", "show.destships.ships.listitem", true);
            }
            t.parse("show.destships.list", "show.destships.listitem", true);
		}

		while (counter % 5 != 0)
		{
			t.parse("show.destships.linefiller.list", "show.destships.linefiller.listitem", true);
			counter++;
		}

		/////////////////////////////
		// Verlorene Schiffe
		/////////////////////////////

		counter = 0;

        long lostcount = (Long) db.createQuery("select count(distinct tick) from ShipLost where ally=:ally")
                .setInteger("ally", this.ally.getId())
                .iterate().next();
        if (lostpos > lostcount)
        {
            lostpos = lostcount - 10;
        }

        if (lostpos < 0)
        {
            lostpos = 0;
        }

        t.setVar("show.battles", 1,
                "show.lostships.list", "",
                "show.lostships.linefiller.list", "",
                "show.lostships.list", "",
                "show.lostships.linefiller.list", "",
                "show.lostpos.back", lostpos - 10,
                "show.lostpos.forward", lostpos + 10);

        ticks = db.createQuery("SELECT distinct tick FROM ShipLost WHERE ally=:ally ORDER BY tick DESC")
                .setInteger("ally", this.ally.getId())
                .setMaxResults(10)
                .setFirstResult((int) lostpos)
                .list();
        for( Object o : ticks )
        {
            int tick = (Integer)o;
            List<?> s = db.createQuery("SELECT distinct count(*),type,destOwner FROM ShipLost WHERE ally=:ally AND tick=:tick GROUP BY type,destOwner")
                    .setInteger("ally", ally.getId())
                    .setInteger("tick", tick)
                    .list();

            counter++;

            t.setVar("show.lostships.time", Common.getIngameTime(tick),
                    "show.lostships.newrow", (counter % 5) == 0,
                    "show.lostships.ships.list", "");

            for( Object o2 : s ) {
                Object[] data = (Object[]) o2;
                ShipTypeData shiptype = Ship.getShipType((Integer) data[1]);
                User auser = (User) db.get(User.class, (Integer) data[2]);

                long count = (Long) data[0];
                String shiptypename;
                String shiptypepicture;
                String ownername;

                if (shiptype != null) {
                    shiptypename = shiptype.getNickname();
                    shiptypepicture = shiptype.getPicture();
                } else {
                    shiptypename = "Unbekannter Schiffstyp";
                    shiptypepicture = "";
                }


                if (auser != null) {
                    ownername = auser.getName();
                } else {
                    ownername = "Unbekannter Spieler (" + data[2] + ")";
                }


                t.setVar("show.lostships.ships.count", count,
                        "show.lostships.ships.type.name", shiptypename,
                        "show.lostships.ships.type", data[1],
                        "show.lostships.ships.type.picture", shiptypepicture,
                        "show.lostships.ships.owner", Common._title(ownername));

                t.parse("show.lostships.ships.list", "show.lostships.ships.listitem", true);
            }
            t.parse("show.lostships.list", "show.lostships.listitem", true);
        }

		while (counter % 5 != 0)
		{
			t.parse("show.lostships.linefiller.list", "show.lostships.linefiller.listitem", true);
			counter++;
		}
		return t;
	}

	/**
	 * Zeigt die Allianzeinstellungen an.
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public Object showAllySettingsAction(String show, RedirectViewResult redirect)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		setDefaultTemplateVars(t);
		t.setVar("show", show);
		if (this.ally == null)
		{
			return new RedirectViewResult("defaultNoAlly");
		}

		User user = (User) getUser();

		if (user.getId() != this.ally.getPresident().getId())
		{
			return new RedirectViewResult("default");
		}

		t.setVar("ally.message", redirect != null ? redirect.getMessage() : null);

		org.hibernate.Session db = getDB();

		t.setVar("show.einstellungen", 1,
				"ally.plainname", this.ally.getName(),
				"ally.description", this.ally.getDescription(),
				"ally.hp", this.ally.getHp(),
				"ally.allytag", this.ally.getAllyTag(),
				"ally.pname", this.ally.getPname(),
				"ally.showastis", this.ally.getShowAstis(),
				"ally.showgtubieter", this.ally.getShowGtuBieter(),
				"ally.showlrs", this.ally.getShowLrs(),
				"show.einstellungen.channels.list", "");

		// Zuerst alle vorhandenen Channels dieser Allianz auslesen (max 2)
		List<ComNetChannel> channels = new ArrayList<>();
		List<?> channelList = db.createQuery("from ComNetChannel where allyOwner=:ally")
				.setEntity("ally", this.ally)
				.setMaxResults(2)
				.list();
		channels.addAll(channelList.stream().map(aChannelList -> (ComNetChannel) aChannelList).collect(Collectors.toList()));
		channels.add(null);

		t.setBlock("_ALLY", "show.einstellungen.channels.listitem", "show.einstellungen.channels.list");

		// Nun die vorhandenen Channels anzeigen und ggf. eine Eingabemaske fuer neue Channels anzeigen
		for (int i = 0; i <= 1; i++)
		{
			t.start_record();
			ComNetChannel comNetChannel = channels.get(i);
			t.setVar("show.einstellungen.channels.id", comNetChannel == null ? 0 : comNetChannel.getId(),
					"show.einstellungen.channels.index", i + 1);

			if (comNetChannel != null)
			{
				t.setVar("show.einstellungen.channels.name", Common._plaintitle(comNetChannel.getName()),
						"show.einstellungen.channels.readall", comNetChannel.isReadAll(),
						"show.einstellungen.channels.writeall", comNetChannel.isWriteAll(),
						"show.einstellungen.channels.readally", comNetChannel.getReadAlly() != null ? comNetChannel.getReadAlly().getId() : 0,
						"show.einstellungen.channels.writeally", comNetChannel.getWriteAlly() != null ? comNetChannel.getWriteAlly().getId() : 0,
						"show.einstellungen.channels.readids", comNetChannel.getReadPlayer().stream().map(User::getId).map(Object::toString).collect(Collectors.joining(",")),
						"show.einstellungen.channels.writeids", comNetChannel.getWritePlayer().stream().map(User::getId).map(Object::toString).collect(Collectors.joining(",")));
			}
			else
			{
				t.setVar("show.einstellungen.channels.name", "",
						"show.einstellungen.channels.readall", 1,
						"show.einstellungen.channels.writeall", 1);
			}

			t.parse("show.einstellungen.channels.list", "show.einstellungen.channels.listitem", true);

			t.stop_record();
			t.clear_record();

			// Maximal eine Eingabemaske anzeigen
			if (comNetChannel == null)
			{
				break;
			}
		}
		return t;
	}

	/**
	 * Zeigt die Mitgliederliste an.
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public Object showMembersAction(String show, RedirectViewResult redirect)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		setDefaultTemplateVars(t);
		t.setVar("show", show);
		if (this.ally == null)
		{
			return new RedirectViewResult("defaultNoAlly");
		}

		t.setVar("ally.message", redirect != null ? redirect.getMessage() : null);
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		t.setVar("show.members", 1,
				"user.president", (user.getId() == this.ally.getPresident().getId()));

		t.setBlock("_ALLY", "show.members.listitem", "show.members.list");

		//Mitglieder auflisten
		List<?> memberList = db.createQuery("from User where ally=:ally order by name")
				.setEntity("ally", this.ally)
				.list();
		for (Object aMemberList : memberList)
		{
			User member = (User) aMemberList;

			t.setVar("show.members.name", Common._title(member.getName()),
					"show.members.id", member.getId());

			if (user.getId() == this.ally.getPresident().getId())
			{
				String inakt_status;
				int inakt = member.getInactivity();
				if (inakt <= 14)
				{
					inakt_status = "<span style=\\'color:#00FF00\\'>aktiv</span>";
				}
				else if (inakt <= 49)
				{
					inakt_status = "<span style=\\'color:#22AA22\\'>weniger aktiv</span>";
				}
				else if (inakt <= 98)
				{
					inakt_status = "<span style=\\'color:#668822\\'>selten aktiv</span>";
				}
				else if (inakt <= 196)
				{
					inakt_status = "<span style=\\'color:#884422\\'>inaktiv</span>";
				}
				else if (inakt <= 300)
				{
					inakt_status = "<span style=\\'color:#AA4422\\'>scheintot</span>";
				}
				else
				{
					inakt_status = "<span style=\\'color:#FF2222\\'>bald gel&ouml;scht</span>";
				}

				t.setVar("show.members.inaktstatus", inakt_status);
			}

			t.parse("show.members.list", "show.members.listitem", true);
		}
		return t;
	}

	/**
	 * Zeigt die GUI, spezifiziert durch den Parameter show,
	 * fuer Spieler mit Allianz, an.
	 * @param show Die Aktion die nach der Durchfuehrung angezeigt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public Object defaultAction(String show, RedirectViewResult redirect)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		setDefaultTemplateVars(t);
		t.setVar("show", show);

		if (this.ally == null)
		{
			return new RedirectViewResult("defaultNoAlly");
		}

		t.setVar("ally.message", redirect != null ? redirect.getMessage() : null);

		/*
			Allgemeines
		*/

		User presi = this.ally.getPresident();
		long membercount = this.ally.getMemberCount();

		t.setVar("show.allgemein", 1,
				"ally.description", Common._text(this.ally.getDescription()),
				"ally.founded", this.ally.getFounded().toString(),
				"ally.wonBattles", this.ally.getWonBattles(),
				"ally.lostBattles", this.ally.getLostBattles(),
				"ally.destroyedShips", this.ally.getDestroyedShips(),
				"ally.lostShips", this.ally.getLostShips(),
				"ally.membercount", membercount,
				"ally.pname", Common._plaintitle(this.ally.getPname()),
				"ally.president.id", this.ally.getPresident().getId(),
				"ally.president.name", Common._title(presi.getName()),
				"ally.posten.list", "");

		if (ally.getItems().length() > 0)
		{
			t.setBlock("_ALLY", "ally.items.listitem", "ally.items.list");
			Cargo itemlist = new Cargo(Cargo.Type.ITEMSTRING, this.ally.getItems());
			ResourceList reslist = itemlist.getResourceList();

			Resources.echoResList(t, reslist, "ally.items.list");
		}

		t.setBlock("_ALLY", "ally.posten.listitem", "ally.posten.list");

		for (AllyPosten aposten : ally.getPosten())
		{
			if (aposten.getUser() == null)
			{
				continue;
			}

			t.setVar("ally.posten.name", Common._title(aposten.getName()),
					"ally.posten.user.name", Common._title(aposten.getUser().getName()),
					"ally.posten.user.id", aposten.getUser().getId());

			t.parse("ally.posten.list", "ally.posten.listitem", true);
		}

		List<?> allymembers = getDB().createQuery("from User " +
				"where ally= :ally and " +
				"id!= :presidentId and " +
				"allyposten is null")
				.setEntity("ally", this.ally)
				.setInteger("presidentId", this.ally.getPresident().getId())
				.list();
		if (allymembers.size() > 0)
		{
			t.setVar("ally.addmembers.list", "");
			t.setBlock("_ALLY", "ally.addmembers.listitem", "ally.addmembers.list");

			for (Object allymember1 : allymembers)
			{
				User allymember = (User) allymember1;
				t.setVar("ally.addmembers.name", Common._title(allymember.getName()),
						"ally.addmembers.id", allymember.getId());

				t.parse("ally.addmembers.list", "ally.addmembers.listitem", true);
			}
		}
		return t;
	}
}
