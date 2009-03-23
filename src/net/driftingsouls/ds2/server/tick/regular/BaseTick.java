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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
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
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.Forschungszentrum;
import net.driftingsouls.ds2.server.entities.GtuWarenKurse;
import net.driftingsouls.ds2.server.entities.StatVerkaeufe;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WeaponFactory;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.db.HibernateFacade;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.werften.WerftObject;
import net.driftingsouls.ds2.server.werften.WerftQueueEntry;

import org.hibernate.StaleObjectStateException;

/**
 * <h1>Berechnung des Ticks fuer Basen.</h1>
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
	
	@Override
	protected void prepare() {
		//getDB().setFlushMode(FlushMode.MANUAL);
		
		// Aktuelle Warenkurse fuer die Kommandozentrale laden
		GtuWarenKurse kurs = (GtuWarenKurse)getDB().get(GtuWarenKurse.class, "asti");
		this.curse = kurs.getKurse();
		
		this.pmcache = new StringBuilder();
		this.lastowner = 0;
		this.usercargo = null;
		this.gtustatslist = new HashMap<Integer,Cargo>();
		this.tick = getContext().get(ContextCommon.class).getTick();
	}
	
	private void tickBase( Base base ) {
		org.hibernate.Session db = getDB();
		
		Map<Integer,Cargo> oldgtustatslist = new HashMap<Integer,Cargo>();
		for( Integer sysid : this.gtustatslist.keySet() ) {
			oldgtustatslist.put(sysid, (Cargo)gtustatslist.get(sysid).clone() );
		}

		Cargo cargo = (Cargo)base.getCargo().clone();
		cargo.setOption( Cargo.Option.NOHTML, true );
		
		this.log("berechne Asti "+base.getId()+":");
	
		Integer[] bebon = base.getActive();
	
		int inhabitants = base.getBewohner();
		int marines = base.getMarines();
		int e = base.getEnergy();
		int worker = base.getArbeiter();
	
		BaseStatus basedata = Base.getStatus(getContext(), base);
	
		//Bevoelkerungswachstum
		if( basedata.getBewohner() > inhabitants ) {
			basedata.getStatus().addResource( Resources.NAHRUNG, inhabitants );
			
			int diff = basedata.getBewohner()-inhabitants;
			int immigrants = (int)(Math.random()*(diff/2));
			inhabitants += immigrants;
			this.log("\t+ "+immigrants+" Bewohner");
			
			basedata.getStatus().substractResource( Resources.NAHRUNG, inhabitants );
		}
		if( marines != 0){
			basedata.getStatus().substractResource( Resources.NAHRUNG, marines );
		}
		
		if( basedata.getBewohner() < inhabitants ) {
			basedata.getStatus().addResource( Resources.NAHRUNG, inhabitants );
			
			this.log("\tBewohner ("+inhabitants+") auf "+basedata.getBewohner()+" gesetzt");
			inhabitants = basedata.getBewohner();
			
			basedata.getStatus().substractResource( Resources.NAHRUNG, inhabitants );
		}
		
		//Get taxes and pay social security benefits
		ConfigValue taxValue = (ConfigValue)db.get(ConfigValue.class, "tax");
		ConfigValue ssbValue = (ConfigValue)db.get(ConfigValue.class, "socialsecuritybenefit");
		int tax = Integer.parseInt(taxValue.getValue());
		int socialSecurityBenefit = Integer.parseInt(ssbValue.getValue());
		
		User owner = base.getOwner();
		BigInteger account = owner.getKonto();
		int income = worker * tax - (inhabitants - worker) * socialSecurityBenefit;
		if(income > 0)
		{
			this.log("Steuern ausbezahlt " + income);
			owner.transferMoneyFrom(owner.getId(), income, "Steuereinnahmen Asteroid " + base.getId() + " - " + base.getName(), true, User.TRANSFER_AUTO);
		}
		else
		{
			this.log("Steuereinnahmen reichen nicht, greife auf Konto zurueck");
			income = Math.abs(income);
			BigInteger incomeHelper = BigInteger.valueOf(income);
			
			User nobody = (User)db.get(User.class, -1);
			//Account is bigger than costs
			if(account.compareTo(incomeHelper) >= 0)
			{
				this.log("Bezahlter Betrag: " + income);
				nobody.transferMoneyFrom(owner.getId(), income, "Arbeitslosenhilfe Asteroid " + base.getId() + " - " + base.getName() + " User: " + owner.getName() + " (" + owner.getId() + ")", false, User.TRANSFER_AUTO);
			}
			else
			{
				//Inhabitants flee
				int excess = Math.abs(account.subtract(incomeHelper).intValue());
				income = income - excess;
				nobody.transferMoneyFrom(owner.getId(), income, "Arbeitslosenhilfe Asteroid " + base.getId() + " - " + base.getName() + " User: " + owner.getName() + " (" + owner.getId() + ")", false, User.TRANSFER_AUTO);
				inhabitants = inhabitants - excess;
				this.log("Konto nicht gedeckt, " + excess + " Einwohner fliehen.");
				this.log("Bezahlter Betrag: " + income);
			}
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
				if( (res.getCount2() < 0) && (res.getCount1() + res.getCount2() < 0) ) {
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
		
		long userNahrung = this.usercargo.getResourceCount(Resources.NAHRUNG) +
			basedata.getStatus().getResourceCount(Resources.NAHRUNG);
		if( userNahrung < 0 ) {
			// Nahrung auf dem Asti spaeter entsprechen reduzieren...
			basedata.getStatus().setResource(Resources.NAHRUNG, userNahrung);
			userNahrung = 0;
		}
		else {
			basedata.getStatus().setResource(Resources.NAHRUNG, 0);
		}
		nc.addCargo(basedata.getStatus());
	
		this.log("\t - Arbeiter : "+worker+" / "+basedata.getArbeiter());
		this.log("\t - Bewohner : "+inhabitants+" / "+basedata.getBewohner());
		this.log("\t - e  : "+e+" / "+basedata.getE());
	
		int newe = e + basedata.getE();
		
		// Mehr als die max-e? Dann versuchen wir mal ein paar Batts zu laden
		if( newe > base.getMaxEnergy() ) { 
			long emptybatts = nc.getResourceCount( Resources.LBATTERIEN );
			long load = newe - base.getMaxEnergy();
			if( emptybatts < load ) {
				load = emptybatts;
			}
			
			if( load != 0 ) {
				this.log("\t\t. lade "+load+" Batterien auf");
			}
			
			nc.addResource( Resources.BATTERIEN, load );
			nc.substractResource( Resources.LBATTERIEN, load );
			
			newe = base.getMaxEnergy();
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
				StatVerkaeufe stat = (StatVerkaeufe)db.createQuery("from StatVerkaeufe where tick=? and place='asti' and system=?")
					.setInteger(0, this.tick)
					.setInteger(1, base.getSystem())
					.uniqueResult();
				
				Cargo stats = null;
				if( stat == null ) {
					stats = new Cargo();
				}
				else {
					stats = new Cargo(stat.getStats());
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
				for( Iterator<ResourceEntry> iter=reslist.iterator(); iter.hasNext(); ) {
					ResourceEntry res = iter.next();
					
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
					
					if( !secondpass && !iter.hasNext() && (nc.getMass() > base.getMaxCargo()) ) {
						this.log("WARNING: Nach verkaeufen auf stat-Basis fehlt immer noch cargo. Verkaufe nun auf Basis vorhandener Resourcen");
						secondpass = true;
						reslist = nc.getResourceList();
						iter = reslist.iterator();
					}
				}
				this.pmcache.append("[b]");
				this.pmcache.append(base.getName());
				this.pmcache.append("[/b] - F&uuml;r den R&auml;umungsverkauf werden ihnen ");
				this.pmcache.append(get2);
				this.pmcache.append(" RE gutgeschrieben\n\n");
				
				get += get2;
			}

			// Haben wir was verkauft? Wenn ja nun das Geld ueberweisen
			if( get > 0 ) {
				User user = base.getOwner();
				user.transferMoneyFrom(Faction.GTU, get, "Automatischer Warenverkauf Asteroid "+base.getId()+" - "+base.getName(), false, User.TRANSFER_AUTO);	
			}
			
			this.usercargo.setResource(Resources.NAHRUNG, userNahrung);
			
			base.setArbeiter(basedata.getArbeiter());
			base.setBewohner(inhabitants);
			base.setEnergy(newe);
			base.setCargo(nc);
		}
		// Offenbar haben wir es mit chaotischen Zustaenden zu tun....
		else {
			this.log("\tEs herschen offenbar chaotische Zust&auml;nde (Grund: "+reason+")");
			newe = (int)(newe/1.5d);
			
			this.usercargo.substractResource( Resources.NAHRUNG, inhabitants/2);
			if( this.usercargo.getResourceCount(Resources.NAHRUNG) < 0 ) {
				this.usercargo.setResource(Resources.NAHRUNG, 0);
			}

			base.setArbeiter(basedata.getArbeiter());
			base.setBewohner(inhabitants);
			base.setEnergy(newe);
			base.setCargo(cargo);
			
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
				base.setCoreActive(false);
				base.setArbeiter(worker);
				base.setBewohner(worker);
				base.setActive(bebon);
				base.setCoreActive(false);
			}
		}

		this.log("");
	}
	

	private void tickBases() {
		org.hibernate.Session db = getDB();
		
		User sourceUser = (User)db.get(User.class, -1);
		
		// Da wir als erstes mit dem Usercargo rumspielen -> sichern der alten Nahrungswerte
		List<?> users = db.createQuery("from User where id!=0 and (vaccount=0 or wait4vac!=0)").list();
		for( Iterator<?> iter = users.iterator(); iter.hasNext(); ) {
			User auser = (User)iter.next();
			
			auser.setNahrungsStat(Long.toString(new Cargo(Cargo.Type.STRING, auser.getCargo()).getResourceCount(Resources.NAHRUNG)));
		}
		
		getContext().commit();
		
		// Nun holen wir uns mal die Basen...
		List<?> bases = db.createQuery("from Base b join fetch b.owner where b.owner!=0 and (b.owner.vaccount=0 or b.owner.wait4vac!=0) order by b.owner").list();
			
		log("Kolonien: "+bases.size());
		log("");
		
		for( Iterator<?> iter = bases.iterator(); iter.hasNext(); ) {
			Base base = (Base)iter.next();
			
			// Muessen ggf noch alte Userdaten geschrieben und neue geladen werden?
			if( base.getOwner().getId() != this.lastowner ) {
				HibernateFacade.evictAll(db, 
						WerftObject.class, 
						WerftQueueEntry.class, 
						Forschungszentrum.class,
						Academy.class,
						WeaponFactory.class);
				
				log(base.getOwner().getId()+":");
				if( this.pmcache.length() != 0 ) {
					PM.send(sourceUser, this.lastowner, "Basis-Tick", this.pmcache.toString());
					this.pmcache.setLength(0);
				}
				
				try {
					getContext().commit();
				}
				catch( Exception e ) {
					getContext().rollback();
					if( e instanceof StaleObjectStateException ) {
						evictStaleObjects(db, (StaleObjectStateException)e);
					}
					HibernateFacade.evictAll(db, PM.class);
					db.evict(db.get(User.class, this.lastowner));
					
					this.log("Base Tick - User #"+this.lastowner+" failed: "+e);
					e.printStackTrace();
					Common.mailThrowable(e, "BaseTick - User #"+this.lastowner+" failed: "+e, "");
				}
								
				this.usercargo = new Cargo( Cargo.Type.STRING, base.getOwner().getCargo());
			}
			
			this.lastowner = base.getOwner().getId();
			
			// Nun wollen wir die Basis mal berechnen....
			try {
				this.tickBase(base);
				
				base.getOwner().setCargo(this.usercargo.save());
				
				getContext().commit();
			}
			catch( Exception e ) {
				getContext().rollback();
				
				if( e instanceof StaleObjectStateException ) {
					evictStaleObjects(db, (StaleObjectStateException)e);
				}
				
				this.log("Base Tick - Base #"+base.getId()+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "BaseTick - Base #"+base.getId()+" Exception", "");
			}
			finally {
				db.evict(base);
			}
		}
		
		// ggf noch vorhandene Userdaten schreiben
		if( this.pmcache.length() != 0 ) {
			PM.send(sourceUser, this.lastowner, "Basis-Tick", this.pmcache.toString());
			this.pmcache.setLength(0);
		}
		
		try {
			getContext().commit();
		}
		catch( Exception e ) {
			getContext().rollback();
			db.clear();
			
			this.log("Base Tick - User #"+this.lastowner+" failed: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "BaseTick - User #"+this.lastowner+" failed: "+e, "");
		}
		
		// Die neuen GTU-Verkaufsstats schreiben
		for( Integer sys : this.gtustatslist.keySet() ) {
			Cargo gtustat = this.gtustatslist.get(sys);
			
			StatVerkaeufe stat = (StatVerkaeufe)db.createQuery("from StatVerkaeufe where tick=? and place='asti' and system=?")
				.setInteger(0, this.tick)
				.setInteger(1, sys)
				.uniqueResult();
			
			if( stat == null ) {
				stat = new StatVerkaeufe(this.tick, sys, "asti");
				db.persist(stat);
			}
			stat.setStats(gtustat);
		}
	}

	private void evictStaleObjects(org.hibernate.Session db, StaleObjectStateException e)
	{
		// Nicht besonders schoen, da moeglicherweise unangenehme Nebeneffekte auftreten koennen
		// Eine solche Entity kann aber ebenso negative Effekte auf den Tick haben, wenn diese
		// nicht entfernt wird
		Object entity = db.get(e.getEntityName(), e.getIdentifier());
		if( entity != null ) {
			db.evict(entity);
		}
	}

	@Override
	protected void tick() {
		// User-Accs sperren
		block(0);
		getContext().commit();
		
		try {
			tickBases();
			getContext().commit();
		}
		catch( Exception e ) {			
			this.log("Base Tick failed: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "BaseTick Exception", "");
		}
		finally {
			// User-Accs wieder entsperren
			unblock(0);
			getContext().commit();
		}
	}
}
