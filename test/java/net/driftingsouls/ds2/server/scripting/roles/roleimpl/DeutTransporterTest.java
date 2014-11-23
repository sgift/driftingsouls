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

import net.driftingsouls.ds2.server.DBSingleTransactionTest;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.items.Ware;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.scripting.roles.interpreter.Interpreter;
import net.driftingsouls.ds2.server.scripting.roles.interpreter.RoleExecuter;
import net.driftingsouls.ds2.server.scripting.roles.parser.RoleDefinition;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Testcase fuer die DeutTransporter-Rolle
 * @author Christopher Jung
 *
 */
public class DeutTransporterTest extends DBSingleTransactionTest
{
	private RoleExecuter roleNearAstiAndNebula;
	
	private Ship transporter;
	private Ship tanker1;
	private Ship tanker2;
	private Ship tanker3;
	private Ship tanker4;
	private Base nearBase;
	private Base farBase;

	private RoleExecuter roleFarAstiAndNebula;
	private StarSystem sys;
	
	@Before
	public void setUp() throws Exception {
		getDB().merge(new Ware(Resources.DEUTERIUM.getItemID(), "Deuterium"));

		sys = persist(new StarSystem());
		sys.setWidth(200);
		sys.setHeight(200);

		persist(new Nebel(new MutableLocation(sys.getID(), 1, 1), Nebel.Typ.LOW_DEUT));
		persist(new Nebel(new MutableLocation(sys.getID(), 50, 50), Nebel.Typ.MEDIUM_EMP));
		persist(new Nebel(new MutableLocation(sys.getID(), 51, 51), Nebel.Typ.MEDIUM_EMP));
		persist(new Nebel(new MutableLocation(sys.getID(), 52, 52), Nebel.Typ.MEDIUM_EMP));
		User user = persist(new User("testuser", "***", 0, "", new Cargo(), "test@localhost"));

		ShipType tankerType = persist(new ShipType(ShipClasses.TANKER));
		tankerType.setDeutFactor(3);
		tankerType.setCargo(1500);
		tankerType.setCost(3);
		tankerType.setHeat(2);
		tankerType.setEps(150);
		tankerType.setRu(2);
		tankerType.setRd(5);
		tankerType.setRm(110);

		ShipType transporterType = persist(new ShipType(ShipClasses.TRANSPORTER));
		transporterType.setCargo(3000);
		transporterType.setCost(1);
		transporterType.setHeat(1);
		transporterType.setEps(150);
		transporterType.setRu(2);
		transporterType.setRd(5);
		transporterType.setRm(110);

		BaseType baseType = persist(new BaseType("TestKlasse"));
		nearBase = persist(new Base(new Location(sys.getID(), 5, 1), user, baseType));
		nearBase.setMaxCargo(10000);
		farBase = persist(new Base(new Location(sys.getID(), 150, 150), user, baseType));
		farBase.setMaxCargo(10000);

		tanker1 = persist(new Ship(user, tankerType, sys.getID(), 1, 1));
		tanker1.setEnergy(150);
		tanker1.setCrew(30);

		tanker2 = persist(new Ship(user, tankerType, sys.getID(), 1, 1));
		tanker2.setEnergy(150);
		tanker2.setCrew(30);
		Cargo cargo2 = tanker2.getCargo();
		cargo2.setResource(Resources.DEUTERIUM, 3000);
		tanker2.setCargo(cargo2);

		tanker3 = persist(new Ship(user, tankerType, sys.getID(), 1, 1));
		tanker3.setCrew(30);
		Cargo cargo3 = tanker3.getCargo();
		cargo3.setResource(Resources.DEUTERIUM, 22);
		tanker3.setCargo(cargo3);

		tanker4 = persist(new Ship(user, tankerType, sys.getID(), 1, 1));
		tanker4.setCrew(30);
		Cargo cargo4 = tanker4.getCargo();
		cargo4.setResource(Resources.DEUTERIUM, 500);
		tanker4.setCargo(cargo4);

		transporter = persist(new Ship(user, transporterType, sys.getID(), 1, 1));
		transporter.setCrew(30);
		transporter.setEnergy(150);
		Cargo cargot = transporter.getCargo();
		cargot.setResource(Resources.DEUTERIUM, 22);
		transporter.setCargo(cargot);

		RoleDefinition deutTransporterRole = new RoleDefinition() {
			public Object getAttribute(String name) {
				if( "base".equals(name) ) {
					return (long)nearBase.getId();
				}
				if( "nebel".equals(name) ) {
					return new Location(sys.getID(), 1, 1);
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
					return (long)farBase.getId();
				}
				if( "nebel".equals(name) ) {
					return new Location(sys.getID(), 1, 1);
				}
				return null;
			}
			
			public String getRoleName() {
				return "DeutTransporter";
			}
		};
		
		this.roleFarAstiAndNebula = Interpreter.executerFromDefinition(deutTransporterRole);
	}

