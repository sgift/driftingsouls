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
public class HandelspostenService
{
	private final EntityManager db;

    public HandelspostenService(EntityManager db) {
        this.db = db;
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
		return handelsposten.isTradepostVisible(aktiverUser, aktiverUser.getRelations());
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
			int curtick = ContextMap.getContext().get(ContextCommon.class).getTick();
			ticks += curtick;

			User user = zuVersteigerndesSchiff.getOwner();
			ShipType st = zuVersteigerndesSchiff.getBaseType();

			VersteigerungSchiff v = new VersteigerungSchiff(user, st, betrag);
			v.setBieter(db.find(User.class, Faction.GTU));
			v.setTick(ticks);
			db.persist(v);

			zuVersteigerndesSchiff.destroy();
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
		return db.createQuery("from Ship where id>0 and system=:sys and x=:x and y=:y and locate('tradepost',status)!=0", Ship.class)
				.setParameter("sys", loc.getSystem())
				.setParameter("x", loc.getX())
				.setParameter("y", loc.getY())
				.setMaxResults(1)
				.getResultList().stream().findFirst().orElse(null);
	}
}
