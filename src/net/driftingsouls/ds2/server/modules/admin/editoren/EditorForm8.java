package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.*;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import org.apache.commons.fileupload.FileItem;

import javax.persistence.Entity;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Klasse zum Erstellen eines Eingabeformulars.
 */
public class EditorForm8<V>
{
	private final EditorMode modus;
	private Writer echo;
	private int action;
	private String page;
	private List<CustomFieldGenerator<V>> fields = new ArrayList<>();
	private int counter;

	public EditorForm8(EditorMode modus, int action, String page, Writer echo)
	{
		this.modus = modus;
		this.echo = echo;
		this.action = action;
		this.page = page;
		this.counter = 0;
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
	public <T extends CustomFieldGenerator<V>> T custom(T generator)
	{
		fields.add(generator);
		return generator;
	}

	public class DynamicContentFieldGenerator<V> implements CustomFieldGenerator<V>
	{
		private String label;
		private String name;
		private Function<V,String> getter;
		private BiConsumer<V,String> setter;
		private boolean withRemove;

		public DynamicContentFieldGenerator(String label, String name, Function<V,String> getter, BiConsumer<V,String> setter)
		{
			this.label = label;
			this.name = name;
			this.getter = getter;
			this.setter = setter;
			this.withRemove = false;
		}

		@Override
		public void generate(Writer echo, V entity) throws IOException
		{
			echo.append("<tr class='dynamicContentEdit'>");

			writeCommonDynamicContentPart(label, name, getter.apply(entity));

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
            setter.accept(entity,img);
        }

        public DynamicContentFieldGenerator<V> withRemove()
		{
			this.withRemove = true;
			return this;
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
	}

	/**
	 * Generiert ein Eingabefeld (Editor) fuer ein via {@link net.driftingsouls.ds2.server.framework.DynamicContent}
	 * gemanagetes Bild.
	 * @param label Der Label fuer das Eingabefeld
	 * @param getter Der getter für das Feld
	 * @return Der erzeugte Generator
	 */
	public DynamicContentFieldGenerator<V> dynamicContentField(String label, Function<V,String> getter, BiConsumer<V,String> setter)
	{
		return custom(new DynamicContentFieldGenerator<V>(label, generateName(getter.getClass().getSimpleName()), getter, setter));
	}

	public class LabelGenerator<V,T> implements CustomFieldGenerator<V>
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
	public <T> LabelGenerator<V,T> label(String label, Function<V,T> getter)
	{
		return custom(new LabelGenerator<V,T>(label, getter));
	}

	public class TextAreaGenerator<V> implements CustomFieldGenerator<V>
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
	public TextAreaGenerator<V> textArea(String label, Function<V,String> getter, BiConsumer<V,String> setter)
	{
		return custom(new TextAreaGenerator<V>(label, generateName(getter.getClass().getSimpleName()), getter, setter));
	}

	public class FieldGenerator<V,T> implements CustomFieldGenerator<V>
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
				editEntityBySelection(name, viewType, value);
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
				echo.append("<input type=\"text\" ").append(readOnly ? "disable='disabled' " : "").append("name=\"").append(name).append("\" value=\"").append(value != null ? value.toString() : "").append("\">");
			}
			echo.append("</td></tr>\n");
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

		private void editEntityBySelection(String name, Class<?> type, Object value) throws IOException
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
					label = AbstractEditPlugin8.generateLabelFor(identifier, entry.getValue());
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
	public <T> FieldGenerator<V,T> field(String label, Class<?> viewType, Class<T> dataType, Function<V,T> getter, BiConsumer<V,T> setter)
	{
		return custom(new FieldGenerator<V,T>(label, generateName(getter.getClass().getSimpleName()), viewType, dataType, getter, setter));
	}

	/**
	 * Erzeugt ein Eingabefeld (Editor) fuer einen bestimmten Datentyp. Das konkret erzeugte Eingabefeld
	 * kann von Datentyp zu Datentyp unterschiedlich sein.
	 * @param label Das Anzeigelabel
	 * @param type Der Datentyp des Views und des Models
	 * @param getter Der getter fuer den momentanen Wert
	 * @param setter Der setter fuer den momentanen Wert
	 */
	public <T> FieldGenerator<V,T> field(String label, Class<T> type, Function<V,T> getter, BiConsumer<V,T> setter)
	{
		return field(label, type, type, getter, setter);
	}

	private String generateName(String suffix)
	{
		return "field"+(counter++)+"_"+suffix.replace('/', '_').replace('$', '_');
	}

	public void generateForm(V entity)
	{
		try
		{
			for (CustomFieldGenerator<V> field : fields)
			{
				field.generate(echo, entity);
			}

			String label = modus == EditorMode.UPDATE ? "Aktualisieren" : "Hinzufügen";
			echo.append("<tr><td colspan='2'></td><td><input type=\"submit\" name=\"change\" value=\"").append(label).append("\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
			echo.append("</div>");
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
    
    public void applyRequestValues(Request request, V entity) throws IOException
	{
        for (CustomFieldGenerator<V> field : fields)
        {
            field.applyRequestValues(request, entity);
        }
    }

	public EditorForm8<V> ifAdding()
	{
		if( modus == EditorMode.CREATE )
		{
			return this;
		}
		return new EditorForm8<>(EditorMode.CREATE, action, page, new StringWriter());
	}

	public EditorForm8<V> ifUpdating()
	{
		if( modus == EditorMode.UPDATE )
		{
			return this;
		}
		return new EditorForm8<>(EditorMode.UPDATE, action, page, new StringWriter());
	}
}
