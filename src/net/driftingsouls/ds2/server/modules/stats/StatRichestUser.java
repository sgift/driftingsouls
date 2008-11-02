/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Simon Dietsch
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

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.StatsController;

/**
 * Zeigt die Liste der reichsten Spieler an
 * @author Simon Dietsch
 *
 */
public class StatRichestUser extends AbstractStatistic implements Statistic {
	private boolean allys;

	/**
	 * Konstruktor
	 * @param allys Sollten Allianzen (<code>true</code>) angezeigt werden?
	 */
	public StatRichestUser(boolean allys) {
		this.allys = allys;
	}
	
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();

		String url = null;
		
		SQLQuery tmp = null;
		if( !allys ) {
			tmp = db.query("SELECT konto count,name,id ",
					    "FROM users ",
						"WHERE id>",StatsController.MIN_USER_ID," ",
						"ORDER BY count DESC,id ASC LIMIT ",size);
			url = getUserURL();
		}
		else {
			tmp = db.query("SELECT SUM(konto) count,a.name,u.ally id ",
					    "FROM users u JOIN ally a ON u.ally=a.id ",
						"WHERE u.id>",StatsController.MIN_USER_ID," AND u.ally>0 ",
						"GROUP BY u.ally ",
						"ORDER BY count DESC,u.id ASC LIMIT ",size);
		
			url = getAllyURL();
		}
	
		this.generateStatistic("Linked Markets Fortune List:", tmp, url, false);

		tmp.free();
	}

	@Override
	public boolean generateAllyData() {
		return allys;
	}
	
	@Override
	public int getRequiredData() {
		return 0;
	}
}
