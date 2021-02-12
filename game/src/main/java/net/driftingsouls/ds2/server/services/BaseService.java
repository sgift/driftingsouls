package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.bases.Fabrik;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@Service
public class BaseService {
    @PersistenceContext
    private EntityManager em;

    private final ShipActionService shipActionService;
    private final BuildingService buildingService;
    private final CargoService cargoService;

    public BaseService(ShipActionService shipActionService, BuildingService buildingService, CargoService cargoService) {
        this.shipActionService = shipActionService;
        this.buildingService = buildingService;
        this.cargoService = cargoService;
    }

    /**
     * Gibt das userspezifische Bild der Basis zurueck. Falls es kein spezielles Bild
     * fuer den angegebenen Benutzer gibt wird <code>null</code> zurueckgegeben.
     *
     * @param base Die Basis für die ein Bild erstellt werden soll.
     * @param location Koordinate fuer die das Bild der Basis ermittelt werden soll.
     * @param user Aktueller Spieler.
     * @param scanned <code>true</code>, wenn die Basis derzeit von einem Schiff des Spielers gescannt werden kann.
     * @return Der Bildstring der Basis oder <code>null</code>
     */
    public String getOverlayImage(Base base, Location location, User user, UserService.Relations relations, boolean scanned)
    {
        if(!location.sameSector(0, base.getLocation(), base.getSize()))
        {
            return null;
        }

        User nobody = em.find(User.class, -1);
        User zero = em.find(User.class, 0);

        if(base.getSize() > 0)
        {
            return null;
        }
        else if(base.getOwner().getId() == user.getId())
        {
            return "data/starmap/asti_own/asti_own.png";
        }
        else if(((base.getOwner().getId() != 0) && (user.getAlly() != null) && (base.getOwner().getAlly() == user.getAlly()) && user.getAlly().getShowAstis()) ||
            relations.isOnly(base.getOwner(), User.Relation.FRIEND))
        {
            return "data/starmap/asti_ally/asti_ally.png";
        }
        else if(scanned && !base.getOwner().equals(nobody) && !base.getOwner().equals(zero))
        {
            return "data/starmap/asti_enemy/asti_enemy.png";
        }
        else
        {
            return null;
        }
    }

