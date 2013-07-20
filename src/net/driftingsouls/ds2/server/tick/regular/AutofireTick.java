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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.AutoFire;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.tick.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.tick.UnitOfWork;

import java.util.List;

/**
 * Fuehrt den Tick fuer Schlachten aus.
 * @author Christopher Jung
 *
 */
public class AutofireTick extends TickController {

	@Override
	protected void prepare() {
		// EMPTY
	}

	@Override
	protected void tick() {
		org.hibernate.Session db = getDB();
        ConfigValue helper = (ConfigValue)db.get(ConfigValue.class, "autofire");
        boolean isAutoFire = helper.getValue().equals("1");

        if(!isAutoFire)
        {
            return;
        }

        List<Integer> battles = Common.cast(db.createQuery("select id from Battle battle where battle.commander1.id < 0").list());
		new EvictableUnitOfWork<Integer>("Battle Tick")
		{
			@Override
			public void doWork(Integer battleId) throws Exception
			{
				org.hibernate.Session db = getDB();
				Battle battle = (Battle)db.get(Battle.class, battleId);
				battle.load( battle.getCommander(0), null, null, 0 );
                log("Automatisches Feuer aktiviert fuer Spieler: " + battle.getCommander(0).getId());
                AutoFire autoFire = new AutoFire(getDB(), battle);
                autoFire.fireShips();
			}
		}.setFlushSize(1).executeFor(battles);

        battles = Common.cast(db.createQuery("select id from Battle battle where battle.commander2.id < 0").list());
        new EvictableUnitOfWork<Integer>("Battle Tick")
        {
            @Override
            public void doWork(Integer battleId) throws Exception
            {
                org.hibernate.Session db = getDB();
                Battle battle = (Battle)db.get(Battle.class, battleId);
                battle.load( battle.getCommander(1), null, null, 0 );
                log("Automatisches Feuer aktiviert fuer Spieler: " + battle.getCommander(1).getId());
                AutoFire autoFire = new AutoFire(getDB(), battle);
                autoFire.fireShips();
            }
        }.setFlushSize(1).executeFor(battles);
	}
}
