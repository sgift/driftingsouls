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
import net.driftingsouls.ds2.server.battles.SchlachtLogAktion;
import net.driftingsouls.ds2.server.battles.SchlachtLogKommandantWechselt;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import java.io.IOException;

/**
 * Leitet die Uebernahme des Kommandos der Schlacht ein, wenn der aktuelle Kommandant inaktiv ist.
 * @author Christopher Jung
 *
 */
public class KSTakeCommandAction extends BasicKSAction {
	/**
	 * Konstruktor.
	 *
	 */
	public KSTakeCommandAction() {
		this.requireActive(false);
		this.requireCommander(false);
	}

	@Override
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}

		Context context = ContextMap.getContext();

		User user = (User)context.getActiveUser();

        if(!user.hasFlag(UserFlag.KS_TAKE_BATTLES))
        {
            if( (battle.getAlly(battle.getOwnSide()) == 0) ||
                ((user.getAlly() != null) && (battle.getAlly(battle.getOwnSide()) != user.getAlly().getId())) ) {

                battle.logme( "Sie geh&ouml;ren nicht der kommandierenden Allianz an\n" );
                return Result.ERROR;
            }
        }

		if( battle.getTakeCommand(battle.getOwnSide()) != 0 ) {
			battle.logme( "Es versucht bereits ein anderer Spieler das Kommando zu &uuml;bernehmen\n" );
			return Result.ERROR;
		}
		/*
		User oldCommander = battle.getCommander(battle.getOwnSide());
		if( oldCommander.getInactivity() <= 0 ) {
			battle.logme( "Der kommandierende Spieler ist noch anwesend\n" );
			return Result.ERROR;
		}
		*/
		battle.setCommander(battle.getOwnSide(), user);
		battle.log(new SchlachtLogAktion(battle.getOwnSide(), "[userprofile="+user.getId()+",profile_alog]"+Common._titleNoFormat(user.getName())+"[/userprofile] hat die Schlacht übernommen"));
		battle.log(new SchlachtLogKommandantWechselt(battle.getOwnSide(), battle.getCommander(battle.getOwnSide())));

		return Result.OK;
	}
}
