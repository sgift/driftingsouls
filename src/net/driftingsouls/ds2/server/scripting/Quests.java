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
package net.driftingsouls.ds2.server.scripting;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.User;

/**
 * Hilfsfunktionen zur Questverarbeitung
 * @author Christopher Jung
 *
 */
public class Quests {
	public static final String EVENT_ONCOMMUNICATE = "1";
	public static final String EVENT_RUNNING_ONTICK = "2";
	public static final String EVENT_ONENTER = "3";
	public static final String EVENT_ONMOVE = "4";
	
	public static final ThreadLocal<String> currentEventURL = new ThreadLocal<String>() {};
	
	/**
	 * Fuehrt einen Lock-String aus (bzw das dem Lock-String zugeordnete Script, sofern vorhanden)
	 * @param scriptparser Der ScriptParser, mit dem der Lock ausgefuehrt werden soll
	 * @param lock Der Lock-String
	 * @param user Der User unter dem der Lock ausgefuehrt werden soll
	 */
	public static void executeLock( ScriptParser scriptparser, String lock, User user ) {
		// TODO
		throw new RuntimeException("STUB");
	}
	
	/**
	 * Fuehrt einen Ereignis-String aus (bzw das zugehoerige Script)
	 * @param scriptparser Der ScriptParser, mit dem das Ereignis ausgefuhert werden soll
	 * @param handler Der Ereignis-String
	 * @param userid Die Benutzer-ID unter der das Ereignis ausgefuehrt werden soll
	 * @param execparameter Weitere Parameter zur Ausfuehrung
	 * @return <code>true</code>, falls das Ereignis ausgefuert werden konnte
	 */
	public static boolean executeEvent( ScriptParser scriptparser, String handler, int userid, String execparameter ) {
		// TODO
		Common.stub();
		return false;
	}
}
