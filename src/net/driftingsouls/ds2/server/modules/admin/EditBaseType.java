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
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.map.TileCache;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import org.hibernate.Session;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Aktualisierungstool fuer die Basis-Klassen.
 * 
 */
@AdminMenuEntry(category = "Asteroiden", name = "Basis-Klasse", permission = WellKnownAdminPermission.EDIT_BASE_TYPE)
public class EditBaseType implements EntityEditor<BaseType>
{
	@Override
	public Class<BaseType> getEntityType()
	{
		return BaseType.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<BaseType> form)
	{
		form.allowAdd();
		form.field("Name", String.class, BaseType::getName, BaseType::setName);
		form.dynamicContentField("Bild (klein)", BaseType::getSmallImage, BaseType::setSmallImage);
		form.dynamicContentField("Bild (groß)", BaseType::getLargeImage, BaseType::setLargeImage);
		form.dynamicContentField("Bild (Sternenkarte)", BaseType::getStarmapImage, BaseType::setStarmapImage);
		form.field("Energie", Integer.class, BaseType::getEnergy, BaseType::setEnergy);
		form.field("Cargo", Integer.class, BaseType::getCargo, BaseType::setCargo);
		form.field("Breite", Integer.class, BaseType::getWidth, BaseType::setWidth);
		form.field("Höhe", Integer.class, BaseType::getHeight, BaseType::setHeight);
		form.field("Max. Feldanzahl", Integer.class, BaseType::getMaxTiles, BaseType::setMaxTiles);
		form.field("Radius", Integer.class, BaseType::getSize, BaseType::setSize);
		form.field("Terrain", String.class, (bt) -> bt.getTerrain() == null ? "" : Common.implode(";", bt.getTerrain()), (bt, value) -> bt.setTerrain(Common.explodeToInteger(";", value)));
		form.field("Zum Spawn freigegebene Ressourcen", String.class, BaseType::getSpawnableRess, BaseType::setSpawnableRess);

		form.postUpdateTask("Sternenkartencache leeren", (btOld, bt) -> {
			Session db = ContextMap.getContext().getDB();
			List<Integer> systems = Common.cast(db.createQuery("select distinct system from Base where klasse=:bt").setParameter("bt", bt).list());
			systems.forEach(s -> TileCache.forSystem(s).resetCache());
		});
	}
}
