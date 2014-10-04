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

import org.springframework.stereotype.Service;

/**
 * TASK_ALLY_FOUND_CONFIRM
 * 		Ein Unterstuetzungsantrag fuer eine Allianzgruendung.
 * 
 * 	- data1 -> die TaskID der zugehoerigen TASK_ALLY_FOUND-Task
 *  - data2 -> die ID des angeschriebenen Spielers (um dessen Unterstuetzung gebeten wurde)
 *  - data3 -> unbenutzt  
 *  @author Christopher Jung
 */
@Service
public class HandleAllyFoundConfirm implements TaskHandler {

	@Override
	public void handleEvent(Task task, String event) {	
		String mastertaskid = task.getData1();
		Taskmanager tm = Taskmanager.getInstance();
		switch (event)
		{
			case "pm_yes":
				tm.handleTask(mastertaskid, "__conf_recv");
				tm.removeTask(task.getTaskID());
				break;
			case "pm_no":
				tm.handleTask(mastertaskid, "__conf_dism");
				tm.removeTask(task.getTaskID());
				break;
			case "tick_timeout":
				tm.removeTask(task.getTaskID());
				break;
		}
	}

}
