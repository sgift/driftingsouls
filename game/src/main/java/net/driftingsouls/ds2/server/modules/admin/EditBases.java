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

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.bases.*;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.map.TileCache;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Aktualisierungstool fuer die Werte eines Spielers.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Asteroiden", name = "Basis", permission = WellKnownAdminPermission.EDIT_BASES)
public class EditBases implements EntityEditor<Base>
{
	@Override
	public Class<Base> getEntityType()
	{
		return Base.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<Base> form)
	{
		form.field("Name", String.class, Base::getName, Base::setName);
		form.field("Besitzer", User.class, Base::getOwner, Base::setOwner).dbColumn(Base_.owner);
		form.field("System", Integer.class, Base::getSystem, Base::setSystem).dbColumn(Base_.system);
		form.field("x", Integer.class, Base::getX, Base::setX);
		form.field("y", Integer.class, Base::getY, Base::setY);
		form.field("Aktuelle Energie", Integer.class, Base::getEnergy, Base::setEnergy);
		form.field("Maximale Energie", Integer.class, Base::getMaxEnergy, Base::setMaxEnergy);
		form.field("Aktuelle Bewohner", Integer.class, Base::getBewohner, Base::setBewohner);
		form.field("Cargo", Cargo.class, Base::getCargo, Base::setCargo);
		form.field("Maximaler Cargo", Long.class, Base::getMaxCargo, Base::setMaxCargo);
		form.field("Core", Core.class, Base::getCore, Base::setCore).withNullOption("[Keine]");
		form.field("Klasse", BaseType.class, Base::getKlasse, Base::setKlasse).dbColumn(Base_.klasse);
		form.field("Breite", Integer.class, Base::getWidth, Base::setWidth);
		form.field("Höhe", Integer.class, Base::getHeight, Base::setHeight);
		form.field("Feldanzahl für Ausbauten", Integer.class, Base::getMaxTiles, Base::setMaxTiles);
		form.field("Größe auf Sternenkarte", Integer.class, Base::getSize, Base::setSize);
		form.field("Terrain", String.class, (base) -> Common.implode("|", base.getTerrain()) , (base,s) -> base.setTerrain(convertAndCapTileList(base, s)));
		form.field("Bebauung", String.class, (base) -> Common.implode("|", base.getBebauung()) , (base,s) -> base.setBebauung(convertAndCapTileList(base, s)));
		form.field("Aktive Gebäude", String.class, (base) -> Common.implode("|", base.getActive()) , (base,s) -> base.setActive(convertAndCapTileList(base, s)));
		form.field("Core aktiv", Boolean.class, Base::isCoreActive, Base::setCoreActive);
		form.field("Zum Spawn freigegebene Ressourcen", String.class, Base::getSpawnableRess, Base::setSpawnableRess);
		form.field("Aktuell verfügbare Ressourcen", String.class, Base::getAvailableSpawnableRess, Base::setAvailableSpawnableRess);
		form.field("Automatischer Verkauf", String.class, (base) -> Common.implode(";", base.getAutoGTUActs()) , this::updateAutoGtuActs);

		form.postUpdateTask("Sternenkarten-Cache leeren", (orgbase,base) -> {
			TileCache.forSystem(orgbase.getSystem()).resetCache();
			TileCache.forSystem(base.getSystem()).resetCache();
		});
	}

	private void updateAutoGtuActs(Base base, String s)
	{
		String[] autogtuacts = StringUtils.split(s, ";");
		List<AutoGTUAction> acts = new ArrayList<>();
		for (String autogtuact : autogtuacts)
		{
			String[] split = StringUtils.split(autogtuact, ":");

			acts.add(new AutoGTUAction(Resources.fromString(split[0]), Integer.parseInt(split[1]), Long.parseLong(split[2])));
		}
		base.setAutoGTUActs(acts);
	}

	private Integer[] convertAndCapTileList(Base base, String str)
	{
		int max = base.getWidth()*base.getHeight();
		Integer[] tiles = Common.explodeToInteger("|", str);
		if( tiles.length > max ) {
			Integer[] newTiles = new Integer[max];
			System.arraycopy(tiles, 0, newTiles, 0, max);
			return newTiles;
		}
		return tiles;
	}
}
