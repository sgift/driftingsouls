package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.config.items.Item;
import org.springframework.stereotype.Service;


@Service
public class ResourceService {

    public Item getItemFromResourceId(ResourceID resourceID) {
        return Item.getItem( resourceID.getItemID());
    }
}
