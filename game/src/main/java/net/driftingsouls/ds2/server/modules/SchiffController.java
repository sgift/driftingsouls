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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.ModuleEntry;
import net.driftingsouls.ds2.server.cargo.modules.ModuleItemModule;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.RedirectViewResult;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.modules.schiffplugins.ADocksDefault;
import net.driftingsouls.ds2.server.modules.schiffplugins.CargoDefault;
import net.driftingsouls.ds2.server.modules.schiffplugins.Handelsposten;
import net.driftingsouls.ds2.server.modules.schiffplugins.JDocksDefault;
import net.driftingsouls.ds2.server.modules.schiffplugins.JumpdriveShivan;
import net.driftingsouls.ds2.server.modules.schiffplugins.NavigationDefault;
import net.driftingsouls.ds2.server.modules.schiffplugins.Parameters;
import net.driftingsouls.ds2.server.modules.schiffplugins.SchiffPlugin;
import net.driftingsouls.ds2.server.modules.schiffplugins.SensorsDefault;
import net.driftingsouls.ds2.server.modules.schiffplugins.UnitsDefault;
import net.driftingsouls.ds2.server.modules.schiffplugins.WerftDefault;
import net.driftingsouls.ds2.server.services.HandelspostenService;
import net.driftingsouls.ds2.server.ships.Alarmstufe;
import net.driftingsouls.ds2.server.ships.SchiffSprungService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Die Schiffsansicht.
 *
 * @author Christopher Jung
 */
@net.driftingsouls.ds2.server.framework.pipeline.Module(name = "schiff")
public class SchiffController extends Controller
{
	private Log log = LogFactory.getLog(SchiffController.class);
	private TemplateViewResultFactory templateViewResultFactory;
	private SchiffSprungService schiffSprungService;
	private HandelspostenService handelspostenService;

	@Autowired
	public SchiffController(TemplateViewResultFactory templateViewResultFactory,
							SchiffSprungService schiffSprungService,
							HandelspostenService handelspostenService)
	{
		this.templateViewResultFactory = templateViewResultFactory;
		this.schiffSprungService = schiffSprungService;
		this.handelspostenService = handelspostenService;

		setPageTitle("Schiff");
	}

	private String genSubColor(int value, int defvalue)
	{
		if (defvalue == 0)
		{
			return "green";
		}

		if (value < defvalue / 2)
		{
			return "red";
		}
		else if (value < defvalue)
		{
			return "yellow";
		}
		else
		{
			return "green";
		}
	}

	private void validiereSchiff(Ship ship)
	{
		User user = (User) getUser();
		if ((ship == null) || (ship.getId() < 0) || (ship.getOwner() != user))
		{
			throw new ValidierungException("Das angegebene Schiff existiert nicht", Common.buildUrl("default", "module", "schiffe"));
		}

		if (ship.getBattle() != null)
		{
			throw new ValidierungException("Das Schiff ist in einen Kampf verwickelt (hier klicken um zu diesem zu gelangen)!", Common.buildUrl("default", "module", "angriff", "battle", ship.getBattle().getId(), "ship", ship.getId()));
		}
	}

	private Map<String, SchiffPlugin> ermittlePluginsFuer(ShipTypeData shiptype, Ship communicate)
	{
		Context context = ContextMap.getContext();
		Map<String, SchiffPlugin> pluginMapper = new LinkedHashMap<>();
		pluginMapper.put("navigation", context.getBean(NavigationDefault.class, null));
		pluginMapper.put("cargo", context.getBean(CargoDefault.class, null));

		if( communicate != null )
		{
			pluginMapper.put("handelsposten", context.getBean(Handelsposten.class, null));
		}

		if (shiptype.getWerft() != 0)
		{
			pluginMapper.put("werft", context.getBean(WerftDefault.class, null));
		}

		if (shiptype.hasFlag(ShipTypeFlag.JUMPDRIVE_SHIVAN))
		{
			pluginMapper.put("jumpdrive", context.getBean(JumpdriveShivan.class, null));
		}

		pluginMapper.put("sensors", context.getBean(SensorsDefault.class, null));

		if (shiptype.getADocks() > 0)
		{
			pluginMapper.put("adocks", context.getBean(ADocksDefault.class, null));
		}

		if (shiptype.getJDocks() > 0)
		{
			pluginMapper.put("jdocks", context.getBean(JDocksDefault.class, null));
		}

		if (shiptype.getUnitSpace() > 0)
		{
			pluginMapper.put("units", context.getBean(UnitsDefault.class, null));
		}
		return pluginMapper;
	}

