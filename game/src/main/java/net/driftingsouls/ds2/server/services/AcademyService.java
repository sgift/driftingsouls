package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.bases.AcademyQueueEntry;
import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AcademyService {
    @PersistenceContext
    private EntityManager em;

    private final UserService userService;

    public AcademyService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Beendet den Trainingsprozess dieses Bauschlangeneintrags erfolgreich.
     *
     * @return <code>true</code> wenn erfolgreich, ansonsten <code>false</code>
     */
    public boolean finishBuildProcess(AcademyQueueEntry academyQueueEntry) {
        academyQueueEntry.MESSAGE.get().setLength(0);

        Context context = ContextMap.getContext();

        if( !academyQueueEntry.isScheduled() ) {
            return false;
        }

        // Speichere alle wichtigen Daten
        int offizier = academyQueueEntry.getTraining();
        int training = academyQueueEntry.getTrainingType();
        User owner = academyQueueEntry.getAcademy().getBase().getOwner();
        int position = academyQueueEntry.getPosition();

        // Loesche Eintrag und berechne Queue neu
        em.remove(this);
        academyQueueEntry.getAcademy().getQueueEntries().remove(academyQueueEntry);

        for(AcademyQueueEntry entry: academyQueueEntry.getAcademy().getQueueEntries())
        {
            if( entry.getPosition() > position )
            {
                entry.setPosition(entry.getPosition() - 1);
            }
        }
        em.flush();
        academyQueueEntry.getAcademy().rescheduleQueue();

        if(training == 0)
        {
            /*
             * Neuer Offizier wurde ausgebildet
             */
            String offiname = userService.getPersonenNamenGenerator(owner).generiere();

            Offizier offz = new Offizier(owner, offiname);

            if( !Offiziere.LIST.containsKey(-offizier) ) {
                offizier = -Offiziere.LIST.keySet().iterator().next();
            }

            Offiziere.Offiziersausbildung offi = Offiziere.LIST.get(-offizier);

            for (Offizier.Ability ability : Offizier.Ability.values())
            {
                offz.setAbility(ability, offi.getAbility(ability));
            }

            int spec = ThreadLocalRandom.current().nextInt(offi.getSpecials().length);
            spec = offi.getSpecials()[spec];

            offz.setSpecial(Offizier.Special.values()[spec-1]);

            offz.setTraining(false);
            offz.stationierenAuf(academyQueueEntry.getAcademy().getBase());
            em.persist(offz);
            academyQueueEntry.setId(offz.getID());
        }
        else
        {
            /*
             * Offizier wurde weitergebildet
             */

            final Offizier.Ability ability = AcademyQueueEntry.getdTrain().get(training);

            final Offizier offz = Offizier.getOffizierByID(offizier);
            if( offz == null )
            {
                return true;
            }
            offz.setAbility(ability, offz.getAbility(ability)+10);
            if( !academyQueueEntry.getAcademy().isOffizierScheduled(offz.getID()) )
            {
                offz.setTraining(false);
            }
            em.persist(offz);
            academyQueueEntry.setId(offz.getID());
        }

        return true;
    }
}
