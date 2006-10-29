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
		ALLY_NEW_MEMBER(1, null),
		ALLY_FOUND(2, null),
		ALLY_FOUND_CONFIRM(3, null),
		ALLY_LOW_MEMBER(4, null),
		SHIP_DESTROY_COUNTDOWN(5, null),
		SHIP_RESPAWN_COUNTDOWN(6, null),
		GANY_TRANSPORT(7, null);
		
		private int typeID;
		private Class cls;
		
		private Types( int typeID, Class cls ) {
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
		 * Gibt die Klasse zurueck, die fuer die Verarbeitung von Ereignissen zustaendig ist
		 * @return die Ereignisverarbeitungsklasse
		 */
		protected Class getHandlerClass() {
			return cls;
		}
	}
	private static Taskmanager instance = null;
	
	private Taskmanager() {
		throw new RuntimeException("CLASS STUB");
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
	
	private void registerTask( Types type ) {
		throw new RuntimeException("STUB");
	}
	
	public void addTask( Types tasktype, int timeout, String data1, String data2, String data3 ) {
		throw new RuntimeException("STUB");
	}
	
	public void modifyTask( String taskid, String data1, String data2, String data3 ) {
		throw new RuntimeException("STUB");
	}
	
	public void setTimeout( String taskid, int timeout ) {
		throw new RuntimeException("STUB");
	}
	
	public void incTimeout( String taskid ) {
		throw new RuntimeException("STUB");
	}
	
	public Task getTaskByID( String taskid ) {
		throw new RuntimeException("STUB");
	}
	
	public Task[] getTasksByTimeout( int timeout ) {
		throw new RuntimeException("STUB");
	}
	
	public Task[] getTasksByData( Types type, String data1, String data2, String data3 ) {
		throw new RuntimeException("STUB");
	}
	
	public void handleTask( String taskid, String signal ) {
		throw new RuntimeException("STUB");
		// TODO
	}
	
	public void removeTask( String taskid ) {
		throw new RuntimeException("STUB");
	}

	public void reduceTimeout( int step ) {
		throw new RuntimeException("STUB");
	}
}
