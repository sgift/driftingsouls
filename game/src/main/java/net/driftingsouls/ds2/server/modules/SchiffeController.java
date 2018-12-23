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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.RedirectViewResult;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParam;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitType;
import net.driftingsouls.ds2.server.werften.WerftObject;
import net.driftingsouls.ds2.server.werften.WerftQueueEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Die Schiffsliste.
 *
 * @author Christopher Jung
 */
@Module(name = "schiffe")
public class SchiffeController extends Controller
{
	private static final Log log = LogFactory.getLog(SchiffeController.class);

	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public SchiffeController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;
	}

	/**
	 * Aendert den Anzeigemodus fuer den Cargo.
	 *
	 * @param mode Der Anzeigemodus fuer den Cargo (<code>carg</code> oder <code>norm</code>)
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult changeModeAction(String mode)
	{
		if (mode.equals("carg") || mode.equals("norm"))
		{
			User user = (User)getUser();
			user.setUserValue(WellKnownUserValue.TBLORDER_SCHIFFE_MODE, mode);
		}

		return new RedirectViewResult("default");
	}

	/**
	 * Aendert den Sortierungsmodus fuer die Schiffe.
	 *
	 * @param order Das neue Sortierkriterium
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult changeOrderAction(String order)
	{
		if (Common.inArray(order, new String[]{"id", "name", "type", "sys", "crew", "hull", "alarm", "e"}))
		{
			User user = (User)getUser();
			user.setUserValue(WellKnownUserValue.TBLORDER_SCHIFFE_ORDER, order);
		}

		return new RedirectViewResult("default");
	}

	/**
	 * Aendert den Anzeigemodus fuer gelandete Jaeger.
	 *
	 * @param showLJaeger Falls != 0 werden gelandete Jaeger angezeigt
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult changeJDockedAction(int showLJaeger)
	{
		User user = (User)getUser();
		user.setUserValue(WellKnownUserValue.TBLORDER_SCHIFFE_SHOWJAEGER, showLJaeger);

		return new RedirectViewResult("default");
	}

	/**
	 * Aendert den Anzeigemodus fuer Handelsposten.
	 *
	 * @param showHandelsposten Falls <code>true</code> werden nur Handelsposten angezeigt
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult changeHandelsposten(boolean showHandelsposten)
	{
		User user = (User)getUser();
		user.setUserValue(WellKnownUserValue.TBLORDER_SCHIFFE_SHOWHANDELSPOSTEN, showHandelsposten);

		return new RedirectViewResult("default");
	}

	private static final int MAX_SHIPS_PER_PAGE = 250;

	/**
	 * Zeigt die Schiffsliste an.
	 *
	 * @param only Die anzuzeigende Schiffsart. Falls leer werden alle Schiffe angezeigt
	 * @param low Falls != 0 werden alle Schiffe mit Mangel angezeigt
	 * @param crewless Falls != 0 werden alle Schiffe ohne Crew angezeigt
	 * @param listoffset Der Offset innerhalb der Liste der Schiffe
	 * @param kampfOnly Falls != 0 werden nur Kriegsschiffe der Schiffsklasse mit der angegebenen ID angezeigt
	 */
	@Action(value = ActionType.DEFAULT, readOnly = true)
	public TemplateEngine defaultAction(String only, int low, int crewless, int listoffset, @UrlParam(name = "kampf_only") int kampfOnly)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		t.setVar("global.low", low,
				"global.crewless", crewless,
				"global.only", only,
				"user.race", user.getRace());

		String ord = user.getUserValue(WellKnownUserValue.TBLORDER_SCHIFFE_ORDER);
		int showjaeger = user.getUserValue(WellKnownUserValue.TBLORDER_SCHIFFE_SHOWJAEGER);
		boolean showHandelsposten = user.getUserValue(WellKnownUserValue.TBLORDER_SCHIFFE_SHOWHANDELSPOSTEN);

		Map<String, String> ordermapper = new HashMap<>();
		ordermapper.put("id", "s.id");
		ordermapper.put("name", "s.name,s.id");
		ordermapper.put("type", "s.shiptype,s.id");
		ordermapper.put("sys", "s.system,s.x+s.y,s.id");
		ordermapper.put("crew", "s.crew,s.id");
		ordermapper.put("hull", "s.hull,s.id");
		ordermapper.put("alarm", "s.alarm desc,s.id");
		ordermapper.put("e", "s.e,s.id");

		String ow = ordermapper.get(ord);

		String query = "select s from Ship as s left join s.modules m " +
				"where s.id>0 and s.owner=:owner and ";

		if (low != 0)
		{
			query += "(locate('mangel_nahrung',s.status)!=0 or locate('mangel_reaktor',s.status)!=0) and locate('nocrew',s.status)=0 and ";
		}
		if (crewless != 0)
		{
			query += "((s.modules is not null and s.crew < (select crew from ShipModules where id=s.modules)) or s.crew < (select crew from ShipType where id = s.shiptype)) and ";
		}

		if (only.equals("kampf") && (showjaeger == 0))
		{
			query += "locate('l ',s.docked)=0 and ";
		}

		switch (only)
		{
			case "tank":
				query += "s.shiptype.shipClass=3 order by " + ow;
				break;
			case "mine":
				query += "s.shiptype.shipClass=23 order by " + ow;
				break;
			case "versorger":
				query += "(locate('versorger',s.shiptype.flags)!=0 or (s.modules is not null and locate('versorger',m.flags)!=0)) order by " + ow;
				break;
			case "def":
				query += "s.shiptype.shipClass=10 order by " + ow;
				break;
			case "werften":
				query += "s.shiptype.werft>0 order by " + ow;
				break;
			case "sensor":
				query += "(s.shiptype.shipClass=13 or s.shiptype.shipClass=11) order by " + ow;
				break;
			case "cargo":
				if( showHandelsposten )
				{
					query += "(locate('tradepost',s.status)!=0 or locate('tradepost',s.shiptype.flags)!=0 or (s.modules is not null and locate('tradepost',m.flags)!=0)) and ";
				}

				query += "s.shiptype.shipClass=8 order by " + ow;

				break;
			case "trans":
				query += "s.shiptype.shipClass=1 order by " + ow;
				break;
			case "zivil":
				query += "(locate('=',s.shiptype.weapons)=0 and (s.modules is null or locate('=',m.weapons)=0)) order by " + ow;
				break;
			case "kampf":
				String sql_only;

				if (kampfOnly == 0)
				{
					sql_only = "s.shiptype.shipClass in (2,4,5,6,7,9,15,16,17,21)";
				}
				else
				{
					sql_only = "s.shiptype.shipClass=" + kampfOnly;
					t.setVar("global.kampf_only", kampfOnly);
				}
				query += sql_only + " order by " + ow;
				break;
			default:
				query += "s.shiptype.shipClass > -1 order by " + ow;
				break;
		}

		switch (only)
		{
			case "cargo":
				t.setVar("only.stationen", 1,
						"only.stationen.showHandelsposten", (showHandelsposten ? "checked=\"checked\"" : ""));
				break;
			case "tank":
				t.setVar("only.tank", 1);
				break;
			case "kampf":
				t.setVar("only.kampf", 1,
						"only.kampf.showljaeger", (showjaeger == 1 ? "checked=\"checked\"" : ""));

				if (kampfOnly == 0)
				{
					t.setVar("only.kampf.selected-1", "selected=\"selected\"");
				}
				else
				{
					t.setVar("only.kampf.selected" + kampfOnly, "selected=\"selected\"");
				}
				break;
			default:
				t.setVar("only.other", 1);
				break;
		}

		int shiplistcount = 0;

		t.setBlock("_SCHIFFE", "schiffe.listitem", "schiffe.list");
		t.setBlock("schiffe.listitem", "schiffe.resitem", "schiffe.reslist");
		t.setBlock("schiffe.listitem", "schiffe.unititem", "schiffe.unitlist");

		if (listoffset > 0)
		{
			//prefoffset is 0 for first page -> can't use it in if
			t.setVar("schiffe.hasprevoffset", 1);
			t.setVar("schiffe.prevoffset", listoffset - MAX_SHIPS_PER_PAGE);
		}

		List<?> ships = db.createQuery(query)
				.setEntity("owner", user)
				.setMaxResults(MAX_SHIPS_PER_PAGE + 1)
				.setFirstResult(listoffset)
				.list();
		for (Object ship1 : ships)
		{
			Ship ship = (Ship) ship1;

			t.start_record();

			shiplistcount++;

			if (shiplistcount > MAX_SHIPS_PER_PAGE)
			{
				t.setVar("schiffe.nextoffset", listoffset + MAX_SHIPS_PER_PAGE);
				break;
			}

			gibSchiffAus(only, low, t, db, ship);
		}
		return t;
	}

	private void gibSchiffAus(String only, int low, TemplateEngine t, Session db, Ship ship)
	{
		ShipTypeData shiptype = ship.getTypeData();

		Cargo cargo = ship.getCargo();

		if (only.equals("zivil") && shiptype.isMilitary())
		{
			return;
		}

		int nr = 0;
		int er = 0;

		boolean ok = false;
		if (low != 0)
		{
			if (ship.getStatus().contains("mangel_nahrung"))
			{
				nr = 1;
			}
			else
			{
				nr = low + 1;
			}

			if (ship.getStatus().contains("mangel_reaktor"))
			{
				er = low / 2 - 1;
			}
			else
			{
				er = low;
			}
		}

		if (!ok)
		{
			String offi = null;

			if (ship.getStatus().contains("offizier"))
			{
				Offizier offizier = ship.getOffizier();
				if (offizier != null)
				{
					offi = " <a class=\"forschinfo\" href=\"" + Common.buildUrl("default", "module", "choff", "off", offizier.getID()) + "\"><img style=\"vertical-align:middle\" src=\"" + offizier.getPicture() + "\" alt=\"Rang " + offizier.getRang() + "\" /></a>";
				}
			}

			String crewcolor = "#ffffff";
			if (ship.getCrew() < shiptype.getCrew() / 2)
			{
				crewcolor = "#ff0000";
			}
			else if (ship.getCrew() < shiptype.getCrew())
			{
				crewcolor = "#ffcc00";
			}

			String hullcolor = "#ffffff";
			if (ship.getHull() < shiptype.getHull() / 2)
			{
				hullcolor = "#ff0000";
			}
			else if (ship.getHull() < shiptype.getHull())
			{
				hullcolor = "#ffcc00";
			}

			if (shiptype.getWerft() != 0)
			{
				WerftObject werft = (WerftObject) db.createQuery("from ShipWerft where ship=:ship")
						.setEntity("ship", ship)
						.uniqueResult();
				if (werft == null)
				{
					log.warn("Schiff " + ship.getId() + " hat keinen Werfteintrag");
				}
				else
				{
					if (werft.getKomplex() != null)
					{
						werft = werft.getKomplex();
					}

					final List<WerftQueueEntry> entries = werft.getBuildQueue();
					final int totalSlots = werft.getWerftSlots();
					int usedSlots = 0;
					int buildingCount = 0;
					String imBau = "";
					for (WerftQueueEntry entry : entries)
					{
						if (entry.isScheduled())
						{
							usedSlots += entry.getSlots();
							buildingCount++;
							imBau = imBau + "<br />Aktuell im Bau: " + entry.getBuildShipType().getNickname() + " <img src='./data/interface/time.gif' alt='Dauer: ' />" + entry.getRemainingTime();
						}
					}

					StringBuilder popup = new StringBuilder(100);
					popup.append("Belegte Werftslots: <img style='vertical-align:middle;border:0px' src='./data/interface/schiffinfo/werftslots.png' alt='' />").append(usedSlots).append("/").append(totalSlots).append("<br />");
					popup.append("Im Bau: ").append(buildingCount).append(" Schiffe<br />");
					popup.append("In der Warteschlange: ").append(entries.size() - buildingCount);
					popup.append(imBau);

					t.setVar("ship.werft.popup", popup.toString(),
							"ship.werft.entries", entries.size(),
							"ship.werft.building", 1);
				}
			}

			t.setVar("ship.id", ship.getId(),
					"ship.name", Common._plaintitle(ship.getName()),
					"ship.battle", ship.getBattle() != null ? ship.getBattle().getId() : 0,
					"ship.type", ship.getType(),
					"ship.type.name", shiptype.getNickname(),
					"ship.location", ship.getLocation().displayCoordinates(false),
					"ship.location.url", ship.getLocation().urlFragment(),
					"ship.e", Common.ln(ship.getEnergy()),
					"ship.hull", Common.ln(ship.getHull()),
					"ship.hullcolor", hullcolor,
					"ship.image", shiptype.getPicture(),
					"ship.crew", Common.ln(ship.getCrew()),
					"ship.nahrungcargo", Common.ln(ship.getNahrungCargo()),
					"ship.mangel_nahrung", (ship.getStatus().contains("mangel_nahrung")),
					"ship.versorger", shiptype.hasFlag(ShipTypeFlag.VERSORGER),
					"ship.feedingstatus", (ship.getEinstellungen().isFeeding() && !ship.getEinstellungen().isAllyFeeding()) ? 1 : (ship.getEinstellungen().isFeeding()) ? 2 : 3,
					"ship.unitspace", Common.ln(shiptype.getUnitSpace()),
					"ship.alarm", ship.getAlarm().name().toLowerCase(),
					"ship.offi", offi,
					"ship.crewcolor", crewcolor,
					"ship.fleet", ship.getFleet() != null ? ship.getFleet().getId() : 0,
					"ship.ablativearmor", Common.ln(ship.getAblativeArmor()),
					"ship.shields", Common.ln(ship.getShields()),
					"ship.werft", shiptype.getWerft(),
					"ship.adocks", shiptype.getADocks(),
					"ship.jdocks", shiptype.getJDocks(),
					"ship.docks", shiptype.getADocks() + shiptype.getJDocks(),
					"schiffe.reslist", "",
					"schiffe.unitlist", "");

			if (ship.getFleet() != null)
			{
				t.setVar("ship.fleet.name", Common._plaintitle(ship.getFleet().getName()));
			}

			if (ship.isDocked())
			{
				Ship master = ship.getBaseShip();
				if (master != null)
				{
					t.setVar("ship.docked.name", master.getName(),
							"ship.docked.id", master.getId());
				}
			}
			else if (ship.isLanded())
			{
				Ship master = ship.getBaseShip();
				if (master != null)
				{
					t.setVar("ship.landed.name", master.getName(),
							"ship.landed.id", master.getId());
				}
			}

			if (shiptype.getADocks() > 0)
			{
				t.setVar("ship.adocks.docked", ship.getDockedCount());
			}

			if (shiptype.getJDocks() > 0)
			{
				t.setVar("ship.jdocks.docked", ship.getLandedCount());
			}

			if ((shiptype.getShipClass() == ShipClasses.AWACS) || (shiptype.getShipClass() == ShipClasses.FORSCHUNGSKREUZER))
			{
				int sensorrange = ship.getEffectiveScanRange();

				if ((sensorrange > 0) && (ship.getCrew() >= shiptype.getMinCrew() / 3))
				{
					Nebel.Typ nebel = Nebel.getNebula(ship.getLocation());
					if (nebel == null || nebel.allowsScan())
					{
						t.setVar("ship.longscan", 1,
								"ship.system", ship.getSystem(),
								"ship.x", ship.getX(),
								"ship.y", ship.getY());
					}
				}
			}

			int wa = 0;

			ResourceList reslist = cargo.getResourceList();
			for (ResourceEntry res : reslist)
			{
				String color = "";
				if (low != 0)
				{
					if (res.getId().equals(Resources.NAHRUNG))
					{
						if (nr <= low)
						{
							color = "red";
						}
					}
					else if (Common.inArray(res.getId(), new ResourceID[]{Resources.URAN, Resources.DEUTERIUM, Resources.ANTIMATERIE}))
					{
						wa++;
						if (er <= low / 2)
						{
							color = "red";
						}
					}
					/*if (res.getId().equals(Resources.BATTERIEN))
					{
						color = "";
						wa--;
					}*/
					else if (!Common.inArray(res.getId(), new ResourceID[]{Resources.NAHRUNG, Resources.URAN, Resources.DEUTERIUM, Resources.ANTIMATERIE, Resources.BATTERIEN}))
					{
						color = "";
					}

					if ((res.getId() == Resources.URAN) && (shiptype.getRu() <= 0))
					{
						color = "";
					}
					else if ((res.getId() == Resources.ANTIMATERIE) && (shiptype.getRa() <= 0))
					{
						color = "";
					}
					else if ((res.getId() == Resources.DEUTERIUM) && (shiptype.getRd() <= 0))
					{
						color = "";
					}
				}

				t.setVar("res.image", res.getImage(),
						"res.color", color,
						"res.count", res.getCargo1(),
						"res.plainname", res.getPlainName());

				t.parse("schiffe.reslist", "schiffe.resitem", true);
			}

			if (shiptype.getCargo() != 0)
			{
				t.setVar("ship.restcargo", Common.ln(shiptype.getCargo() - cargo.getMass()),
						"ship.restcargo.show", 1);
			}
			if ((wa == 0) && (low != 0))
			{
				t.setVar("ship.e.none", 1);
			}

			UnitCargo unitcargo = ship.getUnits();

			if (unitcargo != null && !unitcargo.isEmpty())
			{
				for (Entry<UnitType, Long> unit : unitcargo.getUnitMap().entrySet())
				{
					UnitType unittype = unit.getKey();

					t.setVar("unit.id", unittype.getId(),
							"unit.picture", unittype.getPicture(),
							"unit.count", unit.getValue(),
							"unit.name", unittype.getName());

					t.parse("schiffe.unitlist", "schiffe.unititem", true);
				}

				t.setVar("ship.unitspace", shiptype.getUnitSpace() - unitcargo.getMass());
			}

			t.parse("schiffe.list", "schiffe.listitem", true);

		}
		t.stop_record();
		t.clear_record();
	}
}
