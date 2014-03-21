package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DynamicContent;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import org.apache.commons.fileupload.FileItem;

import javax.persistence.Entity;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Klasse zum Erstellen eines Eingabeformulars.
 */
public class EditorForm8<E>
{
	public static class Job<E,T>
	{
		public final String name;
		public final Function<E,? extends Collection<T>> supplier;
		public final Consumer<T> job;

		public Job(String name, Function<E, ? extends Collection<T>> supplier, Consumer<T> job)
		{
			this.name = name;
			this.supplier = supplier;
			this.job = job;
		}

		public static <E> Job<E,Boolean> forRunnable(String name, Runnable job)
		{
			return new Job<>(name, (entity) -> Arrays.asList(Boolean.TRUE), (b) -> job.run());
		}
	}

	private final EditorMode modus;
	private Writer echo;
	private int action;
	private String page;
	private List<CustomFieldGenerator<E>> fields = new ArrayList<>();
	private int counter;
	private boolean allowAdd;
	private List<Job<E,?>> updateTasks = new ArrayList<>();

	public EditorForm8(EditorMode modus, int action, String page, Writer echo)
	{
		this.modus = modus;
		this.echo = echo;
		this.action = action;
		this.page = page;
		this.counter = 0;
		this.allowAdd = false;
	}

	/**
	 * Aktiviert das Hinzufuegen von Entities.
	 */
	public void allowAdd()
	{
		this.allowAdd = true;
	}

	/**
	 * Gibt zurueck, ob das Hinzufuegen von Entities erlaubt ist.
	 * @return <code>true</code> falls dem so ist
	 */
	public boolean isAddAllowed()
	{
		return this.allowAdd;
	}

	public void addUpdateTask(String name, Runnable job)
	{
		updateTasks.add(Job.forRunnable(name, job));
	}

	public <T> void updateTask(String name, Function<E, ? extends Collection<T>> supplier, Consumer<T> job)
	{
		updateTasks.add(new Job<>(name, supplier, job));
	}

	public List<Job<E,?>> getUpdateTasks()
	{
		return this.updateTasks;
	}

	/**
	 * Standardinterface fuer Feldgeneratoren. Jede Instanz generiert genau
	 * ein Feld fuer ein konkretes Form.
	 */
	public static interface CustomFieldGenerator<V>
	{
		/**
		 * Generiert den HTML-Code fuer das Eingabefeld.
		 * @param echo Der Writer in den der HTML-Code geschrieben werden soll
		 * @param entity Die Entity-Instanz zu der das Feld generiert werden soll
		 * @throws java.io.IOException Bei I/O-Fehlern
		 */
		public void generate(Writer echo, V entity) throws IOException;

		/**
		 * Liesst die Angaben zum Feld aus der Request und speichert sie an der
		 * angebenen Entity.
		 * @param request Die Request
		 * @param entity Die Entity
		 * @throws IOException Bei IO-Fehlern
		 */
        public void applyRequestValues(Request request, V entity) throws IOException;
    }

	/**
	 * Fuegt einen Generator fuer ein Eingabefeld zum Form hinzu.
	 * @param generator Der Generator
	 * @param <T> Der Typ des Generators
	 * @return Der Generator
	 */
	public <T extends CustomFieldGenerator<E>> T custom(T generator)
	{
		fields.add(generator);
		return generator;
	}

	public static class DynamicContentFieldGenerator<V> implements CustomFieldGenerator<V>
	{
		private String label;
		private String name;
		private Function<V,String> getter;
		private BiConsumer<V,String> setter;
		private boolean withRemove;
		private int action;
		private String page;

		public DynamicContentFieldGenerator(int action, String page, String label, String name, Function<V, String> getter, BiConsumer<V, String> setter)
		{
			this.label = label;
			this.name = name;
			this.getter = getter;
			this.setter = setter;
			this.withRemove = false;
			this.action = action;
			this.page = page;
		}

