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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.comm.PM;
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
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import org.hibernate.Transaction;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Berechnung des Ticks fuer GTU-Versteigerungen.
 * @author Christopher Jung
 *
 */
@Service
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RTCTick extends TickController {
	private int ticks;
	private String currentTime;
	private User gtuuser;

	@Override
	protected void prepare() {
		this.ticks = getContext().get(ContextCommon.class).getTick();
		this.ticks++;

		this.currentTime = Common.getIngameTime(ticks);

		Transaction transaction = getDB().beginTransaction();
		this.gtuuser = (User)getDB().get(User.class, Faction.GTU);
		transaction.commit();

		this.log("tick: "+this.ticks);
	}

	@Override
	protected void tick() {
		//Im Kampagnentick brauchen die Versteigerungen nicht bearbeitet zu werden
		if(isCampaignTick()){
			return;
		}
		var db = getEM();
		var transaction = db.getTransaction();
		transaction.begin();

		final User sourceUser = db.find(User.class, -1);

		/*
			Einzelversteigerungen
		*/

		List<Versteigerung> entries = db.createQuery("from Versteigerung where tick<= :tick order by id", Versteigerung.class)
			.setParameter("tick", this.ticks)
			.getResultList();
		for (var entry : entries)
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
				StarSystem system = db.find(StarSystem.class, dropzone);

				Location loc = system.getDropZone();

				if (loc == null)
				{
					system = db.find(StarSystem.class, 605);

					loc = system.getDropZone();
				}

				int gtucost = 100;

				if (entry.getOwner() != this.gtuuser)
				{
					gtucost = entry.getOwner().getUserValue(WellKnownUserValue.GTU_AUCTION_USER_COST);
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
					cargo = cargo.cutCargo(shiptype.getCargo());

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
					ship.setNahrungCargo(Math.min(shiptype.getNahrungCargo(), ship.getFoodConsumption() * 3L));

					db.persist(ship);

					if (shiptype.getWerft() != 0)
					{
						ShipWerft werft = new ShipWerft(ship);
						db.persist(werft);

						this.log("\tWerft '" + shiptype.getWerft() + "' in Liste der Werften eingetragen");
					}

					ship.recalculateShipStatus();

					String msg = "Sie haben " + entryname + " f&uumlr; +" + Common.ln(price) + " RE ersteigert.\nDas Objekt wurde Ihnen bei " + loc.displayCoordinates(false) + " &uuml;bergeben.\n\nmit freundlichen Grüßen\nMun'thar Sethep - GTU-Hauptauktionator";
					PM.send(gtuuser, winner.getId(), entryname + " ersteigert", msg, db);

					if (entry.getOwner() != this.gtuuser)
					{
						msg = "Es wurde ihre " + entryname + " versteigert.\nDas Objekt wurde dem Gewinner " + winner.getName() + " f&uuml;r den Preis von " + Common.ln(price) + " RE &uuml;bergeben. Die GTU berechnet ihnen " + gtucost + "% des Gewinnes als Preis. Dies entspricht " + Common.ln(Math.ceil(price * gtucost / 100d)) + " RE. Ihnen bleiben somit noch " + Common.ln(priceAfterGtuCost) + " RE\n\nmit freundlichen Grüßen\nMun'thar Sethep - GTU-Hauptauktionator";
						PM.send(gtuuser, entry.getOwner().getId(), entryname + " versteigert", msg, db);

						msg = "Es wurde " + entryname + " im Auftrag von " + entry.getOwner().getId() + " versteigert.\nDas Objekt wurde bei " + loc.displayCoordinates(false) + " dem Gewinner " + winner.getId() + " f&uuml;r den Preis von " + Common.ln(price) + " RE &uuml;bergeben. Einnahme: " + Common.ln(Math.ceil(price * gtucost / 100d)) + " RE (" + gtucost + "%)";
						PM.send(sourceUser, Faction.GTU, entryname + " ersteigert", msg, db);
					}
					else
					{
						msg = "Es wurde " + entryname + " versteigert.\nDas Objekt wurde bei " + loc.displayCoordinates(false) + " dem Gewinner " + winner.getId() + " f&uuml;r den Preis von " + Common.ln(price) + " RE &uuml;bergeben.";
						PM.send(winner, Faction.GTU, entryname + " versteigert", msg, db);
					}
				}
				else if (entry instanceof VersteigerungResource)
				{
					VersteigerungResource resEntry = (VersteigerungResource) entry;

					Cargo mycargo = resEntry.getCargo();
					ResourceEntry resource = mycargo.getResourceList().iterator().next();

					entryname = Cargo.getResourceName(resource.getId());

					Ship posten = findBestTradepostForLocation(db, loc);

					this.log("Es wurde " + entryname + " von ID " + winner.getId() + " fuer " + price + " RE ersteigert");

					loc = posten.getLocation();

					String msg = "Sie haben " + entryname + " f&uumlr; " + Common.ln(price) + " RE ersteigert.\nDas Objekt wurde ihnen bei " + loc.displayCoordinates(false) + " auf dem Handelsposten hinterlegt.\n\nGaltracorp Unlimited";
					PM.send(gtuuser, winner.getId(), entryname + " ersteigert", msg, db);

					if (entry.getOwner() != this.gtuuser)
					{
						msg = "Es wurde ihr " + entryname + " versteigert.\nDas Objekt wurde dem Gewinner " + winner.getName() + " f&uuml;r den Preis von " + Common.ln(price) + " RE &uuml;bergeben. Die GTU berechnet Ihnen " + gtucost + "% des Gewinnes als Versteigerungskosten. Dies entspricht " + Common.ln(Math.ceil(price * gtucost / 100d)) + " RE. Ihnen bleiben somit noch " + Common.ln(priceAfterGtuCost) + " RE\n\nmit freundlichen Grüßen\nMun'thar Sethep - GTU-Hauptauktionator";
						PM.send(gtuuser, entry.getOwner().getId(), entryname + " versteigert", msg, db);

						msg = "Es wurde " + entryname + " im Auftrag von " + entry.getOwner().getId() + " versteigert.\nDas Objekt wurde bei " + loc.displayCoordinates(false) + " dem Gewinner " + winner.getId() + " f&uuml;r den Preis von " + Common.ln(price) + " RE hinterlegt. Einnahme: " + Common.ln(Math.ceil(price * gtucost / 100d)) + " RE (" + gtucost + "%)";
					}
					else
					{
						msg = "Es wurde " + entryname + " versteigert.\nDas Objekt wurde bei " + loc.displayCoordinates(false) + " dem Gewinner " + winner.getName() + " f&uuml;r den Preis von " + Common.ln(price) + " RE hinterlegt.";
					}
					PM.send(sourceUser, Faction.GTU, entryname + " ersteigert", msg, db);

					GtuZwischenlager lager = new GtuZwischenlager(posten, winner, entry.getOwner());
					lager.setCargo1(mycargo);
					lager.setCargo1Need(mycargo);

					db.persist(lager);
				}

				if (entry.getOwner() != this.gtuuser)
				{
					entry.getOwner().transferMoneyFrom(Faction.GTU, price - (long) Math.ceil(price * gtucost / 100d),
							"Gewinn Versteigerung #2" + entry.getId() + " abzgl. " + gtucost + "% Auktionskosten",
							false, UserMoneyTransfer.Transfer.AUTO);
				}

				StatGtu stat = new StatGtu(entry, gtucost);
				db.persist(stat);
				db.remove(entry);

				transaction.commit();
			}
			catch (RuntimeException e)
			{
				transaction.rollback();
				this.log("Versteigerung " + entry.getId() + " failed: " + e);
				e.printStackTrace();
				Common.mailThrowable(e, "RTCTick Exception", "versteigerung: " + entry.getId());

				throw e;
			}
		}

		transaction.commit();
	}

	private Ship findBestTradepostForLocation(EntityManager db, Location loc)
	{
		List<User> gtuUsers = db.createQuery("from User where race=:rasse", User.class)
			.setParameter("rasse", Faction.GTU_RASSE)
			.getResultList();
		Ship posten = db.createQuery("from Ship " +
				"where id>0 and locate('tradepost',status)!=0 and owner in (:userlist) and system=:sys and x=:x and y=:y", Ship.class)
			.setParameter("userlist", gtuUsers)
			.setParameter("sys", loc.getSystem())
			.setParameter("x", loc.getX())
			.setParameter("y", loc.getY())
			.setMaxResults(1)
			.getSingleResult();

		if( posten == null ) {
			posten = db.createQuery("from Ship " +
					"where id>0 and locate('tradepost',status)!=0 and owner in (:userlist) and system=:sys", Ship.class)
				.setParameter("userlist", gtuUsers)
				.setParameter("sys", loc.getSystem())
				.setMaxResults(1)
				.getSingleResult();
		}
		if( posten == null ) {
			posten = db.createQuery("from Ship " +
					"where id>0 and locate('tradepost',status)!=0 and owner in (:userlist)", Ship.class)
				.setParameter("userlist", gtuUsers)
				.setMaxResults(1)
				.getSingleResult();
		}
		return posten;
	}
}
