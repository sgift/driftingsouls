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
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.modules.ModuleType;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.npcorders.Order;
import net.driftingsouls.ds2.server.entities.npcorders.OrderOffizier;
import net.driftingsouls.ds2.server.entities.npcorders.OrderShip;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableOffizier;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.framework.db.batch.SingleUnitOfWork;
import net.driftingsouls.ds2.server.namegenerator.PersonenNamenGenerator;
import net.driftingsouls.ds2.server.ships.SchiffHinzufuegenService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.tick.TickController;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Berechnet NPC-Bestellungen.
 *
 * @author Christopher Jung
 *
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class NPCOrderTick extends TickController {
	private static final int OFFIZIERSSCHIFF = 71;
	private static final Location DEFAULT_LOCATION = new Location(2,30,35);

	private Map<Integer,StringBuilder> pmcache;

	@Override
	protected void prepare() {
		this.pmcache = new HashMap<>();
	}

	private String getOffiName(User user) {
		PersonenNamenGenerator generator = user.getPersonenNamenGenerator();
		if( generator != null ) {
			return generator.generiere();
		}

		return "NPC-Lieferservice";
	}

	@Override
	protected void tick()
	{
		org.hibernate.Session db = getDB();

		List<Integer> orders = Common.cast(db
				.createQuery("select id from Order order by user.id")
				.list());
		new EvictableUnitOfWork<Integer>("NPCOrderTick")
		{
			@Override
			public void doWork(Integer orderId)
			{
				org.hibernate.Session db = getDB();
				Order order = (Order)db.get(Order.class, orderId);

				if( order.getTick() != 1 )
				{
					order.setTick(order.getTick()-1);
					return;
				}

				User user = order.getUser();

				Location loc = ermittleLieferposition(user);

				log("  Lieferung erfolgt bei "+loc.getSystem()+":"+loc.getX()+"/"+loc.getY());

				Ship newShip = null;

				if( order instanceof OrderShip )
				{
					newShip = processOrderShip(order, user, loc);
				}
				else if( order instanceof OrderOffizier )
				{
					newShip = processOrderOffizier(db, order, user, loc);
				}

				if( newShip != null )
				{
					newShip.recalculateShipStatus();
				}

				db.delete(order);
			}
		}
		.setFlushSize(5)
		.executeFor(orders);

		this.log("Versende PMs...");
		new SingleUnitOfWork("NPCOrderTick - PMs")
		{
			@Override
			public void doWork() throws Exception
			{
				org.hibernate.Session db = getDB();
				final User sourceUser = (User)db.get(User.class, -1);
				for( Map.Entry<Integer, StringBuilder> entry : pmcache.entrySet() )
				{
					PM.send(sourceUser, entry.getKey(), "NPC-Lieferservice", entry.getValue().toString());
				}
			}
		}
		.execute();

		this.log("Verteile NPC-Punkte...");
		List<Integer> users = Common.cast(db
				.createQuery("select id from User where locate('ordermenu',flags)!=0")
				.list());
		new EvictableUnitOfWork<Integer>("NPCOrderTick - NPC-Punkte")
		{
			@Override
			public void doWork(Integer userId) throws Exception
			{
				User user = (User)getDB().get(User.class, userId);
				user.setNpcPunkte(user.getNpcPunkte()+1);
			}
		}
		.executeFor(users);
	}

	private Ship processOrderShip(Order order, User user, Location loc)
	{
		ShipType shipd = ((OrderShip)order).getShipType();

		this.log("* Order "+order.getId()+" ready: "+shipd.getNickname()+" ("+shipd.getId()+") wird zu User "+
				order.getUser().getId()+" geliefert");

		Ship newShip;
		Ship ship = createShip(user, shipd, loc);

		newShip = ship;

		final String flags = ((OrderShip)order).getFlags();
		if( flags.contains("tradepost") )
		{
			ship.setStatus("tradepost");
		}
		if( flags.contains("disable_iff") )
		{
			Cargo scargo = ship.getCargo();
			scargo.addResource(new ItemID(2), 1); // IFF-Stoersender
			ship.setCargo(scargo);
		}
		if( flags.contains("nicht_kaperbar") )
		{
			ship.addModule(0, ModuleType.ITEMMODULE, "442");
		}

		if( !this.pmcache.containsKey(user.getId()) )
		{
			this.pmcache.put(user.getId(), new StringBuilder());
		}
		StringBuilder pmcache = this.pmcache.get(user.getId());
		pmcache.append("Die von ihnen bestellte ");
		pmcache.append(shipd.getNickname());
		pmcache.append(" wurde geliefert\nSie steht bei ");
		pmcache.append(loc.displayCoordinates(false));
		pmcache.append("\n\n");
		return newShip;
	}

	private Ship processOrderOffizier(org.hibernate.Session db, Order order, User user, Location loc)
	{
		List<Base> bases = Base.byLocationAndBesitzer(loc, user);

		ShipType shipd = (ShipType)db.get(ShipType.class, OFFIZIERSSCHIFF);

		Ship newShip = null;
		if( bases.isEmpty() )
		{
			newShip = createShip(user, shipd, loc);

			this.log("* Order "+order.getId()+" ready: Offizier wird mittels "+
					shipd.getNickname()+" ("+shipd.getId()+") wird zu User "+order.getUser().getId()+" geliefert");
		}
		else
		{
			this.log("* Order "+order.getId()+" ready: Offizier wird zu User "+order.getUser().getId()+" - Basis "+bases.get(0).getId()+" geliefert");
		}

		OrderableOffizier offizier = (OrderableOffizier)db.get(OrderableOffizier.class, ((OrderOffizier)order).getType());
		int special = ThreadLocalRandom.current().nextInt(1, 7);

		Offizier offi = new Offizier(user, this.getOffiName(user));
		offi.setRang(offizier.getRang());
		offi.setAbility(Offizier.Ability.ING, offizier.getIng());
		offi.setAbility(Offizier.Ability.WAF, offizier.getWaf());
		offi.setAbility(Offizier.Ability.NAV, offizier.getNav());
		offi.setAbility(Offizier.Ability.SEC, offizier.getSec());
		offi.setAbility(Offizier.Ability.COM, offizier.getCom());
		if( !bases.isEmpty() )
		{
			offi.stationierenAuf(bases.get(0));
		}
		else
		{
			offi.stationierenAuf(newShip);
		}
		offi.setSpecial(Offizier.Special.values()[special]);

		db.persist(offi);

		// PM-Nachricht erstellen
		if( !this.pmcache.containsKey(user.getId()) )
		{
			this.pmcache.put(user.getId(), new StringBuilder());
		}
		StringBuilder pmcache = this.pmcache.get(user.getId());
		pmcache.append("Der von ihnen bestellte ");
		pmcache.append(offizier.getName());
		pmcache.append(" wurde geliefert.\nEr befindet sich auf ");

		if( bases.isEmpty() ) {
			pmcache.append("einer ");
			pmcache.append(shipd.getNickname());
			pmcache.append(" bei ");
		}
		else {
			pmcache.append("auf ihrer Basis ");
			pmcache.append(bases.get(0).getName());
			pmcache.append(" (");
			pmcache.append(bases.get(0).getId());
			pmcache.append(") im Sektor ");
		}

		pmcache.append(loc.displayCoordinates(false));
		pmcache.append("\n\n");
		return newShip;
	}

	private Ship createShip(User user, ShipType shipd, Location loc)
	{
		Cargo cargo = new Cargo();
		cargo.addResource( Resources.DEUTERIUM, shipd.getRd()*10 );
		cargo.addResource( Resources.URAN, shipd.getRu()*10 );
		cargo.addResource( Resources.ANTIMATERIE, shipd.getRa()*10 );

		SchiffHinzufuegenService schiffHinzufuegenService = new SchiffHinzufuegenService();
		Ship ship = schiffHinzufuegenService.erstelle(user, shipd, loc, "[hide]NPC-Order[/hide]");
		ship.setCargo(cargo);

		this.log("Schiff "+ship.getId()+" erzeugt");
		return ship;
	}

	private Location ermittleLieferposition(User user)
	{
		if( user.getNpcOrderLocation() != null )
		{
			Location loc = Location.fromString(user.getNpcOrderLocation());
			List<Base> candidates = Base.byLocationAndBesitzer(loc, user);
			if( !candidates.isEmpty() )
			{
				return loc;
			}
		}

		if( user.getBases().isEmpty() )
		{
			return DEFAULT_LOCATION;
		}
		Base ersteBasis = user.getBases().iterator().next();
		return ersteBasis.getLocation();
	}

}
