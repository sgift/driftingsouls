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
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.ModuleSlots;
import net.driftingsouls.ds2.server.config.NoSuchSlotException;
import net.driftingsouls.ds2.server.config.NoSuchWeaponException;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.Weapon;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserResearch;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateController;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;

import java.util.Arrays;
import java.util.Map;

/**
 * Die Schiffstypen-Infos.
 *
 * @author Christopher Jung
 */
@Module(name = "schiffinfo")
public class SchiffInfoController extends TemplateController
{
	/**
	 * Konstruktor.
	 *
	 * @param context Der zu verwendende Kontext
	 */
	public SchiffInfoController(Context context)
	{
		super(context);

		setPageTitle("Schiffstyp");
	}

	private void validiereSchiffstype(ShipType ship)
	{
		User user = (User) getUser();

		if ((ship == null) ||
				(ship.isHide() && ((user == null) || !hasPermission("schiffstyp", "versteckteSichtbar"))))
		{

			throw new ValidierungException("Über diesen Schiffstyp liegen leider keine Daten vor");
		}
	}

	private ShipBaubar ermittleBauInformationen(Session db, int ship)
	{
		return (ShipBaubar) db.createQuery("from ShipBaubar where type=:type")
				.setInteger("type", ship)
				.setMaxResults(1)
				.uniqueResult();
	}

	private void outPrerequisites(ShipBaubar shipBuildData)
	{
		TemplateEngine t = getTemplateEngine();

		if (getUser() != null)
		{
			for (int i = 1; i <= 3; i++)
			{
				if (shipBuildData.getRes(i) != 0)
				{
					User user = (User) getUser();
					Forschung research = Forschung.getInstance(shipBuildData.getRes(i));
					UserResearch userResearch = user.getUserResearch(research);
					String cssClass = "error";
					//Has the user this research?
					if (userResearch != null)
					{
						cssClass = "ok";
					}

					t.setVar("shiptype.tr" + i, shipBuildData.getRes(i),
							"shiptype.tr" + i + ".name", Common._title(research.getName()),
							"shiptype.tr" + i + ".status", cssClass);
				}
			}
		}
		else
		{
			for (int i = 1; i <= 3; i++)
			{
				if (shipBuildData.getRes(i) != 0)
				{
					Forschung f = Forschung.getInstance(shipBuildData.getRes(i));

					t.setVar("shiptype.tr" + i, shipBuildData.getRes(i),
							"shiptype.tr" + i + ".name", Common._title(f.getName()));
				}
			}
		}
		String race;
		if (shipBuildData.getRace() == -1)
		{
			race = "Alle";
		}
		else
		{
			race = Rassen.get().rasse(shipBuildData.getRace()).getName();
		}

		t.setVar("shiptype.race", race);
	}

	private void outShipCost(ShipBaubar shipBuildData)
	{
		TemplateEngine t = getTemplateEngine();

		t.setVar("shiptype.cost.energie", shipBuildData.getEKosten(),
				"shiptype.cost.crew", shipBuildData.getCrew(),
				"shiptype.cost.dauer", shipBuildData.getDauer(),
				"shiptype.cost.werftslots", shipBuildData.getWerftSlots());

		t.setBlock("_SCHIFFINFO", "res.listitem", "res.list");

		Cargo costs = shipBuildData.getCosts();
		ResourceList reslist = costs.getResourceList();
		for (ResourceEntry res : reslist)
		{
			t.setVar("res.name", res.getName(),
					"res.image", res.getImage(),
					"res.count", res.getCargo1());
			t.parse("res.list", "res.listitem", true);
		}
	}

