package net.driftingsouls.ds2.server.modules.admin;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DynamicContent;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;
import org.apache.commons.fileupload.FileItem;

import javax.persistence.Entity;

/**
 * Bsisklasse fuer einfache Editor-Plugins (fuer einzelne Entities). Stellt
 * regelmaessig benoetigte Funktionen bereit.
 * @author christopherjung
 *
 */
public abstract class AbstractEditPlugin implements AdminPlugin
{
	private String page;
	private int action;

	protected String processDynamicContent(String name, String currentValue) throws IOException
	{
		Context context = ContextMap.getContext();

		for( FileItem file : context.getRequest().getUploadedFiles() )
		{
			if( name.equals(file.getFieldName()) && file.getSize() > 0 )
			{
				String id = DynamicContentManager.add(file);
				if( id != null )
				{
					processDynamicContentMetadata(name, id);
					return id;
				}
			}
		}
		if( currentValue != null )
		{
			processDynamicContentMetadata(name, currentValue);
		}
		return null;
	}

	private void processDynamicContentMetadata(String name, String id)
	{
		Context context = ContextMap.getContext();

		DynamicContent metadata = DynamicContentManager.lookupMetadata(id, true);
		String lizenzStr = context.getRequest().getParameterString(name+"_dc_lizenz");
		if( !lizenzStr.isEmpty() )
		{
			try
			{
				metadata.setLizenz(DynamicContent.Lizenz.valueOf(lizenzStr));
			}
			catch( IllegalArgumentException e )
			{
				// EMPTY
			}
		}
		metadata.setLizenzdetails(context.getRequest().getParameterString(name + "_dc_lizenzdetails"));
		metadata.setAutor(context.getRequest().getParameterString(name+"_dc_autor"));
		metadata.setQuelle(context.getRequest().getParameterString(name + "_dc_quelle"));
		metadata.setAenderungsdatum(new Date());
		if( !context.getDB().contains(metadata) )
		{
			context.getDB().persist(metadata);
		}
	}

