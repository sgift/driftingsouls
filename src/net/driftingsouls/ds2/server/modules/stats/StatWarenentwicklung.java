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

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceIDComparator;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.statistik.StatCargo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Zeigt allgemeine Daten zu DS und zum Server an.
 * @author Christopher Jung
 *
 */
public class StatWarenentwicklung implements Statistic,AjaxStatistic {

	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		User user = (User)contr.getUser();

		Writer echo = context.getResponse().getWriter();
		echo.write("<h1>Warenentwicklung</h1>");
		SortedMap<ResourceID, SortedMap<Integer, Long>> cargos = generateDataMap(db);
		echo.append("<div id='warenstats'></div><script type='text/javascript'>$(document).ready(function(){\n");
		echo.append("Stats.chart('warenstats',[");
		boolean first = true;
		for( Map.Entry<ResourceID,SortedMap<Integer,Long>> entry : cargos.entrySet() )
		{
			if( !isImportantResource(entry.getValue()) )
			{
				continue;
			}

			Item item = (Item)db.get(Item.class, entry.getKey().getItemID());
			if( item == null || !user.canSeeItem(item) )
			{
				continue;
			}
			if( !first )
			{
				echo.append(",\n");
			}
			first = false;
			echo.append("{label:'"+item.getName()+"',key:'"+item.getID()+"',picture:'"+item.getPicture()+"'}");
		}
		echo.append("],{xaxis:{label:'Tick',pad:0,tickInterval:49}, yaxis:{label:'Menge'}, selection:'single'});\n");
		echo.append("});</script>");
	}

	private boolean isImportantResource(SortedMap<Integer,Long> values)
	{
		long minValue = values.size()*5;
		long count = 0;
		for( Long value : values.values() )
		{
			count += value;
			if( count >= minValue )
			{
				return true;
			}
		}
		return false;
	}

	private SortedMap<ResourceID, SortedMap<Integer, Long>> generateDataMap(org.hibernate.Session db)
	{
		SortedMap<ResourceID,SortedMap<Integer,Long>> cargos =
			new TreeMap<ResourceID,SortedMap<Integer,Long>>(new ResourceIDComparator(false));

		int counter = 0;
		List<StatCargo> stats = Common.cast(db
				.createQuery("from StatCargo order by tick desc")
				.setMaxResults(30+31) // ca 2 Monate
				.list());
		for( StatCargo sc : stats )
		{
			if( counter++ % 2 == 1 ) {
				// Nur jede zweite Zeile verarbeiten
				continue;
			}
			for( ResourceEntry entry : sc.getCargo().getResourceList() ) {
				if( !cargos.containsKey(entry.getId()) )
				{
					cargos.put(entry.getId(), new TreeMap<Integer,Long>());
				}
				cargos.get(entry.getId()).put(sc.getTick(), entry.getCount1());
			}
		}
		return cargos;
	}

	@Override
	public JSON generateData(StatsController contr, int size)
	{
		User user = (User)contr.getUser();
		org.hibernate.Session db = contr.getDB();

		contr.parameterNumber("key");
		ItemID itemId = new ItemID(contr.getInteger("key"));

		Item item = (Item)db.get(Item.class, itemId.getItemID());
		if( item == null || !user.canSeeItem(item) )
		{
			return new JSONObject();
		}

		SortedMap<ResourceID, SortedMap<Integer, Long>> data = generateDataMap(db);
		JSONObject result = new JSONObject();

		JSONObject itemObj = new JSONObject();
		itemObj.accumulate("id", item.getID());
		itemObj.accumulate("name", item.getName());
		result.accumulate("key", itemObj);

		JSONArray dataArray = new JSONArray();
		SortedMap<Integer,Long> itemData = data.get(itemId);
		if( itemData != null )
		{
			for( Map.Entry<Integer,Long> entry : itemData.entrySet() )
			{
				JSONObject entryObj = new JSONObject();
				entryObj.accumulate("index", entry.getKey());
				entryObj.accumulate("count", entry.getValue());
				dataArray.add(entryObj);
			}
		}
		result.accumulate("data", dataArray);

		return result;
	}
}
