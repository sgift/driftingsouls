package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.entities.Weapon;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import net.driftingsouls.ds2.server.ships.SchiffstypModifikation;
import net.driftingsouls.ds2.server.ships.Schiffswaffenkonfiguration;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@AdminMenuEntry(category = "Items", name = "Schiffstypmodifikation", permission = WellKnownAdminPermission.EDIT_ITEM)
public class EditSchiffstypModifikation implements EntityEditor<SchiffstypModifikation>
{
	@Override
	public Class<SchiffstypModifikation> getEntityType()
	{
		return SchiffstypModifikation.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<SchiffstypModifikation> form)
	{
		form.allowAdd();
		form.field("Name", String.class, SchiffstypModifikation::getNickname, SchiffstypModifikation::setNickname);
		form.dynamicContentField("Bild", SchiffstypModifikation::getPicture, SchiffstypModifikation::setPicture);
		form.field("Uranreaktor", Integer.class, SchiffstypModifikation::getRu, SchiffstypModifikation::setRu);
		form.field("Deuteriumreaktor", Integer.class, SchiffstypModifikation::getRd, SchiffstypModifikation::setRd);
		form.field("Antimateriereaktor", Integer.class, SchiffstypModifikation::getRa, SchiffstypModifikation::setRa);
		form.field("Reaktor Maximal", Integer.class, SchiffstypModifikation::getRm, SchiffstypModifikation::setRm);
		form.field("EPS", Integer.class, SchiffstypModifikation::getEps, SchiffstypModifikation::setEps);
		form.field("Flugkosten", Integer.class, SchiffstypModifikation::getCost, SchiffstypModifikation::setCost);
		form.field("Hülle", Integer.class, SchiffstypModifikation::getHull, SchiffstypModifikation::setHull);
		form.field("Panzerung", Integer.class, SchiffstypModifikation::getPanzerung, SchiffstypModifikation::setPanzerung);
		form.field("Cargo", Long.class, SchiffstypModifikation::getCargo, SchiffstypModifikation::setCargo);
		form.field("Nahrungsspeicher", Long.class, SchiffstypModifikation::getNahrungCargo, SchiffstypModifikation::setNahrungCargo);
		form.field("Hitze", Integer.class, SchiffstypModifikation::getHeat, SchiffstypModifikation::setHeat);
		form.field("Crew", Integer.class, SchiffstypModifikation::getCrew, SchiffstypModifikation::setCrew);
		form.field("Maximale Größe für Einheiten", Integer.class, SchiffstypModifikation::getMaxUnitSize, SchiffstypModifikation::setMaxUnitSize);
		form.field("Laderaum für Einheiten", Integer.class, SchiffstypModifikation::getUnitSpace, SchiffstypModifikation::setUnitSpace);
		form.field("Torpedoabwehr", Integer.class, SchiffstypModifikation::getTorpedoDef, SchiffstypModifikation::setTorpedoDef);
		form.field("Schilde", Integer.class, SchiffstypModifikation::getShields, SchiffstypModifikation::setShields);
		form.field("Größe", Integer.class, SchiffstypModifikation::getSize, SchiffstypModifikation::setSize);
		form.field("Jägerdocks", Integer.class, SchiffstypModifikation::getJDocks, SchiffstypModifikation::setJDocks);
		form.field("Aussendocks", Integer.class, SchiffstypModifikation::getADocks, SchiffstypModifikation::setADocks);
		form.field("Sensorreichweite", Integer.class, SchiffstypModifikation::getSensorRange, SchiffstypModifikation::setSensorRange);
		form.field("Hydros", Integer.class, SchiffstypModifikation::getHydro, SchiffstypModifikation::setHydro);
		form.field("RE Kosten", Integer.class, SchiffstypModifikation::getReCost, SchiffstypModifikation::setReCost);
		form.field("Deuteriumsammeln", Integer.class, SchiffstypModifikation::getDeutFactor, SchiffstypModifikation::setDeutFactor);
		form.multiSelection("Flags", ShipTypeFlag.class, SchiffstypModifikation::getFlags, SchiffstypModifikation::setFlags);
		form.field("Werft (Slots)", Integer.class, SchiffstypModifikation::getWerft, SchiffstypModifikation::setWerft);
		form.field("Einmalwerft", ShipType.class, SchiffstypModifikation::getOneWayWerft, SchiffstypModifikation::setOneWayWerft).withNullOption("[Keine]");
		form.field("Ablative Panzerung", Integer.class, SchiffstypModifikation::getAblativeArmor, SchiffstypModifikation::setAblativeArmor);
		Map<Boolean, String> srsOptions = new HashMap<>();
		srsOptions.put(Boolean.TRUE, "Ja");
		srsOptions.put(Boolean.FALSE, "Nein");
		form.field("Besitzt SRS", Boolean.class, SchiffstypModifikation::hasSrs, SchiffstypModifikation::setSrs).withOptions(srsOptions).withNullOption("Keine Änderung");
		form.field("Mindest-Crew", Integer.class, SchiffstypModifikation::getMinCrew, SchiffstypModifikation::setMinCrew);
		form.field("EMP verfliegen", Double.class, SchiffstypModifikation::getLostInEmpChance, SchiffstypModifikation::setLostInEmpChance);
		form.field("Kopfgeld", BigInteger.class, SchiffstypModifikation::getBounty, SchiffstypModifikation::setBounty);
		form.collection("Waffen",
				Schiffswaffenkonfiguration.class,
				SchiffstypModifikation::getWaffen,
				SchiffstypModifikation::setWaffen,
				subform -> {
					subform.field("Waffe", Weapon.class, Schiffswaffenkonfiguration::getWaffe, Schiffswaffenkonfiguration::setWaffe);
					subform.field("Anzahl", Integer.class, Schiffswaffenkonfiguration::getAnzahl, Schiffswaffenkonfiguration::setAnzahl);
					subform.field("Hitze", Integer.class, Schiffswaffenkonfiguration::getHitze, Schiffswaffenkonfiguration::setHitze);
					subform.field("Max-Überhitzung", Integer.class, Schiffswaffenkonfiguration::getMaxUeberhitzung, Schiffswaffenkonfiguration::setMaxUeberhitzung);
				});

		form.postUpdateTask("Schiffe aktualisieren",
				(SchiffstypModifikation mod) -> Common.cast(ContextMap.getContext().getDB()
						.createQuery("select s.id from Ship s where s.modules is not null")
						.list()),
				(SchiffstypModifikation oldMod, SchiffstypModifikation mod, Integer shipId) -> aktualisiereSchiff(shipId)
		);
	}

	private void aktualisiereSchiff(Integer shipId)
	{
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
