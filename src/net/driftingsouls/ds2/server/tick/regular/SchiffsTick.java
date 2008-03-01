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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.tick.TickController;

/**
 * Berechnung des Ticks fuer Schiffe
 * @author Christopher Jung
 *
 */
public class SchiffsTick extends TickController {
	private Map<String,ResourceID> esources;
	private Cargo usercargo;
	private boolean calledByBattle=false;
	private int retries=0;
	
	@Override
	protected void prepare() {
		esources = new HashMap<String,ResourceID>();
		esources.put("a", Resources.ANTIMATERIE);
		esources.put("d", Resources.DEUTERIUM);
		esources.put("u", Resources.URAN);
	}
	
	private void tickShip( Database db, SQLResultRow shipd ) {
		long usercargostart = this.usercargo.getResourceCount(Resources.NAHRUNG);
		SQLResultRow oldshipd = (SQLResultRow)shipd.clone();
		
		this.log(shipd.getString("name")+" ("+shipd.getInt("id")+"):");
	
		SQLResultRow shiptd = ShipTypes.getShipType(shipd);
	
		Cargo shipc = new Cargo( Cargo.Type.STRING, shipd.getString("cargo") );
		
		this.log("\tAlt: crew "+shipd.getInt("crew")+" e "+shipd.getInt("e"));
	
		// Nahrungsproduktion
		if( shiptd.getInt("hydro") > 0 ) {	
			this.usercargo.addResource( Resources.NAHRUNG, shiptd.getInt("hydro") );
		}
		
		if( shipd.getInt("alarm") == 1 ) {
			this.log("\tAlarm: rot");
		}
	
		// Nahrungsverbrauch berechnen
		if( (Configuration.getIntSetting("DISABLE_FOOD_CONSUMPTION") == 0) && 
			(shiptd.getInt("crew") > 0) && (shipd.getInt("crew")>0) /*&& ($shipd['alarm'] == 1)*/ ) {
			this.slog("\tCrew: ");
			int crew = shipd.getInt("crew");
			
			double scalefactor = 1;
			if( (shipd.getInt("alarm") == 1) && (shiptd.getInt("class") != ShipClasses.GESCHUETZ.ordinal()) ) {
				scalefactor = 0.9;	
			}
	
			if( crew > this.usercargo.getResourceCount(Resources.NAHRUNG)*scalefactor ) {
				crew = (int)(this.usercargo.getResourceCount(Resources.NAHRUNG)*scalefactor);
				this.slog("Crew verhungert - ");
				if( crew >= 0 ) {
					shipd.put("crew", crew);
				}
				else {
					shipd.put("crew", 0);
				}
			}
			int tmp = (int)Math.ceil(crew/scalefactor);
			
			this.usercargo.substractResource( Resources.NAHRUNG, tmp );
			this.log(tmp+" verbraucht");
		}
	
		//Berechnung der Energie
		this.log("\tEnergie:");
		int e = shipd.getInt("e");
		
		if( (shipd.getInt("alarm") == 1) && (shiptd.getInt("class") != ShipClasses.GESCHUETZ.ordinal()) ) {
			e -= (int)Math.ceil(shiptd.getInt("rm") * 0.5d);
			if( e < 0 ) {
				e = 0;
			}	
		}	
		
		if( e < shiptd.getInt("eps") ) {
			if( shiptd.getInt("crew") > 0 ) {
				shiptd.put("rm", (int)(shiptd.getInt("rm") * shipd.getInt("crew") / (double)shiptd.getInt("crew")));
			}
			int maxenergie = shiptd.getInt("rm");
		
			for( String resshort : this.esources.keySet() ) {
				ResourceID resid = this.esources.get(resshort);
				String reactres = "r"+resshort;
				
				if( (e < shiptd.getInt("eps")) && (shiptd.getInt(reactres) > 0) ) {
					this.slog("\t * "+Cargo.getResourceName(resid)+": ");
					if( shipc.getResourceCount( resid ) > 0 ) {
						this.log(shipc.getResourceCount( resid )+" vorhanden");
		
						int max = (int)Math.round(shiptd.getInt("rm") / (double)shiptd.getInt(reactres));
						if( max > Math.round(maxenergie/(double)shiptd.getInt(reactres)) ) {
							max = (int)Math.round(maxenergie/(double)shiptd.getInt(reactres));
						}
		
						int need = shiptd.getInt("eps") - e;
						this.log("\t   maximal: "+max+" Energie bei "+shiptd.getInt(reactres)+" Reaktorwert : "+need+" Energie frei im eps");
		
						int counter = 0;
						for( int k=0; k < max; k++ ) {
							if( (need > 0) && (shipc.getResourceCount( resid ) > 0) ) {
								counter++;
								if( maxenergie < shiptd.getInt(reactres) ) {
									e += maxenergie;
									maxenergie = 0;
								} 
								else {
									e += shiptd.getInt(reactres);
									maxenergie -= shiptd.getInt(reactres);
								}
								shipc.substractResource( resid, 1 );
								need -= shiptd.getInt(reactres);
								if( e > shiptd.getInt("eps") ) {
									e = shiptd.getInt("eps");
								}
							}
						}
						this.log("\t   verbrenne "+counter+" "+Cargo.getResourceName(resid));
					} 
					else {
						this.log(" kein "+Cargo.getResourceName(resid)+" vorhanden");
					}
				}
			}
		}
	
		int[] sub = new int[] {shipd.getInt("engine"),shipd.getInt("weapons"),shipd.getInt("comm"),shipd.getInt("sensors")};
		
		// Schiff bei Bedarf und falls moeglich reparieren
		if( (shipd.getInt("battle") == 0) && (shipd.getString("status").indexOf("lowmoney") == -1) &&
			( (shipd.getInt("engine") < 100) || (shipd.getInt("weapons") < 100) || (shipd.getInt("comm") < 100) || (shipd.getInt("sensors") < 100) ) &&
			(Ships.getNebula(Location.fromResult(shipd)) != 6)  ) {
			
			Offizier offizier = Offizier.getOffizierByDest('s', shipd.getInt("id"));
	
			for( int a=0; a<=3; a++ ) {
				int old = sub[a];
				if( shipd.getInt("crew") == shiptd.getInt("crew") ) {
					sub[a] += 20;
				}
				else if( shipd.getInt("crew") > shiptd.getInt("crew")/2 ) {
					sub[a] += 15;
				}
				else if( shipd.getInt("crew") == shiptd.getInt("crew")/2 ) {
					sub[a] += 10;
				}
				else if( shipd.getInt("crew") < shiptd.getInt("crew")/2 ) {
					sub[a] += 5;
				}
	
				if( offizier != null ) {
					sub[a] += (int)(offizier.getAbility(Offizier.Ability.ING) / 3d );
				
					if( offizier.getAbility(Offizier.Ability.ING) > 20 ) {
						if( sub[a] > 40 + (int)(offizier.getAbility(Offizier.Ability.ING)/4d) ) {
							sub[a] = 40 + (int)(offizier.getAbility(Offizier.Ability.ING)/4d);
						}
					}
				} 
				else if( sub[a] > 40 ) {
					sub[a] = 40;
				}
				if( old > sub[a] ) {
					sub[a] = old;
				}
				if( sub[a] > 100 ) {
					sub[a] = 100;
				}
			}
		}
		
		// Evt. Deuterium sammeln
		if( (shipd.getInt("autodeut") != 0) && (shiptd.getInt("deutfactor") != 0) && (shipd.getInt("crew") >= shiptd.getInt("crew")/2) && (e > 0) && (shipc.getMass() < shiptd.getLong("cargo")) ) {
			this.slog("\tS. Deut: ");
	
			int nebel = Ships.getNebula(Location.fromResult(shipd));
				
			if( (nebel >= 0) && (nebel <= 2) ) {
				int tmpe = e;
		
				int deutfactor = shiptd.getInt("deutfactor");
				if( nebel == 1 ) {
					deutfactor--;
				}
				else if( nebel == 2 ) {
					deutfactor++;
				}
	
				if( Cargo.getResourceMass( Resources.DEUTERIUM, tmpe * deutfactor ) > (shiptd.getLong("cargo") - shipc.getMass()) ) {
					tmpe = (int)( (shiptd.getLong("cargo")-shipc.getMass())/(deutfactor*Cargo.getResourceMass( Resources.DEUTERIUM, 1 )) );
					this.slog("[maxcargo]");
				}
				int saugdeut = tmpe * deutfactor;
					
				shipc.addResource( Resources.DEUTERIUM, saugdeut );
				e -= tmpe;
				this.log(tmpe+" Deuterium");
			}
			else {
				this.log("kpn");
			}
		}
		
		// Ueberpruefen, ob es Veraenderungen im Datensatz gab und fals ja in die DB schreiben
		if( (sub[0] != oldshipd.getInt("engine")) || (sub[1] != oldshipd.getInt("weapons")) ||
			(sub[2] != oldshipd.getInt("comm")) || (sub[3] != oldshipd.getInt("sensors")) ||
			(shipd.getInt("crew") != oldshipd.getInt("crew")) || (e != oldshipd.getInt("e")) ||
			(oldshipd.getString("heat").length() > 0) ||
			(shipd.getInt("hull") != oldshipd.getInt("hull")) || !shipc.save().equals(oldshipd.getString("cargo")) ) {
				
			// Neue Werte in die DB eintragen
			db.tBegin(true);
			db.tUpdate(1, "UPDATE ships " ,
						"SET engine=",sub[0],"," ,
							"weapons=",sub[1],"," ,
							"comm=",sub[2],"," ,
							"sensors=",sub[3],"," ,
							"crew=",shipd.getInt("crew"),"," ,
							"e=",e,"," ,
							"heat=''," ,
							"hull='",shipd.getInt("hull"),"'," ,
							"cargo='",shipc.save(),"' " ,
						"WHERE id='",shipd.getInt("id"),"' AND " ,
							"engine='",oldshipd.getInt("engine"),"' AND " ,
							"weapons='",oldshipd.getInt("weapons"),"' AND " ,
							"comm='",oldshipd.getInt("comm"),"' AND " ,
							"sensors='",oldshipd.getInt("sensors"),"' AND " ,
							"crew='",oldshipd.getInt("crew"),"' AND " ,
							"e='",oldshipd.getInt("e"),"' AND " ,
							"heat='",oldshipd.getString("heat"),"' AND " ,
							"hull='",oldshipd.getInt("hull"),"' AND " ,
							"cargo='",oldshipd.getString("cargo"),"'");
			
			String status = Ships.recalculateShipStatus(shipd.getInt("id"));
			this.slog(status);
			
			// Schauen wir mal ob das Commiten auch geht
			if( !db.tCommit() ) {
				this.log("\t++++++++++++++ COMMIT ERROR - RETRYING ++++++++++++++");
				this.usercargo.setResource(Resources.NAHRUNG, usercargostart);
				this.retries--;
				if( this.retries > 0 ) {
					this.log("Lade neuen Datensatz:");
					shipd = db.first("SELECT * FROM ships WHERE id=",oldshipd.getInt("id"));
			 
					tickShip( db, shipd );
					return;	
				}
				this.log("\t+++++++++++++++ GEBE AUF +++++++++++++++");	
			}
		}	
		else {
			this.log("\tKeine Aenderungen");	
		}
		this.slog("\tNeu: crew "+shipd.getInt("crew")+" e "+e+" status: <");
		
		this.log(">");
	}
	
