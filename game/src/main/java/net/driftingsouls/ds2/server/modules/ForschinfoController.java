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

import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.FactoryEntry;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.RedirectViewResult;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;
import java.util.List;

/**
 * Zeigt Details zu einer Forschung an .
 *
 * @author Christopher Jung
 */
@Module(name = "forschinfo")
public class ForschinfoController extends Controller
{
	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public ForschinfoController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;

		setPageTitle("Forschung");
	}

	private void validiereForschung(Forschung forschung)
	{
		User user = (User) getUser();

		if (forschung == null)
		{
			throw new ValidierungException("&Uuml;ber diese Forschung liegen aktuell keine Informationen vor");
		}

		if (!forschung.isVisibile(user) && !hasPermission(WellKnownPermission.FORSCHUNG_ALLES_SICHTBAR))
		{
			throw new ValidierungException("&Uuml;ber diese Forschung liegen aktuell keine Informationen vor");
		}
	}

	/**
	 * Wirft eine Forschung - mit allen davon abhaengigen Forschungen - weg.
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult dropAction(@UrlParam(name = "res") Forschung research)
	{
		validiereForschung(research);

		User user = (User) getUser();
		if (!user.hasResearched(research))
		{
			return null;
		}

		user.dropResearch(research);
		return new RedirectViewResult("default");
	}

	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(@UrlParam(name = "res") Forschung research)
	{
		validiereForschung(research);

		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		// Name und Bild
		t.setVar("tech.name", Common._plaintitle(research.getName()),
				"tech.race.notall", (research.getRace() != -1),
				"tech.id", research.getID(),
				"tech.image", research.getImage(),
				"tech.time", research.getTime(),
				"tech.speccosts", research.getSpecializationCosts());

		// Rasse
		if (research.getRace() != -1)
		{
			String rasse = "???";
			if (Rassen.get().rasse(research.getRace()) != null)
			{
				rasse = Rassen.get().rasse(research.getRace()).getName();
			}

			if (!Rassen.get().rasse(user.getRace()).isMemberIn(research.getRace()))
			{
				rasse = "<span style=\"color:red\">" + rasse + "</span>";
			}

			t.setVar("tech.race.name", rasse);
		}

		// Voraussetzungen
		benoetigteForschungenAnzeigen(t, research);

		// Kosten
		Cargo costs = research.getCosts();
		costs.setOption(Cargo.Option.SHOWMASS, false);

		t.setBlock("_FORSCHINFO", "tech.res.listitem", "tech.res.list");

		ResourceList reslist = costs.getResourceList();
		for (ResourceEntry res : reslist)
		{
			t.setVar("tech.res.item.image", res.getImage(),
					"tech.res.item.cargo", res.getCargo1());

			t.parse("tech.res.list", "tech.res.listitem", true);
		}

		// Ermoeglicht
		ermoeglichteForschungenAnzeigen(db, t, user, research);

		// Beschreibung
		if (research.getDescription().length() > 0)
		{
			int colspan = 4;
			if (research.getRace() != -1)
			{
				colspan = 5;
			}
			t.setVar("tech.descrip", Common._text(research.getDescription()),
					"tech.descrip.colspan", colspan);
		}

		//
		// Gebaeude
		//
		gebaeudeZurForschungAnzeigen(db, t, user, research);

		//
		// Cores
		//
		coresZurForschungAnzeigen(db, t, research);


		//
		// Schiffe
		//
		schiffeZurForschungAnzeigen(db, t, user, research);

		//
		// Munition
		//
		munitionZurForschungAnzeigen(db, t, research);

		if (research.getSpecializationCosts() > 0 && user.hasResearched(research))
		{
			t.setVar("tech.dropable", 1);
		}

		return t;
	}

	private void benoetigteForschungenAnzeigen(TemplateEngine t, Forschung research)
	{
		t.setBlock("_FORSCHINFO", "tech.needs.listitem", "tech.needs.list");
		for (int i = 1; i <= 3; i++)
		{
			t.start_record();

			if ((i > 1) && research.getRequiredResearch(i) != null)
			{
				t.setVar("tech.needs.item.break", true);
			}

			if (research.getRequiredResearch(i) != null && research.isVisibile((User)getUser()))
			{
				Forschung dat = research.getRequiredResearch(i);

				t.setVar("tech.needs.item.researchable", true,
						"tech.needs.item.id", research.getRequiredResearch(i).getID(),
						"tech.needs.item.name", Common._plaintitle(dat.getName()));

				t.parse("tech.needs.list", "tech.needs.listitem", true);
			}
			else if (research.getRequiredResearch(i) != null)
			{
				t.setVar("tech.needs.item.researchable", false);

				t.parse("tech.needs.list", "tech.needs.listitem", true);
			}

			t.stop_record();
			t.clear_record();
		}
	}

	private void ermoeglichteForschungenAnzeigen(Session db, TemplateEngine t, User user, Forschung research)
	{
		t.setBlock("_FORSCHINFO", "tech.allows.listitem", "tech.allows.list");

		boolean entry = false;
		List<?> results = db.createQuery("from Forschung where req1= :fid or req2= :fid or req3= :fid")
				.setInteger("fid", research.getID())
				.list();
		for (Object result : results)
		{
			Forschung res = (Forschung) result;

			if (res.isVisibile(user) ||
					(!res.isVisibile(user) && user.hasResearched(res.getBenoetigteForschungen())))
			{
				t.setVar("tech.allows.item.break", entry,
						"tech.allows.item.id", res.getID(),
						"tech.allows.item.name", Common._plaintitle(res.getName()),
						"tech.allows.item.hidden", false);
				entry = true;

				t.parse("tech.allows.list", "tech.allows.listitem", true);
			}
			else if (hasPermission(WellKnownPermission.FORSCHUNG_ALLES_SICHTBAR) && !res.isVisibile(user))
			{
				t.setVar("tech.allows.item.break", entry,
						"tech.allows.item.id", res.getID(),
						"tech.allows.item.name", Common._plaintitle(res.getName()),
						"tech.allows.item.hidden", true);
				entry = true;

				t.parse("tech.allows.list", "tech.allows.listitem", true);
			}
		}
	}

	private void gebaeudeZurForschungAnzeigen(Session db, TemplateEngine t, User user, Forschung research)
	{
		ResourceList reslist;
		t.setBlock("_FORSCHINFO", "tech.buildings.listitem", "tech.buildings.list");
		t.setBlock("tech.buildings.listitem", "tech.building.buildcosts.listitem", "tech.building.buildcosts.list");
		t.setBlock("tech.buildings.listitem", "tech.building.produces.listitem", "tech.building.produces.list");
		t.setBlock("tech.buildings.listitem", "tech.building.consumes.listitem", "tech.building.consumes.list");
		t.setVar("tech.buildings.list", "");

		boolean firstentry = true;

		Iterator<?> buildingIter = db.createQuery("from Building where techReq=:tech")
				.setParameter("tech", research)
				.iterate();
		for (; buildingIter.hasNext(); )
		{
			Building building = (Building) buildingIter.next();

			t.start_record();

			t.setVar("tech.building.hr", !firstentry,
					"tech.building.picture", building.getPictureForRace(user.getRace()),
					"tech.building.name", Common._plaintitle(building.getName()),
					"tech.building.arbeiter", building.getArbeiter(),
					"tech.building.bewohner", building.getBewohner());

			if (firstentry)
			{
				firstentry = false;
			}

			reslist = building.getBuildCosts().getResourceList();
			Resources.echoResList(t, reslist, "tech.building.buildcosts.list");

			reslist = building.getConsumes().getResourceList();
			Resources.echoResList(t, reslist, "tech.building.consumes.list");

			if (building.getEVerbrauch() > 0)
			{
				t.setVar("res.image", "./data/interface/energie.gif",
						"res.cargo", building.getEVerbrauch());

				t.parse("tech.building.consumes.list", "tech.building.consumes.listitem", true);
			}

			reslist = building.getAllProduces().getResourceList();
			Resources.echoResList(t, reslist, "tech.building.produces.list");

			if (building.getEProduktion() > 0)
			{
				t.setVar("res.image", "./data/interface/energie.gif",
						"res.cargo", building.getEProduktion());

				t.parse("tech.building.produces.list", "tech.building.produces.listitem", true);
			}

			t.parse("tech.buildings.list", "tech.buildings.listitem", true);

			t.stop_record();
			t.clear_record();
		}
	}

	private void coresZurForschungAnzeigen(Session db, TemplateEngine t, Forschung research)
	{
		boolean firstentry;ResourceList reslist;
		t.setBlock("_FORSCHINFO", "tech.cores.listitem", "tech.cores.list");
		t.setBlock("tech.cores.listitem", "tech.core.buildcosts.listitem", "tech.core.buildcosts.list");
		t.setBlock("tech.cores.listitem", "tech.core.consumes.listitem", "tech.core.consumes.list");
		t.setBlock("tech.cores.listitem", "tech.core.produces.listitem", "tech.core.produces.list");
		t.setVar("tech.cores.list", "");

		firstentry = true;
		Iterator<?> coreIter = db.createQuery("from Core where techReq=:tech")
				.setInteger("tech", research.getID())
				.iterate();
		for (; coreIter.hasNext(); )
		{
			Core core = (Core) coreIter.next();

			t.start_record();

			t.setVar("tech.core.astitype", core.getAstiType().getId(),
					"tech.core.astitype.lrsimage", core.getAstiType().getSmallImage(),
					"tech.core.name", Common._plaintitle(core.getName()),
					"tech.core.hr", !firstentry,
					"tech.core.arbeiter", core.getArbeiter(),
					"tech.core.bewohner", core.getBewohner());

			if (firstentry)
			{
				firstentry = false;
			}

			reslist = core.getBuildCosts().getResourceList();
			Resources.echoResList(t, reslist, "tech.core.buildcosts.list");

			reslist = core.getConsumes().getResourceList();
			Resources.echoResList(t, reslist, "tech.core.consumes.list");

			if (core.getEVerbrauch() > 0)
			{
				t.setVar("res.image", "./data/interface/energie.gif",
						"res.cargo", core.getEVerbrauch());

				t.parse("tech.core.consumes.list", "tech.core.consumes.listitem", true);
			}

			reslist = core.getProduces().getResourceList();
			Resources.echoResList(t, reslist, "tech.core.produces.list");

			if (core.getEProduktion() > 0)
			{
				t.setVar("res.image", "./data/interface/energie.gif",
						"res.cargo", core.getEProduktion());

				t.parse("tech.core.produces.list", "tech.core.produces.listitem", true);
			}

			t.parse("tech.cores.list", "tech.cores.listitem", true);

			t.stop_record();
			t.clear_record();
		}
	}

	private void schiffeZurForschungAnzeigen(Session db, TemplateEngine t, User user, Forschung research)
	{
		boolean firstentry;Cargo costs;ResourceList reslist;
		t.setBlock("_FORSCHINFO", "tech.ships.listitem", "tech.ships.list");
		t.setBlock("tech.ships.listitem", "tech.ship.costs.listitem", "tech.ship.costs.list");
		t.setBlock("tech.ships.listitem", "tech.ship.techs.listitem", "tech.ship.techs.list");
		t.setVar("tech.ships.list", "");

		firstentry = true;
		List<?> ships = db.createQuery("from ShipBaubar " +
				"where res1= :fid or res2= :fid or res3= :fid")
				.setInteger("fid", research.getID())
				.list();
		for (Object ship1 : ships)
		{
			ShipBaubar ship = (ShipBaubar) ship1;

			boolean show = true;

			//Schiff sichtbar???
			for( Forschung tmpres : ship.getBenoetigteForschungen() )
			{
				if (!tmpres.isVisibile(user) && (!user.hasResearched(tmpres) || !user.hasResearched(tmpres.getBenoetigteForschungen())))
				{
					show = false;
					break;
				}
			}

			if (!show)
			{
				continue;
			}

			t.start_record();

			ShipTypeData shiptype = ship.getType();

			t.setVar("tech.ship.id", shiptype.getTypeId(),
					"tech.ship.name", Common._plaintitle(shiptype.getNickname()),
					"tech.ship.picture", shiptype.getPicture(),
					"tech.ship.hr", !firstentry,
					"tech.ship.dauer", ship.getDauer(),
					"tech.ship.ekosten", ship.getEKosten(),
					"tech.ship.crew", ship.getCrew());

			if (firstentry)
			{
				firstentry = false;
			}

			costs = ship.getCosts();

			reslist = costs.getResourceList();
			Resources.echoResList(t, reslist, "tech.ship.costs.list");

			//Benoetigt dieses Schiff noch weitere Forschungen???
			if (ship.getBenoetigteForschungen().stream().anyMatch(f -> f.getID() != research.getID()))
			{
				firstentry = true;

				//Es benoetigt weitere!
				t.setVar("tech.ship.techs.list", "");
				for (Forschung benoetigt : ship.getBenoetigteForschungen())
				{
					if (benoetigt.getID() != research.getID())
					{
						t.setVar("tech.ship.tech.break", !firstentry,
								"tech.ship.tech.id", benoetigt.getID(),
								"tech.ship.tech.name", benoetigt.getName());

						if (firstentry)
						{
							firstentry = false;
						}

						t.parse("tech.ship.techs.list", "tech.ship.techs.listitem", true);
					}
				}
			}

			t.parse("tech.ships.list", "tech.ships.listitem", true);

			t.stop_record();
			t.clear_record();
		}
	}

	private void munitionZurForschungAnzeigen(Session db, TemplateEngine t, Forschung research)
	{
		boolean firstentry;ResourceList reslist;
		t.setBlock("_FORSCHINFO", "tech.fac.listitem", "tech.fac.list");
		t.setBlock("tech.fac.listitem", "tech.fac.buildcosts.listitem", "tech.fac.buildcosts.list");
		t.setBlock("tech.fac.listitem", "tech.fac.production.listitem", "tech.fac.production.list");
		t.setVar("tech.fac.list", "");

		firstentry = true;

		List<?> entryList = db.createQuery("from FactoryEntry " +
				"where res1= :fid or res2= :fid or res3= :fid")
				.setInteger("fid", research.getID())
				.list();

		for (Object anEntryList : entryList)
		{
			FactoryEntry facentry = (FactoryEntry) anEntryList;
			t.start_record();

			t.setVar("tech.fac.hr", !firstentry,
					"tech.fac.dauer", facentry.getDauer());

			if (firstentry)
			{
				firstentry = false;
			}

			Cargo buildcosts = facentry.getBuildCosts();
			Cargo production = facentry.getProduce();

			// Produktionskosten
			reslist = buildcosts.getResourceList();
			Resources.echoResList(t, reslist, "tech.fac.buildcosts.list");

			// Produktion
			reslist = production.getResourceList();
			Resources.echoResList(t, reslist, "tech.fac.production.list");

			t.parse("tech.fac.list", "tech.fac.listitem", true);

			t.stop_record();
			t.clear_record();
		}
	}
}
