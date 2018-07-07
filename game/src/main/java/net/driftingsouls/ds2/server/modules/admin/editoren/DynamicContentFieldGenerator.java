package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DynamicContent;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import org.apache.commons.fileupload.FileItem;

import java.io.IOException;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Generator fuer ein {@link DynamicContent}-Editor.
 * @param <V> Der Entity-Typ
 */
public class DynamicContentFieldGenerator<V> implements CustomFieldGenerator<V>
{
	private String label;
	private String name;
	private Function<V,String> getter;
	private BiConsumer<V,String> setter;
	private boolean withRemove;
	private Class<?> plugin;

	public DynamicContentFieldGenerator(Class<?> plugin, String label, String name, Function<V, String> getter, BiConsumer<V, String> setter)
	{
		this.label = label;
		this.name = name;
		this.getter = getter;
		this.setter = setter;
		this.withRemove = false;
		this.plugin = plugin;
	}

	@Override
	public void generate(StringBuilder echo, V entity) throws IOException
	{
		echo.append("<tr class='dynamicContentEdit'>");

		writeCommonDynamicContentPart(echo, getter.apply(entity));

		if( withRemove )
		{
			String entityId = ContextMap.getContext().getRequest().getParameter("entityId");

			echo.append("<td><a title='entfernen' href='./ds?module=admin&amp;namedplugin=").append(plugin.getName()).append("&amp;entityId=").append(entityId).append("&reset=").append(name).append("'>X</a>");
		}

		echo.append("</tr>");
	}


	protected final String processDynamicContent(Request request, String name, String currentValue) throws IOException
	{
		for (FileItem file : request.getUploadedFiles())
		{
			if (name.equals(file.getFieldName()) && file.getSize() > 0)
			{
				String id = DynamicContentManager.add(file);
				if (id != null)
				{
					processDynamicContentMetadata(request, name, id);
					return id;
				}
			}
		}
		if (currentValue != null)
		{
			processDynamicContentMetadata(request, name, currentValue);
		}
		return null;
	}

	private void processDynamicContentMetadata(Request request, String name, String id)
	{
		Context context = ContextMap.getContext();

		DynamicContent metadata = DynamicContentManager.lookupMetadata(id, true);
		String lizenzStr = request.getParameterString(name + "_dc_lizenz");
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
		metadata.setLizenzdetails(request.getParameterString(name + "_dc_lizenzdetails"));
		metadata.setAutor(request.getParameterString(name + "_dc_autor"));
		metadata.setQuelle(request.getParameterString(name + "_dc_quelle"));
		metadata.setAenderungsdatum(new Date());
		if (!context.getDB().contains(metadata))
		{
			context.getDB().persist(metadata);
		}
	}

	@Override
	public void applyRequestValues(Request request, V entity) throws IOException
	{
		String img = processDynamicContent(request, this.name, getter.apply(entity));
		if( img == null )
		{
			return;
		}
		String oldImg = getter.apply(entity);
		setter.accept(entity, "data/dynamicContent/" + img);
		if (oldImg != null && oldImg.startsWith("data/dynamicContent/"))
		{
			DynamicContentManager.remove(oldImg);
		}
	}

	@Override
	public ColumnDefinition<V> getColumnDefinition(boolean forEditing)
	{
		return new ColumnDefinition<>(name, label, String.class, "picture");
	}

	@Override
	public String serializedValueOf(V entity)
	{
		return getter.apply(entity);
	}

	/**
	 * Aktiviert die Funktion zum Entfernen von Bildern.
	 * @return Die Instanz
	 */
	public DynamicContentFieldGenerator<V> withRemove()
	{
		this.withRemove = true;
		return this;
	}

	private void writeCommonDynamicContentPart(StringBuilder echo, String value) throws IOException
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
}
