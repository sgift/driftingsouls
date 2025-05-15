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

import net.driftingsouls.ds2.server.entities.DynamicJumpNode;
import net.driftingsouls.ds2.server.framework.db.batch.UnitOfWork;
import net.driftingsouls.ds2.server.tick.TickController;
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
    protected void prepare() {
        // EMPTY
    }

    private void decreaseRemainingTime() {
        var db = getEM();

        List<DynamicJumpNode> dynamicJumpNodes;
        if(isCampaignTick()) {
            dynamicJumpNodes = db.createQuery("from DynamicJumpNode jn where jn.jumpnode.system in (:systeme)", DynamicJumpNode.class)
                    .setParameter("systeme", affectedSystems)
                    .getResultList();
        }
        else{
            dynamicJumpNodes = db.createQuery("from DynamicJumpNode", DynamicJumpNode.class).getResultList();
        }

        new UnitOfWork<DynamicJumpNode>("DynJNTick - decreaseRemainingTime", db) {
            @Override
            public void doWork(DynamicJumpNode dynamicJumpNode) {
                var db = getEM();
                if (dynamicJumpNode.getRemainingLiveTime() == 0) {
                    dynamicJumpNode.destroy(db);
                } else {
                    dynamicJumpNode.setRemainingLiveTime(dynamicJumpNode.getRemainingLiveTime() - 1);
                }
            }
        }.executeFor(dynamicJumpNodes);
    }

    private void moveDynJN() {
        var db = getEM();

        List<DynamicJumpNode> dynamicJumpNodes;
        if(isCampaignTick()) {
            dynamicJumpNodes = db.createQuery("from DynamicJumpNode jn where jn.jumpnode.system in (:systeme)", DynamicJumpNode.class)
                    .setParameter("systeme", affectedSystems)
                    .getResultList();
        }
        else{
            dynamicJumpNodes = db.createQuery("from DynamicJumpNode", DynamicJumpNode.class).getResultList();
        }

        new UnitOfWork<DynamicJumpNode>("DynJNTick - moveDynJN", db) {
            @Override
            public void doWork(DynamicJumpNode dynjn) {
                var db = getEM();
                if (dynjn.getRemainingTicksUntilMove() <= 1) {
                    dynjn.move(db);
                } else {
                    dynjn.setRemainingTicksUntilMove(dynjn.getRemainingTicksUntilMove() - 1);
                }
            }
        }.executeFor(dynamicJumpNodes);
    }

    @Override
    protected void tick() {
        this.log("Reduziere Zeit.");
        this.decreaseRemainingTime();

        this.log("Setze um.");
        this.moveDynJN();
    }
}
