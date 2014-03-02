package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;

import java.io.IOException;

/**
 * Editiert die Werte von Gebaeudetypen.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Asteroiden", name = "Gebäude editieren")
public class EditBuilding extends AbstractEditPlugin<Building>
{
	public EditBuilding()
	{
		super(Building.class);
	}

	@Override
	protected void update(StatusWriter statusWriter, Building building) throws IOException
	{
		Context context = ContextMap.getContext();
		Request request = context.getRequest();

		Cargo buildcosts = new Cargo(Cargo.Type.ITEMSTRING, request.getParameter("buildcosts"));
		Cargo produces = new Cargo(Cargo.Type.ITEMSTRING, request.getParameter("produces"));
		Cargo consumes = new Cargo(Cargo.Type.ITEMSTRING, request.getParameter("consumes"));

		building.setName(request.getParameterString("name"));
		building.setDefaultPicture(request.getParameterString("picture"));
		building.setArbeiter(request.getParameterInt("worker"));
		int energy = request.getParameterInt("energy");
		if(energy < 0)
		{
			energy = -1 * energy;
			building.setEVerbrauch(energy);
			building.setEProduktion(0);
		}
		else
		{
			building.setEProduktion(energy);
			building.setEVerbrauch(0);
		}
		building.setEps(request.getParameterInt("eps"));
		building.setBewohner(request.getParameterInt("room"));
		building.setTechReq(request.getParameterInt("tech"));
		building.setUcomplex(request.getParameterString("undergroundbuilding").equals("true"));
		building.setPerPlanet(request.getParameterInt("perplanet"));
		building.setPerOwner(request.getParameterInt("perowner"));
		building.setDeakable(request.getParameterString("deactivable").equals("true"));
		building.setCategory(request.getParameterInt("category"));
		building.setTerrainString(request.getParameterString("terrain"));
		building.setBuildCosts(buildcosts);
		building.setProduces(produces);
		building.setConsumes(consumes);
		building.setChanceRessString(request.getParameterString("chanceress"));
		building.setShutDown(request.getParameterString("shutdown").equals("true"));
		building.setRace(request.getParameterInt("race"));
	}

	@Override
	protected void edit(EditorForm form, Building building)
	{
		form.editLabel("Implementierung", building.getClass().getName());
		form.editField("Name", "name", String.class, building.getName());
		form.editField("Bild", "picture", String.class, building.getDefaultPicture());
		form.editField("Arbeiter", "worker", Integer.class, building.getArbeiter());

		int energy = -1*building.getEVerbrauch() + building.getEProduktion();
		form.editField("Energie", "energy", Integer.class, energy);
		form.editField("EPS", "eps", Integer.class, building.getEPS());
		form.editField("Wohnraum", "room", Integer.class, building.getBewohner());
		form.editField("Forschung", "tech", Forschung.class, building.getTechRequired());
		form.editField("Untergrundkomplex", "undergroundbuilding", Boolean.class, building.isUComplex());
		form.editField("Max. pro Planet", "perplanet", Integer.class, building.getPerPlanetCount());
		form.editField("Max. pro Spieler", "perowner", Integer.class, building.getPerUserCount());
		form.editField("Abschaltbar", "deactivable", Boolean.class, building.isDeakAble());
		form.editField("Kategorie", "category", Integer.class, building.getCategory());
		form.editField("Terrain", "terrain", String.class, building.getTerrainString());
		form.editField("Auto Abschalten", "shutdown", Boolean.class, building.isShutDown());
		form.editField("ChanceRess", "chanceress", String.class, building.getChanceRessString());
		form.editField("Rasse", "race", Rasse.class, building.getRace());
		form.editField("Baukosten", "buildcosts", Cargo.class, building.getBuildCosts());
		form.editField("Verbraucht", "consumes", Cargo.class, building.getConsumes());
		form.editField("Produziert", "produces", Cargo.class, building.getProduces());
	}
}
