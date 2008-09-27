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

import java.io.IOException;

import net.driftingsouls.ds2.server.framework.DSApplication;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Kommandozeilentool zur Aenderung einer Schiffs-ID eines Schiffes
 * @author Christopher Jung
 *
 */
public class SRelocate extends DSApplication {
	private int from;
	private int to;
	
	/**
	 * Konstruktor
	 * @param args Die Kommandozeilenargumente
	 * @throws Exception
	 */
	public SRelocate(String[] args) throws Exception {
		super(args);
		
		from = getContext().getRequest().getParameterInt("from");
		to = getContext().getRequest().getParameterInt("to");
	}
	
	private void printHelp() {
		log("Ship relocator");
		log("Aendert die Schiffs-ID eines Schiffes");
		log("");
		log("java "+getClass().getName()+" --config $configpfad --from $id [--to $id] [--help]");
		log(" * --config Der Pfad zum DS2-Konfigurationsverzeichnis");
		log(" * --from Die ID des Schiffes, dessen ID geaendert werden soll");
		log(" * [optional] --to Die neue ID des Schiffes. Falls nicht angegeben wird eine ID im Bereich ueber 10.000 automatisch ausgewaehlt");
		log(" * [optional] --help Zeigt diese Hilfe an");
	}
		
	/**
	 * Startet die Ausfuehrung
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void execute() throws IOException, InterruptedException {
		log("WARNUNG: Dieses Tool funktioniert evt. nicht mehr!");
		
		if( getContext().getRequest().getParameter("help") != null ) {
			printHelp();
			return;
		}
		
		if( from == 0 ) {
			log("Sie muessen eine gueltige Schiffs-ID angeben");
			return;
		}
		log("from: "+this.from);
		if( to == 0 ) {
			log("to: auto");
		}
		else {
			log("to: "+this.to);
		}
		
		log("\nBeginne:");

		Database db = getDatabase();


		if( this.to == 0 ) {
			SQLResultRow newid = db.first("SELECT newIntelliShipID(10000) as intid");
			this.to = newid.getInt("intid");
			log("to-id: "+to+"\n");
		}

		db.update("UPDATE ships SET id="+this.to+" WHERE id="+this.from);
		db.update("UPDATE ships_modules SET id="+this.to+" WHERE id="+this.from);
		db.update("UPDATE ships SET docked='l "+this.to+"' WHERE docked='l "+this.from+"'");
		db.update("UPDATE offiziere SET dest='s "+this.to+"' WHERE dest='s "+this.from+"'");
		db.update("UPDATE werften SET shipid="+this.to+" WHERE shipid="+this.from);
	}

	/**
	 * Main
	 * @param args Die Argumente
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		SRelocate rs = new SRelocate(args);
		rs.execute();
		rs.dispose();
	}

}
