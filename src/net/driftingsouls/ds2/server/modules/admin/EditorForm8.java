package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.*;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import org.apache.commons.fileupload.FileItem;

import javax.persistence.Entity;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Klasse zum Erstellen eines Eingabeformulars.
 */
class EditorForm8
{
	private Writer echo;
	private int action;
	private String page;
	private List<CustomFieldGenerator> fields = new ArrayList<>();
	private int counter;

	public EditorForm8(int action, String page, Writer echo)
	{
		this.echo = echo;
		this.action = action;
		this.page = page;
		this.counter = 0;
	}

	/**
	 * Standardinterface fuer Feldgeneratoren. Jede Instanz generiert genau
	 * ein Feld fuer ein konkretes Form.
	 */
	public static interface CustomFieldGenerator
	{
		/**
		 * Generiert den HTML-Code fuer das Eingabefeld.
		 * @param echo Der Writer in den der HTML-Code geschrieben werden soll
		 * @throws java.io.IOException Bei I/O-Fehlern
		 */
		public void generate(Writer echo) throws IOException;

        public void applyRequestValues(Request request) throws IOException;
    }

	/**
	 * Fuegt einen Generator fuer ein Eingabefeld zum Form hinzu.
	 * @param generator Der Generator
	 * @param <T> Der Typ des Generators
	 * @return Der Generator
	 */
	public <T extends CustomFieldGenerator> T custom(T generator)
	{
		fields.add(generator);
		return generator;
	}

	public class DynamicContentFieldGenerator implements CustomFieldGenerator
	{
		private String label;
		private String name;
		private Supplier<String> getter;
		private Consumer<String> setter;
		private boolean withRemove;

		public DynamicContentFieldGenerator(String label, String name, Supplier<String> getter, Consumer<String> setter)
		{
			this.label = label;
			this.name = name;
			this.getter = getter;
			this.setter = setter;
			this.withRemove = false;
		}