	@Action(ActionType.DEFAULT)
	public void defaultAction(ShipType ship)
	{
		validiereSchiffstype(ship);

		org.hibernate.Session db = getDB();
		User user = (User) getUser();
		TemplateEngine t = getTemplateEngine();

		t.setVar("global.login", (getUser() != null));

		if (ship.isHide() && (user != null) && hasPermission("schiffstyp", "versteckteSichtbar"))
		{
			t.setVar("shiptype.showinvisible", 1);
		}

		ShipBaubar shipBuildData = ermittleBauInformationen(db, ship.getTypeId());

		//Kann der User sehen, dass das Schiff baubar ist?
		int visible = ermittleSichtbarkeitDesSchiffstyps(user, shipBuildData);

		if (visible > 0)
		{
			shipBuildData = null;

			if ((user != null) && hasPermission("schiffstyp", "versteckteSichtbar"))
			{
				t.setVar("shiptype.showbuildable", 1,
						"shiptype.visibletech", visible);
			}
		}

		if ((user != null) && hasPermission("schiffstyp", "npckostenSichtbar"))
		{
			OrderableShip order = (OrderableShip) db.get(OrderableShip.class, ship.getTypeId());

			if (order != null)
			{
				t.setVar("shiptype.showorderable", 1,
						"shiptype.ordercost", order.getCost());
			}
		}

		zeigeWaffenlisteAn(ship);

		// Flags auflisten
		zeigeFlagListeAn(ship);

		// Module
		StringBuilder moduletooltip = new StringBuilder();
		String[] modulelist = new String[0];
		if (ship.getTypeModules().length() != 0)
		{
			modulelist = StringUtils.split(ship.getTypeModules(), ';');

			for (String aModulelist : modulelist)
			{
				String[] amodule = StringUtils.split(aModulelist, ':');
				try
				{
					moduletooltip.append(ModuleSlots.get().slot(amodule[1]).getName());
					moduletooltip.append("<br />");
				}
				catch (NoSuchSlotException e)
				{
					moduletooltip.append("<span style='color:red'>UNGUELTIGER SLOTTYP ");
					moduletooltip.append(amodule[1]);
					moduletooltip.append("</span><br />");
				}
			}
		}

		t.setVar("shiptype.nickname", ship.getNickname(),
				"shiptype.id", ship.getTypeId(),
				"shiptype.class", ship.getShipClass().getSingular(),
				"shiptype.image", ship.getPicture(),
				"shiptype.ru", Common.ln(ship.getRu()),
				"shiptype.rd", Common.ln(ship.getRd()),
				"shiptype.ra", Common.ln(ship.getRa()),
				"shiptype.rm", Common.ln(ship.getRm()),
				"nahrung.image", Cargo.getResourceImage(Resources.NAHRUNG),
				"uran.image", Cargo.getResourceImage(Resources.URAN),
				"deuterium.image", Cargo.getResourceImage(Resources.DEUTERIUM),
				"antimaterie.image", Cargo.getResourceImage(Resources.ANTIMATERIE),
				"shiptype.buildable", shipBuildData != null,
				"shiptype.cost", ship.getCost(),
				"shiptype.heat", ship.getHeat(),
				"shiptype.size", ship.getSize(),
				"shiptype.sensorrange", ship.getSensorRange() + 1,
				"shiptype.eps", Common.ln(ship.getEps()),
				"shiptype.cargo", Common.ln(ship.getCargo()),
				"shiptype.crew", Common.ln(ship.getCrew()),
				"shiptype.nahrungcargo", Common.ln(ship.getNahrungCargo()),
				"shiptype.jdocks", ship.getJDocks(),
				"shiptype.adocks", ship.getADocks(),
				"shiptype.hull", Common.ln(ship.getHull()),
				"shiptype.panzerung", ship.getPanzerung(),
				"shiptype.ablativearmor", Common.ln(ship.getAblativeArmor()),
				"shiptype.shields", Common.ln(ship.getShields()),
				"shiptype.maxunitsize", ship.getMaxUnitSize(),
				"shiptype.deutfactor", ship.getDeutFactor(),
				"shiptype.hydro", Common.ln(ship.getHydro()),
				"shiptype.flagschiff", shipBuildData != null && shipBuildData.isFlagschiff(),
				"shiptype.recost", Common.ln(ship.getReCost()),
				"shiptype.torpedodef", ship.getTorpedoDef(),
				"shiptype.moduleslots", modulelist.length,
				"shiptype.moduleslots.desc", moduletooltip,
				"shiptype.werftslots", ship.getWerft());

		if (ship.getDescrip().length() == 0)
		{
			t.setVar("shiptype.description", Common._text("Keine Beschreibung verf&uuml;gbar"));
		}
		else
		{
			t.setVar("shiptype.description", Common._text(ship.getDescrip()));
		}

		if (ship.getUnitSpace() > 0)
		{
			t.setVar("shiptype.units", true,
					"shiptype.unitspace", Common.ln(ship.getUnitSpace()));
		}

		if (shipBuildData != null)
		{
			outPrerequisites(shipBuildData);
		}

		//Produktionskosten anzeigen, sofern das Schiff baubar ist
		if (shipBuildData != null)
		{
			outShipCost(shipBuildData);
		}

	}

	private void zeigeFlagListeAn(ShipType ship)
	{
		TemplateEngine t = getTemplateEngine();

		t.setBlock("_SCHIFFINFO", "shiptypeflags.listitem", "shiptypeflags.list");
		t.setVar("shiptypeflags.list", "");

		String[] flaglist = Ship.getShipTypeFlagList(ship);
		Arrays.sort(flaglist);
		for (String aFlaglist : flaglist)
		{
			if (aFlaglist.length() == 0)
			{
				continue;
			}
			t.setVar("shiptypeflag.name", ShipTypes.getShipTypeFlagName(aFlaglist),
					"shiptypeflag.description", ShipTypes.getShipTypeFlagDescription(aFlaglist));

			t.parse("shiptypeflags.list", "shiptypeflags.listitem", true);
		}
	}

