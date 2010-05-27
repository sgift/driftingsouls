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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.entities.Feeding;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargo.Crew;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;

/**
 * Berechnung des Ticks fuer Schiffe.
 * @author Drifting-Souls Team
 */
public class SchiffsTick extends TickController {
	private Map<String,ResourceID> esources;
	private Map<Location,List<Ship>> versorgerlist;
	private int unflushedShips = 0;
	
	@Override
	protected void prepare() 
	{
		esources = new LinkedHashMap<String,ResourceID>();
		esources.put("a", Resources.ANTIMATERIE);
		esources.put("d", Resources.DEUTERIUM);
		esources.put("u", Resources.URAN);
	}

	/**
	 * Consumes food from the given cargo.
	 * @param feeder The feeding object.
	 * @param crewToFeed Crew which must be feed.
	 * @param scaleFactor A scaling factor for the food consumption.
	 * @return Crew that couldn't be feed.
	 */
	private int consumeFood(Feeding feeder, int crewToFeed, double scaleFactor) 
	{
		int crewThatCouldBeFeed = 0;
		if( crewToFeed > feeder.getNahrungCargo()*scaleFactor ) 
		{
			crewThatCouldBeFeed = (int)(feeder.getNahrungCargo()*scaleFactor);
			crewToFeed -= crewThatCouldBeFeed;
		}
		else 
		{
			crewThatCouldBeFeed = crewToFeed;
			crewToFeed = 0;
		}
		
		int tmp = (int)Math.ceil(crewThatCouldBeFeed/scaleFactor);
		feeder.setNahrungCargo(feeder.getNahrungCargo() - tmp);
		this.log(tmp+" von "+feeder.getId()+" verbraucht");

		return crewToFeed;
	}
	
	private Map<Location,List<Ship>> getLocationVersorgerList(org.hibernate.Session db,User user)
	{
		Map<Location,List<Ship>> versorgerlist = new HashMap<Location,List<Ship>>();
		this.log("Berechne Versorger");
		List<Ship> ships = Common.cast(db.createQuery("from Ship as s left join fetch s.modules" +
				" where s.id>0 and s.owner=? and system!=0 and (s.shiptype.versorger=1 or s.modules.versorger=1)" +
				"order by s.nahrungcargo DESC")
				.setEntity(0, user)
				.list());
		this.log(ships.size()+" Versorger gefunden");
		for( Ship ship : ships)
		{
			if(ship.isFeeding() && ship.getNahrungCargo() > 0)
			{
				Location loc = ship.getLocation();
				if(versorgerlist.containsKey(loc))
				{
					versorgerlist.get(loc).add(ship);
				}
				else
				{
					List<Ship> shiplist = new ArrayList<Ship>();
					shiplist.add(ship);
					versorgerlist.put(loc, shiplist);
				}
			}
		}
		
		if(user.getAlly() != null)
		{
			this.log("Berechne Allianzversorger");
			ships = Common.cast(db.createQuery("from Ship as s left join fetch s.modules" +
					" where s.id>0 and s.owner!=? and s.owner.ally=? and (s.owner.vaccount=0 or" +
					" s.owner.wait4vac!=0) and system!=0 and" +
					" (s.shiptype.versorger=1 or s.modules.versorger=1) and" +
					" s.isfeeding=1 and s.isallyfeeding=1" +
					" order by s.nahrungcargo DESC")
					.setEntity(0, user)
					.setEntity(1, user.getAlly())
					.list());
			this.log(ships.size()+" Schiffe gefunden");
			for( Ship ship : ships)
			{
				if(ship.isFeeding() && ship.isAllyFeeding() && ship.getNahrungCargo() > 0)
				{
					Location loc = ship.getLocation();
					if(versorgerlist.containsKey(loc))
					{
						versorgerlist.get(loc).add(ship);
					}
					else
					{
						List<Ship> shiplist = new ArrayList<Ship>();
						shiplist.add(ship);
						versorgerlist.put(loc, shiplist);
					}
				}
			}
		}
		
		return versorgerlist;
	}
	
