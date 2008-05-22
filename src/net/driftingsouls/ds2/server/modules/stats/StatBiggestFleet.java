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

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.StatsController;

/**
 * Zeigt die Liste der groessten Flotten an
 * @author Christopher Jung
 *
 */
public class StatBiggestFleet extends AbstractStatistic implements Statistic {
	private boolean allys;

	/**
	 * Konstruktor
	 * @param allys Sollten Allianzen (<code>true</code>) angezeigt werden?
	 */
	public StatBiggestFleet(boolean allys) {
		this.allys = allys;
	}
	
	public void show(StatsController contr, int size) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();

		String url = null;
		
		SQLQuery tmp = null;
		if( !allys ) {
			tmp = db.query("SELECT SUM( (CASE WHEN t4.size IS NULL THEN t3.size*t3.size ELSE t4.size*t4.size END) * (t1.crew/CASE WHEN t4.crew IS NULL THEN t3.crew ELSE t4.crew END) * (t1.hull/CASE WHEN t4.hull IS NULL THEN t3.hull ELSE t4.hull END) * t1.hull ) count,t2.name,t2.id ",
					    "FROM ((ships t1 JOIN users t2 ON t1.owner=t2.id) JOIN ship_types t3 ON t1.type=t3.id) LEFT OUTER JOIN ships_modules t4 ON t1.id=t4.id ",
						"WHERE t1.id>0 AND t1.owner>",StatsController.MIN_USER_ID," AND (((t4.id IS NULL) AND t3.cost>0) OR ((t4.id IS NOT NULL) AND t4.cost>0)) AND t3.class not in (0,1,3,12,14,18,19,20) ",
						"GROUP BY t1.owner ",
						"ORDER BY count DESC,t2.id ASC LIMIT ",size);
			url = getUserURL();
		}
		else {
			tmp = db.query("SELECT SUM( (CASE WHEN sm.size IS NULL THEN st.size*st.size ELSE sm.size*sm.size END) * (s.crew/CASE WHEN sm.crew IS NULL THEN st.crew ELSE sm.crew END) * (s.hull/CASE WHEN sm.hull IS NULL THEN st.hull ELSE sm.hull END) * s.hull ) count,a.name,u.ally id ",
					    "FROM (((ships s JOIN users u ON s.owner=u.id) JOIN ship_types st ON s.type=st.id) JOIN ally a ON u.ally=a.id) LEFT OUTER JOIN ships_modules sm ON s.id=sm.id ",
						"WHERE s.id>0 AND s.owner>",StatsController.MIN_USER_ID," AND u.ally IS NOT NULL AND (((sm.id IS NULL) AND st.cost>0) OR ((sm.id IS NOT NULL) AND sm.cost>0)) AND st.class not in (0,1,3,12,14,18,19,20) ",
						"GROUP BY u.ally ",
						"ORDER BY count DESC,u.id ASC LIMIT ",size);
		
			url = getAllyURL();
		}
	
		this.generateStatistic("Die groessten Flotten:", tmp, url, false);

		tmp.free();
	}

	@Override
	public boolean generateAllyData() {
		return allys;
	}
}
