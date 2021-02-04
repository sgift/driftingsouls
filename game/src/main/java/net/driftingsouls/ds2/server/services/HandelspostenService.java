package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.fraktionsgui.VersteigerungSchiff;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.TradepostVisibility;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Service
public class HandelspostenService
{
	@PersistenceContext
	private EntityManager em;

	private final ConfigService configService;
	private final UserService userService;
	private final DismantlingService dismantlingService;

	public HandelspostenService(ConfigService configService, UserService userService, DismantlingService dismantlingService) {
		this.configService = configService;
		this.userService = userService;
		this.dismantlingService = dismantlingService;
	}

	/**
	 * Gibt zurueck, ob ein bestimmtes Schiff mit dem angegebenen Handelsposten kommunizieren darf.
	 * Die Faehigkeit zur Kommunikation ist dabei keine Voraussetzung zum Handeln mit Waren sondern
	 * ist Voraussetzung fuer die Nutzung weitergehender Angebote.
	 * @param handelsposten Der Handelsposten
	 * @param handelndesSchiff Das Schiff, dass mit dem HP kommunizieren moechte
	 * @return <code>true</code> falls es mit diesem kommunizieren darf
	 */
	public boolean isKommunikationMoeglich(Ship handelsposten, Ship handelndesSchiff) {
		if( handelsposten == null || handelndesSchiff == null || handelndesSchiff.getTypeData().getShipClass() == ShipClasses.SCHUTZSCHILD )
		{
			return false;
		}
		if( !handelsposten.getLocation().sameSector(0, handelndesSchiff, 0) )
		{
			return false;
		}
		if( !handelsposten.isTradepost() ||
				handelsposten.getOwner().getRace() != Faction.GTU_RASSE ||
				!handelndesSchiff.canUseSrs() ||
				handelndesSchiff.getSensors() < 30 ||
				handelndesSchiff.getCrew() < handelndesSchiff.getTypeData().getMinCrew() / 4) {
			return false;
		}

		User aktiverUser = handelndesSchiff.getOwner();
		return isTradepostVisible(handelsposten, aktiverUser);
	}

	/**
	 * returns wether the tradepost is visible or not.
	 * 0 everybody is able to see the tradepost.
	 * 1 everybody except enemys is able to see the tradepost.
	 * 2 every friend is able to see the tradepost.
	 * 3 the own allymembers are able to see the tradepost.
	 * 4 nobody except owner is able to see the tradepost.
	 * @param tradingPost The tradingPost ship object.
	 * @param observer the user who watches the tradepostlist.
	 * @return boolean if tradepost is visible
	 */
	public boolean isTradepostVisible(Ship tradingPost, User observer)
	{
		TradepostVisibility tradepostvisibility = tradingPost.getEinstellungen().getShowtradepost();
		UserService.Relations relations = userService.getRelations(observer);
		int ownerid = tradingPost.getOwner().getId();
		int observerid = observer.getId();
		switch (tradepostvisibility)
		{
			case ALL:
				return true;
			case NEUTRAL_AND_FRIENDS:
				// check whether we are an enemy of the owner
				return relations.beziehungVon(tradingPost.getOwner()) != User.Relation.ENEMY;
			case FRIENDS:
				// check whether we are a friend of the owner
				return (relations.beziehungVon(tradingPost.getOwner()) == User.Relation.FRIEND) || (ownerid == observerid);
			case ALLY:
				// check if we are members of the same ally and if the owner has an ally
				return ((tradingPost.getOwner().getAlly() != null) && observer.getAlly() != null && (tradingPost.getOwner().getAlly().getId() == observer.getAlly().getId())) || (ownerid == observerid);
			case NONE:
				// check if we are the owner of the tradepost
				return ownerid == observerid;
			default:
				// damn it, broken configuration, don't show the tradepost
				return false;
		}
	}

	/**
	 * Erzeugt eine Versteigerung fuer das angegebene Schiff zum angegebenen Betrag.
	 * @param zuVersteigerndesSchiff Das zu versteigernde Schiff
	 * @param betrag Der Startbetrag fuer die Versteigerung
	 */
	public void versteigereSchiff(Ship zuVersteigerndesSchiff, int betrag) {
		if (zuVersteigerndesSchiff.getTypeData().getShipClass() != ShipClasses.SCHUTZSCHILD)
		{
			int ticks = 15;
			int curtick = configService.getValue(WellKnownConfigValue.TICKS);
			ticks += curtick;

			User user = zuVersteigerndesSchiff.getOwner();
			ShipType st = zuVersteigerndesSchiff.getBaseType();

			VersteigerungSchiff v = new VersteigerungSchiff(user, st, betrag);
			v.setBieter(em.find(User.class, Faction.GTU));
			v.setTick(ticks);
			em.persist(v);

			dismantlingService.destroy(zuVersteigerndesSchiff);
		}
	}

	/**
	 * Gibt den erstbesten Handelsposten im angegebenen Sektor zurueck. Falls mehrere Handelsposten im Sektor existieren
	 * wird ein zufaelliger zurueckgegeben.
	 * @param sektor Der Sektor
	 * @return Ein Handelsposten oder <code>null</code>
	 */
	public Ship findeHandelspostenInSektor(Locatable sektor) {
		Location loc = sektor.getLocation();
		Session db = ContextMap.getContext().getDB();
		List<?> handel = db.createQuery("from Ship where id>0 and system=:sys and x=:x and y=:y and locate('tradepost',status)!=0")
				.setParameter("sys", loc.getSystem())
				.setParameter("x", loc.getX())
				.setParameter("y", loc.getY())
				.setMaxResults(1)
				.list();

		return handel.isEmpty() ? null : (Ship)handel.iterator().next();
	}
}
