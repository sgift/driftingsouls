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
package net.driftingsouls.ds2.server.scripting.dsscript;


/**
 * Interface fuer ScriptParser-Funktionen.
 * @author Christopher Jung
 *
 */
public interface SPFunction {
	/**
	 * Der ScriptParser soll weiterlaufen und der Funktionszeiger soll inkrementiert werden.
	 */
	public static final boolean[] CONTINUE = new boolean[] {true,true};
	/**
	 * Der ScriptParser soll stoppen und der Funktionszeiger soll nicht inkrementiert werden.
	 */
	public static final boolean[] STOP = new boolean[] {false,false};
	/**
	 * Der ScriptParser soll stoppen und der Funktionszeiger soll inkrementiert werden.
	 */
	public static final boolean[] STOP_AND_INC = new boolean[] {false,true};
	
	/**
	 * Fuehrt die ScriptParser-Funktion aus.
	 * @param scriptparser Der ScriptParser
	 * @param command Die Parameter
	 * @return Array der Laenge 2. Element 1 besagt, ob der ScriptParser weiterlaufen soll. Element 2, ob der Funktionszeiger inkrementiert werden soll
	 */
	public boolean[] execute(ScriptParser scriptparser, String[] command);
}
