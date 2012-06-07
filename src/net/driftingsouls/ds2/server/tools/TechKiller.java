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

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.GtuZwischenlager;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.DSApplication;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ship;

/**
 * Kommandozeilentool zur Rueckerstattung der in die angegebene
 * Forschungen investierten Resourcen.
 * Achtung: Es werden keine Transaktionen verwendet!
 * @author Christopher Jung
 *
 */
public class TechKiller extends DSApplication {
	private int techid;
	
	/**
	 * Konstruktor.
	 * @param args Die Kommandozeilenargumente
	 * @throws Exception
	 */
	public TechKiller(String[] args) throws Exception {
		super(args);
		
		techid = getContext().getRequest().getParameterInt("techid");
	}
	
	private void printHelp() {
		log("Tech Killer");
		log("Erstattet fuer eine Forschung die investierten Resourcen zurueck. Die Forschung wird nicht geloescht.");
		log("Achtung: Es werden keine Transaktionen verwendet! Bitte vorher DS sperren!");
		log("");
		log("java "+getClass().getName()+" --config $configpfad --techid $techid [--help]");
		log(" * --config Der Pfad zum DS2-Konfigurationsverzeichnis");
		log(" * --techid Die ID der zu entfernenden Forschung (DB-Eintrag muss vorhanden sein)");
		log(" * [optional] --help Zeigt diese Hilfe an");
	}
	
	/**
	 * Startet die Ausfuehrung.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void execute() throws IOException, InterruptedException {
		log("WARNUNG: Dieses Tool funktioniert evt. nicht mehr!");
		
		if( getContext().getRequest().getParameter("help") != null ) {
			printHelp();
			return;
		}
		
		if( techid == 0 ) {
			log("Sie muessen --techid angeben");
			return;
		}
		
		addLogTarget("_ds2_techss_"+techid+".log", false);

		Database database = getDatabase();
		org.hibernate.Session db = getDB();
		
		log("DS2 Tech Killer");
		log("TechID: "+techid+"");
		SQLResultRow techdata = database.first("SELECT * FROM forschungen WHERE id="+techid);
		log("["+techdata.getString("name")+"]");
		
		Cargo techcargo = new Cargo( Cargo.Type.AUTO, techdata.getString("costs") );

		Cargo emptycargo = new Cargo();
		
		final User nullUser = (User)db.get(User.class, 0); 

		log("* Processing Users:");
		SQLQuery query = database.query("SELECT u.* " +
				"FROM user_f f JOIN users u ON f.id=u.id " +
				"WHERE f.r"+techid+"!=0");
		while( query.next() ) {
			User user = (User)db.get(User.class, query.getInt("id")); 
			StarSystem system = (StarSystem)db.get(StarSystem.class, user.getGtuDropZone());
			log("* "+user.getId()+"\n");
		
			Location dz = system.getDropZone();
			
			// Zuerst exakter Match von System, x und y
			Ship gtuposten = (Ship)db.createQuery("from Ship where owner= :owner and locate('tradepost',status) and system= :system and x= :x and y= :y")
				.setInteger("owner", Faction.GTU)
				.setInteger("system", user.getGtuDropZone())
				.setInteger("x", dz.getX())
				.setInteger("y", dz.getY())
				.setMaxResults(1)
				.uniqueResult();
			
			if( gtuposten == null ) {
				// Falls nichts gefunden, dann Match des Systems
				gtuposten = (Ship)db.createQuery("from Ship where owner= :owner and locate('tradepost',status) and system= :system")
					.setInteger("owner", Faction.GTU)
					.setInteger("system", user.getGtuDropZone())
					.setMaxResults(1)
					.uniqueResult();
				
				if( gtuposten == null ) {
					// Falls trotzdem nichts gefunden: Erst bester GTU-Posten
					gtuposten = (Ship)db.createQuery("from Ship where owner= :owner and locate('tradepost',status)")
						.setInteger("owner", Faction.GTU)
						.setMaxResults(1)
						.uniqueResult();
				}
			}
			
			log("\tgtuposten: "+gtuposten.getId()+"\n");
			
			GtuZwischenlager lager = new GtuZwischenlager(gtuposten, user, nullUser);
			lager.setCargo1(techcargo);
			lager.setCargo1Need(techcargo);
			lager.setCargo2(emptycargo);
			lager.setCargo2Need(emptycargo);
			
			db.persist(lager);
		}
		query.free();
	}

	/**
	 * Main.
	 * @param args Die Argumente
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		TechKiller rs = new TechKiller(args);
		rs.execute();
		rs.dispose();
	}

}
