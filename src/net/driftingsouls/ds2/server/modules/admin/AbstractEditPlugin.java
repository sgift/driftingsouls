package net.driftingsouls.ds2.server.modules.admin;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

/**
 * Bsisklasse fuer einfache Editor-Plugins (fuer einzelne Entities). Stellt
 * regelmaessig benoetigte Funktionen bereit.
 * @author christopherjung
 *
 */
public abstract class AbstractEditPlugin implements AdminPlugin
{
	protected void beginSelectionBox(Writer echo, String page, int action) throws IOException
	{
		echo.append("<div class='gfxbox' style='width:390px'>");
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select size=\"1\" name=\"entityId\">");
	}

	protected void addSelectionOption(Writer echo, Object id, String label) throws IOException
	{
		Context context = ContextMap.getContext();
		String currentIdStr = context.getRequest().getParameter("entityid");
		String idStr = id.toString();

		echo.append("<option value=\"" + idStr + "\" " + (idStr.equals(currentIdStr) ? "selected=\"selected\"" : "") + ">" + label + "</option>");
	}

	protected void endSelectionBox(Writer echo) throws IOException
	{
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form>");
		echo.append("</div>");
	}

	protected void beginEditorTable(Writer echo, String page, int action, Object entityId) throws IOException
	{
		echo.append("<div class='gfxbox' style='width:600px'>");
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"hidden\" name=\"entityId\" value=\"" + entityId + "\" />\n");

		echo.append("<table width=\"100%\">");
	}

	protected void endEditorTable(Writer echo) throws IOException
	{
		echo.append("<tr><td></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
		echo.append("</table>");
		echo.append("</form>\n");
		echo.append("</div>");
	}

	protected void editField(Writer echo, String label, String name, Class<?> type, Object value) throws IOException
	{
		echo.append("<tr>");
		echo.append("<td>"+label+":</td>");
		echo.append("<td>");
		if( Cargo.class.isAssignableFrom(type) )
		{
			echo.append("<input type=\"hidden\" name=\""+name+"\" id='"+name+"' value=\"" + (value != null ? value : new Cargo()) + "\">");
			echo.append("<script type='text/javascript'>$(document).ready(function(){new CargoEditor('#"+name+"')});</script>");
		}
		else if( Forschung.class.isAssignableFrom(type) )
		{
			echo.append("<select size=\"1\" name=\"tech\">");
			org.hibernate.Session db = ContextMap.getContext().getDB();

			int selected =-1;
			if( value instanceof Number )
			{
				selected = ((Number)value).intValue();
			}
			else if( value instanceof Forschung )
			{
				selected = ((Forschung)value).getID();
			}

			List<Forschung> researchs = Common.cast(db.createQuery("from Forschung").list());
			for (Forschung research: researchs)
			{
				echo.append("<option value=\"" + research.getID() + "\" " + (research.getID() == selected ? "selected=\"selected\"" : "") + ">" + research.getName() + "</option>");
			}
			echo.append("</select>");
		}
		else if( Boolean.class.isAssignableFrom(type) )
		{
			boolean bool = false;
			if( value != null )
			{
				bool = ((Boolean)value).booleanValue();
			}
			echo.append("<input type=\"checkbox\" name=\""+name+"\" value=\"true\" "+(bool ? "checked='checked'" : "" )+" \">");
		}
		else {
			echo.append("<input type=\"text\" name=\""+name+"\" value=\"" + value + "\">");
		}
		echo.append("</td></tr>\n");
	}

	protected boolean isUpdateExecuted()
	{
		Context context = ContextMap.getContext();
		return context.getRequest().getParameterString("change").equals("Aktualisieren");
	}
}
