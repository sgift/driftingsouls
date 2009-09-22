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

import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.GtuZwischenlager;
import net.driftingsouls.ds2.server.entities.PaketVersteigerung;
import net.driftingsouls.ds2.server.entities.StatGtu;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.Versteigerung;
import net.driftingsouls.ds2.server.entities.VersteigerungResource;
import net.driftingsouls.ds2.server.entities.VersteigerungSchiff;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.werften.ShipWerft;

/**
 * Berechnung des Ticks fuer GTU-Versteigerungen.
 * @author Christopher Jung
 *
 */
public class RTCTick extends TickController {
	// TODO: Umstellung Pakete auf GtuZwischenlager

	private static final int CARGO_TRANSPORTER = 9; //ID des Transportertyps
	private static final int CARGO_TRANSPORTER_LARGE = 50;
	
	private int ticks;
	private String currentTime;
	private User gtuuser;
	
	@Override
	protected void prepare() {
		this.ticks = getContext().get(ContextCommon.class).getTick();
		this.ticks++;
		
		this.currentTime = Common.getIngameTime(ticks);
		
		this.gtuuser = (User)getDB().get(User.class, Faction.GTU);
		
		this.log("tick: "+this.ticks);
	}

	@Override
	protected void tick() {
		org.hibernate.Session db = getDB();

		final User sourceUser = (User)db.get(User.class, -1);
		
		/*
			Einzelversteigerungen
		*/
		
		List<?> entries = db.createQuery("from Versteigerung where tick<= :tick order by id")
			.setInteger("tick", this.ticks)
			.list();
		for( Iterator<?> iter=entries.iterator(); iter.hasNext(); ) {
			Versteigerung entry = (Versteigerung)iter.next();
			
			try {
				if( entry.getBieter() == this.gtuuser ) {
					this.log("Die Versteigerung um "+entry.getObjectName()+" (id: "+entry.getId()+(entry.getOwner() != this.gtuuser ? " - User: "+entry.getOwner().getId() : "")+") wurde um 5 Runden verlaengert. Der Preis wurde um "+(long)(entry.getPreis()*1/10d)+" RE reduziert");
					entry.setTick(this.ticks+5);
					entry.setPreis((long)(entry.getPreis()*1/10d));
					
					continue;
				}
				
				User winner = entry.getBieter();
				long price = entry.getPreis();
				int dropzone = winner.getGtuDropZone();
				StarSystem system = (StarSystem)db.get(StarSystem.class, dropzone);				
				
				Location loc = system.getDropZone();
			
				if( loc == null ) {
					system = (StarSystem)db.get(StarSystem.class, dropzone);
					
					loc = system.getDropZone();
				}
				
				int gtucost = 100;
				User targetuser = null;
				
				if( entry.getOwner() != this.gtuuser ) {
					targetuser = entry.getOwner();
					
					gtucost = Integer.parseInt(targetuser.getUserValue("GTU_AUCTION_USER_COST"));
				}
				
				String entryname = "";
				
				if( entry instanceof VersteigerungSchiff ) {
					VersteigerungSchiff shipEntry = (VersteigerungSchiff)entry;
					
					ShipType shiptype = shipEntry.getShipType();
					entryname = "eine "+shiptype.getNickname();
						
					this.log("Es wurde "+entryname+" (shipid: "+shiptype.getTypeId()+") von ID "+winner+" fuer "+price+" RE ersteigert");
					
					String history = "Indienststellung am "+this.currentTime+" durch "+this.gtuuser.getName()+" (Versteigerung) f&uuml;r "+winner.getName()+" ("+winner.getId()+")\n";
					
					Cargo cargo = new Cargo();
					cargo = cargo.cutCargo(shiptype.getCargo());
					
					Ship ship = new Ship(winner, shiptype, loc.getSystem(), loc.getX(), loc.getY());
					ship.setName("Verkauft");
					ship.setCrew(shiptype.getCrew());
					ship.setEnergy(shiptype.getEps());
					ship.setHull(shiptype.getHull());
					ship.setCargo(cargo);
					ship.setHistory(history);
					ship.setEngine(100);
					ship.setWeapons(100);
					ship.setComm(100);
					ship.setSensors(100);
					
					db.save(ship);
					
					if( shiptype.getWerft() != 0 ) {
						ShipWerft werft = new ShipWerft(ship);
						db.persist(werft);
						
						this.log("\tWerft '"+shiptype.getWerft()+"' in Liste der Werften eingetragen");
					}
					
					ship.recalculateShipStatus();
		
					String msg = "Sie haben "+entryname+" ersteigert.\nDas Objekt wurde ihnen bei "+loc.displayCoordinates(false)+" &uuml;bergeben.\n\nJack Miller\nHan Ronalds";
					PM.send(gtuuser, winner.getId(), entryname+" ersteigert", msg);
		
					if( entry.getOwner() != this.gtuuser ) {				
						msg = "Es wurde ihre "+entryname+" versteigert.\nDas Objekt wurde dem Gewinner "+winner.getName()+" f&uuml;r den Preis von "+Common.ln(price)+" RE &uuml;bergeben. Die GTU berechnet ihnen "+gtucost+"% des Gewinnes als Preis. Dies entspricht "+Common.ln(Math.ceil(price*gtucost/100d))+" RE. Ihnen bleiben somit noch "+Common.ln(price-Math.ceil(price*gtucost/100d))+" RE\n\nJack Miller\nHan Ronalds";
						PM.send(gtuuser, entry.getOwner().getId(), entryname+" versteigert", msg);
						
						msg = "Es wurde "+entryname+" im Auftrag von "+entry.getOwner().getId()+" versteigert.\nDas Objekt wurde bei "+loc+" dem Gewinner "+winner.getId()+" f&uuml;r den Preis von "+Common.ln(price)+" RE &uuml;bergeben. Einnahme: "+Common.ln(Math.ceil(price*gtucost/100d))+" RE ("+gtucost+"%)";
						PM.send(sourceUser, Faction.GTU, entryname+" ersteigert", msg);
					}
					else {
						msg = "Es wurde "+entryname+" versteigert.\nDas Objekt wurde bei "+loc+" dem Gewinner "+winner.getId()+" f&uuml;r den Preis von "+Common.ln(price)+" RE &uuml;bergeben.";
						PM.send(winner, Faction.GTU, entryname+" versteigert", msg);
					}
				}
				else if( entry instanceof VersteigerungResource ) {
					VersteigerungResource resEntry = (VersteigerungResource)entry;
					
					Cargo mycargo = resEntry.getCargo();
					ResourceEntry resource = mycargo.getResourceList().iterator().next();
							
					entryname = Cargo.getResourceName( resource.getId() );
					
					Ship posten = (Ship)db.createQuery("from Ship where id>0 and locate('tradepost',status)!=0 and owner=? and system=? and x=? and y=?")
						.setEntity(0, this.gtuuser)
						.setInteger(1, loc.getSystem())
						.setInteger(2, loc.getX())
						.setInteger(3, loc.getY())
						.setMaxResults(1)
						.uniqueResult();
					
					if( posten == null ) {
						posten = (Ship)db.createQuery("from Ship where id>0 and locate('tradepost',status)!=0 and owner=? and system=?")
							.setEntity(0, this.gtuuser)
							.setInteger(1, loc.getSystem())
							.setMaxResults(1)
							.uniqueResult();
					}
					if( posten == null ) {
						posten = (Ship)db.createQuery("from Ship where id>0 and locate('tradepost',status)!=0 and owner=?")
							.setEntity(0, this.gtuuser)
							.setMaxResults(1)
							.uniqueResult();
					}
					
					this.log("Es wurde "+entryname+" von ID "+winner.getId()+" fuer "+price+" RE ersteigert");
		
					loc = posten.getLocation();
					
					String msg = "Sie haben "+entryname+" ersteigert.\nDas Objekt wurde ihnen bei "+loc+" auf dem Handelsposten hinterlegt.\n\nGaltracorp Unlimited";
					PM.send(gtuuser, winner.getId(), entryname+" ersteigert", msg);
		
					if( entry.getOwner() != this.gtuuser ) {				
						msg = "Es wurde ihr "+entryname+" versteigert.\nDas Objekt wurde dem Gewinner "+winner.getName()+" f&uuml;r den Preis von "+Common.ln(price)+" RE &uuml;bergeben. Die GTU berechnet ihnen "+gtucost+"% des Gewinnes als Preis. Dies entspricht "+Common.ln(Math.ceil(price*gtucost/100d))+" RE. Ihnen bleiben somit noch "+Common.ln((price-Math.ceil(price*gtucost/100d)))+" RE\n\nJack Miller\nHan Ronalds";
						PM.send(gtuuser, entry.getOwner().getId(), entryname+" versteigert", msg);
						
						msg = "Es wurde "+entryname+" im Auftrag von "+entry.getOwner().getId()+" versteigert.\nDas Objekt wurde bei "+loc+" dem Gewinner "+winner.getId()+" f&uuml;r den Preis von "+Common.ln(price)+" RE hinterlegt. Einnahme: "+Common.ln(Math.ceil(price*gtucost/100d))+" RE ("+gtucost+"%)";
						PM.send(sourceUser, Faction.GTU, entryname+" ersteigert", msg);
					}
					else {
						msg = "Es wurde "+entryname+" versteigert.\nDas Objekt wurde bei "+loc+" dem Gewinner "+winner.getName()+" f&uuml;r den Preis von "+Common.ln(price)+" RE hinterlegt.";
						PM.send(sourceUser, Faction.GTU, entryname+" ersteigert", msg);
					}
					
					GtuZwischenlager lager = new GtuZwischenlager(posten, winner, entry.getOwner());
					lager.setCargo1(mycargo);
					lager.setCargo1Need(mycargo);
					
					db.persist(lager);
				}
				else {
					entryname = "Unbekannter Typ <"+entry.getClass().getName()+">";	
				}
			
				if( entry.getOwner() != this.gtuuser ) {
					targetuser.transferMoneyFrom( Faction.GTU, price-(long)Math.ceil(price*gtucost/100d), "Gewinn Versteigerung #2"+entry.getId()+" abzgl. "+gtucost+"% Auktionskosten", false, User.TRANSFER_AUTO );
				}
				
				StatGtu stat = new StatGtu(entry, gtucost);
				db.persist(stat);
				
				db.delete(entry); 
				
				getContext().commit();
			}
			catch( RuntimeException e ) {
				this.log("Versteigerung "+entry.getId()+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "RTCTick Exception", "versteigerung: "+entry.getId());
				
				throw e;
			}
		}
		
		/*
			GTU-Pakete
		*/
		
		entries = db.createQuery("from PaketVersteigerung where tick<= :tick order by id")
			.setInteger("tick", this.ticks)
			.list();
		for( Iterator<?> iter=entries.iterator(); iter.hasNext(); ) {
			PaketVersteigerung paket = (PaketVersteigerung)iter.next();
			
			try {
				if( paket.getBieter() == this.gtuuser ) {
					this.log("Die Versteigerung eines GTU-Paketes (id: "+paket.getId()+") wurde um 5 Runden verlaengert");
					paket.setTick(this.ticks+5);
					
					continue;
				}
				
				User winner = paket.getBieter();
				ShipType[] ships = paket.getShipTypes();
				Cargo cargo = paket.getCargo();

				int dropzone = winner.getGtuDropZone();
				StarSystem system = (StarSystem)db.get(StarSystem.class, dropzone);
				
				Location loc = system.getDropZone();
	
				if( loc == null ) {
					system = (StarSystem)db.get(StarSystem.class, dropzone);
					
					loc = system.getDropZone();
				}
				
				this.log("[GTU-Paket] BEGIN");
		
				ShipType shipd = (ShipType)db.get(ShipType.class, CARGO_TRANSPORTER);
				
				if( cargo.getMass() > shipd.getCargo() ) {		
					shipd = (ShipType)db.get(ShipType.class, CARGO_TRANSPORTER_LARGE);
					
					this.log("\t% Es wird der grosse Transporter verwendet");
				}
			
				String history = "Indienststellung am "+this.currentTime+" durch "+this.gtuuser.getName()+" (Versteigerung) f&uuml;r "+winner.getName()+" ("+winner.getId()+")\n";

				cargo.addResource(Resources.NAHRUNG, shipd.getCrew()*5);
				// Kein cutCargo, da sonst ersteigerte Waren entfernt werden koennten
				
				Ship ship = new Ship(winner, shipd, loc.getSystem(), loc.getX(), loc.getY());
				ship.setName("Verkauft");
				ship.setCrew(shipd.getCrew());
				ship.setEnergy(shipd.getEps());
				ship.setHull(shipd.getHull());
				ship.setCargo(cargo);
				ship.setHistory(history);
				ship.setEngine(100);
				ship.setWeapons(100);
				ship.setComm(100);
				ship.setSensors(100);
				
				db.save(ship);
		
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
						
				ship.recalculateShipStatus();
		
				for( int i=0; i < ships.length; i++ ) {
					ShipType type = ships[i];
					
					this.log("\t* Es wurde eine "+type.getNickname()+" von ID "+winner+" ersteigert");
					
					cargo = new Cargo();
					
					history = "Indienststellung am "+this.currentTime+" durch "+this.gtuuser.getName()+" (Versteigerung) f&uuml;r "+winner.getName()+" ("+winner.getId()+")\n";
					
					ship = new Ship(winner, shipd, loc.getSystem(), loc.getX(), loc.getY());
					ship.setName("Verkauft");
					ship.setCrew(type.getCrew());
					ship.setEnergy(type.getEps());
					ship.setHull(type.getHull());
					ship.setCargo(cargo);
					ship.setHistory(history);
					ship.setEngine(100);
					ship.setWeapons(100);
					ship.setComm(100);
					ship.setSensors(100);
					
					db.save(ship);
					
					if( shipd.getWerft() != 0 ) {
						ShipWerft werft = new ShipWerft(ship);
						db.persist(werft);
						
						this.log("\tWerft '"+shipd.getWerft()+"' in Liste der Werften eingetragen");
					}
					
					ship.recalculateShipStatus();
				}
		
				this.log("[GTU-Paket] END");
		
				String msg = "Sie haben ein GTU-Paket ersteigert.\nEs steht bei "+loc+" f&uuml;r sie bereit.\nJack Miller\nHan Ronalds";
				PM.send(gtuuser, winner.getId(), "GTU-Paket ersteigert", msg);
		
				msg = "Ein GTU-Paket wurde versteigert.\nEs steht bei "+loc+" f&uuml;r "+winner.getId()+" zum Preis von "+Common.ln(paket.getPreis())+" RE bereit.";
				PM.send(sourceUser, Faction.GTU, "GTU-Paket versteigert", msg);
		
				StatGtu stat = new StatGtu(paket);
				db.persist(stat);
				
				db.delete(paket);
			}
			catch( RuntimeException e ) {
				this.log("Paket "+paket.getId()+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "RTCTick Exception", "paket: "+paket.getId());
				
				throw e;
			}
		}
	}

}
