package net.driftingsouls.ds2.server.modules.admin;

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
import net.driftingsouls.ds2.server.modules.admin.editoren.AbstractEditPlugin8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Editiert die Werte von Gebaeudetypen.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Asteroiden", name = "Gebäude editieren")
public class EditBuilding extends AbstractEditPlugin8<Building>
{
	public EditBuilding()
	{
		super(Building.class);
		setEntityClass(DefaultBuilding.class);
	}

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
	protected void configureFor(@NotNull EditorForm8<Building> form)
	{
		form.allowAdd();

		Map<String,String> clsOptions = Arrays.asList(DefaultBuilding.class, AcademyBuilding.class, DigBuilding.class, Fabrik.class, ForschungszentrumBuilding.class, KasernenBuilding.class, Werft.class)
												   .stream()
												   .collect(Collectors.toMap((c) -> c.getName(), (c) -> c.getSimpleName()));
		form.ifAdding().field("Implementierung", String.class, (Building b) -> this.getEntityClass(), (Building b,String s) -> this.setEntityClass(s)).withOptions(clsOptions);
		form.ifUpdating().label("Implementierung", (b) -> b.getClass().getName());
		form.field("Name", String.class, Building::getName, Building::setName);
		form.picture("Bild", Building::getDefaultPicture);
		form.field("Arbeiter", Integer.class, Building::getArbeiter, Building::setArbeiter);

		form.field("Energie", Integer.class, EditBuilding::getEnergiebilanz, EditBuilding::setEnergiebilanz);
		form.field("EPS", Integer.class, Building::getEPS, Building::setEps);
		form.field("Wohnraum", Integer.class, Building::getBewohner, Building::setBewohner);
		form.field("Forschung", Forschung.class, Integer.class, Building::getTechRequired, Building::setTechReq);
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
