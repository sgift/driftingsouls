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
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.JSONUtils;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.AngularGenerator;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParamType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParams;
import net.driftingsouls.ds2.server.ships.RouteFactory;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.Waypoint;
import net.sf.json.JSONObject;

import java.util.List;

/**
 * Die Schiffsfunktionen mit JSON/AJAX-Unterstuetzung.
 * @author Christopher Jung
 *
 */
@net.driftingsouls.ds2.server.framework.pipeline.Module(name="schiffAjax")
@UrlParam(name="schiff", type = UrlParamType.NUMBER, description = "Die ID des anzuzeigenden Schiffes")
public class SchiffAjaxController extends AngularGenerator
{
	private Ship ship = null;
	private ShipTypeData shiptype = null;
	private boolean noob = false;

	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public SchiffAjaxController(Context context) {
		super(context);
	}

	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
		org.hibernate.Session db = getDB();

		int shipid = getInteger("schiff");

		ship = (Ship)db.get(Ship.class, shipid);
		if( (ship == null) || (ship.getId() < 0) || (ship.getOwner() != user) )
		{
			addError("Das angegebene Schiff existiert nicht");
			return false;
		}

		if( ship.getBattle() != null )
		{
			addError("Das Schiff ist in einen Kampf verwickelt!");
			return false;
		}


		shiptype = ship.getTypeData();

		noob = user.isNoob();

		return true;
	}

	/**
	 * Wechselt die Alarmstufe des Schiffes.
	 *
	 */
	@UrlParam(name="alarm", type = UrlParamType.NUMBER, description = "Die neue Alarmstufe")
	@Action(ActionType.AJAX)
	public JSONObject alarmAction() {
		if( noob ) {
			return JSONUtils.failure("Du kannst die Alarmstufe nicht ändern solange du unter GCP-Schutz stehst.");
		}

		if( (shiptype.getShipClass() == ShipClasses.GESCHUETZ) || !shiptype.isMilitary() ) {
			return JSONUtils.failure("Du kannst die Alarmstufe dieses Schiffs nicht ändern.");
		}

		int alarm = getInteger("alarm");

		if( (alarm >= Ship.Alert.GREEN.getCode()) && (alarm <= Ship.Alert.RED.getCode()) ) {
			ship.setAlarm(alarm);
			ship.recalculateShipStatus();
		}

		return JSONUtils.success("Alarmstufe erfolgreich geändert");
	}

	/**
	 * Springt durch den angegebenen Sprungpunkt.
	 *
	 */
	@Action(ActionType.AJAX)
	@UrlParam(name = "sprungpunkt", type = UrlParamType.NUMBER, description = "Die ID des Sprungpunkts")
	public JSONObject springenAction() {
		if( (shiptype.getCost() == 0) || (ship.getEngine() == 0) ) {
			return JSONUtils.failure("Das Schiff besitzt keinen Antrieb");
		}

		int node = getInteger("sprungpunkt");
		if( node == 0 )
		{
			return JSONUtils.error("Es wurde kein Sprungpunkt angegeben.");
		}

		ship.jump(node, false);
		JSONObject result = new JSONObject();
		result.accumulate("log", Ship.MESSAGE.getMessage().trim());
		return result;
	}

	/**
	 * Benutzt einen an ein Schiff assoziierten Sprungpunkt.
	 *
	 */
	@Action(ActionType.AJAX)
	@UrlParam(name = "sprungpunktSchiff", type = UrlParamType.NUMBER, description = "Die ID des Schiffes mit dem Sprungpunkt")
	public JSONObject springenViaSchiffAction() {
		if( (shiptype.getCost() == 0) || (ship.getEngine() == 0) ) {
			return JSONUtils.failure("Das Schiff besitzt keinen Antrieb");
		}

		int knode = getInteger("sprungpunktSchiff");
		if( knode == 0 )
		{
			return JSONUtils.error("Es wurde kein Sprungpunkt angegeben.");
		}

		ship.jump(knode, true);
		JSONObject result = new JSONObject();
		result.accumulate("log", Ship.MESSAGE.getMessage().trim());
		return result;
	}

	/**
	 * Fliegt ein Schiff bzw dessen Flotte zu einem Sektor.
	 */
	@Action(value=ActionType.AJAX)
	@UrlParams({
			@UrlParam(name="x", type=UrlParamType.NUMBER, description = "Die X-Koordinate des Zielsektors"),
			@UrlParam(name="y", type=UrlParamType.NUMBER, description = "Die Y-Koordinate des Zielsektors"),
	})
	public JSONObject fliegeSchiffAction()
	{
		int targetx = getInteger("x");
		int targety = getInteger("y");

		RouteFactory router = new RouteFactory();
		boolean forceLowHeat = false;
		Location from = ship.getLocation();
		Location to = new Location(from.getSystem(), targetx, targety);
		List<Waypoint> route = router.findRoute(from, to);
		if( route.isEmpty() ) {
			return JSONUtils.error("Es wurde keine Route nach "+to.displayCoordinates(false)+" gefunden");
		}

		if( route.size() > 1 || route.iterator().next().distance > 1 )
		{
			// Bei weiteren Strecken keine Ueberhitzung zulassen
			forceLowHeat = true;
		}

		ship.move(route, forceLowHeat, false);

		JSONObject result = new JSONObject();
		result.accumulate("log", Ship.MESSAGE.getMessage().trim());
		return result;
	}

}
