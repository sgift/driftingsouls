package net.driftingsouls.ds2.server.map;

public class BattleData {
    public final int id;
    public final int attackerRace;
    public final int attackerId;
    public final String attackerName;
    public final String plainAttackerName;
    public final int attackerAllyId;
    public final String attackerAllyName;
    public final String plainAttackerAllyName;
    public final int defenderRace;
    public final int defenderId;
    public final String defenderName;
    public final String plainDefenderName;
    public final int defenderAllyId;
    public final String defenderAllyName;
    public final String plainDefenderAllyName;

    public BattleData(int id, int attackerRace, int attackerId, String attackerName, String plainAttackerName, int attackerAllyId, String attackerAllyName, String plainAttackerAllyName, int defenderRace, int defenderId, String defenderName, String plainDefenderName, int defenderAllyId, String defenderAllyName, String plainDefenderAllyName) {
        this.id = id;
        this.attackerRace = attackerRace;
        this.attackerId = attackerId;
        this.attackerName = attackerName;
        this.plainAttackerName = plainAttackerName;
        this.attackerAllyId = attackerAllyId;
        this.attackerAllyName = attackerAllyName;
        this.plainAttackerAllyName = plainAttackerAllyName;
        this.defenderRace = defenderRace;
        this.defenderId = defenderId;
        this.defenderName = defenderName;
        this.plainDefenderName = plainDefenderName;
        this.defenderAllyId = defenderAllyId;
        this.defenderAllyName = defenderAllyName;
        this.plainDefenderAllyName = plainDefenderAllyName;
    }
}
