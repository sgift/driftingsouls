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
package net.driftingsouls.ds2.server.tick.regular;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.services.BaseTickerService;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.map.StarSystemData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * <h1>Berechnung des Ticks fuer Basen.</h1>
 * @author Christopher Jung
 *
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BaseTick extends TickController
{
	private final BaseTickerService baseTickerService;

	@Autowired
	public BaseTick(BaseTickerService baseTickerService)
	{
		this.baseTickerService = baseTickerService;
	}

	@Override
	protected void prepare()
	{
	}

	private void tickBases(List<StarSystemData> systeme)
	{
		org.hibernate.Session db = getDB();

		List<Integer> userIds = Common.cast(db.createQuery("select u.id from User u where u.id != 0 and (u.vaccount=0 or u.wait4vac>0) order by u.id").list());

		new EvictableUnitOfWork<Integer>("Base Tick")
		{
			@Override
			public void doWork(Integer userId) {
				// Get all bases, take everything with them - we need it all.
				List<Base> bases = null;
				if(systeme != null && systeme.size()>0){
					List<Integer> systemIds = systeme.stream().map(StarSystemData::getId).collect(Collectors.toList());
					bases = Common.cast(getDB().createQuery("from Base b fetch all properties where b.owner=:owner and b.system in (:systems)")
						.setInteger("owner", userId)
						.setParameterList("systems", systemIds)
						.setFetchSize(5000)
						.list());
				}
				else
				{
					bases = Common.cast(getDB().createQuery("from Base b fetch all properties where b.owner=:owner")
						.setInteger("owner", userId)
						.setFetchSize(5000)
						.list());
				}

				log(userId+":");

				StringBuilder messages = new StringBuilder();
				for(Base base: bases)
				{
					messages.append(baseTickerService.tick(base));
				}

				if(!messages.toString().trim().equals(""))
				{
					User sourceUser = (User)getDB().get(User.class, -1);
                    User baseUser = (User)getDB().get(User.class, userId);

					if(baseUser.getUserValue(WellKnownUserValue.GAMEPLAY_USER_BASE_DOWN_PM)) {
                        PM.send(sourceUser, userId, "Basis-Tick", messages.toString());
                    }
				}
			}
		}
		.setFlushSize(10)
		.executeFor(userIds);
	}

	@Override
	protected void tick(List<StarSystemData> systeme)
	{
		tickBases(systeme);
		getDB().clear();
	}

	@Override
	protected void tick()
	{
		tick(null);
	}
}
