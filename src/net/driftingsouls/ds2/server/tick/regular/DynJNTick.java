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

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.config.DynamicJumpNodeConfig;
import net.driftingsouls.ds2.server.entities.DynamicJumpNode;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.framework.db.batch.SingleUnitOfWork;
import net.driftingsouls.ds2.server.tick.TickController;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Berechnet dynamische JumpNodes.
 *
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DynJNTick extends TickController {

	@Override
	protected void prepare()
	{
		// EMPTY
	}

    private void decreaseRemainingTime()
    {
        org.hibernate.Session db = getDB();
        List<DynamicJumpNode> dynjnlist = db.createQuery("from DynamicJumpNode").list();

        new EvictableUnitOfWork<DynamicJumpNode>("DynJNTick - decreaseRemainingTime")
        {
            @Override
            public void doWork(DynamicJumpNode dynjn) throws Exception
            {
                if(dynjn.getRestdauer() <= 1)
                {
                    dynjn.destroy();
                }
                else
                {
                    dynjn.setRestdauer(dynjn.getRestdauer()-1);
                }
            }
        }.executeFor(dynjnlist);
    }

    private void moveDynJN()
    {
        org.hibernate.Session db = getDB();
        List<DynamicJumpNode> dynjnlist = db.createQuery("from DynamicJumpNode").list();

        new EvictableUnitOfWork<DynamicJumpNode>("DynJNTick - moveDynJN")
        {
            @Override
            public void doWork(DynamicJumpNode dynjn) throws Exception
            {
                if(dynjn.getNextMove() <= 1)
                {
                    dynjn.move();
                }
                else
                {
                    dynjn.setNextMove(dynjn.getNextMove()-1);
                }
            }
        }.executeFor(dynjnlist);
    }

    private void spawnDynJN()
    {
        new SingleUnitOfWork("DynJNTick - spawnDynJN")
        {
            @Override
            public void doWork() throws Exception
            {
                org.hibernate.Session db = getDB();

                long dynjnactive = (long) db.createQuery("SELECT count(*) FROM DynamicJumpNode").uniqueResult();
                int dynjnwanted = Integer.valueOf(new ConfigService().get(WellKnownConfigValue.MAX_DYN_JN).getValue());
                long dynjnneeded = dynjnwanted - dynjnactive;

                log("Active Dynamische JN: " + dynjnactive);
                log("Max Dyn JN: " + dynjnwanted);
                log("Erstelle " + dynjnneeded + " Dyn JN");

                 if (dynjnneeded <= 0)
                 {
                    return;
                 }

                  List<DynamicJumpNodeConfig> dynjnconfigs = db.createQuery("from DynamicJumpNodeConfig").list();

                  if (dynjnconfigs.isEmpty())
                  {
                      log("Keine Dynamischen SprungpunktConfigs gefunden.");
                      return;
                  }

                  for (int i = 0; i < dynjnneeded; i++)
                  {
                      int rnd = RandomUtils.nextInt(dynjnconfigs.size());
                      dynjnconfigs.get(rnd).spawnJumpNode();
                  }
            }
        }.execute();
    }

	@Override
	protected void tick()
	{
		this.log("Reduziere Zeit.");
        this.decreaseRemainingTime();

        this.log("Setze um.");
        this.moveDynJN();

        this.log("Spawne neue dynamische JumpNodes.");
        this.spawnDynJN();
	}
}
