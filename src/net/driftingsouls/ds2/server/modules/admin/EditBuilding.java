package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.modules.AdminController;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Editiert die Werte von Gebaeudetypen.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Asteroiden", name = "Geb&auml;ude editieren")
public class EditBuilding extends AbstractEditPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		Request request = context.getRequest();
		int buildingId = request.getParameterInt("entityId");;

		if( this.isUpdateExecuted() )
		{
			Cargo buildcosts = new Cargo(Cargo.Type.ITEMSTRING, request.getParameter("buildcosts"));
			Cargo produces = new Cargo(Cargo.Type.ITEMSTRING, request.getParameter("produces"));
			Cargo consumes = new Cargo(Cargo.Type.ITEMSTRING, request.getParameter("consumes"));

			Building building = (Building)db.get(Building.class, buildingId);
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

		List<Building> buildings = Common.cast(db.createQuery("from Building").list());

		beginSelectionBox(echo, page, action);
		for (Building building: buildings)
		{
			addSelectionOption(echo, building.getId(), building.getName()+" ("+building.getId()+")");
		}
		endSelectionBox(echo);

		if(buildingId > 0)
		{
			Building building = (Building)db.get(Building.class, buildingId);
			if(building == null)
			{
				return;
			}

			beginEditorTable(echo, page, action, buildingId);

			editField(echo, "Name", "name", String.class, building.getName());
			editField(echo, "Bild", "picture", String.class, building.getDefaultPicture());
			editField(echo, "Arbeiter", "worker", Integer.class, building.getArbeiter());

			int energy = -1*building.getEVerbrauch() + building.getEProduktion();
			editField(echo, "Energie", "energy", Integer.class, energy);
			editField(echo, "EPS", "eps", Integer.class, building.getEPS());
			editField(echo, "Wohnraum", "room", Integer.class, building.getBewohner());
			editField(echo, "Forschung", "tech", Forschung.class, building.getTechRequired());
			editField(echo, "Untergrundkomplex", "undergroundbuilding", Boolean.class, building.isUComplex());
			editField(echo, "Max. pro Planet", "perplanet", Integer.class, building.getPerPlanetCount());
			editField(echo, "Max. pro Spieler", "perowner", Integer.class, building.getPerUserCount());
			editField(echo, "Abschaltbar", "deactivable", Boolean.class, building.isDeakAble());
			editField(echo, "Kategorie", "category", Integer.class, building.getCategory());
			editField(echo, "Terrain", "terrain", String.class, building.getTerrainString());
			editField(echo, "Auto Abschalten", "shutdown", Boolean.class, building.isShutDown());
			editField(echo, "ChanceRess", "chanceress", String.class, building.getChanceRessString());
			editField(echo, "Rasse", "race", Rasse.class, building.getRace());
			editField(echo, "Baukosten", "buildcosts", Cargo.class, building.getBuildCosts());
			editField(echo, "Verbraucht", "consumes", Cargo.class, building.getConsumes());
			editField(echo, "Produziert", "produces", Cargo.class, building.getProduces());

			endEditorTable(echo);
		}
	}
}
