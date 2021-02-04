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
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.services.LocationService;
import net.driftingsouls.ds2.server.ships.Ship;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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

	@PersistenceContext
	private EntityManager em;

	private final BBCodeParser bbCodeParser;
	private final LocationService locationService;

	public SearchController(BBCodeParser bbCodeParser, LocationService locationService)
	{
		super();
		this.bbCodeParser = bbCodeParser;
		this.locationService = locationService;
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

		public final List<BaseViewModel> bases = new ArrayList<>();
		public final List<ShipViewModel> ships = new ArrayList<>();
		public final List<UserViewModel> users = new ArrayList<>();
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
			List<Base> baseList = findBases(search, max - count);
			for (Base base: baseList)
			{
				SearchViewModel.BaseViewModel baseObj = new SearchViewModel.BaseViewModel();

				baseObj.id = base.getId();
				baseObj.name = Common._plaintitle(base.getName());
				baseObj.location = locationService.displayCoordinates(base.getLocation(), false);
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
				List<Ship> shipList = findShips(search, max - count);
				for (Ship ship: shipList)
				{

					SearchViewModel.ShipViewModel shipObj = new SearchViewModel.ShipViewModel();

					shipObj.id = ship.getId();
					shipObj.name = Common._plaintitle(ship.getName());
					shipObj.location = locationService.displayCoordinates(ship.getLocation(), false);

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
				List<User> userList = findUsers(search, max - count);
				for (User auser: userList)
				{
					SearchViewModel.UserViewModel userObj = new SearchViewModel.UserViewModel();
					userObj.id = auser.getId();
					userObj.name = Common._title(bbCodeParser, auser.getName());
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

	private List<User> findUsers(final String search, int count)
	{
		return em.createQuery("from User where " + (hasPermission(WellKnownPermission.USER_VERSTECKTE_SICHTBAR) ? "" : "locate('hide',flags)=0 and ") +
				" (plainname like :search or id like :searchid)", User.class)
				.setParameter("search", "%" + search + "%")
				.setParameter("searchid", search + "%")
				.setMaxResults(count)
				.getResultList();
	}

	private List<Ship> findShips(final String search, int count)
	{
		return em.createQuery("from Ship as s left join fetch s.modules where s.owner= :user and (s.name like :search or s.id like :searchid)", Ship.class)
				.setParameter("user", getUser())
				.setParameter("search", "%" + search + "%")
				.setParameter("searchid", search + "%")
				.setMaxResults(count)
				.getResultList();
	}

	private List<Base> findBases(final String search, int count)
	{
		return em.createQuery("from Base where owner= :user and (name like :search or id like :searchid)", Base.class)
				.setParameter("user", getUser())
				.setParameter("search", "%" + search + "%")
				.setParameter("searchid", search + "%")
				.setMaxResults(count)
				.getResultList();
	}
}
