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
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.*;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.services.BaseService;
import net.driftingsouls.ds2.server.services.ConsignService;
import net.driftingsouls.ds2.server.services.FleetMgmtService;
import net.driftingsouls.ds2.server.services.LocationService;
import net.driftingsouls.ds2.server.services.PmService;
import net.driftingsouls.ds2.server.services.ShipActionService;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.services.ShipyardService;
import net.driftingsouls.ds2.server.ships.*;
import net.driftingsouls.ds2.server.werften.SchiffBauinformationen;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import net.driftingsouls.ds2.server.werften.WerftObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.*;

import static java.util.stream.Collectors.*;

/**
 * Die Flottenverwaltung.
 *
 * @author Christopher Jung
 */
@Module(name = "fleetmgnt")
public class FleetMgntController extends Controller
{
	private final TemplateViewResultFactory templateViewResultFactory;

	@PersistenceContext
	private EntityManager em;

	private final FleetMgmtService fleetMgmtService;
	private final BaseService baseService;
	private final ShipyardService shipyardService;
	private final PmService pmService;
	private final BBCodeParser bbCodeParser;
	private final LocationService locationService;
	private final ConsignService consignService;
	private final ShipService shipService;
	private final ShipActionService shipActionService;

	@Autowired
	public FleetMgntController(TemplateViewResultFactory templateViewResultFactory, FleetMgmtService fleetMgmtService, BaseService baseService, ShipyardService shipyardService, PmService pmService, BBCodeParser bbCodeParser, LocationService locationService, ConsignService consignService, ShipService shipService, ShipActionService shipActionService)
	{
		this.templateViewResultFactory = templateViewResultFactory;
		this.fleetMgmtService = fleetMgmtService;
		this.baseService = baseService;
		this.shipyardService = shipyardService;
		this.pmService = pmService;
		this.bbCodeParser = bbCodeParser;
		this.locationService = locationService;
		this.consignService = consignService;
		this.shipService = shipService;
		this.shipActionService = shipActionService;
	}

	private int ermittleIdEinesGeeignetenSchiffsDerFlotte(ShipFleet fleet)
	{
		User user = (User)getUser();
		int shipid = 0;
		// Nun brauchen wir die ID eines der Schiffe aus der Flotte fuer den javascript-code....
		if (fleet != null)
		{
			Ship aship = em.createQuery("from Ship where id>0 and owner= :user and fleet= :fleet", Ship.class)
					.setParameter("user", user)
					.setParameter("fleet", fleet)
					.setMaxResults(1)
					.getSingleResult();

			if (aship != null)
			{
				shipid = aship.getId();
			}
			else
			{
				throw new ValidierungException("Die angegebene Flotte ist ungültig.");
			}
		}

		return shipid;
	}

	private void validiereGueltigeFlotteVorhanden(ShipFleet fleet)
	{
		if (fleet == null)
		{
			throw new ValidierungException("Die angegebene Flotte existiert nicht.");
		}
		User user = (User) getUser();

		User owner = em.createQuery("select owner from Ship where id>0 and fleet=:fleet", User.class)
				.setParameter("fleet", fleet)
				.getSingleResult();

		if (user.getId() != owner.getId())
		{
			throw new ValidierungException("Diese Flotte gehört einem anderen Spieler.");
		}

		// Falls sich doch ein Schiff eines anderen Spielers eingeschlichen hat
		em.createQuery("update Ship set fleet=null where fleet= :fleet and owner!= :user")
				.setParameter("fleet", fleet)
				.setParameter("user", user)
				.executeUpdate();
	}

