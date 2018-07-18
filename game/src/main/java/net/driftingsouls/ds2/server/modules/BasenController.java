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
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Die Basenliste.
 * @author Christopher Jung
 */
@Module(name="basen")
public class BasenController extends Controller
{
	private static final Map<String,List<String>> ordmapper = new HashMap<>();
	static {
		ordmapper.put("id", Arrays.asList("id"));
		ordmapper.put("name", Arrays.asList("name"));
		ordmapper.put("type", Arrays.asList("type"));
		ordmapper.put("sys", Arrays.asList("system","x","y"));
		ordmapper.put("bew", Arrays.asList("bewohner"));
		ordmapper.put("e", Arrays.asList("energy"));
	}

	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public BasenController(TemplateViewResultFactory templateViewResultFactory) {
		this.templateViewResultFactory = templateViewResultFactory;

		setPageTitle("Basen");
	}
	
	/**
	 * Zeigt die Liste aller Basen an.
	 * @param l Falls == 1 werden die Cargos der Basen angezeigt
	 * @param ord Das Attribut, nach dem geordnet werden soll
	 * @param order Falls == 1 wird absteigend sortiert
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(Integer l, String ord, Integer order) {
		User user = (User)getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);
		org.hibernate.Session db = getDB();

		String ordSetting = user.getUserValue(WellKnownUserValue.TBLORDER_BASEN_ORDER);
		int orderSetting = user.getUserValue(WellKnownUserValue.TBLORDER_BASEN_ORDER_MODE);
		int lSetting = user.getUserValue(WellKnownUserValue.TBLORDER_BASEN_SHOWCARGO);
		if (ord != null && !ord.isEmpty())
		{
			user.setUserValue(WellKnownUserValue.TBLORDER_BASEN_ORDER, ord);
		}
		else
		{
			ord = ordSetting;
		}

		if (order != null)
		{
			user.setUserValue(WellKnownUserValue.TBLORDER_BASEN_ORDER_MODE, order);
		}
		else
		{
			order = orderSetting;
		}

		if (l != null)
		{
			user.setUserValue(WellKnownUserValue.TBLORDER_BASEN_SHOWCARGO, l);
		}
		else
		{
			l = lSetting;
		}

		t.setVar("global.l", l,
				"global.order", ord,
				"global.order." + ord, 1,
				"global.omode", order);


		List<String> ow;
		if (ordmapper.containsKey(ord))
		{
			ow = ordmapper.get(ord);
		}
		else
		{
			ow = Arrays.asList("id");
		}

		t.setBlock("_BASEN", "bases.listitem", "bases.list");
		t.setBlock("bases.listitem", "bases.mangel.listitem", "bases.mangel.list");
		t.setBlock("bases.listitem", "bases.cargo.listitem", "bases.cargo.list");
		t.setBlock("bases.listitem", "bases.units.listitem", "bases.units.list");

		Criteria criteria = db.createCriteria(Base.class).add(Restrictions.eq("owner", user));
		for (String s : ow)
		{
			criteria.addOrder(order == 1 ? Order.desc(s) : Order.asc(s));
		}

		for (Object aList : criteria.list())
		{
			Base base = (Base) aList;
			BaseStatus basedata = Base.getStatus(base);

			t.setVar("base.id", base.getId(),
					"base.klasse", base.getKlasse().getId(),
					"base.klasse.lrsimage", base.getKlasse().getSmallImage(),
					"base.name", Common._plaintitle(base.getName()),
					"base.system", base.getSystem(),
					"base.x", base.getX(),
					"base.y", base.getY(),
					"base.bewohner", base.getBewohner(),
					"base.e", base.getEnergy(),
					"base.e.diff", basedata.getEnergy(),
					"bases.mangel.list", "",
					"bases.cargo.list", "",
					"bases.units.list", "");
			
			/*
				Mangel + Runden anzeigen
			*/

			Cargo cargo = new Cargo(base.getCargo());
			cargo.addResource(Resources.RE, user.getKonto().longValue());

			ResourceList reslist = basedata.getProduction().getResourceList();
			for (ResourceEntry res : reslist)
			{
				if (res.getCount1() < 0)
				{
					long rounds = -cargo.getResourceCount(res.getId()) / res.getCount1();
					t.setVar("mangel.rounds", rounds,
							"mangel.image", res.getImage(),
							"mangel.plainname", res.getPlainName());

					t.parse("bases.mangel.list", "bases.mangel.listitem", true);
				}
			}

			cargo.substractResource(Resources.RE, user.getKonto().longValue());
			
			/*
				Cargo anzeigen
			*/

			if (l == 1)
			{
				t.setVar("bases.cargo.empty", Common.ln(base.getMaxCargo() - cargo.getMass()));

				reslist = cargo.getResourceList();
				Resources.echoResList(t, reslist, "bases.cargo.list");
			}
			
			/*
			  	Einheiten anzeigen
			 */
			base.getUnits().echoUnitList(t, "bases.units.list", "bases.units.listitem");
			
			/*
				Links auf die einzelnen Gebaeude anzeigen
			*/

			StringBuilder shortcuts = new StringBuilder(10);

			for (Integer bid : basedata.getBuildingLocations().keySet())
			{
				Building building = Building.getBuilding(bid);

				shortcuts.append(building.echoShortcut(getContext(), base, basedata.getBuildingLocations().get(bid), bid));
				shortcuts.append(" ");
			}

			t.setVar("base.shortcuts", shortcuts);

			t.parse("bases.list", "bases.listitem", true);
		}

		return t;
	}

}