	private Ship getVersorger(Location loc)
	{
		if(versorgerlist.containsKey(loc))
		{
			Ship ship = versorgerlist.get(loc).get(0);
			while(ship != null && ship.getNahrungCargo() == 0)
			{
				versorgerlist.get(loc).remove(0);
				if(versorgerlist.get(loc).isEmpty())
				{
					versorgerlist.remove(loc);
					ship = null;
				}
				else
				{
					ship = versorgerlist.get(loc).get(0);
				}
			}
		}
		
		return null;
	}

	private void tickShip( org.hibernate.Session db, Ship shipd, Map<Location, List<Base>> feedingBases) 
	{
		this.log(shipd.getName()+" ("+shipd.getId()+"):");

		ShipTypeData shiptd = shipd.getTypeData();

		Cargo shipc = shipd.getCargo();

		this.log("\tAlt: crew "+shipd.getCrew()+" e "+shipd.getEnergy() +" speicher "+shipd.getNahrungCargo());
		
		boolean isBattle = shipd.getBattle() != null;
				
		//Eigene Basen im selben Sektor
		List<Base> bases = feedingBases.get(shipd.getLocation());
		if(bases == null)
		{
			bases = new ArrayList<Base>();
		}

		//Mein Mutterschiff - relevant bei gedockten Schiffen
		Ship baseShip = shipd.getBaseShip();

		this.slog("\tCrew: ");
		//Crew die noch gefuettert werden muss
		int crewToFeed = shipd.getNettoFoodConsumption();

		//Faktor fuer den Verbrauch
		double scaleFactor = shipd.getAlertScaleFactor();

		Ship versorger = getVersorger(shipd.getLocation());
		//Basencargo, VersorgerCargo, Basisschiffcargo, eigener Cargo - Leerfuttern in der Reihenfolge
		for(Base feedingBase: bases)
		{
			crewToFeed = consumeFood(feedingBase, crewToFeed, scaleFactor);
			if(crewToFeed == 0)
			{
				break;
			}
		}
		
		while(versorger != null && crewToFeed > 0)
		{
			crewToFeed = consumeFood(versorger ,crewToFeed, scaleFactor);
			versorger = getVersorger(shipd.getLocation());
		}

		if(baseShip != null)
		{
			crewToFeed = consumeFood(baseShip, crewToFeed, scaleFactor);
		}
		
		crewToFeed = consumeFood(shipd, crewToFeed, scaleFactor);
		
		//Crew die nicht versorgt werden konnte verhungern lassen
		if(crewToFeed > 0) 
		{
			this.log("Crew verhungert - ");
			if(crewToFeed >= shipd.getUnits().getNahrung())
			{
				crewToFeed = crewToFeed - (int)Math.ceil(shipd.getUnits().getNahrung() / 10.0);
				shipd.setUnits(new UnitCargo());
				ConfigValue maxverhungern = (ConfigValue)db.get(ConfigValue.class, "maxverhungern");
				int maxverhungernfactor = Integer.parseInt(maxverhungern.getValue());
				int maxverhungernvalue = (int)(shiptd.getCrew() * (maxverhungernfactor/100.0));
				if( crewToFeed > maxverhungernvalue/10)
				{
					crewToFeed = maxverhungernvalue/10;
				}
				int crew = shipd.getCrew() - crewToFeed*10;
				if(crew < 0)
				{
					shipd.setCrew(0);
					this.log("Gedockte Schiffe verhungern.");
					crew = Math.abs(crew);
					List<Ship> dockedShips = shipd.getLandedShips();
					for(Iterator<Ship> iter=dockedShips.iterator();iter.hasNext();)
					{
						Ship dockShip = iter.next();
						if(crew > dockShip.getCrew())
						{
							crew -= dockShip.getCrew();
							dockShip.setCrew(0);
						}
						else
						{
							dockShip.setCrew(dockShip.getCrew()-crew);
							break;
						}
					}
				}
				else
				{
					shipd.setCrew(crew);
				}
			}
			else 
			{
				shipd.getUnits().fleeUnits(crewToFeed*10);
				crewToFeed = 0;
			}
		}
		else
		{
			long neededFood = shiptd.getNahrungCargo() - shipd.getNahrungCargo();
			if(neededFood > 0)
			{
				for(Base base: bases)
				{
					Cargo baseCargo = base.getCargo();
					
					long baseFood = baseCargo.getResourceCount(Resources.NAHRUNG);
					long takenFood = Math.min(baseFood, neededFood);
					baseCargo.substractResource(Resources.NAHRUNG, takenFood);
					neededFood = neededFood - takenFood;
					shipd.setNahrungCargo(shipd.getNahrungCargo() + takenFood);
					base.setCargo(baseCargo);
					
					if(neededFood == 0)
					{
						break;
					}
				}
			}
		}
		
		//Damage ships which don't have enough crew
		if(!isBattle)
		{
			int crew = shipd.getCrew();
			int minCrew = shiptd.getMinCrew();
			User user = shipd.getOwner();
			this.log("Crew " + crew);
			this.log("MinCrew " + minCrew);
			if(crew < minCrew && !user.hasFlag(User.FLAG_NO_HULL_DECAY))
			{
				this.log("Schiff hat nicht genug Crew; beschaedige Huelle.");
				ConfigValue value = (ConfigValue)db.get(ConfigValue.class, "nocrewhulldamagescale");
				double scale = Double.parseDouble(value.getValue());
				double damageFactor = (1.0 - (((double)crew) / ((double)minCrew))) / scale;
				this.log("Damage factor is: " + damageFactor);
				
				int oldArmor = shipd.getAblativeArmor();
				if(oldArmor > 0)
				{
					int damage = (int)Math.ceil(shiptd.getAblativeArmor()*damageFactor);
					int newArmor = oldArmor - damage;
					if(newArmor < 0)
					{
						this.log("Ablative Panzerung zerstoert.");
						shipd.setAblativeArmor(0);
					}
					else
					{
						this.log("Ablative Panzerung beschaedigt - neu: " + newArmor);
						shipd.setAblativeArmor(newArmor);
					}
				}
				else
				{
					int damage = (int)Math.ceil(shiptd.getHull()*damageFactor);
					int newHull = shipd.getHull() - damage;
					if(newHull > 0)
					{
						this.log("Huelle beschaedigt - neu: " + newHull);
						shipd.setHull(newHull);
					}
					else
					{
						this.log("Schiff zerstoert.");
						shipd.setStatus("destroy");
						return;
					}
				}
			}
		}
		
		//Pay sold and maintenance
		int reCost = shipd.getBalance();
		if(reCost > 0)
		{
			this.log("Zahle Sold und Wartungskosten");
			User owner = shipd.getOwner();
			BigInteger account = owner.getKonto();
			BigInteger reCostHelp = BigInteger.valueOf(reCost);
			
			//Account is balanced
			if(account.compareTo(reCostHelp) >= 0)
			{
				this.log("Bezahlter Betrag: " + reCost);
				User nobody = (User)db.get(User.class, -1);
				nobody.transferMoneyFrom(owner.getId(), reCost);
			}
			else if(owner.getId() != Faction.PIRATE)
			{
				BigInteger reCostHelper = BigInteger.valueOf(shiptd.getReCost());
				// Wartungskosten koennen aufgebracht werden.
				if(account.compareTo(reCostHelper) >= 0)
				{
					this.log("Konto nicht gedeckt; Besatzung meutert.");
					
					// Sammel alle Daten zusammmen
					User pirate = (User)db.get(User.class, Faction.PIRATE);
					UnitCargo unitcargo = shipd.getUnits();
					UnitCargo meuterer = unitcargo.getMeuterer(account.intValue() - shiptd.getReCost());
					Crew dcrew = new UnitCargo.Crew(shipd.getCrew());
					
					if(meuterer.kapern(unitcargo, new UnitCargo(), new UnitCargo(), dcrew, 1, 1))
					{
						shipd.setCrew(dcrew.getValue());
						shipd.setUnits(meuterer);
						shipd.setOwner(pirate);
						
						PM.send(pirate, owner.getId(), "Besatzung meutert", "Die Besatzung der " + shipd.getName() + " meutert, nachdem Sie den Sold der Einheiten nicht aufbringen konnten. (" + shipd.getLocation().displayCoordinates(false) + ")");
					}
					else
					{
						shipd.setUnits(unitcargo);
						shipd.setCrew(dcrew.getValue());
						PM.send(pirate, owner.getId(), "Besatzung meutert", "Die Besatzung der " + shipd.getName() + " meutert, nachdem Sie den Sold der Einheiten nicht aufbringen konnten. Die Meuterer wurden vernichtet. (" + shipd.getLocation().displayCoordinates(false) + ")");
					}
					owner.setKonto(BigInteger.ZERO);
				}
				else
				{
					User pirate = (User)db.get(User.class, Faction.PIRATE);
					shipd.setOwner(pirate);
					owner.setKonto(BigInteger.ZERO);
					
					this.log("Konto nicht gedeckt; Schiff desertiert zum Piraten.");
					PM.send(pirate, owner.getId(), "Schiff desertiert", "Die " + shipd.getName() + " ist desertiert, nachdem Sie den Sold der Crew nicht aufbringen konnten. (" + shipd.getLocation().displayCoordinates(false) + ")");
				}
			}
		}

		//Berechnung der Energie
		this.log("\tEnergie:");
		int e = shipd.getEnergy();
		
		if(shiptd.getShipClass() != ShipClasses.GESCHUETZ.ordinal()) 
		{
			e -= shipd.getAlertEnergyCost();
			if( e < 0 ) {
				e = 0;
			}	
		}	

		if( e < shiptd.getEps() ) 
		{
			int rm = shiptd.getRm();
			if( shiptd.getCrew() > 0 ) {
				rm = (int)(rm * shipd.getCrew() / (double)shiptd.getCrew());
			}
			int maxenergie = rm;

			// Reihenfolge muss identisch zu this.esources sein!
			int[] reactres = new int[] {shiptd.getRa(), shiptd.getRd(), shiptd.getRu()};
			int index = 0;

			for( String resshort : this.esources.keySet() ) {
				ResourceID resid = this.esources.get(resshort);

				if(reactres[index] > 0) {
					this.slog("\t * "+Cargo.getResourceName(resid)+": ");
					if( shipc.getResourceCount( resid ) > 0 ) {
						this.log(shipc.getResourceCount( resid )+" vorhanden");

						int max = (int)Math.round(rm / (double)reactres[index]);
						if( max > Math.round(maxenergie/(double)reactres[index]) ) {
							max = (int)Math.round(maxenergie/(double)reactres[index]);
						}

						int need = shiptd.getEps() - e;
						this.log("\t   maximal: "+max+" Energie bei "+reactres[index]+" Reaktorwert : "+need+" Energie frei im eps");

						int counter = 0;
						for( int k=0; k < max; k++ ) {
							if( (need > 0) && (shipc.getResourceCount( resid ) > 0) ) {
								counter++;
								if( maxenergie < reactres[index] ) {
									e += maxenergie;
									maxenergie = 0;
								} 
								else {
									e += reactres[index];
									maxenergie -= reactres[index];
								}
								shipc.substractResource( resid, 1 );
								need -= reactres[index];
								if( e > shiptd.getEps() ) {
									e = shiptd.getEps();
								}
							}
						}
						this.log("\t   verbrenne "+counter+" "+Cargo.getResourceName(resid));
					} 
					else {
						this.log(" kein "+Cargo.getResourceName(resid)+" vorhanden");
					}
				}

				index++;
			}
		}

		int[] sub = new int[] {shipd.getEngine(),shipd.getWeapons(),shipd.getComm(),shipd.getSensors()};

		// Schiff bei Bedarf und falls moeglich reparieren
		if(!isBattle && (shipd.getStatus().indexOf("lowmoney") == -1) &&
				( (shipd.getEngine() < 100) || (shipd.getWeapons() < 100) || (shipd.getComm() < 100) || (shipd.getSensors() < 100) ) &&
				(Ships.getNebula(shipd.getLocation()) != 6)  ) {

			Offizier offizier = Offizier.getOffizierByDest('s', shipd.getId());

			for( int a=0; a<=3; a++ ) 
			{
				int old = sub[a];
				if( shipd.getCrew() == shiptd.getCrew() ) 
				{
					sub[a] += 20;
				}
				else if( shipd.getCrew() > shiptd.getCrew()/2 ) 
				{
					sub[a] += 15;
				}
				else if( shipd.getCrew() == shiptd.getCrew()/2 ) 
				{
					sub[a] += 10;
				}
				else if( shipd.getCrew() < shiptd.getCrew()/2 ) 
				{
					sub[a] += 5;
				}

				if( offizier != null ) 
				{
					sub[a] += (int)(offizier.getAbility(Offizier.Ability.ING) / 3d );

					if( sub[a] > 40 + (int)(offizier.getAbility(Offizier.Ability.ING)/4d) ) 
					{
						sub[a] = 40 + (int)(offizier.getAbility(Offizier.Ability.ING)/4d);
					}
				} 
				else if( sub[a] > 40 ) 
				{
					sub[a] = 40;
				}
				if( old > sub[a] ) 
				{
					sub[a] = old;
				}
				if( sub[a] > 100 ) 
				{
					sub[a] = 100;
				}
			}
		}

		// Evt. Deuterium sammeln
		if(!isBattle && shipd.getAutoDeut() && (shiptd.getDeutFactor() != 0) && (shipd.getCrew() >= shiptd.getCrew()/2) && (e > 0) && (shipc.getMass() < shiptd.getCargo()) ) 
		{
			this.slog("\tS. Deut: ");
			int nebel = Ships.getNebula(shipd.getLocation());

			if( (nebel >= 0) && (nebel <= 2) ) 
			{
				int tmpe = e;

				int deutfactor = shiptd.getDeutFactor();
				if( nebel == 1 ) 
				{
					deutfactor--;
				}
				else if( nebel == 2 ) 
				{
					deutfactor++;
				}

				if( Cargo.getResourceMass( Resources.DEUTERIUM, tmpe * deutfactor ) > (shiptd.getCargo() - shipc.getMass()) ) 
				{
					tmpe = (int)( (shiptd.getCargo()-shipc.getMass())/(deutfactor*Cargo.getResourceMass( Resources.DEUTERIUM, 1 )) );
					this.slog("[maxcargo]");
				}
				int saugdeut = tmpe * deutfactor;

				shipc.addResource( Resources.DEUTERIUM, saugdeut );
				e -= tmpe;
				this.log(tmpe+" Deuterium");
			}
			else 
			{
				this.log("kpn");
			}
		}

		shipd.setEngine(sub[0]);
		shipd.setWeapons(sub[1]);
		shipd.setComm(sub[2]);
		shipd.setSensors(sub[3]);
		shipd.setEnergy(e);
		shipd.setWeaponHeat("");
		shipd.setCargo(shipc);
		
		this.slog("\tNeu: crew "+shipd.getCrew()+" e "+e+" speicher "+shipd.getNahrungCargo()+" status: <");
		this.slog(shipd.getStatus());
		

		this.log(">");
	}