    /**
     * Enforces the maximum cargo rules.
     *
     * @param state Der Status der Basis
     * @return The money for resource sales.
     */
    public boolean clearOverfullCargo(Base base, BaseStatus state)
    {
        long maxCargo = base.getMaxCargo();
        Cargo cargo = base.getCargo();
        long surplus = cargoService.getMass(cargo) - maxCargo;

        if(surplus > 0)
        {
            ResourceList production = state.getProduction().getResourceList();
            production.sortByCargo(true);
            for(ResourceEntry resource: production)
            {
                //Only sell produced resources, not consumed
                long resourceCount = resource.getCount1();
                if(resourceCount < 0)
                {
                    continue;
                }

                long productionMass = Cargo.getResourceMass(resource.getId(), resourceCount);
                if(productionMass == 0)
                {
                    continue;
                }

                //Remove only as much as needed, not more
                long toSell;
                if(productionMass <= surplus)
                {
                    toSell = resourceCount;
                }
                else
                {
                    long resourceMass = Cargo.getResourceMass(resource.getId(), 1);
                    toSell = (long)Math.ceil((double)surplus/(double)resourceMass);
                }

                cargo.substractResource(resource.getId(), toSell);
                surplus = cargoService.getMass(cargo) - maxCargo;

                if(cargoService.getMass(cargo) <= maxCargo)
                {
                    return true;
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Transfers crew from the asteroid to a ship.
     *
     * @param ship Ship that gets the crew.
     * @param amount People that should be transfered.
     * @return People that where transfered.
     */
    public int transferCrew(Base base, Ship ship, int amount)
    {
        //Check ship position
        if( !base.getLocation().sameSector(base.getSize(), ship, 0))
        {
            return 0;
        }

        //Only unemployed people can be transferred, when there is enough space on the ship
        int maxAmount = ship.getTypeData().getCrew() - ship.getCrew();
        int unemployed = Math.max(base.getBewohner() - base.getArbeiter(), 0);
        amount = Math.min(amount, maxAmount);
        amount = Math.min(amount, unemployed);

        base.setBewohner(base.getBewohner() - amount);
        ship.setCrew(ship.getCrew() + amount);
        shipActionService.recalculateShipStatus(ship);

        return amount;
    }

    /**
     * Generiert den aktuellen Verbrauch/Produktion-Status einer Basis.
     * @param base die Basis
     * @return der aktuelle Verbrauchs/Produktions-Status
     */
    public BaseStatus getStatus( Base base )
    {

        Fabrik.ContextVars vars = ContextMap.getContext().get(Fabrik.ContextVars.class);
        vars.clear();

        Cargo stat = new Cargo();
        Cargo prodstat = new Cargo();
        Cargo constat = new Cargo();

        int e = 0;
        int arbeiter = 0;
        int bewohner = 0;
        Map<Integer,Integer> buildinglocs = new TreeMap<>();

        if( (base.getCore() != null) && base.isCoreActive() ) {
            Core core = base.getCore();

            stat.substractCargo(core.getConsumes());
            constat.addCargo(core.getConsumes());
            stat.addCargo(core.getProduces());
            prodstat.addCargo(core.getProduces());

            e = e - core.getEVerbrauch() + core.getEProduktion();
            arbeiter += core.getArbeiter();
            bewohner += core.getBewohner();
        }

        Integer[] bebauung = base.getBebauung();
        Integer[] bebon = base.getActive();

        for( int o=0; o < base.getWidth() * base.getHeight(); o++ )
        {
            if( bebauung[o] == 0 )
            {
                continue;
            }

            Building building = buildingService.getBuilding(bebauung[o]);

            if( !buildinglocs.containsKey(building.getId()) ) {
                buildinglocs.put(building.getId(), o);
            }

            bebon[o] = building.isActive( base, bebon[o], o ) ? 1 : 0;

            if( bebon[o] == 0 )
            {
                continue;
            }

            building.modifyStats( base, stat, bebauung[o] );
            building.modifyProductionStats(base, prodstat, bebauung[o]);
            building.modifyConsumptionStats(base, constat, bebauung[o]);

            stat.substractCargo(building.getConsumes());
            constat.addCargo(building.getConsumes());
            stat.addCargo(building.getAllProduces());
            prodstat.addCargo(building.getProduces());

            e = e - building.getEVerbrauch() + building.getEProduktion();
            arbeiter += building.getArbeiter();
            bewohner += building.getBewohner();
        }

        // Nahrung nicht mit in constat rein. Dies wird im Tick benutzt, der betrachtet Nahrungsverbrauch aber separat.
        stat.substractResource( Resources.NAHRUNG, (long)Math.ceil(base.getBewohner()/10.0) );
        stat.substractResource( Resources.NAHRUNG, base.getUnits().getNahrung() );
        // RE nicht mit in constat rein. Dies wird im Tick benutzt, der betrachtet RE-Verbrauch aber separat.
        stat.substractResource( Resources.RE, base.getUnits().getRE() );

        return new BaseStatus(stat, prodstat, constat, e, bewohner, arbeiter, Collections.unmodifiableMap(buildinglocs), bebon);
    }

    /**
     * @return Die Bilanz der Basis.
     */
    public long getBalance(Base base)
    {
        BaseStatus status = getStatus(base );

        Cargo produktion = status.getProduction();

        return produktion.getResourceCount( Resources.RE );
    }

    /**
     * @return Die Nahrungsbilanz der Basis.
     */
    public long getFoodBalance(Base base)
    {
        BaseStatus status = getStatus(base );

        Cargo produktion = status.getProduction();

        return produktion.getResourceCount( Resources.NAHRUNG );
    }
}
