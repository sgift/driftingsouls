/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.FactoryEntry;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Aktualisierungstool fuer die Werte eines Schiffes.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Fabriken", name = "Fabrikeintraege editieren")
public class EditFactoryEntry extends AbstractEditPlugin<FactoryEntry>
{
	public EditFactoryEntry()
	{
		super(FactoryEntry.class);
	}

	@Override
	protected void update(StatusWriter writer, FactoryEntry entry) throws IOException
	{
		Context context = ContextMap.getContext();

		entry.setBuildCosts(new Cargo(Cargo.Type.ITEMSTRING, context.getRequest().getParameterString("buildcosts")));
		entry.setRes1(context.getRequest().getParameterInt("res1"));
		entry.setRes2(context.getRequest().getParameterInt("res2"));
		entry.setRes3(context.getRequest().getParameterInt("res3"));
		entry.setProduce(new Cargo(Cargo.Type.ITEMSTRING, context.getRequest().getParameterString("produces")));
		entry.setDauer(BigDecimal.valueOf(Double.parseDouble(context.getRequest().getParameterString("dauer"))));
		entry.setBuildingIdString(context.getRequest().getParameterString("buildingids"));
	}

	@Override
	protected void edit(EditorForm form, FactoryEntry entry)
	{
		form.field("Baukosten", "buildcosts", Cargo.class, entry.getBuildCosts());
		form.field("Forschung 1", "res1", Forschung.class, entry.getRes1());
		form.field("Forschung 2", "res2", Forschung.class, entry.getRes2());
		form.field("Forschung 3", "res3", Forschung.class, entry.getRes3());
		form.field("Produktion", "produces", Cargo.class, entry.getProduce());
		form.field("Dauer", "dauer", BigDecimal.class, entry.getDauer());
		form.field("BuildingIDs", "buildingids", String.class, entry.getBuildingIdString());
	}
}
