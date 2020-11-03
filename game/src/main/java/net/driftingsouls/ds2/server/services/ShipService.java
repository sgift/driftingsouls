package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.ModuleEntry;
import net.driftingsouls.ds2.server.cargo.modules.ModuleType;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipFlag;
import net.driftingsouls.ds2.server.ships.ShipModules;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Service
public class ShipService {
    public static final int MANGEL_TICKS = 9;

    private static final Log log = LogFactory.getLog(ShipService.class);

    @PersistenceContext
    private EntityManager em;

    private final CargoService cargoService;

    public ShipService(CargoService cargoService) {
        this.cargoService = cargoService;
    }

    /**
     * Laesst das Schiff im angegebenen Nebel Deuterium fuer einen bestimmten
     * Energiebetrag sammeln. Falls der Energiebetrag kleiner 0 ist wird
     * die komplette Schiffsenergie fuer das Sammeln verwendet.
     * @param nebel Der Nebel
     * @param energie Der zu verwendende Energiebetrag, falls negativ
     * die gesammte Schiffsenergie
     * @return Die Menge des gesammelten Deuteriums
     */
    public long sammelDeuterium(Ship ship, Nebel nebel, long energie)
    {
        if( nebel == null || !nebel.getLocation().sameSector(0, ship, 0) )
        {
            return 0;
        }
        if( !nebel.getType().isDeuteriumNebel() )
        {
            return 0;
        }
        ShipTypeData type = ship.getTypeData();
        if( ship.getCrew() < (type.getCrew()/2) ) {
            return 0;
        }
        if( type.getDeutFactor() <= 0 )
        {
            return 0;
        }
        if( energie > ship.getEnergy() || energie < 0 )
        {
            energie = ship.getEnergy();
        }
        Cargo shipCargo = ship.getCargo();
        long cargo = cargoService.getMass(shipCargo);

        long deutfactor = type.getDeutFactor();
        deutfactor = nebel.getType().modifiziereDeutFaktor(deutfactor);

        if( (energie * deutfactor)*Cargo.getResourceMass(Resources.DEUTERIUM, 1) > (type.getCargo() - cargo) ) {
            energie = (type.getCargo()-cargo)/(deutfactor*Cargo.getResourceMass( Resources.DEUTERIUM, 1 ));
        }

        long saugdeut = energie * deutfactor;

        if( saugdeut > 0 ) {
            shipCargo.addResource( Resources.DEUTERIUM, saugdeut );
            ship.setEnergy((int)(ship.getEnergy()-energie));
        }
        return saugdeut;
    }

    /**
     * Gibt das Schiff zurueck, an dem dieses Schiff gedockt/gelandet ist.
     * @return Das Schiff oder <code>null</code>
     */
    public Ship getBaseShip(Ship ship) {
        int baseShipId = getBaseShipId(ship);
        if( baseShipId == -1 ) {
            return null;
        }

        return em.find(Ship.class, baseShipId);
    }

    /**
     * Fuegt dem Schiff ein neues Flag hinzu.
     * Wenn das Schiff das Flag bereits hat und die neue verbleibende Zeit groesser ist als die Alte wird sie aktualisiert.
     *
     * @param ship Das Schiff.
     * @param flag Flagcode.
     * @param remaining Wieviele Ticks soll das Flag bestand haben? -1 fuer unendlich.
     */
    public void addFlag(Ship ship, int flag, int remaining)
    {
        ShipFlag oldFlag = ship.getFlag(flag);

        if(oldFlag != null)
        {
            if(oldFlag.getRemaining() != -1 && oldFlag.getRemaining() < remaining)
            {
                oldFlag.setRemaining(remaining);
            }
        }
        else
        {
            ShipFlag shipFlag = new ShipFlag(flag, ship, remaining);

            em.persist(shipFlag);

            ship.getFlags().add(shipFlag);
        }
    }

    public long timeUntilLackOfFood(Ship ship) {
        //Basisschiff beruecksichtigen
        Ship baseShip = getBaseShip(ship);
        if( baseShip != null ) {
            return this.timeUntilLackOfFood(baseShip);
        }

        int foodConsumption = getFoodConsumption(ship);
        if( foodConsumption <= 0 ) {
            return Long.MAX_VALUE;
        }

        Ship versorger = getVersorger(ship);

        //Den Nahrungsverbrauch berechnen - Ist nen Versorger da ists cool
        if( versorger != null || isBaseInSector(ship)) {
            // Sind wir selbst ein Versorger werden wir ja mit berechnet.
            if( (ship.getTypeData().isVersorger() || ship.getBaseType().isVersorger()) && ship.getEinstellungen().isFeeding())
            {
                return getSectorTimeUntilLackOfFood(ship);
            }
            // Ansonsten schmeissen wir noch das drauf was selbst da ist.
            return getSectorTimeUntilLackOfFood(ship) + ship.getNahrungCargo() / foodConsumption;
        }

        // OK muss alles selbst haben *schnueff*
        return ship.getNahrungCargo() / foodConsumption;
    }