	private void tickUser(org.hibernate.Session db, User auser) 
	{
		Map<Location, List<Base>> feedingBases = new HashMap<Location, List<Base>>();
		ConfigValue value = (ConfigValue)db.get(ConfigValue.class, "corruption");
		double corruption = Double.valueOf(value.getValue()) + auser.getCorruption();

		List<Base> bases = Common.cast(db.createQuery("from Base where owner=? and isfeeding=?")
										 .setEntity(0,auser)
										 .setInteger(1, 1)
										 .list());
		
		for(Base base: bases)
		{
			Location location = base.getLocation();
			if(!feedingBases.containsKey(location))
			{
				feedingBases.put(location, new ArrayList<Base>());
			}
			feedingBases.get(location).add(base);
		}
		
		int balance = auser.getFullBalance()[1];
		
		if(balance > 0 && corruption > 0)
		{
			auser.setKonto(auser.getKonto().subtract(BigInteger.valueOf((long)(balance * corruption))));
		}
		
		// Schiffe berechnen
		ScrollableResults ships = db.createQuery(
				"from Ship as s fetch all properties" +
				" where s.id>0 and s.owner=? " +
				" and system!=0 " + 
				"order by s.shiptype.versorger DESC, " +
				" s.modules.versorger DESC, s.shiptype.jDocks DESC," +
				"s.modules.jDocks DESC,s.shiptype ASC")
				.setEntity(0, auser)
				.scroll(ScrollMode.FORWARD_ONLY);
		
		versorgerlist = getLocationVersorgerList(db, auser);
		
		while(ships.next())
		{
			Ship ship = (Ship)ships.get(0);
			try 
			{
				this.tickShip(db, ship, feedingBases);
			}
			catch( RuntimeException e ) 
			{
				this.log("ship "+ship.getId()+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "SchiffsTick Exception", "ship: "+ship.getId());
			}
			unflushedShips++;
		}
	}

