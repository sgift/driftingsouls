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

import java.io.IOException;

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.services.BattleService;
import net.driftingsouls.ds2.server.services.LocationService;
import net.driftingsouls.ds2.server.services.PmService;
import org.springframework.stereotype.Component;

/**
 * Bricht die Uebernahme des Kommandos durch einen anderen Spieler ab.
 * @author Christopher Jung
 *
 */
@Component
public class KSStopTakeCommandAction extends BasicKSAction {
	private final PmService pmService;
	private final LocationService locationService;

	public KSStopTakeCommandAction(BattleService battleService, PmService pmService, LocationService locationService) {
		super(battleService, null);
		this.pmService = pmService;
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
		
		if( battle.getTakeCommand(battle.getOwnSide()) == 0 ) {
			getBattleService().logme(battle,  "Es versucht niemand das Kommando zu &uuml;bernehmen\n" );
			
			return Result.ERROR;
		}
		
		pmService.send( user, battle.getTakeCommand(battle.getOwnSide()), "Schlacht-&uuml;bergabe abgelehnt", "Die &Uuml;bergabe es Kommandos der Schlacht bei "+locationService.displayCoordinates(battle.getLocation(), false)+" wurde abgelehnt");

		battle.setTakeCommand(battle.getOwnSide(), 0);

		return Result.OK;
	}
}
