package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.ModuleEntry;
import net.driftingsouls.ds2.server.cargo.modules.ModuleItemModule;
import net.driftingsouls.ds2.server.cargo.modules.ModuleType;
import net.driftingsouls.ds2.server.config.ModuleSlots;
import net.driftingsouls.ds2.server.config.items.IffDeaktivierenItem;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Schiffsmodul;
import net.driftingsouls.ds2.server.config.items.effects.IEModule;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DSObject;
import net.driftingsouls.ds2.server.ships.Alarmstufe;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.werften.ReloadCosts;
import net.driftingsouls.ds2.server.werften.RepairCosts;
import net.driftingsouls.ds2.server.werften.SchiffBauinformationen;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import net.driftingsouls.ds2.server.werften.WerftObject;
import net.driftingsouls.ds2.server.werften.WerftQueueEntry;
import net.driftingsouls.ds2.server.werften.WerftTyp;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.driftingsouls.ds2.server.services.ShipService.MANGEL_TICKS;

/**
 * Actions on ships and status recalculation. Extracted because of circular dependency between ShipService and ShipyardService
 */
@Service
public class ShipActionService {
    private static final Log log = LogFactory.getLog(ShipActionService.class);

    @PersistenceContext
    private EntityManager em;

    private final ShipService shipService;
    private final ShipyardService shipyardService;
    private final ConfigService configService;
    private final CargoService cargoService;

    public ShipActionService(ShipService shipService, ShipyardService shipyardService, ConfigService configService, CargoService cargoService) {
        this.shipService = shipService;
        this.shipyardService = shipyardService;
        this.configService = configService;
        this.cargoService = cargoService;
    }

    /**
     * Repariert ein Schiff auf einer Werft.
     * Es werden nur Dinge geprueft, die unmittelbar mit dem Repariervorgang selbst
     * etwas zu tun haben. Die Positionen von Schiff und Werft usw werden jedoch nicht gecheckt.
     * {@link DSObject#MESSAGE} enthaelt die Hinweistexte
     *
     * @param ship Das Schiff
     * @param testonly Soll nur getestet (true) oder auch wirklich repariert (false) werden?
     *
     * @return true, wenn kein Fehler aufgetreten ist
     */
    public boolean repairShip(@NonNull WerftObject werftObject, @NonNull Ship ship, boolean testonly) {
        if(!ship.getLocation().sameSector(0, werftObject.getLocation(), werftObject.getSize()))
        {
            werftObject.MESSAGE.get().append("Diese Werft befindet sich nicht an der gleichen Position wie das Schiff.");
            return false;
        }
        if(werftObject.isEinwegWerft())
        {
            werftObject.MESSAGE.get().append("Diese Werft ist vollständig auf ihr einziges Bauprojekt konzentriert.");
            return false;
        }
        if(ship.hasFlag(Ship.FLAG_RECENTLY_REPAIRED))
        {
            werftObject.MESSAGE.get().append("Das Schiff wurde k&uuml;rzlich repariert und kann derzeit nicht repariert werden.");
            return false;
        }

        ShipTypeData shiptype = ship.getTypeData();

        Cargo cargo = werftObject.getCargo(false);
        boolean ok = true;
        RepairCosts rc = werftObject.getRepairCosts(ship);


        Cargo newcargo = cargo.clone();
        int newe = werftObject.getEnergy();

        //Kosten ausgeben
        ResourceList reslist = rc.cost.compare( cargo, false );
        for( ResourceEntry res : reslist ) {
            if (res.getDiff() > 0) {
                ok = false;
                break;
            }
        }
        newcargo.substractCargo( rc.cost );

        if( rc.e > 0 ) {
            if( rc.e > newe ) {
                ok = false;
            }
            newe -= rc.e;
        }


        if( !ok ) {
            werftObject.MESSAGE.get().append("Nicht gen&uuml;gend Material zur Reperatur vorhanden");
            return false;
        }

        if( !testonly ) {
            shipyardService.setCargo(werftObject, newcargo, false );

            werftObject.setEnergy(newe);
            ship.setAblativeArmor(shiptype.getAblativeArmor());
            ship.setHull(shiptype.getHull());
            ship.setEngine(100);
            ship.setSensors(100);
            ship.setComm(100);
            ship.setWeapons(100);
            shipService.addFlag(ship, Ship.FLAG_RECENTLY_REPAIRED, 5);

            //TODO: Not the most elegant solution, but it does the job for the moment
            if(werftObject instanceof ShipWerft) {
                recalculateShipStatus(((ShipWerft) werftObject).getShip());
            }
        }

        return true;
    }

