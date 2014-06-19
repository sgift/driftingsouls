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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Testfaelle fuer {@link EngineIdentifier}
 * @author Christopher Jung
 *
 */
public class EngineIdentifierTest {

	/**
	 * Testet das Identifizieren von ScriptParser-Scripten via
	 * {@link EngineIdentifier#identifyEngine(String)}
	 */
	@Test
	public void testIdentifyEngine() {
		assertNull(EngineIdentifier.identifyEngine("abcdef"));
		assertThat(EngineIdentifier.identifyEngine("!SHIPMOVE 1:2/3"), is("DSActionScript"));
	}
	
	/**
	 * Testet das Identifizieren von Rollendefinitionen 
	 * via {@link EngineIdentifier#identifyEngine(String)}
	 */
	@Test
	public void testIdentifyRoleEngine() {
		final String roleScript = "role: TestRole\n" +
				"Attribute1: 123";
		assertThat(EngineIdentifier.identifyEngine(roleScript), is("DSRoles"));
	}

}
