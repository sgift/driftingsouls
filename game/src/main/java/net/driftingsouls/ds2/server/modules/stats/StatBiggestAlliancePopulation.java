package net.driftingsouls.ds2.server.modules.stats;

import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.modules.StatsController;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@Component
public class StatBiggestAlliancePopulation extends AbstractStatistic implements Statistic {
    @PersistenceContext
    private EntityManager em;

    public StatBiggestAlliancePopulation(BBCodeParser bbCodeParser) {
        super(bbCodeParser);
    }

    @Override
    public void show(StatsController contr, int size) throws IOException {
        Map<Ally,Long> bevcounts = getAllyPopulationData();
        SortedMap<Ally,Long> sortedBevCounts = new TreeMap<>(new MapValueDescComparator<>(bevcounts));
        sortedBevCounts.putAll(bevcounts);

        this.generateStatistic("Die größten Völker:", sortedBevCounts, ALLY_LINK_GENERATOR, false, size);
    }

    public Map<Ally,Long> getAllyPopulationData()
    {
        Map<Ally,Long> bev = new HashMap<>();

        List<Object[]> rows = em.createQuery("select sum(s.crew), s.owner.ally " +
                "from Ship s where s.id>0 and s.owner.id>:minid and s.owner.ally is not null " +
                "group by s.owner.ally", Object[].class)
            .setParameter("minid", StatsController.MIN_USER_ID)
            .getResultList();
        for(Object[] row : rows )
        {
            Ally ally = (Ally)row[1];
            bev.put(ally, (Long)row[0]);
        }

        //Bevoelkerung (Basis) pro User ermitteln (+ zur Besatzung pro User addieren)
        rows = em.createQuery("select sum(b.bewohner), b.owner.ally " +
                "from Base b where b.owner.id>:minid and b.owner.ally is not null " +
                "group by b.owner.ally", Object[].class)
            .setParameter("minid", StatsController.MIN_USER_ID)
            .getResultList();
        for( Object[] row : rows )
        {
            Ally ally = (Ally)row[1];
            Long sum = (Long)row[0];
            if( !bev.containsKey(ally) ) {
                bev.put(ally, sum);
            }
            else {
                bev.put(ally, bev.get(ally)+sum);
            }
        }

        return bev;
    }
}
