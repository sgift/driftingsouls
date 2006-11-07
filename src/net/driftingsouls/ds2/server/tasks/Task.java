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

import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Eine Task im Taskmanager
 * @author Christopher Jung
 *
 */
public class Task {
	private String taskID;
	private Taskmanager.Types type;
	private int time;
	private int timeout;
	private String data1;
	private String data2;
	private String data3;
	
	protected Task( SQLResultRow task ) {
		taskID = task.getString("taskid");
		time = task.getInt("time");
		timeout = task.getInt("timeout");
		data1 = task.getString("data1");
		data2 = task.getString("data2");
		data3 = task.getString("data3");
		type = Taskmanager.Types.getTypeByID(task.getInt("type"));
		if( type == null ) {
			throw new RuntimeException("Unbekannter Task-Typ '"+task.getInt("type")+"'");
		}
	}

	/**
	 * Gibt den Inhalt des ersten Datenfelds zurueck
	 * @return das erste Datenfeld
	 */
	public String getData1() {
		return data1;
	}

	/**
	 * Gibt den Inhalt des zweiten Datenfelds zurueck
	 * @return das zweite Datenfeld
	 */
	public String getData2() {
		return data2;
	}

	/**
	 * Gibt den Inhalt des dritten Datenfelds zurueck
	 * @return das dritte Datenfeld
	 */
	public String getData3() {
		return data3;
	}

	/**
	 * Gibt die ID der Task zurueck
	 * @return Die Task-ID
	 */
	public String getTaskID() {
		return taskID;
	}

	/**
	 * Gibt den Zeitpunkt zurueck, an dem die Task angelegt wurde
	 * @return Die Timestamp des Erstellungszeitpunkts
	 */
	public int getTime() {
		return time;
	}

	/**
	 * Gibt den Timeout der Task in Ticks zurueck
	 * @return der Timeout
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Gibt den Typ der Task zurueck
	 * @return der Typ
	 */
	public Taskmanager.Types getType() {
		return type;
	}
}
