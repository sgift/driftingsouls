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

import net.driftingsouls.ds2.server.Location;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Testcases fuer den Rollenparser
 * @author Christopher Jung
 *
 */
public class ParserTest {
	/**
	 * Testet ob die Parsefunktion korrekt fehlschlaegt
	 *
	 */
	@Test
	public void testParseFailure() {
		try {
			Parser.parse(null);
			fail("ParsingException erwartet");
		}
		catch( ParsingException e ) {
			// Erwartet
		}
		
		try {
			Parser.parse("abcdef");
			fail("ParsingException erwartet");
		}
		catch( ParsingException e ) {
			// Erwartet
		}
		
		try {
			Parser.parse("role: ");
			fail("ParsingException erwartet");
		}
		catch( ParsingException e ) {
			// Erwartet
		}

		try {
			Parser.parse("role:\t");
			fail("ParsingException erwartet");
		}
		catch( ParsingException e ) {
			// Erwartet
		}
		
		try {
			Parser.parse("role: 0");
			fail("ParsingException erwartet");
		}
		catch( ParsingException e ) {
			// Erwartet
		}

		try {
			Parser.parse("role: 0\r");
			fail("ParsingException erwartet");
		}
		catch( ParsingException e ) {
			// Erwartet
		}
	}
	
	/**
	 * Testet die Parsefunktion
	 */
	@Test
	public void testSimpleParse() {
		final String roleDef = "role: Test1Role09";
		RoleDefinition role = Parser.parse(roleDef);
		assertNotNull(role);
		assertThat(role.getRoleName(), is("Test1Role09"));
	}
	
	/**
	 * Testet die Parsefunktion
	 */
	@Test
	public void testAttributeParse() {
		final String roleDef = "role: TestRole\n" +
				"Attribute1: 5\n" +
				"Attribute2: \"Test\\\"0123\\\\Test\"";
		RoleDefinition role = Parser.parse(roleDef);
		assertNotNull(role);
		assertThat(role.getRoleName(), is("TestRole"));
		assertEquals((long) 5, role.getAttribute("Attribute1"));
		assertEquals("Test\"0123\\Test", role.getAttribute("Attribute2"));
	}
	
	/**
	 * Testet die Parsefunktion bei Koordinaten
	 */
	@Test
	public void testAttributeLocationParse() {
		final String roleDef = "role: TestRole2\n" +
				"Attribute1: 1:23/45";
		RoleDefinition role = Parser.parse(roleDef);
		assertNotNull(role);
		assertThat(role.getRoleName(), is("TestRole2"));
		assertEquals(new Location(1, 23, 45), role.getAttribute("Attribute1"));
	}

	@Test
	public void testDerParserKannAuchMitCrLfUmgehen() {
		final String roleDef = "role: DeutTransporter\r\n" +
				"nebel: 1:44/49\r\n"+
				"base: 146";
		RoleDefinition role = Parser.parse(roleDef);
		assertNotNull(role);
		assertThat(role.getRoleName(), is("DeutTransporter"));
		assertEquals(new Location(1, 44, 49), role.getAttribute("nebel"));
		assertEquals(146L, role.getAttribute("base"));
	}

	@Test
	public void testDerParserAkzeptiertNachEinemDoppelpunktAuchEinTab() {
		final String roleDef = "role:\tDeutTransporter\r\n" +
				"nebel:\t1:44/49\r\n"+
				"base:\t146";
		RoleDefinition role = Parser.parse(roleDef);
		assertNotNull(role);
		assertThat(role.getRoleName(), is("DeutTransporter"));
		assertEquals(new Location(1, 44, 49), role.getAttribute("nebel"));
		assertEquals(146L, role.getAttribute("base"));
	}
}