	@Override
	protected void tick() 
	{
		org.hibernate.Session db = getDB();
		Transaction transaction = db.beginTransaction();
		try
		{
			db.createQuery("update Ship set heat=heat-(case when heat>=70 then 70 else heat end) " +
				"where heat>0 and owner in (from User where vaccount=0 or wait4vac>0) and id>0 and system!=0 and battle is null")
				.executeUpdate();
			transaction.commit();
		}
		catch (Exception e) 
		{
			transaction.rollback();
		}
		
		FlushMode flushMode = db.getFlushMode();
		CacheMode cacheMode = db.getCacheMode();
		db.setFlushMode(FlushMode.MANUAL);
		db.setCacheMode(CacheMode.IGNORE);

		transaction = db.beginTransaction();
		List<User> users = Common.cast(db.createQuery("from User where id!=0 and (vaccount=0 or wait4vac>0) order by id asc")
						  .list());
		
		for(User auser: users)
		{
			try 
			{
				this.tickUser(db, auser);
				transaction.commit();
				transaction = getDB().beginTransaction();
			}
			catch( RuntimeException e ) 
			{
				transaction.rollback();
				transaction = getDB().beginTransaction();
				
				this.log("User "+auser.getId()+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "ShipTick Exception", "User: "+auser.getId());
			}
			
			final int SHIP_FLUSH_SIZE = 50;
			if(unflushedShips / SHIP_FLUSH_SIZE > 0)
			{
				db.flush();
				db.clear();
				unflushedShips = 0;
			}
		}
		
		transaction.commit();
		
		db.flush();
		db.setCacheMode(cacheMode);
		db.setFlushMode(flushMode);

		transaction = db.beginTransaction();
		try
		{
			db.createQuery("update Ship set crew=0 where id>0 and crew<0").executeUpdate();
			transaction.commit();
		}
		catch(Exception e)
		{
			transaction.rollback();
			this.log("Shiptick: Resetting of crew to zero failed.");
		}

		/*
			Schiffe mit destroy-tag im status-Feld entfernen
		 */
		this.log("");
		this.log("Zerstoere Schiffe mit 'destroy'-status");

		transaction = db.beginTransaction();
		try
		{
			List<?> ships = db.createQuery("from Ship where id>0 and locate('destroy',status)!=0").list();
			for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) 
			{
				Ship aship = (Ship)iter.next();
				this.log("\tEntferne "+aship.getId());
				aship.destroy();
			}
			transaction.commit();
		}
		catch(Exception e)
		{
			transaction.rollback();
		}

