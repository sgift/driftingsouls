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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.Order;
import net.driftingsouls.ds2.server.entities.OrderOffizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.namegenerator.NameGenerator;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.tick.TickController;

import org.apache.commons.lang.math.RandomUtils;

/**
 * Berechnet NPC-Bestellungen
 * 
 * @author Christopher Jung
 *
 */
public class NPCOrderTick extends TickController {
	private static final int OFFIZIERSSCHIFF = 71;
	private static final Location DEFAULT_LOCATION = new Location(2,30,35);
	
	private StringBuilder pmcache;
	private int lastowner;
	private String currentTime;
	private Map<Integer,List<String>> offinamelist;	
	
	@Override
	protected void prepare() {
		this.pmcache = new StringBuilder();
		this.lastowner = 0;

		this.currentTime = Common.getIngameTime(getContext().get(ContextCommon.class).getTick());
	}
	
	private String getOffiName(User user) {
		NameGenerator generator = Rassen.get().rasse(user.getRace()).getNameGenerator(Rasse.GeneratorType.PERSON);
		if( generator != null ) {
			return generator.generate(1)[0];
		}

		return "NPC-Lieferservice";
	}

	@Override
	protected void tick() {
		org.hibernate.Session db = getDB();
		Database database = getDatabase();
		
		final User sourceUser = (User)db.get(User.class, -1);
		
		List orders = db.createQuery("from Order where tick=1 order by user").list();
		for( Iterator iter=orders.iterator(); iter.hasNext(); ) {
			Order order = (Order)iter.next();
			try {
				int owner = order.getUser();
				User user = (User)getDB().get(User.class, owner);
					
				if( (owner != this.lastowner) && this.pmcache.length() > 0 ) {
					PM.send(sourceUser, this.lastowner, "NPC-Lieferservice", this.pmcache.toString());
					pmcache.setLength(0);
				}
				lastowner = owner;
			
				int type = OFFIZIERSSCHIFF;
				if( order.getType() > 0 ) {
					type = order.getType();
				}
			
				ShipTypeData shipd = Ship.getShipType( type );
			
				if( order.getType() > 0 ) {
					this.log("* Order "+order.getId()+" ready: "+shipd.getNickname()+" ("+type+") wird zu User "+order.getUser()+" geliefert");
				}
				else {
					this.log("* Order "+order.getId()+" ready: Offizier wird mittels "+shipd.getNickname()+" ("+type+") wird zu User "+order.getUser()+" geliefert");
				}
			
				Base base = (Base)db.createQuery("from Base where owner=?")
					.setEntity(0, user)
					.setMaxResults(1)
					.uniqueResult();
				Location loc = DEFAULT_LOCATION;
				if( base != null ) {
					loc = base.getLocation();
				}
				
				int id = 0;
				
				this.log("  Lieferung erfolgt bei "+loc);
				// Falls ein Schiff geordert wurde oder keine Basis fuer den Offizier existiert (und er braucht somit ein Schiff)...
				if( (order.getType() > 0) || base == null ) {
					Cargo cargo = new Cargo();
					cargo.addResource( Resources.DEUTERIUM, shipd.getRd()*10 );
					cargo.addResource( Resources.URAN, shipd.getRu()*10 );
					cargo.addResource( Resources.ANTIMATERIE, shipd.getRa()*10 );
				
					User auser = (User)getDB().get(User.class, owner);	
					String history = "Indienststellung am "+this.currentTime+" durch "+auser.getName()+" ("+auser.getId()+") [hide]NPC-Order[/hide]\n";
					
					Ship ship = new Ship(user);
					ship.setName("noname");
					ship.setBaseType((ShipType)db.get(ShipType.class, type));
					ship.setX(loc.getX());
					ship.setY(loc.getY());
					ship.setSystem(loc.getSystem());
					ship.setCrew(shipd.getCrew());
					ship.setHull(shipd.getHull());
					ship.setEnergy(shipd.getEps());
					ship.setCargo(cargo);
					ship.setHistory(history);
					ship.setEngine(100);
					ship.setWeapons(100);
					ship.setComm(100);
					ship.setSensors(100);
					ship.setAblativeArmor(shipd.getAblativeArmor());
					
					id = (Integer)db.save(ship);
					
					if( shipd.getWerft() != 0 ) {
						database.update("INSERT INTO werften (shipid) VALUES ('",id,"')");
					}
				}
			
				// Es handelt sich um einen Offizier...
				if( order.getType() < 0 ) {
					OrderOffizier offizier = (OrderOffizier)db.get(OrderOffizier.class, (-order.getType()));
					int special = RandomUtils.nextInt(6)+1;

					Offizier offi = new Offizier(user, this.getOffiName(user));
					offi.setRang(offizier.getRang());
					offi.setAbility(Offizier.Ability.ING, offizier.getIng());
					offi.setAbility(Offizier.Ability.WAF, offizier.getWaf());
					offi.setAbility(Offizier.Ability.NAV, offizier.getNav());
					offi.setAbility(Offizier.Ability.SEC, offizier.getSec());
					offi.setAbility(Offizier.Ability.COM, offizier.getCom());
					if( base != null ) {
						offi.setDest("b", base.getId());
					}
					else {
						offi.setDest("s", id);
					}
					offi.setSpecial(Offizier.Special.values()[special]);
					
					db.persist(offi);
			
					// PM-Nachricht erstellen
					pmcache.append("Der von ihnen bestellte ");
					pmcache.append(offizier.getName());
					pmcache.append(" wurde geliefert.\nEr befindet sich auf ");
					
					if( base == null ) {
						pmcache.append("einer ");
						pmcache.append(shipd.getNickname());
						pmcache.append(" bei ");
					}
					else {
						pmcache.append("auf ihrer Basis ");
						pmcache.append(base.getName());
						pmcache.append(" (");
						pmcache.append(base.getId());
						pmcache.append(") im Sektor ");
					}
					
					pmcache.append(loc);
					pmcache.append("\n\n");
				}
				// Es wurde nur ein Schiff geordert
				else {
					pmcache.append("Die von ihnen bestellte ");
					pmcache.append(shipd.getNickname());
					pmcache.append(" wurde geliefert\nSie steht bei ");
					pmcache.append(loc);
					pmcache.append("\n\n");
				}
				
				if( id != 0 ) {
					Ship ship = (Ship)db.get(Ship.class, id);
					ship.recalculateShipStatus();
				}
			
				db.delete(order);
				
				getContext().commit();
			}
			catch( RuntimeException e ) {
				this.log("Order "+order.getId()+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "NPCOrderTick Exception", "order: "+order.getId());
				
				throw e;
			}
		}
		
		if( pmcache.length() > 0 ) {
			PM.send(sourceUser, lastowner, "NPC-Lieferservice", pmcache.toString());
		}
		
		this.log("Verringere Wartezeit um 1...");
		db.createQuery("update Order set tick=tick-1").executeUpdate();
		
		this.log("Verteile NPC-Punkte...");
		db.createQuery("update User set npcpunkte=npcpunkte+1 where locate('ordermenu',flags)!=0");
	}

}
