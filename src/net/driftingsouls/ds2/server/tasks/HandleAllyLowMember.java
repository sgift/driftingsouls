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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserIterator;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * TASK_ALLY_LOW_MEMBER
 * 		Eine Allianz hat weniger als 3 Mitglieder (Praesi eingerechnet) und ist daher von der Aufloesung bedroht
 * 
 * 	- data1 -> die ID der betroffenen Allianz
 *  - data2 -> unbenutzt
 *  - data3 -> unbenutzt
 *   
 *  @author Christopher Jung
 */
class HandleAllyLowMember implements TaskHandler {

	public void handleEvent(Task task, String event) {	
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		if( event.equals("tick_timeout") ) {
			int allyid = Integer.parseInt(task.getData1());
			
			SQLResultRow ally = db.first("SELECT id FROM ally WHERE id=",allyid);
			if( ally.isEmpty() ) {
				Taskmanager.getInstance().removeTask( task.getTaskID() );
				return;	
			}
		
			db.tBegin();
			
			int tick = context.get(ContextCommon.class).getTick();
		
			UserIterator iter = context.createUserIterator("SELECT * FROM users WHERE ally=",allyid);
			for( User member : iter ) {
				PM.send( context, 0, member.getID(), "Allianzaufl&ouml;sung", "[Automatische Nachricht]\n\nDeine Allianz wurde mit sofortiger Wirkung aufgel&ouml;st. Der Grund ist Spielermangel. Grunds&auml;tzlich m&uuml;ssen Allianzen mindestens 3 Mitglieder haben um bestehen zu k&ouml;nnen. Da deine Allianz in der vorgegebenen Zeit dieses Ziel nicht erreichen konnte war die Aufl&ouml;sung unumg&auml;nglich.");
				member.addHistory(Common.getIngameTime(tick)+": Austritt aus der Allianz "+ally.getString("name")+" im Zuge der Zwangaufl&ouml;sung");
			}
			iter.free();
			
			SQLQuery chn = db.query("SELECT id FROM skn_channels WHERE allyowner=",allyid);
			while( chn.next() ) {
				db.update("DELETE FROM skn_visits WHERE channel=",chn.getInt("id"));
				db.update("DELETE FROM skn WHERE channel="+chn.getInt("id"));
				db.update("DELETE FROM skn_channels WHERE id="+chn.getInt("id"));
			}
			chn.free();

			db.update("UPDATE users SET ally=0,allyposten=0,name=nickname WHERE ally=",allyid);
			db.update("DELETE FROM ally_posten WHERE ally=",allyid);
			db.update("DELETE FROM ally WHERE id=",allyid);
			
			db.tCommit();
			
			Taskmanager.getInstance().removeTask( task.getTaskID() );
		}
	}

}
