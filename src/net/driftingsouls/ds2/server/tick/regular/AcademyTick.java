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
package net.driftingsouls.ds2.server.tick.regular;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.tick.TickController;

/**
 * <h1>Berechnung des Ticks fuer Akademien</h1>
 * Der Ausbildungscountdown wird reduziert und, wenn dieser abgelaufen ist,
 * die Aus- bzw. Weiterbildung durchgefuehrt.
 * Abschliessend werden die Raenge der Offiziere aktuallsiert.
 * @author Christopher Jung
 *
 */
public class AcademyTick extends TickController {
	private Map<Integer,List<String>> namecache;
	private int maxid;
	private HashSet<Integer> vaclist;
	
	@Override
	protected void prepare() {
		Database db = getDatabase();
		
		// Namenscache fuellen
		namecache = new HashMap<Integer,List<String>>();
		for( Rasse race : Rassen.get() ) {
			namecache.put(race.getID(), new ArrayList<String>());
			if( race.getNameGenerator(Rasse.GENERATOR_PERSON) != null ) {
				try {
					Process p = Runtime.getRuntime().exec(race.getNameGenerator(Rasse.GENERATOR_PERSON)+" 50 \\\n");
					BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String tmp = null;
					while( (tmp = in.readLine()) != null ) {
						namecache.get(race.getID()).add(tmp);
					}
				}
				catch( Exception e ) {
					log("FEHLER: Laden der Offiziersnamen nicht moeglich");
				}
			}
		}
		
		// Max-ID berechnen
		maxid = db.first("SELECT max(id) max FROM offiziere").getInt("max");
		log("maxid: "+maxid);
		maxid++;
		
		vaclist = new HashSet<Integer>();
		vaclist.add(0);
	}
	
	@Override
	protected void tick() {
		Database db = getDatabase();
		Random rnd = new Random();
		
		SQLQuery acc = db.query("SELECT * FROM academy WHERE remain!=0 ORDER BY id");
		while( acc.next() ) {
			int id = acc.getInt("id");
			SQLResultRow base = db.first("SELECT t1.name,t1.owner,t2.vaccount,t2.wait4vac FROM bases t1,users t2 WHERE t1.id=",acc.getInt("col")," AND t1.owner=t2.id");
		
			if( (base.getInt("vaccount") == 0) && (base.getInt("wait4vac") != 0) ) {
				log("Ueberspringe Akademie $id [VAC]");
				vaclist.add(id);
				continue;
			}
			
			if( acc.getInt("remain") != 1 ) {
				continue;
			}
			
			log("Akademie "+id+":");
			
			// Einen neuen Offizier ausbilden?
			if( acc.getInt("train") != 0 ) {
				log("\tAusbildung abgeschlossen");
				String offiname = "Offizier "+maxid;
				
				User auser = getContext().createUserObject(base.getInt("owner"));
				if( namecache.get(auser.getRace()).size() > 0 ) {
					//offiname = $this->namecache[$auser->getRace()][rand(0,count($this->namecache[$auser->getRace()]))];
				}
				int spec = 0;
				String query = "INSERT INTO offiziere (userid,name,ing,waf,nav,sec,com,dest,spec) " +
						"VALUES " +
						"("+base.getInt("owner")+",'"+offiname+"',";
				if( Offiziere.LIST.containsKey(acc.getInt("train")) ) {
					SQLResultRow offi = Offiziere.LIST.get(acc.getInt("train"));
					query += offi.getInt("ing")+","+offi.getInt("waf")+","+offi.getInt("nav")+","+offi.getInt("sec")+","+offi.getInt("com");
					
					spec = rnd.nextInt(((int[])offi.get("specials")).length);
					spec = ((int[])offi.get("specials"))[spec];
				}
				else {
					log("FEHLER: Unbekannter Offizierstyp "+acc.getInt("train"));
					query += "25,20,10,5,5";
					
					spec = rnd.nextInt(6)+1;
				}
				
				db.update( query+",'b "+acc.getInt("col")+"',"+spec+")");
				maxid++;
			}
			// Einen bestehenden Offizier weiterbilden?
			else if( acc.getString("upgrade").length() > 0 ) {
				log("\tWeiterbildung abgeschlossen");
				String[] dat = StringUtils.split(acc.getString("upgrade"), ' ');
				StringBuilder query = new StringBuilder(50);
				query.append("UPDATE offiziere SET ");
				if( dat[1].equals("1") ) {
					query.append("ing=ing");
				}
				if( dat[1].equals("2") ) {
					query.append("waf=waf");
				}
				if( dat[1].equals("3") ) {
					query.append("nav=nav");
				}
				if( dat[1].equals("4") ) {
					query.append("sec=sec");
				}
				if( dat[1].equals("5") ) {
					query.append("com=com");
				}
				query.append("+2, dest='b ");
				query.append(acc.getInt("col"));
				query.append("' WHERE id=");
				query.append(dat[0]);
				db.update(query.toString());
			}
			db.update("UPDATE academy SET remain=0,train=0,`upgrade`='' WHERE id=",id);
			
			// Nachricht versenden
			String msg = "Die Flottenakademie auf dem Asteroiden "+base.getString("name")+" hat die Ausbildung abgeschlossen";
			PM.send(getContext(),-1, base.getInt("owner"), "Ausbildung abgeschlossen", msg);
		}
		acc.free();
		
		db.update("UPDATE academy SET remain=remain-1 WHERE remain!=0 AND NOT(id IN (",Common.implode(",",vaclist.toArray()),"))");
		log("Offiziere in der Aus/Weiterbildung: "+db.affectedRows());
		
		//
		// Raenge der Offiziere neu berechnen
		//
		int count = 0;
		for( int i = Offiziere.MAX_RANG; i > 0; i-- ) {
			db.update("UPDATE offiziere SET rang=",i," WHERE rang<",i," AND (ing+waf+nav+sec+com)/125>=",i);
			count += db.affectedRows();
		}
		
		log(count+" Offizier(e) befoerdert");
	}
}
