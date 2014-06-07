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
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.modules.viewmodels.ResourceEntryViewModel;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;


/**
 * <h1>Das Standardgebaeude in DS.</h1>
 * Alle Gebaeude, die ueber keine eigene Gebaeudeklasse verfuegen, werden von dieser
 * Gebaeudeklasse bearbeitet.
 *
 * @author Christopher Jung
 */
@Entity
@DiscriminatorValue("net.driftingsouls.ds2.server.bases.DefaultBuilding")
public class DefaultBuilding extends Building
{
	/**
	 * Erstellt eine neue Gebaeude-Instanz.
	 */
	public DefaultBuilding()
	{
		// EMPTY
	}

	@Override
	public void build(Base base, int building)
	{
		// EMPTY
	}

	@Override
	public void cleanup(Context context, Base base, int building)
	{
		// EMPTY
	}

	@Override
	public String modifyStats(Base base, Cargo stats, int building)
	{
		// EMPTY
		return "";
	}

	@Override
	public String modifyProductionStats(Base base, Cargo stats, int building)
	{
		// EMPTY
		return "";
	}

	@Override
	public String modifyConsumptionStats(Base base, Cargo stats, int building)
	{
		// EMPTY
		return "";
	}

	@Override
	public boolean isActive(Base base, int status, int field)
	{
		return status == 1;
	}

	@Override
	public String echoShortcut(Context context, Base base, int field, int building)
	{
		return "";
	}

	@Override
	public boolean printHeader()
	{
		return true;
	}

	@Override
	public boolean classicDesign()
	{
		return false;
	}

	@Override
	public String output(Context context, Base base, int field, int building)
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("Verbraucht:<br />\n");
		buffer.append("<div align=\"center\">\n");

		boolean entry = false;
		ResourceList reslist = getConsumes().getResourceList();
		for (ResourceEntry res : reslist)
		{
			buffer.append("<img src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append(" ");
			entry = true;
		}

		if (getEVerbrauch() > 0)
		{
			buffer.append("<img src=\"" + "./data/interface/energie.gif\" alt=\"\" />").append(getEVerbrauch()).append(" ");
			entry = true;
		}
		if (!entry)
		{
			buffer.append("-");
		}

		buffer.append("</div>\n");

		buffer.append("Produziert:<br />\n");
		buffer.append("<div align=\"center\">\n");

		entry = false;
		reslist = getProduces().getResourceList();
		for (ResourceEntry res : reslist)
		{
			buffer.append("<img src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append(" ");
			entry = true;
		}

		if (getEProduktion() > 0)
		{
			buffer.append("<img src=\"" + "./data/interface/energie.gif\" alt=\"\" />").append(getEProduktion());
			entry = true;
		}

		if (!entry)
		{
			buffer.append("-");
		}
		buffer.append("</div><br />\n");
		return buffer.toString();
	}

	@Override
	public boolean isSupportsJson()
	{
		return true;
	}

	@Override
	public BuildingUiViewModel outputJson(Context context, Base base, int field, int building)
	{
		BuildingUiViewModel gui = new BuildingUiViewModel();

		if (!getConsumes().isEmpty() || getEVerbrauch() > 0)
		{
			gui.consumes = new BuildingUiViewModel.CPViewModel();

			ResourceList reslist = getConsumes().getResourceList();
			for (ResourceEntry resourceEntry : reslist)
			{
				gui.consumes.cargo.add(ResourceEntryViewModel.map(resourceEntry));
			}

			gui.consumes.energy = new BuildingUiViewModel.EnergyViewModel();
			gui.consumes.energy.count = getEVerbrauch();
		}

		if (!getProduces().isEmpty() || getEProduktion() > 0)
		{
			gui.produces = new BuildingUiViewModel.CPViewModel();

			ResourceList reslist = getProduces().getResourceList();
			for (ResourceEntry resourceEntry : reslist)
			{
				gui.produces.cargo.add(ResourceEntryViewModel.map(resourceEntry));
			}

			gui.produces.energy = new BuildingUiViewModel.EnergyViewModel();
			gui.produces.energy.count = getEProduktion();
		}

		return gui;
	}
}
