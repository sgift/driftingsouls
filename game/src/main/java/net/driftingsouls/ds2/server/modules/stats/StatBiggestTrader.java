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

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.ships.ShipClasses;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Zeigt die Liste der goessten Handelsflotten an.
 * @author Simon Dietsch
 *
 */
public class StatBiggestTrader extends AbstractStatistic implements Statistic {
	private boolean allys;

	/**
	 * Konstruktor.
	 * @param allys Sollten Allianzen (<code>true</code>) angezeigt werden?
	 */
	public StatBiggestTrader(boolean allys) {
		this.allys = allys;
	}

	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		ShipClasses[] zuIgnorierendeSchiffsklassen = {
				ShipClasses.TRANSPORTER,
				ShipClasses.CONTAINER};

		String sumStatement = "sum(COALESCE(sm.cargo, st.cargo)*(s.crew/COALESCE(sm.crew, st.crew))*(s.hull/COALESCE(sm.hull, st.hull))*s.hull)";

		if( !allys ) {
			List<?> tmp = db.createQuery("select "+sumStatement+" as cnt,o " +
					"from Ship s join s.owner o join s.shiptype st left join s.modules sm " +
					"where s.id > 0 and " +
					"	o.id > :minid and " +
					"	(o.vaccount=0 or o.wait4vac>0) and " +
					"	(((sm.id is null) and st.cost>0) or ((sm.id is not null) and sm.cost>0)) and " +
					"	st.shipClass in (:classes) " +
					"group by o " +
					"order by "+sumStatement+" desc, o.id asc")
					.setInteger("minid", StatsController.MIN_USER_ID)
					.setParameterList("classes", zuIgnorierendeSchiffsklassen)
					.setMaxResults(size)
					.list();

			Map<User,Long> result = new LinkedHashMap<>();
			for (Object o : tmp)
			{
				Object[] data = (Object[])o;
				result.put((User)data[1],(Long)data[0]);
			}

			this.generateStatistic("Die größten Handelsflotten:", result, USER_LINK_GENERATOR, false, size);
		}
		else {
			List<?> tmp = db.createQuery("select "+sumStatement+" as cnt,ally " +
					"from Ship s join s.owner o join o.ally ally join s.shiptype st left join s.modules sm " +
					"where s.id > 0 and " +
					"	ally is not null and " +
					"	o.id > :minid and " +
					"	(o.vaccount=0 or o.wait4vac>0) and " +
					"	(((sm.id is null) and st.cost>0) or ((sm.id is not null) and sm.cost>0)) and " +
					"	st.shipClass in (:classes) " +
					"group by ally " +
					"order by "+sumStatement+" desc, ally.id asc")
					.setInteger("minid", StatsController.MIN_USER_ID)
					.setParameterList("classes", zuIgnorierendeSchiffsklassen)
					.setMaxResults(size)
					.list();

			Map<Ally,Long> result = new LinkedHashMap<>();
			for (Object o : tmp)
			{
				Object[] data = (Object[])o;
				result.put((Ally)data[1],(Long)data[0]);
			}

			this.generateStatistic("Die größten Handelsflotten:", result, ALLY_LINK_GENERATOR, false, size);
		}
	}
}