    /**
     * Laedt ein Schiff in einer Werft auf.
     * Es werden nur Dinge geprueft, die unmittelbar mit dem Aufladevorgang selbst
     * etwas zu tun haben. Die Positionen von Schiff und Werft usw werden jedoch nicht gecheckt.
     * {@link DSObject#MESSAGE} enthaelt die Hinweistexte
     *
     * @param ship Das Schiff
     * @param testonly Soll nur getestet (true) oder auch wirklich aufgeladen (false) werden?
     *
     * @return true, wenn kein Fehler aufgetreten ist
     */
    public boolean reloadShip(@NonNull WerftObject werftObject, @NonNull Ship ship, boolean testonly) {
        if(!ship.getLocation().sameSector(0, werftObject.getLocation(), werftObject.getSize()))
        {
            werftObject.MESSAGE.get().append("Diese Werft befindet sich nicht an der gleichen Position wie das Schiff.");
            return false;
        }
        if(werftObject.isEinwegWerft())
        {
            werftObject.MESSAGE.get().append("Diese Werft ist vollständig auf ihr einziges Bauprojekt konzentriert.");
            return false;
        }
        if(ship.hasFlag(Ship.FLAG_RECENTLY_REPAIRED))
        {
            werftObject.MESSAGE.get().append("Das Schiff wurde k&uuml;rzlich aufgeladen und kann derzeit nicht aufgeladen werden.");
            return false;
        }

        boolean ok = true;
        ReloadCosts rc = werftObject.getReloadCosts(ship);
        int newe = werftObject.getEnergy();

        if( rc.e > 0 ) {
            if( rc.e > newe ) {
                ok = false;
            }
            newe -= rc.e;
        }


        if( !ok ) {
            werftObject.MESSAGE.get().append("Nicht gen&uuml;gend Energie f&uuml;r den Aufladevorgang vorhanden");
            return false;
        }
        else if( !testonly ) {
            ShipTypeData shiptype = ship.getTypeData();
            werftObject.setEnergy(newe);
            ship.setEnergy(shiptype.getEps());
            shipService.addFlag(ship, Ship.FLAG_RECENTLY_REPAIRED, 5);

            if(werftObject instanceof ShipWerft) {
                recalculateShipStatus(((ShipWerft) werftObject).getShip());
            }
        }
        return true;
    }

