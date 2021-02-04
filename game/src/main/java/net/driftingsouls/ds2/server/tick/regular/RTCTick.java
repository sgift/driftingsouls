/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.tick.regular;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.GtuZwischenlager;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserMoneyTransfer;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.entities.fraktionsgui.Versteigerung;
import net.driftingsouls.ds2.server.entities.fraktionsgui.VersteigerungResource;
import net.driftingsouls.ds2.server.entities.fraktionsgui.VersteigerungSchiff;
import net.driftingsouls.ds2.server.entities.statistik.StatGtu;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.services.CargoService;
import net.driftingsouls.ds2.server.services.LocationService;
import net.driftingsouls.ds2.server.services.PmService;
import net.driftingsouls.ds2.server.services.ShipActionService;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.services.UserValueService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Berechnung des Ticks fuer GTU-Versteigerungen.
 * @author Christopher Jung
 *
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RTCTick extends TickController {
	private int ticks;
	private String currentTime;
	private User gtuuser;

	@PersistenceContext
	private EntityManager em;

	private final ConfigService configService;
	private final ShipService shipService;
	private final PmService pmService;
	private final UserValueService userValueService;
	private final LocationService locationService;
	private final CargoService cargoService;
	private final ShipActionService shipActionService;

	public RTCTick(ConfigService configService, ShipService shipService, PmService pmService, UserValueService userValueService, LocationService locationService, CargoService cargoService, ShipActionService shipActionService) {
		this.configService = configService;
		this.shipService = shipService;
		this.pmService = pmService;
		this.userValueService = userValueService;
		this.locationService = locationService;
		this.cargoService = cargoService;
		this.shipActionService = shipActionService;
	}

	@Override
	protected void prepare() {
		this.ticks = configService.getValue(WellKnownConfigValue.TICKS);
		this.ticks++;

		this.currentTime = Common.getIngameTime(ticks);

		this.gtuuser = em.find(User.class, Faction.GTU);

		this.log("tick: "+this.ticks);
	}

	@Override
	protected void tick() {
		final User sourceUser = em.find(User.class, -1);

		/*
			Einzelversteigerungen
		*/

		List<Versteigerung> entries = em.createQuery("from Versteigerung where tick<= :tick order by id", Versteigerung.class)
			.setParameter("tick", this.ticks)
			.getResultList();
		for (Versteigerung entry: entries)
		{
			try
			{
				if (entry.getBieter() == this.gtuuser)
				{
					this.log("Die Versteigerung um " + entry.getObjectName() + " (id: " + entry.getId() + (entry.getOwner() != this.gtuuser ? " - User: " + entry.getOwner().getId() : "") + ") wurde um 5 Runden verlaengert. Der Preis wurde um " + (long) (entry.getPreis() / 10d) + " RE reduziert");
					entry.setTick(this.ticks + 5);
					entry.setPreis((long) (entry.getPreis() / 10d));

					continue;
				}

				User winner = entry.getBieter();
				long price = entry.getPreis();
				int dropzone = winner.getGtuDropZone();
				StarSystem system = em.find(StarSystem.class, dropzone);
				if(system == null) {
					pmService.send(gtuuser, winner.getId(), "Unbekannte Dropzone", "Dein als Dropzone gewähltes System existiert nicht oder hat keine Dropzone (mehr). Wähle bitte ein anderes. Im nächsten Tick wird erneut versucht dir dein ersteigertes Objekt zuzustellen.");
					continue;
				}

				Location loc = system.getDropZone();
				if(loc == null) {
					pmService.send(gtuuser, winner.getId(), "Unbekannte Dropzone", "Dein als Dropzone gewähltes System existiert nicht oder hat keine Dropzone (mehr). Wähle bitte ein anderes. Im nächsten Tick wird erneut versucht dir dein ersteigertes Objekt zuzustellen.");
					continue;
				}

				int gtucost = 100;

				if (entry.getOwner() != this.gtuuser)
				{
					gtucost = userValueService.getUserValue(entry.getOwner(), WellKnownUserValue.GTU_AUCTION_USER_COST);
				}

				String entryname;

				double priceAfterGtuCost = price - Math.ceil(price * gtucost / 100d);
				if (entry instanceof VersteigerungSchiff)
				{
					VersteigerungSchiff shipEntry = (VersteigerungSchiff) entry;

					ShipType shiptype = shipEntry.getShipType();
					entryname = "eine " + shiptype.getNickname();

					this.log("Es wurde " + entryname + " (shipid: " + shiptype.getTypeId() + ") von ID " + winner + " fuer " + price + " RE ersteigert");

					String history = "Indienststellung am " + this.currentTime + " durch " + this.gtuuser.getName() + " (Versteigerung) f&uuml;r " + winner.getName() + " (" + winner.getId() + ")";

					Cargo cargo = new Cargo();
					cargo = cargoService.cutCargo(cargo, shiptype.getCargo());

					Ship ship = new Ship(winner, shiptype, loc.getSystem(), loc.getX(), loc.getY());
					ship.getHistory().addHistory(history);
					ship.setName("Verkauft");
					ship.setCrew(shiptype.getCrew());
					ship.setEnergy(shiptype.getEps());
					ship.setHull(shiptype.getHull());
					ship.setCargo(cargo);
					ship.setEngine(100);
					ship.setWeapons(100);
					ship.setComm(100);
					ship.setSensors(100);
					ship.setNahrungCargo(Math.min(shiptype.getNahrungCargo(), shipService.getFoodConsumption(ship) * 3L));

					em.persist(ship);

					if (shiptype.getWerft() != 0)
					{
						ShipWerft werft = new ShipWerft(ship);
						em.persist(werft);

						this.log("\tWerft '" + shiptype.getWerft() + "' in Liste der Werften eingetragen");
					}

					shipActionService.recalculateShipStatus(ship);

					String msg = "Sie haben " + entryname + " f&uumlr; +" + Common.ln(price) + " RE ersteigert.\nDas Objekt wurde Ihnen bei " + locationService.displayCoordinates(loc, false) + " &uuml;bergeben.\n\nmit freundlichen Grüßen\nMun'thar Sethep - GTU-Hauptauktionator";
					pmService.send(gtuuser, winner.getId(), entryname + " ersteigert", msg);

					if (entry.getOwner() != this.gtuuser)
					{
						msg = "Es wurde ihre " + entryname + " versteigert.\nDas Objekt wurde dem Gewinner " + winner.getName() + " f&uuml;r den Preis von " + Common.ln(price) + " RE &uuml;bergeben. Die GTU berechnet ihnen " + gtucost + "% des Gewinnes als Preis. Dies entspricht " + Common.ln(Math.ceil(price * gtucost / 100d)) + " RE. Ihnen bleiben somit noch " + Common.ln(priceAfterGtuCost) + " RE\n\nmit freundlichen Grüßen\nMun'thar Sethep - GTU-Hauptauktionator";
						pmService.send(gtuuser, entry.getOwner().getId(), entryname + " versteigert", msg);

						msg = "Es wurde " + entryname + " im Auftrag von " + entry.getOwner().getId() + " versteigert.\nDas Objekt wurde bei " + locationService.displayCoordinates(loc, false) + " dem Gewinner " + winner.getId() + " f&uuml;r den Preis von " + Common.ln(price) + " RE &uuml;bergeben. Einnahme: " + Common.ln(Math.ceil(price * gtucost / 100d)) + " RE (" + gtucost + "%)";
						pmService.send(sourceUser, Faction.GTU, entryname + " ersteigert", msg);
					}
					else
					{
						msg = "Es wurde " + entryname + " versteigert.\nDas Objekt wurde bei " + locationService.displayCoordinates(loc, false) + " dem Gewinner " + winner.getId() + " f&uuml;r den Preis von " + Common.ln(price) + " RE &uuml;bergeben.";
						pmService.send(winner, Faction.GTU, entryname + " versteigert", msg);
					}
				}
				else if (entry instanceof VersteigerungResource)
				{
					VersteigerungResource resEntry = (VersteigerungResource) entry;

					Cargo mycargo = resEntry.getCargo();
					ResourceEntry resource = mycargo.getResourceList().iterator().next();

					entryname = Cargo.getResourceName(resource.getId());

					Ship posten = findBestTradingPostForLocation(loc);

					this.log("Es wurde " + entryname + " von ID " + winner.getId() + " fuer " + price + " RE ersteigert");

					loc = posten.getLocation();

					String msg = "Sie haben " + entryname + " f&uumlr; " + Common.ln(price) + " RE ersteigert.\nDas Objekt wurde ihnen bei " + locationService.displayCoordinates(loc, false) + " auf dem Handelsposten hinterlegt.\n\nGaltracorp Unlimited";
					pmService.send(gtuuser, winner.getId(), entryname + " ersteigert", msg);

					if (entry.getOwner() != this.gtuuser)
					{
						msg = "Es wurde ihr " + entryname + " versteigert.\nDas Objekt wurde dem Gewinner " + winner.getName() + " f&uuml;r den Preis von " + Common.ln(price) + " RE &uuml;bergeben. Die GTU berechnet Ihnen " + gtucost + "% des Gewinnes als Versteigerungskosten. Dies entspricht " + Common.ln(Math.ceil(price * gtucost / 100d)) + " RE. Ihnen bleiben somit noch " + Common.ln(priceAfterGtuCost) + " RE\n\nmit freundlichen Grüßen\nMun'thar Sethep - GTU-Hauptauktionator";
						pmService.send(gtuuser, entry.getOwner().getId(), entryname + " versteigert", msg);

						msg = "Es wurde " + entryname + " im Auftrag von " + entry.getOwner().getId() + " versteigert.\nDas Objekt wurde bei " + locationService.displayCoordinates(loc, false) + " dem Gewinner " + winner.getId() + " f&uuml;r den Preis von " + Common.ln(price) + " RE hinterlegt. Einnahme: " + Common.ln(Math.ceil(price * gtucost / 100d)) + " RE (" + gtucost + "%)";
					}
					else
					{
						msg = "Es wurde " + entryname + " versteigert.\nDas Objekt wurde bei " + locationService.displayCoordinates(loc, false) + " dem Gewinner " + winner.getName() + " f&uuml;r den Preis von " + Common.ln(price) + " RE hinterlegt.";
					}
					pmService.send(sourceUser, Faction.GTU, entryname + " ersteigert", msg);

					GtuZwischenlager lager = new GtuZwischenlager(posten, winner, entry.getOwner());
					lager.setCargo1(mycargo);
					lager.setCargo1Need(mycargo);

					em.persist(lager);
				}

				if (entry.getOwner() != this.gtuuser)
				{
					entry.getOwner().transferMoneyFrom(Faction.GTU, price - (long) Math.ceil(price * gtucost / 100d),
							"Gewinn Versteigerung #2" + entry.getId() + " abzgl. " + gtucost + "% Auktionskosten",
							false, UserMoneyTransfer.Transfer.AUTO);
				}

				StatGtu stat = new StatGtu(entry, gtucost);
				em.persist(stat);
				em.remove(entry);
			}
			catch (RuntimeException e)
			{
				this.log("Versteigerung " + entry.getId() + " failed: " + e);
				e.printStackTrace();
				Common.mailThrowable(e, "RTCTick Exception", "versteigerung: " + entry.getId());

				throw e;
			}
		}
	}

	private Ship findBestTradingPostForLocation(Location loc)
	{
		List<User> gtuUsers = em.createQuery("from User where race=:rasse", User.class)
			.setParameter("rasse", Faction.GTU_RASSE)
			.getResultList();
		Ship posten = em.createQuery("from Ship " +
				"where id>0 and locate('tradepost',status)!=0 and owner in :userlist and system=:sys and x=:x and y=:y", Ship.class)
			.setParameter("userlist", gtuUsers)
			.setParameter("sys", loc.getSystem())
			.setParameter("x", loc.getX())
			.setParameter("y", loc.getY())
			.setMaxResults(1)
			.getSingleResult();

		if( posten == null ) {
			posten = em.createQuery("from Ship " +
					"where id>0 and locate('tradepost',status)!=0 and owner in :userlist and system=:sys", Ship.class)
				.setParameter("userlist", gtuUsers)
				.setParameter("sys", loc.getSystem())
				.setMaxResults(1)
				.getSingleResult();
		}
		if( posten == null ) {
			posten = em.createQuery("from Ship " +
					"where id>0 and locate('tradepost',status)!=0 and owner in :userlist", Ship.class)
				.setParameter("userlist", gtuUsers)
				.setMaxResults(1)
				.getSingleResult();
		}
		return posten;
	}
}
