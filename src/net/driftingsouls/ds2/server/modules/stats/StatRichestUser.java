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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Zeigt die Liste der reichsten Spieler an.
 * @author Simon Dietsch
 *
 */
public class StatRichestUser extends AbstractStatistic implements Statistic {
	private boolean allys;

	/**
	 * Konstruktor.
	 * @param allys Sollten Allianzen (<code>true</code>) angezeigt werden?
	 */
	public StatRichestUser(boolean allys) {
		this.allys = allys;
	}

	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		if( !allys ) {
			List<User> users = Common.cast(db
				.createQuery("select u from User u where id>:minid and (u.vaccount=0 or u.wait4vac>0) order by konto desc,id desc")
				.setParameter("minid", StatsController.MIN_USER_ID)
				.setMaxResults(size)
				.list());

			Map<User,Long> displayMap = new LinkedHashMap<>();
			for( User user : users )
			{
				displayMap.put(user, user.getKonto().longValue());
			}

			this.generateStatistic("Linked Markets Fortune List:", displayMap, USER_LINK_GENERATOR, false, -1);
		}
		else {
			List<Object[]> allianzen = Common.cast(db
				.createQuery("select a,sum(u.konto) from User u join u.ally a " +
						"where u.id>:minid and (u.vaccount=0 or u.wait4vac>0) group by a,u order by sum(u.konto) desc,u.id desc")
				.setParameter("minid", StatsController.MIN_USER_ID)
				.setMaxResults(size)
				.list());

			Map<Ally,Long> displayMap = new LinkedHashMap<>();
			for( Object[] allianz : allianzen )
			{
				displayMap.put((Ally)allianz[0], ((Number)allianz[1]).longValue());
			}

			this.generateStatistic("Linked Markets Fortune List:", displayMap, ALLY_LINK_GENERATOR, false, -1);
		}
	}
}
