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
package net.driftingsouls.ds2.server.scripting.roles.roleimpl;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import net.driftingsouls.ds2.server.DriftingSoulsDBTestCase;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.scripting.roles.interpreter.Interpreter;
import net.driftingsouls.ds2.server.scripting.roles.interpreter.RoleExecuter;
import net.driftingsouls.ds2.server.scripting.roles.parser.RoleDefinition;
import net.driftingsouls.ds2.server.ships.Ship;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.Test;

/**
 * Testcase fuer die DeutTransporter-Rolle
 * @author Christopher Jung
 *
 */
public class DeutTransporterTest extends DriftingSoulsDBTestCase {
	private RoleExecuter roleNearAstiAndNebula;
	
	private Ship transporter;
	private Ship tanker1;
	private Ship tanker2;
	private Ship tanker3;
	private Ship tanker4;
	private Base nearBase;
	private Base farBase;

	private RoleExecuter roleFarAstiAndNebula;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		RoleDefinition deutTransporterRole = new RoleDefinition() {
			public Object getAttribute(String name) {
				if( "base".equals(name) ) {
					return Long.valueOf(1);
				}
				if( "nebel".equals(name) ) {
					return new Location(1, 1, 1);
				}
				return null;
			}
			
			public String getRoleName() {
				return "DeutTransporter";
			}
		};
		
		this.roleNearAstiAndNebula = Interpreter.executerFromDefinition(deutTransporterRole);
		
		deutTransporterRole = new RoleDefinition() {
			public Object getAttribute(String name) {
				if( "base".equals(name) ) {
					return Long.valueOf(2);
				}
				if( "nebel".equals(name) ) {
					return new Location(1, 1, 1);
				}
				return null;
			}
			
			public String getRoleName() {
				return "DeutTransporter";
			}
		};
		
		this.roleFarAstiAndNebula = Interpreter.executerFromDefinition(deutTransporterRole);
		
		this.transporter = (Ship)this.context.getDB().get(Ship.class, 5);
		this.tanker1 = (Ship)this.context.getDB().get(Ship.class, 1);
		this.tanker2 = (Ship)this.context.getDB().get(Ship.class, 2);
		this.tanker3 = (Ship)this.context.getDB().get(Ship.class, 3);
		this.tanker4 = (Ship)this.context.getDB().get(Ship.class, 4);
		