		@Override
		public void generate(Writer echo, V entity) throws IOException
		{
			echo.append("<tr class='dynamicContentEdit'>");

			writeCommonDynamicContentPart(echo, getter.apply(entity));

			if( withRemove )
			{
				String entityId = ContextMap.getContext().getRequest().getParameter("entityId");

				echo.append("<td><a title='entfernen' href='./ds?module=admin&amp;page=").append(page).append("&amp;act=").append(Integer.toString(action)).append("&amp;entityId=").append(entityId).append("&reset=").append(name).append("'>X</a>");
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
			String oldImg = getter.apply(entity);
			setter.accept(entity, "data/dynamicContent/"+img);
			if( oldImg.startsWith("data/dynamicContent/") )
			{
				DynamicContentManager.remove(oldImg);
			}
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

		private void writeCommonDynamicContentPart(Writer echo, String value) throws IOException
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

	/**
	 * Generiert ein Eingabefeld (Editor) fuer ein via {@link net.driftingsouls.ds2.server.framework.DynamicContent}
	 * gemanagetes Bild.
	 * @param label Der Label fuer das Eingabefeld
	 * @param getter Der getter für das Feld
	 * @return Der erzeugte Generator
	 */
	public DynamicContentFieldGenerator<E> dynamicContentField(String label, Function<E,String> getter, BiConsumer<E,String> setter)
	{
		return custom(new DynamicContentFieldGenerator<>(action, page, label, generateName(getter.getClass().getSimpleName()), getter, setter));
	}

	public static class LabelGenerator<V, T> implements CustomFieldGenerator<V>
	{
		private final String label;
		private final Function<V,T> getter;

		public LabelGenerator(String label, Function<V,T> getter)
		{
			this.label = label;
			this.getter = getter;
		}

		@Override
		public void generate(Writer echo, V entity) throws IOException
		{
			T value = getter.apply(entity);
			echo.append("<tr>");
			echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>").append("<td>").append(value != null ? value.toString() : "").append("</td></tr>\n");
		}

        @Override
        public void applyRequestValues(Request request, V entity)
        {
        }
    }

	/**
	 * Erzeugt ein Eingabefeld (Editor) in Form eines nicht editierbaren Werts.
	 * @param label Der Label zum Wert
	 * @param getter Der getter des Werts
	 */
	public <T> LabelGenerator<E,T> label(String label, Function<E,T> getter)
	{
		return custom(new LabelGenerator<>(label, getter));
	}

	public static class TextAreaGenerator<V> implements CustomFieldGenerator<V>
	{
		private final String label;
		private final String name;
		private final Function<V,String> getter;
		private final BiConsumer<V,String> setter;

		public TextAreaGenerator(String label, String name, Function<V,String> getter, BiConsumer<V,String> setter)
		{
			this.label = label;
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		@Override
		public void generate(Writer echo, V entity) throws IOException
		{
			String value = getter.apply(entity);
			echo.append("<tr>");
			echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>");
			echo.append("<td>");
			echo.append("<textarea rows='3' cols='60' name=\"").append(name).append("\">").append(value != null ? value : "").append("</textarea>");
			echo.append("</td></tr>\n");
		}

        @Override
        public void applyRequestValues(Request request, V entity)
        {
			String value = request.getParameterString(this.name);
			setter.accept(entity,value);
        }
    }

	/**
	 * Erzeugt ein Eingabefeld (Editor) in Form einer Textarea.
	 * @param label Der Label
	 * @param getter Der Getter fuer den momentanen Wert
	 * @param setter Der Setter fuer den momentanen Wert
	 */
	public TextAreaGenerator<E> textArea(String label, Function<E,String> getter, BiConsumer<E,String> setter)
	{
		return custom(new TextAreaGenerator<>(label, generateName(getter.getClass().getSimpleName()), getter, setter));
	}

	public static class FieldGenerator<V,T> implements CustomFieldGenerator<V>
	{
		private final String label;
		private final String name;
		private final Class<?> viewType;
		private final Class<T> dataType;
		private final Function<V,T> getter;
        private final BiConsumer<V,T> setter;
		private final Map<Serializable,Object> selectionOptions = new LinkedHashMap<>();
		private boolean readOnly;

		public FieldGenerator(String label, String name, Class<?> viewType, Class<T> dataType, Function<V,T> getter, BiConsumer<V,T> setter)
		{
			this.label = label;
			this.name = name;
			this.viewType = viewType;
			this.getter = getter;
            this.setter = setter;
			this.dataType = dataType;

            if( this.viewType.isAnnotationPresent(Entity.class) )
			{
				this.selectionOptions.putAll(generateSelectionOptions(this.viewType));
			}
		}

		public FieldGenerator<V,T> withOptions(Map<? extends Serializable, ?> options)
		{
			this.selectionOptions.clear();
			this.selectionOptions.putAll(options);
			return this;
		}

		public FieldGenerator<V,T> withNullOption(String label)
		{
			this.selectionOptions.put(null, label);
			return this;
		}

		public FieldGenerator<V,T> readOnly(boolean readOnly)
		{
			this.readOnly = readOnly;
			return this;
		}

		@Override
		public void generate(Writer echo, V entity) throws IOException
		{
            T value = getter.apply(entity);

			echo.append("<tr>");
			echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>");
			echo.append("<td>");
			if (Cargo.class.isAssignableFrom(viewType))
			{
				// TODO: Readonly-Support
				echo.append("<input type=\"hidden\" name=\"").append(name).append("\" id='").append(name).append("' value=\"").append(value != null ? value.toString() : new Cargo().toString()).append("\">");
				echo.append("<script type='text/javascript'>$(document).ready(function(){new CargoEditor('#").append(name).append("')});</script>");
			}
			else if (viewType.isAnnotationPresent(Entity.class) || !this.selectionOptions.isEmpty() )
			{
				editEntityBySelection(echo, name, viewType, value);
			}
			else if (Boolean.class.isAssignableFrom(viewType))
			{
				boolean bool = false;
				if (value != null)
				{
					bool = (Boolean) value;
				}
				echo.append("<input type=\"checkbox\" name=\"").append(name).append("\" value=\"true\" ").append(bool ? "checked='checked' " : "").append(readOnly ? "disabled='disabled' " : "").append(" \">");
			}
			else
			{
				echo.append("<input type=\"text\" ").append("id=\"").append(name).append("\" ").append(readOnly ? "disable='disabled' " : "").append("name=\"").append(name).append("\" value=\"").append(value != null ? value.toString() : "").append("\">");
				if( Number.class.isAssignableFrom(viewType) )
				{
					writeAutoNumberJavaScript(echo);
				}
			}
			echo.append("</td></tr>\n");
		}

		private void writeAutoNumberJavaScript(Writer echo) throws IOException
		{
			int mDec = 0;
			Number minValue = -999999999.99;
			Number maxValue = 999999999.99;
			if( viewType == Double.class || viewType == Float.class || viewType == BigDecimal.class )
			{
				mDec = 8;
			}
			if( viewType == Integer.class )
			{
				minValue = Integer.MIN_VALUE;
				maxValue = Integer.MAX_VALUE;
			}
			else if( viewType == Long.class )
			{
				minValue = Long.MIN_VALUE;
				maxValue = Long.MAX_VALUE;
			}
			else if( viewType == Short.class )
			{
				minValue = Short.MIN_VALUE;
				maxValue = Short.MAX_VALUE;
			}
			else if( viewType == Byte.class )
			{
				minValue = Byte.MIN_VALUE;
				maxValue = Byte.MAX_VALUE;
			}

			echo.append("<script type=\"text/javascript\">\n");
			echo.append("$('#").append(name).append("').autoNumeric('init', {aSep:'', vMin:").append(minValue.toString())
					.append(", vMax:").append(maxValue.toString())
					.append(", lZero: 'deny', mDec:").append(Integer.toString(mDec)).append("});\n");
			echo.append("</script>");
		}

		@SuppressWarnings("unchecked")
		@Override
        public void applyRequestValues(Request request, V entity)
        {
			if( this.readOnly )
			{
				return;
			}
			Class<?> type = this.dataType;
			// TODO: Datentyp aus Lambda bestimmen - leider nicht so einfach wegen type erasure :(
			String val = request.getParameter(this.name);
			if( val == null )
			{
				return;
			}
			if( Integer.class.isAssignableFrom(type) )
			{
				setter.accept(entity, (T)Integer.valueOf(val));
			}
			else if( Double.class.isAssignableFrom(type) )
			{
				setter.accept(entity, (T)Double.valueOf(val));
			}
			else if( String.class.isAssignableFrom(type) )
			{
				setter.accept(entity, (T)val);
			}
			else if( Cargo.class.isAssignableFrom(type) )
			{
				setter.accept(entity, (T)new Cargo(Cargo.Type.ITEMSTRING, val));
			}
			else if( Boolean.class.isAssignableFrom(type) )
			{
				setter.accept(entity, (T)Boolean.valueOf(val));
			}
			else
			{
				throw new UnsupportedOperationException("Datentyp "+type.getName()+" nicht unterstuetzt");
			}
		}

        private Map<Serializable, Object> generateSelectionOptions(Class<?> entityClass)
		{
			Map<Serializable,Object> result = new LinkedHashMap<>();
			org.hibernate.Session db = ContextMap.getContext().getDB();

			List<?> editities = Common.cast(db.createCriteria(entityClass).list());
			for (Object entity : editities)
			{
				Serializable identifier = db.getIdentifier(entity);
				result.put(identifier, entity);
			}
			return result;
		}

		private void editEntityBySelection(Writer echo, String name, Class<?> type, Object value) throws IOException
		{
			echo.append("<select size=\"1\" ").append(readOnly ? "disabled='disabled' " : "").append("name=\"").append(name).append("\">");
			org.hibernate.Session db = ContextMap.getContext().getDB();

			Serializable selected = -1;
			if (type.isInstance(value) && type.isAnnotationPresent(Entity.class))
			{
				selected = db.getIdentifier(value);
			}
			else if (value instanceof Serializable)
			{
				selected = (Serializable) value;
			}

			boolean containsIdentifier = this.selectionOptions.containsKey(selected);

			for (Map.Entry<Serializable, Object> entry : this.selectionOptions.entrySet())
			{
				Serializable identifier = entry.getKey();
				echo.append("<option value=\"").append(identifier != null ? identifier.toString() : "").append("\" ");
				if( (identifier == null && !containsIdentifier) || (containsIdentifier && identifier != null && identifier.equals(selected)) ) {
					echo.append("selected=\"selected\"");
				}
				String label;
				if( entry.getValue() instanceof String )
				{
					label = (String)entry.getValue();
				}
				else
				{
					label = new ObjectLabelGenerator().generateFor(identifier, entry.getValue());
				}
				echo.append(">").append(label).append("</option>");
			}

			echo.append("</select>");
		}
	}

	/**
	 * Erzeugt ein Eingabefeld (Editor) fuer einen bestimmten Datentyp. Das konkret erzeugte Eingabefeld
	 * kann von Datentyp zu Datentyp unterschiedlich sein.
	 * @param label Das Anzeigelabel
	 * @param viewType Der Datentyp des Views
	 * @param dataType Der Datentyp des Models
	 * @param getter Der getter fuer den momentanen Wert
     * @param setter Der setter fuer den momentanen Wert
	 */
	public <T> FieldGenerator<E,T> field(String label, Class<?> viewType, Class<T> dataType, Function<E,T> getter, BiConsumer<E,T> setter)
	{
		return custom(new FieldGenerator<>(label, generateName(getter.getClass().getSimpleName()), viewType, dataType, getter, setter));
	}

	/**
	 * Erzeugt ein Eingabefeld (Editor) fuer einen bestimmten Datentyp. Das konkret erzeugte Eingabefeld
	 * kann von Datentyp zu Datentyp unterschiedlich sein.
	 * @param label Das Anzeigelabel
	 * @param type Der Datentyp des Views und des Models
	 * @param getter Der getter fuer den momentanen Wert
	 * @param setter Der setter fuer den momentanen Wert
	 */
	public <T> FieldGenerator<E,T> field(String label, Class<T> type, Function<E,T> getter, BiConsumer<E,T> setter)
	{
		return field(label, type, type, getter, setter);
	}

	private String generateName(String suffix)
	{
		return "field"+(counter++)+"_"+suffix.replace('/', '_').replace('$', '_');
	}

	public void generateForm(E entity)
	{
		try
		{
			for (CustomFieldGenerator<E> field : fields)
			{
				field.generate(echo, entity);
			}

			if( modus == EditorMode.UPDATE && !updateTasks.isEmpty() )
			{
				StringBuilder str = new StringBuilder("<ul>");
				for (EditorForm8.Job<E, ?> tJob : updateTasks)
				{
					Collection<?> jobParams = tJob.supplier.apply(entity);
					str.append("<li>").append(jobParams.size()).append("x ").append(tJob.name).append("</li>");
				}
				str.append("</ul>");
				new LabelGenerator<>("Bei Aktualisierung", (e) -> str.toString()).generate(echo, entity);
			}

			String label = modus == EditorMode.UPDATE ? "Aktualisieren" : "Hinzufügen";
			echo.append("<tr><td colspan='2'></td><td><input type=\"submit\" name=\"change\" value=\"").append(label).append("\"></td></tr>\n");
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
    
    public void applyRequestValues(Request request, E entity) throws IOException
	{
        for (CustomFieldGenerator<E> field : fields)
        {
            field.applyRequestValues(request, entity);
        }
    }

	public EditorForm8<E> ifAdding()
	{
		if( modus == EditorMode.CREATE )
		{
			return this;
		}
		return new EditorForm8<>(EditorMode.CREATE, action, page, new StringWriter());
	}

	public EditorForm8<E> ifUpdating()
	{
		if( modus == EditorMode.UPDATE )
		{
			return this;
		}
		return new EditorForm8<>(EditorMode.UPDATE, action, page, new StringWriter());
	}
}
