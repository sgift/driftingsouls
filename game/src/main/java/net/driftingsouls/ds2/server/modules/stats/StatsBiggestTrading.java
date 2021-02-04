package net.driftingsouls.ds2.server.modules.stats;

import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.ships.ShipClasses;

import java.util.List;

public abstract class StatsBiggestTrading extends AbstractStatistic implements Statistic {
    protected final String sumStatement = "sum(COALESCE(sm.cargo, st.cargo)*(s.crew/COALESCE(sm.crew, st.crew))*(s.hull/COALESCE(sm.hull, st.hull))*s.hull)";
    protected final List<ShipClasses> includedShipClasses = List.of(
        ShipClasses.TRANSPORTER,
        ShipClasses.CONTAINER
    );

    protected StatsBiggestTrading(BBCodeParser bbCodeParser) {
        super(bbCodeParser);
    }
}
