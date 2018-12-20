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

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.entities.Feeding;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.framework.db.batch.UnitOfWork;
import net.driftingsouls.ds2.server.ships.SchiffsReKosten;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipFlag;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.units.TransientUnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargo.Crew;
import org.hibernate.CacheMode;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Berechnung des Ticks fuer Schiffe.
 * @author Drifting-Souls Team
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SchiffsTick extends TickController {
	protected static final class ShipNahrungsCargoComparator implements Comparator<Ship>
	{
		@Override
		public int compare(Ship o1, Ship o2)
		{
			long diff = o1.getNahrungCargo()-o2.getNahrungCargo();
			if( diff == 0 ) {
				// Bei gleicher Nahrungsmenge auf Basis der ID sortieren - sonst werden Schiffe aus dem Set eliminiert
				return o1.getId()-o2.getId();
			}
			return diff < 0 ? -1 : 1;
		}
	}

	protected static final class ShipComparator implements Comparator<Ship>
	{
		@Override
		public int compare(Ship ship1, Ship ship2)
		{
			final ShipTypeData typeData0 = ship1.getTypeData();
			final ShipTypeData typeData1 = ship2.getTypeData();
			if( typeData0.isVersorger() != typeData1.isVersorger() ) {
				return typeData0.isVersorger() ? -1 : 1;
			}
			int diff = typeData0.getJDocks()-typeData1.getJDocks();
			if( diff != 0 ) {
				return diff;
			}
			return ship1.getId()-ship2.getId();
		}
	}

	private Map<String,ResourceID> esources;
	private Map<Location,List<Ship>> versorgerlist;

	@Override
	protected void prepare()
	{
		esources = new LinkedHashMap<>();
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
		int crewThatCouldBeFeed;
		final long nahrung = feeder.getNahrungCargo();
		if( crewToFeed*scaleFactor > nahrung )
		{
			crewThatCouldBeFeed = (int)(nahrung/scaleFactor);
			crewToFeed -= crewThatCouldBeFeed;
		}
		else
		{
			crewThatCouldBeFeed = crewToFeed;
			crewToFeed = 0;
		}

		int tmp = (int)Math.ceil(crewThatCouldBeFeed*scaleFactor);
		feeder.setNahrungCargo(nahrung - tmp);
		this.slog(tmp+"@"+feeder.getId()+",");

		return crewToFeed;
	}

	private Map<Location,List<Ship>> getLocationVersorgerList(org.hibernate.Session db,List<Ship> ships, User user)
	{
		Comparator<Ship> comparator = new ShipNahrungsCargoComparator();

		Map<Location,SortedSet<Ship>> versorgerMap = new HashMap<>();
		this.log("Berechne Versorger");

		int versorgerCount = 0;

		for( Ship ship : ships)
		{
			if( ship.getId() <= 0 || ship.getLocation().getSystem() == 0 ||
					!ship.getEinstellungen().isFeeding() ||
					!ship.getTypeData().isVersorger() ||
					ship.getNahrungCargo() <= 0 )
			{
				continue;
			}

			versorgerCount++;
			Location loc = ship.getLocation();
			if(versorgerMap.containsKey(loc))
			{
				versorgerMap.get(loc).add(ship);
			}
			else
			{
				SortedSet<Ship> shiplist = new TreeSet<>(comparator);
				shiplist.add(ship);
				versorgerMap.put(loc, shiplist);
			}
		}

		this.log(versorgerCount+" Versorger gefunden");

		if(user.getAlly() != null)
		{
			this.log("Berechne Allianzversorger");
			List<Ship> allyShips = Common.cast(db.createQuery("select s from Ship as s left join s.modules m " +
					" where s.id>0 and s.owner!=:owner and s.owner.ally=:ally and (s.owner.vaccount=0 or" +
					" s.owner.wait4vac!=0) and s.system!=0 and" +
					" (s.shiptype.versorger=true or m.versorger=true) and" +
					" s.einstellungen.isfeeding=true and s.einstellungen.isallyfeeding=true and s.nahrungcargo>0" +
					" order by s.nahrungcargo DESC")
					.setEntity("owner", user)
					.setEntity("ally", user.getAlly())
					.list());
			this.log(allyShips.size()+" Schiffe gefunden");
			for( Ship ship : allyShips)
			{
				Location loc = ship.getLocation();
				if(versorgerMap.containsKey(loc))
				{
					versorgerMap.get(loc).add(ship);
				}
				else
				{
					SortedSet<Ship> shiplist = new TreeSet<>(comparator);
					shiplist.add(ship);
					versorgerMap.put(loc, shiplist);
				}
			}
		}

		Map<Location,List<Ship>> versorgerList = new HashMap<>();
		for( Map.Entry<Location,SortedSet<Ship>> entry : versorgerMap.entrySet() ) {
			versorgerList.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}

		return versorgerList;
	}

	private Ship getVersorger(Location loc)
	{
		Ship ship = null;
		if(versorgerlist.containsKey(loc))
		{
			ship = versorgerlist.get(loc).get(0);
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

		return ship;
	}

	private void tickShip(org.hibernate.Session db, Ship shipd, Map<Location, List<Base>> feedingBases, SchiffsReKosten schiffsReKosten)
	{
		this.log(shipd.getName()+" ("+shipd.getId()+"):");

		ShipTypeData shiptd = shipd.getTypeData();

		Cargo shipc = shipd.getCargo();

		this.log("\tAlt: crew "+shipd.getCrew()+" e "+shipd.getEnergy() +" nc "+shipd.getNahrungCargo());

		if( shipd.getHeat() > 0 && shipd.getBattle() == null )
		{
			shipd.setHeat(shipd.getHeat()-Math.min(shipd.getHeat(),70));
		}

		berechneNahrungsverbrauch(shipd, shiptd, feedingBases);

		//Damage ships which don't have enough crew
		if( !berechneVerfallWegenCrewmangel(shipd, shiptd) )
		{
			return;
		}

		//Pay sold and maintenance
		berechneSoldUndWartung(db, shipd, shiptd, schiffsReKosten);

		//Berechnung der Energie
		this.log("\tEnergie:");
		int e = shipd.getEnergy();

		if(shiptd.getShipClass() != ShipClasses.GESCHUETZ)
		{
			e -= shipd.getAlertEnergyCost();
			if( e < 0 ) {
				e = 0;
			}
		}


		int[] sub = new int[] {shipd.getEngine(),shipd.getWeapons(),shipd.getComm(),shipd.getSensors()};

		// Schiff bei Bedarf und falls moeglich reparieren
		repairShip(shipd, shiptd, sub);

		db.evict(Offizier.class);

		// Evt. Deuterium sammeln
		e = sammelDeuterium(shipd, shiptd, shipc, e);
		e = abbauenFelsbrocken(shipd, shiptd, shipc, e, db);
		e = produziereEnergie(shipd, shiptd, shipc, e);


		shipd.setEngine(sub[0]);
		shipd.setWeapons(sub[1]);
		shipd.setComm(sub[2]);
		shipd.setSensors(sub[3]);
		shipd.setEnergy(e);
		if( shipd.getBattle() == null )
		{
			shipd.setWeaponHeat(new HashMap<>());
		}
		shipd.setCargo(shipc);

		if( shipd.getCrew() < 0 )
		{
			shipd.setCrew(0);
		}

		shipd.recalculateShipStatus(false);

		this.slog("\tNeu: crew "+shipd.getCrew()+" e "+e+" nc "+shipd.getNahrungCargo()+" : <");
		this.slog(shipd.getStatus());
		this.log(">");
	}

	private int sammelDeuterium(Ship shipd, ShipTypeData shiptd, Cargo shipc, int e)
	{
		if(shipd.getBattle() == null && shipd.getEinstellungen().getAutoDeut() && (shiptd.getDeutFactor() != 0) &&
				(shipd.getCrew() >= shiptd.getCrew()/2) && (e > 0) &&
				(shipc.getMass() < shiptd.getCargo()) )
		{
			this.slog("\tS. Deut: ");
			Nebel.Typ nebel = Nebel.getNebula(shipd.getLocation());

			if( nebel != null && nebel.isDeuteriumNebel() )
			{
				int tmpe = e;

				long deutfactor = shiptd.getDeutFactor();
				deutfactor = nebel.modifiziereDeutFaktor(deutfactor);

				if( Cargo.getResourceMass( Resources.DEUTERIUM, tmpe * deutfactor ) > (shiptd.getCargo() - shipc.getMass()) )
				{
					tmpe = (int)( (shiptd.getCargo()-shipc.getMass())/(deutfactor*Cargo.getResourceMass( Resources.DEUTERIUM, 1 )) );
					this.slog("[maxcargo]");
				}
				long saugdeut = tmpe * deutfactor;

				shipc.addResource( Resources.DEUTERIUM, saugdeut );
				e -= tmpe;
				this.slog(tmpe+" Deuterium\n");
			}
			else
			{
				this.slog("kpn\n");
			}
		}
		return e;
	}

	private int abbauenFelsbrocken(Ship shipd, ShipTypeData shiptd, Cargo shipc, int e, org.hibernate.Session db)
	{
		if(shipd.getBattle() == null && shipd.getEinstellungen().getAutoMine() && e > 0 && shipd.getTypeData().getShipClass() == ShipClasses.MINER)
		{
			this.slog("\tS. Mine\n");

			List<Ship> felsbrockenlist =  Common.cast(db.createQuery("from Ship " +
					"where owner=:owner and x=:x and y=:y and " +
					"system=:system and battle is null)")
					.setInteger("owner", -1)
					.setInteger("x", shipd.getX())
					.setInteger("y", shipd.getY())
					.setInteger("system", shipd.getSystem())
					.list());

			int tmpe = e;
			for (Ship aShip : felsbrockenlist) {
				if(!aShip.hasFlag(Ship.FLAG_RECENTLY_MINED) && aShip.getTypeData().getShipClass() == ShipClasses.FELSBROCKEN && e > 0)
        {
            aShip.addFlag(Ship.FLAG_RECENTLY_MINED, 1);
						int tmphull = aShip.getHull();
						if (tmphull > tmpe){
							tmphull -= tmpe;
							tmpe = 0;
					  }
						else {
							tmpe -= tmphull-1;
							tmphull = 1;
							String status = aShip.recalculateShipStatus();
							if (status.length() > 0){
								if( !status.contains("pluenderbar")){
									status += " pluenderbar";
								}
							}
							else
								status += "pluenderbar";
							aShip.setStatus(status);
						}
						aShip.setHull(tmphull);
						e = tmpe;
        }
			}
		}
		return e;
	}

	private int produziereEnergie(Ship shipd, ShipTypeData shiptd, Cargo shipc, int e)
	{
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
						this.log("\t   kein "+Cargo.getResourceName(resid)+" vorhanden");
					}
				}

				index++;
			}
		}
		return e;
	}

	private void berechneNahrungsverbrauch(Ship shipd,
										   ShipTypeData shiptd, Map<Location, List<Base>> feedingBases)
	{
		//Eigene Basen im selben Sektor
		List<Base> bases = feedingBases.get(shipd.getLocation());
		if(bases == null)
		{
			bases = new ArrayList<>();
		}

		this.slog("\tCrew: ");
		//Crew die noch gefuettert werden muss
		int crewToFeed = shipd.getFoodConsumption();

		//Potentiell teure Berechnungen sparen, wenn wir sowieso nichts zu versorgen haben
		if(crewToFeed <= 0)
		{
			return;
		}
		//Faktor fuer den Verbrauch
		double scaleFactor = shipd.getAlarm().getAlertScaleFactor();

		//Basencargo, VersorgerCargo, Basisschiffcargo, eigener Cargo - Leerfuttern in der Reihenfolge
		for(Base feedingBase: bases)
		{
			crewToFeed = consumeFood(feedingBase, crewToFeed, scaleFactor);
			if(crewToFeed == 0)
			{
				break;
			}
		}

		if( crewToFeed > 0 ) {
			// Unter bestimmten (unklaren) umstaenden kann es zu einer Endlossschleife
			// kommen. Daher vorerst auf 1000 Iterationen beschraenken.
			int maxsteps = 1000;
			Ship versorger = getVersorger(shipd.getLocation());
			while(versorger != null && crewToFeed > 0 && --maxsteps >= 0)
			{
				crewToFeed = consumeFood(versorger ,crewToFeed, scaleFactor);
				versorger = getVersorger(shipd.getLocation());
			}
		}

		//Mein Mutterschiff - relevant bei gedockten Schiffen
		Ship baseShip = shipd.getBaseShip();
		if(baseShip != null)
		{
			crewToFeed = consumeFood(baseShip, crewToFeed, scaleFactor);
		}

		crewToFeed = consumeFood(shipd, crewToFeed, scaleFactor);
		this.slog("\n");

		if( shipd.getNahrungCargo() > shiptd.getNahrungCargo())
		{
			shipd.setNahrungCargo(shiptd.getNahrungCargo());
		}

		//Crew die nicht versorgt werden konnte verhungern lassen
		if(crewToFeed > 0)
		{
			this.log("\tCrew verhungert - ");
			if(crewToFeed >= shipd.getUnits().getNahrung())
			{
				crewToFeed = crewToFeed - (int)Math.ceil(shipd.getUnits().getNahrung());
				shipd.setUnits(new TransientUnitCargo());
				int maxverhungernfactor = new ConfigService().getValue(WellKnownConfigValue.MAX_VERHUNGERN);
				int maxverhungernvalue = (int)Math.ceil(shiptd.getCrew() * (maxverhungernfactor/100.0));
				int crew = shipd.getCrew();
				if( crewToFeed*10 > maxverhungernvalue)
				{
					crew = crew - maxverhungernvalue;
				}
				else
				{
					crew = crew - crewToFeed*10;
				}
				if(crew < 0)
				{
					shipd.setCrew(0);
					this.log("\tGedockte Schiffe verhungern.");
					crew = Math.abs(crew);
					List<Ship> dockedShips = shipd.getLandedShips();
					for (Ship dockShip : dockedShips)
					{
						if (crew > dockShip.getCrew())
						{
							crew -= dockShip.getCrew();
							dockShip.setCrew(0);
						}
						else
						{
							dockShip.setCrew(dockShip.getCrew() - crew);
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
				shipd.getUnits().fleeUnits(crewToFeed);
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
	}

	private boolean berechneVerfallWegenCrewmangel(Ship shipd,
												   ShipTypeData shiptd)
	{
		if( shipd.getBattle() != null )
		{
			return true;
		}
		int crew = shipd.getCrew();
		int minCrew = shiptd.getMinCrew();
		User user = shipd.getOwner();
		this.log("\tCrew " + crew + "("+minCrew+")");
		if(crew < minCrew && !user.hasFlag(UserFlag.NO_HULL_DECAY))
		{
			this.log("\tSchiff hat nicht genug Crew; beschaedige Huelle.");
			double scale = new ConfigService().getValue(WellKnownConfigValue.NO_CREW_HULL_DAMAGE_SCALE);
			double damageFactor = (1.0 - (((double)crew) / ((double)minCrew))) / scale;
			this.log("\tDamage factor is: " + damageFactor);

			int oldArmor = shipd.getAblativeArmor();
			if(oldArmor > 0)
			{
				int damage = (int)Math.ceil(shiptd.getAblativeArmor()*damageFactor);
				int newArmor = oldArmor - damage;
				if(newArmor < 0)
				{
					this.log("\tAblative Panzerung zerstoert.");
					shipd.setAblativeArmor(0);
				}
				else
				{
					this.log("\tAblative Panzerung beschaedigt - neu: " + newArmor);
					shipd.setAblativeArmor(newArmor);
				}
			}
			else
			{
				int damage = (int)Math.ceil(shiptd.getHull() * damageFactor);
				int newHull = shipd.getHull() - damage;
				if(newHull > 0)
				{
					this.log("\tHuelle beschaedigt - neu: " + newHull);
					shipd.setHull(newHull);
				}
				else
				{
					this.log("\tSchiff zerstoert.");
					shipd.setStatus("destroy");
					return false;
				}
			}
		}

		return true;
	}

	private void berechneSoldUndWartung(Session db, Ship shipd, ShipTypeData shiptd, SchiffsReKosten schiffsReKosten)
	{
		if(!new ConfigService().getValue(WellKnownConfigValue.REQUIRE_SHIP_COSTS)) {
			return;
		}

		int reCost = shipd.getBalance();

		if(reCost == 0)
		{
			return;
		}

		User owner = shipd.getOwner();
		if(owner.hasFlag(UserFlag.NO_DESERTEUR) )
		{
			return;
		}

		//Account is balanced
		if(schiffsReKosten.isKostenZuHoch(shipd, owner))
		{
			// Wartungskosten koennen aufgebracht werden.
			if(!schiffsReKosten.isWartungsKostenZuHoch(shipd, owner))
			{
				this.log("\tKonto nicht gedeckt; Besatzung meutert.");

				// Sammel alle Daten zusammmen
				User pirate = (User)db.get(User.class, Faction.PIRATE);
				UnitCargo unitcargo = shipd.getUnits();
				UnitCargo meuterer = unitcargo.getMeuterer(schiffsReKosten.berechneVerbleibendeReOhneSold(shipd, owner));
				Crew dcrew = new UnitCargo.Crew(shipd.getCrew());

				if(meuterer.kapern(unitcargo, new TransientUnitCargo(), new TransientUnitCargo(), dcrew, 1, 1))
				{
					shipd.setCrew(dcrew.getValue());
					shipd.setUnits(meuterer);
					shipd.consign(pirate, false);

					PM.send(pirate, owner.getId(), "Besatzung meutert", "Die Besatzung der " + shipd.getName() + " meutert, nachdem Sie den Sold der Einheiten nicht aufbringen konnten. (" + shipd.getLocation().displayCoordinates(false) + ")");
				}
				else
				{
					shipd.setUnits(unitcargo);
					shipd.setCrew(dcrew.getValue());
					PM.send(pirate, owner.getId(), "Besatzung meutert", "Die Besatzung der " + shipd.getName() + " meutert, nachdem Sie den Sold der Einheiten nicht aufbringen konnten. Die Meuterer wurden vernichtet. (" + shipd.getLocation().displayCoordinates(false) + ")");

					schiffsReKosten.verbucheSchiff(shipd);
				}
			}
			else
			{
				User pirate = (User)db.get(User.class, Faction.PIRATE);
				shipd.consign(pirate, false);

				this.log("\tKonto nicht gedeckt; Schiff desertiert.");
				PM.send(pirate, owner.getId(), "Schiff desertiert", "Die " + shipd.getName() + " ist desertiert, nachdem Sie den Sold der Crew nicht aufbringen konnten. (" + shipd.getLocation().displayCoordinates(false) + ")");
			}
		}
		else {
			schiffsReKosten.verbucheSchiff(shipd);
		}
	}

	private void repairShip(Ship shipd, ShipTypeData shiptd, int[] sub)
	{
		if( shipd.getBattle() != null )
		{
			return;
		}

		if( (!shipd.getStatus().contains("lowmoney")) &&
				( (shipd.getEngine() < 100) || (shipd.getWeapons() < 100) || (shipd.getComm() < 100) || (shipd.getSensors() < 100) ) &&
				(Nebel.getNebula(shipd.getLocation()) != Nebel.Typ.DAMAGE)  ) {

			Offizier offizier = shipd.getOffizier();

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
	}

	protected void tickUser(org.hibernate.Session db, User auser)
	{
		Map<Location, List<Base>> feedingBases = new HashMap<>();

		for(Base base: auser.getBases())
		{
			if( !base.isFeeding() ) {
				continue;
			}
			Location location = base.getLocation();
			if(!feedingBases.containsKey(location))
			{
				feedingBases.put(location, new ArrayList<>());
			}
			feedingBases.get(location).add(base);
		}

		List<Ship> ships = buildSortedShipList(auser);

		versorgerlist = getLocationVersorgerList(db, ships, auser);
		SchiffsReKosten schiffsReKosten = new SchiffsReKosten();

		// Schiffe berechnen
		for( Ship ship : ships )
		{
			if( ship.getId() <= 0 )
			{
				continue;
			}
			if( ship.getSystem() == 0 )
			{
				continue;
			}
			try
			{
				this.tickShip(db, ship, feedingBases, schiffsReKosten);
			}
			catch( RuntimeException e )
			{
				this.log("ship "+ship.getId()+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "SchiffsTick Exception", "ship: "+ship.getId());
			}
		}

		User nobody = (User)db.get(User.class, -1);
		BigInteger gesamtkosten = schiffsReKosten.getGesamtkosten();
		if(auser.getKonto().compareTo(gesamtkosten) >= 0)
		{
			this.log("Kosten: " + gesamtkosten);
			nobody.transferMoneyFrom(auser.getId(), gesamtkosten.longValue());
		}
		else {
			this.log("Kosten uebersteigen Konto ("+auser.getKonto()+")");
			nobody.transferMoneyFrom(auser.getId(), auser.getKonto().longValue());
		}
	}

	private List<Ship> buildSortedShipList(User auser)
	{
		List<Ship> ships = new ArrayList<>(auser.getShips());
		ships.sort(new ShipComparator());
		return ships;
	}

	@Override
	protected void tick()
	{
		org.hibernate.Session db = getDB();

		CacheMode cacheMode = db.getCacheMode();
		db.setCacheMode(CacheMode.IGNORE);

		doShipFlags(db);

		doUsers(db);

		doDestroyStatus(db);

		doSchadensnebel(db);

		db.setCacheMode(cacheMode);
	}

	private void doSchadensnebel(org.hibernate.Session db)
	{
		/*
		 * Schadensnebel
		 */
		this.log("");
		this.log("Behandle Schadensnebel");

		List<Integer> ships = Common.cast(db
				.createQuery("select s.id from Ship as s, Nebel as n " +
						"where s.system=n.loc.system and s.x=n.loc.x and s.y=n.loc.y and " +
						"n.type=6 and (s.owner.vaccount=0 or s.owner.wait4vac>0) and " +
						"s.docked not like 'l %'")
				.list());
		new EvictableUnitOfWork<Integer>("SchiffsTick - Schadensnebel")
		{
			@Override
			public void doWork(Integer shipId) throws Exception
			{
				org.hibernate.Session db = getDB();

				Ship ship = (Ship)db.get(Ship.class, shipId);

				log("* "+ship.getId());
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
		}
		.executeFor(ships);
	}

	private void doDestroyStatus(org.hibernate.Session db)
	{
		/*
			Schiffe mit destroy-tag im status-Feld entfernen
		 */
		this.log("");
		this.log("Zerstoere Schiffe mit 'destroy'-status");

		List<Integer> shipIds = Common.cast(db
				.createQuery("select id from Ship where id>0 and locate('destroy',status)!=0")
				.list());
		new EvictableUnitOfWork<Integer>("SchiffsTick - destroy-status") {
			@Override
			public void doWork(Integer shipId) throws Exception
			{
				org.hibernate.Session db = getDB();

				Ship aship = (Ship)db.get(Ship.class, shipId);
				log("\tEntferne "+aship.getId());
				aship.destroy();
			}

		}
		.setFlushSize(1)
		.executeFor(shipIds);
	}

	private void doUsers(org.hibernate.Session db)
	{
		List<Integer> userIds = Common.cast(db.createQuery("select distinct u.id " +
				"from User u "+
				"where u.id!=0 and (u.vaccount=0 or u.wait4vac>0) order by u.id asc")
				.list());

		new EvictableUnitOfWork<Integer>("SchiffsTick - user")
		{
			@Override
			public void doWork(Integer userId) throws Exception
			{
				org.hibernate.Session db = getDB();

				log("###### User "+userId+" ######");

				User auser = (User)db.createCriteria(User.class)
					.add(Restrictions.idEq(userId))
					.setFetchMode("researches", FetchMode.JOIN)
					.setFetchMode("bases", FetchMode.JOIN)
					.setFetchMode("bases.units", FetchMode.SELECT)
					.setFetchMode("ships", FetchMode.SELECT)
					.setFetchMode("ships.units", FetchMode.SELECT)
					.setFetchMode("ships.modules", FetchMode.JOIN)
					.setFetchMode("ships.einstellungen", FetchMode.JOIN)
					.uniqueResult();

				tickUser(db, auser);
			}
		}
		.setFlushSize(1)
		.executeFor(userIds);
	}

	private void doShipFlags(org.hibernate.Session db)
	{
		List<ShipFlag> flags = Common.cast(db
				.createQuery("from ShipFlag")
				.list());
		new UnitOfWork<ShipFlag>("SchiffsTick - flags")
		{
			@Override
			public void doWork(ShipFlag flag) throws Exception
			{
				org.hibernate.Session db = getDB();

				flag.setRemaining(flag.getRemaining()-1);
				if( flag.getRemaining() <= 0 )
				{
					db.delete(flag);
				}
			}
		}
		.setFlushSize(30)
		.executeFor(flags);
	}
}