    /**
     * Entfernt ein Item-Modul aus einem Schiff. Das Item-Modul
     * befindet sich anschiessend auf der Werft.
     * {@link DSObject#MESSAGE} enthaelt die Hinweistexte
     *
     * @param ship Das Schiff
     * @param slot Modulslot, aus dem das Modul ausgebaut werden soll
     *
     */
    public void removeModule(@NonNull WerftObject werftObject, @NonNull Ship ship, int slot ) {
        if(werftObject.getType() == WerftTyp.EINWEG)
        {
            werftObject.MESSAGE.get().append("Diese Werft ist vollständig auf ihr einziges Bauprojekt konzentriert.");
            return;
        }
        Map<Integer,Integer> usedslots = new HashMap<>();
        ModuleEntry[] modules = ship.getModuleEntries();
        for( int i=0; i < modules.length; i++ ) {
            usedslots.put(modules[i].getSlot(), i);
        }

        if( !usedslots.containsKey(slot) ) {
            werftObject.MESSAGE.get().append(ship.getName()).append(" - Es befindet sich kein Modul in diesem Slot\n");
            return;
        }

        ShipTypeData shiptype = ship.getTypeData();

        String[] aslot = null;

        String[] moduleslots = StringUtils.split(shiptype.getTypeModules(), ';');
        for (String moduleslot : moduleslots)
        {
            String[] data = StringUtils.split(moduleslot, ':');

            if (Integer.parseInt(data[0]) == slot)
            {
                aslot = data;
                break;
            }
        }

        if( aslot == null ) {
            werftObject.MESSAGE.get().append(ship.getName()).append(" - Keinen passenden Slot gefunden\n");
            return;
        }

        ShipTypeData oldshiptype;
        try {
            oldshiptype = (ShipTypeData)shiptype.clone();
        }
        catch( CloneNotSupportedException e ) {
            oldshiptype = shiptype;
        }

        ModuleEntry module = modules[usedslots.get(slot)];
        Module moduleobj = module.createModule();
        if( aslot.length > 2 ) {
            moduleobj.setSlotData(aslot[2]);
        }

        Cargo cargo = werftObject.getCargo(false);

        if( moduleobj instanceof ModuleItemModule) {
            ResourceID itemid = ((ModuleItemModule)moduleobj).getItemID();
            cargo.addResource( itemid, 1 );
        }
        shipService.removeModule(ship, module);

        Cargo transferCargoZurWerft = shipService.postUpdateShipType(ship, oldshiptype);

        if( werftObject.getMaxCargo(false) - cargoService.getMass(cargo) > 0 ) {
            Cargo addwerftcargo = cargoService.cutCargo(transferCargoZurWerft, werftObject.getMaxCargo(false) - cargoService.getMass(cargo) );
            cargo.addCargo( addwerftcargo );

            if( !transferCargoZurWerft.isEmpty() )
            {
                Cargo shipCargo = ship.getCargo();
                shipCargo.addCargo(transferCargoZurWerft);
                ship.setCargo(shipCargo);
            }
        }
        shipyardService.setCargo(werftObject, cargo, false);

        werftObject.MESSAGE.get().append(Ship.MESSAGE.get());
        werftObject.MESSAGE.get().append(ship.getName()).append(" - Modul ausgebaut\n");

        if(werftObject instanceof ShipWerft) {
            recalculateShipStatus(((ShipWerft) werftObject).getShip());
        }
    }

    /**
     * Fuegt einem Schiff ein Item-Modul hinzu. Das Item-Modul
     * muss auf der Werft vorhanden sein.
     * {@link DSObject#MESSAGE} enthaelt die Hinweistexte
     *
     * @param ship Das Schiff
     * @param slot Der Slot, in den das Modul eingebaut werden soll
     * @param itemid Die ID des einzubauenden Item-Moduls
     *
     */
    public void addModule(@NonNull WerftObject werftObject, @NonNull Ship ship, int slot, int itemid ) {
        if(werftObject.isEinwegWerft())
        {
            werftObject.MESSAGE.get().append("Diese Werft ist vollständig auf ihr einziges Bauprojekt konzentriert.");
            return;
        }
        Map<Integer,Integer> usedslots = new HashMap<>();
        ModuleEntry[] modules = ship.getModuleEntries();
        Context context = ContextMap.getContext();

        for( int i=0; i < modules.length; i++ ) {
            usedslots.put(modules[i].getSlot(), i);
        }

        if( usedslots.containsKey(slot) ) {
            werftObject.MESSAGE.get().append(ship.getName()).append(" - Der Slot ist bereits belegt\n");
            return;
        }

        Cargo cargo = werftObject.getCargo(false);
        List<ItemCargoEntry<Schiffsmodul>> itemlist = cargo.getItemsOfType(Schiffsmodul.class);

        ShipTypeData shiptype = ship.getTypeData();

        String[] aslot = null;

        String[] moduleslots = StringUtils.split(shiptype.getTypeModules(), ';');
        for (String moduleslot : moduleslots)
        {
            String[] data = StringUtils.split(moduleslot, ':');

            if (Integer.parseInt(data[0]) == slot)
            {
                aslot = data;
                break;
            }
        }

        if( aslot == null ) {
            werftObject.MESSAGE.get().append(ship.getName()).append(" - Keinen passenden Slot gefunden\n");
            return;
        }

        Item item = em.find(Item.class, itemid);
        if( !ModuleSlots.get().slot(aslot[1]).isMemberIn( ((IEModule)item.getEffect()).getSlots() ) ) {
            werftObject.MESSAGE.get().append(ship.getName()).append(" - Das Item passt nicht in dieses Slot\n");
            return;
        }

        if( item.getAccessLevel() > context.getActiveUser().getAccessLevel() ) {
            werftObject.MESSAGE.get().append(ship.getName()).append(" - Ihre Techniker wissen nichts mit dem Modul anzufangen\n");
            return;
        }

        ItemCargoEntry<Schiffsmodul> myitem = null;

        for (ItemCargoEntry<Schiffsmodul> anItemlist : itemlist)
        {
            if (anItemlist.getItemID() == itemid)
            {
                myitem = anItemlist;
                break;
            }
        }

        if( myitem == null || myitem.getCount() <= 0 ) {
            werftObject.MESSAGE.get().append(ship.getName()).append(" - Kein passendes Item gefunden\n");
            return;
        }

        ShipTypeData oldshiptype;
        try {
            oldshiptype = (ShipTypeData)shiptype.clone();
        }
        catch( CloneNotSupportedException e ) {
            oldshiptype = shiptype;
        }

        shipService.addModule(ship, slot, ModuleType.ITEMMODULE, Integer.toString(itemid) );
        cargo.substractResource( myitem.getResourceID(), 1 );

        Cargo transferCargoZurWerft = shipService.postUpdateShipType(ship, oldshiptype);

        if( werftObject.getMaxCargo(false) - cargoService.getMass(cargo) > 0 ) {
            Cargo addwerftcargo = cargoService.cutCargo(transferCargoZurWerft, werftObject.getMaxCargo(false) - cargoService.getMass(cargo) );
            cargo.addCargo( addwerftcargo );

            if( !transferCargoZurWerft.isEmpty() )
            {
                Cargo shipCargo = ship.getCargo();
                shipCargo.addCargo(transferCargoZurWerft);
                ship.setCargo(shipCargo);
            }
        }
        shipyardService.setCargo(werftObject, cargo,false);

        werftObject.MESSAGE.get().append(Ship.MESSAGE.get());

        werftObject.MESSAGE.get().append(ship.getName()).append(" - Modul eingebaut\n");

        if(werftObject instanceof ShipWerft) {
            recalculateShipStatus(((ShipWerft) werftObject).getShip());
        }
    }

