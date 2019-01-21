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

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.Handel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.RedirectViewResult;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParam;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * Zeigt aktive Handelsangebote an und ermoeglicht das Erstellen eigener Handelsangebote.
 *
 * @author Christopher Jung
 */
@Module(name = "handel")
public class HandelController extends Controller
{
	private TemplateViewResultFactory templateViewResultFactory;
	private ConfigService configService;

	@Autowired
	public HandelController(TemplateViewResultFactory templateViewResultFactory, ConfigService configService)
	{
		this.templateViewResultFactory = templateViewResultFactory;
		this.configService = configService;

		setPageTitle("Handel");
		addPageMenuEntry("Angebote", Common.buildUrl("default"));
		addPageMenuEntry("neues Angebot", Common.buildUrl("add"));
	}

	/**
	 * Speichert ein neues Handelsangebot in der Datenbank.
	 *  @param comm Die Beschreibung
	 * @param needMap Benoetigte Waren
	 * @param haveMap Angebotene Waren
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult enterAction(String comm, @UrlParam(name = "#need") Map<String, Long> needMap, @UrlParam(name = "#have") Map<String, Long> haveMap)
	{
		org.hibernate.Session db = getDB();

		boolean needeverything = false;
		boolean haveeverything = false;
		Cargo need = new Cargo();
		Cargo have = new Cargo();

		// Egal - "-1" (Spezialfall)
		Long needcount = needMap.get("-1");
		Long havecount;

		if (needcount == null || needcount <= 0)
		{
			havecount = haveMap.get("-1");

			if (havecount != null && havecount > 0)
			{
				haveeverything = true;
			}
		}
		else
		{
			needeverything = true;
		}


		ResourceList reslist = Resources.getResourceList().getResourceList();
		for (ResourceEntry res : reslist)
		{
			String name;

			Item item = (Item) db.get(Item.class, res.getId().getItemID());
			if (!item.isHandel())
			{
				continue;
			}
			name = "i" + res.getId().getItemID();

			needcount = needMap.getOrDefault(name, 0L);

			if (needcount == null || needcount <= 0)
			{
				havecount = haveMap.getOrDefault(name, 0L);
				if (havecount != null && havecount > 0)
				{
					have.addResource(res.getId(), havecount);
				}
			}
			else
			{
				need.addResource(res.getId(), needcount);
			}
		}

		Handel entry = new Handel((User) getUser());
		entry.setKommentar(comm);

		if (!needeverything)
		{
			entry.setSucht(need.save());
		}

		if (!haveeverything)
		{
			entry.setBietet(have.save());
		}

		db.persist(entry);

		return new RedirectViewResult("default");
	}

	/**
	 * Zeigt die Seite zur Eingabe eines Handelsangebots an.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine addAction()
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		org.hibernate.Session db = getDB();

		t.setVar("handel.add", 1);

		t.setBlock("_HANDEL", "addresources.listitem", "addresources.list");

		ResourceList reslist = Resources.getResourceList().getResourceList();
		for (ResourceEntry res : reslist)
		{
			Item item = (Item) db.get(Item.class, res.getId().getItemID());
			if (!item.isHandel())
			{
				continue;
			}

			t.setVar("res.id", "i" + res.getId().getItemID(),
					"res.name", res.getName(),
					"res.image", res.getImage());

			t.parse("addresources.list", "addresources.listitem", true);
		}

		t.setVar("trade.runningcost", configService.getValue(WellKnownConfigValue.AD_COST));

		return t;
	}

	/**
	 * Loescht ein Handelsangebot.
	 *
	 * @param entry Die ID des zu loeschenden Handelsangebots
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult deleteAction(@UrlParam(name = "del") Handel entry)
	{
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		String message = null;
		if ((entry != null) && (entry.getWho().equals(user) || hasPermission(WellKnownPermission.HANDEL_ANGEBOTE_LOESCHEN)))
		{
			db.delete(entry);
			message = "Angebot gelöscht";
		}
		else
		{
			addError("Sie haben keine Berechtigung das Angebot zu löschen");
		}

		return new RedirectViewResult("default").withMessage(message);
	}

	/**
	 * Zeigt die vorhandenen Handelsangebote an.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(RedirectViewResult redirect)
	{
		org.hibernate.Session db = getDB();
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();

		t.setVar("handel.view", 1);
		t.setVar("handel.message", redirect != null ? redirect.getMessage() : null);

		int count = 0;

		t.setBlock("_HANDEL", "angebote.listitem", "angebote.list");
		t.setBlock("angebote.listitem", "angebot.want.listitem", "angebot.want.list");
		t.setBlock("angebote.listitem", "angebot.need.listitem", "angebot.need.list");

		List<?> entryList = db.createQuery("from Handel " +
				"where who.vaccount=0 or who.wait4vac!=0 order by time desc")
				.list();
		for (Object anEntryList : entryList)
		{
			Handel entry = (Handel) anEntryList;

			t.setVar("angebot.want.list", "",
					"angebot.need.list", "");

			for (int i = 0; i <= 1; i++)
			{
				String line = (i == 1 ? entry.getBietet() : entry.getSucht());

				if (!line.equals("-1"))
				{
					Cargo cargo = new Cargo(Cargo.Type.AUTO, line);
					cargo.setOption(Cargo.Option.SHOWMASS, false);
					cargo.setOption(Cargo.Option.LINKCLASS, "handelwaren");

					ResourceList reslist = cargo.getResourceList();
					if (i == 0)
					{
						Resources.echoResList(t, reslist, "angebot.want.list");
					}
					else
					{
						Resources.echoResList(t, reslist, "angebot.need.list");
					}
				}
				else
				{
					t.setVar("res.cargo", 1,
							"res.id", -1,
							"res.image", "./data/interface/handel/open.gif");
					if (i == 0)
					{
						t.parse("angebot.want.list", "angebot.want.listitem", true);
					}
					else
					{
						t.parse("angebot.need.list", "angebot.need.listitem", true);
					}
				}
			}

			t.setVar("angebot.id", entry.getId(),
					"angebot.owner", entry.getWho().getId(),
					"angebot.owner.name", Common._title(entry.getWho().getName()),
					"angebot.date", Common.date("d.m.Y H:i:s", entry.getTime()),
					"angebot.description", Common._text(entry.getKommentar()),
					"angebot.description.overflow", Common._text(entry.getKommentar()).length() > 220,
					"angebot.newline", (count % 3 == 0),
					"angebot.endline", (count % 3 == 0) && (count > 0),
					"angebot.showdelete", entry.getWho().equals(user) || hasPermission(WellKnownPermission.HANDEL_ANGEBOTE_LOESCHEN));

			count++;

			t.parse("angebote.list", "angebote.listitem", true);
		}

		t.setBlock("_HANDEL", "emptyangebote.listitem", "emptyangebote.list");
		while (count % 3 != 0)
		{
			t.parse("emptyangebote.list", "emptyangebote.listitem", true);
			count++;
		}

		return t;
	}
}
