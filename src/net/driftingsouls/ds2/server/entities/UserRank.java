package net.driftingsouls.ds2.server.entities;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Der Rang eines Spielers bei einem anderen Spieler.
 */
@Entity
@Table(name = "user_rank")
public class UserRank
{
    @Embeddable
    public static class UserRankKey implements Serializable
    {
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name="owner")
        private User owner;
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name="rank_giver")
        private User rankGiver;
        
        public UserRankKey()
        {
            //Hibernate
        }
        
        public UserRankKey(User owner, User rankGiver)
        {
            this.owner = owner;
            this.rankGiver = rankGiver;
        }
    }

    @Id
    private UserRankKey userRankKey;
    private int rank;

    @Version
    private int version;

    public UserRank()
    {
        //Hibernate
    }

    public UserRank(UserRankKey key, int rank)
    {
        this.userRankKey = key;
        this.rank = rank;
    }
    
    public int getRank()
    {
        return this.rank;
    }
    
    public void setRank(int rank)
    {
        this.rank = rank;
    }
}
