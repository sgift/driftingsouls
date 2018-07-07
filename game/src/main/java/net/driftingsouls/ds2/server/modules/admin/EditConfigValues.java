package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.admin.editoren.HtmlUtils;

import java.io.IOException;

/**
 * Ein Tool, um die diversen globalen Konfigurationswerte zu aendern.
 * 
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Sonstiges", name = "Configwerte editieren", permission = WellKnownAdminPermission.EDIT_CONFIG_VALUES)
public class EditConfigValues implements AdminPlugin
{
	@Override
	@SuppressWarnings("unchecked")
	public void output(StringBuilder echo) throws IOException
	{
		Context context = ContextMap.getContext();

		WellKnownConfigValue[] configValues = WellKnownConfigValue.values();
		ConfigService configService = new ConfigService();
		
		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");
		if(update)
		{
			for(WellKnownConfigValue value: configValues)
			{
				ConfigValue configValue = configService.get(value);
				String newValue = context.getRequest().getParameterString(value.getName());
				configValue.setValue(newValue);
			}
		}

		echo.append("<div class='gfxbox' style='width:790px'>");
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<table width=\"100%\">");
		for(WellKnownConfigValue value: configValues)
		{
			Object configServiceValue = configService.getValue(value);
			echo.append("<tr><td>").append(value.getName()).append("</td>");
			if( Number.class.isAssignableFrom(value.getType()) )
			{
				echo.append("<td>");
				HtmlUtils.textInput(echo, value.getName(), false, value.getType(), configServiceValue);
				echo.append("</td>");
			}
			else if( Boolean.class.isAssignableFrom(value.getType()) )
			{
				echo.append("<td><input type=\"checkbox\" name=\"").append(value.getName());
				echo.append("\" value=\"true\" ").append(Boolean.TRUE.equals(configServiceValue) ? "checked=\"checked\"" : "").append(" /></td>");
			}
			else
			{
				echo.append("<td><textarea name=\"").append(value.getName()).append("\" rows=\"2\" cols=\"30\">");
				echo.append(configServiceValue != null ? configServiceValue.toString() : "").append("</textarea></td>");
			}
			echo.append("<td>").append(value.getDescription()).append("</td></tr>");
		}
		echo.append("<tr><td></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
		echo.append("</table>");
		echo.append("</div>");
	}
}
