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

import org.apache.commons.lang.math.RandomUtils;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Der Taskmanager
 * @author Christopher Jung
 *
 */
public class Taskmanager {
	/**
	 * Die verschiedenen Task-Typen
	 */
	public enum Types {
		/**
		 * Aufnahmeantrag in eine Allianz
		 * data1 - die ID der Allianz
		 * data2 - die ID des Spielers, der den Antrag gestellt hat
		 */
		ALLY_NEW_MEMBER(1, new HandleAllyNewMember()),
		/**
		 * Gruendung einer Allianz
		 * 
		 * data1 - der Name der Allianz
		 * data2 - die Anzahl der noch fehlenden Unterstuetzungen (vgl. TASK_ALLY_FOUND_CONFIRM)
		 * data3 - die Spieler, die in die neu gegruendete Allianz sollen, jeweils durch ein , getrennt (Pos: 0 -> Praesident/Gruender)  
		 */
		ALLY_FOUND(2, new HandleAllyFound()),
		/**
		 *  Ein Unterstuetzungsantrag fuer eine Allianzgruendung
		 * 
		 * 	data1 - die TaskID der zugehoerigen TASK_ALLY_FOUND-Task
		 *  data2 - die ID des angeschriebenen Spielers (um dessen Unterstuetzung gebeten wurde)
		 */
		ALLY_FOUND_CONFIRM(3, new HandleAllyFoundConfirm()),
		/**
		 * Eine Allianz hat weniger als 3 Mitglieder (Praesi eingerechnet) und ist daher von der Aufloesung bedroht
		 * 
		 * data1 - die ID der betroffenen Allianz
		 */
		ALLY_LOW_MEMBER(4, new HandleAllyLowMember()),
		/**
		 * Zerstoert ein Schiff beim Timeout der Task
		 * data1 - Die ID des Schiffes
		 */
		SHIP_DESTROY_COUNTDOWN(5, new HandleShipDestroyCountdown()),
		/**
		 * Ein Countdown bis zum Respawn des Schiffes
		 * 
		 * data1 - die ID des betroffenen Schiffes (neg. id!)
		 */
		SHIP_RESPAWN_COUNTDOWN(6, new HandleShipRespawnCountdown()),
		/**
		 * Ein Gany-Transportauftrag
		 * 
		 * data1 - die Order-ID des Auftrags
		 * data2 - Schiffs-ID [Wird von der Task selbst gesetzt!]
		 * data3 - Status [autom. gesetzt: Nichts = Warte auf Schiff od. flug zur Ganymede; 1 = Gany-Transport; 2 = Rueckweg]
		 */
		GANY_TRANSPORT(7, new HandleGanyTransport());
		
		private int typeID;
		private TaskHandler cls;
		
		private Types( int typeID, TaskHandler cls ) {
			this.typeID = typeID;
			this.cls = cls;
		}
		
		/**
		 * Gibt die Typen-ID der Task zurueck
		 * @return Die Typen-ID
		 */
		public int getTypeID() {
			return typeID;
		}
		
		/**
		 * Gibt die Instanz einer Klasse zurueck, die fuer die Verarbeitung von Ereignissen zustaendig ist
		 * @return die Ereignisverarbeitungsklasseninstanz
		 */
		protected TaskHandler getHandlerClass() {
			return cls;
		}
		
		/**
		 * Gibt den Typ zu einer Typ-ID zurueck
		 * @param id Die Typ-ID
		 * @return Der Typ oder <code>null</code>
		 */
		protected static Types getTypeByID( int id ) {
			for( int i=0; i < values().length; i++ ) {
				if( values()[i].typeID == id ) {
					return values()[i];
				}
			}
			return null;
		}
	}
	private static Taskmanager instance = null;
	
	private Taskmanager() {
		// EMPTY
	}
	
	/**
	 * Gibt eine Instanz des Taskmanagers zurueck
	 * @return Eine Instanz des Taskmanagers
	 */
	public static synchronized Taskmanager getInstance() {
		if( instance == null ) {
			instance = new Taskmanager();
		}
		return instance;
	}

	/**
	 * Fuegt eine neue Task in die Datenbank ein.
	 * Der Inhalt der Datenfelder ist abhaengig von Tasktyp
	 * @param tasktype Der Typ der Task
	 * @param timeout Der Timeout der Task in Ticks (0 bei keinem)
	 * @param data1 Das erste Datenfeld
	 * @param data2 Das zweite Datenfeld
	 * @param data3 Das dritte Datenfeld
	 * @return Die ID der neuen Task
	 */
	public String addTask( Types tasktype, int timeout, String data1, String data2, String data3 ) {
		String taskid = Common.md5(""+RandomUtils.nextInt(Integer.MAX_VALUE))+Common.time();
		
		Database db = ContextMap.getContext().getDatabase();
		
		db.prepare("INSERT INTO tasks ",
				"(`taskid`,`type`,`time`,`timeout`,`data1`,`data2`,`data3`) ",
				"VALUES",
				"( ?, ?, ?, ?, ?, ?, ?)")
			.update(taskid, tasktype.getTypeID(), Common.time(), timeout, data1, data2, data3);
		
		return taskid;
	}
	
	/**
	 * Modifiziert die Datenfelder einer Task
	 * @param taskid Die ID der Task
	 * @param data1 Das erste Datenfeld
	 * @param data2 Das zweite Datenfeld
	 * @param data3 Das dritte Datenfeld
	 */
	public void modifyTask( String taskid, String data1, String data2, String data3 ) {
		Database db = ContextMap.getContext().getDatabase();
		
		db.prepare("UPDATE tasks SET ",
				"`data1`= ?,",
				"`data2`= ?,",
				"`data3`= ? ",
				"WHERE taskid= ? ")
			.update(data1, data2, data3, taskid);
	}
	