	private void zeigeWaffenlisteAn(ShipType ship)
	{
		TemplateEngine t = getTemplateEngine();

		Map<String, String> weapons = Weapons.parseWeaponList(ship.getWeapons());
		Map<String, String> maxheat = Weapons.parseWeaponList(ship.getMaxHeat());

		t.setBlock("_SCHIFFINFO", "shiptype.weapons.listitem", "shiptype.weapons.list");
		for (Map.Entry<String, String> entry : weapons.entrySet())
		{
			int count = Integer.parseInt(entry.getValue());
			String weaponname = entry.getKey();

			Weapon weapon;
			try
			{
				weapon = Weapons.get().weapon(weaponname);
			}
			catch (NoSuchWeaponException e)
			{
				t.setVar("shiptype.weapon.name", "<span style=\"color:red\">UNKNOWN: weapon</span>",
						"shiptype.weapon.count", count,
						"shiptype.weapon.description", "");

				t.parse("shiptype.weapons.list", "shiptype.weapons.listitem", true);

				continue;
			}

			StringBuilder descrip = new StringBuilder(100);
			descrip.append("<span style=\\'font-size:12px\\'>");

			descrip.append("AP-Kosten: ");
			descrip.append(weapon.getAPCost());
			descrip.append("<br />");
			descrip.append("Energie-Kosten: ");
			descrip.append(Common.ln(weapon.getECost()));
			descrip.append("<br />");

			if (weapon.getSingleShots() > 1)
			{
				descrip.append("Sch&uuml;sse: ");
				descrip.append(weapon.getSingleShots());
				descrip.append("<br />");
			}

			descrip.append("Max. &Uuml;berhitzung: ");
			descrip.append(maxheat.get(weaponname));
			descrip.append("<br />");

			descrip.append("Schaden (H/S/Sub): ");
			if (weapon.getAmmoType().length > 0)
			{
				descrip.append("Munition<br />");
			}
			else
			{
				descrip.append(Common.ln(weapon.getBaseDamage(ship)));
				descrip.append("/");
				descrip.append(Common.ln(weapon.getShieldDamage(ship)));
				descrip.append("/");
				descrip.append(Common.ln(weapon.getSubDamage(ship)));
				descrip.append("<br />");
			}

			descrip.append("Trefferws (C/J/Torp): ");
			if (weapon.getAmmoType().length > 0)
			{
				descrip.append("Munition<br />");
			}
			else
			{
				descrip.append(weapon.getDefTrefferWS());
				descrip.append("/");
				descrip.append(weapon.getDefSmallTrefferWS());
				descrip.append("/");
				descrip.append(weapon.getTorpTrefferWS());
				descrip.append("<br />");
			}

			if (weapon.getAreaDamage() != 0)
			{
				descrip.append("Areadamage: ");
				descrip.append(weapon.getAreaDamage());
				descrip.append("<br />");
			}
			if (weapon.getDestroyable())
			{
				descrip.append("Durch Abwehrfeuer zerst&ouml;rbar<br />");
			}
			if (weapon.hasFlag(Weapon.Flags.DESTROY_AFTER))
			{
				descrip.append("Beim Angriff zerst&ouml;rt<br />");
			}
			if (weapon.hasFlag(Weapon.Flags.LONG_RANGE))
			{
				descrip.append("Gro&szlig;e Reichweite<br />");
			}
			if (weapon.hasFlag(Weapon.Flags.VERY_LONG_RANGE))
			{
				descrip.append("Sehr gro&szlig;e Reichweite<br />");
			}

			descrip.append("</span>");

			t.setVar("shiptype.weapon.name", weapon.getName(),
					"shiptype.weapon.count", count,
					"shiptype.weapon.description", descrip);

			t.parse("shiptype.weapons.list", "shiptype.weapons.listitem", true);
		}

		if (weapons.isEmpty())
		{
			t.setVar("shiptype.noweapons", 1);
		}
	}

	private int ermittleSichtbarkeitDesSchiffstyps(User user, ShipBaubar shipBuildData)
	{
		int visible = -1;

		if (shipBuildData != null)
		{
			for (int i = 1; i <= 3; i++)
			{
				if (shipBuildData.getRes(i) != 0)
				{
					Forschung research = Forschung.getInstance(shipBuildData.getRes(i));

					if (!research.isVisibile(user) &&
							(user == null || !user.hasResearched(research.getRequiredResearch(1)) ||
									!user.hasResearched(research.getRequiredResearch(2)) ||
									!user.hasResearched(research.getRequiredResearch(3))))
					{

						visible = shipBuildData.getRes(i);
					}
				}
			}
		}
		return visible;
	}
}
