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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.scripting.roles.NopRole;
import net.driftingsouls.ds2.server.scripting.roles.Role;
import net.driftingsouls.ds2.server.scripting.roles.parser.RoleDefinition;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests fuer den Role-Interpreter
 * @author Christopher Jung
 *
 */
public class InterpreterTest {
	private RoleDefinition dummyRole;
	private RoleDefinition unknownRole;
	
	/**
	 * Dummy-Rolle fuer Tests
	 */
	public static class DummyRole implements Role {
		@Attribute("Attribute1")
		protected String attribute1;
		@Attribute("Attribute2")
		protected Long attribute2;
		@Attribute("NotAvailable")
		protected Long attribute3 = 345L;
		
		protected String noAttribute = "notSet";
		
		protected boolean executed = false;

		public void execute(ScriptContext context) throws ScriptException {
			this.executed = true;
		}
	}
	
	/**
	 * Fuegt die notwendigen Rollen dem Interpreter hinzu
	 */
	@Before
	public void setUpRoles() {
		Interpreter.addRole("DummyRole", DummyRole.class);
	}
	
	/**
	 * Entfernt alle registrierten Rollen aus dem Interpreter
	 */
	@After
	public void tearDown() {
		Interpreter.cleanUpRoles();
	}
	
	/**
	 * Erzeugt fuer die Tests notwendige Objekte
	 */
	@Before
	public void setUp() {
		dummyRole = new RoleDefinition() {

			public Object getAttribute(String name) {
				if( "Attribute1".equals(name) ) {
					return "Test";
				}
				if( "Attribute2".equals(name) ) {
					return Long.valueOf(123);
				}
				return null;
			}

			public String getRoleName() {
				return "DummyRole";
			}
		};
		unknownRole = new RoleDefinition() {

			public Object getAttribute(String name) {
				return null;
			}

			public String getRoleName() {
				return "UnknownRole";
			}
		};
	}
	
	/**
	 * Testet das Hinzufuegen von Rollen
	 */
	@Test
	public void testAddRole() {
		Interpreter.addRole("TestRole", DummyRole.class);
		
		try {
			Interpreter.addRole("TestRole", DummyRole.class);
			fail("IllegalArgumentException erwartet");
		}
		catch( IllegalArgumentException e ) {
			// Erwartet
		}
	}
	
	/**
	 * Testet das erzeugen eines RoleExecuters
	 */
	@Test
	public void testSetupRoleExecuter() {
		try {
			Interpreter.executerFromDefinition(this.unknownRole);
			fail("IllegalRoleDefinition erwartet");
		}
		catch( IllegalRoleDefinitionException e ) {
			// erwartet
		}
		
		RoleExecuter roleExec = Interpreter.executerFromDefinition(this.dummyRole);
		assertEquals(DummyRole.class, roleExec.getRole().getClass());
		DummyRole role = (DummyRole)roleExec.getRole();
		assertThat(role.attribute1, is("Test"));
		assertThat(role.attribute2, is(Long.valueOf(123)));
		assertThat(role.attribute3, nullValue());
		assertThat(role.executed, is(false));
		assertThat(role.noAttribute, is("notSet"));
	}
	
	/**
	 * Testet das Ausfuehren einer Rolle
	 * @throws ScriptException
	 */
	@Test
	public void testExecuteRole() throws ScriptException {
		RoleExecuter roleExec = Interpreter.executerFromDefinition(this.dummyRole);
		roleExec.execute(new ScriptParserContext());
		
		DummyRole role = (DummyRole)roleExec.getRole();
		assertThat(role.executed, is(true));
	}
	
	/**
	 *  Testet ob die NopRole automatisch im Interpreter registriert wurde
	 */
	@Test
	public void testNopRolePresent() {
		RoleDefinition nopRole = new RoleDefinition() {
			public Object getAttribute(String name) {
				return null;
			}
			
			public String getRoleName() {
				return "NopRole";
			}
		};
		
		RoleExecuter roleExec = Interpreter.executerFromDefinition(nopRole);
		assertThat(roleExec.getRole(), instanceOf(NopRole.class));
	}
}
