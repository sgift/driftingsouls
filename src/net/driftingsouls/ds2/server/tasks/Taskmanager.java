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

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.Query;

import java.util.List;

/**
 * Der Taskmanager.
 * @author Christopher Jung
 *
 */
public class Taskmanager {
	/**
	 * Die verschiedenen Task-Typen.
	 */
	public enum Types {
		/**
		 * Aufnahmeantrag in eine Allianz.
		 * data1 - die ID der Allianz
		 * data2 - die ID des Spielers, der den Antrag gestellt hat
		 */
		ALLY_NEW_MEMBER(1, HandleAllyNewMember.class),
		/**
		 * Gruendung einer Allianz.
		 *
		 * data1 - der Name der Allianz
		 * data2 - die Anzahl der noch fehlenden Unterstuetzungen (vgl. TASK_ALLY_FOUND_CONFIRM)
		 * data3 - die Spieler, die in die neu gegruendete Allianz sollen, jeweils durch ein , getrennt (Pos: 0 -> Praesident/Gruender)
		 */
		ALLY_FOUND(2, HandleAllyFound.class),
		/**
		 *  Ein Unterstuetzungsantrag fuer eine Allianzgruendung.
		 *
		 * 	data1 - die TaskID der zugehoerigen TASK_ALLY_FOUND-Task
		 *  data2 - die ID des angeschriebenen Spielers (um dessen Unterstuetzung gebeten wurde)
		 */
		ALLY_FOUND_CONFIRM(3, HandleAllyFoundConfirm.class),
		/**
		 * Eine Allianz hat weniger als 3 Mitglieder (Praesi eingerechnet) und ist daher von der Aufloesung bedroht.
		 *
		 * data1 - die ID der betroffenen Allianz
		 */
		ALLY_LOW_MEMBER(4, HandleAllyLowMember.class),
		/**
		 * Zerstoert ein Schiff beim Timeout der Task.
		 * data1 - Die ID des Schiffes
		 */
		SHIP_DESTROY_COUNTDOWN(5, HandleShipDestroyCountdown.class),
		/**
		 * Ein Gany-Transportauftrag.
		 *
		 * data1 - die Order-ID des Auftrags
		 * data2 - Schiffs-ID [Wird von der Task selbst gesetzt!]
		 * data3 - Status [autom. gesetzt: Nichts = Warte auf Schiff od. flug zur Ganymede; 1 = Gany-Transport; 2 = Rueckweg]
		 */
		GANY_TRANSPORT(7, HandleGanyTransport.class),
		/**
		 * Ein Ausbau-Auftrag.
		 *
		 * data1 - Die Auftrags-ID
		 * data2 - Die Anzahl der bisherigen Versuche den Task durchzuführen
		 */
		UPGRADE_JOB(8, HandleUpgradeJob.class);

		private int typeID;
		private Class<? extends TaskHandler> cls;

		Types(int typeID, Class<? extends TaskHandler> cls) {
			this.typeID = typeID;
			this.cls = cls;
		}

		/**
		 * Gibt die Typen-ID der Task zurueck.
		 * @return Die Typen-ID
		 */
		public int getTypeID() {
			return typeID;
		}

		/**
		 * Gibt die Klasse zurueck, die fuer die Verarbeitung von Ereignissen zustaendig ist.
		 * @return die Ereignisverarbeitungsklasse
		 */
		protected Class<? extends TaskHandler> getHandlerClass() {
			return cls;
		}

		/**
		 * Gibt den Typ zu einer Typ-ID zurueck.
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
	 * Gibt eine Instanz des Taskmanagers zurueck.
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
		Task task = new Task(tasktype);
		task.setTimeout(timeout);
		task.setData1(data1);
		task.setData2(data2);
		task.setData3(data3);

		org.hibernate.Session db = ContextMap.getContext().getDB();
		db.persist(task);

		return task.getTaskID();
	}

	/**
	 * Modifiziert die Datenfelder einer Task.
	 * @param taskid Die ID der Task
	 * @param data1 Das erste Datenfeld
	 * @param data2 Das zweite Datenfeld
	 * @param data3 Das dritte Datenfeld
	 */
	public void modifyTask( String taskid, String data1, String data2, String data3 ) {
		Task task = getTaskByID(taskid);
		task.setData1(data1);
		task.setData2(data2);
		task.setData3(data3);
	}

