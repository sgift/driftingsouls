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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Controller;
import net.driftingsouls.ds2.server.ships.Ship;

import java.io.IOException;
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
	 * @param context Der zu verwendende Kontext
	 */
	public ImpObjectsController(Context context)
	{
		super(context);
	}

	/**
	 * Liefert alle wichtigen Objekte im System als JSON-Objekt zurueck.
	 * @param system Das anzuzeigende Sternensystem
	 * @throws IOException
	 */
	@Action(ActionType.AJAX)
	public void jsonAction(StarSystem system) throws IOException
	{
		org.hibernate.Session db = getDB();
		JsonObject json = new JsonObject();
		User user = (User) getUser();

		if (system == null)
		{
			system = (StarSystem) db.get(StarSystem.class, 1);
		}

		JsonObject sysObj = new JsonObject();
		sysObj.addProperty("name", system.getName());
		sysObj.addProperty("id", system.getID());
		json.add("system", sysObj);

		JsonArray jnListObj = new JsonArray();
		JsonArray postenListObj = new JsonArray();
		if (system.isVisibleFor(user))
		{
			/*
				Sprungpunkte
			*/

			List<?> jnList = db.createQuery("from JumpNode where system=:sys and hidden=0")
					.setInteger("sys", system.getID())
					.list();
			for (Object aJnList : jnList)
			{
				JumpNode node = (JumpNode) aJnList;

				StarSystem systemout = (StarSystem) db.get(StarSystem.class, node.getSystemOut());

				JsonObject jn = new JsonObject();
				jn.addProperty("x", node.getX());
				jn.addProperty("y", node.getY());
				jn.addProperty("name", node.getName());
				jn.addProperty("target", node.getSystemOut());
				jn.addProperty("targetname", systemout.getName());

				jnListObj.add(jn);
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

				JsonObject postenObj = new JsonObject();
				postenObj.addProperty("x", posten.getX());
				postenObj.addProperty("y", posten.getY());
				postenObj.addProperty("name", posten.getName());

				postenListObj.add(postenObj);
			}
		}

		json.add("jumpnodes", jnListObj);
		json.add("posten", postenListObj);

		/*
			Basen
		*/
		JsonArray baseListObj = new JsonArray();

		List<?> baseList = db.createQuery("from Base where owner=:owner and system=:sys")
				.setEntity("owner", getUser())
				.setInteger("sys", system.getID())
				.list();
		for (Object aBaseList : baseList)
		{
			Base base = (Base) aBaseList;

			JsonObject baseObj = new JsonObject();
			baseObj.addProperty("x", base.getX());
			baseObj.addProperty("y", base.getY());
			baseObj.addProperty("name", base.getName());

			baseListObj.add(baseObj);
		}

		json.add("bases", baseListObj);

		getResponse().getWriter().append(json.toString());
	}

	@Action(ActionType.AJAX)
	public void defaultAction(StarSystem system) throws IOException
	{
		jsonAction(system);
	}
}
