package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DynamicContent;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;

import javax.persistence.Entity;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.List;

/**
 * Klasse zum Erstellen eines Eingabeformulars.
 */
class EditorForm implements AutoCloseable
{
	private Writer echo;
	private int action;
	private String page;

	public EditorForm(int action, String page, Writer echo)
	{
		this.echo = echo;
		this.action = action;
		this.page = page;
	}

	public void dynamicContentField(String label, String name, String value)
	{
		try
		{
			echo.append("<tr class='dynamicContentEdit'>");

			writeCommonDynamicContentPart(label, name, value);

			echo.append("</tr>");
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	public void dynamicContentFieldWithRemove(String label, String name, String value)
	{
		try
		{
			echo.append("<tr class='dynamicContentEdit'>");
			writeCommonDynamicContentPart(label, name, value);

			String entityId = ContextMap.getContext().getRequest().getParameter("entityId");

			echo.append("<td><a title='entfernen' href='./ds?module=admin&amp;page=").append(page).append("&amp;act=").append(Integer.toString(action)).append("&amp;entityId=").append(entityId).append("&reset=").append(name).append("'>X</a>");

			echo.append("</tr>");
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private void writeCommonDynamicContentPart(String label, String name, String value) throws IOException
	{
		echo.append("<td>").append(label).append(": </td>").append("<td>").append(value != null && !value.trim().isEmpty() ? "<img src='" + value + "' />" : "").append("</td>").append("<td>");

		DynamicContent content = DynamicContentManager.lookupMetadata(value != null ? value : "dummy", true);

		echo.append("<input type=\"file\" name=\"").append(name).append("\">");

		echo.append("<table>");
		echo.append("<tr><td>Lizenz</td><td><select name='").append(name).append("_dc_lizenz'>");
		echo.append("<option>Bitte wählen</option>");
		for (DynamicContent.Lizenz lizenz : DynamicContent.Lizenz.values())
		{
			echo.append("<option value='").append(lizenz.name()).append("' ").append(content.getLizenz() == lizenz ? "selected='selected'" : "").append(">").append(lizenz.getLabel()).append("</option>");
		}
		echo.append("</select></tr>");
		echo.append("<tr><td>Lizenzdetails</td><td><textarea rows='3' cols='30' name='").append(name).append("_dc_lizenzdetails'>").append(content.getLizenzdetails()).append("</textarea></td></tr>");
		echo.append("<tr><td>Quelle</td><td><input type='text' maxlength='255' name='").append(name).append("_dc_quelle' value='").append(content.getQuelle()).append("'/></td></tr>");
		echo.append("<tr><td>Autor (RL-Name+Nick)</td><td><input type='text' maxlength='255' name='").append(name).append("_dc_autor' value='").append(content.getAutor()).append("'/></td></tr>");
		echo.append("</table>");

		echo.append("</td>\n");
	}

	public void label(String label, Object value)
	{
		try
		{
			echo.append("<tr>");
			echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>").append("<td>").append(value != null ? value.toString() : "").append("</td></tr>\n");
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	public void textArea(String label, String name, Object value)
	{
		try
		{
			echo.append("<tr>");
			echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>");
			echo.append("<td>");
			echo.append("<textarea rows='3' cols='60' name=\"").append(name).append("\">").append(value != null ? value.toString() : "").append("</textarea>");
			echo.append("</td></tr>\n");
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	public static interface CustomFieldGenerator
	{
		public void generate(Writer echo) throws IOException;
	}

	public void custom(CustomFieldGenerator generator)
	{
		try {
			generator.generate(echo);
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	public void field(String label, String name, Class<?> type, Object value)
	{
		try
		{
			echo.append("<tr>");
			echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>");
			echo.append("<td>");
			if (Cargo.class.isAssignableFrom(type))
			{
				echo.append("<input type=\"hidden\" name=\"").append(name).append("\" id='").append(name).append("' value=\"").append(value != null ? value.toString() : new Cargo().toString()).append("\">");
				echo.append("<script type='text/javascript'>$(document).ready(function(){new CargoEditor('#").append(name).append("')});</script>");
			}
			else if (type.isAnnotationPresent(Entity.class))
			{
				editEntityBySelection(name, type, value);
			}
			else if (Boolean.class.isAssignableFrom(type))
			{
				boolean bool = false;
				if (value != null)
				{
					bool = (Boolean) value;
				}
				echo.append("<input type=\"checkbox\" name=\"").append(name).append("\" value=\"true\" ").append(bool ? "checked='checked'" : "").append(" \">");
			}
			else
			{
				echo.append("<input type=\"text\" name=\"").append(name).append("\" value=\"").append(value != null ? value.toString() : "").append("\">");
			}
			echo.append("</td></tr>\n");
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private void editEntityBySelection(String name, Class<?> type, Object value) throws IOException
	{
		echo.append("<select size=\"1\" name=\"").append(name).append("\">");
		org.hibernate.Session db = ContextMap.getContext().getDB();

		Serializable selected = -1;
		if (value instanceof Number)
		{
			selected = (Number) value;
		}
		else if (type.isInstance(value))
		{
			selected = db.getIdentifier(value);
		}

		List<?> editities = Common.cast(db.createCriteria(type).list());
		for (Object entity : editities)
		{
			Serializable identifier = db.getIdentifier(entity);
			echo.append("<option value=\"").append(identifier.toString()).append("\" ").append(identifier.equals(selected) ? "selected=\"selected\"" : "").append(">").append(AbstractEditPlugin.generateLabelFor(entity)).append("</option>");
		}
		echo.append("</select>");
	}

	@Override
	public void close()
	{
		try
		{
			echo.append("<tr><td colspan='2'></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
			echo.append("</div>");
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
