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

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.Rassen;
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
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Der Gebaeudebau.
 *
 * @author Christopher Jung
 */
@Module(name = "build")
public class BuildController extends Controller
{
	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public BuildController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;

		setPageTitle("Bauen");
	}

	private void validiereBasis(Base basis)
	{
		User user = (User) getUser();
		if ((basis == null) || (basis.getOwner() != user))
		{
			throw new ValidierungException("Die angegebene Kolonie existiert nicht", Common.buildUrl("default", "module", "basen"));
		}
	}

	private void validiereGebaeude(Building building)
	{
		if (building == null)
		{
			throw new ValidierungException("Das angegebene Gebäude existiert nicht");
		}
	}

	/**
	 * Baut ein Gebaeute auf der Kolonie.
	 *  @param base Die Basis, auf der das Gebaeude gebaut werden soll
	 * @param build Die ID des zu bauenden Gebaeudes
	 * @param field Die ID des Feldes, auf dem das Gebaeude gebaut werden soll
	 */
	@Action(ActionType.DEFAULT)
	public Object buildAction(@UrlParam(name = "col") Base base, Building build, int field)
	{
		validiereBasis(base);
		validiereGebaeude(build);

		User user = (User) getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);

		t.setVar("base.id", base.getId(),
				"base.name", Common._plaintitle(base.getName()),
				"global.field", field);

		//Darf das Gebaeude ueberhaupt gebaut werden?

		if (field >= base.getWidth() * base.getHeight())
		{
			throw new ValidierungException("Und das elfte Gebot lautet: Du sollst nicht exploiten deines Spieles URL's");
		}

		//Anzahl der Gebaeude berechnen
		Map<Integer, Integer> buildingcount = berechneGebaeudeanzahlDieserBasis(base);

		//Anzahl der Gebaeude pro Spieler berechnen
		Map<Integer, Integer> ownerbuildingcount = berechneGebaeudeanzahlAllerBasen(user);

		//Anzahl der Gebaeude berechnen
		if (build.getPerPlanetCount() != 0)
		{
			if (buildingcount.containsKey(build.getId()) && build.getPerPlanetCount() <= buildingcount.get(build.getId()))
			{
				addError("Sie können dieses Gebäude maximal " + build.getPerPlanetCount() + " Mal pro Asteroid bauen");

				return new RedirectViewResult("default");
			}
		}

		//Anzahl der Gebaeude pro Spieler berechnen
		if (build.getPerUserCount() != 0)
		{
			if (ownerbuildingcount.containsKey(build.getId()) && build.getPerUserCount() <= ownerbuildingcount.get(build.getId()))
			{
				addError("Sie können dieses Gebäude maximal " + build.getPerUserCount() + " Mal insgesamt bauen");

				return new RedirectViewResult("default");
			}
		}

		// Pruefe auf richtiges Terrain
		if (!build.hasTerrain(base.getTerrain()[field]))
		{
			addError("Dieses Gebäude ist nicht auf diesem Terrainfeld baubar.");

			return new RedirectViewResult("default");
		}

		if (base.getBebauung()[field] != 0)
		{
			addError("Es existiert bereits ein Gebäude an dieser Stelle");

			return new RedirectViewResult("default");
		}

		if (build.isUComplex())
		{
			int c = berechneAnzahlUnterirdischerKomplexe(getDB(), buildingcount);
			int grenze = berechneMaximaleAnzahlUnterirdischerKomplexe(base);

			if (c > grenze - 1)
			{
				addError("Es ist nicht möglich, hier mehr als " + grenze + " Unterirdische Komplexe zu installieren");

				return new RedirectViewResult("default");
			}
		}
		if (!Rassen.get().rasse(user.getRace()).isMemberIn(build.getRace()))
		{
			addError("Sie gehören der falschen Spezies an und können dieses Gebäude nicht selbst errichten.");
			return new RedirectViewResult("default");
		}
		if (!user.hasResearched(build.getTechRequired()))
		{
			addError("Sie verfügen nicht über alle nötigen Forschungen um dieses Gebäude zu bauen");

			return new RedirectViewResult("default");
		}

		t.setVar("show.build", 1);

		boolean ok = true;

		//noetige Resourcen berechnen/anzeigen
		Cargo basecargo = base.getCargo();

		t.setBlock("_BUILD", "build.res.listitem", "build.res.list");

		ResourceList compreslist = build.getBuildCosts().compare(basecargo, false);
		for (ResourceEntry compres : compreslist)
		{
			t.setVar("res.image", compres.getImage(),
					"res.owncargo", compres.getCargo2(),
					"res.needcargo", compres.getCargo1(),
					"res.diff", compres.getDiff());

			if (compres.getDiff() > 0)
			{
				ok = false;
			}

			t.parse("build.res.list", "build.res.listitem", true);
		}

		// Alles OK -> bauen
		if (ok)
		{
			Integer[] bebauung = base.getBebauung();
			bebauung[field] = build.getId();
			base.setBebauung(bebauung);

			Integer[] active = base.getActive();
			// Muss das Gebaeude aufgrund von Arbeitermangel deaktiviert werden?
			if ((build.getArbeiter() > 0) && (build.getArbeiter() + base.getArbeiter() > base.getBewohner()))
			{
				active[field] = 0;

				t.setVar("build.lowworker", 1);
			}
			else
			{
				active[field] = 1;
			}

			// Resourcen abziehen
			basecargo.substractCargo(build.getBuildCosts());

			base.setCargo(basecargo);
			base.setArbeiter(base.getArbeiter() + build.getArbeiter());
			base.setActive(active);

			// Evt. muss das Gebaeude selbst noch ein paar Dinge erledigen
			build.build(base, build.getId());
		}

		return t;
	}

	private int berechneMaximaleAnzahlUnterirdischerKomplexe(Base base)
	{
		return (base.getWidth() * base.getHeight()) / 8;
	}

	/**
	 * Zeigt die Liste der baubaren Gebaeude, sortiert nach Kategorien, an.
	 *  @param base Die Basis, auf der das Gebaeude gebaut werden soll
	 * @param field Die ID des Feldes, auf dem das Gebaeude gebaut werden soll
	 * @param cat Die anzuzeigende Kategorie
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(@UrlParam(name = "col") Base base, int field, int cat)
	{
		validiereBasis(base);

		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		t.setVar("base.id", base.getId(),
				"base.name", Common._plaintitle(base.getName()),
				"global.field", field);

		if ((cat < 0) || (cat > 4))
		{
			cat = 0;
		}

		t.setVar("show.buildinglist", 1);

		//Anzahl der Gebaeude berechnen
		Map<Integer, Integer> buildingcount = berechneGebaeudeanzahlDieserBasis(base);

		//Anzahl der Gebaeude pro Spieler berechnen
		Map<Integer, Integer> ownerbuildingcount = berechneGebaeudeanzahlAllerBasen(user);

		Cargo basecargo = base.getCargo();

		//Max UComplex-Gebaeude-Check
		int grenze = berechneMaximaleAnzahlUnterirdischerKomplexe(base);
		int c = berechneAnzahlUnterirdischerKomplexe(db, buildingcount);

		boolean ucomplex = c <= grenze - 1;

		t.setBlock("_BUILD", "buildings.listitem", "buildings.list");
		t.setBlock("buildings.listitem", "buildings.res.listitem", "buildings.res.list");

		//Alle Gebaeude ausgeben
		Iterator<?> buildingIter = db.createQuery("from Building where category=:cat order by name")
				.setInteger("cat", cat)
				.iterate();
		for (; buildingIter.hasNext(); )
		{
			Building building = (Building) buildingIter.next();
			//Existiert bereits die max. Anzahl dieses Geb. Typs auf dem Asti?
			if ((building.getPerPlanetCount() != 0) && buildingcount.containsKey(building.getId()) &&
					(building.getPerPlanetCount() <= buildingcount.get(building.getId())))
			{
				continue;
			}

			if ((building.getPerUserCount() != 0) && ownerbuildingcount.containsKey(building.getId()) &&
					(building.getPerUserCount() <= ownerbuildingcount.get(building.getId())))
			{
				continue;
			}

			if (!ucomplex && building.isUComplex())
			{
				continue;
			}

			if (!user.hasResearched(building.getTechRequired()))
			{
				continue;
			}
			if (!Rassen.get().rasse(user.getRace()).isMemberIn(building.getRace()))
			{
				continue;
			}
			if (!building.hasTerrain(base.getTerrain()[field]))
			{
				continue;
			}
			Cargo buildcosts = building.getBuildCosts();

			boolean ok = true;

			ResourceList compreslist = buildcosts.compare(basecargo, false, true);
			for (ResourceEntry compres : compreslist)
			{
				if (compres.getDiff() > 0)
				{
					ok = false;
				}
			}

			t.setVar("building.picture", building.getPictureForRace(user.getRace()),
					"building.name", Common._plaintitle(building.getName()),
					"building.id", building.getId(),
					"building.buildable", ok,
					"buildings.res.list", "");

			//Kosten
			for (ResourceEntry compres : compreslist)
			{
				t.setVar("res.image", compres.getImage(),
						"res.cargo", compres.getCargo1(),
						"res.diff", compres.getDiff(),
						"res.plainname", compres.getPlainName());

				t.parse("buildings.res.list", "buildings.res.listitem", true);
			}

			t.parse("buildings.list", "buildings.listitem", true);
		}

		return t;
	}

	private Map<Integer, Integer> berechneGebaeudeanzahlDieserBasis(Base base)
	{
		Map<Integer, Integer> buildingcount = new HashMap<>();
		for (int building : base.getBebauung())
		{
			Common.safeIntInc(buildingcount, building);
		}
		return buildingcount;
	}

	private Map<Integer, Integer> berechneGebaeudeanzahlAllerBasen(User user)
	{
		Map<Integer, Integer> ownerbuildingcount = new HashMap<>();

		for (Base abase : user.getBases())
		{
			for (int bid : abase.getBebauung())
			{
				Common.safeIntInc(ownerbuildingcount, bid);
			}
		}
		return ownerbuildingcount;
	}

	private int berechneAnzahlUnterirdischerKomplexe(Session db, Map<Integer, Integer> buildingcount)
	{
		int c = 0;

		Iterator<?> ucBuildingIter = db.createQuery("from Building where ucomplex=true").iterate();
		for (; ucBuildingIter.hasNext(); )
		{
			Building building = (Building) ucBuildingIter.next();
			if (buildingcount.containsKey(building.getId()))
			{
				c += buildingcount.get(building.getId());
			}
		}
		return c;
	}
}
