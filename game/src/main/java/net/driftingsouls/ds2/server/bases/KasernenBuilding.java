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
package net.driftingsouls.ds2.server.bases;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.Kaserne;
import net.driftingsouls.ds2.server.entities.KaserneEntry;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.units.UnitType;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.math.BigInteger;
import java.util.List;

/**
 * Die Kaserne.
 *
 */
@Entity(name="KaserneBuilding")
@DiscriminatorValue("net.driftingsouls.ds2.server.bases.Kaserne")
public class KasernenBuilding extends DefaultBuilding {
	/**
	 * Erstellt eine neue Instanz der Kaserne.
	 */
	public KasernenBuilding() {
		// EMPTY
	}

	@Override
	public boolean classicDesign() {
		return true;
	}

	@Override
	public boolean printHeader() {
		return false;
	}

	@Override
	public void build(Base base, Building building) {
		throw new IllegalArgumentException("should not be called!");
	}


	@Override
	public void cleanup(Context context, Base base, int building) {
		throw new IllegalArgumentException("should not be called!");
	}

	@Override
	public String echoShortcut(Context context, Base base, int field, int building) {

		StringBuilder result = new StringBuilder(200);
		Kaserne kaserne = em.find(Kaserne.class,building);
		if( kaserne != null ) {
			if( !kaserne.isBuilding() ) {
				result.append("<a class=\"back tooltip\" href=\"./ds?module=building");
				result.append("&amp;col=");
				result.append(base.getId());
				result.append("&amp;field=");
				result.append(field);
				result.append("\">[B]<span class='ttcontent'>").append(this.getName()).append("</span></a>");
			}
			else {
				StringBuilder popup = new StringBuilder(100);
				popup.append(this.getName()).append(":<br />");
				for( KaserneEntry entry : kaserne.getQueueEntries() )
				{
					UnitType unittype = entry.getUnit();
					popup.append("<br />Aktuell im Bau: ").append(entry.getCount()).append("x ").append(unittype.getName()).append(" <img src='./data/interface/time.gif' alt='Dauer: ' />").append(entry.getRemaining());
				}

				result.append("<a class=\"error tooltip\" href=\"./ds?module=building");
				result.append("&amp;col=");
				result.append(base.getId());
				result.append("&amp;field=");
				result.append(field);
				result.append("\">[B]<span style=\"font-weight:normal\">");
				result.append(kaserne.getQueueEntries().size());
				result.append("</span><span class='ttcontent'>").append(popup).append("</span></a>");
			}
		}

		return result.toString();
	}

	@Override
	public boolean isActive(Base base, int status, int field) {
		int buildingId = base.getBebauung()[field];
		Kaserne kaserne = em.find(Kaserne.class, buildingId);
		if( kaserne != null ) {
			return kaserne.isBuilding();
		}

		return false;
	}

	@Override
	public String output(Context context, Base base, int field, int building) {
		throw new IllegalArgumentException("should not be called!");
}
