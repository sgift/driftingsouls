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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParamType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParams;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import org.apache.commons.lang.ArrayUtils;

import java.util.List;
import java.util.Map;

/**
 * Die Basenliste.
 * @author Christopher Jung
 */
@Module(name="basen")
@UrlParams({
		@UrlParam(name="l", type= UrlParamType.NUMBER, description = "Falls == 1 werden die Cargos der Basen angezeigt"),
		@UrlParam(name="ord", description = "Das Attribut, nach dem geordnet werden soll"),
		@UrlParam(name="order", type=UrlParamType.NUMBER, description = "Falls == 1 wird absteigend sortiert")
})
public class BasenController extends TemplateGenerator {
	@SuppressWarnings("unchecked")
	private static final Map<String,String> ordmapper = ArrayUtils.toMap( new String[][]
		{	{"id", "id"}, 
			{"name", "name"},
			{"type", "type"}, 
			{"sys", "system,x,y"},
			{"bew", "bewohner"},
			{"e", "e"} } );
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public BasenController(Context context) {
		super(context);
		
		setTemplate("basen.html");

		setPageTitle("Basen");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		return true;
	}
	
	/**
	 * Zeigt die Liste aller Basen an.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		String ord = user.getUserValue("TBLORDER/basen/order");
		int order = Integer.parseInt(user.getUserValue("TBLORDER/basen/order_mode"));
		int l = Integer.parseInt(user.getUserValue("TBLORDER/basen/showcargo"));
		if( !getString("ord").equals("") && !ord.equals(getString("ord")) ) {
			ord = getString("ord");
			user.setUserValue("TBLORDER/basen/order", ord );
		}

		if( (getInteger("order") != 0) && (order != getInteger("order")) ) {
			order = getInteger("order");
			user.setUserValue("TBLORDER/basen/order_mode", Integer.toString(order));
		}
		
		if( (getInteger("l") != 0) && (l != getInteger("l")) ) {
			l = getInteger("l");
			user.setUserValue("TBLORDER/basen/showcargo", Integer.toString(l));
		}
		
		t.setVar(	"global.l",		l,
					"global.order",	ord,
					"global.order."+ord, 1,
					"global.omode", order );
	
		
		String ow;
		String om;
		if( ordmapper.containsKey(ord) ) {
			ow = ordmapper.get(ord);
		}
		else {
			ow = "id";	
		}
			
		if( order == 1 ) {
			om = "DESC";
		}
		else {
			om = "ASC";
		}
		
		t.setBlock("_BASEN", "bases.listitem", "bases.list");
		t.setBlock("bases.listitem", "bases.mangel.listitem", "bases.mangel.list");
		t.setBlock("bases.listitem", "bases.cargo.listitem", "bases.cargo.list");
		t.setBlock("bases.listitem", "bases.units.listitem", "bases.units.list");
		
		List<?> list = db.createQuery("from Base where owner= :user order by "+ow+" "+om)
			.setEntity("user", user)
			.list();
		for (Object aList : list)
		{
			Base base = (Base) aList;
			BaseStatus basedata = Base.getStatus(base);

			t.setVar("base.id", base.getId(),
					"base.klasse", base.getKlasse(),
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
	}

}
