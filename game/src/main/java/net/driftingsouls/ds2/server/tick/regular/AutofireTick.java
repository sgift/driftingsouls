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
import net.driftingsouls.ds2.server.battles.AutoFire;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.db.batch.UnitOfWork;
import net.driftingsouls.ds2.server.tick.TickController;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fuehrt den Tick fuer Schlachten aus.
 * @author Christopher Jung
 *
 */
@Service
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AutofireTick extends TickController {

    private final ConfigService configService;

    public AutofireTick(ConfigService configService) {
        this.configService = configService;
    }

    @Override
	protected void prepare() {
		// EMPTY
	}

	@Override
	protected void tick() {
		var db = getEM();
        boolean isAutoFire = configService.getValue(WellKnownConfigValue.AUTOFIRE);

        if(!isAutoFire)
        {
            return;
        }

        List<Battle> battles;
        if(isCampaignTick()) {
            battles = db.createQuery("from Battle battle where battle.commander1.id < 0 and battle.system in (:systeme)", Battle.class)
                    .setParameter("systeme", affectedSystems)
                    .getResultList();
        }
        else {
            battles = db.createQuery("from Battle battle where battle.commander1.id < 0", Battle.class)
                    .getResultList();
        }

		new UnitOfWork<Battle>("Battle Tick", db)
		{
			@Override
			public void doWork(Battle battle) {
                var db = getEM();
				battle.load(battle.getCommander(0), null, null, 0, db);
                log("Automatisches Feuer aktiviert für Spieler: " + battle.getCommander(0).getId());
                AutoFire autoFire = new AutoFire(db, battle);
                autoFire.fireShips();
			}
		}.setFlushSize(1).executeFor(battles);

        battles = db.createQuery("from Battle battle where battle.commander2.id < 0", Battle.class)
                .getResultList();
        new UnitOfWork<Battle>("Battle Tick", db)
        {
            @Override
            public void doWork(Battle battle) {
                var db = getEM();
                battle.load(battle.getCommander(1), null, null, 0, db);
                log("Automatisches Feuer aktiviert für Spieler: " + battle.getCommander(1).getId());
                AutoFire autoFire = new AutoFire(db, battle);
                autoFire.fireShips();
            }
        }.setFlushSize(1).executeFor(battles);
	}
}
