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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.modules.StatsController;

/**
 * Zeigt die Liste der groessten Bevoelkerungen an.
 * @author Christopher Jung
 *
 */
public class StatBiggestPopulation extends AbstractStatistic implements Statistic {
	private boolean allys;

	/**
	 * Konstruktor.
	 * @param allys Sollten Allianzen (<code>true</code>) angezeigt werden?
	 */
	public StatBiggestPopulation(boolean allys) {
		this.allys = allys;
	}

	@Override
	public void show(StatsController contr, int size) throws IOException {
		if( !allys ) {
			Map<User,Long> bevcounts = getUserPopulationData(contr);
			SortedMap<User,Long> sortedBevCounts = new TreeMap<>(new MapValueDescComparator<>(bevcounts));
			sortedBevCounts.putAll(bevcounts);

			this.generateStatistic("Die größten Völker:", sortedBevCounts, USER_LINK_GENERATOR, false, size);
		}
		else {
			Map<Ally,Long> bevcounts = getAllyPopulationData(contr);
			SortedMap<Ally,Long> sortedBevCounts = new TreeMap<>(new MapValueDescComparator<>(bevcounts));
			sortedBevCounts.putAll(bevcounts);

			this.generateStatistic("Die größten Völker:", sortedBevCounts, ALLY_LINK_GENERATOR, false, size);
		}
	}

	public Map<User,Long> getUserPopulationData(StatsController contr)
	{
		org.hibernate.Session db = contr.getDB();

		Map<User,Long> bev = new HashMap<>();

		List<Object[]> rows = Common.cast(db
			.createQuery("select sum(s.crew), s.owner from Ship s " +
					"where s.id>0 and s.owner.id>:minid and (s.owner.vaccount=0 or s.owner.wait4vac>0) " +
					"group by s.owner")
			.setParameter("minid", StatsController.MIN_USER_ID)
			.list());
		for( Object[] row : rows )
		{
			bev.put((User)row[1], (Long)row[0]);
		}

		//Bevoelkerung (Basis) pro User ermitteln (+ zur Besatzung pro User addieren)
		rows = Common.cast(db
				.createQuery("select sum(b.bewohner), b.owner from Base b " +
						"where b.owner.id>:minid and (b.owner.vaccount=0 or b.owner.wait4vac>0) group by b.owner")
				.setParameter("minid", StatsController.MIN_USER_ID)
				.list());
		for( Object[] row : rows )
		{
			User user = (User)row[1];
			Long sum = (Long)row[0];
			if( !bev.containsKey(user) ) {
				bev.put(user, sum);
			}
			else {
				bev.put(user, bev.get(user)+sum);
			}
		}

		return bev;
	}

	public Map<Ally,Long> getAllyPopulationData(StatsController contr)
	{
		org.hibernate.Session db = contr.getDB();
		Map<Ally,Long> bev = new HashMap<>();

		List<Object[]> rows = Common.cast(db
				.createQuery("select sum(s.crew), s.owner.ally " +
						"from Ship s where s.id>0 and s.owner.id>:minid and s.owner.ally is not null " +
						"group by s.owner.ally")
				.setParameter("minid", StatsController.MIN_USER_ID)
				.list());
		for( Object[] row : rows )
		{
			Ally ally = (Ally)row[1];
			bev.put(ally, (Long)row[0]);
		}

		//Bevoelkerung (Basis) pro User ermitteln (+ zur Besatzung pro User addieren)
		rows = Common.cast(db
				.createQuery("select sum(b.bewohner), b.owner.ally " +
						"from Base b where b.owner.id>:minid and b.owner.ally is not null " +
						"group by b.owner.ally")
				.setParameter("minid", StatsController.MIN_USER_ID)
				.list());
		for( Object[] row : rows )
		{
			Ally ally = (Ally)row[1];
			Long sum = (Long)row[0];
			if( !bev.containsKey(ally) ) {
				bev.put(ally, sum);
			}
			else {
				bev.put(ally, bev.get(ally)+sum);
			}
		}

		return bev;
	}
}