	/**
	 * Testet die Rolle fuer den Fall dass Deuterium verfuegbar und der Asti in einem
	 * Zug erreichbar ist
	 * @throws javax.script.ScriptException
	 */
	@Test
	public void gegebenEinMitDeuteriumGefuellterTransporterUndEineInEinerRundeErreichbareBasis_derTransporterSollteZurBasisFliegenDasDeuteriumAbladenUndZurueckfliegen() throws ScriptException {
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
	 * @throws javax.script.ScriptException
	 */
	@Test
	public void gegebenEinTransporterAusserhalbDesZielnebels_derTransporterSollteInDenNebelFliegen() throws ScriptException {
		// Startposition veraendern
		this.transporter.setX(2);
		
		ScriptParserContext context = new ScriptParserContext();
		context.setAttribute("_SHIP", this.transporter, ScriptContext.ENGINE_SCOPE);
		this.roleNearAstiAndNebula.execute(context);
		
		// Transporter sollte in den Nebel fliegen und dort beenden
		assertThat(this.transporter.getLocation(), is(new Location(sys.getID(), 1, 1)));
		assertThat(this.transporter.getEnergy(), is(149));
	}
	
	/**
	 * Testet den Fall, dass der Transporter nicht im Nebel startet
	 * @throws javax.script.ScriptException
	 */
	@Test
	public void gegebenEinLeererTransporterUndLeereTanker_derTransporterSollteImNebelBleiben() throws ScriptException {
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
		assertThat(this.transporter.getLocation(), is(new Location(sys.getID(), 1, 1)));
		assertThat(this.transporter.getEnergy(), is(150));
	}
	
	/**
	 * Testet den Fall, dass der Transporter nicht im Nebel startet
	 * @throws javax.script.ScriptException
	 */
	@Test
	public void gegebenEinVollerTransporterAusserhalbDesZielnebelsUndLeereTanker_derTransporterSollteDasDeuteriumZurBasisTransportierenUndDannZumNebelFliegen() throws ScriptException {
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
	 * @throws javax.script.ScriptException
	 */
	@Test
	public void gegebenTransporterUndEinPaarVolleTankerUndEineWeitEntfernteBasis_derTransporterSollteUeberMehrereTicksZurBasisFliegenDasDeuteriumAusladenUndZurueckfliegen() throws ScriptException {
		ScriptParserContext context = new ScriptParserContext();
		context.setAttribute("_SHIP", this.transporter, ScriptContext.ENGINE_SCOPE);
		
		// Erster Tick
		this.roleFarAstiAndNebula.execute(context);
		
		assertThat(this.tanker1.getCargo().getResourceCount(Resources.DEUTERIUM), is(0L));
		assertThat(this.tanker2.getCargo().getResourceCount(Resources.DEUTERIUM), is(22L));
		assertThat(this.tanker3.getCargo().getResourceCount(Resources.DEUTERIUM), is(22L));
		assertThat(this.tanker4.getCargo().getResourceCount(Resources.DEUTERIUM), is(500L));
		
		assertThat(this.transporter.getCargo().getResourceCount(Resources.DEUTERIUM), is(3000L));
		
		assertThat(this.transporter.getLocation(), is(new Location(sys.getID(), 101, 101)));
		assertThat(this.transporter.getEnergy(), is(50));
		
		// Zweiter Tick
		this.transporter.setHeat(0);
		this.transporter.setEnergy(150);
		this.roleFarAstiAndNebula.execute(context);
		
		assertThat(this.transporter.getLocation(), is(new Location(sys.getID(), 99, 99)));
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
