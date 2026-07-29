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
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Berechnet dynamische JumpNodes.
 *
 */
@Service
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DynJNTick extends TickController {

    @Override
    protected void prepare() {
        // EMPTY
    }

    private void decreaseRemainingTime() {
        var db = getEM();

        // Only the ids are collected up front: the unit of work below clears the persistence context
        // on every flush, which would detach anything loaded here.
        List<Integer> dynamicJumpNodeIds = loadDynamicJumpNodeIds(db);

        new UnitOfWork<Integer>("DynJNTick - decreaseRemainingTime", db) {
            @Override
            public void doWork(Integer dynamicJumpNodeId) {
                var db = getEM();
                DynamicJumpNode dynamicJumpNode = db.find(DynamicJumpNode.class, dynamicJumpNodeId);
                if (dynamicJumpNode == null) {
                    return;
                }
                if (dynamicJumpNode.getRemainingLiveTime() == 0) {
                    dynamicJumpNode.destroy(db);
                } else {
                    dynamicJumpNode.setRemainingLiveTime(dynamicJumpNode.getRemainingLiveTime() - 1);
                }
            }
        }.setClearOnFlush(true).executeFor(dynamicJumpNodeIds);
    }

    private void moveDynJN() {
        var db = getEM();

        List<Integer> dynamicJumpNodeIds = loadDynamicJumpNodeIds(db);

        new UnitOfWork<Integer>("DynJNTick - moveDynJN", db) {
            @Override
            public void doWork(Integer dynjnId) {
                var db = getEM();
                DynamicJumpNode dynjn = db.find(DynamicJumpNode.class, dynjnId);
                if (dynjn == null) {
                    return;
                }
                if (dynjn.getRemainingTicksUntilMove() <= 1) {
                    dynjn.move(db);
                } else {
                    dynjn.setRemainingTicksUntilMove(dynjn.getRemainingTicksUntilMove() - 1);
                }
            }
        }.setClearOnFlush(true).executeFor(dynamicJumpNodeIds);
    }

    private List<Integer> loadDynamicJumpNodeIds(EntityManager db) {
        if(isCampaignTick()) {
            return db.createQuery("select jn.id from DynamicJumpNode jn where jn.jumpnode.system in (:systeme)", Integer.class)
                    .setParameter("systeme", affectedSystems)
                    .getResultList();
        }
        return db.createQuery("select jn.id from DynamicJumpNode jn", Integer.class).getResultList();
    }

    @Override
    protected void tick() {
        this.log("Reduziere Zeit.");
        this.decreaseRemainingTime();

        this.log("Setze um.");
        this.moveDynJN();
    }
}
