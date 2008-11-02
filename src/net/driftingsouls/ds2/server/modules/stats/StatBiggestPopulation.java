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

import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.StatsController;

/**
 * Zeigt die Liste der groessten Bevoelkerungen an
 * @author Christopher Jung
 *
 */
public class StatBiggestPopulation extends AbstractStatistic implements Statistic {
	private boolean allys;

	/**
	 * Konstruktor
	 * @param allys Sollten Allianzen (<code>true</code>) angezeigt werden?
	 */
	public StatBiggestPopulation(boolean allys) {
		this.allys = allys;
	}
	
	public void show(StatsController contr, int size) throws IOException {
		Database db = getContext().getDatabase();

		String url = null;
		
		SQLQuery tmp = null;
		if( !allys ) {
			tmp = db.query("SELECT u.id, tb.res count, u.name " +
					"FROM tmpbev tb JOIN users u ON tb.id=u.id " +
					"ORDER BY count DESC LIMIT "+size);
			
			url = getUserURL();
		}
		else {
			tmp = db.query("SELECT a.id, tb.res count, a.name " +
					"FROM tmpbev tb JOIN ally a ON tb.id=a.id " +
					"ORDER BY count DESC LIMIT "+size);
		
			url = getAllyURL();
		}
	
		this.generateStatistic("Die gr&ouml;&szlig;ten V&ouml;lker:", tmp, url);
		
		tmp.free();
	}

	@Override
	public boolean generateAllyData() {
		return allys;
	}
	
	@Override
	public int getRequiredData() {
		return Statistic.DATA_CREW;
	}
}