		this.nearBase = (Base)this.context.getDB().get(Base.class, 1);
		this.farBase = (Base)this.context.getDB().get(Base.class, 2);
	}

	public IDataSet getDataSet() throws Exception {
		return new FlatXmlDataSet(DeutTransporterTest.class.getResourceAsStream("DeutTransporterTest.xml"));
	}

	/**
	 * Testet die Rolle fuer den Fall dass Deuterium verfuegbar und der Asti in einem
	 * Zug erreichbar ist
	 * @throws ScriptException 
	 */
	@Test
	public void testDeutAvailAndNearAsti() throws ScriptException {
		ScriptParserContext context = new ScriptParserContext();
		context.setAttribute("_SHIP", this.transporter, ScriptContext.ENGINE_SCOPE);
		roleNearAstiAndNebula.execute(context);

		assertThat(this.tanker1.getCargo().getResourceCount(Resources.DEUTERIUM), is(0L));
		assertThat(this.tanker2.getCargo().getResourceCount(Resources.DEUTERIUM), is(22L));
		assertThat(this.tanker3.getCargo().getResourceCount(Resources.DEUTERIUM), is(22L));
		assertThat(this.tanker4.getCargo().getResourceCount(Resources.DEUTERIUM), is(500L));
		
		assertThat(this.nearBase.getCargo().getResourceCount(Resources.DEUTERIUM), is(2978L));
		assertThat(this.transporter.getCargo().getResourceCount(Resources.DEUTERIUM), is(22L));
		
		assertThat(this.transporter.getLocation(), is(tanker1.getLocation()));
		assertThat(this.transporter.getEnergy(), is(142));
	}
	
	/**
	 * Testet den Fall, dass der Transporter nicht im Nebel startet
	 * @throws ScriptException 
	 */
	@Test
	public void testStartNotInNebula() throws ScriptException {	
		// Startposition veraendern
		this.transporter.setX(2);
		
		ScriptParserContext context = new ScriptParserContext();
		context.setAttribute("_SHIP", this.transporter, ScriptContext.ENGINE_SCOPE);
		this.roleNearAstiAndNebula.execute(context);
		
		// Transporter sollte in den Nebel fliegen und dort beenden
		assertThat(this.transporter.getLocation(), is(new Location(1, 1, 1)));
		assertThat(this.transporter.getEnergy(), is(149));
	}
	
	/**
	 * Testet den Fall, dass der Transporter nicht im Nebel startet
	 * @throws ScriptException 
	 */
	@Test
	public void testNoDeutAvailable() throws ScriptException {	
		// Tanker leeren
		Cargo emptyTanker = new Cargo();
		emptyTanker.addResource(Resources.DEUTERIUM, 22);
		this.tanker1.setCargo(emptyTanker);
		this.tanker2.setCargo(emptyTanker);
		this.tanker3.setCargo(emptyTanker);
		this.tanker4.setCargo(emptyTanker);
		
		ScriptParserContext context = new ScriptParserContext();
		context.setAttribute("_SHIP", this.transporter, ScriptContext.ENGINE_SCOPE);
		this.roleNearAstiAndNebula.execute(context);
		
		// Transporter sollte im Nebel bleiben
		assertThat(this.transporter.getLocation(), is(new Location(1, 1, 1)));
		assertThat(this.transporter.getEnergy(), is(150));
	}
	
	/**
	 * Testet den Fall, dass der Transporter nicht im Nebel startet
	 * @throws ScriptException 
	 */
	@Test
	public void testNoDeutAvailableAndTransporterHasDeut() throws ScriptException {	
		// Tanker leeren
		Cargo emptyTanker = new Cargo();
		emptyTanker.addResource(Resources.DEUTERIUM, 22);
		this.tanker1.setCargo(emptyTanker);
		this.tanker2.setCargo(emptyTanker);
		this.tanker3.setCargo(emptyTanker);
		this.tanker4.setCargo(emptyTanker);
		
		Cargo transporterCargo = new Cargo();
		transporterCargo.addResource(Resources.DEUTERIUM, 500);
		this.transporter.setCargo(transporterCargo);
		
		ScriptParserContext context = new ScriptParserContext();
		context.setAttribute("_SHIP", this.transporter, ScriptContext.ENGINE_SCOPE);
		this.roleNearAstiAndNebula.execute(context);
		
		// Transporter sollte das Deuterium transportiert haben
		assertThat(this.nearBase.getCargo().getResourceCount(Resources.DEUTERIUM), is(478L));
		assertThat(this.transporter.getCargo().getResourceCount(Resources.DEUTERIUM), is(22L));
		
		assertThat(this.transporter.getLocation(), is(tanker1.getLocation()));
		assertThat(this.transporter.getEnergy(), is(142));
	}
	
	/**
	 * Testet den Fall, dass der Asti zu weit entfernt ist um in einem Tick hinzugelangen
	 * @throws ScriptException
	 */
	@Test
	public void testAstiFarAway() throws ScriptException {
		ScriptParserContext context = new ScriptParserContext();
		context.setAttribute("_SHIP", this.transporter, ScriptContext.ENGINE_SCOPE);
		
		// Erster Tick
		this.roleFarAstiAndNebula.execute(context);
		
		assertThat(this.tanker1.getCargo().getResourceCount(Resources.DEUTERIUM), is(0L));
		assertThat(this.tanker2.getCargo().getResourceCount(Resources.DEUTERIUM), is(22L));
		assertThat(this.tanker3.getCargo().getResourceCount(Resources.DEUTERIUM), is(22L));
		assertThat(this.tanker4.getCargo().getResourceCount(Resources.DEUTERIUM), is(500L));
		
		assertThat(this.transporter.getCargo().getResourceCount(Resources.DEUTERIUM), is(3000L));
		
		assertThat(this.transporter.getLocation(), is(new Location(1, 101, 101)));
		assertThat(this.transporter.getEnergy(), is(50));
		
		// Zweiter Tick
		this.transporter.setHeat(0);
		this.transporter.setEnergy(150);
		this.roleFarAstiAndNebula.execute(context);
		
		assertThat(this.transporter.getLocation(), is(new Location(1, 99, 99)));
		assertThat(this.transporter.getEnergy(), is(50));
		
		assertThat(this.farBase.getCargo().getResourceCount(Resources.DEUTERIUM), is(2978L));
		assertThat(this.transporter.getCargo().getResourceCount(Resources.DEUTERIUM), is(22L));
		
		// Dritter Tick
		this.transporter.setHeat(0);
		this.transporter.setEnergy(150);
		this.roleFarAstiAndNebula.execute(context);
		
		assertThat(this.transporter.getLocation(), is(tanker1.getLocation()));
		assertThat(this.transporter.getEnergy(), is(52));
	}
}
