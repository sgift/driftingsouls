package net.driftingsouls.ds2.server.modules.stats;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.modules.StatsController;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

@Component
public class StatBiggestAllianceFleet extends StatBiggestFleet {
    @PersistenceContext
    private EntityManager em;

    private final BBCodeParser bbCodeParser;

    protected StatBiggestAllianceFleet(BBCodeParser bbCodeParser) {
        super(bbCodeParser);
        this.bbCodeParser = bbCodeParser;
    }

    @Override
    public void show(StatsController contr, int size) throws IOException {
        List<Object[]> tmp = em.createQuery("select "+sumStatement+" as cnt,ally " +
            "from Ship s join s.owner o join o.ally ally join s.shiptype st left join s.modules sm " +
            "where s.id > 0 and " +
            "	ally is not null and " +
            "	o.id > :minid and " +
            "	(o.vaccount=0 or o.wait4vac>0) and " +
            "	COALESCE(sm.cost,st.cost)>0 and " +
            "	st.shipClass not in :classes " +
            "group by ally " +
            "order by "+sumStatement+" desc, ally.id asc", Object[].class )
            .setParameter("minid", StatsController.MIN_USER_ID)
            .setParameter("classes", ignoredShipClasses)
            .setMaxResults(size)
            .getResultList();

        String url = getAllyURL();

        Writer echo = getContext().getResponse().getWriter();

        echo.append("<h1>Die größten Flotten:</h1>");
        echo.append("<table class='stats'>\n");

        int count = 0;
        for( Object[] values : tmp )
        {
            echo.append("<tr><td>").append(Integer.valueOf(count + 1).toString()).append(".</td>\n");
            Ally ally = (Ally)values[1];
            echo.append("<td><a class=\"profile\" href=\"").append(url).append(Integer.valueOf(ally.getId()).toString()).append("\">").append(Common._title(bbCodeParser, ally.getName())).append(" (").append(Integer.valueOf(ally.getId()).toString()).append(")</a></td>\n");

            count++;
            echo.append("</tr>");
        }

        echo.append("</table>\n");
    }
}
