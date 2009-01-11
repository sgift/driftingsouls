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

import java.util.ArrayList;
import java.util.List;

import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * TASK_SHIP_RESPAWN_COUNTDOWN
 * 		Ein Countdown bis zum Respawn des Schiffes.
 * 
 * 	- data1 -> die ID des betroffenen Schiffes (neg. id!)
 *  - data2 -> unbenutzt
 *  - data3 -> unbenutzt
 *   
 *  @author Christopher Jung
 */
class HandleShipRespawnCountdown implements TaskHandler {

	public void handleEvent(Task task, String event) {	
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		if( event.equals("tick_timeout") ) {		
			int shipid = Integer.parseInt(task.getData1());
			if( shipid > 0 ) {
				Taskmanager.getInstance().removeTask( task.getTaskID() );
				return;
			}
			
			// Ueberpruefen wir ersteinmal ob die ID noch frei ist....
			SQLResultRow sid = db.first("SELECT id FROM ships WHERE id=",(-shipid));
			if( !sid.isEmpty() ) {
				User sourceUser = (User)context.getDB().get(User.class, -1);
				
				String msg = "[color=orange]WARNUNG[/color]\nDer Taskmanager kann das Schiff mit der ID "+(-shipid)+" nicht respawnen, da die ID durch ein anderes Schiff blockiert wurde. Der Respawn-Vorgang wurde bis zum n&auml;chsten Tick angehalten. Bitte korregieren sie das Problem umgehend.";
				PM.sendToAdmins(sourceUser, "Taskmanager-Warnung", msg, 0);
				
		 		Taskmanager.getInstance().incTimeout( task.getTaskID() );
		 		return;
			}
			
			// Schiff einfuegen
			SQLResultRow ship = db.first("SELECT * FROM ships WHERE id=",shipid);
			if( ship.isEmpty() ) {
				User sourceUser = (User)context.getDB().get(User.class, -1);
				
				String msg = "[color=orange]WARNUNG[/color]\nDer Taskmanager kann das Schiff mit der ID "+(-shipid)+" nicht respawnen, da die Respawn-Vorlage nicht existiert. Der Respawn-Vorgang wurde bis zum n&auml;chsten Tick angehalten. Bitte korregieren sie das Problem umgehend.";
				PM.sendToAdmins(sourceUser, "Taskmanager-Warnung", msg, 0);
				
		 		Taskmanager.getInstance().incTimeout( task.getTaskID() );
		 		return;
			}
			
			List<String> queryfp = new ArrayList<String>();
			List<String> querylp = new ArrayList<String>();
				
			SQLQuery afield = db.query("SHOW FIELDS FROM ships");
			while( afield.next() ) {
				queryfp.add("`"+afield.getString("Field")+"`");
				if( afield.getString("Field").equals("id") ) {
					querylp.add("'"+(-shipid)+"'");
				}
				else {
					if( (ship.get(afield.getString("Field")) == null) && afield.getString("Null").equals("YES") ) {
						querylp.add("NULL");
					}
					else {
						String value = ship.getString(afield.getString("Field"));
						if( ship.get(afield.getString("Field")) instanceof Boolean ) {
							boolean b = ship.getBoolean(afield.getString("Field"));
							value = b ? "1" : "0";
						}
						querylp.add("'"+db.prepareString(value)+"'");
					}
				}
			}
			afield.free();
			
			db.update("INSERT INTO ships (",Common.implode(",",queryfp)+") VALUES ("+Common.implode(",",querylp)+")");
			
			// Moduldaten einfuegen, falls vorhanden
			SQLResultRow shipmodules = db.first("SELECT * FROM ships_modules WHERE id='",shipid,"'");
			if( !shipmodules.isEmpty() ) {
				queryfp.clear();
				querylp.clear();
			
				afield = db.query("SHOW FIELDS FROM ships_modules");
				while( afield.next() ) {	
					queryfp.add("`"+afield.getString("Field")+"`");
					if( afield.getString("Field").equals("id") ) {
						querylp.add("'"+(-shipid)+"'");
					}
					else {
						if( (shipmodules.get(afield.getString("Field")) == null) && afield.getString("Null").equals("YES") ) {
							querylp.add("NULL");
						}
						else {
							String value = shipmodules.getString(afield.getString("Field"));
							if( shipmodules.get(afield.getString("Field")) instanceof Boolean ) {
								boolean b = shipmodules.getBoolean(afield.getString("Field"));
								value = b ? "1" : "0";
							}
							querylp.add("'"+db.prepareString(value)+"'");
						}
					}
				}
				afield.free();
			
				db.update("INSERT INTO ships_modules (",Common.implode(",",queryfp),") VALUES (",Common.implode(",",querylp),")");
			}
			
			// Offiziere einfuegen, falls vorhanden
			SQLQuery offizier = db.query("SELECT * FROM offiziere WHERE dest='s ",shipid,"'");
			while( offizier.next() ) {
				queryfp.clear();
				querylp.clear();
			
				afield = db.query("SHOW FIELDS FROM offiziere");
				while( afield.next() ) {
					queryfp.add("`"+afield.getString("Field")+"`");
					if( afield.getString("Field").equals("dest") ) {
						querylp.add("'s "+(-shipid)+"'");
					}
					else if( afield.getString("Field").equals("id") ) {
						querylp.add("NULL");
					}
					else {
						if( (offizier.get(afield.getString("Field")) == null) && afield.getString("Null").equals("YES") ) {
							querylp.add("NULL");
						}
						else {
							String value = offizier.getString(afield.getString("Field"));
							if( offizier.get(afield.getString("Field")) instanceof Boolean ) {
								boolean b = offizier.getBoolean(afield.getString("Field"));
								value = b ? "1" : "0";
							}
							querylp.add("'"+db.prepareString(value)+"'");
						}
					}
				}
				afield.free();
			
				db.update("INSERT INTO offiziere (",Common.implode(",",queryfp),") VALUES (",Common.implode(",",querylp),")");
			}
			offizier.free();
			
			// Werfteintrag setzen, falls vorhanden
			SQLResultRow werftentry = db.first("SELECT * FROM werften WHERE shipid='",shipid,"'");
			if( !werftentry.isEmpty() ) {
				queryfp.clear();
				querylp.clear();
		
				afield = db.query("SHOW FIELDS FROM werften");
				while( afield.next() ) {
					queryfp.add("`"+afield.getString("Field")+"`");
					if( afield.getString("Field").equals("shipid") ) {
						querylp.add("'"+(-shipid)+"'");
					}
					else if( afield.getString("Field").equals("id") ) {
						querylp.add("NULL");
					}
					else {
						if( (werftentry.get(afield.getString("Field")) == null) && afield.getString("Null").equals("YES") ) {
							querylp.add("NULL");
						}
						else {
							String value = werftentry.getString(afield.getString("Field"));
							if( werftentry.get(afield.getString("Field")) instanceof Boolean ) {
								boolean b = werftentry.getBoolean(afield.getString("Field"));
								value = b ? "1" : "0";
							}
							querylp.add("'"+db.prepareString(value)+"'");
						}
					}
				}
				afield.free();
			
				db.update("INSERT INTO werften (",Common.implode(",",queryfp),") VALUES (",Common.implode(",",querylp),")");
			}
			Taskmanager.getInstance().removeTask( task.getTaskID() );
		}
	}

}
