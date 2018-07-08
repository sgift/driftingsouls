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
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.*;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.Weapon;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.*;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Die Schiffstypen-Infos.
 *
 * @author Christopher Jung
 */
@KeinLoginNotwendig
@Module(name = "schiffinfo")
public class SchiffInfoController extends Controller
{
	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public SchiffInfoController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;

		setPageTitle("Schiffstyp");
	}

	private void validiereSchiffstype(ShipType ship)
	{
		User user = (User) getUser();

		if ((ship == null) ||
				(ship.isHide() && ((user == null) || !hasPermission(WellKnownPermission.SCHIFFSTYP_VERSTECKTE_SICHTBAR))))
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

	private void outPrerequisites(TemplateEngine t, ShipBaubar shipBuildData)
	{
		if (getUser() != null)
		{
			for (int i = 1; i <= 3; i++)
			{
				if (shipBuildData.getRes(i) != null)
				{
					User user = (User) getUser();
					Forschung research = shipBuildData.getRes(i);
					String cssClass = "error";
					//Has the user this research?
					if (user.hasResearched(research))
					{
						cssClass = "ok";
					}

					t.setVar("shiptype.tr" + i, research.getID(),
							"shiptype.tr" + i + ".name", Common._title(research.getName()),
							"shiptype.tr" + i + ".status", cssClass);
				}
			}
		}
		else
		{
			for (int i = 1; i <= 3; i++)
			{
				if (shipBuildData.getRes(i) != null)
				{
					Forschung f = shipBuildData.getRes(i);

					t.setVar("shiptype.tr" + i, f.getID(),
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

	private void outShipCost(TemplateEngine t, ShipBaubar shipBuildData)
	{
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
	public TemplateEngine defaultAction(ShipType ship)
	{
		validiereSchiffstype(ship);

		org.hibernate.Session db = getDB();
		User user = (User) getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);

		t.setVar("global.login", (getUser() != null));

		if (ship.isHide() && (user != null) && hasPermission(WellKnownPermission.SCHIFFSTYP_VERSTECKTE_SICHTBAR))
		{
			t.setVar("shiptype.showinvisible", 1);
		}

		ShipBaubar shipBuildData = ermittleBauInformationen(db, ship.getTypeId());

		//Kann der User sehen, dass das Schiff baubar ist?
		Forschung visible = ermittleSichtbarkeitDesSchiffstyps(user, shipBuildData);

		if (visible != null)
		{
			shipBuildData = null;

			if ((user != null) && hasPermission(WellKnownPermission.SCHIFFSTYP_VERSTECKTE_SICHTBAR))
			{
				t.setVar("shiptype.showbuildable", 1,
						"shiptype.visibletech", visible.getID());
			}
		}

		if ((user != null) && hasPermission(WellKnownPermission.SCHIFFSTYP_NPCKOSTEN_SICHTBAR))
		{
			OrderableShip order = (OrderableShip) db.createQuery("from OrderableShip where shipType=:type")
				.setEntity("type", ship)
				.setMaxResults(1)
				.uniqueResult();

			if (order != null)
			{
				t.setVar("shiptype.showorderable", 1,
						"shiptype.ordercost", order.getCost());
			}
		}

		zeigeWaffenlisteAn(t, ship);

		// Flags auflisten
		zeigeFlagListeAn(t, ship);

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
			outPrerequisites(t, shipBuildData);
		}

		//Produktionskosten anzeigen, sofern das Schiff baubar ist
		if (shipBuildData != null)
		{
			outShipCost(t, shipBuildData);
		}

		return t;
	}

	private void zeigeFlagListeAn(TemplateEngine t, ShipType ship)
	{
		t.setBlock("_SCHIFFINFO", "shiptypeflags.listitem", "shiptypeflags.list");
		t.setVar("shiptypeflags.list", "");

		Set<ShipTypeFlag> flaglist = new TreeSet<>(ship.getFlags());
		for (ShipTypeFlag aFlaglist : flaglist)
		{
			t.setVar("shiptypeflag.name", aFlaglist.getLabel(),
					"shiptypeflag.description", aFlaglist.getDescription());

			t.parse("shiptypeflags.list", "shiptypeflags.listitem", true);
		}
	}

	private void zeigeWaffenlisteAn(TemplateEngine t, ShipType ship)
	{
		Map<String, Integer> weapons = ship.getWeapons();
		Map<String, Integer> maxheat = ship.getMaxHeat();

		t.setBlock("_SCHIFFINFO", "shiptype.weapons.listitem", "shiptype.weapons.list");
		for (Map.Entry<String, Integer> entry : weapons.entrySet())
		{
			int count = entry.getValue();
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
			descrip.append(weapon.getApCost());
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
			if (!weapon.getMunitionstypen().isEmpty())
			{
				descrip.append("Munition<br />");
			}
			else
			{
				descrip.append(Common.ln(weapon.getBaseDamage()));
				descrip.append("/");
				descrip.append(Common.ln(weapon.getShieldDamage()));
				descrip.append("/");
				descrip.append(Common.ln(weapon.getSubDamage()));
				descrip.append("<br />");
			}

			descrip.append("Trefferws (C/J/Torp): ");
			if (!weapon.getMunitionstypen().isEmpty())
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

	private Forschung ermittleSichtbarkeitDesSchiffstyps(User user, ShipBaubar shipBuildData)
	{
		Forschung visible = null;

		if (shipBuildData != null)
		{
			for (Forschung forschung : shipBuildData.getBenoetigteForschungen())
			{
				if (!forschung.isVisibile(user) && (user == null || !user.hasResearched(forschung.getBenoetigteForschungen())))
				{

					visible = forschung;
				}
			}
		}
		return visible;
	}
}
