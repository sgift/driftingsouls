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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;

/**
 * TASK_ALLY_FOUND
 * 		Einer Allianz gruenden
 * 
 * 	- data1 -> der Name der Allianz
 *  - data2 -> die Anzahl der noch fehlenden Unterstuetzungen (vgl. TASK_ALLY_FOUND_CONFIRM)
 *  - data3 -> die Spieler, die in die neu gegruendete Allianz sollen, jeweils durch ein , getrennt (Pos: 0 -> Praesident/Gruender)  
 *  @author Christopher Jung
 */
class HandleAllyFound implements TaskHandler {

	public void handleEvent(Task task, String event) {	
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		if( event.equals("__conf_recv") ) {
			int confcount = Integer.parseInt(task.getData1());
			if( confcount == 1 ) {
				String allyname = task.getData2();
				Integer[] allymember = Common.explodeToInteger(",", task.getData3());
				
				int ticks = context.get(ContextCommon.class).getTick();
				
				PreparedQuery insert = db.prepare("INSERT INTO ally (name,plainname,founded,tick,president) VALUES ( ?, ?, now(), ?, ?)");
				insert.update(allyname, Common._titleNoFormat(allyname), ticks, allymember[0]);
				int allyid = insert.insertID();
				insert.close();
		
				Common.copyFile(Configuration.getSetting("ABSOLUTE_PATH")+"data/logos/ally/0.gif", Configuration.getSetting("ABSOLUTE_PATH")+"data/logos/ally"+allyid+".gif");
				
				db.update("UPDATE users SET ally=",allyid,",allyposten=null WHERE id IN (",Common.implode(",",allymember),")");
				
				for( int i=0; i < allymember.length; i++ ) {
					PM.send( ContextMap.getContext(), 0, allymember[i], "Allianzgr&uuml;ndung", "Die Allianz "+allyname+" wurde erfolgreich gegr&uuml;ndet.\n\nHerzlichen Gl&uuml;ckwunsch!");

					User auser = (User)context.getDB().get(User.class, allymember[i]);
					auser.addHistory(Common.getIngameTime(ticks)+": Gr&uuml;ndung der Allianz "+allyname);	
					
					// Beziehungen auf "Freund" setzen
					for( int j=0; j < allymember.length; j++ ) {
						if( allymember[j] == auser.getId() ) {
							continue;
						}
						User allyuser = (User)context.getDB().get(User.class, allymember[j]);
				
						allyuser.setRelation(auser.getId(), User.Relation.FRIEND);
						auser.setRelation(allyuser.getId(), User.Relation.FRIEND);
					}
				}
				
				Taskmanager.getInstance().removeTask( task.getTaskID() );
			}
			else {
				confcount--;
				Taskmanager.getInstance().modifyTask(task.getTaskID(), Integer.toString(confcount), task.getData2(), task.getData3() );	
			}
		}
		else if( event.equals("__conf_dism") ) {
			Integer[] allymember = Common.explodeToInteger(",", task.getData3());
			
			PM.send( context, 0, allymember[0], "Allianzgr&uuml;ndung", "Die Allianzgr&uuml;ndung ist fehlgeschlagen, da ein Spieler seine Unterst&uuml;tzung verweigert hat.");
			Taskmanager.getInstance().removeTask( task.getTaskID() );
			
			Task[] tasklist = Taskmanager.getInstance().getTasksByData( Taskmanager.Types.ALLY_FOUND_CONFIRM, task.getTaskID(), "*", "*" );
			for( int i=0; i < tasklist.length; i++ ) {
				Taskmanager.getInstance().removeTask( tasklist[i].getTaskID() );	
			}
			
			db.update("UPDATE users SET ally='0' WHERE id IN ("+Common.implode(",",allymember),") AND ally='-1'");
		}
		else if( event.equals("tick_timeout") ) {
			Integer[] allymember = Common.explodeToInteger(",", task.getData3());
			
			db.update("UPDATE users SET ally='0' WHERE id IN ("+Common.implode(",",allymember),")");
			Taskmanager.getInstance().removeTask( task.getTaskID() );
			
			for( int i=0; i < allymember.length; i++ ) {
				PM.send( context, 0, allymember[i], "Allianzgr&uuml;ndung", "Die Allianzgr&uuml;ndung ist fehlgeschlagen, da nicht alle angegebenen Spieler in der notwendigen Zeit ihre Unterst&uuml;tzung signalisiert haben.");
			}
		}
	}

}
