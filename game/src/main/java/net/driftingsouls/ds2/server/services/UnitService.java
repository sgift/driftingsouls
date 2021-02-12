package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.units.UnitType;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Service
public class UnitService {
    @PersistenceContext
    private EntityManager em;

    public UnitType getTypeById(int unitId) {
        return em.find(UnitType.class, unitId);
    }
}