	/**
	 * Setzt den Timeout einer Task.
	 * @param taskid Die Task-ID
	 * @param timeout Das Timeout in Ticks
	 */
	public void setTimeout( String taskid, int timeout ) {
		Task task = getTaskByID(taskid);
		task.setTimeout(timeout);
	}

	/**
	 * Inkrementiert den Timeout einer Task um einen Tick.
	 * @param taskid Die ID der Task
	 */
	public void incTimeout( String taskid ) {
		Task task = getTaskByID(taskid);
		task.setTimeout(task.getTimeout()+1);
	}

	/**
	 * Gibt die Task mit der angegebenen ID zurueck. Wenn keine solche Task existiert,
	 * so wird <code>null</code> zurueckgegeben.
	 * @param taskid Die ID der Task
	 * @return die Task oder <code>null</code>
	 */
	public Task getTaskByID( String taskid ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		return (Task)db.get(Task.class, taskid);
	}

	/**
	 * Gibt alle Tasks zurueck, deren Timeout den angegebenen Wert hat.
	 * @param timeout das gesuchte Timeout
	 * @return Die Liste aller Tasks mit diesem Timeout (oder eine leere Liste)
	 */
	public Task[] getTasksByTimeout( int timeout ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		List<Task> tasks = Common.cast(db.createQuery("from Task where timeout=:timeout order by time asc")
			.setInteger("timeout", timeout)
			.list());

		return tasks.toArray(new Task[tasks.size()]);
	}

	/**
	 * Ermittelt alle Tasks eines Typs deren Datenfelder einen bestimmten Inhalt enthalten.
	 * Als Platzhalter fuer beliebigen Inhalt kann das <code>*</code> verwendet werden.
	 * @param type Der Typ der gesuchten Task
	 * @param data1 Der Inhalt des ersten Datenfelds
	 * @param data2 Der Inhalt des zweiten Datenfelds
	 * @param data3 Der Inhalt des dritten Datenfelds
	 * @return die Liste aller Tasks, die diesem Muster genuegen
	 */
	public Task[] getTasksByData( Types type, String data1, String data2, String data3 ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		String query = "from Task where type=:type";
		if( !data1.equals("*") ) {
			query += " and data1=:d1";
		}
		if( !data2.equals("*") ) {
			query += " and data2=:d2";
		}
		if( !data3.equals("*")) {
			query += " and data3=:d3";
		}
		query += " order by time asc";
		Query q = db.createQuery(query);
		q.setInteger("type", type.getTypeID());
		if( !data1.equals("*") ) {
			q.setString("d1", data1);
		}
		if( !data2.equals("*") ) {
			q.setString("d2", data2);
		}
		if( !data3.equals("*")) {
			q.setString("d3", data3);
		}

		List<Task> tasks = Common.cast(q.list());

		return tasks.toArray(new Task[tasks.size()]);
	}

	/**
	 * Fuehrt fuer eine Task ein Ereignis aus.
	 * @param taskid Die ID der Task
	 * @param signal Das Ereignis
	 */
	public void handleTask( String taskid, String signal ) {
		Task task = getTaskByID(taskid);
		if( task != null ) {
			Class<? extends TaskHandler> handlerClass = task.getType().getHandlerClass();

			TaskHandler handler = ContextMap.getContext().getBean(handlerClass, null);
			if( handler != null ) {
				handler.handleEvent(task, signal);
			}
		}
	}

	/**
	 * Loescht die Task mit der angegebenen ID.
	 * @param taskid Die ID der Task
	 */
	public void removeTask( String taskid ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		Task task = getTaskByID(taskid);
		if( task != null )
		{
			db.delete(task);
		}
	}

	/**
	 * Reduziert den Timeout aller Tasks um den angegebenen Wert.
	 * @param step Die Menge um die das Timeout reduziert werden soll
	 */
	public void reduceTimeout( int step ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		db.createQuery("update Task set timeout=timeout-:timeout where timeout>:timeout2")
			.setInteger("timeout", step)
			.setInteger("timeout2", step-1)
			.executeUpdate();

		if( step > 1 ) {
			db.createQuery("update Task set timeout=0 where timeout<=:timeout")
				.setInteger("timeout", step-1)
				.executeUpdate();
		}
	}
}
