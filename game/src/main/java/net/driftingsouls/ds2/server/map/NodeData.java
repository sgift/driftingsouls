package net.driftingsouls.ds2.server.map;

/**
 * Jump node data needed for field view.
 */
public class NodeData {
    public final int id;
    public final String name;
    public final boolean blocked;

    public NodeData(int id, String name, byte blocked) {
        this.id = id;
        this.name = name;
        this.blocked = blocked == 1;
    }
}
