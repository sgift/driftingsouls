package net.driftingsouls.ds2.server.modules.admin;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.WarenID;
import net.driftingsouls.ds2.server.config.ResourceConfig;
import net.driftingsouls.ds2.server.config.items.Item;
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
		List<Item> itemlist = Common.cast(db.createQuery("from Item").list());

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
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\"");
		echo.append("</form>");


		if(update && buildingId > 0)
		{
			Cargo buildcosts = new Cargo();
			for(ResourceConfig.Entry resource: ResourceConfig.getResources())
			{
				long amount = context.getRequest().getParameterInt("build"+resource.getId());
				buildcosts.addResource(new WarenID(resource.getId()), amount);
			}
			
			for(Item item: itemlist )
			{
				long amount = context.getRequest().getParameterInt("buildi"+item.getID());
				int uses = context.getRequest().getParameterInt("buildi" + item.getID() + "uses");
				
				buildcosts.addResource(new ItemID(item.getID(), uses, 0), amount);
			}

			Cargo produces = new Cargo();
			Cargo consumes = new Cargo();
			for(ResourceConfig.Entry resource: ResourceConfig.getResources())
			{
				long amount = context.getRequest().getParameterInt("prod"+resource.getId());
				if(amount < 0)
				{
					amount = -1*amount;
					consumes.addResource(new WarenID(resource.getId()), amount);
				}
				else
				{
					produces.addResource(new WarenID(resource.getId()), amount);
				}
			}
			
			for(Item item: itemlist)
			{
				long amount = context.getRequest().getParameterInt("i"+item.getID());
				int uses = context.getRequest().getParameterInt("i" + item.getID() + "uses");
				if(amount < 0)
				{
					amount = -1*amount;
					consumes.addResource(new ItemID(item.getID(), uses, 0), amount);
				}
				else
				{
					produces.addResource(new ItemID(item.getID(), uses, 0), amount);
				}
			}

			Building building = (Building)db.get(Building.class, buildingId);
			building.setName(context.getRequest().getParameterString("name"));
			building.setPicture(context.getRequest().getParameterString("picture"));
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
			building.setBuildCosts(buildcosts);
			building.setProduces(produces);
			building.setConsumes(consumes);
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
			echo.append("<table class=\"noBorder\" width=\"100%\">");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"building\" value=\"" + buildingId + "\" />\n");		
			echo.append("<tr><td class=\"noBorderS\">Name: </td><td><input type=\"text\" name=\"name\" value=\"" + building.getName() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Bild: </td><td><input type=\"text\" name=\"picture\" value=\"" + building.getPicture() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Arbeiter: </td><td><input type=\"text\" name=\"worker\" value=\"" + building.getArbeiter() + "\"></td></tr>\n");
			int energy = -1*building.getEVerbrauch() + building.getEProduktion();
			echo.append("<tr><td class=\"noBorderS\">Energie: </td><td><input type=\"text\" name=\"energy\" value=\"" + energy  + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">EPS: </td><td><input type=\"text\" name=\"eps\" value=\"" + building.getEPS() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Wohnraum: </td><td><input type=\"text\" name=\"room\" value=\"" + building.getBewohner() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Forschung: </td><td><select size=\"1\" name=\"tech\">");
			for (Forschung research: researchs)
			{
				echo.append("<option value=\"" + research.getID() + "\" " + (research.getID() == building.getTechRequired() ? "selected=\"selected\"" : "") + ">" + research.getName() + "</option>");
			}
			echo.append("</select>");
			echo.append("<tr><td class=\"noBorderS\">Untergrundkomplex: </td><td><input type=\"text\" name=\"undergroundbuilding\" value=\"" + building.isUComplex() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Max. pro Planet: </td><td><input type=\"text\" name=\"perplanet\" value=\"" + building.getPerPlanetCount() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Max. pro Spieler: </td><td><input type=\"text\" name=\"perowner\" value=\"" + building.getPerUserCount() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Abschaltbar: </td><td><input type=\"text\" name=\"deactivable\" value=\"" + building.isDeakAble() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Kategorie: </td><td><input type=\"text\" name=\"category\" value=\"" + building.getCategory() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Auto Abschalten: </td><td><input type=\"text\" name=\"shutdown\" value=\"" + building.isShutDown() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\"><b>Baukosten</b></td><td class=\"noBorderS\">Menge</td></tr>");
			for(ResourceConfig.Entry resource: ResourceConfig.getResources())
			{
				long amount = building.getBuildCosts().getResourceCount(new WarenID(resource.getId()));
				echo.append("<tr><td class=\"noBorderS\"><img src=\""+resource.getImage()+"\" alt=\"\" />"+resource.getName()+": </td><td><input type=\"text\" name=\"build"+resource.getId()+"\" value=\"" + amount + "\"></td></tr>");
			}
			echo.append("<tr><td class=\"noBorderS\"></td><td class=\"noBorderS\">Menge</td><td class=\"noBorderS\">Nutzungen</td></tr>");
			for(Item item: itemlist)
			{
				long amount = building.getBuildCosts().getResourceCount(new ItemID(item.getID()));
				int uses = 0;
				if(!building.getBuildCosts().getItem(item.getID()).isEmpty())
				{
					uses = building.getBuildCosts().getItem(item.getID()).get(0).getMaxUses();
				}
				
				echo.append("<tr><td class=\"noBorderS\"><img src=\""+item.getPicture()+"\" alt=\"\" />"+item.getName()+": </td><td><input type=\"text\" name=\"buildi"+item.getID()+"\" value=\"" + amount + "\"></td><td><input type=\"text\" name=\"buildi"+item.getID()+"uses\" value=\"" + uses + "\"></td></tr>");
			}
			echo.append("<tr><td class=\"noBorderS\"><b>Produktion</b></td><td class=\"noBorderS\">Menge</td></tr>");
			for(ResourceConfig.Entry resource: ResourceConfig.getResources())
			{
				long amount = -1*building.getConsumes().getResourceCount(new WarenID(resource.getId())) +  building.getProduces().getResourceCount(new WarenID(resource.getId()));
				echo.append("<tr><td class=\"noBorderS\"><img src=\""+resource.getImage()+"\" alt=\"\" />"+resource.getName()+": </td><td><input type=\"text\" name=\"prod"+resource.getId()+"\" value=\"" + amount + "\"></td></tr>");
			}
			echo.append("<tr><td class=\"noBorderS\"></td><td class=\"noBorderS\">Menge</td><td class=\"noBorderS\">Nutzungen</td></tr>");
			for(Item item: itemlist)
			{
				long amount = -1*building.getConsumes().getResourceCount(new ItemID(item.getID())) + building.getProduces().getResourceCount(new ItemID(item.getID()));
				int uses = 0;
				if(!building.getConsumes().getItem(item.getID()).isEmpty())
				{
					uses = building.getConsumes().getItem(item.getID()).get(0).getMaxUses();
				}
				
				if(!building.getProduces().getItem(item.getID()).isEmpty())
				{
					uses = building.getProduces().getItem(item.getID()).get(0).getMaxUses();
				}
				
				echo.append("<tr><td class=\"noBorderS\"><img src=\""+item.getPicture()+"\" alt=\"\" />"+item.getName()+": </td><td><input type=\"text\" name=\"i"+item.getID()+"\" value=\"" + amount + "\"></td><td><input type=\"text\" name=\"i"+item.getID()+"uses\" value=\"" + uses + "\"></td></tr>");
			}
			echo.append("<tr><td class=\"noBorderS\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
		}
	}
}
