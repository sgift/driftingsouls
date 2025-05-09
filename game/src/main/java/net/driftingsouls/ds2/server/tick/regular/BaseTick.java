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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

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

	private void tickBases() 
	{
		javax.persistence.EntityManager em = getEM();

		List<Integer> userIds = Common.cast(em.createQuery("select u.id from User u where u.id != 0 and (u.vaccount=0 or u.wait4vac>0) order by u.id").getResultList());

		new EvictableUnitOfWork<Integer>("Base Tick")
		{
			@Override
			public void doWork(Integer userId) {
				// Get all bases, take everything with them - we need it all.
				List<Base> bases;
				if(isCampaignTick()){
					bases = Common.cast(getEM().createQuery("from Base b fetch all properties where b.owner=:owner and b.system in (:systems)")
							.setParameter("owner", userId)
							.setParameter("systems", affectedSystems)
							.getResultList());
				}
				else
				{
					bases = Common.cast(getEM().createQuery("from Base b fetch all properties where b.owner=:owner")
							.setParameter("owner", userId)
							.getResultList());
				}

				log(userId+":");

				StringBuilder messages = new StringBuilder();
				for(Base base: bases)
				{						
					messages.append(baseTickerService.tick(base));
				}

				if(!messages.toString().isBlank())
				{
					User sourceUser = getEM().find(User.class, -1);
                    User baseUser = getEM().find(User.class, userId);

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
	protected void tick() 
	{
		tickBases();
		getEM().clear();
	}
}
