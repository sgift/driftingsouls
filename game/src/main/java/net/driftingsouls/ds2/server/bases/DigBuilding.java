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
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.framework.Context;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Die BuddelStaette.
 *
 */
@Entity(name="DigBuilding")
@DiscriminatorValue("net.driftingsouls.ds2.server.bases.DigBuilding")
public class DigBuilding extends DefaultBuilding {

	/**
	 * Erstellt eine neue Buddelstaetten-Instanz.
	 */
	public DigBuilding() {
		// EMPTY
	}

	@Override
	public boolean classicDesign() {
		return true;
	}

	@SuppressWarnings("unchecked")
	private Cargo getProzentProduces()
	{
		Cargo production = new Cargo();
		Map<ItemID, Double> productions = getChanceRessMap();

		if(productions.isEmpty())
		{
			return production;
		}

		for (Entry<ItemID, Double> entry : productions.entrySet())
		{
			production.addResource(entry.getKey(), entry.getValue().longValue());
		}

		return production;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Cargo getAllProduces()
	{
		Cargo production = new Cargo();
		Map<ItemID, Double> productions = getChanceRessMap();

		if(productions.isEmpty())
		{
			return production;
		}

		for (Entry<ItemID, Double> entry : productions.entrySet())
		{
			production.addResource(entry.getKey(), 1l);
		}

		return production;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Cargo getProduces()
	{
		Cargo production = new Cargo();
		Map<ItemID, Double> productions = getChanceRessMap();

		if(productions.isEmpty())
		{
			return production;
		}

		for (Entry<ItemID, Double> entry : productions.entrySet())
		{
			double rnd = Math.random();
			if (rnd <= entry.getValue() / 100)
			{
				production.addResource(entry.getKey(), 1l);
			}
		}

		return production;
	}

	@Override
	public String output(Context context, Base base, int field, int building) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Dieses Geb&auml;de ist eine Buddelst&auml;tte.<br />\n");
		buffer.append("Die bei der Produktion angegebenen Werte sind die Chancen die entsprechende Ressource zu finden.<br />\n");
		buffer.append("<br />\n");
		buffer.append("<br />\n");
		buffer.append("Verbraucht:<br />\n");
		buffer.append("<div align=\"center\">\n");

		boolean entry = false;
		ResourceList reslist = getConsumes().getResourceList();
		for( ResourceEntry res : reslist )
		{
			buffer.append("<img src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append(" ");
			entry = true;
		}

		if( getEVerbrauch() > 0 )
		{
			buffer.append("<img src=\"./data/interface/energie.gif\" alt=\"\" />").append(getEVerbrauch()).append(" ");
			entry = true;
		}
		if( !entry )
		{
			buffer.append("-");
		}

		buffer.append("</div>\n");

		buffer.append("Produziert:<br />\n");
		buffer.append("<div align=\"center\">\n");

		entry = false;
		reslist = getProzentProduces().getResourceList();
		for( ResourceEntry res : reslist )
		{
			buffer.append("<img src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCount1()).append("% ");
			entry = true;
		}

		if( getEProduktion() > 0 )
		{
			buffer.append("<img src=\"./data/interface/energie.gif\" alt=\"\" />").append(getEProduktion());
			entry = true;
		}

		if( !entry )
		{
			buffer.append("-");
		}
		buffer.append("</div><br />\n");
		return buffer.toString();
	}

	@Override
	public boolean isSupportsJson()
	{
		return false;
	}
}
