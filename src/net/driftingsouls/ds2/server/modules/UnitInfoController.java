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

import java.util.List;

import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateController;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.units.UnitType;

/**
 * Zeigt Informationen zu Einheiten an.
 */
@Module(name = "unitinfo")
public class UnitInfoController extends TemplateController
{

	/**
	 * Konstruktor.
	 *
	 * @param context Der zu verwendende Kontext
	 */
	public UnitInfoController(Context context)
	{
		super(context);

		setTemplate("unitinfo.html");

		setPageTitle("Einheit");
	}

	/**
	 * Zeigt die Einheitenliste an.
	 */
	@Action(ActionType.DEFAULT)
	public void listAction()
	{
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User) ContextMap.getContext().getActiveUser();
		List<UnitType> unitlist = Common.cast(db.createCriteria(UnitType.class).list());

		t.setVar("unitinfo.list", 1);

		t.setBlock("_UNITINFO", "unitinfo.unitlist.listitem", "unitinfo.unitlist.list");

		for (UnitType unit : unitlist)
		{
			if (!unit.isHidden() || user.isKnownUnit(unit))
			{
				t.setVar("unittype.id", unit.getId(),
						"unittype.name", unit.getName() + ((unit.isHidden() && hasPermission("unittype", "versteckteSichtbar")) ? " [hidden]" : ""),
						"unittype.groesse", Common.ln(unit.getSize()),
						"unittype.picture", unit.getPicture());

				t.parse("unitinfo.unitlist.list", "unitinfo.unitlist.listitem", true);
			}
		}
	}

	/**
	 * Zeigt Details zu einer Einheit an.
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(@UrlParam(name = "unittype") UnitType unittype)
	{
		TemplateEngine t = getTemplateEngine();
		User user = (User) ContextMap.getContext().getActiveUser();

		if (unittype == null)
		{
			t.setVar("unitinfo.message", "Es ist keine Einheit mit dieser Identifikationsnummer bekannt");

			return;
		}
		if (unittype.isHidden() && !user.isKnownUnit(unittype))
		{
			t.setVar("unitinfo.message", "Es ist keine Einheit mit dieser Identifikationsnummer bekannt");

			return;
		}
		String buildcosts = "";
		buildcosts = buildcosts + "<img style=\"vertical-align:middle\" src=\"data/interface/time.gif\" alt=\"\" />" + unittype.getDauer();

		for (ResourceEntry res : unittype.getBuildCosts().getResourceList())
		{
			buildcosts = buildcosts + " <img style=\"vertical-align:middle\" src=\"" + res.getImage() + "\" alt=\"\" />" + res.getCargo1();
		}

		Forschung forschung = Forschung.getInstance(unittype.getRes());
		String forschungstring = "";

		if (forschung != null && forschung.isVisibile(user))
		{
			forschungstring = forschung.getName();
		}
		else if (forschung != null)
		{
			forschungstring = "Unbekannte Technologie";
			if (hasPermission("forschung", "allesSichtbar"))
			{
				forschungstring = forschungstring + " [" + forschung.getID() + "]";
			}
		}

		String name = Common._plaintitle(unittype.getName());

		t.setVar("unitinfo.details", 1,
				"unittype.picture", unittype.getPicture(),
				"unittype.name", name + ((unittype.isHidden() && hasPermission("unittype", "versteckteSichtbar")) ? " [hidden]" : ""),
				"unittype.size", Common.ln(unittype.getSize()),
				"unittype.nahrungcost", Common.ln(unittype.getNahrungCost()),
				"unittype.recost", Common.ln(unittype.getReCost()),
				"unittype.kapervalue", Common.ln(unittype.getKaperValue()),
				"unittype.description", Common._text(unittype.getDescription()),
				"unittype.baukosten", buildcosts,
				"unittype.forschung", forschungstring);
	}
}