	/**
	 * Zeigt den Flottenerstelldialog fuer eine
	 * Flotte aus einer Koordinaten-, Mengen- und Schiffstypenangabe
	 * an.
	 *
	 * @param sector Das Schiff dessen momentaner Sektor als Koordinatenangabe verwendet werden soll
	 * @param type Der Schiffstyp der auszuwaehlenden Schiffe
	 * @param count Die Anzahl der auszuwaehlenden Schiffe
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine createFromSRSGroupAction(Ship sector, int type, int count)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();

		long shipCount = em.createQuery("select count(*) from Ship " +
				"where owner= :user and system= :sys and " +
				"x= :x and y= :y and shiptype= :type and docked=''", Long.class)
				.setParameter("user", user)
				.setParameter("sys", sector.getSystem())
				.setParameter("x", sector.getX())
				.setParameter("y", sector.getY())
				.setParameter("type", type)
				.getSingleResult();

		if ((count < 1) || (shipCount < count))
		{
			t.setVar("fleetmgnt.message", "Es gibt nicht genug Schiffe im Sektor.");
			return t;
		}

		t.setVar("show.create", 1,
				"create.shiplist", "g," + sector.getId() + "," + type + "," + count);

		return t;
	}

	/**
	 * Zeigt den Erstelldialog fuer eine neue Flotte an.
	 * @param shiplistStr Eine mit | separierte Liste von Schiffs-IDs oder eine mit , separierte Liste mit Koordinaten, Schiffstyp und  Mengenangabe
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine createAction(@UrlParam(name = "shiplist") String shiplistStr, RedirectViewResult redirect)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();

		Integer[] shiplist = Common.explodeToInteger("|", shiplistStr);

		if ((shiplistStr.length() == 0) || (shiplist.length == 0))
		{
			t.setVar("fleetmgnt.message", "Sie haben keine Schiffe angegeben.");
			return t;
		}

		t.setVar("fleetmgnt.message", redirect != null ? redirect.getMessage() : null);

		boolean nonEmpty = !em.createQuery("from Ship where id in :shipIds and owner!=:user")
				.setParameter("shipIds", shiplist)
				.setParameter("user", user)
				.setMaxResults(1)
				.getResultList().isEmpty();

		if (nonEmpty)
		{
			t.setVar("fleetmgnt.message", "Alle Schiffe müssen Ihrem Kommando unterstehen.");
		}
		else
		{
			t.setVar("show.create", 1,
					"create.shiplist", Common.implode("|", shiplist));
		}

		return t;
	}

	/**
	 * Erstellt eine Flotte aus einer Schiffsliste oder einer Koordinaten/Typen-Angabe.
	 * @param fleetname der Name der neuen Flotte
	 * @param shiplist Die Liste der Schiffe (IDs) getrennt durch {@code |}
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult create2Action(String fleetname, String shiplist)
	{
		User user = (User) getUser();

		Integer[] shiplistInt;

		if (shiplist.length() == 0)
		{
			throw new ValidierungException("Sie haben keine Schiffe angegeben.");
		}

		if (shiplist.charAt(0) != 'g')
		{
			shiplistInt = Common.explodeToInteger("|", shiplist);
			if (shiplistInt.length == 0)
			{
				throw new ValidierungException("Sie haben keine Schiffe angegeben.");
			}

			boolean nonEmpty = !em.createQuery("from Ship where id in (:shipIds) and (owner!=:user or id < 0)")
					.setParameter("shipIds", shiplistInt)
					.setParameter("user", user)
					.setMaxResults(1)
					.getResultList().isEmpty();
			if (nonEmpty)
			{
				throw new ValidierungException("Alle Schiffe müssen Ihrem Kommando unterstehen.");
			}
		}
		else
		{
			String[] tmp = StringUtils.split(shiplist, ",");
			int sector = Integer.parseInt(tmp[1]);
			int type = Integer.parseInt(tmp[2]);
			int count = Integer.parseInt(tmp[3]);
			Ship sectorShip = em.find(Ship.class, sector);
			if ((sectorShip == null) || (sectorShip.getOwner() != user) || (sectorShip.getId() < 0))
			{
				throw new ValidierungException("Das Schiff untersteht nicht Ihrem Kommando.");
			}

			List<Ship> ships = em.createQuery("from Ship where id>0 and owner=:owner and system=:sys and x=:x and y=:y and shiptype=:type and docked='' order by fleet.id,id asc", Ship.class)
					.setParameter("owner", user)
					.setParameter("sys", sectorShip.getSystem())
					.setParameter("x", sectorShip.getX())
					.setParameter("y", sectorShip.getY())
					.setParameter("type", type)
					.setMaxResults(count)
					.getResultList();
			shiplistInt = new Integer[ships.size()];
			int i = 0;
			for (Ship ship: ships)
			{

				if (ship.getFleet() != null)
				{
					fleetMgmtService.removeShip(ship.getFleet(), ship);
				}
				shiplistInt[i++] = ship.getId();
			}
		}

		if (fleetname.length() > 0)
		{
			ShipFleet fleet = new ShipFleet(fleetname);
			em.persist(fleet);

			for (Integer aShiplistInt : shiplistInt)
			{
				Ship s = em.find(Ship.class, aShiplistInt);
				if (s == null)
				{
					continue;
				}
				s.setFleet(fleet);
			}

			return new RedirectViewResult("default").setParameter("fleet", fleet).withMessage("Flotte " + Common._plaintitle(fleetname) + " erstellt.");
		}
		else
		{
			return new RedirectViewResult("create").withMessage("Sie müssen einen Namen angeben.");
		}
	}

	/**
	 * Fuegt eine definierte Anzahl an Schiffen eines Typs aus einem Sektor zur
	 * Flotte hinzu.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine addFromSRSGroupAction(ShipFleet fleet, @UrlParam(name = "sector") Ship sectorShip, int type, int count)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();

		t.setVar("fleet.id", fleet.getId());

		if ((sectorShip == null) || (sectorShip.getId() < 0) || (sectorShip.getOwner() != user))
		{
			t.setVar("fleetmgnt.message", "Das angegebene Schiff existiert oder gehört Ihnen nicht.");
			return t;
		}

		long shipCount = em.createQuery("select count(*) from Ship where owner=:owner and system=:sys and x=:x and y=:y and shiptype=:type and docked='' and (fleet is null or fleet!=:fleet)", Long.class)
				.setParameter("owner", user)
				.setParameter("sys", sectorShip.getSystem())
				.setParameter("x", sectorShip.getX())
				.setParameter("y", sectorShip.getY())
				.setParameter("type", type)
				.setParameter("fleet", fleet)
				.getSingleResult();

		if ((count < 1) || (shipCount < count))
		{
			t.setVar("fleetmgnt.message", "Es gibt nicht genug Schiffe im Sektor.");
			return t;
		}

		List<Ship> shiplist = new ArrayList<>();
		List<Ship> slist = em.createQuery("from Ship where owner=:owner and system=:sys and x=:x and y=:y and shiptype=:type and " +
				"docked='' and (fleet is null or fleet!=:fleet) order by fleet.id,id asc", Ship.class)
				.setParameter("owner", user)
				.setParameter("sys", sectorShip.getSystem())
				.setParameter("x", sectorShip.getX())
				.setParameter("y", sectorShip.getY())
				.setParameter("type", type)
				.setParameter("fleet", fleet)
				.setMaxResults(count)
				.getResultList();
		for (Ship s : slist)
		{
			if (s.getFleet() != null)
			{
				fleetMgmtService.removeShip(s.getFleet(), s);
			}
			shiplist.add(s);
		}

		if (shiplist.isEmpty())
		{
			t.setVar("fleetmgnt.message", "Es gibt nicht genug Schiffe im Sektor.");
			return t;
		}

		for (Ship s : shiplist)
		{
			s.setFleet(fleet);
		}

		int shipid = ermittleIdEinesGeeignetenSchiffsDerFlotte(fleet);

		t.setVar("jscript.reloadmain.ship", shipid);
		t.setVar("fleetmgnt.message", count + " Schiffe der Flotte hinzugefügt.",
				"jscript.reloadmain", 1);

		return t;
	}

	/**
	 * Zeigt die Seite zum Umbenennen von Flotten an.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine renameAction(ShipFleet fleet, RedirectViewResult redirect)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		TemplateEngine t = templateViewResultFactory.createFor(this);

		t.setVar("fleet.id", fleet.getId());
		t.setVar("fleetmgnt.message", redirect != null ? redirect.getMessage() : null);

		t.setVar("show.rename", 1,
				"fleet.id", fleet.getId(),
				"fleet.name", Common._plaintitle(fleet.getName()));

		return t;
	}

	/**
	 * Benennt eine Flotte um.
	 *
	 * @param fleetname Der neue Name der Flotte
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult rename2Action(ShipFleet fleet, String fleetname)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		if (fleetname.length() > 0)
		{
			fleet.setName(fleetname);

			return new RedirectViewResult("default").withMessage("Flotte " + Common._plaintitle(fleetname) + " umbenannt.");
		}
		else
		{
			return new RedirectViewResult("rename").withMessage("Sie müssen einen Namen angeben.");
		}
	}

	/**
	 * Zeigt die Abfrage an, ob eine Flotte aufgeloest werden soll.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine killAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		TemplateEngine t = templateViewResultFactory.createFor(this);

		t.setVar("fleet.id", fleet.getId());
		t.setVar("fleet.name", Common._plaintitle(fleet.getName()),
				"fleet.id", fleet.getId(),
				"show.kill", 1);

		return t;
	}

	/**
	 * Loest eine Flotte auf.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine kill2Action(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		TemplateEngine t = templateViewResultFactory.createFor(this);

		int shipid = ermittleIdEinesGeeignetenSchiffsDerFlotte(fleet);

		em.createQuery("update Ship set fleet=null where fleet=:fleet")
				.setParameter("fleet", fleet)
				.executeUpdate();
		em.remove(fleet);

		t.setVar("fleet.id", fleet.getId());
		t.setVar("jscript.reloadmain.ship", shipid);
		t.setVar("fleetmgnt.message", "Die Flotte '" + fleet.getName() + "' wurde aufgelöst.",
				"jscript.reloadmain", 1);

		return t;
	}

	/**
	 * Zeigt das Eingabefeld fuer die Uebergabe von Flotten an.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine newownerAction(ShipFleet fleet, RedirectViewResult redirect)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		TemplateEngine t = templateViewResultFactory.createFor(this);

		t.setVar("fleetmgnt.message", redirect != null ? redirect.getMessage() : null);
		t.setVar("fleet.id", fleet.getId());
		t.setVar("show.newowner", 1,
				"fleet.id", fleet.getId(),
				"fleet.name", Common._plaintitle(fleet.getName()));

		return t;
	}

	/**
	 * Zeigt die Bestaetigung fuer die Uebergabe der Flotte an.
	 *
	 * @param newowner Die ID des Users, an den die Flotte uebergeben werden soll
	 */
	@Action(ActionType.DEFAULT)
	public Object newowner2Action(ShipFleet fleet, @UrlParam(name = "ownerid") User newowner)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		if (newowner != null)
		{
			TemplateEngine t = templateViewResultFactory.createFor(this);

			t.setVar("fleet.id", fleet.getId());

			t.setVar("show.newowner2", 1,
					"newowner.name", Common._title(bbCodeParser, newowner.getName()),
					"newowner.id", newowner.getId(),
					"fleet.id", fleet.getId(),
					"fleet.name", Common._plaintitle(fleet.getName()));

			return t;
		}
		else
		{
			return new RedirectViewResult("newowner").withMessage("Der angegebene Spieler existiert nicht.");
		}
	}

	/**
	 * Uebergibt die Flotte an einen neuen Spieler.
	 *
	 * @param newowner Die ID des neuen Besitzers
	 */
	@Action(ActionType.DEFAULT)
	public Object newowner3Action(ShipFleet fleet, @UrlParam(name = "ownerid") User newowner)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		User user = (User) this.getUser();

		if (newowner != null)
		{
			if (consignService.consign(fleet, newowner))
			{
				TemplateEngine t = templateViewResultFactory.createFor(this);
				t.setVar("fleet.id", fleet.getId());

				Ship coords = em.createQuery("from Ship where owner=:owner and fleet=:fleet", Ship.class)
						.setParameter("owner", newowner)
						.setParameter("fleet", fleet)
						.setMaxResults(1)
						.getSingleResult();

				if (coords != null)
				{
					pmService.send(user, newowner.getId(), "Flotte übergeben", "Ich habe Dir die Flotte " + Common._plaintitle(fleet.getName()) + " übergeben. Sie steht bei " + locationService.displayCoordinates(coords.getLocation(), false));
					t.setVar("fleetmgnt.message", ShipFleet.MESSAGE.getMessage() + "Die Flotte wurde übergeben.");
				}
				else
				{
					t.setVar("fleetmgnt.message", ShipFleet.MESSAGE.getMessage() + "Flottenübergabe gescheitert.");
				}

				return t;
			}
			else
			{
				return new RedirectViewResult("newowner").withMessage(ShipFleet.MESSAGE.getMessage() + "Flottenübergabe gescheitert.");
			}
		}
		else
		{
			return new RedirectViewResult("newowner").withMessage("Der angegebene Spieler existiert nicht.");
		}
	}

	/**
	 * Laedt die Schilde aller Schiffe in der Flotte auf.
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult shupAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);


		StringBuilder message = new StringBuilder(100);
		List<Ship> ships = em.createQuery("select s from Ship as s left join s.modules m " +
				"where s.fleet=:fleet and (s.shields < s.shiptype.shields or s.shields < m.shields) and s.battle is null", Ship.class)
				.setParameter("fleet", fleet)
				.getResultList();
		for (Ship ship: ships)
		{
			ShipTypeData shipType = ship.getTypeData();

			int shieldfactor = 100;
			if (shipType.getShields() < 1000)
			{
				shieldfactor = 10;
			}

			int shup = (int) Math.ceil((shipType.getShields() - ship.getShields()) / (double) shieldfactor);
			if (shup > ship.getEnergy())
			{
				shup = ship.getEnergy();
				message.append(ship.getName()).append(" (").append(ship.getId()).append(") - ").append("<span style=\"color:orange\">Schilde bei ").append(Math.round((ship.getShields() + shup * shieldfactor) / (double) shipType.getShields() * 100)).append("%</span><br />");
			}
			ship.setShields(ship.getShields() + shup * shieldfactor);
			if (ship.getShields() > shipType.getShields())
			{
				ship.setShields(shipType.getShields());
			}
			ship.setEnergy(ship.getEnergy() - shup);
		}

		return new RedirectViewResult("default").withMessage(message + " Die Schilde wurden aufgeladen.");
	}

	/**
	 * Exportiert die Schiffsliste der Flotte.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine exportAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		TemplateEngine t = templateViewResultFactory.createFor(this);

		t.setVar("fleet.id", fleet.getId());

		t.setVar("fleet.name", Common._plaintitle(fleet.getName()),
				"show.export", 1);

		t.setBlock("_FLEETMGNT", "exportships.listitem", "exportships.list");

		List<Ship> ships = em.createQuery("from Ship where id>0 and fleet=:fleet", Ship.class)
				.setParameter("fleet", fleet)
				.getResultList();
		for (Ship ship: ships)
		{

			t.setVar("ship.id", ship.getId(),
					"ship.name", Common._plaintitle(ship.getName()));

			t.parse("exportships.list", "exportships.listitem", true);
		}

		return t;
	}

	/**
	 * Dockt alle Schiffe der Flotte ab.
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult undockAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		fleetMgmtService.undockContainers(fleet);

		return new RedirectViewResult("default").withMessage("Alle gedockten Schiffe wurden gestartet.");
	}

	/**
	 * Sammelt alle nicht gedockten eigenen Container im Sektor auf (sofern genug Platz vorhanden ist).
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult redockAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		User user = (User) getUser();

		collectContainers(fleet, user);

		return new RedirectViewResult("default").withMessage("Container wurden aufgesammelt.");
	}

	/**
	 * Sammelt alle nicht gedockten eigenen Geschütze im Sektor auf (sofern genug Platz vorhanden ist).
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult redock2Action(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		User user = (User) getUser();

		collectGunPlatforms(fleet, user);

		return new RedirectViewResult("default").withMessage("Geschütze wurden aufgesammelt.");
	}

	/**
	 * Startet alle Jaeger der Flotte.
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult jstartAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		fleetMgmtService.startFighters(fleet);

		return new RedirectViewResult("default").withMessage("Alle Jäger/Bomber sind gestartet.");
	}

	/**
	 * Sammelt alle nicht gelandeten eigenen Jaeger im Sektor auf (sofern genug Platz vorhanden ist).
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult jlandAction(ShipFleet fleet, int jaegertype)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		User user = (User) getUser();

		collectFightersByType(fleet, user, jaegertype);

		return new RedirectViewResult("default").withMessage("Jäger/Bomber wurden aufgesammelt.");
	}

	/**
	 * Fuegt die Schiffe einer anderen Flotte der aktiven Flotte hinzu.
	 *
	 * @param targetFleet Die ID der Flotte, deren Schiffe zur aktiven Flotte hinzugefuegt werden sollen
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult fleetcombineAction(ShipFleet fleet, @UrlParam(name = "fleetcombine") ShipFleet targetFleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		User user = (User) getUser();

		if (targetFleet == null)
		{
			addError("Die angegebene Flotte existiert nicht!");
			return new RedirectViewResult("default");
		}

		User aowner = fleetMgmtService.getOwner(targetFleet);
		if (aowner == null || (aowner != user))
		{
			addError("Die angegebene Flotte geh&ouml;rt nicht ihnen!");
			return new RedirectViewResult("default");
		}

		fleetMgmtService.joinFleet(fleet, targetFleet);

		return new RedirectViewResult("default").withMessage("Alle Schiffe der Flotte '" + Common._plaintitle(targetFleet.getName()) + "' sind beigetreten.");
	}

	/**
	 * Aendert die Alarmstufe der Schiffe.
	 *
	 * @param alarm Die neue Alarmstufe
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult alarmAction(ShipFleet fleet, Alarmstufe alarm)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		fleetMgmtService.setAlarm(fleet, alarm);

		return new RedirectViewResult("default").withMessage("Die Alarmstufe wurde geändert.");
	}

	/**
	 * Zeigt das Eingabefeld fuer das Umbenennen der Schiffe der Flotte.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine renameShipsAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		TemplateEngine t = templateViewResultFactory.createFor(this);

		t.setVar("fleet.id", fleet.getId());

		t.setVar("show.renameShips", 1,
				"fleet.id", fleet.getId(),
				"fleet.name", Common._plaintitle(fleet.getName()));

		return t;
	}

	/**
	 * Baut ein Schiffstyp n-mal in allen Werften der Flotte, die dazu in der Lage sind.
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult buildAction(ShipFleet fleet, int buildcount, String buildid)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		if (buildcount <= 0)
		{
			return new RedirectViewResult("default");
		}

		User user = (User) getUser();
		List<Ship> shipyards = em.createQuery("from Ship where id>0 and owner=:owner and fleet=:fleet and shiptype.werft > 0 order by id", Ship.class)
				.setParameter("owner", user)
				.setParameter("fleet", fleet)
				.getResultList();

		shipyards.removeIf(ship -> ship.getTypeData().getWerft() == 0);

		if (shipyards.isEmpty())
		{
			return new RedirectViewResult("default");
		}

		//Build
		while (buildcount > 0)
		{
			int couldNotBuild = 0;
			for (Ship ship : shipyards)
			{
				WerftObject shipyard = em.createQuery("from ShipWerft where ship=:ship", WerftObject.class)
						.setParameter("ship", ship.getId())
						.getSingleResult();
				if (shipyard.getKomplex() != null)
				{
					shipyard = shipyard.getKomplex();
				}
				if (shipActionService.buildShip(shipyard, SchiffBauinformationen.fromId(buildid), false, false))
				{
					buildcount--;
				}
				else
				{
					couldNotBuild++;
				}

				if (buildcount == 0)
				{
					break;
				}
			}

			//No shipyard could build -> stop
			if (couldNotBuild == shipyards.size())
			{
				buildcount = 0;
			}
		}

		//Reload main page
		return new RedirectViewResult("default").withMessage("Bauauftrag erteilt!");
	}

	/**
	 * Laedt Nahrung aus dem eigenen Cargo in den Nahrungsspeicher.
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult fillFoodAction(ShipFleet fleet) {
		validiereGueltigeFlotteVorhanden(fleet);

		User user = (User) getUser();

		List<Ship> ships = em.createQuery("from Ship as s WHERE s.id>0 and s.owner=:owner and s.fleet=:fleet and s.battle is null", Ship.class)
				.setParameter("owner", user)
				.setParameter("fleet", fleet)
				.getResultList();

		StringBuilder message = new StringBuilder(100);
		for (Ship ship: ships) {
			Cargo cargo = new Cargo(ship.getCargo());

			long usenahrung = ship.getTypeData().getNahrungCargo() - ship.getNahrungCargo();
			if(usenahrung > cargo.getResourceCount(Resources.NAHRUNG)) {
				usenahrung = cargo.getResourceCount(Resources.NAHRUNG);
			}

			ship.setNahrungCargo(ship.getNahrungCargo()+usenahrung);
			cargo.substractResource(Resources.NAHRUNG, usenahrung);
			ship.setCargo(cargo);

			if(usenahrung > 0) {
				message.append(ship.getName()).append(" (").append(ship.getId()).append(") - <span style=\"color:orange\">")
					.append(usenahrung).append(" Nahrung transferiert</span><br />");
			}
			ship.setCargo(cargo);
		}
		return new RedirectViewResult("default").withMessage(message.append("Nahrung erfolgreich in den Nahrungsspeicher transferiert.").toString());
	}

	/**
	 * Teil eines Formatierungsstrings fuer Schiffsnamen.
	 */
	private interface NamePatternElement
	{
		/**
		 * Gibt den Text fuer das naechste Schiff zurueck.
		 *
		 * @return Der Text
		 */
        String next();
	}

	private static class StringNamePatternElement implements NamePatternElement
	{
		private final String text;

		StringNamePatternElement(String text)
		{
			this.text = text;
		}

		@Override
		public String next()
		{
			return text;
		}
	}

	private static class NumberNamePatternElement implements NamePatternElement
	{
		private int counter = 1;

		NumberNamePatternElement()
		{
			// EMPTY
		}

		@Override
		public String next()
		{
			return Integer.toString(counter++);
		}
	}

	private static class RomanNumberNamePatternElement implements NamePatternElement
	{
		private static final String[] base = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
		private static final String[] tens = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
		private static final String[] hundreds = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
		private static final String[] thousands = {"", "M", "MM", "MMM"};

		private int counter = 1;

		RomanNumberNamePatternElement()
		{
			// EMPTY
		}

		@Override
		public String next()
		{
			int number = counter++;

			if (counter == 4000)
			{
				counter = 1;
			}

			return thousands[(number / 1000)] +
					hundreds[(number / 100) % 10] +
					tens[(number / 10) % 10] +
					base[number % 10];
		}
	}

	/**
	 * Konvertiert das angegebene Formatierungsmuster fuer Schiffsnamen in eine Liste
	 * von <code>NamePatternElements</code>.
	 * Die Sortierung entspricht ihrem vorkommen im String.
	 *
	 * @param name Der Formatierungsstring
	 * @return Die Liste
	 */
	private List<NamePatternElement> parseNamePattern(String name)
	{
		List<NamePatternElement> nameParts = new ArrayList<>();
		do
		{
			if (name.startsWith("$("))
			{
				int end = name.indexOf(')');
				if (end == -1)
				{
					nameParts.add(new StringNamePatternElement(name));
					break;
				}

				String partName = name.substring(2, end);

				if ("number".equalsIgnoreCase(partName))
				{
					nameParts.add(new NumberNamePatternElement());
				}
				else if ("roman".equalsIgnoreCase(partName))
				{
					nameParts.add(new RomanNumberNamePatternElement());
				}
				else
				{
					nameParts.add(new StringNamePatternElement("$(" + partName + ")"));
				}

				if (end == name.length() + 1)
				{
					break;
				}

				name = name.substring(end + 1);
			}
			else
			{
				int index = name.indexOf("$(");
				if (index != -1)
				{
					nameParts.add(new StringNamePatternElement(name.substring(0, index)));
					name = name.substring(index);
				}
				else
				{
					nameParts.add(new StringNamePatternElement(name));
					name = "";
					break;
				}
			}
		}
		while (name.contains("$("));

		if (!name.isEmpty())
		{
			nameParts.add(new StringNamePatternElement(name));
		}

		return nameParts;
	}

	/**
	 * Generiert aus einer Liste von Namensteilen den Gesamtnamen fuer das naechste Schiff.
	 *
	 * @param nameParts Die Namensteile
	 * @return Der Gesamtname
	 */
	private String generateNextShipName(List<NamePatternElement> nameParts)
	{
		StringBuilder builder = new StringBuilder();

		for (NamePatternElement namePart : nameParts)
		{
			builder.append(namePart.next());
		}

		return builder.toString();
	}

	/**
	 * Benennt die Schiffe der Flotte um.
	 *
	 * @param name Das Namensmuster
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult renameShips2Action(ShipFleet fleet, String name)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		List<NamePatternElement> nameParts = parseNamePattern(name);

		List<Ship> ships = em.createQuery("from Ship where id>0 and fleet=:fleet", Ship.class)
				.setParameter("fleet", fleet)
				.getResultList();

		for (Ship ship: ships)
		{
			ship.setName(generateNextShipName(nameParts));
		}

		return new RedirectViewResult("default").withMessage("Die Namen wurden geändert.");
	}

	/**
	 * Transferiert eine bestimmte Menge (in Prozent) an Crew zwischen der Flotte
	 * und einer den Basen des Spielers.
	 *
	 * @param crewinpercent Anzahl der Crew in Prozent (der Maxcrew des Zielschiffes)
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult getCrewAction(ShipFleet fleet, int crewinpercent)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		User user = (User) getUser();

		crewinpercent = Math.min(crewinpercent, 100);
		crewinpercent = Math.max(crewinpercent, 0);

		double crewInPercent = crewinpercent / 100.0;

		List<Ship> ships = em.createQuery("from Ship " +
				"where id>0 and owner=:owner and fleet=:fleet order by id", Ship.class)
				.setParameter("owner", user)
				.setParameter("fleet", fleet)
				.getResultList();

		for (Ship ship : ships)
		{
			int amount = (int) (Math.round((ship.getTypeData().getCrew() * crewInPercent)) - ship.getCrew());
			for (Base base : user.getBases())
			{
				amount -= baseService.transferCrew(base, ship, amount);
			}
		}

		return new RedirectViewResult("default");
	}

	/**
	 * Bestaetigungsanfrage fuers Demontieren.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine askDismantleAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		TemplateEngine t = templateViewResultFactory.createFor(this);

		t.setVar("fleet.name", Common._plaintitle(fleet.getName()),
				"fleet.id", fleet.getId(),
				"show.dismantle", 1);

		return t;
	}

	/**
	 * Demontiert die Flotte.
	 */
	@Action(ActionType.DEFAULT)
	public Object dismantleAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		List<WerftObject> shipyards = new ArrayList<>();

		if (getGanymedCount(fleet) > 0)
		{
			Ship aship = getOneFleetShip(fleet);
			shipyards = em.createQuery("from ShipWerft where ship.system=:system and ship.x=:x and ship.y=:y and ship.owner=:owner", WerftObject.class)
					.setParameter("system", aship.getSystem())
					.setParameter("x", aship.getX())
					.setParameter("y", aship.getY())
					.setParameter("owner", aship.getOwner())
					.getResultList();
		}

		List<Base> bases = getOwnerAsteroids(fleet);
		if (!bases.isEmpty())
		{
			for (Base base : bases)
			{
				WerftObject shipyard = base.getShipyard();
				if (shipyard != null)
				{
					shipyards.add(shipyard);
				}
			}
		}

		if (!shipyards.isEmpty())
		{
			for (WerftObject shipyard : shipyards)
			{
				if (fleetMgmtService.dismantleFleet(shipyard, fleet))
				{
					TemplateEngine t = templateViewResultFactory.createFor(this);
					int shipid = ermittleIdEinesGeeignetenSchiffsDerFlotte(fleet);

					t.setVar("jscript.reloadmain.ship", shipid);
					t.setVar("fleetmgnt.message", "Die Flotte '" + fleet.getName() + "' wurde demontiert.",
							"jscript.reloadmain", 1);

					return t;
				}
			}
		}
		else
		{
			addError("Keine Werft im Sektor gefunden.");
		}

		return new RedirectViewResult("default");
	}

    /**
     * Bestaetigungsanfrage fuers reparieren.
     */
    @Action(ActionType.DEFAULT)
    public TemplateEngine askRepairAction(ShipFleet fleet)
    {
        validiereGueltigeFlotteVorhanden(fleet);

        TemplateEngine t = templateViewResultFactory.createFor(this);

        t.setVar("fleet.name", Common._plaintitle(fleet.getName()),
                "fleet.id", fleet.getId(),
                "show.repair", 1);

        return t;
    }

    /**
     * Repariert die Flotte.
     */
    @Action(ActionType.DEFAULT)
    public Object repairAction(ShipFleet fleet)
    {
        validiereGueltigeFlotteVorhanden(fleet);

        List<WerftObject> shipyards = new ArrayList<>();

        if (getGanymedCount(fleet) > 0)
        {
            Ship aship = getOneFleetShip(fleet);
            List<ShipWerft> tshipyards = em.createQuery("from ShipWerft where ship.system=:system and ship.x=:x and ship.y=:y and ship.owner=:owner", ShipWerft.class)
                    .setParameter("system", aship.getSystem())
                    .setParameter("x", aship.getX())
                    .setParameter("y", aship.getY())
                    .setParameter("owner", aship.getOwner())
                    .getResultList();

            for(ShipWerft shipyard : tshipyards)
            {
                if(shipyard.getKomplex() != null && !shipyards.contains(shipyard.getKomplex()))
                {
					shipyards.add(shipyard.getKomplex());
                }
                else if(shipyard.getKomplex() == null)
                {
                    shipyards.add(shipyard);
                }
            }
        }

        List<Base> bases = getOwnerAsteroids(fleet);
        if (!bases.isEmpty())
        {
            for (Base base : bases)
            {
                WerftObject shipyard = base.getShipyard();
                if (shipyard != null)
                {
					shipyards.add(shipyard);
                }
            }
        }

        if (!shipyards.isEmpty())
        {
            TemplateEngine t = templateViewResultFactory.createFor(this);



            List<Ship> shipsToRepair = fleetMgmtService.getShips(fleet).stream()
				.filter(Ship::needRepair)
				.collect(toList());

			int repairedShips = 0;
            for(Ship shipToRepair: shipsToRepair) {
				for(WerftObject shipyard: shipyards) {
					if (shipActionService.repairShip(shipyard, shipToRepair, false)) {
						repairedShips++;
					}
				}
			}

            int shipid = ermittleIdEinesGeeignetenSchiffsDerFlotte(fleet);

            t.setVar("jscript.reloadmain.ship", shipid);
            t.setVar("fleetmgnt.message", "Es wurden " + repairedShips + " Schiffe repariert.");

            return t;
        }
        else
        {
            addError("Keine Werft im Sektor gefunden.");
        }

        return new RedirectViewResult("default");
    }

    @Action(value = ActionType.DEFAULT)
    public RedirectViewResult activateTanker(ShipFleet fleet)
    {
        validiereGueltigeFlotteVorhanden(fleet);

        User user = (User) getUser();

        List<Ship> ships = em.createQuery("from Ship " +
                "where id>0 and owner=:owner and fleet=:fleet order by id", Ship.class)
                .setParameter("owner", user)
                .setParameter("fleet", fleet)
                .getResultList();

        for(Ship ship : ships)
        {
            if(ship.getTypeData().getDeutFactor() > 0)
            {
                SchiffEinstellungen einstellungen = ship.getEinstellungen();
                einstellungen.setAutoDeut(true);
                einstellungen.persistIfNecessary(ship);
            }
        }
        return new RedirectViewResult("default").withMessage("Alle Tanker angestellt.");
    }

    @Action(value = ActionType.DEFAULT)
    public RedirectViewResult deactivateTanker(ShipFleet fleet)
    {
        validiereGueltigeFlotteVorhanden(fleet);

        User user = (User) getUser();

        List<Ship> ships = em.createQuery("from Ship " +
                "where id>0 and owner=:owner and fleet=:fleet order by id", Ship.class)
                .setParameter("owner", user)
                .setParameter("fleet", fleet)
                .getResultList();

        for(Ship ship: ships)
        {
            if(ship.getTypeData().getDeutFactor() > 0)
            {
                SchiffEinstellungen einstellungen = ship.getEinstellungen();
                einstellungen.setAutoDeut(false);
                einstellungen.persistIfNecessary(ship);
            }
        }
        return new RedirectViewResult("default").withMessage("Alle Tanker ausgestellt.");
    }

	@Action(value = ActionType.DEFAULT, readOnly = true)
	public TemplateEngine defaultAction(ShipFleet fleet, RedirectViewResult redirect)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		User user = (User) getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);

		t.setVar("fleet.id", fleet.getId());

		List<String> sectors = new ArrayList<>();

		Ship aship = getOneFleetShip(fleet);

		if (aship == null)
		{
			t.setVar("fleetmgnt.message", "Die Flotte existiert nicht mehr.");
			return t;
		}

		if( redirect != null )
		{
			int shipid = ermittleIdEinesGeeignetenSchiffsDerFlotte(fleet);
			t.setVar("fleetmgnt.message", redirect.getMessage(),
					"jscript.reloadmain.ship", shipid,
					"jscript.reloadmain", 1);
		}

		sectors.add("(s.x=" + aship.getX() + " and s.y=" + aship.getY() + " and s.system=" + aship.getSystem() + ")");

		t.setVar("show.view", 1,
				"fleet.name", Common._plaintitle(fleet.getName()),
				"fleet.id", fleet.getId());

		t.setBlock("_FLEETMGNT", "ships.listitem", "ships.list");

		Location aloc = aship.getLocation();

		List<Ship> ships = em.createQuery("from Ship where id>0 and owner=:owner and fleet=:fleet order by id", Ship.class)
				.setParameter("owner", user)
				.setParameter("fleet", fleet)
				.getResultList();

		Set<WerftObject> werften = new HashSet<>();
        boolean hasTanker = false;
		for (Ship ship: ships)
		{
			ShipTypeData shiptype = ship.getTypeData();
			Location loc = ship.getLocation();

			//Find shipyards
			if (shiptype.getWerft() > 0)
			{
				WerftObject werft = em.createQuery("from ShipWerft where ship=:ship", WerftObject.class)
						.setParameter("ship", ship.getId())
						.getSingleResult();
				if (werft != null && werft.getKomplex() != null)
				{
					werften.add(werft.getKomplex());
				}
				else if (werft != null)
				{
					werften.add(werft);
				}
			}

            //Finde Tanker
            if( shiptype.getDeutFactor() > 0)
            {
                hasTanker = true;
						}

			String offi = null;

			if (ship.getStatus().contains("offizier"))
			{
				Offizier offizier = ship.getOffizier();
				if (offizier != null)
				{
					offi = " <a class=\"forschinfo\" href=\"" + Common.buildUrl("default", "module", "choff", "off", offizier.getID()) + "\"><img style=\"vertical-align:middle\" src=\"" + offizier.getPicture() + "\" alt=\"Rang " + offizier.getRang() + "\" /></a>";
				}
			}

			t.setVar("ship.id", ship.getId(),
					"ship.name", Common._plaintitle(ship.getName()),
					"ship.type.name", shiptype.getNickname(),
					"ship.offi", offi,
					"ship.showbattle", ship.getBattle() != null ? ship.getBattle() : 0,
					"ship.showwarning", !aloc.sameSector(0, loc, 0));

			String sectorStr = "(s.x=" + ship.getX() + " and s.y=" + ship.getY() + " and s.system=" + ship.getSystem() + ")";
			if (!sectors.contains(sectorStr))
			{
				sectors.add(sectorStr);
			}

			t.parse("ships.list", "ships.listitem", true);
		}

		Set<SchiffBauinformationen> buildableShips = new TreeSet<>();
		for (WerftObject werft : werften)
		{
			buildableShips.addAll(shipyardService.getBuildShipList(werft));
		}

		//List of buildable ships
		if (!buildableShips.isEmpty())
		{
			t.setBlock("_FLEETMGNT", "buildableships.listitem", "buildableships.list");
			for (SchiffBauinformationen ship : buildableShips)
			{
				t.setVar("buildableships.id", ship.getId(),
						"buildableships.name", ship.getBaudaten().getType().getNickname());

				t.parse("buildableships.list", "buildableships.listitem", true);
			}
		}

		//Find asteroids in sector
		long asteroidCount = em.createQuery("select count(id) from Base where system=:system and x=:x and y=:y and owner=:owner", Long.class)
				.setParameter("system", aship.getSystem())
				.setParameter("x", aship.getX())
				.setParameter("y", aship.getY())
				.setParameter("owner", user)
				.getSingleResult();

		if (asteroidCount > 0)
		{
			t.setVar("astiinsector", 1);
		}

        if(hasTanker)
        {
            t.setVar("hastanker", 1);
        }
		//Find shipyards in sector
		long ganymedCount = getGanymedCount(fleet);

		if (ganymedCount > 0)
		{
			t.setVar("shipyardinsector", 1);
		}
		else
		{
			//Find shipyards on asteroids
			if (asteroidCount > 0)
			{
				List<Base> bases = getOwnerAsteroids(fleet);

				for (Base base : bases)
				{
					if (base.hasShipyard())
					{
						t.setVar("shipyardinsector", 1);
						break;
					}
				}
			}
		}

		// Jaegerliste bauen
		String sectorstring = Common.implode(" or ", sectors);

		t.setBlock("_FLEETMGNT", "jaegertypes.listitem", "jaegertypes.list");

		List<Object[]> shiptypes = em.createQuery("select s.shiptype,count(*) " +
				"from Ship as s " +
				"where s.owner=:owner and locate(:flag,s.shiptype.flags)!=0 and s.docked='' and (" + sectorstring + ") " +
				"group by s.shiptype", Object[].class)
				.setParameter("owner", user)
				.setParameter("flag", ShipTypeFlag.JAEGER.getFlag())
				.getResultList();
		for (Object[] shiptype1 : shiptypes)
		{
			ShipType shiptype = (ShipType)shiptype1[0];

			t.setVar("jaegertype.id", shiptype.getId(),
					"jaegertype.name", shiptype.getNickname());

			t.parse("jaegertypes.list", "jaegertypes.listitem", true);
		}

		// Flottenliste bauen
		t.setBlock("_FLEETMGNT", "fleetcombine.listitem", "fleetcombine.list");
		List<ShipFleet> fleets = em.createQuery("select distinct s.fleet from Ship as s " +
				"where s.system=:sys and s.x=:x and s.y=:y AND docked='' AND owner=:owner AND s.fleet!=:fleet", ShipFleet.class)
				.setParameter("sys", aship.getSystem())
				.setParameter("x", aship.getX())
				.setParameter("y", aship.getY())
				.setParameter("owner", user)
				.setParameter("fleet", fleet)
				.getResultList();

		for (ShipFleet afleet : fleets)
		{

			long count = em.createQuery("select count(*) from Ship where fleet=:fleet", Long.class)
					.setParameter("fleet", afleet)
					.getSingleResult();

			t.setVar("fleetcombine.id", afleet.getId(),
					"fleetcombine.name", Common._plaintitle(afleet.getName()),
					"fleetcombine.shipcount", count);

			t.parse("fleetcombine.list", "fleetcombine.listitem", true);
		}

		return t;
	}

	/**
	 * Gibt alle Asteroiden des Flottenbesitzers im Sektor zurueck.
	 *
	 * @return s.o.
	 */
	private List<Base> getOwnerAsteroids(ShipFleet fleet)
	{
		Ship ship = getOneFleetShip(fleet);
		return em.createQuery("from Base where system=:system and x=:x and y=:y and owner=:owner", Base.class)
				.setParameter("system", ship.getSystem())
				.setParameter("x", ship.getX())
				.setParameter("y", ship.getY())
				.setParameter("owner", ship.getOwner())
				.getResultList();
	}

	/**
	 * Gibt irgendein Schiff aus der Flotte zurueck.
	 *
	 * @return Irgendein Schiff.
	 */
	private Ship getOneFleetShip(ShipFleet fleet)
	{
		return em.createQuery("from Ship where id>0 and fleet=:fleet", Ship.class)
				.setParameter("fleet", fleet)
				.setMaxResults(1)
				.getSingleResult();
	}

	/**
	 * Gibt die Anzahl Ganymeds im Sektor zurueck.
	 *
	 * @return Anzahl Ganymeds.
	 */
	private long getGanymedCount(ShipFleet fleet)
	{
		Ship ship = getOneFleetShip(fleet);
		return em.createQuery("select count(id) from ShipWerft where ship.system=:system and ship.x=:x and ship.y=:y", Long.class)
				.setParameter("system", ship.getSystem())
				.setParameter("x", ship.getX())
				.setParameter("y", ship.getY())
				.getSingleResult();
	}

	/**
	 * Sammelt alle Container auf und dockt sie an Schiffe der Flotte.
	 *
	 * @param user Der Besitzer der Flotte/Container
	 */
	private void collectContainers(ShipFleet fleet, User user) {
		List<Ship> ships = em.createQuery("from Ship where id>0 and fleet=:fleet and battle is null", Ship.class)
			.setParameter("fleet", fleet)
			.getResultList();

		for (Ship ship : ships) {
			ShipTypeData shiptype = ship.getTypeData();

			if (shiptype.getADocks() == 0) {
				continue;
			}

			int free = shiptype.getADocks() - (int) shipService.getDockedCount(ship);
			if (free == 0) {
				continue;
			}

			List<Ship> containers = em.createQuery("from Ship as s " +
				"where s.owner=:owner and s.system=:sys and s.x=:x and s.y=:y and s.docked='' and " +
				"s.shiptype.shipClass=:cls and s.battle is null " +
				"order by s.fleet.id,s.shiptype.id", Ship.class)
				.setParameter("owner", user)
				.setParameter("sys", ship.getSystem())
				.setParameter("x", ship.getX())
				.setParameter("y", ship.getY())
				.setParameter("cls", ShipClasses.CONTAINER)
				.getResultList();

			if (containers.isEmpty()) {
				break;
			}

			shipService.dock(ship, containers.subList(0, Math.min(free, containers.size())).toArray(new Ship[0]));
		}
	}

	/**
	 * Sammelt alle Geschütze auf und dockt sie an Schiffe der Flotte.
	 *
	 * @param user Der Besitzer der Flotte/Geschütze
	 */
	public void collectGunPlatforms(ShipFleet fleet, User user) {
		List<Ship> ships = em.createQuery("from Ship where id>0 and fleet=:fleet and battle is null", Ship.class)
			.setParameter("fleet", fleet)
			.getResultList();

		for (Ship ship : ships) {
			ShipTypeData shiptype = ship.getTypeData();

			if (shiptype.getADocks() == 0) {
				continue;
			}

			int free = shiptype.getADocks() - (int) shipService.getDockedCount(ship);
			if (free == 0) {
				continue;
			}

			List<Ship> gunPlatforms = em.createQuery("from Ship as s " +
				"where s.owner=:owner and s.system=:sys and s.x=:x and s.y=:y and s.docked='' and " +
				"s.shiptype.shipClass=:cls and s.battle is null " +
				"order by s.fleet.id,s.shiptype.id", Ship.class)
				.setParameter("owner", user)
				.setParameter("sys", ship.getSystem())
				.setParameter("x", ship.getX())
				.setParameter("y", ship.getY())
				.setParameter("cls", ShipClasses.GESCHUETZ)
				.getResultList();

			if (gunPlatforms.isEmpty()) {
				break;
			}

			shipService.dock(ship, gunPlatforms.subList(0, Math.min(free, gunPlatforms.size())).toArray(new Ship[0]));
		}
	}

	/**
	 * Sammelt alle Jaeger eines Typs auf und landet sie auf den Schiffen
	 * der Flotte. Sollen alle Jaeger aufgesammelt werden, so muss als Typ
	 * <code>0</code> angegeben werden.
	 *
	 * @param user         Der Besitzer der Flotte/Jaeger
	 * @param jaegertypeID Der Typ der Jaeger oder <code>null</code>
	 */
	public void collectFightersByType(ShipFleet fleet, User user, int jaegertypeID) {
		List<Ship> ships = em.createQuery("from Ship where id>0 and fleet=:fleet and battle is null", Ship.class)
			.setParameter("fleet", fleet)
			.getResultList();

		for (Ship ship : ships) {
			ShipTypeData shiptype = ship.getTypeData();

			if (shiptype.getJDocks() == 0) {
				continue;
			}
			int free = shiptype.getJDocks() - (int) shipService.getLandedCount(ship);
			if (free == 0) {
				continue;
			}

			TypedQuery<Ship> fighterQuery = em.createQuery("select s from Ship as s left join s.modules m " +
				"where " + (jaegertypeID > 0 ? "s.shiptype=:shiptype and " : "") + "s.owner=:user and s.system=:system and " +
				"s.x=:x and s.y=:y and s.docked='' and " +
				"(locate(:jaegerFlag,s.shiptype.flags)!=0 or locate(:jaegerFlag,m.flags)!=0) and " +
				"s.battle is null " +
				"order by s.fleet.id,s.shiptype.id ", Ship.class)
				.setParameter("user", user)
				.setParameter("system", ship.getSystem())
				.setParameter("x", ship.getX())
				.setParameter("y", ship.getY())
				.setParameter("jaegerFlag", ShipTypeFlag.JAEGER.getFlag());

			if (jaegertypeID > 0) {
				fighterQuery.setParameter("shiptype", jaegertypeID);
			}
			List<Ship> fighters = fighterQuery.getResultList();

			if (fighters.isEmpty()) {
				break;
			}

			fighters = fighters.subList(0, Math.min(free, fighters.size()));
			shipService.land(ship, fighters.toArray(new Ship[0]));
		}
	}
}
