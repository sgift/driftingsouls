package net.driftingsouls.ds2.server.install;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.GtuWarenKurse;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.SchiffHinzufuegenService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.TradepostVisibility;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import java.util.Arrays;
import java.util.List;

/**
 * Erzeugt Inhalte zur GTU waehrend des Installationsprozesses.
 */
public class GtuContentGenerator
{
	/**
	 * Erzeugt Handelsposten in einem Sternensystem.
	 * @param sys Das Sternensystem
	 */
	public void erzeugeHandelspostenInSystem(StarSystem sys)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		SchiffHinzufuegenService schiffHinzufuegenService = new SchiffHinzufuegenService();
		ShipType station = (ShipType) db.createCriteria(ShipType.class).add(Restrictions.eq("shipClass", ShipClasses.STATION)).setMaxResults(1).uniqueResult();
		User gtuUser = (User) db.createCriteria(User.class).add(Restrictions.idEq(-2)).uniqueResult();

		Ship schiff = schiffHinzufuegenService.erstelle(gtuUser, station, new Location(sys.getID(), sys.getWidth() / 2, sys.getHeight() / 2));
		schiff.getEinstellungen().setShowtradepost(TradepostVisibility.ALL);
		schiff.setName("GTU Handelsposten " + sys.getName());
		schiff.setNahrungCargo(station.getNahrungCargo());
		schiff.setStatus(schiff.getStatus() + " tradepost");

		Cargo cargo = schiff.getCargo();
		cargo.addResource(Resources.URAN, station.getRu() * 10);
		cargo.addResource(Resources.DEUTERIUM, station.getRd() * 10);
		cargo.addResource(Resources.ANTIMATERIE, station.getRa() * 10);
		schiff.setCargo(cargo);

		schiff.recalculateShipStatus();
	}

	/**
	 * Erzeugt die Verkaufsdaten fuer Handelsposten und Kommandozentrale.
	 */
	public void erzeugeVerkaufsdaten()
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		db.persist(new GtuWarenKurse("asti", "Kommandozentrale", erzeugeZufaelligeVerkaufsdaten(db)));
		db.persist(new GtuWarenKurse("tradepost", "Handelsposten", erzeugeZufaelligeVerkaufsdaten(db)));
	}

	private Cargo erzeugeZufaelligeVerkaufsdaten(Session db)
	{
		Cargo cargo = new Cargo();
		for (ResourceID rId : Arrays.asList(Resources.KUNSTSTOFFE, Resources.TITAN, Resources.URAN, Resources.ADAMATIUM, Resources.XENTRONIUM, Resources.ANTIMATERIE))
		{
			long preis = (long) (Math.random() * 1000);
			cargo.addResource(rId, preis);
		}

		List<Item> list = Common.cast(db.createCriteria(Item.class).add(Restrictions.sqlRestriction("1=1 order by rand()")).setMaxResults(15).list());
		for (Item item : list)
		{
			if( cargo.getResourceCount(new ItemID(item.getID())) > 0 )
			{
				continue;
			}
			long preis = (long) (Math.random() * 100000);
			if (preis > 1000)
			{
				preis = preis / 1000 * 1000;
			}
			cargo.addResource(new ItemID(item.getID()), preis);
		}

		return cargo;
	}
}
