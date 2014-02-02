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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.JSONUtils;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.AngularController;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ValidierungException;
import net.driftingsouls.ds2.server.ships.RouteFactory;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.Waypoint;

import java.util.List;

/**
 * Die Schiffsfunktionen mit JSON/AJAX-Unterstuetzung.
 *
 * @author Christopher Jung
 */
@net.driftingsouls.ds2.server.framework.pipeline.Module(name = "schiffAjax")
public class SchiffAjaxController extends AngularController
{
	/**
	 * Konstruktor.
	 *
	 * @param context Der zu verwendende Kontext
	 */
	public SchiffAjaxController(Context context)
	{
		super(context);
	}

	private void validiereSchiff(Ship ship)
	{
		User user = (User) getUser();

		if ((ship == null) || (ship.getId() < 0) || (ship.getOwner() != user))
		{
			throw new ValidierungException("Das angegebene Schiff existiert nicht");
		}

		if (ship.getBattle() != null)
		{
			throw new ValidierungException("Das Schiff ist in einen Kampf verwickelt!");
		}
	}

	/**
	 * Wechselt die Alarmstufe des Schiffes.
	 *
	 * @param schiff Das Schiff
	 * @param alarm Die neue Alarmstufe
	 */
	@Action(ActionType.AJAX)
	public JsonElement alarmAction(Ship schiff, int alarm)
	{
		validiereSchiff(schiff);

		User user = (User) getUser();
		if (user.isNoob())
		{
			return JSONUtils.failure("Du kannst die Alarmstufe nicht ändern solange du unter GCP-Schutz stehst.");
		}

		ShipTypeData shiptype = schiff.getTypeData();
		if ((shiptype.getShipClass() == ShipClasses.GESCHUETZ) || !shiptype.isMilitary())
		{
			return JSONUtils.failure("Du kannst die Alarmstufe dieses Schiffs nicht ändern.");
		}

		if ((alarm >= Ship.Alert.GREEN.getCode()) && (alarm <= Ship.Alert.RED.getCode()))
		{
			schiff.setAlarm(alarm);
			schiff.recalculateShipStatus();
		}

		return JSONUtils.success("Alarmstufe erfolgreich geändert");
	}

	/**
	 * Springt durch den angegebenen Sprungpunkt.
	 *
	 * @param schiff Das Schiff
	 * @param sprungpunkt Die ID des Sprungpunkts
	 */
	@Action(ActionType.AJAX)
	public JsonElement springenAction(Ship schiff, int sprungpunkt)
	{
		validiereSchiff(schiff);

		ShipTypeData shiptype = schiff.getTypeData();
		if ((shiptype.getCost() == 0) || (schiff.getEngine() == 0))
		{
			return JSONUtils.failure("Das Schiff besitzt keinen Antrieb");
		}

		if (sprungpunkt == 0)
		{
			return JSONUtils.error("Es wurde kein Sprungpunkt angegeben.");
		}

		schiff.jump(sprungpunkt, false);
		JsonObject result = new JsonObject();
		result.addProperty("log", Ship.MESSAGE.getMessage().trim());
		return result;
	}

	/**
	 * Benutzt einen an ein Schiff assoziierten Sprungpunkt.
	 *
	 * @param schiff Das Schiff
	 * @param sprungpunktSchiff Die ID des Schiffes mit dem Sprungpunkt
	 */
	@Action(ActionType.AJAX)
	public JsonElement springenViaSchiffAction(Ship schiff, int sprungpunktSchiff)
	{
		validiereSchiff(schiff);

		ShipTypeData shiptype = schiff.getTypeData();
		if ((shiptype.getCost() == 0) || (schiff.getEngine() == 0))
		{
			return JSONUtils.failure("Das Schiff besitzt keinen Antrieb");
		}

		if (sprungpunktSchiff == 0)
		{
			return JSONUtils.error("Es wurde kein Sprungpunkt angegeben.");
		}

		schiff.jump(sprungpunktSchiff, true);
		JsonObject result = new JsonObject();
		result.addProperty("log", Ship.MESSAGE.getMessage().trim());
		return result;
	}

	/**
	 * Fliegt ein Schiff bzw dessen Flotte zu einem Sektor.
	 *
	 * @param schiff Das Schiff
	 * @param x Die X-Koordinate des Zielsektors
	 * @param y Die Y-Koordinate des Zielsektors
	 */
	@Action(value = ActionType.AJAX)
	public JsonElement fliegeSchiffAction(Ship schiff, int x, int y)
	{
		validiereSchiff(schiff);

		RouteFactory router = new RouteFactory();
		boolean forceLowHeat = false;
		Location from = schiff.getLocation();
		Location to = new Location(from.getSystem(), x, y);
		List<Waypoint> route = router.findRoute(from, to);
		if (route.isEmpty())
		{
			return JSONUtils.error("Es wurde keine Route nach " + to.displayCoordinates(false) + " gefunden");
		}

		if (route.size() > 1 || route.iterator().next().distance > 1)
		{
			// Bei weiteren Strecken keine Ueberhitzung zulassen
			forceLowHeat = true;
		}

		schiff.move(route, forceLowHeat, false);

		JsonObject result = new JsonObject();
		result.addProperty("log", Ship.MESSAGE.getMessage().trim());
		return result;
	}

}
