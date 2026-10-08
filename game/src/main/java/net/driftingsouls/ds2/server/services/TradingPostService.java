package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.fraktionsgui.VersteigerungSchiff;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import javax.persistence.EntityManager;

@Service
@RequestScope
public class TradingPostService
{
	private final EntityManager db;

    public TradingPostService(EntityManager db) {
        this.db = db;
    }

    /**
	 * Gibt zurueck, ob ein bestimmtes Schiff mit dem angegebenen Handelsposten kommunizieren darf.
	 * Die Faehigkeit zur Kommunikation ist dabei keine Voraussetzung zum Handeln mit Waren sondern
	 * ist Voraussetzung fuer die Nutzung weitergehender Angebote.
	 * @param tradingPost Der Handelsposten
	 * @param tradingShip Das Schiff, dass mit dem HP kommunizieren moechte
	 * @return <code>true</code> falls es mit diesem kommunizieren darf
	 */
	public boolean isCommunicationPossible(Ship tradingPost, Ship tradingShip) {
		if( tradingPost == null || tradingShip == null || tradingShip.getTypeData().getShipClass() == ShipClasses.SCHUTZSCHILD )
		{
			return false;
		}
		if( !tradingPost.getLocation().sameSector(0, tradingShip, 0) )
		{
			return false;
		}
		if( !tradingPost.isTradepost() ||
				tradingPost.getOwner().getRace() != Faction.GTU_RASSE ||
				!tradingShip.canUseSrs() ||
				tradingShip.getSensors() < 30 ||
				tradingShip.getCrew() < tradingShip.getTypeData().getMinCrew() / 4) {
			return false;
		}

		User activeUser = tradingShip.getOwner();
		return tradingPost.isTradepostVisible(activeUser, activeUser.getRelations());
	}

	/**
	 * Erzeugt eine Versteigerung fuer das angegebene Schiff zum angegebenen Betrag.
	 * @param shipToAuction Das zu versteigernde Schiff
	 * @param amount Der Startbetrag fuer die Versteigerung
	 */
	public void auctionShip(Ship shipToAuction, int amount) {
		if (shipToAuction.getTypeData().getShipClass() != ShipClasses.SCHUTZSCHILD)
		{
			int ticks = 15;
			int curtick = ContextMap.getContext().get(ContextCommon.class).getTick();
			ticks += curtick;

			User user = shipToAuction.getOwner();
			ShipType st = shipToAuction.getBaseType();

			VersteigerungSchiff v = new VersteigerungSchiff(user, st, amount);
			v.setBieter(db.find(User.class, Faction.GTU));
			v.setTick(ticks);
			db.persist(v);

			shipToAuction.destroy();
		}
	}

	/**
	 * Gibt den erstbesten Handelsposten im angegebenen Sektor zurueck. Falls mehrere Handelsposten im Sektor existieren
	 * wird ein zufaelliger zurueckgegeben.
	 * @param sector Der Sektor
	 * @return Ein Handelsposten oder <code>null</code>
	 */
	public Ship findTradingPostInSector(Locatable sector) {
		Location loc = sector.getLocation();
		return db.createQuery("from Ship where id>0 and system=:sys and x=:x and y=:y and locate('tradepost',status)!=0", Ship.class)
				.setParameter("sys", loc.getSystem())
				.setParameter("x", loc.getX())
				.setParameter("y", loc.getY())
				.setMaxResults(1)
				.getResultList().stream().findFirst().orElse(null);
	}
}
