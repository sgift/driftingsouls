package net.driftingsouls.ds2.server.map;


public class UserRelationData {
    private final int userId;
    private final int targetId;
    private final int status;
    public UserRelationData(int userId, int targetId, int status)
    {
        this.userId = userId;
        this.targetId = targetId;
        this.status = status;
    }

    public int getUserId(){return userId;}
    public int getTargetUserId(){return targetId;}
    public int getStatus(){return status;}
}
