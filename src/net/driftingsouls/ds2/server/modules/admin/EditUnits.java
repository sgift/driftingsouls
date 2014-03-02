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
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.units.UnitType;

import java.io.IOException;

/**
 * Aktualisierungstool fuer die Werte einer Einheit.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Einheiten", name = "Einheit editieren")
public class EditUnits extends AbstractEditPlugin<UnitType>
{
	public EditUnits()
	{
		super(UnitType.class);
	}

	@Override
	protected void update(StatusWriter writer, UnitType unit) throws IOException
	{
		Context context = ContextMap.getContext();
		unit.setName(context.getRequest().getParameterString("name"));
		unit.setPicture(context.getRequest().getParameterString("picture"));
		unit.setDauer(context.getRequest().getParameterInt("dauer"));
		unit.setNahrungCost(Double.parseDouble(context.getRequest().getParameterString("nahrungcost")));
		unit.setReCost(Double.parseDouble(context.getRequest().getParameterString("recost")));
		unit.setKaperValue(context.getRequest().getParameterInt("kapervalue"));
		unit.setSize(context.getRequest().getParameterInt("size"));
		unit.setDescription(context.getRequest().getParameterString("description"));
		unit.setRes(context.getRequest().getParameterInt("forschung"));
		unit.setHidden(context.getRequest().getParameterString("hidden").equals("true"));

		Cargo cargo = new Cargo(Cargo.Type.ITEMSTRING, context.getRequest().getParameter("buildcosts"));
		unit.setBuildCosts(cargo);
	}

	@Override
	protected void edit(EditorForm form, UnitType unit)
	{
		form.editField("Name", "name", String.class, unit.getName());
		form.editField("Bild", "picture", String.class, unit.getPicture());
		form.editField("Nahrungskosten", "nahrungcost", Double.class, unit.getNahrungCost());
		form.editField("RE Kosten", "recost", Double.class, unit.getReCost());
		form.editField("Kaper-Wert", "kapervalue", Integer.class, unit.getKaperValue());
		form.editField("Größe", "size", Integer.class, unit.getSize());
		form.editTextArea("Beschreibung", "description", unit.getDescription());
		form.editField("Benötigte Forschung", "forschung", Forschung.class, unit.getRes());
		form.editField("Dauer", "dauer", Integer.class, unit.getDauer());
		form.editField("Hidden", "hidden", Boolean.class, unit.isHidden());
		form.editField("Baukosten", "buildcosts", Cargo.class, unit.getBuildCosts());
	}
}
