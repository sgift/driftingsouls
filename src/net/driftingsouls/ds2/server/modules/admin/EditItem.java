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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.config.ModuleSlot;
import net.driftingsouls.ds2.server.config.items.IffDeaktivierenItem;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Munition;
import net.driftingsouls.ds2.server.config.items.Munitionsbauplan;
import net.driftingsouls.ds2.server.config.items.Quality;
import net.driftingsouls.ds2.server.config.items.Schiffsbauplan;
import net.driftingsouls.ds2.server.config.items.Schiffsmodul;
import net.driftingsouls.ds2.server.config.items.Schiffsmodul_;
import net.driftingsouls.ds2.server.config.items.SchiffsmodulSet;
import net.driftingsouls.ds2.server.config.items.Schiffsverbot;
import net.driftingsouls.ds2.server.config.items.Ware;
import net.driftingsouls.ds2.server.entities.FactoryEntry;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Munitionsdefinition;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.entities.statistik.StatCargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import net.driftingsouls.ds2.server.modules.admin.editoren.MapEntryRef;
import net.driftingsouls.ds2.server.ships.SchiffstypModifikation;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import org.hibernate.Session;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
		form.allowAdd();

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
		form.label("~ im Spielerbesitz", (item) -> {
			Session db = ContextMap.getContext().getDB();
			StatCargo statCargo = (StatCargo) db.createQuery("from StatCargo order by tick desc").setMaxResults(1).uniqueResult();
			if( statCargo == null )
			{
				return 0;
			}
			return statCargo.getCargo().getResourceCount(new ItemID(item));
		});
		form.ifEntityClass(Munition.class).field("Munitionsdefinition", Munitionsdefinition.class, Munition::getMunitionsdefinition, Munition::setMunitionsdefinition);
		form.ifEntityClass(Munitionsbauplan.class).field("Ermöglicht Fabrikeintrag", FactoryEntry.class, Munitionsbauplan::getFabrikeintrag, Munitionsbauplan::setFabrikeintrag);
		form.ifEntityClass(Munitionsbauplan.class).field("Als Allianzbauplan verwendbar", Boolean.class, Munitionsbauplan::isAllianzEffekt, Munitionsbauplan::setAllianzEffekt);
		form.ifEntityClass(Schiffsverbot.class).field("Verbotener Schiffstyp", ShipType.class, Schiffsverbot::getSchiffstyp, Schiffsverbot::setSchiffstyp);
		form.ifEntityClass(Schiffsverbot.class).field("Als Allianzitem verwendbar", Boolean.class, Schiffsverbot::isAllianzEffekt, Schiffsverbot::setAllianzEffekt);
		form.ifEntityClass(Schiffsbauplan.class).field("Schiffstyp", ShipType.class, Schiffsbauplan::getSchiffstyp, Schiffsbauplan::setSchiffstyp);
		form.ifEntityClass(Schiffsbauplan.class).field("Als Allianzbauplan verwendbar", Boolean.class, Schiffsbauplan::isAllianzEffekt, Schiffsbauplan::setAllianzEffekt);
		form.ifEntityClass(Schiffsbauplan.class).field("Rasse", Rasse.class, Schiffsbauplan::getRasse, Schiffsbauplan::setRasse);
		form.ifEntityClass(Schiffsbauplan.class).field("Flagschiff", Boolean.class, Schiffsbauplan::isFlagschiff, Schiffsbauplan::setFlagschiff);
		form.ifEntityClass(Schiffsbauplan.class).field("Baukosten", Cargo.class, Schiffsbauplan::getBaukosten, Schiffsbauplan::setBaukosten);
		form.ifEntityClass(Schiffsbauplan.class).field("Crew", Integer.class, Schiffsbauplan::getCrew, Schiffsbauplan::setCrew);
		form.ifEntityClass(Schiffsbauplan.class).field("Energiekosten", Integer.class, Schiffsbauplan::getEnergiekosten, Schiffsbauplan::setEnergiekosten);
		form.ifEntityClass(Schiffsbauplan.class).field("Dauer", Integer.class, Schiffsbauplan::getDauer, Schiffsbauplan::setDauer);
		form.ifEntityClass(Schiffsbauplan.class).field("Werftslots", Integer.class, Schiffsbauplan::getWerftSlots, Schiffsbauplan::setWerftSlots);
		form.ifEntityClass(Schiffsbauplan.class).multiSelection("Forschungen", Forschung.class, Schiffsbauplan::getBenoetigteForschungen, Schiffsbauplan::setBenoetigteForschungen);

		List<ModuleSlot> list = Common.cast(ContextMap.getContext().getDB().createCriteria(ModuleSlot.class).list());
		form.ifEntityClass(Schiffsmodul.class).multiSelection("Slots", String.class, Schiffsmodul::getSlots, Schiffsmodul::setSlots)
				.withOptions(list.stream().collect(Collectors.toMap(ModuleSlot::getSlotType, ModuleSlot::getName)));
		form.ifEntityClass(Schiffsmodul.class).field("Modifikation", SchiffstypModifikation.class, Schiffsmodul::getMods, Schiffsmodul::setMods).dbColumn(Schiffsmodul_.mods);
		form.ifEntityClass(Schiffsmodul.class).field("Set", SchiffsmodulSet.class, Schiffsmodul::getSet, Schiffsmodul::setSet).withNullOption("[Kein Set]").dbColumn(Schiffsmodul_.set);

		form.ifEntityClass(SchiffsmodulSet.class).map("Effekte", Integer.class, SchiffstypModifikation.class, SchiffsmodulSet::getSetEffekte, SchiffsmodulSet::setSetEffekte, subform -> {
			subform.field("Itemanzahl", Integer.class, MapEntryRef::getKey, MapEntryRef::setKey);
			subform.field("Modifikation", SchiffstypModifikation.class, MapEntryRef::getValue, MapEntryRef::setValue);
		});

		form.postUpdateTask("Schiffe mit Modulen aktualisieren",
				(Item item) -> item instanceof Schiffsmodul ? Common.cast(ContextMap.getContext().getDB()
						.createQuery("select s.id from Ship s where s.modules is not null")
						.list()) : new ArrayList<>(),
				(Item oldItem, Item item, Integer shipId) -> aktualisiereSchiff(shipId, oldItem, item)
		);
	}

	private void aktualisiereSchiff(Integer shipId, Item oldItem, Item item)
	{
		if( ((Schiffsmodul)oldItem).getMods() == ((Schiffsmodul)item).getMods() )
		{
			return;
		}

		Ship ship = (Ship) ContextMap.getContext().getDB().get(Ship.class, shipId);
        boolean modules = ship.getModules().length > 0;
        // Clone bei Modulen notwendig. Sonst werden auch die gespeicherten neu berechnet.
        ShipTypeData oldTypeData;
        try{
            oldTypeData = modules ? (ShipTypeData)ship.getTypeData().clone() : ship.getTypeData();
        }
        catch(CloneNotSupportedException e)
        {
            oldTypeData = ship.getTypeData();
        }
		ship.recalculateModules();
		ship.postUpdateShipType(oldTypeData);

		if (ship.getId() >= 0)
		{
			ship.recalculateShipStatus();
		}
	}
}
