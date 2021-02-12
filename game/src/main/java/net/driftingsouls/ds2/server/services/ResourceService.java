package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.config.items.Item;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Service
public class ResourceService {
    @PersistenceContext
    private EntityManager em;

    public Item getItemFromResourceId(ResourceID resourceID) {
        return em.find(Item.class, resourceID.getItemID());
    }
}
