package net.driftingsouls.ds2.server.cargo;

import net.driftingsouls.ds2.server.config.items.Quality;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;

public class ItemData {
    private final int id;
    private final String type;
    private final long cargo;
    private final String description;
    private final String largepicture;
    private final String name;
    private final String picture;
    private final String quality;
    private final int accessLevel;
    private final boolean unknownItem;
    private final boolean allianceEffect;
    public ItemData(int id, String type, long cargo, String description, String largepicture, String name, String picture, String quality, int accessLevel, Byte unknownItem, Byte allianceEffect)
    {
        this.id = id;
        this.type = type;
        this.cargo = cargo;
        this.description = description;
        this.largepicture = largepicture;
        this.name = name;
        this.picture = picture;

        this.quality = quality;
        this.accessLevel = accessLevel;
        this.unknownItem = unknownItem == 1;
        this.allianceEffect = allianceEffect != null && allianceEffect == 1;
    }

    public int getId(){ return id; }

    public ItemEffect.Type getType() {
        return ItemEffect.Type.getTypeFromEffectName(type);
    }

    public long getCargo() {
        return cargo;
    }

    public String getDescription() {
        return description;
    }

    public String getLargepicture() {
        return largepicture;
    }

    public String getName() {
        return name;
    }

    public String getPicture() {
        return picture;
    }

    public Quality getQuality() {
        return Quality.fromString(quality);
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public boolean isUnknownItem() {
        return unknownItem;
    }
    public boolean isAllyEffect(){ return allianceEffect; }
}
