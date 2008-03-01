/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.scripting.roles.roleimpl;

import java.util.Iterator;
import java.util.List;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.scripting.ShipUtils;
import net.driftingsouls.ds2.server.scripting.roles.Role;
import net.driftingsouls.ds2.server.scripting.roles.interpreter.Attribute;
import net.driftingsouls.ds2.server.ships.Ship;

/**
 * <h1>Die Rolle DeutTransporter</h1>
 * Holt Deuterium von Tankern ab und transportiert es auf einen Asteroiden
 * @author Christopher Jung
 *
 */
public class DeutTransporter implements Role {
	@Attribute("nebel")
	private Location nebel;
	
	@Attribute("base")
	private long base;
	
	private long getAvailableDeuterium(Ship ship) {
		long deut = ship.getCargo().getResourceCount(Resources.DEUTERIUM);
		
		long reqDeut = ship.getTypeData().getRm() / ship.getTypeData().getRd();
		if( reqDeut > deut ) {
			return 0;
		}
		
		return deut - reqDeut;
	}
	
	public void execute(ScriptContext context) throws ScriptException {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		Ship ship = (Ship)context.getAttribute("_SHIP");
		
		if( !nebel.equals(ship.getLocation()) && !isEnougthDeutAvailableForTransport(ship) ) {
			ShipUtils.move(ship, nebel, Integer.MAX_VALUE);
			return;
		}
		
		if( nebel.equals(ship.getLocation())) {
			fetchDeuterium(db, ship);
		}
		
		if( !isEnougthDeutAvailableForTransport(ship) ) {
			return;
		}
		
		Base base = (Base)db.get(Base.class, (int)this.base);
		if( !ShipUtils.move(ship, base.getLocation(), Integer.MAX_VALUE) ) {
			return;
		}
		
		transferDeutToBase(ship, base);
		
		ShipUtils.move(ship, nebel, Integer.MAX_VALUE);
	}

	private boolean isEnougthDeutAvailableForTransport(Ship ship) {
		return getAvailableDeuterium(ship) > 0;
	}

	private void transferDeutToBase(Ship ship, Base base) {
		Cargo shipCargo = ship.getCargo();
		Cargo baseCargo = base.getCargo();
		
		long deut = getAvailableDeuterium(ship);
		if( deut + baseCargo.getMass() > base.getMaxCargo() ) {
			deut = base.getMaxCargo() - baseCargo.getMass();
		}
		baseCargo.addResource(Resources.DEUTERIUM, deut);
		shipCargo.substractResource(Resources.DEUTERIUM, deut);
		
		base.setCargo(baseCargo);
		ship.setCargo(shipCargo);
	}

	private void fetchDeuterium(org.hibernate.Session db, Ship ship) {
		Cargo shipCargo = ship.getCargo();
		
		List tankerList = db.createQuery("from Ship where x= :x and y= :y and system= :system " +
				"and owner= :owner and shiptype.deutFactor > 0 and id!= :transporter")
			.setInteger("x", nebel.getX())
			.setInteger("y", nebel.getY())
			.setInteger("system", nebel.getSystem())
			.setInteger("owner", ship.getOwner().getId())
			.setInteger("transporter", ship.getId())
			.list();
		for( Iterator iter=tankerList.iterator(); iter.hasNext(); ) {
			Ship tanker = (Ship)iter.next();
			Cargo tankerCargo = tanker.getCargo();
			
			long deut = getAvailableDeuterium(tanker);
			if( deut + shipCargo.getMass() > ship.getTypeData().getCargo() ) {
				deut = ship.getTypeData().getCargo() - shipCargo.getMass();
			}
			shipCargo.addResource(Resources.DEUTERIUM, deut);
			tankerCargo.substractResource(Resources.DEUTERIUM, deut);
			
			tanker.setCargo(tankerCargo);
		}
		
		ship.setCargo(shipCargo);
	}

}
