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
package net.driftingsouls.ds2.server.modules.ks;

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.battles.SchlachtLogAktion;
import net.driftingsouls.ds2.server.battles.SchlachtLogKommandantWechselt;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.services.BattleService;
import net.driftingsouls.ds2.server.services.LocationService;
import net.driftingsouls.ds2.server.services.PmService;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.List;

/**
 * Uebergibt das Kommando ueber die Schlacht an einen anderen Spieler.
 * @author Christopher Jung
 *
 */
@Component
public class KSNewCommanderAction extends BasicKSAction {
	@PersistenceContext
	private EntityManager em;

	private final PmService pmService;
	private final BBCodeParser bbCodeParser;
	private final LocationService locationService;

	public KSNewCommanderAction(BattleService battleService, PmService pmService, BBCodeParser bbCodeParser, LocationService locationService) {
		super(battleService, null);
		this.pmService = pmService;
		this.bbCodeParser = bbCodeParser;
		this.locationService = locationService;
		this.requireActive(false);
	}

	@Override
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}

		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();

		int newcom = context.getRequest().getParameterInt("newcom");
		User com = em.find(User.class, newcom);

		if( user.getId() == com.getId() ) {
			getBattleService().logme(battle, "Sie können die Leitung der Schlacht nicht an sich selbst übertragen\n" );
			return Result.ERROR;
		}

		if( (battle.getAlly(battle.getOwnSide()) == 0) ||
			((com.getAlly() != null) && (com.getAlly().getId() != battle.getAlly(battle.getOwnSide()))) ) {

			boolean found = false;
			List<BattleShip> ownShips = battle.getOwnShips();
			for (BattleShip ownShip : ownShips)
			{
				if (ownShip.getOwner() == com)
				{
					found = true;
					break;
				}
			}
			if( !found ) {
				getBattleService().logme(battle, "Sie können diesem Spieler nicht die Leitung der Schlacht übertragen!\n" );
				return Result.ERROR;
			}
		}

		if( (com.getVacationCount() != 0) && (com.getWait4VacationCount() == 0) ) {
			getBattleService().logme(battle, "Der Spieler befindet sich im Urlaubsmodus!\n" );
			return Result.ERROR;
		}

		pmService.send(user, com.getId(), "Schlacht übergeben", "Ich habe Dir die Leitung der Schlacht bei "+locationService.displayCoordinates(battle.getLocation(), false)+" übergeben.");

		getBattleService().log(battle, new SchlachtLogAktion(battle.getOwnSide(), "[userprofile="+com.getId()+",profile_alog]"+Common._titleNoFormat(bbCodeParser, com.getName())+"[/userprofile] kommandiert nun die Truppen."));

		battle.setCommander(battle.getOwnSide(), com);

		getBattleService().log(battle, new SchlachtLogKommandantWechselt(battle.getOwnSide(), battle.getCommander(battle.getOwnSide())));

		battle.setTakeCommand(battle.getOwnSide(), 0);

		return Result.OK;
	}
}
