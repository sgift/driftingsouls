package net.driftingsouls.ds2.server.modules.stats;

import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.modules.StatsController;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class StatsBiggestTradingAlliance extends StatsBiggestTrading {
    @PersistenceContext
    private EntityManager em;

    protected StatsBiggestTradingAlliance(BBCodeParser bbCodeParser) {
        super(bbCodeParser);
    }

    @Override
    public void show(StatsController contr, int size) throws IOException {
        List<Object[]> tmp = em.createQuery("select "+sumStatement+" as cnt,ally " +
            "from Ship s join s.owner o join o.ally ally join s.shiptype st left join s.modules sm " +
            "where s.id > 0 and " +
            "	ally is not null and " +
            "	o.id > :minid and " +
            "	(o.vaccount=0 or o.wait4vac>0) and " +
            "	(((sm.id is null) and st.cost>0) or ((sm.id is not null) and sm.cost>0)) and " +
            "	st.shipClass in :classes " +
            "group by ally " +
            "order by "+sumStatement+" desc, ally.id asc", Object[].class)
            .setParameter("minid", StatsController.MIN_USER_ID)
            .setParameter("classes", includedShipClasses)
            .setMaxResults(size)
            .getResultList();

        Map<Ally,Long> result = new LinkedHashMap<>();
        for (Object[] data : tmp)
        {
            result.put((Ally)data[1],(Long)data[0]);
        }

        this.generateStatistic("Die größten Handelsflotten:", result, ALLY_LINK_GENERATOR, false, size);
    }
}