    /**
     * Baut ein Schiff in der Werft auf Basis der angegebenen Schiffbau-ID und der
     * angegebenen Item-ID (Bauplan). {@link DSObject#MESSAGE} enthaelt die Hinweistexte
     *
     * @param build Schiffbau-ID
     * @param item Item-ID
     * @param costsPerTick Sollen die Baukosten pro Tick (<code>true</code>) oder der Gesamtbetrag jetzt (<code>false</code>) abgezogen werden
     * @param testonly Soll nur getestet (true) oder wirklich gebaut (false) werden?
     * @return true, wenn kein Fehler aufgetreten ist
     */
    public boolean buildShip(@NonNull WerftObject werftObject, int build, int item, boolean costsPerTick, boolean testonly )
    {
        SchiffBauinformationen shipdata = werftObject.getShipBuildData( build, item );
        if( shipdata == null )
        {
            werftObject.MESSAGE.get().append("Diese Werft dieses Bauprojekt nicht durchführen");
            return false;
        }
        return buildShip(werftObject, shipdata, costsPerTick, testonly);
    }

    /**
     * Baut ein Schiff in der Werft auf Basis der angegebenen Schiffbau-ID und der
     * angegebenen Item-ID (Bauplan). {@link DSObject#MESSAGE} enthaelt die Hinweistexte
     *
     * @param build Die Schiffsbaudaten
     * @param costsPerTick Sollen die Baukosten pro Tick (<code>true</code>) oder der Gesamtbetrag jetzt (<code>false</code>) abgezogen werden
     * @param testonly Soll nur getestet (true) oder wirklich gebaut (false) werden?
     * @return true, wenn kein Fehler aufgetreten ist
     */
    public boolean buildShip(@NonNull WerftObject werftObject, @NonNull SchiffBauinformationen build, boolean costsPerTick, boolean testonly ) {
        StringBuilder output = werftObject.MESSAGE.get();

        if( werftObject.getType() == WerftTyp.EINWEG )
        {
            output.append("Diese Werft kann nur ein einziges Bauprojekt durchführen");
            return false;
        }

        if( !shipyardService.getBuildShipList(werftObject).contains(build) )
        {
            output.append("Diese Werft dieses Bauprojekt nicht durchführen");
            return false;
        }
        if(build.getBaudaten().getType().getShipClass() == ShipClasses.SCHUTZSCHILD){
            for( Ship ship : werftObject.getOwner().getShips() ){
                if( ship.getTypeData().getShipClass() == ShipClasses.SCHUTZSCHILD){
                    output.append("Diese Station kann nur einmal existieren");
                    return false;
                }
            }

            for( WerftQueueEntry entry : werftObject.getBuildQueue() ){
                if( entry.getBuildShipType().getShipClass() == ShipClasses.SCHUTZSCHILD){
                    output.append("Diese Station kann nur einmal existieren");
                    return false;
                }
            }
        }

        Cargo cargo = new Cargo(werftObject.getCargo(false));

        ShipBaubar shipdata = build.getBaudaten();
        if( (shipdata == null) ) {
            return false;
        }

        //Resourcenbedarf angeben
        boolean ok = true;

        int e = werftObject.getEnergy();

        Cargo shipdataCosts = new Cargo(shipdata.getCosts());

        if( !costsPerTick ) {
            // Abzug der sofort anfallenden Baukosten
            ResourceList reslist = shipdataCosts.compare( cargo, false );
            for( ResourceEntry res : reslist ) {
                if( res.getDiff() > 0 ) {
                    ok = false;
                    break;
                }
            }

            // E-Kosten
            if( shipdata.getEKosten() > werftObject.getEnergy()) {
                ok = false;
            }
        }

        int frei = werftObject.getCrew();

        //Crew
        if (shipdata.getCrew() > frei) {
            ok = false;
        }

        if( !ok ) {
            output.append("Nicht genug Material verf&uuml;gbar");

            return false;
        }
        else if( testonly ) {
            //Auf bestaetigung des "Auftrags" durch den Spieler warten

            return true;
        }
        else {
            frei -= shipdata.getCrew();

            if( !costsPerTick ) {
                cargo.substractCargo( shipdataCosts );
                e -= shipdata.getEKosten();

                shipyardService.setCargo(werftObject, cargo, false);
                werftObject.setEnergy(e);
            }
            werftObject.setCrew(frei);

			/*
				Werftauftrag einstellen
			*/
            ShipType type = shipdata.getType();

            WerftQueueEntry entry = new WerftQueueEntry(werftObject, type, build.getItem() != null ? build.getItem().getItemID() : -1, shipdata.getDauer(), shipdata.getWerftSlots(), shipyardService.getNextEmptyQueuePosition(werftObject));
            entry.setBuildFlagschiff(shipdata.isFlagschiff());
            if( entry.isBuildFlagschiff() ) {
                werftObject.setBuildFlagschiff(true);
            }
            if( costsPerTick ) {
                shipdataCosts.multiply(1/(double)shipdata.getDauer(), Cargo.Round.CEIL);
                entry.setCostsPerTick(shipdataCosts);
                entry.setEnergyPerTick((int)Math.ceil(shipdata.getEKosten()/(double)shipdata.getDauer()));
            }
            em.persist(entry);
            werftObject.getBuildQueue().add(entry);

            werftObject.rescheduleQueue();

            if(werftObject instanceof ShipWerft) {
                ShipWerft shipyard = (ShipWerft)werftObject;
                if( shipyard.getOneWayFlag() != null) {
                    // Einweg-Werft-Code
                    User user = shipyard.getOwner();

                    ShipType newtype = shipyard.getOneWayFlag();

                    String currentTime = Common.getIngameTime(configService.getValue(WellKnownConfigValue.TICKS));
                    String history = "Baubeginn am "+currentTime+" durch "+user.getName()+" ("+user.getId()+")";

                    Ship ship = shipyard.getShip();
                    ship.getHistory().addHistory(history);
                    ship.setName("Baustelle");
                    ship.setBaseType(newtype);
                    ship.setHull(newtype.getHull());
                    ship.setAblativeArmor(newtype.getAblativeArmor());
                    ship.setCrew(newtype.getCrew());
                    ship.setEnergy(newtype.getEps());
                    ship.setEnergy(newtype.getEps());
                    ship.setOwner(user);
                    shipService.recalculateModules(ship);

                    shipyard.setType(WerftTyp.EINWEG);
                }
                recalculateShipStatus(shipyard.getShip());
            }

            return true;
        }
    }

