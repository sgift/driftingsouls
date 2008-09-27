/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server.tools;

import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.framework.DSApplication;
import net.driftingsouls.ds2.server.framework.db.HibernateFacade;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipModules;
import net.driftingsouls.ds2.server.werften.ShipWerft;

/**
 * Kommandozeilentool zur Neuberechnung von redundanten Schiffsdaten
 * @author Christopher Jung
 *
 */
public class RecalcShips extends DSApplication {
	private Integer shipid;
	
	/**
	 * Konstruktor
	 * @param args Die Kommandozeilenargumente
	 * @throws Exception
	 */
	public RecalcShips(String[] args) throws Exception {
		super(args);
		
		if( getContext().getRequest().getParameter("ship") != null ) {
			shipid = getContext().getRequest().getParameterInt("ship");
		}
	}
	
	private void printHelp() {
		log("Recalculate Ships");
		log("Berechnet redundante Daten eines oder aller Schiffe neu");
		log("");
		log("java "+getClass().getName()+" --config $configpfad [--ship $shipid] [--help]");
		log(" * --config Der Pfad zum DS2-Konfigurationsverzeichnis");
		log(" * [optional] --ship Die ID des zu berechnenden Schiffes. Falls nicht angegeben werden alle Schiffe neuberechnet");
		log(" * [optional] --help Zeigt diese Hilfe an");
	}
	
	/**
	 * Startet die Ausfuehrung
	 *
	 */
	public void execute() {
		if( getContext().getRequest().getParameter("help") != null ) {
			printHelp();
			return;
		}
		
		log("\nBeginne:");

		org.hibernate.Session db = getDB();

		if( shipid == null ) {
			int lastsid = 0;
			while( true ) {
				// TODO: Support fuer Schiffe mit negativer ID einbauen
				final List shipList = db.createQuery("from Ship where id>:minid order by id asc")
					.setInteger("minid", lastsid)
					.setMaxResults(100)
					.list();
				
				if( shipList.isEmpty() ) {
					break;
				}
				
				for( Iterator shipIter = shipList.iterator(); shipIter.hasNext(); ) {
					Ship ship = (Ship)shipIter.next();
					
					try {
						// Es kann Luecken in der Liste der Schiffids geben (...,97,98,103,...)
						// Daher die Zusatzpruefung mit lastsid
						if( (ship.getId() % 100 == 0) || (ship.getId() % 100 < lastsid % 100) ) {
							log("sid: "+ship.getId()+"");
						}
						lastsid = ship.getId();
						ship.recalculateShipStatus();
						ship.recalculateModules();
					}
					catch( Exception e ) {
						log("WARNUNG: Konnte Schiff "+ship.getId()+" nicht neu berechnen\n"+e);
						e.printStackTrace();
					}
				}
				
				getContext().commit();
				HibernateFacade.evictAll(db, Ship.class, ShipModules.class, Offizier.class, ShipWerft.class);
			}
		}
		else {
			log("sid: "+shipid+"\n");
			Ship ship = (Ship)db.get(Ship.class, shipid);
			if( ship != null ) {
				ship.recalculateShipStatus();
				ship.recalculateModules();
			}
			else {
				log("-> Schiff existiert nicht");
			}
		}
	}

	/**
	 * Main
	 * @param args Die Argumente
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		RecalcShips rs = new RecalcShips(args);
		rs.execute();
		rs.dispose();
	}

}