    /**
     * Gibt die Anzahl der an externen Docks gedockten Schiffe zurueck.
     * @return Die Anzahl
     */
    public long getDockedCount(Ship ship) {
        if( ship.getTypeData().getADocks() == 0 ) {
            return 0;
        }

        return em.createQuery("select count(*) from Ship where id>0 AND docked=:docked", Long.class)
            .setParameter("docked", Integer.toString(ship.getId()))
            .getSingleResult();
    }

    /**
     * Gibt die Liste aller auf diesem Schiff gelandeten Schiffe zurueck.
     * @return Die Liste der Schiffe
     */
    public List<Ship> getLandedShips(Ship ship)
    {
        if( ship.getTypeData().getJDocks() == 0 )
        {
            return Collections.emptyList();
        }

        return em.createQuery("from Ship where id>0 and docked= :docked", Ship.class)
            .setParameter("docked", "l " + ship.getId())
            .getResultList();
    }

    /**
     * Gibt die Anzahl der gelandeten Schiffe zurueck.
     * @return Die Anzahl
     */
    public long getLandedCount(Ship ship) {
        if( ship.getTypeData().getJDocks() == 0 ) {
            return 0;
        }

        return em.createQuery("select count(*) from Ship where id>0 AND docked=:landed", Long.class)
            .setParameter("landed", "l "+ship.getId())
            .getSingleResult();
    }

    /**
     * Schiffe an/abdocken sowie Jaeger landen/starten. Der Dockvorgang wird mit den Berechtigungen
     * des Schiffsbesitzers ausgefuehrt.
     * Diese Methode sollte der Uebersichtlichkeit halber nur verwendet werden,
     * wenn die Aktion (docken, abdocken, etc.) nicht im Voraus bestimmt werden kann.
     *
     * @param mode Der Dock-Modus (Andocken, Abdocken usw)
     * @param dockships eine Liste mit Schiffen, welche (ab)docken oder landen/starten sollen. Keine Angabe bewirkt das alle Schiffe abgedockt/gestartet werden
     * @return <code>true</code>, falls ein Fehler aufgetreten ist
     */
    public boolean dock(Ship ship, Ship.DockMode mode, Ship ... dockships)
    {
        if(mode == Ship.DockMode.DOCK)
        {
            return dock(ship, dockships);
        }

        if(mode == Ship.DockMode.LAND)
        {
            return land(ship, dockships);
        }

        if(mode == Ship.DockMode.START)
        {
            start(ship, dockships);
        }

        if(mode == Ship.DockMode.UNDOCK)
        {
            undock(ship, dockships);
        }

        return false;
    }

    /**
     * Dockt eine Menge von Schiffen an dieses Schiff an.
     * @param dockships Die anzudockenden Schiffe
     * @return <code>true</code>, falls Fehler aufgetreten sind
     */
    public boolean dock(Ship carrier, Ship... dockships)
    {
        if(dockships == null || dockships.length == 0)
        {
            throw new IllegalArgumentException("Keine Schiffe zum landen gewaehlt.");
        }


        boolean superdock = carrier.getOwner().hasFlag(UserFlag.SUPER_DOCK);
        StringBuilder outputbuffer = Ship.MESSAGE.get();

        Ship[] help = performLandingChecks(superdock, carrier, dockships);
        boolean errors = false;
        if(help.length < dockships.length)
        {
            errors = true;
        }
        dockships = help;

        if(dockships.length == 0)
        {
            return true;
        }

        long dockedShips = getDockedCount(carrier);
        if(!superdock)
        {
            List<Ship> ships = Arrays.stream(dockships)
                .filter(ship -> ship.getTypeData().getSize() <= ShipType.SMALL_SHIP_MAXSIZE)
                .collect(toList());

            if(ships.size() < dockships.length)
            {
                //TODO: Hackversuch - schweigend ignorieren, spaeter loggen
                dockships = ships.toArray(new Ship[0]);
                errors = true;
            }
        }

        if(dockedShips + dockships.length > carrier.getTypeData().getADocks())
        {
            outputbuffer.append("<span style=\"color:red\">Fehler: Nicht gen&uuml;gend freier Andockplatz vorhanden</span><br />\n");

            //Shorten list to max allowed size
            int maxDockShips = carrier.getTypeData().getADocks() - (int)dockedShips;
            if( maxDockShips < 0 ) {
                maxDockShips = 0;
            }
            help = new Ship[maxDockShips];
            System.arraycopy(dockships, 0, help, 0, maxDockShips);
            dockships = help;
        }

        if(dockships.length == 0)
        {
            return errors;
        }

        Cargo cargo = carrier.getCargo();
        final Cargo emptycargo = new Cargo();

        for(Ship aship: dockships)
        {
            aship.setDocked(Integer.toString(carrier.getId()));
            ShipTypeData type = aship.getTypeData();

            if( type.getShipClass() != ShipClasses.CONTAINER )
            {
                continue;
            }

            Cargo dockcargo = aship.getCargo();
            cargo.addCargo( dockcargo );

            if( !dockcargo.isEmpty() )
            {
                aship.setCargo(emptycargo);
            }

            addModule(aship, 0, ModuleType.CONTAINER_SHIP, aship.getId()+"_"+(-type.getCargo()), false);
            addModule(carrier, 0, ModuleType.CONTAINER_SHIP, aship.getId()+"_"+type.getCargo(), false);
        }

        carrier.setCargo(cargo);

        return errors;
    }