	@Override
	protected void tick() {
		Database database = getDatabase();
		org.hibernate.Session db = getDB();

		String userlist = "";
		String battle = "";

		// Wurden wir von einer Schlacht aufgerufen? (Parameter '--battle $schlachtid')
		int battleid = getContext().getRequest().getParameterInt("battle");
		if( battleid != 0 ) {
			this.calledByBattle = true;
			battle = "s.battle="+battleid;
			
			List<Integer> userIdList = new ArrayList<Integer>();
			SQLQuery oid = database.query("SELECT owner FROM ships WHERE battle='",battleid,"' GROUP BY owner");
			while( oid.next() ) {
				userIdList.add(oid.getInt("owner"));
			}
			oid.free();
			
			if( userIdList.size() > 0 ) {
				userlist = " AND id IN ("+Common.implode(",",userIdList)+") ";
			}
		}
		else {
			this.calledByBattle = false;
			battle = "s.battle is null";
		}
		
		this.usercargo = null;
		this.retries = 0;
		long usernstat = 0;
		List<Integer> nonvacUserlist = new ArrayList<Integer>();
		
		// Ueberhitzung
		if( !this.calledByBattle ) {
			database.update("UPDATE ships s JOIN users u ON s.owner=u.id " +
					"SET s.s=IF(s.s>70,s.s-70,0) " +
					"WHERE s.s>0 AND (u.vaccount=0 OR u.wait4vac>0) AND s.id>0 AND s.system!=0 AND s.battle is null");
			
			getContext().commit();
		}
		
		Iterator useriter = db.createQuery("select id from User " +
				"where id!=0 and (vaccount=0 or wait4vac>0) "+userlist+" order by id asc")
				.list().iterator();
		for(; useriter.hasNext(); ) {
			int auserId = (Integer)useriter.next();
			nonvacUserlist.add(auserId);
			
			User auser = (User)db.get(User.class, auserId);
			
			try {
				this.tickUser(database, battle, auser);
				
				if( !calledByBattle ) {
					getContext().commit();
					getDB().clear();
				}
			}
			catch( RuntimeException e ) {
				if( calledByBattle ) {
					throw e;
				}
				
				getContext().rollback();
				db.clear();

				this.log("User "+auser.getId()+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "ShipTick  Exception", "User: "+auser.getId()+"\nBattle: "+battle);

				auser = (User)db.get(User.class, auser.getId());
			}
			finally {
				this.unblock(auser.getId());
			}
		}
				
		database.update("UPDATE ships SET crew=0 WHERE id>0 AND crew<0");
	
		if( !this.calledByBattle ) {
			getContext().commit();
			getDB().clear();
		}
		
		if( this.calledByBattle ) {
			return;
		}			
	
		/*
			Schiffe mit destroy-tag im status-Feld entfernen
		 */
		this.log("");
		this.log("Zerstoere Schiffe mit 'destroy'-status");
	
		List ships = db.createQuery("from Ship where id>0 and locate('destroy',status)!=0").list();
		for( Iterator iter=ships.iterator(); iter.hasNext(); ) {
			Ship aship = (Ship)iter.next();
	
			this.log("\tEntferne "+aship.getId());
			aship.destroy();
		}
	
		/*
		 * Schadensnebel
		 */
		this.log("");
		this.log("Behandle Schadensnebel");
		ships = db.createQuery("select s from Ship as s, Nebel as n " +
		"where s.system=n.loc.system and s.x=n.loc.x and s.y=n.loc.y and n.type=6 and (s.owner.vaccount=0 or s.owner.wait4vac>0)").list();
		for( Iterator iter=ships.iterator(); iter.hasNext(); ) {
			Ship ship = (Ship)iter.next();
	
			this.log("* "+ship.getId());
			int[] sub = new int[] {ship.getEngine(),ship.getWeapons(),ship.getComm(),ship.getSensors()};
	
			for( int i=0; i < sub.length; i++ ) {
				sub[i] -= 10;
				if( sub[i] < 0 ) {
					sub[i] = 0;
				}
			}
	
			int hull = ship.getHull();
			if( hull > 1 ) {
				hull -= (int)(hull*0.05d);
				if( hull < 1 ) {
					hull = 1;
				}
			}
	
			ship.setEngine(sub[0]);
			ship.setWeapons(sub[1]);
			ship.setComm(sub[2]);
			ship.setSensors(sub[3]);
			ship.setHull(hull);
		}
		
		getContext().commit();
		db.clear();
	}

