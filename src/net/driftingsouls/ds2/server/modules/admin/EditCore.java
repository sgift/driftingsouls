package net.driftingsouls.ds2.server.modules.admin;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.WarenID;
import net.driftingsouls.ds2.server.config.ResourceConfig;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Editiert die Werte von Cores.
 * 
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Asteroiden", name = "Core editieren")
public class EditCore implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();
		
		int coreId = context.getRequest().getParameterInt("core");
		List<Core> cores = Common.cast(db.createQuery("from Core").list());
		
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");
		
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select size=\"1\" name=\"core\">");
		for (Core core: cores)
		{
			echo.append("<option value=\"" + core.getId() + "\" " + (core.getId() == coreId ? "selected=\"selected\"" : "") + ">" + core.getName() + "</option>");
		}
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\"");
		echo.append("</form>");
		
		if(update && coreId > 0)
		{
			Cargo buildcosts = new Cargo();
			for(ResourceConfig.Entry resource: ResourceConfig.getResources())
			{
				long amount = context.getRequest().getParameterInt("build"+resource.getId());
				buildcosts.addResource(new WarenID(resource.getId()), amount);
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
				
				Core core = (Core)db.get(Core.class, coreId);
				core.setName(context.getRequest().getParameterString("name"));
				core.setArbeiter(context.getRequest().getParameterInt("worker"));
				int energy = context.getRequest().getParameterInt("energy");
				if(energy < 0)
				{
					energy = -1 * energy;
					core.setEVerbrauch(energy);
					core.setEProduktion(0);
				}
				else
				{
					core.setEProduktion(energy);
					core.setEVerbrauch(0);
				}
				core.setEps(context.getRequest().getParameterInt("eps"));
				core.setBewohner(context.getRequest().getParameterInt("room"));
				core.setTechReq(context.getRequest().getParameterInt("tech"));
				core.setBuildcosts(buildcosts);
				core.setProduces(produces);
				core.setConsumes(consumes);
			}
		}
		
		if(coreId > 0)
		{
			Core core = (Core)db.get(Core.class, coreId);
			
			if(core == null)
			{
				return;
			}
			
			List<Forschung> researchs = Common.cast(db.createQuery("from Forschung").list());
			
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorder\" width=\"100%\">");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"core\" value=\"" + coreId + "\" />\n");
			echo.append("<tr><td class=\"noBorderS\">Name: </td><td><input type=\"text\" name=\"name\" value=\"" + core.getName() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Astitype: </td><td><input type=\"text\" name=\"asti\" value=\"" + core.getAstiType() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Arbeiter: </td><td><input type=\"text\" name=\"worker\" value=\"" + core.getArbeiter() + "\"></td></tr>\n");
			int energy = -1*core.getEVerbrauch() + core.getEProduktion();
			echo.append("<tr><td class=\"noBorderS\">Energie: </td><td><input type=\"text\" name=\"energy\" value=\"" + energy  + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">EPS: </td><td><input type=\"text\" name=\"eps\" value=\"" + core.getEPS() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Wohnraum: </td><td><input type=\"text\" name=\"room\" value=\"" + core.getBewohner() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Forschung: </td><td><select size=\"1\" name=\"tech\">");
			for (Forschung research: researchs)
			{
				echo.append("<option value=\"" + research.getID() + "\" " + (research.getID() == core.getTechRequired() ? "selected=\"selected\"" : "") + ">" + research.getName() + "</option>");
			}
			echo.append("</select>");
			echo.append("<tr><td class=\"noBorderS\">Baukosten</td><td class=\"noBorderS\">Menge</td></tr>");
			for(ResourceConfig.Entry resource: ResourceConfig.getResources())
			{
				long amount = core.getBuildCosts().getResourceCount(new WarenID(resource.getId()));
				echo.append("<tr><td class=\"noBorderS\"><img src=\""+resource.getImage()+"\" alt=\"\" />"+resource.getName()+": </td><td><input type=\"text\" name=\"build"+resource.getId()+"\" value=\"" + amount + "\"></td></tr>");
			}
			echo.append("<tr><td class=\"noBorderS\">Produktion</td><td class=\"noBorderS\">Menge</td></tr>");
			for(ResourceConfig.Entry resource: ResourceConfig.getResources())
			{
				long amount = -1*core.getConsumes().getResourceCount(new WarenID(resource.getId())) + core.getProduces().getResourceCount(new WarenID(resource.getId()));
				echo.append("<tr><td class=\"noBorderS\"><img src=\""+resource.getImage()+"\" alt=\"\" />"+resource.getName()+": </td><td><input type=\"text\" name=\"prod"+resource.getId()+"\" value=\"" + amount + "\"></td></tr>");
			}
			echo.append("<tr><td class=\"noBorderS\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
		}
	}
}