    /**
     * Aktualisiert die Schiffswerte nach einer Aenderung an den Typendaten des Schiffs.
     * @param oldshiptype Die Schiffstypendaten des Schiffstyps vor der Modifikation
     * @return Der Cargo, der nun nicht mehr auf das Schiff passt
     */
    public Cargo postUpdateShipType(Ship ship, ShipTypeData oldshiptype) {
        ShipTypeData shiptype = ship.getTypeData();

        if( ship.getHull() != shiptype.getHull() ) {
            ship.setHull(berecheNeuenStatusWertViaDelta(ship.getHull(), oldshiptype.getHull(), shiptype.getHull()));
        }

        if( ship.getShields() != shiptype.getShields() ) {
            ship.setShields(berecheNeuenStatusWertViaDelta(ship.getShields(), oldshiptype.getShields(), shiptype.getShields()));
        }

        if( ship.getAblativeArmor() != shiptype.getAblativeArmor() ) {
            ship.setAblativeArmor(berecheNeuenStatusWertViaDelta(ship.getAblativeArmor(), oldshiptype.getAblativeArmor(), shiptype.getAblativeArmor()));
        }

        if( ship.getAblativeArmor() > shiptype.getAblativeArmor() ) {
            ship.setAblativeArmor(shiptype.getAblativeArmor());
        }

        if( ship.getEnergy() != shiptype.getEps() ) {
            ship.setEnergy(berecheNeuenStatusWertViaDelta(ship.getEnergy(), oldshiptype.getEps(), shiptype.getEps()));
        }

        if( ship.getCrew() != shiptype.getCrew() ) {
            ship.setCrew(berecheNeuenStatusWertViaDelta(ship.getCrew(), oldshiptype.getCrew(), shiptype.getCrew()));
        }

        Cargo cargo = new Cargo();
        if( cargoService.getMass(ship.getCargo()) > shiptype.getCargo() ) {
            Cargo newshipcargo = cargoService.cutCargo(ship.getCargo(), shiptype.getCargo());
            cargo.addCargo( ship.getCargo() );
            ship.setCargo(newshipcargo);
        }

        StringBuilder output = Ship.MESSAGE.get();

        int jdockcount = (int)this.getLandedCount(ship);
        if( jdockcount > shiptype.getJDocks() ) {
            int count = 0;

            // toArray(T[]) fuehrt hier leider zu Warnungen...
            Ship[] undockarray = new Ship[jdockcount-shiptype.getJDocks()];
            for( Ship lship : getLandedShips(ship) ) {
                undockarray[count++] = lship;
                if( count >= undockarray.length )
                {
                    break;
                }
            }

            output.append(jdockcount - shiptype.getJDocks()).append(" gelandete Schiffe wurden gestartet\n");

            start(ship, undockarray);
        }

        int adockcount = (int)this.getDockedCount(ship);
        if( adockcount > shiptype.getADocks() ) {
            int count = 0;

            // toArray(T[]) fuehrt hier leider zu Warnungen...
            Ship[] undockarray = new Ship[adockcount-shiptype.getADocks()];
            for( Ship lship : getDockedShips(ship) ) {
                undockarray[count++] = lship;
                if( count >= undockarray.length )
                {
                    break;
                }
            }

            output.append(adockcount - shiptype.getADocks()).append(" extern gedockte Schiffe wurden abgedockt\n");

            dock(ship, Ship.DockMode.UNDOCK, undockarray);
        }

        if( shiptype.getWerft() == 0 ) {
            em.createQuery("delete from ShipWerft where ship=:ship")
                .setParameter("ship", ship)
                .executeUpdate();
        }
        else {
            Optional<ShipWerft> w = em.createQuery("from ShipWerft where ship=:ship", ShipWerft.class)
                .setParameter("ship", ship)
                .getResultStream().findAny();

            em.persist(w.orElse(new ShipWerft(ship)));
        }

        return cargo;
    }