	private void tickUser(Database database, String battle, User auser) {
		long usernstat;
		this.usercargo = new Cargo( Cargo.Type.STRING, auser.getCargo() );
		usernstat = Long.parseLong(auser.getNahrungsStat());
				
		this.block(auser.getId());
		
		List<Integer> idlist = new ArrayList<Integer>();
		
		long prevnahrung = this.usercargo.getResourceCount(Resources.NAHRUNG);
		
		List<SQLResultRow> shipList = new ArrayList<SQLResultRow>();
		
		// Schiffe berechnen
		SQLQuery shipQuery = database.query(
				"SELECT s.id,s.name,s.crew,s.e,s.s,s.type,s.cargo,s.docked,s.engine,s.weapons,s.sensors,s.comm,s.battle,s.autodeut,s.x,s.y,s.system,s.owner,s.status,s.alarm,s.hull,s.heat ",
				"FROM ships s JOIN ship_types st ON s.type=st.id ",
				"WHERE s.id>0 AND s.owner='",auser.getId(),"' AND " +
					"((s.crew > 0) OR (st.crew = 0)) " +
					"AND system!=0 AND " +
					"((s.alarm=1) OR (s.engine+s.weapons+s.sensors+s.comm<400) OR (s.e < st.eps) " +
						"OR (s.modules is not null) OR (st.hydro>0) OR (st.deutfactor>0)) " +
					"AND ",battle," ",
				"ORDER BY s.owner,s.docked,s.type ASC");
		
		while( shipQuery.next() ) {
			shipList.add(shipQuery.getRow());
		}
		shipQuery.free();
		
		this.log(auser.getId()+": Es sind "+shipList.size()+" Schiffe zu berechnen ("+battle+")");
		
		for( int i=0; i < shipList.size(); i++ ) {
			SQLResultRow shipd = shipList.get(i);
			idlist.add(shipd.getInt("id"));
			
			// Anzahl der Wiederholungen, falls ein Commit fehlschlaegt
			this.retries = 5;
			try {
				this.tickShip( database, shipd );
				if( !this.calledByBattle ) {
					getContext().commit();
				}
			}
			catch( Exception e ) {
				this.log("ship "+shipd.getInt("id")+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "SchiffsTick Exception", "ship: "+shipd.getInt("id"));
			}
		}
		
		// Aufraeumen
		Ships.clearShipCache();
		
		// Nahrung verarbeiten
		if( Configuration.getIntSetting("DISABLE_FOOD_CONSUMPTION") == 0 ) {
			int crewcount = 0;
			String idListStr = "";
			if( idlist.size() > 0 ) {
				idListStr = " AND id NOT IN ("+Common.implode(",",idlist)+") ";	
			}
			crewcount = database.first("SELECT sum(s.crew) count " +
					"FROM ships s " +
					"WHERE s.id>0 AND s.system!=0 AND s.owner=",auser.getId()," " +
							"AND ",battle,idListStr).getInt("count");
			
			this.log("# base+: "+(prevnahrung-usernstat));
			this.log("# Verbrauche "+crewcount+" Nahrung");
			this.log("# "+(prevnahrung-this.usercargo.getResourceCount(Resources.NAHRUNG))+" bereits verbucht");
			if( crewcount <= this.usercargo.getResourceCount(Resources.NAHRUNG) ) {
				this.usercargo.substractResource(Resources.NAHRUNG, crewcount);
			}
			else {
				// Nicht genug Nahrung fuer alle -> verhungern
				crewcount -= this.usercargo.getResourceCount(Resources.NAHRUNG);
				this.usercargo.setResource(Resources.NAHRUNG, 0);
				SQLQuery s = database.query("SELECT s.* " +
						"FROM ships s " +
						"WHERE s.id>0 AND s.system!=0 AND s.owner=",auser.getId()," " +
								"AND s.crew>0 AND ",battle);
				while( s.next() ) {
					if( s.getInt("crew") < crewcount ) {
						database.update("UPDATE ships SET crew=0 WHERE id='",s.getInt("id"),"'");
						Ships.recalculateShipStatus(s.getInt("id"));
						this.log(s.getInt("id")+" verhungert");
					}
					else {
						database.update("UPDATE ships SET crew=crew-'",crewcount,"' WHERE id='",s.getInt("id"),"'");
						Ships.recalculateShipStatus(s.getInt("id"));
						this.log(s.getInt("id")+" "+crewcount+" Crew verhungert");
						break;
					}
				}
				s.free();
			}
		}
		
		if( !this.calledByBattle ) {
			getContext().commit();
		}
				
		// Nahrungspool aktualliseren
		if( !this.calledByBattle ) {
			auser.setCargo(this.usercargo.save());
			auser.setNahrungsStat(Long.toString(this.usercargo.getResourceCount(Resources.NAHRUNG) - usernstat));
		}
		else {
			auser.setCargo(this.usercargo.save());
		}
	}
}
