package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MoveableShip {
    private final int id;
    private final int energyCost;
    private final int heatIncrement;
    private final ShipMovementOfficer officer;
    private final int fleet;

    private int engine;
    private int currentEnergy;
    private int currentHeat;

    private final Location startLocation;
    private Location location;


    // stores the effect the officer has for each field for which the flight has been calculated
    private List<ShipMovementOfficer.Reduction> reductions;

    // stores the ships flight data (heat, energy) for each field for which the flight has been calculated
    private List<ShipFlightData> flightData;



    //Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer,Integer
    public MoveableShip(int id, int engine, int currentEnergy, int currentHeat, long energyCost, long heatIncrement, long officerId, long nav, long navu, long ing, long ingu, long spec, int starSystem, int x, int y, long battle, int owner, long fleet)
    {
        this(id, engine, currentEnergy, currentHeat, (int)energyCost, (int)heatIncrement, new ShipMovementOfficer((int)officerId, (int)nav, (int)ing, (int)spec, (int)navu, (int)ingu), new Location(starSystem, x, y), (int)fleet);
    }


    private MoveableShip(int id, int engine, int currentEnergy, int currentHeat, int energyCost, int heatIncrement, ShipMovementOfficer officer, Location location, int fleet)
    {
        this.id = id;

        this.engine = engine;
        this.currentEnergy = currentEnergy;
        this.currentHeat = currentHeat;
        this.energyCost = energyCost;
        this.heatIncrement = heatIncrement;
        this.officer = officer;
        this.location = location;
        this.startLocation = location;
        this.fleet = fleet;
    }

    public int getFleet(){ return fleet; }

    public int computeFlight(Location destination)
    {
        int flyableDistance = 0;
        var distance = destination.getXYDistance(location);
        reductions = officer.computeReductions(distance);
        flightData = new ArrayList<>(distance);
        var path = location.getPathInSystem(destination);

        var heat = currentHeat;
        var energy = currentEnergy;
        var engineDamage = 0;


        for(int i=0;i<distance;i++){
            if(heat >= 100 || energy-Math.max(1, flightCost(i)) <= 0 || engine == engineDamage)
            {
                break;
            }

            flyableDistance = i+1;
            heat += Math.max(heatIncrement - reductions.get(i).heat, 2);
            energy -= Math.max(1, flightCost(i));
            flightData.add(new ShipFlightData(heat, energy, path.get(i))); // location has to be calculated

            // engine damage calculation needs to be calculated here if this method is used for forced flight (flying when heat>=100)
        }

        return flyableDistance;
    }

    public void Fly(int distance)
    {
        officer.Apply(distance);
        var usedFlightData = flightData.get(distance-1);

        this.currentHeat = usedFlightData.getHeat();
        this.currentEnergy = usedFlightData.getEnergy();
        this.location = usedFlightData.getLocation();
    }

    public int flightCost(int field){
        var flightcost = energyCost;

        // Antrieb teilweise beschaedigt?
        if( engine < 20 ) {
            flightcost += 4;
        }
        else if( engine < 40 ) {
            flightcost += 2;
        }
        else if( engine < 60 ) {
            flightcost += 1;
        }

        // officer reduce energy costs?
        if( reductions.get(field).cost > 0 ) {
            flightcost = Math.max(1, flightcost - 2);
        }

        return flightcost;
    }

    public Location getLocation()
    {
        return location;
    }

    public ShipMovementOfficer getOfficer(){ return this.officer; }

    public int getId() {
        return id;
    }
    public int getEnergy(){ return currentEnergy; }
    public int getHeat(){ return currentHeat; }

    public Location getStartLocation() {
        return startLocation;
    }

    public static class ShipMovementOfficer{
        private final int id;
        private int nav;
        private int ing;
        private final int spec;
        private int navu;
        private int ingu;
        private final List<Reduction> reductions;
        private boolean hasChanges = false;

        public ShipMovementOfficer(int id, int nav, int ing, int spec, int navu, int ingu)
        {
            this.id = id;
            this.nav = nav;
            this.ing = ing;
            this.spec = spec;
            this.navu = navu;
            this.ingu = ingu;
            this.reductions = new ArrayList<>();
        }

        public List<Reduction> computeReductions(int fieldsFlown)
        {
            for(int i=0;i<fieldsFlown;i++)
            {
                reductions.add(useAbility(200));
            }

            return reductions;
        }
        public Reduction useAbility(int difficulty ) {
            int heatReduction = 0;
            int energyReduction = 0;
            int newIng = this.ing;
            int newIngU = this.ingu;
            int newNav = this.nav;
            int newNavU = this.navu;

            if(reductions.size() > 0) {
                var lastReduction = reductions.get(reductions.size()-1);
                newIng = lastReduction.ing;
                newIngU = lastReduction.ingu;
                newNav = lastReduction.nav;
                newNavU = lastReduction.navu;
            }

            double fak = difficulty;
            if( this.spec == 3 ) {
                fak *= 0.6;
            }
            if( newIng > fak*(ThreadLocalRandom.current().nextInt(101)/100d) ) {
                heatReduction++;

                if( ThreadLocalRandom.current().nextInt(31) > 10 ) {
                    newIngU++;
                    fak = 2;
                    if( this.spec == 2) {
                        fak = 1;
                    }
                    if( newIngU > newIng * fak ) {
                        //MESSAGE.get().append(Common._plaintitle(this.name)).append(" hat seine Ingeneursf&auml;higkeit verbessert\n");
                        newIng++;
                        newIngU = 0;
                    }
                }
            }

            fak = difficulty;
            if( this.spec == 5 ) {
                fak *= 0.6;
            }
            if( newNav > fak*(ThreadLocalRandom.current().nextInt(101)/100d) ) {
                energyReduction++;

                if( ThreadLocalRandom.current().nextInt(31) > 10 ) {
                    newNavU++;
                    fak = 2;
                    if( this.spec == 2) {
                        fak = 1;
                    }
                    if( newNavU > newNav * fak ) {
                        //MESSAGE.get().append(Common._plaintitle(this.name)).append(" hat seine Navigationsf&auml;higkeit verbessert\n");
                        newNav++;
                        newNavU = 0;
                    }
                }
            }

            /*
            if( count != 0 ) {
                double rangf = (this.ing+this.waf+this.nav+this.sec+this.com)/5.0;
                int rang = (int)(rangf/125);
                if( rang > Offiziere.MAX_RANG ) {
                    rang = Offiziere.MAX_RANG;
                }

                if( rang > this.rang ) {
                    MESSAGE.get().append(this.name).append(" wurde bef&ouml;rdert\n");
                    this.rang = rang;
                }
            }*/

            return new Reduction(heatReduction, energyReduction, newIng, newIngU, newNav, newNavU);
        }

        public void Apply(int fieldsFlown)
        {
            var reduction = reductions.get(fieldsFlown -1);

            if(this.nav != reduction.nav || this.navu != reduction.navu || this.ing != reduction.ing || this.ingu != reduction.ingu) hasChanges = true;

            this.nav = reduction.nav;
            this.navu = reduction.navu;
            this.ing = reduction.ing;
            this.ingu = reduction.ingu;
        }

        public int getNav(){ return nav; }
        public int getNavU(){ return navu; }
        public int getIng(){ return ing; }
        public int getIngU(){ return ingu; }
        public boolean getHasChanges(){ return hasChanges; }

        public int getId() {
            return id;
        }

        public static class Reduction{
            public Reduction(int heat, int cost, int ing, int ingu, int nav, int navu)
            {
                this.heat = heat;
                this.cost = cost;
                this.ing = ing;
                this.ingu = ingu;
                this.nav = nav;
                this.navu = navu;
            }
            public final int heat;
            public final int cost;
            public final int ing;
            public final int ingu;
            public final int nav;
            public final int navu;
        }
    }
    public static class ShipFlightData{
        private final int heat;
        private final int energy;
        private final Location location;
        public ShipFlightData(int heat, int energy, Location location)
        {
            this.heat = heat;
            this.energy = energy;
            this.location = location;
        }

        public int getHeat() {
            return heat;
        }

        public int getEnergy() {
            return energy;
        }

        public Location getLocation() {
            return location;
        }
    }
}
