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
package net.driftingsouls.ds2.server.scripting.roles.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementiert die Rollendefinition.
 * @author Christopher Jung
 *
 */
class RoleDefinitionImpl implements RoleDefinition {
	private String roleName;
	private Map<String,Object> attributes = new HashMap<>();
	
	@Override
	public String getRoleName() {
		return roleName;
	}
	
	/**
	 * Setzt den Rollennamen.
	 * @param roleName Der Rollenname
	 */
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	@Override
	public Object getAttribute(String name) {
		return attributes.get(name);
	}
	
	/**
	 * Setzt das Attribut auf den angegebenen Wert.
	 * @param name Der Name des Attributs
	 * @param value Der Wert
	 */
	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}
}
