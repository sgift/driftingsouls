/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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

/**
 * Versucht zu einem gegebenen Script den Bezeichner der ScriptEngine zu ermitteln.
 * @author Christopher Jung
 *
 */
public class EngineIdentifier {
	private EngineIdentifier() {
		// EMPTY
	}
	
	/**
	 * Versucht die ScriptEngine zu einem Script zu identifizieren. Sollte
	 * die Identifizierung nicht moeglich sein wird <code>null</code> zurueckgegeben.
	 * @param script Das Script
	 * @return Der Name der ScriptEngine oder <code>null</code>
	 */
	public static String identifyEngine(String script) {
		if( script.startsWith("role:") ) {
			return "DSRoles";
		}
		
		if( !script.contains("!") && !script.contains("#") ) {
			return null;
		}

		return "DSActionScript";
	}
}
