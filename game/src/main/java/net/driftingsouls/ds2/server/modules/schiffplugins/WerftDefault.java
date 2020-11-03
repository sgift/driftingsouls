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
package net.driftingsouls.ds2.server.modules.schiffplugins;

import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.werften.WerftObject;
import net.driftingsouls.ds2.server.werften.WerftQueueEntry;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Schiffsmodul fuer die Anzeige des Werftstatus.
 * @author Christopher Jung
 *
 */
@Component
public class WerftDefault implements SchiffPlugin {
	@PersistenceContext
	private EntityManager em;

	@Action(ActionType.DEFAULT)
	public String action(Parameters parameters) {
		return "";
	}

	@Action(ActionType.DEFAULT)
	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		Ship ship = caller.ship;


		WerftObject werft = em.createQuery("from ShipWerft where ship=:ship", WerftObject.class)
			.setParameter("ship", ship)
			.getSingleResult();

		if( werft != null ) {
			TemplateEngine t = caller.t;
			t.setFile("_PLUGIN_"+pluginid, "schiff.werft.default.html");

			if( werft.getKomplex() != null ) {
				werft = werft.getKomplex();
			}

			final List<WerftQueueEntry> entries = werft.getBuildQueue();
			final int totalSlots = werft.getWerftSlots();
			int usedSlots = 0;
			int buildingCount = 0;
			StringBuilder imBau = new StringBuilder();
			for (WerftQueueEntry entry : entries)
			{
				if (entry.isScheduled())
				{
					usedSlots += entry.getSlots();
					buildingCount++;
					imBau.append("<br />Aktuell im Bau: ").append(entry.getBuildShipType().getNickname()).append(" <img src='data/interface/time.gif' alt='Dauer: ' />").append(entry.getRemainingTime());
				}
			}
			t.setVar(
					"global.pluginid",			pluginid,
					"ship.id",					ship.getId(),
					"schiff.werft.usedslots",	usedSlots,
					"schiff.werft.totalslots",	totalSlots,
					"schiff.werft.scheduled",	buildingCount,
					"schiff.werft.waiting",		entries.size()-buildingCount,
					"schiff.werft.bau", imBau.toString()
					);

			t.parse(caller.target,"_PLUGIN_"+pluginid);
		}
	}

}