	protected void beginSelectionBox(Writer echo, String page, int action) throws IOException
	{
		this.page = page;
		this.action = action;
		echo.append("<div class='gfxbox adminSelection' style='width:390px'>");
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select size=\"1\" name=\"entityId\">");
	}

	protected void addSelectionOption(Writer echo, Object id, String label) throws IOException
	{
		Context context = ContextMap.getContext();
		String currentIdStr = context.getRequest().getParameter("entityId");
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
		this.page = page;
		this.action = action;
		echo.append("<div class='gfxbox adminEditor' style='width:700px'>");
		echo.append("<form action=\"./ds\" method=\"post\" enctype='multipart/form-data'>");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"hidden\" name=\"entityId\" value=\"" + entityId + "\" />\n");

		echo.append("<table width=\"100%\">");
	}

	protected void endEditorTable(Writer echo) throws IOException
	{
		echo.append("<tr><td colspan='2'></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
		echo.append("</table>");
		echo.append("</form>\n");
		echo.append("</div>");
	}

	protected void editDynamicContentField(Writer echo, String label, String name, String value) throws IOException
	{
		echo.append("<tr class='dynamicContentEdit'>");

		writeCommonDynamicContentPart(echo, label, name, value);

		echo.append("</tr>");
	}

	protected void editDynamicContentFieldWithRemove(Writer echo, String label, String name, String value) throws IOException
	{
		echo.append("<tr class='dynamicContentEdit'>");
		writeCommonDynamicContentPart(echo, label, name, value);

		String entityId = ContextMap.getContext().getRequest().getParameter("entityId");

		echo.append("<td><a title='entfernen' href='./ds?module=admin&amp;page="+page+"&amp;act="+action+"&amp;entityId="+entityId+"&reset="+name+"'>X</a>");

		echo.append("</tr>");
	}

	private void writeCommonDynamicContentPart(Writer echo, String label, String name, String value) throws IOException
	{
		echo.append("<td>"+label+": </td>" +
				"<td>"+(value != null && !value.trim().isEmpty() ? "<img src='"+value+"' />" : "")+"</td>" +
				"<td>");

		DynamicContent content = DynamicContentManager.lookupMetadata(value != null ? value : "dummy", true);

		echo.append("<input type=\"file\" name=\""+name+"\">");

		echo.append("<table>");
		echo.append("<tr><td>Lizenz</td><td><select name='"+name+"_dc_lizenz'>");
		echo.append("<option>Bitte wählen</option>");
		for( DynamicContent.Lizenz lizenz : DynamicContent.Lizenz.values() )
		{
			echo.append("<option value='"+lizenz.name()+"' "+(content.getLizenz() == lizenz ? "selected='selected'" : "")+">"+lizenz.getLabel()+"</option>");
		}
		echo.append("</select></tr>");
		echo.append("<tr><td>Lizenzdetails</td><td><textarea rows='3' cols='30' name='"+name+"_dc_lizenzdetails'>"+content.getLizenzdetails()+"</textarea></td></tr>");
		echo.append("<tr><td>Quelle</td><td><input type='text' maxlength='255' name='"+name+"_dc_quelle' value='"+content.getQuelle()+"'/></td></tr>");
		echo.append("<tr><td>Autor (RL-Name+Nick)</td><td><input type='text' maxlength='255' name='"+name+"_dc_autor' value='"+content.getAutor()+"'/></td></tr>");
		echo.append("</table>");

		echo.append("</td>\n");
	}

	protected void editLabel(Writer echo, String label, Object value) throws IOException
	{
		echo.append("<tr>");
		echo.append("<td colspan='2'>"+(label.trim().isEmpty() ? "" : label+":")+"</td>"+
				"<td>"+value+"</td></tr>\n");
	}

	protected void editTextArea(Writer echo, String label, String name, Object value) throws IOException
	{
		echo.append("<tr>");
		echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>");
		echo.append("<td>");
		echo.append("<textarea rows='3' cols='60' name=\"").append(name).append("\">").append(value != null ? value.toString() : "").append("</textarea>");
		echo.append("</td></tr>\n");
	}

	protected void editField(Writer echo, String label, String name, Class<?> type, Object value) throws IOException
	{
		echo.append("<tr>");
		echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>");
		echo.append("<td>");
		if( Cargo.class.isAssignableFrom(type) )
		{
			echo.append("<input type=\"hidden\" name=\"").append(name).append("\" id='").append(name).append("' value=\"").append(value != null ? value.toString() : new Cargo().toString()).append("\">");
			echo.append("<script type='text/javascript'>$(document).ready(function(){new CargoEditor('#").append(name).append("')});</script>");
		}
		else if( type.isAnnotationPresent(Entity.class) )
		{
			editEntityBySelection(echo, name, type, value);
		}
		else if( Boolean.class.isAssignableFrom(type) )
		{
			boolean bool = false;
			if( value != null )
			{
				bool = (Boolean) value;
			}
			echo.append("<input type=\"checkbox\" name=\"").append(name).append("\" value=\"true\" ").append(bool ? "checked='checked'" : "").append(" \">");
		}
		else {
			echo.append("<input type=\"text\" name=\"").append(name).append("\" value=\"").append(value != null ? value.toString() : "").append("\">");
		}
		echo.append("</td></tr>\n");
	}

	private void editEntityBySelection(Writer echo, String name, Class<?> type, Object value) throws IOException
	{
		echo.append("<select size=\"1\" name=\"").append(name).append("\">");
		org.hibernate.Session db = ContextMap.getContext().getDB();

		Serializable selected = -1;
		if( value instanceof Number )
		{
			selected = (Number)value;
		}
		else if( type.isInstance(value) )
		{
			selected = db.getIdentifier(value);
		}

		Method labelMethod;
		try
		{
			labelMethod = type.getMethod("getName");
		}
		catch( NoSuchMethodException e ) {
			try
			{
				labelMethod = type.getMethod("toString");
			}
			catch (NoSuchMethodException e1)
			{
				throw new AssertionError("No toString");
			}
		}
		List<?> editities = Common.cast(db.createCriteria(type).list());
		for (Object entity : editities)
		{
			Serializable identifier = db.getIdentifier(entity);
			try
			{
				echo.append("<option value=\"").append(identifier.toString()).append("\" ").append(identifier.equals(selected) ? "selected=\"selected\"" : "").append(">").append(labelMethod.invoke(entity).toString()).append("</option>");
			}
			catch (IllegalAccessException | InvocationTargetException e)
			{
				throw new IllegalStateException(e);
			}
		}
		echo.append("</select>");
	}

	protected boolean isResetted(String name)
	{
		Context context = ContextMap.getContext();
		String reset = context.getRequest().getParameterString("reset");
		return name.equals(reset);
	}

	protected boolean isResetExecuted()
	{
		Context context = ContextMap.getContext();
		String reset = context.getRequest().getParameterString("reset");
		return reset != null && !reset.trim().isEmpty();
	}

	protected boolean isUpdateExecuted()
	{
		Context context = ContextMap.getContext();
		String change = context.getRequest().getParameterString("change");
		return "Aktualisieren".equals(change);
	}
}
