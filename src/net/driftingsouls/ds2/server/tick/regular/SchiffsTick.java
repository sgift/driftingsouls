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
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.ships.ShipClasses;
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
	
		SQLResultRow shiptd = Ships.getShipType(shipd);
	
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
		if( (Configuration.getIntSetting("DISABLE_FOOD_CONSUMPTION") != 0) && (shiptd.getInt("crew") > 0) && (shipd.getInt("crew")>0) /*&& ($shipd['alarm'] == 1)*/ ) {
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
			(Ships.getNebula(shipd) != 6)  ) {
			
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
	
			int nebel = Ships.getNebula(shipd);
				
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
		
		String status = Ships.recalculateShipStatus(shipd.getInt("id"));
		this.slog(status);
		
		this.log(">");
	}
	
	@Override
	protected void tick() {
		Database db = getDatabase();

		String userlist = "";
		String battle = "";

		// Wurden wir von einer Schlacht aufgerufen? (Parameter '--battle $schlachtid')
		int battleid = getContext().getRequest().getParameterInt("battle");
		if( battleid != 0 ) {
			this.calledByBattle = true;
			battle = "s.battle="+battleid;
			
			List<Integer> userIdList = new ArrayList<Integer>();
			SQLQuery oid = db.query("SELECT owner FROM ships WHERE battle='",battleid,"' GROUP BY owner");
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
			battle = "s.battle=0";
		}
		
		this.usercargo = null;
		this.retries = 0;
		long usernstat = 0;
		List<Integer> nonvacUserlist = new ArrayList<Integer>();
		
		// Ueberhitzung
		if( !this.calledByBattle ) {
			db.update("UPDATE ships t1 JOIN users t2 ON t1.owner=t2.id SET t1.s=IF(t1.s>70,t1.s-70,0) WHERE t1.s>0 AND (t2.vaccount=0 OR t2.wait4vac>0) AND t1.id>0 AND t1.system!=0");
		}
		
		SQLQuery auser = db.query("SELECT id,cargo,nstat " +
				"FROM users " +
				"WHERE id!=0 AND (vaccount=0 OR wait4vac) ",userlist," ORDER BY id ASC");
		while( auser.next() ) {
			nonvacUserlist.add(auser.getInt("id"));
					
			this.usercargo = new Cargo( Cargo.Type.STRING, auser.getString("cargo") );
			usernstat = Long.parseLong(auser.getString("nstat"));
					
			this.block(auser.getInt("id"));
			
			List<Integer> idlist = new ArrayList<Integer>();
			
			long prevnahrung = this.usercargo.getResourceCount(Resources.NAHRUNG);
			
			// Schiffe berechnen
			SQLQuery shipd = db.query(
					"SELECT s.id,s.name,s.crew,s.e,s.s,s.type,s.cargo,s.docked,s.engine,s.weapons,s.sensors,s.comm,s.battle,s.autodeut,s.x,s.y,s.system,s.owner,s.status,s.alarm,s.hull,s.heat ",
					"FROM ships s JOIN ship_types st ON s.type=st.id ",
					"WHERE s.id>0 AND s.owner='",auser.getInt("id"),"' AND " +
						"((s.crew > 0) OR (st.crew = 0)) " +
						"AND system!=0 AND " +
						"((s.alarm=1) OR (s.engine+s.weapons+s.sensors+s.comm<400) OR (s.e < st.eps) " +
							"OR LOCATE('tblmodules',s.status) OR (st.hydro>0) OR (st.deutfactor>0)) " +
						"AND ",battle," ",
					"ORDER BY s.owner,s.docked,s.type ASC");
			
			this.log(auser.getInt("id")+": Es sind "+shipd.numRows()+" Schiffe zu berechnen ("+battle+")");
			
			while( shipd.next() ) {
				idlist.add(shipd.getInt("id"));
				
				// Anzahl der Wiederholungen, falls ein Commit fehlschlaegt
				this.retries = 5;
				this.tickShip( db, shipd.getRow() );
			}
			shipd.free();
			
			// Aufraeumen
			Ships.clearShipCache();
			
			// Nahrung verarbeiten
			if( Configuration.getIntSetting("DISABLE_FOOD_CONSUMPTION") != 0 ) {
				int crewcount = 0;
				String idListStr = "";
				if( idlist.size() > 0 ) {
					idListStr = " AND NOT(id IN ("+Common.implode(",",idlist)+")) ";	
				}
				crewcount = db.first("SELECT sum(s.crew) count " +
						"FROM ships s " +
						"WHERE s.id>0 AND s.system!=0 AND s.owner=",auser.getInt("id")," " +
								"AND s.alarm!=1 AND ",battle,idListStr).getInt("count");
				
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
					SQLQuery s = db.query("SELECT s.* " +
							"FROM ships s " +
							"WHERE s.id>0 AND s.system!=0 AND s.owner=",auser.getInt("id")," " +
									"AND s.crew>0 AND ",battle);
					while( s.next() ) {
						if( s.getInt("crew") < crewcount ) {
							db.update("UPDATE ships SET crew=0 WHERE id='",s.getInt("id"),"'");
							this.log(s.getInt("id")+" verhungert");
						}
						else {
							db.update("UPDATE ships SET crew=crew-'",crewcount,"' WHERE id='",s.getInt("id"),"'");
							this.log(s.getInt("id")+" "+crewcount+" Crew verhungert");
							break;
						}
					}
					s.free();
				}
			}
					
			// Nahrungspool aktualliseren
			if( !this.calledByBattle ) {
				db.update("UPDATE users SET cargo='",this.usercargo.save(),"',nstat='",(this.usercargo.getResourceCount(Resources.NAHRUNG) - usernstat),"' WHERE id='",auser.getInt("id"),"'");
			}
			else {
				db.update("UPDATE users SET cargo='",this.usercargo.save(),"' WHERE id='",auser.getInt("id"),"'");
			}
			this.unblock(auser.getInt("id"));
		}
		auser.free();
				
		db.update("UPDATE ships SET crew=0 WHERE id>0 AND crew<0");
	
		if( this.calledByBattle ) {
			return;
		}			
	
		/*
			Schiffe mit destroy-tag im status-Feld entfernen
		*/
		this.log("");
		this.log("Zerstoere Schiffe mit 'destroy'-status");
		
		SQLQuery sid = db.query("SELECT id FROM ships WHERE id>0 AND LOCATE('destroy',status)");
		while( sid.next() ) {
			this.log("\tEntferne "+sid);
			Ships.destroy( sid.getInt("id") );
		}
		sid.free();
		
		/*
		 * Schadensnebel
		 */
		this.log("");
		this.log("Behandle Schadensnebel");
		SQLQuery ship = db.query("SELECT t1.id,t1.owner,t1.hull,t1.engine,t1.weapons,t1.comm,t1.sensors FROM ships t1,nebel t2 WHERE t1.system=t2.system AND t1.x=t2.x AND t1.y=t2.y AND t2.type=6");
		while( ship.next() ) {
			if( nonvacUserlist.contains(ship.getInt("owner")) ) {
				continue;
			}
			this.log("* "+ship.getInt("id"));
			int[] sub = new int[] {ship.getInt("engine"),ship.getInt("weapons"),ship.getInt("comm"),ship.getInt("sensors")};
			
			for( int i=0; i < sub.length; i++ ) {
				sub[i] -= 10;	
			}
			
			int hull = ship.getInt("hull");
			if( hull > 1 ) {
				hull -= (int)(hull*0.05d);
				if( hull < 1 ) {
					hull = 1;
				}
			}
			
			db.update("UPDATE ships SET hull='",ship.getInt("hull"),"',engine=",sub[0],",weapons=",sub[1],",comm=",sub[2],"sensors=",sub[3]," WHERE id='",ship.getInt("id"),"'");
		}
		ship.free();
	}
}
