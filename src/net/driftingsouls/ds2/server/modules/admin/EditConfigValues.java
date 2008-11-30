package net.driftingsouls.ds2.server.modules.admin;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Ein Tool, um die diversen globalen Konfigurationswerte zu aendern.
 * 
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Sonstiges", name = "Configwerte editieren")
public class EditConfigValues implements AdminPlugin
{
	@Override
	@SuppressWarnings("unchecked")
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();
		
		List<ConfigValue> configValues = db.createQuery("from ConfigValue").list();
		
		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");
		if(update)
		{
			for(ConfigValue value: configValues)
			{
				String newValue = context.getRequest().getParameterString(value.getName());
				value.setValue(newValue);
			}
		}
		
		echo.append(Common.tableBegin(750,"left"));
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<table class=\"noBorderX\" width=\"100%\">");
		for(ConfigValue value: configValues)
		{
			echo.append("<tr><td class=\"noBorderX\">"+value.getName()+"</td>" +
					"<td class=\"noBorderX\"><textarea name=\""+ value.getName() +"\" rows=\"2\" cols=\"30\">" + value.getValue() + "</textarea></td>" +
					"<td class=\"noBorderX\">"+value.getDescription()+"</td></tr>");
		}
		echo.append("<tr><td class=\"noBorderX\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
		echo.append("</table>");
		echo.append(Common.tableEnd());
	}
}