	/**
	 * Wechselt die Alarmstufe des Schiffes.
	 *  @param ship Die ID des anzuzeigenden Schiffes
	 * @param alarm Die neue Alarmstufe
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult alarmAction(Ship ship, Alarmstufe alarm)
	{
		validiereSchiff(ship);

		User user = (User)getUser();
		if (user.isNoob())
		{
			return new RedirectViewResult("default");
		}

		ShipTypeData shiptype = ship.getTypeData();
		if ((shiptype.getShipClass() == ShipClasses.GESCHUETZ) || !shiptype.isMilitary())
		{
			return new RedirectViewResult("default");
		}

		String message = null;
		if (alarm != null)
		{
			ship.setAlarm(alarm);

			message = "Alarmstufe erfolgreich geändert<br />";
		}

		ship.recalculateShipStatus();

		return new RedirectViewResult("default").withMessage(message);
	}

	/**
	 * Uebergibt das Schiff an einen anderen Spieler.
	 *  @param ship Die ID des anzuzeigenden Schiffes
	 * @param newownerID Die ID des neuen Besitzers
	 * @param conf 1, falls die Sicherheitsabfrage positiv bestaetigt wurde
	 */
	@Action(ActionType.DEFAULT)
	public Object consignAction(Ship ship, @UrlParam(name = "newowner") String newownerID, int conf)
	{
		validiereSchiff(ship);

		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		User newowner = User.lookupByIdentifier(newownerID);
		if (newowner == null)
		{
			return new RedirectViewResult("default").withMessage("<span style=\"color:red\">Der Spieler existiert nicht</span><br />");
		}

		if (conf == 0)
		{
			String text = "<span style=\"color:white\">Wollen sie das Schiff " + Common._plaintitle(ship.getName()) + " (" + ship.getId() + ") wirklich an " + newowner.getProfileLink() + " &uuml;bergeben?</span><br />";
			text += "<a class=\"ok\" href=\"" + Common.buildUrl("consign", "ship", ship.getId(), "conf", 1, "newowner", newowner.getId()) + "\">&Uuml;bergeben</a></span><br />";

			return new RedirectViewResult("default").withMessage(text);
		}

		ShipFleet fleet = ship.getFleet();

		boolean result = ship.consign(newowner, false);

		if (result)
		{
			return new RedirectViewResult("default").withMessage(Ship.MESSAGE.getMessage());
		}
		else
		{
			TemplateEngine t = templateViewResultFactory.createFor(this);

			ShipTypeData shiptype = ship.getTypeData();
			String msg = "Ich habe dir die [ship="+ship.getId()+"]" + ship.getName() + "[/ship], ein Schiff der " + shiptype.getNickname() + "-Klasse, &uuml;bergeben\nSie steht bei " + ship.getLocation().displayCoordinates(false);
			PM.send(user, newowner.getId(), "Schiff &uuml;bergeben", msg);

			String consMessage = Ship.MESSAGE.getMessage();
			t.setVar("ship.message", (!consMessage.equals("") ? consMessage + "<br />" : "") + "<span style=\"color:green\">Das Schiff wurde erfolgreich an " + newowner.getProfileLink() + " &uuml;bergeben</span><br />");

			if (fleet != null)
			{
				long fleetcount = (Long) db.createQuery("select count(*) from Ship where id>0 and fleet=:fleet")
						.setEntity("fleet", fleet)
						.iterate().next();

				if (fleetcount < 3)
				{
					db.createQuery("update Ship set fleet=null where id>0 and fleet=:fleet")
							.setEntity("fleet", fleet)
							.executeUpdate();

					db.delete(fleet);
				}
			}

			return t;
		}
	}

	/**
	 * Zerstoert das Schiff.
	 * @param ship Die ID des anzuzeigenden Schiffes
	 *
	 */
	@Action(ActionType.DEFAULT)
	public Object destroyAction(Ship ship, int conf)
	{
		validiereSchiff(ship);

		if (ship.isNoSuicide())
		{
			return new RedirectViewResult("default").withMessage("<span style=\"color:red\">Dieses Schiff kann sich nicht selbstzerstören.</span><br />");
		}

		if (conf == 0)
		{
			String text = "<span style=\"color:white\">Wollen sie Selbstzerst&ouml;rung des Schiffes " + Common._plaintitle(ship.getName()) + " (" + ship.getId() + ") wirklich ausführen?</span><br />\n";
			text += "<a class=\"error\" href=\"" + Common.buildUrl("destroy", "ship", ship.getId(), "conf", 1) + "\">Selbstzerstörung</a></span><br />";
			return new RedirectViewResult("default").withMessage(text);
		}

		ship.destroy();

		TemplateEngine t = templateViewResultFactory.createFor(this);
		t.setVar("ship.message", "<span style=\"color:white\">Das Schiff hat sich selbstzerst&ouml;rt</span><br />");
		return t;
	}

