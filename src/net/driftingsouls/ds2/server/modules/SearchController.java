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
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Controller;
import net.driftingsouls.ds2.server.ships.Ship;

import java.io.IOException;
import java.util.List;

/**
 * Zeigt alle Objekte an, welche zu einem Suchbegriff passen.
 *
 * @author Christopher Jung
 */
@Module(name = "search")
public class SearchController extends Controller
{
	private static final int MAX_OBJECTS = 25;

	/**
	 * Konstruktor.
	 *
	 * @param context Der zu verwendende Kontext
	 */
	public SearchController(Context context)
	{
		super(context);
	}

	/**
	 * AJAX-Suche mit JSON-Antwort.
	 *
	 * @param search Der Suchbegriff
	 * @param only Falls angegeben der Objekttyp in dem nur gesucht werden soll
	 * @param max Die maximale Anzahl an zu findenden Eintraegen
	 * @throws IOException
	 */
	@Action(ActionType.AJAX)
	public void searchAction(String search, String only, int max) throws IOException
	{
		org.hibernate.Session db = getDB();
		JsonObject json = new JsonObject();

		if (max <= 0 || max > MAX_OBJECTS)
		{
			max = MAX_OBJECTS;
		}

		if (search.length() < 1)
		{
			getResponse().getWriter().append(json.toString());
			return;
		}

		int count = 0;

		if (only.isEmpty() || "bases".equals(only))
		{
			JsonArray baseListObj = new JsonArray();

			List<?> baseList = findBases(db, search, max - count);
			for (Object aBaseList : baseList)
			{
				Base base = (Base) aBaseList;
				JsonObject baseObj = new JsonObject();

				baseObj.addProperty("id", base.getId());
				baseObj.addProperty("name", Common._plaintitle(base.getName()));
				baseObj.addProperty("location", base.getLocation().displayCoordinates(false));

				baseListObj.add(baseObj);

				count++;
			}

			json.add("bases", baseListObj);
		}

		if (only.isEmpty() || "ships".equals(only))
		{
			JsonArray shipListObj = new JsonArray();

			if (count < max)
			{
				List<?> shipList = findShips(db, search, max - count);
				for (Object aShipList : shipList)
				{
					Ship ship = (Ship) aShipList;

					JsonObject shipObj = new JsonObject();

					shipObj.addProperty("id", ship.getId());
					shipObj.addProperty("name", Common._plaintitle(ship.getName()));
					shipObj.addProperty("location", ship.getLocation().displayCoordinates(false));

					JsonObject typeObj = new JsonObject();
					typeObj.addProperty("name", ship.getTypeData().getNickname());
					typeObj.addProperty("picture", ship.getTypeData().getPicture());

					shipObj.add("type", typeObj);

					shipListObj.add(shipObj);

					count++;
				}
			}
			json.add("ships", shipListObj);
		}

		if (only.isEmpty() || "users".equals(only))
		{
			JsonArray userListObj = new JsonArray();

			if (count < max)
			{
				List<?> userList = findUsers(db, search, max - count);
				for (Object anUserList : userList)
				{
					User auser = (User) anUserList;

					JsonObject userObj = new JsonObject();
					userObj.addProperty("id", auser.getId());
					userObj.addProperty("name", Common._title(auser.getName()));
					userObj.addProperty("plainname", auser.getPlainname());

					userListObj.add(userObj);

					count++;
				}
			}
			json.add("users", userListObj);
		}

		json.addProperty("maxObjects", count >= max);

		getResponse().getWriter().append(json.toString());
	}

	@Action(ActionType.AJAX)
	public void defaultAction(String search, String only, int max) throws IOException
	{
		searchAction(search, only, max);
	}

	private List<?> findUsers(org.hibernate.Session db, final String search, int count)
	{
		List<?> userList = db.createQuery("from User where " + (hasPermission("user", "versteckteSichtbar") ? "" : "locate('hide',flags)=0 and ") +
				" (plainname like :search or id like :searchid)")
				.setString("search", "%" + search + "%")
				.setString("searchid", search + "%")
				.setMaxResults(count)
				.list();
		return userList;
	}

	private List<?> findShips(org.hibernate.Session db, final String search, int count)
	{
		List<?> shipList = db.createQuery("from Ship as s left join fetch s.modules where s.owner= :user and (s.name like :search or s.id like :searchid)")
				.setEntity("user", getUser())
				.setString("search", "%" + search + "%")
				.setString("searchid", search + "%")
				.setMaxResults(count)
				.list();
		return shipList;
	}

	private List<?> findBases(org.hibernate.Session db, final String search, int count)
	{
		List<?> baseList = db.createQuery("from Base where owner= :user and (name like :search or id like :searchid)")
				.setEntity("user", getUser())
				.setString("search", "%" + search + "%")
				.setString("searchid", search + "%")
				.setMaxResults(count)
				.list();
		return baseList;
	}
}
