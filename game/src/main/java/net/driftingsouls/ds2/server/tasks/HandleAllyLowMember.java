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
package net.driftingsouls.ds2.server.tasks;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.services.AllianzService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;

/**
 * TASK_ALLY_LOW_MEMBER
 * 		Eine Allianz hat weniger als 2 Mitglieder (Praesi eingerechnet) und ist daher von der Aufloesung bedroht.
 * 	- data1 -> die ID der betroffenen Allianz
 *  - data2 -> unbenutzt
 *  - data3 -> unbenutzt
 *
 *  @author Christopher Jung
 */
@Service
public class HandleAllyLowMember implements TaskHandler {
	private final AllianzService allianzService;
	private final EntityManager db;
	private final ConfigService configService;

	@Autowired
	public HandleAllyLowMember(AllianzService allianzService, EntityManager db, ConfigService configService)
	{
		this.allianzService = allianzService;
        this.db = db;
        this.configService = configService;
    }

	@Override
	public void handleEvent(Task task, String event) {
		if( event.equals("tick_timeout") ) {
			int allyid = Integer.parseInt(task.getData1());

			Ally ally = db.find(Ally.class, allyid);
			if( ally == null ) {
				Taskmanager.getInstance().removeTask( task.getTaskID() );
				return;
			}

			User source = db.find(User.class, configService.getValue(WellKnownConfigValue.ALLIANZAUFLOESUNG_PM_SENDER));

			PM.sendToAlly(source, ally, "Allianzauflösung", "[Automatische Nachricht]\n\nDeine Allianz wurde mit sofortiger Wirkung aufgel&ouml;st. Der Grund ist Spielermangel. Grunds&auml;tzlich m&uuml;ssen Allianzen mindestens 2 Mitglieder haben um bestehen zu k&ouml;nnen. Da deine Allianz in der vorgegebenen Zeit dieses Ziel nicht erreichen konnte war die Aufl&ouml;sung unumg&auml;nglich.", db);

			allianzService.loeschen(ally);

			Taskmanager.getInstance().removeTask( task.getTaskID() );
		}
	}

}
