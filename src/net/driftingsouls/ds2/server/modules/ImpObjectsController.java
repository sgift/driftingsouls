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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.ships.Ship;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Zeigt alle wichtigen Objekte in einem System an wie z.B.
 * eigene Basen, Sprungpunkte usw.
 * @author Christopher Jung
 *
 * @urlparam Integer system Die ID des Sternensystems
 */
@Module(name="impobjects")
public class ImpObjectsController extends DSGenerator {
	private StarSystem system;
	private boolean viewableSystem;

	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public ImpObjectsController(Context context) {
		super(context);

		this.viewableSystem = true;

		parameterNumber("system");
	}

	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
		int sys = getInteger("system");
		org.hibernate.Session db = getDB();

		if( sys == 0 ) {
			sys = 1;
		}

		StarSystem thissystem = (StarSystem)db.get(StarSystem.class, sys);
		this.viewableSystem = thissystem.isVisibleFor(user);

		system = thissystem;

		return true;
	}

	/**
	 * Liefert alle wichtigen Objekte im System als JSON-Objekt zurueck.
	 * @throws IOException
	 */
	@Action(ActionType.AJAX)
	public void jsonAction() throws IOException
	{
		org.hibernate.Session db = getDB();
		JSONObject json = new JSONObject();
		User user = (User)getUser();

		JSONObject sysObj = new JSONObject();
		sysObj.accumulate("name", system.getName());
		sysObj.accumulate("id", system.getID());
		json.accumulate("system", sysObj);

		JSONArray jnListObj = new JSONArray();
		JSONArray postenListObj = new JSONArray();
		if( viewableSystem )
		{
			/*
				Sprungpunkte
			*/

			List<?> jnList = db.createQuery("from JumpNode where system=:sys and hidden=0")
				.setInteger("sys", system.getID())
				.list();
			for( Iterator<?> iter=jnList.iterator(); iter.hasNext(); )
			{
				JumpNode node = (JumpNode)iter.next();

				StarSystem systemout = (StarSystem)db.get(StarSystem.class, node.getSystemOut());

				JSONObject jn = new JSONObject();
				jn.accumulate("x", node.getX());
				jn.accumulate("y", node.getY());
				jn.accumulate("name", node.getName());
				jn.accumulate("target", node.getSystemOut());
				jn.accumulate("targetname", systemout.getName());

				jnListObj.add(jn);
			}

			/*
				Handelsposten
			*/

			List<?> postenList = db.createQuery("select s from Ship s join s.shiptype st left join s.modules sm " +
					"where s.id>0 and s.system=:sys and (locate('tradepost',s.status)!=0 or locate('tradepost', coalesce(sm.flags, st.flags))!=0)")
				.setInteger("sys", system.getID())
				.list();
			for( Iterator<?> iter=postenList.iterator(); iter.hasNext(); )
			{
				Ship posten = (Ship)iter.next();

				if( !posten.isTradepostVisible(user, user.getRelations()) )
				{
					continue;
				}

				JSONObject postenObj = new JSONObject();
				postenObj.accumulate("x", posten.getX());
				postenObj.accumulate("y", posten.getY());
				postenObj.accumulate("name", posten.getName());

				postenListObj.add(postenObj);
			}
		}

		json.accumulate("jumpnodes", jnListObj);
		json.accumulate("posten", postenListObj);

		/*
			Basen
		*/
		JSONArray baseListObj = new JSONArray();

		List<?> baseList = db.createQuery("from Base where owner=:owner and system=:sys")
			.setEntity("owner", getUser())
			.setInteger("sys", system.getID())
			.list();
		for( Iterator<?> iter=baseList.iterator(); iter.hasNext(); )
		{
			Base base = (Base)iter.next();

			JSONObject baseObj = new JSONObject();
			baseObj.accumulate("x", base.getX());
			baseObj.accumulate("y", base.getY());
			baseObj.accumulate("name", base.getName());

			baseListObj.add(baseObj);
		}

		json.accumulate("bases", baseListObj);

		getResponse().getWriter().append(json.toString());
	}

	@Override
	@Action(ActionType.AJAX)
	public void defaultAction() throws IOException {
		jsonAction();
	}
}
