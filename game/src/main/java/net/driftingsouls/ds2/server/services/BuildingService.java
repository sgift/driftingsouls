package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.Factory;
import net.driftingsouls.ds2.server.entities.Forschungszentrum;
import net.driftingsouls.ds2.server.entities.Kaserne;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.werften.BaseWerft;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class BuildingService {
    @PersistenceContext
    private EntityManager em;

    private final PmService pmService;
    private final ShipyardService shipyardService;
    private final Map<String, CleanupFunction> cleanupHandler;
    private final Map<String, BuildFunction> buildHandler;

    public BuildingService(PmService pmService, ShipyardService shipyardService) {
        this.pmService = pmService;
        this.shipyardService = shipyardService;

        buildHandler = Map.of(
            "net.driftingsouls.ds2.server.bases.DefaultBuilding", this::defaultBuild,
            "net.driftingsouls.ds2.server.bases.AcademyBuilding", this::academyBuild,
            "net.driftingsouls.ds2.server.bases.Fabrik", this::factoryBuild,
            "net.driftingsouls.ds2.server.bases.Werft", this::shipyardBuild
        );

        cleanupHandler = Map.of(
            "net.driftingsouls.ds2.server.bases.DefaultBuilding", this::defaultCleanup,
            "net.driftingsouls.ds2.server.bases.Kommandozentrale", this::commandPostCleanup,
            "net.driftingsouls.ds2.server.bases.Werft", this::shipyardCleanup,
            "net.driftingsouls.ds2.server.bases.AcademyBuilding", this::academyCleanup
        );
    }

    public void build(Base base, Building building) {
        buildHandler.getOrDefault(building.getModule(), this::delegatingBuild).build(base, building);
    }

    public void cleanup(Building building, Base base, int field) {
        cleanupHandler.getOrDefault(building.getModule(), this::delegatingCleanup).cleanup(building, base, field);
    }

    /**
     * Gibt eine Instanz der Gebaudeklasse des angegebenen Gebaeudetyps zurueck.
     * Sollte kein passender Gebaeudetyp existieren, wird <code>null</code> zurueckgegeben.
     *
     * @param id Die ID des Gebaudetyps
     * @return Eine Instanz der zugehoerigen Gebaeudeklasse
     */
    public Building getBuilding(int id) {
        return em.find(Building.class, id);
    }

    private void defaultBuild(Base base, Building building) {
        //Nothing todo
    }

    private void academyBuild(Base base, Building building) {
        Academy academy = new Academy(base);
        em.persist(academy);
        base.setAcademy(academy);
    }

    private void factoryBuild(Base base, Building building) {
        Factory wf = loadFactoryEntity(base, building.getId());

        if (wf != null)
        {
            wf.setCount(wf.getCount() + 1);
        }
        else
        {
            wf = new Factory(base, base.getId());
            em.persist(wf);
        }
    }

    private void shipyardBuild(Base base, Building building) {
        if( base.getWerft() == null )
        {
            return;
        }

        BaseWerft werft = new BaseWerft(base);
        em.persist(werft);
        base.setWerft(werft);
    }

    private Factory loadFactoryEntity(Base base, int buildingid)
    {
        return base.getFactories().stream()
            .filter(factory -> factory.getBuildingID() == buildingid)
            .findAny()
            .orElse(null);
    }

    private void delegatingBuild(Base base, Building building) {
        building.build(base, building);
    }

    private void academyCleanup(Building building, Base base, int field) {
        Academy academy = base.getAcademy();
        if( academy != null )
        {
            // Bereinige Queue Eintraege
            academy.getQueueEntries().clear();

            base.setAcademy(null);
            em.remove(academy);
        }


        for( Offizier offizier : Offizier.getOffiziereByDest(base) )
        {
            offizier.setTraining(false);
        }
    }

    private void shipyardCleanup(Building building, Base base, int field) {
        shipyardService.destroyShipyard(base.getWerft());
    }

    private void defaultCleanup(Building building, Base base, int buildingId) {
        //Nothing to do
    }

    private void delegatingCleanup(Building building, Base base, int buildingId) {
        building.cleanup(ContextMap.getContext(), base, buildingId);
    }

    private void commandPostCleanup(Building building, Base base, int buildingId) {
        // Loesche alle GTU-Aktionen
        base.setAutoGTUActs(List.of());
        // Setze Besitzer auf 0
        User nullUser = em.find(User.class, 0);
        User oldUser = base.getOwner();
        base.setOwner(nullUser);
        // Fahre Basis runter
        Integer[] active = base.getActive();
        Arrays.fill(active, 0);

        base.setActive(active);
        base.setCoreActive(false);

        // Loesche Forschung
        Forschungszentrum zentrum = base.getForschungszentrum();

        if(zentrum != null)
        {
            zentrum.setForschung(null);
            zentrum.setDauer(0);
        }

        // Ueberstelle Offiziere
        em.createQuery("update Offizier set owner=:owner where stationiertAufBasis=:dest")
            .setParameter("owner", nullUser)
            .setParameter("dest", base)
            .executeUpdate();

        // Loesche Verbindungen
        em.createQuery("update ShipWerft set linked=null where linked=:base")
            .setParameter("base", base)
            .executeUpdate();

        // Loesche Eintraege der Basiswerft
        BaseWerft werft = base.getWerft();

        if( werft != null)
        {
            werft.clearQueue();
        }

        // Fabriken abschalten
        Set<Factory> factories = base.getFactories();

        for(Factory factory : factories)
        {
            factory.setProduces(new Factory.Task[0]);
        }

        // Auftraege der Kaserne loeschen
        Optional<Kaserne> kaserne = em.createQuery("from Kaserne where base=:base", Kaserne.class)
            .setParameter("base", base)
            .getResultStream().findAny();

        kaserne.stream().map(Kaserne::getQueueEntries).forEach(em::remove);

        // Auftraege der Akademie loeschen
        Academy academy = base.getAcademy();

        if( academy != null)
        {
            academy.getQueueEntries().clear();
        }

        //Check if we need to change the drop zone of the player to another system
        Set<Integer> systems = oldUser.getAstiSystems();

        if(!systems.contains(oldUser.getGtuDropZone()))
        {
            int defaultDropZone = new ConfigService().getValue(WellKnownConfigValue.GTU_DEFAULT_DROPZONE);
            if(oldUser.getGtuDropZone() != defaultDropZone)
            {
                pmService.send(nullUser, oldUser.getId(), "GTU Dropzone geändert.", "Sie haben Ihren letzten Asteroiden in System "+ oldUser.getGtuDropZone() +" aufgegeben. Ihre GTU-Dropzone wurde auf System "+ defaultDropZone +" gesetzt.");
                oldUser.setGtuDropZone(defaultDropZone);
            }
        }
    }

    @FunctionalInterface
    private interface CleanupFunction {
        void cleanup(Building building, Base base, int buildingId);
    }

    private interface BuildFunction {
        void build(Base base, Building building);
    }
}
