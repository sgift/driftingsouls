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
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.modules.StatsController;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Zeigt die Liste der groessten Bevoelkerungen an.
 * @author Christopher Jung
 *
 */
@Component
public class StatBiggestPopulation extends AbstractStatistic implements Statistic {
	@PersistenceContext
	private EntityManager em;

    public StatBiggestPopulation(BBCodeParser bbCodeParser) {
        super(bbCodeParser);
    }

    @Override
	public void show(StatsController contr, int size) throws IOException {
		Map<User,Long> bevcounts = getUserPopulationData();
		SortedMap<User,Long> sortedBevCounts = new TreeMap<>(new MapValueDescComparator<>(bevcounts));
		sortedBevCounts.putAll(bevcounts);

		this.generateStatistic("Die größten Völker:", sortedBevCounts, USER_LINK_GENERATOR, false, size);
	}

	public Map<User,Long> getUserPopulationData()
	{
		Map<User,Long> bev = new HashMap<>();

		List<Object[]> rows = em
			.createQuery("select sum(s.crew), s.owner from Ship s " +
					"where s.id>0 and s.owner.id>:minid and (s.owner.vaccount=0 or s.owner.wait4vac>0) " +
					"group by s.owner", Object[].class)
			.setParameter("minid", StatsController.MIN_USER_ID)
			.getResultList();
		for( Object[] row : rows )
		{
			bev.put((User)row[1], (Long)row[0]);
		}

		//Bevoelkerung (Basis) pro User ermitteln (+ zur Besatzung pro User addieren)
		rows = em.createQuery("select sum(b.bewohner), b.owner from Base b " +
						"where b.owner.id>:minid and (b.owner.vaccount=0 or b.owner.wait4vac>0) group by b.owner", Object[].class)
				.setParameter("minid", StatsController.MIN_USER_ID)
				.getResultList();
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
}
