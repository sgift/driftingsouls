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
import net.driftingsouls.ds2.server.config.items.IffDeaktivierenItem;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Munition;
import net.driftingsouls.ds2.server.config.items.Munitionsbauplan;
import net.driftingsouls.ds2.server.config.items.Quality;
import net.driftingsouls.ds2.server.config.items.Schiffsbauplan;
import net.driftingsouls.ds2.server.config.items.Schiffsmodul;
import net.driftingsouls.ds2.server.config.items.SchiffsmodulSet;
import net.driftingsouls.ds2.server.config.items.Schiffsverbot;
import net.driftingsouls.ds2.server.config.items.Ware;
import net.driftingsouls.ds2.server.entities.FactoryEntry;
import net.driftingsouls.ds2.server.entities.Munitionsdefinition;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import javax.annotation.Nonnull;

/**
 * Aktualisierungstool fuer die Werte eines Schiffes.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Items", name = "Item", permission = WellKnownAdminPermission.EDIT_ITEM)
public class EditItem implements EntityEditor<Item>
{

	@Override
	public Class<Item> getEntityType()
	{
		return Item.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<Item> form)
	{
		form.entityClass("Typ", Ware.class, IffDeaktivierenItem.class, Munition.class, Munitionsbauplan.class, Schiffsbauplan.class, Schiffsmodul.class, SchiffsmodulSet.class, Schiffsverbot.class);
		form.field("Name", String.class, Item::getName, Item::setName);
		form.picture("Bild", Item::getPicture);
		form.picture("Bild (groß)", Item::getLargePicture);
		form.field("Größe", Long.class, Item::getCargo, Item::setCargo);
		form.field("Unter Handel anzeigen", Boolean.class, Item::isHandel, Item::setHandel);
		form.field("Ben. Accesslevel", Integer.class, Item::getAccessLevel, Item::setAccessLevel);
		form.field("Qualität", Quality.class, Item::getQuality, Item::setQuality);
		form.field("Unbekanntes Item?", Boolean.class, Item::isUnknownItem, Item::setUnknownItem);
		form.field("Darf auf Basen spawnen", Boolean.class, Item::isSpawnableRess, Item::setSpawnableRess);
		form.textArea("Beschreibung", Item::getDescription, Item::setDescription);
		form.ifEntityClass(Munition.class).field("Munitionsdefinition", Munitionsdefinition.class, Munition::getMunitionsdefinition, Munition::setMunitionsdefinition);
		form.ifEntityClass(Munitionsbauplan.class).field("Ermöglicht Fabrikeintrag", FactoryEntry.class, Munitionsbauplan::getFabrikeintrag, Munitionsbauplan::setFabrikeintrag);
		form.ifEntityClass(Munitionsbauplan.class).field("Als Allianzbauplan verwendbar", Boolean.class, Munitionsbauplan::isAllianzEffekt, Munitionsbauplan::setAllianzEffekt);
	}
}
