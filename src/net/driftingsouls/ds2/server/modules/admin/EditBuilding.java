package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Rasse;

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
	protected void configureFor(EditorForm8 form, final Building building)
	{
		form.label("Implementierung", () -> building.getClass().getName());
		form.field("Name", String.class, building::getName, building::setName);
		form.field("Bild", String.class, building::getDefaultPicture, building::setDefaultPicture);
		form.field("Arbeiter", Integer.class, building::getArbeiter, building::setArbeiter);

		form.field("Energie", Integer.class, () -> getEnergiebilanz(building), (energy) -> setEnergiebilanz(building, energy));
		form.field("EPS", Integer.class, building::getEPS, building::setEps);
		form.field("Wohnraum", Integer.class, building::getBewohner, building::setBewohner);
		form.field("Forschung", Forschung.class, Integer.class, building::getTechRequired, building::setTechReq);
		form.field("Untergrundkomplex", Boolean.class, building::isUComplex, building::setUcomplex);
		form.field("Max. pro Planet", Integer.class, building::getPerPlanetCount, building::setPerPlanet);
		form.field("Max. pro Spieler", Integer.class, building::getPerUserCount, building::setPerOwner);
		form.field("Abschaltbar", Boolean.class, building::isDeakAble, building::setDeakable);
		form.field("Kategorie", Integer.class, building::getCategory, building::setCategory);
		form.field("Terrain", String.class, building::getTerrainString, building::setTerrainString);
		form.field("Auto Abschalten", Boolean.class, building::isShutDown, building::setShutDown);
		form.field("ChanceRess", String.class, building::getChanceRessString, building::setChanceRessString);
		form.field("Rasse", Rasse.class, Integer.class, building::getRace, building::setRace);
		form.field("Baukosten", Cargo.class, building::getBuildCosts, building::setBuildCosts);
		form.field("Verbraucht", Cargo.class, building::getConsumes, building::setConsumes);
		form.field("Produziert", Cargo.class, building::getProduces, building::setProduces);
	}
}
