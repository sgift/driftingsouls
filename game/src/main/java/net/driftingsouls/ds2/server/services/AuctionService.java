package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.entities.fraktionsgui.VersteigerungResource;
import net.driftingsouls.ds2.server.entities.fraktionsgui.VersteigerungSchiff;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Service
public class AuctionService {
    @PersistenceContext
    private EntityManager em;

    private final ConfigService configService;
    private final PmService pmService;
    private final UserValueService userValueService;

    public AuctionService(ConfigService configService, PmService pmService, UserValueService userValueService) {
        this.configService = configService;
        this.pmService = pmService;
        this.userValueService = userValueService;
    }

    public void auctionResource(int resourceId, int count, int price, int timeLimit) {
        int tick = configService.getValue(WellKnownConfigValue.TICKS);

        Cargo cargo = new Cargo();
        cargo.addResource( new ItemID(resourceId), count);

        User gtu = em.find(User.class, -2);

        VersteigerungResource auction = new VersteigerungResource(gtu, cargo, price);
        auction.setTick(tick+timeLimit);
        em.persist(auction);

        sendAuctionMessage("Versteigert werden "+cargo.getResourceList().iterator().next().getCount1()+" " +cargo.getResourceList().iterator().next().getPlainName() +". Aktueller Preis: "+price+" RE");
    }

    public void auctionShip(int shipId, int price, int timeLimit) {
        int tick = configService.getValue(WellKnownConfigValue.TICKS);

        User gtu = em.find(User.class, -2);
        ShipType type = em.find(ShipType.class, shipId);

        VersteigerungSchiff auction = new VersteigerungSchiff(gtu, type, price);
        auction.setTick(tick+timeLimit);
        em.persist(auction);

        sendAuctionMessage("Versteigert wird eine "+type.getNickname()+". Aktueller Preis: "+price+" RE");
    }

    private void sendAuctionMessage(String text) {
        User niemand = em.find(User.class, -1);
        List<Integer> userIDs = em.createQuery("select id from User", Integer.class).getResultList();

        for(Integer userID : userIDs)
        {
            User user = em.find(User.class, userID);
            //Abfrage, ob der user eine PM moechte
            if(userValueService.getUserValue(user, WellKnownUserValue.GAMEPLAY_USER_AUKTION_PM)) {
                pmService.send(niemand, user.getId(), "Neue Versteigerung eingestellt.", text);
            }
        }
    }
}
