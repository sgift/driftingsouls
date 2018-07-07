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
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.ships.Ship;

import java.util.ArrayList;
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
	 */
	public SearchController()
	{
		super();
	}

	@ViewModel
	public static class SearchViewModel
	{
		public static class BaseViewModel
		{
			public int id;
			public String name;
			public String location;
			public int klasse;
			public String image;
		}

		public static class ShipTypeViewModel
		{
			public String name;
			public String picture;
		}

		public static class ShipViewModel
		{
			public int id;
			public String name;
			public String location;
			public ShipTypeViewModel type;
		}

		public static class UserViewModel
		{
			public int id;
			public String name;
			public String plainname;
		}

		public List<BaseViewModel> bases = new ArrayList<>();
		public List<ShipViewModel> ships = new ArrayList<>();
		public List<UserViewModel> users = new ArrayList<>();
		public boolean maxObjects;
	}

	/**
	 * AJAX-Suche mit JSON-Antwort.
	 *
	 * @param search Der Suchbegriff
	 * @param only Falls angegeben der Objekttyp in dem nur gesucht werden soll
	 * @param max Die maximale Anzahl an zu findenden Eintraegen
	 */
	@Action(ActionType.AJAX)
	public SearchViewModel searchAction(String search, String only, int max)
	{
		org.hibernate.Session db = getDB();
		SearchViewModel result = new SearchViewModel();

		if (max <= 0 || max > MAX_OBJECTS)
		{
			max = MAX_OBJECTS;
		}

		if (search.length() < 1)
		{
			return result;
		}

		int count = 0;

		if (only.isEmpty() || "bases".equals(only))
		{
			List<?> baseList = findBases(db, search, max - count);
			for (Object aBaseList : baseList)
			{
				Base base = (Base) aBaseList;
				SearchViewModel.BaseViewModel baseObj = new SearchViewModel.BaseViewModel();

				baseObj.id = base.getId();
				baseObj.name = Common._plaintitle(base.getName());
				baseObj.location = base.getLocation().displayCoordinates(false);
				baseObj.klasse = base.getKlasse().getId();
				baseObj.image = base.getKlasse().getSmallImage();

				result.bases.add(baseObj);

				count++;
			}
		}

		if (only.isEmpty() || "ships".equals(only))
		{
			if (count < max)
			{
				List<?> shipList = findShips(db, search, max - count);
				for (Object aShipList : shipList)
				{
					Ship ship = (Ship) aShipList;

					SearchViewModel.ShipViewModel shipObj = new SearchViewModel.ShipViewModel();

					shipObj.id = ship.getId();
					shipObj.name = Common._plaintitle(ship.getName());
					shipObj.location = ship.getLocation().displayCoordinates(false);

					shipObj.type = new SearchViewModel.ShipTypeViewModel();
					shipObj.type.name = ship.getTypeData().getNickname();
					shipObj.type.picture = ship.getTypeData().getPicture();

					result.ships.add(shipObj);

					count++;
				}
			}
		}

		if (only.isEmpty() || "users".equals(only))
		{
			if (count < max)
			{
				List<?> userList = findUsers(db, search, max - count);
				for (Object anUserList : userList)
				{
					User auser = (User) anUserList;

					SearchViewModel.UserViewModel userObj = new SearchViewModel.UserViewModel();
					userObj.id = auser.getId();
					userObj.name = Common._title(auser.getName());
					userObj.plainname = auser.getPlainname();

					result.users.add(userObj);

					count++;
				}
			}
		}

		result.maxObjects = count >= max;

		return result;
	}

	@Action(ActionType.AJAX)
	public SearchViewModel defaultAction(String search, String only, int max)
	{
		return searchAction(search, only, max);
	}

	private List<?> findUsers(org.hibernate.Session db, final String search, int count)
	{
		return db.createQuery("from User where " + (hasPermission(WellKnownPermission.USER_VERSTECKTE_SICHTBAR) ? "" : "locate('hide',flags)=0 and ") +
				" (plainname like :search or id like :searchid)")
				.setString("search", "%" + search + "%")
				.setString("searchid", search + "%")
				.setMaxResults(count)
				.list();
	}

	private List<?> findShips(org.hibernate.Session db, final String search, int count)
	{
		return db.createQuery("from Ship as s left join fetch s.modules where s.owner= :user and (s.name like :search or s.id like :searchid)")
				.setEntity("user", getUser())
				.setString("search", "%" + search + "%")
				.setString("searchid", search + "%")
				.setMaxResults(count)
				.list();
	}

	private List<?> findBases(org.hibernate.Session db, final String search, int count)
	{
		return db.createQuery("from Base where owner= :user and (name like :search or id like :searchid)")
				.setEntity("user", getUser())
				.setString("search", "%" + search + "%")
				.setString("searchid", search + "%")
				.setMaxResults(count)
				.list();
	}
}