    /**
     * Jaeger starten. Der Vorgang wird mit den Berechtigungen des Besitzers des Traegers ausgefuehrt.
     * <b>Warnung:</b> Beim starten aller Schiffe werden die Objekte der Session - sofern sie vorhanden sind -
     * momentan nicht aktualisiert.
     *
     * @param dockships Eine Liste mit Schiffen, die starten sollen. Keine Angabe bewirkt das alle Schiffe gestartet werden.
     */
    public void start(Ship carrier, Ship... dockships)
    {
        List<Ship> ships;

        if(dockships == null || dockships.length == 0)
        {
            ships = em.createQuery("from Ship where system=:system and x=:x and y=:y and docked=:docked", Ship.class)
                .setParameter("system", carrier.getSystem())
                .setParameter("x", carrier.getX())
                .setParameter("y", carrier.getY())
                .setParameter("docked", "l " + carrier.getId())
                .getResultList();
        }
        else
        {
            ships = em.createQuery("from Ship s where system=:system and x=:x and y=:y and docked=:docked and s in :dockships", Ship.class)
                .setParameter("dockships", Arrays.asList(dockships))
                .setParameter("system", carrier.getSystem())
                .setParameter("x", carrier.getX())
                .setParameter("y", carrier.getY())
                .setParameter("docked", "l " + carrier.getId())
                .getResultList();
        }

        for( Ship dockship : ships ) {
            dockship.setDocked("");
            dockship.setLocation(carrier);
        }
    }

    /**
     * Jaeger landen.
     * Der Vorgang wird mit den Berechtigungen des Besitzers des Traegers ausgefuehrt.
     *
     * @param dockships Eine Liste mit Schiffen, die landen sollen.
     * @return <code>true</code>, falls ein Fehler aufgetreten ist
     */
    public boolean land(Ship carrier, Ship... dockships)
    {
        if(dockships == null || dockships.length == 0)
        {
            throw new IllegalArgumentException("Keine Schiffe zum landen gewaehlt.");
        }

        StringBuilder outputbuffer = Ship.MESSAGE.get();

        dockships = Arrays.stream(dockships)
            .filter(s -> s.getTypeData().hasFlag(ShipTypeFlag.JAEGER))
            .toArray(Ship[]::new);

        //No superdock for landing
        Ship[] help = performLandingChecks(false, carrier, dockships);
        boolean errors = false;
        if(help.length < dockships.length)
        {
            errors = true;
        }
        dockships = help;

        if(dockships.length == 0)
        {
            return errors;
        }

        List<Ship> ships = Arrays.stream(dockships)
            .filter(ship -> ship.getTypeData().hasFlag(ShipTypeFlag.JAEGER))
            .collect(toList());

        if(ships.size() < dockships.length)
        {
            //TODO: Hackversuch - schweigend ignorieren, spaeter loggen
            dockships = ships.toArray(new Ship[0]);
            errors = true;
        }

        long landedShips = getLandedCount(carrier);
        if(landedShips + dockships.length > carrier.getTypeData().getJDocks())
        {
            outputbuffer.append("<span style=\"color:red\">Fehler: Nicht gen&uuml;gend freier Landepl&auml;tze vorhanden</span><br />\n");

            //Shorten list to max allowed size
            int maxDockShips = carrier.getTypeData().getJDocks() - (int)landedShips;
            if( maxDockShips < 0 )
            {
                maxDockShips = 0;
            }
            help = new Ship[maxDockShips];
            System.arraycopy(dockships, 0, help, 0, maxDockShips);
            dockships = help;
        }

        if(dockships.length == 0)
        {
            return errors;
        }

        em.createQuery("update Ship s set docked=:docked where s in :dockships and battle is null")
            .setParameter("dockships", Arrays.asList(dockships))
            .setParameter("docked", "l "+carrier.getId())
            .executeUpdate();

        // Die Query aktualisiert leider nicht die bereits im Speicher befindlichen Objekte...
        for( Ship ship : dockships ) {
            if ( ship.getBattle() == null ){
                ship.setDocked("l "+carrier.getId());
            }
        }

        return errors;
    }

    /**
     * Schiffe abdocken.
     * Der Vorgang wird mit den Berechtigungen des Besitzers des Traegers ausgefuehrt.
     *
     * @param dockships Eine Liste mit Schiffen, die abgedockt werden sollen. Keine Angabe bewirkt das alle Schiffe abgedockt werden.
     */
    public void undock(Ship carrier, Ship... dockships)
    {
        if( dockships == null || dockships.length == 0 ) {
            List<Ship> dockshipList = getDockedShips(carrier);
            dockships = dockshipList.toArray(new Ship[0]);
        }

        boolean gotmodule = false;
        for( Ship aship : dockships )
        {
            if(getBaseShipId(aship) != carrier.getId())
            {
                //TODO: Hackversuch - schweigend ignorieren, spaeter loggen
                continue;
            }

            aship.setDocked("");

            ShipTypeData type = aship.getTypeData();

            if( type.getShipClass() != ShipClasses.CONTAINER ) {
                continue;
            }
            gotmodule = true;

            ModuleEntry moduleEntry = new ModuleEntry(0, ModuleType.CONTAINER_SHIP, Integer.toString(aship.getId()));
            removeModule(aship, moduleEntry, false );
            removeModule(carrier, moduleEntry, false );
        }

        if( gotmodule )
        {
            Cargo cargo = carrier.getCargo();

            // Schiffstyp neu abholen, da sich der Maxcargo geaendert hat
            ShipTypeData shiptype = carrier.getTypeData();

            Cargo newcargo = cargo;
            if( cargoService.getMass(cargo) > shiptype.getCargo() )
            {
                newcargo = cargoService.cutCargo(cargo, shiptype.getCargo());
            }
            else
            {
                cargo = new Cargo();
            }

            for( int i=0; i < dockships.length && cargoService.getMass(cargo) > 0; i++ )
            {
                Ship aship = dockships[i];
                ShipTypeData ashiptype = aship.getTypeData();

                if( (ashiptype.getShipClass() == ShipClasses.CONTAINER) && (cargoService.getMass(cargo) > 0) )
                {
                    Cargo acargo = cargoService.cutCargo(cargo, ashiptype.getCargo());
                    if( !acargo.isEmpty() )
                    {
                        aship.setCargo(acargo);
                    }
                }
            }
            carrier.setCargo(newcargo);
        }
    }

