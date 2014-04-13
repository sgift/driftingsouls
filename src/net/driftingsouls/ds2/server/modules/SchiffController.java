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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.ModuleEntry;
import net.driftingsouls.ds2.server.cargo.modules.ModuleItemModule;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateController;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.schiffplugins.Parameters;
import net.driftingsouls.ds2.server.modules.schiffplugins.SchiffPlugin;
import net.driftingsouls.ds2.server.scripting.NullLogger;
import net.driftingsouls.ds2.server.scripting.Quests;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
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
public class SchiffController extends TemplateController
{
	private Log log = LogFactory.getLog(SchiffController.class);

	/**
	 * Konstruktor.
	 *
	 * @param context Der zu verwendende Kontext
	 */
	public SchiffController(Context context)
	{
		super(context);

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

	private SchiffPlugin getPluginByName(String name)
	{
		String clsName = getClass().getPackage().getName() + ".schiffplugins." + name;
		try
		{
			Class<?> cls = Class.forName(clsName);
			Class<? extends SchiffPlugin> spClass = cls.asSubclass(SchiffPlugin.class);
			return spClass.newInstance();
		}
		catch (Exception e)
		{
			log.error(e, e);
			return null;
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

	private Map<String, SchiffPlugin> ermittlePluginsFuer(ShipTypeData shiptype)
	{
		Map<String, SchiffPlugin> pluginMapper = new LinkedHashMap<>();
		pluginMapper.put("navigation", getPluginByName("NavigationDefault"));
		pluginMapper.put("cargo", getPluginByName("CargoDefault"));

		if (shiptype.getWerft() != 0)
		{
			pluginMapper.put("werft", getPluginByName("WerftDefault"));
		}

		if (shiptype.hasFlag(ShipTypeFlag.JUMPDRIVE_SHIVAN))
		{
			pluginMapper.put("jumpdrive", getPluginByName("JumpdriveShivan"));
		}

		pluginMapper.put("sensors", getPluginByName("SensorsDefault"));

		if (shiptype.getADocks() > 0)
		{
			pluginMapper.put("adocks", getPluginByName("ADocksDefault"));
		}

		if (shiptype.getJDocks() > 0)
		{
			pluginMapper.put("jdocks", getPluginByName("JDocksDefault"));
		}

		if (shiptype.getUnitSpace() > 0)
		{
			pluginMapper.put("units", getPluginByName("UnitsDefault"));
		}
		return pluginMapper;
	}

	/**
	 * Wechselt die Alarmstufe des Schiffes.
	 *
	 * @param ship Die ID des anzuzeigenden Schiffes
	 * @param alarm Die neue Alarmstufe
	 */
	@Action(ActionType.DEFAULT)
	public void alarmAction(Ship ship, int alarm)
	{
		validiereSchiff(ship);

		User user = (User)getUser();
		if (user.isNoob())
		{
			redirect();
			return;
		}

		ShipTypeData shiptype = ship.getTypeData();
		if ((shiptype.getShipClass() == ShipClasses.GESCHUETZ) || !shiptype.isMilitary())
		{
			redirect();
			return;
		}

		if ((alarm >= Ship.Alert.GREEN.getCode()) && (alarm <= Ship.Alert.RED.getCode()))
		{
			ship.setAlarm(alarm);

			getTemplateEngine().setVar("ship.message", "Alarmstufe erfolgreich ge&auml;ndert<br />");
		}

		ship.recalculateShipStatus();

		redirect();
	}

	/**
	 * Uebergibt das Schiff an einen anderen Spieler.
	 *
	 * @param ship Die ID des anzuzeigenden Schiffes
	 * @param newownerID Die ID des neuen Besitzers
	 * @param conf 1, falls die Sicherheitsabfrage positiv bestaetigt wurde
	 */
	@Action(ActionType.DEFAULT)
	public void consignAction(Ship ship, @UrlParam(name = "newowner") String newownerID, int conf)
	{
		validiereSchiff(ship);

		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();

		User newowner = User.lookupByIdentifier(newownerID);
		if (newowner == null)
		{
			t.setVar("ship.message", "<span style=\"color:red\">Der Spieler existiert nicht</span><br />");
			redirect();
			return;
		}

		if (conf == 0)
		{
			String text = "<span style=\"color:white\">Wollen sie das Schiff " + Common._plaintitle(ship.getName()) + " (" + ship.getId() + ") wirklich an " + newowner.getProfileLink() + " &uuml;bergeben?</span><br />";
			text += "<a class=\"ok\" href=\"" + Common.buildUrl("consign", "ship", ship.getId(), "conf", 1, "newowner", newowner.getId()) + "\">&Uuml;bergeben</a></span><br />";
			t.setVar("ship.message", text);

			redirect();
			return;
		}

		ShipFleet fleet = ship.getFleet();

		boolean result = ship.consign(newowner, false);

		if (result)
		{
			t.setVar("ship.message", Ship.MESSAGE.getMessage());

			redirect();
		}
		else
		{
			ShipTypeData shiptype = ship.getTypeData();
			String msg = "Ich habe dir die " + ship.getName() + " (" + ship.getId() + "), ein Schiff der " + shiptype.getNickname() + "-Klasse, &uuml;bergeben\nSie steht bei " + ship.getLocation().displayCoordinates(false);
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
		}

	}

	/**
	 * Zerstoert das Schiff.
	 * @param ship Die ID des anzuzeigenden Schiffes
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void destroyAction(Ship ship, int conf)
	{
		validiereSchiff(ship);

		TemplateEngine t = getTemplateEngine();

		if (ship.isNoSuicide())
		{
			t.setVar("ship.message", "<span style=\"color:red\">Dieses Schiff kann sich nicht selbstzerst&ouml;ren.</span><br />");
			redirect();
			return;
		}

		if (conf == 0)
		{
			String text = "<span style=\"color:white\">Wollen sie Selbstzerst&ouml;rung des Schiffes " + Common._plaintitle(ship.getName()) + " (" + ship.getId() + ") wirklich ausf&uuml;hren?</span><br />\n";
			text += "<a class=\"error\" href=\"" + Common.buildUrl("destroy", "ship", ship.getId(), "conf", 1) + "\">Selbstzerst&ouml;rung</a></span><br />";
			t.setVar("ship.message", text);

			redirect();
			return;
		}

		ship.destroy();

		t.setVar("ship.message", "<span style=\"color:white\">Das Schiff hat sich selbstzerst&ouml;rt</span><br />");
	}

	/**
	 * Springt durch den angegebenen Sprungpunkt.
	 *
	 * @param ship Die ID des anzuzeigenden Schiffes
	 * @param node Die ID des Sprungpunkts
	 */
	@Action(ActionType.DEFAULT)
	public void jumpAction(Ship ship, int node)
	{
		validiereSchiff(ship);

		TemplateEngine t = getTemplateEngine();

		ShipTypeData shiptype = ship.getTypeData();
		if ((shiptype.getCost() == 0) || (ship.getEngine() == 0))
		{
			redirect();
			return;
		}

		if (node != 0)
		{
			ship.jump(node, false);
			t.setVar("ship.message", Ship.MESSAGE.getMessage());
		}

		redirect();
	}

	/**
	 * Benutzt einen an ein Schiff assoziierten Sprungpunkt.
	 *
	 * @param ship Die ID des anzuzeigenden Schiffes
	 * @param knode Die ID des Schiffes mit dem Sprungpunkt
	 */
	@Action(ActionType.DEFAULT)
	public void kjumpAction(Ship ship, int knode)
	{
		validiereSchiff(ship);

		TemplateEngine t = getTemplateEngine();

		ShipTypeData shiptype = ship.getTypeData();
		if ((shiptype.getCost() == 0) || (ship.getEngine() == 0))
		{
			redirect();
			return;
		}

		if (knode != 0)
		{
			ship.jump(knode, true);
			t.setVar("ship.message", Ship.MESSAGE.getMessage());
		}

		redirect();
	}

	/**
	 * Benennt das Schiff um.
	 *
	 * @param ship Die ID des anzuzeigenden Schiffes
	 * @param newname Der neue Name des Schiffes
	 */
	@Action(ActionType.DEFAULT)
	public void renameAction(Ship ship, String newname)
	{
		validiereSchiff(ship);

		TemplateEngine t = getTemplateEngine();

		ship.setName(newname);
		t.setVar("ship.message", "Name zu " + Common._plaintitle(newname) + " ge&auml;ndert<br />");

		redirect();
	}

	/**
	 * Fuehrt Aktionen eines Plugins aus. Plugin-spezifische
	 * Parameter haben die Form $PluginName_ops[$ParameterName].
	 *
	 * @param ship Die ID des anzuzeigenden Schiffes
	 * @param plugin Der Name des Plugins
	 */
	@Action(ActionType.DEFAULT)
	public void pluginAction(Ship ship, String plugin) throws ReflectiveOperationException
	{
		validiereSchiff(ship);

		TemplateEngine t = getTemplateEngine();

		ShipTypeData shiptype = ship.getTypeData();
		Parameters caller = new Parameters();
		caller.controller = this;
		caller.pluginId = plugin;
		caller.ship = ship;
		caller.shiptype = shiptype;
		caller.offizier = ship.getOffizier();

		Map<String, SchiffPlugin> pluginMapper = ermittlePluginsFuer(shiptype);
		if (!pluginMapper.containsKey(plugin))
		{
			redirect();
			return;
		}

		String ergebnis = (String)rufeAlsSubActionAuf(plugin+"_ops", pluginMapper.get(plugin), "action", caller);
		t.setVar("ship.message", ergebnis);

		redirect();
	}

	/**
	 * Landet die angegebenen Schiffe auf dem aktuellen Schiff.
	 *
	 * @param ship Die ID des anzuzeigenden Schiffes
	 * @param shipIdList Eine mit | separierte Liste an Schiffs-IDs
	 */
	@Action(ActionType.DEFAULT)
	public void landAction(Ship ship, @UrlParam(name = "shiplist") String shipIdList)
	{
		validiereSchiff(ship);

		TemplateEngine t = getTemplateEngine();

		if (shipIdList.equals(""))
		{
			t.setVar("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
			return;
		}

		int[] shipidlist = Common.explodeToInt("|", shipIdList);
		Ship[] shiplist = new Ship[shipidlist.length];
		for (int i = 0; i < shipidlist.length; i++)
		{
			Ship aship = (Ship) getDB().get(Ship.class, shipidlist[i]);
			if (aship == null)
			{
				addError("Eines der angegebenen Schiffe existiert nicht");
				redirect();
				return;
			}
			shiplist[i] = aship;
		}

		ship.land(shiplist);
		t.setVar("ship.message", Ship.MESSAGE.getMessage());

		redirect();
	}

	/**
	 * Startet die angegebenen Schiffe vom aktuellen Schiff.
	 *
	 * @param ship Die ID des anzuzeigenden Schiffes
	 * @param shipIdList Eine mit | separierte Liste von Schiffs-IDs
	 */
	@Action(ActionType.DEFAULT)
	public void startAction(Ship ship, @UrlParam(name = "shiplist") String shipIdList)
	{
		validiereSchiff(ship);

		TemplateEngine t = getTemplateEngine();

		if (shipIdList.equals(""))
		{
			t.setVar("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
			return;
		}

		int[] shipidlist = Common.explodeToInt("|", shipIdList);
		Ship[] shiplist = new Ship[shipidlist.length];
		for (int i = 0; i < shipidlist.length; i++)
		{
			Ship aship = (Ship) getDB().get(Ship.class, shipidlist[i]);
			if (aship == null)
			{
				addError("Eines der angegebenen Schiffe existiert nicht");
				redirect();
				return;
			}
			shiplist[i] = aship;
		}

		ship.start(shiplist);
		t.setVar("ship.message", Ship.MESSAGE.getMessage());

		redirect();
	}

	/**
	 * Dockt die angegebenen Schiffe an das aktuelle Schiff an.
	 *
	 * @param ship Die ID des anzuzeigenden Schiffes
	 * @param shipIdList Eine mit | separierte Liste von Schiffs-IDs
	 */
	@Action(ActionType.DEFAULT)
	public void aufladenAction(Ship ship, @UrlParam(name = "tar") String shipIdList)
	{
		validiereSchiff(ship);

		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();

		if (shipIdList.equals(""))
		{
			t.setVar("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
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
				redirect();
				return;
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
				redirect();
				return;
			}
			shiplist[i] = aship;
		}

		ship.dock(shiplist);
		t.setVar("ship.message", Ship.MESSAGE.getMessage());

		redirect();
	}

	/**
	 * Dockt die angegebenen Schiffe vom aktuellen Schiff ab.
	 *
	 * @param ship Die ID des anzuzeigenden Schiffes
	 * @param shipIdList Eine mit | separierte Liste von Schiffs-IDs
	 */
	@Action(ActionType.DEFAULT)
	public void abladenAction(Ship ship, @UrlParam(name = "tar") String shipIdList)
	{
		validiereSchiff(ship);

		TemplateEngine t = getTemplateEngine();

		if (shipIdList.equals(""))
		{
			t.setVar("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
		}

		int[] shipidlist = Common.explodeToInt("|", shipIdList);
		Ship[] shiplist = new Ship[shipidlist.length];
		for (int i = 0; i < shipidlist.length; i++)
		{
			Ship aship = (Ship) getDB().get(Ship.class, shipidlist[i]);
			if (aship == null)
			{
				addError("Eines der angegebenen Schiffe existiert nicht");
				redirect();
				return;
			}
			shiplist[i] = aship;
		}

		ship.undock(shiplist);
		t.setVar("ship.message", Ship.MESSAGE.getMessage());

		redirect();
	}

	/**
	 * Laesst ein Schiff einer Flotte beitreten oder aus der aktuellen Flotte austreten.
	 *
	 * @param ship Die ID des anzuzeigenden Schiffes
	 * @param join Die ID der Flotte, der das Schiff beitreten soll oder <code>0</code>, falls es aus der aktuellen Flotte austreten soll
	 */
	@Action(ActionType.DEFAULT)
	public void joinAction(Ship ship, int join)
	{
		validiereSchiff(ship);

		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();

		// Austreten
		if (join == 0)
		{
			ship.removeFromFleet();

			t.setVar("ship.message", "<span style=\"color:green\">" + Ship.MESSAGE.getMessage() + "</span><br />");
		}
		// Beitreten
		else
		{
			Ship fleetship = (Ship) db.get(Ship.class, join);
			if ((fleetship == null) || (fleetship.getId() < 0))
			{
				redirect();
				return;
			}

			ShipFleet fleet = fleetship.getFleet();

			if (fleet == null)
			{
				t.setVar("ship.message", "<span style=\"color:red\">Sie m&uuml;ssen erst eine Flotte erstellen</span><br />");
				redirect();
				return;
			}

			if (!ship.getLocation().sameSector(0, fleetship.getLocation(), 0) || (fleetship.getOwner() != user))
			{
				t.setVar("ship.message", "<span style=\"color:red\">Beitritt zur Flotte &quot;" + Common._plaintitle(fleet.getName()) + "&quot; nicht m&ouml;glich</span><br />");
			}
			else
			{
				ship.setFleet(fleet);
				t.setVar("ship.message", "<span style=\"color:green\">Flotte &quot;" + Common._plaintitle(fleet.getName()) + "&quot; beigetreten</span><br />");
			}
		}

		redirect();
	}

	/**
	 * Laedt die Schilde des aktuellen Schiffes auf.
	 *
	 * @param ship Die ID des anzuzeigenden Schiffes
	 * @param shup Die Menge an Energie, die zum Aufladen der Schilde verwendet werden soll
	 */
	@Action(ActionType.DEFAULT)
	public void shupAction(Ship ship, int shup)
	{
		validiereSchiff(ship);

		TemplateEngine t = getTemplateEngine();

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

		t.setVar("ship.message", "Schilde +" + (shup * shieldfactor) + "<br />");

		ship.setShields(ship.getShields() + shup * shieldfactor);
		if (ship.getShields() > shiptype.getShields())
		{
			ship.setShields(shiptype.getShields());
		}

		ship.setEnergy(ship.getEnergy() - shup);

		ship.recalculateShipStatus();

		redirect();
	}

	/**
	 * Speichert ein neues Schiffsaktionsscript und setzt optional
	 * die aktuellen Ausfuehrungsdaten wieder zurueck.
	 *
	 * @param ship Die ID des anzuzeigenden Schiffes
	 * @param script das neue Schfifsaktionsscript
	 * @param reset Wenn der Wert != 0 ist, dann werden die Ausfuehrungsdaten zurueckgesetzt
	 */
	@Action(ActionType.DEFAULT)
	public void scriptAction(Ship ship, String script, boolean reset)
	{
		validiereSchiff(ship);

		TemplateEngine t = getTemplateEngine();

		if (!script.trim().equals(""))
		{
			if (reset)
			{
				ship.setScriptExeData(null);
			}
			ship.setScript(script);
		}
		else
		{
			ship.setScriptExeData(null);
			ship.setScript(null);
		}

		t.setVar("ship.message", "Script gespeichert<br />");

		redirect();
	}

	/**
	 * Behandelt ein OnCommunicate-Ereigniss.
	 *
	 * @param ship Die ID des anzuzeigenden Schiffes
	 * @param communicate Die ID des Schiffes, mit dem kommuniziert werden soll
	 * @param execparameter Weitere Ausfuehrungsdaten
	 */
	@Action(ActionType.DEFAULT)
	public void communicateAction(Ship ship, int communicate, String execparameter)
	{
		validiereSchiff(ship);

		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();

		ScriptEngine scriptparser = getContext().get(ContextCommon.class).getScriptParser("DSQuestScript");
		final Bindings engineBindings = scriptparser.getContext().getBindings(ScriptContext.ENGINE_SCOPE);

		engineBindings.put("_SHIP", ship);
		if (!user.hasFlag(UserFlag.SCRIPT_DEBUGGING))
		{
			scriptparser.getContext().setErrorWriter(new NullLogger());
		}

		Quests.currentEventURLBase.set("./ds?module=schiff&ship=" + ship.getId());
		Quests.currentEventURL.set("&action=communicate&communicate=" + communicate);

		engineBindings.put("TARGETSHIP", Integer.toString(communicate));

		if (execparameter.equals(""))
		{
			execparameter = "0";
		}

		Ship targetship = (Ship) db.get(Ship.class, communicate);
		if ((targetship == null) || (targetship.getId() < 0) || !targetship.getLocation().sameSector(0, ship.getLocation(), 0))
		{
			t.setVar("ship.message", "<span style=\"color:red\">Sie k&ouml;nnen nur mit Schiffen im selben Sektor kommunizieren</span><br />");
			redirect();
			return;
		}
		Quests.executeEvent(scriptparser, targetship.getOnCommunicate(), user, execparameter, false);

		redirect();
	}

	/**
	 * Transferiert das Schiff ins System 99.
	 * @param ship Die ID des anzuzeigenden Schiffes
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void inselAction(Ship ship)
	{
		validiereSchiff(ship);

		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();

		if (!user.hasFlag(UserFlag.NPC_ISLAND))
		{
			redirect();
			return;
		}

		ship.setX(10);
		ship.setY(10);
		ship.setSystem(99);

		t.setVar("ship.message", "<span style=\"color:green\">Willkommen auf der Insel</span><br />");

		redirect();
	}

	private static final Map<String, String> moduleOutputList = new HashMap<>();

	private static synchronized void initModuleOutputList()
	{
		if (!moduleOutputList.isEmpty())
		{
			return;
		}
		final String url = Configuration.getSetting("URL");

		Map<String, String> mo = new HashMap<>();
		// Nur Number-Spalten!
		mo.put("getRu", "<img align='middle' src='" + Cargo.getResourceImage(Resources.URAN) + "' alt='' />Reaktor ");
		mo.put("getRd", "<img align='middle' src='" + Cargo.getResourceImage(Resources.DEUTERIUM) + "' alt='' />Reaktor ");
		mo.put("getRa", "<img align='middle' src='" + Cargo.getResourceImage(Resources.ANTIMATERIE) + "' alt='' />Reaktor ");
		mo.put("getRm", "<img align='middle' src='" + url + "data/interface/energie.gif' alt='' />Reaktor ");
		mo.put("getCargo", "<img align='middle' src='" + url + "data/interface/leer.gif' alt='' />Cargo ");
		mo.put("getEps", "<img align='middle' src='" + url + "data/interface/energie.gif' alt='' />Energiespeicher ");
		mo.put("getHull", "<img align='middle' src='" + url + "data/interface/schiffe/panzerplatte.png' alt='' />H&uuml;lle ");
		mo.put("getShields", "Shields ");
		mo.put("getCost", "Flugkosten ");
		mo.put("getHeat", "&Uuml;berhitzung ");
		mo.put("getPanzerung", "<img align='middle' src='" + url + "data/interface/schiffe/panzerplatte.png' alt='' />Panzerung ");
		mo.put("getTorpedoDef", "Torpedoabwehr ");
		mo.put("getCrew", "<img align='middle' src='" + url + "data/interface/besatzung.gif' alt='' />Crew ");
		mo.put("getHydro", "<img align='middle' src='" + Cargo.getResourceImage(Resources.NAHRUNG) + "' alt='' />Produktion ");
		mo.put("getSensorRange", "<img align='middle' src='" + url + "data/interface/schiffe/sensorrange.png' alt='' />Sensorreichweite ");
		mo.put("getDeutFactor", "Tanker: <img align='middle' src='" + Cargo.getResourceImage(Resources.DEUTERIUM) + "' alt='' />");
		mo.put("getReCost", "Wartungskosten ");
		mo.put("getADocks", "Externe Docks ");
		mo.put("getJDocks", "J&auml;gerdocks ");
		mo.put("getAblativeArmor", "Ablative Panzerung ");

		moduleOutputList.putAll(mo);
	}

	/**
	 * Zeigt die Schiffsansicht an.
	 * @param ship Die ID des anzuzeigenden Schiffes
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(Ship ship) throws ReflectiveOperationException
	{
		validiereSchiff(ship);

		User user = (User) getUser();
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();

		db.flush();

		Quests.currentEventURLBase.set("./ds?module=schiff&ship=" + ship.getId());

		ScriptEngine scriptparser = getContext().get(ContextCommon.class).getScriptParser("DSQuestScript");
		if (ship.isDestroyed())
		{
			if ((scriptparser != null) && (scriptparser.getContext().getWriter().toString().length() != 0))
			{
				t.setVar("ship.scriptparseroutput", scriptparser.getContext().getWriter().toString());
			}
			else
			{
				throw new ValidierungException("Das Schiff existiert nicht mehr oder geh&ouml;rt nicht mehr ihnen");
			}
			return;
		}

		ShipTypeData shiptype = ship.getTypeData();

		if (ship.getBattle() != null)
		{
			if ((scriptparser != null) && (scriptparser.getContext().getWriter().toString().length() > 0))
			{
				t.setVar("ship.scriptparseroutput", scriptparser.getContext().getWriter().toString());
			}

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
		else if (ship.getHeat() > 40)
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
		Iterator<?> handel = db.createQuery("from Ship where id>0 and system=:sys and x=:x and y=:y and locate('tradepost',status)!=0")
				.setInteger("sys", ship.getSystem())
				.setInteger("x", ship.getX())
				.setInteger("y", ship.getY())
				.iterate();

		if (handel.hasNext())
		{
			Ship handelShip = (Ship) handel.next();
			t.setVar("sector.handel", handelShip.getId(),
					"sector.handel.name", Common._plaintitle(handelShip.getName()));
		}

		// Tooltip: Tradepost
		if (ship.isTradepost())
		{
			t.setVar("tooltip.tradepost", 1);
		}

		// Tooltip: Schiffscripte
		if (user.hasFlag(UserFlag.EXEC_NOTES))
		{

			String script = StringUtils.replace(ship.getScript(), "\r\n", "\n");
			script = StringUtils.replace(script, "\n", "\\n");
			script = StringUtils.replace(script, "\"", "\\\"");

			t.setVar("tooltip.execnotes", 1,
					"tooltip.execnotes.script", script);
		}

		// Tooltip: Schiffsstatusfeld
		if (hasPermission(WellKnownPermission.SCHIFF_STATUSFELD))
		{
			StringBuilder tooltiptext = new StringBuilder(100);
			tooltiptext.append("<span style='text-decoration:underline'>Schiffsstatus:</span><br />").append(ship.getStatus().trim().replace(" ", "<br />"));

			t.setVar("tooltip.admin", tooltiptext.toString());
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
				slotlist.put(new Integer(aslot[0]), aslot);
			}

			List<Module> moduleObjList = new ArrayList<>();
			boolean itemmodules = false;

			for (ModuleEntry module : modulelist)
			{
				if (module.getModuleType() != null)
				{
					Module moduleobj = module.createModule();
					if ((module.getSlot() > 0) && (slotlist.get(module.getSlot()).length > 2))
					{
						moduleobj.setSlotData(slotlist.get(module.getSlot())[2]);
					}

					moduleObjList.add(moduleobj);
					if (moduleobj instanceof ModuleItemModule)
					{
						Cargo acargo = new Cargo();
						acargo.addResource(((ModuleItemModule) moduleobj).getItemID(), 1);
						ResourceEntry res = acargo.getResourceList().iterator().next();
						tooltiplines.add("<span class='nobr'><img style='vertical-align:middle' src='" + res.getImage() + "' alt='' />" + res.getPlainName() + "</span><br />");
						itemmodules = true;
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
			Map<String, String> weaponlist = Weapons.parseWeaponList(type.getWeapons());
			Map<String, String> defweaponlist = Weapons.parseWeaponList(basetype.getWeapons());

			for (Map.Entry<String, String> entry : weaponlist.entrySet())
			{
				String aweapon = entry.getKey();
				int aweaponcount = Integer.parseInt(entry.getValue());
				if (!defweaponlist.containsKey(aweapon))
				{
					tooltiplines.add("<span class='nobr' style='color:green'>+" + aweaponcount + " " + Weapons.get().weapon(aweapon).getName() + "</span><br />");
				}
				else
				{
					String defweapon = defweaponlist.get(aweapon);
					if (Integer.parseInt(defweapon) < aweaponcount)
					{
						tooltiplines.add("<span class='nobr' style='color:green'>+" + (aweaponcount - Integer.parseInt(defweapon)) + " " + Weapons.get().weapon(aweapon).getName() + "</span><br />");
					}
					else if (Integer.parseInt(defweapon) > aweaponcount)
					{
						tooltiplines.add("<span class='nobr' style='color:red'>" + (aweaponcount - Integer.parseInt(defweapon)) + " " + Weapons.get().weapon(aweapon).getName() + "</span><br />");
					}
				}
			}

			for (Map.Entry<String, String> entry : defweaponlist.entrySet())
			{
				String aweapon = entry.getKey();
				if (!weaponlist.containsKey(aweapon))
				{
					int weaponint = Integer.parseInt(entry.getValue());
					tooltiplines.add("<span class='nobr' style='color:red'>-" + weaponint + " " + Weapons.get().weapon(aweapon).getName() + "</span><br />");
				}
			}

			// MaxHeat
			Map<String, String> heatlist = Weapons.parseWeaponList(type.getMaxHeat());
			Map<String, String> defheatlist = Weapons.parseWeaponList(basetype.getMaxHeat());

			for (Map.Entry<String, String> entry : heatlist.entrySet())
			{
				int aweaponheat = Integer.parseInt(entry.getValue());
				String aweapon = entry.getKey();

				if (!defheatlist.containsKey(aweapon))
				{
					tooltiplines.add("<span class='nobr' style='color:green'>+" + aweaponheat + " " + Weapons.get().weapon(aweapon).getName() + " Max-Hitze</span><br />");
				}
				else
				{
					int defweaponheat = Integer.parseInt(defheatlist.get(aweapon));
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

		String[] alarmn = {"gr&uuml;n", "gelb", "rot"};

		// Alarmstufe aendern
		t.setBlock("_SCHIFF", "ship.alarms.listitem", "ship.alarms.list");
		for (int a = 0; a < alarmn.length; a++)
		{
			t.setVar("alarm.id", a,
					"alarm.name", alarmn[a],
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

		Map<String, SchiffPlugin> pluginMapper = ermittlePluginsFuer(shiptype);
		if (pluginMapper.containsKey("navigation"))
		{
			SchiffPlugin plugin = pluginMapper.get("navigation");
			caller.pluginId = "navigation";
			caller.target = "plugin.navigation";
			rufeAlsSubActionAuf(caller.pluginId+"_ops", plugin, "output", caller);

			pluginMapper.remove("navigation");
		}

		if (pluginMapper.containsKey("cargo"))
		{
			SchiffPlugin plugin = pluginMapper.get("cargo");
			caller.pluginId = "cargo";
			caller.target = "plugin.cargo";
			rufeAlsSubActionAuf(caller.pluginId+"_ops", plugin, "output", caller);

			pluginMapper.remove("cargo");
		}

		if (pluginMapper.containsKey("units"))
		{
			SchiffPlugin plugin = pluginMapper.get("units");
			caller.pluginId = "units";
			caller.target = "plugin.units";
			rufeAlsSubActionAuf(caller.pluginId+"_ops", plugin, "output", caller);

			pluginMapper.remove("units");
		}

		/*
			Ok...das ist kein Plugin, es gehoert aber trotzdem zwischen die ganzen Plugins (Questoutput)
		*/

		if ((scriptparser != null))
		{
			t.setVar("ship.scriptparseroutput", scriptparser.getContext().getWriter().toString());
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
			rufeAlsSubActionAuf(caller.pluginId+"_ops", plugin, "output", caller);

			t.parse("plugins.list", "plugins.listitem", true);
		}
	}
}