		@Override
		public void generate(Writer echo) throws IOException
		{
			echo.append("<tr class='dynamicContentEdit'>");

			writeCommonDynamicContentPart(label, name, getter.get());

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
        public void applyRequestValues(Request request) throws IOException
        {
			String img = processDynamicContent(request, this.name, getter.get());
            setter.accept(img);
        }

        public DynamicContentFieldGenerator withRemove()
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
	public DynamicContentFieldGenerator dynamicContentField(String label, Supplier<String> getter, Consumer<String> setter)
	{
		return custom(new DynamicContentFieldGenerator(label, generateName(getter.getClass().getSimpleName()), getter, setter));
	}

	public class LabelGenerator<T> implements CustomFieldGenerator
	{
		private final String label;
		private final Supplier<T> getter;

		public LabelGenerator(String label, Supplier<T> getter)
		{
			this.label = label;
			this.getter = getter;
		}

		@Override
		public void generate(Writer echo) throws IOException
		{
			T value = getter.get();
			echo.append("<tr>");
			echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>").append("<td>").append(value != null ? value.toString() : "").append("</td></tr>\n");
		}

        @Override
        public void applyRequestValues(Request request)
        {
        }
    }

	/**
	 * Erzeugt ein Eingabefeld (Editor) in Form eines nicht editierbaren Werts.
	 * @param label Der Label zum Wert
	 * @param getter Der getter des Werts
	 */
	public <T> LabelGenerator label(String label, Supplier<T> getter)
	{
		return custom(new LabelGenerator<T>(label, getter));
	}

	public class TextAreaGenerator implements CustomFieldGenerator
	{
		private final String label;
		private final String name;
		private final Supplier<String> getter;
		private final Consumer<String> setter;

		public TextAreaGenerator(String label, String name, Supplier<String> getter, Consumer<String> setter)
		{
			this.label = label;
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		@Override
		public void generate(Writer echo) throws IOException
		{
			String value = getter.get();
			echo.append("<tr>");
			echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>");
			echo.append("<td>");
			echo.append("<textarea rows='3' cols='60' name=\"").append(name).append("\">").append(value != null ? value : "").append("</textarea>");
			echo.append("</td></tr>\n");
		}

        @Override
        public void applyRequestValues(Request request)
        {
			String value = request.getParameterString(this.name);
			setter.accept(value);
        }
    }

	/**
	 * Erzeugt ein Eingabefeld (Editor) in Form einer Textarea.
	 * @param label Der Label
	 * @param getter Der Getter fuer den momentanen Wert
	 * @param setter Der Setter fuer den momentanen Wert
	 */
	public TextAreaGenerator textArea(String label, Supplier<String> getter, Consumer<String> setter)
	{
		return custom(new TextAreaGenerator(label, generateName(getter.getClass().getSimpleName()), getter, setter));
	}

	public class FieldGenerator<T> implements CustomFieldGenerator
	{
		private final String label;
		private final String name;
		private final Class<?> viewType;
		private final Class<T> dataType;
		private final Supplier<T> getter;
        private final Consumer<T> setter;
		private final Map<Serializable,Object> selectionOptions = new LinkedHashMap<>();
		private boolean readOnly;

		public FieldGenerator(String label, String name, Class<?> viewType, Class<T> dataType, Supplier<T> getter, Consumer<T> setter)
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

		public FieldGenerator withOptions(Map<? extends Serializable, ?> options)
		{
			this.selectionOptions.clear();
			this.selectionOptions.putAll(options);
			return this;
		}

		public FieldGenerator readOnly(boolean readOnly)
		{
			this.readOnly = readOnly;
			return this;
		}

		@Override
		public void generate(Writer echo) throws IOException
		{
            T value = getter.get();

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
        public void applyRequestValues(Request request)
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
				setter.accept((T)Integer.valueOf(val));
			}
			else if( Double.class.isAssignableFrom(type) )
			{
				setter.accept((T)Double.valueOf(val));
			}
			else if( String.class.isAssignableFrom(type) )
			{
				setter.accept((T)val);
			}
			else if( Cargo.class.isAssignableFrom(type) )
			{
				setter.accept((T)new Cargo(Cargo.Type.ITEMSTRING, val));
			}
			else if( Boolean.class.isAssignableFrom(type) )
			{
				setter.accept((T)Boolean.valueOf(val));
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

			for (Map.Entry<Serializable, Object> entry : this.selectionOptions.entrySet())
			{
				Serializable identifier = entry.getKey();
				echo.append("<option value=\"").append(identifier.toString()).append("\" ").append(identifier.equals(selected) ? "selected=\"selected\"" : "").append(">").append(AbstractEditPlugin.generateLabelFor(identifier, entry.getValue())).append("</option>");
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
	public <T> FieldGenerator field(String label, Class<?> viewType, Class<T> dataType, Supplier<T> getter, Consumer<T> setter)
	{
		return custom(new FieldGenerator<T>(label, generateName(getter.getClass().getSimpleName()), viewType, dataType, getter, setter));
	}

	/**
	 * Erzeugt ein Eingabefeld (Editor) fuer einen bestimmten Datentyp. Das konkret erzeugte Eingabefeld
	 * kann von Datentyp zu Datentyp unterschiedlich sein.
	 * @param label Das Anzeigelabel
	 * @param type Der Datentyp des Views und des Models
	 * @param getter Der getter fuer den momentanen Wert
	 * @param setter Der setter fuer den momentanen Wert
	 */
	public <T> FieldGenerator field(String label, Class<T> type, Supplier<T> getter, Consumer<T> setter)
	{
		return field(label, type, type, getter, setter);
	}

	private String generateName(String suffix)
	{
		return "field"+(counter++)+"_"+suffix.replace('/', '_').replace('$', '_');
	}

	public void generateForm()
	{
		try
		{
			for (CustomFieldGenerator field : fields)
			{
				field.generate(echo);
			}

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
    
    public void applyRequestValues(Request request) throws IOException
	{
        for (CustomFieldGenerator field : fields)
        {
            field.applyRequestValues(request);
        }
    }
}