    /**
     * Entfernt ein Modul aus dem Schiff.
     * @param moduleEntry Der zu entfernende Moduleintrag
     */
    public void removeModule(Ship ship, ModuleEntry moduleEntry ) {
        removeModule(ship, moduleEntry, true);
    }

    private void removeModule(Ship ship, ModuleEntry moduleEntry, boolean validate ) {
        if( ship.getId() < 0 ) {
            throw new UnsupportedOperationException("addModules kann nur bei Schiffen mit positiver ID ausgefuhert werden!");
        }

        if( ship.getModules() == null ) {
            return;
        }

        ShipModules shipModules = ship.getModules();

        List<ModuleEntry> moduletbl = new ArrayList<>(Arrays.asList(ship.getModuleEntries()));

        //check modules

        //rebuild
        ShipTypeData type = ship.getBaseType();

        Map<Integer,String[]>slotlist = new HashMap<>();
        String[] tmpslotlist = StringUtils.split(type.getTypeModules(), ';');
        for (String aTmpslotlist : tmpslotlist)
        {
            String[] aslot = StringUtils.split(aTmpslotlist, ':');
            slotlist.put(Integer.parseInt(aslot[0]), aslot);
        }

        List<Module> moduleobjlist = new ArrayList<>();
        List<String> moduleSlotData = new ArrayList<>();

        for (ModuleEntry module : moduletbl)
        {
            if (module.getModuleType() != null)
            {
                Module moduleobj = module.createModule();

                if (moduleobj.isSame(moduleEntry))
                {
                    continue;
                }

                if ((module.getSlot() > 0) && (slotlist.get(module.getSlot()) != null) && (slotlist.get(module.getSlot()).length > 2)) {
                    moduleobj.setSlotData(slotlist.get(module.getSlot())[2]);
                } else {
                    log.error(String.format("ship: %s - slot: %s - not contained in slot list", ship.getId(), module.getSlot()));
                }
                moduleobjlist.add(moduleobj);

                moduleSlotData.add(module.serialize());
            }
        }

        for( int i=0; i < moduleobjlist.size(); i++ ) {
            type = moduleobjlist.get(i).modifyStats( type, moduleobjlist );
        }

        if( moduleSlotData.size() > 0 ) {
            shipModules.setModules(Common.implode(";",moduleSlotData));
            writeTypeToShipModule(shipModules, type);
        }
        else {
            em.remove(shipModules);

            ship.setModules(null);
        }

        if( validate )
        {
            ueberpruefeGedocktGelandetAnzahl(ship);
        }
    }

    /**
     * Gibt die Typen-Daten des angegebenen Schiffs bzw Schifftyps zurueck.
     * @param shiptype Die ID des Schiffs bzw des Schifftyps
     * @return die Typen-Daten
     */
    public ShipTypeData getShipType( int shiptype ) {
        return em.find(ShipType.class, shiptype);
    }

    /**
     * Gibt die Liste aller an diesem Schiff (extern) gedockten Schiffe zurueck.
     * @return Die Liste der Schiffe
     */
    public List<Ship> getDockedShips(Ship ship)
    {
        return em.createQuery("from Ship where id>0 and docked= :docked", Ship.class)
            .setParameter("docked", Integer.toString(ship.getId()))
            .getResultList();
    }

