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

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.SchlachtLogRundeBeendet;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.services.BattleService;
import net.driftingsouls.ds2.server.tick.TickController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Fuehrt den Tick fuer Schlachten aus.
 * @author Christopher Jung
 *
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BattleTick extends TickController {

	@PersistenceContext
	private EntityManager em;

	private final BattleService battleService;

	public BattleTick(BattleService battleService) {
		this.battleService = battleService;
	}

	@Override
	protected void prepare() {
		// EMPTY
	}

	@Override
	protected void tick() {

		/*
				Schlachten
		 */

		final long lastacttime = Common.time()-1800;

		List<Integer> battles = em.createQuery("select id from Battle", Integer.class).getResultList();

		new EvictableUnitOfWork<Integer>("Battle Tick")
		{
			@Override
			public void doWork(Integer battleId) {
				Battle battle = em.find(Battle.class, battleId);

				if( battle.getBlockCount() > 0 && battle.getLetzteRunde() <= lastacttime )
				{
					battle.decrementBlockCount();
				}

				if( battle.getBlockCount() > 0 && battle.getLetzteAktion() > lastacttime )
				{
					return;
				}

				log("+ Naechste Runde bei Schlacht "+battle.getId());
                battleService.load(battle, battle.getCommander(0), null, null, 0 );
				if( battleService.endTurn(battle,false) )
				{
					// Daten nur aktualisieren, wenn die Schlacht auch weiterhin existiert
					battleService.log(battle, new SchlachtLogRundeBeendet(-1, SchlachtLogRundeBeendet.Modus.ALLE));
				}
			}
		}
		.setFlushSize(1)
		.executeFor(battles);
	}
}