    /**
     * Berechnet das Status-Feld des Schiffes neu. Diese Aktion sollte nach jeder
     * Operation angewendet werden, die das Schiff in irgendeiner Weise veraendert.
     * @return der neue Status-String
     */
    public String recalculateShipStatus(Ship ship) {
        return this.recalculateShipStatus(ship, true);
    }

    /**
     * Berechnet das Status-Feld des Schiffes neu. Diese Aktion sollte nach jeder
     * Operation angewendet werden, die das Schiff in irgendeiner Weise veraendert.
     * Die Berechnung des Nahrungsverbrauchs ist dabei optional.
     * @param nahrungPruefen <code>true</code>, falls der Nahrungsverbrauch geprueft werden soll
     * @return der neue Status-String
     */
    public String recalculateShipStatus(Ship ship, boolean nahrungPruefen) {
        if( ship.getId() < 0 ) {
            throw new UnsupportedOperationException("recalculateShipStatus kann nur bei Schiffen mit positiver ID ausgefuhert werden!");
        }

        ShipTypeData type = ship.getTypeData();

        Cargo cargo = ship.getCargo();

        List<String> status = new ArrayList<>();

        // Alten Status lesen und ggf Elemente uebernehmen
        String[] oldstatus = StringUtils.split(ship.getStatus(), ' ');

        if( oldstatus.length > 0 ) {
            for (String astatus : oldstatus)
            {
                // Aktuelle Statusflags rausfiltern
                if ("disable_iff".equals(astatus) ||
                    "mangel_reaktor".equals(astatus) ||
                    "offizier".equals(astatus) ||
                    "nocrew".equals(astatus))
                {
                    continue;
                }

                if (nahrungPruefen && "mangel_nahrung".equals(astatus))
                {
                    continue;
                }
                status.add(astatus);
            }
        }

        // Treibstoffverbrauch berechnen
        if( type.getRm() > 0 ) {
            long ep = cargo.getResourceCount( Resources.URAN ) * type.getRu() +
                cargo.getResourceCount( Resources.DEUTERIUM ) * type.getRd() +
                cargo.getResourceCount( Resources.ANTIMATERIE ) * type.getRa();
            long er = ep/type.getRm();

            int turns = 2;
            if( (ship.getAlarm() != Alarmstufe.GREEN) && (type.getShipClass() != ShipClasses.GESCHUETZ) ) {
                turns = 4;
            }

            if( er <= MANGEL_TICKS/turns ) {
                status.add("mangel_reaktor");
            }
        }

        // Die Items nach IFF und Hydros durchsuchen

        if( cargo.getItemOfType(IffDeaktivierenItem.class) != null ) {
            status.add("disable_iff");
        }

        // Ist ein Offizier an Bord?
        Offizier offi = ship.getOffizier();
        if( offi != null ) {
            status.add("offizier");
        }

        if( nahrungPruefen && shipService.lackOfFood(ship) ) {
            status.add("mangel_nahrung");
        }

        ship.setStatus(Common.implode(" ", status));

        // Ueberpruefen, ob ein evt vorhandener Werftkomplex nicht exisitert.
        // Oder ein Link zu einem Asteroiden resettet werden muss.
        if( type.getWerft() != 0 ) {
            ShipWerft werft = em.createQuery("from ShipWerft where ship=:ship", ShipWerft.class)
                .setParameter("ship", this)
                .getResultStream().findAny().orElse(null);

            if(werft != null) {
                if (werft.getKomplex() != null) {
                    shipyardService.checkWerftLocations(werft.getKomplex());
                }
                if(werft.isLinked()) {
                    if(!werft.getLinkedBase().getLocation().sameSector(werft.getLinkedBase().getSize(), ship.getLocation(), 0)) {
                        werft.resetLink();
                    }
                }
            }
            else
            {
                log.error("Das Schiff "+ship.getId()+" besitzt keinen Werfteintrag");
            }
        }

        return ship.getStatus();
    }
}