	/**
	 * Springt durch den angegebenen Sprungpunkt.
	 *  @param ship Die ID des anzuzeigenden Schiffes
	 * @param node Die ID des Sprungpunkts
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult jumpAction(Ship ship, JumpNode node)
	{
		validiereSchiff(ship);

		ShipTypeData shiptype = ship.getTypeData();
		if ((shiptype.getCost() == 0) || (ship.getEngine() == 0))
		{
			return new RedirectViewResult("default");
		}

		String message = null;
		if (node != null)
		{
			SchiffSprungService.SprungErgebnis ergebnis = schiffSprungService.sprungViaSprungpunkt(ship, node);
			message = ergebnis.getMeldungen();
		}

		return new RedirectViewResult("default").withMessage(message);
	}

	/**
	 * Benutzt einen an ein Schiff assoziierten Sprungpunkt.
	 *  @param ship Die ID des anzuzeigenden Schiffes
	 * @param knode Die ID des Schiffes mit dem Sprungpunkt
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult kjumpAction(Ship ship, Ship knode)
	{
		validiereSchiff(ship);

		ShipTypeData shiptype = ship.getTypeData();
		if ((shiptype.getCost() == 0) || (ship.getEngine() == 0))
		{
			return new RedirectViewResult("default");
		}

		String message = null;
		if (knode != null)
		{
			SchiffSprungService.SprungErgebnis ergebnis = schiffSprungService.sprungViaSchiff(ship, knode);
			message = ergebnis.getMeldungen();
		}

		return new RedirectViewResult("default").withMessage(message);
	}

	/**
	 * Benennt das Schiff um.
	 *  @param ship Die ID des anzuzeigenden Schiffes
	 * @param newname Der neue Name des Schiffes
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult renameAction(Ship ship, String newname)
	{
		validiereSchiff(ship);

		ship.setName(newname);

		return new RedirectViewResult("default").withMessage("Name zu " + Common._plaintitle(newname) + " geändert<br />");
	}

	/**
	 * Fuehrt Aktionen eines Plugins aus. Plugin-spezifische
	 * Parameter haben die Form $PluginName_ops[$ParameterName].
	 *  @param ship Die ID des anzuzeigenden Schiffes
	 * @param plugin Der Name des Plugins
	 */
	@Action(ActionType.DEFAULT)
	public Object pluginAction(Ship ship, String plugin, Ship communicate) throws ReflectiveOperationException
	{
		validiereSchiff(ship);

		ShipTypeData shiptype = ship.getTypeData();
		Parameters caller = new Parameters();
		caller.controller = this;
		caller.pluginId = plugin;
		caller.ship = ship;
		caller.shiptype = shiptype;
		caller.offizier = ship.getOffizier();

		Map<String,Object> parameters = new HashMap<>();
		parameters.put("caller", caller);
		parameters.put("communicate", communicate);

		Map<String, SchiffPlugin> pluginMapper = ermittlePluginsFuer(shiptype, communicate);
		if (!pluginMapper.containsKey(plugin))
		{
			return new RedirectViewResult("default");
		}

		String ergebnis = (String)rufeAlsSubActionAuf(plugin+"_ops", pluginMapper.get(plugin), "action", parameters);

		if( ship.isDestroyed() )
		{
			TemplateEngine t = templateViewResultFactory.createFor(this);
			t.setVar("ship.message", ergebnis);
			return t;
		}

		return new RedirectViewResult("default").withMessage(ergebnis);
	}

