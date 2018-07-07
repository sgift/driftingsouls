package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DynamicContent;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import org.apache.commons.fileupload.FileItem;
import org.hibernate.Session;

import javax.persistence.Entity;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Bsisklasse fuer einfache Editor-Plugins (fuer einzelne Entities). Stellt
 * regelmaessig benoetigte Funktionen bereit.
 *
 * @author christopherjung
 */
public abstract class AbstractEditPlugin<T> implements AdminPlugin
{
	private Class<T> clazz;

	protected AbstractEditPlugin(Class<T> clazz)
	{
		this.clazz = clazz;
	}

	@Override
	public final void output(StringBuilder echo) throws IOException
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		Request request = context.getRequest();
		int entityId = request.getParameterInt("entityId");

		if (this.isUpdateExecuted())
		{
			try
			{
				@SuppressWarnings("unchecked") T entity = (T) db.get(this.clazz, entityId);
				if (isUpdatePossible(entity))
				{
					update(new DefaultStatusWriter(echo), entity);
					echo.append("<p>Update abgeschlossen.</p>");
				}
			}
			catch (IOException | RuntimeException e)
			{
				echo.append("<p>Fehler bei Update: ").append(e.getMessage());
			}
		}
		else if (this.isResetExecuted())
		{
			try
			{
				@SuppressWarnings("unchecked") T entity = (T) db.get(this.clazz, entityId);
				reset(new DefaultStatusWriter(echo), entity);
				echo.append("<p>Update abgeschlossen.</p>");
			}
			catch (IOException | RuntimeException e)
			{
				echo.append("<p>Fehler bei Reset: ").append(e.getMessage());
			}
		}

		List<Building> entities = Common.cast(db.createCriteria(clazz).list());

		beginSelectionBox(echo);
		for (Object entity : entities)
		{
			addSelectionOption(echo, db.getIdentifier(entity), generateLabelFor(null, entity));
		}
		endSelectionBox(echo);

		if (entityId != 0)
		{
			@SuppressWarnings("unchecked") T entity = (T) db.get(clazz, entityId);
			if (entity == null)
			{
				return;
			}

			try (EditorForm form = beginEditorTable(echo, entityId))
			{
				edit(form, entity);
			}
		}
	}

	protected final Session getDB()
	{
		return ContextMap.getContext().getDB();
	}

	public interface StatusWriter
	{
		StatusWriter append(String text);
	}

	public class DefaultStatusWriter implements StatusWriter
	{
		private StringBuilder echo;

		public DefaultStatusWriter(StringBuilder echo)
		{
			this.echo = echo;
		}

		@Override
		public StatusWriter append(String text)
		{
			this.echo.append(text);
			return this;
		}
	}

	protected abstract void update(StatusWriter writer, T entity) throws IOException;

	protected void reset(StatusWriter writer, T entity) throws IOException
	{
	}

	protected abstract void edit(EditorForm form, T entity);

	protected boolean isUpdatePossible(T entity)
	{
		return true;
	}

	protected static String generateLabelFor(Serializable identifier, Object entity)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		if (identifier == null && entity.getClass().isAnnotationPresent(Entity.class))
		{
			identifier = db.getIdentifier(entity);
		}

		if( entity instanceof User)
		{
			return (((User) entity).getPlainname())+" ("+identifier+")";
		}

		Class<?> type = entity.getClass();

		Method labelMethod = null;
		for (String m : Arrays.asList("getName", "getNickname", "toString"))
		{
			try
			{
				labelMethod = type.getMethod(m);
				break;
			}
			catch (NoSuchMethodException e)
			{
				// Ignore
			}
		}

		if( labelMethod == null )
		{
			throw new AssertionError("No toString");
		}

		try
		{
			return labelMethod.invoke(entity).toString() + (identifier != null ? " (" + identifier + ")" : "");
		}
		catch (IllegalAccessException | InvocationTargetException e)
		{
			throw new IllegalStateException(e);
		}
	}

	protected final String processDynamicContent(String name, String currentValue) throws IOException
	{
		Context context = ContextMap.getContext();

		for (FileItem file : context.getRequest().getUploadedFiles())
		{
			if (name.equals(file.getFieldName()) && file.getSize() > 0)
			{
				String id = DynamicContentManager.add(file);
				if (id != null)
				{
					processDynamicContentMetadata(name, id);
					return id;
				}
			}
		}
		if (currentValue != null)
		{
			processDynamicContentMetadata(name, currentValue);
		}
		return null;
	}

	private void processDynamicContentMetadata(String name, String id)
	{
		Context context = ContextMap.getContext();

		DynamicContent metadata = DynamicContentManager.lookupMetadata(id, true);
		String lizenzStr = context.getRequest().getParameterString(name + "_dc_lizenz");
		if (!lizenzStr.isEmpty())
		{
			try
			{
				metadata.setLizenz(DynamicContent.Lizenz.valueOf(lizenzStr));
			}
			catch (IllegalArgumentException e)
			{
				// EMPTY
			}
		}
		metadata.setLizenzdetails(context.getRequest().getParameterString(name + "_dc_lizenzdetails"));
		metadata.setAutor(context.getRequest().getParameterString(name + "_dc_autor"));
		metadata.setQuelle(context.getRequest().getParameterString(name + "_dc_quelle"));
		metadata.setAenderungsdatum(new Date());
		if (!context.getDB().contains(metadata))
		{
			context.getDB().persist(metadata);
		}
	}

	private void beginSelectionBox(StringBuilder echo) throws IOException
	{
		echo.append("<div class='gfxbox adminSelection' style='width:390px'>");
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select size=\"1\" name=\"entityId\">");
	}

	private void addSelectionOption(StringBuilder echo, Object id, String label) throws IOException
	{
		Context context = ContextMap.getContext();
		String currentIdStr = context.getRequest().getParameter("entityId");
		String idStr = id.toString();

		echo.append("<option value=\"").append(idStr).append("\" ").append(idStr.equals(currentIdStr) ? "selected=\"selected\"" : "").append(">").append(label).append("</option>");
	}

	private void endSelectionBox(StringBuilder echo) throws IOException
	{
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form>");
		echo.append("</div>");
	}

	private EditorForm beginEditorTable(final StringBuilder echo, Object entityId) throws IOException
	{
		echo.append("<div class='gfxbox adminEditor' style='width:700px'>");
		echo.append("<form action=\"./ds\" method=\"post\" enctype='multipart/form-data'>");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"hidden\" name=\"entityId\" value=\"").append(entityId != null ? entityId.toString() : "").append("\" />\n");

		echo.append("<table>");

		return new EditorForm(getClass(), echo);
	}

	protected final boolean isResetted(String name)
	{
		Context context = ContextMap.getContext();
		String reset = context.getRequest().getParameterString("reset");
		return name.equals(reset);
	}

	private boolean isResetExecuted()
	{
		Context context = ContextMap.getContext();
		String reset = context.getRequest().getParameterString("reset");
		return reset != null && !reset.trim().isEmpty();
	}

	private boolean isUpdateExecuted()
	{
		Context context = ContextMap.getContext();
		String change = context.getRequest().getParameterString("change");
		return "Aktualisieren".equals(change);
	}
}
