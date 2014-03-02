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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.units.UnitType;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Aktualisierungstool fuer die Werte einer Einheit.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Einheiten", name = "Einheit editieren")
public class EditUnits extends AbstractEditPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int unitid = context.getRequest().getParameterInt("entityId");

		List<UnitType> unitTypes = Common.cast(db.createCriteria(UnitType.class).list());

		beginSelectionBox(echo, page, action);
		for (UnitType unitType : unitTypes)
		{
			addSelectionOption(echo, unitType.getId(), unitType.getName()+" ("+ unitType.getId()+")");
		}
		endSelectionBox(echo);

		if(isUpdateExecuted())
		{
			UnitType unit = (UnitType)db.get(UnitType.class, unitid);
			if( unit == null )
			{
				return;
			}

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

			echo.append("<p>Update abgeschlossen.</p>");
		}

		if(unitid != 0)
		{
			UnitType unit = (UnitType)db.get(UnitType.class, unitid);

			if(unit == null)
			{
				return;
			}

			beginEditorTable(echo, page, action, unitid);

			editField(echo, "Name", "name", String.class, unit.getName());
			editField(echo, "Bild", "picture", String.class, unit.getPicture());
			editField(echo, "Nahrungskosten", "nahrungcost", Double.class, unit.getNahrungCost());
			editField(echo, "RE Kosten", "recost", Double.class, unit.getReCost());
			editField(echo, "Kaper-Wert", "kapervalue", Integer.class, unit.getKaperValue());
			editField(echo, "Größe", "size", Integer.class, unit.getSize());
			editTextArea(echo, "Beschreibung", "description", unit.getDescription());
			editField(echo, "Benötigte Forschung", "forschung", Forschung.class, unit.getRes());
			editField(echo, "Dauer", "dauer", Integer.class, unit.getDauer());
			editField(echo, "Hidden", "hidden", Boolean.class, unit.isHidden());
			editField(echo, "Baukosten", "buildcosts", Cargo.class, unit.getBuildCosts());

			endEditorTable(echo);
		}
	}
}
