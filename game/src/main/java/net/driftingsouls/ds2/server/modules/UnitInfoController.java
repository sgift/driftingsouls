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
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParam;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.units.UnitType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Zeigt Informationen zu Einheiten an.
 */
@Module(name = "unitinfo")
public class UnitInfoController extends Controller
{
	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public UnitInfoController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;

		setPageTitle("Einheit");
	}

	/**
	 * Zeigt die Einheitenliste an.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine listAction()
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		org.hibernate.Session db = getDB();
		User user = (User) ContextMap.getContext().getActiveUser();
		List<UnitType> unitlist = Common.cast(db.createCriteria(UnitType.class).list());

		t.setVar("unitinfo.list", 1);

		t.setBlock("_UNITINFO", "unitinfo.unitlist.listitem", "unitinfo.unitlist.list");

		for (UnitType unit : unitlist)
		{
			if (!unit.isHidden() || user.isKnownUnit(unit))
			{
				t.setVar("unit.id", unit.getId(),
						"unit.name", unit.getName() + ((unit.isHidden() && hasPermission(WellKnownPermission.UNIT_VERSTECKTE_SICHTBAR)) ? " [hidden]" : ""),
						"unit.groesse", Common.ln(unit.getSize()),
						"unit.picture", unit.getPicture());

				t.parse("unitinfo.unitlist.list", "unitinfo.unitlist.listitem", true);
			}
		}

		return t;
	}

	/**
	 * Zeigt Details zu einer Einheit an.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(@UrlParam(name = "unit") UnitType unittype)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) ContextMap.getContext().getActiveUser();

		if (unittype == null)
		{
			t.setVar("unitinfo.message", "Es ist keine Einheit mit dieser Identifikationsnummer bekannt");

			return t;
		}
		if (unittype.isHidden() && !user.isKnownUnit(unittype))
		{
			t.setVar("unitinfo.message", "Es ist keine Einheit mit dieser Identifikationsnummer bekannt");

			return t;
		}
		String buildcosts = "";
		buildcosts = buildcosts + "<img style=\"vertical-align:middle\" src=\"data/interface/time.gif\" alt=\"\" />" + unittype.getDauer();

		for (ResourceEntry res : unittype.getBuildCosts().getResourceList())
		{
			buildcosts = buildcosts + " <img style=\"vertical-align:middle\" src=\"" + res.getImage() + "\" alt=\"\" />" + res.getCargo1();
		}

		Forschung forschung = unittype.getRes();
		String forschungstring = "";

		if (forschung != null && forschung.isVisibile(user))
		{
			forschungstring = forschung.getName();
		}
		else if (forschung != null)
		{
			forschungstring = "Unbekannte Technologie";
			if (hasPermission(WellKnownPermission.FORSCHUNG_ALLES_SICHTBAR))
			{
				forschungstring = forschungstring + " [" + forschung.getID() + "]";
			}
		}

		String name = Common._plaintitle(unittype.getName());

		t.setVar("unitinfo.details", 1,
				"unit.picture", unittype.getPicture(),
				"unit.name", name + ((unittype.isHidden() && hasPermission(WellKnownPermission.UNIT_VERSTECKTE_SICHTBAR)) ? " [hidden]" : ""),
				"unit.size", Common.ln(unittype.getSize()),
				"unit.nahrungcost", Common.ln(unittype.getNahrungCost()),
				"unit.recost", Common.ln(unittype.getReCost()),
				"unit.kapervalue", Common.ln(unittype.getKaperValue()),
				"unit.description", Common._text(unittype.getDescription()),
				"unit.baukosten", buildcosts,
				"unit.forschung", forschungstring);

		return t;
	}
}
