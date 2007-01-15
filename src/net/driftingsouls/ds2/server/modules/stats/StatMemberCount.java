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
 * Zeigt die Mitgliederanzahl der Allianzen an
 * @author Christopher Jung
 *
 */
public class StatMemberCount extends AbstractStatistic implements Statistic {

	public void show(StatsController contr, int size) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();

		String url = getAllyURL();
		
		SQLQuery tmp = db.query("SELECT a.id, a.name, count(u.id) count " +
				"FROM ally a JOIN users u ON a.id=u.ally " +
				"WHERE a.id>",StatsController.MIN_USER_ID," " +
				"GROUP BY a.id ORDER BY count DESC LIMIT "+size);
	
		this.generateStatistic("Die gr&ouml;&szlig;ten Allianzen:", tmp, url);
		
		tmp.free();
	}

}
