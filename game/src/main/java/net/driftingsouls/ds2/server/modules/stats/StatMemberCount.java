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

import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Zeigt die Mitgliederanzahl der Allianzen an.
 * @author Christopher Jung
 *
 */
public class StatMemberCount extends AbstractStatistic implements Statistic {
	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		List<?> tmp = db.createQuery("SELECT ally, count(u) " +
					"FROM User u join u.ally ally " +
					"WHERE u.id> :minid " +
					"GROUP BY ally ORDER BY count(u) DESC")
				.setInteger("minid", StatsController.MIN_USER_ID)
				.setMaxResults(size)
				.list();

		Map<Ally,Long> result = new LinkedHashMap<>();
		for( Object obj : tmp )
		{
			Object[] values = (Object[])obj;
			result.put((Ally) values[0], (Long) values[1]);
		}

		this.generateStatistic("Die größten Allianzen:", result, ALLY_LINK_GENERATOR, true, size);
	}
}