		/*
		 * Schadensnebel
		 */
		this.log("");
		this.log("Behandle Schadensnebel");
		transaction = db.beginTransaction();
		
		try
		{
			List<?> ships = db.createQuery("select s from Ship as s, Nebel as n " +
			"where s.system=n.loc.system and s.x=n.loc.x and s.y=n.loc.y and n.type=6 and (s.owner.vaccount=0 or s.owner.wait4vac>0) and s.docked not like 'l %'").list();
			for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) 
			{
				Ship ship = (Ship)iter.next();
	
				this.log("* "+ship.getId());
				int[] sub = new int[] {ship.getEngine(),ship.getWeapons(),ship.getComm(),ship.getSensors()};
	
				for( int i=0; i < sub.length; i++ ) 
				{
					sub[i] -= 10;
					if( sub[i] < 0 ) 
					{
						sub[i] = 0;
					}
				}
	
				int hull = ship.getHull();
				if( hull > 1 ) 
				{
					hull -= (int)(hull*0.05d);
					if( hull < 1 ) 
					{
						hull = 1;
					}
				}
	
				ship.setEngine(sub[0]);
				ship.setWeapons(sub[1]);
				ship.setComm(sub[2]);
				ship.setSensors(sub[3]);
				ship.setHull(hull);
			}
			transaction.commit();
		}
		catch(Exception e)
		{
			transaction.rollback();
		}
	}
}
