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
package net.driftingsouls.ds2.server.modules.stats;

import java.io.IOException;

import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.services.UserService;
import net.driftingsouls.ds2.server.uilibs.PlayerList;
import org.springframework.stereotype.Component;

/**
 * Zeigt die Spielerliste an.
 * @author Christopher Jung
 *
 */
@Component
public class StatPlayerList implements Statistic {
	private final UserService userService;
	private final Rassen races;
	private final BBCodeParser bbCodeParser;

	public StatPlayerList(UserService userService, Rassen races, BBCodeParser bbCodeParser) {
		this.userService = userService;
		this.races = races;
		this.bbCodeParser = bbCodeParser;
	}

	@Override
	public void show(StatsController contr, int size) throws IOException {
		new PlayerList(userService, races, bbCodeParser).draw(ContextMap.getContext());
	}
}
