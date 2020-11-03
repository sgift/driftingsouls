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
package net.driftingsouls.ds2.server.modules.stats;

import com.google.gson.Gson;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceIDComparator;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.statistik.StatCargo;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.stream.Collectors.*;

/**
 * Zeigt allgemeine Daten zu DS und zum Server an.
 *
 * @author Christopher Jung
 */
@Component
public class StatWarenentwicklung implements Statistic, AjaxStatistic
{
	@PersistenceContext
	private EntityManager em;

	public static class WareViewModel
	{
		private final String label;
		private final int key;
		private final String picture;

		private WareViewModel(int key, String label, String picture)
		{
			this.key = key;
			this.label = label;
			this.picture = picture;
		}

		public int getKey()
		{
			return key;
		}

		public String getLabel()
		{
			return label;
		}

		public String getPicture()
		{
			return picture;
		}
	}

	@Override
	public void show(StatsController contr, int size) throws IOException
	{
		Context context = ContextMap.getContext();
		User user = (User) contr.getUser();

		Writer echo = context.getResponse().getWriter();
		echo.write("<h1>Warenentwicklung</h1>");
		SortedMap<ResourceID, SortedMap<Integer, Long>> cargos = generateDataMap();
		echo.append("<div id='warenstats'></div><script type='text/javascript'>$(document).ready(function(){\n");
		echo.append("Stats.chart('warenstats',");
		List<WareViewModel> waren = erstelleViewModelDerWarenListe(user, cargos);
		echo.append(new Gson().toJson(waren));
		echo.append(",{xaxis:{label:'Tick',pad:0,tickInterval:49}, yaxis:{label:'Menge'}, selection:'single'});\n");
		echo.append("});</script>");
	}

	private List<WareViewModel> erstelleViewModelDerWarenListe(User user, SortedMap<ResourceID, SortedMap<Integer, Long>> cargos)
	{
		return cargos.entrySet().stream()
			.filter(entry -> isImportantResource(entry.getValue()))
			.map(entry -> em.find(Item.class, entry.getKey().getItemID()))
			.filter(Objects::nonNull)
			.filter(user::canSeeItem)
			.map(item -> new WareViewModel(item.getID(), item.getName(), item.getPicture()))
			.collect(toList());
	}

	private boolean isImportantResource(SortedMap<Integer, Long> values)
	{
		long minValue = values.size() * 5L;
		long count = 0;
		for (Long value : values.values())
		{
			count += value;
			if (count >= minValue)
			{
				return true;
			}
		}
		return false;
	}

	private SortedMap<ResourceID, SortedMap<Integer, Long>> generateDataMap()
	{
		SortedMap<ResourceID, SortedMap<Integer, Long>> cargos =
				new TreeMap<>(new ResourceIDComparator(false));

		int counter = 0;
		List<StatCargo> stats = em.createQuery("from StatCargo order by tick desc", StatCargo.class)
				.setMaxResults(30 + 31) // ca 2 Monate
				.getResultList();
		for (StatCargo sc : stats)
		{
			if (counter++ % 2 == 1)
			{
				// Nur jede zweite Zeile verarbeiten
				continue;
			}
			for (ResourceEntry entry : sc.getCargo().getResourceList())
			{
				if (!cargos.containsKey(entry.getId()))
				{
					cargos.put(entry.getId(), new TreeMap<>());
				}
				cargos.get(entry.getId()).put(sc.getTick(), entry.getCount1());
			}
		}
		return cargos;
	}

	@Override
	public DataViewModel generateData(StatsController contr, int size)
	{
		User user = (User) contr.getUser();

		ItemID itemId = new ItemID(ContextMap.getContext().getRequest().getParameterInt("key"));

		Item item = em.find(Item.class, itemId.getItemID());
		if (item == null || !user.canSeeItem(item))
		{
			return new DataViewModel();
		}

		SortedMap<ResourceID, SortedMap<Integer, Long>> data = generateDataMap();
		DataViewModel result = new DataViewModel();

		result.key = new DataViewModel.KeyViewModel();
		result.key.id = item.getID();
		result.key.name = item.getName();

		SortedMap<Integer, Long> itemData = data.get(itemId);
		if (itemData != null)
		{
			for (Map.Entry<Integer, Long> entry : itemData.entrySet())
			{
				DataViewModel.DataEntryViewModel entryObj = new DataViewModel.DataEntryViewModel();
				entryObj.index = entry.getKey();
				entryObj.count = entry.getValue();
				result.data.add(entryObj);
			}
		}

		return result;
	}
}
