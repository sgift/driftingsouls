package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.bases.AcademyBuilding;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.bases.DefaultBuilding;
import net.driftingsouls.ds2.server.bases.DigBuilding;
import net.driftingsouls.ds2.server.bases.Fabrik;
import net.driftingsouls.ds2.server.bases.ForschungszentrumBuilding;
import net.driftingsouls.ds2.server.bases.KasernenBuilding;
import net.driftingsouls.ds2.server.bases.Werft;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import org.jetbrains.annotations.NotNull;

/**
 * Editiert die Werte von Gebaeudetypen.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Asteroiden", name = "Gebäude", permission = WellKnownAdminPermission.EDIT_BUILDING)
public class EditBuilding implements EntityEditor<Building>
{
	private static int getEnergiebilanz(Building building)
	{
		return -1 * building.getEVerbrauch() + building.getEProduktion();
	}

	private static void setEnergiebilanz(Building building, int energy)
	{
		if (energy < 0)
		{
			building.setEVerbrauch(-energy);
			building.setEProduktion(0);
		}
		else
		{
			building.setEProduktion(energy);
			building.setEVerbrauch(0);
		}
	}

	@Override
	public Class<Building> getEntityType()
	{
		return Building.class;
	}

	@Override
	public void configureFor(@NotNull EditorForm8<Building> form)
	{
		form.allowAdd();

		form.entityClass("Implementierung", DefaultBuilding.class, AcademyBuilding.class, DigBuilding.class, Fabrik.class, ForschungszentrumBuilding.class, KasernenBuilding.class, Werft.class);

		form.field("Name", String.class, Building::getName, Building::setName);
		form.picture("Bild", Building::getDefaultPicture);
		form.field("Arbeiter", Integer.class, Building::getArbeiter, Building::setArbeiter);

		form.field("Energie", Integer.class, EditBuilding::getEnergiebilanz, EditBuilding::setEnergiebilanz);
		form.field("EPS", Integer.class, Building::getEPS, Building::setEps);
		form.field("Wohnraum", Integer.class, Building::getBewohner, Building::setBewohner);
		form.field("Forschung", Forschung.class, Building::getTechRequired, Building::setTechReq).withNullOption("[keine]");
		form.field("Untergrundkomplex", Boolean.class, Building::isUComplex, Building::setUcomplex);
		form.field("Max. pro Planet", Integer.class, Building::getPerPlanetCount, Building::setPerPlanet);
		form.field("Max. pro Spieler", Integer.class, Building::getPerUserCount, Building::setPerOwner);
		form.field("Abschaltbar", Boolean.class, Building::isDeakAble, Building::setDeakable);
		form.field("Kategorie", Integer.class, Building::getCategory, Building::setCategory);
		form.field("Terrain", String.class, Building::getTerrainString, Building::setTerrainString);
		form.field("Auto Abschalten", Boolean.class, Building::isShutDown, Building::setShutDown);
		form.field("ChanceRess", String.class, Building::getChanceRessString, Building::setChanceRessString);
		form.field("Rasse", Rasse.class, Integer.class, Building::getRace, Building::setRace);
		form.field("Baukosten", Cargo.class, Building::getBuildCosts, Building::setBuildCosts);
		form.field("Verbraucht", Cargo.class, Building::getConsumes, Building::setConsumes);
		form.field("Produziert", Cargo.class, Building::getProduces, Building::setProduces);
	}
}
