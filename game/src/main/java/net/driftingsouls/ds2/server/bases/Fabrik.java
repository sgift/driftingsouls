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
package net.driftingsouls.ds2.server.bases;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.items.Munitionsbauplan;
import net.driftingsouls.ds2.server.entities.Factory;
import net.driftingsouls.ds2.server.entities.FactoryEntry;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextInstance;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

/**
 * Die Fabrik.
 */
@Entity(name = "FabrikBuilding")
@DiscriminatorValue("net.driftingsouls.ds2.server.bases.Fabrik")
public class Fabrik extends DefaultBuilding
{
	private static final Log log = LogFactory.getLog(Fabrik.class);

	/**
	 * Daten von einer oder mehreren Fabriken.
	 */
    @ContextInstance(ContextInstance.Scope.REQUEST)
	public static class ContextVars
	{
        List<Integer> buildingidlist = new ArrayList<>();
		Set<FactoryEntry> owneritemsbase = new HashSet<>();
		Map<Integer, Cargo> stats = new HashMap<>();
		Map<Integer, Cargo> productionstats = new HashMap<>();
		Map<Integer, Cargo> consumptionstats = new HashMap<>();
		Map<Integer, BigDecimal> usedcapacity = new HashMap<>();
        List<Integer> modified = new ArrayList<>();
        List<Integer> prodmodified = new ArrayList<>();
        List<Integer> conmodified = new ArrayList<>();
        boolean init = false;

		/**
		 * Konstruktor.
		 */
		public ContextVars()
		{
			// EMPTY
		}

        public void clear()
        {
            buildingidlist.clear();
            consumptionstats.clear();
            productionstats.clear();
            stats.clear();
            usedcapacity.clear();
            owneritemsbase.clear();
            modified.clear();
            prodmodified.clear();
            conmodified.clear();
            init = false;
        }
	}

	/**
	 * Erstellt eine neue Instanz der Fabriken.
	 */
	public Fabrik()
	{
		// EMPTY
	}

	private String loaddata(Base base, int buildingid)
	{
		Context context = ContextMap.getContext();

		User user = base.getOwner();

		ContextVars vars = context.get(ContextVars.class);

		if (!vars.init)
		{
			loadOwnerBase(user, vars);
            vars.init = true;
		}

        if(!vars.buildingidlist.contains(buildingid))
        {
            vars.buildingidlist.add(buildingid);
            return loadAmmoTasks(base, vars, buildingid);
        }
        return "";
	}

	private String loadAmmoTasks(Base base, ContextVars vars, int buildingid)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		StringBuilder wfreason = new StringBuilder(100);

		if (!vars.stats.containsKey(buildingid))
		{
			vars.stats.put(buildingid, new Cargo());
		}
		if (!vars.productionstats.containsKey(buildingid))
		{
			vars.productionstats.put(buildingid, new Cargo());
		}
		if (!vars.consumptionstats.containsKey(buildingid))
		{
			vars.consumptionstats.put(buildingid, new Cargo());
		}

		boolean ok = true;
		Set<FactoryEntry> thisitemslist = vars.owneritemsbase;

		Cargo cargo = base.getCargo();

		List<ItemCargoEntry<Munitionsbauplan>> list = cargo.getItemsOfType(Munitionsbauplan.class);
		for (ItemCargoEntry<Munitionsbauplan> item : list)
		{
			FactoryEntry entry = item.getItem().getFabrikeintrag();

			thisitemslist.add(entry);
		}

		Factory wf = loadFactoryEntity(base, buildingid);

		if (wf == null)
		{
			vars.usedcapacity.put(buildingid, BigDecimal.valueOf(-1));

			log.warn("Basis " + base.getId() + " verfuegt ueber keinen Fabrik-Eintrag, obwohl es eine Fabrik hat");
			return "Basis " + base.getId() + " verfuegt ueber keinen Fabrik-Eintrag, obwohl es eine Fabrik hat";
		}

		Factory.Task[] plist = wf.getProduces();
		for (int i = 0; i < plist.length; i++)
		{
			int id = plist[i].getId();
			int count = plist[i].getCount();

			FactoryEntry entry = (FactoryEntry) db.get(FactoryEntry.class, id);
			if (entry == null)
			{
				plist = (Factory.Task[]) ArrayUtils.remove(plist, i);
				i--;
				continue;
			}

			// Items ohne Plaene melden
			if ((count > 0) && !thisitemslist.contains(entry))
			{
				ok = false;
				wfreason.append("Es existieren nicht die n&ouml;tigen Baupl&auml;ne f&uuml;r ").append(entry.getName()).append("\n");
				break;
			}
		}

