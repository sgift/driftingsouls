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
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.IntTutorial;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.entities.UserRank;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.RedirectViewResult;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Die Uebersicht.
 *
 * @author Christopher Jung
 */
@Module(name = "ueber")
public class UeberController extends Controller
{
	private static final Log log = LogFactory.getLog(UeberController.class);
	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public UeberController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;
	}

	/**
	 * Beendet den Vacation-Modus-Vorlauf.
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult stopWait4VacAction()
	{
		User user = (User) getUser();

		user.setVacationCount(0);
		user.setWait4VacationCount(0);

		Common.writeLog("login.log", Common.date("j+m+Y H:i:s") + ": <" + getRequest().getRemoteAddress() + "> (" + user.getId() + ") <" + user.getUN() + "> Abbruch Vac-Vorlauf Browser <" + getRequest().getUserAgent() + "> \n");

		return new RedirectViewResult("default");
	}

	/**
	 * Wechselt die Tutorial-Seite bzw beendet das Tutorial.
	 *
	 * @param tutorial 1, falls die naechste Tutorialseite angezeigt werden soll. Zum Beenden -1
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult tutorialAction(int tutorial)
	{
		User user = (User) getUser();

		if (tutorial == 1)
		{
			int inttutorial = user.getUserValue(WellKnownUserValue.TBLORDER_UEBERSICHT_INTTUTORIAL);

			user.setUserValue(WellKnownUserValue.TBLORDER_UEBERSICHT_INTTUTORIAL, inttutorial + 1);
		}
		else if (tutorial == -1)
		{
			user.setUserValue(WellKnownUserValue.TBLORDER_UEBERSICHT_INTTUTORIAL, 0);
		}

		return new RedirectViewResult("default");
	}

	/**
	 * Wechselt den Anzeigemodus der Flotten/Bookmark-Box.
	 *
	 * @param box Der Name des neuen Anzeigemodus
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult boxAction(String box)
	{
		User user = (User)getUser();
		String boxSetting = user.getUserValue(WellKnownUserValue.TBLORDER_UEBERSICHT_BOX);
		if (box != null && !box.equals(boxSetting))
		{
			user.setUserValue(WellKnownUserValue.TBLORDER_UEBERSICHT_BOX, box);
		}

		return new RedirectViewResult("default");
	}

	/**
	 * Zeigt die Uebersicht an.
	 */
	@Action(value = ActionType.DEFAULT, readOnly = true)
	public TemplateEngine defaultAction()
	{
		org.hibernate.Session db = getDB();
		User user = (User) getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);
		String ticktime = getTickTime();

		String race = "???";
		if (Rassen.get().rasse(user.getRace()) != null)
		{
			race = Rassen.get().rasse(user.getRace()).getName();
		}

		int ticks = getContext().get(ContextCommon.class).getTick();

		long[] fullbalance = user.getFullBalance();

		t.setVar("user.name", Common._title(user.getName()),
				"user.race", race,
				"res.nahrung.image", Cargo.getResourceImage(Resources.NAHRUNG),
				"res.re.image", Cargo.getResourceImage(Resources.RE),
				"user.nahrung.new", Common.ln(fullbalance[0]),
				"user.nahrung.new.plain", fullbalance[0],
				"user.konto", Common.ln(user.getKonto()),
				"user.balance", Common.ln(fullbalance[1]),
				"user.balance.plain", fullbalance[1],
				//"user.specpoints", user.getFreeSpecializationPoints(),
				//"user.maxspecpoints", user.getSpecializationPoints(),
				"global.ticks", ticks,
				"global.ticktime", ticktime);

		//
		// Ingame-Zeit setzen
		//

		String curtime = Common.getIngameTime(ticks);

		t.setVar("time.current", curtime);

		//------------------------------
		// auf neue Nachrichten checken
		//------------------------------

		long newCount = (Long) db.createQuery("select count(*) from PM where empfaenger= :user and gelesen=0")
				.setEntity("user", user)
				.iterate().next();
		t.setVar("user.newmsgs", Common.ln(newCount));

		//------------------------------
		// Mangel auf Asteroiden checken
		//------------------------------

		int anzahlBasen = mangelAufAsteroidenAnzeigen(db, user, t);

		//------------------------------
		// Mangel auf Schiffen checken
		//------------------------------

		mangelAufSchiffenAnzeigen(db, user, t);

		//------------------------------
		// Schlachten anzeigen
		//------------------------------

		laufendeSchlachtenAnzeigen(db, user, t);
		//------------------------------
		// Logo anzeigen
		//------------------------------

		if (user.getAlly() != null)
		{
			t.setVar("ally.logo", user.getAlly().getId());
		}

		//------------------------------
		// Interaktives Tutorial
		//------------------------------

		int inttutorial = user.getUserValue(WellKnownUserValue.TBLORDER_UEBERSICHT_INTTUTORIAL);

		if (inttutorial != 0)
		{
			long shipcount = (Long) db.createQuery("select count(*) from Ship " +
					"where id>0 and owner= :user")
					.setEntity("user", user)
					.iterate().next();
			showTutorialPages(t, anzahlBasen, shipcount, inttutorial);
		}

		//------------------------------------
		// Die Box (Bookmarks/Flotten)
		//------------------------------------

		t.setBlock("_UEBER", "bookmarks.listitem", "bookmarks.list");
		t.setBlock("_UEBER", "fleets.listitem", "fleets.list");
		t.setVar("bookmarks.list", "",
				"fleets.list", "");


		// Bookmarks zusammenbauen
		t.setVar("show.bookmarks", 1);

		List<?> bookmarks = db.createQuery("from Ship where id>0 and einstellungen.bookmark=true and owner=:owner order by id desc")
				.setEntity("owner", user)
				.list();
		for (Object bookmark1 : bookmarks)
		{
			Ship bookmark = (Ship) bookmark1;
			ShipTypeData shiptype = bookmark.getTypeData();
			t.setVar("bookmark.shipid", bookmark.getId(),
					"bookmark.shipname", bookmark.getName(),
					"bookmark.location", bookmark.getLocation().displayCoordinates(false),
					"bookmark.shiptype", shiptype.getNickname(),
					"bookmark.description", bookmark.getEinstellungen().getDestSystem() + ":" + bookmark.getEinstellungen().getDestX() + "/" + bookmark.getEinstellungen().getDestY() + "<br />" + bookmark.getEinstellungen().getDestCom().replace("\r\n", "<br />"));
			t.parse("bookmarks.list", "bookmarks.listitem", true);
		}
		// Flotten zusammenbauen
		t.setVar("show.fleets", 1);
		boolean jdocked = false;

		List<?> fleets = db.createQuery("select count(*),s.fleet from Ship s " +
				"where s.id>0 and s.owner= :user and s.fleet.id!=0 " +
				"group by s.fleet,s.docked,s.system,s.x,s.y " +
				"order by s.docked,s.system,s.x,s.y")
				.setEntity("user", user)
				.list();
		for (Object fleet1 : fleets)
		{
			Object[] data = (Object[]) fleet1;
			long count = (Long) data[0];
			ShipFleet fleet = (ShipFleet) data[1];

			Ship aship = (Ship) db.createQuery("from Ship where fleet=:fleet")
					.setEntity("fleet", fleet)
					.iterate().next();

			if (!jdocked && aship.isLanded())
			{
				jdocked = true;
				t.setVar("fleet.jaegerfleet", 1);
			}
			else
			{
				t.setVar("fleet.jaegerfleet", 0);
			}


			String locationText = aship.getLocation().displayCoordinates(false);
			
			t.setVar("fleet.shipid", aship.getId(),
					"fleet.name", fleet.getName(),
					"fleet.location", locationText,
					"fleet.shipcount", count);
			t.parse("fleets.list", "fleets.listitem", true);
		}

		return t;
	}

	private void mangelAufSchiffenAnzeigen(Session db, User user, TemplateEngine t)
	{
		long sw;
		long shipNoCrew;

		sw = (Long) db.createQuery("select count(*) " +
				"from Ship " +
				"where id>0 and owner= :user and (locate('mangel_nahrung',status)!=0 or locate('mangel_reaktor',status)!=0) and locate('nocrew',status)=0")
				.setEntity("user", user)
				.iterate().next();

		shipNoCrew = (Long) db.createQuery("select count(*) from Ship as s where s.id>0 and s.owner= :user" +
				" and ((s.modules is not null and s.crew < (select crew from ShipModules where id=s.modules.id)) or s.crew < (select crew from ShipType where id = s.shiptype.id))")
				.setEntity("user", user)
				.iterate().next();

		t.setVar("schiffe.mangel", Common.ln(sw),
				"schiffe.nocrew", Common.ln(shipNoCrew));
	}

	private void laufendeSchlachtenAnzeigen(Session db, User user, TemplateEngine t)
	{
		// Ab hier beginnt das erste Bier
		Set<Battle> battles = erzeugeListeDerRelevantenSchlachten(db, user);

		// Ab hier beginnt das zweite Bier
		StringBuilder battlelist = new StringBuilder();

		for (Battle battle : battles)
		{
			String eparty;
			String eparty2;
			if (battle.getAlly(0) == 0)
			{
				final User commander1 = battle.getCommander(0);
				eparty = Common._title(commander1.getName());
			}
			else
			{
				final Ally ally = (Ally) db.get(Ally.class, battle.getAlly(0));
				eparty = Common._title(ally.getName());
			}

			if (battle.getAlly(1) == 0)
			{
				final User commander2 = battle.getCommander(1);
				eparty2 = Common._title(commander2.getName());
			}
			else
			{
				final Ally ally = (Ally) db.get(Ally.class, battle.getAlly(1));
				eparty2 = Common._title(ally.getName());
			}


			battlelist.append("<a class=\"error\" href=\"ds?module=angriff&amp;battle=").append(battle.getId()).append("\">Schlacht ").append(eparty).append(" vs ").append(eparty2).append(" bei ").append(battle.getLocation().displayCoordinates(false)).append("</a>&nbsp;");

			// Nahrunganzeige der Schlacht
			int nahrung = battle.getNahrungsBalance(user);

			if (nahrung < 0)
			{
				battlelist.append("<span style=\"color:red\">(").append(Common.ln(nahrung));
			}
			else
			{
				battlelist.append("<span style=\"color:green\">(+").append(Common.ln(nahrung));
			}

			battlelist.append(" <img src=\"").append(Cargo.getResourceImage(Resources.NAHRUNG)).append("\" alt=\"Nahrung\" title=\"Nahrung\" />)</span>");

			battlelist.append("<br />\n");
		}

		t.setVar("global.battlelist", battlelist);
	}

	private Set<Battle> erzeugeListeDerRelevantenSchlachten(Session db, User user)
	{
		Set<Battle> battles = new LinkedHashSet<>();

		Set<UserRank> ownRanks = user.getOwnRanks();
		Set<User> commanderSet = new LinkedHashSet<>();
		commanderSet.add(user);
		for (UserRank ownRank : ownRanks)
		{
			if (ownRank.getRank() > 0 && ownRank.getRankGiver().getAlly() != null)
			{
				commanderSet.addAll(ownRank.getRankGiver().getAlly().getMembers());
			}
		}

		if (!hasPermission(WellKnownPermission.SCHLACHT_LISTE))
		{
			// Zwei separate Queries fuer alle Schlachten um einen sehr unvorteilhaften Join zu vermeiden
			String query = "from Battle " +
					"where commander1 in (:commanders) or commander2 in (:commanders) ";

			//hat der Benutzer eine ally, dann haeng das hier an
			if (user.getAlly() != null)
			{
				query += " or ally1 = :ally or ally2 = :ally";
			}
			// ach haengen wir mal den quest kram dran
			if (user.hasFlag(UserFlag.QUEST_BATTLES))
			{
				query += " or quest is not null";
			}

			Query battleQuery = db.createQuery(query)
					.setParameterList("commanders", commanderSet);

			if (user.getAlly() != null)
			{
				battleQuery = battleQuery.setInteger("ally", user.getAlly().getId());
			}

			battles.addAll(Common.cast(battleQuery.list(), Battle.class));

			battles.addAll(Common.cast(
					db.createQuery("select distinct battle from Ship where battle is not null and owner=:user")
							.setEntity("user", user)
							.list(),
					Battle.class));
		}
		// Bei entsprechendem AccessLevel/Flag alle Schlachten anzeigen
		else
		{
			battles.addAll(Common.cast(db.createQuery("from Battle").list(), Battle.class));
		}
		return battles;
	}

	private int mangelAufAsteroidenAnzeigen(Session db, User user, TemplateEngine t)
	{
		int bw = 0;
		int bases = 0;

		List<?> basen = db.createQuery("from Base where owner= :user order by id")
				.setEntity("user", user)
				.list();
		for (Object aBasen : basen)
		{
			Base base = (Base) aBasen;
			bases++;

			BaseStatus basedata = Base.getStatus(base);

			Cargo cargo = new Cargo(base.getCargo());
			cargo.addResource(Resources.RE, user.getKonto().longValue());

			boolean mangel = false;

			ResourceList reslist = basedata.getProduction().getResourceList();
			for (ResourceEntry res : reslist)
			{
				if ((res.getCount1() < 0) && (-(cargo.getResourceCount(res.getId()) / res.getCount1()) <= 9))
				{
					mangel = true;
					break;
				}
			}

			if (basedata.getEnergy() < 0 && base.getEnergy() / -basedata.getEnergy() <= 9)
			{
				mangel = true;
			}

			if (mangel)
			{
				bw++;
			}
		}

		t.setVar("astis.mangel", bw);
		return bases;
	}

	private String getTickTime()
	{
		String ticktime = "";

		// Letzten Tick ermitteln (Zeitpunkt)
        try (BufferedReader bf = new BufferedReader(new FileReader(Configuration.getLogPath() + "ticktime.log")))
        {
            ticktime = bf.readLine();
        }
		catch (IOException e)
		{
			log.warn("Es konnte nicht auf die Datei ticktime.log zugegriffen werden: "+e.getMessage());
		}

		if (ticktime == null)
		{
			ticktime = "";
		}
		return ticktime;
	}

	private void showTutorialPages(TemplateEngine t, int bases, long shipcount, int inttutorial)
	{
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		boolean reqname = !"Kolonist".equals(user.getName());
		boolean reqship = shipcount > 0;
		boolean reqbase = bases > 0;

		if (!reqship)
		{
			reqbase = true;
		}

		if (!reqname)
		{
			reqship = true;
			reqbase = true;
		}

		IntTutorial sheet = (IntTutorial) db.createQuery("from IntTutorial where id= :id and reqName= :reqname and reqBase= :reqbase and reqShip= :reqship")
				.setInteger("id", inttutorial)
				.setBoolean("reqname", reqname)
				.setBoolean("reqbase", reqbase)
				.setBoolean("reqship", reqship)
				.uniqueResult();

		// Ist die aktuelle Tutorialseite veraltet?
		if ((sheet == null) || (sheet.getId() != inttutorial))
		{
			sheet = (IntTutorial) db.createQuery("from IntTutorial where reqName= :reqname and reqBase= :reqbase and reqShip= :reqship order by id")
					.setBoolean("reqname", reqname)
					.setBoolean("reqbase", reqbase)
					.setBoolean("reqship", reqship)
					.setMaxResults(1)
					.uniqueResult();

			if (sheet == null)
			{
				user.setUserValue(WellKnownUserValue.TBLORDER_UEBERSICHT_INTTUTORIAL, 0);
				return;
			}

			// Neue Tutorialseite speichern
			inttutorial = sheet.getId();
			user.setUserValue(WellKnownUserValue.TBLORDER_UEBERSICHT_INTTUTORIAL, inttutorial);
		}

		// Existiert eine Nachfolgerseite?
		IntTutorial nextsheet = (IntTutorial) db.createQuery("from IntTutorial where benoetigteSeite= :reqsheet and reqName= :reqname and reqBase= :reqbase and reqShip= :reqship")
				.setEntity("reqsheet", sheet)
				.setBoolean("reqname", reqname)
				.setBoolean("reqbase", reqbase)
				.setBoolean("reqship", reqship)
				.setMaxResults(1)
				.uniqueResult();

		// Kann das Tutorial jetzt beendet werden?
		if ((nextsheet == null) && reqname && reqship && reqbase)
		{
			t.setVar("sheet.endtutorial", 1);
		}
		else if (nextsheet != null)
		{
			t.setVar("sheet.nextsheet", 1);
		}

		t.setVar("interactivetutorial.show", 1,
				"sheet.headpic", sheet.getHeadImg(),
				"sheet.text", Common._text(sheet.getText()));
	}

}
