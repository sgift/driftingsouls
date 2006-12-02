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
package net.driftingsouls.ds2.server.tick;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.tick.regular.AcademyTick;
import net.driftingsouls.ds2.server.tick.regular.BaseTick;
import net.driftingsouls.ds2.server.tick.regular.ForschungsTick;
import net.driftingsouls.ds2.server.tick.regular.NPCOrderTick;
import net.driftingsouls.ds2.server.tick.regular.NPCScriptTick;
import net.driftingsouls.ds2.server.tick.regular.SchiffsTick;
import net.driftingsouls.ds2.server.tick.regular.WerftTick;

/**
 * Der normale Tick
 * @author Christopher Jung
 *
 */
public class RegularTick extends AbstractTickExecuter {

	@Override
	protected void executeTicks() {
		Common.stub();
		// TODO 
		try {
			publishStatus("berechne Basen");
			execTick(BaseTick.class,false);
	
			publishStatus("berechne Schiffe");
			execTick(SchiffsTick.class,false);
	
			publishStatus("berechne Werften");
			execTick(WerftTick.class, false);
	
			publishStatus("berechne Forschungen");
			execTick(ForschungsTick.class, false);
	
			publishStatus("fuehre NPC-Aktionen aus");
			execTick(NPCScriptTick.class, false);
			execTick(NPCOrderTick.class, false);
	
			publishStatus("berechne Akademien");
			execTick(AcademyTick.class, false);
	
			publishStatus("berechne GTU");
			//execTick(RTCTick.class);
	
			publishStatus("berechne Schlachten");
			//execTick(BattleTick.class);
	
			publishStatus("berechne Sonstiges");
			//execTick(RestTick.class);
		}
		catch( Exception e ) {
			System.err.println("Fehler beim Ausfuehren der Ticks: "+e);
			e.printStackTrace();
		}
	}

	@Override
	protected void prepare() {
		setName("");
		setLogPath(Configuration.getSetting("LOXPATH")+"tick/");
	}
	
	/**
	 * Hauptfunktion
	 * @param args Die Kommandozeilenargumente
	 * @throws Exception
	 */
	public static void main( String[] args ) throws Exception {
		boot(args);
		RegularTick tick = new RegularTick();
		tick.addLogTarget(TickController.STDOUT, false);
		tick.execute();
		tick.dispose();
	}
}
