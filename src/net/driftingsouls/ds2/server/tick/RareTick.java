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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.tick.rare.RestTick;

/**
 * Der seltene Tick
 * @author Christopher Jung
 *
 */
@Configurable
public class RareTick extends AbstractTickExecuter {
	
	private Configuration config;
	
    /**
     * Injiziert die DS-Konfiguration
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
    }

	@Override
	protected void executeTicks() {
		TimeoutChecker timeout = null;
		try {
			timeout = new TimeoutChecker(20*60*1000) {
				private Thread main = Thread.currentThread();
				
				@Override
				public void timeout() {
					StackTraceElement[] stack = main.getStackTrace();
					// Falls der Stack 0 Elemente lang ist, ist der Thread nun doch fertig geworden
					if( stack.length == 0 ) {
						return;
					}
					StringBuilder stacktrace = new StringBuilder();
					for( int i=0; i < stack.length; i++ ) {
						stacktrace.append(stack[i]+"\n");
					}
					System.out.println("Timeout");
					System.out.println(stacktrace);
					Common.mailThrowable(new Exception("Rare Tick Timeout"), "RareTick Timeout", "Status: "+getStatus()+"\nStackTrace: "+stacktrace);
				}
			};
			
			timeout.start();
			
			publishStatus("berechne Sonstiges");
			execTick(RestTick.class, false);
			
			this.mailTickStatistics();
		}
		catch( Throwable e ) {
			System.err.println("Fehler beim Ausfuehren der Ticks: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RareTick Exception", null);
		}
		finally {
			if( timeout != null ) {
				timeout.interrupt();
			}
		}
	}

	@Override
	protected void prepare() {
		setName("");
		setLogPath(config.get("LOXPATH")+"raretick/");
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
		free();
	}
}
