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

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;

/**
 * Zeigt die Liste der groessten Asteroiden an.
 * @author Christopher Jung
 *
 */
public class StatBiggestAsteroid extends AbstractStatistic implements Statistic {
	/**
	 * Konstruktor.
	 *
	 */
	public StatBiggestAsteroid() {
		// EMPTY
	}

	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		List<Base> bases = Common.cast(db
				.createQuery("select b from Base b "+
				"where b.owner.id>:minid and (b.owner.vaccount=0 or b.owner.wait4vac>0) " +
				"order by b.bewohner desc")
				.setParameter("minid", StatsController.MIN_USER_ID)
				.setMaxResults(size)
				.list());

		String url = getUserURL();

		Writer echo = getContext().getResponse().getWriter();

		echo.append("<h1>Die größten Asteroiden:</h1>");
		echo.append("<table class='stats'>\n");

		int count = 0;
		for( Base base : bases )
		{
	   		echo.append("<tr><td>").append(Integer.toString(count + 1)).append(".</td>\n");
			User owner = base.getOwner();
			echo.append("<td><a class=\"profile\" href=\"").append(url).append(Integer.toString(owner.getId())).append("\">").append(Common._title(owner.getName())).append(" (").append(Integer.toString(owner.getId())).append(")</a></td>\n");
			echo.append("<td>").append(Common._plaintitle(base.getName())).append(" (").append(Integer.toString(base.getId())).append(")</td>\n");
			echo.append("<td>").append(Common.ln(base.getBewohner())).append("</td></tr>\n");

	   		count++;
		}

		echo.append("</table>\n");
	}
}
