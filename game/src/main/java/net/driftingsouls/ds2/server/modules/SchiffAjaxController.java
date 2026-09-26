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

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.ViewMessage;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidationException;
import net.driftingsouls.ds2.server.ships.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Die Schiffsfunktionen mit JSON/AJAX-Unterstuetzung.
 *
 * @author Christopher Jung
 */
@net.driftingsouls.ds2.server.framework.pipeline.Module(name = "schiffAjax")
public class SchiffAjaxController extends Controller
{
	@ViewModel
	public static class SchiffsLogViewModel
	{
		public String log;
	}

	private final ShipMovementService shipMovementService;
	private final ShipJumpService shipJumpService;

	@Autowired
	public SchiffAjaxController(ShipMovementService shipMovementService, ShipJumpService shipJumpService)
	{
		this.shipMovementService = shipMovementService;
		this.shipJumpService = shipJumpService;
	}

	private void validiereSchiff(Ship ship)
	{
		User user = (User) getUser();

		if ((ship == null) || (ship.getId() < 0) || (ship.getOwner() != user))
		{
			throw new ValidationException("Das angegebene Schiff existiert nicht.");
		}

		if (ship.getBattle() != null)
		{
			throw new ValidationException("Das Schiff ist in einen Kampf verwickelt!");
		}
	}

	/**
	 * Wechselt die Alarmstufe des Schiffes.
	 *
	 * @param schiff Das Schiff
	 * @param alarm Die neue Alarmstufe
	 */
	@Action(ActionType.AJAX)
	public ViewMessage alarmAction(Ship schiff, AlertLevel alarm)
	{
		validiereSchiff(schiff);

		User user = (User) getUser();
		if (user.isNoob())
		{
			return ViewMessage.failure("Du kannst die Alarmstufe nicht ändern solange Du unter Neuspieler-Schutz stehst.");
		}

		ShipTypeData shiptype = schiff.getTypeData();
		if ((shiptype.getShipClass() == ShipClasses.GESCHUETZ) || !shiptype.isMilitary())
		{
			return ViewMessage.failure("Du kannst die Alarmstufe dieses Schiffs nicht ändern.");
		}

		if (alarm != null)
		{
			schiff.setAlarm(alarm);
			schiff.recalculateShipStatus();
		}

		return ViewMessage.success("Alarmstufe erfolgreich geändert");
	}

	/**
	 * Springt durch den angegebenen Sprungpunkt.
	 *
	 * @param schiff Das Schiff
	 * @param sprungpunkt Die ID des Sprungpunkts
	 */
	@Action(ActionType.AJAX)
	public Object springenAction(Ship schiff, JumpNode sprungpunkt)
	{
		validiereSchiff(schiff);

		ShipTypeData shiptype = schiff.getTypeData();
		if ((shiptype.getCost() == 0) || (schiff.getEngine() == 0))
		{
			return ViewMessage.failure("Das Schiff besitzt keinen Antrieb");
		}

		if (sprungpunkt == null)
		{
			return ViewMessage.error("Es wurde kein Sprungpunkt angegeben.");
		}

		ShipJumpService.JumpResult jumpResult = shipJumpService.jumpViaJumpNode(schiff, sprungpunkt);
		SchiffsLogViewModel result = new SchiffsLogViewModel();
		result.log = jumpResult.getMessages().trim();
		return result;
	}

	/**
	 * Benutzt einen an ein Schiff assoziierten Sprungpunkt.
	 *
	 * @param schiff Das Schiff
	 * @param sprungpunktSchiff Die ID des Schiffes mit dem Sprungpunkt
	 */
	@Action(ActionType.AJAX)
	public Object springenViaSchiffAction(Ship schiff, Ship sprungpunktSchiff)
	{
		validiereSchiff(schiff);

		ShipTypeData shiptype = schiff.getTypeData();
		if ((shiptype.getCost() == 0) || (schiff.getEngine() == 0))
		{
			return ViewMessage.failure("Das Schiff besitzt keinen Antrieb.");
		}

		if (sprungpunktSchiff == null)
		{
			return ViewMessage.error("Es wurde kein Sprungpunkt angegeben.");
		}

		shipJumpService.jumpViaShip(schiff, sprungpunktSchiff);
		SchiffsLogViewModel result = new SchiffsLogViewModel();
		result.log = Ship.MESSAGE.getMessage().trim();
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
	public Object fliegeSchiffAction(Ship schiff, int x, int y)
	{
		validiereSchiff(schiff);

		RouteFactory router = new RouteFactory();
		boolean forceLowHeat = false;
		Location from = schiff.getLocation();
		Location to = new Location(from.getSystem(), x, y);

		System.out.println("test1");

		var shipsMovement = new ShipsMovement(schiff.getId(), ContextMap.getContext().getActiveUser().getId());
		var movementResult = shipsMovement.MoveShips(to);

		SchiffsLogViewModel result = new SchiffsLogViewModel();
		result.log = movementResult.getAutomatedMessage();
		return result;

		/*
		List<Waypoint> route = router.findRoute(from, to);
		if (route.isEmpty())
		{
			return ViewMessage.error("Es wurde keine Route nach " + to.displayCoordinates(false) + " gefunden");
		}

		if (route.size() > 1 || route.iterator().next().distance > 1)
		{
			// Bei weiteren Strecken keine Ueberhitzung zulassen
			forceLowHeat = true;
		}

		ShipMovementService.MovementResult movementResult = shipMovementService.move(schiff, route, forceLowHeat);

		SchiffsLogViewModel result = new SchiffsLogViewModel();
		result.log = movementResult.getMessages().trim();*/
	}

}