		if (ok)
		{
			for (Factory.Task aPlist : plist)
			{
				int id = aPlist.getId();
				int count = aPlist.getCount();

				FactoryEntry entry = (FactoryEntry) db.get(FactoryEntry.class, id);

				if (!vars.usedcapacity.containsKey(buildingid))
				{
					vars.usedcapacity.put(buildingid, new BigDecimal(0, MathContext.DECIMAL32));
				}
				vars.usedcapacity.put(buildingid, vars.usedcapacity.get(buildingid).add(entry.getDauer().multiply((new BigDecimal(count)))));
				if (count > 0)
				{
					Cargo tmpcargo = new Cargo(entry.getBuildCosts());
					if (count > 1)
					{
						tmpcargo.multiply(count, Cargo.Round.NONE);
					}
					vars.consumptionstats.get(buildingid).addCargo(tmpcargo);
					vars.stats.get(buildingid).substractCargo(tmpcargo);
					Cargo addCargo = entry.getProduce();
					addCargo.multiply(count, Cargo.Round.FLOOR);
					vars.stats.get(buildingid).addCargo(addCargo);
					vars.productionstats.get(buildingid).addCargo(addCargo);
				}
			}
		}
		else
		{
			String basename = base.getName();
			wfreason.insert(0, "[b]" + basename + "[/b] - Die Arbeiten in der Fabrik zeitweise eingestellt.\nGrund:\n");
		}

		if (!vars.usedcapacity.containsKey(buildingid) || (vars.usedcapacity.get(buildingid).doubleValue() <= 0))
		{
			vars.usedcapacity.put(buildingid, new BigDecimal(-1));
		}

