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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.tick.TickController;

/**
 * Berechnung des Ticks fuer GTU-Versteigerungen
 * @author Christopher Jung
 *
 */
public class RTCTick extends TickController {
	// TODO: Umstellung auf GtuZwischenlager

	private static final int CARGO_TRANSPORTER = 9; //ID des Transportertyps
	private static final int CARGO_TRANSPORTER_LARGE = 50;
	
	private int ticks;
	private int maxid;
	private String currentTime;
	private User gtuuser;
	
	@Override
	protected void prepare() {
		Database db = getDatabase();
		
		this.ticks = getContext().get(ContextCommon.class).getTick();
		this.ticks++;
		
		this.maxid = db.first("SELECT max(id) max FROM ships").getInt("max");
		
		this.currentTime = Common.getIngameTime(ticks);
		
		this.gtuuser = (User)getContext().getDB().get(User.class, Faction.GTU);
		
		this.log("tick: "+this.ticks);
	}

	@Override
	protected void tick() {
		Database db = getDatabase();
		
		/*
			Einzelversteigerungen
		*/
		
		SQLQuery entry = db.query("SELECT * FROM versteigerungen WHERE tick<=",this.ticks," ORDER BY id");
		while( entry.next() ) {
			try {
				User winner = (User)getContext().getDB().get(User.class, entry.getInt("bieter"));
				long price = entry.getLong("preis");
				String type = entry.getString("type");
				int dropzone = winner.getGtuDropZone();
				 
				Cargo cargo = new Cargo();
			
				Location loc = Systems.get().system(dropzone).getDropZone();
			
				if( loc == null ) {
					dropzone = 2;
					
					loc = Systems.get().system(dropzone).getDropZone();
				}
				
				int owner = winner.getId();
				
				int gtucost = 100;
				User targetuser = null;
				
				if( entry.getInt("owner") != Faction.GTU ) {
					targetuser = (User)getContext().getDB().get(User.class, entry.getInt("owner"));
					
					gtucost = Integer.parseInt(targetuser.getUserValue("GTU_AUCTION_USER_COST"));
				}
				
				String entryname = "";
				
				if( entry.getInt("mtype") == 1 ) {
					SQLResultRow shiptype = ShipTypes.getShipType(Integer.parseInt(type), false);
					entryname = "eine "+shiptype.getString("nickname");
					int spawntype = Integer.parseInt(type);
					
					if( (owner != 0) && (owner != Faction.GTU) ) {
						this.log("Es wurde "+entryname+" (shipid: "+spawntype+") von ID "+winner+" fuer "+price+" RE ersteigert");
					
						String history = "Indienststellung am "+this.currentTime+" durch "+this.gtuuser.getName()+" (Versteigerung) f&uuml;r "+winner.getName()+" ("+winner.getId()+")\n";
					
						this.maxid++;			
						db.prepare("INSERT INTO ships " ,
								"(id,owner,name,type,x,y,system,crew,e,hull,cargo,history) " ,
								"VALUES " ,
								"( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
							.update(maxid, owner, "Verkauft", spawntype, loc.getX(), loc.getY(), loc.getSystem(), shiptype.getInt("crew"), shiptype.getInt("eps"), shiptype.getInt("hull"), cargo.save(), history);
						
						if( shiptype.getString("werft").length() > 0 ) {
							db.update("INSERT INTO werften (shipid) VALUES (",this.maxid,")");
							this.log("\tWerft '"+shiptype.getString("werft")+"' in Liste der Werften eingetragen");
						}
						
						Ships.recalculateShipStatus(this.maxid);
			
						String msg = "Sie haben "+entryname+" ersteigert.\nDas Objekt wurde ihnen bei "+loc+" &uuml;bergeben.\n\nJack Miller\nHan Ronalds";
						PM.send(getContext(), Faction.GTU, winner.getId(), entryname+" ersteigert", msg);
			
						if( entry.getInt("owner") != Faction.GTU ) {				
							msg = "Es wurde ihre "+entryname+" versteigert.\nDas Objekt wurde dem Gewinner "+winner.getName()+" f&uuml;r den Preis von "+Common.ln(price)+" RE &uuml;bergeben. Die GTU berechnet ihnen "+gtucost+"% des Gewinnes als Preis. Dies entspricht "+Common.ln(Math.ceil(price*gtucost/100d))+" RE. Ihnen bleiben somit noch "+Common.ln(price-Math.ceil(price*gtucost/100d))+" RE\n\nJack Miller\nHan Ronalds";
							PM.send(getContext(), Faction.GTU, entry.getInt("owner"), entryname+" versteigert", msg);
							
							msg = "Es wurde "+entryname+" im Auftrag von "+entry.getInt("owner")+" versteigert.\nDas Objekt wurde bei "+loc+" dem Gewinner "+winner.getId()+" f&uuml;r den Preis von "+Common.ln(price)+" RE &uuml;bergeben. Einnahme: "+Common.ln(Math.ceil(price*gtucost/100d))+" RE ("+gtucost+"%)";
							PM.send(getContext(), -1, Faction.GTU, entryname+" ersteigert", msg);
						}
						else {
							msg = "Es wurde "+entryname+" versteigert.\nDas Objekt wurde bei "+loc+" dem Gewinner "+winner.getId()+" f&uuml;r den Preis von "+Common.ln(price)+" RE &uuml;bergeben.";
							PM.send(getContext(), winner.getId(), Faction.GTU, entryname+" versteigert", msg);
						}
					}
				}
				else if( entry.getInt("mtype") == 2 ) {
					Cargo mycargo = new Cargo( Cargo.Type.STRING, type );
					ResourceEntry resource = mycargo.getResourceList().iterator().next();
							
					entryname = Cargo.getResourceName( resource.getId() );
					
					SQLResultRow posten = db.first("SELECT id,x,y,system FROM ships WHERE id>0 AND owner=",Faction.GTU," AND LOCATE('tradepost',status) AND system=",loc.getSystem()," AND x=",loc.getX()," AND y=",loc.getY());
					if( posten.isEmpty() ) {
						posten = db.first("SELECT id,x,y,system FROM ships WHERE id>0 AND owner=",Faction.GTU," AND LOCATE('tradepost',status) AND system="+loc.getSystem());
					}
					if( posten.isEmpty() ) {
						posten = db.first("SELECT id,x,y,system FROM ships WHERE id>0 AND owner=",Faction.GTU," AND LOCATE('tradepost',status)");
					}
					
					if( owner != 0 && (owner != Faction.GTU) ) {
						this.log("Es wurde "+entryname+" von ID "+winner.getId()+" fuer "+price+" RE ersteigert");
			
						loc = Location.fromResult(posten);
						
						String msg = "Sie haben "+entryname+" ersteigert.\nDas Objekt wurde ihnen bei "+loc+" auf dem Handelsposten hinterlegt.\n\nGaltracorp Unlimited";
						PM.send(getContext(), Faction.GTU, winner.getId(), entryname+" ersteigert", msg);
			
						if( entry.getInt("owner") != Faction.GTU ) {				
							msg = "Es wurde ihr "+entryname+" versteigert.\nDas Objekt wurde dem Gewinner "+winner.getName()+" f&uuml;r den Preis von "+Common.ln(price)+" RE &uuml;bergeben. Die GTU berechnet ihnen "+gtucost+"% des Gewinnes als Preis. Dies entspricht "+Common.ln(Math.ceil(price*gtucost/100d))+" RE. Ihnen bleiben somit noch "+Common.ln((price-Math.ceil(price*gtucost/100d)))+" RE\n\nJack Miller\nHan Ronalds";
							PM.send(getContext(), Faction.GTU, entry.getInt("owner"), entryname+" versteigert", msg);
							
							msg = "Es wurde "+entryname+" im Auftrag von "+entry.getInt("owner")+" versteigert.\nDas Objekt wurde bei "+loc+" dem Gewinner "+winner.getId()+" f&uuml;r den Preis von "+Common.ln(price)+" RE hinterlegt. Einnahme: "+Common.ln(Math.ceil(price*gtucost/100d))+" RE ("+gtucost+"%)";
							PM.send(getContext(), -1, Faction.GTU, entryname+" ersteigert", msg);
						}
						else {
							msg = "Es wurde "+entryname+" versteigert.\nDas Objekt wurde bei "+loc+" dem Gewinner "+winner.getName()+" f&uuml;r den Preis von "+Common.ln(price)+" RE hinterlegt.";
							PM.send(getContext(), -1, Faction.GTU, entryname+" ersteigert", msg);
						}
						
						db.update("INSERT INTO gtu_zwischenlager (posten,user1,user2,cargo1,cargo1need,cargo2,cargo2need) VALUES (",posten.getInt("id"),",",owner,",",Faction.GTU,",'",mycargo.save(),"','",mycargo.save(),"','",new Cargo().save(),"','",new Cargo().save(),"')");
					}
				}
				else {
					entryname = "Unbekannter mtype <"+entry.getInt("mtype")+">";	
				}
			
				if( owner != 0 && (owner != Faction.GTU) ) {
					if( entry.getInt("owner") != Faction.GTU ) {
						targetuser.transferMoneyFrom( Faction.GTU, price-(long)Math.ceil(price*gtucost/100d), "Gewinn Versteigerung #2"+entry.getInt("id")+" abzgl. "+gtucost+"% Auktionskosten", false, User.TRANSFER_AUTO );
					}
					
					User entryOwner = (User)getContext().getDB().get(User.class, entry.getInt("owner"));
			
					db.prepare("INSERT INTO stats_gtu " +
							"(username,userid,mtype,type,preis,owner,ownername,gtugew) " +
							"VALUES " +
							"( ?, ?, ?, ?, ?, ?, ?, ?)")
						.update(winner.getName(), winner.getId(), entry.getString("mtype"), type, price, entry.getInt("owner"), entryOwner.getName(), gtucost);
			
					db.update("DELETE FROM versteigerungen WHERE id=",entry.getInt("id"));
				} 
				else {
					this.log("Die Versteigerung um "+entryname+" (id: "+entry.getInt("id")+(entry.getInt("owner") != Faction.GTU ? " - User: "+entry.getInt("owner") : "")+") wurde um 5 Runden verlaengert. Der Preis wurde um "+(long)(price*1/10d)+" RE reduziert");
					db.update("UPDATE versteigerungen SET tick=tick+5,preis=preis-",(long)(price*1/10d)," WHERE id=",entry.getInt("id"));
				}
			}
			catch( Exception e ) {
				this.log("Versteigerung "+entry.getInt("id")+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "RTCTick Exception", "versteigerung: "+entry.getInt("id"));
			}
		}
		entry.free();
		
		/*
			GTU-Pakete
		*/
		
		SQLQuery line = db.query("SELECT * FROM versteigerungen_pakete WHERE tick<=",this.ticks," ORDER BY id");
		while( line.next() ) {
			try {
				User winner = (User)getContext().getDB().get(User.class, line.getInt("bieter"));
				long price = line.getLong("preis");
				String ships = line.getString("ships");
				Cargo cargo = new Cargo( Cargo.Type.STRING, line.getString("cargo"));
	
				int[] shiplist = Common.explodeToInt("|", ships);
			
				int dropzone = winner.getGtuDropZone();
			
				Location loc = Systems.get().system(dropzone).getDropZone();
	
				if( loc == null ) {
					dropzone = 2;
					
					loc = Systems.get().system(dropzone).getDropZone();
				}
				
				int owner = winner.getId();
			
				if( (owner != 0) && (owner != Faction.GTU) ) {
					this.log("[GTU-Paket] BEGIN");
			
					SQLResultRow shipd = ShipTypes.getShipType( CARGO_TRANSPORTER, false );
					
					int transporter = CARGO_TRANSPORTER;
					
					if( cargo.getMass() > shipd.getLong("cargo") ) {		
						shipd = ShipTypes.getShipType( CARGO_TRANSPORTER_LARGE, false );
						
						this.log("\t% Es wird der grosse Transporter verwendet");
						
						transporter = CARGO_TRANSPORTER_LARGE;
					}
				
					String history = "Indienststellung am "+this.currentTime+" durch "+this.gtuuser.getName()+" (Versteigerung) f&uuml;r "+winner.getName()+" ("+winner.getId()+")\n";
					
					this.maxid++;
					db.prepare("INSERT INTO ships " ,
							"(id,owner,name,type,x,y,system,crew,e,hull,cargo,history) " ,
							"VALUES " ,
							"( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
						.update(this.maxid, owner, "noname", transporter, loc.getX(), loc.getY(), loc.getSystem(), shipd.getInt("crew"), shipd.getInt("eps"), shipd.getInt("hull"), cargo.save(), history);
			
					this.slog("\t* Es wurden ");
					ResourceList reslist = cargo.getResourceList();
					int index = 1;
					
					for( ResourceEntry res : reslist ) {
						this.slog(res.getCount1()+" "+Cargo.getResourceName( res.getId() ));
						if( index < reslist.size() ) {
							this.slog(", ");
						}
						
						index++;
					}
					this.log(" von ID "+winner.getId()+" ersteigert");
							
					Ships.recalculateShipStatus(this.maxid);
			
					for( int i=0; i < shiplist.length; i++ ) {
						int type = shiplist[i];
						
						if( type == 0 ) continue;
						
						shipd = ShipTypes.getShipType(type, false);
			
						this.log("\t* Es wurde eine "+shipd.getString("nickname")+" von ID "+winner+" ersteigert");
						this.maxid++;
						
						cargo = new Cargo();
						
						history = "Indienststellung am "+this.currentTime+" durch "+this.gtuuser.getName()+" (Versteigerung) f&uuml;r "+winner.getName()+" ("+winner.getId()+")\n";
						
						db.prepare("INSERT INTO ships " ,
								"(id,owner,name,type,x,y,system,crew,e,hull,cargo,history) " ,
								"VALUES " ,
								"( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
							.update(this.maxid, owner, "Verkauft", type, loc.getX(), loc.getY(), loc.getSystem(), shipd.getInt("crew"), shipd.getInt("eps"), shipd.getInt("hull"), cargo.save(), history);
						
						if( shipd.getString("werft").length() > 0 ) {
							db.update("INSERT INTO werften (shipid) VALUES (",this.maxid,")");
							this.log("\tWerft '"+shipd.getString("werft")+"' in Liste der Werften eingetragen");
						}
						
						Ships.recalculateShipStatus(this.maxid);
					}
			
					this.log("[GTU-Paket] END");
			
					String msg = "Sie haben ein GTU-Paket ersteigert.\nEs steht bei "+loc+" f&uuml;r sie bereit.\nJack Miller\nHan Ronalds";
					PM.send(getContext(), Faction.GTU, winner.getId(), "GTU-Paket ersteigert", msg);
			
					msg = "Ein GTU-Paket wurde versteigert.\nEs steht bei "+loc+" f&uuml;r "+winner.getId()+" zum Preis von "+Common.ln(price)+" RE bereit.";
					PM.send(getContext(), -1, Faction.GTU, "GTU-Paket versteigert", msg);
			
					String type = line.getString("cargo")+"/"+ships;
			
					db.prepare("INSERT INTO stats_gtu (username,userid,mtype,type,preis) " +
							"VALUES " +
							"( ?, ?, ?, ?, ?)")
						.update(winner.getName(), winner.getId(), 3, type, price);
			
					db.update("DELETE FROM versteigerungen_pakete WHERE id=",line.getInt("id"));
				} 
				else {
					this.log("Die Versteigerung eines GTU-Paketes (id: "+line.getInt("id")+") wurde um 5 Runden verlaengert");
					db.update("UPDATE versteigerungen_pakete SET tick=tick+5 WHERE id=",line.getInt("id"));
				}
			}
			catch( Exception e ) {
				this.log("Paket "+line.getInt("id")+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "RTCTick Exception", "paket: "+line.getInt("id"));
			}
		}
		line.free();
	}

}