    /**
     * Berechnet die durch Module verursachten Effekte des Schiffes neu.
     */
    public void recalculateModules(Ship ship) {
        if( ship.getModules() == null ) {
            return;
        }

        ShipModules shipModules = ship.getModules();

        List<ModuleEntry> moduletbl = new ArrayList<>(Arrays.asList(ship.getModuleEntries()));

        //check modules

        //rebuild
        ShipTypeData type = ship.getTypeData();

        Map<Integer,String[]>slotlist = new HashMap<>();
        String[] tmpslotlist = StringUtils.split(type.getTypeModules(), ';');
        for (String aTmpslotlist : tmpslotlist)
        {
            String[] aslot = StringUtils.split(aTmpslotlist, ':');
            slotlist.put(Integer.parseInt(aslot[0]), aslot);
        }

        List<Module> moduleobjlist = new ArrayList<>();
        List<String> moduleSlotData = new ArrayList<>();

        for (ModuleEntry module : moduletbl)
        {
            if (module.getModuleType() != null)
            {
                Module moduleobj = module.createModule();

                if(moduleobj != null) {
                    if ((module.getSlot() > 0) && (slotlist.get(module.getSlot()) != null) && (slotlist.get(module.getSlot()).length > 2)) {
                        moduleobj.setSlotData(slotlist.get(module.getSlot())[2]);
                    } else {
                        log.error(String.format("ship: %s - slot: %s - not contained in slot list", ship.getId(), module.getSlot()));
                    }
                    moduleobjlist.add(moduleobj);

                    moduleSlotData.add(module.serialize());
                } else {
                    log.error(String.format("ship: %s - module type: %s - unable to create module - skipping.", ship.getId(), module.getModuleType()));
                }
            }
        }

        for (Module module : moduleobjlist) {
            type = module.modifyStats( type, moduleobjlist );
        }

        shipModules.setModules(Common.implode(";",moduleSlotData));
        writeTypeToShipModule(shipModules, type);

        ueberpruefeGedocktGelandetAnzahl(ship);
    }

    /**
     * Gibt die Anzahl der auf diesem Schiff gedockten und gelandeten Schiffe zurueck.
     * @return Die Anzahl
     */
    public long getAnzahlGedockterUndGelandeterSchiffe(Ship ship) {
        if( ship.getTypeData().getJDocks() == 0 && ship.getTypeData().getADocks() == 0 ) {
            return 0;
        }

        return em.createQuery("select count(*) from Ship where id>0 AND docked in (:landed,:docked)", Long.class)
            .setParameter("landed", "l " + ship.getId())
            .setParameter("docked", Integer.toString(ship.getId()))
            .getSingleResult();
    }

    /**
     * Gibt die Liste aller an diesem Schiff angedockten und auf diesem gelandeten Schiffe zurueck.
     * @return Die Schiffe
     */
    public List<Ship> getGedockteUndGelandeteSchiffe(Ship ship) {
        if( ship.getTypeData().getJDocks() == 0 && ship.getTypeData().getADocks() == 0 ) {
            return new ArrayList<>();
        }

        return em.createQuery("from Ship where id>0 and docked in (:docked,:landed)", Ship.class)
            .setParameter("docked", Integer.toString(ship.getId()))
            .setParameter("landed", "l "+ship.getId())
            .getResultList();
    }

    /**
     * Calculates the amount of food a ship consumes.
     * The calculation is done with respect to hydros / shiptype.
     * The calculation is done with respect to docked ships
     *
     * @return Amount of food this ship consumes
     */
    public int getFoodConsumption(Ship ship) {
        if(ship.getOwner().hasFlag(UserFlag.NO_FOOD_CONSUMPTION) || ship.isLanded() || ship.isDocked())
        {
            return 0;
        }
        ShipTypeData shiptype = ship.getTypeData();
        int scaledcrew = ship.getScaledCrew();
        int scaledunits = ship.getScaledUnits();
        int dockedcrew = 0;
        int dockedunits = 0;

        if( shiptype.getJDocks() > 0 || shiptype.getADocks() > 0 ) {
            //Angehaengte Schiffe beruecksichtigen
            for (Ship dockedShip : getGedockteUndGelandeteSchiffe(ship))
            {
                dockedcrew += dockedShip.getScaledCrew();
                dockedunits += dockedShip.getScaledUnits();
            }
        }

        return (int)Math.ceil((scaledcrew+dockedcrew)/10.0)+scaledunits+dockedunits;
    }

    /**
     * Fuegt ein Modul in das Schiff ein.
     * @param slot Der Slot, in den das Modul eingebaut werden soll
     * @param moduleid Die Typen-ID des Modultyps
     * @param data Weitere Daten, welche das Modul identifizieren
     */
    public void addModule(Ship ship, int slot, ModuleType moduleid, String data )
    {
        addModule(ship, slot, moduleid, data, true);
    }

    private void addModule(Ship ship, int slot, ModuleType moduleid, String data, boolean validate ) {
        if( ship.getId() < 0 ) {
            throw new UnsupportedOperationException("addModules kann nur bei Schiffen mit positiver ID ausgefuhert werden!");
        }

        ShipModules shipModules = ship.getModules();
        if( shipModules == null ) {
            shipModules = new ShipModules(ship);
            em.persist(shipModules);

            ship.setModules(shipModules);
        }

        List<ModuleEntry> moduletbl = new ArrayList<>(Arrays.asList(ship.getModuleEntries()));

        //check modules

        //rebuild
        moduletbl.add(new ModuleEntry(slot, moduleid, data ));

        ShipTypeData type = ship.getBaseType();

        Map<Integer,String[]> slotlist = new HashMap<>();
        String[] tmpslotlist = StringUtils.splitPreserveAllTokens(type.getTypeModules(), ';');
        for (String aTmpslotlist : tmpslotlist)
        {
            String[] aslot = StringUtils.splitPreserveAllTokens(aTmpslotlist, ':');
            slotlist.put(Integer.parseInt(aslot[0]), aslot);
        }

        List<Module> moduleobjlist = new ArrayList<>();
        List<String> moduleSlotData = new ArrayList<>();

        for (ModuleEntry module : moduletbl)
        {
            if (module.getModuleType() != null)
            {
                Module moduleobj = module.createModule();
                if ((module.getSlot() > 0) && (slotlist.get(module.getSlot()).length > 2))
                {
                    moduleobj.setSlotData(slotlist.get(module.getSlot())[2]);
                }
                moduleobjlist.add(moduleobj);

                moduleSlotData.add(module.serialize());
            }
        }

        for( int i=0; i < moduleobjlist.size(); i++ ) {
            type = moduleobjlist.get(i).modifyStats( type, moduleobjlist );
        }

        shipModules.setModules(Common.implode(";",moduleSlotData));
        writeTypeToShipModule(shipModules, type);

        if( validate )
        {
            ueberpruefeGedocktGelandetAnzahl(ship);
        }
    }

