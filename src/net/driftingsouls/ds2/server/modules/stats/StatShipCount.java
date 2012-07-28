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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.StatsController;

/**
 * Zeigt allgemeine Daten zu DS und zum Server an.
 * @author Christopher Jung
 *
 */
public class StatShipCount implements Statistic {
	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();

		Writer echo = context.getResponse().getWriter();

		echo.append("<div id='shipstats'></div><script type='text/javascript'>$(document).ready(function(){\n");
		echo.append("DS.plot('shipstats', [[");

		int curTick = context.get(ContextCommon.class).getTick();
		boolean first = true;
		SQLQuery query = db.query("SELECT tick,shipcount FROM stats_ships WHERE tick>=",curTick-49," ORDER BY tick ASC");
		while( query.next() ) {
			if( !first ) {
				echo.append(',');
			}
			first = false;
			echo.append("["+query.getString("tick")+","+query.getString("shipcount")+"]");
		}
		query.free();

		echo.append("]], {" +
				"axes:{xaxis:{label:'Tick',pad:0,tickInterval:7}, yaxis:{label:'Schiffe in Spielerhand'} }" +
				"} )});");
		echo.append("</script>");
	}
}
