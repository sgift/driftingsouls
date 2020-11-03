package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.Factory;
import net.driftingsouls.ds2.server.entities.Forschungszentrum;
import net.driftingsouls.ds2.server.entities.Kaserne;
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

    public BuildingService(PmService pmService, ShipyardService shipyardService) {
        this.pmService = pmService;
        this.shipyardService = shipyardService;

        cleanupHandler = Map.of(
            "net.driftingsouls.ds2.server.bases.DefaultBuilding", this::defaultCleanup,
            "net.driftingsouls.ds2.server.bases.Kommandozentrale", this::commandPostCleanup,
            "net.driftingsouls.ds2.server.bases.Werft", this::shipyardCleanup
        );
    }

    private void shipyardCleanup(Building building, Base base, int field) {
        shipyardService.destroyShipyard(base.getWerft());
    }

    public void cleanup(Building building, Base base, int field) {
        cleanupHandler.getOrDefault(building.getModule(), this::delegatingCleanup).cleanup(building, base, field);
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
}
