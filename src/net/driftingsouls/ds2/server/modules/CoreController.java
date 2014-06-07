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
import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.RedirectViewResult;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateController;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import java.util.Iterator;

/**
 * Verwaltung der Core eines Asteroiden.
 *
 * @author Christopher Jung
 */
@Module(name = "core")
public class CoreController extends TemplateController
{
	/**
	 * Konstruktor.
	 *
	 */
	public CoreController()
	{
		super();

		setPageTitle("Core");
	}

	private void validiereBasis(Base base)
	{
		User user = (User) getUser();
		if ((base == null) || (base.getOwner() != user))
		{
			throw new ValidierungException("Die angegebene Kolonie existiert nicht", Common.buildUrl("default", "module", "basen"));
		}
	}

	/**
	 * Baut eine neue Core auf dem Asteroiden, sofern noch keine Core auf dem
	 * Asteroiden vorhanden ist.
	 *  @param base Die Basis
	 * @param core Die zu bauende Core
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult buildAction(@UrlParam(name = "col") Base base, @UrlParam(name = "build") Core core)
	{
		validiereBasis(base);

		User user = (User) getUser();
		TemplateEngine t = getTemplateEngine();

		t.setVar("base.id", base.getId());

		if (base.getCore() != null)
		{
			throw new ValidierungException("Sie k&ouml;nnen nur eine Core pro Asteroid bauen", Common.buildUrl("default", "module", "base", "col", base.getId()));
		}

		if (core == null)
		{
			throw new ValidierungException("Der angegebene Core-Typ existiert nicht", Common.buildUrl("default", "module", "base", "col", base.getId()));
		}

		if (!user.hasResearched(core.getTechRequired()))
		{
			throw new ValidierungException("Sie haben nicht alle ben&ouml;tigten Forschungen", Common.buildUrl("default", "module", "base", "col", base.getId()));
		}

		if (core.getAstiType() != base.getKlasse())
		{
			throw new ValidierungException("Diese Core passt nicht in diesen Asteroiden rein", Common.buildUrl("default", "module", "base", "col", base.getId()));
		}

		Cargo costs = core.getBuildCosts();

		t.setVar("core.build", 1);

		//Benoetigte Res ueberpruefen
		Cargo cargo = base.getCargo();

		t.setBlock("_CORE", "build.res.listitem", "build.res.list");

		boolean ok = true;
		ResourceList reslist = costs.compare(cargo, false, true);
		for (ResourceEntry res : reslist)
		{
			if (res.getDiff() > 0)
			{
				ok = false;
			}
			t.setVar("res.image", res.getImage(),
					"res.cargo.available", res.getCargo2(),
					"res.cargo.needed", res.getCargo1(),
					"res.missing", res.getDiff() > 0 ? res.getDiff() : 0);

			t.parse("build.res.list", "build.res.listitem", true);
		}

		// Genuegend Res vorhanden -> Bauen
		if (ok)
		{
			base.setCore(core);

			base.setCoreActive(false);

			if (core.getArbeiter() + base.getArbeiter() > base.getBewohner())
			{
				t.setVar("build.message", "<span style=\"color:#ff0000\">Nicht gen&uuml;gend Arbeiter</span>");
			}
			else
			{
				base.setCoreActive(true);
				t.setVar("build.message", "<span style=\"color:#00ff00\">aktiviert</span>");
			}
			cargo.substractCargo(costs);

			base.setCargo(cargo);
			base.setCore(core);

			if (base.isCoreActive())
			{
				base.setArbeiter(base.getArbeiter() + core.getArbeiter());
				base.setBewohner(base.getBewohner() + core.getBewohner());
			}
		}

		return new RedirectViewResult("default");
	}

	/**
	 * Deaktiviert die Core auf dem Asteroiden, sofern sie noch nicht deaktiviert ist.
	 *
	 * @param base Die Basis
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult deactivateAction(@UrlParam(name = "col") Base base)
	{
		validiereBasis(base);

		TemplateEngine t = getTemplateEngine();
		t.setVar("base.id", base.getId());

		if (!base.isCoreActive())
		{
			return new RedirectViewResult("default");
		}

		Core core = base.getCore();

		base.setArbeiter(base.getArbeiter() - core.getArbeiter());
		base.setCoreActive(false);

		t.setVar("core.message", "<span class=\"error\">Core deaktiviert</span>");

		return new RedirectViewResult("default");
	}

	/**
	 * Aktiviert die Core auf dem Asteroiden, sofern sie noch nicht aktiviert ist und
	 * die Anzahl der freien Arbeiter dazu ausreicht.
	 *
	 * @param base Die Basis
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult activateAction(@UrlParam(name = "col") Base base)
	{
		validiereBasis(base);

		TemplateEngine t = getTemplateEngine();
		t.setVar("base.id", base.getId());

		if (base.isCoreActive())
		{
			return new RedirectViewResult("default");
		}

		Core core = base.getCore();
		if (core.getArbeiter() + base.getArbeiter() > base.getBewohner())
		{
			t.setVar("core.message", "<span style=\"color:#ff0000\">Nicht gen&uuml;gend Arbeiter</span>");
		}
		else if (core.isShutDown() && !base.getOwner().hasResearched(core.getTechRequired()))
		{
			t.setVar("core.message", "<span sytel=\"color:#ff0000\">Sie haben nicht die notwendigen Voraussetzungen um dieses Geb&auml;ude aktivieren zu k&ouml;nnen.</span>");
		}
		else
		{
			base.setArbeiter(base.getArbeiter() + core.getArbeiter());
			base.setCoreActive(true);

			t.setVar("core.message", "<span class=\"ok\">Core aktiviert</span>");
		}

		return new RedirectViewResult("default");
	}

	private void showCore(Base base)
	{
		TemplateEngine t = getTemplateEngine();

		Core core = base.getCore();

		t.setVar("core.astitype", core.getAstiType().getId(),
				"core.astitype.lrsimage", core.getAstiType().getSmallImage(),
				"core.name", Common._plaintitle(core.getName()),
				"core.activated", base.isCoreActive(),
				"core.ever", core.getEVerbrauch(),
				"core.eprodu", core.getEProduktion());

		Cargo produces = core.getProduces();
		Cargo consumes = core.getConsumes();

		t.setBlock("_CORE", "res.listitem", "consumes.res.list");

		ResourceList reslist = consumes.getResourceList();
		for (ResourceEntry res : reslist)
		{
			t.setVar("res.image", res.getImage(),
					"res.cargo", res.getCargo1());

			t.parse("consumes.res.list", "res.listitem", true);
		}

		reslist = produces.getResourceList();
		for (ResourceEntry res : reslist)
		{
			t.setVar("res.image", res.getImage(),
					"res.cargo", res.getCargo1());

			t.parse("produces.res.list", "res.listitem", true);
		}
	}

	private void showCoreBuildList(Base base)
	{
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();

		// Keine Core vorhanden
		Cargo cargo = base.getCargo();

		t.setBlock("_CORE", "cores.listitem", "cores.list");

		Iterator<?> coreIter = db.createQuery("from Core where astiType=:type")
				.setInteger("type", base.getKlasse().getId())
				.iterate();
		for (; coreIter.hasNext(); )
		{
			Core core = (Core) coreIter.next();

			if (!user.hasResearched(core.getTechRequired()))
			{
				continue;
			}

			Cargo costs = core.getBuildCosts();
			Cargo produces = core.getProduces();
			Cargo consumes = core.getConsumes();

			boolean buildable = true;
			ResourceList reslist = costs.compare(cargo, false, true);
			for (ResourceEntry res : reslist)
			{
				if (res.getDiff() > 0)
				{
					buildable = false;
				}
			}

			t.setVar("core.isbuildable", buildable,
					"core.ever", core.getEVerbrauch(),
					"core.name", Common._plaintitle(core.getName()),
					"core.id", core.getId(),
					"core.eprodu", core.getEProduktion(),
					"core.arbeiter", core.getArbeiter(),
					"core.bewohner", core.getBewohner(),
					"costs.res.list", "",
					"consumes.res.list", "",
					"produces.res.list", "");

			t.setBlock("cores.listitem", "costs.res.listitem", "costs.res.list");

			for (ResourceEntry res : reslist)
			{
				t.setVar("res.red", res.getDiff() > 0,
						"res.image", res.getImage(),
						"res.cargo", res.getCargo1());

				t.parse("costs.res.list", "costs.res.listitem", true);
			}

			reslist = consumes.getResourceList();
			for (ResourceEntry res : reslist)
			{
				t.setVar("res.image", res.getImage(),
						"res.cargo", res.getCargo1());

				t.parse("consumes.res.list", "costs.res.listitem", true);
			}

			reslist = produces.getResourceList();
			for (ResourceEntry res : reslist)
			{
				t.setVar("res.image", res.getImage(),
						"res.cargo", res.getCargo1());

				t.parse("produces.res.list", "costs.res.listitem", true);
			}
			t.parse("cores.list", "cores.listitem", true);
		}
	}

	/**
	 * Zeigt entweder die Liste aller auf dem Asteroiden im Moment
	 * baubaren Cores an (wenn noch keine Core gebaut wurde) oder
	 * die Daten zur aktuellen Core.
	 *
	 * @param base Die Basis
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(@UrlParam(name = "col") Base base)
	{
		validiereBasis(base);

		TemplateEngine t = getTemplateEngine();
		t.setVar("base.id", base.getId());
		t.setVar("base.core", base.getCore() != null ? base.getCore().getId() : 0);

		if (base.getCore() != null)
		{
			showCore(base);
		}
		else
		{
			showCoreBuildList(base);
		}
	}
}
