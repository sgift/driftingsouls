package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.entities.Nebel;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.ApplicationScope;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ApplicationScope
public class NebulaService {
    @PersistenceContext
    private EntityManager em;

    // Hibernate cachet nur Ergebnisse, die nicht leer waren.
    // Da es jedoch viele Positionen ohne Nebel gibt wuerden viele Abfragen
    // mehrfach durchgefuehrt. Daher wird in der Session vermerkt, welche
    // Positionen bereits geprueft wurden
    private final Set<Location> emptySpace = ConcurrentHashMap.newKeySet();

    /**
     * Gibt den Nebeltyp an der angegebenen Position zurueck. Sollte sich an der Position kein
     * Nebel befinden, wird <code>null</code> zurueckgegeben.
     * @param loc Die Position
     * @return Der Nebeltyp oder <code>null</code>
     */
    public Nebel.Typ getNebula(Location loc) {
        if(!emptySpace.contains(loc) ) {
            Nebel nebel = em.find(Nebel.class, new MutableLocation(loc));
            if( nebel == null ) {
                emptySpace.add(loc);
                return null;
            }

            return nebel.getType();
        }

        if(emptySpace.contains(loc)) {
            return null;
        }

        Nebel nebel = em.find(Nebel.class, new MutableLocation(loc));
        return nebel.getType();
    }
}
