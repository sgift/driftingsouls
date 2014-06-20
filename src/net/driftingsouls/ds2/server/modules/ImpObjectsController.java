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
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.ships.Ship;

import java.util.ArrayList;
import java.util.List;

/**
 * Zeigt alle wichtigen Objekte in einem System an wie z.B.
 * eigene Basen, Sprungpunkte usw.
 *
 * @author Christopher Jung
 */
@Module(name = "impobjects")
public class ImpObjectsController extends Controller
{
	/**
	 * Konstruktor.
	 *
	 */
	public ImpObjectsController()
	{
		super();
	}

	@ViewModel
	public static class JsonViewModel
	{
		public static class SystemViewModel
		{
			public String name;
			public int id;
		}

		public static class JumpNodeViewModel
		{
			public int x;
			public int y;
			public String name;
			public int target;
			public String targetname;
		}

		public static class HandelspostenViewModel
		{
			public int x;
			public int y;
			public String name;
		}

		public static class BasisViewModel
		{
			public int x;
			public int y;
			public String name;
		}

		public SystemViewModel system;
		public List<JumpNodeViewModel> jumpnodes = new ArrayList<>();
		public List<HandelspostenViewModel> posten = new ArrayList<>();
		public List<BasisViewModel> bases = new ArrayList<>();
	}

	/**
	 * Liefert alle wichtigen Objekte im System als JSON-Objekt zurueck.
	 * @param system Das anzuzeigende Sternensystem
	 */
	@Action(ActionType.AJAX)
	public JsonViewModel jsonAction(StarSystem system)
	{
		org.hibernate.Session db = getDB();
		JsonViewModel json = new JsonViewModel();
		User user = (User) getUser();

		if (system == null)
		{
			system = (StarSystem) db.get(StarSystem.class, 1);
		}

		json.system = new JsonViewModel.SystemViewModel();
		json.system.name = system.getName();
		json.system.id = system.getID();

		if (system.isVisibleFor(user))
		{
			/*
				Sprungpunkte
			*/

			List<?> jnList = db.createQuery("from JumpNode where system=:sys and hidden=false")
					.setInteger("sys", system.getID())
					.list();
			for (Object aJnList : jnList)
			{
				JumpNode node = (JumpNode) aJnList;

				StarSystem systemout = (StarSystem) db.get(StarSystem.class, node.getSystemOut());

				JsonViewModel.JumpNodeViewModel jn = new JsonViewModel.JumpNodeViewModel();
				jn.x = node.getX();
				jn.y = node.getY();
				jn.name = node.getName();
				jn.target = node.getSystemOut();
				jn.targetname = systemout.getName();

				json.jumpnodes.add(jn);
			}

			/*
				Handelsposten
			*/

			List<?> postenList = db.createQuery("select s from Ship s join s.shiptype st left join s.modules sm " +
					"where s.id>0 and s.system=:sys and (locate('tradepost',s.status)!=0 or locate('tradepost', coalesce(sm.flags, st.flags))!=0)")
					.setInteger("sys", system.getID())
					.list();
			for (Object aPostenList : postenList)
			{
				Ship posten = (Ship) aPostenList;

				if (!posten.isTradepostVisible(user, user.getRelations()))
				{
					continue;
				}

				JsonViewModel.HandelspostenViewModel postenObj = new JsonViewModel.HandelspostenViewModel();
				postenObj.x = posten.getX();
				postenObj.y = posten.getY();
				postenObj.name = posten.getName();

				json.posten.add(postenObj);
			}
		}

		/*
			Basen
		*/
		List<?> baseList = db.createQuery("from Base where owner=:owner and system=:sys")
				.setEntity("owner", getUser())
				.setInteger("sys", system.getID())
				.list();
		for (Object aBaseList : baseList)
		{
			Base base = (Base) aBaseList;

			JsonViewModel.BasisViewModel baseObj = new JsonViewModel.BasisViewModel();
			baseObj.x = base.getX();
			baseObj.y = base.getY();
			baseObj.name = base.getName();

			json.bases.add(baseObj);
		}

		return json;
	}

	@Action(ActionType.AJAX)
	public JsonViewModel defaultAction(StarSystem system)
	{
		return jsonAction(system);
	}
}
