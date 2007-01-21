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
package net.driftingsouls.ds2.server.comm;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.tasks.Taskmanager;

/**
 * PM-Verwaltung
 * @author Christopher Jung
 * @author Christian Peltz
 *
 */
public class PM {
	/**
	 * Die PM hat einen Admin-Hintergrund
	 */
	public static final int FLAGS_ADMIN = 1;
	/**
	 * Es handelt sich um eine automatisch versendete PM
	 */
	public static final int FLAGS_AUTOMATIC = 2;
	/**
	 * Die PM wurde durch den Tick versendet
	 */
	public static final int FLAGS_TICK = 4; 
	/**
	 * Die PM hat einen rassenspezifischen Hintergrund
	 */
	public static final int FLAGS_OFFICIAL = 8;	// Spezieller (fraktions/rassenspezifischer) Hintergrund
	/**
	 * Die PM muss gelesen werden bevor sie geloescht werden kann
	 */
	public static final int FLAGS_IMPORTANT = 16;	// Muss "absichtlich" gelesen werden
	
	/**
	 * Der PM-Empfaenger des Taskmanagers
	 */
	public static final int TASK = Integer.MIN_VALUE;

	/**
	 * Sendet eine PM von einem Spieler zu einem anderen
	 * @param context Der Kontext
	 * @param from Die ID des versendenden Spielers
	 * @param to Die ID des Spielers, der die PM erhalten soll
	 * @param title Der Titel der PM
	 * @param txt Der Text
	 */
	public static void send( Context context, int from, int to, String title, String txt ) {
		send( context, from, to, title, txt, false, 0);
	}
	
	/**
	 * Sendet eine PM von einem Spieler zu einem anderen Spieler oder einer Allianz
	 * @param context Der Kontext
	 * @param from Die ID des versendenden Spielers
	 * @param to Die ID des Spielers/der Allianz, welche die PM erhalten soll
	 * @param title Der Titel der PM
	 * @param txt Der Text
	 * @param toAlly <code>true</code>, falls es sich um eine Allianz handelt
	 */
	public static void send( Context context, int from, int to, String title, String txt, boolean toAlly ) {
		send( context, from, to, title, txt, toAlly, 0);
	}
	
	/**
	 * Sendet eine PM von einem Spieler zu einem anderen Spieler oder einer Allianz
	 * @param context Der Kontext
	 * @param from Die ID des versendenden Spielers
	 * @param to Die ID des Spielers/der Allianz, welche die PM erhalten soll
	 * @param title Der Titel der PM
	 * @param txt Der Text
	 * @param toAlly <code>true</code>, falls es sich um eine Allianz handelt
	 * @param flags Flags, welche die PM erhalten soll
	 */
	public static void send( Context context, int from, int to, String title, String txt, boolean toAlly, int flags ) {
		Database db = context.getDatabase();

		if( !toAlly ) {
			/*
			 *  Normale PM
			 */
			
			if( to != TASK ) {	
				String msg = db.prepareString(txt);
				title = db.prepareString(title);
			
				User user = context.createUserObject(to);
				if( user.getID() != 0 ) {
					db.update("INSERT INTO transmissionen (sender,empfaenger,inhalt,time,title,flags) VALUES ('",from,"','",to,"','",msg,"','",Common.time(),"','",title,"','",flags,"')");
						
					String forward = user.getUserValue("TBLORDER/pms/forward");
					if( !"".equals(forward) && (Integer.parseInt(forward) != 0) ) {
						User sender = context.createUserObject(from);
						send(context, to, Integer.parseInt(forward), "Fwd: "+title, "[align=center][color=green]- Folgende Nachricht ist soeben eingegangen -[/color][/align]\n[b]Absender:[/b] [userprofile="+sender.getID()+"]"+sender.getName()+"[/userprofile] ("+sender.getID()+")\n\n"+txt, false, flags);
					}
				} 
				else {
					context.addError("Transmission an Spieler "+to+" fehlgeschlagen");	
				}
			}
			/*
			 * Taskverarbeitung (Spezial-PM)
			 */
			else {
				String taskid = title;
				String taskcmd = txt;
				
				Taskmanager taskmanager = Taskmanager.getInstance();
				
				if( taskcmd.equals("handletm") ) {
					taskmanager.handleTask( taskid, "pm_yes" );
				}
				else {
					taskmanager.handleTask( taskid, "pm_no" );	
				}
			}
		}
		else {
			String nameto = db.first("SELECT name FROM ally WHERE id='",to,"'").getString("name");
	
			String msg = "an Allianz "+nameto+"\n"+txt;
	
			msg = db.prepareString(msg);
			title = db.prepareString(title);
	
			SQLQuery auid = db.query("SELECT id FROM users WHERE ally='",to,"'");
			while( auid.next() ) {
				db.update("INSERT INTO transmissionen (sender,empfaenger,inhalt,time,title,flags) VALUES ('"+from+"','"+auid.getInt("id")+"','"+msg+"','"+Common.time()+"','"+title+"','"+flags+"')");
			}
			auid.free();
		}
	}
	
