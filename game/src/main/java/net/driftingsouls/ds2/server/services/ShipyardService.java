package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.items.Schiffsbauplan;
import net.driftingsouls.ds2.server.config.items.Schiffsverbot;
import net.driftingsouls.ds2.server.config.items.effects.IEDraftShip;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.werften.BaseWerft;
import net.driftingsouls.ds2.server.werften.BauinformationenQuelle;
import net.driftingsouls.ds2.server.werften.SchiffBauinformationen;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import net.driftingsouls.ds2.server.werften.WerftKomplex;
import net.driftingsouls.ds2.server.werften.WerftObject;
import net.driftingsouls.ds2.server.werften.WerftQueueEntry;
import net.driftingsouls.ds2.server.werften.WerftTyp;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShipyardService {
    @PersistenceContext
    private EntityManager em;

    private final Rassen races;
    private final CargoService cargoService;

    public ShipyardService(Rassen races, CargoService cargoService) {
        this.races = races;
        this.cargoService = cargoService;
    }

    /**
     * Liefert die Liste aller theoretisch baubaren Schiffe auf dieser Werft.
     * Das vorhanden sein von Resourcen wird hierbei nicht beruecksichtigt.
     * @return array mit Schiffsbaudaten (ships_baubar) sowie
     * 			'_item' => array( ('local' | 'ally'), $resourceid) oder '_item' => false
     * 			zur Bestimmung ob und wenn ja welcher Bauplan benoetigt wird zum bauen
     */
    public @NonNull List<SchiffBauinformationen> getBuildShipList(@NonNull WerftObject werftObject) {
        List<SchiffBauinformationen> result = new ArrayList<>();

        User user = werftObject.getOwner();

        Cargo availablecargo = werftObject.getCargo(false);

        Cargo allyitems;
        if( user.getAlly() != null ) {
            allyitems = new Cargo(Cargo.Type.ITEMSTRING, user.getAlly().getItems());
        }
        else {
            allyitems = new Cargo();
        }

        Map<ShipType,Boolean> disableShips = new HashMap<>();

        for (ItemCargoEntry<Schiffsverbot> anItemlist : availablecargo.getItemsOfType(Schiffsverbot.class))
        {
            disableShips.put(anItemlist.getItem().getSchiffstyp(), true);
        }

        for (ItemCargoEntry<Schiffsverbot> anItemlist : allyitems.getItemsOfType(Schiffsverbot.class))
        {
            disableShips.put(anItemlist.getItem().getSchiffstyp(), true);
        }

        List<ShipBaubar> baubarList = em.createQuery("select sb from ShipBaubar sb WHERE sb.werftSlots <= :werftslots order by sb.type.nickname", ShipBaubar.class)
            .setParameter("werftslots", werftObject.getWerftSlots())
            .getResultList();
        for( Object obj : baubarList )
        {
            ShipBaubar sb = (ShipBaubar)obj;
            if( disableShips.containsKey(sb.getType()) ) {
                continue;
            }
            if( !races.rasse(user.getRace()).isMemberIn(sb.getRace()) ) {
                continue;
            }

            if( sb.getType().getShipClass() == ShipClasses.SCHUTZSCHILD && !(werftObject.getType() == WerftTyp.BASIS)) {
                continue;
            }

            //Forschungen checken
            if( !user.hasResearched(sb.getBenoetigteForschungen()) )
            {
                continue;
            }

            result.add(new SchiffBauinformationen(sb, BauinformationenQuelle.FORSCHUNG, null));
        }

        //Items
        Cargo localcargo = werftObject.getCargo(true);
        for (ItemCargoEntry<Schiffsbauplan> item : localcargo.getItemsOfType(Schiffsbauplan.class))
        {
            IEDraftShip effect = item.getItem().getEffect();

            if (effect.getWerftSlots() > werftObject.getWerftSlots())
            {
                continue;
            }

            //Forschungen checken
            if (!user.hasResearched(effect.getBenoetigteForschungen()) )
            {
                continue;
            }

            result.add(new SchiffBauinformationen(effect.toShipBaubar(), BauinformationenQuelle.LOKALES_ITEM, item.getResourceID()));
        }

        for (ItemCargoEntry<Schiffsbauplan> item : allyitems.getItemsOfType(Schiffsbauplan.class))
        {
            IEDraftShip effect = item.getItem().getEffect();

            if (effect.getWerftSlots() > werftObject.getWerftSlots())
            {
                continue;
            }

            //Forschungen checken
            if (!user.hasResearched(effect.getBenoetigteForschungen()) )
            {
                continue;
            }

            result.add(new SchiffBauinformationen(effect.toShipBaubar(), BauinformationenQuelle.ALLIANZ_ITEM, item.getResourceID()));
        }

        return result;
    }

    public void setCargo(WerftObject shipyard, Cargo cargo, boolean localonly) {
        for (ResourceEntry entry : cargo.getResourceList())
        {
            if( entry.getCount1() < 0 )
            {
                throw new IllegalArgumentException("Der Cargo kann nicht negativ sein ("+entry.getId()+": "+entry.getCount1());
            }
        }

        if(shipyard instanceof BaseWerft) {
            ((BaseWerft) shipyard).getBase().setCargo(cargo);
        } else if(shipyard instanceof ShipWerft) {
            setCargoForOrbitalShipyard((ShipWerft) shipyard, cargo, localonly);
        } else if(shipyard instanceof WerftKomplex) {
            setCargoForShipyardComplex((WerftKomplex) shipyard, cargo, localonly);
        }
    }

    /**
     * Kopiert den Inhalt dieses Bauschlangeneintrags in einen neuen Eintrag einer anderen Werft.
     * Dieser Bauschlangeneintrag wird dabei weder modifiziert noch geloescht.
     * @param targetWerft Die Zielwerft
     * @return Der neue Bauschlangeneintrag
     */
    private WerftQueueEntry copyToWerft(WerftQueueEntry source, WerftObject targetWerft) {
        WerftQueueEntry entry = new WerftQueueEntry(targetWerft, source.getBuilding(), source.getBuildItem(), source.getRemainingTime(), source.getSlots(), getNextEmptyQueuePosition(targetWerft));
        entry.setCostsPerTick(source.getCostsPerTick());
        entry.setEnergyPerTick(source.getEnergyPerTick());
        entry.setBuildFlagschiff(source.isBuildFlagschiff());

        em.persist(entry);

        targetWerft.addQueueEntry(entry);

        return entry;
    }

    public int getNextEmptyQueuePosition(WerftObject werft) {
        Integer position = em.createQuery("select max(wq.position) from WerftQueueEntry as wq where wq.werft.id=:werft", Integer.class)
            .setParameter("werft", werft.getWerftID())
            .getSingleResult();

        if( position == null ) {
            return 1;
        }
        return position+1;
    }

    private void setCargoForOrbitalShipyard(ShipWerft orbitalShipyard, Cargo cargo, boolean localonly) {
        if( (orbitalShipyard.getLinkedBase() != null) && !localonly ) {
            ShipTypeData shiptype = orbitalShipyard.getShip().getTypeData();

            Cargo basecargo = orbitalShipyard.getLinkedBase().getCargo();

            cargo.substractCargo( basecargo );

            ResourceList reslist = cargo.getResourceList();
            for( ResourceEntry res : reslist ) {
                if( res.getCount1() < 0 ) {
                    basecargo.addResource( res.getId(), cargo.getResourceCount( res.getId() ) );
                    cargo.setResource( res.getId(), 0 );
                }
            }

            // Ueberpruefen, ob wir nun zu viel Cargo auf dem Schiff haben
            long cargocount = cargo.getMass();

            if( cargocount > shiptype.getCargo() ) {
                Cargo shipcargo = cargoService.cutCargo(cargo, shiptype.getCargo());
                basecargo.addCargo(cargo);
                cargo = shipcargo;
            }
            orbitalShipyard.getLinkedBase().setCargo(basecargo);
        }

        orbitalShipyard.getShip().setCargo(cargo);
    }

    private void setCargoForShipyardComplex(WerftKomplex shipyardComplex, Cargo cargo, boolean localonly) {
        if( cargo.getMass() > shipyardComplex.getMaxCargo(localonly) ) {
            cargo = cargoService.cutCargo(cargo, shipyardComplex.getMaxCargo(localonly));
        }

        // Zuerst alles gemaess der vorher vorhandenen Verhaeltnisse verteilen
        Cargo oldCargo = shipyardComplex.getCargo(localonly);
        ResourceList reslist = oldCargo.compare(cargo, true);
        for( ResourceEntry res : reslist ) {
            final long oldCount = res.getCount1();
            long count = res.getCount2();

            if( count == oldCount ) {
                continue;
            }

            double factor = 0;
            if( oldCount > 0 ) {
                factor = count/(double)oldCount;
            }

            for (WerftObject aWerften : shipyardComplex.getWerften())
            {
                Cargo werftCargo = aWerften.getCargo(localonly);
                long newCount = (long) (werftCargo.getResourceCount(res.getId()) * factor);

                count -= newCount;
                if (count < 0)
                {
                    newCount += count;
                    count = 0;
                }
                werftCargo.setResource(res.getId(), newCount);
                setCargo(aWerften, werftCargo, localonly);
            }

            // Falls noch Crew uebrig geblieben ist, diese auf die erst beste Werft schicken
            if( count > 0 ) {
                Cargo werftCargo = shipyardComplex.getWerften().get(0).getCargo(localonly);
                werftCargo.addResource(res.getId(), count);
                setCargo(shipyardComplex.getWerften().get(0), werftCargo, localonly);
            }
        }

        // MaxCargo ueberpruefen und Waren ggf umverteilen. Maximal 100 Iterationen
        for( int iteration=100; iteration>0; iteration-- ) {
            Cargo overflow = new Cargo();

            for (WerftObject aWerften : shipyardComplex.getWerften())
            {
                Cargo werftCargo = aWerften.getCargo(localonly);
                if (werftCargo.getMass() > aWerften.getMaxCargo(localonly))
                {
                    Cargo tmp = cargoService.cutCargo(werftCargo, aWerften.getMaxCargo(localonly));
                    overflow.addCargo(werftCargo);
                    setCargo(aWerften, tmp, localonly);
                }
                else if (!overflow.isEmpty())
                {
                    if (overflow.getMass() + werftCargo.getMass() < aWerften.getMaxCargo(localonly))
                    {
                        werftCargo.addCargo(overflow);
                        overflow = new Cargo();
                    }
                    else
                    {
                        werftCargo.addCargo(cargoService.cutCargo(overflow,aWerften.getMaxCargo(localonly) - werftCargo.getMass()));
                    }
                    setCargo(aWerften, werftCargo, localonly);
                }
            }

            // Falls nichts mehr zu verteilen ist abbrechen
            if( overflow.isEmpty() ) {
                break;
            }
        }
    }

    /**
     * Entfernt die Werft aus dem Werftkomplex.
     * Wenn sich die Werft in keinem Komplex befindet wird
     * keinerlei Aktion durchgefuehrt.
     *
     */
    public void removeFromKomplex(WerftObject werftObject) {
        if( werftObject.getLinkedWerft() == null ) {
            return;
        }
        List<WerftObject> werften = werftObject.getLinkedWerft().getMembers();

        if( werften.size() < 3 ) {
            final WerftKomplex komplex = werftObject.getLinkedWerft();

            for (WerftObject aWerften : werften)
            {
                aWerften.setLinkedWerft(null);
            }

            // Die Werftauftraege der groessten Werft zuordnen
            final WerftObject largest = werften.get(0).getWerftSlots() > werften.get(1).getWerftSlots() ? werften.get(0) : werften.get(1);

            List<WerftQueueEntry> entries = komplex.getBuildQueue();
            for (WerftQueueEntry entry : entries)
            {
                // if (entry.getSlots() <= largest.getWerftSlots())
                // {
                //     copyToWerft(entry, largest);
                // }
                // Auch zu grosse Eintraege duerfen mitgenommen werden - diese pausieren bis die Werft wieder Teil eines ausreichend großen Komplexes sind
                copyToWerft(entry, largest);
                komplex.removeQueueEntry(entry);
            }

            em.remove(komplex);
            em.flush();
            largest.rescheduleQueue();
        }
        else {
            WerftKomplex komplex = werftObject.getLinkedWerft();
            werftObject.setLinkedWerft(null);
            komplex.getWerften().remove(werftObject);

            komplex.rescheduleQueue();
        }
    }


    /**
     * <p>Setzt den Bau fort. Dies umfasst u.a. das Dekrementieren
     * der verbleibenden Bauzeit um 1 sowie des Abzugs der pro Tick
     * anfallenden Baukosten.</p>
     * <p>Es wird nicht geprueft, ob die Bedingungen fuer ein fortsetzen des
     * Baus erfuellt sind</p>
     * @see WerftQueueEntry#isBuildContPossible()
     */
    public void continueBuild(WerftQueueEntry entry) {
        decRemainingTime(entry);
        substractBuildCosts(entry);
    }

    public void destroyShipyard(WerftObject werftObject) {
        removeFromKomplex(werftObject);

        if(werftObject instanceof BaseWerft) {
            ((BaseWerft) werftObject).getBase().setWerft(null);
        }

        werftObject.clearQueue();
        em.remove(werftObject);
    }

    /**
     * Gibt den Bauschlangeneintrag mit der angegebenen Position zurueck.
     * @param position Die Position
     * @return Der Bauschlangeneintrag
     */
    public @Nullable
    WerftQueueEntry getBuildQueueEntry(WerftObject object, int position) {
        return em.createQuery("from WerftQueueEntry where werft=:werft and position=:pos", WerftQueueEntry.class)
            .setParameter("werft", object)
            .setParameter("pos", position)
            .getResultStream().findAny().orElse(null);
    }

    private void substractBuildCosts(WerftQueueEntry entry) {
        if( !entry.getCostsPerTick().isEmpty() ) {
            Cargo cargo = entry.getWerft().getCargo(false);
            cargo.substractCargo(entry.getCostsPerTick());
            setCargo(entry.getWerft(), cargo, false);
        }

        if( entry.getEnergyPerTick() != 0 ) {
            entry.getWerft().setEnergy(entry.getWerft().getEnergy() - entry.getEnergyPerTick());
        }
    }

    /**
     * Dekrementiert die verbliebene Bauzeit um 1.
     */
    private void decRemainingTime(WerftQueueEntry entry) {
        if( entry.getRemainingTime() <= 0 ) {
            return;
        }

        entry.setRemainingTime(entry.getRemainingTime()-1);
    }

    /**
     * Ueberprueft, ob noch alle Werften an der selben Position sind. Wenn dies nicht mehr der Fall ist,
     * werden Werften an anderen Positionen entfernt so, dass sich wieder nur Werften an der selben Position
     * befinden.
     *
     */
    public void checkWerftLocations(WerftKomplex shipyardComplex) {
        // Werften in eine Positionsmap eintragen. Die Position mit den meisten Werften merken
        Location maxLocation = null;
        Map<Location,List<WerftObject>> werftPos = new HashMap<>();

        for (WerftObject aWerften : shipyardComplex.getWerften())
        {
            Location loc = aWerften.getLocation();

            if (!werftPos.containsKey(loc))
            {
                werftPos.put(loc, new ArrayList<>());
            }

            werftPos.get(loc).add(aWerften);

            if (maxLocation == null)
            {
                maxLocation = loc;
            }
            else if (!maxLocation.equals(loc))
            {
                if (werftPos.get(maxLocation).size() < werftPos.get(loc).size())
                {
                    maxLocation = loc;
                }
            }
        }

        // Alle Werften, die nicht an der Stelle mit den meisten Werften sind, entfernen
        if( werftPos.keySet().size() != 1 ) {
            for( Map.Entry<Location, List<WerftObject>> entry: werftPos.entrySet() ) {
                Location loc = entry.getKey();
                if( !loc.equals(maxLocation) ) {
                    List<WerftObject> werftListe = entry.getValue();

                    for (WerftObject aWerftListe : werftListe)
                    {
                        removeFromKomplex(aWerftListe);
                    }
                }
            }
        }
    }
}
