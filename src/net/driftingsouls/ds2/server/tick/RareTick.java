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
import net.driftingsouls.ds2.server.tick.rare.RestTick;

/**
 * Der seltene Tick
 * @author Christopher Jung
 *
 */
public class RareTick extends AbstractTickExecuter {

	@Override
	protected void executeTicks() {
		try {
			publishStatus("berechne Sonstiges");
			execTick(RestTick.class, false);
		}
		catch( Throwable e ) {
			System.err.println("Fehler beim Ausfuehren der Ticks: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RareTick Exception", null);
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
		RareTick tick = new RareTick();
		tick.addLogTarget(TickController.STDOUT, false);
		tick.execute();
		tick.dispose();
	}
}