	/**
	 * Setzt den Timeout einer Task
	 * @param taskid Die Task-ID
	 * @param timeout Das Timeout in Ticks
	 */
	public void setTimeout( String taskid, int timeout ) {
		Database db = ContextMap.getContext().getDatabase();
		db.prepare("UPDATE tasks SET ",
				"`timeout`= ? ",
				"WHERE taskid= ?")
			.update(taskid, timeout);
	}
	
	/**
	 * Inkrementiert den Timeout einer Task um einen Tick
	 * @param taskid Die ID der Task
	 */
	public void incTimeout( String taskid ) {
		Database db = ContextMap.getContext().getDatabase();
		db.prepare("UPDATE tasks SET ",
				"`timeout`= `timeout`+1 ",
				"WHERE taskid= ?")
			.update(taskid);
	}
	
	/**
	 * Gibt die Task mit der angegebenen ID zurueck. Wenn keine solche Task existiert,
	 * so wird <code>null</code> zurueckgegeben.
	 * @param taskid Die ID der Task
	 * @return die Task oder <code>null</code>
	 */
	public Task getTaskByID( String taskid ) {
		Database db = ContextMap.getContext().getDatabase();
		
		SQLResultRow task = db.prepare("SELECT * FROM tasks WHERE taskid= ?").first(taskid);
		if( !task.isEmpty() ) {
			return new Task(task);	
		}
		return null;
	}
	
	/**
	 * Gibt alle Tasks zurueck, deren Timeout den angegebenen Wert hat
	 * @param timeout das gesuchte Timeout
	 * @return Die Liste aller Tasks mit diesem Timeout (oder eine leere Liste)
	 */
	public Task[] getTasksByTimeout( int timeout ) {
		Database db = ContextMap.getContext().getDatabase();
		List<Task> resultlist = new ArrayList<Task>();
		
		SQLQuery atask = db.prepare("SELECT * FROM tasks WHERE timeout=? ORDER BY `time` ASC").query(timeout);
		while( atask.next() ) {
			resultlist.add(new Task(atask.getRow()));
		}
		atask.free();
		
		return resultlist.toArray(new Task[resultlist.size()]);
	}
	
	/**
	 * Ermittelt alle Tasks eines Typs deren Datenfelder einen bestimmten Inhalt enthalten.
	 * Als Platzhalter fuer beliebigen Inhalt kann das <code>*</code> verwendet werden
	 * @param type Der Typ der gesuchten Task
	 * @param data1 Der Inhalt des ersten Datenfelds
	 * @param data2 Der Inhalt des zweiten Datenfelds
	 * @param data3 Der Inhalt des dritten Datenfelds
	 * @return die Liste aller Tasks, die diesem Muster genuegen
	 */
	public Task[] getTasksByData( Types type, String data1, String data2, String data3 ) {
		Database db = ContextMap.getContext().getDatabase();
		List<Task> resultlist = new ArrayList<Task>();
		
		String query = "SELECT * FROM tasks WHERE type=?";
		if( !data1.equals("*") ) {
			query += " AND data1=?";	
		}
		if( !data2.equals("*") ) {
			query += " AND data2=?";	
		}
		if( !data3.equals("*")) {
			query += " AND data3=?";	
		}
		query += " ORDER BY `time` ASC";
		PreparedQuery pq = db.prepare(query);
		int index=1;
		pq.setInt(index++, type.getTypeID());
		if( !data1.equals("*") ) {
			pq.setString(index++, data1);
		}
		if( !data2.equals("*") ) {
			pq.setString(index++, data2);	
		}
		if( !data3.equals("*")) {
			query += " AND data3=?";
			pq.setString(index++, data3);
		}
		
		SQLQuery atask = pq.query();
		while( atask.next() ) {
			resultlist.add(new Task(atask.getRow()));
		}
		atask.free();
		
		return resultlist.toArray(new Task[resultlist.size()]);
	}
	
	/**
	 * Fuehrt fuer eine Task ein Ereignis aus
	 * @param taskid Die ID der Task
	 * @param signal Das Ereignis
	 */
	public void handleTask( String taskid, String signal ) {
		Task task = getTaskByID(taskid);
		if( task != null ) {
			TaskHandler handler = task.getType().getHandlerClass();
			if( handler != null ) {
				handler.handleEvent(task, signal);
			}
		}
	}
	
	/**
	 * Loescht die Task mit der angegebenen ID
	 * @param taskid Die ID der Task
	 */
	public void removeTask( String taskid ) {
		Database db = ContextMap.getContext().getDatabase();
		db.prepare("DELETE FROM tasks WHERE taskid= ?")
			.update(taskid);
	}
	
	/**
	 * Reduziert den Timeout aller Tasks um den angegebenen Wert
	 * @param step Die Menge um die das Timeout reduziert werden soll
	 */
	public void reduceTimeout( int step ) {
		Database db = ContextMap.getContext().getDatabase();
		db.prepare("UPDATE tasks SET timeout=timeout-? WHERE timeout>?")
			.update(step, step-1);
		
		if( step > 1 ) { 
			db.prepare("UPDATE tasks SET timeout=0 WHERE timeout<=?").update(step-1);
		}
	}
}