	/**
	 * Sendet eine PM an alle Admins (spezifiziert durch den Konfigurationseintrag <code>ADMIN_PMS_ACCOUT</code>)
	 * @param context Der Kontext
	 * @param from Der versendende Spieler
	 * @param title Der Titel der PM
	 * @param txt Der Text
	 * @param flags Flags, welche die PM erhalten soll
	 */
	public static void sendToAdmins( Context context, int from, String title, String txt, int flags  ) {
		String[] adminlist = Configuration.getSetting("ADMIN_PMS_ACCOUNT").split(",");
		for( String admin : adminlist ) {
			send(context, from, Integer.parseInt(admin), title, txt, false, flags);
		}
	}

	/**
	 * Loescht alle PMs aus einem Ordner eines bestimmten Spielers.
	 * Der Vorgang schlaegt fehl, wenn noch nicht alle wichtigen PMs gelesen wurden
	 * @param ordner_id Der Ordner, dessen Inhalt geloescht werden soll
	 * @param user_id Die ID des Besitzers des Ordners
	 * @return 0, falls der Vorgang erfolgreich war. 1, wenn ein Fehler aufgetreten ist und 2, falls nicht alle PMs gelesen wurden
	 */
	public static int deleteAllInOrdner( int ordner_id, int user_id ){
		Database db = ContextMap.getContext().getDatabase();
		
		int trash = Ordner.getTrash( user_id ).getID();
		
		SQLQuery pm = db.query("SELECT id,empfaenger,flags,gelesen FROM transmissionen WHERE ordner="+ordner_id);
		while( pm.next() ){
			if( pm.getInt("empfaenger") == user_id ) {
				if( ((pm.getInt("flags") & PM.FLAGS_IMPORTANT) != 0) && (pm.getInt("gelesen") < 1) ) {
					return 1;	//PM muss gelesen werden
				}
				db.update("UPDATE transmissionen SET gelesen=2, ordner="+trash+" WHERE id="+pm.getInt("id"));
			} 
			else {
				return 2;	//Loeschen fehlgeschlagen
			}
		}
		pm.free();
		return 0;	//geloescht
	}

	/**
	 * Loescht die PM eines Benutzers
	 * @param pm_id Die ID der PM
	 * @param user_id Die ID des Emfpaengers der PM
	 * @return 0, falls der Vorgang erfolgreich war. 1, wenn ein Fehler aufgetreten ist und 2, falls nicht alle PMs gelesen wurden
	 */
	public static int deleteByID( int pm_id, int user_id ){
		Database db = ContextMap.getContext().getDatabase();
		
		int trash = Ordner.getTrash( user_id ).getID();
		SQLResultRow pm = db.first("SELECT empfaenger,flags,gelesen FROM transmissionen WHERE id="+pm_id);
		if( pm.getInt("empfaenger") == user_id ) {
			if( ((pm.getInt("flags") & PM.FLAGS_IMPORTANT) != 0) && (pm.getInt("gelesen") < 1) ) {
				return 1;	//PM muss gelesen werden
			}
			db.update("UPDATE transmissionen SET gelesen=2, ordner="+trash+" WHERE id="+pm_id);
		} 
		else {
			return 2;	//Loeschen fehlgeschlagen
		}
		return 0;	//geloescht
	}

	/**
	 * Verschiebt alle PMs von einem Ordner in einen anderen
	 * @param source Die ID des Ausgangsordners
	 * @param dest Die ID des Zielordners
	 * @param user_id Die ID des Besitzers der PM
	 */
	public static void moveAllToOrdner( int source, int dest , int user_id){
		Database db = ContextMap.getContext().getDatabase();
		int trash = Ordner.getTrash( user_id ).getID();

		SQLQuery pm = db.query("SELECT id,gelesen FROM transmissionen WHERE ordner="+source+" AND empfaenger="+user_id);
		while( pm.next() ){
			int gelesen = (trash == source) ? 1 : pm.getInt("gelesen");
			gelesen = (trash == dest) ? 2 : gelesen;
			db.update("UPDATE transmissionen SET ordner="+dest+", gelesen='"+gelesen+"' WHERE id="+pm.getInt("id"));
		}
		pm.free();
	}

	/**
	 * Stellt eine geloeschte PM wieder her
	 * @param pm_id Die ID der PM
	 * @param user_id Die ID des Empfaengers der PM
	 */
	public static void recoverByID( int pm_id, int user_id ){
		Database db = ContextMap.getContext().getDatabase();
		int trash = Ordner.getTrash( user_id ).getID();

		db.update("UPDATE transmissionen SET ordner=0,gelesen=1 WHERE ordner='"+trash+"' AND empfaenger='"+user_id+"' AND id='"+pm_id+"'");
	}

	/**
	 * Stelllt alle geloeschten PMs eines Spielers wieder her
	 * @param user_id Die ID des Spielers
	 */
	public static void recoverAll( int user_id ){
		Database db = ContextMap.getContext().getDatabase();
		int trash = Ordner.getTrash( user_id ).getID();
	
		db.update("UPDATE transmissionen SET ordner=0,gelesen=1 WHERE ordner='"+trash+"'");
	}
}
