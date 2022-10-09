package net.driftingsouls.ds2.server.map;


public class UserRelationData {
    private final int userId;
    private final int targetId;
    private final int relationTo;
    private final int relationFrom;
    public UserRelationData(int userId, int targetId, int relationTo, int relationFrom)
    {
        this.userId = userId;
        this.targetId = targetId;
        this.relationTo = relationTo;
        this.relationFrom = relationFrom;
    }

    public int getUserId(){return userId;}
    public int getTargetUserId(){return targetId;}
    public int getRelationTo(){return relationTo;}
    public int getRelationFrom(){return relationFrom;}
}
