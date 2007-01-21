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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.bases.AutoGTUAction;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.tick.TickController;

/**
 * <h1>Berechnung des Ticks fuer Basen</h1>
 * @author Christopher Jung
 *
 */
public class BaseTick extends TickController {
	private Cargo curse;
	private StringBuilder pmcache;
	private int lastowner;
	private Map<Integer,Cargo> gtustatslist; // GTU-Cargostats pro System
	private Cargo usercargo;
	private int tick;
	private int retries;
	private PreparedQuery updateBaseQuery = null;
	
	@Override
	protected void prepare() {
		Database db = getDatabase();
		
		// Aktuelle Warenkurse fuer die Kommandozentrale laden
		String curse = db.first("SELECT kurse FROM gtu_warenkurse WHERE place='asti'").getString("kurse");
		this.curse = new Cargo( Cargo.Type.STRING, curse );
		
		this.pmcache = new StringBuilder();
		this.lastowner = 0;
		this.usercargo = null;
		this.gtustatslist = new HashMap<Integer,Cargo>();
		this.tick = getContext().get(ContextCommon.class).getTick();
		this.retries = 0;
	}
	
	private void tickBase( Base base ) {
		Database db = getContext().getDatabase();
		
		long usercargostart = this.usercargo.getResourceCount(Resources.NAHRUNG);
		Map<Integer,Cargo> oldgtustatslist = new HashMap<Integer,Cargo>();
		for( Integer sysid : this.gtustatslist.keySet() ) {
			oldgtustatslist.put(sysid, (Cargo)gtustatslist.get(sysid).clone() );
		}
		StringBuilder oldpmcache = new StringBuilder(this.pmcache);
		Base oldbase = (Base)base.clone();
		
		Cargo cargo = (Cargo)base.getCargo().clone();
		cargo.setOption( Cargo.Option.NOHTML, true );
		
		this.log("berechne Asti "+base.getID()+":");
	
		Integer[] bebon = base.getActive();
	
		int bewohner = base.getBewohner();
		int e = base.getE();
		int arbeiter = base.getArbeiter();
	
		BaseStatus basedata = Base.getStatus(getContext(), base);
	
		//Bevoelkerungswachstum
		if( basedata.getBewohner() > bewohner ) {
			basedata.getStatus().addResource( Resources.NAHRUNG, bewohner );
			
			int diff = basedata.getBewohner()-bewohner;
			bewohner += diff/2+1;
			this.log("\t+ "+(diff/2+1)+" Bewohner");
			
			basedata.getStatus().substractResource( Resources.NAHRUNG, bewohner );
		}
		
		if( basedata.getBewohner() < bewohner ) {
			basedata.getStatus().addResource( Resources.NAHRUNG, bewohner );
			
			this.log("\tBewohner ("+bewohner+") auf "+basedata.getBewohner()+" gesetzt");
			bewohner = basedata.getBewohner();
			
			basedata.getStatus().substractResource( Resources.NAHRUNG, bewohner );
		}
	
		//Grund zurcksetzen
		String reason = "";
	
		//Neuen Cargo berechnen + Ausgabe einiger Werte
		boolean allok = true;
		this.log("\tStat:\n\t - Waren:");
		ResourceList reslist = cargo.compare(basedata.getStatus(), true);
		for( ResourceEntry res : reslist ) {
			this.log("\t   * "+res.getName()+" ("+res.getId()+") : cargo "+res.getCount1()+" stat "+res.getCount2());
			
			if( !res.getId().equals(Resources.NAHRUNG) ) {
				if( res.getCount1() + res.getCount2() < 0 ) {
					allok = false;
					reason = "Sie haben zu wenig "+res.getName();
				}
			}
			else if( res.getCount2() + this.usercargo.getResourceCount(Resources.NAHRUNG) < 0 ) {
				allok = false;
				reason = "Sie haben zu wenig "+res.getName();
			}
		}
		
		Cargo nc = (Cargo)cargo.clone();
		
		this.usercargo.addResource(Resources.NAHRUNG, basedata.getStatus().getResourceCount(Resources.NAHRUNG));
		if( this.usercargo.getResourceCount(Resources.NAHRUNG) < 0 ) {
			basedata.getStatus().setResource(Resources.NAHRUNG, this.usercargo.getResourceCount(Resources.NAHRUNG));	
		}
		else {
			basedata.getStatus().setResource(Resources.NAHRUNG, 0);
		}
		nc.addCargo(basedata.getStatus());
	
		this.log("\t - Arbeiter : "+arbeiter+" / "+basedata.getArbeiter());
		this.log("\t - Bewohner : "+bewohner+" / "+basedata.getBewohner());
		this.log("\t - e  : "+e+" / "+basedata.getE());
	
		int newe = e + basedata.getE();
		
		// Mehr als die max-e? Dann versuchen wir mal ein paar Batts zu laden
		if( newe > base.getMaxE() ) { 
			long emptybatts = nc.getResourceCount( Resources.LBATTERIEN );
			long load = newe - base.getMaxE();
			if( emptybatts < load ) {
				load = emptybatts;
			}
			
			if( load != 0 ) {
				this.log("\t\t. lade "+load+" Batterien auf");
			}
			
			nc.addResource( Resources.BATTERIEN, load );
			nc.substractResource( Resources.LBATTERIEN, load );
			
			newe = 1000;
		}
		
		if( newe < 0 ) {
			allok = false;
			reason = "Nicht genug Energie";
			newe = 0;
		}
		if( basedata.getArbeiter() > basedata.getBewohner() ) {
			allok = false;
			reason = "Sie haben mehr Arbeiter als (maximal) Bewohner";
		}
	
		//Cargocheck und dann speichern
		if( allok ) {
			this.log("\tAlles ok");
			
			if( !this.gtustatslist.containsKey(base.getSystem()) ) {
				SQLResultRow statsRow = db.first("SELECT stats FROM stats_verkaeufe WHERE tick=",this.tick," AND place='asti' AND system=",base.getSystem());
				Cargo stats = null;
				if( statsRow.isEmpty() ) {
					stats = new Cargo();
				}
				else {
					stats = new Cargo( Cargo.Type.STRING, statsRow.getString("stats") );
				}
				this.gtustatslist.put(base.getSystem(), stats);
			}
			
			long get = 0;
			
			// Autoverkaufauftraege bearbeiten
			if( !base.getAutoGTUActs().isEmpty() ) {				
				this.log("\tOffenbar haben wir ein paar auto-verkauf Auftraege:");
				List<AutoGTUAction> autoactlist = base.getAutoGTUActs();
				for( AutoGTUAction autoact : autoactlist ) {
					long count = 0;
					if( autoact.getActID() == 0 ) {
						count = autoact.getCount();
						if( count > nc.getResourceCount(autoact.getResID()) ) {
							count = nc.getResourceCount(autoact.getResID());	
						}	
					}
					else if( autoact.getActID() == 1 ) {
						count = nc.getResourceCount(autoact.getResID()) - autoact.getCount();	
					}
									
					if( count > 0 ) {
						get += this.curse.getResourceCount(autoact.getResID())/1000d*count;
						nc.substractResource(autoact.getResID(), count);
						this.gtustatslist.get(base.getSystem()).addResource(autoact.getResID(),count);
						
						this.log("\t * "+autoact.getResID()+" - "+autoact.getActID()+": verkaufe "+count+" . "+get+" RE");
					}
				}
			}
			
			// ggf noch ein paar weitere Waren verkaufen
			if( nc.getMass() > base.getMaxCargo() ) {
				this.log("\t...noch ist nicht genug Platz im Cargo:");
				
				boolean secondpass = false;
				
				long get2 = 0;
				reslist = basedata.getStatus().getResourceList();
				reslist.sortByCargo(true);
				while( reslist.hasNext() ) {
					ResourceEntry res = reslist.next();
					
					this.log("\tmass: "+nc.getMass()+" ; max: "+(base.getMaxCargo() - 2000)+" ; pc: "+nc.getResourceCount(res.getId())+" ; stat: "+basedata.getStatus().getResourceCount(res.getId()));
					if( (nc.getMass() > base.getMaxCargo() - 2000) && ( nc.getResourceCount(res.getId()) > 200) && (Cargo.getResourceMass(res.getId(), nc.getResourceCount(res.getId())) > 0 ) ) {
						long maxrtc = nc.getResourceCount(res.getId()) - 200;
						long diff = nc.getMass() - base.getMaxCargo()+ 2000;
						if( maxrtc > diff ) {
							maxrtc = diff;
						}
						nc.substractResource(res.getId(), maxrtc);
						this.gtustatslist.get(base.getSystem()).addResource(res.getId(),maxrtc);
						
						get2 += maxrtc * this.curse.getResourceCount(res.getId())/1000d;
						
						this.log("\t * "+maxrtc+" "+res.getName()+" an die GTU verkauft . "+get2+" RE");
					}
					else if( nc.getMass() <= base.getMaxCargo() - 2000 ) {
						break;
					}
					
					if( !secondpass && !reslist.hasNext() && (nc.getMass() > base.getMaxCargo()) ) {
						this.log("WARNING: Nach verkaeufen auf stat-Basis fehlt immer noch cargo. Verkaufe nun auf Basis vorhandener Resourcen");
						secondpass = true;
						reslist = nc.getResourceList();
					}
				}
				this.pmcache.append("[b]");
				this.pmcache.append(base.getName());
				this.pmcache.append("[/b] - F&uuml;r den R&auml;umungsverkauf werden ihnen ");
				this.pmcache.append(get2);
				this.pmcache.append(" RE gutgeschrieben\n\n");
				
				get += get2;
			}
			
			db.tBegin(true);
			
			// Haben wir was verkauft? Wenn ja nun das Geld ueberweisen
			if( get > 0 ) {
				User user = getContext().createUserObject(base.getOwner());
				user.transferMoneyFrom(Faction.GTU, get, "Automatischer Warenverkauf Asteroid "+base.getID()+" - "+base.getName(), false, User.TRANSFER_AUTO);	
			}
			
			if( (basedata.getArbeiter() != oldbase.getArbeiter()) || (bewohner != oldbase.getBewohner()) ||
				(e != oldbase.getE()) || !nc.save().equals(oldbase.getCargo().save()) ) {
					
				updateBaseQuery.tUpdate(1, basedata.getArbeiter(), bewohner, newe, nc.save(), 
						base.getID(), oldbase.getArbeiter(), oldbase.getBewohner(), oldbase.getE(), oldbase.getCargo().save() );
			}
			if( !db.tCommit() ) {
				this.log("\t++++++++++++++ COMMIT ERROR - RETRYING ++++++++++++++");
				this.usercargo.setResource(Resources.NAHRUNG, usercargostart);
				this.gtustatslist = oldgtustatslist;
				this.pmcache = oldpmcache;
				
				this.retries--;
				if( this.retries > 0 ) {
					base = new Base(db.first("SELECT * FROM bases WHERE id=",oldbase.getID()));
						  				 
					tickBase( base );
					return;	
				}
				this.log("\t+++++++++++++++ GEBE AUF +++++++++++++++");	
			}
		}
		// Offenbar haben wir es mit chaotischen Zustaenden zu tun....
		else {
			this.log("\tEs herschen offenbar chaotische Zust&auml;nde (Grund: "+reason+")");
			newe = (int)(newe/1.5d);
			
			this.usercargo.substractResource( Resources.NAHRUNG, bewohner/2);
			if( this.usercargo.getResourceCount(Resources.NAHRUNG) < 0 ) {
				this.usercargo.setResource(Resources.NAHRUNG, 0);
			}
	
			db.tBegin(true);
	
			if( (basedata.getArbeiter() != oldbase.getArbeiter()) || (basedata.getBewohner() != oldbase.getBewohner()) || 
				(newe != oldbase.getE()) || !cargo.save().equals(oldbase.getCargo().save()) ) {
				
				this.log("Schreibe neue Werte...");

				updateBaseQuery.tUpdate(1, basedata.getArbeiter(), basedata.getBewohner(), newe, cargo.save(), 
						base.getID(), oldbase.getArbeiter(), oldbase.getBewohner(), oldbase.getE(), oldbase.getCargo().save() );
			}
	
			this.pmcache.append("[b]");
			this.pmcache.append(base.getName());
			this.pmcache.append("[/b] - Es herschen chaotische Zust&auml;de - s&auml;mtliche Einrichtungen sind ausgefallen.");
			if( reason.length() != 0 ) {
				this.pmcache.append(" Die Ursache: ");
				this.pmcache.append(reason);
			}
			this.pmcache.append("\n\n");
	
			// Haben wir es zusaetzlich noch mit einer Hungersnot zu tun?
			if( this.usercargo.getResourceCount(Resources.NAHRUNG)+nc.getResourceCount( Resources.NAHRUNG ) <= 0 ) {
				this.log("\tOffenbar herscht eine Hungersnot");
	
				this.pmcache.setLength(0);
				this.pmcache.append("[b]");
				this.pmcache.append(base.getName());
				this.pmcache.append("[/b] - Es herscht eine Hungersnot - Einwohner fliehen - s&auml;mtliche Einrichtungen sind ausgefallen\n\n");
	
				for( int i=0; i < base.getWidth()*base.getHeight(); i++ ) {
					bebon[i] = 0;
				}
				base.put("coreactive", 0);
				
				if( (arbeiter != oldbase.getArbeiter()) || (arbeiter != oldbase.getBewohner()) ||
					oldbase.isCoreActive() ) {
						
					this.log("Schreibe neue Werte [hungersnot]...");
					
					db.tUpdate(1,"UPDATE bases " ,
						"SET arbeiter=",arbeiter,"," ,
							"bewohner=",arbeiter,"," ,
							"active='",Common.implode("|",bebon),"'," ,
							"coreactive='",base.isCoreActive() ? 1 : 0,"' " ,
						"WHERE id='",base.getID(),"' AND " ,
							"arbeiter='",basedata.getArbeiter(),"' AND " ,
							"bewohner='",basedata.getBewohner(),"' AND " ,
							"active='",Common.implode("|", oldbase.getActive()),"' AND " ,
							"coreactive='",oldbase.isCoreActive() ? 1 : 0,"'");
				}
			}
			
			if( !db.tCommit() ) {
				this.log("\t++++++++++++++ COMMIT ERROR - RETRYING ++++++++++++++");
				this.usercargo.setResource(Resources.NAHRUNG, usercargostart);
				this.gtustatslist = oldgtustatslist;
				this.pmcache = oldpmcache;
				
				this.retries--;
				if( this.retries > 0 ) {
					base = new Base(db.first("SELECT * FROM bases WHERE id=",oldbase.getID()));
						  				 
					tickBase( base );
					return;	
				}
				this.log("\t+++++++++++++++ GEBE AUF +++++++++++++++");	
			}
		}
		this.log("");
	}

