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
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.*;
import net.driftingsouls.ds2.server.config.ModuleSlots;
import net.driftingsouls.ds2.server.config.NoSuchSlotException;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Schiffsmodul;
import net.driftingsouls.ds2.server.config.items.SchiffsmodulSet;
import net.driftingsouls.ds2.server.config.items.effects.*;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Munitionsdefinition;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.Weapon;
import net.driftingsouls.ds2.server.entities.statistik.StatItemLocations;
import net.driftingsouls.ds2.server.entities.statistik.StatUserCargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.*;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.modules.viewmodels.ItemViewModel;
import net.driftingsouls.ds2.server.ships.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Zeigt Informationen zu Items an.
 *
 * @author Christopher Jung
 */
@Module(name = "iteminfo")
public class ItemInfoController extends Controller
{
	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public ItemInfoController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;

		setPageTitle("Item");
	}

	private void colorize(StringBuilder effecttext, Object mod)
	{
		long value = ((Number) mod).longValue();
		if (value < 0)
		{
			effecttext.append("<span style=\"color:red\">");
		}
		else
		{
			effecttext.append("<span style=\"color:green\">");
		}
	}

	private String parseModuleModifiers(SchiffstypModifikation mods)
	{
		StringBuilder effecttext = new StringBuilder(300);

		if (mods.getRu() != 0)
		{
			colorize(effecttext, mods.getRu());
			effecttext.append("Reaktor <img src=\"").append(Cargo.getResourceImage(Resources.URAN)).append("\" alt=\"\" /> ").append(mods.getRu());
			effecttext.append("</span><br />\n");
		}

		if (mods.getRd() != 0)
		{
			colorize(effecttext, mods.getRd());
			effecttext.append("Reaktor <img src=\"").append(Cargo.getResourceImage(Resources.DEUTERIUM)).append("\" alt=\"\" /> ").append(mods.getRd());
			effecttext.append("</span><br />\n");
		}

		if (mods.getRa() != 0)
		{
			colorize(effecttext, mods.getRa());
			effecttext.append("Reaktor <img src=\"").append(Cargo.getResourceImage(Resources.ANTIMATERIE)).append("\" alt=\"\" /> ").append(mods.getRa());
			effecttext.append("</span><br />\n");
		}

		if (mods.getRm() != 0)
		{
			colorize(effecttext, mods.getRm());
			effecttext.append("Reaktor <img src=\"./data/interface/energie.gif\" alt=\"\" /> ").append(mods.getRm());
			effecttext.append("</span><br />\n");
		}

		if (mods.getEps() != 0)
		{
			colorize(effecttext, mods.getEps());
			effecttext.append("Energiespeicher ").append(mods.getEps());
			effecttext.append("</span><br />\n");
		}

		if (mods.getCost() != 0)
		{
			colorize(effecttext, -mods.getCost());
			effecttext.append("Flugkosten ").append(mods.getCost());
			effecttext.append("</span><br />\n");
		}

		if (mods.getHeat() != 0)
		{
			colorize(effecttext, -mods.getHeat());
			effecttext.append("&Uuml;berhitzung ").append(mods.getHeat());
			effecttext.append("</span><br />\n");
		}

		if (mods.getHull() != 0)
		{
			colorize(effecttext, mods.getHull());
			effecttext.append("H&uuml;lle ").append(mods.getHull());
			effecttext.append("</span><br />\n");
		}

		if (mods.getPanzerung() != 0)
		{
			colorize(effecttext, mods.getPanzerung());
			effecttext.append("Panzerung ").append(mods.getPanzerung());
			effecttext.append("</span><br />\n");
		}

		if (mods.getAblativeArmor() != 0)
		{
			colorize(effecttext, mods.getAblativeArmor());
			effecttext.append("Ablative Panzerung ").append(mods.getAblativeArmor());
			effecttext.append("</span><br />\n");
		}

		if (mods.getCargo() != 0)
		{
			colorize(effecttext, mods.getCargo());
			effecttext.append("Cargo ").append(mods.getCargo());
			effecttext.append("</span><br />\n");
		}

		if (mods.getCrew() != 0)
		{
			colorize(effecttext, mods.getCrew());
			effecttext.append("Crew ").append(mods.getCrew());
			effecttext.append("</span><br />\n");
		}

		if (mods.getNahrungCargo() != 0)
		{
			colorize(effecttext, mods.getNahrungCargo());
			effecttext.append("Nahrungsspeicher ").append(mods.getNahrungCargo());
			effecttext.append("</span><br />\n");
		}

		if (mods.getUnitSpace() != 0)
		{
			colorize(effecttext, mods.getUnitSpace());
			effecttext.append("Einheitenladeraum ").append(mods.getUnitSpace());
			effecttext.append("</span><br />\n");
		}

		if (mods.getMaxUnitSize() != 0)
		{
			colorize(effecttext, mods.getMaxUnitSize());
			effecttext.append("Maximale Einheitengr&ouml;&szlig;e ").append(mods.getMaxUnitSize());
			effecttext.append("</span><br />\n");
		}

		if (mods.getShields() != 0)
		{
			colorize(effecttext, mods.getShields());
			effecttext.append("Schilde ").append(mods.getShields());
			effecttext.append("</span><br />\n");
		}

		if (mods.getSize() != 0)
		{
			colorize(effecttext, -mods.getSize());
			effecttext.append("Gr&ouml;&szlig;e ").append(mods.getSize());
			effecttext.append("</span><br />\n");
		}

		if (mods.getJDocks() != 0)
		{
			colorize(effecttext, mods.getJDocks());
			effecttext.append("J&auml;gerdocks ").append(mods.getJDocks());
			effecttext.append("</span><br />\n");
		}

		if (mods.getADocks() != 0)
		{
			colorize(effecttext, mods.getADocks());
			effecttext.append("Externe Docks ").append(mods.getADocks());
			effecttext.append("</span><br />\n");
		}

		if (mods.getSensorRange() != 0)
		{
			colorize(effecttext, mods.getSensorRange());
			effecttext.append("Sensorreichweite ").append(mods.getSensorRange());
			effecttext.append("</span><br />\n");
		}

		if (mods.getHydro() != 0)
		{
			colorize(effecttext, mods.getHydro());
			effecttext.append("Produziert <img src=\"").append(Cargo.getResourceImage(Resources.NAHRUNG)).append("\" alt=\"\" />").append(mods.getHydro());
			effecttext.append("</span><br />\n");
		}

		if (mods.getDeutFactor() != 0)
		{
			colorize(effecttext, mods.getDeutFactor());
			effecttext.append("Sammelt <img src=\"").append(Cargo.getResourceImage(Resources.DEUTERIUM)).append("\" alt=\"\" />").append(mods.getDeutFactor());
			effecttext.append("</span><br />\n");
		}

		if (mods.getReCost() != 0)
		{
			colorize(effecttext, -mods.getReCost());
			effecttext.append("Wartungskosten ").append(mods.getReCost());
			effecttext.append("</span><br />\n");
		}

		if (mods.getWerft() != 0)
		{
			colorize(effecttext, mods.getWerft());
			effecttext.append("Werftslots ").append(mods.getWerft());
			effecttext.append("</span><br />\n");
		}

		if (!mods.getFlags().isEmpty())
		{
			Set<ShipTypeFlag> flags = mods.getFlags();
			effecttext.append(flags.stream().map(ShipTypeFlag::getLabel).collect(Collectors.joining("<br />")));
			effecttext.append("<br />\n");
		}

		Set<Schiffswaffenkonfiguration> weaponlist = mods.getWaffen();

		StringBuilder wpntext = new StringBuilder(50);
		for (Schiffswaffenkonfiguration weaponclass : weaponlist)
		{
			if (wpntext.length() > 0)
			{
				wpntext.append("<br />");
			}
			wpntext.append("<span class=\"nobr\">");
			if (Math.abs(weaponclass.getAnzahl()) > 1)
			{
				wpntext.append(weaponclass.getAnzahl()).append("x ");
			}
			wpntext.append(weaponclass.getWaffe().getName());
			wpntext.append("</span><br />[Hitze: ").append(weaponclass.getHitze()).append("]");
			if( weaponclass.getMaxUeberhitzung() != 0 ) {
				effecttext.append("<br />\n");
				wpntext.append("[Max-Hitze: ").append(weaponclass.getMaxUeberhitzung()).append("]");
			}
		}
		if( wpntext.length() > 0 )
		{
			effecttext.append(wpntext);
			effecttext.append("<br />\n");
		}

		return effecttext.toString();
	}

	/**
	 * Zeigt Details zu einem Item an.
	 *
	 * @param itemStr Die ID des anzuzeigenden Items
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine detailsAction(@UrlParam(name = "item") String itemStr, @UrlParam(name = "itemlist") String itemlistStr)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		if( itemlistStr != null && !itemlistStr.trim().isEmpty() )
		{
			itemStr = itemlistStr;
		}

		int itemid = -1;
		if (ItemID.isItemRID(itemStr))
		{
			itemid = ItemID.fromString(itemStr).getItemID();
		}
		else if (NumberUtils.isCreatable(itemStr))
		{
			itemid = Integer.parseInt(itemStr);
		}

		Item item = (Item) db.get(Item.class, itemid);

		if (item == null)
		{
			t.setVar("iteminfo.message", "Es ist kein Item mit dieser Identifikationsnummer bekannt");

			return t;
		}

		if (item.getAccessLevel() > user.getAccessLevel())
		{
			t.setVar("iteminfo.message", "Es ist kein Item mit dieser Identifikationsnummer bekannt");

			return t;
		}

		if (item.isUnknownItem() && !user.isKnownItem(itemid) && !hasPermission(WellKnownPermission.ITEM_UNBEKANNTE_SICHTBAR))
		{
			t.setVar("iteminfo.message", "Es ist kein Item mit dieser Identifikationsnummer bekannt");

			return t;
		}

		String name = Common._plaintitle(item.getName());
		if (item.getQuality().color().length() > 0)
		{
			name = "<span style=\"color:" + item.getQuality().color() + "\">" + name + "</span>";
		}

		t.setVar("iteminfo.details", 1,
				"item.picture", item.getPicture(),
				"item.largePicture", item.getLargePicture() != null ? item.getLargePicture() : item.getPicture(),
				"item.name", name,
				"item.cargo", item.getCargo(),
				"item.accesslevel", item.getAccessLevel(),
				"item.allyitem", item.getEffect().hasAllyEffect(),
				"item.class", item.getEffect().getType().getName(),
				"item.isspawnable", item.isSpawnableRess(),
				"item.description", Common._text(item.getDescription()));

		t.setBlock("_ITEMINFO", "itemdetails.entry", "itemdetails.entrylist");

		switch (item.getEffect().getType())
		{
		/*

			EFFECT_DRAFT_SHIP

		*/
			case DRAFT_SHIP:
			{
				IEDraftShip effect = (IEDraftShip) item.getEffect();

				ShipTypeData shiptype = Ship.getShipType(effect.getShipType());

				StringBuilder data = new StringBuilder(100);
				if (shiptype == null)
				{
					data.append("<span style=\"color:red\">Unbekannter Schiffstyp</span>");
				}
				else if (shiptype.isHide())
				{
					if (hasPermission(WellKnownPermission.SCHIFFSTYP_VERSTECKTE_SICHTBAR))
					{
						data.append("<a class=\"forschinfo\" onclick='ShiptypeBox.show(").append(effect.getShipType()).append(");return false;' href=\"").append(Common.buildUrl("default", "module", "schiffinfo", "ship", effect.getShipType())).append("\">").append(shiptype.getNickname()).append("</a><br /><span style=\"font-style:italic;color:red\" class=\"verysmallfont\">[unsichtbar]</span>\n");
					}
					else
					{
						data.append("Unbekannt");
					}
				}
				else
				{
					data.append("<a class=\"forschinfo\" onclick='ShiptypeBox.show(").append(effect.getShipType()).append(");return false;' href=\"").append(Common.buildUrl("default", "module", "schiffinfo", "ship", effect.getShipType())).append("\">").append(shiptype.getNickname()).append("</a>\n");
				}

				t.setVar("entry.name", "Schiffstyp",
						"entry.data", data);

				t.parse("itemdetails.entrylist", "itemdetails.entry", true);

				t.setVar("entry.name", "Rasse",
						"entry.data", Rassen.get().rasse(effect.getRace()).getName());

				t.parse("itemdetails.entrylist", "itemdetails.entry", true);

				data.setLength(0);
				boolean entry = false;
				for( Forschung forschung : effect.getBenoetigteForschungen() )
				{
					if (!forschung.isVisibile(user) && !user.hasResearched(forschung.getBenoetigteForschungen()))
					{
						data.append("Unbekannt");
						if (hasPermission(WellKnownPermission.FORSCHUNG_ALLES_SICHTBAR))
						{
							data.append(" [ID:").append(forschung.getID()).append("]");
						}
						data.append("<br />\n");
						entry = true;

						continue;
					}

					data.append("<a class=\"nonbold\" href=\"").append(Common.buildUrl("default", "module", "forschinfo", "res", forschung.getID())).append("\">");

					if (user.hasResearched(forschung))
					{
						data.append("<span style=\"color:green; font-size:14px\">");
					}
					else
					{
						data.append("<span style=\"color:red; font-size:14px\">");
					}
					data.append(forschung.getName());
					data.append("</span></a><br />\n");
					entry = true;
				}
				if (!entry)
				{
					data.append("-");
				}

				t.setVar("entry.name", "Ben&ouml;tigte Technologie",
						"entry.data", data);

				t.parse("itemdetails.entrylist", "itemdetails.entry", true);

				data.setLength(0);
				Cargo costs = effect.getBuildCosts();
				ResourceList reslist = costs.getResourceList();
				for (ResourceEntry res : reslist)
				{
					data.append("<img src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append("<br />\n");
				}
				data.append("<img src=\"./data/interface/energie.gif\" alt=\"\" />").append(effect.getE()).append("<br />\n");
				data.append("<img src=\"./data/interface/besatzung.gif\" alt=\"\" />").append(effect.getCrew()).append("<br />\n");

				t.setVar("entry.name", "Kosten",
						"entry.data", data);

				t.parse("itemdetails.entrylist", "itemdetails.entry", true);

				t.setVar("entry.name", "Dauer",
						"entry.data", "<img valign=\"middle\" src=\"./data/interface/time.gif\" alt=\"\" />" + effect.getDauer());

				t.parse("itemdetails.entrylist", "itemdetails.entry", true);

				t.setVar("entry.name", "Werftslots",
						"entry.data", "<img valign=\"middle\" src=\"./data/interface/schiffinfo/werftslots.png\" alt=\"\" />" + effect.getWerftSlots());

				t.parse("itemdetails.entrylist", "itemdetails.entry", true);

				if (effect.isFlagschiff())
				{
					t.setVar("entry.name", "Flagschiff",
							"entry.data", "ja");
					t.parse("itemdetails.entrylist", "itemdetails.entry", true);
				}

				break;
			}
		/*

			EFFECT_DISABLE_SHIP

		*/
			case DISABLE_SHIP:
			{
				IEDisableShip effect = (IEDisableShip) item.getEffect();
				t.setVar("entry.name", "Schiffstyp");

				ShipTypeData shiptype = effect.getShipType();
				if (shiptype != null)
				{
					t.setVar("entry.data", "<a class=\"forschinfo\" onclick='ShiptypeBox.show(" + effect.getShipType().getId() + ");return false;' href=\"" + Common.buildUrl("default", "module", "schiffinfo", "ship", effect.getShipType().getId()) + "\">" + shiptype.getNickname() + "</a>");
				}
				else
				{
					t.setVar("entry.data", "<span style=\"color:red\">Unbekannter Schiffstyp</span>");
				}

				t.parse("itemdetails.entrylist", "itemdetails.entry", true);

				break;
			}
		/*

			EFFECT_MODULE

		*/
			case MODULE:
			{
				IEModule effect = (IEModule) item.getEffect();

				StringBuilder targetslots = new StringBuilder(50);
				for (String aslot : effect.getSlots())
				{
					if (targetslots.length() > 0)
					{
						targetslots.append(", ");
					}
					try
					{
						targetslots.append(ModuleSlots.get().slot(aslot).getName());
					}
					catch (NoSuchSlotException e)
					{
						targetslots.append("Ungueltiger Slot '").append(aslot).append("'");
					}
				}

				int setknowncount = 0;
				if (effect.getSet() != null)
				{
					int setcount = 0;

					List<Schiffsmodul> itemlist = Common.cast(db.createQuery("from Schiffsmodul").list());

					for (Schiffsmodul aitem : itemlist)
					{
						if (aitem.getEffect().getSet() != effect.getSet())
						{
							continue;
						}
						setcount++;
						if (user.canSeeItem(aitem))
						{
							setknowncount++;
						}
					}

					SchiffsmodulSet setItem = effect.getSet();
					t.setVar("entry.name", "Set",
							"entry.data", setItem.getEffect().getName() + " (" + setcount + ")");

					t.parse("itemdetails.entrylist", "itemdetails.entry", true);
				}

				t.setVar("entry.name", "Passt in",
						"entry.data", targetslots.toString());

				t.parse("itemdetails.entrylist", "itemdetails.entry", true);

				String effecttext = this.parseModuleModifiers(effect.getMods());

				t.setVar("entry.name", "Effekt",
						"entry.data", effecttext);

				t.parse("itemdetails.entrylist", "itemdetails.entry", true);

				if (effect.getSet() != null)
				{
					SchiffsmodulSet setItem = effect.getSet();
					IEModuleSetMeta meta = setItem.getEffect();
					Map<Integer, SchiffstypModifikation> modlist = meta.getCombos();

					for (Map.Entry<Integer, SchiffstypModifikation> entry : modlist.entrySet())
					{
						Integer modulecount = entry.getKey();
						SchiffstypModifikation mods = entry.getValue();

						if (modulecount <= setknowncount)
						{
							effecttext = this.parseModuleModifiers(mods);

							t.setVar("entry.name", "Set-Combo (" + modulecount + " Items)",
									"entry.data", effecttext);
							t.parse("itemdetails.entrylist", "itemdetails.entry", true);
						}
					}
				}

				break;
			}
		/*

			EFFECT_AMMO

		*/
			case AMMO:
			{
				IEAmmo effect = (IEAmmo) item.getEffect();

				Munitionsdefinition ammo = effect.getAmmo();

				if (ammo == null)
				{
					t.setVar("entry.name", "Munition",
							"entry.data", "Es liegen keine genaueren Daten zur Munition vor");

					t.parse("itemdetails.entrylist", "itemdetails.entry", true);
				}
				else
				{
					StringBuilder data = new StringBuilder(100);

					t.parse("itemdetails.entrylist", "itemdetails.entry", true);

					data.setLength(0);
					if (ammo.getShotsPerShot() > 1)
					{
						data.append(ammo.getShotsPerShot()).append(" Salven<br />\n");
					}
					if (ammo.getDamage() != 0)
					{
						data.append(ammo.getDamage()).append(" Schaden<br />\n");
					}
					if (ammo.getDamage() != ammo.getShieldDamage())
					{
						data.append(ammo.getShieldDamage()).append(" Schildschaden<br />\n");
					}
					if (ammo.getSubDamage() != 0)
					{
						data.append(ammo.getSubDamage()).append(" Subsystemschaden<br />\n");
						data.append(ammo.getSubWS()).append("% Subsystem-Trefferwahrscheinlichkeit<br />\n");
					}
					data.append(ammo.getSmallTrefferWS()).append("% Trefferwahrscheinlichkeit gegen J&auml;ger<br />\n");
					data.append(ammo.getTrefferWS()).append("% Trefferwahrscheinlichkeit gegen Capitals\n");
					if (ammo.getTorpTrefferWS() != 0)
					{
						data.append("<br />").append(ammo.getTorpTrefferWS()).append("% Trefferwahrscheinlichkeit gegen Torpedos\n");
					}
					if (ammo.getAreaDamage() != 0)
					{
						data.append("<br />Umgebungsschaden (").append(ammo.getAreaDamage()).append(")\n");
					}
					if (ammo.getDestroyable() > 0)
					{
						data.append("<br />Durch Abwehrfeuer zerst&ouml;rbar\n");
					}

					t.setVar("entry.name", "Daten",
							"entry.data", data);

					t.parse("itemdetails.entrylist", "itemdetails.entry", true);

					StringBuilder weapons = new StringBuilder(50);
					for (Weapon weapon : Weapons.get())
					{
						if (!weapon.getMunitionstypen().contains(ammo.getType()))
						{
							continue;
						}

						if (weapons.length() == 0)
						{
							weapons.append(weapon.getName());
						}
						else
						{
							weapons.append(",<br />\n").append(weapon.getName());
						}
					}
					if (weapons.length() > 0)
					{
						t.setVar("entry.name", "Passende Waffen",
								"entry.data", weapons);
						t.parse("itemdetails.entrylist", "itemdetails.entry", true);
					}
				}

				break;
			}
		/*
		 *
		 *  EFFECT_MODULE_SET_META
		 *
		 */
			case MODULE_SET_META:
			{
				if (hasPermission(WellKnownPermission.ITEM_MODULESETMETA_SICHTBAR))
				{
					return t;
				}
				Cargo setitemlist = new Cargo();

				List<Schiffsmodul> itemlist = Common.cast(db.createQuery("from Schiffsmodul ").list());

				for (Schiffsmodul thisitem : itemlist)
				{
					if (thisitem.getEffect().getSet() == item)
					{
						setitemlist.addResource(new ItemID(thisitem.getID()), 1);
					}
				}

				StringBuilder tmp = new StringBuilder(50);
				ResourceList reslist = setitemlist.getResourceList();
				for (ResourceEntry res : reslist)
				{
					tmp.append("<span class=\"nobr\"><img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\">").append(res.getName()).append("</span><br />\n");
				}

				if (tmp.length() > 0)
				{
					t.setVar("entry.name", "Set-Items",
							"entry.data", tmp);

					t.parse("itemdetails.entrylist", "itemdetails.entry", true);
				}

				break;
			}
		} // Ende switch

		return t;
	}

	/**
	 * Zeigt die Liste aller bekannten Items sowie ihren Aufenthaltsort, sofern man sie besitzt, an.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine knownAction()
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();
		org.hibernate.Session db = getDB();
		List<Item> itemlist = Common.cast(db.createQuery("from Item").list());

		StatUserCargo ownCargoRow = (StatUserCargo) db.createQuery("from StatUserCargo where user=:user")
				.setEntity("user", user)
				.uniqueResult();
		Cargo owncargo;
		if (ownCargoRow != null)
		{
			owncargo = new Cargo(ownCargoRow.getCargo());
		}
		else
		{
			owncargo = new Cargo();
		}

		t.setVar("iteminfo.knownlist", 1);

		t.setBlock("_ITEMINFO", "knownlist.listitem", "knownlist.list");

		Map<Integer, String[]> reslocations = new HashMap<>();
		List<?> modules = db.createQuery("from StatItemLocations where user=:user")
						  .setEntity("user", user)
						  .list();
		for (Object module : modules)
		{
			StatItemLocations amodule = (StatItemLocations) module;
			reslocations.put(amodule.getItemId(), StringUtils.split(amodule.getLocations(), ';'));
		}

		final String shipimage = "<td class='noBorderX' style='text-align:right'><img style='vertical-align:middle' src='./data/interface/schiffe/" + user.getRace() + "/icon_schiff.gif' alt='' title='Schiff' /></td>";
		final String baseimage = "<td class='noBorderX' style='text-align:right'><img style='vertical-align:middle;width:15px;height:15px' src='./data/starmap/asti/asti.png' alt='' title='Asteroid' /></td>";

		for (Item aitem : itemlist)
		{
			int itemid = aitem.getID();

			ItemEffect itemeffect = aitem.getEffect();

			if (!user.canSeeItem(aitem))
			{
				continue;
			}

			String name = Common._plaintitle(aitem.getName());
			if (aitem.getQuality().color().length() > 0)
			{
				name = "<span style=\"color:" + aitem.getQuality().color() + "\">" + name + "</span>";
			}
			String tooltip = "";
			if (reslocations.containsKey(itemid))
			{
				StringBuilder tooltiptext = new StringBuilder(200);
				tooltiptext.append("<table class='noBorderX'>");
				String[] locations = reslocations.get(itemid);
				for (String alocation : locations)
				{
					int objectid = Integer.parseInt(alocation.substring(1));

					tooltiptext.append("<tr>");
					switch (alocation.charAt(0))
					{
						case 's':
						{
							Ship ship = (Ship) db.get(Ship.class, objectid);
							if (ship == null)
							{
								continue;
							}
							tooltiptext.append(shipimage).append("<td class='noBorderX'><a style='font-size:14px' class='forschinfo' href='").append(Common.buildUrl("default", "module", "schiff", "ship", objectid)).append("'>").append(ship.getName()).append(" (").append(ship.getId()).append(")</a></td>");
							break;
						}
						case 'b':
						{
							Base base = (Base) db.get(Base.class, objectid);
							if (base == null)
							{
								continue;
							}
							tooltiptext.append(baseimage + "<td class='noBorderX'><a style='font-size:14px' class='forschinfo' href='").append(Common.buildUrl("default", "module", "base", "col", objectid)).append("'>").append(base.getName()).append(" - ").append(base.getLocation().displayCoordinates(false)).append("</a></td>");
							break;
						}
						case 'g':
						{
							Ship ship = (Ship) db.get(Ship.class, objectid);
							if (ship == null)
							{
								continue;
							}
							tooltiptext.append("<td colspan='2' class='noBorderX' style='font-size:14px'>").append(ship.getName()).append("</td>");
							break;
						}
						default:
							tooltiptext.append("<td colspan='2' class='noBorderX' style='font-size:14px'>Unbekanntes Objekt ").append(alocation).append("</td>");
					}

					tooltiptext.append("</tr>");
				}
				tooltiptext.append("</table>");
				tooltip = tooltiptext.toString();
			}

			t.setVar("item.picture", aitem.getPicture(),
					"item.id", itemid,
					"item.name", name,
					"item.class", itemeffect.getType().getName(),
					"item.cargo", Common.ln(aitem.getCargo()),
					"item.locationtext", tooltip,
					"item.count", Common.ln(owncargo.getResourceCount(new ItemID(itemid))));

			t.parse("knownlist.list", "knownlist.listitem", true);
		}

		return t;
	}

	@ViewModel
	public static class AjaxViewModel
	{
		public List<ItemViewModel> items = new ArrayList<>();
	}

	/**
	 * Gibt alle fuer den Nutzer sichtbaren Items als JSON-Objekte zurueck.
	 */
	@Action(ActionType.AJAX)
	public AjaxViewModel ajaxAction()
	{
		User user = (User) getUser();
		org.hibernate.Session db = getDB();
		List<Item> itemlist = Common.cast(db.createQuery("from Item").list());

		AjaxViewModel result = new AjaxViewModel();

		for (Item aitem : itemlist)
		{
			if (!user.canSeeItem(aitem))
			{
				continue;
			}

			result.items.add(ItemViewModel.map(aitem));
		}

		return result;
	}

	/**
	 * Zeigt eine Itemliste an.
	 *
	 * @param itemlistStr Die Itemliste
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult defaultAction(@UrlParam(name = "itemlist") String itemlistStr)
	{
		return new RedirectViewResult("details");
	}
}
