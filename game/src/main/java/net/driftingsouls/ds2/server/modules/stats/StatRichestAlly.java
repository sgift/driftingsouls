package net.driftingsouls.ds2.server.modules.stats;

import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.modules.StatsController;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatRichestAlly extends AbstractStatistic implements Statistic {
    @PersistenceContext
    private EntityManager em;

    protected StatRichestAlly(BBCodeParser bbCodeParser) {
        super(bbCodeParser);
    }

    @Override
    public void show(StatsController contr, int size) throws IOException {
        List<Object[]> allianzen = em
            .createQuery("select a,sum(u.konto) from User u join u.ally a " +
                "where u.id>:minid and (u.vaccount=0 or u.wait4vac>0) group by a,u order by sum(u.konto) desc,u.id desc", Object[].class)
            .setParameter("minid", StatsController.MIN_USER_ID)
            .setMaxResults(size)
            .getResultList();

        Map<Ally,Long> displayMap = new LinkedHashMap<>();
        for( Object[] allianz : allianzen )
        {
            displayMap.put((Ally)allianz[0], ((Number)allianz[1]).longValue());
        }

        this.generateStatistic("Linked Markets Fortune List:", displayMap, ALLY_LINK_GENERATOR, false, -1);
    }
}