	@Override
	protected void tick() {
		Database db = getDatabase();
		
		// Da wir als erstes mit dem Usercargo rumspielen -> sichern der alten Nahrungswerte
		SQLQuery auser = db.query("SELECT id,cargo FROM users WHERE id!=0 AND (vaccount=0 OR wait4vac!=0)");
		while( auser.next() ) {
			Cargo cargo = new Cargo( Cargo.Type.STRING, auser.getString("cargo") );
			
			db.update("UPDATE users SET nstat='",cargo.getResourceCount(Resources.NAHRUNG),"' WHERE id='",auser.getInt("id"),"'");
		}
		auser.free();
		
		// User-Accs sperren
		block(0);
		
		PreparedQuery updateUserCargo = db.prepare("UPDATE users SET cargo= ? WHERE id= ?");
		updateBaseQuery = db.prepare("UPDATE bases " ,
				"SET arbeiter= ? ," ,
					"bewohner= ? ," ,
					"e= ? ," ,
					"cargo= ?  " ,
				"WHERE id= ? AND " ,
					"arbeiter= ? AND " ,
					"bewohner= ? AND " ,
					"e= ? AND " ,
					"cargo= ? ");
		
		// Nun holen wir uns mal die Basen...
		SQLQuery base = db.query("SELECT b.* " +
				"FROM bases b JOIN users u ON b.owner=u.id " +
				"WHERE b.owner!=0 AND (u.vaccount=0 OR u.wait4vac!=0) " +
				"ORDER BY t1.owner");
		
		log("Kolonien: "+base.numRows());
		log("");
		
		while( base.next() ) {
			// Muessen ggf noch alte Userdaten geschrieben und neue geladen werden?
			if( base.getInt("owner") != this.lastowner ) {
				log(base.getInt("owner")+":");
				if( this.pmcache.length() != 0 ) {
					PM.send(getContext(),-1, this.lastowner, "Basis-Tick", this.pmcache.toString(),false);
					this.pmcache.setLength(0);
				}
				
				if( this.usercargo != null ) {
					updateUserCargo.update(this.usercargo.save(), this.lastowner);
				}
				
				this.usercargo = new Cargo( Cargo.Type.STRING, db.first("SELECT cargo FROM users WHERE id='",base.getInt("owner"),"'").getString("cargo"));
			}
			this.lastowner = base.getInt("owner");
		
			this.retries = 5; // Max 5 versuche. Wenn die Basis dann noch immer nicht korrekt berechnet wurde: aufgeben

			// Nun wollen wir die Basis mal berechnen....
			this.tickBase(new Base(base.getRow()));
		}
		base.free();
		
		// ggf noch vorhandene Userdaten schreiben
		if( this.pmcache.length() != 0 ) {
			PM.send(getContext(),-1, this.lastowner, "Basis-Tick", this.pmcache.toString(), false);
			this.pmcache.setLength(0);
		}
		if( this.usercargo != null ) {
			updateUserCargo.update(this.usercargo.save(), this.lastowner);
		}
		
		updateUserCargo.close();
		updateBaseQuery.close();
		
		// Die neuen GTU-Verkaufsstats schreiben
		for( Integer sys : this.gtustatslist.keySet() ) {
			Cargo gtustat = this.gtustatslist.get(sys);
			SQLResultRow statid = db.first("SELECT id FROM stats_verkaeufe WHERE tick='",this.tick,"' AND place='asti' AND system='",sys,"'");
			if( statid.isEmpty() ) {
				db.update("INSERT INTO stats_verkaeufe (tick,place,system,stats) VALUES (",this.tick,",'asti',",sys,",'",gtustat.save(),"')");
			}
			else {		
				db.update("UPDATE stats_verkaeufe SET stats='",gtustat.save(),"' WHERE id='",statid,"'");
			}
		}
		
		// User-Accs wieder entsperren
		unblock(0);
	}

}
