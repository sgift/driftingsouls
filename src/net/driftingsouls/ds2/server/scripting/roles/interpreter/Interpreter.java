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
package net.driftingsouls.ds2.server.scripting.roles.interpreter;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import net.driftingsouls.ds2.server.scripting.roles.Role;
import net.driftingsouls.ds2.server.scripting.roles.parser.RoleDefinition;

/**
 * Interpreter fuer Rollendefinitionen.
 * @author Christopher Jung
 *
 */
public class Interpreter {
	private static final Map<String,Class<? extends Role>> roles = new HashMap<>();
	
	static {
		loadRegisteredRoles();
	}
	
	private static void loadRegisteredRoles() {
		ServiceLoader<Role> roleLoader = ServiceLoader.load(Role.class);
		for( Role role : roleLoader ) {
			roles.put(role.getClass().getSimpleName(), role.getClass());
		}
	}
	
	/**
	 * Fuegt die Implementierung einer Rolle zum Interpreter hinzu. Die Implementierung
	 * erhaelt dabei zur Identifizierung einen Namen. Dieser Name muss eindeutig sein.
	 * @param name Der Name der Rolle
	 * @param roleCls Die Implementierung der Rolle
	 * @throws IllegalArgumentException Falls der Rollenname bereits vergeben ist
	 */
	public static void addRole(String name, Class<? extends Role> roleCls) throws IllegalArgumentException {
		synchronized(roles) {
			if( roles.containsKey(name) ) {
				throw new IllegalArgumentException("Rolle '"+name+"' bereits vorhanden");
			}
		
			roles.put(name, roleCls);
		}
	}
	
	/**
	 * Entfernt alle Rollen aus dem Interpreter.
	 */
	public static void cleanUpRoles() {
		synchronized(roles) {
			roles.clear();
			loadRegisteredRoles();
		}
	}
	
	/**
	 * Erzeugt aus der Rollendefinition einen Executer.
	 * @param def Die Rollendefinition
	 * @return Der Executer fuer die Rollendefinition
	 * @throws IllegalRoleDefinitionException Falls die Rollendefinition ungueltig ist
	 */
	public static RoleExecuter executerFromDefinition(RoleDefinition def) throws IllegalRoleDefinitionException {
		synchronized(roles) {
			if( !roles.containsKey(def.getRoleName()) ) {
				throw new IllegalRoleDefinitionException("Unbekannte Rolle '"+def.getRoleName()+"'");
			}
		
			return new RoleExecuterImpl(roles.get(def.getRoleName()), def);
		}
	}
}
