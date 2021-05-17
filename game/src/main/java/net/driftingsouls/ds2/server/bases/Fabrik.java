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

	public static Log getLog() {
		return log;
	}

	/**
	 * Daten von einer oder mehreren Fabriken.
	 */
    @ContextInstance(ContextInstance.Scope.REQUEST)
	public static class ContextVars
	{
        final List<Integer> buildingidlist = new ArrayList<>();
		final Set<FactoryEntry> owneritemsbase = new HashSet<>();
		final Map<Integer, Cargo> stats = new HashMap<>();
		final Map<Integer, Cargo> productionstats = new HashMap<>();
		final Map<Integer, Cargo> consumptionstats = new HashMap<>();
		final Map<Integer, BigDecimal> usedcapacity = new HashMap<>();
        final List<Integer> modified = new ArrayList<>();
        final List<Integer> prodmodified = new ArrayList<>();
        final List<Integer> conmodified = new ArrayList<>();
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
				public List<Integer> getBuildingidlist() {
					return buildingidlist;
				}

				public Set<FactoryEntry> getOwneritemsbase() {
					return owneritemsbase;
				}

				public Map<Integer, Cargo> getStats() {
					return stats;
				}

				public Map<Integer, Cargo> getProductionstats() {
					return productionstats;
				}

				public Map<Integer, Cargo> getConsumptionstats() {
					return consumptionstats;
				}

				public Map<Integer, BigDecimal> getUsedcapacity() {
					return usedcapacity;
				}

				public List<Integer> getModified() {
					return modified;
				}

				public List<Integer> getProdmodified() {
					return prodmodified;
				}

				public List<Integer> getConmodified() {
					return conmodified;
				}

				public boolean isInit() {
					return init;
				}

				public void setInit(boolean init) {
					this.init = init;
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

			log.warn("Basis " + base.getId() + " verfügt über keinen Fabrik-Eintrag, obwohl es eine Fabrik hat.");
			return "Basis " + base.getId() + " verfügt über keinen Fabrik-Eintrag, obwohl es eine Fabrik hat.";
		}

		Factory.Task[] plist = wf.getProduces();
		for (int i = 0; i < plist.length; i++)
		{
			int id = plist[i].getId();
			int count = plist[i].getCount();

			FactoryEntry entry = em.find(FactoryEntry.class, id);
			if (entry == null)
			{
				plist = ArrayUtils.remove(plist, i);
				i--;
				continue;
			}

			// Items ohne Plaene melden
			if ((count > 0) && !thisitemslist.contains(entry))
			{
				ok = false;
				wfreason.append("Es existieren nicht die nötigen Baupläne für ").append(entry.getName()).append("\n");
				break;
			}
		}

		if (ok)
		{
			for (Factory.Task aPlist : plist)
			{
				int id = aPlist.getId();
				int count = aPlist.getCount();

				FactoryEntry entry = em.find(FactoryEntry.class, id);

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
			wfreason.insert(0, "[b]" + basename + "[/b] - Die Arbeiten in der Fabrik sind zeitweise eingestellt.\nGrund:\n");
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

		List<FactoryEntry> entrylist = Common.cast(em.createQuery("from FactoryEntry").list());
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
	public void build(Base base, Building building)
	{
		throw new IllegalArgumentException("Shouldn't be called!");
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
		throw new IllegalArgumentException("Shouldn't be called!");
	}

	@Override
	public String echoShortcut(Context context, Base base, int field, int building)
	{

		StringBuilder result = new StringBuilder(200);

		loaddata(base, building);
		ContextVars vars = ContextMap.getContext().get(ContextVars.class);

		if (vars.usedcapacity.get(building).doubleValue() > 0)
		{
			Factory wf = Objects.requireNonNull(loadFactoryEntity(base, building));
			Factory.Task[] prodlist = wf.getProduces();

			StringBuilder popup = new StringBuilder(200);

			popup.append(this.getName()).append("<br /><br />");

			for (Factory.Task aProdlist : prodlist)
			{
				int id = aProdlist.getId();
				int count = aProdlist.getCount();
				FactoryEntry entry = em.find(FactoryEntry.class, id);

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
		throw new IllegalArgumentException("should not be called!");
	}

	private int findExistingItemTask(FactoryEntry entry, List<Factory.Task> producelist) {
		int entryId = -1;
		for (int i = 0; i < producelist.size(); i++)
		{
			int aId = producelist.get(i).getId();
			if(aId == entry.getId()) {
				entryId = i;
				break;
			}
		}
		return entryId;
	}

	private int computeNewBuildCount(int count, List<Factory.Task> producelist, int entryId) {
		int currentCount;
		if(entryId == -1) {
			currentCount = 0;
		} else {
			currentCount = producelist.get(entryId).getCount();
		}
		return currentCount + count;
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
