package net.driftingsouls.ds2.server.modules.admin;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Editiert die Werte von Gebaeudetypen.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Asteroiden", name = "Geb&auml;ude editieren")
public class EditBuilding implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int buildingId = context.getRequest().getParameterInt("building");
		List<Building> buildings = Common.cast(db.createQuery("from Building").list());

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");

		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select size=\"1\" name=\"building\">");
		for (Building building: buildings)
		{
			echo.append("<option value=\"" + building.getId() + "\" " + (building.getId() == buildingId ? "selected=\"selected\"" : "") + ">" + building.getName() + "</option>");
		}
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form>");


		if(update && buildingId > 0)
		{
			Cargo buildcosts = new Cargo(Cargo.Type.ITEMSTRING,context.getRequest().getParameter("buildcosts"));
			Cargo produces = new Cargo(Cargo.Type.ITEMSTRING,context.getRequest().getParameter("produces"));
			Cargo consumes = new Cargo(Cargo.Type.ITEMSTRING,context.getRequest().getParameter("consumes"));

			Building building = (Building)db.get(Building.class, buildingId);
			building.setName(context.getRequest().getParameterString("name"));
			building.setDefaultPicture(context.getRequest().getParameterString("picture"));
			building.setArbeiter(context.getRequest().getParameterInt("worker"));
			int energy = context.getRequest().getParameterInt("energy");
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
			building.setEps(context.getRequest().getParameterInt("eps"));
			building.setBewohner(context.getRequest().getParameterInt("room"));
			building.setTechReq(context.getRequest().getParameterInt("tech"));
			building.setUcomplex(context.getRequest().getParameterString("undergroundbuilding").equals("true") ? true : false);
			building.setPerPlanet(context.getRequest().getParameterInt("perplanet"));
			building.setPerOwner(context.getRequest().getParameterInt("perowner"));
			building.setDeakable(context.getRequest().getParameterString("deactivable").equals("true") ? true : false);
			building.setCategory(context.getRequest().getParameterInt("category"));
			building.setTerrain(context.getRequest().getParameterString("terrain"));
			building.setBuildCosts(buildcosts);
			building.setProduces(produces);
			building.setConsumes(consumes);
			building.setChanceRess(context.getRequest().getParameterString("chanceress"));
			building.setShutDown(context.getRequest().getParameterString("shutdown").equals("true") ? true : false);
		}

		if(buildingId > 0)
		{
			Building building = (Building)db.get(Building.class, buildingId);
			if(building == null)
			{
				return;
			}

			List<Forschung> researchs = Common.cast(db.createQuery("from Forschung").list());

			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<div class='gfxbox' style='width:600px'>");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"building\" value=\"" + buildingId + "\" />\n");
			echo.append("<table width=\"100%\">");
			echo.append("<tr><td>Name: </td><td><input type=\"text\" name=\"name\" value=\"" + building.getName() + "\"></td></tr>\n");
			echo.append("<tr><td>Bild: </td><td><input type=\"text\" name=\"picture\" value=\"" + building.getDefaultPicture() + "\"></td></tr>\n");
			echo.append("<tr><td>Arbeiter: </td><td><input type=\"text\" name=\"worker\" value=\"" + building.getArbeiter() + "\"></td></tr>\n");
			int energy = -1*building.getEVerbrauch() + building.getEProduktion();
			echo.append("<tr><td>Energie: </td><td><input type=\"text\" name=\"energy\" value=\"" + energy  + "\"></td></tr>\n");
			echo.append("<tr><td>EPS: </td><td><input type=\"text\" name=\"eps\" value=\"" + building.getEPS() + "\"></td></tr>\n");
			echo.append("<tr><td>Wohnraum: </td><td><input type=\"text\" name=\"room\" value=\"" + building.getBewohner() + "\"></td></tr>\n");
			echo.append("<tr><td>Forschung: </td><td><select size=\"1\" name=\"tech\">");
			for (Forschung research: researchs)
			{
				echo.append("<option value=\"" + research.getID() + "\" " + (research.getID() == building.getTechRequired() ? "selected=\"selected\"" : "") + ">" + research.getName() + "</option>");
			}
			echo.append("</select>");
			echo.append("<tr><td>Untergrundkomplex: </td><td><input type=\"text\" name=\"undergroundbuilding\" value=\"" + building.isUComplex() + "\"></td></tr>\n");
			echo.append("<tr><td>Max. pro Planet: </td><td><input type=\"text\" name=\"perplanet\" value=\"" + building.getPerPlanetCount() + "\"></td></tr>\n");
			echo.append("<tr><td>Max. pro Spieler: </td><td><input type=\"text\" name=\"perowner\" value=\"" + building.getPerUserCount() + "\"></td></tr>\n");
			echo.append("<tr><td>Abschaltbar: </td><td><input type=\"text\" name=\"deactivable\" value=\"" + building.isDeakAble() + "\"></td></tr>\n");
			echo.append("<tr><td>Kategorie: </td><td><input type=\"text\" name=\"category\" value=\"" + building.getCategory() + "\"></td></tr>\n");
			echo.append("<tr><td>Terrain: </td><td><input type=\"text\" name=\"terrain\" value=\"" + building.getTerrain() + "\"></td></tr>\n");
			echo.append("<tr><td>Auto Abschalten: </td><td><input type=\"text\" name=\"shutdown\" value=\"" + building.isShutDown() + "\"></td></tr>\n");
			echo.append("<tr><td>ChanceRess: </td><td><input type=\"text\" name=\"chanceress\" value=\"" + building.getChanceRess() + "\"></td></tr>\n");
			echo.append("<tr><td>Baukosten: </td><td><input type='hidden' id='buildcosts' name='buildcosts' value='"+building.getBuildCosts().save()+"'></td></tr>\n");
			echo.append("<tr><td>Verbraucht: </td><td><input type='hidden' id='consumes' name='consumes' value='"+building.getConsumes().save()+"'></td></tr>\n");
			echo.append("<tr><td>Produziert: </td><td><input type='hidden' id='produces' name='produces' value='"+building.getProduces().save()+"'></td></tr>\n");
			echo.append("<tr><td></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("<script type='text/javascript'>$(document).ready(function() {new CargoEditor('#buildcosts');new CargoEditor('#consumes');new CargoEditor('#produces');});</script>");
			echo.append("</div>");
			echo.append("</form>\n");
		}
	}
}
