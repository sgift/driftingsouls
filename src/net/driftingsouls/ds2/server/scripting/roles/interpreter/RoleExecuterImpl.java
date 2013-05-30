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

import java.lang.reflect.Field;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import net.driftingsouls.ds2.server.scripting.roles.Role;
import net.driftingsouls.ds2.server.scripting.roles.parser.RoleDefinition;

class RoleExecuterImpl implements RoleExecuter {
	private Role role;
	
	/**
	 * Konstruktor.
	 * @param roleCls Die Implementierung der Rolle
	 * @param roleDef Die Definition der Rolle
	 * @throws IllegalRoleDefinitionException Falls das erzeugen des Executers nicht moeglich ist
	 */
	public RoleExecuterImpl(Class<? extends Role> roleCls, RoleDefinition roleDef) throws IllegalRoleDefinitionException {
		try {
			this.role = roleCls.newInstance();
				
			for( Field field : roleCls.getDeclaredFields() ) {
				final Attribute attr = field.getAnnotation(Attribute.class);
				if( attr == null ) {
					continue;
				}
				
				final String roleDefAttr = attr.value();
				field.setAccessible(true);
				field.set(this.role, roleDef.getAttribute(roleDefAttr));
			}
		}
		catch( InstantiationException | IllegalAccessException e ) {
			throw new IllegalRoleDefinitionException("Kann Rolle nicht erzeugen", e);
		}
	}
	
	@Override
	public Role getRole() {
		return this.role;
	}

	@Override
	public void execute(ScriptContext context) throws ScriptException {
		this.role.execute(context);
	}
}