	/**
	 * Landet die angegebenen Schiffe auf dem aktuellen Schiff.
	 *  @param ship Die ID des anzuzeigenden Schiffes
	 * @param shipIdList Eine mit | separierte Liste an Schiffs-IDs
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult landAction(Ship ship, @UrlParam(name = "shiplist") String shipIdList)
	{
		validiereSchiff(ship);

		if (shipIdList.equals(""))
		{
			return new RedirectViewResult("default").withMessage("Es wurden keine Schiffe angegeben");
		}

		int[] shipidlist = Common.explodeToInt("|", shipIdList);
		Ship[] shiplist = new Ship[shipidlist.length];
		for (int i = 0; i < shipidlist.length; i++)
		{
			Ship aship = (Ship) getDB().get(Ship.class, shipidlist[i]);
			if (aship == null)
			{
				addError("Eines der angegebenen Schiffe existiert nicht");
				return new RedirectViewResult("default");
			}
			shiplist[i] = aship;
		}

		ship.land(shiplist);

		return new RedirectViewResult("default").withMessage(Ship.MESSAGE.getMessage());
	}

	/**
	 * Startet die angegebenen Schiffe vom aktuellen Schiff.
	 *  @param ship Die ID des anzuzeigenden Schiffes
	 * @param shipIdList Eine mit | separierte Liste von Schiffs-IDs
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult startAction(Ship ship, @UrlParam(name = "shiplist") String shipIdList)
	{
		validiereSchiff(ship);

		if (shipIdList.equals(""))
		{
			return new RedirectViewResult("default").withMessage("Es wurden keine Schiffe angegeben");
		}

		int[] shipidlist = Common.explodeToInt("|", shipIdList);
		Ship[] shiplist = new Ship[shipidlist.length];
		for (int i = 0; i < shipidlist.length; i++)
		{
			Ship aship = (Ship) getDB().get(Ship.class, shipidlist[i]);
			if (aship == null)
			{
				addError("Eines der angegebenen Schiffe existiert nicht");
				return new RedirectViewResult("default");
			}
			shiplist[i] = aship;
		}

		ship.start(shiplist);

		return new RedirectViewResult("default").withMessage(Ship.MESSAGE.getMessage());
	}

	/**
	 * Dockt die angegebenen Schiffe an das aktuelle Schiff an.
	 *  @param ship Die ID des anzuzeigenden Schiffes
	 * @param shipIdList Eine mit | separierte Liste von Schiffs-IDs
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult aufladenAction(Ship ship, @UrlParam(name = "tar") String shipIdList)
	{
		validiereSchiff(ship);

		User user = (User) getUser();

		if (shipIdList.equals(""))
		{
			return new RedirectViewResult("default").withMessage("Es wurden keine Schiffe angegeben");
		}

		int[] shipidlist = Common.explodeToInt("|", shipIdList);

		org.hibernate.Session db = getDB();

		List<?> dockedList = db.createQuery("from Ship where id>0 and id in (" + Common.implode(",", shipidlist) + ") and docked!=''")
				.list();
		for (Object aDockedList : dockedList)
		{
			Ship docked = (Ship) aDockedList;

			if (docked.getOwner() != user)
			{
				addError("Eines der Schiffe gehoert nicht ihnen");
				return new RedirectViewResult("default");
			}

			Ship targetShip = docked.getBaseShip();

			targetShip.undock(docked);
		}

		Ship[] shiplist = new Ship[shipidlist.length];
		for (int i = 0; i < shipidlist.length; i++)
		{
			Ship aship = (Ship) getDB().get(Ship.class, shipidlist[i]);
			if (aship == null)
			{
				addError("Eines der angegebenen Schiffe existiert nicht");
				return new RedirectViewResult("default");
			}
			shiplist[i] = aship;
		}

		ship.dock(shiplist);

		return new RedirectViewResult("default").withMessage(Ship.MESSAGE.getMessage());
	}

	/**
	 * Dockt die angegebenen Schiffe vom aktuellen Schiff ab.
	 *  @param ship Die ID des anzuzeigenden Schiffes
	 * @param shipIdList Eine mit | separierte Liste von Schiffs-IDs
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult abladenAction(Ship ship, @UrlParam(name = "tar") String shipIdList)
	{
		validiereSchiff(ship);

		if (shipIdList.equals(""))
		{
			return new RedirectViewResult("default").withMessage("Es wurden keine Schiffe angegeben");
		}

		int[] shipidlist = Common.explodeToInt("|", shipIdList);
		Ship[] shiplist = new Ship[shipidlist.length];
		for (int i = 0; i < shipidlist.length; i++)
		{
			Ship aship = (Ship) getDB().get(Ship.class, shipidlist[i]);
			if (aship == null)
			{
				addError("Eines der angegebenen Schiffe existiert nicht");
				return new RedirectViewResult("default");
			}
			shiplist[i] = aship;
		}

		ship.undock(shiplist);

		return new RedirectViewResult("default").withMessage(Ship.MESSAGE.getMessage());
	}

	/**
	 * Laesst ein Schiff einer Flotte beitreten oder aus der aktuellen Flotte austreten.
	 *  @param ship Die ID des anzuzeigenden Schiffes
	 * @param join Die ID der Flotte, der das Schiff beitreten soll oder <code>0</code>, falls es aus der aktuellen Flotte austreten soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult joinAction(Ship ship, int join)
	{
		validiereSchiff(ship);

		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		String message;
		// Austreten
		if (join == 0)
		{
			ship.removeFromFleet();

			message = "<span style=\"color:green\">" + Ship.MESSAGE.getMessage() + "</span><br />";
		}
		// Beitreten
		else
		{
			Ship fleetship = (Ship) db.get(Ship.class, join);
			if ((fleetship == null) || (fleetship.getId() < 0))
			{
				return new RedirectViewResult("default");
			}

			ShipFleet fleet = fleetship.getFleet();

			if (fleet == null)
			{
				return new RedirectViewResult("default").withMessage("<span style=\"color:red\">Sie müssen erst eine Flotte erstellen</span><br />");
			}

			if (!ship.getLocation().sameSector(0, fleetship.getLocation(), 0) || (fleetship.getOwner() != user))
			{
				message = "<span style=\"color:red\">Beitritt zur Flotte &quot;" + Common._plaintitle(fleet.getName()) + "&quot; nicht möglich</span><br />";
			}
			else
			{
				ship.setFleet(fleet);
				message = "<span style=\"color:green\">Flotte &quot;" + Common._plaintitle(fleet.getName()) + "&quot; beigetreten</span><br />";
			}
		}

		return new RedirectViewResult("default").withMessage(message);
	}

	/**
	 * Laedt die Schilde des aktuellen Schiffes auf.
	 *  @param ship Die ID des anzuzeigenden Schiffes
	 * @param shup Die Menge an Energie, die zum Aufladen der Schilde verwendet werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult shupAction(Ship ship, int shup)
	{
		validiereSchiff(ship);

		ShipTypeData shiptype = ship.getTypeData();
		int shieldfactor = 100;
		if (shiptype.getShields() < 1000)
		{
			shieldfactor = 10;
		}

		final int maxshup = (int) Math.ceil((shiptype.getShields() - ship.getShields()) / (double) shieldfactor);
		if (shup > maxshup)
		{
			shup = maxshup;
		}
		if (shup > ship.getEnergy())
		{
			shup = ship.getEnergy();
		}

		String message = "Schilde +" + (shup * shieldfactor) + "<br />";

		ship.setShields(ship.getShields() + shup * shieldfactor);
		if (ship.getShields() > shiptype.getShields())
		{
			ship.setShields(shiptype.getShields());
		}

		ship.setEnergy(ship.getEnergy() - shup);

		ship.recalculateShipStatus();

		return new RedirectViewResult("default").withMessage(message);
	}

	/**
	 * Transferiert das Schiff ins System 99.
	 * @param ship Die ID des anzuzeigenden Schiffes
	 *
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult inselAction(Ship ship)
	{
		validiereSchiff(ship);

		User user = (User) getUser();

		if (!user.hasFlag(UserFlag.NPC_ISLAND))
		{
			return new RedirectViewResult("default");
		}

		ship.setX(10);
		ship.setY(10);
		ship.setSystem(99);

		return new RedirectViewResult("default").withMessage("<span style=\"color:green\">Willkommen auf der Insel</span><br />");
	}

	private static final Map<String, String> moduleOutputList = new HashMap<>();

	private static synchronized void initModuleOutputList()
	{
		if (!moduleOutputList.isEmpty())
		{
			return;
		}

		Map<String, String> mo = new HashMap<>();
		// Nur Number-Spalten!
		mo.put("getRu", "<img align='middle' src='" + Cargo.getResourceImage(Resources.URAN) + "' alt='' />Reaktor ");
		mo.put("getRd", "<img align='middle' src='" + Cargo.getResourceImage(Resources.DEUTERIUM) + "' alt='' />Reaktor ");
		mo.put("getRa", "<img align='middle' src='" + Cargo.getResourceImage(Resources.ANTIMATERIE) + "' alt='' />Reaktor ");
		mo.put("getRm", "<img align='middle' src='data/interface/energie.gif' alt='' />Reaktor ");
		mo.put("getCargo", "<img align='middle' src='data/interface/leer.gif' alt='' />Cargo ");
		mo.put("getEps", "<img align='middle' src='data/interface/energie.gif' alt='' />Energiespeicher ");
		mo.put("getHull", "<img align='middle' src='data/interface/schiffe/panzerplatte.png' alt='' />H&uuml;lle ");
		mo.put("getShields", "Shields ");
		mo.put("getCost", "Flugkosten ");
		mo.put("getHeat", "&Uuml;berhitzung ");
		mo.put("getPanzerung", "<img align='middle' src='data/interface/schiffe/panzerplatte.png' alt='' />Panzerung ");
		mo.put("getTorpedoDef", "Torpedoabwehr ");
		mo.put("getCrew", "<img align='middle' src='data/interface/besatzung.gif' alt='' />Crew ");
		mo.put("getHydro", "<img align='middle' src='" + Cargo.getResourceImage(Resources.NAHRUNG) + "' alt='' />Produktion ");
		mo.put("getSensorRange", "<img align='middle' src='data/interface/schiffe/sensorrange.png' alt='' />Sensorreichweite ");
		mo.put("getDeutFactor", "Tanker: <img align='middle' src='" + Cargo.getResourceImage(Resources.DEUTERIUM) + "' alt='' />");
		mo.put("getReCost", "Wartungskosten ");
		mo.put("getADocks", "Externe Docks ");
		mo.put("getJDocks", "J&auml;gerdocks ");
		mo.put("getAblativeArmor", "Ablative Panzerung ");
		mo.put("getSize", "Gr&ouml;&szlig;e ");

		moduleOutputList.putAll(mo);
	}

	/**
	 * Zeigt die Schiffsansicht an.
	 * @param ship Die ID des anzuzeigenden Schiffes
	 *
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(Ship ship, Ship communicate, RedirectViewResult redirect) throws ReflectiveOperationException
	{
		validiereSchiff(ship);

		User user = (User) getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);
		org.hibernate.Session db = getDB();

		db.flush();

		t.setVar("ship.message", redirect != null ? redirect.getMessage() : null);

		ShipTypeData shiptype = ship.getTypeData();

		if (ship.getBattle() != null)
		{
			throw new ValidierungException("Das Schiff ist in einen Kampf verwickelt (hier klicken um zu diesem zu gelangen)!", Common.buildUrl("default", "module", "angriff", "battle", ship.getBattle().getId(), "ship", ship.getId()));
		}

		ship.recalculateShipStatus();
		Offizier offizier = ship.getOffizier();

		t.setVar("ship.showui", 1,
				"ship.islanded", ship.isLanded(),
				"ship.id", ship.getId(),
				"ship.location.system", ship.getLocation().getSystem(),
				"ship.name", Common._plaintitle(ship.getName()),
				"ship.location", ship.getLocation().displayCoordinates(false),
				"ship.location.url", ship.getLocation().urlFragment(),
				"ship.type", ship.getType(),
				"shiptype.picture", shiptype.getPicture(),
				"shiptype.name", shiptype.getNickname(),
				"ship.hull.color", genSubColor(ship.getHull(), shiptype.getHull()),
				"ship.hull", Common.ln(ship.getHull()),
				"shiptype.hull", Common.ln(shiptype.getHull()),
				"ship.ablativearmor.color", genSubColor(ship.getAblativeArmor(), shiptype.getAblativeArmor()),
				"ship.ablativearmor", Common.ln(ship.getAblativeArmor()),
				"shiptype.ablativearmor", Common.ln(shiptype.getAblativeArmor()),
				"ship.shields.color", genSubColor(ship.getShields(), shiptype.getShields()),
				"ship.shields", Common.ln(ship.getShields()),
				"shiptype.shields", Common.ln(shiptype.getShields()),
				"shiptype.cost", shiptype.getCost(),
				"ship.engine.color", genSubColor(ship.getEngine(), 100),
				"ship.engine", ship.getEngine(),
				"shiptype.weapon", shiptype.isMilitary(),
				"ship.weapons.color", genSubColor(ship.getWeapons(), 100),
				"ship.weapons", ship.getWeapons(),
				"ship.comm.color", genSubColor(ship.getComm(), 100),
				"ship.comm", ship.getComm(),
				"ship.sensors.color", genSubColor(ship.getSensors(), 100),
				"ship.sensors", ship.getSensors(),
				"shiptype.crew", Common.ln(shiptype.getCrew()),
				"ship.crew", Common.ln(ship.getCrew()),
				"ship.crew.color", genSubColor(ship.getCrew(), shiptype.getCrew()),
				"ship.marines", (shiptype.getUnitSpace() > 0),
				"ship.e", Common.ln(ship.getEnergy()),
				"shiptype.eps", Common.ln(shiptype.getEps()),
				"ship.e.color", genSubColor(ship.getEnergy(), shiptype.getEps()),
				"ship.s", ship.getHeat(),
				"ship.fleet", ship.getFleet() != null ? ship.getFleet().getId() : 0,
				"shiptype.werft", shiptype.getWerft(),
				"ship.showalarm", !user.isNoob() && (shiptype.getShipClass() != ShipClasses.GESCHUETZ) && shiptype.isMilitary());

		if (ship.getHeat() >= 100)
		{
			t.setVar("ship.s.color", "red");
		}
		else if (ship.getHeat() > 70)
		{
			t.setVar("ship.s.color", "yellow");
		}
		else
		{
			t.setVar("ship.s.color", "white");
		}

		if (offizier != null)
		{
			t.setBlock("_SCHIFF", "offiziere.listitem", "offiziere.list");

			Set<Offizier> offiziere = ship.getOffiziere();
			for (Offizier offi : offiziere)
			{
				t.setVar("offizier.id", offi.getID(),
						"offizier.name", Common._plaintitle(offi.getName()),
						"offizier.picture", offi.getPicture(),
						"offizier.rang", offi.getRang());

				t.parse("offiziere.list", "offiziere.listitem", true);
			}
		}

		// Flottenlink
		if (ship.getFleet() != null)
		{
			t.setVar(
					"fleet.name", Common._plaintitle(ship.getFleet().getName()),
					"fleet.id", ship.getFleet().getId());
		}

		// Aktion: Schnelllink GTU-Handelsdepot
		Ship handelShip = handelspostenService.findeHandelspostenInSektor(ship);
		if ( handelShip != null )
		{
			t.setVar("sector.handel", handelShip.getId(),
					"sector.handel.name", Common._plaintitle(handelShip.getName()));
		}

		// Tooltip: Tradepost
		if (ship.isTradepost())
		{
			t.setVar("tooltip.tradepost", 1);
		}

		// Tooltip: Schiffsstatusfeld
		if (hasPermission(WellKnownPermission.SCHIFF_STATUSFELD))
		{
			t.setVar("tooltip.admin", "<span style='text-decoration:underline'>Schiffsstatus:</span><br />" + ship.getStatus().trim().replace(" ", "<br />"));
		}

		if (user.hasFlag(UserFlag.NPC_ISLAND))
		{
			t.setVar("ship.npcislandlink", 1);
		}

		// Tooltip: Module
		final ModuleEntry[] modulelist = ship.getModules();
		if ((modulelist.length > 0) && shiptype.getTypeModules().length() > 0)
		{
			List<String> tooltiplines = new ArrayList<>();
			tooltiplines.add("<h1>Module</h1>");

			ShipTypeData type = Ship.getShipType(ship.getType());
			ShipTypeData basetype = type;

			Map<Integer, String[]> slotlist = new HashMap<>();
			String[] tmpslotlist = StringUtils.split(type.getTypeModules(), ';');
			for (String aTmpslotlist : tmpslotlist)
			{
				String[] aslot = StringUtils.split(aTmpslotlist, ':');
				slotlist.put(Integer.valueOf(aslot[0]), aslot);
			}

			List<Module> moduleObjList = new ArrayList<>();
			boolean itemmodules = false;

			for (ModuleEntry module : modulelist)
			{
				if (module.getModuleType() != null)
				{
					Module moduleobj = module.createModule();

					if(moduleobj != null) {
						if ((module.getSlot() > 0) && (slotlist.get(module.getSlot()) != null) && (slotlist.get(module.getSlot()).length > 2)) {
							moduleobj.setSlotData(slotlist.get(module.getSlot())[2]);
						} else {
							log.error(String.format("ship: %s - slot: %s - not contained in slot list", ship.getId(), module.getSlot()));
						}

						moduleObjList.add(moduleobj);
						if (moduleobj instanceof ModuleItemModule) {
							Cargo acargo = new Cargo();
							acargo.addResource(((ModuleItemModule) moduleobj).getItemID(), 1);
							ResourceEntry res = acargo.getResourceList().iterator().next();
							tooltiplines.add("<span class='nobr'><img style='vertical-align:middle' src='" + res.getImage() + "' alt='' />" + res.getPlainName() + "</span><br />");
							itemmodules = true;
						}
					}
				}
			}

			if (itemmodules)
			{
				tooltiplines.add("<hr style='height:1px; border:0px; background-color:#606060; color:#606060' />");
			}

			for (int i = 0; i < moduleObjList.size(); i++)
			{
				type = moduleObjList.get(i).modifyStats(type, moduleObjList);
			}

			initModuleOutputList();
			for (String method : moduleOutputList.keySet())
			{
				try
				{
					Method m = type.getClass().getMethod(method);
					m.setAccessible(true);
					Number value = (Number) m.invoke(type);

					m = basetype.getClass().getMethod(method);
					m.setAccessible(true);
					Number baseValue = (Number) m.invoke(basetype);

					// Alles was in moduleOutputList sitzt, muss in der DB durch einen von Number abgeleiteten Typ definiert sein!
					if (!value.equals(baseValue))
					{
						String text;
						if (baseValue.doubleValue() < value.doubleValue())
						{
							text = moduleOutputList.get(method) + Common.ln(value) + " (<span class='nobr' style='color:green'>+";
						}
						else
						{
							text = moduleOutputList.get(method) + Common.ln(value) + " (<span class='nobr' style='color:red'>";
						}
						text += Common.ln(value.doubleValue() - baseValue.doubleValue()) + "</span>)<br />";
						tooltiplines.add(text);
					}
				}
				catch (InvocationTargetException e)
				{
					log.error("Fehler beim Aufruf der Property " + method, e);
				}
				catch (NoSuchMethodException | IllegalAccessException e)
				{
					log.error("Ungueltige Property " + method, e);
				}
			}

			// Weapons
			Map<String, Integer> weaponlist = type.getWeapons();
			Map<String, Integer> defweaponlist = basetype.getWeapons();

			for (Map.Entry<String, Integer> entry : weaponlist.entrySet())
			{
				String aweapon = entry.getKey();
				int aweaponcount = entry.getValue();
				if (!defweaponlist.containsKey(aweapon))
				{
					tooltiplines.add("<span class='nobr' style='color:green'>+" + aweaponcount + " " + Weapons.get().weapon(aweapon).getName() + "</span><br />");
				}
				else
				{
					int defweapon = defweaponlist.get(aweapon);
					if (defweapon < aweaponcount)
					{
						tooltiplines.add("<span class='nobr' style='color:green'>+" + (aweaponcount - defweapon) + " " + Weapons.get().weapon(aweapon).getName() + "</span><br />");
					}
					else if (defweapon > aweaponcount)
					{
						tooltiplines.add("<span class='nobr' style='color:red'>" + (aweaponcount - defweapon) + " " + Weapons.get().weapon(aweapon).getName() + "</span><br />");
					}
				}
			}

			for (Map.Entry<String, Integer> entry : defweaponlist.entrySet())
			{
				String aweapon = entry.getKey();
				if (!weaponlist.containsKey(aweapon))
				{
					int weaponint = entry.getValue();
					tooltiplines.add("<span class='nobr' style='color:red'>-" + weaponint + " " + Weapons.get().weapon(aweapon).getName() + "</span><br />");
				}
			}

			// MaxHeat
			Map<String, Integer> heatlist = type.getMaxHeat();
			Map<String, Integer> defheatlist = basetype.getMaxHeat();

			for (Map.Entry<String, Integer> entry : heatlist.entrySet())
			{
				int aweaponheat = entry.getValue();
				String aweapon = entry.getKey();

				if (!defheatlist.containsKey(aweapon))
				{
					tooltiplines.add("<span class='nobr' style='color:green'>+" + aweaponheat + " " + Weapons.get().weapon(aweapon).getName() + " Max-Hitze</span><br />");
				}
				else
				{
					int defweaponheat = defheatlist.get(aweapon);
					if (defweaponheat < aweaponheat)
					{
						tooltiplines.add("<span class='nobr' style='color:green'>+" + (aweaponheat - defweaponheat) + " " + Weapons.get().weapon(aweapon).getName() + " Max-Hitze</span><br />");
					}
					else if (defweaponheat > aweaponheat)
					{
						tooltiplines.add("<span class='nobr' style='color:red'>" + (aweaponheat - defweaponheat) + " " + Weapons.get().weapon(aweapon).getName() + " Max-Hitze</span><br />");
					}
				}
			}

			// Flags
			EnumSet<ShipTypeFlag> newflaglist = type.getFlags();
			tooltiplines.addAll(newflaglist.stream()
					.filter(aNewflaglist -> !basetype.hasFlag(aNewflaglist))
					.map(aNewflaglist -> "<span class='nobr' style='color:green'>" + aNewflaglist.getLabel() + "</span><br />")
					.collect(Collectors.toList()));

			StringBuilder tooltiptext = new StringBuilder(100);
			if (tooltiplines.size() > 15)
			{
				tooltiptext.append("<div style='height:300px; overflow:auto'>");
			}
			tooltiptext.append(Common.implode("", tooltiplines));
			if (tooltiplines.size() > 15)
			{
				tooltiptext.append("</div>");
			}

			if (tooltiplines.size() > 15)
			{
				t.setVar("tooltip.moduleext", tooltiptext.toString());
			}
			else
			{
				t.setVar("tooltip.module", tooltiptext.toString());
			}
		}

		// Schilde aufladen
		if (shiptype.getShields() > 0 && (ship.getShields() < shiptype.getShields()))
		{
			int shieldfactor = 100;
			if (shiptype.getShields() < 1000)
			{
				shieldfactor = 10;
			}

			t.setVar("ship.shields.reloade", Common.ln((int) Math.ceil((shiptype.getShields() - ship.getShields()) / (double) shieldfactor)));
		}

		// Alarmstufe aendern
		t.setBlock("_SCHIFF", "ship.alarms.listitem", "ship.alarms.list");
		for( Alarmstufe a : Alarmstufe.values())
		{
			t.setVar("alarm.id", a,
					"alarm.name", a.getName(),
					"alarm.selected", (ship.getAlarm() == a));
			t.parse("ship.alarms.list", "ship.alarms.listitem", true);
		}

		if (!ship.getStatus().contains("noconsign"))
		{
			t.setVar("ship.consignable", 1);
		}

		//------------------------------------------------------------
		//
		// Die Plugins
		//
		//------------------------------------------------------------

		Parameters caller = new Parameters();
		caller.controller = this;
		caller.ship = ship;
		caller.shiptype = shiptype;
		caller.offizier = offizier;
		caller.t = t;

		Map<String,Object> parameters = new HashMap<>();
		parameters.put("caller", caller);
		parameters.put("communicate", communicate);

		Map<String, SchiffPlugin> pluginMapper = ermittlePluginsFuer(shiptype, communicate);
		if (pluginMapper.containsKey("navigation"))
		{
			SchiffPlugin plugin = pluginMapper.get("navigation");
			caller.pluginId = "navigation";
			caller.target = "plugin.navigation";
			rufeAlsSubActionAuf(caller.pluginId+"_ops", plugin, "output", parameters);

			pluginMapper.remove("navigation");
		}

		if (pluginMapper.containsKey("cargo"))
		{
			SchiffPlugin plugin = pluginMapper.get("cargo");
			caller.pluginId = "cargo";
			caller.target = "plugin.cargo";
			rufeAlsSubActionAuf(caller.pluginId+"_ops", plugin, "output", parameters);

			pluginMapper.remove("cargo");
		}

		if (pluginMapper.containsKey("units"))
		{
			SchiffPlugin plugin = pluginMapper.get("units");
			caller.pluginId = "units";
			caller.target = "plugin.units";
			rufeAlsSubActionAuf(caller.pluginId+"_ops", plugin, "output", parameters);

			pluginMapper.remove("units");
		}

		caller.target = "plugin.output";
		t.setBlock("_SCHIFF", "plugins.listitem", "plugins.list");

		// Und nun weiter mit den Plugins
		for (String pluginName : pluginMapper.keySet())
		{
			SchiffPlugin plugin = pluginMapper.get(pluginName);

			//Plugin-ID
			caller.pluginId = pluginName;

			//Aufruf der entsprechenden Funktion
			rufeAlsSubActionAuf(caller.pluginId+"_ops", plugin, "output", parameters);

			t.parse("plugins.list", "plugins.listitem", true);
		}

		return t;
	}
}
