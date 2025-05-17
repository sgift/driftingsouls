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

import net.driftingsouls.ds2.server.ships.Ship;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;

/**
 * TASK_SHIP_DESTROY_COUNTDOWN
 * Ein Countdown bis zur Loeschung des Schiffes.
 * 
 * 	- data1 -> die ID des betroffenen Schiffes
 *  - data2 -> unbenutzt
 *  - data3 -> unbenutzt
 *  
 *  @author Christopher Jung
 */
@Service
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class HandleShipDestroyCountdown implements TaskHandler {
	private final EntityManager db;

    public HandleShipDestroyCountdown(EntityManager db) {
        this.db = db;
    }

    @Override
	public void handleEvent(Task task, String event) {	
		if( event.equals("tick_timeout") ) {
			Ship ship = db.find(Ship.class, Integer.parseInt(task.getData1()));
			
			ship.destroy();
			
			Taskmanager.getInstance().removeTask( task.getTaskID() );
		}
	}

}
