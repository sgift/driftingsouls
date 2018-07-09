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
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
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
        org.hibernate.Session db = getDB();
        @SuppressWarnings("unchecked")
        List<DynamicJumpNode> dynamicJumpNodes = db.createQuery("from DynamicJumpNode").list();

        new EvictableUnitOfWork<DynamicJumpNode>("DynJNTick - decreaseRemainingTime") {
            @Override
            public void doWork(DynamicJumpNode dynamicJumpNode) {
                if (dynamicJumpNode.getRemainingLiveTime() == 0) {
                    dynamicJumpNode.destroy();
                } else {
                    dynamicJumpNode.setRemainingLiveTime(dynamicJumpNode.getRemainingLiveTime() - 1);
                }
            }
        }.executeFor(dynamicJumpNodes);
    }

    private void moveDynJN() {
        org.hibernate.Session db = getDB();
        @SuppressWarnings("unchecked")
        List<DynamicJumpNode> dynamicJumpNodes = db.createQuery("from DynamicJumpNode").list();

        new EvictableUnitOfWork<DynamicJumpNode>("DynJNTick - moveDynJN") {
            @Override
            public void doWork(DynamicJumpNode dynjn) {
                if (dynjn.getRemainingTicksUntilMove() <= 1) {
                    dynjn.move();
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
