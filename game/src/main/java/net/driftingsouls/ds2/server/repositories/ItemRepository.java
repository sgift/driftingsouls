package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.cargo.ItemData;
import net.driftingsouls.ds2.server.config.items.Munition;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import org.jooq.Records;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static net.driftingsouls.ds2.server.entities.jooq.Tables.ITEMS;

public class ItemRepository {
    private final static ItemRepository instance = new ItemRepository();

    private final Map<Integer, ItemData> itemsDisplayData = new HashMap<>();

    private ItemRepository() {}

    public static ItemRepository getInstance() {
        return instance;
    }

    public Map<Integer, ItemData> getItemsData()
    {
        if(itemsDisplayData.isEmpty()) {
            synchronized (itemsDisplayData) {
                if(itemsDisplayData.isEmpty()) {
                    try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
                        var db = DBUtil.getDSLContext(conn);
                        try(var itemsDisplayDataRecords = db
                                .select(
                                        ITEMS.ID,
                                        ITEMS.TYP,
                                        ITEMS.CARGO,
                                        ITEMS.DESCRIPTION,
                                        ITEMS.LARGEPICTURE,
                                        ITEMS.NAME,
                                        ITEMS.PICTURE,
                                        ITEMS.QUALITY,
                                        ITEMS.ACCESSLEVEL,
                                        ITEMS.UNKNOWNITEM,
                                        ITEMS.ALLIANZEFFEKT
                                ).from(ITEMS)
                        ) {
                            var result = itemsDisplayDataRecords.fetch(Records.mapping(ItemData::new)).stream()
                                    .collect(Collectors.toMap(ItemData::getId, item -> item));
                            itemsDisplayData.putAll(result);
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return Collections.unmodifiableMap(itemsDisplayData);
    }

    public ItemData getItemData(int itemId)
    {
        return getItemsData().get(itemId);
    }

    public void clearItemCache() {
        itemsDisplayData.clear();
    }
}
