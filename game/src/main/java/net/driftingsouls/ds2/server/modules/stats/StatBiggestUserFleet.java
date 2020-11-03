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

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Zeigt die Liste der groessten Flotten an.
 * @author Christopher Jung
 *
 */

@Component
public class StatBiggestUserFleet extends StatBiggestFleet {
	@PersistenceContext
	private EntityManager em;

	private final BBCodeParser bbCodeParser;

	public StatBiggestUserFleet(BBCodeParser bbCodeParser) {
        super(bbCodeParser);
        this.bbCodeParser = bbCodeParser;
	}

	@Override
	public void show(StatsController contr, int size) throws IOException {
		List<Object[]> tmp = em.createQuery("select "+sumStatement+" as cnt,o " +
			"from Ship s join s.owner o join s.shiptype st left join s.modules sm " +
			"where s.id > 0 and " +
			"	o.id > :minid and " +
			"	(o.vaccount=0 or o.wait4vac>0) and " +
			"	COALESCE(sm.cost,st.cost)>0 and " +
			"	st.shipClass not in :classes " +
			"group by o " +
			"order by "+sumStatement+" desc, o.id asc", Object[].class)
			.setParameter("minid", StatsController.MIN_USER_ID)
			.setParameter("classes", ignoredShipClasses)
			.setMaxResults(size)
			.getResultList();

		String url = getUserURL();

		Writer echo = getContext().getResponse().getWriter();

		echo.append("<h1>Die größten Flotten:</h1>");
		echo.append("<table class='stats'>\n");

		int count = 0;
		for( Object[] values : tmp )
		{
			echo.append("<tr><td>").append(Integer.valueOf(count + 1).toString()).append(".</td>\n");
			User owner = (User)values[1];
			echo.append("<td><a class=\"profile\" href=\"").append(url).append(Integer.valueOf(owner.getId()).toString()).append("\">").append(Common._title(bbCodeParser, owner.getName())).append(" (").append(Integer.valueOf(owner.getId()).toString()).append(")</a></td>\n");

			count++;
			echo.append("</tr>");
		}

		echo.append("</table>\n");
	}
}
