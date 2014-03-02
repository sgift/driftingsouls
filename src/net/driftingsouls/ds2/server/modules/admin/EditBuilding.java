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
		form.label("Implementierung", building.getClass().getName());
		form.field("Name", "name", String.class, building.getName());
		form.field("Bild", "picture", String.class, building.getDefaultPicture());
		form.field("Arbeiter", "worker", Integer.class, building.getArbeiter());

		int energy = -1*building.getEVerbrauch() + building.getEProduktion();
		form.field("Energie", "energy", Integer.class, energy);
		form.field("EPS", "eps", Integer.class, building.getEPS());
		form.field("Wohnraum", "room", Integer.class, building.getBewohner());
		form.field("Forschung", "tech", Forschung.class, building.getTechRequired());
		form.field("Untergrundkomplex", "undergroundbuilding", Boolean.class, building.isUComplex());
		form.field("Max. pro Planet", "perplanet", Integer.class, building.getPerPlanetCount());
		form.field("Max. pro Spieler", "perowner", Integer.class, building.getPerUserCount());
		form.field("Abschaltbar", "deactivable", Boolean.class, building.isDeakAble());
		form.field("Kategorie", "category", Integer.class, building.getCategory());
		form.field("Terrain", "terrain", String.class, building.getTerrainString());
		form.field("Auto Abschalten", "shutdown", Boolean.class, building.isShutDown());
		form.field("ChanceRess", "chanceress", String.class, building.getChanceRessString());
		form.field("Rasse", "race", Rasse.class, building.getRace());
		form.field("Baukosten", "buildcosts", Cargo.class, building.getBuildCosts());
		form.field("Verbraucht", "consumes", Cargo.class, building.getConsumes());
		form.field("Produziert", "produces", Cargo.class, building.getProduces());
	}
}
