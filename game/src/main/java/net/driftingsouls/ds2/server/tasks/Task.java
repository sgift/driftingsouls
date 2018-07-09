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
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Eine Task im Taskmanager.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="tasks")
@org.hibernate.annotations.Table(
	appliesTo = "tasks",
	indexes = {@Index(name="taskkey_idx", columnNames = {"type", "time", "data1", "data2", "data3"})}
)
public class Task {
	@Id()
	@Column(name="taskid")
	private String taskID;
	private int type;
	private long time;
	private int timeout;
	private String data1 = "";
	private String data2 = "";
	private String data3 = "";
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public Task() {
		// EMPTY
	}
	
	/**
	 * Erstellt eine neue Task.
	 * @param type Der Typ der Task
	 */
	public Task(Taskmanager.Types type) {
		this.taskID =  Common.md5(Integer.toString(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE)))+Common.time();
		this.type = type.getTypeID();
		this.time = Common.time();
	}

	/**
	 * Gibt den Inhalt des ersten Datenfelds zurueck.
	 * @return das erste Datenfeld
	 */
	public String getData1() {
		return data1;
	}
	
	/**
	 * Setzt den Inhalt des ersten Datenfelds.
	 * @param data Der Inhalt
	 */
	public void setData1(String data) {
		this.data1 = data;
	}

	/**
	 * Gibt den Inhalt des zweiten Datenfelds zurueck.
	 * @return das zweite Datenfeld
	 */
	public String getData2() {
		return data2;
	}
	
	/**
	 * Setzt den Inhalt des zweiten Datenfelds.
	 * @param data Der Inhalt
	 */
	public void setData2(String data) {
		this.data2 = data;
	}

	/**
	 * Gibt den Inhalt des dritten Datenfelds zurueck.
	 * @return das dritte Datenfeld
	 */
	public String getData3() {
		return data3;
	}
	
	/**
	 * Setzt den Inhalt des dritten Datenfelds.
	 * @param data Der Inhalt
	 */
	public void setData3(String data) {
		this.data3 = data;
	}

	/**
	 * Gibt die ID der Task zurueck.
	 * @return Die Task-ID
	 */
	public String getTaskID() {
		return taskID;
	}

	/**
	 * Gibt den Zeitpunkt zurueck, an dem die Task angelegt wurde.
	 * @return Die Timestamp des Erstellungszeitpunkts
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Gibt den Timeout der Task in Ticks zurueck.
	 * @return der Timeout
	 */
	public int getTimeout() {
		return timeout;
	}
	
	/**
	 * Setzt den Timeout der Task in Ticks.
	 * @param timeout der Timeout
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * Gibt den Typ der Task zurueck.
	 * @return der Typ
	 */
	public Taskmanager.Types getType() {
		return Taskmanager.Types.getTypeByID(type);
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
