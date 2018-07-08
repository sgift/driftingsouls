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
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.*;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.ships.*;
import net.driftingsouls.ds2.server.werften.SchiffBauinformationen;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import net.driftingsouls.ds2.server.werften.WerftObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Die Flottenverwaltung.
 *
 * @author Christopher Jung
 */
@Module(name = "fleetmgnt")
public class FleetMgntController extends Controller
{
	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public FleetMgntController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;
	}

	private int ermittleIdEinesGeeignetenSchiffsDerFlotte(ShipFleet fleet)
	{
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		int shipid = 0;
		// Nun brauchen wir die ID eines der Schiffe aus der Flotte fuer den javascript-code....
		if (fleet != null)
		{
			Ship aship = (Ship) db.createQuery("from Ship where id>0 and owner= :user and fleet= :fleet")
					.setEntity("user", user)
					.setEntity("fleet", fleet)
					.setMaxResults(1)
					.uniqueResult();

			if (aship != null)
			{
				shipid = aship.getId();
			}
			else
			{
				throw new ValidierungException("Die angegebene Flotte ist ungueltig");
			}
		}

		return shipid;
	}

	private void validiereGueltigeFlotteVorhanden(ShipFleet fleet)
	{
		if (fleet == null)
		{
			throw new ValidierungException("Die angegebene Flotte existiert nicht");
		}
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		User owner = (User) db.createQuery("select owner from Ship where id>0 and fleet=:fleet")
				.setEntity("fleet", fleet)
				.iterate().next();

		if (user.getId() != owner.getId())
		{
			throw new ValidierungException("Diese Flotte geh&ouml;rt einem anderen Spieler");
		}

		// Falls sich doch ein Schiff eines anderen Spielers eingeschlichen hat
		db.createQuery("update Ship set fleet=null where fleet= :fleet and owner!= :user")
				.setEntity("fleet", fleet)
				.setEntity("user", user)
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
		org.hibernate.Session db = getDB();

		long shipcount = (Long) db.createQuery("select count(*) from Ship " +
				"where owner= :user and system= :sys and " +
				"x= :x and y= :y and shiptype= :type and docked=''")
				.setEntity("user", user)
				.setInteger("sys", sector.getSystem())
				.setInteger("x", sector.getX())
				.setInteger("y", sector.getY())
				.setInteger("type", type)
				.iterate().next();

		if ((count < 1) || (shipcount < count))
		{
			t.setVar("fleetmgnt.message", "Es gibt nicht genug Schiffe im Sektor");
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
		org.hibernate.Session db = getDB();

		Integer[] shiplist = Common.explodeToInteger("|", shiplistStr);

		if ((shiplistStr.length() == 0) || (shiplist.length == 0))
		{
			t.setVar("fleetmgnt.message", "Sie haben keine Schiffe angegeben");
			return t;
		}

		t.setVar("fleetmgnt.message", redirect != null ? redirect.getMessage() : null);

		boolean nonEmpty = db.createQuery("from Ship where id in (:shipIds) and owner!=:user")
				.setParameterList("shipIds", shiplist)
				.setEntity("user", user)
				.iterate().hasNext();

		if (nonEmpty)
		{
			t.setVar("fleetmgnt.message", "Alle Schiffe m&uuml;ssen ihrem Kommando unterstehen");
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
		org.hibernate.Session db = getDB();

		Integer[] shiplistInt;

		if (shiplist.length() == 0)
		{
			throw new ValidierungException("Sie haben keine Schiffe angegeben");
		}

		if (shiplist.charAt(0) != 'g')
		{
			shiplistInt = Common.explodeToInteger("|", shiplist);
			if ((shiplist.length() == 0) || shiplistInt.length == 0)
			{
				throw new ValidierungException("Sie haben keine Schiffe angegeben");
			}

			boolean nonEmpty = db.createQuery("from Ship where id in (:shipIds) and (owner!=:user or id < 0)")
					.setParameterList("shipIds", shiplistInt)
					.setEntity("user", user)
					.iterate().hasNext();
			if (nonEmpty)
			{
				throw new ValidierungException("Alle Schiffe müssen ihrem Kommando unterstehen");
			}
		}
		else
		{
			String[] tmp = StringUtils.split(shiplist, ",");
			int sector = Integer.parseInt(tmp[1]);
			int type = Integer.parseInt(tmp[2]);
			int count = Integer.parseInt(tmp[3]);
			Ship sectorShip = (Ship) db.get(Ship.class, sector);
			if ((sectorShip == null) || (sectorShip.getOwner() != user) || (sectorShip.getId() < 0))
			{
				throw new ValidierungException("Das Schiff untersteht nicht ihrem Kommando");
			}

			List<?> ships = db.createQuery("from Ship where id>0 and owner=:owner and system=:sys and x=:x and y=:y and shiptype=:type and docked='' order by fleet.id,id asc")
					.setEntity("owner", user)
					.setInteger("sys", sectorShip.getSystem())
					.setInteger("x", sectorShip.getX())
					.setInteger("y", sectorShip.getY())
					.setInteger("type", type)
					.setMaxResults(count)
					.list();
			shiplistInt = new Integer[ships.size()];
			int i = 0;
			for (Object ship : ships)
			{
				Ship s = (Ship) ship;

				if (s.getFleet() != null)
				{
					s.removeFromFleet();
				}
				shiplistInt[i++] = s.getId();
			}
		}

		if (fleetname.length() > 0)
		{
			ShipFleet fleet = new ShipFleet(fleetname);
			db.persist(fleet);

			for (Integer aShiplistInt : shiplistInt)
			{
				Ship s = (Ship) db.get(Ship.class, aShiplistInt);
				if (s == null)
				{
					continue;
				}
				s.setFleet(fleet);
			}

			return new RedirectViewResult("default").setParameter("fleet", fleet).withMessage("Flotte " + Common._plaintitle(fleetname) + " erstellt");
		}
		else
		{
			return new RedirectViewResult("create").withMessage("Sie müssen einen Namen angeben");
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
		org.hibernate.Session db = getDB();

		t.setVar("fleet.id", fleet.getId());

		if ((sectorShip == null) || (sectorShip.getId() < 0) || (sectorShip.getOwner() != user))
		{
			t.setVar("fleetmgnt.message", "Das angegebene Schiff existiert oder gehoert ihnen nicht");
			return t;
		}

		long shipcount = (Long) db.createQuery("select count(*) from Ship where owner=:owner and system=:sys and x=:x and y=:y and shiptype=:type and docked='' and (fleet is null or fleet!=:fleet)")
				.setEntity("owner", user)
				.setInteger("sys", sectorShip.getSystem())
				.setInteger("x", sectorShip.getX())
				.setInteger("y", sectorShip.getY())
				.setInteger("type", type)
				.setEntity("fleet", fleet)
				.iterate().next();

		if ((count < 1) || (shipcount < count))
		{
			t.setVar("fleetmgnt.message", "Es gibt nicht genug Schiffe im Sektor");
			return t;
		}

		List<Ship> shiplist = new ArrayList<>();
		List<?> slist = db.createQuery("from Ship where owner=:owner and system=:sys and x=:x and y=:y and shiptype=:type and " +
				"docked='' and (fleet is null or fleet!=:fleet) order by fleet.id,id asc")
				.setEntity("owner", user)
				.setInteger("sys", sectorShip.getSystem())
				.setInteger("x", sectorShip.getX())
				.setInteger("y", sectorShip.getY())
				.setInteger("type", type)
				.setEntity("fleet", fleet)
				.setMaxResults(count)
				.list();
		for (Object aSlist : slist)
		{
			Ship s = (Ship) aSlist;
			if (s.getFleet() != null)
			{
				s.removeFromFleet();
			}
			shiplist.add(s);
		}

		if (shiplist.isEmpty())
		{
			t.setVar("fleetmgnt.message", "Es gibt nicht genug Schiffe im Sektor");
			return t;
		}

		for (Ship s : shiplist)
		{
			s.setFleet(fleet);
		}

		int shipid = ermittleIdEinesGeeignetenSchiffsDerFlotte(fleet);

		t.setVar("jscript.reloadmain.ship", shipid);
		t.setVar("fleetmgnt.message", count + " Schiffe der Flotte hinzugef&uuml;gt",
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

			return new RedirectViewResult("default").withMessage("Flotte " + Common._plaintitle(fleetname) + " umbenannt");
		}
		else
		{
			return new RedirectViewResult("rename").withMessage("Sie müssen einen Namen angeben");
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
		org.hibernate.Session db = getDB();

		int shipid = ermittleIdEinesGeeignetenSchiffsDerFlotte(fleet);

		db.createQuery("update Ship set fleet=null where fleet=:fleet")
				.setEntity("fleet", fleet)
				.executeUpdate();
		db.delete(fleet);

		t.setVar("fleet.id", fleet.getId());
		t.setVar("jscript.reloadmain.ship", shipid);
		t.setVar("fleetmgnt.message", "Die Flotte '" + fleet.getName() + "' wurde aufgel&ouml;st",
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
					"newowner.name", Common._title(newowner.getName()),
					"newowner.id", newowner.getId(),
					"fleet.id", fleet.getId(),
					"fleet.name", Common._plaintitle(fleet.getName()));

			return t;
		}
		else
		{
			return new RedirectViewResult("newowner").withMessage("Der angegebene Spieler existiert nicht");
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
		org.hibernate.Session db = getDB();

		if (newowner != null)
		{
			if (fleet.consign(newowner))
			{
				TemplateEngine t = templateViewResultFactory.createFor(this);
				t.setVar("fleet.id", fleet.getId());

				Ship coords = (Ship) db.createQuery("from Ship where owner=:owner and fleet=:fleet")
						.setEntity("owner", newowner)
						.setEntity("fleet", fleet)
						.setMaxResults(1)
						.uniqueResult();

				if (coords != null)
				{
					PM.send(user, newowner.getId(), "Flotte &uuml;bergeben", "Ich habe dir die Flotte " + Common._plaintitle(fleet.getName()) + " &uuml;bergeben. Sie steht bei " + coords.getLocation().displayCoordinates(false));
					t.setVar("fleetmgnt.message", ShipFleet.MESSAGE.getMessage() + "Die Flotte wurde &uuml;bergeben");
				}
				else
				{
					t.setVar("fleetmgnt.message", ShipFleet.MESSAGE.getMessage() + "Flotten&uuml;bergabe gescheitert");
				}

				return t;
			}
			else
			{
				return new RedirectViewResult("newowner").withMessage(ShipFleet.MESSAGE.getMessage() + "Flottenübergabe gescheitert");
			}
		}
		else
		{
			return new RedirectViewResult("newowner").withMessage("Der angegebene Spieler existiert nicht");
		}
	}

	/**
	 * Laedt die Schilde aller Schiffe in der Flotte auf.
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult shupAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		org.hibernate.Session db = getDB();

		StringBuilder message = new StringBuilder(100);
		List<?> ships = db.createQuery("select s from Ship as s left join s.modules m " +
				"where s.fleet=:fleet and (s.shields < s.shiptype.shields or s.shields < m.shields) and s.battle is null")
				.setEntity("fleet", fleet)
				.list();
		for (Object ship : ships)
		{
			Ship s = (Ship) ship;
			ShipTypeData stype = s.getTypeData();

			int shieldfactor = 100;
			if (stype.getShields() < 1000)
			{
				shieldfactor = 10;
			}

			int shup = (int) Math.ceil((stype.getShields() - s.getShields()) / (double) shieldfactor);
			if (shup > s.getEnergy())
			{
				shup = s.getEnergy();
				message.append(s.getName()).append(" (").append(s.getId()).append(") - ").append("<span style=\"color:orange\">Schilde bei ").append(Math.round((s.getShields() + shup * shieldfactor) / (double) stype.getShields() * 100)).append("%</span><br />");
			}
			s.setShields(s.getShields() + shup * shieldfactor);
			if (s.getShields() > stype.getShields())
			{
				s.setShields(stype.getShields());
			}
			s.setEnergy(s.getEnergy() - shup);
		}

		return new RedirectViewResult("default").withMessage(message + " Die Schilde wurden aufgeladen");
	}

	/**
	 * Entlaedt die Batterien auf den Schiffen der Flotte, um die EPS wieder aufzuladen.
	 */
/*	@Action(ActionType.DEFAULT)
	public RedirectViewResult dischargeBatteriesAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		org.hibernate.Session db = getDB();

		StringBuilder message = new StringBuilder(100);

		List<?> ships = db.createQuery("select s from Ship as s left join s.modules m " +
				"where s.fleet=:fleet and (s.e < s.shiptype.eps or (s.e < m.eps)) and s.battle is null")
				.setEntity("fleet", fleet)
				.list();
		for (Object ship : ships)
		{
			Ship s = (Ship) ship;
			ShipTypeData stype = s.getTypeData();

			if (s.getEnergy() >= stype.getEps())
			{
				continue;
			}

			Cargo cargo = s.getCargo();

			long unload = stype.getEps() - s.getEnergy();
			if (unload > cargo.getResourceCount(Resources.BATTERIEN))
			{
				unload = cargo.getResourceCount(Resources.BATTERIEN);

				message.append(s.getName()).append(" (").append(s.getId()).append(") - <span style=\"color:orange\">Energie bei ").append(Math.round((s.getEnergy() + unload) / (double) stype.getEps() * 100)).append("%</span><br />");
			}
			cargo.substractResource(Resources.BATTERIEN, unload);
			cargo.addResource(Resources.LBATTERIEN, unload);

			s.setEnergy((int) (s.getEnergy() + unload));
			s.setCargo(cargo);
		}

		return new RedirectViewResult("default").withMessage(message + "Batterien wurden entladen");
	}*/

	/**
	 * Laedt die Batterien auf den Schiffen der Flotte auf.
	 */
	/*@Action(ActionType.DEFAULT)
	public RedirectViewResult chargeBatteriesAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		org.hibernate.Session db = getDB();

		StringBuilder message = new StringBuilder(100);

		List<?> ships = db.createQuery("from Ship as s WHERE s.fleet=:fleet and s.battle is null")
				.setEntity("fleet", fleet)
				.list();
		for (Object ship : ships)
		{
			Ship s = (Ship) ship;

			Cargo cargo = new Cargo(s.getCargo());
			if (!cargo.hasResource(Resources.LBATTERIEN))
			{
				continue;
			}

			long load = cargo.getResourceCount(Resources.LBATTERIEN);
			if (load > s.getEnergy())
			{
				load = s.getEnergy();

				message.append(s.getName()).append(" (").append(s.getId()).append(") - <span style=\"color:orange\">").append(load).append("/").append(cargo.getResourceCount(Resources.LBATTERIEN)).append(" Batterien aufgeladen</span><br />");
			}
			cargo.substractResource(Resources.LBATTERIEN, load);
			cargo.addResource(Resources.BATTERIEN, load);

			s.setEnergy((int) (s.getEnergy() - load));
			s.setCargo(cargo);
		}

		return new RedirectViewResult("default").withMessage(message + "Batterien wurden aufgeladen");
	}*/

	/**
	 * Exportiert die Schiffsliste der Flotte.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine exportAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		TemplateEngine t = templateViewResultFactory.createFor(this);
		org.hibernate.Session db = getDB();

		t.setVar("fleet.id", fleet.getId());

		t.setVar("fleet.name", Common._plaintitle(fleet.getName()),
				"show.export", 1);

		t.setBlock("_FLEETMGNT", "exportships.listitem", "exportships.list");

		List<?> ships = db.createQuery("from Ship where id>0 and fleet=:fleet")
				.setEntity("fleet", fleet)
				.list();
		for (Object ship : ships)
		{
			Ship aship = (Ship) ship;

			t.setVar("ship.id", aship.getId(),
					"ship.name", Common._plaintitle(aship.getName()));

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

		fleet.undockContainers();

		return new RedirectViewResult("default").withMessage("Alle gedockten Schiffe wurden gestartet");
	}

	/**
	 * Sammelt alle nicht gedockten eigenen Container im Sektor auf (sofern genug Platz vorhanden ist).
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult redockAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		User user = (User) getUser();

		fleet.collectContainers(user);

		return new RedirectViewResult("default").withMessage("Container wurden aufgesammelt");
	}

	/**
	 * Sammelt alle nicht gedockten eigenen Geschütze im Sektor auf (sofern genug Platz vorhanden ist).
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult redock2Action(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		User user = (User) getUser();

		fleet.collectGeschuetze(user);

		return new RedirectViewResult("default").withMessage("Geschütze wurden aufgesammelt");
	}

	/**
	 * Startet alle Jaeger der Flotte.
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult jstartAction(ShipFleet fleet)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		fleet.startFighters();

		return new RedirectViewResult("default").withMessage("Alle Jäger sind gestartet");
	}

	/**
	 * Sammelt alle nicht gelandeten eigenen Jaeger im Sektor auf (sofern genug Platz vorhanden ist).
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult jlandAction(ShipFleet fleet, int jaegertype)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		User user = (User) getUser();

		fleet.collectFightersByType(user, jaegertype);

		return new RedirectViewResult("default").withMessage("Jäger wurden aufgesammelt");
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

		User aowner = targetFleet.getOwner();
		if (aowner == null || (aowner != user))
		{
			addError("Die angegebene Flotte geh&ouml;rt nicht ihnen!");
			return new RedirectViewResult("default");
		}

		fleet.joinFleet(targetFleet);

		return new RedirectViewResult("default").withMessage("Alle Schiffe der Flotte '" + Common._plaintitle(targetFleet.getName()) + "' sind beigetreten");
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

		fleet.setAlarm(alarm);

		return new RedirectViewResult("default").withMessage("Die Alarmstufe wurde geändert");
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
	@SuppressWarnings("unchecked")
	public RedirectViewResult buildAction(ShipFleet fleet, int buildcount, String buildid)
	{
		validiereGueltigeFlotteVorhanden(fleet);

		if (buildcount <= 0)
		{
			return new RedirectViewResult("default");
		}

		org.hibernate.Session db = getDB();
		User user = (User) getUser();
		List<Ship> shipyards = db.createQuery("from Ship where id>0 and owner=:owner and fleet=:fleet and shiptype.werft > 0 order by id")
				.setEntity("owner", user)
				.setEntity("fleet", fleet)
				.list();

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
				WerftObject shipyard = (WerftObject) db.createQuery("from ShipWerft where ship=:ship")
						.setInteger("ship", ship.getId())
						.uniqueResult();
				if (shipyard.getKomplex() != null)
				{
					shipyard = shipyard.getKomplex();
				}
				if (shipyard.buildShip(SchiffBauinformationen.fromId(buildid), false, false))
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
		return new RedirectViewResult("default").withMessage("Bauauftrag erteilt");
	}
	
	/**
	 * Laedt Nahrung aus dem eigenen Cargo in den Nahrungsspeicher.
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult fillFoodAction(ShipFleet fleet) {
		validiereGueltigeFlotteVorhanden(fleet);
		
		org.hibernate.Session db = getDB();
		User user = (User) getUser();
		
		List<?> ships = db.createQuery("from Ship as s WHERE s.id>0 and s.owner=:owner and s.fleet=:fleet and s.battle is null")
				.setEntity("owner", user)
				.setEntity("fleet", fleet)
				.list();
		
		StringBuilder message = new StringBuilder(100);
		for (Object ship : ships) {
			Ship s = (Ship) ship;
			Cargo cargo = new Cargo(s.getCargo());
			
			long usenahrung = s.getTypeData().getNahrungCargo() - s.getNahrungCargo();
			if(usenahrung > cargo.getResourceCount(Resources.NAHRUNG)) {
				usenahrung = cargo.getResourceCount(Resources.NAHRUNG);
			}
			
			s.setNahrungCargo(s.getNahrungCargo()+usenahrung);
			cargo.substractResource(Resources.NAHRUNG, usenahrung);
			s.setCargo(cargo);
			
			if(usenahrung > 0) {
				message.append(s.getName()).append(" (").append(s.getId()).append(") - <span style=\"color:orange\">")
					.append(usenahrung).append(" Nahrung transferiert</span><br />");
			}
			s.setCargo(cargo);
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
		private String text;

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

		org.hibernate.Session db = getDB();
		List<NamePatternElement> nameParts = parseNamePattern(name);

		List<?> ships = db.createQuery("from Ship where id>0 and fleet=:fleet")
				.setEntity("fleet", fleet)
				.list();

		for (Object ship1 : ships)
		{
			Ship ship = (Ship) ship1;

			ship.setName(generateNextShipName(nameParts));
		}

		return new RedirectViewResult("default").withMessage("Die Namen wurden geändert");
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

		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		crewinpercent = Math.min(crewinpercent, 100);
		crewinpercent = Math.max(crewinpercent, 0);

		double crewInPercent = crewinpercent / 100.0;

		List<Ship> ships = Common.cast(db.createQuery("from Ship " +
				"where id>0 and owner=:owner and fleet=:fleet order by id")
				.setEntity("owner", user)
				.setEntity("fleet", fleet)
				.list());

		for (Ship ship : ships)
		{
			int amount = (int) (Math.round((ship.getTypeData().getCrew() * crewInPercent)) - ship.getCrew());
			for (Base base : user.getBases())
			{
				amount -= base.transferCrew(ship, amount);
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

		org.hibernate.Session db = getDB();
		if (getGanymedCount(fleet) > 0)
		{
			Ship aship = getOneFleetShip(fleet);
			shipyards = Common.cast(db.createQuery("from ShipWerft where ship.system=:system and ship.x=:x and ship.y=:y and ship.owner=:owner")
					.setParameter("system", aship.getSystem())
					.setParameter("x", aship.getX())
					.setParameter("y", aship.getY())
					.setParameter("owner", aship.getOwner())
					.list());
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
				if (fleet.dismantleFleet(shipyard))
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

        org.hibernate.Session db = getDB();
        if (getGanymedCount(fleet) > 0)
        {
            Ship aship = getOneFleetShip(fleet);
            List<ShipWerft> tshipyards = Common.cast(db.createQuery("from ShipWerft where ship.system=:system and ship.x=:x and ship.y=:y and ship.owner=:owner")
                    .setParameter("system", aship.getSystem())
                    .setParameter("x", aship.getX())
                    .setParameter("y", aship.getY())
                    .setParameter("owner", aship.getOwner())
                    .list());

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
            int repaired = 0;

            for (Ship aship : fleet.getShips())
            {
                if(aship.needRepair())
                {
                    for (WerftObject shipyard : shipyards)
                    {
                        if (shipyard.repairShip(aship, false))
                        {
                            repaired++;
                        }
                    }
                }
            }

            int shipid = ermittleIdEinesGeeignetenSchiffsDerFlotte(fleet);

            t.setVar("jscript.reloadmain.ship", shipid);
            t.setVar("fleetmgnt.message", "Es wurden " + repaired + " Schiffe repariert.");

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

        org.hibernate.Session db = getDB();
        User user = (User) getUser();

        List<Ship> ships = Common.cast(db.createQuery("from Ship " +
                "where id>0 and owner=:owner and fleet=:fleet order by id")
                .setEntity("owner", user)
                .setEntity("fleet", fleet)
                .list());

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

        org.hibernate.Session db = getDB();
        User user = (User) getUser();

        List<Ship> ships = Common.cast(db.createQuery("from Ship " +
                "where id>0 and owner=:owner and fleet=:fleet order by id")
                .setEntity("owner", user)
                .setEntity("fleet", fleet)
                .list());

        for(Ship ship : ships)
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

		org.hibernate.Session db = getDB();
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

		List<?> ships = db.createQuery("from Ship where id>0 and owner=:owner and fleet=:fleet order by id")
				.setEntity("owner", user)
				.setEntity("fleet", fleet)
				.list();

		Set<WerftObject> werften = new HashSet<>();
        boolean hasTanker = false;
		for (Object ship1 : ships)
		{
			Ship ship = (Ship) ship1;

			ShipTypeData shiptype = ship.getTypeData();
			Location loc = ship.getLocation();

			//Find shipyards
			if (shiptype.getWerft() > 0)
			{
				WerftObject werft = (WerftObject) db.createQuery("from ShipWerft where ship=:ship")
						.setInteger("ship", ship.getId())
						.uniqueResult();
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

			t.setVar("ship.id", ship.getId(),
					"ship.name", Common._plaintitle(ship.getName()),
					"ship.type.name", shiptype.getNickname(),
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
			buildableShips.addAll(werft.getBuildShipList());
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
		long asteroidcount = (Long) db.createQuery("select count(id) from Base where system=:system and x=:x and y=:y and owner=:owner")
				.setParameter("system", aship.getSystem())
				.setParameter("x", aship.getX())
				.setParameter("y", aship.getY())
				.setParameter("owner", user)
				.uniqueResult();

		if (asteroidcount > 0)
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
			if (asteroidcount > 0)
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

		List<?> shiptypes = db.createQuery("select s.shiptype,count(*) " +
				"from Ship as s " +
				"where s.owner=:owner and locate(:flag,s.shiptype.flags)!=0 and s.docked='' and (" + sectorstring + ") " +
				"group by s.shiptype")
				.setEntity("owner", user)
				.setString("flag", ShipTypeFlag.JAEGER.getFlag())
				.list();
		for (Object shiptype1 : shiptypes)
		{
			ShipType shiptype = (ShipType) ((Object[]) shiptype1)[0];

			t.setVar("jaegertype.id", shiptype.getId(),
					"jaegertype.name", shiptype.getNickname());

			t.parse("jaegertypes.list", "jaegertypes.listitem", true);
		}

		// Flottenliste bauen
		t.setBlock("_FLEETMGNT", "fleetcombine.listitem", "fleetcombine.list");
		List<?> fleetList = db.createQuery("select distinct s.fleet from Ship as s " +
				"where s.system=:sys and s.x=:x and s.y=:y AND docked='' AND owner=:owner AND s.fleet!=:fleet")
				.setInteger("sys", aship.getSystem())
				.setInteger("x", aship.getX())
				.setInteger("y", aship.getY())
				.setEntity("owner", user)
				.setEntity("fleet", fleet)
				.list();

		for (Object aFleetList : fleetList)
		{
			ShipFleet afleet = (ShipFleet) aFleetList;

			long count = (Long) db.createQuery("select count(*) from Ship where fleet=:fleet")
					.setEntity("fleet", afleet)
					.iterate().next();

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
		org.hibernate.Session db = getDB();
		Ship aship = getOneFleetShip(fleet);
		return Common.cast(db.createQuery("from Base where system=:system and x=:x and y=:y and owner=:owner")
				.setParameter("system", aship.getSystem())
				.setParameter("x", aship.getX())
				.setParameter("y", aship.getY())
				.setParameter("owner", aship.getOwner())
				.list());
	}

	/**
	 * Gibt irgendein Schiff aus der Flotte zurueck.
	 *
	 * @return Irgendein Schiff.
	 */
	private Ship getOneFleetShip(ShipFleet fleet)
	{
		org.hibernate.Session db = getDB();
		return (Ship) db.createQuery("from Ship where id>0 and fleet=:fleet")
				.setEntity("fleet", fleet)
				.setMaxResults(1)
				.uniqueResult();
	}

	/**
	 * Gibt die Anzahl Ganymeds im Sektor zurueck.
	 *
	 * @return Anzahl Ganymeds.
	 */
	private long getGanymedCount(ShipFleet fleet)
	{
		org.hibernate.Session db = getDB();
		Ship aship = getOneFleetShip(fleet);
		return (Long) db.createQuery("select count(id) from ShipWerft where ship.system=:system and ship.x=:x and ship.y=:y")
				.setParameter("system", aship.getSystem())
				.setParameter("x", aship.getX())
				.setParameter("y", aship.getY())
				.uniqueResult();
	}
}