    private int berecheNeuenStatusWertViaDelta(int currentVal, int oldMax, int newMax)
    {
        int delta = newMax - oldMax;
        currentVal += delta;
        if( currentVal > newMax ) {
            currentVal = newMax;
        }
        else if( currentVal < 0 ) {
            currentVal = 0;
        }
        return currentVal;
    }

    private void writeTypeToShipModule(ShipModules shipModules, ShipTypeData type) {
        shipModules.setNickname(type.getNickname());
        shipModules.setPicture(type.getPicture());
        shipModules.setRu(type.getRu());
        shipModules.setRd(type.getRd());
        shipModules.setRa(type.getRa());
        shipModules.setRm(type.getRm());
        shipModules.setEps(type.getEps());
        shipModules.setCost(type.getCost());
        shipModules.setHull(type.getHull());
        shipModules.setPanzerung(type.getPanzerung());
        shipModules.setCargo(type.getCargo());
        shipModules.setNahrungCargo(type.getNahrungCargo());
        shipModules.setHeat(type.getHeat());
        shipModules.setCrew(type.getCrew());
        shipModules.setMaxUnitSize(type.getMaxUnitSize());
        shipModules.setUnitSpace(type.getUnitSpace());
        shipModules.setWeapons(type.getWeapons());
        shipModules.setMaxHeat(type.getMaxHeat());
        shipModules.setTorpedoDef(type.getTorpedoDef());
        shipModules.setShields(type.getShields());
        shipModules.setSize(type.getSize());
        shipModules.setJDocks(type.getJDocks());
        shipModules.setADocks(type.getADocks());
        shipModules.setSensorRange(type.getSensorRange());
        shipModules.setHydro(type.getHydro());
        shipModules.setDeutFactor(type.getDeutFactor());
        shipModules.setReCost(type.getReCost());
        shipModules.setFlags(type.getFlags().stream().map(ShipTypeFlag::getFlag).collect(joining(" ")));
        shipModules.setWerft(type.getWerft());
        shipModules.setOneWayWerft(type.getOneWayWerft());
        shipModules.setAblativeArmor(type.getAblativeArmor());
        shipModules.setSrs(type.hasSrs());
        shipModules.setMinCrew(type.getMinCrew());
        shipModules.setVersorger(type.isVersorger());
        shipModules.setLostInEmpChance(type.getLostInEmpChance());
    }

    private void ueberpruefeGedocktGelandetAnzahl(Ship ship) {
        ShipTypeData type = ship.getTypeData();

        List<Ship> dockshipList = getDockedShips(ship);
        if( dockshipList.size() > type.getADocks() )
        {
            List<Ship> undock = dockshipList.subList(0, dockshipList.size()-type.getADocks());
            undock(ship, undock.toArray(new Ship[0]));
        }

        List<Ship> jdockshipList = getLandedShips(ship);
        if( jdockshipList.size() > type.getJDocks() )
        {
            List<Ship> undock = jdockshipList.subList(0, jdockshipList.size()-type.getJDocks());
            start(ship, undock.toArray(new Ship[0]));
        }
    }

    /**
     * Gibt das erstbeste Schiff im Sektor zurueck, dass als Versorger fungiert und noch Nahrung besitzt.
     * @return Das Schiff
     */
    private Ship getVersorger(Ship ship) {
        return em.createQuery("select s from Ship as s left join s.modules m" +
            " where (s.shiptype.versorger!=false or m.versorger!=false)" +
            " and s.owner=:owner and s.system=:sys and s.x=:x and s.y=:y and s.nahrungcargo > 0 and s.einstellungen.isfeeding != false " +
            "ORDER BY s.nahrungcargo DESC", Ship.class)
            .setParameter("owner", ship.getOwner())
            .setParameter("sys", ship.getSystem())
            .setParameter("x", ship.getX())
            .setParameter("y", ship.getY())
            .setMaxResults(1)
            .getSingleResult();
    }