		return wfreason.toString();
	}

	private Factory loadFactoryEntity(Base base, int buildingid)
	{
		for (Factory factory : base.getFactories())
		{
			if (factory.getBuildingID() == buildingid)
			{
				return factory;
			}
		}
		return null;
	}

	private void loadOwnerBase(User user, ContextVars vars)
	{

		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		List<FactoryEntry> entrylist = Common.cast(db.createQuery("from FactoryEntry").list());
		for (FactoryEntry entry : entrylist)
		{
			if (!user.hasResearched(entry.getBenoetigteForschungen()))
			{
				continue;
			}
			vars.owneritemsbase.add(entry);
		}

		if (user.getAlly() != null)
		{
			Cargo itemlist = new Cargo(Cargo.Type.ITEMSTRING, user.getAlly().getItems());

			List<ItemCargoEntry<Munitionsbauplan>> list = itemlist.getItemsOfType(Munitionsbauplan.class);
			for (ItemCargoEntry<Munitionsbauplan> item : list)
			{
				FactoryEntry entry = item.getItem().getFabrikeintrag();

				vars.owneritemsbase.add(entry);
			}
		}
	}

	@Override
	public void build(Base base, int buildingid)
	{
		super.build(base, buildingid);

		org.hibernate.Session db = ContextMap.getContext().getDB();
		Factory wf = loadFactoryEntity(base, buildingid);

		if (wf != null)
		{
			wf.setCount(wf.getCount() + 1);
		}
		else
		{
			wf = new Factory(base, buildingid);
			db.persist(wf);
		}
	}

	@Override
	public boolean classicDesign()
	{
		return true;
	}

	@Override
	public boolean printHeader()
	{
		return true;
	}

	@Override
	public void cleanup(Context context, Base base, int buildingid)
	{
		org.hibernate.Session db = context.getDB();

		Factory wf = loadFactoryEntity(base, buildingid);
		if (wf == null)
		{
			return;
		}

		if (wf.getCount() > 1)
		{
			BigDecimal usedcapacity = new BigDecimal(0, MathContext.DECIMAL32);

			Factory.Task[] plist = wf.getProduces();
			for (Factory.Task aPlist : plist)
			{
				int id = aPlist.getId();
				int count = aPlist.getCount();

				FactoryEntry entry = (FactoryEntry) db.get(FactoryEntry.class, id);

				usedcapacity = usedcapacity.add(entry.getDauer().multiply(new BigDecimal(count)));
			}

			if (usedcapacity.compareTo(new BigDecimal(wf.getCount() - 1)) > 0)
			{
				BigDecimal targetCapacity = new BigDecimal(wf.getCount() - 1);

				for (int i = 0; i < plist.length; i++)
				{
					int id = plist[i].getId();
					int count = plist[i].getCount();

					FactoryEntry entry = (FactoryEntry) db.get(FactoryEntry.class, id);

					BigDecimal capUsed = new BigDecimal(count).multiply(entry.getDauer());

					if (usedcapacity.subtract(capUsed).compareTo(targetCapacity) < 0)
					{
						BigDecimal capLeft = capUsed.subtract(usedcapacity.subtract(targetCapacity));
						plist[i] = new Factory.Task(id, capLeft.divide(entry.getDauer(), RoundingMode.DOWN).intValue());
						break;
					}
					plist = (Factory.Task[]) ArrayUtils.remove(plist, i);
					i--;

					usedcapacity = usedcapacity.subtract(capUsed);

					if (usedcapacity.compareTo(targetCapacity) <= 0)
					{
						break;
					}
				}
				wf.setProduces(plist);
			}

			wf.setCount(wf.getCount() - 1);
		}
		else
		{
			db.delete(wf);
			base.getFactories().remove(wf);
		}
	}

	@Override
	public String echoShortcut(Context context, Base base, int field, int building)
	{
		org.hibernate.Session db = context.getDB();

		StringBuilder result = new StringBuilder(200);

		loaddata(base, building);
		ContextVars vars = ContextMap.getContext().get(ContextVars.class);

		if (vars.usedcapacity.get(building).doubleValue() > 0)
		{
			Factory wf = loadFactoryEntity(base, building);
			Factory.Task[] prodlist = wf.getProduces();

			StringBuilder popup = new StringBuilder(200);

			Building buildingobj = (Building) db.get(Building.class, building);
			popup.append(buildingobj.getName()).append("<br /><br />");

			for (Factory.Task aProdlist : prodlist)
			{
				int id = aProdlist.getId();
				int count = aProdlist.getCount();
				FactoryEntry entry = (FactoryEntry) db.get(FactoryEntry.class, id);

				if ((count > 0) && vars.owneritemsbase.contains(entry))
				{
					Cargo production = new Cargo(entry.getProduce());
					production.setOption(Cargo.Option.SHOWMASS, false);
					ResourceList reslist = production.getResourceList();
					popup.append("[").append(count).append("x]  ");
					for (ResourceEntry res : reslist)
					{
						popup.append("<img style='vertical-align:middle' src='").append(res.getImage()).append("' alt='' />").append(res.getCount1());
					}
					popup.append("<br />");
				}
			}


			result.append("<a class=\"error tooltip\" " +
						  "href=\"./ds?module=building&amp;col=").append(base.getId()).append("&amp;field=").append(field).append("\">[FA]<span class='ttcontent'>").append(popup).append("</span></a>");
		}
		else
		{
			result.append("<a class=\"back tooltip\" href=\"./ds?module=building&amp;col=").append(base.getId()).append("&amp;field=").append(field).append("\">[FA]" +
						  "<span class='ttcontent'>").append(this.getName()).append("</span></a>");
		}

		return result.toString();
	}

	@Override
	public boolean isActive(Base base, int status, int field)
	{
		loaddata(base, base.getBebauung()[field]);
		ContextVars vars = ContextMap.getContext().get(ContextVars.class);
		return vars.usedcapacity.get(base.getBebauung()[field]).doubleValue() > 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String modifyStats(Base base, Cargo stats, int building)
	{
		String msg = loaddata(base, building);

		Context context = ContextMap.getContext();
		ContextVars vars = context.get(ContextVars.class);

		if ((vars.usedcapacity.get(building).compareTo(new BigDecimal(0)) > 0) && !vars.modified.contains(building))
		{
			stats.addCargo(vars.stats.get(building));
            vars.modified.add(building);
		}

		return msg;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String modifyProductionStats(Base base, Cargo stats, int building)
	{
		String msg = loaddata(base, building);

		Context context = ContextMap.getContext();
		ContextVars vars = context.get(ContextVars.class);

        if ((vars.usedcapacity.get(building).compareTo(new BigDecimal(0)) > 0) && !vars.prodmodified.contains(building))
        {
            stats.addCargo(vars.productionstats.get(building));
            vars.prodmodified.add(building);
        }

		return msg;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String modifyConsumptionStats(Base base, Cargo stats, int building)
	{
		String msg = loaddata(base, building);

		Context context = ContextMap.getContext();
		ContextVars vars = context.get(ContextVars.class);

        if ((vars.usedcapacity.get(building).compareTo(new BigDecimal(0)) > 0) && !vars.conmodified.contains(building))
        {
            stats.addCargo(vars.consumptionstats.get(building));
            vars.conmodified.add(building);
        }

		return msg;
	}

	@Override
	public String output(Context context, Base base, int field, int building)
	{
		org.hibernate.Session db = context.getDB();
		User user = (User) context.getActiveUser();

		int produce = context.getRequest().getParameterInt("produce");
		int count = context.getRequest().getParameterInt("count");

		StringBuilder echo = new StringBuilder(2000);

		Factory wf = loadFactoryEntity(base, building);

		if (wf == null)
		{
			echo.append("<div style=\"color:red\">FEHLER: Diese Fabrik besitzt keinen Eintrag<br /></div>\n");
			return echo.toString();
		}
		/*
			Liste der baubaren Items zusammenstellen
		*/

		Set<FactoryEntry> itemslist = new HashSet<>();

		Iterator<?> itemsIter = db.createQuery("from FactoryEntry").list().iterator();
		for (; itemsIter.hasNext(); )
		{
			FactoryEntry entry = (FactoryEntry) itemsIter.next();

			if (!user.hasResearched(entry.getBenoetigteForschungen()) || !entry.hasBuildingId(building))
			{
				continue;
			}

			itemslist.add(entry);
		}

		Cargo cargo = base.getCargo();

		// Lokale Ammobauplaene ermitteln
		List<ItemCargoEntry<Munitionsbauplan>> itemlist = cargo.getItemsOfType(Munitionsbauplan.class);
		for (ItemCargoEntry<Munitionsbauplan> item : itemlist)
		{
			Munitionsbauplan itemobject = item.getItem();
			final FactoryEntry entry = itemobject.getFabrikeintrag();

			if (!itemslist.contains(entry))
			{
				itemslist.add(entry);
			}
		}

		// Moegliche Allybauplaene ermitteln
		if (user.getAlly() != null)
		{
			Cargo allyitems = new Cargo(Cargo.Type.ITEMSTRING, user.getAlly().getItems());

			itemlist = allyitems.getItemsOfType(Munitionsbauplan.class);
			for (ItemCargoEntry<Munitionsbauplan> item : itemlist)
			{
				Munitionsbauplan itemobject = item.getItem();
				final FactoryEntry entry = itemobject.getFabrikeintrag();

				if (!itemslist.contains(entry))
				{
					itemslist.add(entry);
				}
			}
		}

		/*
			Neue Bauauftraege behandeln
		*/

		echo.append("<div class=\"smallfont\">");
		if ((produce != 0) && (count != 0))
		{
			final FactoryEntry entry = (FactoryEntry) db.get(FactoryEntry.class, produce);

			if (entry == null)
			{
				echo.append("<span style=\"color:red\">Fehler: Der angegebene Bauplan existiert nicht</span>\n");
				return echo.toString();
			}

			if (itemslist.contains(entry))
			{
				BigDecimal usedcapacity = new BigDecimal(0, MathContext.DECIMAL32);

				Factory.Task[] plist = wf.getProduces();
				for (Factory.Task aPlist : plist)
				{
					final int aId = aPlist.getId();
					final int aCount = aPlist.getCount();

					final FactoryEntry aEntry = (FactoryEntry) db.get(FactoryEntry.class, aId);
					usedcapacity = usedcapacity.add(aEntry.getDauer().multiply(new BigDecimal(aCount)));
				}
				if (usedcapacity.add(new BigDecimal(count).multiply(entry.getDauer())).doubleValue() > wf.getCount())
				{
					BigDecimal availableCap = usedcapacity.multiply(new BigDecimal(-1)).add(new BigDecimal(wf.getCount()));
					count = availableCap.divide(entry.getDauer(), RoundingMode.DOWN).intValue();
				}

				if (count != 0)
				{
					boolean gotit = false;
					List<Factory.Task> producelist = new ArrayList<>(
																	Arrays.asList(wf.getProduces())
					);

					for (int i = 0; i < producelist.size(); i++)
					{
						int aId = producelist.get(i).getId();
						int ammoCount = producelist.get(i).getCount();

						FactoryEntry aEntry = (FactoryEntry) db.get(FactoryEntry.class, aId);

						if ((aEntry == null) || (ammoCount <= 0))
						{
							producelist.remove(i);
							i--;
							continue;
						}

						if (aEntry == entry)
						{
							if ((count < 0) && (ammoCount + count < 0))
							{
								count = -ammoCount;
							}
							ammoCount += count;
							gotit = true;
						}
						if (ammoCount > 0)
						{
							producelist.set(i, new Factory.Task(aEntry.getId(), ammoCount));
						}
						else
						{
							producelist.remove(i);
							i--;
						}
					}
					if (!gotit && (count > 0))
					{
						producelist.add(new Factory.Task(entry.getId(), count));
					}

					wf.setProduces(producelist.toArray(new Factory.Task[producelist.size()]));

					echo.append(Math.abs(count)).append(" ").append(entry.getName()).append(" wurden ").append((count >= 0 ? "hinzugef&uuml;gt" : "abgezogen")).append("<br /><br />");
				}
			}
			else
			{
				echo.append("Sie haben nicht alle ben&ouml;tigten Forschungen f&uuml;r ").append(entry.getName()).append("<br /><br />");
			}
		}

		/*
			Aktuelle Bauauftraege ermitteln
		*/
		// Warum BigDecimal? Weil 0.05 eben nicht 0.05000000074505806 ist (Ungenauigkeit von double/float)....
		BigDecimal usedcapacity = new BigDecimal(0, MathContext.DECIMAL32);
		Map<FactoryEntry, Integer> productlist = new HashMap<>();
		Cargo consumes = new Cargo();
		Cargo produceCargo = new Cargo();
		consumes.setOption(Cargo.Option.SHOWMASS, false);
		produceCargo.setOption(Cargo.Option.SHOWMASS, false);

		if (wf.getProduces().length > 0)
		{
			Factory.Task[] plist = wf.getProduces();
			for (Factory.Task aPlist : plist)
			{
				final int id = aPlist.getId();
				final int ammoCount = aPlist.getCount();

				FactoryEntry entry = (FactoryEntry) db.get(FactoryEntry.class, id);

				if (!itemslist.contains(entry))
				{
					echo.append("WARNUNG: Ungueltiges Item >").append(entry.getId()).append("< (count: ").append(ammoCount).append(") in der Produktionsliste entdeckt<br />\n");
					continue;
				}

				usedcapacity = usedcapacity.add(entry.getDauer().multiply(new BigDecimal(ammoCount)));

				if (ammoCount > 0)
				{
					Cargo tmpcargo = new Cargo(entry.getBuildCosts());
					Cargo prodcargo = new Cargo(entry.getProduce());
					if (ammoCount > 1)
					{
						tmpcargo.multiply(ammoCount, Cargo.Round.NONE);
						prodcargo.multiply(ammoCount, Cargo.Round.NONE);
					}
					consumes.addCargo(tmpcargo);
					produceCargo.addCargo(prodcargo);
				}
				productlist.put(entry, ammoCount);
			}
		}
		echo.append("</div>\n");

		/*
			Ausgabe: Verbrauch, Auftraege, Liste baubarer Munitionstypen
		*/
		echo.append("<div class='gfxbox' style='width:1100px'>");

		echo.append("<img style=\"vertical-align:middle\" src=\"./data/interface/time.gif\" alt=\"Zeiteinheiten\" />").append(Common.ln(usedcapacity)).append("/").append(wf.getCount()).append(" ausgelastet<br />\n");
		echo.append("Verbrauch: ");
		ResourceList reslist = consumes.getResourceList();
		for (ResourceEntry res : reslist)
		{
			echo.append("<img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append("&nbsp;");
		}
		echo.append("<br/>");
		echo.append("Produktion: ");
		reslist = produceCargo.getResourceList();
		for (ResourceEntry res : reslist)
		{
			echo.append("<img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append("&nbsp;");
		}
		echo.append("<br /><br />\n");
		echo.append("<table class=\"noBorderX\" cellpadding=\"2\">");
		echo.append("<tr>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:20px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"font-weight:bold\">Kosten</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"font-weight:bold\">Produktion</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:130px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"width:30px\">&nbsp;</td>\n");
		echo.append("</tr>");

		List<FactoryEntry> entries = Common.cast(db.createQuery("from FactoryEntry").list());

		for (FactoryEntry entry : entries)
		{
			if (!itemslist.contains(entry))
			{
				continue;
			}


			echo.append("<tr>\n");
			if (productlist.containsKey(entry))
			{
				echo.append("<td class=\"noBorderX\" valign=\"top\">").append(productlist.get(entry)).append("x</td>\n");
			}
			else
			{
				echo.append("<td class=\"noBorderX\" valign=\"top\">-</td>\n");
			}

			echo.append("<td class=\"noBorderX\" valign=\"top\">\n");
			echo.append("<img style=\"vertical-align:middle\" src=\"./data/interface/time.gif\" alt=\"Dauer\" />").append(Common.ln(entry.getDauer())).append(" \n");

			Cargo buildcosts = new Cargo(entry.getBuildCosts());
			buildcosts.setOption(Cargo.Option.SHOWMASS, false);
			reslist = buildcosts.getResourceList();
			for (ResourceEntry res : reslist)
			{
				echo.append("<span class=\"nobr\"><img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append("</span>\n");
			}

			echo.append("</td>\n");
			echo.append("<td class=\"noBorderX\" valign=\"top\">\n");

			Cargo produceCosts = new Cargo(entry.getProduce());
			produceCosts.setOption(Cargo.Option.SHOWMASS, false);
			reslist = produceCosts.getResourceList();
			for (ResourceEntry res : reslist)
			{
				echo.append("<span class=\"nobr\"><img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append("</span>\n");
			}

			echo.append("</td>\n");
			echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; width:130px\">\n");
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<div>\n");
			echo.append("<input name=\"count\" type=\"text\" size=\"2\" value=\"0\" />\n");
			echo.append("<input name=\"produce\" type=\"hidden\" value=\"").append(entry.getId()).append("\" />\n");
			echo.append("<input name=\"col\" type=\"hidden\" value=\"").append(base.getId()).append("\" />\n");
			echo.append("<input name=\"field\" type=\"hidden\" value=\"").append(field).append("\" />\n");
			echo.append("<input name=\"module\" type=\"hidden\" value=\"building\" />\n");
			echo.append("<input type=\"submit\" value=\"herstellen\" />\n");
			echo.append("</div>\n");
			echo.append("</form></td>\n");

			fabrikEintragButton(echo, entry, base, field, productlist.containsKey(entry) ? -productlist.get(entry) : 0, "reset");
			fabrikEintragButton(echo, entry, base, field, 1, "+ 1");
			fabrikEintragButton(echo, entry, base, field, 5, "+ 5");
			fabrikEintragButton(echo, entry, base, field, -1, "- 1");
			fabrikEintragButton(echo, entry, base, field, -5, "- 5");

			echo.append("</tr>\n");
		}

		echo.append("</table><br />\n");
		echo.append("</div>");
		echo.append("<div><br /></div>\n");

		return echo.toString();
	}

	private void fabrikEintragButton(StringBuilder echo, FactoryEntry entry, Base base, int field, int count, String label)
	{
		echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; width:30px\">\n");
		echo.append("<form action=\"./ds\" method=\"post\">\n");
		echo.append("<div>\n");
		echo.append("<input name=\"count\" type=\"hidden\" size=\"2\" value=\"").append(count).append("\" />\n");
		echo.append("<input name=\"produce\" type=\"hidden\" value=\"").append(entry.getId()).append("\" />\n");
		echo.append("<input name=\"col\" type=\"hidden\" value=\"").append(base.getId()).append("\" />\n");
		echo.append("<input name=\"field\" type=\"hidden\" value=\"").append(field).append("\" />\n");
		echo.append("<input name=\"module\" type=\"hidden\" value=\"building\" />\n");
		echo.append("<input type=\"submit\" value=\"").append(label).append("\" />\n");
		echo.append("</div>\n");
		echo.append("</form></td>\n");
	}

	@Override
	public boolean isSupportsJson()
	{
		return false;
	}
}
