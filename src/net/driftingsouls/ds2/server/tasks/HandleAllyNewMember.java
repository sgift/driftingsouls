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

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserIterator;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * TASK_ALLY_NEW_MEMBER
 * 		Einer Allianz beitreten (Aufnahmeantrag)
 * 
 * 	- data1 -> die ID der Allianz
 *  - data2 -> die ID des Spielers, der den Antrag gestellt hat
 *  - data3 -> unbenutzt  
 *
 *  @author Christopher Jung
 */
class HandleAllyNewMember implements TaskHandler {

	public void handleEvent(Task task, String event) {	
		Context context = ContextMap.getContext();
		User user = context.getActiveUser();
		Database db = context.getDatabase();
		
		int playerID = Integer.parseInt(task.getData2());
		
		if( event.equals("pm_yes") ) {
			SQLResultRow ally = db.first("SELECT id,name,allytag FROM ally WHERE id=",task.getData1());
			
			User player = context.createUserObject(playerID);
			String newname = ally.getString("allytag");
			newname = StringUtils.replace(newname, "[name]", player.getNickname());
			player.setAlly(ally.getInt("id"));
			player.setName(newname);
			
			int tick = context.get(ContextCommon.class).getTick();
			player.addHistory(Common.getIngameTime(tick)+": Beitritt zur Allianz "+ally.getString("name"));
			
			int membercount = 1;
			
			// Beziehungen auf "Freund" setzen
			UserIterator iter = context.createUserIterator("SELECT * FROM users WHERE ally=",ally.getInt("id")," AND id!=",player.getID());
			for( User allymember : iter ) {
				allymember.setRelation(player.getID(), User.Relation.FRIEND);
				player.setRelation(allymember.getID(), User.Relation.FRIEND);
				
				membercount++;
			}
			iter.free();
			
			PM.send( context, user.getID(), player.getID(), "Aufnahmeantrag", "[Automatische Nachricht]\nDu wurdest in die Allianz >"+ally.getString("name")+"< aufgenommen\n\nHerzlichen Gr&uuml;ckwunsch!");
			
			// Check, ob wir eine TM_TASK_LOW_MEMBER entfernen muessen
			if( membercount == 3 ) {
				Task[] tasks = Taskmanager.getInstance().getTasksByData( Taskmanager.Types.ALLY_LOW_MEMBER, Integer.toString(ally.getInt("id")), "*", "*" );
				for( int i=0; i < tasks.length; i++ ) {
					Taskmanager.getInstance().removeTask( tasks[i].getTaskID() );
				}
			}
		}
		else if( event.equals("pm_no") ) {
			PM.send( context, 0, playerID, "Aufnahmeantrag", "[Automatische Nachricht]\nDein Antrag wurde leider abgelehnt. Es steht dir nun frei ob du einen neuen Antrag nach absprache mit der Allianz stellen willst oder ob du dich an eine andere Allianz wendest.");
		}
		else if( event.equals("tick_timeout") ) {
			PM.send( context, 0, playerID, "Aufnahmeantrag", "[Automatische Nachricht]\nDein Antrag wurde leider nicht innerhalb der vorgegebenen Zeit bearbeitet und daher entfernt. Du hast jedoch jederzeit die M&ouml;glichkeit einen neuen Antrag zu stellen.");
		}
		
		Taskmanager.getInstance().removeTask( task.getTaskID() );
	}

}
