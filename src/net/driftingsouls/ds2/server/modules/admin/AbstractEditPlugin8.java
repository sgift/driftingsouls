package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.modules.AdminController;
import org.hibernate.Session;

import javax.persistence.Entity;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Bsisklasse fuer einfache Editor-Plugins (fuer einzelne Entities). Stellt
 * regelmaessig benoetigte Funktionen bereit.
 *
 * @author christopherjung
 */
public abstract class AbstractEditPlugin8<T> implements AdminPlugin
{
	private Class<T> clazz;

	protected AbstractEditPlugin8(Class<T> clazz)
	{
		this.clazz = clazz;
	}

	@Override
	public final void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Session db = context.getDB();

		Writer echo = context.getResponse().getWriter();

		Request request = context.getRequest();
		int entityId = request.getParameterInt("entityId");

		if (this.isUpdateExecuted())
		{
			try
			{
				@SuppressWarnings("unchecked") T entity = (T) db.get(this.clazz, entityId);
				if (isUpdatePossible(entity))
				{
					EditorForm8 form = new EditorForm8(action, page, echo);
					configureFor(form, entity);
					form.applyRequestValues(request);
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

		beginSelectionBox(echo, page, action);
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

			EditorForm8 form = beginEditorTable(echo, page, action, entityId);
			configureFor(form, entity);
			form.generateForm();
		}
	}

	protected final Session getDB()
	{
		return ContextMap.getContext().getDB();
	}

	public interface StatusWriter
	{
		public StatusWriter append(String text);
	}

	public class DefaultStatusWriter implements StatusWriter
	{
		private Writer echo;

		public DefaultStatusWriter(Writer echo)
		{
			this.echo = echo;
		}

		@Override
		public StatusWriter append(String text)
		{
			try
			{
				this.echo.append(text);
			}
			catch (IOException e)
			{
				throw new IllegalStateException(e);
			}
			return this;
		}
	}

	protected void reset(StatusWriter writer, T entity) throws IOException
	{
	}

	protected abstract void configureFor(EditorForm8 form, T entity);

	protected boolean isUpdatePossible(T entity)
	{
		return true;
	}

	protected static String generateLabelFor(Serializable identifier, Object entity)
	{
		Context context = ContextMap.getContext();
		Session db = context.getDB();

		if (identifier == null && entity.getClass().isAnnotationPresent(Entity.class))
		{
			identifier = db.getIdentifier(entity);
		}

		if( entity instanceof User)
		{
			return (((User) entity).getPlainname())+" ("+identifier+")";
		}

		Class<?> type = entity.getClass();

		Method labelMethod;
		try
		{
			labelMethod = type.getMethod("getName");
		}
		catch (NoSuchMethodException e)
		{
			try
			{
				labelMethod = type.getMethod("toString");
			}
			catch (NoSuchMethodException e1)
			{
				throw new AssertionError("No toString");
			}
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

	private void beginSelectionBox(Writer echo, String page, int action) throws IOException
	{
		echo.append("<div class='gfxbox adminSelection' style='width:390px'>");
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"").append(page).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"").append(Integer.toString(action)).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select size=\"1\" name=\"entityId\">");
	}

	private void addSelectionOption(Writer echo, Object id, String label) throws IOException
	{
		Context context = ContextMap.getContext();
		String currentIdStr = context.getRequest().getParameter("entityId");
		String idStr = id.toString();

		echo.append("<option value=\"").append(idStr).append("\" ").append(idStr.equals(currentIdStr) ? "selected=\"selected\"" : "").append(">").append(label).append("</option>");
	}

	private void endSelectionBox(Writer echo) throws IOException
	{
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form>");
		echo.append("</div>");
	}

	private EditorForm8 beginEditorTable(final Writer echo, String page, int action, Object entityId) throws IOException
	{
		echo.append("<div class='gfxbox adminEditor' style='width:700px'>");
		echo.append("<form action=\"./ds\" method=\"post\" enctype='multipart/form-data'>");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"").append(page).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"").append(Integer.toString(action)).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"hidden\" name=\"entityId\" value=\"").append(entityId != null ? entityId.toString() : "").append("\" />\n");

		echo.append("<table width=\"100%\">");

		return new EditorForm8(action, page, echo);
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