    private boolean isBaseInSector(Ship ship) {
        List<Base> bases = em.createQuery("from Base where owner=:owner and system=:sys and x=:x and y=:y", Base.class)
            .setParameter("owner", ship.getOwner())
            .setParameter("sys", ship.getSystem())
            .setParameter("x", ship.getX())
            .setParameter("y", ship.getY())
            .getResultList();

        return !bases.isEmpty();
    }

    private long getSectorTimeUntilLackOfFood(Ship ship)
    {
        double unitstofeed = em.createQuery("select sum(e.amount*e.unittype.nahrungcost) from ShipUnitCargoEntry as e where e.schiff.system=:system and e.schiff.x=:x and e.schiff.y=:y and e.schiff.owner = :user", Double.class)
            .setParameter("system", ship.getSystem())
            .setParameter("x", ship.getX())
            .setParameter("y", ship.getY())
            .setParameter("user", ship.getOwner())
            .getSingleResult();

        long crewtofeed = em.createQuery("select sum(crew) from Ship where system=:system and x=:x and y=:y and owner=:user", Long.class)
            .setParameter("system", ship.getSystem())
            .setParameter("x", ship.getX())
            .setParameter("y", ship.getY())
            .setParameter("user", ship.getOwner())
            .getSingleResult();

        long nahrungtofeed = (long)Math.ceil(unitstofeed + crewtofeed/10.0);

        if(nahrungtofeed == 0)
        {
            return Long.MAX_VALUE;
        }

        long versorgernahrung = em.createQuery("select sum(s.nahrungcargo) from Ship as s left join s.modules m " +
            " where (s.shiptype.versorger!=false or m.versorger!=false)" +
            " and s.owner=:user and s.system=:system and s.x=:x and s.y=:y and s.einstellungen.isfeeding != false", Long.class)
            .setParameter("system", ship.getSystem())
            .setParameter("x", ship.getX())
            .setParameter("y", ship.getY())
            .setParameter("user", ship.getOwner())
            .getSingleResult();

        List<Base> bases = em.createQuery("from Base where owner=:user and system=:system and x=:x and y=:y and isfeeding=true", Base.class)
            .setParameter("user", ship.getOwner())
            .setParameter("system", ship.getSystem())
            .setParameter("x", ship.getX())
            .setParameter("y", ship.getY())
            .getResultList();

        long basenahrung = 0;
        for(Base base : bases)
        {
            basenahrung += base.getCargo().getResourceCount(Resources.NAHRUNG);
        }

        return (versorgernahrung+basenahrung) / nahrungtofeed;
    }

    private int getBaseShipId(Ship ship) {
        if( ship.getDocked().isEmpty() ) {
            return -1;
        }

        if( ship.getDocked().charAt(0) == 'l' ) {
            return Integer.parseInt(ship.getDocked().substring(2));
        }

        return Integer.parseInt(ship.getDocked());
    }

    public boolean lackOfFood(Ship ship) {
        long ticks = timeUntilLackOfFood(ship);
        return ticks <= MANGEL_TICKS && ticks >= 0;
    }

    /**
     * Checks, die sowohl fuers landen, als auch fuers andocken durchgefuehrt werden muessen.
     *
     * @param superdock <code>true</code>, falls im Superdock-Modus
     * 			(Keine Ueberpruefung von Groesse/Besitzer) gedockt/gelandet werden soll
     * @param dockships Schiffe auf die geprueft werden soll.
     * @return Die Liste der zu dockenden/landenden Schiffe
     */
    private Ship[] performLandingChecks(boolean superdock, Ship carrier, Ship ... dockships)
    {
        if(dockships.length == 0)
        {
            return dockships;
        }

        //Enforce position
        List<Ship> ships = Arrays.stream(dockships)
            .filter(ship -> ship.getSystem() == carrier.getSystem())
            .filter(ship -> ship.getX() == carrier.getX())
            .filter(ship -> ship.getY() == carrier.getY())
            .collect(toList());

        if(ships.size() < dockships.length)
        {
            //TODO: Hackversuch - schweigend ignorieren, spaeter loggen
            dockships = ships.toArray(new Ship[0]);

            if(dockships.length == 0)
            {
                return dockships;
            }
        }

        //Check already docked
        ships = Arrays.stream(dockships)
            .filter(ship -> ship.getDocked().isBlank())
            .collect(toList());

        if(ships.size() < dockships.length)
        {
            //TODO: Hackversuch - schweigend ignorieren, spaeter loggen
            dockships = ships.toArray(new Ship[0]);

            if(dockships.length == 0)
            {
                return dockships;
            }
        }

        //Enforce owner
        if(!superdock)
        {
            ships = Arrays.stream(dockships)
                .filter(ship -> ship.getOwner().equals(carrier.getOwner()))
                .collect(toList());

            if(ships.size() < dockships.length)
            {
                //TODO: Hackversuch - schweigend ignorieren, spaeter loggen
                dockships = ships.toArray(new Ship[0]);

                if(dockships.length == 0)
                {
                    return dockships;
                }
            }
        }

        return dockships;
    }
}
