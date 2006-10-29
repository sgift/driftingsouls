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
	
	protected Task() {
		throw new RuntimeException("STUB");
	}

	public String getData1() {
		return data1;
	}

	public String getData2() {
		return data2;
	}

	public String getData3() {
		return data3;
	}

	public String getTaskID() {
		return taskID;
	}

	public int getTime() {
		return time;
	}

	public int getTimeout() {
		return timeout;
	}

	public Taskmanager.Types getType() {
		return type;
	}
}
