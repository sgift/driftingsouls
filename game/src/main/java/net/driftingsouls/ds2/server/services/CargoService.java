package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.config.items.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Service
public class CargoService {
    private static final Logger log = LoggerFactory.getLogger(CargoService.class);

    @PersistenceContext
    private EntityManager em;

    /**
     * Gibt die Gesamtmasse aller Waren und Items im <code>Cargo</code>-Objekt zurueck.
     * @return Die Gesamtmasse
     */
    public long getMass(Cargo cargo) {
        long tmp = 0;

        for (Long[] item1 : cargo.getItems())
        {
            Item item = em.find(Item.class, item1[0].intValue());
            if (item == null)
            {
                log.warn("Unbekanntes Item " + item1[0] + " geortet");
                continue;
            }
            tmp += item1[1] * item.getCargo();
        }

        return tmp;
    }

    /**
     * Spaltet vom Cargo ein Cargo-Objekt ab. Das neue Cargo-Objekt enthaelt
     * Resourcen in der angegebenen Masse (oder weniger, falls nicht genug im
     * Ausgangsobjekt waren). Das alte Cargoobjekt wird um diese Resourcenmenge
     * verringert.
     * @param mass Die gewuenschte Masse
     * @return Das abgespaltene Cargoobjekt.
     */
    public Cargo cutCargo(Cargo cargo, long mass ) {
        Cargo retcargo;

        if( mass >= getMass(cargo) ) {
            retcargo = cargo.clone();
            cargo.getItems().clear();

            return retcargo;
        }
        retcargo = new Cargo();

        long currentmass = 0;

        if( currentmass != mass ) {
            for( int i=0; i < cargo.getItems().size(); i++ ) {
                Long[] aitem = cargo.getItems().get(i);

                Item item = em.find(Item.class, aitem[0].intValue());
                if( item.getCargo()*aitem[1] + currentmass < mass ) {
                    currentmass += item.getCargo()*aitem[1];
                    retcargo.getItemArray().add(aitem);
                    cargo.getItems().remove(aitem);
                    i--;
                }
                else if( item.getCargo() > 0 ) {
                    Long[] newitem = aitem.clone();
                    newitem[1] = (mass-currentmass)/item.getCargo();
                    aitem[1] -= newitem[1];
                    currentmass += item.getCargo()*newitem[1];
                    retcargo.getItemArray().add(newitem);
                }
            }
        }

        return retcargo;
    }
}
