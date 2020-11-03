package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.ships.SchiffEinstellungen;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import net.driftingsouls.ds2.server.ships.TradepostVisibility;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

/**
 * Consigns ships (or fleets of ships) to another player.
 *
 * Split out of ShipService/FleetMgmtService to stop cyclic dependency problems.
 */
@Service
public class ConsignService {
    @PersistenceContext
    private EntityManager em;

    private final ConfigService configService;
    private final FleetMgmtService fleetMgmtService;
    private final ShipService shipService;
    private final ShipyardService shipyardService;

    public ConsignService(ConfigService configService, FleetMgmtService fleetMgmtService, ShipService shipService, ShipyardService shipyardService) {
        this.configService = configService;
        this.fleetMgmtService = fleetMgmtService;
        this.shipService = shipService;
        this.shipyardService = shipyardService;
    }


    /**
     * Uebergibt ein Schiff an einen anderen Spieler. Gedockte/Gelandete Schiffe
     * werden, falls moeglich, mituebergeben.
     * @param newowner Der neue Besitzer des Schiffs
     * @param testonly Soll nur getestet (<code>true</code>) oder wirklich uebergeben werden (<code>false</code>)
     * @return <code>true</code>, falls ein Fehler bei der Uebergabe aufgetaucht ist (Uebergabe nicht erfolgreich)
     */
    public boolean consign(Ship ship, User newowner, boolean testonly ) {
        if( ship.getId() < 0 ) {
            throw new UnsupportedOperationException("consign kann nur bei Schiffen mit positiver ID ausgefuhert werden!");
        }

        if( ship.getTypeData().hasFlag(ShipTypeFlag.NICHT_UEBERGEBBAR) ) {
            Ship.MESSAGE.get().append("Sie können dieses Schiff nicht &uuml;bergeben.");
            return true;
        }

        if( newowner == null ) {
            Ship.MESSAGE.get().append("Der angegebene Spieler existiert nicht");
            return true;
        }

        if( (newowner.getVacationCount() != 0) && (newowner.getWait4VacationCount() == 0) ) {
            Ship.MESSAGE.get().append("Sie k&ouml;nnen keine Schiffe an Spieler &uuml;bergeben, welche sich im Vacation-Modus befinden");
            return true;
        }

        if( newowner.hasFlag( UserFlag.NO_SHIP_CONSIGN ) ) {
            Ship.MESSAGE.get().append("Sie k&ouml;nnen diesem Spieler keine Schiffe &uuml;bergeben");
            return true;
        }

        if(ship.getStatus().contains("noconsign")) {
            Ship.MESSAGE.get().append("Die '").append(ship.getName()).append("' (").append(ship.getId()).append(") kann nicht &uuml;bergeben werden");
            return true;
        }

        if( !testonly ) {
            ship.getHistory().addHistory("&Uuml;bergeben am [tick="+configService.getValue(WellKnownConfigValue.TICKS)+"] an "+newowner.getName()+" ("+newowner.getId()+")");

            fleetMgmtService.removeShip(ship.getFleet(), ship);
            ship.setOwner(newowner);

            if(ship.isLanded())
            {
                shipService.start(shipService.getBaseShip(ship), ship);
            }

            if(ship.isDocked())
            {
                shipService.undock(shipService.getBaseShip(ship), ship);
            }

            for( Offizier offi : ship.getOffiziere() )
            {
                offi.setOwner(newowner);
            }

            if( ship.getTypeData().getWerft() != 0 ) {
                Optional<ShipWerft> werft = em.createQuery("from ShipWerft where ship=:shipid", ShipWerft.class)
                    .setParameter("shipid", this)
                    .getResultStream().findAny();

                werft.ifPresent(w -> {
                    if(w.isLinked()) {
                        w.resetLink();
                    }

                    if(w.getKomplex() != null) {
                        shipyardService.removeFromKomplex(w);
                    }
                });
            }

            if( ship.isTradepost() )
            {
                // Um exploits zu verhindern die Sichtbarkeit des Haandelsposten
                // auf nichts stellen
                SchiffEinstellungen einstellungen = ship.getEinstellungen();
                einstellungen.setShowtradepost(TradepostVisibility.NONE);
                einstellungen.persistIfNecessary(ship);
            }
        }

        StringBuilder message = Ship.MESSAGE.get();
        List<Ship> s = shipService.getGedockteUndGelandeteSchiffe(ship);
        for (Ship aship : s)
        {
            int oldlength = message.length();
            boolean tmp = consign(aship, newowner, testonly);
            if (tmp && !testonly)
            {
                shipService.dock(ship, aship.isLanded() ? Ship.DockMode.START : Ship.DockMode.UNDOCK, aship);
            }

            if ((oldlength > 0) && (oldlength != message.length()))
            {
                message.insert(oldlength - 1, "<br />");
            }
        }

        Cargo cargo = ship.getCargo();
        List<ItemCargoEntry<Item>> itemlist = cargo.getItemEntries();
        for( ItemCargoEntry<Item> item : itemlist ) {
            Item itemobject = item.getItem();
            if( itemobject.isUnknownItem() ) {
                newowner.addKnownItem(item.getItemID());
            }
        }

        return false;
    }

    /**
     * Uebergibt alle Schiffe der Flotte an den angegebenen Spieler. Meldungen
     * werden dabei nach {@link ShipFleet#MESSAGE} geschrieben.
     * @param newowner Der neue Besitzer.
     * @return <code>true</code>, falls mindestens ein Schiff der Flotte uebergeben werden konnte
     */
    public boolean consign(ShipFleet fleet, User newowner) {
        int count = 0;

        fleet.setConsignMode(true);
        try {
            List<Ship> shiplist = em.createQuery("from Ship where fleet=:fleet and battle is null", Ship.class)
                .setParameter("fleet", fleet)
                .getResultList();
            for (Ship aship : shiplist)
            {
                boolean tmp = consign(aship, newowner, false);

                String msg = Ship.MESSAGE.getMessage();
                if (msg.length() > 0)
                {
                    ShipFleet.MESSAGE.get().append(msg).append("<br />");
                }
                if (!tmp)
                {
                    count++;
                    aship.setFleet(fleet);
                }
            }
        }
        finally {
            fleet.setConsignMode(false);
        }

        return count > 0;
    }
}
