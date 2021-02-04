package net.driftingsouls.ds2.server.modules.stats;

import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.ships.ShipClasses;

import java.util.List;

public abstract class StatBiggestFleet extends AbstractStatistic implements Statistic {
    protected final List<ShipClasses> ignoredShipClasses = List.of(
        ShipClasses.UNBEKANNT,
        ShipClasses.TRANSPORTER,
        ShipClasses.TANKER,
        ShipClasses.CONTAINER,
        ShipClasses.SCHROTT,
        ShipClasses.RETTUNGSKAPSEL,
        ShipClasses.EMTPY,
        ShipClasses.FELSBROCKEN
    );

    protected final String sumStatement = "sum(COALESCE(sm.size, st.size)*COALESCE(sm.size, st.size)*s.crew/COALESCE(sm.crew, st.crew)*s.hull/COALESCE(sm.hull,st.hull)*s.hull)";

    protected StatBiggestFleet(BBCodeParser bbCodeParser) {
        super(bbCodeParser);
    }
}
